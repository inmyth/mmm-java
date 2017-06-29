package com.mbcu.mmm.main;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.mbcu.mmm.main.Events.WSRequestSendText;
import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.rx.RxBus;
import com.mbcu.mmm.rx.RxBusProvider;
import com.neovisionaries.ws.client.ThreadType;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketExtension;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;
import com.neovisionaries.ws.client.WebSocketListener;
import com.neovisionaries.ws.client.WebSocketState;

public class WebSocketClient {
	private final static Logger LOGGER = Logger.getLogger(WebSocketClient.class.getName());
	private WebSocket ws;
	private RxBus bus = RxBusProvider.getInstance();

	public WebSocketClient(Config config) throws IOException {

		this.ws = new WebSocketFactory().setConnectionTimeout(5000).createSocket(config.getNet()).addListener(new WebSocketListener() {

			@Override
			public void onUnexpectedError(WebSocket arg0, WebSocketException e) throws Exception {
				bus.send(new Events.WSError(e));
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
				bus.send(new Events.WSError(e));
			}

			@Override
			public void onTextMessage(WebSocket arg0, String raw) throws Exception {
				bus.send(new Events.WSGotText(raw));
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
				bus.send(new Events.WSError(e));
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
				bus.send(new Events.WSError(e));
			}

			@Override
			public void onMessageDecompressionError(WebSocket arg0, WebSocketException e, byte[] arg2) throws Exception {
				bus.send(new Events.WSError(e));
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
				bus.send(new Events.WSError(e));
			}

			@Override
			public void onFrame(WebSocket arg0, WebSocketFrame arg1) throws Exception {
				// TODO Auto-generated method stub

			}

			@Override
			public void onError(WebSocket arg0, WebSocketException e) throws Exception {
				bus.send(new Events.WSError(e));
			}

			@Override
			public void onDisconnected(WebSocket arg0, WebSocketFrame arg1, WebSocketFrame arg2, boolean arg3) throws Exception {
				bus.send(new Events.WSDisconnected());
			}

			@Override
			public void onContinuationFrame(WebSocket arg0, WebSocketFrame arg1) throws Exception {
				// TODO Auto-generated method stub
			}

			@Override
			public void onConnected(WebSocket ws, Map<String, List<String>> arg1) throws Exception {
				bus.send(new Events.WSConnected());
			}

			@Override
			public void onConnectError(WebSocket arg0, WebSocketException e) throws Exception {
				bus.send(new Events.WSError(e));
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
			public void handleCallbackError(WebSocket arg0, Throwable e) throws Exception {
				LOGGER.severe(e.getMessage());

			}
		}).addExtension(WebSocketExtension.PERMESSAGE_DEFLATE);
		
		initBus();
	}

	private void initBus(){
		bus.toObservable().subscribe(o -> {
			if (o instanceof Events.WSRequestSendText){
				WSRequestSendText event = (WSRequestSendText) o;
				ws.sendText(event.request);		
			}
			
		});
	}
	
	public void start() throws WebSocketException, IOException {
		ws.connect();
	}

}
