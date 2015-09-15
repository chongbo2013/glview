package com.glview.graphics;

import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Build;
import android.util.DisplayMetrics;

public class Bitmap {
	
	/**
     * Indicates that the bitmap was created for an unknown pixel density.
     *
     * @see Bitmap#getDensity()
     * @see Bitmap#setDensity(int)
     */
    public static final int DENSITY_NONE = 0;
    
	private android.graphics.Bitmap mBitmap;
	private byte[] lock = new byte[0];
	
	private int mWidth;
    private int mHeight;
    private boolean mRecycled;
	private int mGenerationId;
	private int mAllocationByteCount;
	private int mByteCount;
	private int mRowBytes;
	private byte[] mNinePatchChunk;
	private boolean mIsMutable;
	private boolean mHasMipMap;
	private boolean mHasAlpha;
	
	// Package-scoped for fast access.
    int mDensity = getDefaultDensity();

    private static volatile int sDefaultDensity = -1;
    
    /**
     * For backwards compatibility, allows the app layer to change the default
     * density when running old apps.
     * @hide
     */
    public static void setDefaultDensity(int density) {
        sDefaultDensity = density;
    }

    static int getDefaultDensity() {
        if (sDefaultDensity >= 0) {
            return sDefaultDensity;
        }
        //noinspection deprecation
        sDefaultDensity = DisplayMetrics.DENSITY_DEFAULT;
        return sDefaultDensity;
    }
	
	public Bitmap() {
	}
	
	public Bitmap(android.graphics.Bitmap bitmap) {
		setBitmap(bitmap);
	}
	
	protected final void setBitmap(android.graphics.Bitmap bitmap) {
		synchronized (lock) {
			this.mBitmap = bitmap;
			initialProperties();
		}
	}
	
	public final android.graphics.Bitmap getBitmap() {
		synchronized (lock) {
			if (mBitmap == null) {
				setBitmap(onGotBitmap());
			}
			return mBitmap;
		}
	}
	
	private void initialProperties() {
		if (mBitmap != null) {
			mWidth = mBitmap.getWidth();
			mHeight = mBitmap.getHeight();
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
				mGenerationId = mBitmap.getGenerationId();
			} else {
				mGenerationId = 0;
			}
			mByteCount = mBitmap.getByteCount();
			mAllocationByteCount = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT ? mBitmap.getAllocationByteCount() : mByteCount;
			mRowBytes = mBitmap.getRowBytes();
			mDensity = mBitmap.getDensity();
			mNinePatchChunk = mBitmap.getNinePatchChunk();
			mRecycled = mBitmap.isRecycled();
			mIsMutable = mBitmap.isMutable();
			mHasAlpha = mBitmap.hasAlpha();
//			mHasMipMap = BitmapCompat.hasMipMap(mBitmap);
		}
	}
	
	public void invalidate() {
		synchronized (lock) {
			initialProperties();
		}
	}
	
	protected android.graphics.Bitmap onGotBitmap() {
		return null;
	}

	public int getWidth() {
		return mWidth;
	}

	public int getHeight() {
		return mHeight;
	}

	public int getGenerationId() {
		synchronized (lock) {
			if (mBitmap != null) {
				return mBitmap.getGenerationId();
			}
		}
		return mGenerationId;
	}

	public int getAllocationByteCount() {
		return mAllocationByteCount;
	}

	public int getByteCount() {
		return mByteCount;
	}

	public int getRowBytes() {
		return mRowBytes;
	}

	public int getDensity() {
		return mDensity;
	}

	public byte[] getNinePatchChunk() {
		return mNinePatchChunk;
	}
	
	public boolean isRecycled() {
		return mRecycled;
	}
	
	public boolean isMutable() {
		return mIsMutable;
	}
	
	/**
     * Indicates whether the renderer responsible for drawing this
     * bitmap should attempt to use mipmaps when this bitmap is drawn
     * scaled down.
     * 
     * If you know that you are going to draw this bitmap at less than
     * 50% of its original size, you may be able to obtain a higher
     * quality
     * 
     * This property is only a suggestion that can be ignored by the
     * renderer. It is not guaranteed to have any effect.
     * 
     * @return true if the renderer should attempt to use mipmaps,
     *         false otherwise
     * 
     * @see #setHasMipMap(boolean)
     */
    public final boolean hasMipMap() {
        return mHasMipMap;
    }

    /**
     * Set a hint for the renderer responsible for drawing this bitmap
     * indicating that it should attempt to use mipmaps when this bitmap
     * is drawn scaled down.
     *
     * If you know that you are going to draw this bitmap at less than
     * 50% of its original size, you may be able to obtain a higher
     * quality by turning this property on.
     * 
     * Note that if the renderer respects this hint it might have to
     * allocate extra memory to hold the mipmap levels for this bitmap.
     *
     * This property is only a suggestion that can be ignored by the
     * renderer. It is not guaranteed to have any effect.
     *
     * @param hasMipMap indicates whether the renderer should attempt
     *                  to use mipmaps
     *
     * @see #hasMipMap()
     */
    public final void setHasMipMap(boolean hasMipMap) {
    	mHasMipMap = hasMipMap;
    }
	
	public boolean hasAlpha() {
		synchronized (lock) {
			if (mBitmap != null) {
				return mBitmap.hasAlpha();
			}
		}
		return mHasAlpha;
	}
	
	public void recycle() {
		synchronized (lock) {
			if (mBitmap != null && !mBitmap.isRecycled()) {
				mBitmap.recycle();
			}
			mBitmap = null;
		}
		mRecycled = true;
	}
	
	/**
     * Convenience for calling {@link #getScaledWidth(int)} with the target
     * density of the given {@link Canvas}.
     */
    public int getScaledWidth(Canvas canvas) {
        return scaleFromDensity(getWidth(), mDensity, canvas.getDensity());
    }

    /**
     * Convenience for calling {@link #getScaledHeight(int)} with the target
     * density of the given {@link Canvas}.
     */
    public int getScaledHeight(Canvas canvas) {
        return scaleFromDensity(getHeight(), mDensity, canvas.getDensity());
    }

    /**
     * Convenience for calling {@link #getScaledWidth(int)} with the target
     * density of the given {@link DisplayMetrics}.
     */
    public int getScaledWidth(DisplayMetrics metrics) {
        return scaleFromDensity(getWidth(), mDensity, metrics.densityDpi);
    }

    /**
     * Convenience for calling {@link #getScaledHeight(int)} with the target
     * density of the given {@link DisplayMetrics}.
     */
    public int getScaledHeight(DisplayMetrics metrics) {
        return scaleFromDensity(getHeight(), mDensity, metrics.densityDpi);
    }

    /**
     * Convenience method that returns the width of this bitmap divided
     * by the density scale factor.
     *
     * @param targetDensity The density of the target canvas of the bitmap.
     * @return The scaled width of this bitmap, according to the density scale factor.
     */
    public int getScaledWidth(int targetDensity) {
        return scaleFromDensity(getWidth(), mDensity, targetDensity);
    }

    /**
     * Convenience method that returns the height of this bitmap divided
     * by the density scale factor.
     *
     * @param targetDensity The density of the target canvas of the bitmap.
     * @return The scaled height of this bitmap, according to the density scale factor.
     */
    public int getScaledHeight(int targetDensity) {
        return scaleFromDensity(getHeight(), mDensity, targetDensity);
    }
    
    /**
     * @hide
     */
    static public int scaleFromDensity(int size, int sdensity, int tdensity) {
        if (sdensity == DENSITY_NONE || tdensity == DENSITY_NONE || sdensity == tdensity) {
            return size;
        }
        
        // Scale by tdensity / sdensity, rounding up.
        return ((size * tdensity) + (sdensity >> 1)) / sdensity;
    }
	
    /**
     * Returns a mutable bitmap with the specified width and height.  Its
     * initial density is as per {@link #getDensity}.
     *
     * @param width    The width of the bitmap
     * @param height   The height of the bitmap
     * @param config   The bitmap config to create.
     * @throws IllegalArgumentException if the width or height are <= 0
     */
    public static Bitmap createBitmap(int width, int height, Config config) {
        return new Bitmap(android.graphics.Bitmap.createBitmap(width, height, config));
    }
    
    /**
     * Returns a immutable bitmap with the specified width and height, with each
     * pixel value set to the corresponding value in the colors array.  Its
     * initial density is as per {@link #getDensity}.
     *
     * @param colors   Array of {@link Color} used to initialize the pixels.
     * @param offset   Number of values to skip before the first color in the
     *                 array of colors.
     * @param stride   Number of colors in the array between rows (must be >=
     *                 width or <= -width).
     * @param width    The width of the bitmap
     * @param height   The height of the bitmap
     * @param config   The bitmap config to create. If the config does not
     *                 support per-pixel alpha (e.g. RGB_565), then the alpha
     *                 bytes in the colors[] will be ignored (assumed to be FF)
     * @throws IllegalArgumentException if the width or height are <= 0, or if
     *         the color array's length is less than the number of pixels.
     */
    public static Bitmap createBitmap(int colors[], int offset, int stride,
            int width, int height, Config config) {
    	return new Bitmap(android.graphics.Bitmap.createBitmap(colors, offset, stride, width, height, config));
    }
    
    /**
     * Returns a immutable bitmap with the specified width and height, with each
     * pixel value set to the corresponding value in the colors array.  Its
     * initial density is as per {@link #getDensity}.
     *
     * @param colors   Array of {@link Color} used to initialize the pixels.
     *                 This array must be at least as large as width * height.
     * @param width    The width of the bitmap
     * @param height   The height of the bitmap
     * @param config   The bitmap config to create. If the config does not
     *                 support per-pixel alpha (e.g. RGB_565), then the alpha
     *                 bytes in the colors[] will be ignored (assumed to be FF)
     * @throws IllegalArgumentException if the width or height are <= 0, or if
     *         the color array's length is less than the number of pixels.
     */
    public static Bitmap createBitmap(int colors[], int width, int height, Config config) {
    	return new Bitmap(android.graphics.Bitmap.createBitmap(colors, width, height, config));
    }
    
    /**
     * <p>Modifies the bitmap to have a specified width, height, and {@link
     * Config}, without affecting the underlying allocation backing the bitmap.
     * Bitmap pixel data is not re-initialized for the new configuration.</p>
     *
     * <p>This method can be used to avoid allocating a new bitmap, instead
     * reusing an existing bitmap's allocation for a new configuration of equal
     * or lesser size. If the Bitmap's allocation isn't large enough to support
     * the new configuration, an IllegalArgumentException will be thrown and the
     * bitmap will not be modified.</p>
     *
     * <p>The result of {@link #getByteCount()} will reflect the new configuration,
     * while {@link #getAllocationByteCount()} will reflect that of the initial
     * configuration.</p>
     *
     * <p>Note: This may change this result of hasAlpha(). When converting to 565,
     * the new bitmap will always be considered opaque. When converting from 565,
     * the new bitmap will be considered non-opaque, and will respect the value
     * set by setPremultiplied().</p>
     *
     * <p>WARNING: This method should NOT be called on a bitmap currently used
     * by the view system. It does not make guarantees about how the underlying
     * pixel buffer is remapped to the new config, just that the allocation is
     * reused. Additionally, the view system does not account for bitmap
     * properties being modifying during use, e.g. while attached to
     * drawables.</p>
     *
     * @see #setWidth(int)
     * @see #setHeight(int)
     * @see #setConfig(Config)
     */
    public void reconfigure(int width, int height, Config config) {
    	synchronized (lock) {
	        if (mBitmap != null) {
	        	mBitmap.reconfigure(width, height, config);
	        	initialProperties();
	        } else {
	        	 throw new IllegalArgumentException("bitmap has been recycled.");
	        }
    	}
    }
    
    protected boolean desireFreeBitmap(){
		return false;
	}
    
    /**
     * TODO 需要特别注意bitmap释放后，如果外部修改了bitmap，需要重新bind纹理
     * @hide
     */
    public final void freeBitmap() {
    	if (desireFreeBitmap()) {
    		synchronized (lock) {
    			mBitmap = null;
    		}
    		onFreeBitmap();
    	}
    }
    
    protected void onFreeBitmap() {
	}
    
    private final String mCacheKey = super.toString();
    public final String generateCacheKey() {
    	return mCacheKey;
    }
}
