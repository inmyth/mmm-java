package com.mbcu.mmm.sequences;

import java.math.BigDecimal;
import java.math.MathContext;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import com.mbcu.mmm.api.ApiManager;
import com.mbcu.mmm.models.dataapi.AccountBalance;
import com.mbcu.mmm.models.dataapi.Balance;
import com.mbcu.mmm.models.internal.AgBalance;
import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.models.internal.NameIssuer;
import com.mbcu.mmm.rx.BusBase;
import com.mbcu.mmm.sequences.Common.OnLedgerClosed;
import com.mbcu.mmm.utils.MyLogger;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Dataapi extends Base{
	private CompositeDisposable disposable = new CompositeDisposable();
	private ApiManager apiManager;
  private AgBalance  lastAgBalance = new AgBalance();
  private AtomicInteger ledgerValidated = new AtomicInteger(-1);
	
	public Dataapi(Config config) {
		super(MyLogger.getLogger(Dataapi.class.getName()), config);
		apiManager = new ApiManager(config);
		
		disposable.add(
				bus.toObservable().subscribeOn(Schedulers.newThread())
				.subscribeWith(new DisposableObserver<Object>() {

					@Override
					public void onNext(Object o) {
						BusBase base = (BusBase) o;
						try{						
							if (base instanceof Common.OnLedgerClosed) {
								OnLedgerClosed event = (OnLedgerClosed) base;
								if (ledgerValidated.get() == -1 
										|| event.ledgerEvent.getValidated() - ledgerValidated.get() >= config.getIntervals().getAccountBalance()){
									ledgerValidated.set(event.ledgerEvent.getValidated());
									callBalances();
								}
							}
						}catch (Exception e) {
							MyLogger.exception(LOGGER, base.toString(), e);		
							throw e;		
						}
					}

					@Override
					public void onError(Throwable e) {
						log(e.getMessage(), Level.SEVERE);
					}

					@Override
					public void onComplete() {}
					
				})
				
				);
		
		
	}
	
	private void callBalances(){	
  	ZonedDateTime now = ZonedDateTime.now( ZoneOffset.UTC );
  	final long nowTs 	= now.toEpochSecond();  	
    Call<AccountBalance> callBalances = apiManager.getService().getBalances(config.getCredentials().getAddress(), now.toString());
		callBalances.enqueue(new Callback<AccountBalance>() {
			
			@Override
			public void onResponse(Call<AccountBalance> arg0, Response<AccountBalance> response) {
				AccountBalance ab = response.body();
				if (ab == null){
					return;
				}
				AgBalance newAgBalance 		= AgBalance.from(ab, nowTs);
				logBalance(newAgBalance);
				lastAgBalance = newAgBalance;
			}
			
			@Override
			public void onFailure(Call<AccountBalance> arg0, Throwable arg1) {
				// TODO Auto-generated method stub
				
			}
		});
		
	}
	
	private Map<NameIssuer, BigDecimal> deltaBalance(AgBalance newAg){
		final Map<NameIssuer, BigDecimal> res = new HashMap<>();
		Map<NameIssuer, Balance> lastData = lastAgBalance.getData();
		
		newAg.getData().entrySet().stream().forEach(e -> {
			BigDecimal newValue = new BigDecimal(e.getValue().getValue());
			Balance lastBalance = lastData.get(e.getKey());
			if (lastBalance != null){
				BigDecimal oldValue = new BigDecimal(lastBalance.getValue());
				BigDecimal d 				= newValue.subtract(oldValue, MathContext.DECIMAL32);
				res.put(e.getKey(), d);
			}			
		});
		return res;	
	}
	
	private void logBalance(AgBalance newAg) {
		Map<NameIssuer, BigDecimal> deltas = deltaBalance(newAg);
		StringBuilder t = new StringBuilder("\n");
		t.append("Balance ");
		t.append(newAg.getDt().toString());
		t.append("\n");
		t.append("pair | now | change");
		t.append("\n");
		newAg.getData().entrySet().stream().forEach(e -> {
			t.append(e.getKey());
			t.append(" ");
			t.append(e.getValue().getValue());
			t.append(" ");
			BigDecimal delta = deltas.get(e.getKey());
			t.append(delta != null ? delta.stripTrailingZeros() : "-");
			t.append("\n");

		});
		t.append("\n");
		log(t.toString(), Level.FINER);
	}
	
	public static Dataapi newInstance(Config config){
		Dataapi res = new Dataapi(config);
		return res;
	}


}
