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
	
	@Override
	public boolean equals(Object o) {
    if (o == null) {
      return false;
    }    
    if (!(o instanceof NameIssuer)){
    	return false;
    }
    NameIssuer test = (NameIssuer) o;
    if (test.currency.equals(this.currency) && test.issuer.equals(this.issuer)){
    	return true;
    }
    return false;
	}

	@Override
	public int hashCode() {
    int result = 17;
    result = 31 * result + currency.hashCode();
    result = 31 * result + issuer.hashCode();
    return result;
	}
	
}
