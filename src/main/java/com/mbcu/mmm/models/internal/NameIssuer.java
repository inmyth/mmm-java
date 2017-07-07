package com.mbcu.mmm.models.internal;

import java.math.BigDecimal;

import com.mbcu.mmm.models.Asset;
import com.ripple.core.coretypes.AccountID;
import com.ripple.core.coretypes.Amount;
import com.ripple.core.coretypes.Currency;

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

	public Amount amountWith(BigDecimal value){
		if (currency.equals(Currency.XRP.toString())){
			return new Amount(value.setScale(6, BigDecimal.ROUND_HALF_UP));		

		}
		return new Amount(value, Currency.fromString(this.currency), AccountID.fromAddress(issuer));		
	}
	
}