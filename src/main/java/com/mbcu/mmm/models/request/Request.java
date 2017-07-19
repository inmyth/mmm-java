package com.mbcu.mmm.models.request;

import com.mbcu.mmm.models.Base;

public abstract class  Request extends Base{

	public enum Command {
		SUBSCRIBE("subscribe"), SUBMIT("submit"), ACCOUNT_INFO("account_info"), LEDGER_CLOSED("ledger_closed");

		private String text;

		Command(String text) {
			this.text = text;
		}

		public String text() {
			return text;
		}
	}

	String command;

	public Request(Command command) {
		this.command = command.text;
	}




}
