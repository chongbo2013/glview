package com.glview.view;

import java.util.ArrayList;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.PointF;
import android.os.Build;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.TextureView;

import com.glview.R;
import com.glview.animation.LayoutTransition;
import com.glview.graphics.Rect;
import com.glview.hwui.GLCanvas;
import com.glview.view.animation.Animation;
import com.glview.view.animation.LayoutAnimationController;

public abstract class ViewGroup extends View{
	
    private static final int ARRAY_INITIAL_CAPACITY = 12;
    private static final int ARRAY_CAPACITY_INCREMENT = 12;
	
    // Child views of this GLViewGroup
    protected View[] mChildren;
    protected int mChildrenCount;
    
 // Whether layout calls are currently being suppressed, controlled by calls to
    // suppressLayout()
    boolean mSuppressLayout = false;

    // Whether any layout calls have actually been suppressed while mSuppressLayout
    // has been true. This tracks whether we need to issue a requestLayout() when
    // layout is later re-enabled.
    private boolean mLayoutCalledWhileSuppressed = false;
    
    // The view contained within this GLViewGroup that has or contains focus.
    protected View mFocused;
    
    // Layout animation
    private LayoutAnimationController mLayoutAnimationController;
    private Animation.AnimationListener mAnimationListener;
    
 // First touch target in the linked list of touch targets.
    private TouchTarget mFirstTouchTarget;

    // For debugging only.  You can see these in hierarchyviewer.
    private long mLastTouchDownTime;
    private int mLastTouchDownIndex = -1;
    private float mLastTouchDownX;
    private float mLastTouchDownY;
    
    private final static String TAG = "GLViewGroup";    

    private LayoutTransition mTransition;
    
 // The set of views that are currently being transitioned. This list is used to track views
    // being removed that should not actually be removed from the parent yet because they are
    // being animated.
    private ArrayList<View> mTransitioningViews;
    
 // List of children changing visibility. This is used to potentially keep rendering
    // views during a transition when they otherwise would have become gone/invisible
    private ArrayList<View> mVisibilityChangingChildren;
    
    /**
     * GLViews which have been hidden or removed which need to be animated on
     * their way out.
     * This field should be made private, so it is hidden from the SDK.
     * 
     */
    protected ArrayList<View> mDisappearingChildren;
    
    protected int mGroupFlags;
    
    /**
     * When set, the drawing method will call {@link #getChildDrawingOrder(int, int)}
     * to get the index of the child to draw for that iteration.
     * 
     * @hide
     */
    protected static final int FLAG_USE_CHILD_DRAWING_ORDER = 0x400;
    
    /**
     * When set, this ViewGroup should not intercept touch events.
     * {@hide}
     */
    protected static final int FLAG_DISALLOW_INTERCEPT = 0x80000;
    
    /**
     * When set, this ViewGroup will split MotionEvents to multiple child Views when appropriate.
     */
    private static final int FLAG_SPLIT_MOTION_EVENTS = 0x200000;
    
    /**
     * When set, this ViewGroup's drawable states also include those
     * of its children.
     */
    private static final int FLAG_ADD_STATES_FROM_CHILDREN = 0x2000;
    
    /**
     * When set, this group will go through its list of children to notify them of
     * any drawable state change.
     */
    private static final int FLAG_NOTIFY_CHILDREN_ON_DRAWABLE_STATE_CHANGE = 0x10000;
    
    /**
     * When set, this ViewGroup will not dispatch onAttachedToWindow calls
     * to children when adding new views. This is used to prevent multiple
     * onAttached calls when a ViewGroup adds children in its own onAttached method.
     */
    private static final int FLAG_PREVENT_DISPATCH_ATTACHED_TO_WINDOW = 0x400000;
    
    
    /**
     * This constant is a {@link #setLayoutMode(int) layoutMode}.
     * Clip bounds are the raw values of {@link #getLeft() left}, {@link #getTop() top},
     * {@link #getRight() right} and {@link #getBottom() bottom}.
     *
     * @hide
     */
    public static final int CLIP_BOUNDS = 0;

    /**
     * This constant is a {@link #setLayoutMode(int) layoutMode}.
     * Optical bounds describe where a widget appears to be. They sit inside the clip
     * bounds which need to cover a larger area to allow other effects,
     * such as shadows and glows, to be drawn.
     *
     * @hide
     */
    public static final int OPTICAL_BOUNDS = 1;
    
    /*
     * The layout mode: either {@link #CLIP_BOUNDS} or {@link #OPTICAL_BOUNDS}
     */
    private int mLayoutMode = CLIP_BOUNDS;
    
    // When set, ViewGroup invalidates only the child's rectangle
    // Set by default
    static final int FLAG_CLIP_CHILDREN = 0x1;

    // When set, ViewGroup excludes the padding area from the invalidate rectangle
    // Set by default
    private static final int FLAG_CLIP_TO_PADDING = 0x2;
    
    // When set, dispatchDraw() will invoke invalidate(); this is set by drawChild() when
    // a child needs to be invalidated and FLAG_OPTIMIZE_INVALIDATE is set
    static final int FLAG_INVALIDATE_REQUIRED  = 0x4;
    
    // When set, dispatchDraw() will run the layout animation and unset the flag
    private static final int FLAG_RUN_ANIMATION = 0x8;

    // When set, there is either no layout animation on the ViewGroup or the layout
    // animation is over
    // Set by default
    static final int FLAG_ANIMATION_DONE = 0x10;
    
 // If set, this ViewGroup has padding; if unset there is no padding and we don't need
    // to clip it, even if FLAG_CLIP_TO_PADDING is set
    private static final int FLAG_PADDING_NOT_NULL = 0x20;
    
    /**
     * We clip to padding when FLAG_CLIP_TO_PADDING and FLAG_PADDING_NOT_NULL
     * are set at the same time.
     */
    protected static final int CLIP_TO_PADDING_MASK = FLAG_CLIP_TO_PADDING | FLAG_PADDING_NOT_NULL;
    
    private static final int FLAG_MASK_FOCUSABILITY = 0x60000;

    /**
     * This view will get focus before any of its descendants.
     */
    public static final int FOCUS_BEFORE_DESCENDANTS = 0x20000;

    /**
     * This view will get focus only if none of its descendants want it.
     */
    public static final int FOCUS_AFTER_DESCENDANTS = 0x40000;

    /**
     * This view will block any of its descendants from getting focus, even
     * if they are focusable.
     */
    public static final int FOCUS_BLOCK_DESCENDANTS = 0x60000;

    /**
     * Used to map between enum in attrubutes and flag values.
     */
    private static final int[] DESCENDANT_FOCUSABILITY_FLAGS =
            {FOCUS_BEFORE_DESCENDANTS, FOCUS_AFTER_DESCENDANTS,
                    FOCUS_BLOCK_DESCENDANTS};
    
    // Indicates how many of this container's child subtrees contain transient state
    private int mChildCountWithTransientState = 0;
    
    /**
     * Currently registered axes for nested scrolling. Flag set consisting of
     * {@link #SCROLL_AXIS_HORIZONTAL} {@link #SCROLL_AXIS_VERTICAL} or {@link #SCROLL_AXIS_NONE}
     * for null.
     */
    private int mNestedScrollAxes;
    
    public ViewGroup(Context context) {
        this(context, null);
    }

    public ViewGroup(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ViewGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ViewGroup(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initViewGroup();
        initFromAttributes(context, attrs, defStyleAttr, defStyleRes);
    }
    
    private void initViewGroup(){
    	mGroupFlags |= FLAG_SPLIT_MOTION_EVENTS;
    	
    	setDescendantFocusability(FOCUS_BEFORE_DESCENDANTS);
    	
        mChildren = new View[ARRAY_INITIAL_CAPACITY];
        mChildrenCount = 0;
    }
    
    private void initFromAttributes(
            Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ViewGroup, defStyleAttr,
                defStyleRes);

        final int N = a.getIndexCount();
		for (int i = 0; i < N; i++) {
			int attr = a.getIndex(i);
			if (attr == R.styleable.ViewGroup_clipChildren) {
				setClipChildren(a.getBoolean(attr, true));
			} else if (attr == R.styleable.ViewGroup_clipToPadding) {
				// setClipToPadding(a.getBoolean(attr, true));
			} else if (attr == R.styleable.ViewGroup_animationCache) {
				// setAnimationCacheEnabled(a.getBoolean(attr, true));
			} else if (attr == R.styleable.ViewGroup_persistentDrawingCache) {
				// setPersistentDrawingCache(a.getInt(attr,
				// PERSISTENT_SCROLLING_CACHE));
			} else if (attr == R.styleable.ViewGroup_addStatesFromChildren) {
				// setAddStatesFromChildren(a.getBoolean(attr, false));
			} else if (attr == R.styleable.ViewGroup_alwaysDrawnWithCache) {
				// setAlwaysDrawnWithCacheEnabled(a.getBoolean(attr, true));
			} else if (attr == R.styleable.ViewGroup_layoutAnimation) {
				// int id = a.getResourceId(attr, -1);
				// if (id > 0) {
				// setLayoutAnimation(AnimationUtils.loadLayoutAnimation(mContext,
				// id));
				// }
			} else if (attr == R.styleable.ViewGroup_descendantFocusability) {
				// setDescendantFocusability(DESCENDANT_FOCUSABILITY_FLAGS[a.getInt(attr,
				// 0)]);
			} else if (attr == R.styleable.ViewGroup_splitMotionEvents) {
				// setMotionEventSplittingEnabled(a.getBoolean(attr, false));
			} else if (attr == R.styleable.ViewGroup_animateLayoutChanges) {
				// boolean animateLayoutChanges = a.getBoolean(attr, false);
				// if (animateLayoutChanges) {
				// setLayoutTransition(new LayoutTransition());
				// }
				break;
			}
		}

        a.recycle();
    }
    
    public static final int INVALID_POSITION = -1;
    protected boolean mDrawSelectorOnTop = true;
    
    public void setDrawSelectorOnTop(boolean onTop) {
        mDrawSelectorOnTop = onTop;
    }
    
    
    public int indexOfChild(View view){
    	if(view == null){
    		return INVALID_POSITION;
    	}
    	for(int i = 0; i < mChildrenCount; i++){
    		if(mChildren[i] == view){
    			return i;
    		}
    	}
    	
    	return INVALID_POSITION;
    }
    
    
    
    /**
     * <p>Adds a child view. If no layout parameters are already set on the child, the
     * default parameters for this GLViewGroup are set on the child.</p>
     * 
     * <p><strong>Note:</strong> do not invoke this method from
     * {@link #draw(android.graphics.Canvas)}, {@link #onDraw(android.graphics.Canvas)},
     * {@link #dispatchDraw(android.graphics.Canvas)} or any related method.</p>
     *
     * @param child the child view to add
     *
     * @see #generateDefaultLayoutParams()
     */
    public void addView(View child) {
        addView(child, -1);
    }
    
    /**
     * Adds a child view. If no layout parameters are already set on the child, the
     * default parameters for this GLViewGroup are set on the child.
     * 
     *
     * @param child the child view to add
     * @param index the position at which to add the child
     *
     * @see #generateDefaultLayoutParams()
     */
    public void addView(View child, int index){
        LayoutParams params = child.getLayoutParams();
        if (params == null) {
            params = generateDefaultLayoutParams();
            if (params == null) {
                throw new IllegalArgumentException("generateDefaultLayoutParams() cannot return null");
            }
        }
        addView(child, index, params);
    }
    
    /**
     * Adds a child view with the specified layout parameters.
     *
     * <p><strong>Note:</strong> do not invoke this method from
     * {@link #draw(android.graphics.Canvas)}, {@link #onDraw(android.graphics.Canvas)},
     * {@link #dispatchDraw(android.graphics.Canvas)} or any related method.</p>
     *
     * @param child the child view to add
     * @param params the layout parameters to set on the child
     */
    public void addView(View child, LayoutParams params) {
    	addView(child, -1, params);
    }

    /**
     * Adds a child view with the specified layout parameters.
     *
     * <p><strong>Note:</strong> do not invoke this method from
     * {@link #draw(android.graphics.Canvas)}, {@link #onDraw(android.graphics.Canvas)},
     * {@link #dispatchDraw(android.graphics.Canvas)} or any related method.</p>
     *
     * @param child the child view to add
     * @param index the position at which to add the child
     * @param params the layout parameters to set on the child
     */
    public void addView(View child, int index, LayoutParams params) {
        
        if(params == null){
        	 params = generateDefaultLayoutParams();
        } else if (!checkLayoutParams(params)) {
            params = generateLayoutParams(params);
        }
        

        // addViewInner() will call child.requestLayout() when setting the new LayoutParams
        // therefore, we call requestLayout() on ourselves before, so that the child's request
        // will be blocked at our level
        requestLayout();
        invalidate();
        addViewInner(child, index, params, false);
    }

    /**
     * {@inheritDoc}
     */
    public void updateViewLayout(View view, ViewGroup.LayoutParams params) {
        if (!checkLayoutParams(params)) {
            throw new IllegalArgumentException("Invalid LayoutParams supplied to " + this);
        }
        if (view.mParent != this) {
            throw new IllegalArgumentException("Given view not a child of " + this);
        }
        view.setLayoutParams(params);
    }

    /**
     * {@inheritDoc}
     */
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return  p != null;
    }
    
    /**
     * Returns a set of default layout parameters. These parameters are requested
     * when the View passed to {@link #addView(View)} has no layout parameters
     * already set. If null is returned, an exception is thrown from addView.
     *
     * @return a set of default layout parameters or null
     */
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams();
    }
    
    /**
     * Returns a safe set of layout parameters based on the supplied layout params.
     * When a GLViewGroup is passed a View whose layout params do not pass the test of
     * {@link #checkLayoutParams(ViewGroup.LayoutParams)}, this method
     * is invoked. This method should return a new set of layout params suitable for
     * this GLViewGroup, possibly by copying the appropriate attributes from the
     * specified set of layout params.
     *
     * @param p The layout parameters to convert into a suitable set of layout parameters
     *          for this GLViewGroup.
     *
     * @return an instance of {@link ViewGroup.LayoutParams} or one
     *         of its descendants
     */
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return p;
    }
    
    /**
     * Returns a new set of layout parameters based on the supplied attributes set.
     *
     * @param attrs the attributes to build the layout parameters from
     *
     * @return an instance of {@link android.view.ViewGroup.LayoutParams} or one
     *         of its descendants
     */
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(mContext, attrs);
    }
    
    /**
     * Adds a view during layout. This is useful if in your onLayout() method,
     * you need to add more views (as does the list view for example).
     *
     * If index is negative, it means put it at the end of the list.
     *
     * @param child the view to add to the group
     * @param index the index at which the child must be added
     * @param params the layout parameters to associate with the child
     * @return true if the child was added, false otherwise
     */
    protected boolean addViewInLayout(View child, int index, LayoutParams params) {
        return addViewInLayout(child, index, params, false);
    }

    /**
     * Adds a view during layout. This is useful if in your onLayout() method,
     * you need to add more views (as does the list view for example).
     *
     * If index is negative, it means put it at the end of the list.
     *
     * @param child the view to add to the group
     * @param index the index at which the child must be added
     * @param params the layout parameters to associate with the child
     * @param preventRequestLayout if true, calling this method will not trigger a
     *        layout request on child
     * @return true if the child was added, false otherwise
     */
    protected boolean addViewInLayout(View child, int index, LayoutParams params,
            boolean preventRequestLayout) {
        child.mParent = null;
        addViewInner(child, index, params, preventRequestLayout);
//        child.mPrivateFlags = (child.mPrivateFlags & ~PFLAG_DIRTY_MASK) | PFLAG_DRAWN;
        return true;
    }
    
    private void addViewInner(View child, int index, LayoutParams params,
            boolean preventRequestLayout) {

    	if (mTransition != null) {
            // Don't prevent other add transitions from completing, but cancel remove
            // transitions to let them complete the process before we add to the container
    		mTransition.cancel(LayoutTransition.DISAPPEARING);
        }
    	
        if (child.getParent() != null) {
            throw new IllegalStateException("The specified child already has a parent. " +
                    "You must call removeView() on the child's parent first.");
        }

        if (mTransition != null) {
        	mTransition.addChild(this, child);
        }

        if (!checkLayoutParams(params)) {
            params = generateLayoutParams(params);
        }

        if (preventRequestLayout) {
            child.mLayoutParams = params;
        } else {
            child.setLayoutParams(params);
        }

        if (index < 0) {
            index = mChildrenCount;
        }

        addInArray(child, index);

//        // tell our children
//        if (preventRequestLayout) {
//            child.assignParent(this);
//        } else {
//            child.mParent = this;
//        }
        child.mParent = this;
        if (child.hasFocus()) {
            requestChildFocus(child, child.findFocus());
        }

        AttachInfo ai = mAttachInfo;
        if (ai != null && (mGroupFlags & FLAG_PREVENT_DISPATCH_ATTACHED_TO_WINDOW) == 0) {
            boolean lastKeepOn = ai.mKeepScreenOn;
            ai.mKeepScreenOn = false;
            child.dispatchAttachedToWindow(mAttachInfo, (mViewFlags&VISIBILITY_MASK));
            if (ai.mKeepScreenOn) {
                needGlobalAttributesUpdate(true);
            }
            ai.mKeepScreenOn = lastKeepOn;
        }

        if (child.isLayoutDirectionInherited()) {
            child.resetRtlProperties();
        }

        onViewAdded(child);

//        if ((child.mViewFlags & DUPLICATE_PARENT_STATE) == DUPLICATE_PARENT_STATE) {
//            mGroupFlags |= FLAG_NOTIFY_CHILDREN_ON_DRAWABLE_STATE_CHANGE;
//        }
//
//        if (child.hasTransientState()) {
//            childHasTransientStateChanged(child, true);
//        }
        
        
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void dispatchSetSelected(boolean selected) {
        final View[] children = mChildren;
        final int count = mChildrenCount;
        for (int i = 0; i < count; i++) {
            children[i].setSelected(selected);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispatchSetActivated(boolean activated) {
        final View[] children = mChildren;
        final int count = mChildrenCount;
        for (int i = 0; i < count; i++) {
            children[i].setActivated(activated);
        }
    }

    @Override
    protected void dispatchSetPressed(boolean pressed) {
        final View[] children = mChildren;
        final int count = mChildrenCount;
        for (int i = 0; i < count; i++) {
            final View child = children[i];
            // Children that are clickable on their own should not
            // show a pressed state when their parent view does.
            // Clearing a pressed state always propagates.
            if (!pressed || (!child.isClickable() && !child.isLongClickable())) {
                child.setPressed(pressed);
            }
        }
    }
    
	/**
	 * {@hide}
	 */
	@Override
	protected View findViewTraversal(int id) {
		if (id == mID) {
			return this;
		}

		final View[] where = mChildren;
		final int len = mChildrenCount;

		for (int i = 0; i < len; i++) {
			View v = where[i];
			v = v.findViewById(id);
			if (v != null) {
				return v;
			}
		}

		return null;
	}
	
	public boolean hasContainDescendant(View descendant){
		boolean ret = false;
		
		while(descendant != null && descendant != this){
			descendant = descendant.getParent();
		}
		
		if(descendant == this){
			ret = true;
		}
		
		return ret;
	}
	
    /**
     * Ask all of the children of this view to measure themselves, taking into
     * account both the MeasureSpec requirements for this view and its padding.
     * We skip children that are in the GONE state The heavy lifting is done in
     * getChildMeasureSpec.
     *
     * @param widthMeasureSpec The width requirements for this view
     * @param heightMeasureSpec The height requirements for this view
     */
    protected void measureChildren(int widthMeasureSpec, int heightMeasureSpec) {
        final int size = mChildrenCount;
        final View[] children = mChildren;
        for (int i = 0; i < size; ++i) {
            final View child = children[i];
            if ((child.mViewFlags & VISIBILITY_MASK) != GONE) {
                measureChild(child, widthMeasureSpec, heightMeasureSpec);
            }
        }
    }

    /**
     * Ask one of the children of this view to measure itself, taking into
     * account both the MeasureSpec requirements for this view and its padding.
     * The heavy lifting is done in getChildMeasureSpec.
     *
     * @param child The child to measure
     * @param parentWidthMeasureSpec The width requirements for this view
     * @param parentHeightMeasureSpec The height requirements for this view
     */
    protected void measureChild(View child, int parentWidthMeasureSpec,
            int parentHeightMeasureSpec) {
        final LayoutParams lp = child.getLayoutParams();

        final int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec,
                mPaddingLeft + mPaddingRight, lp.width);
        final int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec,
                mPaddingTop + mPaddingBottom, lp.height);

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }
    
    /**
     * Ask one of the children of this view to measure itself, taking into
     * account both the MeasureSpec requirements for this view and its padding
     * and margins. The child must have MarginLayoutParams The heavy lifting is
     * done in getChildMeasureSpec.
     *
     * @param child The child to measure
     * @param parentWidthMeasureSpec The width requirements for this view
     * @param widthUsed Extra space that has been used up by the parent
     *        horizontally (possibly by other children of the parent)
     * @param parentHeightMeasureSpec The height requirements for this view
     * @param heightUsed Extra space that has been used up by the parent
     *        vertically (possibly by other children of the parent)
     */
    protected void measureChildWithMargins(View child,
            int parentWidthMeasureSpec, int widthUsed,
            int parentHeightMeasureSpec, int heightUsed) {
        final MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();

        final int childWidthMeasureSpec = getChildMeasureSpec(parentWidthMeasureSpec,
                mPaddingLeft + mPaddingRight + lp.leftMargin + lp.rightMargin
                        + widthUsed, lp.width);
        final int childHeightMeasureSpec = getChildMeasureSpec(parentHeightMeasureSpec,
                mPaddingTop + mPaddingBottom + lp.topMargin + lp.bottomMargin
                        + heightUsed, lp.height);

        child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
    }
    
    /**
     * Does the hard part of measureChildren: figuring out the MeasureSpec to
     * pass to a particular child. This method figures out the right MeasureSpec
     * for one dimension (height or width) of one child view.
     *
     * The goal is to combine information from our MeasureSpec with the
     * LayoutParams of the child to get the best possible results. For example,
     * if the this view knows its size (because its MeasureSpec has a mode of
     * EXACTLY), and the child has indicated in its LayoutParams that it wants
     * to be the same size as the parent, the parent should ask the child to
     * layout given an exact size.
     *
     * @param spec The requirements for this view
     * @param padding The padding of this view for the current dimension and
     *        margins, if applicable
     * @param childDimension How big the child wants to be in the current
     *        dimension
     * @return a MeasureSpec integer for the child
     */
    public static int getChildMeasureSpec(int spec, int padding, int childDimension) {
        int specMode = MeasureSpec.getMode(spec);
        int specSize = MeasureSpec.getSize(spec);

        int size = Math.max(0, specSize - padding);

        int resultSize = 0;
        int resultMode = 0;

        switch (specMode) {
        // Parent has imposed an exact size on us
        case MeasureSpec.EXACTLY:
            if (childDimension >= 0) {
                resultSize = childDimension;
                resultMode = MeasureSpec.EXACTLY;
            } else if (childDimension == LayoutParams.MATCH_PARENT) {
                // Child wants to be our size. So be it.
                resultSize = size;
                resultMode = MeasureSpec.EXACTLY;
            } else if (childDimension == LayoutParams.WRAP_CONTENT) {
                // Child wants to determine its own size. It can't be
                // bigger than us.
                resultSize = size;
                resultMode = MeasureSpec.AT_MOST;
            }
            break;

        // Parent has imposed a maximum size on us
        case MeasureSpec.AT_MOST:
            if (childDimension >= 0) {
                // Child wants a specific size... so be it
                resultSize = childDimension;
                resultMode = MeasureSpec.EXACTLY;
            } else if (childDimension == LayoutParams.MATCH_PARENT) {
                // Child wants to be our size, but our size is not fixed.
                // Constrain child to not be bigger than us.
                resultSize = size;
                resultMode = MeasureSpec.AT_MOST;
            } else if (childDimension == LayoutParams.WRAP_CONTENT) {
                // Child wants to determine its own size. It can't be
                // bigger than us.
                resultSize = size;
                resultMode = MeasureSpec.AT_MOST;
            }
            break;

        // Parent asked to see how big we want to be
        case MeasureSpec.UNSPECIFIED:
            if (childDimension >= 0) {
                // Child wants a specific size... let him have it
                resultSize = childDimension;
                resultMode = MeasureSpec.EXACTLY;
            } else if (childDimension == LayoutParams.MATCH_PARENT) {
                // Child wants to be our size... find out how big it should
                // be
                resultSize = 0;
                resultMode = MeasureSpec.UNSPECIFIED;
            } else if (childDimension == LayoutParams.WRAP_CONTENT) {
                // Child wants to determine its own size.... find out how
                // big it should be
                resultSize = 0;
                resultMode = MeasureSpec.UNSPECIFIED;
            }
            break;
        }
        return MeasureSpec.makeMeasureSpec(resultSize, resultMode);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void layout(int l, int t, int r, int b) {
        if (!mSuppressLayout && (mTransition == null || !mTransition.isChangingLayout())) {
            if (mTransition != null) {
            	mTransition.layoutChange(this);
            }
            super.layout(l, t, r, b);
        } else {
            // record the fact that we noop'd it; request layout when transition finishes
            mLayoutCalledWhileSuppressed = true;
        }
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    protected abstract void onLayout(boolean changed,
            int l, int t, int r, int b);
    
    /**
     * Indicates whether the view group has the ability to animate its children
     * after the first layout.
     *
     * @return true if the children can be animated, false otherwise
     */
    protected boolean canAnimate() {
        return mLayoutAnimationController != null;
    }

    /**
     * Runs the layout animation. Calling this method triggers a relayout of
     * this view group.
     */
    public void startLayoutAnimation() {
        if (mLayoutAnimationController != null) {
            mGroupFlags |= FLAG_RUN_ANIMATION;
            requestLayout();
        }
    }

    /**
     * Schedules the layout animation to be played after the next layout pass
     * of this view group. This can be used to restart the layout animation
     * when the content of the view group changes or when the activity is
     * paused and resumed.
     */
    public void scheduleLayoutAnimation() {
        mGroupFlags |= FLAG_RUN_ANIMATION;
    }

    /**
     * Sets the layout animation controller used to animate the group's
     * children after the first layout.
     *
     * @param controller the animation controller
     */
    public void setLayoutAnimation(LayoutAnimationController controller) {
        mLayoutAnimationController = controller;
        if (mLayoutAnimationController != null) {
            mGroupFlags |= FLAG_RUN_ANIMATION;
        }
    }

    /**
     * Returns the layout animation controller used to animate the group's
     * children.
     *
     * @return the current animation controller
     */
    public LayoutAnimationController getLayoutAnimation() {
        return mLayoutAnimationController;
    }
    
    /**
     * Returns the basis of alignment during layout operations on this view group:
     * either {@link #CLIP_BOUNDS} or {@link #OPTICAL_BOUNDS}.
     *
     * @return the layout mode to use during layout operations
     *
     * @see #setLayoutMode(int)
     *
     * @hide
     */
    public int getLayoutMode() {
        return mLayoutMode;
    }

    /**
     * Sets the basis of alignment during the layout of this view group.
     * Valid values are either {@link #CLIP_BOUNDS} or {@link #OPTICAL_BOUNDS}.
     * <p>
     * The default is {@link #CLIP_BOUNDS}.
     *
     * @param layoutMode the layout mode to use during layout operations
     *
     * @see #getLayoutMode()
     *
     * @hide
     */
    public void setLayoutMode(int layoutMode) {
        if (mLayoutMode != layoutMode) {
            mLayoutMode = layoutMode;
            requestLayout();
        }
    }
    
    /**
     * Called when a view's visibility has changed. Notify the parent to take any appropriate
     * action.
     *
     * @param child The view whose visibility has changed
     * @param oldVisibility The previous visibility value (GONE, INVISIBLE, or VISIBLE).
     * @param newVisibility The new visibility value (GONE, INVISIBLE, or VISIBLE).
     * @hide
     */
    protected void onChildVisibilityChanged(View child, int oldVisibility, int newVisibility) {
        if (mTransition != null) {
            if (newVisibility == VISIBLE) {
            	mTransition.showChild(this, child, oldVisibility);
            } else {
            	mTransition.hideChild(this, child, newVisibility);
                if (mTransitioningViews != null && mTransitioningViews.contains(child)) {
                    // Only track this on disappearing views - appearing views are already visible
                    // and don't need special handling during drawChild()
                    if (mVisibilityChangingChildren == null) {
                        mVisibilityChangingChildren = new ArrayList<View>();
                    }
                    mVisibilityChangingChildren.add(child);
                    addDisappearingView(child);
                }
            }
        }

        // in all cases, for drags
        /*if (mCurrentDrag != null) {
            if (newVisibility == VISIBLE) {
                notifyChildOfDrag(child);
            }
        }*/
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void dispatchVisibilityChanged(View changedView, int visibility) {
        super.dispatchVisibilityChanged(changedView, visibility);
        final int count = mChildrenCount;
        final View[] children = mChildren;
        for (int i = 0; i < count; i++) {
            children[i].dispatchVisibilityChanged(changedView, visibility);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void dispatchWindowVisibilityChanged(int visibility) {
        super.dispatchWindowVisibilityChanged(visibility);
        final int count = mChildrenCount;
        final View[] children = mChildren;
        for (int i = 0; i < count; i++) {
            children[i].dispatchWindowVisibilityChanged(visibility);
        }
    }
    
    /**
     * @hide
     */
    protected void onDebugDraw(GLCanvas canvas) {
/*        // Draw optical bounds
        if (getLayoutMode() == OPTICAL_BOUNDS) {
            for (int i = 0; i < getChildCount(); i++) {
                GLView c = getChildAt(i);
                Insets insets = c.getOpticalInsets();
                drawRect(canvas,
                        c.getLeft() + insets.left,
                        c.getTop() + insets.top,
                        c.getRight() - insets.right,
                        c.getBottom() - insets.bottom, Color.RED);
            }
        }

        // Draw margins
        onDebugDrawMargins(canvas);

        // Draw bounds
        for (int i = 0; i < getChildCount(); i++) {
        	GLView c = getChildAt(i);
            drawRect(canvas, c.getLeft(), c.getTop(), c.getRight(), c.getBottom(), Color.BLUE);
        }*/
    }
    
    /**
     * @hide
     */
    protected void onDebugDrawMargins(GLCanvas canvas) {
        for (int i = 0; i < getChildCount(); i++) {
            View c = getChildAt(i);
            c.getLayoutParams().onDebugDraw(c, canvas);
        }
    }
    
    /**
     * Return true if the pressed state should be delayed for children or descendants of this
     * ViewGroup. Generally, this should be done for containers that can scroll, such as a List.
     * This prevents the pressed state from appearing when the user is actually trying to scroll
     * the content.
     *
     * The default implementation returns true for compatibility reasons. Subclasses that do
     * not scroll should generally override this method and return false.
     */
    public boolean shouldDelayChildPressedState() {
        return true;
    }
    
    /**
     * @inheritDoc
     */
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        return false;
    }

    /**
     * @inheritDoc
     */
    public void onNestedScrollAccepted(View child, View target, int axes) {
        mNestedScrollAxes = axes;
    }

    /**
     * @inheritDoc
     *
     * <p>The default implementation of onStopNestedScroll calls
     * {@link #stopNestedScroll()} to halt any recursive nested scrolling in progress.</p>
     */
    public void onStopNestedScroll(View child) {
        // Stop any recursive nested scrolling.
        stopNestedScroll();
    }

    /**
     * @inheritDoc
     */
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed,
            int dxUnconsumed, int dyUnconsumed) {
        // Do nothing
    }

    /**
     * @inheritDoc
     */
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        // Do nothing
    }

    /**
     * @inheritDoc
     */
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        return false;
    }

    /**
     * @inheritDoc
     */
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        return false;
    }

    /**
     * Return the current axes of nested scrolling for this ViewGroup.
     *
     * <p>A ViewGroup returning something other than {@link #SCROLL_AXIS_NONE} is currently
     * acting as a nested scrolling parent for one or more descendant views in the hierarchy.</p>
     *
     * @return Flags indicating the current axes of nested scrolling
     * @see #SCROLL_AXIS_HORIZONTAL
     * @see #SCROLL_AXIS_VERTICAL
     * @see #SCROLL_AXIS_NONE
     */
    public int getNestedScrollAxes() {
        return mNestedScrollAxes;
    }
    
    /** @hide */
    protected void onSetLayoutParams(View child, LayoutParams layoutParams) {}
    
	public void requestChildFocus(View child, View focused) {
        if (getDescendantFocusability() == FOCUS_BLOCK_DESCENDANTS) {
            return;
        }
        
		// Unfocus us, if necessary
		super.unFocus();

		// We had a previous notion of who had focus. Clear it.
		if (mFocused != child) {
			if (mFocused != null) {
				mFocused.unFocus();
			}
			mFocused = child;
		}
		if (mParent != null) {
			mParent.requestChildFocus(this, focused);
		}
	}
	
	/**
     * {@inheritDoc}
     */
	public void focusableViewAvailable(View v) {
        if (mParent != null
                // shortcut: don't report a new focusable view if we block our descendants from
                // getting focus
                && (getDescendantFocusability() != FOCUS_BLOCK_DESCENDANTS)
                // shortcut: don't report a new focusable view if we already are focused
                // (and we don't prefer our descendants)
                //
                // note: knowing that mFocused is non-null is not a good enough reason
                // to break the traversal since in that case we'd actually have to find
                // the focused view and make sure it wasn't FOCUS_AFTER_DESCENDANTS and
                // an ancestor of v; this will get checked for at ViewAncestor
                && !(isFocused() && getDescendantFocusability() != FOCUS_AFTER_DESCENDANTS)) {
            mParent.focusableViewAvailable(v);
        }
    }
	
    @Override
   void unFocus() {
        if (mFocused == null) {
            super.unFocus();
        } else {
            mFocused.unFocus();
            mFocused = null;
        }
    }
    
    protected void onViewAdded(View child) {
    }

    protected void onViewRemoved(View child) {
    }
    
    private void addInArray(View child, int index) {
    	View[] children = mChildren;
        final int count = mChildrenCount;
        final int size = children.length;
        if (index == count) {
            if (size == count) {
                mChildren = new View[size + ARRAY_CAPACITY_INCREMENT];
                System.arraycopy(children, 0, mChildren, 0, size);
                children = mChildren;
            }
            children[mChildrenCount++] = child;
        } else if (index < count) {
            if (size == count) {
                mChildren = new View[size + ARRAY_CAPACITY_INCREMENT];
                System.arraycopy(children, 0, mChildren, 0, index);
                System.arraycopy(children, index, mChildren, index + 1, count - index);
                children = mChildren;
            } else {
                System.arraycopy(children, index, children, index + 1, count - index);
            }
            children[index] = child;
            mChildrenCount++;
            if (mLastTouchDownIndex >= index) {
                mLastTouchDownIndex++;
            }
        } else {
            throw new IndexOutOfBoundsException("index=" + index + " count=" + count);
        }
    }
    
    /**
     * Offset a rectangle that is in a descendant's coordinate
     * space into our coordinate space.
     * @param descendant A descendant of this view
     * @param rect A rectangle defined in descendant's coordinate space.
     */
    public final void offsetDescendantRectToMyCoords(View descendant, Rect rect) {
        offsetRectBetweenParentAndChild(descendant, rect, true, false);
    }
    
    /**
     * Offset a rectangle that is in our coordinate space into an ancestor's
     * coordinate space.
     * @param descendant A descendant of this view
     * @param rect A rectangle defined in descendant's coordinate space.
     */
    public final void offsetRectIntoDescendantCoords(View descendant, Rect rect) {
        offsetRectBetweenParentAndChild(descendant, rect, false, false);
    }
    
    /**
     * Helper method that offsets a rect either from parent to descendant or
     * descendant to parent.
     * descendant 后裔
     */
    void offsetRectBetweenParentAndChild(View descendant, Rect rect,
            boolean offsetFromChildToParent, boolean clipToBounds) {

        // already in the same coord system :)
        if (descendant == this) {
            return;
        }

        ViewGroup theParent = descendant.mParent;

        // search and offset up to the parent
        while ((theParent != null)
                && (theParent instanceof View)
                && (theParent != this)) {

            if (offsetFromChildToParent) {
                rect.offset(descendant.mLeft - descendant.mScrollX,
                        descendant.mTop - descendant.mScrollY);
                if (clipToBounds) {
                    View p = (View) theParent;
                    rect.intersect(0, 0, p.mRight - p.mLeft, p.mBottom - p.mTop);
                }
            } else {
                if (clipToBounds) {
                	View p = (View) theParent;
                    rect.intersect(0, 0, p.mRight - p.mLeft, p.mBottom - p.mTop);
                }
                rect.offset(descendant.mScrollX - descendant.mLeft,
                        descendant.mScrollY - descendant.mTop);
            }

            descendant = (View) theParent;
            theParent = descendant.mParent;
        }

        // now that we are up to this view, need to offset one more time
        // to get into our coordinate space
        if (theParent == this) {
            if (offsetFromChildToParent) {
                rect.offset(descendant.mLeft - descendant.mScrollX,
                        descendant.mTop - descendant.mScrollY);
            } else {
                rect.offset(descendant.mScrollX - descendant.mLeft,
                        descendant.mScrollY - descendant.mTop);
            }
        } else {
            throw new IllegalArgumentException("parameter must be a descendant of this view");
        }
    }
    
    /**
     * Offset the vertical location of all children of this view by the specified number of pixels.
     *
     * @param offset the number of pixels to offset
     *
     * @hide
     */
    public void offsetChildrenTopAndBottom(int offset) {
        final int count = mChildrenCount;
        final View[] children = mChildren;

        for (int i = 0; i < count; i++) {
            final View v = children[i];
            v.offsetTopAndBottom(offset);
        }
        invalidate();
    }
    
    // This method also sets the child's mParent to null
    private void removeFromArray(int index) {
        final View[] children = mChildren;
        if (!(mTransitioningViews != null && mTransitioningViews.contains(children[index]))) {
            children[index].mParent = null;
        }
        
        final int count = mChildrenCount;
        if (index == count - 1) {
            children[--mChildrenCount] = null;
        } else if (index >= 0 && index < count) {
            System.arraycopy(children, index + 1, children, index, count - index - 1);
            children[--mChildrenCount] = null;
        } else {
            throw new IndexOutOfBoundsException();
        }
        if (mLastTouchDownIndex == index) {
            mLastTouchDownTime = 0;
            mLastTouchDownIndex = -1;
        } else if (mLastTouchDownIndex > index) {
            mLastTouchDownIndex--;
        }
    }

    // This method also sets the children's mParent to null
    private void removeFromArray(int start, int count) {
        final View[] children = mChildren;
        final int childrenCount = mChildrenCount;

        start = Math.max(0, start);
        final int end = Math.min(childrenCount, start + count);

        if (start == end) {
            return;
        }

        if (end == childrenCount) {
            for (int i = start; i < end; i++) {
                children[i].mParent = null;
                children[i] = null;
            }
        } else {
            for (int i = start; i < end; i++) {
                children[i].mParent = null;
            }

            // Since we're looping above, we might as well do the copy, but is arraycopy()
            // faster than the extra 2 bounds checks we would do in the loop?
            System.arraycopy(children, end, children, start, childrenCount - end);

            for (int i = childrenCount - (end - start); i < childrenCount; i++) {
                children[i] = null;
            }
        }

        mChildrenCount -= (end - start);
    }
    
    public  int getChildCount(){
    	return mChildrenCount;
    }
    
    public View getChildAt(int index) {
        if (index < 0 || index >= mChildrenCount) {
            return null;
        }
        return mChildren[index];
    }
   
   public void removeViewAt(int index){
	   if(index >= 0 && index < mChildrenCount){
		   removeViewInternal(index, mChildren[index]);		   
	   }
   }
   
   public void removeView(View view){
	   removeViewInternal(view);
   }
   
   private void removeViewInternal(View view) {
       final int index = indexOfChild(view);
       if (index >= 0) {
           removeViewInternal(index, view);
       }
   }
   
   private void removeViewInternal(int index, View child) {

	   //remove animation
	   if (mTransition != null) {
		   mTransition.removeChild(this, child);
       }

       boolean clearChildFocus = false;
       if (child == mFocused) {
           child.unFocus();
           clearChildFocus = true;
       }

       cancelTouchTarget(child);
       
       if (child.getAnimation() != null ||
               (mTransitioningViews != null && mTransitioningViews.contains(child))) {
           addDisappearingView(child);
       } else if (child.mAttachInfo != null) {
    	   child.dispatchDetachedFromWindow();
       }
       
       needGlobalAttributesUpdate(false);

       removeFromArray(index);

       if (clearChildFocus) {
           clearChildFocus(child);
           if (!rootViewRequestFocus()) {
               notifyGlobalFocusCleared(this);
           }
       }
       
       onViewRemoved(child);
   }
   
   /**
    * Cleanup a view when its animation is done. This may mean removing it from
    * the list of disappearing views.
    *
    * @param view The view whose animation has finished
    * @param animation The animation, cannot be null
    */
   void finishAnimatingView(final View view, Animation animation) {
       final ArrayList<View> disappearingChildren = mDisappearingChildren;
       if (disappearingChildren != null) {
           if (disappearingChildren.contains(view)) {
               disappearingChildren.remove(view);
               
               onViewRemoved(view);
               view.clearAnimation();
               /*if (view.mAttachInfo != null) {
                   view.dispatchDetachedFromWindow();
               }

               view.clearAnimation();
               mGroupFlags |= FLAG_INVALIDATE_REQUIRED;*/
           }
       }

       if (animation != null && !animation.getFillAfter()) {
           view.clearAnimation();
       }

       if ((view.mPrivateFlags & PFLAG_ANIMATION_STARTED) == PFLAG_ANIMATION_STARTED) {
           view.onAnimationEnd();
           // Should be performed by onAnimationEnd() but this avoid an infinite loop,
           // so we'd rather be safe than sorry
           view.mPrivateFlags &= ~PFLAG_ANIMATION_STARTED;
           /*// Draw one more frame after the animation is done
           mGroupFlags |= FLAG_INVALIDATE_REQUIRED;*/
       }
   }

   /**
    * Utility function called by View during invalidation to determine whether a view that
    * is invisible or gone should still be invalidated because it is being transitioned (and
    * therefore still needs to be drawn).
    */
   boolean isViewTransitioning(View view) {
       return (mTransitioningViews != null && mTransitioningViews.contains(view));
   }

   /**
    * This method tells the ViewGroup that the given View object, which should have this
    * ViewGroup as its parent,
    * should be kept around  (re-displayed when the ViewGroup draws its children) even if it
    * is removed from its parent. This allows animations, such as those used by
    * {@link android.app.Fragment} and {@link android.animation.LayoutTransition} to animate
    * the removal of views. A call to this method should always be accompanied by a later call
    * to {@link #endViewTransition(View)}, such as after an animation on the View has finished,
    * so that the View finally gets removed.
    *
    * @param view The View object to be kept visible even if it gets removed from its parent.
    */
   public void startViewTransition(View view) {
       if (view.mParent == this) {
           if (mTransitioningViews == null) {
               mTransitioningViews = new ArrayList<View>();
           }
           mTransitioningViews.add(view);
       }
   }

   /**
    * This method should always be called following an earlier call to
    * {@link #startViewTransition(View)}. The given View is finally removed from its parent
    * and will no longer be displayed. Note that this method does not perform the functionality
    * of removing a view from its parent; it just discontinues the display of a View that
    * has previously been removed.
    *
    * @return view The View object that has been removed but is being kept around in the visible
    * hierarchy by an earlier call to {@link #startViewTransition(View)}.
    */
   public void endViewTransition(View view) {
       if (mTransitioningViews != null) {
           mTransitioningViews.remove(view);
           final ArrayList<View> disappearingChildren = mDisappearingChildren;
           if (disappearingChildren != null && disappearingChildren.contains(view)) {
               disappearingChildren.remove(view);
               if (mVisibilityChangingChildren != null &&
                       mVisibilityChangingChildren.contains(view)) {
                   mVisibilityChangingChildren.remove(view);
               } else {
            	   onViewRemoved(view);
                   if (view.mParent != null) {
                       view.mParent = null;
                   }
               }
               invalidate();
           }
       }
   }
   
   /**
    * Tells this ViewGroup to suppress all layout() calls until layout
    * suppression is disabled with a later call to suppressLayout(false).
    * When layout suppression is disabled, a requestLayout() call is sent
    * if layout() was attempted while layout was being suppressed.
    *
    * @hide
    */
   public void suppressLayout(boolean suppress) {
       mSuppressLayout = suppress;
       if (!suppress) {
           if (mLayoutCalledWhileSuppressed) {
               requestLayout();
               mLayoutCalledWhileSuppressed = false;
           }
       }
   }

   /**
    * Returns whether layout calls on this container are currently being
    * suppressed, due to an earlier call to {@link #suppressLayout(boolean)}.
    *
    * @return true if layout calls are currently suppressed, false otherwise.
    *
    * @hide
    */
   public boolean isLayoutSuppressed() {
       return mSuppressLayout;
   }

   private LayoutTransition.TransitionListener mLayoutTransitionListener =
           new LayoutTransition.TransitionListener() {
       @Override
       public void startTransition(LayoutTransition transition, ViewGroup container,
               View view, int transitionType) {
           // We only care about disappearing items, since we need special logic to keep
           // those items visible after they've been 'removed'
           if (transitionType == LayoutTransition.DISAPPEARING) {
               startViewTransition(view);
           }
       }

       @Override
       public void endTransition(LayoutTransition transition, ViewGroup container,
               View view, int transitionType) {
    	   if (mLayoutCalledWhileSuppressed && !transition.isChangingLayout()) {
               requestLayout();
               mLayoutCalledWhileSuppressed = false;
           }
           if (transitionType == LayoutTransition.DISAPPEARING && mTransitioningViews != null) {
               endViewTransition(view);
           }
       }
   };
   
   /**
    * Sets the LayoutTransition object for this ViewGroup. If the LayoutTransition object is
    * not null, changes in layout which occur because of children being added to or removed from
    * the ViewGroup will be animated according to the animations defined in that LayoutTransition
    * object. By default, the transition object is null (so layout changes are not animated).
    *
    * <p>Replacing a non-null transition will cause that previous transition to be
    * canceled, if it is currently running, to restore this container to
    * its correct post-transition state.</p>
    *
    * @param transition The LayoutTransition object that will animated changes in layout. A value
    * of <code>null</code> means no transition will run on layout changes.
    * @attr ref android.R.styleable#ViewGroup_animateLayoutChanges
    */
   public void setLayoutTransition(LayoutTransition transition) {
       if (mTransition != null) {
           LayoutTransition previousTransition = mTransition;
           previousTransition.cancel();
           previousTransition.removeTransitionListener(mLayoutTransitionListener);
       }
       mTransition = transition;
       if (mTransition != null) {
    	   mTransition.addTransitionListener(mLayoutTransitionListener);
       }
   }
   
   /**
    * Gets the LayoutTransition object for this ViewGroup. If the LayoutTransition object is
    * not null, changes in layout which occur because of children being added to or removed from
    * the ViewGroup will be animated according to the animations defined in that LayoutTransition
    * object. By default, the transition object is null (so layout changes are not animated).
    *
    * @return LayoutTranstion The LayoutTransition object that will animated changes in layout.
    * A value of <code>null</code> means no transition will run on layout changes.
    */
   public LayoutTransition getLayoutTransition() {
       return mTransition;
   }
   
   public void removeAllViewsInLayout() {
       final int count = mChildrenCount;
       if (count <= 0) {
           return;
       }

       final View[] children = mChildren;
       mChildrenCount = 0;

       final View focused = mFocused;
       final boolean detach = mAttachInfo != null;
       View clearChildFocus = null;
       
       needGlobalAttributesUpdate(false);

       for (int i = count - 1; i >= 0; i--) {
           final View view = children[i];

           if (mTransition != null) {
        	   mTransition.removeChild(this, view);
           }

           if (view == focused) {
               view.unFocus();
               clearChildFocus = view;
           }

           cancelTouchTarget(view);

           if (view.getAnimation() != null ||
                   (mTransitioningViews != null && mTransitioningViews.contains(view))) {
               addDisappearingView(view);
           } else if (detach) {
              view.dispatchDetachedFromWindow();
           }
           
           onViewRemoved(view);

           view.mParent = null;
           children[i] = null;
       }

       if (clearChildFocus != null) {
           clearChildFocus(clearChildFocus);
           if (!rootViewRequestFocus()) {
               notifyGlobalFocusCleared(focused);
           }
       }
   }
   
   /**
    * Finishes the removal of a detached view. This method will dispatch the detached from
    * window event and notify the hierarchy change listener.
    * <p>
    * This method is intended to be lightweight and makes no assumptions about whether the
    * parent or child should be redrawn. Proper use of this method will include also making
    * any appropriate {@link #requestLayout()} or {@link #invalidate()} calls.
    * For example, callers can {@link #post(Runnable) post} a {@link Runnable}
    * which performs a {@link #requestLayout()} on the next frame, after all detach/remove
    * calls are finished, causing layout to be run prior to redrawing the view hierarchy.
    *
    * @param child the child to be definitely removed from the view hierarchy
    * @param animate if true and the view has an animation, the view is placed in the
    *                disappearing views list, otherwise, it is detached from the window
    *
    * @see #attachViewToParent(View, int, android.view.ViewGroup.LayoutParams)
    * @see #detachAllViewsFromParent()
    * @see #detachViewFromParent(View)
    * @see #detachViewFromParent(int)
    */
   protected void removeDetachedView(View child, boolean animate) {
       if (mTransition != null) {
           mTransition.removeChild(this, child);
       }

       if (child == mFocused) {
           child.clearFocus();
       }

//       child.clearAccessibilityFocus();

       cancelTouchTarget(child);
//       cancelHoverTarget(child);

       if ((animate && child.getAnimation() != null) ||
               (mTransitioningViews != null && mTransitioningViews.contains(child))) {
           addDisappearingView(child);
       } else if (child.mAttachInfo != null) {
           child.dispatchDetachedFromWindow();
       }

       /*if (child.hasTransientState()) {
           childHasTransientStateChanged(child, false);
       }*/

       onViewRemoved(child);
   }
   
   /**
    * Attaches a view to this view group. Attaching a view assigns this group as the parent,
    * sets the layout parameters and puts the view in the list of children so that
    * it can be retrieved by calling {@link #getChildAt(int)}.
    * <p>
    * This method is intended to be lightweight and makes no assumptions about whether the
    * parent or child should be redrawn. Proper use of this method will include also making
    * any appropriate {@link #requestLayout()} or {@link #invalidate()} calls.
    * For example, callers can {@link #post(Runnable) post} a {@link Runnable}
    * which performs a {@link #requestLayout()} on the next frame, after all detach/attach
    * calls are finished, causing layout to be run prior to redrawing the view hierarchy.
    * <p>
    * This method should be called only for views which were detached from their parent.
    *
    * @param child the child to attach
    * @param index the index at which the child should be attached
    * @param params the layout parameters of the child
    *
    * @see #removeDetachedView(View, boolean)
    * @see #detachAllViewsFromParent()
    * @see #detachViewFromParent(View)
    * @see #detachViewFromParent(int)
    */
   protected void attachViewToParent(View child, int index, LayoutParams params) {
       child.mLayoutParams = params;

       if (index < 0) {
           index = mChildrenCount;
       }

       addInArray(child, index);

       child.mParent = this;
       child.mPrivateFlags = (child.mPrivateFlags
                       & ~PFLAG_DRAWING_CACHE_VALID)
                | PFLAG_INVALIDATED;
       this.mPrivateFlags |= PFLAG_INVALIDATED;

       if (child.hasFocus()) {
           requestChildFocus(child, child.findFocus());
       }
   }

   /**
    * Detaches a view from its parent. Detaching a view should be followed
    * either by a call to
    * {@link #attachViewToParent(View, int, android.view.ViewGroup.LayoutParams)}
    * or a call to {@link #removeDetachedView(View, boolean)}. Detachment should only be
    * temporary; reattachment or removal should happen within the same drawing cycle as
    * detachment. When a view is detached, its parent is null and cannot be retrieved by a
    * call to {@link #getChildAt(int)}.
    *
    * @param child the child to detach
    *
    * @see #detachViewFromParent(int)
    * @see #detachViewsFromParent(int, int)
    * @see #detachAllViewsFromParent()
    * @see #attachViewToParent(View, int, android.view.ViewGroup.LayoutParams)
    * @see #removeDetachedView(View, boolean)
    */
   protected void detachViewFromParent(View child) {
       removeFromArray(indexOfChild(child));
   }

   /**
    * Detaches a view from its parent. Detaching a view should be followed
    * either by a call to
    * {@link #attachViewToParent(View, int, android.view.ViewGroup.LayoutParams)}
    * or a call to {@link #removeDetachedView(View, boolean)}. Detachment should only be
    * temporary; reattachment or removal should happen within the same drawing cycle as
    * detachment. When a view is detached, its parent is null and cannot be retrieved by a
    * call to {@link #getChildAt(int)}.
    *
    * @param index the index of the child to detach
    *
    * @see #detachViewFromParent(View)
    * @see #detachAllViewsFromParent()
    * @see #detachViewsFromParent(int, int)
    * @see #attachViewToParent(View, int, android.view.ViewGroup.LayoutParams)
    * @see #removeDetachedView(View, boolean)
    */
   protected void detachViewFromParent(int index) {
       removeFromArray(index);
   }

   /**
    * Detaches a range of views from their parents. Detaching a view should be followed
    * either by a call to
    * {@link #attachViewToParent(View, int, android.view.ViewGroup.LayoutParams)}
    * or a call to {@link #removeDetachedView(View, boolean)}. Detachment should only be
    * temporary; reattachment or removal should happen within the same drawing cycle as
    * detachment. When a view is detached, its parent is null and cannot be retrieved by a
    * call to {@link #getChildAt(int)}.
    *
    * @param start the first index of the childrend range to detach
    * @param count the number of children to detach
    *
    * @see #detachViewFromParent(View)
    * @see #detachViewFromParent(int)
    * @see #detachAllViewsFromParent()
    * @see #attachViewToParent(View, int, android.view.ViewGroup.LayoutParams)
    * @see #removeDetachedView(View, boolean)
    */
   protected void detachViewsFromParent(int start, int count) {
       removeFromArray(start, count);
   }

   /**
    * Detaches all views from the parent. Detaching a view should be followed
    * either by a call to
    * {@link #attachViewToParent(View, int, android.view.ViewGroup.LayoutParams)}
    * or a call to {@link #removeDetachedView(View, boolean)}. Detachment should only be
    * temporary; reattachment or removal should happen within the same drawing cycle as
    * detachment. When a view is detached, its parent is null and cannot be retrieved by a
    * call to {@link #getChildAt(int)}.
    *
    * @see #detachViewFromParent(View)
    * @see #detachViewFromParent(int)
    * @see #detachViewsFromParent(int, int)
    * @see #attachViewToParent(View, int, android.view.ViewGroup.LayoutParams)
    * @see #removeDetachedView(View, boolean)
    */
   protected void detachAllViewsFromParent() {
       final int count = mChildrenCount;
       if (count <= 0) {
           return;
       }

       final View[] children = mChildren;
       mChildrenCount = 0;

       for (int i = count - 1; i >= 0; i--) {
           children[i].mParent = null;
           children[i] = null;
       }
   }
   
   /**
    * Add a view which is removed from mChildren but still needs animation
    *
    * @param v View to add
    */
   private void addDisappearingView(View v) {
       ArrayList<View> disappearingChildren = mDisappearingChildren;

       if (disappearingChildren == null) {
           disappearingChildren = mDisappearingChildren = new ArrayList<View>();
       }

       disappearingChildren.add(v);
   }
   
   private void renderDisappearingChildren(GLCanvas canvas){
	   // Draw any disappearing views that have animations
	   if (mDisappearingChildren != null) {
		   final ArrayList<View> disappearingChildren = mDisappearingChildren;
		   final int disappearingCount = disappearingChildren.size() - 1;
		   // Go backwards -- we may delete as animations finish
		   for (int i = disappearingCount; i >= 0; i--) {
			   //draw disappearing gl views
			   drawChildInternal(canvas, disappearingChildren.get(i), 0);
		   }
	   }
   }
   
   public void removeAllViews(){
	   removeAllViewsInLayout();
   }
    
    @Override
    void dispatchAttachedToWindow(AttachInfo info, int visibility) {
    	mGroupFlags |= FLAG_PREVENT_DISPATCH_ATTACHED_TO_WINDOW;
        super.dispatchAttachedToWindow(info, visibility);
        mGroupFlags &= ~FLAG_PREVENT_DISPATCH_ATTACHED_TO_WINDOW;
    	
    	for (int i = 0; i < mChildrenCount; ++i) {
    		final View child = mChildren[i];
    		child.dispatchAttachedToWindow(info,
                    visibility | (child.mViewFlags & VISIBILITY_MASK));
        }
    }
    
    @Override
    void dispatchDetachedFromWindow() {
    	// If we still have a touch target, we are still in the process of
        // dispatching motion events to a child; we need to get rid of that
        // child to avoid dispatching events to it after the window is torn
        // down. To make sure we keep the child in a consistent state, we
        // first send it an ACTION_CANCEL motion event.
        cancelAndClearTouchTargets(null);
        
        // In case view is detached while transition is running
        mLayoutCalledWhileSuppressed = false;
    	
    	for (int i = 0; i < mChildrenCount; ++i) {
        	mChildren[i].dispatchDetachedFromWindow();
        }
    	super.dispatchDetachedFromWindow();
    }
    
    Rect mTmpClipRect = new Rect();
    /**
     * Private draw method.
     */
    @Override
    void pdraw(GLCanvas canvas) {
    	boolean clipChildren = getClipChildren();
    	if (clipChildren) {
    		canvas.save(GLCanvas.SAVE_FLAG_CLIP);
    		mTmpClipRect.set(0, 0, getWidth(), getHeight());
    		mTmpClipRect.offset(getScrollX(), getScrollY());
    		canvas.clipRect(mTmpClipRect);
    	}
    	super.pdraw(canvas);
    	if (clipChildren) {
			canvas.restore();
		}
    }
    
    @Override
	protected void dispatchDraw(GLCanvas canvas) {
    	final int childrenCount = mChildrenCount;
    	final View[] children = mChildren;
    	
    	// Build Z-Sort children.
		
        final long drawingTime = getDrawingTime();
		// Only use the preordered list if not HW accelerated, since the HW pipeline will do the
        // draw reordering internally
        final ArrayList<View> preorderedList = /*usingRenderNodeProperties
                ? null : */buildOrderedChildList();
        final boolean customOrder = preorderedList == null
                && isChildrenDrawingOrderEnabled();
        for (int i = 0; i < childrenCount; i++) {
            int childIndex = customOrder ? getChildDrawingOrder(childrenCount, i) : i;
            final View child = (preorderedList == null)
                    ? children[childIndex] : preorderedList.get(childIndex);
            if ((child.mViewFlags & VISIBILITY_MASK) == VISIBLE || child.getAnimation() != null) {
                drawChild(canvas, child, drawingTime);
            }
        }
        if (preorderedList != null) preorderedList.clear();
        
		renderDisappearingChildren(canvas);
		
	}
    
    /**
     * Returns the ViewGroupOverlay for this view group, creating it if it does
     * not yet exist. In addition to {@link ViewOverlay}'s support for drawables,
     * {@link ViewGroupOverlay} allows views to be added to the overlay. These
     * views, like overlay drawables, are visual-only; they do not receive input
     * events and should not be used as anything other than a temporary
     * representation of a view in a parent container, such as might be used
     * by an animation effect.
     *
     * <p>Note: Overlays do not currently work correctly with {@link
     * SurfaceView} or {@link TextureView}; contents in overlays for these
     * types of views may not display correctly.</p>
     *
     * @return The ViewGroupOverlay object for this view.
     * @see ViewGroupOverlay
     */
    @Override
    public ViewGroupOverlay getOverlay() {
        if (mOverlay == null) {
            mOverlay = new ViewGroupOverlay(mContext, this);
        }
        return (ViewGroupOverlay) mOverlay;
    }
    
    protected void drawChild(GLCanvas canvas, View component, long drawintTime) {
        if (component.getVisibility() != View.VISIBLE
                && component.getAnimation() == null) return;

    	drawChildInternal(canvas, component, drawintTime);
    }
    
    private void drawChildInternal(GLCanvas canvas, View child, long drawintTime) {
    	child.dispatchRender(canvas);
    }
    
    /**
     * Returns whether this group's children are clipped to their bounds before drawing.
     * The default value is true.
     * @see #setClipChildren(boolean)
     *
     * @return True if the group's children will be clipped to their bounds,
     * false otherwise.
     */
    public boolean getClipChildren() {
        return ((mGroupFlags & FLAG_CLIP_CHILDREN) != 0);
    }

    /**
     * By default, children are clipped to their bounds before drawing. This
     * allows view groups to override this behavior for animations, etc.
     *
     * @param clipChildren true to clip children to their bounds,
     *        false otherwise
     * @attr ref android.R.styleable#ViewGroup_clipChildren
     */
    public void setClipChildren(boolean clipChildren) {
        boolean previousValue = (mGroupFlags & FLAG_CLIP_CHILDREN) == FLAG_CLIP_CHILDREN;
        if (clipChildren != previousValue) {
            setBooleanFlag(FLAG_CLIP_CHILDREN, clipChildren);
            invalidate(true);
        }
    }
    
    /**
     * By default, children are clipped to the padding of the ViewGroup. This
     * allows view groups to override this behavior
     *
     * @param clipToPadding true to clip children to the padding of the
     *        group, false otherwise
     * @attr ref android.R.styleable#ViewGroup_clipToPadding
     */
    public void setClipToPadding(boolean clipToPadding) {
        if (hasBooleanFlag(FLAG_CLIP_TO_PADDING) != clipToPadding) {
            setBooleanFlag(FLAG_CLIP_TO_PADDING, clipToPadding);
            invalidate(true);
        }
    }

    /**
     * Check if this ViewGroup is configured to clip child views to its padding.
     *
     * @return true if this ViewGroup clips children to its padding, false otherwise
     *
     * @attr ref android.R.styleable#ViewGroup_clipToPadding
     */
    public boolean getClipToPadding() {
        return hasBooleanFlag(FLAG_CLIP_TO_PADDING);
    }
    
    @Override
    public void outOfRenderRect() {
    	final int childCount = mChildrenCount;
    	for(int i = 0; i < childCount; i++){
    		mChildren[i].outOfRenderRect();
    	}
    }
    
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
		if (!isEnabled())
			return false;
		
		boolean handled = false;
		
		final int action = ev.getAction();
        final int actionMasked = action & MotionEvent.ACTION_MASK;

        // Handle an initial down.
        if (actionMasked == MotionEvent.ACTION_DOWN) {
            // Throw away all previous state when starting a new touch gesture.
            // The framework may have dropped the up or cancel event for the previous gesture
            // due to an app switch, ANR, or some other state change.
            cancelAndClearTouchTargets(ev);
            resetTouchState();
        }
        
        // Check for interception.
        final boolean intercepted;
        if (actionMasked == MotionEvent.ACTION_DOWN
                || mFirstTouchTarget != null) {
            final boolean disallowIntercept = (mGroupFlags & FLAG_DISALLOW_INTERCEPT) != 0;
            if (!disallowIntercept) {
                intercepted = onInterceptTouchEvent(ev);
                ev.setAction(action); // restore action in case it was changed
            } else {
                intercepted = false;
            }
        } else {
            // There are no touch targets and this action is not an initial down
            // so this view group continues to intercept touches.
            intercepted = true;
        }

        // Check for cancelation.
        final boolean canceled = resetCancelNextUpFlag(this)
                || actionMasked == MotionEvent.ACTION_CANCEL;

        // Update list of touch targets for pointer down, if needed.
        final boolean split = (mGroupFlags & FLAG_SPLIT_MOTION_EVENTS) != 0;
        TouchTarget newTouchTarget = null;
        boolean alreadyDispatchedToNewTouchTarget = false;
        if (!canceled && !intercepted) {
            if (actionMasked == MotionEvent.ACTION_DOWN
                    || (split && actionMasked == MotionEvent.ACTION_POINTER_DOWN)
                    || actionMasked == MotionEvent.ACTION_HOVER_MOVE) {
                final int actionIndex = ev.getActionIndex(); // always 0 for down
                final int idBitsToAssign = split ? 1 << ev.getPointerId(actionIndex)
                        : TouchTarget.ALL_POINTER_IDS;

                // Clean up earlier touch targets for this pointer id in case they
                // have become out of sync.
                removePointersFromTouchTargets(idBitsToAssign);

                final int childrenCount = mChildrenCount;
                if (newTouchTarget == null && childrenCount != 0) {
                    final float x = ev.getX(actionIndex);
                    final float y = ev.getY(actionIndex);
                    // Find a child that can receive the event.
                    // Scan children from front to back.
                    final ArrayList<View> preorderedList = buildOrderedChildList();
                    final boolean customOrder = preorderedList == null
                            && isChildrenDrawingOrderEnabled();
                    final View[] children = mChildren;
                    for (int i = childrenCount - 1; i >= 0; i--) {
                        final int childIndex = customOrder
                                ? getChildDrawingOrder(childrenCount, i) : i;
                        final View child = (preorderedList == null)
                                ? children[childIndex] : preorderedList.get(childIndex);
                        if (!canViewReceivePointerEvents(child)
                                || !isTransformedTouchPointInView(x, y, child, null)) {
                            continue;
                        }

                        newTouchTarget = getTouchTarget(child);
                        if (newTouchTarget != null) {
                            // Child is already receiving touch within its bounds.
                            // Give it the new pointer in addition to the ones it is handling.
                            newTouchTarget.pointerIdBits |= idBitsToAssign;
                            break;
                        }

                        resetCancelNextUpFlag(child);
                        if (dispatchTransformedTouchEvent(ev, false, child, idBitsToAssign)) {
                            // Child wants to receive touch within its bounds.
                            mLastTouchDownTime = ev.getDownTime();
                            if (preorderedList != null) {
                                // childIndex points into presorted list, find original index
                                for (int j = 0; j < childrenCount; j++) {
                                    if (children[childIndex] == mChildren[j]) {
                                        mLastTouchDownIndex = j;
                                        break;
                                    }
                                }
                            } else {
                                mLastTouchDownIndex = childIndex;
                            }
                            mLastTouchDownX = ev.getX();
                            mLastTouchDownY = ev.getY();
                            newTouchTarget = addTouchTarget(child, idBitsToAssign);
                            alreadyDispatchedToNewTouchTarget = true;
                            break;
                        }
                    }
                    if (preorderedList != null) preorderedList.clear();
                }

                if (newTouchTarget == null && mFirstTouchTarget != null) {
                    // Did not find a child to receive the event.
                    // Assign the pointer to the least recently added target.
                    newTouchTarget = mFirstTouchTarget;
                    while (newTouchTarget.next != null) {
                        newTouchTarget = newTouchTarget.next;
                    }
                    newTouchTarget.pointerIdBits |= idBitsToAssign;
                }
            }
        }
        
     // Dispatch to touch targets.
        if (mFirstTouchTarget == null) {
            // No touch targets so treat this as an ordinary view.
            handled = dispatchTransformedTouchEvent(ev, canceled, null,
                    TouchTarget.ALL_POINTER_IDS);
        } else {
            // Dispatch to touch targets, excluding the new touch target if we already
            // dispatched to it.  Cancel touch targets if necessary.
            TouchTarget predecessor = null;
            TouchTarget target = mFirstTouchTarget;
            while (target != null) {
                final TouchTarget next = target.next;
                if (alreadyDispatchedToNewTouchTarget && target == newTouchTarget) {
                    handled = true;
                } else {
                    final boolean cancelChild = resetCancelNextUpFlag(target.child)
                            || intercepted;
                    if (dispatchTransformedTouchEvent(ev, cancelChild,
                            target.child, target.pointerIdBits)) {
                        handled = true;
                    }
                    if (cancelChild) {
                        if (predecessor == null) {
                            mFirstTouchTarget = next;
                        } else {
                            predecessor.next = next;
                        }
                        target.recycle();
                        target = next;
                        continue;
                    }
                }
                predecessor = target;
                target = next;
            }
        }
        
     	// Update list of touch targets for pointer up or cancel, if needed.
        if (canceled
                || actionMasked == MotionEvent.ACTION_UP
                || actionMasked == MotionEvent.ACTION_HOVER_MOVE) {
            resetTouchState();
        } else if (split && actionMasked == MotionEvent.ACTION_POINTER_UP) {
            final int actionIndex = ev.getActionIndex();
            final int idBitsToRemove = 1 << ev.getPointerId(actionIndex);
            removePointersFromTouchTargets(idBitsToRemove);
        }
        
        return handled;
        
//		int x = (int) ev.getX();
//		int y = (int) ev.getY();
//		int action = ev.getAction();
//		if (action == MotionEvent.ACTION_DOWN) {
//			// in the reverse rendering order
//			int childCount = mChildrenCount;
//			for (int i = childCount - 1; i >= 0; --i) {
//				View component = mChildren[i];
//				if (component.getVisibility() != View.VISIBLE)
//					continue;
//				if (component.onTouchEvent(ev, x, y, component, true)) {
//					mMotionTarget = component;
//					return true;
//				}
//			}
//		}
//		return super.dispatchTouchEvent(ev);
    }
    
    /**
     * Enable or disable the splitting of MotionEvents to multiple children during touch event
     * dispatch. This behavior is enabled by default for applications that target an
     * SDK version of {@link Build.VERSION_CODES#HONEYCOMB} or newer.
     *
     * <p>When this option is enabled MotionEvents may be split and dispatched to different child
     * views depending on where each pointer initially went down. This allows for user interactions
     * such as scrolling two panes of content independently, chording of buttons, and performing
     * independent gestures on different pieces of content.
     *
     * @param split <code>true</code> to allow MotionEvents to be split and dispatched to multiple
     *              child views. <code>false</code> to only allow one child view to be the target of
     *              any MotionEvent received by this ViewGroup.
     * @attr ref android.R.styleable#ViewGroup_splitMotionEvents
     */
    public void setMotionEventSplittingEnabled(boolean split) {
        // TODO Applications really shouldn't change this setting mid-touch event,
        // but perhaps this should handle that case and send ACTION_CANCELs to any child views
        // with gestures in progress when this is changed.
        if (split) {
            mGroupFlags |= FLAG_SPLIT_MOTION_EVENTS;
        } else {
            mGroupFlags &= ~FLAG_SPLIT_MOTION_EVENTS;
        }
    }

    /**
     * Returns true if MotionEvents dispatched to this ViewGroup can be split to multiple children.
     * @return true if MotionEvents dispatched to this ViewGroup can be split to multiple children.
     */
    public boolean isMotionEventSplittingEnabled() {
        return (mGroupFlags & FLAG_SPLIT_MOTION_EVENTS) == FLAG_SPLIT_MOTION_EVENTS;
    }

    /**
     * Resets all touch state in preparation for a new cycle.
     */
    private void resetTouchState() {
        clearTouchTargets();
        resetCancelNextUpFlag(this);
        mGroupFlags &= ~FLAG_DISALLOW_INTERCEPT;
        mNestedScrollAxes = SCROLL_AXIS_NONE;
    }

    /**
     * Resets the cancel next up flag.
     * Returns true if the flag was previously set.
     */
    private static boolean resetCancelNextUpFlag(View view) {
        if ((view.mPrivateFlags & PFLAG_CANCEL_NEXT_UP_EVENT) != 0) {
            view.mPrivateFlags &= ~PFLAG_CANCEL_NEXT_UP_EVENT;
            return true;
        }
        return false;
    }
    
    /**
     * Clears all touch targets.
     */
    private void clearTouchTargets() {
        TouchTarget target = mFirstTouchTarget;
        if (target != null) {
            do {
                TouchTarget next = target.next;
                target.recycle();
                target = next;
            } while (target != null);
            mFirstTouchTarget = null;
        }
    }

    /**
     * Cancels and clears all touch targets.
     */
    private void cancelAndClearTouchTargets(MotionEvent event) {
        if (mFirstTouchTarget != null) {
            boolean syntheticEvent = false;
            if (event == null) {
                final long now = SystemClock.uptimeMillis();
                event = MotionEvent.obtain(now, now,
                        MotionEvent.ACTION_CANCEL, 0.0f, 0.0f, 0);
                event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
                syntheticEvent = true;
            }

            for (TouchTarget target = mFirstTouchTarget; target != null; target = target.next) {
                resetCancelNextUpFlag(target.child);
                dispatchTransformedTouchEvent(event, true, target.child, target.pointerIdBits);
            }
            clearTouchTargets();

            if (syntheticEvent) {
                event.recycle();
            }
        }
    }

    /**
     * Gets the touch target for specified child view.
     * Returns null if not found.
     */
    private TouchTarget getTouchTarget(View child) {
        for (TouchTarget target = mFirstTouchTarget; target != null; target = target.next) {
            if (target.child == child) {
                return target;
            }
        }
        return null;
    }

    /**
     * Adds a touch target for specified child to the beginning of the list.
     * Assumes the target child is not already present.
     */
    private TouchTarget addTouchTarget(View child, int pointerIdBits) {
        TouchTarget target = TouchTarget.obtain(child, pointerIdBits);
        target.next = mFirstTouchTarget;
        mFirstTouchTarget = target;
        return target;
    }
    
    /**
     * Removes the pointer ids from consideration.
     */
    private void removePointersFromTouchTargets(int pointerIdBits) {
        TouchTarget predecessor = null;
        TouchTarget target = mFirstTouchTarget;
        while (target != null) {
            final TouchTarget next = target.next;
            if ((target.pointerIdBits & pointerIdBits) != 0) {
                target.pointerIdBits &= ~pointerIdBits;
                if (target.pointerIdBits == 0) {
                    if (predecessor == null) {
                        mFirstTouchTarget = next;
                    } else {
                        predecessor.next = next;
                    }
                    target.recycle();
                    target = next;
                    continue;
                }
            }
            predecessor = target;
            target = next;
        }
    }

    private void cancelTouchTarget(View view) {
        TouchTarget predecessor = null;
        TouchTarget target = mFirstTouchTarget;
        while (target != null) {
            final TouchTarget next = target.next;
            if (target.child == view) {
                if (predecessor == null) {
                    mFirstTouchTarget = next;
                } else {
                    predecessor.next = next;
                }
                target.recycle();

                final long now = SystemClock.uptimeMillis();
                MotionEvent event = MotionEvent.obtain(now, now,
                        MotionEvent.ACTION_CANCEL, 0.0f, 0.0f, 0);
                event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
                view.dispatchTouchEvent(event);
                event.recycle();
                return;
            }
            predecessor = target;
            target = next;
        }
    }
    
    /**
     * Returns true if a child view can receive pointer events.
     * @hide
     */
    private static boolean canViewReceivePointerEvents(View child) {
        return (child.mViewFlags & VISIBILITY_MASK) == VISIBLE
                || child.getAnimation() != null;
    }

    /**
     * Returns true if a child view contains the specified point when transformed
     * into its coordinate space.
     * Child must not be null.
     * @hide
     */
    protected boolean isTransformedTouchPointInView(float x, float y, View child,
            PointF outLocalPoint) {
        float localX = x + mScrollX - child.mLeft;
        float localY = y + mScrollY - child.mTop;
        final boolean isInView = child.pointInView(localX, localY);
        if (isInView && outLocalPoint != null) {
            outLocalPoint.set(localX, localY);
        }
        return isInView;
    }

    /**
     * Transforms a motion event into the coordinate space of a particular child view,
     * filters out irrelevant pointer ids, and overrides its action if necessary.
     * If child is null, assumes the MotionEvent will be sent to this ViewGroup instead.
     */
    private boolean dispatchTransformedTouchEvent(MotionEvent event, boolean cancel,
            View child, int desiredPointerIdBits) {
        final boolean handled;

        // Canceling motions is a special case.  We don't need to perform any transformations
        // or filtering.  The important part is the action, not the contents.
        final int oldAction = event.getAction();
        if (cancel || oldAction == MotionEvent.ACTION_CANCEL) {
            event.setAction(MotionEvent.ACTION_CANCEL);
            if (child == null) {
                handled = super.dispatchTouchEvent(event);
            } else {
                handled = child.dispatchTouchEvent(event);
            }
            event.setAction(oldAction);
            return handled;
        }
        if (child == null) {
            handled = super.dispatchTouchEvent(event);
        } else {
            final float offsetX = mScrollX - child.mLeft;
            final float offsetY = mScrollY - child.mTop;
            event.offsetLocation(offsetX, offsetY);

            handled = child.dispatchTouchEvent(event);

            event.offsetLocation(-offsetX, -offsetY);
        }
        return handled;
    }
    
    /* Describes a touched view and the ids of the pointers that it has captured.
    *
    * This code assumes that pointer ids are always in the range 0..31 such that
    * it can use a bitfield to track which pointer ids are present.
    * As it happens, the lower layers of the input dispatch pipeline also use the
    * same trick so the assumption should be safe here...
    */
   private static final class TouchTarget {
       private static final int MAX_RECYCLED = 32;
       private static final Object sRecycleLock = new Object[0];
       private static TouchTarget sRecycleBin;
       private static int sRecycledCount;

       public static final int ALL_POINTER_IDS = -1; // all ones

       // The touched child view.
       public View child;

       // The combined bit mask of pointer ids for all pointers captured by the target.
       public int pointerIdBits;

       // The next target in the target list.
       public TouchTarget next;

       private TouchTarget() {
       }

       public static TouchTarget obtain(View child, int pointerIdBits) {
           final TouchTarget target;
           synchronized (sRecycleLock) {
               if (sRecycleBin == null) {
                   target = new TouchTarget();
               } else {
                   target = sRecycleBin;
                   sRecycleBin = target.next;
                    sRecycledCount--;
                   target.next = null;
               }
           }
           target.child = child;
           target.pointerIdBits = pointerIdBits;
           return target;
       }

       public void recycle() {
           synchronized (sRecycleLock) {
               if (sRecycledCount < MAX_RECYCLED) {
                   next = sRecycleBin;
                   sRecycleBin = this;
                   sRecycledCount += 1;
               } else {
                   next = null;
               }
               child = null;
           }
       }
   }
    
    /**
     * {@inheritDoc}
     */
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {

        if (disallowIntercept == ((mGroupFlags & FLAG_DISALLOW_INTERCEPT) != 0)) {
            // We're already in this state, assume our ancestors are too
            return;
        }

        if (disallowIntercept) {
            mGroupFlags |= FLAG_DISALLOW_INTERCEPT;
        } else {
            mGroupFlags &= ~FLAG_DISALLOW_INTERCEPT;
        }

        // Pass it up to our parent
        if (mParent != null) {
            mParent.requestDisallowInterceptTouchEvent(disallowIntercept);
        }
    }
    
    /**
     * Implement this method to intercept all touch screen motion events.  This
     * allows you to watch events as they are dispatched to your children, and
     * take ownership of the current gesture at any point.
     *
     * <p>Using this function takes some care, as it has a fairly complicated
     * interaction with {@link View#onTouchEvent(MotionEvent)
     * View.onTouchEvent(MotionEvent)}, and using it requires implementing
     * that method as well as this one in the correct way.  Events will be
     * received in the following order:
     *
     * <ol>
     * <li> You will receive the down event here.
     * <li> The down event will be handled either by a child of this view
     * group, or given to your own onTouchEvent() method to handle; this means
     * you should implement onTouchEvent() to return true, so you will
     * continue to see the rest of the gesture (instead of looking for
     * a parent view to handle it).  Also, by returning true from
     * onTouchEvent(), you will not receive any following
     * events in onInterceptTouchEvent() and all touch processing must
     * happen in onTouchEvent() like normal.
     * <li> For as long as you return false from this function, each following
     * event (up to and including the final up) will be delivered first here
     * and then to the target's onTouchEvent().
     * <li> If you return true from here, you will not receive any
     * following events: the target view will receive the same event but
     * with the action {@link MotionEvent#ACTION_CANCEL}, and all further
     * events will be delivered to your onTouchEvent() method and no longer
     * appear here.
     * </ol>
     *
     * @param ev The motion event being dispatched down the hierarchy.
     * @return Return true to steal motion events from the children and have
     * them dispatched to this ViewGroup through onTouchEvent().
     * The current target will receive an ACTION_CANCEL event, and no further
     * messages will be delivered here.
     */
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return false;
    }
    
    @Override
    public void dispatchWindowSystemUiVisiblityChanged(int visible) {
        super.dispatchWindowSystemUiVisiblityChanged(visible);

        final int count = mChildrenCount;
        final View[] children = mChildren;
        for (int i=0; i <count; i++) {
            final View child = children[i];
            child.dispatchWindowSystemUiVisiblityChanged(visible);
        }
    }

    @Override
    public void dispatchSystemUiVisibilityChanged(int visible) {
        super.dispatchSystemUiVisibilityChanged(visible);

        final int count = mChildrenCount;
        final View[] children = mChildren;
        for (int i=0; i <count; i++) {
            final View child = children[i];
            child.dispatchSystemUiVisibilityChanged(visible);
        }
    }

    @Override
    boolean updateLocalSystemUiVisibility(int localValue, int localChanges) {
        boolean changed = super.updateLocalSystemUiVisibility(localValue, localChanges);

        final int count = mChildrenCount;
        final View[] children = mChildren;
        for (int i=0; i <count; i++) {
            final View child = children[i];
            changed |= child.updateLocalSystemUiVisibility(localValue, localChanges);
        }
        return changed;
    }
    
    /**
     * {@inheritDoc}
     */
    public void recomputeViewAttributes(View child) {
        if (mAttachInfo != null && !mAttachInfo.mRecomputeGlobalAttributes) {
            ViewGroup parent = mParent;
            if (parent != null) parent.recomputeViewAttributes(this);
            else if (mAttachInfo != null) getViewRootImpl().recomputeViewAttributes(this);
        }
    }
    
    @Override
    void dispatchCollectViewAttributes(AttachInfo attachInfo, int visibility) {
        if ((visibility & VISIBILITY_MASK) == VISIBLE) {
            super.dispatchCollectViewAttributes(attachInfo, visibility);
            final int count = mChildrenCount;
            final View[] children = mChildren;
            for (int i = 0; i < count; i++) {
                final View child = children[i];
                child.dispatchCollectViewAttributes(attachInfo,
                        visibility | (child.mViewFlags&VISIBILITY_MASK));
            }
        }
    }
    
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
    	if ((mPrivateFlags & (PFLAG_FOCUSED | PFLAG_HAS_BOUNDS))
                == (PFLAG_FOCUSED | PFLAG_HAS_BOUNDS)) {
            if (super.dispatchKeyEvent(event)) {
                return true;
            }
        } else if (mFocused != null && (mFocused.mPrivateFlags & PFLAG_HAS_BOUNDS)
                == PFLAG_HAS_BOUNDS) {
            if (mFocused.dispatchKeyEvent(event)) {
                return true;
            }
        }
    	
    	return false;
    }
    
    /*
     * (non-Javadoc)
     *
     * @see android.view.View#findFocus()
     */
    @Override
    public View findFocus() {
        if (isFocused()) {
            return this;
        }

        if (mFocused != null) {
            return mFocused.findFocus();
        }
        return null;
    }
    
    /**
     * Find the nearest view in the specified direction that wants to take
     * focus.
     *
     * @param focused The view that currently has focus
     * @param direction One of FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, and
     *        FOCUS_RIGHT, or 0 for not applicable.
     */
    public View focusSearch(View focused, int direction) {
        if (isRootNamespace()) {
            // root namespace means we should consider ourselves the top of the
            // tree for focus searching; otherwise we could be focus searching
            // into other tabs.  see LocalActivityManager and TabHost for more info
            return FocusFinder.getInstance().findNextFocus(this, focused, direction);
        } else
        if (mParent != null) {
            return mParent.focusSearch(focused, direction);
        }
        return null;
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean requestChildRectangleOnScreen(View child, Rect rectangle, boolean immediate) {
        return false;
    }
    
    /**
     * 查找该layout中第一个有focusable的子孙控件，会判断是否控件的VISIBLE状态
     * @return
     */
    public View findFirstFocusableChild(){
    	View  focusableChild = null;
    	if(getVisibility() == View.VISIBLE && mChildrenCount > 0){
	    	for(View child : mChildren){
	    		if(child != null && child.getVisibility() == View.VISIBLE){	
		    		if(child instanceof ViewGroup){
		    			focusableChild = ((ViewGroup)child).findFirstFocusableChild();
		    			if(focusableChild == null && child.isFocusable()){
		    				focusableChild = child;
		    			}
		    		} else if(child != null){
		    			focusableChild = child.isFocusable() ? child : null;
		    		}
		    		
		    		if(focusableChild != null){
		    			break;
		    		}
	    		}
	    	}
    	}
    	return focusableChild;
    }
    
    /**
     * Returns the focused child of this view, if any. The child may have focus
     * or contain focus.
     *
     * @return the focused child or null.
     */
    public View getFocusedChild() {
        return mFocused;
    }
    
    /**
     * Returns true if this view has or contains focus
     *
     * @return true if this view has or contains focus
     */
    @Override
    public boolean hasFocus() {
        return (mPrivateFlags & PFLAG_FOCUSED) != 0 || mFocused != null;
    }
    
    /**
     * 查询是否有 focusable child
     * @return
     */
    public boolean hasFocusable() {
        if ((mViewFlags & VISIBILITY_MASK) != VISIBLE) {
            return false;
        }

        if (isFocusable()) {
            return true;
        }

        final int descendantFocusability = getDescendantFocusability();
        if (descendantFocusability != FOCUS_BLOCK_DESCENDANTS) {
            final int count = mChildrenCount;
            final View[] children = mChildren;

            for (int i = 0; i < count; i++) {
                final View child = children[i];
                if (child.hasFocusable()) {
                    return true;
                }
            }
        }

        return false;
    }
    
    /**
     * Called when a child view has changed whether or not it is tracking transient state.
     */
    public void childHasTransientStateChanged(View child, boolean childHasTransientState) {
        final boolean oldHasTransientState = hasTransientState();
        if (childHasTransientState) {
            mChildCountWithTransientState++;
        } else {
            mChildCountWithTransientState--;
        }

        final boolean newHasTransientState = hasTransientState();
        if (mParent != null && oldHasTransientState != newHasTransientState) {
            try {
                mParent.childHasTransientStateChanged(this, newHasTransientState);
            } catch (AbstractMethodError e) {
                Log.e(TAG, mParent.getClass().getSimpleName() +
                        " does not fully implement ViewParent", e);
            }
        }
    }

    @Override
    public boolean hasTransientState() {
        return mChildCountWithTransientState > 0 || super.hasTransientState();
    }
    
    /**
     * {@inheritDoc}
     */
    public void clearChildFocus(View child) {

        mFocused = null;
        if (mParent != null) {
            mParent.clearChildFocus(this);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void dispatchWindowFocusChanged(boolean hasFocus) {
        super.dispatchWindowFocusChanged(hasFocus);
        final int count = mChildrenCount;
        final View[] children = mChildren;
        for (int i = 0; i < count; i++) {
            children[i].dispatchWindowFocusChanged(hasFocus);
        }
    }
	
    /**
     * {@inheritDoc}
     */
    @Override
    public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
        final int focusableCount = views.size();
      
        super.addFocusables(views, direction, focusableMode);

        final int descendantFocusability = getDescendantFocusability();

        if (descendantFocusability != FOCUS_BLOCK_DESCENDANTS) {
            final int count = mChildrenCount;
            final View[] children = mChildren;

            for (int i = 0; i < count; i++) {
                final View child = children[i];
                if ((child.mViewFlags & VISIBILITY_MASK) == VISIBLE) {
                    child.addFocusables(views, direction, focusableMode);
                }
            }
        }

        // we add ourselves (if focusable) in all cases except for when we are
        // FOCUS_AFTER_DESCENDANTS and there are some descendants focusable.  this is
        // to avoid the focus search finding layouts when a more precise search
        // among the focusable children would be more interesting.
        if (descendantFocusability != FOCUS_AFTER_DESCENDANTS
                // No focusable descendants
                || (focusableCount == views.size())) {
            super.addFocusables(views, direction, focusableMode);
        }
    }
    
    /**
     * Gets the descendant focusability of this view group.  The descendant
     * focusability defines the relationship between this view group and its
     * descendants when looking for a view to take focus in
     * {@link #requestFocus(int, android.graphics.Rect)}.
     *
     * @return one of {@link #FOCUS_BEFORE_DESCENDANTS}, {@link #FOCUS_AFTER_DESCENDANTS},
     *   {@link #FOCUS_BLOCK_DESCENDANTS}.
     */
    public int getDescendantFocusability() {
        return mGroupFlags & FLAG_MASK_FOCUSABILITY;
    }

    /**
     * Set the descendant focusability of this view group. This defines the relationship
     * between this view group and its descendants when looking for a view to
     * take focus in {@link #requestFocus(int, android.graphics.Rect)}.
     *
     * @param focusability one of {@link #FOCUS_BEFORE_DESCENDANTS}, {@link #FOCUS_AFTER_DESCENDANTS},
     *   {@link #FOCUS_BLOCK_DESCENDANTS}.
     */
    public void setDescendantFocusability(int focusability) {
        switch (focusability) {
            case FOCUS_BEFORE_DESCENDANTS:
            case FOCUS_AFTER_DESCENDANTS:
            case FOCUS_BLOCK_DESCENDANTS:
                break;
            default:
                throw new IllegalArgumentException("must be one of FOCUS_BEFORE_DESCENDANTS, "
                        + "FOCUS_AFTER_DESCENDANTS, FOCUS_BLOCK_DESCENDANTS");
        }
        mGroupFlags &= ~FLAG_MASK_FOCUSABILITY;
        mGroupFlags |= (focusability & FLAG_MASK_FOCUSABILITY);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    void handleFocusGainInternal(int direction, Rect previouslyFocusedRect) {
        if (mFocused != null) {
            mFocused.unFocus();
            mFocused = null;
        }
        super.handleFocusGainInternal(direction, previouslyFocusedRect);
    }
    
    @Override
    public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
    	int descendantFocusability = getDescendantFocusability();

        switch (descendantFocusability) {
            case FOCUS_BLOCK_DESCENDANTS:
                return super.requestFocus(direction, previouslyFocusedRect);
            case FOCUS_BEFORE_DESCENDANTS: {
                final boolean took = super.requestFocus(direction, previouslyFocusedRect);
                return took ? took : onRequestFocusInDescendants(direction, previouslyFocusedRect);
            }
            case FOCUS_AFTER_DESCENDANTS: {
                final boolean took = onRequestFocusInDescendants(direction, previouslyFocusedRect);
                return took ? took : super.requestFocus(direction, previouslyFocusedRect);
            }
            default:
                throw new IllegalStateException("descendant focusability must be "
                        + "one of FOCUS_BEFORE_DESCENDANTS, FOCUS_AFTER_DESCENDANTS, FOCUS_BLOCK_DESCENDANTS "
                        + "but is " + descendantFocusability);
        }
    }
    
    /**
     * Look for a descendant to call {@link View#requestFocus} on.
     * Called by {@link ViewGroup#requestFocus(int, android.graphics.Rect)}
     * when it wants to request focus within its children.  Override this to
     * customize how your {@link ViewGroup} requests focus within its children.
     * @param direction One of FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, and FOCUS_RIGHT
     * @param previouslyFocusedRect The rectangle (in this View's coordinate system)
     *        to give a finer grained hint about where focus is coming from.  May be null
     *        if there is no hint.
     * @return Whether focus was taken.
     */
    protected boolean onRequestFocusInDescendants(int direction,
            Rect previouslyFocusedRect) {
    	return onRequestFocusInDescendants(direction, previouslyFocusedRect, 0);
    }
    
    protected boolean onRequestFocusInDescendants(int direction,
            Rect previouslyFocusedRect, int startIndex){
        int index = startIndex;
        int increment;
        int end;
        int count = mChildrenCount;
        if ((direction & FOCUS_FORWARD) != 0) {
            index = startIndex;
            increment = 1;
            end = count;
        } else {
            index = count - 1;
            increment = -1;
            end = -1;
        }
        final View[] children = mChildren;
        for (int i = index; i != end; i += increment) {
            View child = children[i];
            if ((child.mViewFlags & VISIBILITY_MASK) == VISIBLE) {
                if (child.requestFocus(direction, previouslyFocusedRect)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * LayoutParams are used by views to tell their parents how they want to be
     * laid out. See
     * {@link android.R.styleable#ViewGroup_Layout ViewGroup Layout Attributes}
     * for a list of all child view attributes that this class supports.
     *
     * <p>
     * The base LayoutParams class just describes how big the view wants to be
     * for both width and height. For each dimension, it can specify one of:
     * <ul>
     * <li>FILL_PARENT (renamed MATCH_PARENT in API Level 8 and higher), which
     * means that the view wants to be as big as its parent (minus padding)
     * <li> WRAP_CONTENT, which means that the view wants to be just big enough
     * to enclose its content (plus padding)
     * <li> an exact number
     * </ul>
     * There are subclasses of LayoutParams for different subclasses of
     * ViewGroup. For example, AbsoluteLayout has its own subclass of
     * LayoutParams which adds an X and Y value.</p>
     *
     * <div class="special reference">
     * <h3>Developer Guides</h3>
     * <p>For more information about creating user interface layouts, read the
     * <a href="{@docRoot}guide/topics/ui/declaring-layout.html">XML Layouts</a> developer
     * guide.</p></div>
     *
     * @attr ref android.R.styleable#ViewGroup_Layout_layout_height
     * @attr ref android.R.styleable#ViewGroup_Layout_layout_width
     */
    public static class LayoutParams {
        /**
         * Special value for the height or width requested by a View.
         * FILL_PARENT means that the view wants to be as big as its parent,
         * minus the parent's padding, if any. This value is deprecated
         * starting in API Level 8 and replaced by {@link #MATCH_PARENT}.
         */
        @SuppressWarnings({"UnusedDeclaration"})
        @Deprecated
        public static final int FILL_PARENT = -1;

        /**
         * Special value for the height or width requested by a View.
         * MATCH_PARENT means that the view wants to be as big as its parent,
         * minus the parent's padding, if any. Introduced in API Level 8.
         */
        public static final int MATCH_PARENT = -1;

        /**
         * Special value for the height or width requested by a View.
         * WRAP_CONTENT means that the view wants to be just large enough to fit
         * its own internal content, taking its own padding into account.
         */
        public static final int WRAP_CONTENT = -2;

        /**
         * Information about how wide the view wants to be. Can be one of the
         * constants FILL_PARENT (replaced by MATCH_PARENT ,
         * in API Level 8) or WRAP_CONTENT. or an exact size.
         */
        public int width;

        /**
         * Information about how tall the view wants to be. Can be one of the
         * constants FILL_PARENT (replaced by MATCH_PARENT ,
         * in API Level 8) or WRAP_CONTENT. or an exact size.
         */
        public int height;

        /**
         * Used to animate layouts.
         */
        public LayoutAnimationController.AnimationParameters layoutAnimationParameters;

        /**
         * Creates a new set of layout parameters. The values are extracted from
         * the supplied attributes set and context. The XML attributes mapped
         * to this set of layout parameters are:
         *
         * <ul>
         *   <li><code>layout_width</code>: the width, either an exact value,
         *   {@link #WRAP_CONTENT}, or {@link #FILL_PARENT} (replaced by
         *   {@link #MATCH_PARENT} in API Level 8)</li>
         *   <li><code>layout_height</code>: the height, either an exact value,
         *   {@link #WRAP_CONTENT}, or {@link #FILL_PARENT} (replaced by
         *   {@link #MATCH_PARENT} in API Level 8)</li>
         * </ul>
         *
         * @param c the application environment
         * @param attrs the set of attributes from which to extract the layout
         *              parameters' values
         */
        public LayoutParams(Context c, AttributeSet attrs) {
            TypedArray a = c.obtainStyledAttributes(attrs, com.glview.R.styleable.ViewGroup_Layout);
            try {
            	setBaseAttributes(a,
            			com.glview.R.styleable.ViewGroup_Layout_layout_width,
            			com.glview.R.styleable.ViewGroup_Layout_layout_height);
            } catch (Throwable tr) {
            	a.recycle();
            	a = c.obtainStyledAttributes(attrs, com.glview.AndroidR.styleable.ViewGroup_Layout);
            	setBaseAttributes(a,
            			com.glview.AndroidR.styleable.ViewGroup_Layout_layout_width,
            			com.glview.AndroidR.styleable.ViewGroup_Layout_layout_height);
            }
            a.recycle();
        }

        /**
         * Creates a new set of layout parameters with the specified width
         * and height.
         *
         * @param width the width, either {@link #WRAP_CONTENT},
         *        {@link #FILL_PARENT} (replaced by {@link #MATCH_PARENT} in
         *        API Level 8), or a fixed size in pixels
         * @param height the height, either {@link #WRAP_CONTENT},
         *        {@link #FILL_PARENT} (replaced by {@link #MATCH_PARENT} in
         *        API Level 8), or a fixed size in pixels
         */
        public LayoutParams(int width, int height) {
            this.width = width;
            this.height = height;
        }

        /**
         * Copy constructor. Clones the width and height values of the source.
         *
         * @param source The layout params to copy from.
         */
        public LayoutParams(LayoutParams source) {
            this.width = source.width;
            this.height = source.height;
        }

        /**
         * Used internally by MarginLayoutParams.
         * @hide
         */
        LayoutParams() {
        }

        /**
         * Extracts the layout parameters from the supplied attributes.
         *
         * @param a the style attributes to extract the parameters from
         * @param widthAttr the identifier of the width attribute
         * @param heightAttr the identifier of the height attribute
         */
        protected void setBaseAttributes(TypedArray a, int widthAttr, int heightAttr) {
            width = a.getLayoutDimension(widthAttr, "layout_width");
            height = a.getLayoutDimension(heightAttr, "layout_height");
        }

        /**
         * Resolve layout parameters depending on the layout direction. Subclasses that care about
         * layoutDirection changes should override this method. The default implementation does
         * nothing.
         *
         * @param layoutDirection the direction of the layout
         *
         * {@link View#LAYOUT_DIRECTION_LTR}
         * {@link View#LAYOUT_DIRECTION_RTL}
         */
        public void resolveLayoutDirection(int layoutDirection) {
        }

        /**
         * Returns a String representation of this set of layout parameters.
         *
         * @param output the String to prepend to the internal representation
         * @return a String with the following format: output +
         *         "ViewGroup.LayoutParams={ width=WIDTH, height=HEIGHT }"
         *
         * @hide
         */
        public String debug(String output) {
            return output + "ViewGroup.LayoutParams={ width="
                    + sizeToString(width) + ", height=" + sizeToString(height) + " }";
        }

        /**
         * Use {@code canvas} to draw suitable debugging annotations for these LayoutParameters.
         *
         * @param view the view that contains these layout parameters
         * @param canvas the canvas on which to draw
         *
         * @hide
         */
        public void onDebugDraw(View view, GLCanvas canvas) {
        }

        /**
         * Converts the specified size to a readable String.
         *
         * @param size the size to convert
         * @return a String instance representing the supplied size
         *
         * @hide
         */
        protected static String sizeToString(int size) {
            if (size == WRAP_CONTENT) {
                return "wrap-content";
            }
            if (size == MATCH_PARENT) {
                return "match-parent";
            }
            return String.valueOf(size);
        }
    }

    /**
     * Per-child layout information for layouts that support margins.
     * See
     * {@link android.R.styleable#ViewGroup_MarginLayout ViewGroup Margin Layout Attributes}
     * for a list of all child view attributes that this class supports.
     */
    public static class MarginLayoutParams extends ViewGroup.LayoutParams {
        /**
         * The left margin in pixels of the child.
         * Call {@link ViewGroup#setLayoutParams(LayoutParams)} after reassigning a new value
         * to this field.
         */
        public int leftMargin;

        /**
         * The top margin in pixels of the child.
         * Call {@link ViewGroup#setLayoutParams(LayoutParams)} after reassigning a new value
         * to this field.
         */
        public int topMargin;

        /**
         * The right margin in pixels of the child.
         * Call {@link ViewGroup#setLayoutParams(LayoutParams)} after reassigning a new value
         * to this field.
         */
        public int rightMargin;

        /**
         * The bottom margin in pixels of the child.
         * Call {@link ViewGroup#setLayoutParams(LayoutParams)} after reassigning a new value
         * to this field.
         */
        public int bottomMargin;

        /**
         * The start margin in pixels of the child.
         * Call {@link ViewGroup#setLayoutParams(LayoutParams)} after reassigning a new value
         * to this field.
         */
        private int startMargin = DEFAULT_MARGIN_RELATIVE;

        /**
         * The end margin in pixels of the child.
         * Call {@link ViewGroup#setLayoutParams(LayoutParams)} after reassigning a new value
         * to this field.
         */
        private int endMargin = DEFAULT_MARGIN_RELATIVE;

        /**
         * The default start and end margin.
         * @hide
         */
        public static final int DEFAULT_MARGIN_RELATIVE = Integer.MIN_VALUE;

        /**
         * Bit  0: layout direction
         * Bit  1: layout direction
         * Bit  2: left margin undefined
         * Bit  3: right margin undefined
         * Bit  4: is RTL compatibility mode
         * Bit  5: need resolution
         *
         * Bit 6 to 7 not used
         *
         * @hide
         */
        byte mMarginFlags;

        private static final int LAYOUT_DIRECTION_MASK = 0x00000003;
        private static final int LEFT_MARGIN_UNDEFINED_MASK = 0x00000004;
        private static final int RIGHT_MARGIN_UNDEFINED_MASK = 0x00000008;
        private static final int RTL_COMPATIBILITY_MODE_MASK = 0x00000010;
        private static final int NEED_RESOLUTION_MASK = 0x00000020;

        private static final int DEFAULT_MARGIN_RESOLVED = 0;
        private static final int UNDEFINED_MARGIN = DEFAULT_MARGIN_RELATIVE;

        /**
         * Creates a new set of layout parameters. The values are extracted from
         * the supplied attributes set and context.
         *
         * @param c the application environment
         * @param attrs the set of attributes from which to extract the layout
         *              parameters' values
         */
        public MarginLayoutParams(Context c, AttributeSet attrs) {
            super();

            TypedArray a = c.obtainStyledAttributes(attrs, com.glview.R.styleable.ViewGroup_MarginLayout);
            TypedArray androidA = c.obtainStyledAttributes(attrs, com.glview.AndroidR.styleable.ViewGroup_MarginLayout);
            try {
            	setBaseAttributes(a,
            			com.glview.R.styleable.ViewGroup_MarginLayout_layout_width,
            			com.glview.R.styleable.ViewGroup_MarginLayout_layout_height);
            } catch (Throwable tr) {
            	setBaseAttributes(androidA,
            			com.glview.AndroidR.styleable.ViewGroup_Layout_layout_width,
            			com.glview.AndroidR.styleable.ViewGroup_Layout_layout_height);
            }

            int margin = a.getDimensionPixelSize(
            		com.glview.R.styleable.ViewGroup_MarginLayout_layout_margin, -1);
            if (margin < 0) {
            	margin = androidA.getDimensionPixelSize(
                		com.glview.AndroidR.styleable.ViewGroup_MarginLayout_layout_margin, -1);
            }
            if (margin >= 0) {
                leftMargin = margin;
                topMargin = margin;
                rightMargin= margin;
                bottomMargin = margin;
            } else {
                leftMargin = a.getDimensionPixelSize(
                		com.glview.R.styleable.ViewGroup_MarginLayout_layout_marginLeft,
                        UNDEFINED_MARGIN);
                if (leftMargin == UNDEFINED_MARGIN) {
                	leftMargin = androidA.getDimensionPixelSize(
                    		com.glview.AndroidR.styleable.ViewGroup_MarginLayout_layout_marginLeft,
                            UNDEFINED_MARGIN);
                }
                if (leftMargin == UNDEFINED_MARGIN) {
                    mMarginFlags |= LEFT_MARGIN_UNDEFINED_MASK;
                    leftMargin = DEFAULT_MARGIN_RESOLVED;
                }
                rightMargin = a.getDimensionPixelSize(
                		com.glview.R.styleable.ViewGroup_MarginLayout_layout_marginRight,
                        UNDEFINED_MARGIN);
                if (rightMargin == UNDEFINED_MARGIN) {
                	rightMargin = androidA.getDimensionPixelSize(
                    		com.glview.AndroidR.styleable.ViewGroup_MarginLayout_layout_marginRight,
                            UNDEFINED_MARGIN);
                }
                if (rightMargin == UNDEFINED_MARGIN) {
                    mMarginFlags |= RIGHT_MARGIN_UNDEFINED_MASK;
                    rightMargin = DEFAULT_MARGIN_RESOLVED;
                }

                topMargin = a.getDimensionPixelSize(
                		com.glview.R.styleable.ViewGroup_MarginLayout_layout_marginTop,
                        DEFAULT_MARGIN_RESOLVED);
                if (topMargin == UNDEFINED_MARGIN) {
                	topMargin = androidA.getDimensionPixelSize(
                    		com.glview.AndroidR.styleable.ViewGroup_MarginLayout_layout_marginTop,
                            UNDEFINED_MARGIN);
                }
                bottomMargin = a.getDimensionPixelSize(
                		com.glview.R.styleable.ViewGroup_MarginLayout_layout_marginBottom,
                        DEFAULT_MARGIN_RESOLVED);
                if (bottomMargin == UNDEFINED_MARGIN) {
                	bottomMargin = androidA.getDimensionPixelSize(
                    		com.glview.AndroidR.styleable.ViewGroup_MarginLayout_layout_marginBottom,
                            UNDEFINED_MARGIN);
                }
                
                startMargin = a.getDimensionPixelSize(
                		com.glview.R.styleable.ViewGroup_MarginLayout_layout_marginStart,
                        DEFAULT_MARGIN_RELATIVE);
                endMargin = a.getDimensionPixelSize(
                		com.glview.R.styleable.ViewGroup_MarginLayout_layout_marginEnd,
                        DEFAULT_MARGIN_RELATIVE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                	if (startMargin == DEFAULT_MARGIN_RELATIVE) {
                		startMargin = androidA.getDimensionPixelSize(
                				com.glview.AndroidR.styleable.ViewGroup_MarginLayout_layout_marginStart,
                				DEFAULT_MARGIN_RELATIVE);
                	}
                	if (endMargin == DEFAULT_MARGIN_RELATIVE) {
                		endMargin = androidA.getDimensionPixelSize(
                				com.glview.AndroidR.styleable.ViewGroup_MarginLayout_layout_marginEnd,
                				DEFAULT_MARGIN_RELATIVE);
                	}
                }

                if (isMarginRelative()) {
                   mMarginFlags |= NEED_RESOLUTION_MASK;
                }
            }

            final boolean hasRtlSupport = true;

            // Layout direction is LTR by default
            mMarginFlags |= LAYOUT_DIRECTION_LTR;

            a.recycle();
            androidA.recycle();
        }

        /**
         * {@inheritDoc}
         */
        public MarginLayoutParams(int width, int height) {
            super(width, height);

            mMarginFlags |= LEFT_MARGIN_UNDEFINED_MASK;
            mMarginFlags |= RIGHT_MARGIN_UNDEFINED_MASK;

            mMarginFlags &= ~NEED_RESOLUTION_MASK;
            mMarginFlags &= ~RTL_COMPATIBILITY_MODE_MASK;
        }

        /**
         * Copy constructor. Clones the width, height and margin values of the source.
         *
         * @param source The layout params to copy from.
         */
        public MarginLayoutParams(MarginLayoutParams source) {
            this.width = source.width;
            this.height = source.height;

            this.leftMargin = source.leftMargin;
            this.topMargin = source.topMargin;
            this.rightMargin = source.rightMargin;
            this.bottomMargin = source.bottomMargin;
            this.startMargin = source.startMargin;
            this.endMargin = source.endMargin;

            this.mMarginFlags = source.mMarginFlags;
        }

        /**
         * {@inheritDoc}
         */
        public MarginLayoutParams(LayoutParams source) {
            super(source);

            mMarginFlags |= LEFT_MARGIN_UNDEFINED_MASK;
            mMarginFlags |= RIGHT_MARGIN_UNDEFINED_MASK;

            mMarginFlags &= ~NEED_RESOLUTION_MASK;
            mMarginFlags &= ~RTL_COMPATIBILITY_MODE_MASK;
        }

        /**
         * @hide Used internally.
         */
        public final void copyMarginsFrom(MarginLayoutParams source) {
            this.leftMargin = source.leftMargin;
            this.topMargin = source.topMargin;
            this.rightMargin = source.rightMargin;
            this.bottomMargin = source.bottomMargin;
            this.startMargin = source.startMargin;
            this.endMargin = source.endMargin;

            this.mMarginFlags = source.mMarginFlags;
        }

        /**
         * Sets the margins, in pixels. A call to {@link android.view.View#requestLayout()} needs
         * to be done so that the new margins are taken into account. Left and right margins may be
         * overriden by {@link android.view.View#requestLayout()} depending on layout direction.
         *
         * @param left the left margin size
         * @param top the top margin size
         * @param right the right margin size
         * @param bottom the bottom margin size
         *
         * @attr ref android.R.styleable#ViewGroup_MarginLayout_layout_marginLeft
         * @attr ref android.R.styleable#ViewGroup_MarginLayout_layout_marginTop
         * @attr ref android.R.styleable#ViewGroup_MarginLayout_layout_marginRight
         * @attr ref android.R.styleable#ViewGroup_MarginLayout_layout_marginBottom
         */
        public void setMargins(int left, int top, int right, int bottom) {
            leftMargin = left;
            topMargin = top;
            rightMargin = right;
            bottomMargin = bottom;
            mMarginFlags &= ~LEFT_MARGIN_UNDEFINED_MASK;
            mMarginFlags &= ~RIGHT_MARGIN_UNDEFINED_MASK;
            if (isMarginRelative()) {
                mMarginFlags |= NEED_RESOLUTION_MASK;
            } else {
                mMarginFlags &= ~NEED_RESOLUTION_MASK;
            }
        }

        /**
         * Sets the relative margins, in pixels. A call to {@link android.view.View#requestLayout()}
         * needs to be done so that the new relative margins are taken into account. Left and right
         * margins may be overriden by {@link android.view.View#requestLayout()} depending on layout
         * direction.
         *
         * @param start the start margin size
         * @param top the top margin size
         * @param end the right margin size
         * @param bottom the bottom margin size
         *
         * @attr ref android.R.styleable#ViewGroup_MarginLayout_layout_marginStart
         * @attr ref android.R.styleable#ViewGroup_MarginLayout_layout_marginTop
         * @attr ref android.R.styleable#ViewGroup_MarginLayout_layout_marginEnd
         * @attr ref android.R.styleable#ViewGroup_MarginLayout_layout_marginBottom
         *
         * @hide
         */
        public void setMarginsRelative(int start, int top, int end, int bottom) {
            startMargin = start;
            topMargin = top;
            endMargin = end;
            bottomMargin = bottom;
            mMarginFlags |= NEED_RESOLUTION_MASK;
        }

        /**
         * Sets the relative start margin.
         *
         * @param start the start margin size
         *
         * @attr ref android.R.styleable#ViewGroup_MarginLayout_layout_marginStart
         */
        public void setMarginStart(int start) {
            startMargin = start;
            mMarginFlags |= NEED_RESOLUTION_MASK;
        }

        /**
         * Returns the start margin in pixels.
         *
         * @attr ref android.R.styleable#ViewGroup_MarginLayout_layout_marginStart
         *
         * @return the start margin in pixels.
         */
        public int getMarginStart() {
            if (startMargin != DEFAULT_MARGIN_RELATIVE) return startMargin;
            if ((mMarginFlags & NEED_RESOLUTION_MASK) == NEED_RESOLUTION_MASK) {
                doResolveMargins();
            }
            switch(mMarginFlags & LAYOUT_DIRECTION_MASK) {
                case View.LAYOUT_DIRECTION_RTL:
                    return rightMargin;
                case View.LAYOUT_DIRECTION_LTR:
                default:
                    return leftMargin;
            }
        }

        /**
         * Sets the relative end margin.
         *
         * @param end the end margin size
         *
         * @attr ref android.R.styleable#ViewGroup_MarginLayout_layout_marginEnd
         */
        public void setMarginEnd(int end) {
            endMargin = end;
            mMarginFlags |= NEED_RESOLUTION_MASK;
        }

        /**
         * Returns the end margin in pixels.
         *
         * @attr ref android.R.styleable#ViewGroup_MarginLayout_layout_marginEnd
         *
         * @return the end margin in pixels.
         */
        public int getMarginEnd() {
            if (endMargin != DEFAULT_MARGIN_RELATIVE) return endMargin;
            if ((mMarginFlags & NEED_RESOLUTION_MASK) == NEED_RESOLUTION_MASK) {
                doResolveMargins();
            }
            switch(mMarginFlags & LAYOUT_DIRECTION_MASK) {
                case View.LAYOUT_DIRECTION_RTL:
                    return leftMargin;
                case View.LAYOUT_DIRECTION_LTR:
                default:
                    return rightMargin;
            }
        }

        /**
         * Check if margins are relative.
         *
         * @attr ref android.R.styleable#ViewGroup_MarginLayout_layout_marginStart
         * @attr ref android.R.styleable#ViewGroup_MarginLayout_layout_marginEnd
         *
         * @return true if either marginStart or marginEnd has been set.
         */
        public boolean isMarginRelative() {
            return (startMargin != DEFAULT_MARGIN_RELATIVE || endMargin != DEFAULT_MARGIN_RELATIVE);
        }

        /**
         * Set the layout direction
         * @param layoutDirection the layout direction.
         *        Should be either {@link View#LAYOUT_DIRECTION_LTR}
         *                     or {@link View#LAYOUT_DIRECTION_RTL}.
         */
        public void setLayoutDirection(int layoutDirection) {
            if (layoutDirection != View.LAYOUT_DIRECTION_LTR &&
                    layoutDirection != View.LAYOUT_DIRECTION_RTL) return;
            if (layoutDirection != (mMarginFlags & LAYOUT_DIRECTION_MASK)) {
                mMarginFlags &= ~LAYOUT_DIRECTION_MASK;
                mMarginFlags |= (layoutDirection & LAYOUT_DIRECTION_MASK);
                if (isMarginRelative()) {
                    mMarginFlags |= NEED_RESOLUTION_MASK;
                } else {
                    mMarginFlags &= ~NEED_RESOLUTION_MASK;
                }
            }
        }

        /**
         * Retuns the layout direction. Can be either {@link View#LAYOUT_DIRECTION_LTR} or
         * {@link View#LAYOUT_DIRECTION_RTL}.
         *
         * @return the layout direction.
         */
        public int getLayoutDirection() {
            return (mMarginFlags & LAYOUT_DIRECTION_MASK);
        }

        /**
         * This will be called by {@link android.view.View#requestLayout()}. Left and Right margins
         * may be overridden depending on layout direction.
         */
        @Override
        public void resolveLayoutDirection(int layoutDirection) {
            setLayoutDirection(layoutDirection);

            // No relative margin or pre JB-MR1 case or no need to resolve, just dont do anything
            // Will use the left and right margins if no relative margin is defined.
            if (!isMarginRelative() ||
                    (mMarginFlags & NEED_RESOLUTION_MASK) != NEED_RESOLUTION_MASK) return;

            // Proceed with resolution
            doResolveMargins();
        }

        private void doResolveMargins() {
            if ((mMarginFlags & RTL_COMPATIBILITY_MODE_MASK) == RTL_COMPATIBILITY_MODE_MASK) {
                // if left or right margins are not defined and if we have some start or end margin
                // defined then use those start and end margins.
                if ((mMarginFlags & LEFT_MARGIN_UNDEFINED_MASK) == LEFT_MARGIN_UNDEFINED_MASK
                        && startMargin > DEFAULT_MARGIN_RELATIVE) {
                    leftMargin = startMargin;
                }
                if ((mMarginFlags & RIGHT_MARGIN_UNDEFINED_MASK) == RIGHT_MARGIN_UNDEFINED_MASK
                        && endMargin > DEFAULT_MARGIN_RELATIVE) {
                    rightMargin = endMargin;
                }
            } else {
                // We have some relative margins (either the start one or the end one or both). So use
                // them and override what has been defined for left and right margins. If either start
                // or end margin is not defined, just set it to default "0".
                switch(mMarginFlags & LAYOUT_DIRECTION_MASK) {
                    case View.LAYOUT_DIRECTION_RTL:
                        leftMargin = (endMargin > DEFAULT_MARGIN_RELATIVE) ?
                                endMargin : DEFAULT_MARGIN_RESOLVED;
                        rightMargin = (startMargin > DEFAULT_MARGIN_RELATIVE) ?
                                startMargin : DEFAULT_MARGIN_RESOLVED;
                        break;
                    case View.LAYOUT_DIRECTION_LTR:
                    default:
                        leftMargin = (startMargin > DEFAULT_MARGIN_RELATIVE) ?
                                startMargin : DEFAULT_MARGIN_RESOLVED;
                        rightMargin = (endMargin > DEFAULT_MARGIN_RELATIVE) ?
                                endMargin : DEFAULT_MARGIN_RESOLVED;
                        break;
                }
            }
            mMarginFlags &= ~NEED_RESOLUTION_MASK;
        }

        /**
         * @hide
         */
        public boolean isLayoutRtl() {
            return ((mMarginFlags & LAYOUT_DIRECTION_MASK) == View.LAYOUT_DIRECTION_RTL);
        }

    }

    /**
     * Indicates whether the ViewGroup is drawing its children in the order defined by
     * {@link #getChildDrawingOrder(int, int)}.
     *
     * @return true if children drawing order is defined by {@link #getChildDrawingOrder(int, int)},
     *         false otherwise
     *
     * @see #setChildrenDrawingOrderEnabled(boolean)
     * @see #getChildDrawingOrder(int, int)
     */
    protected boolean isChildrenDrawingOrderEnabled() {
        return hasBooleanFlag(FLAG_USE_CHILD_DRAWING_ORDER);
    }

    /**
     * Tells the ViewGroup whether to draw its children in the order defined by the method
     * {@link #getChildDrawingOrder(int, int)}.
     * <p>
     * Note that {@link View#getZ() Z} reordering, done by {@link #dispatchDraw(Canvas)},
     * will override custom child ordering done via this method.
     *
     * @param enabled true if the order of the children when drawing is determined by
     *        {@link #getChildDrawingOrder(int, int)}, false otherwise
     *
     * @see #isChildrenDrawingOrderEnabled()
     * @see #getChildDrawingOrder(int, int)
     */
    protected void setChildrenDrawingOrderEnabled(boolean enabled) {
        setBooleanFlag(FLAG_USE_CHILD_DRAWING_ORDER, enabled);
    }

    private boolean hasBooleanFlag(int flag) {
        return (mGroupFlags & flag) == flag;
    }

    private void setBooleanFlag(int flag, boolean value) {
        if (value) {
            mGroupFlags |= flag;
        } else {
            mGroupFlags &= ~flag;
        }
    }
    
    
    protected int getChildDrawingOrder(int childCount, int zOrder) {
        return zOrder;
    }
    
    /**
     * Populates (and returns) mPreSortedChildren with a pre-ordered list of the View's children,
     * sorted first by Z, then by child drawing order (if applicable).
     *
     * Uses a stable, insertion sort which is commonly O(n) for ViewGroups with very few elevated
     * children.
     */
    ArrayList<View> buildOrderedChildList() {
        /*final int count = mChildrenCount;
        if (count <= 1 || !hasChildWithZ()) return null;

        if (mPreSortedChildren == null) {
            mPreSortedChildren = new ArrayList<View>(count);
        } else {
            mPreSortedChildren.ensureCapacity(count);
        }

        final boolean useCustomOrder = isChildrenDrawingOrderEnabled();
        for (int i = 0; i < mChildrenCount; i++) {
            // add next child (in child order) to end of list
            int childIndex = useCustomOrder ? getChildDrawingOrder(mChildrenCount, i) : i;
            View nextChild = mChildren[childIndex];
            float currentZ = nextChild.getZ();

            // insert ahead of any Views with greater Z
            int insertIndex = i;
            while (insertIndex > 0 && mPreSortedChildren.get(insertIndex - 1).getZ() > currentZ) {
                insertIndex--;
            }
            mPreSortedChildren.add(insertIndex, nextChild);
        }
        return mPreSortedChildren;*/
    	return null;
    }

    @Override
    public void forceStopFocusAnimation(){
        for (int i = 0; i < mChildrenCount; i++) {
            final View child = mChildren[i];
            child.forceStopFocusAnimation();
        }
    }
    
    /**
     * 由于特殊情况下，想直接设置某个特定的view为focused，一般不建议调用该函数
     * @param view
     */
    @Deprecated
    protected void setChildFocused(View view){
    	mFocused = view;
       mPrivateFlags |= PFLAG_FOCUSED;
    }
    
    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        if ((mGroupFlags & FLAG_NOTIFY_CHILDREN_ON_DRAWABLE_STATE_CHANGE) != 0) {
            if ((mGroupFlags & FLAG_ADD_STATES_FROM_CHILDREN) != 0) {
                throw new IllegalStateException("addStateFromChildren cannot be enabled if a"
                        + " child has duplicateParentState set to true");
            }

            final View[] children = mChildren;
            final int count = mChildrenCount;

            for (int i = 0; i < count; i++) {
                final View child = children[i];
                if ((child.mViewFlags & DUPLICATE_PARENT_STATE) != 0) {
                    child.refreshDrawableState();
                }
            }
        }
    }
    
    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        if ((mGroupFlags & FLAG_ADD_STATES_FROM_CHILDREN) == 0) {
            return super.onCreateDrawableState(extraSpace);
        }

        int need = 0;
        int n = mChildrenCount;
        for (int i = 0; i < n; i++) {
            int[] childState = getChildAt(i).getDrawableState();

            if (childState != null) {
                need += childState.length;
            }
        }

        int[] state = super.onCreateDrawableState(extraSpace + need);

        for (int i = 0; i < n; i++) {
            int[] childState = getChildAt(i).getDrawableState();

            if (childState != null) {
                state = mergeDrawableStates(state, childState);
            }
        }

        return state;
    }
    
    /**
     * If {@link #addStatesFromChildren} is true, refreshes this group's
     * drawable state (to include the states from its children).
     */
    public void childDrawableStateChanged(View child) {
        if ((mGroupFlags & FLAG_ADD_STATES_FROM_CHILDREN) != 0) {
            refreshDrawableState();
        }
    }
    
	public Rect getChildDrawRect(Rect rect, int childDrawingOrder){
		if(childDrawingOrder < 0 || childDrawingOrder >= mChildrenCount){
			return null;
		}
		
		View component = mChildren[childDrawingOrder];
		rect.set(component.mLeft, component.mTop, component.mRight, component.mBottom);
		return rect;
	}
    
	public boolean isOverlap(Rect rectChild, Rect rectParent){		
		if(rectChild.left > rectParent.right){
			return false;
		}
		
		if(rectChild.right < rectParent.left){
			return false;
		}
		
		if(rectChild.top > rectParent.bottom){
			return false;
		}
		
		if(rectChild.bottom < rectParent.top){
			return false;
		}
		
		return true;
	}
    
	// TODO RenderNode begin
	/**
     * This method is used to cause children of this ViewGroup to restore or recreate their
     * display lists. It is called by getDisplayList() when the parent ViewGroup does not need
     * to recreate its own display list, which would happen if it went through the normal
     * draw/dispatchDraw mechanisms.
     *
     * @hide
     */
    @Override
    protected void dispatchGetDisplayList() {
        final int count = mChildrenCount;
        final View[] children = mChildren;
        for (int i = 0; i < count; i++) {
            final View child = children[i];
            if (((child.mViewFlags & VISIBILITY_MASK) == VISIBLE || child.getAnimation() != null)) {
            	recreateChildDisplayList(child);
            }
        }
        
        if (mOverlay != null) {
            View overlayView = mOverlay.getOverlayView();
            recreateChildDisplayList(overlayView);
        }
        if (mDisappearingChildren != null) {
            final ArrayList<View> disappearingChildren = mDisappearingChildren;
            final int disappearingCount = disappearingChildren.size();
            for (int i = 0; i < disappearingCount; ++i) {
                final View child = disappearingChildren.get(i);
                recreateChildDisplayList(child);
            }
        }
    }
    
    private void recreateChildDisplayList(View child) {
        child.mRecreateDisplayList = (child.mPrivateFlags & PFLAG_INVALIDATED)
                == PFLAG_INVALIDATED;
        child.mPrivateFlags &= ~PFLAG_INVALIDATED;
        child.getDisplayList();
        child.mRecreateDisplayList = false;
    }
    
    void invalidateChild(View child, int l, int t, int r, int b) {
    	mPrivateFlags &= ~PFLAG_DRAWING_CACHE_VALID;
    	if (mParent != null) {
    		if (mRenderNode.getLayerType() == LAYER_TYPE_HARDWARE) {
    			// invalidate layer.
    			mPrivateFlags |= PFLAG_INVALIDATED;
    		}
    		mParent.invalidateChild(this, l, t, r, b);
    	} else if (mAttachInfo != null) {
    		mAttachInfo.mViewRootImpl.requestRender();
    	}
    }
    
    /**
     * @hide
     */
    @Override
    public boolean resolveRtlPropertiesIfNeeded() {
        final boolean result = super.resolveRtlPropertiesIfNeeded();
        // We dont need to resolve the children RTL properties if nothing has changed for the parent
        if (result) {
            int count = getChildCount();
            for (int i = 0; i < count; i++) {
                final View child = getChildAt(i);
                if (child.isLayoutDirectionInherited()) {
                    child.resolveRtlPropertiesIfNeeded();
                }
            }
        }
        return result;
    }

    /**
     * @hide
     */
    @Override
    public boolean resolveLayoutDirection() {
        final boolean result = super.resolveLayoutDirection();
        if (result) {
            int count = getChildCount();
            for (int i = 0; i < count; i++) {
                final View child = getChildAt(i);
                if (child.isLayoutDirectionInherited()) {
                    child.resolveLayoutDirection();
                }
            }
        }
        return result;
    }
    
    /**
     * @hide
     */
    @Override
    public void resolvePadding() {
        super.resolvePadding();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.isLayoutDirectionInherited()) {
                child.resolvePadding();
            }
        }
    }

    /**
     * @hide
     */
    @Override
    protected void resolveDrawables() {
        super.resolveDrawables();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.isLayoutDirectionInherited()) {
                child.resolveDrawables();
            }
        }
    }

    /**
     * @hide
     */
    @Override
    public void resolveLayoutParams() {
        super.resolveLayoutParams();
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            child.resolveLayoutParams();
        }
    }

    /**
     * @hide
     */
    @Override
    public void resetResolvedLayoutDirection() {
        super.resetResolvedLayoutDirection();

        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.isLayoutDirectionInherited()) {
                child.resetResolvedLayoutDirection();
            }
        }
    }

    /**
     * @hide
     */
    @Override
    public void resetResolvedPadding() {
        super.resetResolvedPadding();

        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.isLayoutDirectionInherited()) {
                child.resetResolvedPadding();
            }
        }
    }

    /**
     * @hide
     */
    @Override
    protected void resetResolvedDrawables() {
        super.resetResolvedDrawables();

        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.isLayoutDirectionInherited()) {
                child.resetResolvedDrawables();
            }
        }
    }
    
}
