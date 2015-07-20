package com.glview.hwui.cache;

import java.nio.IntBuffer;

import android.opengl.GLES20;

public class FboCache {
	
	final static int MAX_SIZE = 32;
	
	int mSize = 0;
	int[] mCaches = new int[MAX_SIZE];
 
	IntBuffer mIntBuffer = IntBuffer.allocate(1);
	
	public int get() {
		if (mSize <= 0) {
			mIntBuffer.clear();
			GLES20.glGenFramebuffers(1, mIntBuffer);
			return mIntBuffer.get(0);
		}
		return mCaches[mSize --];
	}
	
	public boolean push(int frameBuffer) {
		if (mSize >= MAX_SIZE) {
			mIntBuffer.clear();
			mIntBuffer.put(0, frameBuffer);
			GLES20.glDeleteFramebuffers(1, mIntBuffer);
			return false;
		}
		mCaches[mSize ++] = frameBuffer;
		return true;
	}
}
