package com.mbcu.mmm.sequences.counters;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import com.mbcu.mmm.models.internal.BotConfig;
import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.models.internal.RLOrder;
import com.mbcu.mmm.models.internal.RLOrder.Direction;
import com.ripple.core.coretypes.AccountID;
import com.ripple.core.coretypes.Amount;
import com.ripple.core.coretypes.Currency;

public class YukiTest {
	private static final String configPath = "config.txt";
	private Yuki yuki; 

	@Before
	public void init() throws IOException{
		yuki = Yuki.newInstance(Config.build(configPath));
	}

	/*
	  2017-07-12, 10:54:30 Made offer to buy 2 JPY.TokyoJPY at price 1.00579 JPY.MrRipple
    bought 0.693638 JPY.TokyoJPY at price 0.050171 XRP
    bought 0.100348 XRP at price 19.8 JPY.MrRipple
    bought 1.30636 JPY.TokyoJPY at price 0.050175 XRP   
      
		buy
		quantity:-0.69363877168/JPY/r94s8px6kSw1uZ1MV98dhSRTvc6VMPoPcN
		totalPrice:-0.6890598/JPY/rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS
		rate:0.9933744005069883
		buy
		quantity:-1.30636122832/JPY/r94s8px6kSw1uZ1MV98dhSRTvc6VMPoPcN
		totalPrice:-1.2978306/JPY/rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS
		rate:0.9934771700953337
	 */
	
	@Test
	public void testAutobridgeArbitrage(){
		Amount q1 = RLOrder.amount(new BigDecimal("-0.69363877168"), Currency.fromString("JPY"), AccountID.fromAddress("r94s8px6kSw1uZ1MV98dhSRTvc6VMPoPcN"));
		Amount t1 = RLOrder.amount(new BigDecimal("-0.6890598"), Currency.fromString("JPY"), AccountID.fromAddress("rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS"));
		Amount q2 = RLOrder.amount(new BigDecimal("-1.30636122832"), Currency.fromString("JPY"), AccountID.fromAddress("r94s8px6kSw1uZ1MV98dhSRTvc6VMPoPcN"));
		Amount t2 = RLOrder.amount(new BigDecimal("-1.2978306"), Currency.fromString("JPY"), AccountID.fromAddress("rB3gZey7VWHYRqJHLoHDEJXJ2pEPNieKiS"));
		RLOrder r1 = RLOrder.basic(Direction.BUY, q1, t1);
		RLOrder r2 = RLOrder.basic(Direction.BUY, q2, t2);

	  List<RLOrder> list = new ArrayList<>();
	  list.add(r1);
	  list.add(r2);
	  list.forEach(oe -> {
	  	
	  	 System.out.println(yuki.buildCounter(oe).stringify());
	  });
	  

	
	}
	
}
