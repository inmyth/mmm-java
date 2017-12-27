package com.mbcu.mmm.sequences;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Stream;

import com.mbcu.mmm.helpers.TAccountOffer;
import com.mbcu.mmm.models.internal.BefAf;
import com.mbcu.mmm.models.internal.BotConfig;
import com.mbcu.mmm.models.internal.BotConfig.Strategy;
import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.models.internal.LastBuySellTuple;
import com.mbcu.mmm.models.internal.RLOrder;
import com.mbcu.mmm.models.internal.RLOrder.Direction;
import com.mbcu.mmm.models.internal.TRLOrder;
import com.mbcu.mmm.rx.BusBase;
import com.mbcu.mmm.rx.RxBus;
import com.mbcu.mmm.rx.RxBusProvider;
import com.mbcu.mmm.sequences.state.State;
import com.mbcu.mmm.sequences.state.State.BroadcastPendings;
import com.mbcu.mmm.sequences.state.State.OnOrderReady;
import com.mbcu.mmm.utils.MyLogger;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class Orderbook extends Base {

	private final BotConfig botConfig;
	private final ConcurrentHashMap<Integer, TRLOrder> buys = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Integer, TRLOrder> sels = new ConcurrentHashMap<>();

	private final RxBus bus = RxBusProvider.getInstance();
	private final CompositeDisposable disposables = new CompositeDisposable();
	private final CompositeDisposable accountOffersDispo = new CompositeDisposable();
	private final AtomicInteger lastBalanced = new AtomicInteger(0);
	private final AtomicInteger ledgerValidated = new AtomicInteger(0);
	private final AtomicInteger ledgerClosed = new AtomicInteger(0);
	private LastBuySellTuple start; 

	private Orderbook(Config config, BotConfig botConfig) {
		super(MyLogger.getLogger(String.format(Txc.class.getName())), config);
		this.botConfig = botConfig;
		this.start    = new LastBuySellTuple(botConfig.getStartMiddlePrice(), botConfig.getBuyOrderQuantity(), botConfig.getStartMiddlePrice(), botConfig.getSellOrderQuantity(), false, false);

		accountOffersDispo
				.add(bus.toObservable().subscribeOn(Schedulers.newThread()).subscribeWith(new DisposableObserver<Object>() {

					@Override
					public void onNext(Object o) {
						BusBase base = (BusBase) o;
						if (base instanceof Common.OnAccountOffers) {
							Common.OnAccountOffers event = (Common.OnAccountOffers) o;
							boolean buyMatched = false;
							boolean selMatched = false;
							Optional<Boolean> pairMatched = null;
							
							for (TAccountOffer t : event.accOffs) {
								pairMatched = pairMatched(t.getOrder());
								if (pairMatched.isPresent()) {
									if (pairMatched.get()) {
										buyMatched = true;
									} else {
										selMatched = true;
									}
									insert(t.getOrder(), t.getOrder(), t.getSeq(), pairMatched.get());
								}
							}
							if (buyMatched || selMatched) {
								LastBuySellTuple worstRates = RLOrder.nextTRates(buys, sels, start.buy.unitPrice, start.sel.unitPrice, botConfig);
								start = worstRates;
							}
						}
					}

					@Override
					public void onError(Throwable e) {
						log(e.getMessage(), Level.SEVERE);
					}

					@Override
					public void onComplete() {
					}
				}));

		disposables.add(bus.toObservable().subscribeOn(Schedulers.newThread()).subscribeWith(new DisposableObserver<Object>() {

					@Override
					public void onNext(Object o) {
						BusBase base = (BusBase) o;
						try {
							if (base instanceof Common.OnLedgerClosed) {
								lastBalanced.incrementAndGet();
								Common.OnLedgerClosed event = (Common.OnLedgerClosed) o;
								ledgerClosed.set(event.ledgerEvent.getClosed());
								ledgerValidated.set(event.ledgerEvent.getValidated());
								requestPendings();
							} else if (base instanceof Common.OnDifference) {
								Common.OnDifference event = (Common.OnDifference) o;
								boolean isBelongToThisOrderbook = false;
								
								List<RLOrder> preFullCounters 	 = new ArrayList<>();
								List<RLOrder> prePartialCounters = new ArrayList<>();
								for (BefAf ba : event.bas) {
									Optional<Boolean> pairMatched = pairMatched(ba.after);
									if (pairMatched.isPresent()) {
										isBelongToThisOrderbook = true;
										if (ba.source != null){											
											if (ba.after.getQuantity().value().compareTo(BigDecimal.ZERO) != 0){ 
												insert(ba.before, ba.after, ba.befSeq.intValue(), pairMatched.get());
											} 
											else {
												preFullCounters.add(ba.source);
											}				
										}
										else if (ba.source == null){
											if(ba.after.getQuantity().value().compareTo(BigDecimal.ZERO) == 0) { // fully consumed
												TRLOrder entry = pairMatched.get() ? buys.get(ba.befSeq.intValue()) : sels.get(ba.befSeq.intValue());
												if (entry != null){
													preFullCounters.add(entry.getOrigin());
													remove(ba.befSeq.intValue());
												}
												else{
													log("Orderbook Trying to remove already gone :" + ba.befSeq.intValue(), Level.SEVERE);
												}

											}
											else {
												prePartialCounters.add(ba.after);
												update(ba.after, ba.befSeq.intValue(), ba.befSeq.intValue(), pairMatched.get()); // partially consumed
											}
										}
									}
								}

								if (isBelongToThisOrderbook) {
									LastBuySellTuple worstRates = RLOrder.nextTRates(buys, sels, start.buy.unitPrice, start.sel.unitPrice, botConfig);
									start = worstRates;
								}	
								
								if (!preFullCounters.isEmpty()){
									bus.send(new OnOrderFullConsumed(preFullCounters));
								}
								
							} else if (base instanceof Common.OnOfferEdited) {
								Common.OnOfferEdited event = (Common.OnOfferEdited) o;
								Optional<Boolean> pairMatched = pairMatched(event.ba.after);
								if (pairMatched.isPresent()) {
									update(event.ba.after, event.ba.befSeq.intValue(), event.newSeq.intValue(), pairMatched.get());
									LastBuySellTuple worstRates = RLOrder.nextTRates(buys, sels, start.buy.unitPrice, start.sel.unitPrice, botConfig);
									start = worstRates;
								}
							} else if (base instanceof Common.OnOfferCanceled) {
								Common.OnOfferCanceled event = (Common.OnOfferCanceled) o;
								Boolean pairMatched = remove(event.prevSeq.intValue());
								if (pairMatched != null) {
									LastBuySellTuple worstRates = RLOrder.nextTRates(buys, sels, start.buy.unitPrice, start.sel.unitPrice, botConfig);
									start = worstRates;
								}
							} else if (base instanceof State.BroadcastPendings) {
								BroadcastPendings event = (BroadcastPendings) o;
								if (event.pair.equals(botConfig.getPair())) {
									if (event.creates.isEmpty() && event.cancels.isEmpty() && lastBalanced.get() >= config.getIntervals().getBalancer()) {
										lastBalanced.set(0);
										List<Entry<Integer, RLOrder>> sortedSels, sortedBuys;
										sortedSels = RLOrder.sortTSels(sels, false);
										sortedBuys = RLOrder.sortTBuys(buys, false);
										warnEmptyOrderbook(sortedSels, sortedBuys);
										printOrderbook(sortedSels, sortedBuys);
										balancer(sortedSels, sortedBuys);
									}
								}
							}
						} catch (Exception e) {
							MyLogger.exception(LOGGER, base.toString(), e);
							throw e;
						}
					}

					@Override
					public void onError(Throwable e) {
						log(e.getMessage(), Level.SEVERE);
					}

					@Override
					public void onComplete() {
						// TODO Auto-generated method stub
					}

				}));
	}

	private void printOrderbook(List<Entry<Integer, RLOrder>> sortedSels, List<Entry<Integer, RLOrder>> sortedBuys) {
		String ob = dump(ledgerClosed.get(), ledgerValidated.get(), sortedSels, sortedBuys);
		// MyUtils.toFile(ob, path);
		log(ob, Level.FINER);
	}
	
	private void warnEmptyOrderbook(List<Entry<Integer, RLOrder>> sortedSels, List<Entry<Integer, RLOrder>> sortedBuys){
		String warning = sortedSels.isEmpty() ? "Warning. Orderbook empty : sell " : null;
		warning = sortedBuys.isEmpty() ? "Warning. Orderbook empty : buy" : null;
		warning = sortedBuys.isEmpty() && sortedSels.isEmpty() ? "Warning. Orderbook empty : both" : null;
		if (warning != null){
			log(warning + botConfig.getPair(), Level.WARNING);
//			bus.send(new Notifier.RequestEmailNotice(warning, botConfig.getPair(), System.currentTimeMillis()));
		}
	}

	private void requestPendings() {
		bus.send(new Balancer.OnRequestNonOrderbookRLOrder(botConfig.getPair()));
	}

	private void balancer(List<Entry<Integer, RLOrder>> sortedSels, List<Entry<Integer, RLOrder>> sortedBuys) {
		// using pendings is unrealiable.
		BigDecimal sumBuys = sum(Direction.BUY);
		BigDecimal sumSels = sum(Direction.SELL);
		log(printLog(sumBuys, sumSels, count(Direction.BUY), count(Direction.SELL)));
		List<RLOrder> gens = new ArrayList<>();

		BigDecimal buysGap = margin(sumBuys, Direction.BUY);
		if (buysGap.compareTo(BigDecimal.ZERO) > 0) {
			gens.addAll(generate(sortedBuys, buysGap, Direction.BUY));
		}

		BigDecimal selsGap = margin(sumSels, Direction.SELL);
		if (selsGap.compareTo(BigDecimal.ZERO) > 0) {
			gens.addAll(generate(sortedSels, selsGap, Direction.SELL));
		}
		gens.forEach(rlo -> bus.send(new State.OnOrderReady(rlo, OnOrderReady.Source.BALANCER)));
	}

	private List<RLOrder> generate(List<Entry<Integer, RLOrder>> sorteds, BigDecimal margin, Direction direction) {
		List<RLOrder> res = new ArrayList<>();
		margin = margin.abs();
		int levels = margin
				.divide(direction == Direction.BUY ? botConfig.getBuyOrderQuantity() : botConfig.getSellOrderQuantity(), MathContext.DECIMAL32)
				.intValue();
		if (direction == Direction.SELL) {
			Collections.reverse(sorteds);
		}

		if (levels > 0) {
			if (botConfig.getStrategy() == Strategy.FULLRATEPCT || botConfig.getStrategy() == Strategy.FULLRATESEEDPCT ) {
				res.addAll(RLOrder.buildSeedPct(direction == Direction.BUY, start, levels, botConfig, LOGGER));			
			} 
			else {
				res.addAll(direction == Direction.BUY ? RLOrder.buildBuysSeed(start.buy.unitPrice, levels, botConfig, super.LOGGER)
						: RLOrder.buildSelsSeed(start.sel.unitPrice, levels, botConfig));
			}
		}
		return res;
	}

	/**
	 * 
	 * @param sum
	 * @param direction
	 * @return negative if orderbook is bigger than config, positive if smaller
	 */
	private BigDecimal margin(BigDecimal sum, Direction direction) {
		BigDecimal configTotalQuantity = direction == Direction.BUY ? botConfig.getTotalBuyQty() : botConfig.getTotalSelQty();
		return configTotalQuantity.subtract(sum);
	}

	private String printLog(BigDecimal sumBuys, BigDecimal sumSels, int countBuys, int countSels) {
		StringBuffer res = new StringBuffer("\nOrderbook ");
		res.append(botConfig.getPair());
		res.append("\nBUYS : ");
		res.append("n: ");
		res.append(countBuys);
		res.append(" totalQty ");
		res.append(sumBuys.toPlainString());
		res.append("\nSELLS : ");
		res.append("n: ");
		res.append(countSels);
		res.append(" totalQty ");
		res.append(sumSels.toPlainString());
		res.append("\n");
		return res.toString();
	}

	private int count(Direction direction) {
		int res = direction == Direction.BUY ? buys.size() : sels.size();
		return res;
	}

	private BigDecimal sum(Direction direction) {
		Stream<TRLOrder> orderbook;
		Function<TRLOrder, BigDecimal> fun;
		if (direction == Direction.BUY) {
			orderbook = buys.values().stream();
			fun = tRlOrder -> tRlOrder.getNow().getQuantity().value();
		} else {
			orderbook = sels.values().stream();
			fun = rlOrder -> rlOrder.getNow().getTotalPrice().value();
		}
		return orderbook.map(fun).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private void update(RLOrder now, int oldSeq, int newSeq, Boolean isAligned){
//		ConcurrentHashMap<Integer, TRLOrder> orders = isAligned ? buys : sels;
		if (buys.containsKey(oldSeq)){
			buys.put(newSeq, buys.get(oldSeq).updatedWith(now));
			remove(oldSeq);
		} 
		else if (sels.contains(oldSeq)){
			sels.put(newSeq, sels.get(oldSeq).updatedWith(now));
			remove(oldSeq);
		}	
		else {
			log("Update orderbook " + botConfig.getPair() + " failed, seq " + oldSeq +  " not found" , Level.WARNING);
		}			
	}

	private void insert(RLOrder source, RLOrder now, int seq, Boolean isAligned) {	
		ConcurrentHashMap<Integer, TRLOrder> orders = isAligned ? buys : sels;
		if (orders.contains(seq)){
			orders.put(seq, orders.get(seq).updatedWith(now));
		}
		else{
			orders.put(seq, new TRLOrder(source, now));
		}
	}

	// Because it's map, it's O(1) and idempotent
	private Boolean remove(int seq) {
		TRLOrder a = buys.remove(seq);
		if (a != null) {
			return true;
		}
		TRLOrder b = sels.remove(seq);
		if (b != null) {
			return false;
		}
		return null;
	}

//	private void shelve(RLOrder after, UInt32 seq, boolean isAligned) {
//		shelve(after, seq.intValue(), isAligned);
//	}

	/**
	 * @param in
	 * @return null if not matched, true if aligned, false if reversed
	 */
	private Optional<Boolean> pairMatched(RLOrder in) {
		return in.getCpair().isMatch(this.botConfig.getPair());
	}

	private String dump(int ledgerClosed, int ledgerValidated, List<Entry<Integer, RLOrder>> sortedSels,
			List<Entry<Integer, RLOrder>> sortedBuys) {
		StringBuffer sb = new StringBuffer("Orderbook");
		sb.append(botConfig.getPair());
		sb.append("\n");
		sb.append("Ledger closed : ");
		sb.append(ledgerClosed);
		sb.append(" ");
		sb.append("Ledger validated : ");
		sb.append(ledgerValidated);
		sb.append("\n\n");
		sb.append("SELLS\n");
		sortedSels.stream().forEach(entry -> {
			sb.append(entry.getValue().getTotalPrice().toText());
			sb.append(" ");
			sb.append(entry.getValue().getQuantity());
			sb.append(" ");
			sb.append(BigDecimal.ONE.divide(entry.getValue().getRate(), MathContext.DECIMAL32).toPlainString());
			sb.append(" ");
			sb.append(" Seq:");
			sb.append(entry.getKey());
			sb.append("\n");
		});
		sb.append("\n");
		sb.append("BUYS\n");
		sortedBuys.stream().forEach(entry -> {
			sb.append(entry.getValue().getQuantity().toText());
			sb.append(" ");
			sb.append(entry.getValue().getTotalPrice());
			sb.append(" ");
			sb.append(entry.getValue().getRate().toPlainString());
			sb.append(" ");
			sb.append(" Seq:");
			sb.append(entry.getKey());
			sb.append("\n");
		});

		return sb.toString();
	}

	public static class OnOrderFullConsumed extends BusBase {
		public final List<RLOrder> origins;

		public OnOrderFullConsumed(List<RLOrder> origins) {
			this.origins = origins;
		}
	}
	
	public static class OnOrderPartialConsumed extends BusBase {
		public final List<RLOrder> orders;

		public OnOrderPartialConsumed(List<RLOrder> orders) {
			this.orders = orders;
		}
	}
	
	public static Orderbook newInstance(BotConfig botConfig, Config config) {
		Orderbook res = new Orderbook(config, botConfig);
		return res;
	}

	public static class OnAccOffersDone extends BusBase {
	}

}
