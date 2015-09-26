/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.glview.graphics.drawable;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.PixelFormat;
import android.os.Build;
import android.util.AttributeSet;
import android.util.DisplayMetrics;

import com.glview.R;
import com.glview.graphics.Bitmap;
import com.glview.graphics.BitmapFactory;
import com.glview.graphics.Rect;
import com.glview.graphics.bitmap.ResourceBitmap;
import com.glview.graphics.shader.BitmapShader;
import com.glview.graphics.shader.TileMode;
import com.glview.hwui.GLCanvas;
import com.glview.hwui.GLPaint;
import com.glview.util.ArrayUtils;
import com.glview.view.Gravity;

/**
 * A Drawable that wraps a bitmap and can be tiled, stretched, or aligned. You can create a
 * BitmapDrawable from a file path, an input stream, through XML inflation, or from
 * a {@link android.graphics.Bitmap} object.
 * <p>It can be defined in an XML file with the <code>&lt;bitmap></code> element.  For more
 * information, see the guide to <a
 * href="{@docRoot}guide/topics/resources/drawable-resource.html">Drawable Resources</a>.</p>
 * <p>
 * Also see the {@link android.graphics.Bitmap} class, which handles the management and
 * transformation of raw bitmap graphics, and should be used when drawing to a
 * {@link android.graphics.Canvas}.
 * </p>
 *
 * @attr ref android.R.styleable#BitmapDrawable_src
 * @attr ref android.R.styleable#BitmapDrawable_antialias
 * @attr ref android.R.styleable#BitmapDrawable_filter
 * @attr ref android.R.styleable#BitmapDrawable_dither
 * @attr ref android.R.styleable#BitmapDrawable_gravity
 * @attr ref android.R.styleable#BitmapDrawable_mipMap
 * @attr ref android.R.styleable#BitmapDrawable_tileMode
 */
public class BitmapDrawable extends Drawable {

    // Constants for {@link android.R.styleable#BitmapDrawable_tileMode}.
    private static final int TILE_MODE_UNDEFINED = -2;
    private static final int TILE_MODE_DISABLED = -1;
    private static final int TILE_MODE_CLAMP = 0;
    private static final int TILE_MODE_REPEAT = 1;
    private static final int TILE_MODE_MIRROR = 2;

    private final Rect mDstRect = new Rect();   // #updateDstRectAndInsetsIfDirty() sets this

    private BitmapState mBitmapState;

    private int mTargetDensity = DisplayMetrics.DENSITY_DEFAULT;

    private boolean mDstRectAndInsetsDirty = true;
    private boolean mMutated;

     // These are scaled to match the target density.
    private int mBitmapWidth;
    private int mBitmapHeight;

    /** Optical insets due to gravity. */
    private Insets mOpticalInsets = Insets.NONE;

    /**
     * Create an empty drawable, not dealing with density.
     * @deprecated Use {@link #BitmapDrawable(android.content.res.Resources, android.graphics.Bitmap)}
     * instead to specify a bitmap to draw with and ensure the correct density is set.
     */
    @Deprecated
    public BitmapDrawable() {
        mBitmapState = new BitmapState((Bitmap) null);
    }

    /**
     * Create an empty drawable, setting initial target density based on
     * the display metrics of the resources.
     *
     * @deprecated Use {@link #BitmapDrawable(android.content.res.Resources, android.graphics.Bitmap)}
     * instead to specify a bitmap to draw with.
     */
    @SuppressWarnings("unused")
    @Deprecated
    public BitmapDrawable(Resources res) {
        mBitmapState = new BitmapState((Bitmap) null);
        mBitmapState.mTargetDensity = mTargetDensity;
    }

    /**
     * Create drawable from a bitmap, not dealing with density.
     * @deprecated Use {@link #BitmapDrawable(Resources, Bitmap)} to ensure
     * that the drawable has correctly set its target density.
     */
    @Deprecated
    public BitmapDrawable(Bitmap bitmap) {
        this(new BitmapState(bitmap), null);
    }

    /**
     * Create drawable from a bitmap, setting initial target density based on
     * the display metrics of the resources.
     */
    public BitmapDrawable(Resources res, Bitmap bitmap) {
        this(new BitmapState(bitmap), res);
        mBitmapState.mTargetDensity = mTargetDensity;
    }

    /**
     * Create a drawable by opening a given file path and decoding the bitmap.
     * @deprecated Use {@link #BitmapDrawable(Resources, String)} to ensure
     * that the drawable has correctly set its target density.
     */
    @Deprecated
    public BitmapDrawable(String filepath) {
        this(new BitmapState(BitmapFactory.decodeFile(filepath)), null);
        if (mBitmapState.mBitmap == null) {
            android.util.Log.w("BitmapDrawable", "BitmapDrawable cannot decode " + filepath);
        }
    }

    /**
     * Create a drawable by opening a given file path and decoding the bitmap.
     */
    @SuppressWarnings("unused")
    public BitmapDrawable(Resources res, String filepath) {
        this(new BitmapState(BitmapFactory.decodeFile(filepath)), null);
        mBitmapState.mTargetDensity = mTargetDensity;
        if (mBitmapState.mBitmap == null) {
            android.util.Log.w("BitmapDrawable", "BitmapDrawable cannot decode " + filepath);
        }
    }

    /**
     * Create a drawable by decoding a bitmap from the given input stream.
     * @deprecated Use {@link #BitmapDrawable(Resources, java.io.InputStream)} to ensure
     * that the drawable has correctly set its target density.
     */
    @Deprecated
    public BitmapDrawable(java.io.InputStream is) {
        this(new BitmapState(BitmapFactory.decodeStream(is)), null);
        if (mBitmapState.mBitmap == null) {
            android.util.Log.w("BitmapDrawable", "BitmapDrawable cannot decode " + is);
        }
    }

    /**
     * Create a drawable by decoding a bitmap from the given input stream.
     */
    @SuppressWarnings("unused")
    public BitmapDrawable(Resources res, java.io.InputStream is) {
        this(new BitmapState(BitmapFactory.decodeStream(is)), null);
        mBitmapState.mTargetDensity = mTargetDensity;
        if (mBitmapState.mBitmap == null) {
            android.util.Log.w("BitmapDrawable", "BitmapDrawable cannot decode " + is);
        }
    }

    /**
     * Returns the paint used to render this drawable.
     */
    public final GLPaint getPaint() {
        return mBitmapState.mPaint;
    }

    /**
     * Returns the bitmap used by this drawable to render. May be null.
     */
    public final Bitmap getBitmap() {
        return mBitmapState.mBitmap;
    }

    private void computeBitmapSize() {
        final Bitmap bitmap = mBitmapState.mBitmap;
        if (bitmap != null) {
            mBitmapWidth = bitmap.getScaledWidth(mTargetDensity);
            mBitmapHeight = bitmap.getScaledHeight(mTargetDensity);
        } else {
            mBitmapWidth = mBitmapHeight = -1;
        }
    }

    private void setBitmap(Bitmap bitmap) {
        if (mBitmapState.mBitmap != bitmap) {
            mBitmapState.mBitmap = bitmap;
            computeBitmapSize();
            invalidateSelf();
        }
    }

    /**
     * Set the density scale at which this drawable will be rendered. This
     * method assumes the drawable will be rendered at the same density as the
     * specified canvas.
     *
     * @param canvas The Canvas from which the density scale must be obtained.
     *
     * @see android.graphics.Bitmap#setDensity(int)
     * @see android.graphics.Bitmap#getDensity()
     */
    /*public void setTargetDensity(Canvas canvas) {
        setTargetDensity(canvas.getDensity());
    }*/

    /**
     * Set the density scale at which this drawable will be rendered.
     *
     * @param metrics The DisplayMetrics indicating the density scale for this drawable.
     *
     * @see android.graphics.Bitmap#setDensity(int)
     * @see android.graphics.Bitmap#getDensity()
     */
    public void setTargetDensity(DisplayMetrics metrics) {
        setTargetDensity(metrics.densityDpi);
    }

    /**
     * Set the density at which this drawable will be rendered.
     *
     * @param density The density scale for this drawable.
     *
     * @see android.graphics.Bitmap#setDensity(int)
     * @see android.graphics.Bitmap#getDensity()
     */
    public void setTargetDensity(int density) {
        if (mTargetDensity != density) {
            mTargetDensity = density == 0 ? DisplayMetrics.DENSITY_DEFAULT : density;
            if (mBitmapState.mBitmap != null) {
                computeBitmapSize();
            }
            invalidateSelf();
        }
    }

    /** Get the gravity used to position/stretch the bitmap within its bounds.
     * See android.view.Gravity
     * @return the gravity applied to the bitmap
     */
    public int getGravity() {
        return mBitmapState.mGravity;
    }

    /** Set the gravity used to position/stretch the bitmap within its bounds.
        See android.view.Gravity
     * @param gravity the gravity
     */
    public void setGravity(int gravity) {
        if (mBitmapState.mGravity != gravity) {
            mBitmapState.mGravity = gravity;
            mDstRectAndInsetsDirty = true;
            invalidateSelf();
        }
    }

    /**
     * Enables or disables the mipmap hint for this drawable's bitmap.
     * See {@link Bitmap#setHasMipMap(boolean)} for more information.
     *
     * If the bitmap is null calling this method has no effect.
     *
     * @param mipMap True if the bitmap should use mipmaps, false otherwise.
     *
     * @see #hasMipMap()
     */
    public void setMipMap(boolean mipMap) {
        if (mBitmapState.mBitmap != null) {
        	mBitmapState.mBitmap.setHasMipMap(mipMap);
            invalidateSelf();
        }
    }

    /**
     * Indicates whether the mipmap hint is enabled on this drawable's bitmap.
     *
     * @return True if the mipmap hint is set, false otherwise. If the bitmap
     *         is null, this method always returns false.
     *
     * @see #setMipMap(boolean)
     * @attr ref android.R.styleable#BitmapDrawable_mipMap
     */
    public boolean hasMipMap() {
        return mBitmapState.mBitmap != null && mBitmapState.mBitmap.hasMipMap();
    }

    /**
     * Enables or disables anti-aliasing for this drawable. Anti-aliasing affects
     * the edges of the bitmap only so it applies only when the drawable is rotated.
     *
     * @param aa True if the bitmap should be anti-aliased, false otherwise.
     *
     * @see #hasAntiAlias()
     */
    public void setAntiAlias(boolean aa) {
//        mBitmapState.mPaint.setAntiAlias(aa);
        invalidateSelf();
    }

    /**
     * Indicates whether anti-aliasing is enabled for this drawable.
     *
     * @return True if anti-aliasing is enabled, false otherwise.
     *
     * @see #setAntiAlias(boolean)
     */
    public boolean hasAntiAlias() {
        return false;//mBitmapState.mPaint.isAntiAlias();
    }

    @Override
    public void setFilterBitmap(boolean filter) {
//        mBitmapState.mPaint.setFilterBitmap(filter);
        invalidateSelf();
    }

    @Override
    public void setDither(boolean dither) {
//        mBitmapState.mPaint.setDither(dither);
        invalidateSelf();
    }

    /**
     * Indicates the repeat behavior of this drawable on the X axis.
     *
     * @return {@link android.graphics.Shader.TileMode#CLAMP} if the bitmap does not repeat,
     *         {@link android.graphics.Shader.TileMode#REPEAT} or
     *         {@link android.graphics.Shader.TileMode#MIRROR} otherwise.
     */
    public TileMode getTileModeX() {
        return mBitmapState.mTileModeX;
    }

    /**
     * Indicates the repeat behavior of this drawable on the Y axis.
     *
     * @return {@link android.graphics.Shader.TileMode#CLAMP} if the bitmap does not repeat,
     *         {@link android.graphics.Shader.TileMode#REPEAT} or
     *         {@link android.graphics.Shader.TileMode#MIRROR} otherwise.
     */
    public TileMode getTileModeY() {
        return mBitmapState.mTileModeY;
    }

    /**
     * Sets the repeat behavior of this drawable on the X axis. By default, the drawable
     * does not repeat its bitmap. Using {@link android.graphics.Shader.TileMode#REPEAT} or
     * {@link android.graphics.Shader.TileMode#MIRROR} the bitmap can be repeated (or tiled)
     * if the bitmap is smaller than this drawable.
     *
     * @param mode The repeat mode for this drawable.
     *
     * @see #setTileModeY(android.graphics.Shader.TileMode)
     * @see #setTileModeXY(android.graphics.Shader.TileMode, android.graphics.Shader.TileMode)
     * @attr ref android.R.styleable#BitmapDrawable_tileModeX
     */
    public void setTileModeX(TileMode mode) {
        setTileModeXY(mode, mBitmapState.mTileModeY);
    }

    /**
     * Sets the repeat behavior of this drawable on the Y axis. By default, the drawable
     * does not repeat its bitmap. Using {@link android.graphics.Shader.TileMode#REPEAT} or
     * {@link android.graphics.Shader.TileMode#MIRROR} the bitmap can be repeated (or tiled)
     * if the bitmap is smaller than this drawable.
     *
     * @param mode The repeat mode for this drawable.
     *
     * @see #setTileModeX(android.graphics.Shader.TileMode)
     * @see #setTileModeXY(android.graphics.Shader.TileMode, android.graphics.Shader.TileMode)
     * @attr ref android.R.styleable#BitmapDrawable_tileModeY
     */
    public final void setTileModeY(TileMode mode) {
        setTileModeXY(mBitmapState.mTileModeX, mode);
    }

    /**
     * Sets the repeat behavior of this drawable on both axis. By default, the drawable
     * does not repeat its bitmap. Using {@link android.graphics.Shader.TileMode#REPEAT} or
     * {@link android.graphics.Shader.TileMode#MIRROR} the bitmap can be repeated (or tiled)
     * if the bitmap is smaller than this drawable.
     *
     * @param xmode The X repeat mode for this drawable.
     * @param ymode The Y repeat mode for this drawable.
     *
     * @see #setTileModeX(android.graphics.Shader.TileMode)
     * @see #setTileModeY(android.graphics.Shader.TileMode)
     */
    public void setTileModeXY(TileMode xmode, TileMode ymode) {
        final BitmapState state = mBitmapState;
        if (state.mTileModeX != xmode || state.mTileModeY != ymode) {
            state.mTileModeX = xmode;
            state.mTileModeY = ymode;
            state.mRebuildShader = true;
            mDstRectAndInsetsDirty = true;
            invalidateSelf();
        }
    }

    @Override
    public void setAutoMirrored(boolean mirrored) {
        if (mBitmapState.mAutoMirrored != mirrored) {
            mBitmapState.mAutoMirrored = mirrored;
            invalidateSelf();
        }
    }

    @Override
    public final boolean isAutoMirrored() {
        return mBitmapState.mAutoMirrored;
    }

    @Override
    public int getChangingConfigurations() {
        return super.getChangingConfigurations() | mBitmapState.mChangingConfigurations;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        mDstRectAndInsetsDirty = true;

        /*final Shader shader = mBitmapState.mPaint.getShader();
        if (shader != null) {
            if (needMirroring()) {
                updateMirrorMatrix(bounds.right - bounds.left);
                shader.setLocalMatrix(mMirrorMatrix);
                mBitmapState.mPaint.setShader(shader);
            } else {
                if (mMirrorMatrix != null) {
                    mMirrorMatrix = null;
//                    shader.setLocalMatrix(Matrix.IDENTITY_MATRIX);
                    mBitmapState.mPaint.setShader(shader);
                }
            }
        }*/
    }

    @Override
    public void draw(GLCanvas canvas) {
        final Bitmap bitmap = mBitmapState.mBitmap;
        if (bitmap == null) {
            return;
        }

        final BitmapState state = mBitmapState;
        final GLPaint paint = state.mPaint;
        if (state.mRebuildShader) {
            final TileMode tmx = state.mTileModeX;
            final TileMode tmy = state.mTileModeY;
            if (tmx == null && tmy == null) {
                paint.setShader(null);
            } else {
                paint.setShader(new BitmapShader(bitmap,
                        tmx == null ? TileMode.CLAMP : tmx,
                        tmy == null ? TileMode.CLAMP : tmy));
            }

            state.mRebuildShader = false;
        }

        final int restoreAlpha;
        if (state.mBaseAlpha != 1.0f) {
            final GLPaint p = getPaint();
            restoreAlpha = p.getAlpha();
            p.setAlpha((int) (restoreAlpha * state.mBaseAlpha + 0.5f));
        } else {
            restoreAlpha = -1;
        }

        updateDstRectAndInsetsIfDirty();
        
        canvas.drawBitmap(bitmap, (Rect) null, mDstRect, paint);

        if (restoreAlpha >= 0) {
            paint.setAlpha(restoreAlpha);
        }
    }

    private void updateDstRectAndInsetsIfDirty() {
        if (mDstRectAndInsetsDirty) {
            if (mBitmapState.mTileModeX == null && mBitmapState.mTileModeY == null) {
                final Rect bounds = getBounds();
                final int layoutDirection = getLayoutDirection();
                Gravity.apply(mBitmapState.mGravity, mBitmapWidth, mBitmapHeight,
                        bounds, mDstRect, layoutDirection);

                final int left = mDstRect.left - bounds.left;
                final int top = mDstRect.top - bounds.top;
                final int right = bounds.right - mDstRect.right;
                final int bottom = bounds.bottom - mDstRect.bottom;
                mOpticalInsets = Insets.of(left, top, right, bottom);
            } else {
                copyBounds(mDstRect);
                mOpticalInsets = Insets.NONE;
            }
        }
        mDstRectAndInsetsDirty = false;
    }
    
    /**
     * @hide
     */
    @Override
    public Insets getOpticalInsets() {
        updateDstRectAndInsetsIfDirty();
        return mOpticalInsets;
    }

    /*@Override
    public void getOutline(Outline outline) {
        updateDstRectAndInsetsIfDirty();
        outline.setRect(mDstRect);

        // Only opaque Bitmaps can report a non-0 alpha,
        // since only they are guaranteed to fill their bounds
        boolean opaqueOverShape = mBitmapState.mBitmap != null
                && !mBitmapState.mBitmap.hasAlpha();
        outline.setAlpha(opaqueOverShape ? getAlpha() / 255.0f : 0.0f);
    }*/

    @Override
    public void setAlpha(int alpha) {
        final int oldAlpha = mBitmapState.mPaint.getAlpha();
        if (alpha != oldAlpha) {
            mBitmapState.mPaint.setAlpha(alpha);
            invalidateSelf();
        }
    }

    @Override
    public int getAlpha() {
        return mBitmapState.mPaint.getAlpha();
    }

    /**
     * A mutable BitmapDrawable still shares its Bitmap with any other Drawable
     * that comes from the same resource.
     *
     * @return This drawable.
     */
    @Override
    public Drawable mutate() {
        if (!mMutated && super.mutate() == this) {
            mBitmapState = new BitmapState(mBitmapState);
            mMutated = true;
        }
        return this;
    }

    @Override
    protected boolean onStateChange(int[] stateSet) {
        final BitmapState state = mBitmapState;
        if (state.mTint != null) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isStateful() {
        final BitmapState s = mBitmapState;
        return super.isStateful() || (s.mTint != null && s.mTint.isStateful());
    }
    
    @Override
    public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs)
            throws XmlPullParserException, IOException {
        super.inflate(r, parser, attrs);

        final TypedArray a = obtainAttributes(r, attrs, com.glview.R.styleable.BitmapDrawable);
        updateStateFromTypedArray(a);
        verifyState(a);
        a.recycle();
    }

    /**
     * Ensures all required attributes are set.
     *
     * @throws XmlPullParserException if any required attributes are missing
     */
    private void verifyState(TypedArray a) throws XmlPullParserException {
        final BitmapState state = mBitmapState;
        if (state.mBitmap == null) {
            throw new XmlPullParserException(a.getPositionDescription() +
                    ": <bitmap> requires a valid src attribute");
        }
    }

    /**
     * Updates the constant state from the values in the typed array.
     */
    private void updateStateFromTypedArray(TypedArray a) throws XmlPullParserException {
        final Resources r = a.getResources();
        final BitmapState state = mBitmapState;

        // Account for any configuration changes.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        	state.mChangingConfigurations |= a.getChangingConfigurations();	
        }
        
        // Extract the theme attributes, if any.
//        state.mThemeAttrs = a.extractThemeAttrs();

        int srcResId = a.getResourceId(R.styleable.BitmapDrawable_src, 0);
        if (srcResId != 0) {
            final Bitmap bitmap = new ResourceBitmap(r, srcResId);
            state.mBitmap = bitmap;
        }

        state.mTargetDensity = r.getDisplayMetrics().densityDpi;

        boolean defMipMap = state.mBitmap != null ? state.mBitmap.hasMipMap() : false;
        setMipMap(a.getBoolean(R.styleable.BitmapDrawable_mipMap, defMipMap));

        state.mAutoMirrored = a.getBoolean(
                R.styleable.BitmapDrawable_autoMirrored, state.mAutoMirrored);
//        state.mBaseAlpha = a.getFloat(R.styleable.BitmapDrawable_alpha, state.mBaseAlpha);
//
//        final int tintMode = a.getInt(R.styleable.BitmapDrawable_tintMode, -1);
//        if (tintMode != -1) {
//            state.mTintMode = Drawable.parseTintMode(tintMode, Mode.SRC_IN);
//        }
//
//        final ColorStateList tint = a.getColorStateList(R.styleable.BitmapDrawable_tint);
//        if (tint != null) {
//            state.mTint = tint;
//        }
//
//        final Paint paint = mBitmapState.mPaint;
//        paint.setAntiAlias(a.getBoolean(
//        		R.styleable.BitmapDrawable_antialias, paint.isAntiAlias()));
//        paint.setFilterBitmap(a.getBoolean(
//        		R.styleable.BitmapDrawable_filter, paint.isFilterBitmap()));
//        paint.setDither(a.getBoolean(R.styleable.BitmapDrawable_dither, paint.isDither()));
//
        setGravity(a.getInt(R.styleable.BitmapDrawable_gravity, state.mGravity));

        final int tileMode = a.getInt(R.styleable.BitmapDrawable_tileMode, TILE_MODE_UNDEFINED);
        if (tileMode != TILE_MODE_UNDEFINED) {
            final TileMode mode = parseTileMode(tileMode);
            setTileModeXY(mode, mode);
        }

//        final int tileModeX = a.getInt(R.styleable.BitmapDrawable_tileModeX, TILE_MODE_UNDEFINED);
//        if (tileModeX != TILE_MODE_UNDEFINED) {
//            setTileModeX(parseTileMode(tileModeX));
//        }
//
//        final int tileModeY = a.getInt(R.styleable.BitmapDrawable_tileModeY, TILE_MODE_UNDEFINED);
//        if (tileModeY != TILE_MODE_UNDEFINED) {
//            setTileModeY(parseTileMode(tileModeY));
//        }

        // Update local properties.
        initializeWithState(state, r);
    }

    private static TileMode parseTileMode(int tileMode) {
        switch (tileMode) {
            case TILE_MODE_CLAMP:
                return TileMode.CLAMP;
            case TILE_MODE_REPEAT:
                return TileMode.REPEAT;
            case TILE_MODE_MIRROR:
                return TileMode.MIRROR;
            default:
                return null;
        }
    }

    @Override
    public int getIntrinsicWidth() {
        return mBitmapWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        return mBitmapHeight;
    }

    @Override
    public int getOpacity() {
        if (mBitmapState.mGravity != Gravity.FILL) {
            return PixelFormat.TRANSLUCENT;
        }

        final Bitmap bitmap = mBitmapState.mBitmap;
        return (bitmap == null || bitmap.hasAlpha() || mBitmapState.mPaint.getAlpha() < 1) ?
                PixelFormat.TRANSLUCENT : PixelFormat.OPAQUE;
    }

    @Override
    public final ConstantState getConstantState() {
    	mBitmapState.mChangingConfigurations = getChangingConfigurations();
        return mBitmapState;
    }

    final static class BitmapState extends ConstantState {
        final GLPaint mPaint;

        Bitmap mBitmap = null;
        ColorStateList mTint = null;
        int mGravity = Gravity.FILL;
        float mBaseAlpha = 1.0f;
        TileMode mTileModeX = null;
        TileMode mTileModeY = null;
        int mTargetDensity = DisplayMetrics.DENSITY_DEFAULT;
        boolean mAutoMirrored = false;

        int mChangingConfigurations;
        boolean mRebuildShader;

        BitmapState(Bitmap bitmap) {
            mBitmap = bitmap;
            mPaint = new GLPaint();
        }

        BitmapState(BitmapState bitmapState) {
            mBitmap = bitmapState.mBitmap;
            mTint = bitmapState.mTint;
            mChangingConfigurations = bitmapState.mChangingConfigurations;
            mGravity = bitmapState.mGravity;
            mTileModeX = bitmapState.mTileModeX;
            mTileModeY = bitmapState.mTileModeY;
            mTargetDensity = bitmapState.mTargetDensity;
            mBaseAlpha = bitmapState.mBaseAlpha;
            mPaint = bitmapState.mPaint;
            mRebuildShader = bitmapState.mRebuildShader;
            mAutoMirrored = bitmapState.mAutoMirrored;
        }

        @Override
        public Bitmap getBitmap() {
            return mBitmap;
        }

        @Override
        public Drawable newDrawable() {
            return new BitmapDrawable(this, null);
        }

        @Override
        public Drawable newDrawable(Resources res) {
            return new BitmapDrawable(this, res);
        }

        @Override
        public int getChangingConfigurations() {
            return mChangingConfigurations;
        }
    }

    /**
     * The one constructor to rule them all. This is called by all public
     * constructors to set the state and initialize local properties.
     */
    private BitmapDrawable(BitmapState state, Resources res) {
        mBitmapState = state;
        initializeWithState(state, res);
    }

    /**
     * Initializes local dynamic properties from state. This should be called
     * after significant state changes, e.g. from the One True Constructor and
     * after inflating or applying a theme.
     */
    private void initializeWithState(BitmapState state, Resources res) {
        if (res != null) {
            mTargetDensity = res.getDisplayMetrics().densityDpi;
        } else {
            mTargetDensity = state.mTargetDensity;
        }
        computeBitmapSize();
    }
}
