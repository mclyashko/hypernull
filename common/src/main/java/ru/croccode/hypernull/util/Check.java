package ru.croccode.hypernull.util;

import java.util.Collection;

public final class Check {
	
	private Check() {
	}
	
	public static <T> T notNull(T reference) {
		return notNull(reference, null);
	}

	public static <T> T notNull(T reference, String message) {
		if (reference == null) {
			throw new NullPointerException(message != null
					? message
					: "Object reference is null");
		}
		return reference;
	}

	public static String notEmpty(String str) {
		return notEmpty(str, null);
	}

	public static String notEmpty(String str, String message) {
		if (str == null || str.isEmpty()) {
			throw new IllegalArgumentException(message != null
					? message
					: "String is empty");
		}
		return str;
	}

	public static Collection<?> notEmpty(Collection<?> collection) {
		notNull(collection);
		condition(!collection.isEmpty());
		return collection;
	}

	public static void condition(boolean condition) {
		condition(condition, null);
	}

	public static void condition(boolean condition, String message) {
		if (!condition) {
			throw new IllegalArgumentException(message != null
					? message
					: "Condition violated");
		}
	}
}
