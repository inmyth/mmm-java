package com.mbcu.mmm.models.internal;

import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class TRLOrder {
	
	private final RLOrder origin;
	private RLOrder now;
	
	public TRLOrder(RLOrder origin) {
		this.origin = origin;
		this.now    = origin;
	}

	public RLOrder getNow() {
		return now;
	}

	public void setNow(RLOrder now) {
		this.now = now;
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

}
