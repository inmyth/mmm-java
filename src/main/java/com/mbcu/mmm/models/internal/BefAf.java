package com.mbcu.mmm.models.internal;

import com.mbcu.mmm.models.Base;

public class BefAf extends Base{
	public  RLOrder before;
	public  RLOrder after;
	
	public BefAf(RLOrder before, RLOrder after) {
		super();
		this.before = before;
		this.after = after;
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