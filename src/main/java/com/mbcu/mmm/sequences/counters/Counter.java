package com.mbcu.mmm.sequences.counters;

import com.mbcu.mmm.models.internal.RLOrder;

public interface Counter {
	
	public void onCounterReady(RLOrder counter);
	
	
	public static class CounterReady{
		public final RLOrder counter;

		public CounterReady(RLOrder counter) {
			super();
			this.counter = counter;
		}		
	}

}
