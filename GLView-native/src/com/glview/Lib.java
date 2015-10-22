package com.glview;

public class Lib {
	
	private static boolean inited = false;

	public final static synchronized void init() {
		if (!inited) {
			inited = true;
			System.loadLibrary("GLView-native");
		}
	}
}
