package com.mbcu.mmm.sequences;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import com.mbcu.mmm.main.WebSocketClient;
import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.models.request.AccountInfo;
import com.mbcu.mmm.rx.RxBus;
import com.mbcu.mmm.rx.RxBusProvider;
import com.mbcu.mmm.sequences.counters.Yuki;
import com.mbcu.mmm.sequences.state.StateProvider;
import com.mbcu.mmm.utils.MyLogger;
import com.neovisionaries.ws.client.WebSocketException;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public class Starter extends Base{
  private final CountDownLatch latch = new CountDownLatch(2);
  private final RxBus bus = RxBusProvider.getInstance();
   
	private Starter(Config config) {
		super(MyLogger.getLogger(Starter.class.getName()), config);

		bus.toObservable()
		.subscribe(new Observer<Object>() {

			@Override
			public void onSubscribe(Disposable d) {
				// TODO Auto-generated method stub			
			}

			@Override
			public void onNext(Object o) {
				if (o instanceof WebSocketClient.WSConnected){
					bus.send(new WebSocketClient.WSRequestSendText(AccountInfo.of(config).stringify()));						
				}
				else if (o instanceof Common.OnAccountInfoSequence){
					latch.countDown();
				}
				else if (o instanceof Common.OnLedgerClosed){
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
		StateProvider.getInstance(config);
		return res;
	}
	
	
	public void start() throws IOException, WebSocketException, InterruptedException{
		log("Initiating ...");
		Common.newInstance(config);
		WebSocketClient webSocketClient = new WebSocketClient(super.config);
		webSocketClient.start();	
		latch.await();
    postInit();
	}
	
	private void postInit(){
		log("Initiation complete");	
		Yuki.newInstance(config);
		Balancer.newInstance(config);
		bus.send(new OnInitiated());	
	}
	
	
	public static class OnInitiated{}

	
}
