package com.mbcu.mmm.main;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.rx.BusBase;
import com.mbcu.mmm.rx.RxBus;
import com.mbcu.mmm.rx.RxBusProvider;
import com.mbcu.mmm.sequences.Emailer;
import com.mbcu.mmm.utils.MyLogger;
import com.neovisionaries.ws.client.ThreadType;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;
import com.neovisionaries.ws.client.WebSocketListener;
import com.neovisionaries.ws.client.WebSocketState;

public class WebSocketClient {
	private final static Logger LOGGER = Logger.getLogger(WebSocketClient.class.getName());
	private WebSocket ws;
	private RxBus bus = RxBusProvider.getInstance();

	public WebSocketClient(Config config) throws IOException {

		this.ws = new WebSocketFactory().setConnectionTimeout(5000).createSocket(config.getNet())
				.addListener(new WebSocketListener() {

					@Override
					public void onUnexpectedError(WebSocket arg0, WebSocketException e) throws Exception {
						bus.send(new WSError(e));
					}

					@Override
					public void onThreadStopping(WebSocket arg0, ThreadType arg1, Thread arg2) throws Exception {
						// TODO Auto-generated method stub

					}

					@Override
					public void onThreadStarted(WebSocket arg0, ThreadType arg1, Thread arg2) throws Exception {
						// TODO Auto-generated method stub

					}

					@Override
					public void onThreadCreated(WebSocket arg0, ThreadType arg1, Thread arg2) throws Exception {
						// TODO Auto-generated method stub

					}

					@Override
					public void onTextMessageError(WebSocket arg0, WebSocketException e, byte[] arg2) throws Exception {
						bus.send(new WSError(e));
					}

					@Override
					public void onTextMessage(WebSocket arg0, String raw) throws Exception {
						bus.send(new WSGotText(raw));
					}

					@Override
					public void onTextFrame(WebSocket arg0, WebSocketFrame arg1) throws Exception {
						// TODO Auto-generated method stub

					}

					@Override
					public void onStateChanged(WebSocket arg0, WebSocketState arg1) throws Exception {
						// TODO Auto-generated method stub

					}

					@Override
					public void onSendingHandshake(WebSocket arg0, String arg1, List<String[]> arg2) throws Exception {
						// TODO Auto-generated method stub

					}

					@Override
					public void onSendingFrame(WebSocket arg0, WebSocketFrame arg1) throws Exception {
						// TODO Auto-generated method stub

					}

					@Override
					public void onSendError(WebSocket arg0, WebSocketException e, WebSocketFrame arg2) throws Exception {
						bus.send(new WSError(e));
					}

					@Override
					public void onPongFrame(WebSocket arg0, WebSocketFrame arg1) throws Exception {
						// TODO Auto-generated method stub

					}

					@Override
					public void onPingFrame(WebSocket arg0, WebSocketFrame arg1) throws Exception {
						// TODO Auto-generated method stub

					}

					@Override
					public void onMessageError(WebSocket arg0, WebSocketException e, List<WebSocketFrame> arg2) throws Exception {
						bus.send(new WSError(e));
					}

					@Override
					public void onMessageDecompressionError(WebSocket arg0, WebSocketException e, byte[] arg2) throws Exception {
						bus.send(new WSError(e));
					}

					@Override
					public void onFrameUnsent(WebSocket arg0, WebSocketFrame arg1) throws Exception {
						// TODO Auto-generated method stub

					}

					@Override
					public void onFrameSent(WebSocket arg0, WebSocketFrame arg1) throws Exception {
						// TODO Auto-generated method stub

					}

					@Override
					public void onFrameError(WebSocket arg0, WebSocketException e, WebSocketFrame arg2) throws Exception {
						bus.send(new WSError(e));
					}

					@Override
					public void onFrame(WebSocket arg0, WebSocketFrame arg1) throws Exception {
						// TODO Auto-generated method stub

					}

					@Override
					public void onError(WebSocket arg0, WebSocketException e) throws Exception {
						bus.send(new WSError(e));
					}

					@Override
					public void onDisconnected(WebSocket arg0, WebSocketFrame arg1, WebSocketFrame arg2, boolean arg3)
							throws Exception {
						bus.send(new WSDisconnected());
					}

					@Override
					public void onContinuationFrame(WebSocket arg0, WebSocketFrame arg1) throws Exception {
						// TODO Auto-generated method stub
					}

					@Override
					public void onConnected(WebSocket ws, Map<String, List<String>> arg1) throws Exception {
						bus.send(new WSConnected());
					}

					@Override
					public void onConnectError(WebSocket arg0, WebSocketException e) throws Exception {
						bus.send(new WSError(e));
					}

					@Override
					public void onCloseFrame(WebSocket arg0, WebSocketFrame arg1) throws Exception {
						// TODO Auto-generated method stub

					}

					@Override
					public void onBinaryMessage(WebSocket arg0, byte[] arg1) throws Exception {
						// TODO Auto-generated method stub

					}

					@Override
					public void onBinaryFrame(WebSocket arg0, WebSocketFrame arg1) throws Exception {
						// TODO Auto-generated method stub

					}

					@Override
					public void handleCallbackError(WebSocket ws, Throwable e) {
						LOGGER.severe(e.getMessage());
						System.exit(-1);
					}
				});

		initBus();
	}

	private void initBus() {
		bus.toObservable().subscribe(o -> {
			BusBase base = (BusBase) o;
			try {
				if (base instanceof WSRequestSendText) {
					WSRequestSendText event = (WSRequestSendText) o;
					ws.sendText(event.request);
				} 
				else if (base instanceof WSError) {
					WSError event = (WSError) base;
					bus.send(new Emailer.SendEmailWSError(event));
				}
				else if (base instanceof WSRequestShutdown) {
					ws.disconnect(1013); // try again later code
					WSRequestShutdown event = (WSRequestShutdown) base;
					System.exit(event.isUnexpected ? 1 : 0);
				}
			} catch (Exception e) {
				MyLogger.exception(LOGGER, base.toString(), e);
			}

		});
	}

	public void start() throws WebSocketException, IOException {
		ws.connect();
	}

	public static class WSConnected extends BusBase {
	}

	public static class WSDisconnected extends BusBase {
	}

	public static class WSError extends BusBase {
		public final Exception e;

		public WSError(Exception e) {
			super();
			this.e = e;
		}

	}

	public static class WSGotText extends BusBase {
		public String raw;

		public WSGotText(String raw) {
			super();
			this.raw = raw;
		}

	}

	public static class WSRequestSendText extends BusBase {
		public String request;

		public WSRequestSendText(String request) {
			super();
			this.request = request;
		}

	}

	public static class WSRequestShutdown extends BusBase {
		public final boolean isUnexpected;
		
		public WSRequestShutdown(boolean isUnexpected) {
			this.isUnexpected = isUnexpected;
		}
	}

}
