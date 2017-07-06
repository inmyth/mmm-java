package com.mbcu.mmm.sequences;

import java.util.concurrent.CountDownLatch;

import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.utils.MyLogger;

public class Starter extends Base{
  final CountDownLatch latch = new CountDownLatch(3);
  
	public Starter(Config config) {
		super(MyLogger.getLogger(Starter.class.getName()), config);
		
	}
	
	public static Starter newInstance(Config config){
		Starter res = new Starter(config);
		return res;
	}
	
	public static class OnReady{
		
	}
	
}
