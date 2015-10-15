package com.glview.hwui.font;

import java.nio.ByteBuffer;

import com.glview.App;
import com.glview.hwui.Caches;
import com.glview.libgdx.graphics.opengl.GL;
import com.glview.libgdx.graphics.opengl.GL20;

public class PixelBuffer {
	
	final int mFormat;
	final int mWidth, mHeight;
	
	final ByteBuffer mByteBuffer;
	
	final Caches mCaches;
	final GL mGL;
	
	private PixelBuffer(int format, int width, int height) {
		mFormat = format;
		mWidth = width;
		mHeight = height;
		
		mByteBuffer = ByteBuffer.allocateDirect(width * height * formatSize(format));
		
		mCaches = Caches.getInstance();
		mGL = App.getGL20();
	}
	
	public static PixelBuffer create(int format, int width, int height) {
		return new PixelBuffer(format, width, height);
	}
	
	public void upload(int x, int y, int width, int height) {
		upload(x, y, width, height, getOffset(x, y));
	}
	
	public void upload(int x, int y, int width, int height, int offset) {
		mByteBuffer.position(offset);
		mGL.glTexSubImage2D(GL20.GL_TEXTURE_2D, 0, x, y, width, height,
				mFormat, GL20.GL_UNSIGNED_BYTE, mByteBuffer);
	}
	
	/**
     * Returns the offset of a pixel in this pixel buffer, in bytes.
     */
    int getOffset(int x, int y) {
        return (y * mWidth + x) * formatSize(mFormat);
    }
	
	public ByteBuffer map() {
		return mByteBuffer;
	}
	
    static int formatSize(int format) {
        switch (format) {
            case GL20.GL_ALPHA:
                return 1;
            case GL20.GL_RGBA:
                return 4;
        }
        return 0;
    }

}
