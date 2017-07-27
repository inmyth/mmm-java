package com.mbcu.mmm.models.internal;

import com.ripple.core.coretypes.Amount;

public class NameIssuer {
	String currency;
	String issuer;

	private NameIssuer() {
		super();
	}
	
	public static NameIssuer from(Amount amount){
		NameIssuer res = new NameIssuer();
		res.currency = amount.currencyString();
		res.issuer = (!amount.isNative() || !amount.currencyString().equals("XRP")) ? res.issuer = amount.issuerString() : null;
		return res;
	}
	
	public static NameIssuer from(String string){
		NameIssuer res = new NameIssuer();
		String[] els = string.split("[.]");
		res.currency = els[0];
		res.issuer = els[1];
		return res;
		
		
	}
	
}
