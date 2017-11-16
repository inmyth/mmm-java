package com.mbcu.mmm.models.dataapi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccountBalance {

	String result;
	int ledger_index;
	int limit;
	List<Balance> balances = new ArrayList<Balance>();

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public int getLedger_index() {
		return ledger_index;
	}

	public void setLedger_index(int ledger_index) {
		this.ledger_index = ledger_index;
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public List<Balance> getBalances() {
		return balances;
	}

	public void setBalances(List<Balance> balances) {
		if (balances != null) {
			this.balances.addAll(balances);
		}
	}

}
