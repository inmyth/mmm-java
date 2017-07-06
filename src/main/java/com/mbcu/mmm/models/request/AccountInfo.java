package com.mbcu.mmm.models.request;

public class AccountInfo extends Request {

	final String account;
	
	public AccountInfo(String account) {
		super(Command.ACCOUNT_INFO);
		this.account = account;
	}

	
	public static final AccountInfo newInstance(String account){
		return new AccountInfo(account);
	}
	
	@Override
	public String stringify(){
		return super.stringify(this);	
	}
	
	
	
	
	

}
