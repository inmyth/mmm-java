package com.mbcu.mmm.models.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mbcu.mmm.models.Base;

public class Orderbook extends Base{
	
	private final String pair;
	private final List<RLOrder> buys = Collections.synchronizedList(new ArrayList<>());
	private final List<RLOrder> sels = Collections.synchronizedList(new ArrayList<>());

	private Orderbook(String pair) {
		super();
		this.pair = pair;
	}

	
	private void pushRLOrder(RLOrder in){
	}

	private void balancer(){
		
	}
	
	@Override
	public String stringify() {
		// TODO Auto-generated method stub
		return null;
	}

}
