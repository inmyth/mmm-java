package com.mbcu.mmm.main;

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
	
	public static class OnResponseOrderCancel {
		public Hash256 previousTxnId;
		
		public OnResponseOrderCancel(Hash256 previousTxnId){
			this.previousTxnId = previousTxnId;
		}		
	}
	
	public static class OnResponseNewOrderCreated {
		
	}
	
	public static class onResponseOrderEdited {
		
	}
}
