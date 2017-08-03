package com.mbcu.mmm.models.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.mbcu.mmm.models.Base;
import com.mbcu.mmm.rx.RxBus;
import com.mbcu.mmm.rx.RxBusProvider;
import com.mbcu.mmm.sequences.Common;
import com.mbcu.mmm.sequences.Common.OnAccountOffers;
import com.mbcu.mmm.sequences.Common.OnLedgerClosed;

import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class Orderbook extends Base{
	
	private final BotConfig botConfig;
	private final ConcurrentHashMap<Integer, RLOrder> buys = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Integer, RLOrder> sels = new ConcurrentHashMap<>();
	private final RxBus bus = RxBusProvider.getInstance();
	private final CompositeDisposable disposables = new CompositeDisposable();
  private final Path path;

	private Orderbook(BotConfig botConfig) {
		super();
		this.botConfig = botConfig;
		path = Paths.get("orderbook_" + botConfig.pair.replaceAll("[/]", "_") + ".txt");
		disposables.add(
		bus.toObservable()
		.subscribeOn(Schedulers.newThread())
		.subscribeWith(new DisposableObserver<Object>(){

			@Override
			public void onNext(Object o) {
				if (o instanceof Common.OnAccountOffers){					
					OnAccountOffers event = (OnAccountOffers) o;
					event.accOffs.forEach(accOff -> {
						Boolean isPairMatched = pairMatched(accOff.getOrder());
						if (isPairMatched == null){
							return;
						}
						push(accOff.getSeq(), isPairMatched, accOff.getOrder());	
					});
				}				
				else if (o instanceof Common.OnLedgerClosed){
					OnLedgerClosed event = (OnLedgerClosed) o;
					if (event.ledgerEvent.closed % 2 == 0 ){
						List<RLOrder> sortedSels = new ArrayList<>(sels.values());
						sortedSels.sort((RLOrder o1, RLOrder o2) -> o2.getRate().compareTo(o1.getRate()));
						List<RLOrder> sortedBuys = new ArrayList<>(buys.values());
						sortedBuys.sort((RLOrder o1, RLOrder o2) -> o1.getRate().compareTo(o2.getRate()));
				    
						dump(event.ledgerEvent.closed, event.ledgerEvent.validated, sortedSels, sortedBuys);
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
	
	/** 
	 * @param in
	 * @return null if not matched, true if aligned, false if reversed
	 */
	private Boolean pairMatched (RLOrder in){
		if (in.getPair().equals(this.botConfig.pair)){
			return true;
		}
		else if (in.getReversePair().equals(this.botConfig.pair)){
			return false;
		}
		return null;
	}
	
	private void dump(int ledgerClosed, int ledgerValidated, List<RLOrder> sortedSels, List<RLOrder> sortedBuys){
		StringBuffer sb = new StringBuffer("Ledger closed : ");
		sb.append(ledgerClosed);
		sb.append(" ");
		sb.append("Ledger validated : ");
		sb.append(ledgerValidated);
		sb.append("\n\n");
		sb.append("SELLS\n");
    sortedSels.stream().forEach(order ->{
			sb.append(orderbookLine(order));		
			sb.append("\n");
		});    
		sb.append("BUYS\n");
		buys.values().stream().forEach(order -> {
			sb.append(orderbookLine(order));		
			sb.append("\n");
		});
		byte[] strToBytes = sb.toString().getBytes();	
    try {
			Files.write(path, strToBytes);
		} catch (IOException e) {
			e.printStackTrace();
		}
    }

	private String orderbookLine(RLOrder order){
		StringBuffer sb = new StringBuffer(order.getQuantity().toText());
		sb.append(" ");
		sb.append(order.getTotalPrice());
		sb.append(" ");
		sb.append(order.getRate().toPlainString());
		return sb.toString();
	}
	
	private void push (int seq, boolean isAligned, RLOrder in){
		if (isAligned){
			buys.put(seq, in);
		}
		else {
			sels.put(seq, in.reverse());
		}		
	}

	private void balancer(){

	}
	
	
	public static Orderbook newInstance(BotConfig botConfig){
		Orderbook res = new Orderbook(botConfig);
		return res;
	}
	
	@Override
	public String stringify() {
		return botConfig.pair;
	}

}
