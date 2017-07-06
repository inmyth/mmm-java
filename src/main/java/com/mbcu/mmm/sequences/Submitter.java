package com.mbcu.mmm.sequences;

import com.mbcu.mmm.main.Events;
import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.models.request.Submit;
import com.mbcu.mmm.sequences.counters.Counter;
import com.mbcu.mmm.sequences.counters.Counter.CounterReady;
import com.mbcu.mmm.sequences.state.State;
import com.mbcu.mmm.sequences.state.StateProvider;
import com.mbcu.mmm.utils.MyLogger;

public class Submitter extends Base {
	private static final int DEFAULT_FEES_DROPS = 12;
	private State state;

	private Submitter(Config config) {
		super(MyLogger.getLogger(Submitter.class.getName()), config);
		this.config = config;
		this.state = StateProvider.getInstance(config);
		bus.toObservable().subscribe(o -> {
			if (o instanceof Counter.CounterReady) {
				CounterReady event = (CounterReady) o;
				String txBlob = event.counter.sign(config, state.getSequence(), DEFAULT_FEES_DROPS).tx_blob;
				log("COUNTERING \n + " + event.counter.stringify());
				bus.send(new Events.WSRequestSendText(Submit.build(txBlob).stringify()));
			}
		});

	}

	public static Submitter newInstance(Config config) {
		Submitter res = new Submitter(config);
		return res;
	}

}
