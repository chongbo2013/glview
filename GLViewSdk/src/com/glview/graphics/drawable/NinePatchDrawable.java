package com.glview.graphics.drawable;

import java.io.IOException;
import java.io.InputStream;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.BitmapFactory.Options;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import com.glview.R;
import com.glview.graphics.Bitmap;
import com.glview.graphics.BitmapFactory;
import com.glview.graphics.Rect;
import com.glview.graphics.drawable.ninepatch.NinePatch;
import com.glview.hwui.GLCanvas;
import com.glview.hwui.GLPaint;

public class NinePatchDrawable extends Drawable {
    // dithering helps a lot, and is pretty cheap, so default is true
    private static final boolean DEFAULT_DITHER = false;
    private NinePatchState mNinePatchState;
    private NinePatch mNinePatch;
    private Rect mPadding;
    private Insets mOpticalInsets = Insets.NONE;
    private GLPaint mPaint = new GLPaint();
    private boolean mMutated;

    private int mTargetDensity = DisplayMetrics.DENSITY_DEFAULT;

    // These are scaled to match the target density.
    private int mBitmapWidth = -1;
    private int mBitmapHeight = -1;

    NinePatchDrawable() {
        mNinePatchState = new NinePatchState();
    }

    /**
     * Create drawable from raw nine-patch data, not dealing with density.
     * @deprecated Use {@link #NinePatchDrawable(Resources, Bitmap, byte[], Rect, String)}
     * to ensure that the drawable has correctly set its target density.
     */
    @Deprecated
    public NinePatchDrawable(Bitmap bitmap, byte[] chunk, Rect padding, String srcName) {
        this(new NinePatchState(new NinePatch(bitmap, chunk, srcName), padding), null);
    }

    /**
     * Create drawable from raw nine-patch data, setting initial target density
     * based on the display metrics of the resources.
     */
    public NinePatchDrawable(Resources res, Bitmap bitmap, byte[] chunk,
            Rect padding, String srcName) {
        this(new NinePatchState(new NinePatch(bitmap, chunk, srcName), padding), res);
        mNinePatchState.mTargetDensity = mTargetDensity;
    }

    /**
     * Create drawable from existing nine-patch, not dealing with density.
     * @deprecated Use {@link #NinePatchDrawable(Resources, NinePatch)}
     * to ensure that the drawable has correctly set its target density.
     */
    @Deprecated
    public NinePatchDrawable(NinePatch patch) {
        this(new NinePatchState(patch, new Rect()), null);
    }

    /**
     * Create drawable from existing nine-patch, setting initial target density
     * based on the display metrics of the resources.
     */
    public NinePatchDrawable(Resources res, NinePatch patch) {
        this(new NinePatchState(patch, new Rect()), res);
        mNinePatchState.mTargetDensity = mTargetDensity;
    }
    
    /**
     * The one constructor to rule them all. This is called by all public
     * constructors to set the state and initialize local properties.
     */
    private NinePatchDrawable(NinePatchState state, Resources res) {
        mNinePatchState = state;
        initializeWithState(state, res);
    }
    
    /**
     * Initializes local dynamic properties from state.
     */
    private void initializeWithState(NinePatchState state, Resources res) {
        if (res != null) {
            mTargetDensity = res.getDisplayMetrics().densityDpi;
        } else {
            mTargetDensity = state.mTargetDensity;
        }

        // Make a local copy of the padding.
        if (state.mPadding != null) {
            mPadding = new Rect(state.mPadding);
        }
        setNinePatch(state.mNinePatch);
    }

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
        if (density != mTargetDensity) {
            mTargetDensity = density == 0 ? DisplayMetrics.DENSITY_DEFAULT : density;
            if (mNinePatch != null) {
                computeBitmapSize();
            }
            invalidateSelf();
        }
    }

    private static Insets scaleFromDensity(Insets insets, int sdensity, int tdensity) {
        int left = scaleFromDensity(insets.left, sdensity, tdensity);
        int top = scaleFromDensity(insets.top, sdensity, tdensity);
        int right = scaleFromDensity(insets.right, sdensity, tdensity);
        int bottom = scaleFromDensity(insets.bottom, sdensity, tdensity);
        return Insets.of(left, top, right, bottom);
    }

    private void computeBitmapSize() {
        final int sdensity = mNinePatch.getDensity();
        final int tdensity = mTargetDensity;
        if (sdensity == tdensity) {
            mBitmapWidth = mNinePatch.getWidth();
            mBitmapHeight = mNinePatch.getHeight();
            mOpticalInsets = mNinePatchState.mOpticalInsets;
        } else {
            mBitmapWidth = scaleFromDensity(mNinePatch.getWidth(), sdensity, tdensity);
            mBitmapHeight = scaleFromDensity(mNinePatch.getHeight(), sdensity, tdensity);
            if (mNinePatchState.mPadding != null && mPadding != null) {
                Rect dest = mPadding;
                Rect src = mNinePatchState.mPadding;
                if (dest == src) {
                    mPadding = dest = new Rect(src);
                }
                dest.left = scaleFromDensity(src.left, sdensity, tdensity);
                dest.top = scaleFromDensity(src.top, sdensity, tdensity);
                dest.right = scaleFromDensity(src.right, sdensity, tdensity);
                dest.bottom = scaleFromDensity(src.bottom, sdensity, tdensity);
            }
            mOpticalInsets = scaleFromDensity(mNinePatchState.mOpticalInsets, sdensity, tdensity);
        }
    }
    
    /**
     * @hide
     */
    static public int scaleFromDensity(int size, int sdensity, int tdensity) {
        if (sdensity == Bitmap.DENSITY_NONE || tdensity == Bitmap.DENSITY_NONE || sdensity == tdensity) {
            return size;
        }
        
        // Scale by tdensity / sdensity, rounding up.
        return ((size * tdensity) + (sdensity >> 1)) / sdensity;
    }

    private void setNinePatch(NinePatch ninePatch) {
        if (mNinePatch != ninePatch) {
            mNinePatch = ninePatch;
            if (ninePatch != null) {
                computeBitmapSize();
            } else {
                mBitmapWidth = mBitmapHeight = -1;
                mOpticalInsets = Insets.NONE;
            }
            invalidateSelf();
        }
    }

    @Override
    public void draw(GLCanvas canvas) {
        final Rect bounds = getBounds();

        final int restoreAlpha;
        if (mNinePatchState.mBaseAlpha != 1.0f) {
            restoreAlpha = mPaint.getAlpha();
            mPaint.setAlpha((int) (restoreAlpha * mNinePatchState.mBaseAlpha + 0.5f));
        } else {
            restoreAlpha = -1;
        }

        mNinePatch.draw(canvas, bounds, mPaint);
        if (restoreAlpha >= 0) {
            mPaint.setAlpha(restoreAlpha);
        }
    }

    @Override
    public int getChangingConfigurations() {
        return super.getChangingConfigurations() | mNinePatchState.mChangingConfigurations;
    }

    @Override
    public boolean getPadding(Rect padding) {
        final Rect scaledPadding = mPadding;
        if (scaledPadding != null) {
            if (needsMirroring()) {
                padding.set(scaledPadding.right, scaledPadding.top,
                        scaledPadding.left, scaledPadding.bottom);
            } else {
                padding.set(scaledPadding);
            }
            return (padding.left | padding.top | padding.right | padding.bottom) != 0;
        }
        return false;
    }

    /**
     * @hide
     */
    @Override
    public Insets getOpticalInsets() {
        if (needsMirroring()) {
            return Insets.of(mOpticalInsets.right, mOpticalInsets.top,
                    mOpticalInsets.left, mOpticalInsets.bottom);
        } else {
            return mOpticalInsets;
        }
    }

    @Override
    public void setAlpha(int alpha) {
        if (mPaint == null && alpha == 0xFF) {
            // Fast common case -- leave at normal alpha.
            return;
        }
        getPaint().setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public int getAlpha() {
        if (mPaint == null) {
            // Fast common case -- normal alpha.
            return 0xFF;
        }
        return getPaint().getAlpha();
    }
	
    @Override
    public void setDither(boolean dither) {
        //noinspection PointlessBooleanExpression
        if (mPaint == null && dither == DEFAULT_DITHER) {
            // Fast common case -- leave at default dither.
            return;
        }

//        getPaint().setDither(dither);
        invalidateSelf();
    }

    @Override
    public void setAutoMirrored(boolean mirrored) {
        mNinePatchState.mAutoMirrored = mirrored;
    }

    private boolean needsMirroring() {
        return false;//isAutoMirrored() && getLayoutDirection() == LayoutDirection.RTL;
    }

    @Override
    public boolean isAutoMirrored() {
        return mNinePatchState.mAutoMirrored;
    }

    @Override
    public void setFilterBitmap(boolean filter) {
//        getPaint().setFilterBitmap(filter);
        invalidateSelf();
    }

    @Override
    public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs)
            throws XmlPullParserException, IOException {
        super.inflate(r, parser, attrs);

        final TypedArray a = obtainAttributes(r, attrs, R.styleable.NinePatchDrawable);
        updateStateFromTypedArray(a);
        a.recycle();
    }

    /**
     * Updates the constant state from the values in the typed array.
     */
    private void updateStateFromTypedArray(TypedArray a) throws XmlPullParserException {
        final Resources r = a.getResources();
        final NinePatchState state = mNinePatchState;

        final int srcResId = a.getResourceId(R.styleable.NinePatchDrawable_src, 0);
        if (srcResId != 0) {
            final Options options = new Options();

            final Rect padding = new Rect();
            final Rect opticalInsets = new Rect();
            Bitmap bitmap = null;

            try {
                final TypedValue value = new TypedValue();
                final InputStream is = r.openRawResource(srcResId, value);

                bitmap = BitmapFactory.decodeResourceStream(r, value, is, padding, options);
                is.close();
            } catch (IOException e) {
                // Ignore
            }

            if (bitmap == null) {
                throw new XmlPullParserException(a.getPositionDescription() +
                        ": <nine-patch> requires a valid src attribute");
            } else if (bitmap.getNinePatchChunk() == null) {
                throw new XmlPullParserException(a.getPositionDescription() +
                        ": <nine-patch> requires a valid 9-patch source image");
            }

            state.mNinePatch = new NinePatch(bitmap, bitmap.getNinePatchChunk());
            state.mPadding = padding;
        }

        // Update local properties.
        initializeWithState(state, r);

        // Push density applied by setNinePatchState into state.
        state.mTargetDensity = mTargetDensity;
    }

    public GLPaint getPaint() {
        if (mPaint == null) {
            mPaint = new GLPaint();
        }
        return mPaint;
    }

    /**
     * Retrieves the width of the source .png file (before resizing).
     */
    @Override
    public int getIntrinsicWidth() {
        return mBitmapWidth;
    }

    /**
     * Retrieves the height of the source .png file (before resizing).
     */
    @Override
    public int getIntrinsicHeight() {
        return mBitmapHeight;
    }

    @Override
    public int getMinimumWidth() {
        return mBitmapWidth;
    }

    @Override
    public int getMinimumHeight() {
        return mBitmapHeight;
    }

    /**
     * Returns a {@link android.graphics.PixelFormat graphics.PixelFormat}
     * value of OPAQUE or TRANSLUCENT.
     */
    @Override
    public int getOpacity() {
        return mNinePatch.hasAlpha() || (mPaint != null && mPaint.getAlpha() < 255) ?
                PixelFormat.TRANSLUCENT : PixelFormat.OPAQUE;
    }

    @Override
    public ConstantState getConstantState() {
        mNinePatchState.mChangingConfigurations = getChangingConfigurations();
        return mNinePatchState;
    }

    @Override
    public Drawable mutate() {
        if (!mMutated && super.mutate() == this) {
            mNinePatchState = new NinePatchState(mNinePatchState);
            mNinePatch = mNinePatchState.mNinePatch;
            mMutated = true;
        }
        return this;
    }

    @Override
    protected boolean onStateChange(int[] stateSet) {
        final NinePatchState state = mNinePatchState;
        if (state.mTint != null) {
            return true;
        }

        return false;
    }

    @Override
    public boolean isStateful() {
        final NinePatchState s = mNinePatchState;
        return super.isStateful() || (s.mTint != null && s.mTint.isStateful());
    }
	
	final static class NinePatchState extends ConstantState {
        // Values loaded during inflation.
        NinePatch mNinePatch = null;
        ColorStateList mTint = null;
        Rect mPadding = null;
        Insets mOpticalInsets = Insets.NONE;
        float mBaseAlpha = 1.0f;
        boolean mDither = DEFAULT_DITHER;
        int mTargetDensity = DisplayMetrics.DENSITY_DEFAULT;
        boolean mAutoMirrored = false;

        int mChangingConfigurations;

        NinePatchState() {
            // Empty constructor.
        }

        NinePatchState(NinePatch ninePatch, Rect padding) {
            this(ninePatch, padding, null, DEFAULT_DITHER, false);
        }

        NinePatchState(NinePatch ninePatch, Rect padding,
                Rect opticalInsets) {
            this(ninePatch, padding, opticalInsets, DEFAULT_DITHER, false);
        }

        NinePatchState(NinePatch ninePatch, Rect padding,
                Rect opticalInsets, boolean dither, boolean autoMirror) {
            mNinePatch = ninePatch;
            mPadding = padding;
            mOpticalInsets = Insets.of(opticalInsets);
            mDither = dither;
            mAutoMirrored = autoMirror;
        }
        // Copy constructor

        NinePatchState(NinePatchState state) {
            // We don't deep-copy any fields because they are all immutable.
            mNinePatch = state.mNinePatch;
            mTint = state.mTint;
            mPadding = state.mPadding;
            mBaseAlpha = state.mBaseAlpha;
            mDither = state.mDither;
            mChangingConfigurations = state.mChangingConfigurations;
            mTargetDensity = state.mTargetDensity;
            mAutoMirrored = state.mAutoMirrored;
        }

        @Override
        public Bitmap getBitmap() {
            return mNinePatch.getBitmap();
        }

        @Override
        public Drawable newDrawable() {
            return new NinePatchDrawable(this, null);
        }

        @Override
        public Drawable newDrawable(Resources res) {
            return new NinePatchDrawable(this, res);
        }

        @Override
        public int getChangingConfigurations() {
            return mChangingConfigurations;
        }
    }

}
