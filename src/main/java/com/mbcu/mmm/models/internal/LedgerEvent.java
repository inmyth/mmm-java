package com.mbcu.mmm.models.internal;

import org.json.JSONObject;

public class LedgerEvent {
	final int closed;
	final int validated;

	private LedgerEvent(int closed, int validated) {
		super();
		this.closed = closed;
		this.validated = validated;
	}

	public int getClosed() {
		return closed;
	}

	public int getValidated() {
		return validated;
	}

	public static LedgerEvent fromJSON(JSONObject root) {
		if (!root.has("validated_ledgers")) {
			return new LedgerEvent(-1, -1);
		}
		// 158188-419709,419711
		String a = root.getString("validated_ledgers");
		String els[] = a.split(",");
		String raw = els[els.length - 1];
		String[] rangeValids = raw.split("-");
		int validated = Integer.parseInt(rangeValids[rangeValids.length - 1]);
		int closed = root.getInt("ledger_index");
		return new LedgerEvent(closed, validated);
	}

	@Override
	public String toString() {
		return String.format("Ledger closed: %d, Ledger validated: %d", this.closed, this.validated);
	}

}
