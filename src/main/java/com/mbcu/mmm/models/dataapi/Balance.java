package com.mbcu.mmm.models.dataapi;

import java.math.BigDecimal;

import com.mbcu.mmm.models.Asset.Currency;
import com.mbcu.mmm.models.internal.NameIssuer;
import com.ripple.core.coretypes.AccountID;
import com.ripple.core.coretypes.Amount;

public class Balance {
	
	String currency;
	String value;
	String counterparty;
	
	
	
	public String getCurrency() {
		return currency;
	}
	public void setCurrency(String currency) {
		this.currency = currency;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public String getCounterparty() {
		return counterparty;
	}
	public void setCounterparty(String counterparty) {
		this.counterparty = counterparty;
	}

	public Amount toAmount(){
		if (currency.equals(Currency.XRP.toString())){
			return new Amount(new BigDecimal(value));
		}
		return  new Amount(new BigDecimal(value), com.ripple.core.coretypes.Currency.fromString(currency) , AccountID.fromAddress(counterparty));
	}
	
	public NameIssuer toSignature(){
		Amount a; 
		if (currency.equals(Currency.XRP.toString())){
			a =  Amount.ONE_XRP;
		} else {
			a = new Amount(com.ripple.core.coretypes.Currency.fromString(currency) , AccountID.fromAddress(counterparty));
		}
		return NameIssuer.from(a);
	}
	
	@Override
	public String toString() {
		return toAmount().stringRepr();
	}
 
}
