package com.mbcu.mmm.utils;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.json.JSONObject;

import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.models.internal.Credentials;
import com.mbcu.mmm.models.internal.RLOrder;
import com.mbcu.mmm.models.internal.RLOrder.Direction;
import com.mbcu.mmm.sequences.Common;
import com.mbcu.mmm.sequences.counters.Yuki;
import com.ripple.core.coretypes.AccountID;
import com.ripple.core.coretypes.Amount;
import com.ripple.core.coretypes.Currency;
import com.ripple.core.coretypes.STObject;
import com.ripple.core.coretypes.hash.Hash256;
import com.ripple.core.coretypes.uint.UInt32;
import com.ripple.core.fields.Field;
import com.ripple.core.types.known.sle.LedgerEntry;
import com.ripple.core.types.known.sle.entries.Offer;
import com.ripple.core.types.known.tx.Transaction;
import com.ripple.core.types.known.tx.result.AffectedNode;
import com.ripple.core.types.known.tx.result.TransactionMeta;
import com.ripple.core.types.known.tx.signed.SignedTransaction;
import com.ripple.core.types.known.tx.txns.OfferCreate;

import io.reactivex.Observable;

public class Mock {

	public static void main(String[] args) {
   Amount a = new Amount(new BigDecimal(-20));
   Amount b = new Amount(new BigDecimal(-10));
   
   Amount p = new Amount(new BigDecimal(3));
   Amount q = new Amount(new BigDecimal(-3));
   
   RLOrder m = RLOrder.rateUnneeded(Direction.BUY, a, b);		
   RLOrder n = RLOrder.rateUnneeded(Direction.BUY, p, q);		
   
   List<RLOrder> t = new ArrayList<>();
   t.add(m);
   t.add(n);
   List<RLOrder> res = t
   .stream()
   .filter(e -> e.getQuantity().value().compareTo(BigDecimal.ONE) > 0)
   .filter(e -> e.getTotalPrice().value().compareTo(BigDecimal.ONE) > 0)
   .collect(Collectors.toList());
//   res.forEach(System.out::println);
   System.out.println(res.isEmpty());
   
;
   

 

   
//		RLOrder buy = RLOrder.rateUnneeded(Direction.BUY, newAmt, totalPrice);		

		
		
//		BigDecimal test = new BigDecimal("0.99");
//		BigDecimal res = Collections.nCopies(1, test).stream().reduce((x, y) -> x.multiply(y, MathContext.DECIMAL64)).get();
//		System.out.println(res);
//		int a = IntStream.range(1, 4).
//		
//				BigDecimal result = bdList.stream()
//        .reduce(BigDecimal.ZERO, BigDecimal::add);
//		System.out.println(a);
		
//		IntStream.range(1, 5).mapToObj(l -> {
//			int a = IntStream.range(1, l).reduce((x,y) -> x+y).getAsInt();
//			return a;
//			
//		}).forEach(System.out::println);
		
//		IntStream a = IntStream.range(1, 5);
//		IntStream b = IntStream.range(90, 100);

		//
		// IntStream.concat(a, b).forEach(c -> {
		// System.out.println(c);
		//
		// });

		// Observable d = Observable.range(1, 5);
		// Observable e = Observable.range(90, 110);

		// Observable.range(1, 5)
		// .mergeWith(Observable.range(90, 110))
		// .subscribe(System.out::println);
		//
		// BigDecimal aaa = new BigDecimal("-2");
		// System.out.println(aaa.compareTo(BigDecimal.ZERO) <= 0);

		// Bla bla = new Bla();
		// System.out.println(bla.getClass().getName());

//		Foo foo = new Foo();
//		System.out.println(foo);

	}

	static class Bla {
		String a = "aaa";

		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return this.getClass().getName() + GsonUtils.toJson(this);
		}

	}

	static class Foo extends Bla {
		String c = "ccc";

		public Foo() {
			super.a = "bbb";
		}

	}

}
