package com.mbcu.mmm.sequences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.mbcu.mmm.sequences.Notifier.RequestEmailNotice;

public class NotifierTest {

	@Test
	public void testRequestEmailNotice() {
		long tsa = System.currentTimeMillis();
		Notifier.RequestEmailNotice a = new RequestEmailNotice("aaa", "XRP/JPY", tsa);
		Notifier.RequestEmailNotice b = new RequestEmailNotice("aaa", "XRP/JPY", tsa);

		assertEquals(a, b);
		Map<RequestEmailNotice, Long> map = new HashMap<>();
		map.put(a, 1L);
		map.put(b, 2l);
		assertEquals(map.size(), 1);

		Notifier.RequestEmailNotice c = new RequestEmailNotice("cc", "XRP/JPY", tsa);

		assertFalse(a.equals(c));
		map.clear();
		map.put(a, 1L);
		map.put(c, 1L);
		assertEquals(map.size(), 2);
	}

}
