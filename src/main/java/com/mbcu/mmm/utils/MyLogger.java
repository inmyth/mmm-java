package com.mbcu.mmm.utils;

import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class MyLogger {

	private static final String LOG_NAME = "log.ob.txt";
	
	public static void setup() throws IOException {

		// Get the global logger to configure it
		Logger logger = Logger.getLogger("");

		logger.setLevel(Level.ALL);
		FileHandler fileHandler = new FileHandler(LOG_NAME, true);

		// Create txt Formatter
		SimpleFormatter formatter = new SimpleFormatter();
		fileHandler.setFormatter(formatter);
		logger.addHandler(fileHandler);

		Handler consoleHandler = new ConsoleHandler();
		consoleHandler.setLevel(Level.ALL);
		consoleHandler.setFormatter(formatter);
		logger.addHandler(consoleHandler);

	}

	public static Logger getLogger(String name) {
		return Logger.getLogger(name);
	}
}