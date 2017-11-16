package com.mbcu.mmm.utils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.mbcu.mmm.models.internal.Config;

public class MyLogger {

	private static final String LOG_NAME = "log.%s.%s.txt";
	private static final int limit = 1024 * 1024 * 20; // 20 Mb
	private static final int numLogFiles = 20;
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SS");

	public static void setup(Config config, boolean... isConsole) throws IOException {

		// Get the global logger to configure it
		Logger logger = Logger.getLogger("");
		logger.setLevel(Level.ALL);
		String timeStamp = sdf.format(new Date());
		sdf.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));

		String fileName = String.format(LOG_NAME, config.getCredentials().getAddress(), timeStamp);
		FileHandler fileHandler = new FileHandler(fileName, limit, numLogFiles, true);

		// Create txt Formatter
		SimpleFormatter formatter = new SimpleFormatter();
		fileHandler.setFormatter(formatter);
		logger.addHandler(fileHandler);

		if (isConsole.length > 0) {
			if (isConsole[0]) {
				Handler consoleHandler = new ConsoleHandler();
				consoleHandler.setLevel(Level.ALL);
				consoleHandler.setFormatter(formatter);
				logger.addHandler(consoleHandler);
			}
		}
	}

	public static Logger getLogger(String name) {
		return Logger.getLogger(name);
	}

	public static void exception(Logger log, String cause, Exception e) {
		StringBuffer res = new StringBuffer("EXCEPTION\n");
		res.append("object-info start\n");
		res.append(cause);
		res.append("\nobject-info end\ntrace start\n");
		res.append(MyUtils.stackTrace(e));
		res.append("\ntrace end");
		log.log(Level.SEVERE, res.toString());
	}
}