package com.glview.hwui;

import android.graphics.Color;

import com.glview.graphics.Typeface;
import com.glview.graphics.font.FontUtils;
import com.glview.graphics.shader.BaseShader;

public class GLPaint {
	
	float mStrokeWidth;
    public int mColor;
    int mAlpha;
    
    Style mStyle;
    
    BaseShader mShader;
    
    int mFlags = 0;
    
    int mTextSize;
    Typeface mTypeface;
    
    float mShadowRadius;
    float mShadowDx, mShadowDy;
    int mShadowColor;
    boolean mHasShadow = false;
    
    /**
     * Paint flag that enables antialiasing when drawing.
     *
     * <p>Enabling this flag will cause all draw operations that support
     * antialiasing to use it.</p>
     *
     * @see #Paint(int)
     * @see #setFlags(int)
     */
    public static final int ANTI_ALIAS_FLAG     = 0x01;
    /**
     * Paint flag that enables bilinear sampling on scaled bitmaps.
     *
     * <p>If cleared, scaled bitmaps will be drawn with nearest neighbor
     * sampling, likely resulting in artifacts. This should generally be on
     * when drawing bitmaps, unless performance-bound (rendering to software
     * canvas) or preferring pixelation artifacts to blurriness when scaling
     * significantly.</p>
     *
     * <p>If bitmaps are scaled for device density at creation time (as
     * resource bitmaps often are) the filtering will already have been
     * done.</p>
     *
     * @see #Paint(int)
     * @see #setFlags(int)
     */
    public static final int FILTER_BITMAP_FLAG  = 0x02;
    /**
     * Paint flag that enables dithering when blitting.
     *
     * <p>Enabling this flag applies a dither to any blit operation where the
     * target's colour space is more constrained than the source.
     *
     * @see #Paint(int)
     * @see #setFlags(int)
     */
    public static final int DITHER_FLAG         = 0x04;
    /**
     * Paint flag that applies an underline decoration to drawn text.
     *
     * @see #Paint(int)
     * @see #setFlags(int)
     */
    public static final int UNDERLINE_TEXT_FLAG = 0x08;
    /**
     * Paint flag that applies a strike-through decoration to drawn text.
     *
     * @see #Paint(int)
     * @see #setFlags(int)
     */
    public static final int STRIKE_THRU_TEXT_FLAG = 0x10;
    /**
     * Paint flag that applies a synthetic bolding effect to drawn text.
     *
     * <p>Enabling this flag will cause text draw operations to apply a
     * simulated bold effect when drawing a {@link Typeface} that is not
     * already bold.</p>
     *
     * @see #Paint(int)
     * @see #setFlags(int)
     */
    public static final int FAKE_BOLD_TEXT_FLAG = 0x20;
    /**
     * Paint flag that enables smooth linear scaling of text.
     *
     * <p>Enabling this flag does not actually scale text, but rather adjusts
     * text draw operations to deal gracefully with smooth adjustment of scale.
     * When this flag is enabled, font hinting is disabled to prevent shape
     * deformation between scale factors, and glyph caching is disabled due to
     * the large number of glyph images that will be generated.</p>
     *
     * <p>{@link #SUBPIXEL_TEXT_FLAG} should be used in conjunction with this
     * flag to prevent glyph positions from snapping to whole pixel values as
     * scale factor is adjusted.</p>
     *
     * @see #Paint(int)
     * @see #setFlags(int)
     */
    public static final int LINEAR_TEXT_FLAG    = 0x40;
    /**
     * Paint flag that enables subpixel positioning of text.
     *
     * <p>Enabling this flag causes glyph advances to be computed with subpixel
     * accuracy.</p>
     *
     * <p>This can be used with {@link #LINEAR_TEXT_FLAG} to prevent text from
     * jittering during smooth scale transitions.</p>
     *
     * @see #Paint(int)
     * @see #setFlags(int)
     */
    public static final int SUBPIXEL_TEXT_FLAG  = 0x80;
    /** Legacy Paint flag, no longer used. */
    public static final int DEV_KERN_TEXT_FLAG  = 0x100;
    /** @hide bit mask for the flag enabling subpixel glyph rendering for text */
    public static final int LCD_RENDER_TEXT_FLAG = 0x200;
    /**
     * Paint flag that enables the use of bitmap fonts when drawing text.
     *
     * <p>Disabling this flag will prevent text draw operations from using
     * embedded bitmap strikes in fonts, causing fonts with both scalable
     * outlines and bitmap strikes to draw only the scalable outlines, and
     * fonts with only bitmap strikes to not draw at all.</p>
     *
     * @see #Paint(int)
     * @see #setFlags(int)
     */
    public static final int EMBEDDED_BITMAP_TEXT_FLAG = 0x400;
    /** @hide bit mask for the flag forcing freetype's autohinter on for text */
    public static final int AUTO_HINTING_TEXT_FLAG = 0x800;
    /** @hide bit mask for the flag enabling vertical rendering for text */
    public static final int VERTICAL_TEXT_FLAG = 0x1000;

    // we use this when we first create a paint
    static final int DEFAULT_PAINT_FLAGS = DEV_KERN_TEXT_FLAG | EMBEDDED_BITMAP_TEXT_FLAG;
    
    public GLPaint() {
    	this(0);
	}
    
    public GLPaint(int flags) {
    	setFlags(flags | DEFAULT_PAINT_FLAGS);
    	reset();
    }
    
    public void set(GLPaint paint) {
    	if (paint != null) {
    		mStrokeWidth = paint.mStrokeWidth;
    		mColor = paint.mColor;
    		mAlpha = paint.mAlpha;
    		mStyle = paint.mStyle;
    		mTextSize = paint.mTextSize;
    		mTypeface = paint.mTypeface;
    		mShader = paint.mShader;
    		
    		setShadowLayer(paint.mShadowRadius, paint.mShadowDx, paint.mShadowDy, paint.mShadowColor);
    	} else {
    		reset();
    	}
    }
    
    public void reset() {
    	mStrokeWidth = 1f;
    	mColor = Color.WHITE;
    	mAlpha = 255;
    	mStyle = Style.FILL;
    	mTextSize = 25;
    	mTypeface = null;
    	mShader = null;
    	clearShadowLayer();
    }
    
    /**
     * Return the paint's flags. Use the Flag enum to test flag values.
     *
     * @return the paint's flags (see enums ending in _Flag for bit masks)
     */
    public int getFlags() {
    	return mFlags;
    }

    /**
     * Set the paint's flags. Use the Flag enum to specific flag values.
     *
     * @param flags The new flag bits for the paint
     */
    public void setFlags(int flags) {
    	mFlags = flags;
    }
    
    /**
     * Helper for getFlags(), returning true if ANTI_ALIAS_FLAG bit is set
     * AntiAliasing smooths out the edges of what is being drawn, but is has
     * no impact on the interior of the shape. See setDither() and
     * setFilterBitmap() to affect how colors are treated.
     *
     * @return true if the antialias bit is set in the paint's flags.
     */
    public final boolean isAntiAlias() {
        return (getFlags() & ANTI_ALIAS_FLAG) != 0;
    }

    /**
     * Helper for setFlags(), setting or clearing the ANTI_ALIAS_FLAG bit
     * AntiAliasing smooths out the edges of what is being drawn, but is has
     * no impact on the interior of the shape. See setDither() and
     * setFilterBitmap() to affect how colors are treated.
     *
     * @param aa true to set the antialias bit in the flags, false to clear it
     */
    public void setAntiAlias(boolean aa) {
    	mFlags |= ANTI_ALIAS_FLAG;
    }

    /**
     * Helper for getFlags(), returning true if DITHER_FLAG bit is set
     * Dithering affects how colors that are higher precision than the device
     * are down-sampled. No dithering is generally faster, but higher precision
     * colors are just truncated down (e.g. 8888 -> 565). Dithering tries to
     * distribute the error inherent in this process, to reduce the visual
     * artifacts.
     *
     * @return true if the dithering bit is set in the paint's flags.
     */
    public final boolean isDither() {
        return (getFlags() & DITHER_FLAG) != 0;
    }

    /**
     * Helper for setFlags(), setting or clearing the DITHER_FLAG bit
     * Dithering affects how colors that are higher precision than the device
     * are down-sampled. No dithering is generally faster, but higher precision
     * colors are just truncated down (e.g. 8888 -> 565). Dithering tries to
     * distribute the error inherent in this process, to reduce the visual
     * artifacts.
     *
     * @param dither true to set the dithering bit in flags, false to clear it
     */
    public void setDither(boolean dither) {
    	mFlags |= DITHER_FLAG;
    }
    
    public void setColor(int color) {
        mColor = color;
    }

    public int getColor() {
        return mColor;
    }

    public void setStrokeWidth(float width) {
    	mStrokeWidth = width;
    }

    public float getStrokeWidth() {
        return mStrokeWidth;
    }
    
    public int getAlpha() {
    	return mAlpha;
    }
    
    public void setAlpha(int alpha) {
    	mAlpha = alpha;
    }
    
    public Style getStyle() {
    	return mStyle;
    }
    
    public void setStyle(Style style) {
    	mStyle = style;
    }
    
    public int getTextSize() {
    	return mTextSize;
    }
    
    public void setTextSize(float textSize) {
    	if (textSize > 500 || textSize < 5) {
    		throw new IllegalArgumentException("textSize should be in range[5-500], set=" + textSize);
    	}
    	mTextSize = (int) (textSize + 0.5f);
    }
    
    public Typeface getTypeface() {
    	return mTypeface != null ? mTypeface : Typeface.DEFAULT;
    }
    
    public void setTypeface(Typeface typeface) {
    	mTypeface = typeface;
    }
    
    public void setShadowLayer(float radius, float dx, float dy, int shadowColor) {
    	if (mShadowRadius > 25) {
    		throw new IllegalArgumentException("Shadow radius must not big than 25!");
    	}
    	mShadowRadius = radius;
    	mShadowDx = dx;
    	mShadowDy = dy;
    	mShadowColor = shadowColor;
    	mHasShadow = mShadowRadius >= 1 && mShadowColor != 0;
    }
    
    public float getShadowRadius() {
    	return mShadowRadius;
    }
    
    public float getShadowDx() {
    	return mShadowDx;
    }
    
    public float getShadowDy() {
    	return mShadowDy;
    }
    
    public int getShadowColor() {
    	return mShadowColor;
    }
    
    public boolean hasShadow() {
    	return mHasShadow;
    }
    
    /**
     * Clear the shadow layer.
     */
    public void clearShadowLayer() {
        setShadowLayer(0, 0, 0, 0);
    }
    
    public void setShader(BaseShader shader) {
    	mShader = shader;
    }
    
    public BaseShader getShader() {
    	return mShader;
    }
    
    /**
     * The Style specifies if the primitive being drawn is filled, stroked, or
     * both (in the same color). The default is FILL.
     */
    public enum Style {
        /**
         * Geometry and text drawn with this style will be filled, ignoring all
         * stroke-related settings in the paint.
         */
        FILL,
        /**
         * Geometry and text drawn with this style will be stroked, respecting
         * the stroke-related fields on the paint.
         */
        STROKE,
        /**
         * Geometry and text drawn with this style will be both filled and
         * stroked at the same time, respecting the stroke-related fields on
         * the paint. This mode can give unexpected results if the geometry
         * is oriented counter-clockwise. This restriction does not apply to
         * either FILL or STROKE.
         */
        FILL_AND_STROKE;
    }
    
    public float measureText(char[] text, int index, int count) {
        return FontUtils.measureText(this, text, index, count);
    }
    
    public float measureText(CharSequence text, int start, int end) {
        return FontUtils.measureText(this, text, start, end);
    }
    public float measureText(CharSequence text) {
    	return measureText(text, 0, text.length());
    }
    
    /**
     * Return the font's interline spacing, given the Paint's settings for
     * typeface, textSize, etc. If metrics is not null, return the fontmetric
     * values in it. Note: all values have been converted to integers from
     * floats, in such a way has to make the answers useful for both spacing
     * and clipping. If you want more control over the rounding, call
     * getFontMetrics().
     *
     * @return the font's interline spacing.
     */
    public int getFontMetricsInt(FontMetricsInt fmi) {
    	return FontUtils.getFontMetricsInt(this, fmi);
    }

    public FontMetricsInt getFontMetricsInt() {
        FontMetricsInt fm = new FontMetricsInt();
        getFontMetricsInt(fm);
        return fm;
    }

    /**
     * Convenience method for callers that want to have FontMetrics values as
     * integers.
     */
    public static class FontMetricsInt {
        public int   top;
        public int   ascent;
        public int   descent;
        public int   bottom;
        public int   leading;

        @Override public String toString() {
            return "FontMetricsInt: top=" + top + " ascent=" + ascent +
                    " descent=" + descent + " bottom=" + bottom +
                    " leading=" + leading;
        }
    }
}
