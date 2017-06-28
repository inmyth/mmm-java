package com.mbcu.mmm.main;

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
}
