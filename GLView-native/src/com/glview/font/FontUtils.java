package com.glview.font;

import java.nio.ByteBuffer;

import com.glview.Lib;

public class FontUtils {

	static {
		Lib.init();
	}
	
	public final static native void loadGlyphBitmap(ByteBuffer src, int srcWidth, int srcHeight, int pitch, int border,
			ByteBuffer dst, int dstWidth, int dstHeight, int xOffset, int yOffset);
	
	public final static native void loadGlyphBitmap(byte[] src, int srcWidth, int srcHeight, int pitch, int border,
			ByteBuffer dst, int dstWidth, int dstHeight, int xOffset, int yOffset);
	
	public final static native void loadGlyphBlurBitmap(ByteBuffer src, int width, int height, int pitch,
			ByteBuffer dst, int radius);
	
	public final static native void loadGlyphBlurBitmap(ByteBuffer src, int width, int height, int pitch,
			byte[] dst, int radius);

}
