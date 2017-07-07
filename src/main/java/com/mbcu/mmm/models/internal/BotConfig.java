package com.mbcu.mmm.models.internal;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;
import java.util.stream.IntStream;

import com.mbcu.mmm.models.internal.RLOrder.Direction;
import com.ripple.core.coretypes.AccountID;
import com.ripple.core.coretypes.Amount;
import com.ripple.core.coretypes.Currency;

public class BotConfig {

	String pair;
	float startMiddlePrice;
	float gridSpace;
	int buyGridLevels;
	int sellGridLevels;
	float buyOrderQuantity;
	float sellOrderQuantity;	
	transient NameIssuer base;
	transient NameIssuer quote;

	public static HashMap<String, BotConfig> buildMap(ArrayList<BotConfig> bots)  {
		HashMap<String, BotConfig> res = new HashMap<>();
		for (BotConfig bot : bots) {
			String[] pair = buildBaseAndQuote(bot.getPair());
			NameIssuer base = NameIssuer.fromDotForm(pair[0]);
			NameIssuer quote = NameIssuer.fromDotForm(pair[1]);
			bot.setBase(base);
			bot.setQuote(quote);
			res.put(bot.getPair(), bot);
		}
		return res;
	}
	
	public static String[] buildBaseAndQuote(String pair) {
		return pair.split("[/]");
	}

	public String getPair() {
		return pair;
	}

	public void setPair(String pair) {
		this.pair = pair;
	}

	public float getStartMiddlePrice() {
		return startMiddlePrice;
	}

	public void setStartMiddlePrice(float startMiddlePrice) {
		this.startMiddlePrice = startMiddlePrice;
	}

	public float getGridSpace() {
		return gridSpace;
	}

	public void setGridSpace(float gridSpace) {
		this.gridSpace = gridSpace;
	}

	public int getBuyGridLevels() {
		return buyGridLevels;
	}

	public void setBuyGridLevels(int buyGridLevels) {
		this.buyGridLevels = buyGridLevels;
	}

	public int getSellGridLevels() {
		return sellGridLevels;
	}

	public void setSellGridLevels(int sellGridLevels) {
		this.sellGridLevels = sellGridLevels;
	}

	public float getBuyOrderQuantity() {
		return buyOrderQuantity;
	}

	public void setBuyOrderQuantity(float buyOrderQuantity) {
		this.buyOrderQuantity = buyOrderQuantity;
	}

	public float getSellOrderQuantity() {
		return sellOrderQuantity;
	}

	public void setSellOrderQuantity(float sellOrderQuantity) {
		this.sellOrderQuantity = sellOrderQuantity;
	}

	public NameIssuer getBase() {
		return base;
	}

	public void setBase(NameIssuer base) {
		this.base = base;
	}

	public NameIssuer getQuote() {
		return quote;
	}

	public void setQuote(NameIssuer quote) {
		this.quote = quote;
	}

	private Queue<Integer> getLevels(int max){
		Queue<Integer> res = new LinkedList<>();
		IntStream.range(1, max).forEach(a -> {res.add(a);});
		return res;
	}

	public Amount getBuyQuantityAmount(){
		return base.amountWith(new BigDecimal(buyOrderQuantity));
	}
	
	public Amount getSellQuantityAmount(){
		return base.amountWith(new BigDecimal(sellOrderQuantity));
	}
	
	public ArrayList<RLOrder> buildSeed(){
		ArrayList<RLOrder> res = new ArrayList<>();
		BigDecimal middlePrice = new BigDecimal(this.startMiddlePrice);
		BigDecimal margin = new BigDecimal(this.gridSpace);
		Queue<Integer> buyLevels = getLevels(this.buyGridLevels);
		Queue<Integer> sellLevels = getLevels(this.sellGridLevels);
	
		
		while (true){
			if (buyLevels.isEmpty() && sellLevels.isEmpty()){
				break;
			}
			if (!buyLevels.isEmpty()){
				Amount quantity = getBuyQuantityAmount();				
				BigDecimal rate = middlePrice.subtract(margin.multiply(new BigDecimal(buyLevels.remove()), MathContext.DECIMAL64));
				if (rate.compareTo(BigDecimal.ZERO) <= 0){
					buyLevels.clear();				
				}else{
					BigDecimal totalPriceValue = quantity.value().multiply(rate, MathContext.DECIMAL64);
					Amount totalPrice = new Amount(totalPriceValue, Currency.fromString(quote.currency), AccountID.fromAddress(quote.issuer));
					RLOrder buy = RLOrder.basic(Direction.BUY, RLAmount.newInstance(quantity), RLAmount.newInstance(totalPrice));
					res.add(buy);
				}
			}
			if (!sellLevels.isEmpty()){
				Amount quantity = getSellQuantityAmount();
				BigDecimal rate = middlePrice.add(margin.multiply(new BigDecimal(sellLevels.remove()), MathContext.DECIMAL64));
				BigDecimal totalPriceValue = quantity.value().multiply(rate, MathContext.DECIMAL64);
				Amount totalPrice = new Amount(totalPriceValue, Currency.fromString(quote.currency), AccountID.fromAddress(quote.issuer));
				RLOrder sell = RLOrder.basic(Direction.SELL, RLAmount.newInstance(quantity), RLAmount.newInstance(totalPrice));
				res.add(sell);	
			}			
		}	
		return res;
	}

}
