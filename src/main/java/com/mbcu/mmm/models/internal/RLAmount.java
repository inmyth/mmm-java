package com.mbcu.mmm.models.internal;

import java.math.BigDecimal;
import java.math.MathContext;

import com.mbcu.mmm.models.Base;
import com.ripple.core.coretypes.AccountID;
import com.ripple.core.coretypes.Amount;
import com.ripple.core.coretypes.Currency;

public final class RLAmount extends Base {
	
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
	
	public RLAmount(String currency, String counterparty, BigDecimal value, Amount amount) {
		this(currency, counterparty, value.toPlainString(), amount);
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
	
	public BigDecimal getBigDecimalValue(){		
		BigDecimal res = new BigDecimal(this.value);
		return res;	
	}
	
	public BigDecimal getValueXMinOne(){
		BigDecimal res = new BigDecimal(this.value);
		return res.multiply(new BigDecimal(-1));
	}

	@Override
	public String stringify(){
		return super.stringify(this);	
	}
	
	
	public static RLAmount newInstance(Amount amount){
		String currency = amount.currencyString();
		String issuer = amount.issuerString();
		String value = amount.value().toPlainString();	
		return new RLAmount(currency, issuer, value, amount);		
	}
	

	
	public Amount amount(){
		Amount res;
		if (currency.equals(Currency.XRP.toString())){
			 res = new Amount(new BigDecimal(this.value, MathContext.DECIMAL64).setScale(6, BigDecimal.ROUND_DOWN));
		}else{
			 res = new Amount(new BigDecimal(this.value, MathContext.DECIMAL64).setScale(16, BigDecimal.ROUND_DOWN), Currency.fromString(this.currency), AccountID.fromString(this.counterparty));
		}
		return res;	
	}
	
	
}
