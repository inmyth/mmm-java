package com.mbcu.mmm.sequences;

import com.mbcu.mmm.main.WebSocketClient;
import com.mbcu.mmm.models.internal.RLOrder;
import com.mbcu.mmm.rx.BusBase;
import com.mbcu.mmm.rx.RxBus;
import com.mbcu.mmm.rx.RxBusProvider;
import com.mbcu.mmm.sequences.Common.OnLedgerClosed;
import com.mbcu.mmm.sequences.Common.OnOfferCreate;
import com.mbcu.mmm.sequences.Common.OnRPCTesFail;
import com.mbcu.mmm.sequences.Common.OnRPCTesSuccess;
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
	private boolean isTesSuccess;
	
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
				BusBase base = (BusBase) o;
				try {			
					if (base instanceof Common.OnOfferCreate) {
						OnOfferCreate event = (OnOfferCreate) o;
						if (event.sequence.intValue() == seq) {
							disposables.dispose();
							bus.send(new State.RequestRemoveCreate(seq));		
						}
					} 
					else if (base instanceof Common.OnLedgerClosed) {
						OnLedgerClosed event = (OnLedgerClosed) o;
						if (isTesSuccess && event.ledgerEvent.getValidated() > maxLedger){ // failed to enter ledger
							disposables.dispose();
							bus.send(new State.RequestSequenceSync());
							bus.send(new State.RequestRemoveCreate(seq));
							bus.send(new State.OnOrderReady(outbound, hash, " MaxLedger passed"));	
							return;
						}
					} 
					else if (base instanceof Common.OnRPCTesSuccess){
						OnRPCTesSuccess event = (OnRPCTesSuccess) o;
						if (event.hash.compareTo(hash) == 0){
							isTesSuccess = true;
						}
					}
					else if (base instanceof Common.OnRPCTesFail) {
						OnRPCTesFail event = (OnRPCTesFail) o;
						if (event.sequence.intValue() != seq || event.hash.compareTo(hash) != 0){
							return;
						}
						String er = event.engineResult;
						
						if (er.equals(EngineResult.terINSUF_FEE_B.toString())) {
							// no fund
							bus.send(new WebSocketClient.WSRequestDisconnect());
							return;
						}
						
						if (er.startsWith("ter")) {
	//					this includes terQUEUED and terPRE_SEQ and behave like tesSUCCESS
	//					https://www.xrpchat.com/topic/2654-transaction-failed-with-terpre_seq-but-still-executed/?page=2
	//					disposables.dispose();
	//					bus.send(new RequestRemove(seq));
							isTesSuccess = true;							
							return;
						}					
	//					if (er.equals(EngineResult.tefALREADY.toString())){
	//						// This means a tx with the same sequence number is already queued.
	//						disposables.dispose();
	//						bus.send(new RequestRemove(seq ));
	//						return;
	//					}
						if (er.equals(EngineResult.tefPAST_SEQ.toString())) {
							disposables.dispose();
							bus.send(new State.RequestSequenceSync());
							bus.send(new State.RequestRemoveCreate(seq));
							bus.send(new State.OnOrderReady(outbound, hash, " was tefPAST_SEQ"));
							return;
						}
					
						if (er.equals(EngineResult.telINSUF_FEE_P.toString())){
							// retry next ledger
							disposables.dispose();
							bus.send(new State.RequestWaitNextLedger());
							bus.send(new State.RequestSequenceSync());
							bus.send(new State.RequestRemoveCreate(seq));
							bus.send(new State.OnOrderReady(outbound, hash, " was telINSUF_FEE_P"));
							return;
						}
						
						
						disposables.dispose();
						bus.send(new State.RequestRemoveCreate(seq));
						bus.send(new Notifier.RequestEmailNotice(er, outbound.getCpair().getFw(), System.currentTimeMillis()));
	//					bus.send(new State.OnOrderReady(outbound, hash, "retry " + er));
					}	
				} catch (Exception e) {
					MyLogger.exception(LOGGER, base.toString(), e);		
					throw e;
				}			
			}

			@Override
			public void onError(Throwable e) {}

			@Override
			public void onComplete() {}
		}));

	}

	public int getSeq() {
		return seq;
	}
	
	public RLOrder getOutbound() {
		return outbound;
	}
	
	public static Txc newInstance(RLOrder outbound, Hash256 hash, int seq, int maxLedger) {
		Txc res = new Txc(outbound, hash, seq,maxLedger);
		res.initBus();
		return res;
	}
	
}
