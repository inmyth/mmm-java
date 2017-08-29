package com.mbcu.mmm.utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MyUtils {

	public static String readFile(String path) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, StandardCharsets.UTF_8);
	}

	public static String stackTrace(Throwable t){
		Exception e = (Exception) t;
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		String sStackTrace = sw.toString(); // stack trace as a string
//		System.out.println(sStackTrace);
		pw.close();
		try {
			sw.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return sStackTrace;
	}
	
	public static void toFile(String string, Path path){
		byte[] strToBytes = string.getBytes();
		try {
			Files.write(path, strToBytes);
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
}
