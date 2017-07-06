package com.mbcu.mmm.sequences;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.rx.RxBus;
import com.mbcu.mmm.rx.RxBusProvider;

public class Base {
	protected RxBus bus = RxBusProvider.getInstance();
	protected Config config;
	protected Logger LOGGER;
	
	public Base(Logger logger, Config config) {
		this.LOGGER = logger;
		this.config = config;
	}
	
	
	protected void log(String message, Level...level){
		Level l = level.length > 0 ? level[0] : Level.FINE;
		this.LOGGER.log(l, message);
		System.out.println(message);		
	}

}
