package com.mbcu.mmm.models.internal;

import java.util.ArrayList;
import java.util.List;

import com.ripple.core.coretypes.Amount;

public class PartialOrder {
	
	private final List<RLOrder> parts = new ArrayList<>();
	
	
	public void add (RLOrder part){
		parts.add(part);
	}
	
	public Amount sum(){
		return parts.stream()
				.map(RLOrder::getQuantity)			
				.reduce((x, y) -> x.add(y))
				.get();		
	}
	

}
