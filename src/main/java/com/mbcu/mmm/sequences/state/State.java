package com.mbcu.mmm.sequences.state;

import java.math.BigDecimal;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import com.mbcu.mmm.main.WebSocketClient;
import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.models.internal.LedgerEvent;
import com.mbcu.mmm.models.internal.RLOrder;
import com.mbcu.mmm.models.request.AccountInfo;
import com.mbcu.mmm.models.request.Submit;
import com.mbcu.mmm.rx.RxBus;
import com.mbcu.mmm.rx.RxBusProvider;
import com.mbcu.mmm.sequences.Base;
import com.mbcu.mmm.sequences.Common;
import com.mbcu.mmm.sequences.Txc;
import com.mbcu.mmm.sequences.Common.OnAccountInfoSequence;
import com.mbcu.mmm.sequences.Common.OnLedgerClosed;
import com.mbcu.mmm.sequences.Common.OnOfferCreate;
import com.mbcu.mmm.sequences.Txc.RequestRemove;
import com.mbcu.mmm.utils.MyLogger;
import com.ripple.core.coretypes.uint.UInt32;
import com.ripple.core.types.known.tx.signed.SignedTransaction;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class State extends Base {
	private static final BigDecimal DEFAULT_FEES_DROPS = new BigDecimal("0.000012");
	private static final int DEFAULT_MAX_LEDGER_GAP = 5;
	
	private final AtomicInteger ledgerClosed = new AtomicInteger(0);
	private final AtomicInteger ledgerValidated = new AtomicInteger(0);
	private final AtomicInteger sequence = new AtomicInteger(0);
	private final ConcurrentHashMap<Integer, Txc> pending = new ConcurrentHashMap<>();
	private final ConcurrentLinkedQueue<RLOrder> qWait = new ConcurrentLinkedQueue<>();
	private final AtomicBoolean flagWaitSeq = new AtomicBoolean(false);
	private final AtomicBoolean flagWaitLedger = new AtomicBoolean(false);
	private RxBus bus = RxBusProvider.getInstance();
	
  private Subject<Boolean> seqSyncObs = PublishSubject.create();  

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
					System.out.println("New Sequence " + event.sequence);
					setSequence(event.sequence);
					flagWaitSeq.set(false);
					if (!flagWaitLedger.get() && !flagWaitSeq.get()){
						drain();
					}									
				}
				else if (o instanceof Txc.RequestRemove){
					RequestRemove event = (RequestRemove) o;
					pending.remove(event.seq);
				}
				else if (o instanceof Txc.RequestSequenceSync){
					seqSyncObs.onNext(flagWaitSeq.compareAndSet(false, true));

//					if (!flagWaitLedger.get()){
//						flagWaitSeq.set(true);
//						bus.send(new WebSocketClient.WSRequestSendText(AccountInfo.of(config).stringify()));		
//					}						
				}
				else if (o instanceof Txc.RequestWaitNextLedger){
					flagWaitLedger.set(true);					
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
		
		seqSyncObs.subscribe(flag -> {
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
	
	private void drain(){
		while(!qWait.isEmpty()){
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

		public OnOrderReady(RLOrder outbound) {
			super();
			this.outbound = outbound;
		}	
	}

	public static class RequestSynchronizeSequence{}
}
