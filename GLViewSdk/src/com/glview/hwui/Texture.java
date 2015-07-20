package com.glview.hwui;

public class Texture {
	
	public int mId;
	
	public int mGenerationId;
	
	int mWidth, mHeight;
	boolean mMipMap;
	
	int mByteCount;
	
	public int getWidth() {
		return mWidth;
	}
	
	public void setWidth(int width) {
		mWidth = width;
	}
	
	public int getHeight() {
		return mHeight;
	}
	
	public void setHeight(int height) {
		mHeight = height;
	}
	
	public boolean isMipMap() {
		return mMipMap;
	}
	
	public void setMipMap(boolean mipMap) {
		mMipMap = mipMap;
	}
	
	public void setByteCount(int byteCount) {
		mByteCount = byteCount;
	}
	
	public int getByteCount() {
		return mByteCount;
	}
	
}
