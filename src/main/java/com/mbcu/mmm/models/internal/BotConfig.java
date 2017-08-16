package com.mbcu.mmm.models.internal;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.IntStream;

import org.parceler.Parcel;

import com.mbcu.mmm.models.Asset;
import com.mbcu.mmm.models.request.BookOffers;
import com.ripple.core.coretypes.AccountID;
import com.ripple.core.coretypes.Amount;
import com.ripple.core.coretypes.Currency;

@Parcel
public class BotConfig {

	String pair;
	float startMiddlePrice;
	String gridSpace;
	int buyGridLevels;
	int sellGridLevels;
	String buyOrderQuantity;
	String sellOrderQuantity;	
	boolean isPartialCounter;

	transient Amount base;
	transient Amount quote;
	transient BigDecimal totalBuyQty, totalSelQty; 
	transient List<String> orderbookReqs;

	public static HashMap<String, BotConfig> buildMap(Credentials credentials, ArrayList<BotConfig> bots)  {
		HashMap<String, BotConfig> res = new HashMap<>();
		for (BotConfig bot : bots) {
			String[] pair 				= buildBaseAndQuote(bot.getPair());
			bot.base 							= fromDotForm(pair[0]);
			bot.quote 						= fromDotForm(pair[1]);	
			bot.orderbookReqs 		= BookOffers.buildRequest(credentials.address, bot);
			bot.totalBuyQty 			= buildTotalQuantity(bot.buyGridLevels, bot.buyOrderQuantity);
			bot.totalSelQty				= buildTotalQuantity(bot.sellGridLevels, bot.sellOrderQuantity);
			res.put(bot.getPair(), bot);
		}
		return res;
	}
	
	private static BigDecimal buildTotalQuantity(int gridLevels, String orderQuantity){
		BigDecimal a = new BigDecimal(gridLevels);
		BigDecimal b = new BigDecimal(orderQuantity);
		return a.multiply(b, MathContext.DECIMAL64);
	}
	
	public static Amount fromDotForm(String part) throws IllegalArgumentException {
		String currency = null;
		String issuer = null;
		String[] b = part.split("[.]");
		if (!b[0].equals(Asset.Currency.XRP.text()) && b.length != 2) {
			throw new IllegalArgumentException("currency pair not formatted in base.issuer/quote.issuer");
		} else {
			currency = b[0];
			if (b.length == 2) {
				issuer = b[1];
			}
		}		
		return issuer == null ? new Amount(new BigDecimal("0")) : new Amount(Currency.fromString(currency), AccountID.fromAddress(issuer));		
	}
	
	public static String[] buildBaseAndQuote(String pair) {
		return pair.split("[/]");
	}

	public String getPair() {
		return pair;
	}
	
	public String getReversePair(){
		String[] els = pair.split("[/]");
		StringBuffer res = new StringBuffer(els[1]);
		res.append("/");
		res.append(els[0]);
		return res.toString();
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

	public BigDecimal getGridSpace() {
		return new BigDecimal(this.gridSpace);
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


	public BigDecimal getBuyOrderQuantity() {
		return new BigDecimal(buyOrderQuantity);
	}

	public BigDecimal getSellOrderQuantity() {
		return new BigDecimal(sellOrderQuantity);
	}
	
	public List<String> getOrderbookRequests() {
		return orderbookReqs;
	}
	
	public boolean isPartialCounter() {
		return isPartialCounter;
	}
	
	public Amount getBase() {
		return base;
	}
	
	public Amount getQuote() {
		return quote;
	}

	public BigDecimal getTotalBuyQty() {
		return totalBuyQty;
	}
	
	public BigDecimal getTotalSelQty() {
		return totalSelQty;
	}
	
	public Queue<Integer> getLevels(int max){
		Queue<Integer> res = new LinkedList<>();
		IntStream.range(1, max + 1).forEach(a -> {res.add(a);});
		return res;
	}

}
