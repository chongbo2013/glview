package com.glview.utils;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

import com.glview.Lib;

public class BufferUtils {
	
	static {
		Lib.init();
	}
	
	public static FloatBuffer newFloatBuffer (int numFloats) {
		ByteBuffer buffer = ByteBuffer.allocateDirect(numFloats * 4);
		buffer.order(ByteOrder.nativeOrder());
		return buffer.asFloatBuffer();
	}

	public static DoubleBuffer newDoubleBuffer (int numDoubles) {
		ByteBuffer buffer = ByteBuffer.allocateDirect(numDoubles * 8);
		buffer.order(ByteOrder.nativeOrder());
		return buffer.asDoubleBuffer();
	}

	public static ByteBuffer newByteBuffer (int numBytes) {
		ByteBuffer buffer = ByteBuffer.allocateDirect(numBytes);
		buffer.order(ByteOrder.nativeOrder());
		return buffer;
	}

	public static ShortBuffer newShortBuffer (int numShorts) {
		ByteBuffer buffer = ByteBuffer.allocateDirect(numShorts * 2);
		buffer.order(ByteOrder.nativeOrder());
		return buffer.asShortBuffer();
	}

	public static CharBuffer newCharBuffer (int numChars) {
		ByteBuffer buffer = ByteBuffer.allocateDirect(numChars * 2);
		buffer.order(ByteOrder.nativeOrder());
		return buffer.asCharBuffer();
	}

	public static IntBuffer newIntBuffer (int numInts) {
		ByteBuffer buffer = ByteBuffer.allocateDirect(numInts * 4);
		buffer.order(ByteOrder.nativeOrder());
		return buffer.asIntBuffer();
	}

	public static LongBuffer newLongBuffer (int numLongs) {
		ByteBuffer buffer = ByteBuffer.allocateDirect(numLongs * 8);
		buffer.order(ByteOrder.nativeOrder());
		return buffer.asLongBuffer();
	}
	
	public static void copy (float[] src, Buffer dst, int numFloats, int offset) {
		if (dst instanceof ByteBuffer)
			dst.limit(numFloats << 2);
		else if (dst instanceof FloatBuffer) dst.limit(numFloats);

		copyJni(src, dst, numFloats, offset);
		dst.position(0);
	}
	
	public static void copy(ByteBuffer src, int srcOffset, ByteBuffer dst, int dstOffset, int numBytes) {
		copyJni(src, srcOffset, dst, dstOffset, numBytes);
	}
	
	/** Writes the specified number of zeros to the buffer. This is generally faster than reallocating a new buffer. */
	public static native void clear (ByteBuffer buffer, int numBytes); /*
		memset(buffer, 0, numBytes);
	*/
	
	private native static void copyJni (float[] src, Buffer dst, int numFloats, int offset); /*
		memcpy(dst, src + offset, numFloats << 2 );
	*/

	private native static void copyJni (byte[] src, int srcOffset, Buffer dst, int dstOffset, int numBytes); /*
		memcpy(dst + dstOffset, src + srcOffset, numBytes);
	*/

	private native static void copyJni (char[] src, int srcOffset, Buffer dst, int dstOffset, int numBytes); /*
		memcpy(dst + dstOffset, src + srcOffset, numBytes);
	*/

	private native static void copyJni (short[] src, int srcOffset, Buffer dst, int dstOffset, int numBytes); /*
		memcpy(dst + dstOffset, src + srcOffset, numBytes);
	 */

	private native static void copyJni (int[] src, int srcOffset, Buffer dst, int dstOffset, int numBytes); /*
		memcpy(dst + dstOffset, src + srcOffset, numBytes);
	*/
	
	private native static void copyJni (long[] src, int srcOffset, Buffer dst, int dstOffset, int numBytes); /*
		memcpy(dst + dstOffset, src + srcOffset, numBytes);
	*/

	private native static void copyJni (float[] src, int srcOffset, Buffer dst, int dstOffset, int numBytes); /*
		memcpy(dst + dstOffset, src + srcOffset, numBytes);
	*/
	
	private native static void copyJni (double[] src, int srcOffset, Buffer dst, int dstOffset, int numBytes); /*
		memcpy(dst + dstOffset, src + srcOffset, numBytes);
	*/
	
	private native static void copyJni (Buffer src, int srcOffset, Buffer dst, int dstOffset, int numBytes); /*
		memcpy(dst + dstOffset, src + srcOffset, numBytes);
	*/

}
