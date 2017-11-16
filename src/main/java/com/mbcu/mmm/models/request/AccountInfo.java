package com.mbcu.mmm.models.request;

import com.mbcu.mmm.models.internal.Config;

public class AccountInfo extends Request {

	final String account;

	public AccountInfo(String account) {
		super(Command.ACCOUNT_INFO);
		this.account = account;
	}

	public static final AccountInfo of(Config config) {
		return new AccountInfo(config.getCredentials().getAddress());
	}

	@Override
	public String stringify() {
		return super.stringify(this);
	}

}
