package com.mbcu.mmm.models.internal;

import com.mbcu.mmm.models.Asset.Currency;
import com.ripple.core.coretypes.Amount;

public class NameIssuer {
	String currency;
	String issuer;

	private NameIssuer() {
		super();
	}

	public static NameIssuer from(Amount amount) {
		NameIssuer res = new NameIssuer();
		res.currency = amount.currencyString();
		res.issuer = (!amount.isNative() || !amount.currencyString().equals("XRP")) ? res.issuer = amount.issuerString()
				: null;
		return res;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		if (!(o instanceof NameIssuer)) {
			return false;
		}
		NameIssuer test = (NameIssuer) o;
		if (test.currency.equals(this.currency)) {
			if (test.issuer == null && this.issuer == null) {
				return true;
			}
			if (test.issuer != null && this.issuer != null && test.issuer.equals(this.issuer)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int hashCode() {
		int result = 17;
		result = 31 * result + currency.hashCode();
		result = issuer != null ? 31 * result + issuer.hashCode() : result;
		return result;
	}

	@Override
	public String toString() {
		StringBuilder res = new StringBuilder(currency);
		if (!currency.equals(Currency.XRP.toString())) {
			res.append(".");
			res.append(issuer);
		}
		return res.toString();
	}
}
