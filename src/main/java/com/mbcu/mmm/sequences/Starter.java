package com.mbcu.mmm.sequences;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import com.mbcu.mmm.main.WebSocketClient;
import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.models.request.AccountInfo;
import com.mbcu.mmm.rx.RxBus;
import com.mbcu.mmm.rx.RxBusProvider;
import com.mbcu.mmm.sequences.Common.OnAccountInfoSequence;
import com.mbcu.mmm.sequences.Common.OnResponseLedgerClosed;
import com.mbcu.mmm.sequences.balancer.Balancer;
import com.mbcu.mmm.sequences.counters.Yuki;
import com.mbcu.mmm.sequences.state.State;
import com.mbcu.mmm.sequences.state.StateProvider;
import com.mbcu.mmm.utils.MyLogger;
import com.neovisionaries.ws.client.WebSocketException;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public class Starter extends Base{
  private final CountDownLatch latch = new CountDownLatch(2);
  private final RxBus bus = RxBusProvider.getInstance();
  private State state;
  
  
	public Starter(Config config) {
		super(MyLogger.getLogger(Starter.class.getName()), config);
		this.state = StateProvider.getInstance(config);

		bus.toObservable()
		.subscribe(new Observer<Object>() {

			@Override
			public void onSubscribe(Disposable d) {
				// TODO Auto-generated method stub			
			}

			@Override
			public void onNext(Object o) {
				if (o instanceof WebSocketClient.WSConnected){
					bus.send(new WebSocketClient.WSRequestSendText(AccountInfo.newInstance(config.getCredentials().getAddress()).stringify()));			
				}else if (o instanceof Common.OnAccountInfoSequence){
					Common.OnAccountInfoSequence event = (OnAccountInfoSequence) o;
					state.setSequence(event.sequence);
					latch.countDown();
				}else if (o instanceof Common.OnResponseLedgerClosed){
					Common.OnResponseLedgerClosed event =  (OnResponseLedgerClosed) o;
					state.setLedgerIndex(event.ledgerEvent);
					latch.countDown();
				}
			}

			@Override
			public void onError(Throwable e) {
				// TODO Auto-generated method stub			
			}

			@Override
			public void onComplete() {
				// TODO Auto-generated method stub				
			}
		});
		
	}
	
	public static Starter newInstance(Config config){
		Starter res = new Starter(config);
		return res;
	}
	
	
	public void start() throws IOException, WebSocketException{
//	Tester tester = Tester.newInstance(state);
//	tester.loop();
		log("Initiating ...");
		Common.newInstance(config);
		WebSocketClient webSocketClient = new WebSocketClient(super.config);
		webSocketClient.start();	
		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
    try {
      latch.await();
	  } catch (InterruptedException e) {
	      e.printStackTrace();
	  }
    postInit();
	}
	
	private void postInit(){
		log(String.format("Initiation complete, current ledger closed: %d, validated: %d, sequence : %d", 
				state.getLedgerClosed(), state.getLedgerValidated(), state.getSequence()));	
		Yuki.newInstance(config);
		Submitter.newInstance(config);
		Balancer.newInstance(config);
		bus.send(new OnInitiated());	
	}
	
	
	public static class OnInitiated{}

	
}
