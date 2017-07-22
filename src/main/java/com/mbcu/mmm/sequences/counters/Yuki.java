package com.mbcu.mmm.sequences.counters;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.logging.Level;

import com.mbcu.mmm.models.internal.BefAf;
import com.mbcu.mmm.models.internal.BotConfig;
import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.models.internal.RLOrder;
import com.mbcu.mmm.models.internal.RLOrder.Direction;
import com.mbcu.mmm.rx.RxBus;
import com.mbcu.mmm.rx.RxBusProvider;
import com.mbcu.mmm.sequences.Base;
import com.mbcu.mmm.sequences.Common;
import com.mbcu.mmm.sequences.Common.OnOfferExecuted;
import com.mbcu.mmm.sequences.state.State;
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
					counterOE(event.oes);
				}
				else if (o instanceof Common.OnRemainder){
					Common.OnRemainder event = (Common.OnRemainder) o;
					counterOR(event.ba);
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
	
	public void counterOR(BefAf ba){
		BotConfigDirection bcd = new BotConfigDirection(config, ba.before);
		if (bcd.botConfig == null || bcd.botConfig.getPercentToCounter() == 0) {
			return;
		}
		Amount quantity = bcd.isDirectionMatch ? ba.after.getQuantity() : ba.after.getTotalPrice();
		Amount totalPrice = bcd.isDirectionMatch ? ba.after.getTotalPrice() : ba.after.getQuantity();
		BigDecimal rate = bcd.isDirectionMatch ? ba.before.getAsk() : BigDecimal.ONE.divide(ba.before.getAsk(), MathContext.DECIMAL64);		
		BigDecimal botQuantity = bcd.isDirectionMatch ? bcd.botConfig.getBuyOrderQuantity() : bcd.botConfig.getSellOrderQuantity();
		
		Amount remainder = quantity.subtract(botQuantity).abs();
		BigDecimal reference = botQuantity.multiply(new BigDecimal(bcd.botConfig.getPercentToCounter()), MathContext.DECIMAL64).divide(new BigDecimal("100"), MathContext.DECIMAL64);
		
		if (remainder.value().compareTo(reference) >= 0){
			Amount newTotalPrice = new Amount(remainder.multiply(rate).value(), totalPrice.currency(), totalPrice.issuer());			
			RLOrder counter = RLOrder.rateUnneeded(Direction.BUY, remainder, newTotalPrice);
			System.out.println("OFFER REMAINDER COUNTER\n" + counter.stringify());		
			onCounterReady(counter);
			
		}
	}

	public void counterOE(List<RLOrder> oes) {
		oes.forEach(oe -> {
			RLOrder counter = buildOECounter(oe);
			if (counter != null) {
				onCounterReady(counter);
			}
		});
	}
	
	
	private static class BotConfigDirection {
		boolean isDirectionMatch = true;
		BotConfig botConfig;
		
		BotConfigDirection(Config config, RLOrder offer){
			botConfig = config.getBotConfigMap().get(offer.getPair());
			if (botConfig == null) {
				botConfig = config.getBotConfigMap().get(offer.getReversePair());
				isDirectionMatch = false;
			}
		}	
	}

	@Nullable
	public RLOrder buildOECounter(RLOrder origin) {
		
		BotConfigDirection bcd = new BotConfigDirection(config, origin);
		if (bcd.botConfig == null || bcd.botConfig.getPercentToCounter() != 0) {
			return null;
		}
		boolean isDirectionMatch = bcd.isDirectionMatch;
		BotConfig botConfig = bcd.botConfig;

		Amount oldQuantity = origin.getQuantity();
		Amount oldTotalPrice = origin.getTotalPrice();
		RLOrder res = null;
		if (isDirectionMatch) {
			Amount newQuantity = origin.getQuantity().multiply(new BigDecimal("-1"));
			BigDecimal newRate = origin.getAsk().add(botConfig.getGridSpace());
			Amount newTotalPrice = RLOrder.amount(newQuantity.value().multiply(newRate), oldTotalPrice.currency(), oldTotalPrice.issuer());
			res = RLOrder.rateUnneeded(Direction.SELL, newQuantity, newTotalPrice);
		} else {
			Amount newQuantity = origin.getTotalPrice().multiply(new BigDecimal("-1"));
			BigDecimal newRate = BigDecimal.ONE.divide(origin.getAsk(), MathContext.DECIMAL128).subtract(botConfig.getGridSpace());
			if (newRate.compareTo(BigDecimal.ZERO) <= 0) {
				log("counter rate below zero " + newRate + " " + origin.getPair(), Level.SEVERE);
			}
			Amount newTotalPrice = RLOrder.amount(newQuantity.value().multiply(newRate), oldQuantity.currency(),
					oldQuantity.issuer());
			res = RLOrder.rateUnneeded(Direction.BUY, newQuantity, newTotalPrice);
		}
		return res;
	}

	@Override
	public void onCounterReady(RLOrder counter) {
		bus.send(new State.OnOrderReady(counter));
	}
}
