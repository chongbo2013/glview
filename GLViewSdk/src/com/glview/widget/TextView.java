package com.glview.widget;

import java.lang.ref.WeakReference;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.text.GetChars;
import android.util.AttributeSet;
import android.util.TypedValue;

import com.glview.content.GLContext;
import com.glview.graphics.Rect;
import com.glview.graphics.Typeface;
import com.glview.graphics.drawable.Drawable;
import com.glview.hwui.GLCanvas;
import com.glview.hwui.GLPaint;
import com.glview.text.BoringLayout;
import com.glview.text.Layout;
import com.glview.text.StaticLayout;
import com.glview.text.TextDirectionHeuristic;
import com.glview.text.TextDirectionHeuristics;
import com.glview.text.TextUtils;
import com.glview.text.TextUtils.TruncateAt;
import com.glview.thread.Handler;
import com.glview.thread.Looper;
import com.glview.util.FastMath;
import com.glview.view.Gravity;
import com.glview.view.View;
import com.glview.view.ViewGroup.LayoutParams;
import com.glview.view.ViewTreeObserver;
import com.glview.view.animation.AnimationUtils;

public class TextView extends View implements ViewTreeObserver.OnPreDrawListener {
	
	private final static String TAG = "TextView";
	
	/**
     * Draw marquee text with fading edges as usual
     */
    private static final int MARQUEE_FADE_NORMAL = 0;

    /**
     * Draw marquee text as ellipsize end while inactive instead of with the fade.
     * (Useful for devices where the fade can be expensive if overdone)
     */
    private static final int MARQUEE_FADE_SWITCH_SHOW_ELLIPSIS = 1;

    /**
     * Draw marquee text with fading edges because it is currently active/animating.
     */
    private static final int MARQUEE_FADE_SWITCH_SHOW_FADE = 2;
	
	private static final int LINES = 1;
    private static final int EMS = LINES;
    private static final int PIXELS = 2;
    
    // XXX should be much larger
    private static final int VERY_WIDE = 1024*1024;
    private static final int ANIMATED_SCROLL_GAP = 250;
    
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
    
    private CharWrapper mCharWrapper;
    
    private Marquee mMarquee;
    private boolean mRestartMarquee;
    private int mMarqueeRepeatLimit = 3;

    private int mLastLayoutDirection = -1;

    /**
     * On some devices the fading edges add a performance penalty if used
     * extensively in the same layout. This mode indicates how the marquee
     * is currently being shown, if applicable. (mEllipsize will == MARQUEE)
     */
    private int mMarqueeFadeMode = MARQUEE_FADE_NORMAL;
    
    /**
     * When mMarqueeFadeMode is not MARQUEE_FADE_NORMAL, this stores
     * the layout that should be used when the mode switches.
     */
    private Layout mSavedMarqueeModeLayout;
    
    private CharSequence mText;
    private boolean mDrawDefer = true;;
    
    private final GLPaint mTextPaint;
    private boolean mUserSetTextScaleX;
    private Layout mLayout;
    
    private int mGravity = Gravity.TOP | Gravity.START;
    private boolean mHorizontallyScrolling;
    
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
    
    private BoringLayout.Metrics mBoring;
    private BoringLayout mSavedLayout;
    
    private TextDirectionHeuristic mTextDir;
    
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
				 setMarqueeRepeatLimit(a.getInt(attr, mMarqueeRepeatLimit));
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
        
     // This call will save the initial left/right drawables
        setCompoundDrawablesWithIntrinsicBounds(
            drawableLeft, drawableTop, drawableRight, drawableBottom);
        setRelativeDrawablesIfNeeded(drawableStart, drawableEnd);
        setCompoundDrawablePadding(drawablePadding);

        applySingleLine(singleLine, singleLine);
        
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
	        	mMarqueeFadeMode = MARQUEE_FADE_NORMAL;
	        	 setEllipsize(TextUtils.TruncateAt.MARQUEE);
	            break;
	    }
        
		setRawTextSize(textSize);
		setTextColor(textColor);
		if(shadowcolor != 0){
			setShadowLayer(r, dx, dy, shadowcolor);
		}
    }
    
    /**
     * Returns the top padding of the view, plus space for the top
     * Drawable if any.
     */
    public int getCompoundPaddingTop() {
        final Drawables dr = mDrawables;
        if (dr == null || dr.mDrawableTop == null) {
            return mPaddingTop;
        } else {
            return mPaddingTop + dr.mDrawablePadding + dr.mDrawableSizeTop;
        }
    }

    /**
     * Returns the bottom padding of the view, plus space for the bottom
     * Drawable if any.
     */
    public int getCompoundPaddingBottom() {
        final Drawables dr = mDrawables;
        if (dr == null || dr.mDrawableBottom == null) {
            return mPaddingBottom;
        } else {
            return mPaddingBottom + dr.mDrawablePadding + dr.mDrawableSizeBottom;
        }
    }

    /**
     * Returns the left padding of the view, plus space for the left
     * Drawable if any.
     */
    public int getCompoundPaddingLeft() {
        final Drawables dr = mDrawables;
        if (dr == null || dr.mDrawableLeft == null) {
            return mPaddingLeft;
        } else {
            return mPaddingLeft + dr.mDrawablePadding + dr.mDrawableSizeLeft;
        }
    }

    /**
     * Returns the right padding of the view, plus space for the right
     * Drawable if any.
     */
    public int getCompoundPaddingRight() {
        final Drawables dr = mDrawables;
        if (dr == null || dr.mDrawableRight == null) {
            return mPaddingRight;
        } else {
            return mPaddingRight + dr.mDrawablePadding + dr.mDrawableSizeRight;
        }
    }

    /**
     * Returns the start padding of the view, plus space for the start
     * Drawable if any.
     */
    public int getCompoundPaddingStart() {
        resolveDrawables();
        switch(getLayoutDirection()) {
            default:
            case LAYOUT_DIRECTION_LTR:
                return getCompoundPaddingLeft();
            case LAYOUT_DIRECTION_RTL:
                return getCompoundPaddingRight();
        }
    }

    /**
     * Returns the end padding of the view, plus space for the end
     * Drawable if any.
     */
    public int getCompoundPaddingEnd() {
        resolveDrawables();
        switch(getLayoutDirection()) {
            default:
            case LAYOUT_DIRECTION_LTR:
                return getCompoundPaddingRight();
            case LAYOUT_DIRECTION_RTL:
                return getCompoundPaddingLeft();
        }
    }
    
    /**
     * Returns the extended top padding of the view, including both the
     * top Drawable if any and any extra space to keep more than maxLines
     * of text from showing.  It is only valid to call this after measuring.
     */
    public int getExtendedPaddingTop() {
        if (mMaxMode != LINES) {
            return getCompoundPaddingTop();
        }

        if (mLayout == null) {
            assumeLayout();
        }

        if (mLayout.getLineCount() <= mMaximum) {
            return getCompoundPaddingTop();
        }

        int top = getCompoundPaddingTop();
        int bottom = getCompoundPaddingBottom();
        int viewht = getHeight() - top - bottom;
        int layoutht = mLayout.getLineTop(mMaximum);

        if (layoutht >= viewht) {
            return top;
        }

        final int gravity = mGravity & Gravity.VERTICAL_GRAVITY_MASK;
        if (gravity == Gravity.TOP) {
            return top;
        } else if (gravity == Gravity.BOTTOM) {
            return top + viewht - layoutht;
        } else { // (gravity == Gravity.CENTER_VERTICAL)
            return top + (viewht - layoutht) / 2;
        }
    }

    /**
     * Returns the extended bottom padding of the view, including both the
     * bottom Drawable if any and any extra space to keep more than maxLines
     * of text from showing.  It is only valid to call this after measuring.
     */
    public int getExtendedPaddingBottom() {
        if (mMaxMode != LINES) {
            return getCompoundPaddingBottom();
        }

        if (mLayout == null) {
            assumeLayout();
        }

        if (mLayout.getLineCount() <= mMaximum) {
            return getCompoundPaddingBottom();
        }

        int top = getCompoundPaddingTop();
        int bottom = getCompoundPaddingBottom();
        int viewht = getHeight() - top - bottom;
        int layoutht = mLayout.getLineTop(mMaximum);

        if (layoutht >= viewht) {
            return bottom;
        }

        final int gravity = mGravity & Gravity.VERTICAL_GRAVITY_MASK;
        if (gravity == Gravity.TOP) {
            return bottom + viewht - layoutht;
        } else if (gravity == Gravity.BOTTOM) {
            return bottom;
        } else { // (gravity == Gravity.CENTER_VERTICAL)
            return bottom + (viewht - layoutht) / 2;
        }
    }

    /**
     * Returns the total left padding of the view, including the left
     * Drawable if any.
     */
    public int getTotalPaddingLeft() {
        return getCompoundPaddingLeft();
    }

    /**
     * Returns the total right padding of the view, including the right
     * Drawable if any.
     */
    public int getTotalPaddingRight() {
        return getCompoundPaddingRight();
    }

    /**
     * Returns the total start padding of the view, including the start
     * Drawable if any.
     */
    public int getTotalPaddingStart() {
        return getCompoundPaddingStart();
    }

    /**
     * Returns the total end padding of the view, including the end
     * Drawable if any.
     */
    public int getTotalPaddingEnd() {
        return getCompoundPaddingEnd();
    }

    /**
     * Returns the total top padding of the view, including the top
     * Drawable if any, the extra space to keep more than maxLines
     * from showing, and the vertical offset for gravity, if any.
     */
    public int getTotalPaddingTop() {
        return getExtendedPaddingTop() + getVerticalOffset(true);
    }

    /**
     * Returns the total bottom padding of the view, including the bottom
     * Drawable if any, the extra space to keep more than maxLines
     * from showing, and the vertical offset for gravity, if any.
     */
    public int getTotalPaddingBottom() {
        return getExtendedPaddingBottom() + getBottomVerticalOffset(true);
    }
    
    private void setRelativeDrawablesIfNeeded(Drawable start, Drawable end) {
        boolean hasRelativeDrawables = (start != null) || (end != null);
        if (hasRelativeDrawables) {
            Drawables dr = mDrawables;
            if (dr == null) {
                mDrawables = dr = new Drawables(getContext());
            }
            mDrawables.mOverride = true;
            final Rect compoundRect = dr.mCompoundRect;
            int[] state = getDrawableState();
            if (start != null) {
                start.setBounds(0, 0, start.getIntrinsicWidth(), start.getIntrinsicHeight());
                start.setState(state);
                start.copyBounds(compoundRect);
                start.setCallback(this);

                dr.mDrawableStart = start;
                dr.mDrawableSizeStart = compoundRect.width();
                dr.mDrawableHeightStart = compoundRect.height();
            } else {
                dr.mDrawableSizeStart = dr.mDrawableHeightStart = 0;
            }
            if (end != null) {
                end.setBounds(0, 0, end.getIntrinsicWidth(), end.getIntrinsicHeight());
                end.setState(state);
                end.copyBounds(compoundRect);
                end.setCallback(this);

                dr.mDrawableEnd = end;
                dr.mDrawableSizeEnd = compoundRect.width();
                dr.mDrawableHeightEnd = compoundRect.height();
            } else {
                dr.mDrawableSizeEnd = dr.mDrawableHeightEnd = 0;
            }
            resetResolvedDrawables();
            resolveDrawables();
        }
    }
    
    /**
     * Sets the Drawables (if any) to appear to the left of, above, to the
     * right of, and below the text. Use {@code null} if you do not want a
     * Drawable there. The Drawables must already have had
     * {@link Drawable#setBounds} called.
     * <p>
     * Calling this method will overwrite any Drawables previously set using
     * {@link #setCompoundDrawablesRelative} or related methods.
     *
     * @attr ref android.R.styleable#TextView_drawableLeft
     * @attr ref android.R.styleable#TextView_drawableTop
     * @attr ref android.R.styleable#TextView_drawableRight
     * @attr ref android.R.styleable#TextView_drawableBottom
     */
    public void setCompoundDrawables( Drawable left, Drawable top,
            Drawable right, Drawable bottom) {
        Drawables dr = mDrawables;

        // We're switching to absolute, discard relative.
        if (dr != null) {
            if (dr.mDrawableStart != null) dr.mDrawableStart.setCallback(null);
            dr.mDrawableStart = null;
            if (dr.mDrawableEnd != null) dr.mDrawableEnd.setCallback(null);
            dr.mDrawableEnd = null;
            dr.mDrawableSizeStart = dr.mDrawableHeightStart = 0;
            dr.mDrawableSizeEnd = dr.mDrawableHeightEnd = 0;
        }

        final boolean drawables = left != null || top != null || right != null || bottom != null;
        if (!drawables) {
            // Clearing drawables...  can we free the data structure?
            if (dr != null) {
                if (dr.mDrawablePadding == 0) {
                    mDrawables = null;
                } else {
                    // We need to retain the last set padding, so just clear
                    // out all of the fields in the existing structure.
                    if (dr.mDrawableLeft != null) dr.mDrawableLeft.setCallback(null);
                    dr.mDrawableLeft = null;
                    if (dr.mDrawableTop != null) dr.mDrawableTop.setCallback(null);
                    dr.mDrawableTop = null;
                    if (dr.mDrawableRight != null) dr.mDrawableRight.setCallback(null);
                    dr.mDrawableRight = null;
                    if (dr.mDrawableBottom != null) dr.mDrawableBottom.setCallback(null);
                    dr.mDrawableBottom = null;
                    dr.mDrawableSizeLeft = dr.mDrawableHeightLeft = 0;
                    dr.mDrawableSizeRight = dr.mDrawableHeightRight = 0;
                    dr.mDrawableSizeTop = dr.mDrawableWidthTop = 0;
                    dr.mDrawableSizeBottom = dr.mDrawableWidthBottom = 0;
                }
            }
        } else {
            if (dr == null) {
                mDrawables = dr = new Drawables(getContext());
            }

            mDrawables.mOverride = false;

            if (dr.mDrawableLeft != left && dr.mDrawableLeft != null) {
                dr.mDrawableLeft.setCallback(null);
            }
            dr.mDrawableLeft = left;

            if (dr.mDrawableTop != top && dr.mDrawableTop != null) {
                dr.mDrawableTop.setCallback(null);
            }
            dr.mDrawableTop = top;

            if (dr.mDrawableRight != right && dr.mDrawableRight != null) {
                dr.mDrawableRight.setCallback(null);
            }
            dr.mDrawableRight = right;

            if (dr.mDrawableBottom != bottom && dr.mDrawableBottom != null) {
                dr.mDrawableBottom.setCallback(null);
            }
            dr.mDrawableBottom = bottom;

            final Rect compoundRect = dr.mCompoundRect;
            int[] state;

            state = getDrawableState();

            if (left != null) {
                left.setState(state);
                left.copyBounds(compoundRect);
                left.setCallback(this);
                dr.mDrawableSizeLeft = compoundRect.width();
                dr.mDrawableHeightLeft = compoundRect.height();
            } else {
                dr.mDrawableSizeLeft = dr.mDrawableHeightLeft = 0;
            }

            if (right != null) {
                right.setState(state);
                right.copyBounds(compoundRect);
                right.setCallback(this);
                dr.mDrawableSizeRight = compoundRect.width();
                dr.mDrawableHeightRight = compoundRect.height();
            } else {
                dr.mDrawableSizeRight = dr.mDrawableHeightRight = 0;
            }

            if (top != null) {
                top.setState(state);
                top.copyBounds(compoundRect);
                top.setCallback(this);
                dr.mDrawableSizeTop = compoundRect.height();
                dr.mDrawableWidthTop = compoundRect.width();
            } else {
                dr.mDrawableSizeTop = dr.mDrawableWidthTop = 0;
            }

            if (bottom != null) {
                bottom.setState(state);
                bottom.copyBounds(compoundRect);
                bottom.setCallback(this);
                dr.mDrawableSizeBottom = compoundRect.height();
                dr.mDrawableWidthBottom = compoundRect.width();
            } else {
                dr.mDrawableSizeBottom = dr.mDrawableWidthBottom = 0;
            }
        }

        // Save initial left/right drawables
        if (dr != null) {
            dr.mDrawableLeftInitial = left;
            dr.mDrawableRightInitial = right;
        }

        resetResolvedDrawables();
        resolveDrawables();
        invalidate();
        requestLayout();
    }

    /**
     * Sets the Drawables (if any) to appear to the left of, above, to the
     * right of, and below the text. Use 0 if you do not want a Drawable there.
     * The Drawables' bounds will be set to their intrinsic bounds.
     * <p>
     * Calling this method will overwrite any Drawables previously set using
     * {@link #setCompoundDrawablesRelative} or related methods.
     *
     * @param left Resource identifier of the left Drawable.
     * @param top Resource identifier of the top Drawable.
     * @param right Resource identifier of the right Drawable.
     * @param bottom Resource identifier of the bottom Drawable.
     *
     * @attr ref android.R.styleable#TextView_drawableLeft
     * @attr ref android.R.styleable#TextView_drawableTop
     * @attr ref android.R.styleable#TextView_drawableRight
     * @attr ref android.R.styleable#TextView_drawableBottom
     */
    public void setCompoundDrawablesWithIntrinsicBounds(int left, int top, int right, int bottom) {
        final Context context = getContext();
        setCompoundDrawablesWithIntrinsicBounds(left != 0 ? GLContext.get().getResources().getDrawable(left) : null,
                top != 0 ? GLContext.get().getResources().getDrawable(top) : null,
                right != 0 ? GLContext.get().getResources().getDrawable(right) : null,
                bottom != 0 ? GLContext.get().getResources().getDrawable(bottom) : null);
    }

    /**
     * Sets the Drawables (if any) to appear to the left of, above, to the
     * right of, and below the text. Use {@code null} if you do not want a
     * Drawable there. The Drawables' bounds will be set to their intrinsic
     * bounds.
     * <p>
     * Calling this method will overwrite any Drawables previously set using
     * {@link #setCompoundDrawablesRelative} or related methods.
     *
     * @attr ref android.R.styleable#TextView_drawableLeft
     * @attr ref android.R.styleable#TextView_drawableTop
     * @attr ref android.R.styleable#TextView_drawableRight
     * @attr ref android.R.styleable#TextView_drawableBottom
     */
    public void setCompoundDrawablesWithIntrinsicBounds(Drawable left,
            Drawable top, Drawable right, Drawable bottom) {

        if (left != null) {
            left.setBounds(0, 0, left.getIntrinsicWidth(), left.getIntrinsicHeight());
        }
        if (right != null) {
            right.setBounds(0, 0, right.getIntrinsicWidth(), right.getIntrinsicHeight());
        }
        if (top != null) {
            top.setBounds(0, 0, top.getIntrinsicWidth(), top.getIntrinsicHeight());
        }
        if (bottom != null) {
            bottom.setBounds(0, 0, bottom.getIntrinsicWidth(), bottom.getIntrinsicHeight());
        }
        setCompoundDrawables(left, top, right, bottom);
    }

    /**
     * Sets the Drawables (if any) to appear to the start of, above, to the end
     * of, and below the text. Use {@code null} if you do not want a Drawable
     * there. The Drawables must already have had {@link Drawable#setBounds}
     * called.
     * <p>
     * Calling this method will overwrite any Drawables previously set using
     * {@link #setCompoundDrawables} or related methods.
     *
     * @attr ref android.R.styleable#TextView_drawableStart
     * @attr ref android.R.styleable#TextView_drawableTop
     * @attr ref android.R.styleable#TextView_drawableEnd
     * @attr ref android.R.styleable#TextView_drawableBottom
     */
    public void setCompoundDrawablesRelative(Drawable start, Drawable top,
            Drawable end, Drawable bottom) {
        Drawables dr = mDrawables;

        // We're switching to relative, discard absolute.
        if (dr != null) {
            if (dr.mDrawableLeft != null) dr.mDrawableLeft.setCallback(null);
            dr.mDrawableLeft = dr.mDrawableLeftInitial = null;
            if (dr.mDrawableRight != null) dr.mDrawableRight.setCallback(null);
            dr.mDrawableRight = dr.mDrawableRightInitial = null;
            dr.mDrawableSizeLeft = dr.mDrawableHeightLeft = 0;
            dr.mDrawableSizeRight = dr.mDrawableHeightRight = 0;
        }

        final boolean drawables = start != null || top != null
                || end != null || bottom != null;

        if (!drawables) {
            // Clearing drawables...  can we free the data structure?
            if (dr != null) {
                if (dr.mDrawablePadding == 0) {
                    mDrawables = null;
                } else {
                    // We need to retain the last set padding, so just clear
                    // out all of the fields in the existing structure.
                    if (dr.mDrawableStart != null) dr.mDrawableStart.setCallback(null);
                    dr.mDrawableStart = null;
                    if (dr.mDrawableTop != null) dr.mDrawableTop.setCallback(null);
                    dr.mDrawableTop = null;
                    if (dr.mDrawableEnd != null) dr.mDrawableEnd.setCallback(null);
                    dr.mDrawableEnd = null;
                    if (dr.mDrawableBottom != null) dr.mDrawableBottom.setCallback(null);
                    dr.mDrawableBottom = null;
                    dr.mDrawableSizeStart = dr.mDrawableHeightStart = 0;
                    dr.mDrawableSizeEnd = dr.mDrawableHeightEnd = 0;
                    dr.mDrawableSizeTop = dr.mDrawableWidthTop = 0;
                    dr.mDrawableSizeBottom = dr.mDrawableWidthBottom = 0;
                }
            }
        } else {
            if (dr == null) {
                mDrawables = dr = new Drawables(getContext());
            }

            mDrawables.mOverride = true;

            if (dr.mDrawableStart != start && dr.mDrawableStart != null) {
                dr.mDrawableStart.setCallback(null);
            }
            dr.mDrawableStart = start;

            if (dr.mDrawableTop != top && dr.mDrawableTop != null) {
                dr.mDrawableTop.setCallback(null);
            }
            dr.mDrawableTop = top;

            if (dr.mDrawableEnd != end && dr.mDrawableEnd != null) {
                dr.mDrawableEnd.setCallback(null);
            }
            dr.mDrawableEnd = end;

            if (dr.mDrawableBottom != bottom && dr.mDrawableBottom != null) {
                dr.mDrawableBottom.setCallback(null);
            }
            dr.mDrawableBottom = bottom;

            final Rect compoundRect = dr.mCompoundRect;
            int[] state;

            state = getDrawableState();

            if (start != null) {
                start.setState(state);
                start.copyBounds(compoundRect);
                start.setCallback(this);
                dr.mDrawableSizeStart = compoundRect.width();
                dr.mDrawableHeightStart = compoundRect.height();
            } else {
                dr.mDrawableSizeStart = dr.mDrawableHeightStart = 0;
            }

            if (end != null) {
                end.setState(state);
                end.copyBounds(compoundRect);
                end.setCallback(this);
                dr.mDrawableSizeEnd = compoundRect.width();
                dr.mDrawableHeightEnd = compoundRect.height();
            } else {
                dr.mDrawableSizeEnd = dr.mDrawableHeightEnd = 0;
            }

            if (top != null) {
                top.setState(state);
                top.copyBounds(compoundRect);
                top.setCallback(this);
                dr.mDrawableSizeTop = compoundRect.height();
                dr.mDrawableWidthTop = compoundRect.width();
            } else {
                dr.mDrawableSizeTop = dr.mDrawableWidthTop = 0;
            }

            if (bottom != null) {
                bottom.setState(state);
                bottom.copyBounds(compoundRect);
                bottom.setCallback(this);
                dr.mDrawableSizeBottom = compoundRect.height();
                dr.mDrawableWidthBottom = compoundRect.width();
            } else {
                dr.mDrawableSizeBottom = dr.mDrawableWidthBottom = 0;
            }
        }

        resetResolvedDrawables();
        resolveDrawables();
        invalidate();
        requestLayout();
    }

    /**
     * Sets the Drawables (if any) to appear to the start of, above, to the end
     * of, and below the text. Use 0 if you do not want a Drawable there. The
     * Drawables' bounds will be set to their intrinsic bounds.
     * <p>
     * Calling this method will overwrite any Drawables previously set using
     * {@link #setCompoundDrawables} or related methods.
     *
     * @param start Resource identifier of the start Drawable.
     * @param top Resource identifier of the top Drawable.
     * @param end Resource identifier of the end Drawable.
     * @param bottom Resource identifier of the bottom Drawable.
     *
     * @attr ref android.R.styleable#TextView_drawableStart
     * @attr ref android.R.styleable#TextView_drawableTop
     * @attr ref android.R.styleable#TextView_drawableEnd
     * @attr ref android.R.styleable#TextView_drawableBottom
     */
    public void setCompoundDrawablesRelativeWithIntrinsicBounds(int start, int top, int end,
            int bottom) {
        final Context context = getContext();
        setCompoundDrawablesRelativeWithIntrinsicBounds(
                start != 0 ? GLContext.get().getResources().getDrawable(start) : null,
                top != 0 ? GLContext.get().getResources().getDrawable(top) : null,
                end != 0 ? GLContext.get().getResources().getDrawable(end) : null,
                bottom != 0 ? GLContext.get().getResources().getDrawable(bottom) : null);
    }

    /**
     * Sets the Drawables (if any) to appear to the start of, above, to the end
     * of, and below the text. Use {@code null} if you do not want a Drawable
     * there. The Drawables' bounds will be set to their intrinsic bounds.
     * <p>
     * Calling this method will overwrite any Drawables previously set using
     * {@link #setCompoundDrawables} or related methods.
     *
     * @attr ref android.R.styleable#TextView_drawableStart
     * @attr ref android.R.styleable#TextView_drawableTop
     * @attr ref android.R.styleable#TextView_drawableEnd
     * @attr ref android.R.styleable#TextView_drawableBottom
     */
    public void setCompoundDrawablesRelativeWithIntrinsicBounds(Drawable start,
            Drawable top, Drawable end, Drawable bottom) {

        if (start != null) {
            start.setBounds(0, 0, start.getIntrinsicWidth(), start.getIntrinsicHeight());
        }
        if (end != null) {
            end.setBounds(0, 0, end.getIntrinsicWidth(), end.getIntrinsicHeight());
        }
        if (top != null) {
            top.setBounds(0, 0, top.getIntrinsicWidth(), top.getIntrinsicHeight());
        }
        if (bottom != null) {
            bottom.setBounds(0, 0, bottom.getIntrinsicWidth(), bottom.getIntrinsicHeight());
        }
        setCompoundDrawablesRelative(start, top, end, bottom);
    }

    /**
     * Returns drawables for the left, top, right, and bottom borders.
     *
     * @attr ref android.R.styleable#TextView_drawableLeft
     * @attr ref android.R.styleable#TextView_drawableTop
     * @attr ref android.R.styleable#TextView_drawableRight
     * @attr ref android.R.styleable#TextView_drawableBottom
     */
    public Drawable[] getCompoundDrawables() {
        final Drawables dr = mDrawables;
        if (dr != null) {
            return new Drawable[] {
                dr.mDrawableLeft, dr.mDrawableTop, dr.mDrawableRight, dr.mDrawableBottom
            };
        } else {
            return new Drawable[] { null, null, null, null };
        }
    }

    /**
     * Returns drawables for the start, top, end, and bottom borders.
     *
     * @attr ref android.R.styleable#TextView_drawableStart
     * @attr ref android.R.styleable#TextView_drawableTop
     * @attr ref android.R.styleable#TextView_drawableEnd
     * @attr ref android.R.styleable#TextView_drawableBottom
     */
    public Drawable[] getCompoundDrawablesRelative() {
        final Drawables dr = mDrawables;
        if (dr != null) {
            return new Drawable[] {
                dr.mDrawableStart, dr.mDrawableTop, dr.mDrawableEnd, dr.mDrawableBottom
            };
        } else {
            return new Drawable[] { null, null, null, null };
        }
    }

    /**
     * Sets the size of the padding between the compound drawables and
     * the text.
     *
     * @attr ref android.R.styleable#TextView_drawablePadding
     */
    public void setCompoundDrawablePadding(int pad) {
        Drawables dr = mDrawables;
        if (pad == 0) {
            if (dr != null) {
                dr.mDrawablePadding = pad;
            }
        } else {
            if (dr == null) {
                mDrawables = dr = new Drawables(getContext());
            }
            dr.mDrawablePadding = pad;
        }

        invalidate();
        requestLayout();
    }

    /**
     * Returns the padding between the compound drawables and the text.
     *
     * @attr ref android.R.styleable#TextView_drawablePadding
     */
    public int getCompoundDrawablePadding() {
        final Drawables dr = mDrawables;
        return dr != null ? dr.mDrawablePadding : 0;
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        if (left != mPaddingLeft ||
            right != mPaddingRight ||
            top != mPaddingTop ||
            bottom != mPaddingBottom) {
            nullLayouts();
        }

        // the super call will requestLayout()
        super.setPadding(left, top, right, bottom);
        invalidate();
    }

    @Override
    public void setPaddingRelative(int start, int top, int end, int bottom) {
        if (start != getPaddingStart() ||
            end != getPaddingEnd() ||
            top != mPaddingTop ||
            bottom != mPaddingBottom) {
            nullLayouts();
        }

        // the super call will requestLayout()
        super.setPaddingRelative(start, top, end, bottom);
        invalidate();
    }
    
    /**
     * Sets whether the text should be allowed to be wider than the
     * View is.  If false, it will be wrapped to the width of the View.
     *
     * @attr ref android.R.styleable#TextView_scrollHorizontally
     */
    public void setHorizontallyScrolling(boolean whether) {
        if (mHorizontallyScrolling != whether) {
            mHorizontallyScrolling = whether;

            if (mLayout != null) {
                nullLayouts();
                requestLayout();
                invalidate();
            }
        }
    }

    /**
     * Returns whether the text is allowed to be wider than the View is.
     * If false, the text will be wrapped to the width of the View.
     *
     * @attr ref android.R.styleable#TextView_scrollHorizontally
     * @hide
     */
    public boolean getHorizontallyScrolling() {
        return mHorizontallyScrolling;
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
    
    private void nullLayouts() {
        if (mLayout instanceof BoringLayout && mSavedLayout == null) {
            mSavedLayout = (BoringLayout) mLayout;
        }

        mSavedMarqueeModeLayout = mLayout = null;

        mBoring = null;

    }

    /**
     * Make a new Layout based on the already-measured size of the view,
     * on the assumption that it was measured correctly at some point.
     */
    private void assumeLayout() {
        int width = mRight - mLeft - getCompoundPaddingLeft() - getCompoundPaddingRight();

        if (width < 1) {
            width = 0;
        }

        int physicalWidth = width;

        if (mHorizontallyScrolling) {
            width = VERY_WIDE;
        }

        makeNewLayout(width, UNKNOWN_BORING,
                      physicalWidth, false);
    }
    
    private int getBoxHeight(Layout l) {
        int padding = getExtendedPaddingTop() + getExtendedPaddingBottom();
        return getMeasuredHeight() - padding;
    }

    int getVerticalOffset(boolean forceNormal) {
        int voffset = 0;
        final int gravity = mGravity & Gravity.VERTICAL_GRAVITY_MASK;

        Layout l = mLayout;

        if (gravity != Gravity.TOP) {
            int boxht = getBoxHeight(l);
            int textht = l.getHeight();

            if (textht < boxht) {
                if (gravity == Gravity.BOTTOM)
                    voffset = boxht - textht;
                else // (gravity == Gravity.CENTER_VERTICAL)
                    voffset = (boxht - textht) >> 1;
            }
        }
        return voffset;
    }

    private int getBottomVerticalOffset(boolean forceNormal) {
        int voffset = 0;
        final int gravity = mGravity & Gravity.VERTICAL_GRAVITY_MASK;

        Layout l = mLayout;

        if (gravity != Gravity.BOTTOM) {
            int boxht = getBoxHeight(l);
            int textht = l.getHeight();

            if (textht < boxht) {
                if (gravity == Gravity.TOP)
                    voffset = boxht - textht;
                else // (gravity == Gravity.CENTER_VERTICAL)
                    voffset = (boxht - textht) >> 1;
            }
        }
        return voffset;
    }
    
    private void registerForPreDraw() {
        if (!mPreDrawRegistered) {
            getViewTreeObserver().addOnPreDrawListener(this);
            mPreDrawRegistered = true;
        }
    }

    private void unregisterForPreDraw() {
        getViewTreeObserver().removeOnPreDrawListener(this);
        mPreDrawRegistered = false;
        mPreDrawListenerDetached = false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean onPreDraw() {
        if (mLayout == null) {
            assumeLayout();
        }

        bringTextIntoView();

        unregisterForPreDraw();

        return true;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (mPreDrawListenerDetached) {
            getViewTreeObserver().addOnPreDrawListener(this);
            mPreDrawListenerDetached = false;
        }
    }

    /** @hide */
    @Override
    protected void onDetachedFromWindowInternal() {
        if (mPreDrawRegistered) {
            getViewTreeObserver().removeOnPreDrawListener(this);
            mPreDrawListenerDetached = true;
        }

        resetResolvedDrawables();

        super.onDetachedFromWindowInternal();
    }
    
    @Override
    protected boolean isPaddingOffsetRequired() {
        return mShadowRadius != 0 || mDrawables != null;
    }

    @Override
    protected int getLeftPaddingOffset() {
        return getCompoundPaddingLeft() - mPaddingLeft +
                (int) Math.min(0, mShadowDx - mShadowRadius);
    }

    @Override
    protected int getTopPaddingOffset() {
        return (int) Math.min(0, mShadowDy - mShadowRadius);
    }

    @Override
    protected int getBottomPaddingOffset() {
        return (int) Math.max(0, mShadowDy + mShadowRadius);
    }

    @Override
    protected int getRightPaddingOffset() {
        return -(getCompoundPaddingRight() - mPaddingRight) +
                (int) Math.max(0, mShadowDx + mShadowRadius);
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        final boolean verified = super.verifyDrawable(who);
        if (!verified && mDrawables != null) {
            return who == mDrawables.mDrawableLeft || who == mDrawables.mDrawableTop ||
                    who == mDrawables.mDrawableRight || who == mDrawables.mDrawableBottom ||
                    who == mDrawables.mDrawableStart || who == mDrawables.mDrawableEnd;
        }
        return verified;
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        if (mDrawables != null) {
            if (mDrawables.mDrawableLeft != null) {
                mDrawables.mDrawableLeft.jumpToCurrentState();
            }
            if (mDrawables.mDrawableTop != null) {
                mDrawables.mDrawableTop.jumpToCurrentState();
            }
            if (mDrawables.mDrawableRight != null) {
                mDrawables.mDrawableRight.jumpToCurrentState();
            }
            if (mDrawables.mDrawableBottom != null) {
                mDrawables.mDrawableBottom.jumpToCurrentState();
            }
            if (mDrawables.mDrawableStart != null) {
                mDrawables.mDrawableStart.jumpToCurrentState();
            }
            if (mDrawables.mDrawableEnd != null) {
                mDrawables.mDrawableEnd.jumpToCurrentState();
            }
        }
    }

    @Override
    public void invalidateDrawable(Drawable drawable) {
        boolean handled = false;

        if (verifyDrawable(drawable)) {
            final Rect dirty = drawable.getBounds();
            int scrollX = mScrollX;
            int scrollY = mScrollY;

            // IMPORTANT: The coordinates below are based on the coordinates computed
            // for each compound drawable in onDraw(). Make sure to update each section
            // accordingly.
            final TextView.Drawables drawables = mDrawables;
            if (drawables != null) {
                if (drawable == drawables.mDrawableLeft) {
                    final int compoundPaddingTop = getCompoundPaddingTop();
                    final int compoundPaddingBottom = getCompoundPaddingBottom();
                    final int vspace = mBottom - mTop - compoundPaddingBottom - compoundPaddingTop;

                    scrollX += mPaddingLeft;
                    scrollY += compoundPaddingTop + (vspace - drawables.mDrawableHeightLeft) / 2;
                    handled = true;
                } else if (drawable == drawables.mDrawableRight) {
                    final int compoundPaddingTop = getCompoundPaddingTop();
                    final int compoundPaddingBottom = getCompoundPaddingBottom();
                    final int vspace = mBottom - mTop - compoundPaddingBottom - compoundPaddingTop;

                    scrollX += (mRight - mLeft - mPaddingRight - drawables.mDrawableSizeRight);
                    scrollY += compoundPaddingTop + (vspace - drawables.mDrawableHeightRight) / 2;
                    handled = true;
                } else if (drawable == drawables.mDrawableTop) {
                    final int compoundPaddingLeft = getCompoundPaddingLeft();
                    final int compoundPaddingRight = getCompoundPaddingRight();
                    final int hspace = mRight - mLeft - compoundPaddingRight - compoundPaddingLeft;

                    scrollX += compoundPaddingLeft + (hspace - drawables.mDrawableWidthTop) / 2;
                    scrollY += mPaddingTop;
                    handled = true;
                } else if (drawable == drawables.mDrawableBottom) {
                    final int compoundPaddingLeft = getCompoundPaddingLeft();
                    final int compoundPaddingRight = getCompoundPaddingRight();
                    final int hspace = mRight - mLeft - compoundPaddingRight - compoundPaddingLeft;

                    scrollX += compoundPaddingLeft + (hspace - drawables.mDrawableWidthBottom) / 2;
                    scrollY += (mBottom - mTop - mPaddingBottom - drawables.mDrawableSizeBottom);
                    handled = true;
                }
            }

            if (handled) {
                invalidate(dirty.left + scrollX, dirty.top + scrollY,
                        dirty.right + scrollX, dirty.bottom + scrollY);
            }
        }

        if (!handled) {
            super.invalidateDrawable(drawable);
        }
    }
    
    /**
     * Returns true if anything changed.
     */
    private boolean bringTextIntoView() {
        Layout layout = mLayout;
        int line = 0;
        if ((mGravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.BOTTOM) {
            line = layout.getLineCount() - 1;
        }

        Layout.Alignment a = layout.getParagraphAlignment(line);
        int dir = layout.getParagraphDirection(line);
        int hspace = mRight - mLeft - getCompoundPaddingLeft() - getCompoundPaddingRight();
        int vspace = mBottom - mTop - getExtendedPaddingTop() - getExtendedPaddingBottom();
        int ht = layout.getHeight();

        int scrollx, scrolly;

        // Convert to left, center, or right alignment.
        if (a == Layout.Alignment.ALIGN_NORMAL) {
            a = dir == Layout.DIR_LEFT_TO_RIGHT ? Layout.Alignment.ALIGN_LEFT :
                Layout.Alignment.ALIGN_RIGHT;
        } else if (a == Layout.Alignment.ALIGN_OPPOSITE){
            a = dir == Layout.DIR_LEFT_TO_RIGHT ? Layout.Alignment.ALIGN_RIGHT :
                Layout.Alignment.ALIGN_LEFT;
        }

        if (a == Layout.Alignment.ALIGN_CENTER) {
            /*
             * Keep centered if possible, or, if it is too wide to fit,
             * keep leading edge in view.
             */

            int left = (int) Math.floor(layout.getLineLeft(line));
            int right = (int) Math.ceil(layout.getLineRight(line));

            if (right - left < hspace) {
                scrollx = (right + left) / 2 - hspace / 2;
            } else {
                if (dir < 0) {
                    scrollx = right - hspace;
                } else {
                    scrollx = left;
                }
            }
        } else if (a == Layout.Alignment.ALIGN_RIGHT) {
            int right = (int) Math.ceil(layout.getLineRight(line));
            scrollx = right - hspace;
        } else { // a == Layout.Alignment.ALIGN_LEFT (will also be the default)
            scrollx = (int) Math.floor(layout.getLineLeft(line));
        }

        if (ht < vspace) {
            scrolly = 0;
        } else {
            if ((mGravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.BOTTOM) {
                scrolly = ht - vspace;
            } else {
                scrolly = 0;
            }
        }

        if (scrollx != mScrollX || scrolly != mScrollY) {
            scrollTo(scrollx, scrolly);
            return true;
        } else {
            return false;
        }
    }
	
	//interface 
	
	@Override
	protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
		startStopMarquee(focused);
		super.onFocusChanged(focused, direction, previouslyFocusedRect);
	}
	
    public void setEllipsize(TextUtils.TruncateAt where) {
    	// TruncateAt is an enum. != comparison is ok between these singleton objects.
        if (mEllipsize != where) {
            mEllipsize = where;

            if (mLayout != null) {
                nullLayouts();
                requestLayout();
                invalidate();
            }
        }
    }
    
    /**
     * Sets how many times to repeat the marquee animation. Only applied if the
     * TextView has marquee enabled. Set to -1 to repeat indefinitely.
     *
     * @see #getMarqueeRepeatLimit()
     *
     * @attr ref android.R.styleable#TextView_marqueeRepeatLimit
     */
    public void setMarqueeRepeatLimit(int marqueeLimit) {
        mMarqueeRepeatLimit = marqueeLimit;
    }

    /**
     * Gets the number of times the marquee animation is repeated. Only meaningful if the
     * TextView has marquee enabled.
     *
     * @return the number of times the marquee animation is repeated. -1 if the animation
     * repeats indefinitely
     *
     * @see #setMarqueeRepeatLimit(int)
     *
     * @attr ref android.R.styleable#TextView_marqueeRepeatLimit
     */
    public int getMarqueeRepeatLimit() {
        return mMarqueeRepeatLimit;
    }
    
    /**
     * Sets the string value of the TextView. TextView <em>does not</em> accept
     * HTML-like formatting, which you can do with text strings in XML resource files.
     * To style your strings, attach android.text.style.* objects to a
     * {@link android.text.SpannableString SpannableString}, or see the
     * <a href="{@docRoot}guide/topics/resources/available-resources.html#stringresources">
     * Available Resource Types</a> documentation for an example of setting
     * formatted text in the XML resource file.
     *
     * @attr ref android.R.styleable#TextView_text
     */
    public final void setText(CharSequence text) {
        setText(text, 0);
        
        if (mCharWrapper != null) {
            mCharWrapper.mChars = null;
        }
    }

    private void setText(CharSequence text, int oldlen) {
        if (text == null) {
            text = "";
        }

//        if (!mUserSetTextScaleX) mTextPaint.setTextScaleX(1.0f);

        if (mText != null) {
            oldlen = mText.length();
        }

        mText = text;

        final int textLength = text.length();

        if (mLayout != null) {
            checkForRelayout();
        }

        onTextChanged(text, 0, oldlen, textLength);
    }

    /**
     * Sets the TextView to display the specified slice of the specified
     * char array.  You must promise that you will not change the contents
     * of the array except for right before another call to setText(),
     * since the TextView has no way to know that the text
     * has changed and that it needs to invalidate and re-layout.
     */
    public final void setText(char[] text, int start, int len) {
        int oldlen = 0;

        if (start < 0 || len < 0 || start + len > text.length) {
            throw new IndexOutOfBoundsException(start + ", " + len);
        }

        /*
         * We must do the before-notification here ourselves because if
         * the old text is a CharWrapper we destroy it before calling
         * into the normal path.
         */
        if (mText != null) {
            oldlen = mText.length();
        } else {
        }

        if (mCharWrapper == null) {
            mCharWrapper = new CharWrapper(text, start, len);
        } else {
            mCharWrapper.set(text, start, len);
        }

        setText(mCharWrapper, oldlen);
    }

    public final void setText(int resid) {
        setText(getContext().getResources().getText(resid));
    }
	
	@Override
    protected boolean setFrame(int l, int t, int r, int b) {
        boolean result = super.setFrame(l, t, r, b);

        restartMarqueeIfNeeded();

        return result;
    }

    private void restartMarqueeIfNeeded() {
        if (mRestartMarquee && mEllipsize == TextUtils.TruncateAt.MARQUEE) {
            mRestartMarquee = false;
            startMarquee();
        }
    }
	
	public void setTextColor(int textColor){
		mTextPaint.setColor(textColor);
		requestLayout();
	}
	
	/**
     * Set the default text size to the given value, interpreted as "scaled
     * pixel" units.  This size is adjusted based on the current density and
     * user font size preference.
     *
     * @param size The scaled pixel size.
     *
     * @attr ref android.R.styleable#TextView_textSize
     */
    public void setTextSize(float size) {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
    }

    /**
     * Set the default text size to a given unit and value.  See {@link
     * TypedValue} for the possible dimension units.
     *
     * @param unit The desired dimension unit.
     * @param size The desired size in the given units.
     *
     * @attr ref android.R.styleable#TextView_textSize
     */
    public void setTextSize(int unit, float size) {
        Context c = getContext();
        Resources r;

        if (c == null)
            r = Resources.getSystem();
        else
            r = c.getResources();

        setRawTextSize(TypedValue.applyDimension(
                unit, size, r.getDisplayMetrics()));
    }

    private void setRawTextSize(float size) {
        if (size != mTextPaint.getTextSize()) {
            mTextPaint.setTextSize(size);

            if (mLayout != null) {
                nullLayouts();
                requestLayout();
                invalidate();
            }
        }
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
	
	private Layout.Alignment getLayoutAlignment() {
        Layout.Alignment alignment;
        switch (mGravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) {
            case Gravity.START:
                alignment = Layout.Alignment.ALIGN_NORMAL;
                break;
            case Gravity.END:
                alignment = Layout.Alignment.ALIGN_OPPOSITE;
                break;
            case Gravity.LEFT:
                alignment = Layout.Alignment.ALIGN_LEFT;
                break;
            case Gravity.RIGHT:
                alignment = Layout.Alignment.ALIGN_RIGHT;
                break;
            case Gravity.CENTER_HORIZONTAL:
                alignment = Layout.Alignment.ALIGN_CENTER;
                break;
            default:
                alignment = Layout.Alignment.ALIGN_NORMAL;
                break;
        }
        return alignment;
    }
	
	/**
     * The width passed in is now the desired layout width,
     * not the full view width with padding.
     * {@hide}
     */
    protected void makeNewLayout(int wantWidth,
                                 BoringLayout.Metrics boring,
                                 int ellipsisWidth, boolean bringIntoView) {
        stopMarquee();

        // Update "old" cached values
        mOldMaximum = mMaximum;
        mOldMaxMode = mMaxMode;

        if (wantWidth < 0) {
            wantWidth = 0;
        }

        Layout.Alignment alignment = getLayoutAlignment();
        final boolean testDirChange = mSingleLine && mLayout != null &&
            (alignment == Layout.Alignment.ALIGN_NORMAL ||
             alignment == Layout.Alignment.ALIGN_OPPOSITE);
        int oldDir = 0;
        if (testDirChange) oldDir = mLayout.getParagraphDirection(0);
        boolean shouldEllipsize = mEllipsize != null;
        final boolean switchEllipsize = mEllipsize == TruncateAt.MARQUEE &&
                mMarqueeFadeMode != MARQUEE_FADE_NORMAL;
        TruncateAt effectiveEllipsize = mEllipsize;
        if (mEllipsize == TruncateAt.MARQUEE &&
                mMarqueeFadeMode == MARQUEE_FADE_SWITCH_SHOW_ELLIPSIS) {
            effectiveEllipsize = TruncateAt.END_SMALL;
        }

        if (mTextDir == null) {
            mTextDir = getTextDirectionHeuristic();
        }

        mLayout = makeSingleLayout(wantWidth, boring, ellipsisWidth, alignment, shouldEllipsize,
                effectiveEllipsize, effectiveEllipsize == mEllipsize);
        if (switchEllipsize) {
            TruncateAt oppositeEllipsize = effectiveEllipsize == TruncateAt.MARQUEE ?
                    TruncateAt.END : TruncateAt.MARQUEE;
            mSavedMarqueeModeLayout = makeSingleLayout(wantWidth, boring, ellipsisWidth, alignment,
                    shouldEllipsize, oppositeEllipsize, effectiveEllipsize != mEllipsize);
        }

        if (bringIntoView || (testDirChange && oldDir != mLayout.getParagraphDirection(0))) {
            registerForPreDraw();
        }

        if (mEllipsize == TextUtils.TruncateAt.MARQUEE) {
            final int height = mLayoutParams.height;
            // If the size of the view does not depend on the size of the text, try to
            // start the marquee immediately
            if (height != LayoutParams.WRAP_CONTENT && height != LayoutParams.MATCH_PARENT) {
                startMarquee();
            } else {
                // Defer the start of the marquee until we know our width (see setFrame())
                mRestartMarquee = true;
            }
        }

    }

    private Layout makeSingleLayout(int wantWidth, BoringLayout.Metrics boring, int ellipsisWidth,
            Layout.Alignment alignment, boolean shouldEllipsize, TruncateAt effectiveEllipsize,
            boolean useSaved) {
        Layout result = null;
        if (boring == UNKNOWN_BORING) {
            boring = BoringLayout.isBoring(getText(), mTextPaint, mTextDir, mBoring);
            if (boring != null) {
                mBoring = boring;
            }
        }

        if (boring != null) {
            if (boring.width <= wantWidth &&
                    (effectiveEllipsize == null || boring.width <= ellipsisWidth)) {
                if (useSaved && mSavedLayout != null) {
                    result = mSavedLayout.replaceOrMake(getText(), mTextPaint,
                            wantWidth, alignment, mSpacingMult, mSpacingAdd,
                            boring, mIncludePad);
                } else {
                    result = BoringLayout.make(getText(), mTextPaint,
                            wantWidth, alignment, mSpacingMult, mSpacingAdd,
                            boring, mIncludePad, mDrawDefer);
                }

                if (useSaved) {
                    mSavedLayout = (BoringLayout) result;
                }
            } else if (shouldEllipsize && boring.width <= wantWidth) {
                if (useSaved && mSavedLayout != null) {
                    result = mSavedLayout.replaceOrMake(getText(), mTextPaint,
                            wantWidth, alignment, mSpacingMult, mSpacingAdd,
                            boring, mIncludePad, effectiveEllipsize,
                            ellipsisWidth);
                } else {
                    result = BoringLayout.make(getText(), mTextPaint,
                            wantWidth, alignment, mSpacingMult, mSpacingAdd,
                            boring, mIncludePad, effectiveEllipsize,
                            ellipsisWidth, mDrawDefer);
                }
            } else if (shouldEllipsize) {
                result = new StaticLayout(getText(),
                        0, getText().length(),
                        mTextPaint, wantWidth, alignment, mTextDir, mSpacingMult,
                        mSpacingAdd, mIncludePad, effectiveEllipsize,
                        ellipsisWidth, mMaxMode == LINES ? mMaximum : Integer.MAX_VALUE, mDrawDefer);
            } else {
                result = new StaticLayout(getText(), mTextPaint,
                        wantWidth, alignment, mTextDir, mSpacingMult, mSpacingAdd,
                        mIncludePad, mDrawDefer);
            }
        } else if (shouldEllipsize) {
            result = new StaticLayout(getText(),
                    0, getText().length(),
                    mTextPaint, wantWidth, alignment, mTextDir, mSpacingMult,
                    mSpacingAdd, mIncludePad, effectiveEllipsize,
                    ellipsisWidth, mMaxMode == LINES ? mMaximum : Integer.MAX_VALUE, mDrawDefer);
        } else {
            result = new StaticLayout(getText(), mTextPaint,
                    wantWidth, alignment, mTextDir, mSpacingMult, mSpacingAdd,
                    mIncludePad, mDrawDefer);
        }
        return result;
    }
    
    private static int desired(Layout layout) {
        int n = layout.getLineCount();
        CharSequence text = layout.getText();
        float max = 0;

        // if any line was wrapped, we can't use it.
        // but it's ok for the last line not to have a newline

        for (int i = 0; i < n - 1; i++) {
            if (text.charAt(layout.getLineEnd(i) - 1) != '\n')
                return -1;
        }

        for (int i = 0; i < n; i++) {
            max = Math.max(max, layout.getLineWidth(i));
        }

        return (int) Math.ceil(max);
    }
    
    @Override
    public void setSelected(boolean selected) {
        boolean wasSelected = isSelected();

        super.setSelected(selected);

        if (selected != wasSelected && mEllipsize == TextUtils.TruncateAt.MARQUEE) {
            if (selected) {
                startMarquee();
            } else {
                stopMarquee();
            }
        }
    }
	
	private static final BoringLayout.Metrics UNKNOWN_BORING = new BoringLayout.Metrics();
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;

        BoringLayout.Metrics boring = UNKNOWN_BORING;

        if (mTextDir == null) {
            mTextDir = getTextDirectionHeuristic();
        }

        int des = -1;
        boolean fromexisting = false;

        if (widthMode == MeasureSpec.EXACTLY) {
            // Parent has told us how big to be. So be it.
            width = widthSize;
        } else {
            if (mLayout != null && mEllipsize == null) {
                des = desired(mLayout);
            }

            if (des < 0) {
                boring = BoringLayout.isBoring(mText, mTextPaint, mTextDir, mBoring);
                if (boring != null) {
                    mBoring = boring;
                }
            } else {
                fromexisting = true;
            }

            if (boring == null || boring == UNKNOWN_BORING) {
                if (des < 0) {
                    des = (int) Math.ceil(Layout.getDesiredWidth(mText, mTextPaint));
                }
                width = des;
            } else {
                width = boring.width;
            }

            final Drawables dr = mDrawables;
            if (dr != null) {
                width = Math.max(width, dr.mDrawableWidthTop);
                width = Math.max(width, dr.mDrawableWidthBottom);
            }

            width += getCompoundPaddingLeft() + getCompoundPaddingRight();

            if (mMaxWidthMode == EMS) {
                width = Math.min(width, mMaxWidth * getLineHeight());
            } else {
                width = Math.min(width, mMaxWidth);
            }

            if (mMinWidthMode == EMS) {
                width = Math.max(width, mMinWidth * getLineHeight());
            } else {
                width = Math.max(width, mMinWidth);
            }

            // Check against our minimum width
            width = Math.max(width, getSuggestedMinimumWidth());

            if (widthMode == MeasureSpec.AT_MOST) {
                width = Math.min(widthSize, width);
            }
        }

        int want = width - getCompoundPaddingLeft() - getCompoundPaddingRight();
        int unpaddedWidth = want;

        if (mHorizontallyScrolling) want = VERY_WIDE;

        if (mLayout == null) {
            makeNewLayout(want, boring,
                          width - getCompoundPaddingLeft() - getCompoundPaddingRight(), false);
        } else {
            final boolean layoutChanged = (mLayout.getWidth() != want) ||
                    (mLayout.getEllipsizedWidth() !=
                            width - getCompoundPaddingLeft() - getCompoundPaddingRight());

            final boolean widthChanged = 
                    (mEllipsize == null) &&
                    (want > mLayout.getWidth()) &&
                    (mLayout instanceof BoringLayout || (fromexisting && des >= 0 && des <= want));

            final boolean maximumChanged = (mMaxMode != mOldMaxMode) || (mMaximum != mOldMaximum);

            if (layoutChanged || maximumChanged) {
                if (!maximumChanged && widthChanged) {
                    mLayout.increaseWidthTo(want);
                } else {
                    makeNewLayout(want, boring,
                            width - getCompoundPaddingLeft() - getCompoundPaddingRight(), false);
                }
            } else {
                // Nothing has changed
            }
        }

        if (heightMode == MeasureSpec.EXACTLY) {
            // Parent has told us how big to be. So be it.
            height = heightSize;
            mDesiredHeightAtMeasure = -1;
        } else {
            int desired = getDesiredHeight();

            height = desired;
            mDesiredHeightAtMeasure = desired;

            if (heightMode == MeasureSpec.AT_MOST) {
                height = Math.min(desired, heightSize);
            }
        }

        int unpaddedHeight = height - getCompoundPaddingTop() - getCompoundPaddingBottom();
        if (mMaxMode == LINES && mLayout.getLineCount() > mMaximum) {
            unpaddedHeight = Math.min(unpaddedHeight, mLayout.getLineTop(mMaximum));
        }

        /*
         * We didn't let makeNewLayout() register to bring the cursor into view,
         * so do it here if there is any possibility that it is needed.
         */
        if (mLayout.getWidth() > unpaddedWidth ||
            mLayout.getHeight() > unpaddedHeight) {
            registerForPreDraw();
        } else {
            scrollTo(0, 0);
        }

        setMeasuredDimension(width, height);
	}
	
	private int getDesiredHeight() {
        return getDesiredHeight(mLayout, true);
    }

    private int getDesiredHeight(Layout layout, boolean cap) {
        if (layout == null) {
            return 0;
        }

        int linecount = layout.getLineCount();
        int pad = getCompoundPaddingTop() + getCompoundPaddingBottom();
        int desired = layout.getLineTop(linecount);

        final Drawables dr = mDrawables;
        if (dr != null) {
            desired = Math.max(desired, dr.mDrawableHeightLeft);
            desired = Math.max(desired, dr.mDrawableHeightRight);
        }

        desired += pad;

        if (mMaxMode == LINES) {
            /*
             * Don't cap the hint to a certain number of lines.
             * (Do cap it, though, if we have a maximum pixel height.)
             */
            if (cap) {
                if (linecount > mMaximum) {
                    desired = layout.getLineTop(mMaximum);

                    if (dr != null) {
                        desired = Math.max(desired, dr.mDrawableHeightLeft);
                        desired = Math.max(desired, dr.mDrawableHeightRight);
                    }

                    desired += pad;
                    linecount = mMaximum;
                }
            }
        } else {
            desired = Math.min(desired, mMaximum);
        }

        if (mMinMode == LINES) {
            if (linecount < mMinimum) {
                desired += getLineHeight() * (mMinimum - linecount);
            }
        } else {
            desired = Math.max(desired, mMinimum);
        }

        // Check against our minimum height
        desired = Math.max(desired, getSuggestedMinimumHeight());

        return desired;
    }
    
    /**
     * Check whether a change to the existing text layout requires a
     * new view layout.
     */
    private void checkForResize() {
        boolean sizeChanged = false;

        if (mLayout != null) {
            // Check if our width changed
            if (mLayoutParams.width == LayoutParams.WRAP_CONTENT) {
                sizeChanged = true;
                invalidate();
            }

            // Check if our height changed
            if (mLayoutParams.height == LayoutParams.WRAP_CONTENT) {
                int desiredHeight = getDesiredHeight();

                if (desiredHeight != this.getHeight()) {
                    sizeChanged = true;
                }
            } else if (mLayoutParams.height == LayoutParams.MATCH_PARENT) {
                if (mDesiredHeightAtMeasure >= 0) {
                    int desiredHeight = getDesiredHeight();

                    if (desiredHeight != mDesiredHeightAtMeasure) {
                        sizeChanged = true;
                    }
                }
            }
        }

        if (sizeChanged) {
            requestLayout();
            // caller will have already invalidated
        }
    }

    /**
     * Check whether entirely new text requires a new view layout
     * or merely a new text layout.
     */
    private void checkForRelayout() {
        // If we have a fixed width, we can just swap in a new text layout
        // if the text height stays the same or if the view height is fixed.

        if ((mLayoutParams.width != LayoutParams.WRAP_CONTENT ||
                (mMaxWidthMode == mMinWidthMode && mMaxWidth == mMinWidth)) &&
                (mRight - mLeft - getCompoundPaddingLeft() - getCompoundPaddingRight() > 0)) {
            // Static width, so try making a new text layout.

            int oldht = mLayout.getHeight();
            int want = mLayout.getWidth();

            /*
             * No need to bring the text into view, since the size is not
             * changing (unless we do the requestLayout(), in which case it
             * will happen at measure).
             */
            makeNewLayout(want, UNKNOWN_BORING,
                          mRight - mLeft - getCompoundPaddingLeft() - getCompoundPaddingRight(),
                          false);

            if (mEllipsize != TextUtils.TruncateAt.MARQUEE) {
                // In a fixed-height view, so use our new text layout.
                if (mLayoutParams.height != LayoutParams.WRAP_CONTENT &&
                    mLayoutParams.height != LayoutParams.MATCH_PARENT) {
                    invalidate();
                    return;
                }

                // Dynamic height, but height has stayed the same,
                // so use our new text layout.
                if (mLayout.getHeight() == oldht) {
                    invalidate();
                    return;
                }
            }

            // We lose: the height has changed and we have a dynamic height.
            // Request a new view layout using our new text layout.
            requestLayout();
            invalidate();
        } else {
            // Dynamic width, so we have no choice but to request a new
            // view layout with a new text layout.
            nullLayouts();
            requestLayout();
            invalidate();
        }
    }
	
	private boolean canMarquee() {
        int width = (mRight - mLeft - getCompoundPaddingLeft() - getCompoundPaddingRight());
        return width > 0 && (mLayout.getLineWidth(0) > width ||
                (mMarqueeFadeMode != MARQUEE_FADE_NORMAL && mSavedMarqueeModeLayout != null &&
                        mSavedMarqueeModeLayout.getLineWidth(0) > width));
    }

    private void startMarquee() {
        if ((mMarquee == null || mMarquee.isStopped()) && (isFocused() || isSelected()) &&
                getLineCount() == 1 && canMarquee()) {

            if (mMarqueeFadeMode == MARQUEE_FADE_SWITCH_SHOW_ELLIPSIS) {
                mMarqueeFadeMode = MARQUEE_FADE_SWITCH_SHOW_FADE;
                final Layout tmp = mLayout;
                mLayout = mSavedMarqueeModeLayout;
                mSavedMarqueeModeLayout = tmp;
                requestLayout();
                invalidate();
            }

            if (mMarquee == null) mMarquee = new Marquee(this);
            mMarquee.start(mMarqueeRepeatLimit);
        }
    }

    private void stopMarquee() {
    	if (mMarquee != null && !mMarquee.isStopped()) {
            mMarquee.stop();
        }

        if (mMarqueeFadeMode == MARQUEE_FADE_SWITCH_SHOW_FADE) {
            mMarqueeFadeMode = MARQUEE_FADE_SWITCH_SHOW_ELLIPSIS;
            final Layout tmp = mSavedMarqueeModeLayout;
            mSavedMarqueeModeLayout = mLayout;
            mLayout = tmp;
            requestLayout();
            invalidate();
        }
    }

    private void startStopMarquee(boolean start) {
        if (mEllipsize == TextUtils.TruncateAt.MARQUEE) {
            if (start) {
                startMarquee();
            } else {
                stopMarquee();
            }
        }
    }
    
    /**
     * This method is called when the text is changed, in case any subclasses
     * would like to know.
     *
     * Within <code>text</code>, the <code>lengthAfter</code> characters
     * beginning at <code>start</code> have just replaced old text that had
     * length <code>lengthBefore</code>. It is an error to attempt to make
     * changes to <code>text</code> from this callback.
     *
     * @param text The text the TextView is displaying
     * @param start The offset of the start of the range of the text that was
     * modified
     * @param lengthBefore The length of the former text that has been replaced
     * @param lengthAfter The length of the replacement modified text
     */
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        // intentionally empty, template pattern method can be overridden by subclasses
    }
    
	public int getLineHeight(){
		return FastMath.round(mTextPaint.getFontMetricsInt(null));
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
    
    /**
     * @hide
     */
    public int getHorizontalOffsetForDrawables() {
        return 0;
    }
    
	@Override
	protected void onDraw(GLCanvas canvas) {
		restartMarqueeIfNeeded();

        // Draw the background for this view
        super.onDraw(canvas);

        final int compoundPaddingLeft = getCompoundPaddingLeft();
        final int compoundPaddingTop = getCompoundPaddingTop();
        final int compoundPaddingRight = getCompoundPaddingRight();
        final int compoundPaddingBottom = getCompoundPaddingBottom();
        final int scrollX = mScrollX;
        final int scrollY = mScrollY;
        final int right = mRight;
        final int left = mLeft;
        final int bottom = mBottom;
        final int top = mTop;
        final boolean isLayoutRtl = isLayoutRtl();
        final int offset = getHorizontalOffsetForDrawables();
        final int leftOffset = isLayoutRtl ? 0 : offset;
        final int rightOffset = isLayoutRtl ? offset : 0 ;

        final Drawables dr = mDrawables;
        if (dr != null) {
            /*
             * Compound, not extended, because the icon is not clipped
             * if the text height is smaller.
             */

            int vspace = bottom - top - compoundPaddingBottom - compoundPaddingTop;
            int hspace = right - left - compoundPaddingRight - compoundPaddingLeft;

            // IMPORTANT: The coordinates computed are also used in invalidateDrawable()
            // Make sure to update invalidateDrawable() when changing this code.
            if (dr.mDrawableLeft != null) {
                canvas.save();
                canvas.translate(scrollX + mPaddingLeft + leftOffset,
                                 scrollY + compoundPaddingTop +
                                 (vspace - dr.mDrawableHeightLeft) / 2);
                dr.mDrawableLeft.draw(canvas);
                canvas.restore();
            }

            // IMPORTANT: The coordinates computed are also used in invalidateDrawable()
            // Make sure to update invalidateDrawable() when changing this code.
            if (dr.mDrawableRight != null) {
                canvas.save();
                canvas.translate(scrollX + right - left - mPaddingRight
                        - dr.mDrawableSizeRight - rightOffset,
                         scrollY + compoundPaddingTop + (vspace - dr.mDrawableHeightRight) / 2);
                dr.mDrawableRight.draw(canvas);
                canvas.restore();
            }

            // IMPORTANT: The coordinates computed are also used in invalidateDrawable()
            // Make sure to update invalidateDrawable() when changing this code.
            if (dr.mDrawableTop != null) {
                canvas.save();
                canvas.translate(scrollX + compoundPaddingLeft +
                        (hspace - dr.mDrawableWidthTop) / 2, scrollY + mPaddingTop);
                dr.mDrawableTop.draw(canvas);
                canvas.restore();
            }

            // IMPORTANT: The coordinates computed are also used in invalidateDrawable()
            // Make sure to update invalidateDrawable() when changing this code.
            if (dr.mDrawableBottom != null) {
                canvas.save();
                canvas.translate(scrollX + compoundPaddingLeft +
                        (hspace - dr.mDrawableWidthBottom) / 2,
                         scrollY + bottom - top - mPaddingBottom - dr.mDrawableSizeBottom);
                dr.mDrawableBottom.draw(canvas);
                canvas.restore();
            }
        }

//        int color = mCurTextColor;

        if (mLayout == null) {
            assumeLayout();
        }

        Layout layout = mLayout;

//        mTextPaint.setColor(color);
//        mTextPaint.drawableState = getDrawableState();
        
        boolean marquee = mEllipsize == TextUtils.TruncateAt.MARQUEE &&
                mMarqueeFadeMode != MARQUEE_FADE_SWITCH_SHOW_ELLIPSIS;

        if (marquee) {
        	canvas.save(GLCanvas.SAVE_FLAG_ALL);
        } else {
        	canvas.save();
        }
        /*  Would be faster if we didn't have to do this. Can we chop the
            (displayable) text so that we don't need to do this ever?
        */

        int extendedPaddingTop = getExtendedPaddingTop();
        int extendedPaddingBottom = getExtendedPaddingBottom();
        
        if (marquee) {
	        final int vspace = mBottom - mTop - compoundPaddingBottom - compoundPaddingTop;
	        final int maxScrollY = mLayout.getHeight() - vspace;
	
	        float clipLeft = compoundPaddingLeft + scrollX;
	        float clipTop = (scrollY == 0) ? 0 : extendedPaddingTop + scrollY;
	        float clipRight = right - left - compoundPaddingRight + scrollX;
	        float clipBottom = bottom - top + scrollY -
	                ((scrollY == maxScrollY) ? 0 : extendedPaddingBottom);
	
	        if (mShadowRadius != 0) {
	            clipLeft += Math.min(0, mShadowDx - mShadowRadius);
	            clipRight += Math.max(0, mShadowDx + mShadowRadius);
	
	            clipTop += Math.min(0, mShadowDy - mShadowRadius);
	            clipBottom += Math.max(0, mShadowDy + mShadowRadius);
	        }
	
	        canvas.clipRect(clipLeft, clipTop, clipRight, clipBottom);
        }

        int voffsetText = 0;
        int voffsetCursor = 0;

        // translate in by our padding
        /* shortcircuit calling getVerticaOffset() */
        if ((mGravity & Gravity.VERTICAL_GRAVITY_MASK) != Gravity.TOP) {
            voffsetText = getVerticalOffset(false);
            voffsetCursor = getVerticalOffset(true);
        }
        canvas.translate(compoundPaddingLeft, extendedPaddingTop + voffsetText);

        final int layoutDirection = getLayoutDirection();
        final int absoluteGravity = Gravity.getAbsoluteGravity(mGravity, layoutDirection);
        if (mEllipsize == TextUtils.TruncateAt.MARQUEE &&
                mMarqueeFadeMode != MARQUEE_FADE_SWITCH_SHOW_ELLIPSIS) {
            if (!mSingleLine && getLineCount() == 1 && canMarquee() &&
                    (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) != Gravity.LEFT) {
                final int width = mRight - mLeft;
                final int padding = getCompoundPaddingLeft() + getCompoundPaddingRight();
                final float dx = mLayout.getLineRight(0) - (width - padding);
                canvas.translate(layout.getParagraphDirection(0) * dx, 0.0f);
            }

            if (mMarquee != null && mMarquee.isRunning()) {
                final float dx = -mMarquee.getScroll();
                canvas.translate(layout.getParagraphDirection(0) * dx, 0.0f);
            }
        }

        layout.draw(canvas);

        if (mMarquee != null && mMarquee.shouldDrawGhost()) {
            final float dx = mMarquee.getGhostOffset();
            canvas.translate(layout.getParagraphDirection(0) * dx, 0.0f);
//            layout.draw(canvas, highlight, mHighlightPaint, cursorOffsetVertical);
            layout.draw(canvas);
        }

        canvas.restore();
	}
	
	/**
     * Return the number of lines of text, or 0 if the internal Layout has not
     * been built.
     */
    public int getLineCount() {
        return mLayout != null ? mLayout.getLineCount() : 0;
    }

    /**
     * Return the baseline for the specified line (0...getLineCount() - 1)
     * If bounds is not null, return the top, left, right, bottom extents
     * of the specified line in it. If the internal Layout has not been built,
     * return 0 and set bounds to (0, 0, 0, 0)
     * @param line which line to examine (0..getLineCount() - 1)
     * @param bounds Optional. If not null, it returns the extent of the line
     * @return the Y-coordinate of the baseline
     */
    public int getLineBounds(int line, Rect bounds) {
        if (mLayout == null) {
            if (bounds != null) {
                bounds.set(0, 0, 0, 0);
            }
            return 0;
        }
        else {
            int baseline = mLayout.getLineBounds(line, bounds);

            int voffset = getExtendedPaddingTop();
            if ((mGravity & Gravity.VERTICAL_GRAVITY_MASK) != Gravity.TOP) {
                voffset += getVerticalOffset(true);
            }
            if (bounds != null) {
                bounds.offset(getCompoundPaddingLeft(), voffset);
            }
            return baseline + voffset;
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
        
        if (mLayout != null && newLayout) {
            // XXX this is heavy-handed because no actual content changes.
            int want = mLayout.getWidth();

            makeNewLayout(want, UNKNOWN_BORING,
                          mRight - mLeft - getCompoundPaddingLeft() -
                          getCompoundPaddingRight(), true);
        }
    }
	
    /**
     * Sets the properties of this field (lines, horizontally scrolling,
     * transformation method) to be for a single-line input.
     *
     * @attr ref android.R.styleable#TextView_singleLine
     */
    public void setSingleLine() {
        setSingleLine(true);
    }

    /**
     * If true, sets the properties of this field (number of lines, horizontally scrolling,
     * transformation method) to be for a single-line input; if false, restores these to the default
     * conditions.
     *
     * Note that the default conditions are not necessarily those that were in effect prior this
     * method, and you may want to reset these properties to your custom values.
     *
     * @attr ref android.R.styleable#TextView_singleLine
     */
    public void setSingleLine(boolean singleLine) {
        // Could be used, but may break backward compatibility.
        // if (mSingleLine == singleLine) return;
        applySingleLine(singleLine, true);
    }
    
    private void applySingleLine(boolean singleLine,
            boolean changeMaxLines) {
        mSingleLine = singleLine;
        if (singleLine) {
            setLines(1);
            setHorizontallyScrolling(true);
        } else {
            if (changeMaxLines) {
                setMaxLines(Integer.MAX_VALUE);
            }
            setHorizontallyScrolling(false);
        }
    }
    
    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);

        mTextDir = getTextDirectionHeuristic();

        if (mLayout != null) {
            checkForRelayout();
        }
    }
    
    TextDirectionHeuristic getTextDirectionHeuristic() {
        // Always need to resolve layout direction first
        final boolean defaultIsRtl = (getLayoutDirection() == LAYOUT_DIRECTION_RTL);

        return (defaultIsRtl ? TextDirectionHeuristics.FIRSTSTRONG_RTL :
            TextDirectionHeuristics.FIRSTSTRONG_LTR);
        // Now, we can select the heuristic
//        switch (getTextDirection()) {
//            default:
//            case TEXT_DIRECTION_FIRST_STRONG:
//                return (defaultIsRtl ? TextDirectionHeuristics.FIRSTSTRONG_RTL :
//                        TextDirectionHeuristics.FIRSTSTRONG_LTR);
//            case TEXT_DIRECTION_ANY_RTL:
//                return TextDirectionHeuristics.ANYRTL_LTR;
//            case TEXT_DIRECTION_LTR:
//                return TextDirectionHeuristics.LTR;
//            case TEXT_DIRECTION_RTL:
//                return TextDirectionHeuristics.RTL;
//        }
    }

    /**
     * @hide
     */
    @Override
    public void onResolveDrawables(int layoutDirection) {
        // No need to resolve twice
        if (mLastLayoutDirection == layoutDirection) {
            return;
        }
        mLastLayoutDirection = layoutDirection;

        // Resolve drawables
        if (mDrawables != null) {
            mDrawables.resolveWithLayoutDirection(layoutDirection);
        }
    }
    
    private static class CharWrapper implements CharSequence, GetChars {
        private char[] mChars;
        private int mStart, mLength;

        public CharWrapper(char[] chars, int start, int len) {
            mChars = chars;
            mStart = start;
            mLength = len;
        }

        /* package */ void set(char[] chars, int start, int len) {
            mChars = chars;
            mStart = start;
            mLength = len;
        }

        public int length() {
            return mLength;
        }

        public char charAt(int off) {
            return mChars[off + mStart];
        }

        @Override
        public String toString() {
            return new String(mChars, mStart, mLength);
        }

        public CharSequence subSequence(int start, int end) {
            if (start < 0 || end < 0 || start > mLength || end > mLength) {
                throw new IndexOutOfBoundsException(start + ", " + end);
            }

            return new String(mChars, start + mStart, end - start);
        }

        public void getChars(int start, int end, char[] buf, int off) {
            if (start < 0 || end < 0 || start > mLength || end > mLength) {
                throw new IndexOutOfBoundsException(start + ", " + end);
            }

            System.arraycopy(mChars, start + mStart, buf, off, end - start);
        }
    }
    
    private static final class Marquee {
        // TODO: Add an option to configure this
        private static final float MARQUEE_DELTA_MAX = 0.07f;
        private static final int MARQUEE_DELAY = 1200;
        private static final int MARQUEE_RESTART_DELAY = 1200;
        private static final int MARQUEE_DP_PER_SECOND = 30;

        private static final byte MARQUEE_STOPPED = 0x0;
        private static final byte MARQUEE_STARTING = 0x1;
        private static final byte MARQUEE_RUNNING = 0x2;

        private final WeakReference<TextView> mView;
        private final Handler mHandler;

        private byte mStatus = MARQUEE_STOPPED;
        private final float mPixelsPerSecond;
        private float mMaxScroll;
        private float mMaxFadeScroll;
        private float mGhostStart;
        private float mGhostOffset;
        private float mFadeStop;
        private int mRepeatLimit;

        private float mScroll;
        private long mLastAnimationMs;

        Marquee(TextView v) {
            final float density = v.getContext().getResources().getDisplayMetrics().density;
            mPixelsPerSecond = MARQUEE_DP_PER_SECOND * density;
            mView = new WeakReference<TextView>(v);
            mHandler = new Handler(Looper.getMainLooper());
        }

        private Runnable mTickCallback = new Runnable() {
            @Override
            public void run() {
                tick();
            }
        };

        private Runnable mStartCallback = new Runnable() {
            @Override
            public void run() {
                mStatus = MARQUEE_RUNNING;
                mLastAnimationMs = AnimationUtils.currentAnimationTimeMillis();
                tick();
            }
        };

        private Runnable mRestartCallback = new Runnable() {
            @Override
            public void run() {
                if (mStatus == MARQUEE_RUNNING) {
                    if (mRepeatLimit >= 0) {
                        mRepeatLimit--;
                    }
                    start(mRepeatLimit);
                }
            }
        };

        void tick() {
            if (mStatus != MARQUEE_RUNNING) {
                return;
            }

            mHandler.removeCallbacks(mTickCallback);

            final TextView textView = mView.get();
            if (textView != null && (textView.isFocused() || textView.isSelected())) {
                long currentMs = AnimationUtils.currentAnimationTimeMillis();
                long deltaMs = currentMs - mLastAnimationMs;
                mLastAnimationMs = currentMs;
                float deltaPx = deltaMs / 1000f * mPixelsPerSecond;
                mScroll += deltaPx;
                if (mScroll > mMaxScroll) {
                    mScroll = mMaxScroll;
                    mHandler.postDelayed(mRestartCallback, MARQUEE_DELAY);
                } else {
                	mHandler.post(mTickCallback);
                }
                textView.invalidate();
            }
        }

        void stop() {
            mStatus = MARQUEE_STOPPED;
            mHandler.removeCallbacks(mStartCallback);
            mHandler.removeCallbacks(mRestartCallback);
            mHandler.removeCallbacks(mTickCallback);
            resetScroll();
        }

        private void resetScroll() {
            mScroll = 0.0f;
            final TextView textView = mView.get();
            if (textView != null) textView.invalidate();
        }

        void start(int repeatLimit) {
            if (repeatLimit == 0) {
                stop();
                return;
            }
            mRepeatLimit = repeatLimit;
            final TextView textView = mView.get();
            if (textView != null && textView.mLayout != null) {
                mStatus = MARQUEE_STARTING;
                mScroll = 0.0f;
                final int textWidth = textView.getWidth() - textView.getCompoundPaddingLeft() -
                        textView.getCompoundPaddingRight();
                final float lineWidth = textView.mLayout.getLineWidth(0);
                final float gap = textWidth / 3.0f;
                mGhostStart = lineWidth - textWidth + gap;
                mMaxScroll = mGhostStart + textWidth;
                mGhostOffset = lineWidth + gap;
                mFadeStop = lineWidth + textWidth / 6.0f;
                mMaxFadeScroll = mGhostStart + lineWidth + lineWidth;

                textView.invalidate();
                mHandler.post(mStartCallback);
            }
        }

        float getGhostOffset() {
            return mGhostOffset;
        }

        float getScroll() {
            return mScroll;
        }

        float getMaxFadeScroll() {
            return mMaxFadeScroll;
        }

        boolean shouldDrawLeftFade() {
            return mScroll <= mFadeStop;
        }

        boolean shouldDrawGhost() {
            return mStatus == MARQUEE_RUNNING && mScroll > mGhostStart;
        }

        boolean isRunning() {
            return mStatus == MARQUEE_RUNNING;
        }

        boolean isStopped() {
            return mStatus == MARQUEE_STOPPED;
        }
    }
}
