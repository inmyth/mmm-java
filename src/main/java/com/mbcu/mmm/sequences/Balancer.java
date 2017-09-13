package com.mbcu.mmm.sequences;

import java.util.ArrayList;
import java.util.List;

import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.models.internal.RLOrder;
import com.mbcu.mmm.rx.BusBase;
import com.mbcu.mmm.sequences.state.State;
import com.mbcu.mmm.utils.MyLogger;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public class Balancer extends Base{
	
	private final List<Orderbook> perm = new ArrayList<>();
	
	public Balancer(Config config) {
		super(MyLogger.getLogger(Starter.class.getName()), config);
		buildOrderbooks();
		bus.toObservable()
		.subscribe(new Observer<Object>() {

			@Override
			public void onSubscribe(Disposable d) {
				// TODO Auto-generated method stub				
			}

			@Override
			public void onNext(Object o) {			
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
	
	private void buildOrderbooks(){
		super.config.getBotConfigMap().values().stream()
		.forEach(botConfig -> {
			perm.add(Orderbook.newInstance(botConfig, config));
		});		
	}
	
	private void seed(){
		super.config.getBotConfigMap().values()
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
	
	public static class OnRequestNonOrderbookRLOrder extends BusBase{		
		public String pair;

		public OnRequestNonOrderbookRLOrder(String pair) {
			super();
			this.pair = pair;
		}
	}

	
}
