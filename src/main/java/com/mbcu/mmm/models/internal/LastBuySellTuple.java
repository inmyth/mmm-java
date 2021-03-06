package com.mbcu.mmm.models.internal;

import java.math.BigDecimal;

public class LastBuySellTuple {

		public final LastAmount buy, sel;
		public final boolean isBuyPulledFromSel;
		public final boolean isSelPulledFromBuy;

		
		public LastBuySellTuple(BigDecimal buyUnitPrice, BigDecimal buyQty, BigDecimal selUnitPrice, BigDecimal selQty, boolean isBuyPulledFromSel, boolean isSelPulledFromBuy){
			LastAmount buy = new LastAmount(buyUnitPrice, buyQty);
			LastAmount sel = new LastAmount(selUnitPrice, selQty);
			this.buy       = buy;
			this.sel       = sel;		
			this.isBuyPulledFromSel = isBuyPulledFromSel;
			this.isSelPulledFromBuy = isSelPulledFromBuy;
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
