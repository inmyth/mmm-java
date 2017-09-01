package com.mbcu.mmm.sequences;

import java.util.concurrent.ConcurrentHashMap;

import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.notice.SenderSES;
import com.mbcu.mmm.rx.BusBase;
import com.mbcu.mmm.utils.MyLogger;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class Notifier extends Base {
	private final CompositeDisposable disposables = new CompositeDisposable();
	private final ConcurrentHashMap<RequestEmailNotice, Long> pending = new ConcurrentHashMap<>();
	private long lastSend;
	

	public Notifier(Config config) {
		super(MyLogger.getLogger(Notifier.class.getName()), config);
		
		bus.toObservable()
		.subscribeOn(Schedulers.newThread())
		.subscribeWith(new DisposableObserver<Object>() {

			@Override
			public void onNext(Object t) {
				
				
			}

			@Override
			public void onError(Throwable e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onComplete() {
				// TODO Auto-generated method stub
				
			}
			
		});
		
	}
	
	
	
	
	public static Notifier newInstance(Config config){
		Notifier res = new Notifier(config);
		return res;
	}

	
	public static class RequestEmailNotice extends BusBase{
		public String error;
		public String pair;
		public long ts;
		
		public RequestEmailNotice(String error, String pair, long ts) {
			super();
			this.error = error;
			this.pair = pair;
			this.ts = ts;
		}
		
		@Override
		public int hashCode() {
			return pair.hashCode() + error.hashCode();
		}
		
		@Override
		public boolean equals(Object o) {
	    if(o == null)                return false;
	    if(!(o instanceof RequestEmailNotice)) return false;

		}
	}
}
