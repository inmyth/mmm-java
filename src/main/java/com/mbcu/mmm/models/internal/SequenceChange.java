package com.mbcu.mmm.models.internal;

import com.ripple.core.coretypes.hash.Hash256;

public class SequenceChange {
	
	
	public enum Type {
		NEW("new"), EDIT("edit"), CANCEL("cancel") ;

		private String text;

		Type(String text) {
			this.text = text;
		}

		public String text() {
			return text;
		}
	}



	
	
	
	
}
