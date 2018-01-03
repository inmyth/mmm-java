package com.mbcu.mmm.sequences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.mbcu.mmm.sequences.Emailer.SendEmailError;

public class NotifierTest {

	@Test
	public void testRequestEmailNotice() {
		long tsa = System.currentTimeMillis();
		Emailer.SendEmailError a = new SendEmailError("aaa", "XRP/JPY", tsa);
		Emailer.SendEmailError b = new SendEmailError("aaa", "XRP/JPY", tsa);

		assertEquals(a, b);
		Map<SendEmailError, Long> map = new HashMap<>();
		map.put(a, 1L);
		map.put(b, 2l);
		assertEquals(map.size(), 1);

		Emailer.SendEmailError c = new SendEmailError("cc", "XRP/JPY", tsa);

		assertFalse(a.equals(c));
		map.clear();
		map.put(a, 1L);
		map.put(c, 1L);
		assertEquals(map.size(), 2);
	}

}
