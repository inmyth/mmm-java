package com.mbcu.mmm.models.internal;

import java.util.ArrayList;
import java.util.HashMap;

import com.mbcu.mmm.models.Asset;

public class BotConfig {

	String pair;
	float startMiddlePrice;
	float gridSpace;
	int buyGridLevels, sellGridLevels;
	float buyOrderQuantity, sellOrderQuantity;
	transient Currency base;
	transient Currency quote;

	public static HashMap<String, BotConfig> buildMap(ArrayList<BotConfig> bots)  {
		HashMap<String, BotConfig> res = new HashMap<>();
		for (BotConfig bot : bots) {
			String[] pair = buildBaseAndQuote(bot.getPair());
			Currency base = buildCurrencyAndIssuer(pair[0]);
			Currency quote = buildCurrencyAndIssuer(pair[1]);
			bot.setBase(base);
			bot.setQuote(quote);
			res.put(bot.getPair(), bot);
		}
		return res;
	}
	
	

	public static Currency buildCurrencyAndIssuer(String part) throws IllegalArgumentException {
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
		return new Currency(currency, issuer);
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

	public Currency getBase() {
		return base;
	}

	public void setBase(Currency base) {
		this.base = base;
	}

	public Currency getQuote() {
		return quote;
	}

	public void setQuote(Currency quote) {
		this.quote = quote;
	}

}
