package com.mbcu.mmm.sequences.state;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.mbcu.mmm.main.WebSocketClient;
import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.models.internal.LedgerEvent;
import com.mbcu.mmm.models.internal.RLOrder;
import com.mbcu.mmm.models.request.AccountInfo;
import com.mbcu.mmm.models.request.Submit;
import com.mbcu.mmm.rx.RxBus;
import com.mbcu.mmm.rx.RxBusProvider;
import com.mbcu.mmm.sequences.Balancer;
import com.mbcu.mmm.sequences.Base;
import com.mbcu.mmm.sequences.Common;
import com.mbcu.mmm.sequences.Common.OnAccountInfoSequence;
import com.mbcu.mmm.sequences.Common.OnLedgerClosed;
import com.mbcu.mmm.sequences.Common.OnOfferCreate;
import com.mbcu.mmm.sequences.Txc;
import com.mbcu.mmm.sequences.Balancer.OnRequestNonOrderbookRLOrder;
import com.mbcu.mmm.utils.MyLogger;
import com.ripple.core.coretypes.hash.Hash256;
import com.ripple.core.coretypes.uint.UInt32;
import com.ripple.core.types.known.tx.signed.SignedTransaction;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class State extends Base {
	private static final BigDecimal DEFAULT_FEES_DROPS = new BigDecimal("0.000015");
	private static final int DEFAULT_MAX_LEDGER_GAP = 5;
	
	private final AtomicInteger ledgerClosed = new AtomicInteger(0);
	private final AtomicInteger ledgerValidated = new AtomicInteger(0);
	private final AtomicInteger sequence = new AtomicInteger(0);
	private final ConcurrentHashMap<Integer, Txc> pending = new ConcurrentHashMap<>();
	private final ConcurrentLinkedQueue<RLOrder> qWait = new ConcurrentLinkedQueue<>();
	private final AtomicBoolean flagWaitSeq = new AtomicBoolean(false);
	private final AtomicBoolean flagWaitLedger = new AtomicBoolean(false);
	private RxBus bus = RxBusProvider.getInstance();
	
  private Subject<Boolean> sequenceRefreshObs = PublishSubject.create();  

	public State(Config config) {
		super(MyLogger.getLogger(Common.class.getName()), config);
		
		bus.toObservable()
		.subscribeOn(Schedulers.newThread())
		.subscribe(new Observer<Object>() {

			@Override
			public void onSubscribe(Disposable d) {
			}

			@Override
			public void onNext(Object o) {
				if (o instanceof Common.OnOfferCreate){
					OnOfferCreate event = (OnOfferCreate) o;
					if (config.getCredentials().addressEquals(event.account)){
						setSequence(event.sequence);
						pending.remove(event.sequence.intValue());
					}
				}
				else if (o instanceof OnOrderReady){
					OnOrderReady event = (OnOrderReady) o;
					if (event.from != null){
						log("retry from " + event.from + " " + event.memo);
					}
					if (flagWaitSeq.get() || flagWaitLedger.get()){
						qWait.add(event.outbound);												
					}else {
						process(event.outbound);						
					}	
				}			
				else if (o instanceof Common.OnLedgerClosed){
					OnLedgerClosed event = (OnLedgerClosed) o;		
					log(event.ledgerEvent.toString());
					setLedgerIndex(event.ledgerEvent);	
					flagWaitLedger.set(false);
					if (!flagWaitLedger.get() && !flagWaitSeq.get()){
						drain();
					}										
				}
				else if (o instanceof Common.OnAccountInfoSequence){
					OnAccountInfoSequence event = (OnAccountInfoSequence) o;
					log(String.format("New Sequence %d", event.sequence.intValue()));
					setSequence(event.sequence);
					flagWaitSeq.set(false);
					if (!flagWaitLedger.get() && !flagWaitSeq.get()){
						drain();
					}									
				}
				else if (o instanceof RequestRemove){
					RequestRemove event = (RequestRemove) o;
					pending.remove(event.seq);
				}
				else if (o instanceof RequestSequenceSync){
					sequenceRefreshObs.onNext(flagWaitSeq.compareAndSet(false, true));					
				}
				else if (o instanceof RequestWaitNextLedger){
					flagWaitLedger.set(true);					
				}
				else if (o instanceof Balancer.OnRequestNonOrderbookRLOrder){
					OnRequestNonOrderbookRLOrder event = (OnRequestNonOrderbookRLOrder) o;
					List<RLOrder> outbounds = Stream
							.concat(pending.values().stream().map(txc -> {return txc.getOutbound();}), qWait.stream())
							.filter(rlo -> event.pair.equals(rlo.getPair()) || event.pair.equals(rlo.getReversePair()))
							.collect(Collectors.toList());
					bus.send(new BroadcastTxcRLOrder(outbounds, event.pair));
				}
			}

			@Override
			public void onError(Throwable e) {
				log(e.getMessage(), Level.SEVERE);
			}

			@Override
			public void onComplete() {				
			}
		});		
		
		sequenceRefreshObs.subscribe(flag -> {
			if(flag){
				bus.send(new WebSocketClient.WSRequestSendText(AccountInfo.of(config).stringify()));		
			}	
		});
						
	}
	
	public void setLedgerIndex(LedgerEvent event){
		synchronized (ledgerValidated) {
			this.ledgerClosed.set(event.getClosed());
			this.ledgerValidated.set(event.getValidated());
		}
	}
	
	public static State newInstance(Config config){
		return new State(config);
		
	}
	
	private void setSequence(UInt32 seq){
		sequence.set(seq.intValue());			
	}
	
	private int getIncrementSequence(){
		return this.sequence.getAndIncrement();
	}
	
	private void drain() {
		while (!qWait.isEmpty()) {
			process(qWait.poll());
		}
	}
	
	private void process(RLOrder outbound){
		int seq = getIncrementSequence();
		int maxLedger = ledgerValidated.get() + DEFAULT_MAX_LEDGER_GAP;	
		SignedTransaction signed = sign(outbound, seq, maxLedger, DEFAULT_FEES_DROPS);
		Txc txc = Txc.newInstance(outbound, signed.hash, seq, maxLedger);		
		pending.put(txc.getSeq(), txc);
		log("submitting : " + signed.hash + " " + seq);
		submit(signed.tx_blob);
	}
	

	private SignedTransaction sign(RLOrder order, int seq, int maxLedger, BigDecimal fees){
		SignedTransaction signed = order.sign(config, seq, maxLedger, fees);
		return signed;
	}
	
	private void submit(String txBlob){
		bus.send(new WebSocketClient.WSRequestSendText(Submit.build(txBlob).stringify()));
	}
		
	public static class OnOrderValidated{
		public int seq;

		public OnOrderValidated(int seq) {
			super();
			this.seq = seq;
		}			
	}
	
	public static class OnOrderReady {
		public RLOrder outbound;
		public Hash256 from;
		public String memo;

		public OnOrderReady(RLOrder outbound) {
			super();
			this.outbound = outbound;
		}	
		
		public OnOrderReady(RLOrder outbound, Hash256 from, String memo){
			this(outbound);
			this.from = from;
			this.memo = memo;
		}
	}

	public static class RequestRemove {
		public int seq;

		public RequestRemove(int seq) {
			super();
			this.seq = seq;
		}
	}
	
	public static class RequestSequenceSync{}
	
	public static class RequestWaitNextLedger{}
	
	public static class BroadcastTxcRLOrder{
		public final String pair;
		public final List<RLOrder> outbounds;

		public BroadcastTxcRLOrder(List<RLOrder> outbounds, String pair) {
			super();
			this.outbounds = outbounds;
			this.pair = pair;
		}
	}
}
