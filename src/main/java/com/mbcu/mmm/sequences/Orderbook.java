package com.mbcu.mmm.sequences;

import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Stream;

import com.mbcu.mmm.helpers.TAccountOffer;
import com.mbcu.mmm.models.internal.BefAf;
import com.mbcu.mmm.models.internal.BotConfig;
import com.mbcu.mmm.models.internal.RLOrder;
import com.mbcu.mmm.models.internal.RLOrder.Direction;
import com.mbcu.mmm.rx.BusBase;
import com.mbcu.mmm.rx.RxBus;
import com.mbcu.mmm.rx.RxBusProvider;
import com.mbcu.mmm.sequences.state.State;
import com.mbcu.mmm.sequences.state.State.BroadcastPendings;
import com.mbcu.mmm.utils.MyLogger;
import com.ripple.core.coretypes.uint.UInt32;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class Orderbook extends Base {
	private final String fileName = "orderbook_%s.txt";
	private final int balancerInterval = 4;
	private final int seedMidThreshold = 8;
	private final int hardOutThreshold = 5;

	private final BotConfig botConfig;
	private final ConcurrentHashMap<Integer, RLOrder> buys = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Integer, RLOrder> sels = new ConcurrentHashMap<>();
	private final RxBus bus = RxBusProvider.getInstance();
	private final CompositeDisposable disposables = new CompositeDisposable();
	private final Path path;
	private final AtomicInteger lastBalanced = new AtomicInteger(0);
	private final AtomicInteger ledgerValidated = new AtomicInteger(0);
	private final AtomicInteger ledgerClosed = new AtomicInteger(0);
	private BigDecimal worstSel, worstBuy;

	private Orderbook(BotConfig botConfig) {
		super(MyLogger.getLogger(String.format(Txc.class.getName())), null);
		this.botConfig = botConfig;
		this.worstBuy = botConfig.getStartMiddlePrice();
		this.worstSel = botConfig.getStartMiddlePrice();
		path = Paths.get(String.format(fileName, botConfig.getPair().replaceAll("[/]", "_")));
		disposables.add(bus.toObservable().subscribeOn(Schedulers.newThread()).subscribeWith(new DisposableObserver<Object>() {

			@Override
			public void onNext(Object o) {
				BusBase base = (BusBase) o;
				try{
				if (base instanceof Common.OnLedgerClosed) {
					lastBalanced.incrementAndGet();
					Common.OnLedgerClosed event = (Common.OnLedgerClosed) o;
					ledgerClosed.set(event.ledgerEvent.getClosed());
					ledgerValidated.set(event.ledgerEvent.getValidated());
					requestPendings();
				} else if (base instanceof Common.OnAccountOffers) {
					Common.OnAccountOffers event = (Common.OnAccountOffers) o;
					boolean buyMatched = false; 
					boolean selMatched = false;
					for (TAccountOffer t : event.accOffs) {
						Boolean pairMatched = pairMatched(t.getOrder());
						if (pairMatched != null) {
							if (pairMatched){
								buyMatched = true;
							} else {
								selMatched = true;
							}
							shelve(t.getOrder(), t.getSeq(), pairMatched);
						}
					}
					if (buyMatched) {
						worstRates(Direction.BUY);
					} 
					if (selMatched){
						worstRates(Direction.SELL);
					}
				} else if (base instanceof Common.OnDifference) {
					Common.OnDifference event = (Common.OnDifference) o;
					boolean buyMatched = false; 
					boolean selMatched = false;
					for (BefAf ba : event.bas) {
						Boolean pairMatched = pairMatched(ba.after);
						if (pairMatched != null) {
							if (pairMatched){
								buyMatched = true;
							} else {
								selMatched = true;
							}
							shelve(ba.after, ba.befSeq, pairMatched);
						}
					}
					if (buyMatched) {
						worstRates(Direction.BUY);
					} 
					if (selMatched){
						worstRates(Direction.SELL);
					}
				} else if (base instanceof Common.OnOfferEdited) {
					Common.OnOfferEdited event = (Common.OnOfferEdited) o;
					Boolean pairMatched = pairMatched(event.ba.after);
					if (pairMatched != null) {
						edit(event.ba, event.newSeq, pairMatched);
						worstRates(pairMatched ? Direction.BUY : Direction.SELL);
					}
				} else if (base instanceof Common.OnOfferCanceled) {
					Common.OnOfferCanceled event = (Common.OnOfferCanceled) o;
					Boolean pairMatched = remove(event.prevSeq.intValue());
					if (pairMatched != null) {
						worstRates(pairMatched ? Direction.BUY : Direction.SELL);
					}
				} else if (base instanceof State.BroadcastPendings) {
					BroadcastPendings event = (BroadcastPendings) o;
					if (event.pair.equals(botConfig.getPair())) {
						if (event.creates.isEmpty() && event.cancels.isEmpty() && lastBalanced.get() >= balancerInterval) {
							lastBalanced.set(0);
							List<Entry<Integer, RLOrder>> sortedSels, sortedBuys;
							sortedSels = sortSels();
							sortedBuys = sortBuys();
							printOrderbook(sortedSels, sortedBuys);
							balancer(sortedSels, sortedBuys);
						}
					}
				}
				} catch (Exception e) {
					MyLogger.exception(LOGGER, base.toString(), e);		
					throw e;				}
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
//		MyUtils.toFile(ob, path);
		log(ob,  Level.FINER);
	}
	
	private void requestPendings() {
		bus.send(new Balancer.OnRequestNonOrderbookRLOrder(botConfig.getPair()));
	}

	private void worstRates(Direction direction) {		
		List<Entry<Integer, RLOrder>> sorted;
		if (direction == Direction.BUY){
			sorted = sortBuys();
			if (sorted.isEmpty()) {
				worstBuy = worstBuy.subtract(botConfig.getGridSpace());
			} else {
				Collections.reverse(sorted);
				worstBuy = sorted.get(0).getValue().getRate();
			}
		} else{
			sorted = sortSels();
			if (sorted.isEmpty()) {	
				worstSel = worstSel.add(botConfig.getGridSpace());
			} else {
				worstSel = BigDecimal.ONE.divide(sorted.get(0).getValue().getRate(), MathContext.DECIMAL64);
			}
		}
	}

	private void balancer(List<Entry<Integer, RLOrder>> sortedSels, List<Entry<Integer, RLOrder>> sortedBuys) {
		// using pendings is unrealiable.
		BigDecimal sumBuys = sum(Direction.BUY);
		BigDecimal sumSels = sum(Direction.SELL);
		log(printLog(sumBuys, sumSels, count(Direction.BUY), count(Direction.SELL)));
		List<RLOrder> gens = new ArrayList<>();

		BigDecimal selsGap = margin(sumSels, Direction.SELL);
		if (selsGap.compareTo(BigDecimal.ZERO) >= 0) {
			gens.addAll(generate(sortedSels, selsGap, Direction.SELL));
		} 		

		BigDecimal buysGap = margin(sumBuys, Direction.BUY);
		if (buysGap.compareTo(BigDecimal.ZERO) >= 0) {
			gens.addAll(generate(sortedBuys, buysGap, Direction.BUY));
		} 

		gens.forEach(rlo -> bus.send(new State.OnOrderReady(rlo)));
	}

	private List<RLOrder> generate(List<Entry<Integer, RLOrder>> sorteds, BigDecimal margin, Direction direction) {
		List<RLOrder> res = new ArrayList<>();
		margin = margin.abs();	
		int levels = margin.divide(direction == Direction.BUY ? botConfig.getBuyOrderQuantity() : botConfig.getSellOrderQuantity(), MathContext.DECIMAL32).intValue();
		int configLevels = direction == Direction.BUY ? botConfig.getBuyGridLevels() : botConfig.getSellGridLevels();
		if (direction == Direction.SELL){
			Collections.reverse(sorteds);
		}
		
		int inserted = 0;		
		for (int i = 0; i < sorteds.size() - 1; i++){
			BigDecimal p, delta, q;
			int locLevels = 0;
			
			if (direction == Direction.BUY){
				p = sorteds.get(i).getValue().getRate();
				q = sorteds.get(i + 1).getValue().getRate();
				delta = p.subtract(q);
			} else {
				p = BigDecimal.ONE.divide(sorteds.get(i).getValue().getRate(), MathContext.DECIMAL64);
				q = BigDecimal.ONE.divide(sorteds.get(i + 1).getValue().getRate(), MathContext.DECIMAL64);
  			delta = q.subtract(p);
			}		
			locLevels = delta.divide(botConfig.getGridSpace(), MathContext.DECIMAL64).intValue() - 1;				
			if (locLevels > 0 && levels > 0 && inserted < seedMidThreshold){
				levels 		= levels - locLevels;
				inserted  = inserted + locLevels;
				res.addAll(direction == Direction.BUY ? RLOrder.buildBuysSeed(p, locLevels, botConfig) : RLOrder.buildSelsSeed(p, locLevels, botConfig));
			}		
			if (levels <= 0 || inserted > seedMidThreshold){
				break;
			}
		}				
		if (levels > 0){		
			res.addAll(direction == Direction.BUY ? RLOrder.buildBuysSeed(worstBuy, levels, botConfig) : RLOrder.buildSelsSeed(worstSel, levels, botConfig));
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
			BigDecimal newRef = ref.add(direction == Direction.BUY ? e.getValue().getQuantity().value() : e.getValue().getTotalPrice().value());
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
		if (a != null){
			return true;
		}
		RLOrder b = sels.remove(seq);
		if (b != null){
			return false;
		}
		return null;
	}

	private void shelve(RLOrder after, UInt32 seq, boolean isAligned) {
		shelve(after, seq.intValue(), isAligned);
	}

	private List<Entry<Integer, RLOrder>> sortBuys() {
		Set<Entry<Integer, RLOrder>> entries = buys.entrySet();
		List<Entry<Integer, RLOrder>> res = new ArrayList<Entry<Integer, RLOrder>>(entries);
		Collections.sort(res, Collections.reverseOrder(obMapComparator));
		return res;
	}

	private List<Entry<Integer, RLOrder>> sortSels() {
		Set<Entry<Integer, RLOrder>> entries = sels.entrySet();
		List<Entry<Integer, RLOrder>> res = new ArrayList<Entry<Integer, RLOrder>>(entries);
		Collections.sort(res, obMapComparator);
		return res;
	}

	public Comparator<Entry<Integer, RLOrder>> obMapComparator = new Comparator<Entry<Integer, RLOrder>>() {

		@Override
		public int compare(Entry<Integer, RLOrder> e1, Entry<Integer, RLOrder> e2) {
			return e1.getValue().getRate().compareTo(e2.getValue().getRate());
		}
	};

	/**
	 * @param in
	 * @return null if not matched, true if aligned, false if reversed
	 */
	private Boolean pairMatched(RLOrder in) {
		return in.getCpair().isMatch(this.botConfig.getPair());
	}

	private String dump(int ledgerClosed, int ledgerValidated, List<Entry<Integer, RLOrder>> sortedSels, List<Entry<Integer, RLOrder>> sortedBuys) {
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

	public static Orderbook newInstance(BotConfig botConfig) {
		Orderbook res = new Orderbook(botConfig);
		return res;
	}

}
