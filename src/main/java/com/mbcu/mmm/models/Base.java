package com.mbcu.mmm.models;

import com.mbcu.mmm.utils.GsonUtils;

public class Base {

	protected String stringify(Object object) {
		return GsonUtils.toJson(object);
	}
}
