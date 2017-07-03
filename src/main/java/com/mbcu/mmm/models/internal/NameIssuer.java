package com.mbcu.mmm.models.internal;

import com.google.gson.annotations.Expose;
import com.mbcu.mmm.models.Asset;

public class NameIssuer {
	
	String currency;
	String issuer;
	
	private NameIssuer(String currency, String issuer) {
		super();
		this.currency = currency;
		this.issuer = issuer;
	}
	
	public static NameIssuer fromDotForm(String part) throws IllegalArgumentException {	
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
		return new NameIssuer(currency, issuer);
	}

}