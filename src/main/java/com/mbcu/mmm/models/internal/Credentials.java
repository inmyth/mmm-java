package com.mbcu.mmm.models.internal;

import org.parceler.Parcel;

import com.ripple.core.coretypes.AccountID;

@Parcel
public class Credentials {
	String address;	
	String secret;

	public Credentials(String address, String secret) {
		super();
		this.address = address;
		this.secret = secret;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public boolean addressEquals(AccountID accountID){
		return accountID.address.equals(this.address);
	}

}