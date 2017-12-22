package com.mbcu.mmm.models.internal;

import java.math.BigDecimal;

public class LastAmount {

	public final BigDecimal rate;
	public final BigDecimal trade;

	public LastAmount(BigDecimal rate, BigDecimal trade) {
		super();
		this.rate = rate;
		this.trade = trade;
	}

}
