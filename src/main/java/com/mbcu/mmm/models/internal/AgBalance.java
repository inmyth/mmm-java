package com.mbcu.mmm.models.internal;

import java.util.HashMap;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.mbcu.mmm.models.dataapi.AccountBalance;
import com.mbcu.mmm.models.dataapi.Balance;

public class AgBalance {
	
	long ts;
	int ledgerIndex;
	DateTime dt;
	Map<NameIssuer, Balance> data = new HashMap<>();
	
	public static AgBalance from(AccountBalance in, long ts){
		AgBalance res = new AgBalance();
		Map<NameIssuer, Balance> data = new HashMap<>();	
		in.getBalances()
		.forEach(balance -> {
			data.put(balance.toSignature(), balance);
		});		
		
		res.data.putAll(data);
		res.ts = ts;	
		res.dt = new DateTime(ts * 1000, DateTimeZone.forID("Asia/Tokyo"));
		return res;	
	}

	public long getTs() {
		return ts;
	}

	public int getLedgerIndex() {
		return ledgerIndex;
	}

	public DateTime getDt() {
		return dt;
	}

	public Map<NameIssuer, Balance> getData() {
		return data;
	}



	
	

	
}
