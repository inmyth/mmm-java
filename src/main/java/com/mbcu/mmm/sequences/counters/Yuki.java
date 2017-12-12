package com.mbcu.mmm.sequences.counters;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

import com.mbcu.mmm.models.internal.BefAf;
import com.mbcu.mmm.models.internal.BotConfig;
import com.mbcu.mmm.models.internal.BotConfig.Strategy;
import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.models.internal.RLOrder;
import com.mbcu.mmm.models.internal.RLOrder.Direction;
import com.mbcu.mmm.rx.BusBase;
import com.mbcu.mmm.rx.RxBus;
import com.mbcu.mmm.rx.RxBusProvider;
import com.mbcu.mmm.sequences.Base;
import com.mbcu.mmm.sequences.Common;
import com.mbcu.mmm.sequences.Orderbook;
import com.mbcu.mmm.sequences.Orderbook.OnOrderFullConsumed;
import com.mbcu.mmm.sequences.Common.OnOfferExecuted;
import com.mbcu.mmm.sequences.state.State;
import com.mbcu.mmm.sequences.state.State.OnOrderReady.Source;
import com.mbcu.mmm.utils.MyLogger;
import com.ripple.core.coretypes.Amount;

import io.reactivex.Observer;
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
				BusBase base = (BusBase) o;
				try {
					if (o instanceof Orderbook.OnOrderFullConsumed){
						OnOrderFullConsumed event = (OnOrderFullConsumed) o;
						counterFull(event.origins);
					}
					
					
//					if (o instanceof Common.OnOfferExecuted) {
//						OnOfferExecuted event = (OnOfferExecuted) o;
//						counterPartial(event.oes);
//					} 
//					else if (o instanceof Common.OnDifference) {
//						Common.OnDifference event = (Common.OnDifference) o;		
//						counterFull(event.bas);
//					}
				} catch (Exception e) {
					MyLogger.exception(LOGGER, base.toString(), e);
					throw e;
				}
			}

			@Override
			public void onError(Throwable e) {}

			@Override
			public void onComplete() {}
		});

	}
	
	public static Yuki newInstance(Config config) {
		Yuki counter = new Yuki(config);
		return counter;
	}
	
	
//  change this to List<RLOrder> for incoming original orders
	public void counterFull(List<RLOrder> origins) {
		origins.stream()
		.map(o -> {return new FullCounterWrap(o, config);})
		.filter(wrap -> wrap.bcd.botConfig != null)
		.filter(wrap -> wrap.bcd.botConfig.getStrategy() 		== Strategy.FULLFIXED 
										|| wrap.bcd.botConfig.getStrategy() == Strategy.FULLRATEPCT 
										|| wrap.bcd.botConfig.getStrategy() == Strategy.FULLRATESEEDPCT )
		.map(this::buildFullCounter)
		.filter(Optional::isPresent)
		.map(Optional::get)
		.forEach(this::onCounterReady);
	}

	/*
	 * This part can be configured so instead of countering, the bot will replace
	 * taken order.
	 */
	public Optional<RLOrder> buildFullCounter(FullCounterWrap wrap) {
		RLOrder						 res = null;
		BotConfigDirection bcd = wrap.bcd;
		RLOrder 			 	   src = wrap.origin;
//		BefAf 						 ba  = wrap.ba;
		
		switch (bcd.botConfig.getStrategy()){
			case FULLRATEPCT : 
			case FULLRATESEEDPCT : 
//				BigDecimal rate = ba.before.getRate();
				BigDecimal oldRate = src.getRate();
				RLOrder origin;
//				if (bcd.isDirectionMatch) {
//					BigDecimal botQuantity = bcd.botConfig.getSellOrderQuantity();
//					// subtract is used to conveniently preserve Amount issuer and currency
////					Amount quantity = ba.after.getQuantity().subtract(botQuantity);
//					Amount quantity  	= src.
//					origin = RLOrder.fromWholeConsumed(Direction.BUY, quantity, ba.after.getTotalPrice(), rate);
//				} 
//				else {
//					BigDecimal botQuantity = bcd.botConfig.getBuyOrderQuantity();
//					Amount totalPrice = ba.after.getTotalPrice().subtract(botQuantity);
//					origin = RLOrder.fromWholeConsumed(Direction.BUY, ba.before.getQuantity().multiply(new BigDecimal("-1")), totalPrice, rate);
//				}
				res = yukiPct(src, bcd.botConfig, bcd.isDirectionMatch);
				break;
//			case FULLFIXED :
//				rate = ba.before.getRate();
//				if (bcd.isDirectionMatch) {
//					BigDecimal botQuantity = bcd.botConfig.getSellOrderQuantity();
//					Amount quantity = ba.after.getQuantity().subtract(botQuantity);
//					origin = RLOrder.fromWholeConsumed(Direction.BUY, quantity, ba.after.getTotalPrice(), rate);
//				} 
//				else {
//					BigDecimal botQuantity = bcd.botConfig.getBuyOrderQuantity();
//					Amount totalPrice = ba.after.getTotalPrice().subtract(botQuantity);
//					origin = RLOrder.fromWholeConsumed(Direction.BUY, ba.after.getQuantity(), totalPrice, rate);
//				}
//				bcd.rlOrder = origin;
//				res = yuki(bcd);
//				break;
			default : 
		}	
		if (res != null && (res.getQuantity().value().compareTo(BigDecimal.ZERO) <= 0 || res.getTotalPrice().value().compareTo(BigDecimal.ZERO) <= 0)) {
			log("Counter anomaly\n" + res.stringify());
		}
		log("Full Counter");
		return Optional.of(res);
	}
	
	private static class FullCounterWrap {
		RLOrder origin;
		BotConfigDirection bcd;
		
		public FullCounterWrap(RLOrder origin, Config config) {
			this.origin = origin;
			this.bcd = new BotConfigDirection(config, origin);
		}
	}

	/**
	 * 
	 * @param base
	 *          RLOrder reflecting consumed offer. Both totalPrice and quantity
	 *          have to be negative
	 * @param botConfig
	 * @param isDirectionMatch
	 *          Direction reflecting counter direction (not consumed order
	 *          direction)
	 * @return
	 */
	private RLOrder yukiPct(RLOrder source, BotConfig botConfig, boolean isSellCounter) {
		BigDecimal mtp 	 		 = BigDecimal.ONE.add(botConfig.getGridSpace());
    Amount srcQuantity 	 = source.getQuantity();
    Amount srcTotalPrice = source.getTotalPrice();
		BigDecimal newRate = null;
		RLOrder res 			 = null;

		if (isSellCounter){
			System.out.println(source.getRate().toPlainString());
			newRate = source.getRate().multiply(mtp, MathContext.DECIMAL64);
			Amount totalPrice = RLOrder.amount(srcQuantity.value().multiply(newRate), srcTotalPrice.currency(), srcTotalPrice.issuer());
//			Amount quantity   = srcQuantity.divide(mtp);
			res  = RLOrder.rateUnneeded(Direction.SELL, srcQuantity, totalPrice);
		} 
		else {
			newRate = source.getRate().divide(mtp, MathContext.DECIMAL64);
			Amount quantity 	= srcTotalPrice;
//			Amount totalPrice = srcQuantity.divide(mtp);
			Amount totalPrice = RLOrder.amount(srcTotalPrice.value().multiply(newRate), srcQuantity.currency(), srcQuantity.issuer());
			res  = RLOrder.rateUnneeded(Direction.BUY, quantity, totalPrice);
		}
		
//		BigDecimal minOne = new BigDecimal("-1");
//		Amount baseTotalPrice = base.getTotalPrice().multiply(minOne);
//		BigDecimal pct = botConfig.getGridSpace();
//		BigDecimal baseRate = base.getRate();
//		BigDecimal newRate = null;
//		Amount baseQuantity = base.getQuantity().multiply(minOne);
//
//		RLOrder res = null;
//		if (isSellCounter) {
//			newRate = baseRate.multiply(BigDecimal.ONE.add(pct), MathContext.DECIMAL64);
//			Amount newTotalPrice = RLOrder.amount(baseQuantity.value().multiply(newRate), baseTotalPrice.currency(), baseTotalPrice.issuer());
//			res = RLOrder.rateUnneeded(Direction.SELL, baseQuantity, newTotalPrice);
//		} else {
//			newRate = BigDecimal.ONE.divide(baseRate, MathContext.DECIMAL128);
//			newRate = newRate.multiply(BigDecimal.ONE.subtract(pct));
//			if (newRate.compareTo(BigDecimal.ZERO) <= 0) {
//				log("counter rate below zero " + newRate + " " + base.getCpair(), Level.SEVERE);
//				return null;
//			}
//			Amount newTotalPrice = RLOrder.amount(baseTotalPrice.value().multiply(newRate), baseQuantity.currency(), baseQuantity.issuer());
//			res = RLOrder.rateUnneeded(Direction.BUY, baseTotalPrice, newTotalPrice);
//		}
		return res;
	}

	public void counterPartial(List<RLOrder> oes) {
		oes.stream()
		.map(this::buildBotDirection)
		.filter(Optional::isPresent)
		.filter(optBcd  -> optBcd.get().botConfig.getStrategy() == Strategy.PARTIAL)
		.map(optBcd -> yuki(optBcd.get()))
		.forEach(this::onCounterReady);
	}

	private static class BotConfigDirection {
		boolean isDirectionMatch = true;
		BotConfig botConfig;
		RLOrder rlOrder;

		BotConfigDirection(Config config, RLOrder offer) {
			this.rlOrder = offer;
			botConfig = config.getBotConfigMap().get(offer.getCpair().getFw());
			if (botConfig == null) {
				botConfig = config.getBotConfigMap().get(offer.getCpair().getRv());
				isDirectionMatch = false;
			}		
		}
	}
	
	private Optional<BotConfigDirection> buildBotDirection(RLOrder origin){
		BotConfigDirection res = new BotConfigDirection(config, origin);
		if (res.botConfig == null) {
			return Optional.empty();
		}
		return Optional.of(res);
	}

	private RLOrder yuki(BotConfigDirection bcd) {
		RLOrder origin 	 	 		= bcd.rlOrder;
		BotConfig botConfig 	= bcd.botConfig;
		Amount oldQuantity 		= origin.getQuantity().multiply(new BigDecimal("-1"));
		Amount oldTotalPrice 	= origin.getTotalPrice().multiply(new BigDecimal("-1"));

		RLOrder res = null;
		if (bcd.isDirectionMatch) {
			BigDecimal newRate = origin.getRate().add(botConfig.getGridSpace());
			Amount newTotalPrice = RLOrder.amount(oldQuantity.value().multiply(newRate), oldTotalPrice.currency(), oldTotalPrice.issuer());
			res = RLOrder.rateUnneeded(Direction.SELL, oldQuantity, newTotalPrice);
		} 
		else {
			BigDecimal newRate = BigDecimal.ONE.divide(origin.getRate(), MathContext.DECIMAL32).subtract(botConfig.getGridSpace());
			if (newRate.compareTo(BigDecimal.ZERO) <= 0) {
				log("counter rate below zero " + newRate + " " + origin.getCpair(), Level.SEVERE);
				return null;
			}
			Amount newTotalPrice = RLOrder.amount(oldTotalPrice.value().multiply(newRate), oldQuantity.currency(), oldQuantity.issuer());
			res = RLOrder.rateUnneeded(Direction.BUY, oldTotalPrice, newTotalPrice);
		}
		return res;
	}

	@Override
	public void onCounterReady(RLOrder counter) {
		bus.send(new State.OnOrderReady(counter, Source.COUNTER));
	}
}
