package com.mbcu.mmm.models.request;

import com.mbcu.mmm.models.Base;

public class Request extends Base{

	public enum Command {
		SUBSCRIBE("subscribe"), SUBMIT("submit");

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
