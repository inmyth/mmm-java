package com.mbcu.mmm.models.internal;

import com.mbcu.mmm.models.Base;
import com.ripple.core.coretypes.uint.UInt32;

public class BefAf extends Base{
	public final RLOrder before;
	public final RLOrder after;
	public final UInt32 befSeq;
	
	public BefAf(RLOrder before, RLOrder after, UInt32 befSeq) {
		super();
		this.before 	= before;
		this.after 		= after;
		this.befSeq 	= befSeq;
	}

	@Override
	public String stringify() {
		StringBuffer res = new StringBuffer(before.stringify());
		if (after != null){
			res.append(after.stringify());
		}else{
			res.append("After is null");
		}
		return res.toString();
	}
	
}