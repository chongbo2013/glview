package com.glview.utils;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import com.glview.Lib;

public class BufferUtils {
	
	static {
		Lib.init();
	}
	
	public static void copy(ByteBuffer src, int srcOffset, ByteBuffer dst, int dstOffset, int numBytes) {
		copyJni(src, srcOffset, dst, dstOffset, numBytes);
	}
	
	private native static void copyJni (Buffer src, int srcOffset, Buffer dst, int dstOffset, int numBytes); /*
		memcpy(dst + dstOffset, src + srcOffset, numBytes);
	*/

}
