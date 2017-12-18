package com.mbcu.mmm.models.internal;

import java.math.BigDecimal;

public class LastAmount {

	public final BigDecimal rate;
	public final BigDecimal quantity;

	public LastAmount(BigDecimal rate, BigDecimal quantity) {
		super();
		this.rate = rate;
		this.quantity = quantity;
	}

}
