package com.mbcu.mmm.sequences;

import java.util.concurrent.ConcurrentHashMap;

import com.mbcu.mmm.main.WebSocketClient;
import com.mbcu.mmm.main.WebSocketClient.WSError;
import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.notice.SenderSES;
import com.mbcu.mmm.rx.BusBase;
import com.mbcu.mmm.utils.MyLogger;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class Emailer extends Base {
	private final CompositeDisposable disposables = new CompositeDisposable();
	private final ConcurrentHashMap<SendEmailBotError, Long> notices = new ConcurrentHashMap<>();
	private final long GAP_MS = 1000 * 60 * 30; // 30 mins
	private SenderSES sender;

	public Emailer(Config config) {
		super(MyLogger.getLogger(Emailer.class.getName()), config);
		sender = new SenderSES(config, LOGGER);

		disposables
				.add(bus.toObservable().subscribeOn(Schedulers.newThread()).subscribeWith(new DisposableObserver<Object>() {

					@Override
					public void onNext(Object o) {
						BusBase base = (BusBase) o;

						if (base instanceof SendEmailBotError) {
							check((SendEmailBotError) base);
						}
						else if (base instanceof SendEmailWSError) {
							SendEmailWSError event = (SendEmailWSError) base;
							sender.sendWSError(event.e.e);
							bus.send(new WebSocketClient.WSRequestShutdown(true));
						}
					}

					@Override
					public void onError(Throwable e) {
						// TODO Auto-generated method stub

					}

					@Override
					public void onComplete() {
						// TODO Auto-generated method stub

					}

				}));
	}

	private void check(SendEmailBotError r) {
		long now = System.currentTimeMillis();
		Long lastInserted = notices.get(r);
		if (lastInserted != null && lastInserted + GAP_MS > now) {
			return;
		}
		notices.put(r, now);
		sender.sendBotError(r);
	}

	public static Emailer newInstance(Config config) {
		Emailer res = new Emailer(config);
		return res;
	}

	public static class SendEmailBotError extends BusBase {
		public final String error;
		public final String pair;
		public final long millis;
		public final boolean shutDown;

		public SendEmailBotError(String error, String pair, long millis, boolean shutDown) {
			this.error = error;
			this.pair = pair;
			this.millis = millis;
			this.shutDown = shutDown;
		}

		@Override
		public int hashCode() {
			return (pair.hashCode() + error.hashCode()) / 17;
		}

		@Override
		public boolean equals(Object o) {
			if (o == null)
				return false;
			if (!(o instanceof SendEmailBotError))
				return false;
			SendEmailBotError t = (SendEmailBotError) o;
			if (t.pair.equals(pair) && t.error.equals(error)) {
				return true;
			}
			return false;
		}
	}
	
	public static class SendEmailWSError extends BusBase{
		public final WSError e;

		public SendEmailWSError(WSError e) {
			super();
			this.e = e;
		}		
	}
	
}
