package com.mbcu.mmm.models.internal;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.mbcu.mmm.models.Base;
import com.mbcu.mmm.models.internal.BotConfig.Strategy;
import com.mbcu.mmm.utils.MyUtils;
import com.ripple.core.coretypes.AccountID;
import com.ripple.core.coretypes.Amount;
import com.ripple.core.coretypes.Currency;
import com.ripple.core.coretypes.STObject;
import com.ripple.core.coretypes.hash.Hash256;
import com.ripple.core.coretypes.uint.UInt32;
import com.ripple.core.fields.Field;
import com.ripple.core.types.known.sle.entries.Offer;
import com.ripple.core.types.known.tx.Transaction;
import com.ripple.core.types.known.tx.signed.SignedTransaction;
import com.ripple.core.types.known.tx.txns.OfferCancel;
import com.ripple.core.types.known.tx.txns.OfferCreate;

import io.reactivex.annotations.Nullable;

public final class RLOrder extends Base {

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
		this.direction = direction.text;
		this.quantity = amount(quantity);
		this.totalPrice = amount(totalPrice);
		this.passive = false;
		this.fillOrKill = false;
		this.rate = rate;
		this.cpair = cpair;
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

	public RLOrder reverse() {
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
		if (rate != null) {
			return rate;
		}
		if (quantity.value().compareTo(BigDecimal.ZERO) == 0) {
			return null;
		}
		return totalPrice.value().divide(quantity.value(), MathContext.DECIMAL128);
	}

	public static Amount amount(BigDecimal value, Currency currency, AccountID issuer) {
		if (currency.isNative()) {
			value = value.round(new MathContext(6, RoundingMode.HALF_DOWN)).setScale(6, RoundingMode.HALF_DOWN);
			return new Amount(value);
		}
		value = value.round(new MathContext(16, RoundingMode.HALF_DOWN)).setScale(16, RoundingMode.HALF_DOWN);
		return new Amount(value, currency, issuer);
	}

	public static Amount amount(Amount amount) {
		return amount(amount.value(), amount.currency(), amount.issuer());
	}

	public static Amount amount(BigDecimal value) {
		return amount(value, Currency.XRP, null);
	}

	/**
	 * Instantiate RLORder where ask rate is not needed or used for log. This
	 * object typically goes to submit or test.
	 * 
	 * @param direction
	 * @param quantity
	 * @param totalPrice
	 * @return
	 */
	public static RLOrder rateUnneeded(Direction direction, Amount quantity, Amount totalPrice) {
		Cpair cpair = direction == Direction.BUY ? Cpair.newInstance(totalPrice, quantity)
				: Cpair.newInstance(quantity, totalPrice);
		return new RLOrder(direction, quantity, totalPrice, null, cpair);
	}

	public static RLOrder fromWholeConsumed(Direction direction, Amount quantity, Amount totalPrice, BigDecimal rate) {
		Cpair cpair = direction == Direction.BUY ? Cpair.newInstance(totalPrice, quantity)
				: Cpair.newInstance(quantity, totalPrice);
		return new RLOrder(direction, quantity, totalPrice, rate, cpair);
	}

	public static RLOrder fromOfferCreate(Transaction txn) {
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
		Amount rlGot = isOCOwn ? amount(paid.value(), paid.currency(), paid.issuer())
				: amount(got.value(), got.currency(), got.issuer());
		Amount rlPaid = isOCOwn ? amount(got.value(), got.currency(), got.issuer())
				: amount(paid.value(), paid.currency(), paid.issuer());
		Cpair cpair = Cpair.newInstance(isOCOwn ? got : paid, isOCOwn ? paid : got);
		RLOrder res = new RLOrder(Direction.BUY, rlGot, rlPaid, ask, cpair);
		return res;
	}

	public static BefAf toBA(Amount bTakerPays, Amount bTakerGets, Amount aTakerPays, Amount aTakerGets, UInt32 seq,
			Hash256 txnId, RLOrder source) {
		Cpair bPair = Cpair.newInstance(bTakerGets, bTakerPays);
		BigDecimal bAsk = bTakerGets.value().divide(bTakerPays.value(), MathContext.DECIMAL64);
		RLOrder before = new RLOrder(Direction.BUY, bTakerPays, bTakerGets, bAsk, bPair);
		if (aTakerPays == null) {
			aTakerPays = new Amount(new BigDecimal("0"), bTakerPays.currency(), bTakerPays.issuer());
			aTakerGets = new Amount(new BigDecimal("0"), bTakerGets.currency(), bTakerGets.issuer());
		}
		RLOrder after = RLOrder.rateUnneeded(Direction.BUY, aTakerPays, aTakerGets);
		return new BefAf(before, after, seq, txnId, source);
	}

	public static List<RLOrder> fromAutobridge(Map<String, ArrayList<Offer>> map) {
		List<RLOrder> res = new ArrayList<>();

		ArrayList<Offer> majorities = null;
		ArrayList<Offer> minorities = null;

		for (ArrayList<Offer> offers : map.values()) {
			if (majorities == null && minorities == null) {
				majorities = offers;
				minorities = offers;
			} else {
				if (offers.size() > majorities.size()) {
					majorities = offers;
				} else {
					minorities = offers;
				}
			}
		}

		BigDecimal refAsk = oeAvg(minorities);
		STObject oeExecutedMinor = minorities.get(0).executed(minorities.get(0).get(STObject.FinalFields));
		boolean isXRPGotInMajority = majorities.get(0).getPayCurrencyPair().startsWith(Currency.XRP.toString());

		Direction direction = Direction.BUY;
		for (Offer oe : majorities) {
			STObject oeExecuted = oe.executed(oe.get(STObject.FinalFields));
			BigDecimal newAsk = refAsk.multiply(oe.directoryAskQuality(), MathContext.DECIMAL64);
			Amount oePaid = oeExecuted.get(Amount.TakerPays);
			Amount oeGot = oeExecuted.get(Amount.TakerGets);
			if (!isXRPGotInMajority) {
				Amount oePaidRef = oeExecutedMinor.get(Amount.TakerPays);
				Amount newPaid = new Amount(oePaid.value().multiply(refAsk, MathContext.DECIMAL64), oePaidRef.currency(),
						oePaidRef.issuer());
				Cpair cpair = Cpair.newInstance(newPaid, oeGot);
				Amount oeGotPositive = new Amount(oeGot.value(), oeGot.currency(), oeGot.issuer());
				res.add(new RLOrder(direction, oeGotPositive, newPaid, newAsk, cpair));
			} else {
				Amount oeGotRef = oeExecutedMinor.get(Amount.TakerGets);
				Amount newGot = new Amount(oeGot.value().divide(refAsk, MathContext.DECIMAL64), oeGotRef.currency(),
						oeGotRef.issuer());
				Amount oePaidPositive = new Amount(oePaid.value(), oePaid.currency(), oePaid.issuer());
				Cpair cpair = Cpair.newInstance(oePaid, newGot);
				res.add(new RLOrder(direction, newGot, oePaidPositive, newAsk, cpair));
			}
		}
		return res;
	}

	private static BigDecimal oeAvg(ArrayList<Offer> offers) {
		BigDecimal paids = new BigDecimal(0);
		BigDecimal gots = new BigDecimal(0);
		for (Offer oe : offers) {
			STObject executed = oe.executed(oe.get(STObject.FinalFields));
			paids = paids.add(executed.get(Amount.TakerPays).value(), MathContext.DECIMAL128);
			gots = gots.add(executed.get(Amount.TakerGets).value(), MathContext.DECIMAL128);
		}
		return paids.divide(gots, MathContext.DECIMAL64);
	}

	private static BigDecimal askFrom(Offer offer) {
		return offer.directoryAskQuality().stripTrailingZeros();
	}

	public SignedTransaction signOfferCreate(Config config, int sequence, int maxLedger, BigDecimal fees) {
		OfferCreate offerCreate = new OfferCreate();
		if (this.direction.equals(Direction.BUY.text())) {
			offerCreate.takerGets(clearXRPIssuer(totalPrice));
			offerCreate.takerPays(clearXRPIssuer(quantity));
		} else if (this.direction.equals(Direction.SELL.text())) {
			offerCreate.takerGets(clearXRPIssuer(quantity));
			offerCreate.takerPays(clearXRPIssuer(totalPrice));
		} else {
			throw new IllegalArgumentException("Direction not valid");
		}
		offerCreate.sequence(new UInt32(String.valueOf(sequence)));
		offerCreate.fee(new Amount(fees));
		offerCreate.lastLedgerSequence(new UInt32(String.valueOf(maxLedger)));
		offerCreate.account(AccountID.fromAddress(config.getCredentials().getAddress()));
		SignedTransaction signed = offerCreate.sign(config.getCredentials().getSecret());
		return signed;
	}

	public static SignedTransaction signOfferCancel(Config config, int seq, int newSeq, int maxLedger, BigDecimal fees) {
		OfferCancel res = new OfferCancel();
		res.put(Field.OfferSequence, new UInt32(String.valueOf(seq)));
		res.sequence(new UInt32(String.valueOf(seq)));
		res.fee(new Amount(fees));
		res.lastLedgerSequence(new UInt32(String.valueOf(maxLedger)));
		res.account(AccountID.fromAddress(config.getCredentials().getAddress()));

		SignedTransaction signed = res.sign(config.getCredentials().getSecret());
		return signed;
	}

	private static Amount clearXRPIssuer(Amount in) {
		if (in.issuer() != null && in.issuer().address.equals("rrrrrrrrrrrrrrrrrrrrrhoLvTp")) {
			return new Amount(in.value());
		}
		return in;
	}

	private static Queue<Integer> getLevels(int max) {
		Queue<Integer> res = new LinkedList<>();
		IntStream.range(1, max + 1).forEach(a -> {
			res.add(a);
		});
		return res;
	}

	public static List<RLOrder> buildBuysSeed(BigDecimal startRate, int levels, BotConfig bot, Logger log) {
		ArrayList<RLOrder> res = new ArrayList<>();
		BigDecimal margin = bot.getGridSpace();
		Queue<Integer> buyLevels = getLevels(levels);

		while (true) {
			if (buyLevels.isEmpty()) {
				break;
			}
			if (!buyLevels.isEmpty()) {
				Amount quantity = bot.base.add(bot.getBuyOrderQuantity());
				BigDecimal rate = startRate
						.subtract(margin.multiply(new BigDecimal(buyLevels.remove()), MathContext.DECIMAL64));
				if (rate.compareTo(BigDecimal.ZERO) <= 0) {
					log.severe("RLOrder.buildBuySeed rate below zero. Check config for the pair " + bot.getPair());
				} else {
					BigDecimal totalPriceValue = quantity.value().multiply(rate, MathContext.DECIMAL64);
					Amount totalPrice = RLOrder.amount(totalPriceValue, Currency.fromString(bot.quote.currencyString()),
							AccountID.fromAddress(bot.quote.issuerString()));
					RLOrder buy = RLOrder.rateUnneeded(Direction.BUY, quantity, totalPrice);
					res.add(buy);
				}
			}
		}
		return res;
	}

	public static List<RLOrder> buildSeedPct(boolean isBuySeed, LastBuySellTuple last, int levels, BotConfig bot, Logger log) {		
		MathContext mc = MathContext.DECIMAL64;
		BigDecimal mtp = bot.getGridSpace();
		BigDecimal unitPrice0	= isBuySeed ? last.buy.unitPrice  : last.sel.unitPrice;
		BigDecimal qty0 = isBuySeed ? last.buy.qty : last.sel.qty;
		/*
		 ell 	2.0000 	120.00 	
Sell 	2.0100 	118.81 	
Sell 	2.0200 	117.64 	
Sell 	2.0301 	116.47 	
Sell 	2.0402 	115.32 	
Buy 	2.0606 	113.06 	
Buy 	2.0709 	111.93 	
Buy 	2.0812 	110.83 	
		 */

		int range = isBuySeed ? last.isBuyPulledFromSel ? 3 : 2 : last.isSelPulledFromBuy ? 3 : 2;
		List<RLOrder> res = IntStream
				.range(range, levels + range)
				.mapToObj(n -> {
					BigDecimal rate = Collections.nCopies(n, BigDecimal.ONE).stream().reduce((x, y) -> x.multiply(mtp, mc)).get();					
					BigDecimal unitPrice1 = isBuySeed ? unitPrice0.divide(rate, mc) : unitPrice0.multiply(rate, mc);
					BigDecimal sqrt  = MyUtils.bigSqrt(rate);
					BigDecimal qty1 = isBuySeed ? qty0.multiply(sqrt, mc): qty0.divide(sqrt, mc);
					if (unitPrice1.compareTo(BigDecimal.ZERO) <= 0) {
						log.severe("RLOrder.buildBuySeedPct rate below zero. Check config for the pair " + bot.getPair());
					}			
					BigDecimal total1 = qty1.multiply(unitPrice1, mc);
					Amount qtyAmount1		= bot.base.add(qty1);
					Amount totalAmount1  = RLOrder.amount(total1, Currency.fromString(bot.quote.currencyString()), AccountID.fromAddress(bot.quote.issuerString()));
					Direction direction1 = isBuySeed ? Direction.BUY: Direction.SELL;
					RLOrder buy = RLOrder.rateUnneeded(direction1, qtyAmount1, totalAmount1);		
					return buy;
			})
	   .filter(o -> o.getQuantity().value().compareTo(BigDecimal.ZERO) > 0)
	   .filter(o -> o.getTotalPrice().value().compareTo(BigDecimal.ZERO) > 0)
	   .collect(Collectors.toList());
		return res;
	}
		
//	public static List<RLOrder> buildSelsSeedPct(LastAmount last, int levels, BotConfig bot, boolean isBlankStart) {			
//		BigDecimal mtp = MyUtils.bigSqrt(bot.getGridSpace());
////		BigDecimal botQuantity = bot.getSellOrderQuantity();
//		BigDecimal startPrice = last.rate;
//		BigDecimal startQuantity = last.quantity;
//		int range = isBlankStart ? 2 : 3;
//		List<RLOrder> res = IntStream
//				.range(range, levels + range)
//				.mapToObj(n -> {
//					BigDecimal rate = Collections.nCopies(n, BigDecimal.ONE).stream().reduce((x, y) -> x.multiply(mtp, MathContext.DECIMAL64)).get();					
//					BigDecimal newPri = startPrice.multiply(rate, MathContext.DECIMAL64);
//					BigDecimal newQty = startQuantity.divide(rate, MathContext.DECIMAL64);
//
//					Amount newAmt		  = bot.base.add(newQty);
//					Amount totalPrice = RLOrder.amount(newPri, Currency.fromString(bot.quote.currencyString()), AccountID.fromAddress(bot.quote.issuerString()));
//					RLOrder buy = RLOrder.rateUnneeded(Direction.SELL, newAmt, totalPrice);		
//					return buy;
//			})
//	   .filter(o -> o.getQuantity().value().compareTo(BigDecimal.ZERO) > 0)
//	   .filter(o -> o.getTotalPrice().value().compareTo(BigDecimal.ZERO) > 0)
//	   .collect(Collectors.toList());
//		return res;
//	}

	public static List<RLOrder> buildSelsSeed(BigDecimal startRate, int levels, BotConfig bot) {
		ArrayList<RLOrder> res 		= new ArrayList<>();
		BigDecimal margin 				= bot.getGridSpace();
		Queue<Integer> sellLevels = getLevels(levels);

		while (true) {
			if (sellLevels.isEmpty()) {
				break;
			}
			if (!sellLevels.isEmpty()) {
				Amount quantity = bot.base.add(bot.getSellOrderQuantity());
				BigDecimal rate = startRate.add(margin.multiply(new BigDecimal(sellLevels.remove()), MathContext.DECIMAL64));
				BigDecimal totalPriceValue = quantity.value().multiply(rate, MathContext.DECIMAL64);
				Amount totalPrice = RLOrder.amount(totalPriceValue, Currency.fromString(bot.quote.currencyString()),
						AccountID.fromAddress(bot.quote.issuerString()));
				RLOrder sell = RLOrder.rateUnneeded(Direction.SELL, quantity, totalPrice);
				res.add(sell);
			}
		}
		return res;
	}



	public static LastBuySellTuple nextTRates(ConcurrentMap<Integer, TRLOrder> buys, ConcurrentMap<Integer, TRLOrder> sels, BigDecimal worstBuy, BigDecimal worstSel, BotConfig botConfig) {
		return nextRates(TRLOrder.origins(buys), TRLOrder.origins(sels), worstBuy, worstSel, botConfig);
	}
	
	/**
	 * Routes last orders in orderbook or config to quantity and unit price
	 */
	public static LastBuySellTuple nextRates(ConcurrentMap<Integer, RLOrder> buys, ConcurrentMap<Integer, RLOrder> sels, BigDecimal lastBuy, BigDecimal lastSel, BotConfig botConfig) {
		MathContext mc = MathContext.DECIMAL64;
		BigDecimal selAm, buyAm;
		BigDecimal selUnPr = lastSel;
		BigDecimal buyUnPr = lastBuy;
		boolean isBuyPulledFromSel = false;
		boolean isSelPulledFromBuy = false;
		if (buys.isEmpty() && sels.isEmpty()) {		
			selAm = botConfig.getSellOrderQuantity();
			buyAm = botConfig.getBuyOrderQuantity();			
		}
		else {			
			List<Entry<Integer, RLOrder>> sorted = new ArrayList<>();
			RLOrder last;
			if (buys.isEmpty()) {				
				last = sortSels(sels, true).get(0).getValue();
				buyAm = last.getTotalPrice().value(); 
				buyUnPr = last.getQuantity().value().divide(buyAm, mc);
				isBuyPulledFromSel = true;
			} 
			else {
				sorted.addAll(sortBuys(buys, false));
				Collections.reverse(sorted);
				last = sorted.get(0).getValue();
				buyAm   = last.getQuantity().value();
				buyUnPr = last.getTotalPrice().value().divide(buyAm, mc);
			}		
			sorted.clear();
			if (sels.isEmpty()) {
				last = sortBuys(buys, false).get(0).getValue();
				selAm   = last.quantity.value();
				selUnPr = last.totalPrice.value().divide(selAm, mc);
				isSelPulledFromBuy = true;
			} 
			else {				
				sorted.addAll(sortSels(sels, false));
				last = sorted.get(0).getValue();			
//				lastSel = BigDecimal.ONE.divide(last.getRate(), MathContext.DECIMAL64);
				selAm   = last.getTotalPrice().value();
				selUnPr = last.quantity.value().divide(selAm, mc);
			}		
		}	
		return new LastBuySellTuple(buyUnPr, buyAm, selUnPr, selAm, isBuyPulledFromSel, isSelPulledFromBuy);
	}

	public static List<Entry<Integer, RLOrder>> sortTBuys(ConcurrentMap<Integer, TRLOrder> buys, boolean isReversed) {
		return sortBuys(TRLOrder.origins(buys), isReversed);
	}
	
	private static List<Entry<Integer, RLOrder>> sortBuys(ConcurrentMap<Integer, RLOrder> buys, boolean isReversed) {
		Set<Entry<Integer, RLOrder>> entries = buys.entrySet();
		List<Entry<Integer, RLOrder>> res = new ArrayList<Entry<Integer, RLOrder>>(entries);
		Collections.sort(res, !isReversed ? Collections.reverseOrder(obMapComparator) : obMapComparator);
		return res;
	}
	
	public static List<Entry<Integer, RLOrder>> sortTSels(ConcurrentMap<Integer, TRLOrder> sels, boolean isReversed) {
		return sortSels(TRLOrder.origins(sels), isReversed);
	}
	
	private static List<Entry<Integer, RLOrder>> sortSels(ConcurrentMap<Integer, RLOrder> sels, boolean isReversed) {
		Set<Entry<Integer, RLOrder>> entries = sels.entrySet();
		List<Entry<Integer, RLOrder>> res = new ArrayList<Entry<Integer, RLOrder>>(entries);
		Collections.sort(res, !isReversed ? obMapComparator : Collections.reverseOrder(obMapComparator));
		return res;
	}

	private static Comparator<Entry<Integer, RLOrder>> obMapComparator = new Comparator<Entry<Integer, RLOrder>>() {

		@Override
		public int compare(Entry<Integer, RLOrder> e1, Entry<Integer, RLOrder> e2) {
			return e1.getValue().getRate().compareTo(e2.getValue().getRate());
		}
	};

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
