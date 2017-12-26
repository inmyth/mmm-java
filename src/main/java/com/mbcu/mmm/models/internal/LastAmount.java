package com.mbcu.mmm.models.internal;

import java.math.BigDecimal;

public class LastAmount {

	public final BigDecimal unitPrice;
	public final BigDecimal qty;

	public LastAmount(BigDecimal unitPrice, BigDecimal qty) {
		super();
		this.unitPrice = unitPrice;
		this.qty = qty;
	}

}
