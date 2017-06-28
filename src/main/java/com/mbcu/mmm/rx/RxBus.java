package com.mbcu.mmm.rx;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

public final class RxBus {
	private final PublishSubject<Object> bus = PublishSubject.create();

	public void send(final Object event) {
		bus.onNext(event);
	}

	public Observable<Object> toObservable() {
		return bus;
	}

	public boolean hasObservers() {
		return bus.hasObservers();
	}
	

  public Flowable<Object> asFlowable() {
    return bus.toFlowable(BackpressureStrategy.LATEST);
  }


}