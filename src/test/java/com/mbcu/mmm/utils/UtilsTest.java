package com.mbcu.mmm.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import com.ripple.core.coretypes.AccountID;
import com.ripple.core.coretypes.Amount;
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
	
	@Test
	public void testCurrencyFromString(){	
		Amount b = new Amount(Currency.fromString("JPY"), AccountID.fromAddress("rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS")); 
		Amount bb = new Amount(Currency.fromString("JPY"), AccountID.fromAddress("rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS")); 
		assertEquals(b, bb);
		Amount c = new Amount(Currency.fromString("USD"), AccountID.fromAddress("rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS")); 
		assertNotEquals(b, c);
		Amount d= new Amount(new BigDecimal("0")); 
		assertNotEquals(b, d);
		Amount e = new Amount(Currency.fromString("JPY"), AccountID.fromAddress("rHMjZANhquizS74FutV3CNoWfQcoVkBxf")); 
		assertNotEquals(b, e);		
	}
	
	@Test
	public void testBigDecimal(){
		BigDecimal a = new BigDecimal("345.24245364745677678968089554");
		BigDecimal b = a.setScale(6, BigDecimal.ROUND_HALF_DOWN);
		assertEquals(b.toString(), "345.242454");
		MathContext mc = new MathContext(16, RoundingMode.HALF_DOWN);
		BigDecimal c = a.round(mc);
		assertEquals(c.toString(), "345.2424536474568");
	}
	
}
