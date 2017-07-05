package com.mbcu.mmm.main;

import java.io.IOException;

import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.sequences.Common;
import com.mbcu.mmm.sequences.Counter;
import com.mbcu.mmm.sequences.Tester;
import com.mbcu.mmm.sequences.state.State;
import com.mbcu.mmm.sequences.state.StateProvider;
import com.mbcu.mmm.utils.MyLogger;
import com.neovisionaries.ws.client.WebSocketException;

public class Main {

	public static void main(String[] args) throws IOException, WebSocketException {
		start(args);
//		Counter.sign();
	}

	private static void start(String[] args) throws IOException, WebSocketException{	
		MyLogger.setup();
		Config config = Config.build(args[0]);
		State state = StateProvider.getInstance(config);
//		Tester tester = Tester.newInstance(state);
//		tester.loop();
		Counter.newInstance(config);
		Common.newInstance(config);
		WebSocketClient webSocketClient = new WebSocketClient(config);
		webSocketClient.start();			
	}
	
	

}
