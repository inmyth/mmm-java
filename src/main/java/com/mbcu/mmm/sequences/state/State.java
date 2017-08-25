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
import com.mbcu.mmm.models.internal.Cpair;
import com.mbcu.mmm.models.internal.LedgerEvent;
import com.mbcu.mmm.models.internal.RLOrder;
import com.mbcu.mmm.models.request.AccountInfo;
import com.mbcu.mmm.models.request.Submit;
import com.mbcu.mmm.rx.BusBase;
import com.mbcu.mmm.rx.RxBus;
import com.mbcu.mmm.rx.RxBusProvider;
import com.mbcu.mmm.sequences.Balancer;
import com.mbcu.mmm.sequences.Balancer.OnRequestNonOrderbookRLOrder;
import com.mbcu.mmm.sequences.Base;
import com.mbcu.mmm.sequences.Common;
import com.mbcu.mmm.sequences.Common.OnAccountInfoSequence;
import com.mbcu.mmm.sequences.Common.OnLedgerClosed;
import com.mbcu.mmm.sequences.Common.OnOfferCanceled;
import com.mbcu.mmm.sequences.Common.OnOfferCreate;
import com.mbcu.mmm.sequences.Common.OnOfferEdited;
import com.mbcu.mmm.sequences.Txc;
import com.mbcu.mmm.sequences.Txd;
import com.mbcu.mmm.sequences.Txd.TxdMini;
import com.mbcu.mmm.utils.MyLogger;
import com.ripple.core.coretypes.AccountID;
import com.ripple.core.coretypes.Amount;
import com.ripple.core.coretypes.hash.Hash256;
import com.ripple.core.coretypes.uint.UInt32;
import com.ripple.core.types.known.tx.signed.SignedTransaction;
import com.ripple.core.types.known.tx.txns.OfferCancel;

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
	private final ConcurrentLinkedQueue<RLOrder> qPens = new ConcurrentLinkedQueue<>();
	private final ConcurrentHashMap<Integer, Txd> cancels = new ConcurrentHashMap<>();
	private final ConcurrentLinkedQueue<TxdMini> qCans = new ConcurrentLinkedQueue<>();
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
				BusBase base = (BusBase) o;
				try {
					if (base instanceof Common.OnOfferCreate){
						OnOfferCreate event = (OnOfferCreate) o;
						setSequence(event.sequence);
						pending.remove(event.sequence.intValue());					
					}
					else if (base instanceof Common.OnOfferEdited){
						OnOfferEdited event = (OnOfferEdited) o;
						setSequence(event.newSeq);
					}
					else if (base instanceof Common.OnOfferCanceled){
						OnOfferCanceled event = (OnOfferCanceled) o;
						cancels.remove(event.prevSeq.intValue());
						setSequence(event.newSeq);
					}			
					else if (base instanceof OnOrderReady){
						OnOrderReady event = (OnOrderReady) o;
						if (event.from != null){
							log("retry from " + event.from + " " + event.memo);
						}
						if (flagWaitSeq.get() || flagWaitLedger.get()){
							qPens.add(event.outbound);												
						} else {
							ocreate(event.outbound);						
						}	
					}			
					else if (base instanceof OnCancelReady){
						OnCancelReady event = (OnCancelReady) o;
						if (flagWaitSeq.get() || flagWaitLedger.get()){
							qCans.add(new TxdMini(Cpair.newInstance(event.pair), event.seq));												
						} else {
							ocancel(Cpair.newInstance(event.pair), event.seq);					
						}	
					}				
					else if (base instanceof Common.OnLedgerClosed){
						OnLedgerClosed event = (OnLedgerClosed) o;		
						log(event.ledgerEvent.toString() +  " seq : " + sequence);
						setLedgerIndex(event.ledgerEvent);	
						flagWaitLedger.set(false);
						if (!flagWaitLedger.get() && !flagWaitSeq.get()){
							drain();
						}										
					}
					else if (base instanceof Common.OnAccountInfoSequence){
						OnAccountInfoSequence event = (OnAccountInfoSequence) o;
						log(String.format("New Sequence %d", event.sequence.intValue()));
						setSequence(event.sequence);
						flagWaitSeq.set(false);
						if (!flagWaitLedger.get() && !flagWaitSeq.get()){
							drain();
						}									
					}
					else if (base instanceof RequestRemoveCreate){
						RequestRemoveCreate event = (RequestRemoveCreate) o;
						pending.remove(event.seq);
					}
					else if (base instanceof RequestRemoveCancel){
						RequestRemoveCancel event = (RequestRemoveCancel) o;
						System.out.println("Removing from cancels " + event.seq);
						cancels.remove(event.seq);
						
					}
					else if (base instanceof RequestSequenceSync){
						sequenceRefreshObs.onNext(flagWaitSeq.compareAndSet(false, true));					
					}
					else if (base instanceof RequestWaitNextLedger){
						flagWaitLedger.set(true);					
					}
					else if (base instanceof Balancer.OnRequestNonOrderbookRLOrder){
						OnRequestNonOrderbookRLOrder event = (OnRequestNonOrderbookRLOrder) o;
						List<RLOrder> crts = Stream
							.concat(pending.values().stream().map(txc -> {return txc.getOutbound();}), qPens.stream())
							.filter(rlo -> rlo.getCpair().isMatch(event.pair) != null)
							.collect(Collectors.toList());
	
						List<Integer> cans = Stream.concat(
								cancels.values().stream()
								.filter(txd -> txd.getPair().isMatch(event.pair) != null)
								.map(txd -> {return txd.getCanSeq();})
								, 
								qCans.stream()
								.filter(mini -> mini.cpair.isMatch(event.pair) != null)
								.map(mini -> {return mini.canSeq;})							
								)
								.collect(Collectors.toList());
	
						bus.send(new BroadcastPendings(event.pair, crts, cans));
					}
				} catch (Exception e) {
					MyLogger.exception(LOGGER, base.toString(), e);		
					throw e;				
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
		while (!qPens.isEmpty()) {
			ocreate(qPens.poll());
		}
		while (!qCans.isEmpty()) {
			TxdMini load = qCans.poll();
			ocancel(load.cpair, load.canSeq);
		}
	}
	
	private void ocreate(RLOrder outbound){
		int seq = getIncrementSequence();
		int maxLedger = ledgerValidated.get() + DEFAULT_MAX_LEDGER_GAP;	
		SignedTransaction signed = signCreate(outbound, seq, maxLedger, DEFAULT_FEES_DROPS);
		Txc txc = Txc.newInstance(outbound, signed.hash, seq, maxLedger);		
		pending.put(txc.getSeq(), txc);
		log("submitting offerCreate: " + signed.hash + " " + seq);
		submit(signed.tx_blob);
	}
 
	private void ocancel(Cpair cpair, int canSeq){
		int newSeq = getIncrementSequence();
		int maxLedger = ledgerValidated.get() + DEFAULT_MAX_LEDGER_GAP;	
		SignedTransaction signed = signCancel(newSeq, canSeq, maxLedger);
		Txd txd = Txd.newInstance(cpair, canSeq, newSeq, maxLedger);
		cancels.put(canSeq, txd);
		log("submitting offerCancel: " + signed.hash + " " + canSeq + " " + newSeq);
		submit(signed.tx_blob);
	}

	private SignedTransaction signCreate(RLOrder order, int seq, int maxLedger, BigDecimal fees){
		SignedTransaction signed = order.signOfferCreate(config, seq, maxLedger, fees);
		return signed;
	}
	
	private SignedTransaction signCancel(int seq, int canSeq, int maxLedger){
		OfferCancel offerCancel = new OfferCancel();
		offerCancel.put(UInt32.OfferSequence, new UInt32(String.valueOf(canSeq)));
		offerCancel.fee(new Amount(DEFAULT_FEES_DROPS));
		offerCancel.sequence(new UInt32(String.valueOf(seq)));
		offerCancel.lastLedgerSequence(new UInt32(String.valueOf(maxLedger)));
		offerCancel.account(AccountID.fromAddress(config.getCredentials().getAddress()));
		SignedTransaction signed = offerCancel.sign(config.getCredentials().getSecret());
		return signed;	

	}
	
	private void submit(String txBlob){
		bus.send(new WebSocketClient.WSRequestSendText(Submit.build(txBlob).stringify()));
	}
		
	public static class OnOrderValidated extends BusBase {
		public int seq;

		public OnOrderValidated(int seq) {
			super();
			this.seq = seq;
		}			
	}
	
	public static class OnOrderReady extends BusBase {
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
	
	public static class OnCancelReady extends BusBase {
		public final int seq;
		public final String pair;

		public OnCancelReady(String pair, int seq) {
			super();
			this.pair = pair;
			this.seq = seq;
		}
		
	}

	public static class RequestRemoveCreate extends BusBase {
		public int seq;

		public RequestRemoveCreate(int seq) {
			super();
			this.seq = seq;
		}
	}
	
	public static class RequestRemoveCancel extends BusBase {
		public int seq;

		public RequestRemoveCancel(int seq) {
			super();
			this.seq = seq;
		}
		
	}
	
	public static class RequestSequenceSync extends BusBase {}
	
	public static class RequestWaitNextLedger extends BusBase {}
	
	public static class BroadcastPendings extends BusBase {
		public final String pair;
		public final List<RLOrder> creates;
		public final List<Integer> cancels;

		public BroadcastPendings(String pair, List<RLOrder> creates, List<Integer> cancels) {
			this.pair = pair;
			this.creates = creates;
			this.cancels = cancels;
		}
	}
	
	public static class RequestOrderCancel extends BusBase {
		public final int seq;

		public RequestOrderCancel(int seq) {
			super();
			this.seq = seq;
		}	
	}
}
