package com.mbcu.mmm.sequences;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.file.Files;
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
import java.util.stream.Stream;

import com.mbcu.mmm.models.internal.BefAf;
import com.mbcu.mmm.models.internal.BotConfig;
import com.mbcu.mmm.models.internal.RLOrder;
import com.mbcu.mmm.models.internal.RLOrder.Direction;
import com.mbcu.mmm.rx.RxBus;
import com.mbcu.mmm.rx.RxBusProvider;
import com.mbcu.mmm.sequences.state.State;
import com.mbcu.mmm.sequences.state.State.BroadcastPendings;
import com.mbcu.mmm.utils.MyLogger;
import com.ripple.core.coretypes.uint.UInt32;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class Orderbook extends Base{
	private final String fileName = "orderbook_%s.txt";
	private final int balancerInterval = 2; 
	
	private final BotConfig botConfig;
	private final ConcurrentHashMap<Integer, RLOrder> buys = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Integer, RLOrder> sels = new ConcurrentHashMap<>();
	private final RxBus bus = RxBusProvider.getInstance();
	private final CompositeDisposable disposables = new CompositeDisposable();
  private final Path path;
  private final AtomicInteger lastBalanced = new AtomicInteger(0);
  private final AtomicInteger ledgerValidated = new AtomicInteger(0);
  private final AtomicInteger ledgerClosed = new AtomicInteger(0);

	private Orderbook(BotConfig botConfig) {
		super(MyLogger.getLogger(String.format(Txc.class.getName())), null);
		this.botConfig = botConfig;
		path = Paths.get(String.format(fileName, botConfig.getPair().replaceAll("[/]", "_") ));
		disposables.add(
		bus.toObservable()
		.subscribeOn(Schedulers.newThread())
		.subscribeWith(new DisposableObserver<Object>(){

			@Override
			public void onNext(Object o) {
				if (o instanceof Common.OnLedgerClosed){
					lastBalanced.incrementAndGet();
					Common.OnLedgerClosed event = (Common.OnLedgerClosed) o;
					ledgerClosed.set(event.ledgerEvent.getClosed());
					ledgerValidated.set(event.ledgerEvent.getValidated());
					requestPendings();					
				}	
				else if (o instanceof Common.OnAccountOffers){					
					Common.OnAccountOffers event = (Common.OnAccountOffers) o;
					event.accOffs.forEach(accOff -> {
						shelve(accOff.getOrder(), accOff.getSeq());
					});
				}				
				else if (o instanceof Common.OnDifference){
					Common.OnDifference event = (Common.OnDifference) o;			
					event.bas.forEach(ba ->{
						shelve(ba.after, ba.befSeq);
					});
				}
				else if (o instanceof Common.OnOfferEdited){
					Common.OnOfferEdited event = (Common.OnOfferEdited) o;
					edit(event.ba, event.newSeq);
				}
				else if (o instanceof Common.OnOfferCanceled){
					Common.OnOfferCanceled event = (Common.OnOfferCanceled) o;
					remove(event.prevSeq.intValue());
				}
				else if (o instanceof State.BroadcastPendings){
					BroadcastPendings event = (BroadcastPendings) o;
					if (event.pair.equals(botConfig.getPair())){
						if (event.creates.isEmpty() && event.creates.isEmpty() && lastBalanced.get() > balancerInterval){
							lastBalanced.set(0);
							List<Entry<Integer, RLOrder>> sortedSels, sortedBuys;
							sortedSels = sortSels();
							sortedBuys = sortBuys();
							printOrderbook(sortedSels, sortedBuys);
							balancer(sortedSels, sortedBuys);						
						}
					}					
				}
			}

			@Override
			public void onError(Throwable e) {
				// TODO Auto-generated method stub				
			}

			@Override
			public void onComplete() {
				// TODO Auto-generated method stub				
			}
			
		}));		
	}
	
	private void printOrderbook (List<Entry<Integer, RLOrder>> sortedSels, List<Entry<Integer, RLOrder>> sortedBuys){
		dump(ledgerClosed.get(), ledgerValidated.get(), sortedSels, sortedBuys);
	}
	
	private void requestPendings(){
		bus.send(new Balancer.OnRequestNonOrderbookRLOrder(botConfig.getPair()));
	}
	
	
	private void balancer(List<Entry<Integer, RLOrder>> sortedSels, List<Entry<Integer, RLOrder>> sortedBuys){		
		// using pendings is unrealiable. 
		BigDecimal sumBuys  	= sum(Direction.BUY);	
		BigDecimal sumSels  	= sum(Direction.SELL);
		log(printLog(sumBuys, sumSels, count(Direction.BUY), count(Direction.SELL)));
		List<Integer> cans 		= new ArrayList<>();
		List<RLOrder> gens 		= new ArrayList<>(); 
	
		BigDecimal selsGap = margin(sumSels, Direction.SELL);
		if (selsGap.compareTo(BigDecimal.ZERO) < 0){
			cans.addAll(trim(sortedSels, selsGap, Direction.SELL));
		} else {
			gens.addAll(generate(sortedSels, selsGap, Direction.SELL));
		}
		BigDecimal buysGap = margin(sumBuys, Direction.BUY);
		if (buysGap.compareTo(BigDecimal.ZERO) < 0){
			cans.addAll(trim(sortedBuys, buysGap, Direction.BUY));
		} else {
			gens.addAll(generate(sortedBuys, buysGap, Direction.BUY));
		}
		cans.forEach(canSeq -> bus.send(new State.OnCancelReady(botConfig.getPair(), canSeq)));
		gens.forEach(rlo -> bus.send(new State.OnOrderReady(rlo)));
	}
	
	private List<RLOrder> generate(List<Entry<Integer, RLOrder>> sorteds, BigDecimal margin, Direction direction) {
		List<RLOrder> res;	
		if (sorteds.isEmpty()){
			if (direction == Direction.BUY){
				res = RLOrder.buildBuysSeed(new BigDecimal(botConfig.getStartMiddlePrice()), botConfig.getBuyGridLevels(), botConfig);
			} else {
				res = RLOrder.buildSelsSeed(new BigDecimal(botConfig.getStartMiddlePrice()), botConfig.getSellGridLevels(), botConfig);
			}
			return res;
		}	
		BigDecimal lastRate;
		if (direction == Direction.BUY){
			Collections.reverse(sorteds);	
			lastRate =  sorteds.get(0).getValue().getRate();
		} else {
			lastRate =  BigDecimal.ONE.divide(sorteds.get(0).getValue().getRate(), MathContext.DECIMAL64);
		}
		margin = margin.abs();
		int levels = margin
			.divide(direction == Direction.BUY ? botConfig.getBuyOrderQuantity() : botConfig.getSellOrderQuantity(), MathContext.DECIMAL32)
			.intValue();		
		res = direction == Direction.BUY ? 
				RLOrder.buildBuysSeed(lastRate, levels, botConfig) : RLOrder.buildSelsSeed(lastRate, levels, botConfig);
		return res;	
	}
	
	/**
	 * 
	 * @param sum
	 * @param direction
	 * @return negative if orderbook is bigger than config, positive if smaller
	 */
	private BigDecimal margin(BigDecimal sum, Direction direction){
		BigDecimal configTotalQuantity = direction == Direction.BUY ? botConfig.getTotalBuyQty() : botConfig.getTotalSelQty();
		return configTotalQuantity.subtract(sum);
	}
	
	private List<Integer> trim(List<Entry<Integer, RLOrder>> sorteds, BigDecimal margin, Direction direction){
		List<Integer> res = new ArrayList<>();
		margin = margin.abs();
		BigDecimal ref = BigDecimal.ZERO;	
		if (direction == Direction.BUY){
			Collections.reverse(sorteds);	
		} 				
		for (Entry<Integer, RLOrder> e : sorteds){
			BigDecimal newRef = ref.add(direction == Direction.BUY ? e.getValue().getQuantity().value() : e.getValue().getTotalPrice().value());
			if (newRef.compareTo(margin) > 0){
				break;
			} else{
				ref = newRef;
				res.add(e.getKey());
			}
		}
		return res;	
	}
		
	private String printLog(BigDecimal sumBuys, BigDecimal sumSels, int countBuys, int countSels){
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
	
	private int count(Direction direction){
		int res = direction == Direction.BUY ? buys.size() : sels.size();
		return res;
	}
	
	private BigDecimal sum(Direction direction){		
		Stream<RLOrder> orderbook;
		Function<RLOrder, BigDecimal> fun;
		if (direction == Direction.BUY){
			orderbook = buys.values().stream();
			fun	 			= rlOrder -> rlOrder.getQuantity().value();
		} else {
			orderbook = sels.values().stream();
			fun 			= rlOrder -> rlOrder.getTotalPrice().value();
		}				
		return orderbook
				.map(fun)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}
	
	private void edit(BefAf ba, UInt32 newSeq){
		Boolean pairMatched = pairMatched(ba.after);
		if (pairMatched == null){
			return;
		}
		remove(ba.befSeq.intValue());
		insert(ba.after, newSeq.intValue(), pairMatched);
	}
	
	private void shelve(RLOrder after , int seq){
		Boolean pairMatched = pairMatched(after);
		if (pairMatched == null){
			return;
		}
		if (after.getQuantity().value().compareTo(BigDecimal.ZERO) == 0){
			remove(seq);
			return;
		}
		insert(after, seq, pairMatched);
	}

	private void insert(RLOrder after, int seq, Boolean isAligned){	
		if (isAligned){
			buys.put(seq, after);
		} else {
			sels.put(seq, after);
		}
	}
	
	// Because it's map, it's O(1) and idempotent
	private void remove(int seq){
		buys.remove(seq);
		sels.remove(seq);		
	}
	
	private void shelve(RLOrder after, UInt32 seq){
		shelve(after, seq.intValue());
	}
	
	private List<Entry<Integer, RLOrder>> sortBuys(){
    Set<Entry<Integer, RLOrder>> entries = buys.entrySet();
    List<Entry<Integer, RLOrder>> res = new ArrayList<Entry<Integer, RLOrder>>(entries);   
    Collections.sort(res, Collections.reverseOrder(obMapComparator));
    return res;
	}
	
	private List<Entry<Integer, RLOrder>> sortSels(){
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
	private Boolean pairMatched (RLOrder in){
		return in.getCpair().isMatch(this.botConfig.getPair());
	}
	
	private void dump(int ledgerClosed, int ledgerValidated, List<Entry<Integer, RLOrder>> sortedSels, List<Entry<Integer, RLOrder>> sortedBuys){
		StringBuffer sb = new StringBuffer("Ledger closed : ");
		sb.append(ledgerClosed);
		sb.append(" ");
		sb.append("Ledger validated : ");
		sb.append(ledgerValidated);
		sb.append("\n\n");
		sb.append("SELLS\n");
    sortedSels.stream().forEach(entry ->{
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
		byte[] strToBytes = sb.toString().getBytes();	
    try {
			Files.write(path, strToBytes);
		} catch (IOException e) {
			e.printStackTrace();
		}
  }

	public static Orderbook newInstance(BotConfig botConfig){
		Orderbook res = new Orderbook(botConfig);
		return res;
	}
	


}
