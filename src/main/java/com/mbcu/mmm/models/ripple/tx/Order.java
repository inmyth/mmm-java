package com.mbcu.mmm.models.ripple.tx;

public class Order {
	
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
	Amount quantity;
	Amount totalPrice;
	
	boolean passive;
	boolean fillOrKill;

}

/*
{
  "direction": "buy",
  "quantity": {
    "currency": "USD",
    "counterparty": "rMH4UxPrbuMa1spCBR98hLLyNJp4d8p4tM",
    "value": "10.1"
  },
  "totalPrice": {
    "currency": "XRP",
    "value": "2"
  },
  "passive": true,
  "fillOrKill": true
}
*/