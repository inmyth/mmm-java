package com.mbcu.mmm.models.internal;

import java.math.BigDecimal;

public class LastBuySellTuple {

		public LastAmount buy, sel;
		public boolean isBlankStart; 


		
		public LastBuySellTuple(BigDecimal buyRate, BigDecimal buyQuantity, BigDecimal sellRate, BigDecimal sellQuantity, boolean isBlankStart){
			LastAmount buy = new LastAmount(buyRate, buyQuantity);
			LastAmount sel = new LastAmount(sellRate, sellQuantity);
			this.buy       = buy;
			this.sel       = sel;		
			this.isBlankStart = isBlankStart;
			
		}
		
	

	
//	public Amount getBuyAmount() {
//		return buyAmount;
//	}
//
//	public void setBuyAmount(Amount buyAmount) {
//		this.buyAmount = buyAmount;
//	}
//
//	public Amount getSellAmount() {
//		return sellAmount;
//	}
//
//	public void setSellAmount(Amount sellAmount) {
//		this.sellAmount = sellAmount;
//	}
//
//	public BigDecimal getBuyRate() {
//		return buyRate;
//	}
//
//	public void setBuyRate(BigDecimal buyRate) {
//		this.buyRate = buyRate;
//	}
//
//	public BigDecimal getSelRate() {
//		return selRate;
//	}
//
//	public void setSelRate(BigDecimal selRate) {
//		this.selRate = selRate;
//	}

}