package com.mbcu.mmm.rx;

public class RxBusProvider {
	private static RxBus BUS = null;

	private RxBusProvider() {
		// No instances.
	}

	public static RxBus getInstance() {
		if (BUS == null) {
			BUS = new RxBus();
		}
		return BUS;
	}
}