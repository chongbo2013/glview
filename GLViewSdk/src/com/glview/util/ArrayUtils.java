package com.glview.util;

public class ArrayUtils {
	
	public static int[] concat(int[] a, int[] b) {
		int[] dst = new  int[a.length + b.length];
		System.arraycopy(a, 0, dst, 0, a.length);
		System.arraycopy(b, 0, dst, a.length, b.length);
		return dst;
	}

}
