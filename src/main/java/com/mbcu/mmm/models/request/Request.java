package com.mbcu.mmm.models.request;

import com.mbcu.mmm.utils.GsonUtils;

public class Request {

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

	protected String stringify(Object object) {
		return GsonUtils.toJson(object);
	}

}
