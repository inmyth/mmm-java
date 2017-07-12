package com.mbcu.mmm.sequences.counters;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.logging.Level;

import com.mbcu.mmm.models.internal.BotConfig;
import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.models.internal.RLOrder;
import com.mbcu.mmm.models.internal.RLOrder.Direction;
import com.mbcu.mmm.rx.RxBus;
import com.mbcu.mmm.rx.RxBusProvider;
import com.mbcu.mmm.sequences.Base;
import com.mbcu.mmm.sequences.Common;
import com.mbcu.mmm.sequences.Common.OnOfferExecuted;
import com.mbcu.mmm.utils.MyLogger;
import com.ripple.core.coretypes.Amount;

import io.reactivex.Observer;
import io.reactivex.annotations.Nullable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class Yuki extends Base implements Counter {
	private RxBus bus = RxBusProvider.getInstance();
	private Config config;
	int count;

	public Yuki(Config config) {
		super(MyLogger.getLogger(Yuki.class.getName()), config);
		this.config = config;

		bus.toObservable().subscribeOn(Schedulers.newThread()).subscribe(new Observer<Object>() {

			@Override
			public void onSubscribe(Disposable d) {
			}

			@Override
			public void onNext(Object o) {
				if (o instanceof Common.OnOfferExecuted) {
					OnOfferExecuted event = (OnOfferExecuted) o;
					counter(event.oes);
				}
			}

			@Override
			public void onError(Throwable e) {
				log(e.getMessage(), Level.SEVERE);
			}

			@Override
			public void onComplete() {
			}
		});

	}

	public static Yuki newInstance(Config config) {
		Yuki counter = new Yuki(config);
		return counter;
	}

	public void counter(List<RLOrder> oes) {
		oes.forEach(oe -> {
			RLOrder counter = buildCounter(oe);
			if (counter != null) {
				onCounterReady(counter);
			}

		});
	}

	@Nullable
	public RLOrder buildCounter(RLOrder origin) {
		boolean isDirectionMatch = true;
		BotConfig botConfig = config.getBotConfigMap().get(origin.getPair());
		if (botConfig == null) {
			botConfig = config.getBotConfigMap().get(origin.getReversePair());
			isDirectionMatch = false;
		}
		if (botConfig == null) {
			return null;
		}
		Amount oldQuantity = origin.getQuantity();
		Amount oldTotalPrice = origin.getTotalPrice();
		RLOrder res = null;
		if (isDirectionMatch) {
			Amount newQuantity = origin.getQuantity().multiply(new BigDecimal("-1"));
			BigDecimal oldRate = origin.getAsk();
			BigDecimal newRate = origin.getAsk().add(botConfig.getGridSpace());
			Amount newTotalPrice = RLOrder.amount(newQuantity.value().multiply(newRate), oldTotalPrice.currency(),
					oldTotalPrice.issuer());
			res = RLOrder.basic(Direction.SELL, newQuantity, newTotalPrice);
		} else {
			Amount newQuantity = origin.getTotalPrice().multiply(new BigDecimal("-1"));
			BigDecimal newRate = BigDecimal.ONE.divide(origin.getAsk().add(botConfig.getGridSpace()), MathContext.DECIMAL128);
			if (newRate.compareTo(BigDecimal.ZERO) <= 0) {
				log("counter rate below zero " + newRate + " " + origin.getPair(), Level.SEVERE);
			}
			Amount newTotalPricce = RLOrder.amount(newQuantity.value().multiply(newRate), oldQuantity.currency(),
					oldQuantity.issuer());
			res = RLOrder.basic(Direction.BUY, newQuantity, newTotalPricce );
		}
		return res;
	}

	@Override
	public void onCounterReady(RLOrder counter) {
		bus.send(new Counter.CounterReady(counter));
	}
}
