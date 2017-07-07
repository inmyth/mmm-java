package com.mbcu.mmm.sequences.state;

import java.util.concurrent.atomic.AtomicInteger;

import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.rx.RxBus;
import com.mbcu.mmm.rx.RxBusProvider;
import com.mbcu.mmm.sequences.Common;
import com.mbcu.mmm.sequences.Common.OnOfferCreate;
import com.ripple.core.coretypes.uint.UInt32;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public class State {
	
	private AtomicInteger sequence = new AtomicInteger(0);
	
	private RxBus bus = RxBusProvider.getInstance();

	public State(Config config) {
		bus.toObservable()
		.subscribe(new Observer<Object>() {

			@Override
			public void onSubscribe(Disposable d) {
			}

			@Override
			public void onNext(Object o) {
				if (o instanceof Common.OnOfferCreate){
					OnOfferCreate event = (OnOfferCreate) o;
					if (event.account.address.equals(config.getCredentials().getAddress())){
						setSequence(event.sequence);
					}
				}
				
			}

			@Override
			public void onError(Throwable e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onComplete() {
				// TODO Auto-generated method stub
				
			}
		});
		
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
	
	public int getApplicableSequence(){
		return this.sequence.getAndIncrement();		
	}

}
