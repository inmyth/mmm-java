package com.mbcu.mmm.models.request;

import java.util.ArrayList;
import java.util.List;

import com.mbcu.mmm.models.internal.BotConfig;
import com.mbcu.mmm.models.internal.NameIssuer;

public class BookOffers extends Request {

	String taker;
	Integer limit;
	NameIssuer taker_gets;
	NameIssuer taker_pays;
	
	private BookOffers() {
		super(Command.BOOK_OFFERS);
	}

	@Override
	public String stringify() {
		return super.stringify(this);
	}
	
	
	public static List<String> buildRequest(String taker, BotConfig botConfig){
		List<String> res = new ArrayList<String>();
		BookOffers one = new BookOffers();
		one.taker_gets = NameIssuer.from(botConfig.getBase());
		one.taker_pays = NameIssuer.from(botConfig.getQuote());
		one.taker = taker;
		
		BookOffers two = new BookOffers();
		two.taker_gets = one.taker_pays;
		two.taker_pays = one.taker_gets;
		two.taker = taker;
		res.add(one.stringify());
		res.add(two.stringify());
		return res;
	}
	
}
