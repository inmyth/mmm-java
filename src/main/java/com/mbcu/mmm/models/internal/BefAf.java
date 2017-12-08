package com.mbcu.mmm.models.internal;

import com.mbcu.mmm.models.Base;
import com.ripple.core.coretypes.hash.Hash256;
import com.ripple.core.coretypes.uint.UInt32;

public class BefAf extends Base {
	public final Hash256 txnId;
	public final RLOrder before;
	public final RLOrder after;
	public final UInt32 befSeq;
	public final RLOrder source;
	
	public BefAf(RLOrder before, RLOrder after, UInt32 befSeq, Hash256 txnId, RLOrder source) {
		super();
		this.before = before;
		this.after = after;
		this.befSeq = befSeq;
		this.txnId = txnId;
		this.source = source;
	}

	@Override
	public String stringify() {
		StringBuffer res = new StringBuffer("BefAf");
		res.append("\n");
		res.append(txnId.toString());
		res.append("\n");
		res.append(before.stringify());
		if (after != null) {
			res.append(after.stringify());
		} else {
			res.append("After is null");
		}
		return res.toString();
	}

}