package com.mbcu.mmm.sequences;

import java.util.logging.Level;

import com.mbcu.mmm.main.WebSocketClient;
import com.mbcu.mmm.models.internal.RLOrder;
import com.mbcu.mmm.rx.RxBus;
import com.mbcu.mmm.rx.RxBusProvider;
import com.mbcu.mmm.sequences.Common.OnLedgerClosed;
import com.mbcu.mmm.sequences.Common.OnOfferCreate;
import com.mbcu.mmm.sequences.Common.OnResponseFail;
import com.mbcu.mmm.sequences.state.State;
import com.mbcu.mmm.utils.MyLogger;
import com.ripple.core.coretypes.hash.Hash256;
import com.ripple.core.serialized.enums.EngineResult;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class Txc extends Base {

	private final RLOrder outbound;
	private final Hash256 hash;
	private final int seq;
	private final int maxLedger;
	private RxBus bus = RxBusProvider.getInstance();
	private final CompositeDisposable disposables = new CompositeDisposable();
	
	private Txc(RLOrder outbound, Hash256 hash, int seq, int maxLedger) {
		super(MyLogger.getLogger(String.format(Txc.class.getName())), null);
		this.outbound = outbound;
		this.hash = hash;
		this.seq = seq;
		this.maxLedger = maxLedger;
	}

	private void initBus() {
		disposables.add(bus.toObservable().subscribeOn(Schedulers.newThread()).
		subscribeWith(new DisposableObserver<Object>() {

			@Override
			public void onNext(Object o) {
				if (o instanceof Common.OnOfferCreate) {
					OnOfferCreate event = (OnOfferCreate) o;
					if (event.sequence.intValue() == seq) {
						disposables.dispose();
						bus.send(new RequestRemove(seq));		
					}
				} 
				else if (o instanceof Common.OnLedgerClosed) {
						OnLedgerClosed event = (OnLedgerClosed) o;
						if (event.ledgerEvent.getValidated() > maxLedger){ // failed to enter ledger
							disposables.dispose();
							bus.send(new RequestRemove(seq));
							bus.send(new State.OnOrderReady(outbound));	
						}
				} 				
				else if (o instanceof Common.OnResponseFail) {
					OnResponseFail event = (OnResponseFail) o;
					String er = event.engineResult;

					if (er.equals("terQUEUED")) {
						disposables.dispose();
						bus.send(new RequestRemove(seq));
						return;
					}					
//					if (er.equals(EngineResult.tefALREADY.toString())){
//						// This means a tx with the same sequence number is already queued.
//						disposables.dispose();
//						bus.send(new RequestRemove(seq));
//						return;
//					}
					if (er.equals(EngineResult.terPRE_SEQ.toString())	|| er.equals(EngineResult.tefPAST_SEQ.toString())) {
						disposables.dispose();
						bus.send(new RequestSequenceSync());
						bus.send(new RequestRemove(seq));
						bus.send(new State.OnOrderReady(outbound));
						return;
					}

					if (er.equals(EngineResult.terINSUF_FEE_B)) {
						// no fund
						bus.send(new WebSocketClient.WSRequestDisconnect());
						return;
					}
					
					if (er.equals(EngineResult.telINSUF_FEE_P)){
						// retry next ledger
						disposables.dispose();
						bus.send(new RequestWaitNextLedger());
						bus.send(new RequestRemove(seq));
						bus.send(new State.OnOrderReady(outbound));
					}
						
					// any other error 				
					// int usableSequence = event.sequence.intValue();
					// revertSequence(usableSequence);
					// SubmitCache retry = pending.get(usableSequence);
					// pending.remove(event.sequence.intValue());
					disposables.dispose();
					bus.send(new RequestRemove(seq));
					bus.send(new State.OnOrderReady(outbound));
				}	
			}

			@Override
			public void onError(Throwable e) {
				log(e.getMessage(), Level.SEVERE);				
			}

			@Override
			public void onComplete() {
				// TODO Auto-generated method stub				
			}
		}));
	
	}

	public int getSeq() {
		return seq;
	}
	
	public static Txc newInstance(RLOrder outbound, Hash256 hash, int seq, int maxLedger) {
		Txc res = new Txc(outbound, hash, seq,maxLedger);
		res.initBus();
		return res;
	}
	
	public static class RequestRemove {
		public int seq;

		public RequestRemove(int seq) {
			super();
			this.seq = seq;
		}
	}
	
	public static class RequestSequenceSync{}
	
	public static class RequestWaitNextLedger{}
}
