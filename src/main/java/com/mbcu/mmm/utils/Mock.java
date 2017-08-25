package com.mbcu.mmm.utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.json.JSONObject;

import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.models.internal.Credentials;
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
		
		IntStream a = IntStream
		.range(1, 5);
		IntStream b = IntStream.range(90, 100);
		
		//		
//		IntStream.concat(a, b).forEach(c -> {
//			System.out.println(c);
//			
//		});
		
//		Observable d = Observable.range(1, 5);
//		Observable e = Observable.range(90, 110);
		
//		Observable.range(1, 5)
//		 .mergeWith(Observable.range(90, 110))
//		 .subscribe(System.out::println);
//		
//		BigDecimal aaa = new BigDecimal("-2");
//		System.out.println(aaa.compareTo(BigDecimal.ZERO) <= 0);
		
//		Bla bla = new Bla();
//		System.out.println(bla.getClass().getName());
		
		Foo foo = new Foo();
		System.out.println(foo);

	}
	
	static class Bla {
		String a = "aaa";
		
		@Override
		public String toString() {
			// TODO Auto-generated method stub
			return this.getClass().getName() + GsonUtils.toJson(this);
		}
		
	}
	
	static class Foo extends Bla{
		String c = "ccc";
		
		public Foo() {
			super.a = "bbb";
		}
		 
	}

}
