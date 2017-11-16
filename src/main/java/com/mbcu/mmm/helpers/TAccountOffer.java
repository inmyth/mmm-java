package com.mbcu.mmm.helpers;

import java.math.BigDecimal;

import org.json.JSONException;
import org.json.JSONObject;

import com.mbcu.mmm.models.internal.RLOrder;
import com.mbcu.mmm.models.internal.RLOrder.Direction;
import com.ripple.core.coretypes.AccountID;
import com.ripple.core.coretypes.Amount;
import com.ripple.core.coretypes.Currency;

public class TAccountOffer {
	RLOrder order;
	int seq;
	private static BigDecimal MILLION = new BigDecimal("1000000");

	public static TAccountOffer of(JSONObject j) {
		TAccountOffer res = new TAccountOffer();
		res.seq = j.getInt("seq");
		Amount takerGets = from(j, "taker_gets");
		Amount takerPays = from(j, "taker_pays");
		res.order = RLOrder.rateUnneeded(Direction.BUY, takerPays, takerGets);
		return res;
	}

	private static Amount from(JSONObject whole, String key) {
		Amount res;
		try {
			JSONObject inside = whole.getJSONObject(key);
			BigDecimal value = new BigDecimal(inside.getString("value"));
			Currency currency = Currency.fromString(inside.getString("currency"));
			AccountID issuer = AccountID.fromAddress(inside.getString("issuer"));
			res = RLOrder.amount(value, currency, issuer);
		} catch (JSONException e) {
			BigDecimal drops = new BigDecimal(whole.getString(key));
			res = RLOrder.amount(drops.divide(MILLION));
		}
		return res;
	}

	public RLOrder getOrder() {
		return order;
	}

	public int getSeq() {
		return seq;
	}
}
