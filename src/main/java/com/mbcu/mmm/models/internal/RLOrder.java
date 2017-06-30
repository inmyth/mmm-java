package com.mbcu.mmm.models.internal;

import com.mbcu.mmm.models.ripple.tx.Order;

public class RLOrder {

	public enum Direction {
		BUY("buy"), SELL("sell");

		private String text;

		Direction(String text) {
			this.text = text;
		}

		public String text() {
			return text;
		}
	}

	String direction;
	RLAmount quantity;
	RLAmount totalPrice;
	boolean passive;
	boolean fillOrKill;

	
	
}
