package com.mbcu.mmm.sequences;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Stream;

import com.mbcu.mmm.helpers.TAccountOffer;
import com.mbcu.mmm.models.internal.BefAf;
import com.mbcu.mmm.models.internal.BotConfig;
import com.mbcu.mmm.models.internal.BuySellRateTuple;
import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.models.internal.RLOrder;
import com.mbcu.mmm.models.internal.RLOrder.Direction;
import com.mbcu.mmm.rx.BusBase;
import com.mbcu.mmm.rx.RxBus;
import com.mbcu.mmm.rx.RxBusProvider;
import com.mbcu.mmm.sequences.state.State;
import com.mbcu.mmm.sequences.state.State.BroadcastPendings;
import com.mbcu.mmm.sequences.state.State.OnOrderReady;
import com.mbcu.mmm.utils.MyLogger;
import com.ripple.core.coretypes.uint.UInt32;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class Orderbook extends Base {
	// private final int seedMidThreshold = 8;

	private final BotConfig botConfig;
	private final ConcurrentHashMap<Integer, RLOrder> buys = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Integer, RLOrder> sels = new ConcurrentHashMap<>();
	private final RxBus bus = RxBusProvider.getInstance();
	private final CompositeDisposable disposables = new CompositeDisposable();
	private final CompositeDisposable accountOffersDispo = new CompositeDisposable();
	private final AtomicInteger lastBalanced = new AtomicInteger(0);
	private final AtomicInteger ledgerValidated = new AtomicInteger(0);
	private final AtomicInteger ledgerClosed = new AtomicInteger(0);
	private BigDecimal worstSel, worstBuy;

	private Orderbook(Config config, BotConfig botConfig) {
		super(MyLogger.getLogger(String.format(Txc.class.getName())), config);
		this.botConfig = botConfig;
		this.worstBuy = botConfig.getStartMiddlePrice();
		this.worstSel = botConfig.getStartMiddlePrice();

		accountOffersDispo
				.add(bus.toObservable().subscribeOn(Schedulers.newThread()).subscribeWith(new DisposableObserver<Object>() {

					@Override
					public void onNext(Object o) {
						BusBase base = (BusBase) o;
						if (base instanceof Common.OnAccountOffers) {
							Common.OnAccountOffers event = (Common.OnAccountOffers) o;
							boolean buyMatched = false;
							boolean selMatched = false;
							Boolean pairMatched = null;
							for (TAccountOffer t : event.accOffs) {
								pairMatched = pairMatched(t.getOrder());
								if (pairMatched != null) {
									if (pairMatched) {
										buyMatched = true;
									} else {
										selMatched = true;
									}
									shelve(t.getOrder(), t.getSeq(), pairMatched);
								}
							}
							if (buyMatched || selMatched) {
								BuySellRateTuple worstBuySel = RLOrder.worstRates(buys, sels, worstBuy, worstSel, botConfig);
								worstBuy = worstBuySel.getBuyRate();
								worstSel = worstBuySel.getSelRate();
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

		disposables
				.add(bus.toObservable().subscribeOn(Schedulers.newThread()).subscribeWith(new DisposableObserver<Object>() {

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
								boolean buyMatched = false;
								boolean selMatched = false;
								for (BefAf ba : event.bas) {
									Boolean pairMatched = pairMatched(ba.after);
									if (pairMatched != null) {
										if (pairMatched) {
											buyMatched = true;
										} else {
											selMatched = true;
										}
										shelve(ba.after, ba.befSeq, pairMatched);
									}
								}
								if (buyMatched || selMatched) {
									BuySellRateTuple worstBuySel = RLOrder.worstRates(buys, sels, worstBuy, worstSel, botConfig);
									worstBuy = worstBuySel.getBuyRate();
									worstSel = worstBuySel.getSelRate();
								}
							} else if (base instanceof Common.OnOfferEdited) {
								Common.OnOfferEdited event = (Common.OnOfferEdited) o;
								Boolean pairMatched = pairMatched(event.ba.after);
								if (pairMatched != null) {
									edit(event.ba, event.newSeq, pairMatched);
									BuySellRateTuple worstBuySel = RLOrder.worstRates(buys, sels, worstBuy, worstSel, botConfig);
									worstBuy = worstBuySel.getBuyRate();
									worstSel = worstBuySel.getSelRate();
								}
							} else if (base instanceof Common.OnOfferCanceled) {
								Common.OnOfferCanceled event = (Common.OnOfferCanceled) o;
								Boolean pairMatched = remove(event.prevSeq.intValue());
								if (pairMatched != null) {
									BuySellRateTuple worstBuySel = RLOrder.worstRates(buys, sels, worstBuy, worstSel, botConfig);
									worstBuy = worstBuySel.getBuyRate();
									worstSel = worstBuySel.getSelRate();
								}
							} else if (base instanceof State.BroadcastPendings) {
								BroadcastPendings event = (BroadcastPendings) o;
								if (event.pair.equals(botConfig.getPair())) {
									if (event.creates.isEmpty() && event.cancels.isEmpty()
											&& lastBalanced.get() >= config.getIntervals().getBalancer()) {
										lastBalanced.set(0);
										List<Entry<Integer, RLOrder>> sortedSels, sortedBuys;
										sortedSels = RLOrder.sortSels(sels, false);
										sortedBuys = RLOrder.sortBuys(buys, false);
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
			bus.send(new Notifier.RequestEmailNotice(warning, botConfig.getPair(), System.currentTimeMillis()));
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
			res.addAll(direction == Direction.BUY ? RLOrder.buildBuysSeed(worstBuy, levels, botConfig, super.LOGGER)
					: RLOrder.buildSelsSeed(worstSel, levels, botConfig));
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

	private List<Integer> trim(List<Entry<Integer, RLOrder>> sorteds, BigDecimal margin, Direction direction) {
		List<Integer> res = new ArrayList<>();
		margin = margin.abs();
		BigDecimal ref = BigDecimal.ZERO;
		if (direction == Direction.BUY) {
			Collections.reverse(sorteds);
		}
		for (Entry<Integer, RLOrder> e : sorteds) {
			BigDecimal newRef = ref
					.add(direction == Direction.BUY ? e.getValue().getQuantity().value() : e.getValue().getTotalPrice().value());
			if (newRef.compareTo(margin) > 0) {
				break;
			} else {
				ref = newRef;
				res.add(e.getKey());
			}
		}
		return res;
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
		Stream<RLOrder> orderbook;
		Function<RLOrder, BigDecimal> fun;
		if (direction == Direction.BUY) {
			orderbook = buys.values().stream();
			fun = rlOrder -> rlOrder.getQuantity().value();
		} else {
			orderbook = sels.values().stream();
			fun = rlOrder -> rlOrder.getTotalPrice().value();
		}
		return orderbook.map(fun).reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private boolean edit(BefAf ba, UInt32 newSeq, boolean pairMatched) {
		remove(ba.befSeq.intValue());
		insert(ba.after, newSeq.intValue(), pairMatched);
		return true;
	}

	private boolean shelve(RLOrder after, int seq, boolean isAligned) {
		if (after.getQuantity().value().compareTo(BigDecimal.ZERO) == 0) {
			remove(seq);
		} else {
			insert(after, seq, isAligned);
		}
		return true;
	}

	private void insert(RLOrder after, int seq, Boolean isAligned) {
		if (isAligned) {
			buys.put(seq, after);
		} else {
			sels.put(seq, after);
		}
	}

	// Because it's map, it's O(1) and idempotent
	private Boolean remove(int seq) {
		RLOrder a = buys.remove(seq);
		if (a != null) {
			return true;
		}
		RLOrder b = sels.remove(seq);
		if (b != null) {
			return false;
		}
		return null;
	}

	private void shelve(RLOrder after, UInt32 seq, boolean isAligned) {
		shelve(after, seq.intValue(), isAligned);
	}

	/**
	 * @param in
	 * @return null if not matched, true if aligned, false if reversed
	 */
	private Boolean pairMatched(RLOrder in) {
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

	public static Orderbook newInstance(BotConfig botConfig, Config config) {
		Orderbook res = new Orderbook(config, botConfig);
		return res;
	}

	public static class OnAccOffersDone extends BusBase {
	}

}
