package com.mbcu.mmm.models.internal;

import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class TRLOrder {
	
	private final RLOrder origin;
	private final RLOrder now;
	
	public TRLOrder(RLOrder origin, RLOrder now){
		this.origin = origin;
		this.now 		= now;
	}

	public RLOrder getNow() {
		return now;
	}


  public RLOrder getOrigin() {
		return origin;
	}
	
	public static ConcurrentMap<Integer, RLOrder> origins(ConcurrentMap<Integer, TRLOrder> in){
		return in.entrySet().stream()
    .collect(Collectors.toConcurrentMap(
        e -> e.getKey(),
        e -> e.getValue().getOrigin()
    ));
	}
	
	public TRLOrder updatedWith(RLOrder now){
		return new TRLOrder(origin, now);
	}

}
