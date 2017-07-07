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
import com.ripple.core.coretypes.Amount;
import com.ripple.core.coretypes.STObject;
import com.ripple.core.coretypes.hash.Hash256;
import com.ripple.core.fields.Field;
import com.ripple.core.types.known.sle.LedgerEntry;
import com.ripple.core.types.known.sle.entries.Offer;
import com.ripple.core.types.known.tx.Transaction;
import com.ripple.core.types.known.tx.result.AffectedNode;
import com.ripple.core.types.known.tx.result.TransactionMeta;

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
		
		Observable.range(1, 5)
		 .mergeWith(Observable.range(90, 110))
		 .subscribe(System.out::println);
		
		BigDecimal aaa = new BigDecimal("-2");
		System.out.println(aaa.compareTo(BigDecimal.ZERO) <= 0);
	}

}
