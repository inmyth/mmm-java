package com.mbcu.mmm.utils;

import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

public class GsonUtils {
	public static <T> T toBean(String response, Class<T> classOfT) {
		GsonBuilder builder = new GsonBuilder();
		// builder.excludeFieldsWithoutExposeAnnotation();
		Gson gson = builder.create();
		try {
			return gson.fromJson(response, classOfT);
		} catch (JsonSyntaxException e) {
			return null;
		}
	}

	public static String toJson(Object object) {
		GsonBuilder builder = new GsonBuilder();
		builder.setPrettyPrinting();
		Gson gson = builder.create();
		String json = gson.toJson(object);
		return json;
	}

	public static <T> T toBean(String response, Type t) {
		GsonBuilder builder = new GsonBuilder();
		// builder.excludeFieldsWithoutExposeAnnotation();
		Gson gson = builder.create();
		try {
			return gson.fromJson(response, t);
		} catch (JsonSyntaxException e) {
			return null;
		}
	}
}