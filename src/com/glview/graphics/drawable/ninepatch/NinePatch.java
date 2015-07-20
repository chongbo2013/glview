package com.glview.graphics.drawable.ninepatch;

import com.glview.graphics.Bitmap;
import com.glview.graphics.Rect;
import com.glview.hwui.GLCanvas;
import com.glview.hwui.GLPaint;

public class NinePatch {

	private final Bitmap mBitmap;
	private final NinePatchChunk mChunk;

	private String mSrcName;
	
	/**
     * Create a drawable projection from a bitmap to nine patches.
     *
     * @param bitmap The bitmap describing the patches.
     * @param chunk The 9-patch data chunk describing how the underlying bitmap
     *              is split apart and drawn.
     */
    public NinePatch(Bitmap bitmap, byte[] chunk) {
        this(bitmap, chunk, null);
    }
    
    /** 
     * Create a drawable projection from a bitmap to nine patches.
     *
     * @param bitmap The bitmap describing the patches.
     * @param chunk The 9-patch data chunk describing how the underlying
     *              bitmap is split apart and drawn.
     * @param srcName The name of the source for the bitmap. Might be null.
     */
	public NinePatch(Bitmap bitmap, byte[] trunk, String srcName) {
		mChunk = NinePatchChunk.deserialize(trunk);
		if (mChunk == null) throw new RuntimeException("Chunk deserialize failed.");
		mBitmap = bitmap;
		mSrcName = srcName;
	}
	
	public NinePatchChunk getChunk() {
		return mChunk;
	}
	
	public Bitmap getBitmap() {
		return mBitmap;
	}
	
	/**
     * Returns the name of this NinePatch object if one was specified
     * when calling the constructor.
     */
    public String getName() {
        return mSrcName;
    }
    
    /**
     * Return the underlying bitmap's density, as per
     * {@link Bitmap#getDensity() Bitmap.getDensity()}.
     */
    public int getDensity() {
        return mBitmap.getDensity();
    }

    /**
     * Returns the intrinsic width, in pixels, of this NinePatch. This is equivalent
     * to querying the width of the underlying bitmap returned by {@link #getBitmap()}.
     */
    public int getWidth() {
        return mBitmap.getWidth();
    }

    /**
     * Returns the intrinsic height, in pixels, of this NinePatch. This is equivalent
     * to querying the height of the underlying bitmap returned by {@link #getBitmap()}.
     */
    public int getHeight() {
        return mBitmap.getHeight();
    }

    /**
     * Indicates whether this NinePatch contains transparent or translucent pixels.
     * This is equivalent to calling <code>getBitmap().hasAlpha()</code> on this
     * NinePatch.
     */
    public final boolean hasAlpha() {
        return mBitmap.hasAlpha();
    }
    
    public final void draw(GLCanvas canvas, Rect rect, GLPaint paint) {
    	canvas.drawPatch(this, rect, paint);
    }
}
