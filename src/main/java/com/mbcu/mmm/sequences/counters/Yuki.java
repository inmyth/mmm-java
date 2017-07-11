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

import io.reactivex.schedulers.Schedulers;

public class Yuki extends Base implements Counter {
	private RxBus bus = RxBusProvider.getInstance();
	private Config config;
	int count;

	public Yuki(Config config) {
		super(MyLogger.getLogger(Yuki.class.getName()), config);
		this.config = config;
		
		bus.toObservable()
		.subscribeOn(Schedulers.newThread())
		.subscribe(o -> {
			if (o instanceof Common.OnOfferExecuted) {
				OnOfferExecuted event = (OnOfferExecuted) o;
				counter(event.oes);
			} 
		});

	}

	public static Yuki newInstance(Config config) {
		Yuki counter = new Yuki(config);
		return counter;
	}

	private void counter(List<RLOrder> oes){
	oes.forEach(oe -> {
		boolean isDirectionMatch = true;
		BotConfig botConfig = config.getBotConfigMap().get(oe.getPair());
		if (botConfig == null){
			botConfig = config.getBotConfigMap().get(oe.getReversePair());		
			isDirectionMatch = false;
		}
		if (botConfig == null){
			return;
		}		
		RLOrder counter = buildCounter(botConfig, oe, isDirectionMatch);
		onCounterReady(counter);			
		});	
	}
	
	private RLOrder buildCounter(BotConfig botConfig, RLOrder origin, boolean isDirectionMatch){
//		RLAmount oldQuantity = origin.getQuantity();
//		RLAmount oldTotalPrice = origin.getTotalPrice();
//		RLAmount newQuantity =  RLAmount.newInstance(oldQuantity.amount().multiply(new BigDecimal("-1")));
//		BigDecimal oldAsk = origin.getAsk();
//		BigDecimal botRate = new BigDecimal(botConfig.getGridSpace());
//		BigDecimal newAsk = null;
//		BigDecimal newTotalPriceValue = null
//				
		
	Amount oldQuantity = origin.getQuantity();
	Amount oldTotalPrice = origin.getTotalPrice();
		RLOrder res = null;
		if (isDirectionMatch){
			Amount newQuantity = origin.getQuantity().multiply(new BigDecimal("-1"));
			BigDecimal newRate = origin.getAsk().add(botConfig.getGridSpace());
			Amount newTotalPrice = RLOrder.amount(newQuantity.value().multiply(newRate), oldTotalPrice.currency(), oldTotalPrice.issuer());
			res = RLOrder.basic(Direction.SELL, newQuantity, newTotalPrice);				
		}else{
			Amount newTotalPrice =  origin.getTotalPrice().multiply(new BigDecimal("-1"));
			BigDecimal newRate = BigDecimal.ONE.divide(origin.getAsk(), MathContext.DECIMAL128).subtract(botConfig.getGridSpace());
			if (newRate.compareTo(BigDecimal.ZERO) <= 0){
				log("counter rate below zero " + newRate + " " + origin.getDirection(), Level.SEVERE);
				return null;			
			}
//			value precision of 40 is greater than maximum iou precision of 16
			Amount newQuantity = RLOrder.amount( newTotalPrice.value().multiply(newRate), oldQuantity.currency(), oldQuantity.issuer());
		  res = RLOrder.basic(Direction.BUY, newQuantity, newTotalPrice);
		}
		return res;
	}


	
	@Override
	public void onCounterReady(RLOrder counter) {
		bus.send(new Counter.CounterReady(counter));
	}

	/*
	 * { "direction": "buy", "quantity": { "currency": "JPY", "counterparty":
	 * "rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS", "value": "-27" }, "totalPrice": {
	 * "currency": "XRP", "counterparty": "rrrrrrrrrrrrrrrrrrrrrhoLvTp", "value":
	 * "-0.962566" }, "passive": false, "fillOrKill": false, "ask":
	 * 0.035650623885918, "pair": "JPY.rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS/XRP" }
	 */

}
