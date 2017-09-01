package com.mbcu.mmm.rx;

import com.mbcu.mmm.utils.GsonUtils;

public class BusBase {
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(this.getClass().getName());
		sb.append("\n");
		sb.append(GsonUtils.toJson(this));
		return sb.toString();
	}
	
}
