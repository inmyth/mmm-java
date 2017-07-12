package com.mbcu.mmm.models.request;

import java.util.ArrayList;

import com.mbcu.mmm.models.internal.Config;
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
	ArrayList<Book> books;

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

	public Subscribe withOrderbook(Amount takerGets, Amount takerPays){
		if (this.books == null){
			this.books = new ArrayList<>();
		}
		this.books.add(new Book(takerGets, takerPays));
		return this;
	}
	
	public Subscribe withOrderbookFromConfig(Config config){
		config.getBotConfigMap().values().forEach(bot ->{
			this.withOrderbook(bot.getBase(), bot.getQuote());		
		});
		return this;
	}
		
	private static class Book{
		NameIssuer taker_gets, taker_pays;
		boolean snapshot = false;
		boolean both = true;
		public Book(Amount taker_gets, Amount taker_pays) {
			super();
			this.taker_gets = new NameIssuer(taker_gets);
			this.taker_pays = new NameIssuer(taker_pays);
		}
	}
	
	private static class NameIssuer {
		String currency;
		String issuer;

		private NameIssuer(Amount amount) {
			super();
			
			this.currency = amount.currencyString();
			if (!amount.isNative()){
				this.issuer = amount.issuerString();
			}
		}
	}
	
	@Override
	public String stringify(){
		return super.stringify(this);	
	}
	
}
