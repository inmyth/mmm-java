package com.mbcu.mmm.models.request;

import com.mbcu.mmm.models.internal.Config;

public class AccountOffers extends Request{

	String account;
	
	private AccountOffers() {
		super(Command.ACCOUNT_OFFERS);
	}

	@Override
	public String stringify() {
		return super.stringify(this);
	}
	
	public static AccountOffers of(Config config){
		AccountOffers res = new AccountOffers();
		res.account = config.getCredentials().getAddress();
		return res;
	}

}
