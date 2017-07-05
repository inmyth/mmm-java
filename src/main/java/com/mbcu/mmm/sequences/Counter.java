package com.mbcu.mmm.sequences;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Logger;

import org.json.JSONObject;

import com.mbcu.mmm.main.Events;
import com.mbcu.mmm.main.Events.WSGotText;
import com.mbcu.mmm.models.internal.Config;
import com.mbcu.mmm.models.internal.RLOrder;
import com.mbcu.mmm.models.request.Request.Command;
import com.mbcu.mmm.models.request.Submit;
import com.mbcu.mmm.models.request.Subscribe;
import com.mbcu.mmm.rx.RxBus;
import com.mbcu.mmm.rx.RxBusProvider;
import com.mbcu.mmm.utils.MyLogger;
import com.ripple.core.coretypes.AccountID;
import com.ripple.core.coretypes.Amount;
import com.ripple.core.coretypes.Currency;
import com.ripple.core.coretypes.STObject;
import com.ripple.core.coretypes.hash.Hash256;
import com.ripple.core.coretypes.uint.UInt16;
import com.ripple.core.coretypes.uint.UInt32;
import com.ripple.core.coretypes.uint.UInt64;
import com.ripple.core.coretypes.uint.UInt8;
import com.ripple.core.fields.Field;
import com.ripple.core.fields.STObjectField;
import com.ripple.core.fields.UInt16Field;
import com.ripple.core.fields.UInt8Field;
import com.ripple.core.serialized.SerializedType;
import com.ripple.core.types.known.sle.LedgerEntry;
import com.ripple.core.types.known.sle.entries.Offer;
import com.ripple.core.types.known.tx.Transaction;
import com.ripple.core.types.known.tx.result.AffectedNode;
import com.ripple.core.types.known.tx.result.TransactionMeta;
import com.ripple.core.types.known.tx.signed.SignedTransaction;
import com.ripple.core.types.known.tx.txns.OfferCreate;

public class Counter extends Base {
	private RxBus bus = RxBusProvider.getInstance();

	 int count;
	public Counter(Config config) {
		super(MyLogger.getLogger(Counter.class.getName()));

		bus.toObservable().subscribe(o -> {
			if (o instanceof Events.WSConnected) {

			} else if (o instanceof Events.WSGotText) {
				WSGotText event = (WSGotText) o;

			}

		});

	}

	public static Counter newInstance(Config config) {
		return new Counter(config);

	}
	
	
	public static RLOrder rawCounter(RLOrder orderExecuted, boolean isOwn){
		return null;
	}



}
