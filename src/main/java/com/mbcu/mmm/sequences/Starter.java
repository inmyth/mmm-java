package com.mbcu.mmm.sequences;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;

import com.mbcu.mmm.main.WebSocketClient;
import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.models.request.AccountInfo;
import com.mbcu.mmm.models.request.AccountOffers;
import com.mbcu.mmm.models.request.Request.Command;
import com.mbcu.mmm.models.request.Subscribe;
import com.mbcu.mmm.models.request.Subscribe.Stream;
import com.mbcu.mmm.rx.BusBase;
import com.mbcu.mmm.sequences.counters.Yuki;
import com.mbcu.mmm.sequences.state.StateProvider;
import com.mbcu.mmm.utils.MyLogger;
import com.neovisionaries.ws.client.WebSocketException;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class Starter extends Base {
	private CountDownLatch latch;

	private Starter(Config config) {
		super(MyLogger.getLogger(Starter.class.getName()), config);
		List<CompositeDisposable> dispos = new ArrayList<>();

		CompositeDisposable disSubscribeLedger = new CompositeDisposable();
		disSubscribeLedger
				.add(bus.toObservable().subscribeOn(Schedulers.newThread()).subscribeWith(new DisposableObserver<Object>() {

					@Override
					public void onNext(Object o) {
						BusBase base = (BusBase) o;
						try {
							if (base instanceof Common.OnAccountInfoSequence) {
								latch.countDown();
								disSubscribeLedger.dispose();
							}
						} catch (Exception e) {
							MyLogger.exception(LOGGER, base.toString(), e);
							throw e;
						}
					}

					@Override
					public void onError(Throwable e) {
					}

					@Override
					public void onComplete() {
					}

				}));
		dispos.add(disSubscribeLedger);

		CompositeDisposable disAccInfo = new CompositeDisposable();
		disAccInfo
				.add(bus.toObservable().subscribeOn(Schedulers.newThread()).subscribeWith(new DisposableObserver<Object>() {

					@Override
					public void onNext(Object o) {
						BusBase base = (BusBase) o;
						try {
							if (o instanceof Common.OnAccountInfoSequence) {
								latch.countDown();
								disAccInfo.dispose();
							}
						} catch (Exception e) {
							MyLogger.exception(LOGGER, base.toString(), e);
							throw e;
						}
					}

					@Override
					public void onError(Throwable e) {
					}

					@Override
					public void onComplete() {
					}

				}));
		dispos.add(disAccInfo);

		CompositeDisposable disAccountOffers = new CompositeDisposable();
		disAccountOffers
				.add(bus.toObservable().subscribeOn(Schedulers.newThread()).subscribeWith(new DisposableObserver<Object>() {

					@Override
					public void onNext(Object o) {
						BusBase base = (BusBase) o;
						try {
							if (base instanceof Orderbook.OnAccOffersDone) {
								latch.countDown();
								disAccountOffers.dispose();
							}
						} catch (Exception e) {
							MyLogger.exception(LOGGER, base.toString(), e);
							throw e;
						}
					}

					@Override
					public void onError(Throwable e) {
					}

					@Override
					public void onComplete() {
					}

				}));
		dispos.add(disAccountOffers);

		CompositeDisposable disLedgerClosed = new CompositeDisposable();
		disLedgerClosed
				.add(bus.toObservable().subscribeOn(Schedulers.newThread()).subscribeWith(new DisposableObserver<Object>() {

					@Override
					public void onNext(Object o) {
						BusBase base = (BusBase) o;
						try {
							if (base instanceof Common.OnLedgerClosed) {
								latch.countDown();
								disLedgerClosed.dispose();
							}
						} catch (Exception e) {
							MyLogger.exception(LOGGER, base.toString(), e);
							throw e;
						}
					}

					@Override
					public void onError(Throwable e) {
					}

					@Override
					public void onComplete() {
					}

				}));
		dispos.add(disLedgerClosed);

		CompositeDisposable disOnWSConnected = new CompositeDisposable();
		disOnWSConnected
				.add(bus.toObservable().subscribeOn(Schedulers.newThread()).subscribeWith(new DisposableObserver<Object>() {

					@Override
					public void onNext(Object o) {
						BusBase base = (BusBase) o;
						try {
							if (base instanceof WebSocketClient.WSConnected) {
								log("connected", Level.FINER);
								latch.countDown();
								sendInitRequests();
								disOnWSConnected.dispose();
							}
						} catch (Exception e) {
							MyLogger.exception(LOGGER, base.toString(), e);
							throw e;
						}
					}

					@Override
					public void onError(Throwable e) {
					}

					@Override
					public void onComplete() {
					}

				}));
		dispos.add(disOnWSConnected);

		CompositeDisposable disOnAccountOffersDone = new CompositeDisposable();
		disOnAccountOffersDone
				.add(bus.toObservable().subscribeOn(Schedulers.newThread()).subscribeWith(new DisposableObserver<Object>() {

					@Override
					public void onNext(Object o) {
						if (o instanceof Orderbook.OnAccOffersDone) {
							latch.countDown();
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
		dispos.add(disOnAccountOffersDone);
		latch = new CountDownLatch(dispos.size());
	}

	private void sendInitRequests() {
		bus.send(new WebSocketClient.WSRequestSendText(AccountInfo.of(config).stringify()));
		bus.send(new WebSocketClient.WSRequestSendText(AccountOffers.of(config).stringify()));
		String subscribeRequest = Subscribe.build(Command.SUBSCRIBE).withStream(Stream.LEDGER)
				.withAccount(config.getCredentials().getAddress()).stringify();
		bus.send(new WebSocketClient.WSRequestSendText(subscribeRequest));
	}

	public static Starter newInstance(Config config) {
		Starter res = new Starter(config);
		StateProvider.getInstance(config);
		return res;
	}

	public void start() throws IOException, WebSocketException, InterruptedException {
		log("Initiating ...");
		Common.newInstance(config);
		Balancer.newInstance(config);
		Emailer.newInstance(config);
		WebSocketClient webSocketClient = new WebSocketClient(super.config);
		webSocketClient.start();
		latch.await();
		postInit();
	}

	private void postInit() {
		log("Initiation complete");
		Yuki.newInstance(config);
		Dataapi.newInstance(config);
		bus.send(new OnInitiated());
	}

	public static class OnInitiated extends BusBase {
	}

}
