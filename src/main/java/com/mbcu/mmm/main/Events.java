package com.mbcu.mmm.main;

import java.util.List;

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
	
	public static class OnResponseOfferCancel {
		public Hash256 previousTxnId;
		
		public OnResponseOfferCancel(Hash256 previousTxnId){
			this.previousTxnId = previousTxnId;
		}		
	}
	
	public static class OnResponseNewOfferCreated {
		public Hash256 newHash;
		public RLOrder newOrder;
		
		public OnResponseNewOfferCreated(Hash256 newHash, RLOrder newOrder) {
			super();
			this.newHash = newHash;
			this.newOrder = newOrder;
		}
		
		
	}
	
	public static class onResponseOfferEdited {
		public Hash256 newHash;
		public Hash256 previousTxnId;
		public RLOrder newOrder;
		public onResponseOfferEdited(Hash256 newHash, Hash256 previousTxnId, RLOrder newOrder) {
			super();
			this.newHash = newHash;
			this.previousTxnId = previousTxnId;
			this.newOrder = newOrder;
		}		
		
	}
	
	public static class onResponseOfferExecuted{
		public List<RLOrder> oes;

		public onResponseOfferExecuted(List<RLOrder> oes) {
			super();
			this.oes = oes;
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
