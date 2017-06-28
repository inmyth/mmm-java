package com.mbcu.mmm.models.request;

import java.util.ArrayList;

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

	public String stringify() {
		return super.stringify(this);

	}
}
