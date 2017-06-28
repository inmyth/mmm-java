package com.mbcu.mmm.models.ripple.tx;

import org.parceler.Parcel;

@Parcel
public class Amount {
	
	String currency;
	transient String counterparty;
	String value;
	
	
	public Amount(String currency, String counterparty, String value) {
		super();
		this.currency = currency;
		this.counterparty = counterparty;
		this.value = value;
	}
	
	

}
