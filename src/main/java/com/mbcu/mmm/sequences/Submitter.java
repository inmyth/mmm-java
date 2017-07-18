package com.mbcu.mmm.sequences;

import java.math.BigDecimal;

import com.mbcu.mmm.main.WebSocketClient;
import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.models.internal.RLOrder;
import com.mbcu.mmm.models.internal.cache.Txc;
import com.mbcu.mmm.models.request.Submit;
import com.mbcu.mmm.sequences.balancer.Balancer;
import com.mbcu.mmm.sequences.balancer.Balancer.SeedReady;
import com.mbcu.mmm.sequences.counters.Counter;
import com.mbcu.mmm.sequences.counters.Counter.CounterReady;
import com.mbcu.mmm.sequences.state.State;
import com.mbcu.mmm.sequences.state.StateProvider;
import com.mbcu.mmm.utils.MyLogger;
import com.ripple.core.types.known.tx.signed.SignedTransaction;

import io.reactivex.schedulers.Schedulers;

public class Submitter extends Base {
	private static final BigDecimal DEFAULT_FEES_DROPS = new BigDecimal("0.000012");
	private static final int DEFAULT_MAX_LEDGER_GAP = 2;
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
				signAndCache(event.counter);
			}else if (o instanceof Balancer.SeedReady){
				SeedReady event = (SeedReady) o;
				log("SEEDING \n + " + event.seed.stringify());
				signAndCache(event.seed);				
			}else if (o instanceof SubmitTxBlob){
				SubmitTxBlob event = (SubmitTxBlob) o;
				submit(event.txBlob);
			}else  if (o instanceof Retry){
				Retry event = (Retry) o;
				log("RETRYING \n + " + event.order.stringify());
				signAndCache(event.order);
			}
		});
	}
	
	private void signAndCache(RLOrder order){
		int seq = state.getIncrementSequence();
		int maxLedger = state.getLedgerValidated() + DEFAULT_MAX_LEDGER_GAP;		
		SignedTransaction signed = order.sign(config, seq, maxLedger, DEFAULT_FEES_DROPS);
		bus.send(new OnSubmitCache(Txc.newInstance(order, signed.hash, maxLedger, seq), signed, seq));
	}
	
	private void submit(String txBlob){
		bus.send(new WebSocketClient.WSRequestSendText(Submit.build(txBlob).stringify()));
	}

	public static Submitter newInstance(Config config) {
		Submitter res = new Submitter(config);
		return res;
	}
	
//	public static class OnSubmitCache{
//		public final Txc cache;
//		public final SignedTransaction signed;
//		public final int sequence;
//		
//		public OnSubmitCache(Txc cache, SignedTransaction signed, int sequence) {
//			super();
//			this.cache = cache;
//			this.signed = signed;
//			this.sequence = sequence;
//		}
//	}
//	

	public static class SubmitTxBlob{
		public final String txBlob;

		public SubmitTxBlob(String txBlob) {
			super();
			this.txBlob = txBlob;
		}
	}

	public static class Retry {
		public RLOrder order;

		public Retry(RLOrder order) {
			super();
			this.order = order;
		}
		
		
	}
	
}
