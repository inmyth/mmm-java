package com.mbcu.mmm.sequences;

import com.mbcu.mmm.main.WebSocketClient;
import com.mbcu.mmm.models.internal.Cpair;
import com.mbcu.mmm.rx.BusBase;
import com.mbcu.mmm.rx.RxBus;
import com.mbcu.mmm.rx.RxBusProvider;
import com.mbcu.mmm.sequences.Common.OnLedgerClosed;
import com.mbcu.mmm.sequences.Common.OnOfferCanceled;
import com.mbcu.mmm.sequences.Common.OnRPCTesFail;
import com.mbcu.mmm.sequences.Common.OnRPCTesSuccess;
import com.mbcu.mmm.sequences.state.State;
import com.mbcu.mmm.utils.MyLogger;
import com.ripple.core.serialized.enums.EngineResult;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class Txd extends Base {

	private final Cpair cpair;
	private final int canSeq;
	private final int newSeq;
	private final int maxLedger;
	private final RxBus bus = RxBusProvider.getInstance();
	private boolean isTesSuccess;

	private final CompositeDisposable disposables = new CompositeDisposable();

	private Txd(Cpair cpair, int canSeq, int newSeq, int maxLedger) {
		super(MyLogger.getLogger(String.format(Txd.class.getName())), null);
		this.cpair = cpair;
		this.canSeq = canSeq;
		this.newSeq = newSeq;
		this.maxLedger = maxLedger;
	}

	private void initBus() {
		disposables
				.add(bus.toObservable().subscribeOn(Schedulers.newThread()).subscribeWith(new DisposableObserver<Object>() {

					@Override
					public void onNext(Object o) {
						BusBase base = (BusBase) o;
						try {
							if (base instanceof Common.OnOfferCanceled) {
								OnOfferCanceled event = (OnOfferCanceled) o;
								if (event.prevSeq.intValue() == canSeq) {
									disposables.dispose();
									bus.send(new State.RequestRemoveCancel(canSeq));
								}
							} else if (base instanceof Common.OnLedgerClosed) {
								OnLedgerClosed event = (OnLedgerClosed) o;
								if (isTesSuccess && event.ledgerEvent.getValidated() > maxLedger) { // failed
																																										// to
																																										// enter
																																										// ledger
									disposables.dispose();
									bus.send(new State.RequestSequenceSync());
									bus.send(new State.RequestRemoveCancel(canSeq));
									bus.send(new State.OnCancelReady(cpair.toString(), canSeq));
									return;
								}
							} else if (base instanceof Common.OnRPCTesSuccess) {
								OnRPCTesSuccess event = (OnRPCTesSuccess) o;
								if (event.sequence.intValue() == newSeq) {
									isTesSuccess = true;
								}
							} else if (base instanceof Common.OnRPCTesFail) {
								OnRPCTesFail event = (OnRPCTesFail) o;
								if (event.sequence.intValue() != newSeq) {
									return;
								}
								String er = event.engineResult;

								if (er.equals(EngineResult.terINSUF_FEE_B.toString())) {
									// no fund
									bus.send(new WebSocketClient.WSRequestDisconnect());
									return;
								}

								if (er.startsWith("ter")) {
									isTesSuccess = true;
									return;
								}
								if (er.equals(EngineResult.tefPAST_SEQ.toString())) {
									disposables.dispose();
									bus.send(new State.RequestSequenceSync());
									bus.send(new State.RequestRemoveCancel(canSeq));
									bus.send(new State.OnCancelReady(cpair.toString(), canSeq));
									return;
								}
								if (er.equals(EngineResult.telINSUF_FEE_P.toString())) {
									disposables.dispose();
									bus.send(new State.RequestWaitNextLedger());
									bus.send(new State.RequestSequenceSync());
									bus.send(new State.RequestRemoveCancel(canSeq));
									bus.send(new State.OnCancelReady(cpair.toString(), canSeq));
									return;
								}
								disposables.dispose();
								bus.send(new State.RequestRemoveCancel(canSeq));
								// bus.send(new State.OnOrderReady(outbound, hash, "retry " +
								// er));
							}
						} catch (Exception e) {
							MyLogger.exception(LOGGER, base.toString(), e);
							throw e;
						}
					}

					@Override
					public void onError(Throwable e) {
					}

					@Override
					public void onComplete() {
						// TODO Auto-generated method stub

					}
				}));

	}

	public int getCanSeq() {
		return canSeq;
	}

	public Cpair getPair() {
		return cpair;
	}

	public static Txd newInstance(Cpair cpair, int canSeq, int newSeq, int maxLedger) {
		Txd res = new Txd(cpair, canSeq, newSeq, maxLedger);
		res.initBus();
		return res;
	}

	public static class TxdMini {
		public final Cpair cpair;
		public final int canSeq;

		public TxdMini(Cpair cpair, int canSeq) {
			super();
			this.cpair = cpair;
			this.canSeq = canSeq;
		}

	}

}
