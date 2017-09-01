package com.mbcu.mmm.notice;

public class Content {
	
	public static String body(String account, String pair, String error){
		StringBuilder sb = new StringBuilder(account);
		sb.append("\n");
		sb.append(pair);
		sb.append("\n");
		sb.append(error);
		return sb.toString();	
	}

}
