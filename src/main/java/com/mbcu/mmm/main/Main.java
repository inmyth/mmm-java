package com.mbcu.mmm.main;

import java.io.IOException;

import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.sequences.Starter;
import com.mbcu.mmm.utils.MyLogger;
import com.neovisionaries.ws.client.WebSocketException;

public class Main {

	public static void main(String[] args) throws IOException, WebSocketException, InterruptedException {
		Config config = Config.build(args[0]);
		MyLogger.setup(config);
		Starter manager = Starter.newInstance(config);
		manager.start();
	}

}
