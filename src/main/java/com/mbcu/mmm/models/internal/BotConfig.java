package com.mbcu.mmm.models.internal;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.mbcu.mmm.models.Asset;
import com.mbcu.mmm.models.request.BookOffers;
import com.mbcu.mmm.utils.MyUtils;
import com.ripple.core.coretypes.AccountID;
import com.ripple.core.coretypes.Amount;
import com.ripple.core.coretypes.Currency;


public class BotConfig {

	private String pair;
	private String startMiddlePrice;
	private String gridSpace;
	private int buyGridLevels;
	private int sellGridLevels;
	private String buyOrderQuantity;
	private String sellOrderQuantity;
	private String strategy;
	
	transient Amount base;
	transient Amount quote;
	transient BigDecimal totalBuyQty, totalSelQty;
	transient List<String> orderbookReqs;
	
	
	public enum Strategy {
		PARTIAL,	
		FULLFIXED,	
		FULLRATEPCT,	
		FULLRATESEEDPCT;	
	}

	public static HashMap<String, BotConfig> buildMap(Credentials credentials, ArrayList<BotConfig> bots) throws IOException {
		HashMap<String, BotConfig> res = new HashMap<>();
		for (BotConfig bot : bots) {
			String[] pair = buildBaseAndQuote(bot.getPair());
			bot.base = fromDotForm(pair[0]);
			bot.quote = fromDotForm(pair[1]);
			bot.orderbookReqs = BookOffers.buildRequest(credentials.address, bot);
			bot.totalBuyQty = buildTotalQuantity(bot.buyGridLevels, bot.buyOrderQuantity);
			bot.totalSelQty = buildTotalQuantity(bot.sellGridLevels, bot.sellOrderQuantity);

			res.put(bot.getPair(), bot);
			if (!MyUtils.isInEnum(bot.strategy, Strategy.class)){
				throw new IOException(String.format("Strategy \"%s\" is not recognized", bot.strategy));
			}		
			if (bot.getStrategy().equals(BotConfig.Strategy.FULLRATEPCT) || bot.getStrategy().equals(BotConfig.Strategy.FULLRATESEEDPCT)) {
				BigDecimal pct = new BigDecimal(bot.gridSpace);
				pct = pct.divide(new BigDecimal("100"), MathContext.DECIMAL64);
				bot.gridSpace = pct.toPlainString();
			}
		}
		return res;
	}
	
	private static BigDecimal buildTotalQuantity(int gridLevels, String orderQuantity) {
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
		return issuer == null ? new Amount(new BigDecimal("0"))
				: new Amount(Currency.fromString(currency), AccountID.fromAddress(issuer));
	}

	public static String[] buildBaseAndQuote(String pair) {
		return pair.split("[/]");
	}

	public String getPair() {
		return pair;
	}

	public String getReversePair() {
		String[] els = pair.split("[/]");
		StringBuffer res = new StringBuffer(els[1]);
		res.append("/");
		res.append(els[0]);
		return res.toString();
	}

	public void setPair(String pair) {
		this.pair = pair;
	}

	public BigDecimal getStartMiddlePrice() {
		return new BigDecimal(startMiddlePrice);
	}

	public void setStartMiddlePrice(String startMiddlePrice) {
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

	
	public Strategy getStrategy() {
		return Strategy.valueOf(strategy.toUpperCase());
	}

}
