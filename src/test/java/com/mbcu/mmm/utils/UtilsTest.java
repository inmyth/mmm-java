package com.mbcu.mmm.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import com.mbcu.mmm.main.WebSocketClient;
import com.mbcu.mmm.models.internal.NameIssuer;
import com.mbcu.mmm.models.request.AccountInfo;
import com.mbcu.mmm.rx.RxBus;
import com.mbcu.mmm.rx.RxBusProvider;
import com.ripple.core.coretypes.AccountID;
import com.ripple.core.coretypes.Amount;
import com.ripple.core.coretypes.Currency;

import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

public class UtilsTest {
	
	private final AtomicBoolean flagWaitSeq1 = new AtomicBoolean(true);
	private final AtomicBoolean flagWaitSeq2 = new AtomicBoolean(false);

  private Subject<Boolean> seqSyncObs = PublishSubject.create();  

  @Test
  public void splitTest(){
  	String a = "111, 999";
  	String[] els = a.split("-");
  	assertEquals(a, els[els.length - 1]);
  	assertEquals(els.length, 1);
  	
  }

  @Test
  public void nameIssuerMap(){
  	Map<NameIssuer, String> map = new HashMap<>();
  	
   	
  	Amount a1 = new Amount(Currency.fromString("ABC"), AccountID.fromAddress("rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS"));
  	NameIssuer a = NameIssuer.from(a1);
  	Amount b1 = new Amount(Currency.fromString("ABC"), AccountID.fromAddress("rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS"));
  	NameIssuer b = NameIssuer.from(b1); 	
  	map.put(a, "a");
  	map.put(b, "b"); 	
  	assertEquals(map.size(), 1);	
  	Amount c1 = new Amount(Currency.fromString("ABD"), AccountID.fromAddress("rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS"));
  	NameIssuer c = NameIssuer.from(c1);	
  	map.put(c, "c");
  	assertEquals(map.size(), 2); 	
  	Amount d1 = new Amount(Currency.fromString("ABC"), AccountID.fromAddress("raNDu1gNyZ5hipBTKxm5zx7NovA1rNnNRf"));
  	NameIssuer d = NameIssuer.from(d1);
  	map.put(d, "c");
  	assertEquals(map.size(), 3);
  }
  
  
  @Test
  public void amountEqual(){

  	
  	Amount c = new Amount(Currency.fromString("ABC"), AccountID.fromAddress("rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS"));
  	Amount d = new Amount(Currency.fromString("ABC"), AccountID.fromAddress("rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS"));
  	assertEquals(c, d);
  	
  	Amount e = new Amount(Currency.fromString("ABD"), AccountID.fromAddress("rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS"));
  	assertNotEquals(c, e);

  	

  }
  

  
  @Test
  public void testBigDecimalFloor(){
  	BigDecimal a = new BigDecimal(4.202);
  	BigDecimal res1 = a.divide(new BigDecimal(5.0), MathContext.DECIMAL32);
  	assertEquals(res1.intValue(), 0);
  	BigDecimal res2 = a.divide(new BigDecimal(2), MathContext.DECIMAL32);
  	assertEquals(res2.intValue(), 2);

  }
  
  @Test
  public void testBlockingObs(){
    Subject<Object> pendingOrdersObs = PublishSubject.create();  
  	System.out.println("a");

    pendingOrdersObs
    .subscribeOn(Schedulers.io())
    .subscribe(o -> {
    	System.out.println("b");
    	System.out.println("c");

    	
    });
  	System.out.println("d");

  	pendingOrdersObs.onNext(new Object());
  }
  
  @Test
  public void testStreamFilter(){
  	
    List<String> lines = Arrays.asList("spring", "node", "mkyong");
    List<String> res = lines.stream().filter(line -> "aaa".equals(line))
    .collect(Collectors.toList());
    
    assertNotEquals(res, null);
    assertEquals(0, res.size());
    
  }

  @Test
  public void testJsonObjectOrString(){
  	String t1 = "      {\r\n        \"flags\": 0,\r\n        \"quality\": \"100000\",\r\n        \"seq\": 12486,\r\n        \"taker_gets\": {\r\n          \"currency\": \"JPY\",\r\n          \"issuer\": \"rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS\",\r\n          \"value\": \"10\"\r\n        },\r\n        \"taker_pays\": \"1000000\"\r\n      }";
		JSONObject whole = new JSONObject(t1);	
		
  	Amount res;
  	String key = "taker_pays";
  	try {
  		JSONObject inside = whole.getJSONObject(key);
  		BigDecimal value = new BigDecimal(inside.getString("value"));
  		Currency currency = Currency.fromString(inside.getString("currency"));
  		AccountID issuer = AccountID.fromAddress(inside.getString("issuer"));	
  		res = new Amount(value, currency, issuer);
  	}catch (JSONException e) {
  		System.out.println();
  		res = new Amount(new BigDecimal(whole.getString(key)));
		}
//		assertNotNull(whole.getJSONObject("taker_gets"));	
//		assertNotNull(whole.getJSONObject("taker_pays"));	
  }
  
//  
//	@Test
//	public void testRemoveFirstChar(){
//		String s = "-0.0525506494749084/RJP/rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS";
//		s = s.substring(1, s.length());
//		assertEquals(s, "0.0525506494749084/RJP/rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS");
//		
//	}
//
//	
//	@Test
//	public void testCurrencyNative(){
//		Currency c = Currency.XRP;
//		assertTrue(c.isNative());	
//	}
//	
//	@Test
//	public void testCurrencyFromString(){	
//		Amount b = new Amount(Currency.fromString("JPY"), AccountID.fromAddress("rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS")); 
//		Amount bb = new Amount(Currency.fromString("JPY"), AccountID.fromAddress("rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS")); 
//		assertEquals(b, bb);
//		Amount c = new Amount(Currency.fromString("USD"), AccountID.fromAddress("rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS")); 
//		assertNotEquals(b, c);
//		Amount d= new Amount(new BigDecimal("0")); 
//		assertNotEquals(b, d);
//		Amount e = new Amount(Currency.fromString("JPY"), AccountID.fromAddress("rHMjZANhquizS74FutV3CNoWfQcoVkBxf")); 
//		assertNotEquals(b, e);		
//	}
//	
//	@Test
//	public void testBigDecimal(){
//		BigDecimal a = new BigDecimal("345.24245364745677678968089554");
//		BigDecimal b = a.setScale(6, BigDecimal.ROUND_HALF_DOWN);
//		assertEquals(b.toString(), "345.242454");
//		MathContext mc = new MathContext(16, RoundingMode.HALF_DOWN);
//		BigDecimal c = a.round(mc);
//		assertEquals(c.toString(), "345.2424536474568");
//	}
//	
//	@Test
//	public void testObs(){
//		seqSyncObs.subscribe(flag -> {
//			System.out.println(flag);
//		});
//		
//		seqSyncObs.onNext(flagWaitSeq1.compareAndSet(false, true)); // false
//		seqSyncObs.onNext(flagWaitSeq2.compareAndSet(false, true)); // true
//
//
//	}
	
}
