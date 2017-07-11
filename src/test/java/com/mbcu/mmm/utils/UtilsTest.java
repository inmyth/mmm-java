package com.mbcu.mmm.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.ripple.core.coretypes.Currency;

public class UtilsTest {
	
	
	
	@Test
	public void testRemoveFirstChar(){
		String s = "-0.0525506494749084/RJP/rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS";
		s = s.substring(1, s.length());
		assertEquals(s, "0.0525506494749084/RJP/rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS");
		
	}

	
	@Test
	public void testCurrencyNative(){
		Currency c = Currency.XRP;
		assertTrue(c.isNative());
		
	}
}
