package com.mbcu.mmm.sequences.counters;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

import com.mbcu.mmm.models.internal.BotConfig;
import com.mbcu.mmm.models.internal.BotConfig.Strategy;
import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.models.internal.RLOrder;
import com.mbcu.mmm.models.internal.RLOrder.Direction;
import com.mbcu.mmm.rx.BusBase;
import com.mbcu.mmm.rx.RxBus;
import com.mbcu.mmm.rx.RxBusProvider;
import com.mbcu.mmm.sequences.Base;
import com.mbcu.mmm.sequences.Orderbook;
import com.mbcu.mmm.sequences.Orderbook.OnOrderConsumed;
import com.mbcu.mmm.sequences.state.State;
import com.mbcu.mmm.sequences.state.State.OnOrderReady.Source;
import com.mbcu.mmm.utils.MyLogger;
import com.mbcu.mmm.utils.MyUtils;
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
					if (base instanceof Orderbook.OnOrderConsumed){
						OnOrderConsumed event = (OnOrderConsumed) o;
						counter(event.origins);
					}
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
	public void counter(List<RLOrder> origins) {
		origins.stream()
		.map(o -> {return new FullCounterWrap(o, config);})
		.filter(wrap -> wrap.bcd.botConfig != null)
//		.filter(wrap -> wrap.bcd.botConfig.getStrategy() 		== Strategy.FULLFIXED 
//										|| wrap.bcd.botConfig.getStrategy() == Strategy.PPT )
		.map(this::buildCounter)
		.filter(Optional::isPresent)
		.map(Optional::get)
		.forEach(this::onCounterReady);
	}

//	public void counterPartial(List<RLOrder> origins) {
//		origins.stream()
//		.map(o -> {return new FullCounterWrap(o, config);})
//		.filter(wrap -> wrap.bcd.botConfig != null)
//		.map(this::buildCounter)
//		.filter(Optional::isPresent)
//		.map(Optional::get)
//		.forEach(this::onCounterReady);
//
//		
////		oes.stream()
////		.map(this::buildBotDirection)
////		.filter(Optional::isPresent)
////		.filter(optBcd  -> optBcd.get().botConfig.getStrategy() == Strategy.PARTIAL)
////		.map(optBcd -> yuki(optBcd.get()))
////		.forEach(this::onCounterReady);
//	}
	
	/*
	 * This part can be configured so instead of countering, the bot will replace
	 * taken order.
	 */
	public Optional<RLOrder> buildCounter(FullCounterWrap wrap) {
		RLOrder							 res = null;
		BotConfigDirection 	bcd  = wrap.bcd;
		RLOrder 			 	   	 src   = wrap.origin;
		
		switch (bcd.botConfig.getStrategy()){
			case PPT : 
				res = yukiPct(src, bcd.botConfig, bcd.isDirectionMatch);
				break;
			case FULLFIXED :
			case PARTIAL:
				res = yuki(src, bcd.botConfig, bcd.isDirectionMatch);
				break;
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
	
	private RLOrder yuki(RLOrder source, BotConfig botConfig, boolean isCounteringSell) {
		MathContext mc 		= MathContext.DECIMAL64;
//		BigDecimal mtp 		= botConfig.getGridSpace();
//		BigDecimal sqrt 		= MyUtils.bigSqrt(botConfig.getGridSpace());
//		Amount srcQuantity = source.getQuantity();
//		Amount srcTotalPrice = source.getTotalPrice();
//		BigDecimal newRate = null;
//		RLOrder res 				= null;
//
//		if (isCounteringSell){
//			newRate = source.getRate().multiply(mtp, MathContext.DECIMAL64);
//			Amount quantity = srcQuantity.divide(sqrt);
//			Amount totalPrice = RLOrder.amount(quantity.value().multiply(newRate), srcTotalPrice.currency(), srcTotalPrice.issuer());
//			res  = RLOrder.rateUnneeded(Direction.SELL, quantity, totalPrice);
//		} 
//		else {
//			newRate = BigDecimal.ONE.divide(source.getRate(), MathContext.DECIMAL64).divide(mtp, MathContext.DECIMAL64);
//			Amount quantity 	= srcTotalPrice.multiply(sqrt);
//			Amount totalPrice = RLOrder.amount(quantity.value().multiply(newRate, mc), srcQuantity.currency(), srcQuantity.issuer());
//			res  = RLOrder.rateUnneeded(Direction.BUY, quantity, totalPrice);
//		}
//		return res;
		
//		RLOrder origin 	 	 		= bcd.rlOrder;
//		Amount oldQuantity 		= origin.getQuantity().multiply(new BigDecimal("-1"));
//		Amount oldTotalPrice 	= origin.getTotalPrice().multiply(new BigDecimal("-1"));
		Amount srcQuantity 		= source.getQuantity();
		Amount srcTotalPrice   = source.getTotalPrice();
		BigDecimal newRate 		= null;
		RLOrder res 						= null;

		if (isCounteringSell) {
			newRate = source.getRate().add(botConfig.getGridSpace());
			Amount totalPrice = RLOrder.amount(srcQuantity.value().multiply(newRate), srcTotalPrice.currency(), srcTotalPrice.issuer());
			res = RLOrder.rateUnneeded(Direction.SELL, srcQuantity, totalPrice);
		} 
		else {
			System.out.println(source.getRate().toPlainString());
			newRate = BigDecimal.ONE.divide(source.getRate(), mc).subtract(botConfig.getGridSpace());
			if (newRate.compareTo(BigDecimal.ZERO) <= 0) {
				log("counter rate below zero " + newRate + " " + source.getCpair(), Level.SEVERE);
				return null;
			}
			Amount totalPrice = RLOrder.amount(srcTotalPrice.value().multiply(newRate), srcQuantity.currency(), srcQuantity.issuer());
			res = RLOrder.rateUnneeded(Direction.BUY, srcTotalPrice, totalPrice);
		}
		return res;
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
	private RLOrder yukiPct(RLOrder source, BotConfig botConfig, boolean isCounteringSell) {
		MathContext mc 		= MathContext.DECIMAL64;
		BigDecimal mtp 		= botConfig.getGridSpace();
		BigDecimal sqrt 		= MyUtils.bigSqrt(botConfig.getGridSpace());
		Amount srcQuantity = source.getQuantity();
		Amount srcTotalPrice = source.getTotalPrice();
		BigDecimal newRate = null;
		RLOrder res 				= null;

		if (!isCounteringSell){
			newRate = source.getRate().multiply(mtp, MathContext.DECIMAL64);
			Amount quantity = srcQuantity.divide(sqrt);
			Amount totalPrice = RLOrder.amount(quantity.value().multiply(newRate), srcTotalPrice.currency(), srcTotalPrice.issuer());
			res  = RLOrder.rateUnneeded(Direction.SELL, quantity, totalPrice);
		} 
		else {
			newRate = BigDecimal.ONE.divide(source.getRate(), MathContext.DECIMAL64).divide(mtp, MathContext.DECIMAL64);
			Amount quantity 	= srcTotalPrice.multiply(sqrt);
			Amount totalPrice = RLOrder.amount(quantity.value().multiply(newRate, mc), srcQuantity.currency(), srcQuantity.issuer());
			res  = RLOrder.rateUnneeded(Direction.BUY, quantity, totalPrice);
		}
		return res;
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



	@Override
	public void onCounterReady(RLOrder counter) {
		bus.send(new State.OnOrderReady(counter, Source.COUNTER));
	}
}
