package com.mbcu.mmm.sequences.balancer;

import java.util.Collection;
import java.util.stream.Stream;

import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.models.internal.RLOrder;
import com.mbcu.mmm.sequences.Base;
import com.mbcu.mmm.sequences.Manager;
import com.mbcu.mmm.utils.MyLogger;

public class Balancer extends Base{
		
	public Balancer(Config config) {
		super(MyLogger.getLogger(Manager.class.getName()), config);
		bus.toObservable()
		.subscribe(o -> {
			if (o instanceof Manager.OnInitiated){
				seed();
			}
		});
	}
	
	private void seed(){
		config.getBotConfigMap().values()
		.stream()
		.map(bot -> { 
			return bot.buildSeed();
		})
		.flatMap(l->l.stream())
		.forEach(seed -> {
			System.out.println(seed.stringify());
				bus.send(new SeedReady(seed));		
		});
	}
	
	public static Balancer newInstance(Config config){
		return new Balancer(config);
	}
	
	public static class SeedReady {
		public final RLOrder seed;

		public SeedReady(RLOrder seed) {
			super();
			this.seed = seed;
		}		
	}
	
}
