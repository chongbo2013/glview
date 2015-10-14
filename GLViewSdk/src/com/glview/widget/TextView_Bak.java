package com.glview.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.PorterDuff.Mode;
import android.graphics.Typeface;
import android.os.Build;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.glview.graphics.Bitmap;
import com.glview.graphics.Rect;
import com.glview.hwui.GLCanvas;
import com.glview.util.FastMath;
import com.glview.view.Gravity;
import com.glview.view.View;

public class TextView_Bak extends View {
	
	private final static String TAG = "TextView";
	
	public boolean isScaleView;
//	protected MarqueeRender mTileRender;
    private final TextPaint mTextPaint;
    private float mTextSize;
    private String mText;
    private String mOldText;
    private int mTextColor;
    private Bitmap mTextBitmap;
    private Canvas mTextCanvas;
    private final Config mConfig;
    
    private int mMaxWidth = Integer.MAX_VALUE;
    private int mMinWidth = 0;
    
    private final static int DEFAULT_TEXT_SIZE = 24;
    private final static int DEFAULT_TEXT_COLOR = 0xffffffff;
	
    TruncateAt mEllipsize = TruncateAt.END;	
	
    private int mGravity = Gravity.TOP | Gravity.START;
    
    private float mShadowRadius, mShadowDx, mShadowDy;
	
    public TextView_Bak(Context context) {
        this(context, null);
    }

    public TextView_Bak(Context context, AttributeSet attrs) {
        this(context, attrs, com.glview.R.attr.textViewStyle);
    }

    public TextView_Bak(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public TextView_Bak(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    	mConfig = Config.ARGB_4444;
    	
		isScaleView = true;
		mTextSize = DEFAULT_TEXT_SIZE;
		mTextColor = DEFAULT_TEXT_COLOR;
		
		int ellipsize = -1;
        float dx = 0, dy = 0, r = 0;
        int shadowcolor = 0;
		
        final Resources.Theme theme = context.getTheme();
        
		final TypedArray a = theme.obtainStyledAttributes(attrs, com.glview.R.styleable.TextView, defStyleAttr, defStyleRes);
        final int n = a.getIndexCount();
		for (int i = 0; i < n; i++) {
			int attr = a.getIndex(i);
			if (attr == com.glview.R.styleable.TextView_autoLink) {
				// mAutoLinkMask = a.getInt(attr, 0);
			} else if (attr == com.glview.R.styleable.TextView_linksClickable) {
				// mLinksClickable = a.getBoolean(attr, true);
			} else if (attr == com.glview.R.styleable.TextView_maxLines) {
				// setMaxLines(a.getInt(attr, -1));
			} else if (attr == com.glview.R.styleable.TextView_maxHeight) {
				// setMaxHeight(a.getDimensionPixelSize(attr, -1));
			} else if (attr == com.glview.R.styleable.TextView_lines) {
				// setLines(a.getInt(attr, -1));
			} else if (attr == com.glview.R.styleable.TextView_height) {
				// setHeight(a.getDimensionPixelSize(attr, -1));
			} else if (attr == com.glview.R.styleable.TextView_minLines) {
				// setMinLines(a.getInt(attr, -1));
			} else if (attr == com.glview.R.styleable.TextView_minHeight) {
				// setMinHeight(a.getDimensionPixelSize(attr, -1));
			} else if (attr == com.glview.R.styleable.TextView_maxWidth) {
				setMaxWidth(a.getDimensionPixelSize(attr, -1));
			} else if (attr == com.glview.R.styleable.TextView_width) {
				// setWidth(a.getDimensionPixelSize(attr, -1));
			} else if (attr == com.glview.R.styleable.TextView_minWidth) {
				setMinWidth(a.getDimensionPixelSize(attr, -1));
			} else if (attr == com.glview.R.styleable.TextView_gravity) {
				setGravity(a.getInt(attr, -1));
			} else if (attr == com.glview.R.styleable.TextView_hint) {
				// hint = a.getText(attr);
			} else if (attr == com.glview.R.styleable.TextView_text) {
				mText = (String) a.getText(attr);
			} else if (attr == com.glview.R.styleable.TextView_scrollHorizontally) {
				// if (a.getBoolean(attr, false)) {
				// setHorizontallyScrolling(true);
				// }
			} else if (attr == com.glview.R.styleable.TextView_singleLine) {
				// singleLine = a.getBoolean(attr, singleLine);
			} else if (attr == com.glview.R.styleable.TextView_ellipsize) {
				// ellipsize = a.getInt(attr, ellipsize);
				ellipsize = a.getInt(attr, ellipsize);
			} else if (attr == com.glview.R.styleable.TextView_marqueeRepeatLimit) {
				// setMarqueeRepeatLimit(a.getInt(attr, mMarqueeRepeatLimit));
			} else if (attr == com.glview.R.styleable.TextView_includeFontPadding) {
				// if (!a.getBoolean(attr, true)) {
				// setIncludeFontPadding(false);
				// }
			} else if (attr == com.glview.R.styleable.TextView_maxLength) {
				// maxlength = a.getInt(attr, -1);
			} else if (attr == com.glview.R.styleable.TextView_textScaleX) {
				// setTextScaleX(a.getFloat(attr, 1.0f));
			} else if (attr == com.glview.R.styleable.TextView_shadowColor) {
				shadowcolor = a.getInt(attr, 0);
			} else if (attr == com.glview.R.styleable.TextView_shadowDx) {
				dx = a.getFloat(attr, 0);
			} else if (attr == com.glview.R.styleable.TextView_shadowDy) {
				dy = a.getFloat(attr, 0);
			} else if (attr == com.glview.R.styleable.TextView_shadowRadius) {
				r = a.getFloat(attr, 0);
			} else if (attr == com.glview.R.styleable.TextView_enabled) {
				setEnabled(a.getBoolean(attr, isEnabled()));
			} else if (attr == com.glview.R.styleable.TextView_textColorHighlight) {
				// textColorHighlight = a.getColor(attr, textColorHighlight);
			} else if (attr == com.glview.R.styleable.TextView_textColor) {
				// textColor = a.getColorStateList(attr);
				mTextColor = a.getColor(attr, DEFAULT_TEXT_COLOR);
			} else if (attr == com.glview.R.styleable.TextView_textColorHint) {
				// textColorHint = a.getColorStateList(attr);
			} else if (attr == com.glview.R.styleable.TextView_textColorLink) {
				// textColorLink = a.getColorStateList(attr);
			} else if (attr == com.glview.R.styleable.TextView_textSize) {
				mTextSize = a.getDimensionPixelSize(attr, DEFAULT_TEXT_SIZE);
			} else if (attr == com.glview.R.styleable.TextView_typeface) {
				// typefaceIndex = a.getInt(attr, typefaceIndex);
			} else if (attr == com.glview.R.styleable.TextView_textStyle) {
				// styleIndex = a.getInt(attr, styleIndex);
			} else if (attr == com.glview.R.styleable.TextView_fontFamily) {
				// fontFamily = a.getString(attr);
			}
		}
        a.recycle();
        
		if (ellipsize < 0) {
		    ellipsize = 3; // END
		}

	    switch (ellipsize) {
	        case 1:
	            setEllipsize(TruncateAt.START);
	            break;
	        case 2:
	        	 setEllipsize(TruncateAt.MIDDLE);
	            break;
	        case 3:
	        	 setEllipsize(TruncateAt.END);
	            break;
	        case 4:
	        	 setEllipsize(TruncateAt.MARQUEE);
	            break;
	    }
        
		mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
		mTextPaint.setTextSize(mTextSize);
		mTextPaint.setColor(mTextColor);
		if(shadowcolor != 0){
			setShadowLayer(r, dx, dy, shadowcolor);
		}
    }
    
    /**
     * Makes the TextView at least this many pixels wide
     *
     * @attr ref android.R.styleable#TextView_minWidth
     */
    public void setMinWidth(int minpixels) {
        mMinWidth = minpixels;

        requestLayout();
        invalidate();
    }

    /**
     * @return the minimum width of the TextView, in pixels or -1 if the minimum width
     * was set in ems instead (using {@link #setMinEms(int)} or {@link #setEms(int)}).
     *
     * @see #setMinWidth(int)
     * @see #setWidth(int)
     *
     * @attr ref android.R.styleable#TextView_minWidth
     */
    public int getMinWidth() {
        return mMinWidth;
    }

    /**
     * Makes the TextView at most this many pixels wide
     *
     * @attr ref android.R.styleable#TextView_maxWidth
     */
    public void setMaxWidth(int maxpixels) {
        mMaxWidth = maxpixels;

        requestLayout();
        invalidate();
    }

    /**
     * @return the maximum width of the TextView, in pixels or -1 if the maximum width
     * was set in ems instead (using {@link #setMaxEms(int)} or {@link #setEms(int)}).
     *
     * @see #setMaxWidth(int)
     * @see #setWidth(int)
     *
     * @attr ref android.R.styleable#TextView_maxWidth
     */
    public int getMaxWidth() {
        return mMaxWidth;
    }
	
	//interface 
	
	@Override
	protected void onFocusChanged(boolean gainFocus, int direction,
			Rect previouslyFocusedRect) {
		super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
	}
	
    public enum TruncateAt {
        START,
        MIDDLE,
        END,
        MARQUEE,
        /**
         * @hide
         */
        END_SMALL
    }  
    
    public void setEllipsize(TruncateAt where) {
        // TruncateAt is an enum. != comparison is ok between these singleton objects.
    	mEllipsize = where;
    }
    
	public void setText(String text){
		mText = text;
		requestLayout();
	}
	
	public void setText(CharSequence cs) {
		setText(cs != null ? cs.toString() : null);
	}
	
	public void setText(int resId) {
		setText(resId > 0 ? getContext().getResources().getText(resId) : null);
	}
	
	public void setTextColor(int textColor){
		mTextPaint.setColor(textColor);
		requestLayout();
	}
	
	public void setTextSize(float textSize){
		mTextPaint.setTextSize(textSize);
		requestLayout();
	}
	
	public float getTextSize(){
		return mTextPaint.getTextSize();
	}
	
	public int getTextColor(){
		return mTextPaint.getColor();
	}
	
	public CharSequence getText(){
		return mText;
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		
		int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
		
        int width;
        int height;

        if (widthMode == MeasureSpec.EXACTLY) {
            // Parent has told us how big to be. So be it.
            width = widthSize;
        } else {
        	width = mText != null ? (int)mTextPaint.measureText(mText) : 0;
        	
        	width += getPaddingLeft() + getPaddingRight();
        	
            // Check against our minimum width
            width = Math.max(width, getSuggestedMinimumWidth());
            
            width = Math.min(width, mMaxWidth);
            width = Math.max(width, mMinWidth);
            
            if (widthMode == MeasureSpec.AT_MOST) {
                width = Math.min(widthSize, width);
            }
        }
        
        if (heightMode == MeasureSpec.EXACTLY) {
            // Parent has told us how big to be. So be it.
            height = heightSize;
        } else {
            int desired = getLineHeight();
            height = desired;
            
            height += getPaddingTop() + getPaddingBottom();
            
            if (heightMode == MeasureSpec.AT_MOST) {
                height = Math.min(height, heightSize);
            }
        }

		
		if(mText != null){
			boolean changed = false;
			if(mOldText != null){
				changed = (!mText.equals(mOldText))
						|| mTextColor != mTextPaint.getColor() 
						|| mTextSize != mTextPaint.getTextSize();
				if(changed){
				}
			} else {
				changed = true;
			}
			
			if(changed){
				renderText(width - getPaddingLeft() - getPaddingRight(), height - getPaddingTop() - getPaddingBottom());
			}
		} else {
		}
		mOldText = mText;
		mTextColor = mTextPaint.getColor();
		mTextSize = mTextPaint.getTextSize();
		setMeasuredDimension(width, height);
	}
	
	public int getLineHeight(){
		return FastMath.round(mTextPaint.getFontMetricsInt(null));
	}
	
    /**
     * Sets the typeface and style in which the text should be displayed,
     * and turns on the fake bold and italic bits in the Paint if the
     * Typeface that you provided does not have all the bits in the
     * style that you specified.
     *
     */
    public void setTypeface(Typeface tf, int style) {
        if (style > 0) {
            if (tf == null) {
                tf = Typeface.defaultFromStyle(style);
            } else {
                tf = Typeface.create(tf, style);
            }

            setTypeface(tf);
            // now compute what (if any) algorithmic styling is needed
            int typefaceStyle = tf != null ? tf.getStyle() : 0;
            int need = style & ~typefaceStyle;
            mTextPaint.setFakeBoldText((need & Typeface.BOLD) != 0);
            mTextPaint.setTextSkewX((need & Typeface.ITALIC) != 0 ? -0.25f : 0);
        } else {
            mTextPaint.setFakeBoldText(false);
            mTextPaint.setTextSkewX(0);
            setTypeface(tf);
        }
    }
    
    /**
     * Sets the typeface and style in which the text should be displayed.
     * Note that not all Typeface families actually have bold and italic
     * variants, so you may need to use
     * {@link #setTypeface(Typeface, int)} to get the appearance
     * that you actually want.
     *
     * @see #getTypeface()
     *
     */
    public void setTypeface(Typeface tf) {
        if (mTextPaint.getTypeface() != tf) {
            mTextPaint.setTypeface(tf);
        }
    }
    /**
     * @return the current typeface and style in which the text is being
     * displayed.
     *
     * @see #setTypeface(Typeface)
     *
     */
    public Typeface getTypeface(){
    	return mTextPaint.getTypeface();
    }
    
    /**
     * Gives the text a shadow of the specified radius and color, the specified
     * distance from its normal position.
     *
     * @attr ref android.R.styleable#TextView_shadowColor
     * @attr ref android.R.styleable#TextView_shadowDx
     * @attr ref android.R.styleable#TextView_shadowDy
     * @attr ref android.R.styleable#TextView_shadowRadius
     */
    public void setShadowLayer(float radius, float dx, float dy, int color) {
        mTextPaint.setShadowLayer(radius, dx, dy, color);

        mShadowRadius = radius;
        mShadowDx = dx;
        mShadowDy = dy;

        // Will change text clip region
    }
    
    int getVerticalOffset() {
        int voffset = 0;
        final int gravity = mGravity & Gravity.VERTICAL_GRAVITY_MASK;

        if (gravity != Gravity.TOP) {
            int boxht = getHeight() - getPaddingTop() - getPaddingBottom();
            int textht = getLineHeight();

            if (textht < boxht) {
                if (gravity == Gravity.BOTTOM)
                    voffset = boxht - textht;
                else // (gravity == Gravity.CENTER_VERTICAL)
                    voffset = (boxht - textht) >> 1;
            }
        }
        return voffset;
    }
    
    int getHorizontalOffset() {
        int voffset = 0;

        final int layoutDirection = getLayoutDirection();
        final int absoluteGravity = Gravity.getAbsoluteGravity(mGravity, layoutDirection);
        final int gravity = absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK;
        if (gravity != Gravity.LEFT) {
        	int boxwh = getWidth() - getPaddingLeft() - getPaddingRight();
            int textwh = getContentWidth();

            if (textwh < boxwh) {
                if (gravity == Gravity.RIGHT)
                    voffset = boxwh - textwh;
                else // (gravity == Gravity.CENTER_VERTICAL)
                    voffset = (boxwh - textwh) >> 1;
            }
        }
        return voffset;
    }
    
    int getContentWidth() {
    	if (mTextBitmap != null) {
    		return mTextBitmap.getWidth();
    	}
    	return getWidth();
    }
	
	@Override
	protected void onDraw(GLCanvas canvas) {
		if(mTextBitmap != null && !TextUtils.isEmpty(mText)) {
			final int compoundPaddingLeft = getPaddingLeft();
	        final int compoundPaddingTop = getPaddingTop();
	        final int compoundPaddingRight = getPaddingRight();
	        final int compoundPaddingBottom = getPaddingBottom();
	        
	        canvas.save();
			canvas.translate(getHorizontalOffset() + compoundPaddingLeft, getVerticalOffset() + compoundPaddingTop);
			canvas.drawBitmap(mTextBitmap, 0, 0, null);
			canvas.restore();
		}
	}
	
    /**
     * Sets the horizontal alignment of the text and the
     * vertical gravity that will be used when there is extra space
     * in the TextView beyond what is required for the text itself.
     *
     * @see android.view.Gravity
     */
    public void setGravity(int gravity) {
        if ((gravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) == 0) {
            gravity |= Gravity.START;
        }
        if ((gravity & Gravity.VERTICAL_GRAVITY_MASK) == 0) {
            gravity |= Gravity.TOP;
        }

        boolean newLayout = false;

        if ((gravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) !=
            (mGravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK)) {
            newLayout = true;
        }

        if (gravity != mGravity) {
        	mGravity = gravity;
        }
    }
	
	private void renderText(int width, int height) {
		if (TextUtils.isEmpty(mText)) return;
		FontMetricsInt metrics = mTextPaint.getFontMetricsInt();
        // The texture size needs to be at least 1x1.
        if (width <= 0) width = 1;
        if (height <= 0) height = 1;
        
        width = Math.min(width, (int) mTextPaint.measureText(mText));
        height = Math.min(height, getLineHeight());
        
		if (mTextBitmap == null) {
			mTextBitmap = Bitmap.createBitmap(width, height, mConfig);
		} /*else if ((width != mTextBitmap.getWidth() || height != mTextBitmap.getHeight()) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
			try {
				mTextBitmap.reconfigure(width, height, mConfig);
			} catch (IllegalArgumentException e) {
				mTextBitmap = Bitmap.createBitmap(width, height, mConfig);
			}
		} */else if (width > mTextBitmap.getWidth() || height > mTextBitmap.getHeight()) {
			mTextBitmap = Bitmap.createBitmap(width, height, mConfig);
		}
		mTextCanvas = new Canvas(mTextBitmap.getBitmap());
		// clear
		mTextCanvas.drawColor(Color.TRANSPARENT, Mode.SRC);
		mTextCanvas.translate(0, - metrics.ascent);
		mTextCanvas.drawText(measureText(mTextPaint, mText, width, mEllipsize), 0, 0, mTextPaint);
		invalidate();
	}
	
	private static String measureText(Paint paint, String text, int width, TruncateAt ellipsize) {
		if (width <= 0) {
			return text;
		}
		int w = width;
		if (paint.measureText(text, 0, text.length()) <= w)
			return text;

		int i;
		String str = null;
		if (ellipsize == TruncateAt.START) {
			for (i = 0; i < text.length(); i ++) {
				str = "..." + text.substring(i, text.length());
				float w1 = paint.measureText(str);
				if (w1 < w) {
					break;
				}
			}
		} else {
			for (i = text.length(); i > 0; i --) {
				str = text.substring(0, i) + "...";
				float w1 = paint.measureText(str);
				if (w1 < w) {
					break;
				}
			}
		}
		return str;
	}
}
