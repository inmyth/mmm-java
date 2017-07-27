package com.mbcu.mmm.models.request;

import java.util.ArrayList;

import com.mbcu.mmm.models.internal.BotConfig;
import com.mbcu.mmm.models.internal.NameIssuer;
import com.ripple.core.coretypes.Amount;

public class Subscribe extends Request {

	private Subscribe(Command command) {
		super(command);
	}

	public enum Stream {
		LEDGER("ledger"), SERVER("server");

		private String text;

		Stream(String text) {
			this.text = text;
		}

		public String text() {
			return text;
		}
	}

	ArrayList<String> accounts;
	ArrayList<String> streams;
	Book[] books;

	public void addAccount(String account) {
		if (accounts == null) {
			accounts = new ArrayList<>();
		}
		accounts.add(account);
	}

	public void addStream(Stream stream) {
		if (streams == null) {
			streams = new ArrayList<>();
		}
		streams.add(stream.text);
	}

	public static Subscribe build(Command command) {
		return new Subscribe(command);
	}

	public Subscribe withAccount(String account) {
		this.addAccount(account);
		return this;
	}

	public Subscribe withStream(Stream stream) {
		this.addStream(stream);
		return this;
	}

	public Subscribe withOrderbook(BotConfig botConfig){
		this.books = new Book[1];
		this.books[0] = new Book(botConfig.getQuote(), botConfig.getBase());
		return this;
	}
		
	public static class Book{
		NameIssuer taker_gets, taker_pays;
		boolean snapshot = true;
		boolean both = true;
		public Book(Amount taker_gets, Amount taker_pays) {
			super();
			this.taker_gets = NameIssuer.from(taker_gets);
			this.taker_pays = NameIssuer.from(taker_pays);
		}
	}
	



	
	@Override
	public String stringify(){
		return super.stringify(this);	
	}
	
}
