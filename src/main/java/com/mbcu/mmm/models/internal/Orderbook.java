package com.mbcu.mmm.models.internal;

import java.util.concurrent.ConcurrentHashMap;

import com.mbcu.mmm.models.Base;
import com.mbcu.mmm.models.internal.RLOrder.Direction;
import com.mbcu.mmm.rx.RxBus;
import com.mbcu.mmm.rx.RxBusProvider;

public class Orderbook extends Base{
	
	private final BotConfig botConfig;
	private final ConcurrentHashMap<Integer, RLOrder> buys = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Integer, RLOrder> sels = new ConcurrentHashMap<>();
	private final RxBus bus = RxBusProvider.getInstance();

	private Orderbook(BotConfig botConfig) {
		super();
		this.botConfig = botConfig;
		
	}

	private RLOrder align(RLOrder in){
		if (botConfig.pair.equals(in.getPair())){
			return in;
		}
		return in.reverse();		
	}
	
	private void pushRLOrder(int seq, RLOrder in){
		if (in.getDirection().equals(Direction.BUY)){
			buys.put(seq, in);
		}
		else {
			sels.put(seq, in);
		}		
	}

	private void balancer(){
		
	}
	
	
	public static Orderbook newInstance(BotConfig botConfig){
		Orderbook res = new Orderbook(botConfig);
		return res;
	}
	
	@Override
	public String stringify() {
		return botConfig.pair;
	}

}
