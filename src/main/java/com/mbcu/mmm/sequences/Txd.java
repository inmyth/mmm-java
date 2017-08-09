package com.mbcu.mmm.sequences;

import com.mbcu.mmm.rx.RxBus;
import com.mbcu.mmm.rx.RxBusProvider;
import com.mbcu.mmm.utils.MyLogger;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class Txd extends Base {
	
	private final int delSeq;
	private final int newSeq;
	private final RxBus bus = RxBusProvider.getInstance();
	private final CompositeDisposable disposables = new CompositeDisposable();


	private Txd(int delSeq, int newSeq) {
		super(MyLogger.getLogger(String.format(Txd.class.getName())), null);
		this.delSeq = delSeq;
		this.newSeq = newSeq;
	}
	
	private void initBus() {
		disposables.add(bus.toObservable().subscribeOn(Schedulers.newThread())
			.subscribeWith(new DisposableObserver<Object>() {

				@Override
				public void onNext(Object t) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void onError(Throwable e) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void onComplete() {
					// TODO Auto-generated method stub
					
				}
			})				
			);
		
	}
 	

}
