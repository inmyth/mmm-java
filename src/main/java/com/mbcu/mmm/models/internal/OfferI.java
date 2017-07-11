package com.mbcu.mmm.models.internal;

import java.math.BigDecimal;
import java.math.MathContext;

import com.mbcu.mmm.models.Base;
import com.ripple.core.coretypes.Amount;
import com.ripple.core.coretypes.Currency;
import com.ripple.core.coretypes.STObject;
import com.ripple.core.types.known.sle.entries.Offer;

public class OfferI extends Base {

	public final Amount iPay;
	public final Amount iGet;
	public final BigDecimal ask;
	public final String currencyPair;
	
	
	public OfferI(Amount iPay, Amount iGet, BigDecimal ask, String currencyPair) {
		super();
		this.iPay = iPay;
		this.iGet = iGet;
		this.ask = ask;
		this.currencyPair = currencyPair;
	}

	
	public static OfferI fromMinusQuantity(Amount quantity, BigDecimal ask){
		quantity = quantity.multiply(new BigDecimal("-1"));
		
		Amount totalPrice = quantity
	}
	
	public static OfferI normalizeOE(Offer oe){
		STObject executed = oe.executed(oe.get(STObject.FinalFields));
		Amount takerPays = executed.get(Amount.TakerGets);
		Amount takerGets = executed.get(Amount.TakerPays);
		BigDecimal ask = takerPays.value().divide(takerGets.value(), MathContext.DECIMAL64);
		String pair = pairFrom(takerGets, takerPays);		
		return new OfferI(takerPays, takerGets, ask, pair);
	}
	
	
	public static String pairFrom(Offer offer){
		STObject executed = offer.executed(offer.get(STObject.FinalFields));
		return pairFrom(executed.get(Amount.TakerGets), executed.get(Amount.TakerPays));
	}
	
	public static String pairFrom(Amount get, Amount pay){
		return pairFrom(get.currencyString(), get.issuerString(), pay.currencyString(), pay.issuerString());
	}
	

	
	
	public static String pairFrom(String base, String baseIssuer, String quote, String quoteIssuer) {
	StringBuffer res = new StringBuffer(base);
	if (!base.equals(Currency.XRP.toString())) {
		res.append(".");
		res.append(baseIssuer);
	}
	res.append("/");
	res.append(quote);
	if (!quote.equals(Currency.XRP.toString())) {
		res.append(".");
		res.append(quoteIssuer);
	}
	
	return res.toString();
	}
	
	@Override
	public String stringify() {
		return stringify(this);
	}

}
