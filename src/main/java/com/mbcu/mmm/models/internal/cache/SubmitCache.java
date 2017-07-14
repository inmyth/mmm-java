package com.mbcu.mmm.models.internal.cache;

import java.util.logging.Level;

import com.mbcu.mmm.main.WebSocketClient;
import com.mbcu.mmm.models.internal.RLOrder;
import com.mbcu.mmm.rx.RxBus;
import com.mbcu.mmm.rx.RxBusProvider;
import com.mbcu.mmm.sequences.Base;
import com.mbcu.mmm.sequences.Common;
import com.mbcu.mmm.sequences.Common.OnLedgerClosed;
import com.mbcu.mmm.sequences.Common.OnOfferCreate;
import com.mbcu.mmm.sequences.Common.OnResponseFail;
import com.mbcu.mmm.sequences.Submitter;
import com.mbcu.mmm.utils.MyLogger;
import com.ripple.core.coretypes.hash.Hash256;
import com.ripple.core.serialized.enums.EngineResult;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class SubmitCache extends Base {

	private final RLOrder outbound;
	private Hash256 hash;
	private int seq;
	private int maxLedger;
	private RxBus bus = RxBusProvider.getInstance();
	private final CompositeDisposable disposables = new CompositeDisposable();
	
	private SubmitCache(RLOrder outbound, Hash256 hash, int maxLedger, int seq) {
		super(MyLogger.getLogger(String.format("%s sequence : %d", SubmitCache.class.getName(), seq)), null);
		this.outbound = outbound;
		this.maxLedger = maxLedger;
		this.hash = hash;
		this.seq = seq;
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
						bus.send(new CacheEvents.RequestRemove(seq));		
					}
				} else if (o instanceof Common.OnLedgerClosed) {
						OnLedgerClosed event = (OnLedgerClosed) o;
						if (event.ledgerEvent.getValidated() > maxLedger){ // failed to enter ledger
							disposables.dispose();
							bus.send(new CacheEvents.RequestRemove(seq));
							bus.send(new Submitter.Retry(outbound));	
						}
				}	else if (o instanceof Common.OnResponseFail) {
					OnResponseFail event = (OnResponseFail) o;

					if (event.engineResult.equals("terQUEUED")) {
						// "Held until escalated fee drops". May show up in later ledger.
						return;
					}
					
					if (event.engineResult.equals(EngineResult.tefALREADY.toString())){
						// This means a tx with the same sequence is already queued.
						disposables.dispose();
						bus.send(new CacheEvents.RequestRemove(seq));
						return;
					}

					if (event.engineResult.equals(EngineResult.terPRE_SEQ.toString())
							|| event.engineResult.equals(EngineResult.tefPAST_SEQ.toString())) {
						// this triggers Account Info
						return;
					}

					if (event.engineResult.equals(EngineResult.terINSUF_FEE_B)) {
						bus.send(new WebSocketClient.WSRequestDisconnect());
						return;
					}
						
					// any other error insuf_fee_p etc 
					
					// int usableSequence = event.sequence.intValue();
					// revertSequence(usableSequence);
					// SubmitCache retry = pending.get(usableSequence);
					// pending.remove(event.sequence.intValue());
					disposables.dispose();
					bus.send(new CacheEvents.RequestRemove(seq));
					bus.send(new Submitter.Retry(outbound));
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

	@Override
	public int hashCode() {
		return seq;
	}

	public static SubmitCache newInstance(RLOrder outbound, Hash256 hash, int maxLEdger, int sequence) {
		SubmitCache res = new SubmitCache(outbound, hash, maxLEdger, sequence);
		res.initBus();
		return res;
	}
}
