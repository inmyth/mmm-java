 package com.mbcu.mmm.models.internal;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.IntStream;
import java.util.Queue;

import com.mbcu.mmm.models.Base;
import com.ripple.core.coretypes.AccountID;
import com.ripple.core.coretypes.Amount;
import com.ripple.core.coretypes.Currency;
import com.ripple.core.coretypes.STObject;
import com.ripple.core.coretypes.uint.UInt32;
import com.ripple.core.fields.Field;
import com.ripple.core.types.known.sle.entries.Offer;
import com.ripple.core.types.known.tx.Transaction;
import com.ripple.core.types.known.tx.signed.SignedTransaction;
import com.ripple.core.types.known.tx.txns.OfferCancel;
import com.ripple.core.types.known.tx.txns.OfferCreate;

import io.reactivex.annotations.Nullable;

public final class RLOrder extends Base{
	
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
  private final Cpair cpair;

	private RLOrder(Direction direction, Amount quantity, Amount totalPrice, BigDecimal rate, Cpair cpair) {
		super();
		this.direction 	= direction.text;
		this.quantity 	= amount(quantity);
		this.totalPrice = amount(totalPrice);
		this.passive 		= false;
		this.fillOrKill = false;
		this.rate 			= rate;
		this.cpair 			= cpair;
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

	public Cpair getCpair() {
		return cpair;
	}
	
	public RLOrder reverse(){
		Direction newDirection = this.direction.equals(Direction.BUY.text()) ? Direction.SELL : Direction.BUY;
		String newPair = cpair.toString();
		Amount newQuantity = totalPrice;
		Amount newTotalPrice = quantity;
		BigDecimal rate = newTotalPrice.value().divide(newQuantity.value(), MathContext.DECIMAL64);	
		RLOrder res = new RLOrder(newDirection, newQuantity, newTotalPrice, rate, Cpair.newInstance(newPair));
		return res;
	}
	
	@Nullable
	public BigDecimal getRate() {
		if (rate != null){
			return rate;
		}
		if (quantity.value().compareTo(BigDecimal.ZERO) == 0){
			return null;
		}
		return totalPrice.value().divide(quantity.value(), MathContext.DECIMAL128);
	}
	
	public static Amount amount(BigDecimal value, Currency currency, AccountID issuer){
		if (currency.isNative()){
			value = value.round(new MathContext(6, RoundingMode.HALF_DOWN));
			return new Amount(value);
		}
		value = value.round(new MathContext(16, RoundingMode.HALF_DOWN));
		return new Amount(value, currency, issuer);		
	}
	public static Amount amount(Amount amount){
		return amount(amount.value(), amount.currency(), amount.issuer());
	}
	
	
	public static Amount amount(BigDecimal value){
		return amount(value, Currency.XRP, null);
	}
	
	/**
	 * Instantiate RLORder where ask rate is not needed or used for log. This object typically goes to submit or test. 
	 * @param direction
	 * @param quantity
	 * @param totalPrice
	 * @return
	 */
	public static RLOrder rateUnneeded(Direction direction, Amount quantity, Amount totalPrice){
		Cpair cpair = direction == Direction.BUY ? Cpair.newInstance(totalPrice, quantity) : Cpair.newInstance(quantity, totalPrice);
		return new RLOrder(direction, quantity, totalPrice, null, cpair);
	}
	
	public static RLOrder fromWholeConsumed(Direction direction, Amount quantity, Amount totalPrice, BigDecimal rate){
		Cpair cpair = direction == Direction.BUY ? Cpair.newInstance(totalPrice, quantity) : Cpair.newInstance(quantity, totalPrice);
		return new RLOrder(direction, quantity, totalPrice, rate, cpair);
	}
	
	public static RLOrder fromOfferCreate(Transaction txn){
		Amount gets = txn.get(Amount.TakerPays);
		Amount pays = txn.get(Amount.TakerGets);
		Cpair cpair = Cpair.newInstance(gets, pays);
		RLOrder res = new RLOrder(Direction.BUY, gets, pays, null, cpair);
		return res;
	}

	public static RLOrder fromOfferCreated(Offer offer) {		
		BigDecimal ask = askFrom(offer);		
		Amount pays = offer.takerPays();
		Amount gets = offer.takerGets();
		Cpair cpair = Cpair.newInstance(gets, pays);
		RLOrder res = new RLOrder(Direction.BUY, gets, pays, ask, cpair);
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
		Cpair cpair = Cpair.newInstance(isOCOwn ? got : paid , isOCOwn ? paid: got);
		RLOrder res = new RLOrder(Direction.BUY, rlGot, rlPaid, ask, cpair);	
		return res;
	}
	
	public static BefAf toBA(Amount bTakerPays, Amount bTakerGets, Amount aTakerPays, Amount aTakerGets, UInt32 seq){		
		Cpair bPair = Cpair.newInstance(bTakerGets, bTakerPays);		
		BigDecimal bAsk = bTakerGets.value().divide(bTakerPays.value(), MathContext.DECIMAL64);
		RLOrder before = new RLOrder(Direction.BUY, bTakerPays, bTakerGets, bAsk, bPair);	
		if (aTakerPays == null){
			aTakerPays = new Amount(new BigDecimal("0"), bTakerPays.currency(), bTakerPays.issuer());
			aTakerGets = new Amount(new BigDecimal("0"), bTakerGets.currency(), bTakerGets.issuer());
		}
		RLOrder after = RLOrder.rateUnneeded(Direction.BUY, aTakerPays, aTakerGets);		
		return new BefAf(before, after, seq);		
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
				Cpair cpair = Cpair.newInstance(newPaid, oeGot);
				Amount oeGotPositive = new Amount(oeGot.value(), oeGot.currency(), oeGot.issuer());
				res.add(new RLOrder(direction, oeGotPositive, newPaid, newAsk, cpair));			
			}
			else{
				Amount oeGotRef = oeExecutedMinor.get(Amount.TakerGets);
				Amount newGot = new Amount(oeGot.value().divide(refAsk,  MathContext.DECIMAL64), oeGotRef.currency(), oeGotRef.issuer());
				Amount oePaidPositive = new Amount(oePaid.value(), oePaid.currency(), oePaid.issuer());
				Cpair cpair = Cpair.newInstance(oePaid, newGot);
				res.add(new RLOrder(direction, newGot, oePaidPositive, newAsk, cpair));			
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
	


	private static BigDecimal askFrom(Offer offer) {
		return offer.directoryAskQuality().stripTrailingZeros();
	}
	
	public SignedTransaction signOfferCreate(Config config, int sequence, int maxLedger, BigDecimal fees){	
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
	
	public static SignedTransaction signOfferCancel(Config config, int seq, int newSeq, int maxLedger, BigDecimal fees){
		OfferCancel res = new OfferCancel();
		res.put(Field.OfferSequence, new UInt32(String.valueOf(seq)));
		res.sequence(new UInt32(String.valueOf(seq)));
		res.fee(new Amount(fees));
		res.lastLedgerSequence(new UInt32(String.valueOf(maxLedger)));
		res.account(AccountID.fromAddress(config.getCredentials().getAddress()));

		SignedTransaction signed = res.sign(config.getCredentials().getSecret());
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
		res.addAll(buildBuysSeed(new BigDecimal(bot.startMiddlePrice), bot.getBuyGridLevels(), bot));
		res.addAll(buildSelsSeed(new BigDecimal(bot.startMiddlePrice), bot.getSellGridLevels(), bot));
		return res;
	}
	
	private static Queue<Integer> getLevels(int max){
		Queue<Integer> res = new LinkedList<>();
		IntStream.range(1, max + 1).forEach(a -> {res.add(a);});
		return res;
	}
	
	
	public static List<RLOrder> buildBuysSeed(BigDecimal startRate, int levels, BotConfig bot){
		ArrayList<RLOrder> res = new ArrayList<>();
		BigDecimal margin = new BigDecimal(bot.gridSpace);
		Queue<Integer> buyLevels = getLevels(levels);
		
		while (true){
			if (buyLevels.isEmpty()){
				break;
			}
			if (!buyLevels.isEmpty()){
				Amount quantity =	bot.base.add(bot.getBuyOrderQuantity());					
				BigDecimal rate = startRate.subtract(margin.multiply(new BigDecimal(buyLevels.remove()), MathContext.DECIMAL64));
				if (rate.compareTo(BigDecimal.ZERO) <= 0){
					buyLevels.clear();				
				}else{
					BigDecimal totalPriceValue = quantity.value().multiply(rate, MathContext.DECIMAL64);
					Amount totalPrice =  RLOrder.amount(totalPriceValue, Currency.fromString(bot.quote.currencyString()), AccountID.fromAddress(bot.quote.issuerString()));
					RLOrder buy = RLOrder.rateUnneeded(Direction.BUY, quantity, totalPrice);
					res.add(buy);
				}
			}		
		}	
		return res;
	}
	
	public static List<RLOrder> buildSelsSeed(BigDecimal startRate, int levels, BotConfig bot){
		ArrayList<RLOrder> res = new ArrayList<>();
		BigDecimal margin = new BigDecimal(bot.gridSpace);
		Queue<Integer> sellLevels = getLevels(levels);
		
		while (true){
			if (sellLevels.isEmpty()){
				break;
			}
			if (!sellLevels.isEmpty()){
				Amount quantity = bot.base.add(bot.getSellOrderQuantity());
				BigDecimal rate = startRate.add(margin.multiply(new BigDecimal(sellLevels.remove()), MathContext.DECIMAL64));
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
		BigDecimal rate = getRate();
		sb.append(rate != null ? rate.toPlainString() : "rate na");
		sb.append("\n");
		sb.append("pair:");
		sb.append(getCpair());
		sb.append("\n");
		return sb.toString();		
	}
	
	
}
