/*
 * Copyright (C) 2008 The Android Open Source Project
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
import android.view.ViewDebug;

import com.glview.R;
import com.glview.graphics.Rect;
import com.glview.hwui.GLCanvas;
import com.glview.hwui.GLPaint;

/**
 * A specialized Drawable that fills the Canvas with a specified color.
 * Note that a ColorDrawable ignores the ColorFilter.
 *
 * <p>It can be defined in an XML file with the <code>&lt;color></code> element.</p>
 *
 * @attr ref android.R.styleable#ColorDrawable_color
 */
public class ColorDrawable extends Drawable {
    private final GLPaint mPaint = new GLPaint();

    @ViewDebug.ExportedProperty(deepExport = true, prefix = "state_")
    private ColorState mColorState;

    private boolean mMutated;

    /**
     * Creates a new black ColorDrawable.
     */
    public ColorDrawable() {
        mColorState = new ColorState();
    }

    /**
     * Creates a new ColorDrawable with the specified color.
     *
     * @param color The color to draw.
     */
    public ColorDrawable(int color) {
        mColorState = new ColorState();

        setColor(color);
    }

    @Override
    public int getChangingConfigurations() {
        return super.getChangingConfigurations() | mColorState.mChangingConfigurations;
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
            mColorState = new ColorState(mColorState);
            mMutated = true;
        }
        return this;
    }

    @Override
    public void draw(GLCanvas canvas) {
    	Rect bounds = getBounds();
        if ((mColorState.mUseColor >>> 24) != 0 && !bounds.isEmpty()) {
            mPaint.setColor(mColorState.mUseColor);
            canvas.drawRect(bounds.left, bounds.top, bounds.right, bounds.bottom, mPaint);
        }
    }

    /**
     * Gets the drawable's color value.
     *
     * @return int The color to draw.
     */
    public int getColor() {
        return mColorState.mUseColor;
    }

    /**
     * Sets the drawable's color value. This action will clobber the results of
     * prior calls to {@link #setAlpha(int)} on this object, which side-affected
     * the underlying color.
     *
     * @param color The color to draw.
     */
    public void setColor(int color) {
        if (mColorState.mBaseColor != color || mColorState.mUseColor != color) {
            mColorState.mBaseColor = mColorState.mUseColor = color;
            invalidateSelf();
        }
    }

    /**
     * Returns the alpha value of this drawable's color.
     *
     * @return A value between 0 and 255.
     */
    @Override
    public int getAlpha() {
        return mColorState.mUseColor >>> 24;
    }

    /**
     * Sets the color's alpha value.
     *
     * @param alpha The alpha value to set, between 0 and 255.
     */
    @Override
    public void setAlpha(int alpha) {
        alpha += alpha >> 7;   // make it 0..256
        final int baseAlpha = mColorState.mBaseColor >>> 24;
        final int useAlpha = baseAlpha * alpha >> 8;
        final int useColor = (mColorState.mBaseColor << 8 >>> 8) | (useAlpha << 24);
        if (mColorState.mUseColor != useColor) {
            mColorState.mUseColor = useColor;
            invalidateSelf();
        }
    }

    @Override
    protected boolean onStateChange(int[] stateSet) {
        final ColorState state = mColorState;
        if (state.mTint != null) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isStateful() {
        return mColorState.mTint != null && mColorState.mTint.isStateful();
    }

    @Override
    public int getOpacity() {
        switch (mColorState.mUseColor >>> 24) {
            case 255:
                return PixelFormat.OPAQUE;
            case 0:
                return PixelFormat.TRANSPARENT;
        }
        return PixelFormat.TRANSLUCENT;
    }

    /*@Override
    public void getOutline(Outline outline) {
        outline.setRect(getBounds());
        outline.setAlpha(getAlpha() / 255.0f);
    }*/

    @Override
    public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs)
            throws XmlPullParserException, IOException {
        super.inflate(r, parser, attrs);

        final TypedArray a = obtainAttributes(r, attrs, R.styleable.ColorDrawable);
        updateStateFromTypedArray(a);
        a.recycle();
    }

    /**
     * Updates the constant state from the values in the typed array.
     */
    private void updateStateFromTypedArray(TypedArray a) {
        final ColorState state = mColorState;

        // Account for any configuration changes.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        	state.mChangingConfigurations |= a.getChangingConfigurations();	
        }

        state.mBaseColor = a.getColor(R.styleable.ColorDrawable_color, state.mBaseColor);
        state.mUseColor = state.mBaseColor;
    }

    @Override
    public ConstantState getConstantState() {
    	mColorState.mChangingConfigurations = getChangingConfigurations();
        return mColorState;
    }

    final static class ColorState extends ConstantState {
        int mBaseColor; // base color, independent of setAlpha()
        @ViewDebug.ExportedProperty
        int mUseColor;  // basecolor modulated by setAlpha()
        int mChangingConfigurations;
        ColorStateList mTint = null;

        ColorState() {
            // Empty constructor.
        }

        ColorState(ColorState state) {
            mBaseColor = state.mBaseColor;
            mUseColor = state.mUseColor;
            mChangingConfigurations = state.mChangingConfigurations;
            mTint = state.mTint;
        }

        @Override
        public Drawable newDrawable() {
            return new ColorDrawable(this, null);
        }

        @Override
        public Drawable newDrawable(Resources res) {
            return new ColorDrawable(this, res);
        }

        @Override
        public int getChangingConfigurations() {
            return mChangingConfigurations;
        }
    }

    private ColorDrawable(ColorState state, Resources res) {
        mColorState = state;
    }
}
