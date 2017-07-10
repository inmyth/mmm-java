package com.mbcu.mmm.sequences;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import com.mbcu.mmm.main.Events;
import com.mbcu.mmm.main.WebSocketClient;
import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.models.request.AccountInfo;
import com.mbcu.mmm.rx.RxBus;
import com.mbcu.mmm.rx.RxBusProvider;
import com.mbcu.mmm.sequences.Common.OnAccountInfoSequence;
import com.mbcu.mmm.sequences.state.State;
import com.mbcu.mmm.sequences.state.StateProvider;
import com.mbcu.mmm.utils.MyLogger;
import com.neovisionaries.ws.client.WebSocketException;

public class Manager extends Base{
  private final CountDownLatch latch = new CountDownLatch(1);
  private final RxBus bus = RxBusProvider.getInstance();
  private State state;
  
  
	public Manager(Config config) {
		super(MyLogger.getLogger(Manager.class.getName()), config);
		this.state = StateProvider.getInstance(config);

		bus
		.toObservable()
		.subscribe(o -> {
			if (o instanceof Events.WSConnected){
				bus.send(new Events.WSRequestSendText(AccountInfo.newInstance(config.getCredentials().getAddress()).stringify()));			
			}else if (o instanceof Common.OnAccountInfoSequence){
				Common.OnAccountInfoSequence event = (OnAccountInfoSequence) o;
				state.setSequence(event.sequence);
				latch.countDown();
			}
		});
		
	}
	
	public static Manager newInstance(Config config){
		Manager res = new Manager(config);
		return res;
	}
	
	
	public void start() throws IOException, WebSocketException{
		log("Initiating ...");
		Common.newInstance(config);
		WebSocketClient webSocketClient = new WebSocketClient(super.config);
		webSocketClient.start();	
		try {
			latch.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    postInit();
	}
	
	private void postInit(){
		log("Initiation complete, current sequence is : " + state.getSequence());	
//		Yuki.newInstance(config);
		Submitter.newInstance(config);
//		Balancer.newInstance(config);
		bus.send(new OnInitiated());	
	}
	
	
	public static class OnInitiated{}

	
}
