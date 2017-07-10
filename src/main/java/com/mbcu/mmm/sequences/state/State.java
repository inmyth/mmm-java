package com.mbcu.mmm.sequences.state;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.apache.tomcat.util.file.ConfigFileLoader;

import com.mbcu.mmm.main.Events;
import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.rx.RxBus;
import com.mbcu.mmm.rx.RxBusProvider;
import com.mbcu.mmm.sequences.Base;
import com.mbcu.mmm.sequences.Common;
import com.mbcu.mmm.sequences.Common.OnOfferCreate;
import com.mbcu.mmm.sequences.Common.OnOfferCreated;
import com.mbcu.mmm.sequences.Common.OnResponseFail;
import com.mbcu.mmm.sequences.Common.OnResponseSuccess;
import com.mbcu.mmm.sequences.Submitter;
import com.mbcu.mmm.sequences.Submitter.OnSubmitCache;
import com.mbcu.mmm.sequences.Submitter.SubmitCache;
import com.mbcu.mmm.utils.MyLogger;
import com.ripple.core.coretypes.uint.UInt32;
import com.ripple.core.serialized.enums.EngineResult;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class State extends Base {
	
	private final AtomicInteger sequence = new AtomicInteger(0);
	private final ConcurrentHashMap<Integer, SubmitCache> pending = new ConcurrentHashMap<>();
	private RxBus bus = RxBusProvider.getInstance();
	
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
//					OnOfferCreate event = (OnOfferCreate) o;
//					if (config.getCredentials().addressEquals(event.account)){
//						setSequence(event.sequence);
//						pending.remove(event.sequence.intValue());
//						System.out.println("ON RESPONSE SUCCESS REMOVING " + event.sequence.intValue());					
//					}
				}else if (o instanceof Submitter.OnSubmitCache){				
					OnSubmitCache event = (Submitter.OnSubmitCache) o;
					pending.putIfAbsent(event.sequence, event.cache);
					bus.send(new Submitter.SubmitTxBlob(event.signed.tx_blob));
				}else if (o instanceof Common.OnResponseFail){
					OnResponseFail event = (OnResponseFail) o;
					if (event.engineResult.equals(EngineResult.terPRE_SEQ.toString()) || event.engineResult.equals(EngineResult.tefPAST_SEQ.toString())){
						return;
					}
					
					if (event.engineResult.equals(EngineResult.terINSUF_FEE_B)){
						bus.send(new Events.WSRequestDisconnect());
						return;
					}
					
					int usableSequence = event.sequence.intValue();
					revertSequence(usableSequence);
					SubmitCache retry = pending.get(usableSequence);
					if (retry == null){
						System.out.println("PENDING INRESPONSE FAIL" + pending.size());
						pending.keySet().stream().forEach(a -> {System.out.println("KEY =  " + a);});
						System.out.println("CANNOT FIND KEY " + event.sequence.intValue());
						return;
					}
					pending.remove(event.sequence.intValue());
					bus.send(new Submitter.Retry(retry.outbound));
				}
			}

			@Override
			public void onError(Throwable e) {
				log(e.getMessage(), Level.SEVERE);
			}

			@Override
			public void onComplete() {
				// TODO Auto-generated method stub
				
			}
		});
		
	}
	
	public static State newInstance(Config config){
		return new State(config);
		
	}
	
	public void setSequence(UInt32 seq){
		synchronized (sequence) {
			if (this.sequence.get() < seq.intValue()){
				sequence.set(seq.intValue());
			}
		}
	}
	
	public int getSequence(){
		return this.sequence.intValue();
	}
	
	public int getIncrementSequence(){
		return this.sequence.getAndIncrement();
	}
	
	private void revertSequence(int usableSequence){
		if (usableSequence < this.sequence.get()){
			this.sequence.set(usableSequence);
		}
	}
	
	

}
