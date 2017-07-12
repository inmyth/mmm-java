package com.mbcu.mmm.main;

import com.mbcu.mmm.models.internal.RLOrder;
import com.ripple.core.coretypes.hash.Hash256;

public class Events {

	public static class WSConnected {
	}

	public static class WSDisconnected {
	}

	public static class WSError {
		public Exception e;

		public WSError(Exception e) {
			super();
			this.e = e;
		}

	}

	public static class WSGotText {
		public String raw;

		public WSGotText(String raw) {
			super();
			this.raw = raw;
		}

	}

	public static class WSRequestSendText {
		public String request;

		public WSRequestSendText(String request) {
			super();
			this.request = request;
		}

	}
	
	public static class WSRequestDisconnect{}
	

	
	public static class OnResponseNewOfferCreated {
		public Hash256 newHash;
		public RLOrder newOrder;
		
		public OnResponseNewOfferCreated(Hash256 newHash, RLOrder newOrder) {
			super();
			this.newHash = newHash;
			this.newOrder = newOrder;
		}
		
		
	}
	

	

	
	public static class onOfferConsumed{
		public boolean isFullyMatched;
		public RLOrder newOrder;
		
		public onOfferConsumed(boolean isFullyMatched, RLOrder newOrder) {
			super();
			this.isFullyMatched = isFullyMatched;
			this.newOrder = newOrder;
		}
		
		
		
	}
	
	
	
}
