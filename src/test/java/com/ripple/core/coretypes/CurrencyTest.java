package com.ripple.core.coretypes;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CurrencyTest {

	@Test
	public void fromString() {
		Currency currency = Currency.fromString("JPY");
		assertEquals(currency.toString(), "JPY");

	}

	@Test
	public void testXRPCode() {
		assertEquals(Currency.XRP.toString(), "XRP");

	}

}
