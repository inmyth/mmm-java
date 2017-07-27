package com.mbcu.mmm.sequences;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import com.mbcu.mmm.main.WebSocketClient;
import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.models.request.AccountInfo;
import com.mbcu.mmm.models.request.Request.Command;
import com.mbcu.mmm.models.request.Subscribe;
import com.mbcu.mmm.rx.RxBus;
import com.mbcu.mmm.rx.RxBusProvider;
import com.mbcu.mmm.sequences.counters.Yuki;
import com.mbcu.mmm.sequences.state.StateProvider;
import com.mbcu.mmm.utils.MyLogger;
import com.neovisionaries.ws.client.WebSocketException;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class Starter extends Base{
	private final int numBases = 2; // AccountInfo and first ledger closed
  private  CountDownLatch latch;
  private final RxBus bus = RxBusProvider.getInstance();
  private int orderbookReturns;
  
	private Starter(Config config) {
		super(MyLogger.getLogger(Starter.class.getName()), config);
		
		int numOrderbooks = config.getBotConfigMap().size(); 
		latch = new CountDownLatch(numOrderbooks + numBases);
		
		CompositeDisposable disAccInfo = new CompositeDisposable();
		disAccInfo
			.add(bus.toObservable().subscribeOn(Schedulers.newThread())
			.subscribeWith(new DisposableObserver<Object>() {

				@Override
				public void onNext(Object o) {
					if (o instanceof Common.OnAccountInfoSequence){
						latch.countDown();
						disAccInfo.dispose();
					}					
				}

				@Override
				public void onError(Throwable e) {}

				@Override
				public void onComplete() {}
				
			}));
		
		CompositeDisposable disLedgerClosed = new CompositeDisposable();
		disLedgerClosed
		.add(bus.toObservable().subscribeOn(Schedulers.newThread())
		.subscribeWith(new DisposableObserver<Object>() {

			@Override
			public void onNext(Object o) {
				if (o instanceof Common.OnLedgerClosed){
					latch.countDown();
					disLedgerClosed.dispose();
				}					
			}

			@Override
			public void onError(Throwable e) {}

			@Override
			public void onComplete() {}
			
		}));
		
		CompositeDisposable disOnWSConnected = new CompositeDisposable();
		disOnWSConnected
		.add(bus.toObservable().subscribeOn(Schedulers.newThread())
		.subscribeWith(new DisposableObserver<Object>() {

			@Override
			public void onNext(Object o) {
				if (o instanceof WebSocketClient.WSConnected){
					bus.send(new WebSocketClient.WSRequestSendText(AccountInfo.of(config).stringify()));						
					config.getBotConfigMap().values().stream().forEach(botConfig -> {
						String ob = Subscribe.build(Command.SUBSCRIBE).withOrderbook(botConfig).stringify();
						bus.send(new WebSocketClient.WSRequestSendText(ob));						
					});
					disOnWSConnected.dispose();
				}				
			}

			@Override
			public void onError(Throwable e) {}

			@Override
			public void onComplete() {}
			
		}));
		
		CompositeDisposable disOrderbooks = new CompositeDisposable();
		disOrderbooks
		.add(bus.toObservable().subscribeOn(Schedulers.newThread())
		.subscribeWith(new DisposableObserver<Object>() {

			@Override
			public void onNext(Object o) {
				if (o instanceof Common.OnOrderbook){
					orderbookReturns++;
					if (orderbookReturns == config.getBotConfigMap().size()){
						disOrderbooks.dispose();
					}
					latch.countDown();
				}		
			}

			@Override
			public void onError(Throwable e) {}

			@Override
			public void onComplete() {}
			
		}));
			
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
