package com.mbcu.mmm.main;

import java.io.IOException;
import java.math.BigDecimal;

import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.sequences.Starter;
import com.mbcu.mmm.utils.MyLogger;
import com.neovisionaries.ws.client.WebSocketException;
import com.ripple.core.coretypes.AccountID;
import com.ripple.core.coretypes.Amount;
import com.ripple.core.coretypes.uint.UInt32;
import com.ripple.core.fields.Field;
import com.ripple.core.types.known.tx.signed.SignedTransaction;
import com.ripple.core.types.known.tx.txns.OfferCancel;
import com.ripple.core.types.known.tx.txns.OfferCreate;

public class Main {

	public static void main(String[] args) throws IOException, WebSocketException, InterruptedException{
		start(args);
//		Counter.sign();
	}

	private static void start(String[] args) throws IOException, WebSocketException, InterruptedException{	
		MyLogger.setup();
		Config config = Config.build(args[0]);
		Starter manager = Starter.newInstance(config);
		manager.start();	
	}
	
	

	
	

}
