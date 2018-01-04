package com.mbcu.mmm.models;

import com.mbcu.mmm.utils.GsonUtils;

public abstract class Base {

	protected String stringify(Object object) {
		return GsonUtils.toJson(object);
	}

	public abstract String stringify();
}