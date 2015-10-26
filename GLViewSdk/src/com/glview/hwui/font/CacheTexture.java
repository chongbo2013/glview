package com.glview.hwui.font;

import com.glview.graphics.Rect;
import com.glview.hwui.Caches;
import com.glview.hwui.Texture;
import com.glview.hwui.packer.Packer;
import com.glview.libgdx.graphics.opengl.GL20;

class CacheTexture {
	
	boolean mHasUnpackRowLength = false;
	
	PixelBuffer mPixelBuffer;
	Texture mTexture;
	
	Caches mCaches;
	
	final int mFormat;
	final int mWidth, mHeight;
	boolean mDirty = false;
	
	Rect mDirtyRect = new Rect();
	
	final Packer mPacker;
	final FontRenderer mFontRenderer;
	
	FontBatch mFontBatch;

	public CacheTexture(FontRenderer fontRenderer, int width, int height, int format) {
		mFontRenderer = fontRenderer;
		mWidth = width;
		mHeight = height;
		mFormat = format;
		mCaches = Caches.getInstance();
		
		mPacker = new ColumnBasePacker(mWidth, mHeight);
		
		mTexture = new Texture();
		mTexture.setWidth(width);
		mTexture.setHeight(height);
		mTexture.setFormat(format);
		mTexture.setType(GL20.GL_UNSIGNED_BYTE);
	}
	
	public int getWidth() {
		return mWidth;
	}
	
	public int getHeight() {
		return mHeight;
	}
	
	public PixelBuffer getPixelBuffer() {
		return mPixelBuffer;
	}
	
	void allocateTexture() {
	    if (mPixelBuffer == null) {
	    	mPixelBuffer = PixelBuffer.create(mFormat, mWidth, mHeight);
	    }
	    if (mTexture.mId <= 0) {
	    	mCaches.textureCache.generateTexture(mTexture);
	    }
	}
	
	void release() {
		if (mFontBatch != null) {
			mFontBatch.dispose();
		}
		mPacker.reset();
		mFontBatch = null;
		mCaches.deleteTexture(mTexture);
		mPixelBuffer = null;
	}
	
	void allocateMesh() {
		if (mPixelBuffer == null) {
			allocateTexture();
		}
		if (mFontBatch == null) {
			mFontBatch = new FontBatch(mFontRenderer, this);
		}
	}
	
	boolean upload() {
		if (!isDirty()) return false;
	    final Rect dirtyRect = mDirtyRect;

	    int x = mHasUnpackRowLength ? dirtyRect.left : 0;
	    int y = dirtyRect.top;
	    int width = mHasUnpackRowLength ? dirtyRect.width() : mWidth;
	    int height = dirtyRect.height();

	    // The unpack row length only needs to be specified when a new
	    // texture is bound
//	    if (mHasUnpackRowLength) {
//	        glPixelStorei(GL_UNPACK_ROW_LENGTH, mWidth);
//	    }
	    mCaches.bindTexture(mTexture);
	    mPixelBuffer.upload(x, y, width, height);
	    setDirty(false);

	    return mHasUnpackRowLength;
	}
	
	void setDirty(boolean dirty) {
		mDirty = dirty;
		if (!dirty) {
			mDirtyRect.setEmpty();
		}
	}
	
	boolean isDirty() {
		return mDirty;
	}

}
