package com.mbcu.mmm.sequences.state;

import com.mbcu.mmm.models.internal.Config;

public class StateProvider {

	private static State STATE = null;

	private StateProvider() {
		// No instances.
	}

	public static State getInstance(Config config) {
		if (STATE == null) {
			STATE = State.newInstance(config);
		}
		return STATE;
	}

}
