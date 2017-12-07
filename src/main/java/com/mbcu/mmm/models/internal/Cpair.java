package com.mbcu.mmm.models.internal;

import java.util.Optional;

import com.ripple.core.coretypes.Amount;
import com.ripple.core.coretypes.Currency;
import com.ripple.core.coretypes.STObject;
import com.ripple.core.types.known.sle.entries.Offer;

public class Cpair {

	private final String fw;
	private final String rv;

	private Cpair(String pair) {
		super();
		this.fw = pair;
		this.rv = getReversePair();
	}

	private String getReversePair() {
		String[] p = this.fw.split("[/]");
		StringBuffer res = new StringBuffer(p[1]);
		res.append("/");
		res.append(p[0]);
		return res.toString();
	}

	public Optional<Boolean> isMatch(String test) {
		if (test.equals(fw)) {
			return Optional.of(true);
		} else if (test.equals(rv)) {
			return Optional.of(false);
		}
		return Optional.empty();
	}

	public String getFw() {
		return fw;
	}

	public String getRv() {
		return rv;
	}

	@Override
	public String toString() {
		return fw;
	}

	public static Cpair newInstance(String pair) {
		return new Cpair(pair);
	}

	public static Cpair newInstance(String base, String baseIssuer, String quote, String quoteIssuer) {
		StringBuffer sb = new StringBuffer(base);
		if (!base.equals(Currency.XRP.toString())) {
			sb.append(".");
			sb.append(baseIssuer);
		}
		sb.append("/");
		sb.append(quote);
		if (!quote.equals(Currency.XRP.toString())) {
			sb.append(".");
			sb.append(quoteIssuer);
		}
		Cpair res = new Cpair(sb.toString());
		return res;
	}

	public static Cpair newInstance(Offer offer) {
		STObject executed = offer.executed(offer.get(STObject.FinalFields));
		Amount paid = executed.get(Amount.TakerPays);
		Amount got = executed.get(Amount.TakerGets);
		Cpair res = newInstance(paid, got);
		return res;
	}

	public static Cpair newInstance(Amount pay, Amount get) {
		return newInstance(get.currencyString(), get.issuerString(), pay.currencyString(), pay.issuerString());
	}
}
