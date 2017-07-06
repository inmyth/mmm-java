package com.mbcu.mmm.sequences.counters;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;

import com.mbcu.mmm.models.internal.BotConfig;
import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.models.internal.RLAmount;
import com.mbcu.mmm.models.internal.RLOrder;
import com.mbcu.mmm.models.internal.RLOrder.Direction;
import com.mbcu.mmm.rx.RxBus;
import com.mbcu.mmm.rx.RxBusProvider;
import com.mbcu.mmm.sequences.Base;
import com.mbcu.mmm.sequences.Common;
import com.mbcu.mmm.sequences.Common.OnOfferExecuted;
import com.mbcu.mmm.utils.MyLogger;

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
		boolean isReversed = false;
		BotConfig botConfig = config.getBotConfigMap().get(oe.getPair());
		if (botConfig == null){
			botConfig = config.getBotConfigMap().get(oe.getReversePair());		
			isReversed = true;
		}
		if (botConfig == null){
			return;
		}
		
		RLOrder counter = buildCounter(botConfig, oe, isReversed);
		onCounterReady(counter);			
		});	
	}
	
	private RLOrder buildCounter(BotConfig botConfig, RLOrder origin, boolean isReversed){
		RLAmount oldQuantity = origin.getQuantity();
		RLAmount oldTotalPrice = origin.getTotalPrice();
		RLAmount newQuantity =  new RLAmount(oldQuantity.getCurrency(), oldQuantity.getCounterparty(), oldQuantity.getValueXMinOne(), null);
		BigDecimal oldAsk = origin.getAsk();
		BigDecimal botRate = new BigDecimal(botConfig.getGridSpace());
		BigDecimal newAsk = null;
		BigDecimal newTotalPriceValue = null;	

		if (!isReversed){
			newAsk = oldAsk.min(botRate);
		}else{
			newAsk = oldAsk.min(BigDecimal.ONE.divide(botRate, MathContext.DECIMAL64));
		}
		
		newTotalPriceValue = newAsk.multiply(newQuantity.getBigDecimalValue());
		RLAmount newTotalPrice = new RLAmount(oldTotalPrice.getCurrency(), oldTotalPrice.getCounterparty(), newTotalPriceValue, null);
		
		RLOrder res = RLOrder.defaultCounter(Direction.BUY, newQuantity, newTotalPrice);
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
	 * 
	 */

}
