package com.mbcu.mmm.sequences;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.concurrent.LinkedBlockingQueue;

import com.mbcu.mmm.api.ApiManager;
import com.mbcu.mmm.models.dataapi.AccountBalance;
import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.rx.BusBase;
import com.mbcu.mmm.utils.GsonUtils;
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
  private LinkedBlockingQueue<AccountBalance> accountBalances = new LinkedBlockingQueue<>(10);
	
	public Dataapi(Config config) {
		super(MyLogger.getLogger(Dataapi.class.getName()), config);
		apiManager = new ApiManager(config);
		
		disposable.add(
				bus.toObservable().subscribeOn(Schedulers.newThread())
				.subscribeWith(new DisposableObserver<Object>() {

					@Override
					public void onNext(Object o) {
						BusBase base = (BusBase) o;
						if (base instanceof Common.OnLedgerClosed) {
							callBalances();
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
				})
				
				);
		
		
	}
	
	private void callBalances(){	
  	ZonedDateTime now = ZonedDateTime.now( ZoneOffset.UTC );
    Call<AccountBalance> callBalances = apiManager.getService().getBalances(config.getCredentials().getAddress(), now.toString());
		callBalances.enqueue(new Callback<AccountBalance>() {
			
			@Override
			public void onResponse(Call<AccountBalance> arg0, Response<AccountBalance> response) {
				AccountBalance ab = response.body();
				ab.getBalances().forEach(System.out::println);
				
			}
			
			@Override
			public void onFailure(Call<AccountBalance> arg0, Throwable arg1) {
				// TODO Auto-generated method stub
				
			}
		});
		
	}
	
	
	public static Dataapi newInstance(Config config){
		Dataapi res = new Dataapi(config);
		return res;
	}


}
