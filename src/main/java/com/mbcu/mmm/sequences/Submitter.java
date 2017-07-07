package com.mbcu.mmm.sequences;

import com.mbcu.mmm.main.Events;
import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.models.internal.RLOrder;
import com.mbcu.mmm.models.request.Submit;
import com.mbcu.mmm.sequences.balancer.Balancer;
import com.mbcu.mmm.sequences.balancer.Balancer.SeedReady;
import com.mbcu.mmm.sequences.counters.Counter;
import com.mbcu.mmm.sequences.counters.Counter.CounterReady;
import com.mbcu.mmm.sequences.state.State;
import com.mbcu.mmm.sequences.state.StateProvider;
import com.mbcu.mmm.utils.MyLogger;

import io.reactivex.schedulers.Schedulers;

public class Submitter extends Base {
	private static final int DEFAULT_FEES_DROPS = 12;
	private State state;

	private Submitter(Config config) {
		super(MyLogger.getLogger(Submitter.class.getName()), config);
		this.config = config;
		this.state = StateProvider.getInstance(config);
		bus.toObservable()
			.subscribeOn(Schedulers.newThread())
			.subscribe(o -> {
			if (o instanceof Counter.CounterReady) {
				CounterReady event = (CounterReady) o;
				log("COUNTERING \n + " + event.counter.stringify());
				submit(event.counter);
			}else if (o instanceof Balancer.SeedReady){
				SeedReady event = (SeedReady) o;
				log("SEEDING \n + " + event.seed.stringify());
				submit(event.seed);				
			}
		});
	}
	
	private void submit(RLOrder order){
		int seq = state.getApplicableSequence();
		System.out.println("Seq " + seq);
		
		String txBlob = order.sign(config, seq, DEFAULT_FEES_DROPS).tx_blob;
		bus.send(new Events.WSRequestSendText(Submit.build(txBlob).stringify()));
	}

	public static Submitter newInstance(Config config) {
		Submitter res = new Submitter(config);
		return res;
	}

}
