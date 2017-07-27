package com.mbcu.mmm.models.request;

import java.util.ArrayList;
import java.util.List;

import com.mbcu.mmm.models.internal.BotConfig;
import com.mbcu.mmm.models.internal.NameIssuer;

public class ROrderbook extends Request {

	String taker;
	Integer limit;
	NameIssuer taker_gets;
	NameIssuer taker_pays;
	
	private ROrderbook() {
		super(Command.ORDER_BOOK);
	}

	@Override
	public String stringify() {
		return super.stringify(this);
	}
	
	
	public static List<ROrderbook> buildBoth(BotConfig botConfig){
		List<ROrderbook> res = new ArrayList<ROrderbook>();
		
		
		
		
	}
	
}
