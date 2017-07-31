 package com.mbcu.mmm.models.internal;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

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
	private final Amount quantity;
	private final Amount totalPrice;
	private final boolean passive;
	private final boolean fillOrKill;

	private final BigDecimal rate;
  private final String pair;

	private RLOrder(Direction direction, Amount quantity, Amount totalPrice, BigDecimal rate, String pair) {
		super();
		this.direction = direction.text;
		this.quantity = quantity;
		this.totalPrice = totalPrice;
		this.passive = false;
		this.fillOrKill = false;
		this.rate = rate;
		this.pair = pair;
	}

	public String getDirection() {
		return direction;
	}


	public Amount getQuantity() {
		return quantity;
	}
	
	public Amount getTotalPrice() {
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
	
	public BigDecimal getRate() {
		if (rate != null){
			return rate;
		}
		return totalPrice.value().divide(quantity.value(), MathContext.DECIMAL128);
	}
	
	public static Amount amount(BigDecimal value, Currency currency, AccountID issuer){
		if(currency.isNative()){
			value = value.round(new MathContext(6, RoundingMode.HALF_DOWN));
			return new Amount(value);
		}else{
			value = value.round(new MathContext(16, RoundingMode.HALF_DOWN));
			return new Amount(value, currency, issuer);
		}
	}
	
	public static Amount amount(String value, String currency, String issuer){
		if (currency.equals(Currency.XRP.toString())){
			return amount(new BigDecimal(value), Currency.XRP, AccountID.XRP_ISSUER);
		}
		return amount(new BigDecimal(value), Currency.fromString(currency), AccountID.fromAddress(issuer));
	}

	/**
	 * Instantiate RLORder where ask rate is not needed or used for log. This object typically goes to submit or test. 
	 * @param direction
	 * @param quantity
	 * @param totalPrice
	 * @return
	 */
	public static RLOrder rateUnneeded(Direction direction, Amount quantity, Amount totalPrice){
		String pair = direction == Direction.BUY ? buildPair(totalPrice, quantity) : buildPair(quantity, totalPrice);
		return new RLOrder(direction, quantity, totalPrice, null, pair);
	}
	
	public static RLOrder fromOfferCreate(Transaction txn){
		Amount gets = txn.get(Amount.TakerPays);
		Amount pays = txn.get(Amount.TakerGets);
		String pair = buildPair(gets, pays);
		RLOrder res = new RLOrder(Direction.BUY, gets, pays, null, pair);
		return res;
	}

	public static RLOrder fromOfferCreated(Offer offer) {		
		BigDecimal ask = askFrom(offer);		
		Amount pays = offer.takerPays();
		Amount gets = offer.takerGets();
		String pair = buildPair(gets, pays);
		RLOrder res = new RLOrder(Direction.BUY, gets, pays, ask, pair);
		return res;
	}

	public static RLOrder fromOfferExecuted(Offer offer, boolean isOCOwn) {
		// All OE's paid and got are negative
		BigDecimal ask = isOCOwn ? BigDecimal.ONE.divide(askFrom(offer), MathContext.DECIMAL64) : askFrom(offer);
		STObject executed = offer.executed(offer.get(STObject.FinalFields));
		Amount paid = executed.get(Amount.TakerPays);
		Amount got = executed.get(Amount.TakerGets);	
		Amount rlGot = isOCOwn ? amount(paid.value(), paid.currency(), paid.issuer()) : amount(got.value(), got.currency(), got.issuer());
		Amount rlPaid = isOCOwn ? amount(got.value(), got.currency(), got.issuer()) :amount(paid.value(), paid.currency(), paid.issuer()) ;
		String pair = buildPair(isOCOwn ? got : paid , isOCOwn ? paid: got);
		RLOrder res = new RLOrder(Direction.BUY, rlGot, rlPaid, ask, pair);	
		return res;
	}
	
	public static BefAf toBA(Amount bTakerPays, Amount bTakerGets, Amount aTakerPays, Amount aTakerGets){		
		String bPair = buildPair(bTakerGets, bTakerPays);		
		BigDecimal bAsk = bTakerGets.value().divide(bTakerPays.value(), MathContext.DECIMAL64);
		RLOrder before = new RLOrder(Direction.BUY, bTakerPays, bTakerGets, bAsk, bPair);	
		if (aTakerPays == null){
			aTakerPays = new Amount(new BigDecimal("0"), bTakerPays.currency(), bTakerPays.issuer());
			aTakerGets = new Amount(new BigDecimal("0"), bTakerGets.currency(), bTakerGets.issuer());
		}
		RLOrder after = RLOrder.rateUnneeded(Direction.BUY, aTakerPays, aTakerGets);		
		return new BefAf(before, after);		
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
				res.add(new RLOrder(direction, oeGotPositive, newPaid, newAsk, pair));			
			}
			else{
				Amount oeGotRef = oeExecutedMinor.get(Amount.TakerGets);
				Amount newGot = new Amount(oeGot.value().divide(refAsk,  MathContext.DECIMAL64), oeGotRef.currency(), oeGotRef.issuer());
				Amount oePaidPositive = new Amount(oePaid.value(), oePaid.currency(), oePaid.issuer());
				String pair = buildPair(oePaid, newGot);
				res.add(new RLOrder(direction, newGot, oePaidPositive, newAsk, pair));			
			}
		}
		return res;
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
	
	public SignedTransaction sign(Config config, int sequence, int maxLedger, BigDecimal fees){	
		OfferCreate offerCreate = new OfferCreate();
		if (this.direction.equals(Direction.BUY.text())){
			offerCreate.takerGets(clearXRPIssuer(totalPrice));
			offerCreate.takerPays(clearXRPIssuer(quantity));
		}else if (this.direction.equals(Direction.SELL.text())){
			offerCreate.takerGets(clearXRPIssuer(quantity));
			offerCreate.takerPays(clearXRPIssuer(totalPrice));			
		}else{
			throw new IllegalArgumentException("Direction not valid");
		}
		offerCreate.sequence(new UInt32(String.valueOf(sequence)));
		offerCreate.fee(new Amount(fees));
		offerCreate.lastLedgerSequence(new UInt32(String.valueOf(maxLedger)));
		offerCreate.account(AccountID.fromAddress(config.getCredentials().getAddress()));
		SignedTransaction signed = offerCreate.sign(config.getCredentials().getSecret());
		return signed;	
	}
	
	private static Amount clearXRPIssuer(Amount in){		
		if (in.issuer() != null && in.issuer().address.equals("rrrrrrrrrrrrrrrrrrrrrhoLvTp")){
			return new Amount(in.value());		
		}
		return in;		
	}

	public static ArrayList<RLOrder> buildSeed(BotConfig bot){
		ArrayList<RLOrder> res = new ArrayList<>();
		BigDecimal middlePrice = new BigDecimal(bot.startMiddlePrice);
		BigDecimal margin = new BigDecimal(bot.gridSpace);
		Queue<Integer> buyLevels = bot.getLevels(bot.buyGridLevels);
		Queue<Integer> sellLevels = bot.getLevels(bot.sellGridLevels);
		
		while (true){
			if (buyLevels.isEmpty() && sellLevels.isEmpty()){
				break;
			}
			if (!buyLevels.isEmpty()){
				Amount quantity =	bot.base.add(bot.getBuyOrderQuantity());	
				
				BigDecimal rate = middlePrice.subtract(margin.multiply(new BigDecimal(buyLevels.remove()), MathContext.DECIMAL64));
				if (rate.compareTo(BigDecimal.ZERO) <= 0){
					buyLevels.clear();				
				}else{
					BigDecimal totalPriceValue = quantity.value().multiply(rate, MathContext.DECIMAL64);
					Amount totalPrice =  RLOrder.amount(totalPriceValue, Currency.fromString(bot.quote.currencyString()), AccountID.fromAddress(bot.quote.issuerString()));
					RLOrder buy = RLOrder.rateUnneeded(Direction.BUY, quantity, totalPrice);
					res.add(buy);
				}
			}
			if (!sellLevels.isEmpty()){
				Amount quantity = bot.base.add(bot.getSellOrderQuantity());
				BigDecimal rate = middlePrice.add(margin.multiply(new BigDecimal(sellLevels.remove()), MathContext.DECIMAL64));
				BigDecimal totalPriceValue = quantity.value().multiply(rate, MathContext.DECIMAL64);
				Amount totalPrice = RLOrder.amount(totalPriceValue, Currency.fromString(bot.quote.currencyString()), AccountID.fromAddress(bot.quote.issuerString()));
				RLOrder sell = RLOrder.rateUnneeded(Direction.SELL, quantity, totalPrice);
				res.add(sell);	
			}			
		}	
		return res;
	}
	
	@Override
	public String stringify() {	
		StringBuffer sb = new StringBuffer(direction);
		sb.append("\n");
		sb.append("quantity:");
		sb.append(quantity.toTextFull());
		sb.append("\n");
		sb.append("totalPrice:");
		sb.append(totalPrice.toTextFull());
		sb.append("\n");
		sb.append("rate:");
		sb.append(getRate().toString());
		sb.append("\n");
		sb.append("pair:");
		sb.append(getPair());
		sb.append("\n");
		return sb.toString();		
	}
	
	
}
