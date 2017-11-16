package com.mbcu.mmm.models.request;

import com.mbcu.mmm.models.internal.Config;

public class AccountOffers extends Request {

	String account, ledger, marker;
	int limit; // max 400

	private AccountOffers() {
		super(Command.ACCOUNT_OFFERS);
	}

	@Override
	public String stringify() {
		return super.stringify(this);
	}

	public static AccountOffers of(Config config) {
		AccountOffers res = new AccountOffers();
		res.account = config.getCredentials().getAddress();
		res.limit = 200;
		return res;
	}

	public AccountOffers withMarker(String marker) {
		this.marker = marker;
		return this;
	}

}
