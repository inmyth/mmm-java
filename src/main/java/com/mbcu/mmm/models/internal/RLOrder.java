package com.mbcu.mmm.models.internal;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.mbcu.mmm.models.Base;
import com.ripple.core.coretypes.AccountID;
import com.ripple.core.coretypes.Amount;
import com.ripple.core.coretypes.Currency;
import com.ripple.core.coretypes.STObject;
import com.ripple.core.coretypes.uint.UInt32;
import com.ripple.core.types.known.sle.entries.Offer;
import com.ripple.core.types.known.tx.Transaction;
import com.ripple.core.types.known.tx.signed.SignedTransaction;
import com.ripple.core.types.known.tx.txns.OfferCreate;

public final class RLOrder extends Base{

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
    private final String pair;

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

	public String getReversePair(){
		String[] p = this.pair.split("[/]");
		StringBuffer res = new StringBuffer(p[1]);
		res.append("/");
		res.append(p[0]);
		return res.toString();	
	}
	
	public BigDecimal getAsk() {
		return ask;
	}
	
	public static RLOrder fromOfferCreate(Transaction txn){
		Amount gets = txn.get(Amount.TakerGets);
		Amount pays = txn.get(Amount.TakerPays);
		RLAmount rlRealGot = RLAmount.newInstance(pays);
		RLAmount rlRealPaid = RLAmount.newInstance(gets);
		String pair = buildPair(gets.currencyString(), gets.issuerString(), pays.currencyString(), pays.issuerString());
		RLOrder res = new RLOrder(Direction.BUY, rlRealGot, rlRealPaid, null, pair);
		return res;
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

	public static RLOrder fromOfferExecuted(Offer offer, boolean isOwn) {
		// All OE's paid and got are negative and need to be reversed
		BigDecimal ask = isOwn ? BigDecimal.ONE.divide(askFrom(offer), MathContext.DECIMAL64) : askFrom(offer);
		STObject executed = offer.executed(offer.get(STObject.FinalFields));
		Amount paid = executed.get(Amount.TakerPays);
		Amount got = executed.get(Amount.TakerGets);	
		RLAmount rlGot = isOwn ? RLAmount.newInstance(new Amount(paid.value(), paid.currency(), paid.issuer())) 
				: RLAmount.newInstance(new Amount(got.value(), got.currency(), got.issuer()));
		RLAmount rlPaid = isOwn ? RLAmount.newInstance(new Amount(got.value(), got.currency(), got.issuer()))
				: RLAmount.newInstance(new Amount(paid.value(), paid.currency(), paid.issuer())) ;
		String pair = buildPair(paid, got);

		RLOrder res = new RLOrder(Direction.BUY, rlGot, rlPaid, ask, pair);
		return res;
	}
	
	public static List<RLOrder> fromAutobridge(Map<String, ArrayList<Offer>> map){
		List<RLOrder> res = new ArrayList<>();
		
		ArrayList<Offer> majorities = null;
		ArrayList<Offer> minorities = null;

		for (ArrayList<Offer> offers : map.values()){
			if (majorities == null && minorities == null){
				majorities = offers;
				minorities = offers;
			}else{
				if (offers.size() > majorities.size()){
					majorities = offers;
				}else{
					minorities = offers;
				}
			}
		}
		
		BigDecimal refAsk = oeAvg(minorities);
		STObject oeExecutedMinor = minorities.get(0).executed(minorities.get(0).get(STObject.FinalFields));
		boolean isXRPGotInMajority = majorities.get(0).getPayCurrencyPair().startsWith(Currency.XRP.toString());
		
		Direction direction = Direction.BUY;		
		for (Offer oe : majorities){

			STObject oeExecuted = oe.executed(oe.get(STObject.FinalFields));
			BigDecimal newAsk = refAsk.multiply(oe.directoryAskQuality(), MathContext.DECIMAL64);			
			Amount oePaid = oeExecuted.get(Amount.TakerPays);	
			Amount oeGot = oeExecuted.get(Amount.TakerGets);
			if(!isXRPGotInMajority){
				Amount oePaidRef = oeExecutedMinor.get(Amount.TakerPays);			
				Amount newPaid = new Amount(oePaid.value().multiply(refAsk, MathContext.DECIMAL64), oePaidRef.currency(), oePaidRef.issuer());
				String pair = buildPair(newPaid, oeGot);
				Amount oeGotPositive = new Amount(oeGot.value(), oeGot.currency(), oeGot.issuer());
				res.add(new RLOrder(direction, RLAmount.newInstance(oeGotPositive), RLAmount.newInstance(newPaid), newAsk, pair));			
			}
			else{
				Amount oeGotRef = oeExecutedMinor.get(Amount.TakerGets);
				Amount newGot = new Amount(oeGot.value().divide(refAsk,  MathContext.DECIMAL64), oeGotRef.currency(), oeGotRef.issuer());
				Amount oePaidPositive = new Amount(oePaid.value(), oePaid.currency(), oePaid.issuer());
				String pair = buildPair(oePaid, newGot);
				res.add(new RLOrder(direction, RLAmount.newInstance(newGot), RLAmount.newInstance(oePaidPositive), newAsk, pair));			
			}
		}
		return res;
	}
	
	public static RLOrder basic(Direction direction, RLAmount quantity, RLAmount totalPrice){
		return new RLOrder(direction, quantity, totalPrice, null, null);

	}
	
	
	
	private static BigDecimal oeAvg(ArrayList<Offer> offers){
		BigDecimal paids = new BigDecimal(0);
		BigDecimal gots = new BigDecimal(0);
		for (Offer oe : offers){
			STObject executed = oe.executed(oe.get(STObject.FinalFields));
			paids = paids.add(executed.get(Amount.TakerPays).value(), MathContext.DECIMAL128);
			gots = gots.add(executed.get(Amount.TakerGets).value(), MathContext.DECIMAL128);
		}
		return paids.divide(gots, MathContext.DECIMAL64);	
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
	
	public SignedTransaction sign(Config config, int sequence, int fees){	
		OfferCreate offerCreate = new OfferCreate();
		if (this.direction.equals(Direction.BUY.text())){
			offerCreate.takerGets(totalPrice.amount());
			offerCreate.takerPays(quantity.amount());
		}else if (this.direction.equals(Direction.SELL.text())){
			offerCreate.takerGets(quantity.amount());
			offerCreate.takerPays(totalPrice.amount());			
		}else{
			throw new IllegalArgumentException("Direction not valid");
		}
		offerCreate.sequence(new UInt32(new BigInteger(String.valueOf(sequence))));
		offerCreate.fee(new Amount(new BigDecimal(fees)));
		offerCreate.account(AccountID.fromAddress(config.getCredentials().getAddress()));
		SignedTransaction signed = offerCreate.sign(config.getCredentials().getSecret());
		return signed;	
	}

	@Override
	public String stringify() {
		return super.stringify(this);
	}

}
