package com.glview.widget;

import java.util.ArrayList;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.FrameLayout.LayoutParams;

import com.glview.content.GLContext;
import com.glview.graphics.Rect;
import com.glview.graphics.drawable.Drawable;
import com.glview.hwui.GLCanvas;
import com.glview.view.Gravity;
import com.glview.view.View;
import com.glview.view.ViewGroup;

public class FrameLayout extends ViewGroup{
	
	private final static String TAG = "GLFrameLayout";
	
    boolean mMeasureAllChildren = false;
    private static final int DEFAULT_CHILD_GRAVITY = Gravity.TOP | Gravity.START;
    
    private final ArrayList<View> mMatchParentChildren = new ArrayList<View>(1);
    
    private int mForegroundPaddingLeft = 0;
    private int mForegroundPaddingTop = 0;
    private int mForegroundPaddingRight = 0;
    private int mForegroundPaddingBottom = 0;
    
    private final Rect mSelfBounds = new Rect();
    private final Rect mOverlayBounds = new Rect();
    
    private int mForegroundGravity = Gravity.FILL;
    protected boolean mForegroundInPadding = true;
    
    private Drawable mForeground;
    
    boolean mForegroundBoundsChanged = false;

    public FrameLayout(Context context) {
        super(context);
    }
    
    public FrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public FrameLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        TypedArray a = context.obtainStyledAttributes(
        		attrs, com.glview.R.styleable.FrameLayout, defStyleAttr, defStyleRes);
        
        mForegroundGravity = a.getInt(
                com.glview.R.styleable.FrameLayout_foregroundGravity, mForegroundGravity);

        final Drawable d = GLContext.get().getResources().getDrawable(a.getResourceId(com.glview.R.styleable.FrameLayout_foreground, 0));
        if (d != null) {
            setForeground(d);
        }

        a.recycle();
    }
	
    /**
     * {@inheritDoc}
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int count = mChildrenCount;

        final boolean measureMatchParentChildren =
                MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY ||
                MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY;
        mMatchParentChildren.clear();
        
        int maxHeight = 0;
        int maxWidth = 0;
        int childState = 0;

        for (int i = 0; i < count; i++) {
            final View child = mChildren[i];
            if (mMeasureAllChildren || child.getVisibility() != GONE) {
                measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                maxWidth = Math.max(maxWidth,
                        child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin);
                maxHeight = Math.max(maxHeight,
                        child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin);
                childState = combineMeasuredStates(childState, child.getMeasuredState());
                if (measureMatchParentChildren) {
                    if (lp.width == LayoutParams.MATCH_PARENT ||
                            lp.height == LayoutParams.MATCH_PARENT) {
                        mMatchParentChildren.add(child);
                    }
                }
            }
        }

        // Account for padding too
        maxWidth += getPaddingLeftWithForeground() + getPaddingRightWithForeground();
        maxHeight += getPaddingTopWithForeground() + getPaddingBottomWithForeground();

        // Check against our minimum height and width
        maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());
        maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());

        // Check against our foreground's minimum height and width
//        final Drawable drawable = getForeground();
//        if (drawable != null) {
//            maxHeight = Math.max(maxHeight, drawable.getMinimumHeight());
//            maxWidth = Math.max(maxWidth, drawable.getMinimumWidth());
//        }

        setMeasuredDimension(resolveSizeAndState(maxWidth, widthMeasureSpec, childState),
                resolveSizeAndState(maxHeight, heightMeasureSpec,
                        childState << MEASURED_HEIGHT_STATE_SHIFT));
        

        count = mMatchParentChildren.size();
        if (count > 1) {
        	for (int i = 0; i < count; i++) {
        		 final View child = mMatchParentChildren.get(i);
//        		final GLView child = mChildren[i];
        		
        		final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        		int childWidthMeasureSpec;
        		int childHeightMeasureSpec;

        		if (lp.width == LayoutParams.MATCH_PARENT) {
        			childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
							getMeasuredWidth() 
							- getPaddingLeftWithForeground() 
							- getPaddingRightWithForeground() -
        					lp.leftMargin - lp.rightMargin, MeasureSpec.EXACTLY);
        		} else {
        			childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec,
        					getPaddingLeftWithForeground() +
        					getPaddingRightWithForeground() +
        					lp.leftMargin + lp.rightMargin, lp.width);
        		}

        		if (lp.height == LayoutParams.MATCH_PARENT) {
        			childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
        					getMeasuredHeight() -
        					getPaddingTopWithForeground() -
        					getPaddingBottomWithForeground() -
        					lp.topMargin - lp.bottomMargin, MeasureSpec.EXACTLY);
        		} else {
        			childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec,
        					getPaddingTopWithForeground() +
        					getPaddingBottomWithForeground() +
        					lp.topMargin + lp.bottomMargin, lp.height);
        		}
        		child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
        	}
        }
    }
    
    /**
     * Sets whether to consider all children, or just those in
     * the VISIBLE or INVISIBLE state, when measuring. Defaults to false.
     *
     * @param measureAll true to consider children marked GONE, false otherwise.
     * Default value is false.
     *
     */
    public void setMeasureAllChildren(boolean measureAll) {
        mMeasureAllChildren = measureAll;
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        layoutChildren(left, top, right, bottom, false /* no force left gravity */);
    }

    void layoutChildren(int left, int top, int right, int bottom,
                                  boolean forceLeftGravity) {
        final int count = getChildCount();

        final int parentLeft = getPaddingLeftWithForeground();
        final int parentRight = right - left - getPaddingRightWithForeground();

        final int parentTop = getPaddingTopWithForeground();
        final int parentBottom = bottom - top - getPaddingBottomWithForeground();

        mForegroundBoundsChanged = true;
        
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();

                final int width = child.getMeasuredWidth();
                final int height = child.getMeasuredHeight();

                int childLeft;
                int childTop;

                int gravity = lp.gravity;
                if (gravity == -1) {
                    gravity = DEFAULT_CHILD_GRAVITY;
                }

                final int layoutDirection = getLayoutDirection();
                final int absoluteGravity = Gravity.getAbsoluteGravity(gravity, layoutDirection);
                final int verticalGravity = gravity & Gravity.VERTICAL_GRAVITY_MASK;

                switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
                    case Gravity.CENTER_HORIZONTAL:
                        childLeft = parentLeft + (parentRight - parentLeft - width) / 2 +
                        lp.leftMargin - lp.rightMargin;
                        break;
                    case Gravity.RIGHT:
                        if (!forceLeftGravity) {
                            childLeft = parentRight - width - lp.rightMargin;
                            break;
                        }
                    case Gravity.LEFT:
                    default:
                        childLeft = parentLeft + lp.leftMargin;
                }

                switch (verticalGravity) {
                    case Gravity.TOP:
                        childTop = parentTop + lp.topMargin;
                        break;
                    case Gravity.CENTER_VERTICAL:
                        childTop = parentTop + (parentBottom - parentTop - height) / 2 +
                        lp.topMargin - lp.bottomMargin;
                        break;
                    case Gravity.BOTTOM:
                        childTop = parentBottom - height - lp.bottomMargin;
                        break;
                    default:
                        childTop = parentTop + lp.topMargin;
                }

                child.layout(childLeft, childTop, childLeft + width, childTop + height);
            }
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mForegroundBoundsChanged = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void draw(GLCanvas canvas) {
        super.draw(canvas);

        if (mForeground != null) {
            final Drawable foreground = mForeground;

            if (mForegroundBoundsChanged) {
                mForegroundBoundsChanged = false;
                final Rect selfBounds = mSelfBounds;
                final Rect overlayBounds = mOverlayBounds;

                final int w = mRight-mLeft;
                final int h = mBottom-mTop;

                if (mForegroundInPadding) {
                    selfBounds.set(0, 0, w, h);
                } else {
                    selfBounds.set(mPaddingLeft, mPaddingTop, w - mPaddingRight, h - mPaddingBottom);
                }

                final int layoutDirection = getLayoutDirection();
                Gravity.apply(mForegroundGravity, foreground.getIntrinsicWidth(),
                        foreground.getIntrinsicHeight(), selfBounds, overlayBounds,
                        layoutDirection);
                foreground.setBounds(overlayBounds);
            }
            
            foreground.draw(canvas);
        }
    }
    
    /**
     * Supply a Drawable that is to be rendered on top of all of the child
     * views in the frame layout.  Any padding in the Drawable will be taken
     * into account by ensuring that the children are inset to be placed
     * inside of the padding area.
     * 
     * @param d The Drawable to be drawn on top of the children.
     * 
     * @attr ref android.R.styleable#FrameLayout_foreground
     */
    public void setForeground(Drawable d) {
        if (mForeground != d) {
            if (mForeground != null) {
                mForeground.setCallback(null);
                unscheduleDrawable(mForeground);
            }

            mForeground = d;
            mForegroundPaddingLeft = 0;
            mForegroundPaddingTop = 0;
            mForegroundPaddingRight = 0;
            mForegroundPaddingBottom = 0;

            if (d != null) {
                setWillNotDraw(false);
                d.setCallback(this);
                d.setLayoutDirection(getLayoutDirection());
                if (d.isStateful()) {
                    d.setState(getDrawableState());
                }
                if (mForegroundGravity == Gravity.FILL) {
                    Rect padding = new Rect();
                    if (d.getPadding(padding)) {
                        mForegroundPaddingLeft = padding.left;
                        mForegroundPaddingTop = padding.top;
                        mForegroundPaddingRight = padding.right;
                        mForegroundPaddingBottom = padding.bottom;
                    }
                }
            }  else {
                setWillNotDraw(true);
            }
            requestLayout();
            invalidate();
        }
    }

    /**
     * Returns the drawable used as the foreground of this FrameLayout. The
     * foreground drawable, if non-null, is always drawn on top of the children.
     *
     * @return A Drawable or null if no foreground was set.
     */
    public Drawable getForeground() {
        return mForeground;
    }
    
    public void setForegroundGravity(int foregroundGravity) {
        if (mForegroundGravity != foregroundGravity) {
            if ((foregroundGravity & Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK) == 0) {
                foregroundGravity |= Gravity.START;
            }

            if ((foregroundGravity & Gravity.VERTICAL_GRAVITY_MASK) == 0) {
                foregroundGravity |= Gravity.TOP;
            }

            mForegroundGravity = foregroundGravity;


            if (mForegroundGravity == Gravity.FILL && mForeground != null) {
                Rect padding = new Rect();
                if (mForeground.getPadding(padding)) {
                    mForegroundPaddingLeft = padding.left;
                    mForegroundPaddingTop = padding.top;
                    mForegroundPaddingRight = padding.right;
                    mForegroundPaddingBottom = padding.bottom;
                }
            } else {
                mForegroundPaddingLeft = 0;
                mForegroundPaddingTop = 0;
                mForegroundPaddingRight = 0;
                mForegroundPaddingBottom = 0;
            }

            requestLayout();
        }
    }
    
    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (mForeground != null) {
            mForeground.setVisible(visibility == VISIBLE, false);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who) || (who == mForeground);
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        if (mForeground != null) mForeground.jumpToCurrentState();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (mForeground != null && mForeground.isStateful()) {
            mForeground.setState(getDrawableState());
        }
    }

    int getPaddingLeftWithForeground() {
        return mForegroundInPadding ? Math.max(mPaddingLeft, mForegroundPaddingLeft) :
            mPaddingLeft + mForegroundPaddingLeft;
    }

    int getPaddingRightWithForeground() {
        return mForegroundInPadding ? Math.max(mPaddingRight, mForegroundPaddingRight) :
            mPaddingRight + mForegroundPaddingRight;
    }

    int getPaddingTopWithForeground() {
        return mForegroundInPadding ? Math.max(mPaddingTop, mForegroundPaddingTop) :
            mPaddingTop + mForegroundPaddingTop;
    }

    int getPaddingBottomWithForeground() {
        return mForegroundInPadding ? Math.max(mPaddingBottom, mForegroundPaddingBottom) :
            mPaddingBottom + mForegroundPaddingBottom;
    }
    
    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new FrameLayout.LayoutParams(mContext, attrs);        
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }
    
    public static class LayoutParams extends ViewGroup.MarginLayoutParams{
    	
    	/**
         * The gravity to apply with the View to which these layout parameters
         * are associated.
         *
         * @see android.view.Gravity
         * 
         * @attr ref android.R.styleable#FrameLayout_Layout_layout_gravity
         */
        public int gravity = -1;
    	

        /**
         * {@inheritDoc}
         */
        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);

            TypedArray a = c.obtainStyledAttributes(attrs, com.glview.R.styleable.FrameLayout_Layout);
            gravity = a.getInt(com.glview.R.styleable.FrameLayout_Layout_layout_gravity, -1);
            a.recycle();
        }

        /**
         * {@inheritDoc}
         */
        public LayoutParams(int width, int height) {
            super(width, height);
        }

        /**
         * Creates a new set of layout parameters with the specified width, height
         * and weight.
         *
         * @param width the width, either {@link #MATCH_PARENT},
         *        {@link #WRAP_CONTENT} or a fixed size in pixels
         * @param height the height, either {@link #MATCH_PARENT},
         *        {@link #WRAP_CONTENT} or a fixed size in pixels
         * @param gravity the gravity
         *
         * @see android.view.Gravity
         */
        public LayoutParams(int width, int height, int gravity) {
            super(width, height);
            this.gravity = gravity;
        }

        /**
         * {@inheritDoc}
         */
        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }

}
