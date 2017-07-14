package com.mbcu.mmm.sequences.state;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import com.mbcu.mmm.main.WebSocketClient;
import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.models.internal.LedgerEvent;
import com.mbcu.mmm.models.internal.cache.CacheEvents;
import com.mbcu.mmm.models.internal.cache.CacheEvents.RequestRemove;
import com.mbcu.mmm.models.internal.cache.SubmitCache;
import com.mbcu.mmm.rx.RxBus;
import com.mbcu.mmm.rx.RxBusProvider;
import com.mbcu.mmm.sequences.Base;
import com.mbcu.mmm.sequences.Common;
import com.mbcu.mmm.sequences.Common.OnLedgerClosed;
import com.mbcu.mmm.sequences.Common.OnOfferCreate;
import com.mbcu.mmm.sequences.Common.OnResponseFail;
import com.mbcu.mmm.sequences.Submitter;
import com.mbcu.mmm.sequences.Submitter.OnSubmitCache;
import com.mbcu.mmm.utils.MyLogger;
import com.ripple.core.coretypes.uint.UInt32;
import com.ripple.core.serialized.enums.EngineResult;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class State extends Base {
	
	private final AtomicInteger ledgerClosed = new AtomicInteger(0);
	private final AtomicInteger ledgerValidated = new AtomicInteger(0);
	private final AtomicInteger sequence = new AtomicInteger(0);
	private final ConcurrentHashMap<Integer, SubmitCache> pending = new ConcurrentHashMap<>();
	private RxBus bus = RxBusProvider.getInstance();

	public State(Config config) {
		super(MyLogger.getLogger(Common.class.getName()), config);
		
		bus.toObservable()
		.subscribeOn(Schedulers.newThread())
		.subscribe(new Observer<Object>() {

			@Override
			public void onSubscribe(Disposable d) {
			}

			@Override
			public void onNext(Object o) {
				if (o instanceof Common.OnOfferCreate){
					OnOfferCreate event = (OnOfferCreate) o;
					if (config.getCredentials().addressEquals(event.account)){
						setSequence(event.sequence);
						pending.remove(event.sequence.intValue());
					}
				}else if (o instanceof Submitter.OnSubmitCache){				
					OnSubmitCache event = (Submitter.OnSubmitCache) o;
					pending.putIfAbsent(event.sequence, event.cache);
					bus.send(new Submitter.SubmitTxBlob(event.signed.tx_blob));
				}else if (o instanceof Common.OnLedgerClosed){
					OnLedgerClosed event = (OnLedgerClosed) o;		
					log(event.ledgerEvent.toString());
					setLedgerIndex(event.ledgerEvent);
				}else if (o instanceof CacheEvents.RequestRemove){
					RequestRemove event = (RequestRemove) o;
					pending.remove(event.seq);
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
		});		
	}
	
	public void setLedgerIndex(LedgerEvent event){
		synchronized (ledgerValidated) {
			this.ledgerClosed.set(event.getClosed());
			this.ledgerValidated.set(event.getValidated());
		}
	}
	
	public static State newInstance(Config config){
		return new State(config);
		
	}
	
	public void setSequence(UInt32 seq){
		synchronized (sequence) {
			if (this.sequence.get() < seq.intValue()){
				sequence.set(seq.intValue());
			}
		}
	}
	
	public int getSequence(){
		return this.sequence.intValue();
	}
	
	public int getLedgerClosed(){
		return this.ledgerClosed.get();
	}
	
	public int getLedgerValidated() {
		return this.ledgerValidated.get();
	}
	
	public int getIncrementSequence(){
		return this.sequence.getAndIncrement();
	}
	
	private void revertSequence(int usableSequence){
		if (usableSequence < this.sequence.get()){
			this.sequence.set(usableSequence);
		}
	}
	
	public static class OnOrderValidated{
		public int seq;

		public OnOrderValidated(int seq) {
			super();
			this.seq = seq;
		}
			
	}

}
