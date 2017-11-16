package com.mbcu.mmm.api;

import com.mbcu.mmm.models.dataapi.AccountBalance;
import com.mbcu.mmm.models.internal.Config;

import retrofit2.Call;
import retrofit2.GsonConverterFactory;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public class ApiManager {
	private Retrofit REST_ADAPTER;
	private DataApiService DATAAPI_SERVICE;

	public interface DataApiService {

		@GET("/v2/accounts/{address}/balances")
		Call<AccountBalance> getBalances(@Path("address") String address, @Query("date") String date);

	}

	public ApiManager(Config config) {
		REST_ADAPTER = new Retrofit.Builder().baseUrl(config.getDatanet())
				.addConverterFactory(GsonConverterFactory.create()).build();
		DATAAPI_SERVICE = REST_ADAPTER.create(DataApiService.class);
	}

	public DataApiService getService() {
		return DATAAPI_SERVICE;
	}

}
