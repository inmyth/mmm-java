package com.mbcu.mmm.models.internal;

import com.ripple.core.coretypes.hash.Hash256;

public class EditedOrder extends SequenceChange {
		
	private final Hash256 previousTxnId;
	private final Hash256 hash;
	private final RLOrder rlOrder;
	
	
	public EditedOrder(Hash256 previousTxnId, Hash256 hash, RLOrder rlOrder) {
		super();
		this.previousTxnId = previousTxnId;
		this.hash = hash;
		this.rlOrder = rlOrder;
	}
	
	
}
