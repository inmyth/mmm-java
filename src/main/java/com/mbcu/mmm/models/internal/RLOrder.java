package com.mbcu.mmm.models.internal;

import java.math.BigDecimal;
import java.math.MathContext;

import com.google.gson.annotations.Expose;
import com.mbcu.mmm.models.ripple.tx.Order;
import com.ripple.core.coretypes.AccountID;
import com.ripple.core.coretypes.Amount;
import com.ripple.core.coretypes.Currency;
import com.ripple.core.coretypes.STObject;
import com.ripple.core.coretypes.hash.Hash256;
import com.ripple.core.fields.STObjectField;
import com.ripple.core.types.known.sle.entries.Offer;

public final class RLOrder {

	private static final String XRP = "XRP";

	public enum Direction {
		BUY("buy"), SELL("sell");

		private String text;

		Direction(String text) {
			this.text = text;
		}

		public String text() {
			return text;
		}
	}

	private final String direction;
	private final RLAmount quantity;
	private final RLAmount totalPrice;
	private final boolean passive;
	private final boolean fillOrKill;

	private final BigDecimal ask;
	transient private final String pair;

	public RLOrder(Direction direction, RLAmount quantity, RLAmount totalPrice, BigDecimal ask, String pair) {
		super();
		this.direction = direction.text;
		this.quantity = quantity;
		this.totalPrice = totalPrice;
		this.passive = false;
		this.fillOrKill = false;
		this.ask = ask;
		this.pair = pair;
	}

	public String getDirection() {
		return direction;
	}

	public RLAmount getQuantity() {
		return quantity;
	}

	public RLAmount getTotalPrice() {
		return totalPrice;
	}

	public boolean isPassive() {
		return passive;
	}

	public boolean isFillOrKill() {
		return fillOrKill;
	}

	public String getPair() {
		return pair;
	}

	public BigDecimal getAsk() {
		return ask;
	}

	public static RLOrder fromOfferCreated(Offer offer) {		
		BigDecimal ask = askFrom(offer);
		Amount pays = offer.takerPays();
		Amount gets = offer.takerGets();
		RLAmount rlRealGot = RLAmount.newInstance(pays);
		RLAmount rlRealPaid = RLAmount.newInstance(gets);
		String pair = buildPair(gets.currencyString(), gets.issuerString(), pays.currencyString(), pays.issuerString());
		RLOrder res = new RLOrder(Direction.BUY, rlRealGot, rlRealPaid, ask, pair);
		return res;
	}

	public static RLOrder fromOfferExecuted(Offer offer) {
		// All OE's paid and got are negative and need to be reversed
		BigDecimal ask = askFrom(offer);
		STObject executed = offer.executed(offer.get(STObject.FinalFields));
		Amount paid = executed.get(Amount.TakerPays);
		Amount got = executed.get(Amount.TakerGets);	
		RLAmount rlGot = RLAmount.newInstance(new Amount(got.value().multiply(new BigDecimal(-1)), got.currency(), got.issuer()));
		RLAmount rlPaid = RLAmount.newInstance(new Amount(paid.value().multiply(new BigDecimal(-1)), paid.currency(), paid.issuer()));
		String pair = buildPair(paid, got);

		RLOrder res = new RLOrder(Direction.BUY, rlGot, rlPaid, ask, pair);
		return res;
	}

	public static RLOrder fromAutobridge(Offer oe, Offer bridge){
		Direction direction;
		String base, baseIssuer, quote, quoteIssuer;
		STObject oeExecuted = oe.executed(oe.get(STObject.FinalFields));
		Amount oePaid = oeExecuted.get(Amount.TakerPays);	
		Amount oeGot = oeExecuted.get(Amount.TakerGets);
		
		STObject bridgeExecuted = bridge.executed(bridge.get(STObject.FinalFields));
		Amount bridgePaid = bridgeExecuted.get(Amount.TakerPays);	
		Amount bridgeGot = bridgeExecuted.get(Amount.TakerGets);
		Amount quantity= null, totalPrice = null;
		String pair;
		BigDecimal ask  = oe.directoryAskQuality().multiply(bridge.directoryAskQuality());

		if (bridgeGot.currencyString().equals(Currency.XRP.toString())){
			direction = Direction.BUY;
			base = oeGot.currencyString();
			baseIssuer = oeGot.issuerString();
			quote = bridgePaid.currencyString();
			quoteIssuer = bridgePaid.issuerString();
			quantity =  new Amount(oeGot.value().multiply(new BigDecimal(-1)), oeGot.currency(), oeGot.issuer());
			BigDecimal totalPriceValue = oePaid.value().multiply(new BigDecimal(-1)).multiply(bridge.directoryAskQuality(), MathContext.DECIMAL64);
			totalPrice = new Amount(totalPriceValue, bridgePaid.currency(), bridgePaid.issuer());			
		}else{
			direction = Direction.SELL;
			base = oePaid.currencyString();
			baseIssuer = oePaid.issuerString();
			quote = bridgeGot.currencyString();
			quoteIssuer = bridgeGot.issuerString();
			totalPrice = new Amount(oePaid.value().multiply(new BigDecimal(-1)), oePaid.currency(), oePaid.issuer());
			BigDecimal quantityValue = oeGot.value().multiply(new BigDecimal(-1)).divide(bridge.directoryAskQuality(), MathContext.DECIMAL64);
			quantity = new Amount(quantityValue, bridgeGot.currency(), bridgeGot.issuer());
		}
		pair = buildPair(base, baseIssuer, quote, quoteIssuer);
		return new RLOrder(direction, RLAmount.newInstance(quantity), RLAmount.newInstance(totalPrice), ask, pair);	
	}
	
	public static String buildPair(String base, String baseIssuer, String quote, String quoteIssuer) {
		StringBuffer res = new StringBuffer(base);
		if (!base.equals(XRP)) {
			res.append(".");
			res.append(baseIssuer);
		}
		res.append("/");
		res.append(quote);
		if (!quote.equals(XRP)) {
			res.append(".");
			res.append(quoteIssuer);
		}

		return res.toString();
	}
	
	public static String buildPair(Offer offer){
		STObject executed = offer.executed(offer.get(STObject.FinalFields));
		Amount paid = executed.get(Amount.TakerPays);
		Amount got = executed.get(Amount.TakerGets);
		String pair = RLOrder.buildPair(paid, got);
		return pair;
	}
	
	public static String buildPair(Amount pay, Amount get){
		return buildPair(get.currencyString(), get.issuerString(), pay.currencyString(), pay.issuerString());
	}

	private static BigDecimal askFrom(Offer offer) {
		return offer.directoryAskQuality().stripTrailingZeros();
	}

}
