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
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.mbcu.mmm.models.internal.BefAf;
import com.mbcu.mmm.models.internal.BotConfig;
import com.mbcu.mmm.models.internal.RLOrder;
import com.mbcu.mmm.models.internal.RLOrder.Direction;
import com.mbcu.mmm.rx.RxBus;
import com.mbcu.mmm.rx.RxBusProvider;
import com.mbcu.mmm.sequences.state.State;
import com.mbcu.mmm.sequences.state.State.BroadcastTxcRLOrder;
import com.mbcu.mmm.utils.MyLogger;
import com.ripple.core.coretypes.uint.UInt32;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class Orderbook extends Base{
	private final String fileName = "orderbook_%s.txt";
	
	private final BotConfig botConfig;
	private final ConcurrentHashMap<Integer, RLOrder> buys = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Integer, RLOrder> sels = new ConcurrentHashMap<>();
	private final RxBus bus = RxBusProvider.getInstance();
	private final CompositeDisposable disposables = new CompositeDisposable();
  private final Path path;

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
					Common.OnLedgerClosed event = (Common.OnLedgerClosed) o;
					if (event.ledgerEvent.getClosed() % 2 == 0 ){					
						List<Entry<Integer, RLOrder>> sortedSels, sortedBuys;
						sortedSels = sortSels();
						sortedBuys = sortBuys();
						dump(event.ledgerEvent.getClosed(), event.ledgerEvent.getValidated(), sortedSels, sortedBuys);
						requestPendings();
//						dumpWithSeq(event.ledgerEvent.getClosed(), event.ledgerEvent.getValidated());
					}
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
					remove(event.newSeq.intValue());
				}
				else if (o instanceof State.BroadcastTxcRLOrder){
					BroadcastTxcRLOrder event = (BroadcastTxcRLOrder) o;
					if (event.pair.equals(botConfig.getPair())){
						balancer(event.outbounds);						
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
	
	private void requestPendings(){
		bus.send(new Balancer.OnRequestNonOrderbookRLOrder(botConfig.getPair()));
	}
	
	
	private void balancer(List<RLOrder> pendings){			
		BigDecimal sumBuys  = sum(pendings.stream(), Direction.BUY);	
		BigDecimal sumSels  = sum(pendings.stream(), Direction.SELL);
		
		int compare = sumBuys.compareTo(botConfig.getTotalBuyQty());
		
		if (compare < 0){
			
		} else if (compare > 0){
			int levels = sumBuys.subtract(botConfig.getTotalBuyQty()).divide(botConfig.getBuyOrderQuantity(), MathContext.DECIMAL64).intValue();
			levels = Math.abs(levels);
			
		}
		
		log(printLog(sumBuys, sumSels, count(pendings, Direction.BUY), count(pendings, Direction.SELL)));
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
	
	private int count(List<RLOrder> pendings, Direction direction){
		int res = direction == Direction.BUY ? buys.size() : sels.size();
		return res + pendings.size();
	}
	
	private BigDecimal sum(Stream<RLOrder> pendings, Direction direction){		
		Predicate<RLOrder> pred;
		Stream<RLOrder> orderbook;
		Function<RLOrder, BigDecimal> fun;
		if (direction == Direction.BUY){
			pred			= rlOrder -> rlOrder.getPair().equals(botConfig.getPair());
			orderbook = buys.values().stream();
			fun	 			= rlOrder -> rlOrder.getQuantity().value();
			return Stream.concat(pendings.filter(pred), orderbook)	
					.map(fun)
					.reduce(BigDecimal.ZERO, BigDecimal::add);
		} else {
			pred 			= rlOrder -> rlOrder.getReversePair().equals(botConfig.getPair());
			orderbook = sels.values().stream();
			fun 			= rlOrder -> rlOrder.getTotalPrice().value();
			return Stream.concat(pendings.filter(pred).map(rlOrder -> rlOrder.getQuantity().value())
					, orderbook.map(fun)
					)
			.reduce(BigDecimal.ZERO, BigDecimal::add);
		}				

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
		if (in.getPair().equals(this.botConfig.getPair())){
			return true;
		}
		else if (in.getReversePair().equals(this.botConfig.getPair())){
			return false;
		}
		return null;
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
