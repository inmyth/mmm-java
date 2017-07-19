package com.mbcu.mmm.sequences;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.mbcu.mmm.sequences.state.State;

public class Tester {
	
	private State state;
	ScheduledExecutorService executor =  Executors.newSingleThreadScheduledExecutor();
	Runnable periodicTask = new Runnable() {
    public void run() {
        // Invoke method(s) to do the work
        doPeriodicWork();
    }
	};
	private Tester(State state){
		this.state = state;
	}
	
	public static Tester newInstance(State state){
		return new Tester(state);
	}
	
	public void loop(){
		executor.scheduleAtFixedRate(periodicTask, 0, 1, TimeUnit.SECONDS);
	}
	
	private void doPeriodicWork(){
//		System.out.println("Current sequence " + state.getSequence());
	}
	
}
