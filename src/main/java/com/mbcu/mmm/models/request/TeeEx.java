package com.mbcu.mmm.models.request;

public class TeeEx extends Request {

	private final String transaction;
	
	public TeeEx(String transaction) {
		super(Command.TX);
		this.transaction = transaction;
	}

	@Override
	public String stringify() {
		return super.stringify(this);
	}
	
	public static TeeEx newInstance(String hash){
		return new TeeEx(hash);
	}
	
}
