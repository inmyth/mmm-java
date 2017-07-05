package com.mbcu.mmm.sequences;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Base {
	protected Logger LOGGER;
	
	public Base(Logger logger) {
		this.LOGGER = logger;
	}
	
	
	protected void log(String message, Level...level){
		Level l = level.length > 0 ? level[0] : Level.FINE;
		this.LOGGER.log(l, message);
		System.out.println(message);		
	}

}
