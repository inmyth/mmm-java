package com.mbcu.mmm.sequences;

import java.util.logging.Logger;

import com.mbcu.mmm.main.Events;
import com.mbcu.mmm.main.Events.WSError;
import com.mbcu.mmm.main.Events.WSGotText;
import com.mbcu.mmm.rx.RxBus;
import com.mbcu.mmm.rx.RxBusProvider;
import com.mbcu.mmm.utils.MyLogger;

import io.reactivex.schedulers.Schedulers;

public class Common extends Base {
	private final static Logger LOGGER = MyLogger.getLogger(Common.class.getName());
	private RxBus bus = RxBusProvider.getInstance();

	public Common() {
		bus.asFlowable()
//		.subscribeOn(Schedulers.newThread())
		.subscribe(o -> {
			if (o instanceof Events.WSConnected) {
				LOGGER.fine("connected");
			} else if (o instanceof Events.WSDisconnected) {
				LOGGER.fine("disconnected");
			} else if (o instanceof Events.WSError) {
				Events.WSError event = (WSError) o;
				LOGGER.severe(event.e.getMessage());
			} else if (o instanceof Events.WSGotText){
				Events.WSGotText event = (WSGotText) o;
				LOGGER.finer(event.raw);			
			}
		});

	}

	public static Common newInstance() {
		return new Common();
	}
	
	private static void responseSelector(){
		
		
	}

}
