package com.mbcu.mmm.sequences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.mbcu.mmm.sequences.Emailer.SendEmailBotError;

public class NotifierTest {

	@Test
	public void testRequestEmailNotice() {
		long tsa = System.currentTimeMillis();
		Emailer.SendEmailBotError a = new SendEmailBotError("aaa", "XRP/JPY", tsa, false);
		Emailer.SendEmailBotError b = new SendEmailBotError("aaa", "XRP/JPY", tsa, false);

		assertEquals(a, b);
		Map<SendEmailBotError, Long> map = new HashMap<>();
		map.put(a, 1L);
		map.put(b, 2l);
		assertEquals(map.size(), 1);

		Emailer.SendEmailBotError c = new SendEmailBotError("cc", "XRP/JPY", tsa, false);

		assertFalse(a.equals(c));
		map.clear();
		map.put(a, 1L);
		map.put(c, 1L);
		assertEquals(map.size(), 2);
	}

}
