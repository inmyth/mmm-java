package com.mbcu.mmm.sequences.counters;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.mbcu.mmm.models.internal.BefAf;
import com.mbcu.mmm.models.internal.BotConfig;
import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.models.internal.RLOrder;
import com.mbcu.mmm.models.internal.RLOrder.Direction;
import com.mbcu.mmm.rx.BusBase;
import com.mbcu.mmm.rx.RxBus;
import com.mbcu.mmm.rx.RxBusProvider;
import com.mbcu.mmm.sequences.Base;
import com.mbcu.mmm.sequences.Common;
import com.mbcu.mmm.sequences.Common.OnOfferExecuted;
import com.mbcu.mmm.sequences.state.State;
import com.mbcu.mmm.sequences.state.State.OnOrderReady.Source;
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
			public void onNext(Object o){	
				BusBase base = (BusBase) o;
				try {
					if (o instanceof Common.OnOfferExecuted) {
						OnOfferExecuted event = (OnOfferExecuted) o;
						counterOE(event.oes);
					}				
					else if (o instanceof Common.OnDifference){
						Common.OnDifference event = (Common.OnDifference) o;
						List<BefAf> fullyConsumeds = event.bas.stream()
								.filter(ba -> {
									return ba.after.getQuantity().value().compareTo(BigDecimal.ZERO) == 0 && ba.after.getTotalPrice().value().compareTo(BigDecimal.ZERO) == 0;
								})
								.collect(Collectors.toList());
						counterOR(fullyConsumeds);
					}
				} catch (Exception e) {
					MyLogger.exception(LOGGER, base.toString(), e);		
					throw e;
				}
			}

			@Override
			public void onError(Throwable e) {}

			@Override
			public void onComplete() {
			}
		});

	}

	public static Yuki newInstance(Config config) {
		Yuki counter = new Yuki(config);
		return counter;
	}
	
	public void counterOR(List<BefAf> bas) {
		bas.forEach(ba -> {
			log("Full Counter");
			BotConfigDirection bcd = new BotConfigDirection(config, ba.before);
			if (bcd.botConfig != null && !bcd.botConfig.isPartialCounter()) {
				RLOrder counter = buildWholeCounter(bcd, ba);
				if (counter != null) {
					onCounterReady(counter);
				}
			}

		});
	}
	
	
	/*
	 * This part can be configured so instead of countering, the bot will replace taken order. 
	 */
	@Nullable
	public RLOrder buildWholeCounter(BotConfigDirection bcd, BefAf ba){
		RLOrder res 		= null;
		BigDecimal rate = ba.before.getRate();	
		RLOrder origin;
		
		if (bcd.botConfig.isPctGridSpace()){
			if (bcd.isDirectionMatch){
				BigDecimal botQuantity 	= bcd.botConfig.getSellOrderQuantity();		
				//subtract is used to conveniently preserve Amount issuer and currency
				Amount quantity 				= ba.before.getQuantity().subtract(botQuantity);
				origin = RLOrder.fromWholeConsumed(Direction.BUY, quantity, ba.after.getTotalPrice(), rate);			
			}
			else {
				BigDecimal botQuantity 	= bcd.botConfig.getBuyOrderQuantity();
				Amount totalPrice 			= ba.after.getTotalPrice().subtract(botQuantity);			
				origin = RLOrder.fromWholeConsumed(Direction.BUY, ba.before.getQuantity(), totalPrice, rate);
			}		
			res = yukiPct(origin, bcd.botConfig, bcd.isDirectionMatch);
		}
		else {
			if (bcd.isDirectionMatch){
				BigDecimal botQuantity 	= bcd.botConfig.getSellOrderQuantity();		
				Amount quantity 				= ba.after.getQuantity().subtract(botQuantity);
				origin = RLOrder.fromWholeConsumed(Direction.BUY, quantity, ba.after.getTotalPrice(), rate);
			}
			else {
				BigDecimal botQuantity 	= bcd.botConfig.getBuyOrderQuantity();
				Amount totalPrice 			= ba.after.getTotalPrice().subtract(botQuantity);			
				origin = RLOrder.fromWholeConsumed(Direction.BUY, ba.after.getQuantity(), totalPrice, rate);
			}		
			res = yuki(origin, bcd.botConfig, bcd.isDirectionMatch);	
		}
		return res;
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
			botConfig = config.getBotConfigMap().get(offer.getCpair().getFw());
			if (botConfig == null) {
				botConfig = config.getBotConfigMap().get(offer.getCpair().getRv());
				isDirectionMatch = false;
			}
		}	
	}

	@Nullable
	public RLOrder buildOECounter(RLOrder origin) {
		
		BotConfigDirection bcd = new BotConfigDirection(config, origin);
		if (bcd.botConfig == null || !bcd.botConfig.isPartialCounter()) {
			return null;
		}
		boolean isDirectionMatch = bcd.isDirectionMatch;
		BotConfig botConfig = bcd.botConfig;
		
		return yuki(origin, botConfig, isDirectionMatch);
	}
	
	private RLOrder yuki(RLOrder origin, BotConfig botConfig, boolean isDirectionMatch){
		Amount oldQuantity   = origin.getQuantity();
		Amount oldTotalPrice = origin.getTotalPrice();
		Amount newQuantity   = oldQuantity.multiply(new BigDecimal("-1"));

		RLOrder res = null;
		if (isDirectionMatch) {
			BigDecimal newRate = origin.getRate().add(botConfig.getGridSpace());
			Amount newTotalPrice = RLOrder.amount(newQuantity.value().multiply(newRate), oldTotalPrice.currency(), oldTotalPrice.issuer());
			res = RLOrder.rateUnneeded(Direction.SELL, newQuantity, newTotalPrice);
		} 
		else {
			BigDecimal newRate = BigDecimal.ONE.divide(origin.getRate(), MathContext.DECIMAL128).subtract(botConfig.getGridSpace());
			if (newRate.compareTo(BigDecimal.ZERO) <= 0) {
				log("counter rate to or below zero " + newRate + " " + origin.getCpair(), Level.SEVERE);
				return null;
			}
			Amount newTotalPrice = RLOrder.amount(newQuantity.value().multiply(newRate), oldQuantity.currency(), oldQuantity.issuer());
			res = RLOrder.rateUnneeded(Direction.BUY, newQuantity, newTotalPrice);
		}
		return res;
	}
	
	private RLOrder yukiPct(RLOrder origin, BotConfig botConfig, boolean isDirectionMatch){
		Amount oldQuantity 		 = origin.getQuantity();
		Amount oldTotalPrice	 = origin.getTotalPrice();
		BigDecimal pct				 = botConfig.getGridSpace();	
		BigDecimal oldRate   	 = origin.getRate();
		BigDecimal newRate     = null;
		Amount newQuantity 		 = origin.getTotalPrice().multiply(new BigDecimal("-1"));
		
		RLOrder res = null;
		if (isDirectionMatch){
		  newRate 				  	 = oldRate.add(pct.multiply(oldRate, MathContext.DECIMAL64));
			Amount newTotalPrice = RLOrder.amount(oldQuantity.value().multiply(newRate), oldTotalPrice.currency(), oldTotalPrice.issuer());
			res = RLOrder.rateUnneeded(Direction.SELL, oldQuantity, newTotalPrice);
		}
		else {
			newRate 						 = BigDecimal.ONE.divide(origin.getRate(), MathContext.DECIMAL128);
			newRate						   = newRate.subtract(pct.multiply(newRate, MathContext.DECIMAL64));
			if (newRate.compareTo(BigDecimal.ZERO) <= 0) {
				log("counter rate to or below zero " + newRate + " " + origin.getCpair(), Level.SEVERE);
				return null;
			}
			Amount newTotalPrice = RLOrder.amount(newQuantity.value().multiply(newRate), oldQuantity.currency(), oldQuantity.issuer());
			res = RLOrder.rateUnneeded(Direction.BUY, newQuantity, newTotalPrice);
		}
		return res;	
	}
	
	

	@Override
	public void onCounterReady(RLOrder counter) {
		bus.send(new State.OnOrderReady(counter, Source.COUNTER));
	}
	

}
