package com.glview.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.BoringLayout;
import android.text.TextPaint;
import android.util.AttributeSet;

import com.glview.graphics.Bitmap;
import com.glview.graphics.Rect;
import com.glview.graphics.Typeface;
import com.glview.graphics.drawable.Drawable;
import com.glview.hwui.GLCanvas;
import com.glview.hwui.GLPaint;
import com.glview.text.Layout;
import com.glview.text.TextUtils;
import com.glview.util.FastMath;
import com.glview.view.Gravity;
import com.glview.view.View;

public class TextView extends View {
	
	private final static String TAG = "TextView";
	
	private static final int LINES = 1;
    private static final int EMS = LINES;
    private static final int PIXELS = 2;
    
    private float mShadowRadius, mShadowDx, mShadowDy;
    private int mShadowColor;


    private boolean mPreDrawRegistered;
    private boolean mPreDrawListenerDetached;

    // A flag to prevent repeated movements from escaping the enclosing text view. The idea here is
    // that if a user is holding down a movement key to traverse text, we shouldn't also traverse
    // the view hierarchy. On the other hand, if the user is using the movement key to traverse views
    // (i.e. the first movement was to traverse out of this view, or this view was traversed into by
    // the user holding the movement key down) then we shouldn't prevent the focus from changing.
    private boolean mPreventDefaultMovement;

    private TextUtils.TruncateAt mEllipsize;
    
    static class Drawables {
        final static int DRAWABLE_NONE = -1;
        final static int DRAWABLE_RIGHT = 0;
        final static int DRAWABLE_LEFT = 1;

        final Rect mCompoundRect = new Rect();

        Drawable mDrawableTop, mDrawableBottom, mDrawableLeft, mDrawableRight,
                mDrawableStart, mDrawableEnd, mDrawableError, mDrawableTemp;

        Drawable mDrawableLeftInitial, mDrawableRightInitial;
        boolean mOverride;

        int mDrawableSizeTop, mDrawableSizeBottom, mDrawableSizeLeft, mDrawableSizeRight,
                mDrawableSizeStart, mDrawableSizeEnd, mDrawableSizeError, mDrawableSizeTemp;

        int mDrawableWidthTop, mDrawableWidthBottom, mDrawableHeightLeft, mDrawableHeightRight,
                mDrawableHeightStart, mDrawableHeightEnd, mDrawableHeightError, mDrawableHeightTemp;

        int mDrawablePadding;

        int mDrawableSaved = DRAWABLE_NONE;

        public Drawables(Context context) {
            final int targetSdkVersion = context.getApplicationInfo().targetSdkVersion;
            mOverride = false;
        }

        public void resolveWithLayoutDirection(int layoutDirection) {
            // First reset "left" and "right" drawables to their initial values
            mDrawableLeft = mDrawableLeftInitial;
            mDrawableRight = mDrawableRightInitial;

            // JB-MR1+ normal case: "start" / "end" drawables are overriding "left" / "right"
            // drawable if and only if they have been defined
            switch(layoutDirection) {
                case LAYOUT_DIRECTION_RTL:
                    if (mOverride) {
                        mDrawableRight = mDrawableStart;
                        mDrawableSizeRight = mDrawableSizeStart;
                        mDrawableHeightRight = mDrawableHeightStart;

                        mDrawableLeft = mDrawableEnd;
                        mDrawableSizeLeft = mDrawableSizeEnd;
                        mDrawableHeightLeft = mDrawableHeightEnd;
                    }
                    break;

                case LAYOUT_DIRECTION_LTR:
                default:
                    if (mOverride) {
                        mDrawableLeft = mDrawableStart;
                        mDrawableSizeLeft = mDrawableSizeStart;
                        mDrawableHeightLeft = mDrawableHeightStart;

                        mDrawableRight = mDrawableEnd;
                        mDrawableSizeRight = mDrawableSizeEnd;
                        mDrawableHeightRight = mDrawableHeightEnd;
                    }
                    break;
            }
            applyErrorDrawableIfNeeded(layoutDirection);
            updateDrawablesLayoutDirection(layoutDirection);
        }

        private void updateDrawablesLayoutDirection(int layoutDirection) {
            if (mDrawableLeft != null) {
                mDrawableLeft.setLayoutDirection(layoutDirection);
            }
            if (mDrawableRight != null) {
                mDrawableRight.setLayoutDirection(layoutDirection);
            }
            if (mDrawableTop != null) {
                mDrawableTop.setLayoutDirection(layoutDirection);
            }
            if (mDrawableBottom != null) {
                mDrawableBottom.setLayoutDirection(layoutDirection);
            }
        }

        public void setErrorDrawable(Drawable dr, TextView tv) {
            if (mDrawableError != dr && mDrawableError != null) {
                mDrawableError.setCallback(null);
            }
            mDrawableError = dr;

            final Rect compoundRect = mCompoundRect;
            int[] state = tv.getDrawableState();

            if (mDrawableError != null) {
                mDrawableError.setState(state);
                mDrawableError.copyBounds(compoundRect);
                mDrawableError.setCallback(tv);
                mDrawableSizeError = compoundRect.width();
                mDrawableHeightError = compoundRect.height();
            } else {
                mDrawableSizeError = mDrawableHeightError = 0;
            }
        }

        private void applyErrorDrawableIfNeeded(int layoutDirection) {
            // first restore the initial state if needed
            switch (mDrawableSaved) {
                case DRAWABLE_LEFT:
                    mDrawableLeft = mDrawableTemp;
                    mDrawableSizeLeft = mDrawableSizeTemp;
                    mDrawableHeightLeft = mDrawableHeightTemp;
                    break;
                case DRAWABLE_RIGHT:
                    mDrawableRight = mDrawableTemp;
                    mDrawableSizeRight = mDrawableSizeTemp;
                    mDrawableHeightRight = mDrawableHeightTemp;
                    break;
                case DRAWABLE_NONE:
                default:
            }
            // then, if needed, assign the Error drawable to the correct location
            if (mDrawableError != null) {
                switch(layoutDirection) {
                    case LAYOUT_DIRECTION_RTL:
                        mDrawableSaved = DRAWABLE_LEFT;

                        mDrawableTemp = mDrawableLeft;
                        mDrawableSizeTemp = mDrawableSizeLeft;
                        mDrawableHeightTemp = mDrawableHeightLeft;

                        mDrawableLeft = mDrawableError;
                        mDrawableSizeLeft = mDrawableSizeError;
                        mDrawableHeightLeft = mDrawableHeightError;
                        break;
                    case LAYOUT_DIRECTION_LTR:
                    default:
                        mDrawableSaved = DRAWABLE_RIGHT;

                        mDrawableTemp = mDrawableRight;
                        mDrawableSizeTemp = mDrawableSizeRight;
                        mDrawableHeightTemp = mDrawableHeightRight;

                        mDrawableRight = mDrawableError;
                        mDrawableSizeRight = mDrawableSizeError;
                        mDrawableHeightRight = mDrawableHeightError;
                        break;
                }
            }
        }
    }

    Drawables mDrawables;
    
    private CharSequence mText;
    
    private final GLPaint mTextPaint;
    private boolean mUserSetTextScaleX;
    private Layout mLayout;
    
    private int mGravity = Gravity.TOP | Gravity.START;
    
    private float mSpacingMult = 1.0f;
    private float mSpacingAdd = 0.0f;

    private int mMaximum = Integer.MAX_VALUE;
    private int mMaxMode = LINES;
    private int mMinimum = 0;
    private int mMinMode = LINES;

    private int mOldMaximum = mMaximum;
    private int mOldMaxMode = mMaxMode;

    private int mMaxWidth = Integer.MAX_VALUE;
    private int mMaxWidthMode = PIXELS;
    private int mMinWidth = 0;
    private int mMinWidthMode = PIXELS;

    private boolean mSingleLine;
    private int mDesiredHeightAtMeasure = -1;
    private boolean mIncludePad = true;
    
    private BoringLayout.Metrics mBoring, mHintBoring;
    private BoringLayout mSavedLayout, mSavedHintLayout;
    
    public TextView(Context context) {
        this(context, null);
    }

    public TextView(Context context, AttributeSet attrs) {
        this(context, attrs, com.glview.R.attr.textViewStyle);
    }

    public TextView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public TextView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        mText = "";

        final Resources res = context.getResources();
        
        mTextPaint = new GLPaint();
		
        int textColor = Color.WHITE;
        int textSize = 15;
        boolean allCaps = false;
        int shadowcolor = 0;
        float dx = 0, dy = 0, r = 0;
        boolean elegant = false;
        float letterSpacing = 0;
        
        Drawable drawableLeft = null, drawableTop = null, drawableRight = null,
                drawableBottom = null, drawableStart = null, drawableEnd = null;
        int drawablePadding = 0;
        int ellipsize = -1;
        boolean singleLine = false;
        int maxlength = -1;
        CharSequence text = "";
		
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
				 setMaxLines(a.getInt(attr, -1));
			} else if (attr == com.glview.R.styleable.TextView_maxHeight) {
				 setMaxHeight(a.getDimensionPixelSize(attr, -1));
			} else if (attr == com.glview.R.styleable.TextView_lines) {
				 setLines(a.getInt(attr, -1));
			} else if (attr == com.glview.R.styleable.TextView_height) {
				 setHeight(a.getDimensionPixelSize(attr, -1));
			} else if (attr == com.glview.R.styleable.TextView_minLines) {
				 setMinLines(a.getInt(attr, -1));
			} else if (attr == com.glview.R.styleable.TextView_minHeight) {
				 setMinHeight(a.getDimensionPixelSize(attr, -1));
			} else if (attr == com.glview.R.styleable.TextView_maxWidth) {
				setMaxWidth(a.getDimensionPixelSize(attr, -1));
			} else if (attr == com.glview.R.styleable.TextView_width) {
				 setWidth(a.getDimensionPixelSize(attr, -1));
			} else if (attr == com.glview.R.styleable.TextView_minWidth) {
				setMinWidth(a.getDimensionPixelSize(attr, -1));
			} else if (attr == com.glview.R.styleable.TextView_gravity) {
				setGravity(a.getInt(attr, -1));
			} else if (attr == com.glview.R.styleable.TextView_hint) {
				// hint = a.getText(attr);
			} else if (attr == com.glview.R.styleable.TextView_text) {
				mText = a.getText(attr);
			} else if (attr == com.glview.R.styleable.TextView_scrollHorizontally) {
				// if (a.getBoolean(attr, false)) {
				// setHorizontallyScrolling(true);
				// }
			} else if (attr == com.glview.R.styleable.TextView_singleLine) {
				singleLine = a.getBoolean(attr, singleLine);
			} else if (attr == com.glview.R.styleable.TextView_ellipsize) {
				ellipsize = a.getInt(attr, ellipsize);
			} else if (attr == com.glview.R.styleable.TextView_marqueeRepeatLimit) {
				// setMarqueeRepeatLimit(a.getInt(attr, mMarqueeRepeatLimit));
			} else if (attr == com.glview.R.styleable.TextView_includeFontPadding) {
				// if (!a.getBoolean(attr, true)) {
				// setIncludeFontPadding(false);
				// }
			} else if (attr == com.glview.R.styleable.TextView_maxLength) {
				 maxlength = a.getInt(attr, -1);
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
				textColor = a.getColor(attr, textColor);
			} else if (attr == com.glview.R.styleable.TextView_textColorHint) {
				// textColorHint = a.getColorStateList(attr);
			} else if (attr == com.glview.R.styleable.TextView_textColorLink) {
				// textColorLink = a.getColorStateList(attr);
			} else if (attr == com.glview.R.styleable.TextView_textSize) {
				textSize = a.getDimensionPixelSize(attr, textSize);
			} else if (attr == com.glview.R.styleable.TextView_typeface) {
				// typefaceIndex = a.getInt(attr, typefaceIndex);
			} else if (attr == com.glview.R.styleable.TextView_textStyle) {
				// styleIndex = a.getInt(attr, styleIndex);
			} else if (attr == com.glview.R.styleable.TextView_fontFamily) {
				// fontFamily = a.getString(attr);
			}
		}
        a.recycle();
        
		if (singleLine && ellipsize < 0) {
		    ellipsize = 3; // END
		}

	    switch (ellipsize) {
	        case 1:
	            setEllipsize(TextUtils.TruncateAt.START);
	            break;
	        case 2:
	        	 setEllipsize(TextUtils.TruncateAt.MIDDLE);
	            break;
	        case 3:
	        	 setEllipsize(TextUtils.TruncateAt.END);
	            break;
	        case 4:
	        	 setEllipsize(TextUtils.TruncateAt.MARQUEE);
	            break;
	    }
        
		mTextPaint.setTextSize(textSize);
		mTextPaint.setColor(textColor);
		if(shadowcolor != 0){
			setShadowLayer(r, dx, dy, shadowcolor);
		}
    }
    
    /**
     * Makes the TextView at least this many lines tall.
     *
     * Setting this value overrides any other (minimum) height setting. A single line TextView will
     * set this value to 1.
     *
     * @see #getMinLines()
     *
     * @attr ref android.R.styleable#TextView_minLines
     */
    public void setMinLines(int minlines) {
        mMinimum = minlines;
        mMinMode = LINES;

        requestLayout();
        invalidate();
    }

    /**
     * @return the minimum number of lines displayed in this TextView, or -1 if the minimum
     * height was set in pixels instead using {@link #setMinHeight(int) or #setHeight(int)}.
     *
     * @see #setMinLines(int)
     *
     * @attr ref android.R.styleable#TextView_minLines
     */
    public int getMinLines() {
        return mMinMode == LINES ? mMinimum : -1;
    }

    /**
     * Makes the TextView at least this many pixels tall.
     *
     * Setting this value overrides any other (minimum) number of lines setting.
     *
     * @attr ref android.R.styleable#TextView_minHeight
     */
    public void setMinHeight(int minHeight) {
        mMinimum = minHeight;
        mMinMode = PIXELS;

        requestLayout();
        invalidate();
    }

    /**
     * @return the minimum height of this TextView expressed in pixels, or -1 if the minimum
     * height was set in number of lines instead using {@link #setMinLines(int) or #setLines(int)}.
     *
     * @see #setMinHeight(int)
     *
     * @attr ref android.R.styleable#TextView_minHeight
     */
    public int getMinHeight() {
        return mMinMode == PIXELS ? mMinimum : -1;
    }

    /**
     * Makes the TextView at most this many lines tall.
     *
     * Setting this value overrides any other (maximum) height setting.
     *
     * @attr ref android.R.styleable#TextView_maxLines
     */
    public void setMaxLines(int maxlines) {
        mMaximum = maxlines;
        mMaxMode = LINES;

        requestLayout();
        invalidate();
    }

    /**
     * @return the maximum number of lines displayed in this TextView, or -1 if the maximum
     * height was set in pixels instead using {@link #setMaxHeight(int) or #setHeight(int)}.
     *
     * @see #setMaxLines(int)
     *
     * @attr ref android.R.styleable#TextView_maxLines
     */
    public int getMaxLines() {
        return mMaxMode == LINES ? mMaximum : -1;
    }

    /**
     * Makes the TextView at most this many pixels tall.  This option is mutually exclusive with the
     * {@link #setMaxLines(int)} method.
     *
     * Setting this value overrides any other (maximum) number of lines setting.
     *
     * @attr ref android.R.styleable#TextView_maxHeight
     */
    public void setMaxHeight(int maxHeight) {
        mMaximum = maxHeight;
        mMaxMode = PIXELS;

        requestLayout();
        invalidate();
    }

    /**
     * @return the maximum height of this TextView expressed in pixels, or -1 if the maximum
     * height was set in number of lines instead using {@link #setMaxLines(int) or #setLines(int)}.
     *
     * @see #setMaxHeight(int)
     *
     * @attr ref android.R.styleable#TextView_maxHeight
     */
    public int getMaxHeight() {
        return mMaxMode == PIXELS ? mMaximum : -1;
    }

    /**
     * Makes the TextView exactly this many lines tall.
     *
     * Note that setting this value overrides any other (minimum / maximum) number of lines or
     * height setting. A single line TextView will set this value to 1.
     *
     * @attr ref android.R.styleable#TextView_lines
     */
    public void setLines(int lines) {
        mMaximum = mMinimum = lines;
        mMaxMode = mMinMode = LINES;

        requestLayout();
        invalidate();
    }

    /**
     * Makes the TextView exactly this many pixels tall.
     * You could do the same thing by specifying this number in the
     * LayoutParams.
     *
     * Note that setting this value overrides any other (minimum / maximum) number of lines or
     * height setting.
     *
     * @attr ref android.R.styleable#TextView_height
     */
    public void setHeight(int pixels) {
        mMaximum = mMinimum = pixels;
        mMaxMode = mMinMode = PIXELS;

        requestLayout();
        invalidate();
    }

    /**
     * Makes the TextView at least this many ems wide
     *
     * @attr ref android.R.styleable#TextView_minEms
     */
    public void setMinEms(int minems) {
        mMinWidth = minems;
        mMinWidthMode = EMS;

        requestLayout();
        invalidate();
    }

    /**
     * @return the minimum width of the TextView, expressed in ems or -1 if the minimum width
     * was set in pixels instead (using {@link #setMinWidth(int)} or {@link #setWidth(int)}).
     *
     * @see #setMinEms(int)
     * @see #setEms(int)
     *
     * @attr ref android.R.styleable#TextView_minEms
     */
    public int getMinEms() {
        return mMinWidthMode == EMS ? mMinWidth : -1;
    }

    /**
     * Makes the TextView at least this many pixels wide
     *
     * @attr ref android.R.styleable#TextView_minWidth
     */
    public void setMinWidth(int minpixels) {
        mMinWidth = minpixels;
        mMinWidthMode = PIXELS;

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
        return mMinWidthMode == PIXELS ? mMinWidth : -1;
    }

    /**
     * Makes the TextView at most this many ems wide
     *
     * @attr ref android.R.styleable#TextView_maxEms
     */
    public void setMaxEms(int maxems) {
        mMaxWidth = maxems;
        mMaxWidthMode = EMS;

        requestLayout();
        invalidate();
    }

    /**
     * @return the maximum width of the TextView, expressed in ems or -1 if the maximum width
     * was set in pixels instead (using {@link #setMaxWidth(int)} or {@link #setWidth(int)}).
     *
     * @see #setMaxEms(int)
     * @see #setEms(int)
     *
     * @attr ref android.R.styleable#TextView_maxEms
     */
    public int getMaxEms() {
        return mMaxWidthMode == EMS ? mMaxWidth : -1;
    }

    /**
     * Makes the TextView at most this many pixels wide
     *
     * @attr ref android.R.styleable#TextView_maxWidth
     */
    public void setMaxWidth(int maxpixels) {
        mMaxWidth = maxpixels;
        mMaxWidthMode = PIXELS;

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
        return mMaxWidthMode == PIXELS ? mMaxWidth : -1;
    }

    /**
     * Makes the TextView exactly this many ems wide
     *
     * @see #setMaxEms(int)
     * @see #setMinEms(int)
     * @see #getMinEms()
     * @see #getMaxEms()
     *
     * @attr ref android.R.styleable#TextView_ems
     */
    public void setEms(int ems) {
        mMaxWidth = mMinWidth = ems;
        mMaxWidthMode = mMinWidthMode = EMS;

        requestLayout();
        invalidate();
    }

    /**
     * Makes the TextView exactly this many pixels wide.
     * You could do the same thing by specifying this number in the
     * LayoutParams.
     *
     * @see #setMaxWidth(int)
     * @see #setMinWidth(int)
     * @see #getMinWidth()
     * @see #getMaxWidth()
     *
     * @attr ref android.R.styleable#TextView_width
     */
    public void setWidth(int pixels) {
        mMaxWidth = mMinWidth = pixels;
        mMaxWidthMode = mMinWidthMode = PIXELS;

        requestLayout();
        invalidate();
    }

    /**
     * Sets line spacing for this TextView.  Each line will have its height
     * multiplied by <code>mult</code> and have <code>add</code> added to it.
     *
     * @attr ref android.R.styleable#TextView_lineSpacingExtra
     * @attr ref android.R.styleable#TextView_lineSpacingMultiplier
     */
    public void setLineSpacing(float add, float mult) {
        if (mSpacingAdd != add || mSpacingMult != mult) {
            mSpacingAdd = add;
            mSpacingMult = mult;

            if (mLayout != null) {
                nullLayouts();
                requestLayout();
                invalidate();
            }
        }
    }

    /**
     * Gets the line spacing multiplier
     *
     * @return the value by which each line's height is multiplied to get its actual height.
     *
     * @see #setLineSpacing(float, float)
     * @see #getLineSpacingExtra()
     *
     * @attr ref android.R.styleable#TextView_lineSpacingMultiplier
     */
    public float getLineSpacingMultiplier() {
        return mSpacingMult;
    }

    /**
     * Gets the line spacing extra space
     *
     * @return the extra space that is added to the height of each lines of this TextView.
     *
     * @see #setLineSpacing(float, float)
     * @see #getLineSpacingMultiplier()
     *
     * @attr ref android.R.styleable#TextView_lineSpacingExtra
     */
    public float getLineSpacingExtra() {
        return mSpacingAdd;
    }
	
	//interface 
	
	@Override
	protected void onFocusChanged(boolean gainFocus, int direction,
			Rect previouslyFocusedRect) {
		super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
	}
	
    public void setEllipsize(TextUtils.TruncateAt where) {
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
