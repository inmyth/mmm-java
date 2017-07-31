package com.mbcu.mmm.sequences;

import java.util.concurrent.ConcurrentHashMap;

import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.models.internal.RLOrder;
import com.mbcu.mmm.sequences.state.State;
import com.mbcu.mmm.utils.MyLogger;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public class Balancer extends Base{
	
	private final ConcurrentHashMap<Integer, RLOrder> perm = new ConcurrentHashMap<>();
		
	public Balancer(Config config) {
		super(MyLogger.getLogger(Starter.class.getName()), config);
		bus.toObservable()
		.subscribe(new Observer<Object>() {

			@Override
			public void onSubscribe(Disposable d) {
				// TODO Auto-generated method stub				
			}

			@Override
			public void onNext(Object o) {
				if (o instanceof Starter.OnInitiated){
//					seed();
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
	
	private void seed(){
		config.getBotConfigMap().values()
		.stream()
		.map(bot -> { 
			return RLOrder.buildSeed(bot);
		})
		.flatMap(l->l.stream())
		.forEach(seed -> {
			System.out.println(seed.stringify());
			bus.send(new State.OnOrderReady(seed));		
		});
	}
	
	public static Balancer newInstance(Config config){
		return new Balancer(config);
	}
	

	
}
