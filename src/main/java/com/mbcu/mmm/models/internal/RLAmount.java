package com.mbcu.mmm.models.internal;

import com.ripple.core.coretypes.Amount;

public final class RLAmount {
	
	private final String currency;
	private final String counterparty;
	private final String value;
	transient  private final Amount amount;

	public RLAmount(String currency, String counterparty, String value, Amount amount) {
		super();
		this.currency = currency;
		this.counterparty = counterparty;
		this.value = value;
		this.amount = amount;
	}

	public String getCurrency() {
		return currency;
	}

	public String getCounterparty() {
		return counterparty;
	}

	public String getValue() {
		return value;
	}

	public Amount getAmount() {
		return amount;
	}
	
	

	
	public static RLAmount newInstance(Amount amount){
		String currency = amount.currencyString();
		String issuer = amount.issuerString();
		String value = amount.value().toPlainString();	
		if (value.startsWith("-1")){
			throw new IllegalArgumentException("Amount value cannot be negative");
		}
		return new RLAmount(currency, issuer, value, amount);		
	}
}
