package com.mbcu.mmm.models;

public class Asset {

	public enum Currency {
		XRP("XRP");

		private String text;

		Currency(String text) {
			this.text = text;
		}

		public String text() {
			return text;
		}
	}
	
}
