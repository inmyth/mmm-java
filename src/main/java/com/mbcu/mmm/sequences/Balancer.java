package com.mbcu.mmm.sequences;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.mbcu.mmm.main.WebSocketClient;
import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.models.request.AccountOffers;
import com.mbcu.mmm.rx.BusBase;
import com.mbcu.mmm.sequences.Common.OnAccountOffers;
import com.mbcu.mmm.sequences.Orderbook.OnAccOffersDone;
import com.mbcu.mmm.utils.MyLogger;

import io.reactivex.Observer;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class Balancer extends Base {

	private final List<Orderbook> perm = new ArrayList<>();
	private AtomicInteger existingOrderSize = new AtomicInteger(0);

	public Balancer(Config config) {
		super(MyLogger.getLogger(Starter.class.getName()), config);
		buildOrderbooks();
		CompositeDisposable accOfferDis = new CompositeDisposable();
		accOfferDis
				.add(bus.toObservable().subscribeOn(Schedulers.newThread()).subscribeWith(new DisposableObserver<Object>() {

					@Override
					public void onNext(Object o) {
						BusBase base = (BusBase) o;
						if (base instanceof Common.OnAccountOffers) {
							OnAccountOffers event = (OnAccountOffers) base;
							existingOrderSize.addAndGet(event.accOffs.size());
							if (event.marker != null) {
								bus.send(new WebSocketClient.WSRequestSendText(
										AccountOffers.of(config).withMarker(event.marker).stringify()));
							} else {
								bus.send(new OnAccOffersDone());
								log("Account offers : " + existingOrderSize.get());
								accOfferDis.clear();
							}
						}

					}

					@Override
					public void onError(Throwable e) {
						// TODO Auto-generated method stub

					}

					@Override
					public void onComplete() {
					}
				}));

		bus.toObservable().subscribe(new Observer<Object>() {

			@Override
			public void onSubscribe(Disposable d) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onNext(Object o) {

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

	private void buildOrderbooks() {
		super.config.getBotConfigMap().values().stream().forEach(botConfig -> {
			perm.add(Orderbook.newInstance(botConfig, config));
		});
	}

	public static Balancer newInstance(Config config) {
		return new Balancer(config);
	}

	public static class OnRequestNonOrderbookRLOrder extends BusBase {
		public String pair;

		public OnRequestNonOrderbookRLOrder(String pair) {
			super();
			this.pair = pair;
		}
	}

}
