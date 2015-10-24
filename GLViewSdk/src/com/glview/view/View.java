/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.glview.view;

import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;
import static java.lang.Math.max;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Interpolator;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.os.Build;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.HapticFeedbackConstants;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.VelocityTracker;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnDragListener;
import android.view.View.OnGenericMotionListener;
import android.view.View.OnHoverListener;
import android.view.ViewDebug;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;

import com.glview.animation.AnimatorInflater;
import com.glview.animation.FloatProperty;
import com.glview.animation.StateListAnimator;
import com.glview.content.GLContext;
import com.glview.content.res.GLResources;
import com.glview.graphics.Bitmap;
import com.glview.graphics.Point;
import com.glview.graphics.Rect;
import com.glview.graphics.drawable.ColorDrawable;
import com.glview.graphics.drawable.Drawable;
import com.glview.graphics.shader.BaseShader;
import com.glview.graphics.shader.LinearGradient;
import com.glview.graphics.shader.TileMode;
import com.glview.hwui.GLCanvas;
import com.glview.hwui.GLPaint;
import com.glview.hwui.RenderNode;
import com.glview.thread.Handler;
import com.glview.util.LayoutDirection;
import com.glview.util.Predicate;
import com.glview.util.Property;
import com.glview.view.animation.Animation;
import com.glview.view.animation.AnimationUtils;
import com.glview.widget.ScrollBarDrawable;

// GLView is a UI component. It can render to a GLCanvas and accept touch
// events. A GLView may have zero or more child GLView and they form a tree
// structure. The rendering and event handling will pass through the tree
// structure.
//
// A GLView tree should be attached to a GLRoot before event dispatching and
// rendering happens. GLView asks GLRoot to re-render or re-layout the
// GLView hierarchy using requestRender() and requestLayoutContentPane().
//
// The render() method is called in a separate thread. Before calling
// dispatchTouchEvent() and layout(), GLRoot acquires a lock to avoid the
// rendering thread running at the same time. If there are other entry points
// from main thread (like a Handler) in your GLView, you need to call
// lockRendering() if the rendering thread should not run at the same time.
//
/**
* @attr ref android.R.styleable#View_alpha
* @attr ref android.R.styleable#View_background
* @attr ref android.R.styleable#View_clickable
* @attr ref android.R.styleable#View_contentDescription
* @attr ref android.R.styleable#View_drawingCacheQuality
* @attr ref android.R.styleable#View_duplicateParentState
* @attr ref android.R.styleable#View_id
* @attr ref android.R.styleable#View_requiresFadingEdge
* @attr ref android.R.styleable#View_fadeScrollbars
* @attr ref android.R.styleable#View_fadingEdgeLength
* @attr ref android.R.styleable#View_filterTouchesWhenObscured
* @attr ref android.R.styleable#View_fitsSystemWindows
* @attr ref android.R.styleable#View_isScrollContainer
* @attr ref android.R.styleable#View_focusable
* @attr ref android.R.styleable#View_focusableInTouchMode
* @attr ref android.R.styleable#View_hapticFeedbackEnabled
* @attr ref android.R.styleable#View_keepScreenOn
* @attr ref android.R.styleable#View_layerType
* @attr ref android.R.styleable#View_layoutDirection
* @attr ref android.R.styleable#View_longClickable
* @attr ref android.R.styleable#View_minHeight
* @attr ref android.R.styleable#View_minWidth
* @attr ref android.R.styleable#View_nextFocusDown
* @attr ref android.R.styleable#View_nextFocusLeft
* @attr ref android.R.styleable#View_nextFocusRight
* @attr ref android.R.styleable#View_nextFocusUp
* @attr ref android.R.styleable#View_onClick
* @attr ref android.R.styleable#View_padding
* @attr ref android.R.styleable#View_paddingBottom
* @attr ref android.R.styleable#View_paddingLeft
* @attr ref android.R.styleable#View_paddingRight
* @attr ref android.R.styleable#View_paddingTop
* @attr ref android.R.styleable#View_paddingStart
* @attr ref android.R.styleable#View_paddingEnd
* @attr ref android.R.styleable#View_saveEnabled
* @attr ref android.R.styleable#View_rotation
* @attr ref android.R.styleable#View_rotationX
* @attr ref android.R.styleable#View_rotationY
* @attr ref android.R.styleable#View_scaleX
* @attr ref android.R.styleable#View_scaleY
* @attr ref android.R.styleable#View_scrollX
* @attr ref android.R.styleable#View_scrollY
* @attr ref android.R.styleable#View_scrollbarSize
* @attr ref android.R.styleable#View_scrollbarStyle
* @attr ref android.R.styleable#View_scrollbars
* @attr ref android.R.styleable#View_scrollbarDefaultDelayBeforeFade
* @attr ref android.R.styleable#View_scrollbarFadeDuration
* @attr ref android.R.styleable#View_scrollbarTrackHorizontal
* @attr ref android.R.styleable#View_scrollbarThumbHorizontal
* @attr ref android.R.styleable#View_scrollbarThumbVertical
* @attr ref android.R.styleable#View_scrollbarTrackVertical
* @attr ref android.R.styleable#View_scrollbarAlwaysDrawHorizontalTrack
* @attr ref android.R.styleable#View_scrollbarAlwaysDrawVerticalTrack
* @attr ref android.R.styleable#View_soundEffectsEnabled
* @attr ref android.R.styleable#View_tag
* @attr ref android.R.styleable#View_textAlignment
* @attr ref android.R.styleable#View_textDirection
* @attr ref android.R.styleable#View_transformPivotX
* @attr ref android.R.styleable#View_transformPivotY
* @attr ref android.R.styleable#View_translationX
* @attr ref android.R.styleable#View_translationY
* @attr ref android.R.styleable#View_visibility
*
* @see com.ViewGroup.image.view.GLViewGroup
*/

public class View implements KeyEvent.Callback, Drawable.Callback{
	
	private static final boolean DBG = false;

    /**
     * The logging tag used by this class with android.util.Log.
     */
    protected static final String VIEW_LOG_TAG = "View";

//    static final int FLAG_SET_MEASURED_SIZE = 2;

    protected Context mContext;
    
    protected ViewGroup mParent;
    
    /**
     * Indicates no axis of view scrolling.
     */
    public static final int SCROLL_AXIS_NONE = 0;

    /**
     * Indicates scrolling along the horizontal axis.
     */
    public static final int SCROLL_AXIS_HORIZONTAL = 1 << 0;

    /**
     * Indicates scrolling along the vertical axis.
     */
    public static final int SCROLL_AXIS_VERTICAL = 1 << 1;
    
    /**
     * Controls the over-scroll mode for this view.
     * See {@link #overScrollBy(int, int, int, int, int, int, int, int, boolean)},
     * {@link #OVER_SCROLL_ALWAYS}, {@link #OVER_SCROLL_IF_CONTENT_SCROLLS},
     * and {@link #OVER_SCROLL_NEVER}.
     */
    private int mOverScrollMode;
    
    /**
     * {@hide}
     */
    AttachInfo mAttachInfo;
    
    /**
     * When this view has focus and the next focus is {@link #FOCUS_LEFT},
     * the user may specify which view to go to next.
     */
    private int mNextFocusLeftId = View.NO_ID;

    /**
     * When this view has focus and the next focus is {@link #FOCUS_RIGHT},
     * the user may specify which view to go to next.
     */
    private int mNextFocusRightId = View.NO_ID;

    /**
     * When this view has focus and the next focus is {@link #FOCUS_UP},
     * the user may specify which view to go to next.
     */
    private int mNextFocusUpId = View.NO_ID;

    /**
     * When this view has focus and the next focus is {@link #FOCUS_DOWN},
     * the user may specify which view to go to next.
     */
    private int mNextFocusDownId = View.NO_ID;

    /**
     * When this view has focus and the next focus is {@link #FOCUS_FORWARD},
     * the user may specify which view to go to next.
     */
    int mNextFocusForwardId = View.NO_ID;
    
    private PerformClick mPerformClick;
    private CheckForLongPress mPendingCheckForLongPress;
    private CheckForTap mPendingCheckForTap = null;
    
    private UnsetPressedState mUnsetPressedState;
    
    /**
     * Whether the long press's action has been invoked.  The tap's action is invoked on the
     * up event while a long press is invoked as soon as the long press duration is reached, so
     * a long press could be performed before the tap is checked, in which case the tap's action
     * should not be invoked.
     */
    private boolean mHasPerformedLongPress;
    
    /**
     * The minimum height of the view. We'll try our best to have the height
     * of this view to at least this amount.
     */
    private int mMinHeight;

    /**
     * The minimum width of the view. We'll try our best to have the width
     * of this view to at least this amount.
     */
    private int mMinWidth;
    
    /**
     * The delegate to handle touch events that are physically in this view
     * but should be handled by another view.
     */
    private TouchDelegate mTouchDelegate = null;
    
    /**
     * Solid color to use as a background when creating the drawing cache. Enables
     * the cache to use 16 bit bitmaps instead of 32 bit.
     */
    private int mDrawingCacheBackgroundColor = 0;

    /**
     * Special tree observer used when mAttachInfo is null.
     */
    private ViewTreeObserver mFloatingTreeObserver;
    
    /**
     * Vertical scroll factor cached by {@link #getVerticalScrollFactor}.
     */
    private float mVerticalScrollFactor;

    /**
     * Position of the vertical scroll bar.
     */
    private int mVerticalScrollbarPosition;

    /**
     * Position the scroll bar at the default position as determined by the system.
     */
    public static final int SCROLLBAR_POSITION_DEFAULT = 0;

    /**
     * Position the scroll bar along the left edge.
     */
    public static final int SCROLLBAR_POSITION_LEFT = 1;

    /**
     * Position the scroll bar along the right edge.
     */
    public static final int SCROLLBAR_POSITION_RIGHT = 2;
    
	/**
	 * The view's tag.
	 * 
	 * @see #setTag(Object)
	 * @see #getTag()
	 */
	protected Object mTag;
    
    /**
     * The view's identifier.
     * {@hide}
     *
     * @see #setId(int)
     * @see #getId()
     */
    public int mID = NO_ID;

    /**
     * The view flags hold various views states.
     * {@hide}
     */
    int mViewFlags = 0x0;
    
    /**
     * The currently active parent view for receiving delegated nested scrolling events.
     * This is set by {@link #startNestedScroll(int)} during a touch interaction and cleared
     * by {@link #stopNestedScroll()} at the same point where we clear
     * requestDisallowInterceptTouchEvent.
     */
    private ViewGroup mNestedScrollingParent;
    
    private int[] mTempNestedScrollConsumed;
    
    /**
     * Used to mark a View that has no ID.
     */
    public static final int NO_ID = -1;

    /**
     * This view does not want keystrokes. Use with TAKES_FOCUS_MASK when
     * calling setFlags.
     */
    private static final int NOT_FOCUSABLE = 0x00000000;
    
    /**
     * This view wants keystrokes. Use with TAKES_FOCUS_MASK when calling
     * setFlags.
     */
    private static final int FOCUSABLE = 0x00000001;
    
    /**
     * Mask for use with setFlags indicating bits used for focus.
     */
    private static final int FOCUSABLE_MASK = 0x00000001;
    
    /**
     * This view will adjust its padding to fit sytem windows (e.g. status bar)
     */
    private static final int FITS_SYSTEM_WINDOWS = 0x00000002;
    
    /**
     * This view is visible.
     * Use with {@link #setVisibility} and <a href="#attr_android:visibility">{@code
     * android:visibility}.
     */
    public static final int VISIBLE = 0x00000000;

    /**
     * This view is invisible, but it still takes up space for layout purposes.
     * Use with {@link #setVisibility} and <a href="#attr_android:visibility">{@code
     * android:visibility}.
     */
    public static final int INVISIBLE = 0x00000004;

    /**
     * This view is invisible, and it doesn't take any space for layout
     * purposes. Use with {@link #setVisibility} and <a href="#attr_android:visibility">{@code
     * android:visibility}.
     */
    public static final int GONE = 0x00000008;
    
    /**
     * Mask for use with setFlags indicating bits used for visibility.
     * {@hide}
     */
    static final int VISIBILITY_MASK = 0x0000000C;
    
    
    /**
     * <p>Indicates this view can be clicked. When clickable, a View reacts
     * to clicks by notifying the OnClickListener.<p>
     * {@hide}
     */
    static final int CLICKABLE = 0x00004000;
    
    /**
     * <p>Indicates this view is caching its drawing into a bitmap.</p>
     * {@hide}
     */
    static final int DRAWING_CACHE_ENABLED = 0x00008000;

    /**
     * <p>Indicates that no icicle should be saved for this view.<p>
     * {@hide}
     */
    static final int SAVE_DISABLED = 0x000010000;

    /**
     * <p>Mask for use with setFlags indicating bits used for the saveEnabled
     * property.</p>
     * {@hide}
     */
    static final int SAVE_DISABLED_MASK = 0x000010000;

    /**
     * <p>Indicates that no drawing cache should ever be created for this view.<p>
     * {@hide}
     */
    static final int WILL_NOT_CACHE_DRAWING = 0x000020000;
    
    /**
     * <p>Indicates this view can take / keep focus when int touch mode.</p>
     * {@hide}
     */
    static final int FOCUSABLE_IN_TOUCH_MODE = 0x00040000;
    
    /**
     * <p>
     * Indicates this view can be long clicked. When long clickable, a View
     * reacts to long clicks by notifying the OnLongClickListener or showing a
     * context menu.
     * </p>
     * {@hide}
     */
    static final int LONG_CLICKABLE = 0x00200000;
    
    /**
     * The scrollbar style to display the scrollbars inside the content area,
     * without increasing the padding. The scrollbars will be overlaid with
     * translucency on the view's content.
     */
    public static final int SCROLLBARS_INSIDE_OVERLAY = 0;

    /**
     * The scrollbar style to display the scrollbars inside the padded area,
     * increasing the padding of the view. The scrollbars will not overlap the
     * content area of the view.
     */
    public static final int SCROLLBARS_INSIDE_INSET = 0x01000000;

    /**
     * The scrollbar style to display the scrollbars at the edge of the view,
     * without increasing the padding. The scrollbars will be overlaid with
     * translucency.
     */
    public static final int SCROLLBARS_OUTSIDE_OVERLAY = 0x02000000;

    /**
     * The scrollbar style to display the scrollbars at the edge of the view,
     * increasing the padding of the view. The scrollbars will only overlap the
     * background, if any.
     */
    public static final int SCROLLBARS_OUTSIDE_INSET = 0x03000000;

    /**
     * Mask to check if the scrollbar style is overlay or inset.
     * {@hide}
     */
    static final int SCROLLBARS_INSET_MASK = 0x01000000;

    /**
     * Mask to check if the scrollbar style is inside or outside.
     * {@hide}
     */
    static final int SCROLLBARS_OUTSIDE_MASK = 0x02000000;

    /**
     * Mask for scrollbar style.
     * {@hide}
     */
    static final int SCROLLBARS_STYLE_MASK = 0x03000000;
    
    /**
     * View flag indicating that the screen should remain on while the
     * window containing this view is visible to the user.  This effectively
     * takes care of automatically setting the WindowManager's
     * {@link WindowManager.LayoutParams#FLAG_KEEP_SCREEN_ON}.
     */
    public static final int KEEP_SCREEN_ON = 0x04000000;
    
    /**
     * View flag indicating whether this view should have sound effects enabled
     * for events such as clicking and touching.
     */
    public static final int SOUND_EFFECTS_ENABLED = 0x08000000;
    
    /**
     * View flag indicating whether this view should have haptic feedback
     * enabled for events such as long presses.
     */
    public static final int HAPTIC_FEEDBACK_ENABLED = 0x10000000;
    
    /**
     * This view is enabled. Interpretation varies by subclass.
     * Use with ENABLED_MASK when calling setFlags.
     * {@hide}
     */
    static final int ENABLED = 0x00000000;

    /**
     * This view is disabled. Interpretation varies by subclass.
     * Use with ENABLED_MASK when calling setFlags.
     * {@hide}
     */
    static final int DISABLED = 0x00000020;

   /**
    * Mask for use with setFlags indicating bits used for indicating whether
    * this view is enabled
    * {@hide}
    */
    static final int ENABLED_MASK = 0x00000020;
    
    /**
     * This view won't draw. {@link #onDraw(android.graphics.Canvas)} won't be
     * called and further optimizations will be performed. It is okay to have
     * this flag set and a background. Use with DRAW_MASK when calling setFlags.
     * {@hide}
     */
    static final int WILL_NOT_DRAW = 0x00000080;

    /**
     * Mask for use with setFlags indicating bits used for indicating whether
     * this view is will draw
     * {@hide}
     */
    static final int DRAW_MASK = 0x00000080;
    
    /**
     * <p>This view doesn't show scrollbars.</p>
     * {@hide}
     */
    static final int SCROLLBARS_NONE = 0x00000000;

    /**
     * <p>This view shows horizontal scrollbars.</p>
     * {@hide}
     */
    static final int SCROLLBARS_HORIZONTAL = 0x00000100;

    /**
     * <p>This view shows vertical scrollbars.</p>
     * {@hide}
     */
    static final int SCROLLBARS_VERTICAL = 0x00000200;

    /**
     * <p>Mask for use with setFlags indicating bits used for indicating which
     * scrollbars are enabled.</p>
     * {@hide}
     */
    static final int SCROLLBARS_MASK = 0x00000300;
    
    /**
     * Bits of {@link #getMeasuredWidthAndState()} and
     * {@link #getMeasuredWidthAndState()} that provide the actual measured size.
     */
    public static final int MEASURED_SIZE_MASK = 0x00ffffff;

    /**
     * Bits of {@link #getMeasuredWidthAndState()} and
     * {@link #getMeasuredWidthAndState()} that provide the additional state bits.
     */
    public static final int MEASURED_STATE_MASK = 0xff000000;

    private static final int[] VISIBILITY_FLAGS = {VISIBLE, INVISIBLE, GONE};

    protected int mMeasuredWidth = 0;
    protected int mMeasuredHeight = 0;

    int mOldWidthMeasureSpec = -1;
    int mOldHeightMeasureSpec = -1;
    static final int FLAG_INVISIBLE = VISIBILITY_MASK;

    /**
     * The offset, in pixels, by which the content of this view is scrolled horizontally.
     */
    protected int mScrollX;
    /**
     * The offset, in pixels, by which the content of this view is scrolled vertically.
     */
    protected int mScrollY;
    
    protected int mScrollHeight = 0;
    protected int mScrollWidth = 0;

    private int mBackgroundResource = 0;
    private boolean mBackgroundSizeChanged;
    private Drawable mBackground;
    
    private String mTransitionName;
    
    protected Animation mCurrentAnimation;
//    protected boolean mBisViewEnable = true;
    
    
    /**
     * View flag indicating whether {@link #addFocusables(ArrayList, int, int)}
     * should add all focusable Views regardless if they are focusable in touch mode.
     */
    public static final int FOCUSABLES_ALL = 0x00000000;

    /**
     * View flag indicating whether {@link #addFocusables(ArrayList, int, int)}
     * should add only Views focusable in touch mode.
     */
    public static final int FOCUSABLES_TOUCH_MODE = 0x00000001;
    
    /**
     * Use with {@link #focusSearch(int)}. Move focus to the previous selectable
     * item.
     */
    public static final int FOCUS_BACKWARD = 0x00000001;

    /**
     * Use with {@link #focusSearch(int)}. Move focus to the next selectable
     * item.
     */
    public static final int FOCUS_FORWARD = 0x00000002;

    /**
     * Use with {@link #focusSearch(int)}. Move focus to the left.
     */
    public static final int FOCUS_LEFT = 0x00000011;

    /**
     * Use with {@link #focusSearch(int)}. Move focus up.
     */
    public static final int FOCUS_UP = 0x00000021;

    /**
     * Use with {@link #focusSearch(int)}. Move focus to the right.
     */
    public static final int FOCUS_RIGHT = 0x00000042;

    /**
     * Use with {@link #focusSearch(int)}. Move focus down.
     */
    public static final int FOCUS_DOWN = 0x00000082;
    
    /**
     * SoundEffectConstants.CLICK
     */
    public static final int CLICK = SoundEffectConstants.CLICK;
    
    /**
     * Bit shift of {@link #MEASURED_STATE_MASK} to get to the height bits
     * for functions that combine both width and height into a single int,
     * such as {@link #getMeasuredState()} and the childState argument of
     * {@link #resolveSizeAndState(int, int, int)}.
     */
    public static final int MEASURED_HEIGHT_STATE_SHIFT = 16;
    
    /**
     * Bit of {@link #getMeasuredWidthAndState()} and
     * {@link #getMeasuredWidthAndState()} that indicates the measured size
     * is smaller that the space the view would like to have.
     */
    public static final int MEASURED_STATE_TOO_SMALL = 0x01000000;
    
    int mPrivateFlags = 0;
    
    
    /** {@hide} */
    static final int PFLAG_FOCUSED                     = 0x00000002;
    
    /** {@hide} */
    static final int PFLAG_SELECTED                    = 0x00000004;
    /** {@hide} */
    static final int PFLAG_IS_ROOT_NAMESPACE           = 0x00000008;
    
    /** {@hide} */
    static final int PFLAG_HAS_BOUNDS                  = 0x00000010;
    
    /** {@hide} */
//    static final int PFLAG_FORCE_LAYOUT                = 0x00001000;
    /** {@hide} */
    static final int PFLAG_FORCE_LAYOUT             = 0x00001000;
    
    /** {@hide} */
    static final int PFLAG_LAYOUT_REQUIRED             = 0x00002000;
    
    static final int PFLAG_MEASURED_DIMENSION_SET      = 0x00000800;
    
    private static final int PFLAG_PRESSED             = 0x00004000;
    
    /**
     * Flag used to indicate that this view should be drawn once more (and only once
     * more) after its animation has completed.
     * {@hide}
     */
    static final int PFLAG_ANIMATION_STARTED           = 0x00010000;
    
    
    int mPrivateTransformationFlags = 0;
    
    /**
     * Indicates that the View returned true when onSetAlpha() was called and that
     * the alpha must be restored.
     * {@hide}
     */
    static final int PFLAG_ALPHA_SET                   = 0x00040000;
    
    static final int PFLAG_TRANSLATE_SET               = 0x00000001;
    
    static final int PFLAG_ROTATION_SET                = 0x00000002;
    
    static final int PFLAG_SCALE_SET 				   = 0x00000004;
    
    /** {@hide} */
    static final int PFLAG_DRAWABLE_STATE_DIRTY        = 0x00000400;
    
    /**
     * Indicates whether the background is opaque.
     *
     * @hide
     */
    static final int PFLAG_OPAQUE_BACKGROUND           = 0x00800000;
    
    /**
     * Indicates whether the scrollbars are opaque.
     *
     * @hide
     */
    static final int PFLAG_OPAQUE_SCROLLBARS           = 0x01000000;

    /**
     * Indicates whether the view is opaque.
     *
     * @hide
     */
    static final int PFLAG_OPAQUE_MASK                 = 0x01800000;
    
    /**
     * Indicates a prepressed state;
     * the short time between ACTION_DOWN and recognizing
     * a 'real' press. Prepressed is used to recognize quick taps
     * even when they are shorter than ViewConfiguration.getTapTimeout().
     *
     * @hide
     */
    private static final int PFLAG_PREPRESSED          = 0x02000000;
    
    /**
     * Indicates whether the view is temporarily detached.
     *
     * @hide
     */
    static final int PFLAG_CANCEL_NEXT_UP_EVENT        = 0x04000000;
    
    /**
     * Indicates that we should awaken scroll bars once attached
     *
     * @hide
     */
    private static final int PFLAG_AWAKEN_SCROLL_BARS_ON_ATTACH = 0x08000000;
    
    /**
     * Indicates that the view has received HOVER_ENTER.  Cleared on HOVER_EXIT.
     * @hide
     */
    private static final int PFLAG_HOVERED             = 0x10000000;
    
    /** {@hide} */
    static final int PFLAG_ACTIVATED                   = 0x40000000;
    
    /**
     * Masks for mPrivateFlags2, as generated by dumpFlags():
     *
     * -------|-------|-------|-------|
     *                                  PFLAG2_TEXT_ALIGNMENT_FLAGS[0]
     *                                  PFLAG2_TEXT_DIRECTION_FLAGS[0]
     *                                1 PFLAG2_DRAG_CAN_ACCEPT
     *                               1  PFLAG2_DRAG_HOVERED
     *                               1  PFLAG2_LAYOUT_DIRECTION_MASK_SHIFT
     *                              11  PFLAG2_TEXT_DIRECTION_MASK_SHIFT
     *                             1 1  PFLAG2_TEXT_DIRECTION_RESOLVED_MASK_SHIFT
     *                             11   PFLAG2_LAYOUT_DIRECTION_MASK
     *                             11 1 PFLAG2_TEXT_ALIGNMENT_MASK_SHIFT
     *                            1     PFLAG2_LAYOUT_DIRECTION_RESOLVED_RTL
     *                            1   1 PFLAG2_TEXT_ALIGNMENT_RESOLVED_MASK_SHIFT
     *                            1 1   PFLAG2_IMPORTANT_FOR_ACCESSIBILITY_SHIFT
     *                           1      PFLAG2_LAYOUT_DIRECTION_RESOLVED
     *                           11     PFLAG2_LAYOUT_DIRECTION_RESOLVED_MASK
     *                          1       PFLAG2_TEXT_DIRECTION_FLAGS[1]
     *                         1        PFLAG2_TEXT_DIRECTION_FLAGS[2]
     *                         11       PFLAG2_TEXT_DIRECTION_FLAGS[3]
     *                        1         PFLAG2_TEXT_DIRECTION_FLAGS[4]
     *                        1 1       PFLAG2_TEXT_DIRECTION_FLAGS[5]
     *                        111       PFLAG2_TEXT_DIRECTION_MASK
     *                       1          PFLAG2_TEXT_DIRECTION_RESOLVED
     *                      1           PFLAG2_TEXT_DIRECTION_RESOLVED_DEFAULT
     *                    111           PFLAG2_TEXT_DIRECTION_RESOLVED_MASK
     *                   1              PFLAG2_TEXT_ALIGNMENT_FLAGS[1]
     *                  1               PFLAG2_TEXT_ALIGNMENT_FLAGS[2]
     *                  11              PFLAG2_TEXT_ALIGNMENT_FLAGS[3]
     *                 1                PFLAG2_TEXT_ALIGNMENT_FLAGS[4]
     *                 1 1              PFLAG2_TEXT_ALIGNMENT_FLAGS[5]
     *                 11               PFLAG2_TEXT_ALIGNMENT_FLAGS[6]
     *                 111              PFLAG2_TEXT_ALIGNMENT_MASK
     *                1                 PFLAG2_TEXT_ALIGNMENT_RESOLVED
     *               1                  PFLAG2_TEXT_ALIGNMENT_RESOLVED_DEFAULT
     *             111                  PFLAG2_TEXT_ALIGNMENT_RESOLVED_MASK
     *           11                     PFLAG2_IMPORTANT_FOR_ACCESSIBILITY_MASK
     *          1                       PFLAG2_HAS_TRANSIENT_STATE
     *      1                           PFLAG2_ACCESSIBILITY_FOCUSED
     *     1                            PFLAG2_ACCESSIBILITY_STATE_CHANGED
     *    1                             PFLAG2_VIEW_QUICK_REJECTED
     *   1                              PFLAG2_PADDING_RESOLVED
     * -------|-------|-------|-------|
     */
    int mPrivateFlags2;
    
    /**
     * Indicates that this view has reported that it can accept the current drag's content.
     * Cleared when the drag operation concludes.
     * @hide
     */
    static final int PFLAG2_DRAG_CAN_ACCEPT            = 0x00000001;

    /**
     * Indicates that this view is currently directly under the drag location in a
     * drag-and-drop operation involving content that it can accept.  Cleared when
     * the drag exits the view, or when the drag operation concludes.
     * @hide
     */
    static final int PFLAG2_DRAG_HOVERED               = 0x00000002;
    
    /**
     * Horizontal layout direction of this view is from Left to Right.
     * Use with {@link #setLayoutDirection}.
     */
    public static final int LAYOUT_DIRECTION_LTR = LayoutDirection.LTR;

    /**
     * Horizontal layout direction of this view is from Right to Left.
     * Use with {@link #setLayoutDirection}.
     */
    public static final int LAYOUT_DIRECTION_RTL = LayoutDirection.RTL;

    /**
     * Horizontal layout direction of this view is inherited from its parent.
     * Use with {@link #setLayoutDirection}.
     */
    public static final int LAYOUT_DIRECTION_INHERIT = LayoutDirection.INHERIT;

    /**
     * Horizontal layout direction of this view is from deduced from the default language
     * script for the locale. Use with {@link #setLayoutDirection}.
     */
    public static final int LAYOUT_DIRECTION_LOCALE = LayoutDirection.LOCALE;
    
    /**
     * Default horizontal layout direction.
     */
    private static final int LAYOUT_DIRECTION_DEFAULT = LAYOUT_DIRECTION_INHERIT;
    
    /**
     * Default horizontal layout direction.
     * @hide
     */
    static final int LAYOUT_DIRECTION_RESOLVED_DEFAULT = LAYOUT_DIRECTION_LTR;
    
    /**
     * Bit shift to get the horizontal layout direction. (bits after DRAG_HOVERED)
     * @hide
     */
    static final int PFLAG2_LAYOUT_DIRECTION_MASK_SHIFT = 2;

    /**
     * Mask for use with private flags indicating bits used for horizontal layout direction.
     * @hide
     */
    static final int PFLAG2_LAYOUT_DIRECTION_MASK = 0x00000003 << PFLAG2_LAYOUT_DIRECTION_MASK_SHIFT;

    /**
     * Indicates whether the view horizontal layout direction has been resolved and drawn to the
     * right-to-left direction.
     * @hide
     */
    static final int PFLAG2_LAYOUT_DIRECTION_RESOLVED_RTL = 4 << PFLAG2_LAYOUT_DIRECTION_MASK_SHIFT;

    /**
     * Indicates whether the view horizontal layout direction has been resolved.
     * @hide
     */
    static final int PFLAG2_LAYOUT_DIRECTION_RESOLVED = 8 << PFLAG2_LAYOUT_DIRECTION_MASK_SHIFT;

    /**
     * Mask for use with private flags indicating bits used for resolved horizontal layout direction.
     * @hide
     */
    static final int PFLAG2_LAYOUT_DIRECTION_RESOLVED_MASK = 0x0000000C
            << PFLAG2_LAYOUT_DIRECTION_MASK_SHIFT;

    /*
     * Array of horizontal layout direction flags for mapping attribute "layoutDirection" to correct
     * flag value.
     * @hide
     */
    private static final int[] LAYOUT_DIRECTION_FLAGS = {
            LAYOUT_DIRECTION_LTR,
            LAYOUT_DIRECTION_RTL,
            LAYOUT_DIRECTION_INHERIT,
            LAYOUT_DIRECTION_LOCALE
    };
    
    /**
     * Flag indicating that start/end padding has been resolved into left/right padding
     * for use in measurement, layout, drawing, etc. This is set by {@link #resolvePadding()}
     * and checked by {@link #measure(int, int)} to determine if padding needs to be resolved
     * during measurement. In some special cases this is required such as when an adapter-based
     * view measures prospective children without attaching them to a window.
     */
    static final int PFLAG2_PADDING_RESOLVED = 0x20000000;

    /**
     * Flag indicating that the start/end drawables has been resolved into left/right ones.
     */
    static final int PFLAG2_DRAWABLE_RESOLVED = 0x40000000;

    /**
     * Indicates that the view is tracking some sort of transient state
     * that the app should not need to be aware of, but that the framework
     * should take special care to preserve.
     */
    static final int PFLAG2_HAS_TRANSIENT_STATE = 0x80000000;

    /**
     * Group of bits indicating that RTL properties resolution is done.
     */
    static final int ALL_RTL_PROPERTIES_RESOLVED = PFLAG2_LAYOUT_DIRECTION_RESOLVED |
            /*PFLAG2_TEXT_DIRECTION_RESOLVED |
            PFLAG2_TEXT_ALIGNMENT_RESOLVED |*/
            PFLAG2_PADDING_RESOLVED |
            PFLAG2_DRAWABLE_RESOLVED;
    
    
    int mPrivateFlags3;
    
    /**
     * This view's request for the visibility of the status bar.
     * @hide
     */
    int mSystemUiVisibility;
    
    /**
     * <p>Indicates that this view gets its drawable states from its direct parent
     * and ignores its original internal states.</p>
     *
     * @hide
     */
    static final int DUPLICATE_PARENT_STATE = 0x00400000;
    
    protected int mLeft, mTop, mRight, mBottom;
    protected int mXCenter, mYCenter;
    protected int mPaddingLeft, mPaddingTop, mPaddingRight, mPaddingBottom;
    
    /**
     * Predicate for matching a view by its id.
     */
    private MatchIdPredicate mMatchIdPredicate;
    
    /**
     * Cache the paddingRight set by the user to append to the scrollbar's size.
     *
     * @hide
     */
    protected int mUserPaddingRight;

    /**
     * Cache the paddingBottom set by the user to append to the scrollbar's size.
     *
     * @hide
     */
    protected int mUserPaddingBottom;

    /**
     * Cache the paddingLeft set by the user to append to the scrollbar's size.
     *
     * @hide
     */
    protected int mUserPaddingLeft;

    /**
     * Cache the paddingStart set by the user to append to the scrollbar's size.
     *
     */
    int mUserPaddingStart;

    /**
     * Cache the paddingEnd set by the user to append to the scrollbar's size.
     *
     */
    int mUserPaddingEnd;

    /**
     * Cache initial left padding.
     *
     * @hide
     */
    int mUserPaddingLeftInitial;

    /**
     * Cache initial right padding.
     *
     * @hide
     */
    int mUserPaddingRightInitial;

    /**
     * Default undefined padding
     */
    private static final int UNDEFINED_PADDING = Integer.MIN_VALUE;

    /**
     * Cache if a left padding has been defined
     */
    private boolean mLeftPaddingDefined = false;

    /**
     * Cache if a right padding has been defined
     */
    private boolean mRightPaddingDefined = false;
    
    /**
     * Reference count for transient state.
     * @see #setHasTransientState(boolean)
     */
    int mTransientStateCount = 0;
    
    /**
     * Count of how many windows this view has been attached to.
     */
    int mWindowAttachCount;
    
    /**
     * The layout parameters associated with this view and used by the parent
     * {@link ViewGroup} to determine how this view should be
     * laid out.
     * {@hide}
     */
    protected ViewGroup.LayoutParams mLayoutParams;
    
    public final static int NO_IN_PARENT = -1;
    
    /**
     * Cache the touch slop from the context that created the view.
     */
    private int mTouchSlop;
    
    /**
     * Briefly describes the view and is primarily used for accessibility support.
     */
    private CharSequence mContentDescription;
    
    public final static int CREATE_BACKGROUND = 1;	
    
    private ScrollabilityCache mScrollCache;
    
    private int[] mDrawableState = null;
    
    /**
     * Animator that automatically runs based on state changes.
     */
    private StateListAnimator mStateListAnimator;
    
    /**set drawable state**/
    
    /**
     * Base View state sets
     */
    // Singles
    /**
     * Indicates the view has no states set. States are used with
     * {@link android.graphics.drawable.Drawable} to change the drawing of the
     * view depending on its state.
     *
     * @see android.graphics.drawable.Drawable
     * @see #getDrawableState()
     */
    protected static final int[] EMPTY_STATE_SET;
    /**
     * Indicates the view is enabled. States are used with
     * {@link android.graphics.drawable.Drawable} to change the drawing of the
     * view depending on its state.
     *
     * @see android.graphics.drawable.Drawable
     * @see #getDrawableState()
     */
    protected static final int[] ENABLED_STATE_SET;
    /**
     * Indicates the view is focused. States are used with
     * {@link android.graphics.drawable.Drawable} to change the drawing of the
     * view depending on its state.
     *
     * @see android.graphics.drawable.Drawable
     * @see #getDrawableState()
     */
    protected static final int[] FOCUSED_STATE_SET;
    /**
     * Indicates the view is selected. States are used with
     * {@link android.graphics.drawable.Drawable} to change the drawing of the
     * view depending on its state.
     *
     * @see android.graphics.drawable.Drawable
     * @see #getDrawableState()
     */
    protected static final int[] SELECTED_STATE_SET;
    /**
     * Indicates the view is pressed. States are used with
     * {@link android.graphics.drawable.Drawable} to change the drawing of the
     * view depending on its state.
     *
     * @see android.graphics.drawable.Drawable
     * @see #getDrawableState()
     * @hide
     */
    protected static final int[] PRESSED_STATE_SET;
    /**
     * Indicates the view's window has focus. States are used with
     * {@link android.graphics.drawable.Drawable} to change the drawing of the
     * view depending on its state.
     *
     * @see android.graphics.drawable.Drawable
     * @see #getDrawableState()
     */
    protected static final int[] WINDOW_FOCUSED_STATE_SET;
    // Doubles
    /**
     * Indicates the view is enabled and has the focus.
     *
     * @see #ENABLED_STATE_SET
     * @see #FOCUSED_STATE_SET
     */
    protected static final int[] ENABLED_FOCUSED_STATE_SET;
    /**
     * Indicates the view is enabled and selected.
     *
     * @see #ENABLED_STATE_SET
     * @see #SELECTED_STATE_SET
     */
    protected static final int[] ENABLED_SELECTED_STATE_SET;
    /**
     * Indicates the view is enabled and that its window has focus.
     *
     * @see #ENABLED_STATE_SET
     * @see #WINDOW_FOCUSED_STATE_SET
     */
    protected static final int[] ENABLED_WINDOW_FOCUSED_STATE_SET;
    /**
     * Indicates the view is focused and selected.
     *
     * @see #FOCUSED_STATE_SET
     * @see #SELECTED_STATE_SET
     */
    protected static final int[] FOCUSED_SELECTED_STATE_SET;
    /**
     * Indicates the view has the focus and that its window has the focus.
     *
     * @see #FOCUSED_STATE_SET
     * @see #WINDOW_FOCUSED_STATE_SET
     */
    protected static final int[] FOCUSED_WINDOW_FOCUSED_STATE_SET;
    /**
     * Indicates the view is selected and that its window has the focus.
     *
     * @see #SELECTED_STATE_SET
     * @see #WINDOW_FOCUSED_STATE_SET
     */
    protected static final int[] SELECTED_WINDOW_FOCUSED_STATE_SET;
    // Triples
    /**
     * Indicates the view is enabled, focused and selected.
     *
     * @see #ENABLED_STATE_SET
     * @see #FOCUSED_STATE_SET
     * @see #SELECTED_STATE_SET
     */
    protected static final int[] ENABLED_FOCUSED_SELECTED_STATE_SET;
    /**
     * Indicates the view is enabled, focused and its window has the focus.
     *
     * @see #ENABLED_STATE_SET
     * @see #FOCUSED_STATE_SET
     * @see #WINDOW_FOCUSED_STATE_SET
     */
    protected static final int[] ENABLED_FOCUSED_WINDOW_FOCUSED_STATE_SET;
    /**
     * Indicates the view is enabled, selected and its window has the focus.
     *
     * @see #ENABLED_STATE_SET
     * @see #SELECTED_STATE_SET
     * @see #WINDOW_FOCUSED_STATE_SET
     */
    protected static final int[] ENABLED_SELECTED_WINDOW_FOCUSED_STATE_SET;
    /**
     * Indicates the view is focused, selected and its window has the focus.
     *
     * @see #FOCUSED_STATE_SET
     * @see #SELECTED_STATE_SET
     * @see #WINDOW_FOCUSED_STATE_SET
     */
    protected static final int[] FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET;
    /**
     * Indicates the view is enabled, focused, selected and its window
     * has the focus.
     *
     * @see #ENABLED_STATE_SET
     * @see #FOCUSED_STATE_SET
     * @see #SELECTED_STATE_SET
     * @see #WINDOW_FOCUSED_STATE_SET
     */
    protected static final int[] ENABLED_FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET;
    /**
     * Indicates the view is pressed and its window has the focus.
     *
     * @see #PRESSED_STATE_SET
     * @see #WINDOW_FOCUSED_STATE_SET
     */
    protected static final int[] PRESSED_WINDOW_FOCUSED_STATE_SET;
    /**
     * Indicates the view is pressed and selected.
     *
     * @see #PRESSED_STATE_SET
     * @see #SELECTED_STATE_SET
     */
    protected static final int[] PRESSED_SELECTED_STATE_SET;
    /**
     * Indicates the view is pressed, selected and its window has the focus.
     *
     * @see #PRESSED_STATE_SET
     * @see #SELECTED_STATE_SET
     * @see #WINDOW_FOCUSED_STATE_SET
     */
    protected static final int[] PRESSED_SELECTED_WINDOW_FOCUSED_STATE_SET;
    /**
     * Indicates the view is pressed and focused.
     *
     * @see #PRESSED_STATE_SET
     * @see #FOCUSED_STATE_SET
     */
    protected static final int[] PRESSED_FOCUSED_STATE_SET;
    /**
     * Indicates the view is pressed, focused and its window has the focus.
     *
     * @see #PRESSED_STATE_SET
     * @see #FOCUSED_STATE_SET
     * @see #WINDOW_FOCUSED_STATE_SET
     */
    protected static final int[] PRESSED_FOCUSED_WINDOW_FOCUSED_STATE_SET;
    /**
     * Indicates the view is pressed, focused and selected.
     *
     * @see #PRESSED_STATE_SET
     * @see #SELECTED_STATE_SET
     * @see #FOCUSED_STATE_SET
     */
    protected static final int[] PRESSED_FOCUSED_SELECTED_STATE_SET;
    /**
     * Indicates the view is pressed, focused, selected and its window has the focus.
     *
     * @see #PRESSED_STATE_SET
     * @see #FOCUSED_STATE_SET
     * @see #SELECTED_STATE_SET
     * @see #WINDOW_FOCUSED_STATE_SET
     */
    protected static final int[] PRESSED_FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET;
    /**
     * Indicates the view is pressed and enabled.
     *
     * @see #PRESSED_STATE_SET
     * @see #ENABLED_STATE_SET
     */
    protected static final int[] PRESSED_ENABLED_STATE_SET;
    /**
     * Indicates the view is pressed, enabled and its window has the focus.
     *
     * @see #PRESSED_STATE_SET
     * @see #ENABLED_STATE_SET
     * @see #WINDOW_FOCUSED_STATE_SET
     */
    protected static final int[] PRESSED_ENABLED_WINDOW_FOCUSED_STATE_SET;
    /**
     * Indicates the view is pressed, enabled and selected.
     *
     * @see #PRESSED_STATE_SET
     * @see #ENABLED_STATE_SET
     * @see #SELECTED_STATE_SET
     */
    protected static final int[] PRESSED_ENABLED_SELECTED_STATE_SET;
    /**
     * Indicates the view is pressed, enabled, selected and its window has the
     * focus.
     *
     * @see #PRESSED_STATE_SET
     * @see #ENABLED_STATE_SET
     * @see #SELECTED_STATE_SET
     * @see #WINDOW_FOCUSED_STATE_SET
     */
    protected static final int[] PRESSED_ENABLED_SELECTED_WINDOW_FOCUSED_STATE_SET;
    /**
     * Indicates the view is pressed, enabled and focused.
     *
     * @see #PRESSED_STATE_SET
     * @see #ENABLED_STATE_SET
     * @see #FOCUSED_STATE_SET
     */
    protected static final int[] PRESSED_ENABLED_FOCUSED_STATE_SET;
    /**
     * Indicates the view is pressed, enabled, focused and its window has the
     * focus.
     *
     * @see #PRESSED_STATE_SET
     * @see #ENABLED_STATE_SET
     * @see #FOCUSED_STATE_SET
     * @see #WINDOW_FOCUSED_STATE_SET
     */
    protected static final int[] PRESSED_ENABLED_FOCUSED_WINDOW_FOCUSED_STATE_SET;
    /**
     * Indicates the view is pressed, enabled, focused and selected.
     *
     * @see #PRESSED_STATE_SET
     * @see #ENABLED_STATE_SET
     * @see #SELECTED_STATE_SET
     * @see #FOCUSED_STATE_SET
     */
    protected static final int[] PRESSED_ENABLED_FOCUSED_SELECTED_STATE_SET;
    /**
     * Indicates the view is pressed, enabled, focused, selected and its window
     * has the focus.
     *
     * @see #PRESSED_STATE_SET
     * @see #ENABLED_STATE_SET
     * @see #SELECTED_STATE_SET
     * @see #FOCUSED_STATE_SET
     * @see #WINDOW_FOCUSED_STATE_SET
     */
    protected static final int[] PRESSED_ENABLED_FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET;
    
    /**
     * The order here is very important to {@link #getDrawableState()}
     */
    private static final int[][] VIEW_STATE_SETS;

    static final int VIEW_STATE_WINDOW_FOCUSED = 1;
    static final int VIEW_STATE_SELECTED = 1 << 1;
    static final int VIEW_STATE_FOCUSED = 1 << 2;
    static final int VIEW_STATE_ENABLED = 1 << 3;
    static final int VIEW_STATE_PRESSED = 1 << 4;
    static final int VIEW_STATE_ACTIVATED = 1 << 5;
    static final int VIEW_STATE_ACCELERATED = 1 << 6;
    static final int VIEW_STATE_HOVERED = 1 << 7;
    static final int VIEW_STATE_DRAG_CAN_ACCEPT = 1 << 8;
    static final int VIEW_STATE_DRAG_HOVERED = 1 << 9;

    static final int[] VIEW_STATE_IDS = new int[] {
        com.glview.R.attr.state_window_focused,    VIEW_STATE_WINDOW_FOCUSED,
        com.glview.R.attr.state_selected,          VIEW_STATE_SELECTED,
        com.glview.R.attr.state_focused,           VIEW_STATE_FOCUSED,
        com.glview.R.attr.state_enabled,           VIEW_STATE_ENABLED,
        com.glview.R.attr.state_pressed,           VIEW_STATE_PRESSED,
        com.glview.R.attr.state_activated,         VIEW_STATE_ACTIVATED,
        com.glview.R.attr.state_accelerated,       VIEW_STATE_ACCELERATED,
        com.glview.R.attr.state_hovered,           VIEW_STATE_HOVERED,
        com.glview.R.attr.state_drag_can_accept,   VIEW_STATE_DRAG_CAN_ACCEPT,
        com.glview.R.attr.state_drag_hovered,      VIEW_STATE_DRAG_HOVERED
    };
    
    static {
        if ((VIEW_STATE_IDS.length/2) != com.glview.R.styleable.ViewDrawableStates.length) {
            throw new IllegalStateException(
                    "VIEW_STATE_IDs array length does not match ViewDrawableStates style array");
        }
        int[] orderedIds = new int[VIEW_STATE_IDS.length];
        int drawStatesLength = com.glview.R.styleable.ViewDrawableStates.length;
        int viewStateIdsLength = 0;
        for (int i = 0; i < drawStatesLength; i++) {
            int viewState = com.glview.R.styleable.ViewDrawableStates[i];
            viewStateIdsLength = VIEW_STATE_IDS.length;
            for (int j = 0; j< viewStateIdsLength; j += 2) {
                if (VIEW_STATE_IDS[j] == viewState) {
                    orderedIds[i * 2] = viewState;
                    orderedIds[i * 2 + 1] = VIEW_STATE_IDS[j + 1];
                }
            }
        }
        final int NUM_BITS = VIEW_STATE_IDS.length / 2;
        VIEW_STATE_SETS = new int[1 << NUM_BITS][];
        int viewStateSetsLength = VIEW_STATE_SETS.length;
        int orderedIdsLength = 0;
        for (int i = 0; i < viewStateSetsLength; i++) {
            int numBits = Integer.bitCount(i);
            int[] set = new int[numBits];
            int pos = 0;
            orderedIdsLength = orderedIds.length;
            for (int j = 0; j < orderedIdsLength; j += 2) {
                if ((i & orderedIds[j+1]) != 0) {
                    set[pos++] = orderedIds[j];
                }
            }
            VIEW_STATE_SETS[i] = set;
        }

        EMPTY_STATE_SET = VIEW_STATE_SETS[0];
        WINDOW_FOCUSED_STATE_SET = VIEW_STATE_SETS[VIEW_STATE_WINDOW_FOCUSED];
        SELECTED_STATE_SET = VIEW_STATE_SETS[VIEW_STATE_SELECTED];
        SELECTED_WINDOW_FOCUSED_STATE_SET = VIEW_STATE_SETS[
                VIEW_STATE_WINDOW_FOCUSED | VIEW_STATE_SELECTED];
        FOCUSED_STATE_SET = VIEW_STATE_SETS[VIEW_STATE_FOCUSED];
        FOCUSED_WINDOW_FOCUSED_STATE_SET = VIEW_STATE_SETS[
                VIEW_STATE_WINDOW_FOCUSED | VIEW_STATE_FOCUSED];
        FOCUSED_SELECTED_STATE_SET = VIEW_STATE_SETS[
                VIEW_STATE_SELECTED | VIEW_STATE_FOCUSED];
        FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET = VIEW_STATE_SETS[
                VIEW_STATE_WINDOW_FOCUSED | VIEW_STATE_SELECTED
                | VIEW_STATE_FOCUSED];
        ENABLED_STATE_SET = VIEW_STATE_SETS[VIEW_STATE_ENABLED];
        ENABLED_WINDOW_FOCUSED_STATE_SET = VIEW_STATE_SETS[
                VIEW_STATE_WINDOW_FOCUSED | VIEW_STATE_ENABLED];
        ENABLED_SELECTED_STATE_SET = VIEW_STATE_SETS[
                VIEW_STATE_SELECTED | VIEW_STATE_ENABLED];
        ENABLED_SELECTED_WINDOW_FOCUSED_STATE_SET = VIEW_STATE_SETS[
                VIEW_STATE_WINDOW_FOCUSED | VIEW_STATE_SELECTED
                | VIEW_STATE_ENABLED];
        ENABLED_FOCUSED_STATE_SET = VIEW_STATE_SETS[
                VIEW_STATE_FOCUSED | VIEW_STATE_ENABLED];
        ENABLED_FOCUSED_WINDOW_FOCUSED_STATE_SET = VIEW_STATE_SETS[
                VIEW_STATE_WINDOW_FOCUSED | VIEW_STATE_FOCUSED
                | VIEW_STATE_ENABLED];
        ENABLED_FOCUSED_SELECTED_STATE_SET = VIEW_STATE_SETS[
                VIEW_STATE_SELECTED | VIEW_STATE_FOCUSED
                | VIEW_STATE_ENABLED];
        ENABLED_FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET = VIEW_STATE_SETS[
                VIEW_STATE_WINDOW_FOCUSED | VIEW_STATE_SELECTED
                | VIEW_STATE_FOCUSED| VIEW_STATE_ENABLED];

        PRESSED_STATE_SET = VIEW_STATE_SETS[VIEW_STATE_PRESSED];
        PRESSED_WINDOW_FOCUSED_STATE_SET = VIEW_STATE_SETS[
                VIEW_STATE_WINDOW_FOCUSED | VIEW_STATE_PRESSED];
        PRESSED_SELECTED_STATE_SET = VIEW_STATE_SETS[
                VIEW_STATE_SELECTED | VIEW_STATE_PRESSED];
        PRESSED_SELECTED_WINDOW_FOCUSED_STATE_SET = VIEW_STATE_SETS[
                VIEW_STATE_WINDOW_FOCUSED | VIEW_STATE_SELECTED
                | VIEW_STATE_PRESSED];
        PRESSED_FOCUSED_STATE_SET = VIEW_STATE_SETS[
                VIEW_STATE_FOCUSED | VIEW_STATE_PRESSED];
        PRESSED_FOCUSED_WINDOW_FOCUSED_STATE_SET = VIEW_STATE_SETS[
                VIEW_STATE_WINDOW_FOCUSED | VIEW_STATE_FOCUSED
                | VIEW_STATE_PRESSED];
        PRESSED_FOCUSED_SELECTED_STATE_SET = VIEW_STATE_SETS[
                VIEW_STATE_SELECTED | VIEW_STATE_FOCUSED
                | VIEW_STATE_PRESSED];
        PRESSED_FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET = VIEW_STATE_SETS[
                VIEW_STATE_WINDOW_FOCUSED | VIEW_STATE_SELECTED
                | VIEW_STATE_FOCUSED | VIEW_STATE_PRESSED];
        PRESSED_ENABLED_STATE_SET = VIEW_STATE_SETS[
                VIEW_STATE_ENABLED | VIEW_STATE_PRESSED];
        PRESSED_ENABLED_WINDOW_FOCUSED_STATE_SET = VIEW_STATE_SETS[
                VIEW_STATE_WINDOW_FOCUSED | VIEW_STATE_ENABLED
                | VIEW_STATE_PRESSED];
        PRESSED_ENABLED_SELECTED_STATE_SET = VIEW_STATE_SETS[
                VIEW_STATE_SELECTED | VIEW_STATE_ENABLED
                | VIEW_STATE_PRESSED];
        PRESSED_ENABLED_SELECTED_WINDOW_FOCUSED_STATE_SET = VIEW_STATE_SETS[
                VIEW_STATE_WINDOW_FOCUSED | VIEW_STATE_SELECTED
                | VIEW_STATE_ENABLED | VIEW_STATE_PRESSED];
        PRESSED_ENABLED_FOCUSED_STATE_SET = VIEW_STATE_SETS[
                VIEW_STATE_FOCUSED | VIEW_STATE_ENABLED
                | VIEW_STATE_PRESSED];
        PRESSED_ENABLED_FOCUSED_WINDOW_FOCUSED_STATE_SET = VIEW_STATE_SETS[
                VIEW_STATE_WINDOW_FOCUSED | VIEW_STATE_FOCUSED
                | VIEW_STATE_ENABLED | VIEW_STATE_PRESSED];
        PRESSED_ENABLED_FOCUSED_SELECTED_STATE_SET = VIEW_STATE_SETS[
                VIEW_STATE_SELECTED | VIEW_STATE_FOCUSED
                | VIEW_STATE_ENABLED | VIEW_STATE_PRESSED];
        PRESSED_ENABLED_FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET = VIEW_STATE_SETS[
                VIEW_STATE_WINDOW_FOCUSED | VIEW_STATE_SELECTED
                | VIEW_STATE_FOCUSED| VIEW_STATE_ENABLED
                | VIEW_STATE_PRESSED];
    }
    
    /**
     * Temporary Rect currently for use in setBackground().  This will probably
     * be extended in the future to hold our own class with more than just
     * a Rect. :)
     */
    static final ThreadLocal<Rect> sThreadLocal = new ThreadLocal<Rect>();
    
    /**
     * Map used to store views' tags.
     */
    private SparseArray<Object> mKeyedTags;
    
    
    public View(Context context){
    	mContext = context;
//        mResources = context != null ? context.getResources() : null;
    	mPrivateFlags |= PFLAG_HAS_BOUNDS; 
    	mViewFlags = SOUND_EFFECTS_ENABLED | HAPTIC_FEEDBACK_ENABLED;
    }

    /**
     * Constructor that is called when inflating a view from XML. This is called
     * when a view is being constructed from an XML file, supplying attributes
     * that were specified in the XML file. This version uses a default style of
     * 0, so the only attribute values applied are those in the Context's Theme
     * and the given AttributeSet.
     *
     * <p>
     * The method onFinishInflate() will be called after all children have been
     * added.
     *
     * @param context The Context the view is running in, through which it can
     *        access the current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view.
     * @see #GLView(Context, AttributeSet, int)
     */
    public View(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    /**
     * Perform inflation from XML and apply a class-specific base style from a
     * theme attribute. This constructor of View allows subclasses to use their
     * own base style when they are inflating. For example, a Button class's
     * constructor would call this version of the super class constructor and
     * supply <code>R.attr.buttonStyle</code> for <var>defStyleAttr</var>; this
     * allows the theme's button style to modify all of the base view attributes
     * (in particular its background) as well as the Button class's attributes.
     *
     * @param context The Context the view is running in, through which it can
     *        access the current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view.
     * @param defStyleAttr An attribute in the current theme that contains a
     *        reference to a style resource that supplies default values for
     *        the view. Can be 0 to not look for defaults.
     * @see #View(Context, AttributeSet)
     */
    public View(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    /**
     * Perform inflation from XML and apply a class-specific base style from a
     * theme attribute or style resource. This constructor of View allows
     * subclasses to use their own base style when they are inflating.
     * <p>
     * When determining the final value of a particular attribute, there are
     * four inputs that come into play:
     * <ol>
     * <li>Any attribute values in the given AttributeSet.
     * <li>The style resource specified in the AttributeSet (named "style").
     * <li>The default style specified by <var>defStyleAttr</var>.
     * <li>The default style specified by <var>defStyleRes</var>.
     * <li>The base values in this theme.
     * </ol>
     * <p>
     * Each of these inputs is considered in-order, with the first listed taking
     * precedence over the following ones. In other words, if in the
     * AttributeSet you have supplied <code>&lt;Button * textColor="#ff000000"&gt;</code>
     * , then the button's text will <em>always</em> be black, regardless of
     * what is specified in any of the styles.
     *
     * @param context The Context the view is running in, through which it can
     *        access the current theme, resources, etc.
     * @param attrs The attributes of the XML tag that is inflating the view.
     * @param defStyleAttr An attribute in the current theme that contains a
     *        reference to a style resource that supplies default values for
     *        the view. Can be 0 to not look for defaults.
     * @param defStyleRes A resource identifier of a style resource that
     *        supplies default values for the view, used only if
     *        defStyleAttr is 0 or can not be found in the theme. Can be 0
     *        to not look for defaults.
     * @see #View(Context, AttributeSet, int)
     */
    public View(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        this(context);

		Drawable background = null;

        int leftPadding = -1;
        int topPadding = -1;
        int rightPadding = -1;
        int bottomPadding = -1;
        int startPadding = UNDEFINED_PADDING;
        int endPadding = UNDEFINED_PADDING;

        int padding = -1;

        int viewFlagValues = 0;
        int viewFlagMasks = 0;

        boolean setScrollContainer = false;

        int x = 0;
        int y = 0;

        float tx = 0;
        float ty = 0;
        float tz = 0;
        float elevation = 0;
        float rotation = 0;
        float rotationX = 0;
        float rotationY = 0;
        float sx = 1f;
        float sy = 1f;
        boolean transformSet = false;

        int scrollbarStyle = SCROLLBARS_INSIDE_OVERLAY;
        int overScrollMode = mOverScrollMode;
        boolean initializeScrollbars = false;

        boolean startPaddingDefined = false;
        boolean endPaddingDefined = false;
        boolean leftPaddingDefined = false;
        boolean rightPaddingDefined = false;

		// final int targetSdkVersion =
		// context.getApplicationInfo().targetSdkVersion;

        final TypedArray a = context.obtainStyledAttributes(attrs,
				com.glview.R.styleable.View, defStyleAttr, defStyleRes);
		final int N = a.getIndexCount();
		for (int i = 0; i < N; i++) {
			int attr = a.getIndex(i);
			if (attr == com.glview.R.styleable.View_background) {
				background = GLContext.get().getResources().getDrawable(a.getResourceId(attr, 0));
			} else if (attr == com.glview.R.styleable.View_padding) {
				padding = a.getDimensionPixelSize(attr, -1);
                mUserPaddingLeftInitial = padding;
                mUserPaddingRightInitial = padding;
                leftPaddingDefined = true;
                rightPaddingDefined = true;
			} else if (attr == com.glview.R.styleable.View_paddingLeft) {
				leftPadding = a.getDimensionPixelSize(attr, -1);
                mUserPaddingLeftInitial = leftPadding;
                leftPaddingDefined = true;
			} else if (attr == com.glview.R.styleable.View_paddingTop) {
				topPadding = a.getDimensionPixelSize(attr, -1);
			} else if (attr == com.glview.R.styleable.View_paddingRight) {
				rightPadding = a.getDimensionPixelSize(attr, -1);
                mUserPaddingRightInitial = rightPadding;
                rightPaddingDefined = true;
			} else if (attr == com.glview.R.styleable.View_paddingBottom) {
				bottomPadding = a.getDimensionPixelSize(attr, -1);
			} else if (attr == com.glview.R.styleable.View_paddingStart) {
				startPadding = a.getDimensionPixelSize(attr, UNDEFINED_PADDING);
                startPaddingDefined = (startPadding != UNDEFINED_PADDING);
			} else if (attr == com.glview.R.styleable.View_paddingEnd) {
				endPadding = a.getDimensionPixelSize(attr, UNDEFINED_PADDING);
                endPaddingDefined = (endPadding != UNDEFINED_PADDING);
			} else if (attr == com.glview.R.styleable.View_scrollX) {
				x = a.getDimensionPixelOffset(attr, 0);
			} else if (attr == com.glview.R.styleable.View_scrollY) {
				y = a.getDimensionPixelOffset(attr, 0);
			} else if (attr == com.glview.R.styleable.View_alpha) {
				setAlpha(a.getFloat(attr, 1f));
			} else if (attr == com.glview.R.styleable.View_transformPivotX) {
				// setPivotX(a.getDimensionPixelOffset(attr, 0));
			} else if (attr == com.glview.R.styleable.View_transformPivotY) {
				// setPivotY(a.getDimensionPixelOffset(attr, 0));
			} else if (attr == com.glview.R.styleable.View_translationX) {
				tx = a.getDimensionPixelOffset(attr, 0);
				transformSet = true;
			} else if (attr == com.glview.R.styleable.View_translationY) {
				ty = a.getDimensionPixelOffset(attr, 0);
				transformSet = true;
			} else if (attr == com.glview.R.styleable.View_rotation) {
				rotation = a.getFloat(attr, 0);
				transformSet = true;
			} else if (attr == com.glview.R.styleable.View_rotationX) {
				rotationX = a.getFloat(attr, 0);
				transformSet = true;
			} else if (attr == com.glview.R.styleable.View_rotationY) {
				rotationY = a.getFloat(attr, 0);
				transformSet = true;
			} else if (attr == com.glview.R.styleable.View_scaleX) {
				sx = a.getFloat(attr, 1f);
				transformSet = true;
			} else if (attr == com.glview.R.styleable.View_scaleY) {
				sy = a.getFloat(attr, 1f);
				transformSet = true;
			} else if (attr == com.glview.R.styleable.View_id) {
				mID = a.getResourceId(attr, NO_ID);
			} else if (attr == com.glview.R.styleable.View_tag) {
				mTag = a.getText(attr);
			} else if (attr == com.glview.R.styleable.View_focusable) {
				if (a.getBoolean(attr, false)) {
					viewFlagValues |= FOCUSABLE;
					viewFlagMasks |= FOCUSABLE_MASK;
				}
			} else if (attr == com.glview.R.styleable.View_focusableInTouchMode) {
				 if (a.getBoolean(attr, false)) {
					 viewFlagValues |= FOCUSABLE_IN_TOUCH_MODE | FOCUSABLE;
					 viewFlagMasks |= FOCUSABLE_IN_TOUCH_MODE | FOCUSABLE_MASK;
				 }
			} else if (attr == com.glview.R.styleable.View_clickable) {
				if (a.getBoolean(attr, false)) {
					viewFlagValues |= CLICKABLE;
					viewFlagMasks |= CLICKABLE;
				}
			} else if (attr == com.glview.R.styleable.View_longClickable) {
				if (a.getBoolean(attr, false)) {
					viewFlagValues |= LONG_CLICKABLE;
					viewFlagMasks |= LONG_CLICKABLE;
				}
			} else if (attr == com.glview.R.styleable.View_duplicateParentState) {
				if (a.getBoolean(attr, false)) {
					viewFlagValues |= DUPLICATE_PARENT_STATE;
					viewFlagMasks |= DUPLICATE_PARENT_STATE;
				}
			} else if (attr == com.glview.R.styleable.View_visibility) {
				final int visibility = a.getInt(attr, 0);
				if (visibility != 0) {
					viewFlagValues |= VISIBILITY_FLAGS[visibility];
					viewFlagMasks |= VISIBILITY_MASK;
				}
			} else if (attr == com.glview.R.styleable.View_contentDescription) {
				// setContentDescription(a.getString(attr));
			} else if (attr == com.glview.R.styleable.View_soundEffectsEnabled) {
				 if (!a.getBoolean(attr, true)) {
					 viewFlagValues &= ~SOUND_EFFECTS_ENABLED;
					 viewFlagMasks |= SOUND_EFFECTS_ENABLED;
				 }
			} else if (attr == com.glview.R.styleable.View_scrollbars) {
                final int scrollbars = a.getInt(attr, SCROLLBARS_NONE);
                if (scrollbars != SCROLLBARS_NONE) {
                    viewFlagValues |= scrollbars;
                    viewFlagMasks |= SCROLLBARS_MASK;
                    initializeScrollbars = true;
                }
			} else if (attr == com.glview.R.styleable.View_keepScreenOn) {
				if (a.getBoolean(attr, false)) {
                    viewFlagValues |= KEEP_SCREEN_ON;
                    viewFlagMasks |= KEEP_SCREEN_ON;
                }
			} else if (attr == com.glview.R.styleable.View_nextFocusLeft) {
				 mNextFocusLeftId = a.getResourceId(attr, View.NO_ID);
			} else if (attr == com.glview.R.styleable.View_nextFocusRight) {
				 mNextFocusRightId = a.getResourceId(attr, View.NO_ID);
			} else if (attr == com.glview.R.styleable.View_nextFocusUp) {
				 mNextFocusUpId = a.getResourceId(attr, View.NO_ID);
			} else if (attr == com.glview.R.styleable.View_nextFocusDown) {
				 mNextFocusDownId = a.getResourceId(attr, View.NO_ID);
			} else if (attr == com.glview.R.styleable.View_nextFocusForward) {
				 mNextFocusForwardId = a.getResourceId(attr, View.NO_ID);
			} else if (attr == com.glview.R.styleable.View_minWidth) {
				mMinWidth = a.getDimensionPixelSize(attr, 0);
			} else if (attr == com.glview.R.styleable.View_minHeight) {
				mMinHeight = a.getDimensionPixelSize(attr, 0);
			} else if (attr == com.glview.R.styleable.View_onClick) {
                if (context.isRestricted()) {
                    throw new IllegalStateException("The android:onClick attribute cannot "
                            + "be used within a restricted context");
                }

                final String handlerName = a.getString(attr);
                if (handlerName != null) {
                    setOnClickListener(new OnClickListener() {
                        private Method mHandler;

                        public void onClick(View v) {
                            if (mHandler == null) {
                                try {
                                    mHandler = getContext().getClass().getMethod(handlerName,
                                            View.class);
                                } catch (NoSuchMethodException e) {
                                    int id = getId();
                                    String idText = id == NO_ID ? "" : " with id '"
                                            + getContext().getResources().getResourceEntryName(
                                                id) + "'";
                                    throw new IllegalStateException("Could not find a method " +
                                            handlerName + "(View) in the activity "
                                            + getContext().getClass() + " for onClick handler"
                                            + " on view " + View.this.getClass() + idText, e);
                                }
                            }

                            try {
                                mHandler.invoke(getContext(), View.this);
                            } catch (IllegalAccessException e) {
                                throw new IllegalStateException("Could not execute non "
                                        + "public method of the activity", e);
                            } catch (InvocationTargetException e) {
                                throw new IllegalStateException("Could not execute "
                                        + "method of the activity", e);
                            }
                        }
                    });
                }
			} else if (attr == com.glview.R.styleable.View_overScrollMode) {
                overScrollMode = a.getInt(attr, OVER_SCROLL_IF_CONTENT_SCROLLS);
			} else if (attr == com.glview.R.styleable.View_stateListAnimator) {
				setStateListAnimator(AnimatorInflater.loadStateListAnimator(context,
                        a.getResourceId(attr, 0)));
			} else if (attr == com.glview.R.styleable.View_transitionName) {
				 setTransitionName(a.getString(attr));
			}
		}
		
		setOverScrollMode(overScrollMode);

        // Cache start/end user padding as we cannot fully resolve padding here (we dont have yet
        // the resolved layout direction). Those cached values will be used later during padding
        // resolution.
        mUserPaddingStart = startPadding;
        mUserPaddingEnd = endPadding;

        if (background != null) {
            setBackground(background);
        }
        
        // setBackground above will record that padding is currently provided by the background.
        // If we have padding specified via xml, record that here instead and use it.
        mLeftPaddingDefined = leftPaddingDefined;
        mRightPaddingDefined = rightPaddingDefined;

        if (padding >= 0) {
            leftPadding = padding;
            topPadding = padding;
            rightPadding = padding;
            bottomPadding = padding;
            mUserPaddingLeftInitial = padding;
            mUserPaddingRightInitial = padding;
        }
        
        // Jelly Bean MR1 and after case: if start/end defined, they will override any left/right
        // values defined. Otherwise, left /right values are used.
        // Padding from the background drawable is stored at this point in mUserPaddingLeftInitial
        // and mUserPaddingRightInitial) so drawable padding will be used as ultimate default if
        // defined.
        final boolean hasRelativePadding = startPaddingDefined || endPaddingDefined;

        if (mLeftPaddingDefined && !hasRelativePadding) {
            mUserPaddingLeftInitial = leftPadding;
        }
        if (mRightPaddingDefined && !hasRelativePadding) {
            mUserPaddingRightInitial = rightPadding;
        }
        
        internalSetPadding(
                mUserPaddingLeftInitial,
                topPadding >= 0 ? topPadding : mPaddingTop,
                mUserPaddingRightInitial,
                bottomPadding >= 0 ? bottomPadding : mPaddingBottom);

        if (viewFlagMasks != 0) {
            setFlags(viewFlagValues, viewFlagMasks);
        }
        
        if (initializeScrollbars) {
            initializeScrollbarsInternal(a);
        }
        
		a.recycle();
		
		// Needs to be called after mViewFlags is set
        if (scrollbarStyle != SCROLLBARS_INSIDE_OVERLAY) {
            recomputePadding();
        }

        if (x != 0 || y != 0) {
            scrollTo(x, y);
        }

        if (transformSet) {
            setTranslationX(tx);
            setTranslationY(ty);
            setTranslationZ(tz);
//            setElevation(elevation);
            setRotation(rotation);
            setRotationX(rotationX);
            setRotationY(rotationY);
            setScaleX(sx);
            setScaleY(sy);
        }
    }

    /**
     * Non-public constructor for use in testing
     */
    View() {
    }

    // Sets the visiblity of this GLView (either GLView.VISIBLE or
    // GLView.INVISIBLE).
    public void setVisibility(int visibility) {
    	setFlags(visibility, VISIBILITY_MASK);
        if (mBackground != null) mBackground.setVisible(visibility == VISIBLE, false);
    }

    // Returns GLView.VISIBLE or GLView.INVISIBLE
    public int getVisibility() {
        return mViewFlags & VISIBILITY_MASK;
    }
    
    /**
     * @param info the {@link android.view.View.AttachInfo} to associated with
     *        this view
     */
    void dispatchAttachedToWindow(AttachInfo info, int visibility) {
    	mAttachInfo = info;
    	// FIXME for getDrawingCache
    	mViewRootImpl = mAttachInfo.mViewRootImpl;
    	if (mOverlay != null) {
            mOverlay.getOverlayView().dispatchAttachedToWindow(info, visibility);
        }
    	mWindowAttachCount ++;
    	
    	if (mFloatingTreeObserver != null) {
            info.mTreeObserver.merge(mFloatingTreeObserver);
            mFloatingTreeObserver = null;
        }
    	
    	performCollectViewAttributes(mAttachInfo, visibility);
    	onAttachedToWindow();

    	ListenerInfo li = mListenerInfo;
        final CopyOnWriteArrayList<OnAttachStateChangeListener> listeners =
                li != null ? li.mOnAttachStateChangeListeners : null;
        if (listeners != null && listeners.size() > 0) {
            // NOTE: because of the use of CopyOnWriteArrayList, we *must* use an iterator to
            // perform the dispatching. The iterator is a safe guard against listeners that
            // could mutate the list by calling the various add/remove methods. This prevents
            // the array from being modified while we iterate it.
            for (OnAttachStateChangeListener listener : listeners) {
                listener.onViewAttachedToWindow(this);
            }
        }
        
    	needGlobalAttributesUpdate(false);
    }
    
    void dispatchDetachedFromWindow() {
    	onDetachedFromWindow();
    	onDetachedFromWindowInternal();
    	
    	ListenerInfo li = mListenerInfo;
        final CopyOnWriteArrayList<OnAttachStateChangeListener> listeners =
                li != null ? li.mOnAttachStateChangeListeners : null;
        if (listeners != null && listeners.size() > 0) {
            // NOTE: because of the use of CopyOnWriteArrayList, we *must* use an iterator to
            // perform the dispatching. The iterator is a safe guard against listeners that
            // could mutate the list by calling the various add/remove methods. This prevents
            // the array from being modified while we iterate it.
            for (OnAttachStateChangeListener listener : listeners) {
                listener.onViewDetachedFromWindow(this);
            }
        }
        
    	mAttachInfo = null;
    	if (mOverlay != null) {
            mOverlay.getOverlayView().dispatchDetachedFromWindow();
        }
    }
    
    /**
     * @return The number of times this view has been attached to a window
     */
    protected int getWindowAttachCount() {
        return mWindowAttachCount;
    }

    public int getWidth() {
        return mRight - mLeft;
    }

    public int getHeight() {
        return mBottom - mTop;
    }
    
    /**
     * Changes the selection state of this view. A view can be selected or not.
     * Note that selection is not the same as focus. Views are typically
     * selected in the context of an AdapterView like ListView or GridView;
     * the selected view is the view that is highlighted.
     *
     * @param selected true if the view must be selected, false otherwise
     */
    public void setSelected(boolean selected) {
        //noinspection DoubleNegation
        if (((mPrivateFlags & PFLAG_SELECTED) != 0) != selected) {
            mPrivateFlags = (mPrivateFlags & ~PFLAG_SELECTED) | (selected ? PFLAG_SELECTED : 0);
            if (!selected) resetPressedState();
            invalidate(true);
            refreshDrawableState();
            dispatchSetSelected(selected);
//            notifyViewAccessibilityStateChangedIfNeeded(
//                    AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED);
        }
    }

    /**
     * Dispatch setSelected to all of this View's children.
     *
     * @see #setSelected(boolean)
     *
     * @param selected The new selected state
     */
    protected void dispatchSetSelected(boolean selected) {
    }

    /**
     * Indicates the selection state of this view.
     *
     * @return true if the view is selected, false otherwise
     */
    public boolean isSelected() {
        return (mPrivateFlags & PFLAG_SELECTED) != 0;
    }

    /**
     * Changes the activated state of this view. A view can be activated or not.
     * Note that activation is not the same as selection.  Selection is
     * a transient property, representing the view (hierarchy) the user is
     * currently interacting with.  Activation is a longer-term state that the
     * user can move views in and out of.  For example, in a list view with
     * single or multiple selection enabled, the views in the current selection
     * set are activated.  (Um, yeah, we are deeply sorry about the terminology
     * here.)  The activated state is propagated down to children of the view it
     * is set on.
     *
     * @param activated true if the view must be activated, false otherwise
     */
    public void setActivated(boolean activated) {
        //noinspection DoubleNegation
        if (((mPrivateFlags & PFLAG_ACTIVATED) != 0) != activated) {
            mPrivateFlags = (mPrivateFlags & ~PFLAG_ACTIVATED) | (activated ? PFLAG_ACTIVATED : 0);
            invalidate(true);
            refreshDrawableState();
            dispatchSetActivated(activated);
        }
    }

    /**
     * Dispatch setActivated to all of this View's children.
     *
     * @see #setActivated(boolean)
     *
     * @param activated The new activated state
     */
    protected void dispatchSetActivated(boolean activated) {
    }

    /**
     * Indicates the activation state of this view.
     *
     * @return true if the view is activated, false otherwise
     */
    public boolean isActivated() {
        return (mPrivateFlags & PFLAG_ACTIVATED) != 0;
    }
    
    /**
     * Returns the ViewTreeObserver for this view's hierarchy. The view tree
     * observer can be used to get notifications when global events, like
     * layout, happen.
     *
     * The returned ViewTreeObserver observer is not guaranteed to remain
     * valid for the lifetime of this View. If the caller of this method keeps
     * a long-lived reference to ViewTreeObserver, it should always check for
     * the return value of {@link ViewTreeObserver#isAlive()}.
     *
     * @return The ViewTreeObserver for this view's hierarchy.
     */
    public ViewTreeObserver getViewTreeObserver() {
        if (mAttachInfo != null) {
            return mAttachInfo.mTreeObserver;
        }
        if (mFloatingTreeObserver == null) {
            mFloatingTreeObserver = new ViewTreeObserver();
        }
        return mFloatingTreeObserver;
    }
    
    /**
     * <p>Finds the topmost view in the current view hierarchy.</p>
     *
     * @return the topmost view containing this view
     */
    public View getRootView() {
        if (mAttachInfo != null) {
            final View v = mAttachInfo.mRootView;
            if (v != null) {
                return v;
            }
        }

        View parent = this;

        while (parent.mParent != null) {
            parent = (View) parent.mParent;
        }

        return parent;
    }

    /**
     * @hide
     * @return
     */
    public GLRootView getViewRootImpl() {
        return mAttachInfo != null ? mAttachInfo.mViewRootImpl : null;
    }
    
    /**
     * <p>Computes the coordinates of this view on the screen. The argument
     * must be an array of two integers. After the method returns, the array
     * contains the x and y location in that order.</p>
     *
     * @param location an array of two integers in which to hold the coordinates
     */
    public void getLocationOnScreen(int[] location){
    	if(mAttachInfo == null){
    		location[0] = location[1] = 0;
    		return;
    	}
    	getLocationInWindow(location);
    	
    	int[] glSurfaceViewLocation = new int[2];
    	mAttachInfo.mViewRootImpl.getLocationOnScreen(glSurfaceViewLocation);
    	location[0] += glSurfaceViewLocation[0];
    	location[1] += glSurfaceViewLocation[1];
    }
    
    /**
     * <p>Computes the coordinates of this view in its window. The argument
     * must be an array of two integers. After the method returns, the array
     * contains the x and y location in that order.</p>
     *
     * @param location an array of two integers in which to hold the coordinates
     */
    public void getLocationInWindow(int[] location) {
    	
        if (location == null || location.length < 2) {
            throw new IllegalArgumentException("location must be an array of two integers");
        }

        if (mAttachInfo == null) {
            // When the view is not attached to a window, this method does not make sense
            location[0] = location[1] = 0;
            return;
        }
        
        mAttachInfo.mViewRootImpl.getLocationInWindow(location);

//        float[] position = mAttachInfo.mTmpTransformLocation;
        float[] position = new float[2];
//        position[0] = position[1] = 0.0f;

        position[0] = location != null ? location[0] : 0.0f;
        position[1] = location != null ? location[1] : 0.0f;
        
//        if (!hasIdentityMatrix()) {
//            getMatrix().mapPoints(position);
//        }

        position[0] += mLeft;
        position[1] += mTop;

        ViewGroup viewParent = mParent;
        while (viewParent instanceof View) {
            final View view = (View) viewParent;

            position[0] -= view.mScrollX;
            position[1] -= view.mScrollY;

//            if (!view.hasIdentityMatrix()) {
//                view.getMatrix().mapPoints(position);
//            }

            position[0] += view.mLeft;
            position[1] += view.mTop;

            viewParent = view.mParent;
         }

        location[0] = (int) (position[0] + 0.5f);
        location[1] = (int) (position[1] + 0.5f);
    }

    // Request re-rendering of the view hierarchy.
    // This is used for animation or when the contents changed.
    /*public void invalidate() {
        GLRoot root = getGLRoot();
        if (root != null) root.requestRender();
    }*/
    public void setBlurOffset(int offset)
    {
    	return;
    }
    
    // Request re-layout of the view hierarchy.
    public void requestLayout() {
    	mPrivateFlags |= PFLAG_FORCE_LAYOUT;
        mPrivateFlags |= PFLAG_INVALIDATED;
        if (mParent != null) {
        	mParent.requestLayout();
        } else if(mAttachInfo != null){
            // Is this a content pane ?
        	mAttachInfo.mViewRootImpl.requestLayoutGLContentView();
        }
    }
    
    /**
     * Forces this view to be laid out during the next layout pass.
     * This method does not call requestLayout() or forceLayout()
     * on the parent.
     */
    public void forceLayout() {
        mPrivateFlags |= PFLAG_FORCE_LAYOUT;
        mPrivateFlags |= PFLAG_INVALIDATED;
    }
    
    void pdraw(GLCanvas canvas) {
    	draw(canvas);
    }

    public void draw(GLCanvas canvas) {
        canvas.save();
        drawAnimation(canvas);
        
        //render background
        drawBackground(canvas);
        
        if(enableFillColor) renderColorDebug(canvas, fillColor);
        
        //render child or other operator
        onDraw(canvas);
        dispatchDraw(canvas);
        
        onDrawScrollBars(canvas);
        
        canvas.restore();
        if (mOverlay != null && !mOverlay.isEmpty()) {
            mOverlay.getOverlayView().dispatchDraw(canvas);
        }
    }
    
    /**
     * If the View draws content inside its padding and enables fading edges,
     * it needs to support padding offsets. Padding offsets are added to the
     * fading edges to extend the length of the fade so that it covers pixels
     * drawn inside the padding.
     *
     * Subclasses of this class should override this method if they need
     * to draw content inside the padding.
     *
     * @return True if padding offset must be applied, false otherwise.
     *
     * @see #getLeftPaddingOffset()
     * @see #getRightPaddingOffset()
     * @see #getTopPaddingOffset()
     * @see #getBottomPaddingOffset()
     *
     * @since CURRENT
     */
    protected boolean isPaddingOffsetRequired() {
        return false;
    }

    /**
     * Amount by which to extend the left fading region. Called only when
     * {@link #isPaddingOffsetRequired()} returns true.
     *
     * @return The left padding offset in pixels.
     *
     * @see #isPaddingOffsetRequired()
     *
     * @since CURRENT
     */
    protected int getLeftPaddingOffset() {
        return 0;
    }

    /**
     * Amount by which to extend the right fading region. Called only when
     * {@link #isPaddingOffsetRequired()} returns true.
     *
     * @return The right padding offset in pixels.
     *
     * @see #isPaddingOffsetRequired()
     *
     * @since CURRENT
     */
    protected int getRightPaddingOffset() {
        return 0;
    }

    /**
     * Amount by which to extend the top fading region. Called only when
     * {@link #isPaddingOffsetRequired()} returns true.
     *
     * @return The top padding offset in pixels.
     *
     * @see #isPaddingOffsetRequired()
     *
     * @since CURRENT
     */
    protected int getTopPaddingOffset() {
        return 0;
    }

    /**
     * Amount by which to extend the bottom fading region. Called only when
     * {@link #isPaddingOffsetRequired()} returns true.
     *
     * @return The bottom padding offset in pixels.
     *
     * @see #isPaddingOffsetRequired()
     *
     * @since CURRENT
     */
    protected int getBottomPaddingOffset() {
        return 0;
    }
    
    protected void drawAnimation(GLCanvas canvas){
        boolean more = false;
        if(mCurrentAnimation != null){
        	Animation a = mCurrentAnimation;
        	long currentTime = getDrawingTime();
        	initAnimation(a, currentTime);
        	more = a.getTransformation(currentTime, 1f, canvas);
        }
        
        if(more){
        	invalidate();
        } else if (mCurrentAnimation != null && !mCurrentAnimation.getFillAfter()) {
        	clearAnimation();
        }
        if (!more && mCurrentAnimation != null && mParent != null) {
        	mParent.finishAnimatingView(this, mCurrentAnimation);
        }
    }
    
    /**
     * <p>Return the time at which the drawing of the view hierarchy started.</p>
     *
     * @return the drawing start time in milliseconds
     */
    public long getDrawingTime() {
        return mAttachInfo != null ? mAttachInfo.mDrawingTime : 0;
    }
    
    public void outOfRenderRect(){
    	if(mCurrentAnimation != null){
        	initAnimation(mCurrentAnimation, AnimationUtils.currentAnimationTimeMillis());
    	}
    }
    
    protected void initAnimation(Animation a, long currentTime){
    	boolean initialized = a.isInitialized();
    	if(!initialized){
    		a.setStartTime(currentTime);
    		ViewGroup parent = getParent();
    		int parentWidth = parent != null ? parent.getWidth() : 0;
    		int parentHeight = parent != null ? parent.getHeight() : 0;
            a.initialize(mRight - mLeft, mBottom - mTop, parentWidth, parentHeight);
    		onAnimationStart();
    	}
    }
    
    GLPaint mDebugPaint = new GLPaint();
    protected void renderColorDebug(GLCanvas canvas, int debugColor){
    	mDebugPaint.setColor(debugColor);
    	canvas.drawRect(mScrollX, mScrollY, mScrollX + getWidth(), mScrollY + getHeight(), mDebugPaint);
    }
    
    boolean enableFillColor = false;
    int fillColor = 0xffffffff;
    
    public void enableFillColor(boolean enable, int fillColor){
    	this.enableFillColor = enable;
    	this.fillColor = fillColor;
    }
    
    /**
     * Call {@link Drawable#jumpToCurrentState() Drawable.jumpToCurrentState()}
     * on all Drawable objects associated with this view.
     */
    public void jumpDrawablesToCurrentState() {
        if (mBackground != null) {
            mBackground.jumpToCurrentState();
        }
        
        if (mStateListAnimator != null) {
            mStateListAnimator.jumpToCurrentState();
        }
    }

    protected void drawBackground(GLCanvas canvas) {
        final Drawable background = mBackground;
        if (background == null) {
            return;
        }

        if (mBackgroundSizeChanged) {
            background.setBounds(0, 0,  mRight - mLeft, mBottom - mTop);
            mBackgroundSizeChanged = false;
        }

        final int scrollX = mScrollX;
        final int scrollY = mScrollY;
        if ((scrollX | scrollY) == 0) {
            background.draw(canvas);
        } else {
            canvas.translate(scrollX, scrollY);
            background.draw(canvas);
            canvas.translate(-scrollX, -scrollY);
        }
    }
    
    /**
     * Returns the overlay for this view, creating it if it does not yet exist.
     * Adding drawables to the overlay will cause them to be displayed whenever
     * the view itself is redrawn. Objects in the overlay should be actively
     * managed: remove them when they should not be displayed anymore. The
     * overlay will always have the same size as its host view.
     *
     * <p>Note: Overlays do not currently work correctly with {@link
     * SurfaceView} or {@link TextureView}; contents in overlays for these
     * types of views may not display correctly.</p>
     *
     * @return The ViewOverlay object for this view.
     * @see ViewOverlay
     */
    public ViewOverlay getOverlay() {
        if (mOverlay == null) {
            mOverlay = new ViewOverlay(mContext, this);
        }
        return mOverlay;
    }
    
    public void clearState(GLCanvas canvas){
    	
    }
    
    /**
     * Implement this method to handle generic motion events.
     * <p>
     * Generic motion events describe joystick movements, mouse hovers, track pad
     * touches, scroll wheel movements and other input events.  The
     * {@link MotionEvent#getSource() source} of the motion event specifies
     * the class of input that was received.  Implementations of this method
     * must examine the bits in the source before processing the event.
     * The following code example shows how this is done.
     * </p><p>
     * Generic motion events with source class {@link InputDevice#SOURCE_CLASS_POINTER}
     * are delivered to the view under the pointer.  All other generic motion events are
     * delivered to the focused view.
     * </p>
     * <pre> public boolean onGenericMotionEvent(MotionEvent event) {
     *     if (event.isFromSource(InputDevice.SOURCE_CLASS_JOYSTICK)) {
     *         if (event.getAction() == MotionEvent.ACTION_MOVE) {
     *             // process the joystick movement...
     *             return true;
     *         }
     *     }
     *     if (event.isFromSource(InputDevice.SOURCE_CLASS_POINTER)) {
     *         switch (event.getAction()) {
     *             case MotionEvent.ACTION_HOVER_MOVE:
     *                 // process the mouse hover movement...
     *                 return true;
     *             case MotionEvent.ACTION_SCROLL:
     *                 // process the scroll wheel movement...
     *                 return true;
     *         }
     *     }
     *     return super.onGenericMotionEvent(event);
     * }</pre>
     *
     * @param event The generic motion event being processed.
     * @return True if the event was handled, false otherwise.
     */
    public boolean onGenericMotionEvent(MotionEvent event) {
        return false;
    }
    
    /**
     * Gets a scale factor that determines the distance the view should scroll
     * vertically in response to {@link MotionEvent#ACTION_SCROLL}.
     * @return The vertical scroll scale factor.
     * @hide
     */
    protected float getVerticalScrollFactor() {
        if (mVerticalScrollFactor == 0) {
            TypedValue outValue = new TypedValue();
            mVerticalScrollFactor = outValue.getDimension(
                    mContext.getResources().getDisplayMetrics());
        }
        return mVerticalScrollFactor;
    }
    
    /**
     * Returns the size of the vertical faded edges used to indicate that more
     * content in this view is visible.
     *
     * @return The size in pixels of the vertical faded edge or 0 if vertical
     *         faded edges are not enabled for this view.
     * @attr ref android.R.styleable#View_fadingEdgeLength
     */
    public int getVerticalFadingEdgeLength() {
        return 0;
    }

    /**
     * Set the size of the faded edge used to indicate that more content in this
     * view is available.  Will not change whether the fading edge is enabled; use
     * {@link #setVerticalFadingEdgeEnabled(boolean)} or
     * {@link #setHorizontalFadingEdgeEnabled(boolean)} to enable the fading edge
     * for the vertical or horizontal fading edges.
     *
     * @param length The size in pixels of the faded edge used to indicate that more
     *        content in this view is visible.
     */
    public void setFadingEdgeLength(int length) {
    }
    
    /**
     * Returns the size of the horizontal faded edges used to indicate that more
     * content in this view is visible.
     *
     * @return The size in pixels of the horizontal faded edge or 0 if horizontal
     *         faded edges are not enabled for this view.
     * @attr ref android.R.styleable#View_fadingEdgeLength
     */
    public int getHorizontalFadingEdgeLength() {
        return 0;
    }
    
    /**
     * Returns the width of the vertical scrollbar.
     *
     * @return The width in pixels of the vertical scrollbar or 0 if there
     *         is no vertical scrollbar.
     */
    public int getVerticalScrollbarWidth() {
        return 0;
    }

    /**
     * Returns the height of the horizontal scrollbar.
     *
     * @return The height in pixels of the horizontal scrollbar or 0 if
     *         there is no horizontal scrollbar.
     */
    protected int getHorizontalScrollbarHeight() {
        return 0;
    }
    
    /**
     * <p>
     * Initializes the scrollbars from a given set of styled attributes. This
     * method should be called by subclasses that need scrollbars and when an
     * instance of these subclasses is created programmatically rather than
     * being inflated from XML. This method is automatically called when the XML
     * is inflated.
     * </p>
     *
     * @param a the styled attributes set to initialize the scrollbars from
     *
     * @removed
     */
    protected void initializeScrollbars(TypedArray a) {
    	// It's not safe to use this method from apps. The parameter 'a' must have been obtained
        // using the View filter array which is not available to the SDK. As such, internal
        // framework usage now uses initializeScrollbarsInternal and we grab a default
        // TypedArray with the right filter instead here.
        TypedArray arr = mContext.obtainStyledAttributes(com.glview.R.styleable.View);

        initializeScrollbarsInternal(arr);

        // We ignored the method parameter. Recycle the one we actually did use.
        arr.recycle();
    }

    /**
     * <p>
     * Initializes the scrollbars from a given set of styled attributes. This
     * method should be called by subclasses that need scrollbars and when an
     * instance of these subclasses is created programmatically rather than
     * being inflated from XML. This method is automatically called when the XML
     * is inflated.
     * </p>
     *
     * @param a the styled attributes set to initialize the scrollbars from
     * @hide
     */
    protected void initializeScrollbarsInternal(TypedArray a) {
        initScrollCache();
        

        final ScrollabilityCache scrollabilityCache = mScrollCache;

        if (scrollabilityCache.scrollBar == null) {
            scrollabilityCache.scrollBar = new ScrollBarDrawable();
        }

       final boolean  fadeScrollbars = a.getBoolean(com.glview.R.styleable.View_fadeScrollbars, true);

        if (!fadeScrollbars) {
            scrollabilityCache.state = ScrollabilityCache.ON;
        }
        scrollabilityCache.fadeScrollBars = fadeScrollbars;


        scrollabilityCache.scrollBarFadeDuration = a.getInt(
        		com.glview.R.styleable.View_scrollbarFadeDuration, ViewConfiguration
                .getScrollBarFadeDuration());
        scrollabilityCache.scrollBarDefaultDelayBeforeFade = a.getInt(
        		com.glview.R.styleable.View_scrollbarDefaultDelayBeforeFade,
        		ViewConfiguration.getScrollDefaultDelay());


        scrollabilityCache.scrollBarSize = a.getDimensionPixelSize(
        		com.glview.R.styleable.View_scrollbarSize,
        		ViewConfiguration.get(mContext).getScaledScrollBarSize());

        Drawable track = GLContext.get().getResources().getDrawable(a.getResourceId(com.glview.R.styleable.View_scrollbarTrackHorizontal, 0));
        scrollabilityCache.scrollBar.setHorizontalTrackDrawable(track);

        Drawable thumb = GLContext.get().getResources().getDrawable(a.getResourceId(com.glview.R.styleable.View_scrollbarThumbHorizontal, 0));
        if (thumb != null) {
            scrollabilityCache.scrollBar.setHorizontalThumbDrawable(thumb);
        }

        boolean alwaysDraw = a.getBoolean(com.glview.R.styleable.View_scrollbarAlwaysDrawHorizontalTrack,
                false);
        if (alwaysDraw) {
            scrollabilityCache.scrollBar.setAlwaysDrawHorizontalTrack(true);
        }

        track = GLContext.get().getResources().getDrawable(a.getResourceId(com.glview.R.styleable.View_scrollbarTrackVertical, 0));
        scrollabilityCache.scrollBar.setVerticalTrackDrawable(track);

        thumb = GLContext.get().getResources().getDrawable(a.getResourceId(com.glview.R.styleable.View_scrollbarThumbVertical, 0));
        if (thumb != null) {
            scrollabilityCache.scrollBar.setVerticalThumbDrawable(thumb);
        }

        alwaysDraw = a.getBoolean(com.glview.R.styleable.View_scrollbarAlwaysDrawVerticalTrack,
                alwaysDraw);
        if (alwaysDraw) {
            scrollabilityCache.scrollBar.setAlwaysDrawVerticalTrack(true);
        }

        // Apply layout direction to the new Drawables if needed
        final int layoutDirection = getLayoutDirection();
        if (track != null) {
            track.setLayoutDirection(layoutDirection);
        }
        if (thumb != null) {
            thumb.setLayoutDirection(layoutDirection);
        }

        // Re-apply user/background padding so that scrollbar(s) get added
        resolvePadding();
        a.recycle();
    }

    /**
     * <p>
     * Initalizes the scrollability cache if necessary.
     * </p>
     */
    private void initScrollCache() {
        if (mScrollCache == null) {
            mScrollCache = new ScrollabilityCache(ViewConfiguration.get(mContext), this);
        }
    }

    private ScrollabilityCache getScrollCache() {
        initScrollCache();
        return mScrollCache;
    }

    /**
     * Set the position of the vertical scroll bar. Should be one of
     * {@link #SCROLLBAR_POSITION_DEFAULT}, {@link #SCROLLBAR_POSITION_LEFT} or
     * {@link #SCROLLBAR_POSITION_RIGHT}.
     *
     * @param position Where the vertical scroll bar should be positioned.
     */
    public void setVerticalScrollbarPosition(int position) {
        if (mVerticalScrollbarPosition != position) {
            mVerticalScrollbarPosition = position;
            computeOpaqueFlags();
            resolvePadding();
        }
    }

    /**
     * @return The position where the vertical scroll bar will show, if applicable.
     * @see #setVerticalScrollbarPosition(int)
     */
    public int getVerticalScrollbarPosition() {
        return mVerticalScrollbarPosition;
    }
    
    /**
     * Gets a scale factor that determines the distance the view should scroll
     * horizontally in response to {@link MotionEvent#ACTION_SCROLL}.
     * @return The horizontal scroll scale factor.
     * @hide
     */
    protected float getHorizontalScrollFactor() {
        // TODO: Should use something else.
        return getVerticalScrollFactor();
    }
    
    /**
     * @hide
     */
    protected void computeOpaqueFlags() {
        // Opaque if:
        //   - Has a background
        //   - Background is opaque
        //   - Doesn't have scrollbars or scrollbars overlay

        if (mBackground != null && mBackground.getOpacity() == PixelFormat.OPAQUE) {
            mPrivateFlags |= PFLAG_OPAQUE_BACKGROUND;
        } else {
            mPrivateFlags &= ~PFLAG_OPAQUE_BACKGROUND;
        }

        final int flags = mViewFlags;
        if (((flags & SCROLLBARS_VERTICAL) == 0 && (flags & SCROLLBARS_HORIZONTAL) == 0) ||
                (flags & SCROLLBARS_STYLE_MASK) == SCROLLBARS_INSIDE_OVERLAY ||
                (flags & SCROLLBARS_STYLE_MASK) == SCROLLBARS_OUTSIDE_OVERLAY) {
            mPrivateFlags |= PFLAG_OPAQUE_SCROLLBARS;
        } else {
            mPrivateFlags &= ~PFLAG_OPAQUE_SCROLLBARS;
        }
    }

    /**
     * @hide
     */
    protected boolean hasOpaqueScrollbars() {
        return (mPrivateFlags & PFLAG_OPAQUE_SCROLLBARS) == PFLAG_OPAQUE_SCROLLBARS;
    }
    
    /**
     * Implement this method to handle hover events.
     * <p>
     * This method is called whenever a pointer is hovering into, over, or out of the
     * bounds of a view and the view is not currently being touched.
     * Hover events are represented as pointer events with action
     * {@link MotionEvent#ACTION_HOVER_ENTER}, {@link MotionEvent#ACTION_HOVER_MOVE},
     * or {@link MotionEvent#ACTION_HOVER_EXIT}.
     * </p>
     * <ul>
     * <li>The view receives a hover event with action {@link MotionEvent#ACTION_HOVER_ENTER}
     * when the pointer enters the bounds of the view.</li>
     * <li>The view receives a hover event with action {@link MotionEvent#ACTION_HOVER_MOVE}
     * when the pointer has already entered the bounds of the view and has moved.</li>
     * <li>The view receives a hover event with action {@link MotionEvent#ACTION_HOVER_EXIT}
     * when the pointer has exited the bounds of the view or when the pointer is
     * about to go down due to a button click, tap, or similar user action that
     * causes the view to be touched.</li>
     * </ul>
     * <p>
     * The view should implement this method to return true to indicate that it is
     * handling the hover event, such as by changing its drawable state.
     * </p><p>
     * The default implementation calls {@link #setHovered} to update the hovered state
     * of the view when a hover enter or hover exit event is received, if the view
     * is enabled and is clickable.  The default implementation also sends hover
     * accessibility events.
     * </p>
     *
     * @param event The motion event that describes the hover.
     * @return True if the view handled the hover event.
     *
     * @see #isHovered
     * @see #setHovered
     * @see #onHoverChanged
     */
    /*public boolean onHoverEvent(MotionEvent event) {
        // The root view may receive hover (or touch) events that are outside the bounds of
        // the window.  This code ensures that we only send accessibility events for
        // hovers that are actually within the bounds of the root view.
        final int action = event.getActionMasked();
        if (!mSendingHoverAccessibilityEvents) {
            if ((action == MotionEvent.ACTION_HOVER_ENTER
                    || action == MotionEvent.ACTION_HOVER_MOVE)
                    && !hasHoveredChild()
                    && pointInView(event.getX(), event.getY())) {
                sendAccessibilityHoverEvent(AccessibilityEvent.TYPE_VIEW_HOVER_ENTER);
                mSendingHoverAccessibilityEvents = true;
            }
        } else {
            if (action == MotionEvent.ACTION_HOVER_EXIT
                    || (action == MotionEvent.ACTION_MOVE
                            && !pointInView(event.getX(), event.getY()))) {
                mSendingHoverAccessibilityEvents = false;
                sendAccessibilityHoverEvent(AccessibilityEvent.TYPE_VIEW_HOVER_EXIT);
            }
        }

        if (isHoverable()) {
            switch (action) {
                case MotionEvent.ACTION_HOVER_ENTER:
                    setHovered(true);
                    break;
                case MotionEvent.ACTION_HOVER_EXIT:
                    setHovered(false);
                    break;
            }

            // Dispatch the event to onGenericMotionEvent before returning true.
            // This is to provide compatibility with existing applications that
            // handled HOVER_MOVE events in onGenericMotionEvent and that would
            // break because of the new default handling for hoverable views
            // in onHoverEvent.
            // Note that onGenericMotionEvent will be called by default when
            // onHoverEvent returns false (refer to dispatchGenericMotionEvent).
            dispatchGenericMotionEventInternal(event);
            // The event was already handled by calling setHovered(), so always
            // return true.
            return true;
        }

        return false;
    }*/

    /**
     * Returns true if the view should handle {@link #onHoverEvent}
     * by calling {@link #setHovered} to change its hovered state.
     *
     * @return True if the view is hoverable.
     */
    private boolean isHoverable() {
        final int viewFlags = mViewFlags;
        if ((viewFlags & ENABLED_MASK) == DISABLED) {
            return false;
        }

        return (viewFlags & CLICKABLE) == CLICKABLE
                || (viewFlags & LONG_CLICKABLE) == LONG_CLICKABLE;
    }

    /**
     * Returns true if the view is currently hovered.
     *
     * @return True if the view is currently hovered.
     *
     * @see #setHovered
     * @see #onHoverChanged
     */
    public boolean isHovered() {
        return (mPrivateFlags & PFLAG_HOVERED) != 0;
    }

    /**
     * Sets whether the view is currently hovered.
     * <p>
     * Calling this method also changes the drawable state of the view.  This
     * enables the view to react to hover by using different drawable resources
     * to change its appearance.
     * </p><p>
     * The {@link #onHoverChanged} method is called when the hovered state changes.
     * </p>
     *
     * @param hovered True if the view is hovered.
     *
     * @see #isHovered
     * @see #onHoverChanged
     */
    public void setHovered(boolean hovered) {
        if (hovered) {
            if ((mPrivateFlags & PFLAG_HOVERED) == 0) {
                mPrivateFlags |= PFLAG_HOVERED;
                refreshDrawableState();
                onHoverChanged(true);
            }
        } else {
            if ((mPrivateFlags & PFLAG_HOVERED) != 0) {
                mPrivateFlags &= ~PFLAG_HOVERED;
                refreshDrawableState();
                onHoverChanged(false);
            }
        }
    }

    /**
     * Implement this method to handle hover state changes.
     * <p>
     * This method is called whenever the hover state changes as a result of a
     * call to {@link #setHovered}.
     * </p>
     *
     * @param hovered The current hover state, as returned by {@link #isHovered}.
     *
     * @see #isHovered
     * @see #setHovered
     */
    public void onHoverChanged(boolean hovered) {
    }

    /**
     * Implement this method to handle touch screen motion events.
     * <p>
     * If this method is used to detect click actions, it is recommended that
     * the actions be performed by implementing and calling
     * {@link #performClick()}. This will ensure consistent system behavior,
     * including:
     * <ul>
     * <li>obeying click sound preferences
     * <li>dispatching OnClickListener calls
     * <li>handling {@link AccessibilityNodeInfo#ACTION_CLICK ACTION_CLICK} when
     * accessibility features are enabled
     * </ul>
     *
     * @param event The motion event.
     * @return True if the event was handled, false otherwise.
     */
    public boolean onTouchEvent(MotionEvent event) {
        final float x = event.getX();
        final float y = event.getY();
        final int viewFlags = mViewFlags;

        if ((viewFlags & ENABLED_MASK) == DISABLED) {
            if (event.getAction() == MotionEvent.ACTION_UP && (mPrivateFlags & PFLAG_PRESSED) != 0) {
                setPressed(false);
            }
            // A disabled view that is clickable still consumes the touch
            // events, it just doesn't respond to them.
            return (((viewFlags & CLICKABLE) == CLICKABLE ||
                    (viewFlags & LONG_CLICKABLE) == LONG_CLICKABLE));
        }

        if (mTouchDelegate != null) {
            if (mTouchDelegate.onTouchEvent(event)) {
                return true;
            }
        }

        if (((viewFlags & CLICKABLE) == CLICKABLE ||
                (viewFlags & LONG_CLICKABLE) == LONG_CLICKABLE)) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_UP:
                    boolean prepressed = (mPrivateFlags & PFLAG_PREPRESSED) != 0;
                    if ((mPrivateFlags & PFLAG_PRESSED) != 0 || prepressed) {
                        // take focus if we don't have it already and we should in
                        // touch mode.
                        boolean focusTaken = false;
                        if (isFocusable() && isFocusableInTouchMode() && !isFocused()) {
                            focusTaken = requestFocus();
                        }

                        if (prepressed) {
                            // The button is being released before we actually
                            // showed it as pressed.  Make it show the pressed
                            // state now (before scheduling the click) to ensure
                            // the user sees it.
                            setPressed(true, x, y);
                       }

                        if (!mHasPerformedLongPress) {
                            // This is a tap, so remove the longpress check
                            removeLongPressCallback();

                            // Only perform take click actions if we were in the pressed state
                            if (!focusTaken) {
                                // Use a Runnable and post this rather than calling
                                // performClick directly. This lets other visual state
                                // of the view update before click actions start.
                                if (mPerformClick == null) {
                                    mPerformClick = new PerformClick();
                                }
                                if (!post(mPerformClick)) {
                                    performClick();
                                }
                            }
                        }

                        if (mUnsetPressedState == null) {
                            mUnsetPressedState = new UnsetPressedState();
                        }

                        if (prepressed) {
                            postDelayed(mUnsetPressedState,
                                    ViewConfiguration.getPressedStateDuration());
                        } else if (!post(mUnsetPressedState)) {
                            // If the post failed, unpress right now
                            mUnsetPressedState.run();
                        }

                        removeTapCallback();
                    }
                    break;

                case MotionEvent.ACTION_DOWN:
                    mHasPerformedLongPress = false;

                    if (performButtonActionOnTouchDown(event)) {
                        break;
                    }

                    // Walk up the hierarchy to determine if we're inside a scrolling container.
                    boolean isInScrollingContainer = isInScrollingContainer();

                    // For views inside a scrolling container, delay the pressed feedback for
                    // a short period in case this is a scroll.
                    if (isInScrollingContainer) {
                        mPrivateFlags |= PFLAG_PREPRESSED;
                        if (mPendingCheckForTap == null) {
                            mPendingCheckForTap = new CheckForTap();
                        }
                        mPendingCheckForTap.x = event.getX();
                        mPendingCheckForTap.y = event.getY();
                        postDelayed(mPendingCheckForTap, ViewConfiguration.getTapTimeout());
                    } else {
                        // Not inside a scrolling container, so show the feedback right away
                        setPressed(true, x, y);
                        checkForLongClick(0);
                    }
                    break;

                case MotionEvent.ACTION_CANCEL:
                    setPressed(false);
                    removeTapCallback();
                    removeLongPressCallback();
                    break;

                case MotionEvent.ACTION_MOVE:
                    // Be lenient about moving outside of buttons
                    if (!pointInView(x, y, mTouchSlop)) {
                        // Outside button
                        removeTapCallback();
                        if ((mPrivateFlags & PFLAG_PRESSED) != 0) {
                            // Remove any future long press/tap checks
                            removeLongPressCallback();

                            setPressed(false);
                        }
                    }
                    break;
            }

            return true;
        }

        return false;
    }
    
    /**
     * @hide
     */
    public boolean isInScrollingContainer() {
    	ViewGroup p = getParent();
        while (p != null) {
            if (((ViewGroup) p).shouldDelayChildPressedState()) {
                return true;
            }
            p = p.getParent();
        }
        return false;
    }
    
    /**
     * Remove the longpress detection timer.
     */
    private void removeLongPressCallback() {
        if (mPendingCheckForLongPress != null) {
          removeCallbacks(mPendingCheckForLongPress);
        }
    }

    /**
     * Remove the pending click action
     */
    private void removePerformClickCallback() {
        if (mPerformClick != null) {
            removeCallbacks(mPerformClick);
        }
    }

    /**
     * Remove the prepress detection timer.
     */
    private void removeUnsetPressCallback() {
        if ((mPrivateFlags & PFLAG_PRESSED) != 0 && mUnsetPressedState != null) {
            setPressed(false);
            removeCallbacks(mUnsetPressedState);
        }
    }

    /**
     * Remove the tap detection timer.
     */
    private void removeTapCallback() {
        if (mPendingCheckForTap != null) {
            mPrivateFlags &= ~PFLAG_PREPRESSED;
            removeCallbacks(mPendingCheckForTap);
        }
    }

    /**
     * Cancels a pending long press.  Your subclass can use this if you
     * want the context menu to come up if the user presses and holds
     * at the same place, but you don't want it to come up if they press
     * and then move around enough to cause scrolling.
     */
    public void cancelLongPress() {
        removeLongPressCallback();

        /*
         * The prepressed state handled by the tap callback is a display
         * construct, but the tap callback will post a long press callback
         * less its own timeout. Remove it here.
         */
        removeTapCallback();
    }

	/**
     * Set the horizontal scrolled position of your view. This will cause a call to
     * {@link #onScrollChanged(int, int, int, int)} and the view will be
     * invalidated.
     * @param value the x position to scroll to
     */
    public void setScrollX(int value) {
        scrollTo(value, mScrollY);
    }

    /**
     * Set the vertical scrolled position of your view. This will cause a call to
     * {@link #onScrollChanged(int, int, int, int)} and the view will be
     * invalidated.
     * @param value the y position to scroll to
     */
    public void setScrollY(int value) {
        scrollTo(mScrollX, value);
    }
	
    /**
     * Set the scrolled position of your view. This will cause a call to
     * {@link #onScrollChanged(int, int, int, int)} and the view will be
     * invalidated.
     * @param x the x position to scroll to
     * @param y the y position to scroll to
     */
    public void scrollTo(int x, int y) {
        if (mScrollX != x || mScrollY != y) {
            int oldX = mScrollX;
            int oldY = mScrollY;
            mScrollX = x;
            mScrollY = y;
            invalidate();
//            invalidateParentCaches();
            onScrollChanged(mScrollX, mScrollY, oldX, oldY);
            if (!awakenScrollBars()) {
                postInvalidateOnAnimation();
            }
        }
    }
    
    /**
     * This is called in response to an internal scroll in this view (i.e., the
     * view scrolled its own contents). This is typically as a result of
     * {@link #scrollBy(int, int)} or {@link #scrollTo(int, int)} having been
     * called.
     *
     * @param l Current horizontal scroll origin.
     * @param t Current vertical scroll origin.
     * @param oldl Previous horizontal scroll origin.
     * @param oldt Previous vertical scroll origin.
     */
    protected void onScrollChanged(int l, int t, int oldl, int oldt) {
        mBackgroundSizeChanged = true;
        
        final AttachInfo ai = mAttachInfo;
        if (ai != null) {
            ai.mViewScrollChanged = true;
        }
    }

    /**
     * Move the scrolled position of your view. This will cause a call to
     * {@link #onScrollChanged(int, int, int, int)} and the view will be
     * invalidated.
     * @param x the amount of pixels to scroll by horizontally
     * @param y the amount of pixels to scroll by vertically
     */
    public void scrollBy(int x, int y) {
        scrollTo(mScrollX + x, mScrollY + y);
    }
    
    /**
     * <p>Trigger the scrollbars to draw. When invoked this method starts an
     * animation to fade the scrollbars out after a default delay. If a subclass
     * provides animated scrolling, the start delay should equal the duration
     * of the scrolling animation.</p>
     *
     * <p>The animation starts only if at least one of the scrollbars is
     * enabled, as specified by {@link #isHorizontalScrollBarEnabled()} and
     * {@link #isVerticalScrollBarEnabled()}. When the animation is started,
     * this method returns true, and false otherwise. If the animation is
     * started, this method calls {@link #invalidate()}; in that case the
     * caller should not call {@link #invalidate()}.</p>
     *
     * <p>This method should be invoked every time a subclass directly updates
     * the scroll parameters.</p>
     *
     * <p>This method is automatically invoked by {@link #scrollBy(int, int)}
     * and {@link #scrollTo(int, int)}.</p>
     *
     * @return true if the animation is played, false otherwise
     *
     * @see #awakenScrollBars(int)
     * @see #scrollBy(int, int)
     * @see #scrollTo(int, int)
     * @see #isHorizontalScrollBarEnabled()
     * @see #isVerticalScrollBarEnabled()
     * @see #setHorizontalScrollBarEnabled(boolean)
     * @see #setVerticalScrollBarEnabled(boolean)
     */
    protected boolean awakenScrollBars() {
        return mScrollCache != null &&
                awakenScrollBars(mScrollCache.scrollBarDefaultDelayBeforeFade, true);
    }

    /**
     * Trigger the scrollbars to draw.
     * This method differs from awakenScrollBars() only in its default duration.
     * initialAwakenScrollBars() will show the scroll bars for longer than
     * usual to give the user more of a chance to notice them.
     *
     * @return true if the animation is played, false otherwise.
     */
    private boolean initialAwakenScrollBars() {
        return mScrollCache != null &&
                awakenScrollBars(mScrollCache.scrollBarDefaultDelayBeforeFade * 4, true);
    }

    /**
     * <p>
     * Trigger the scrollbars to draw. When invoked this method starts an
     * animation to fade the scrollbars out after a fixed delay. If a subclass
     * provides animated scrolling, the start delay should equal the duration of
     * the scrolling animation.
     * </p>
     *
     * <p>
     * The animation starts only if at least one of the scrollbars is enabled,
     * as specified by {@link #isHorizontalScrollBarEnabled()} and
     * {@link #isVerticalScrollBarEnabled()}. When the animation is started,
     * this method returns true, and false otherwise. If the animation is
     * started, this method calls {@link #invalidate()}; in that case the caller
     * should not call {@link #invalidate()}.
     * </p>
     *
     * <p>
     * This method should be invoked everytime a subclass directly updates the
     * scroll parameters.
     * </p>
     *
     * @param startDelay the delay, in milliseconds, after which the animation
     *        should start; when the delay is 0, the animation starts
     *        immediately
     * @return true if the animation is played, false otherwise
     *
     * @see #scrollBy(int, int)
     * @see #scrollTo(int, int)
     * @see #isHorizontalScrollBarEnabled()
     * @see #isVerticalScrollBarEnabled()
     * @see #setHorizontalScrollBarEnabled(boolean)
     * @see #setVerticalScrollBarEnabled(boolean)
     */
    protected boolean awakenScrollBars(int startDelay) {
        return awakenScrollBars(startDelay, true);
    }

    /**
     * <p>
     * Trigger the scrollbars to draw. When invoked this method starts an
     * animation to fade the scrollbars out after a fixed delay. If a subclass
     * provides animated scrolling, the start delay should equal the duration of
     * the scrolling animation.
     * </p>
     *
     * <p>
     * The animation starts only if at least one of the scrollbars is enabled,
     * as specified by {@link #isHorizontalScrollBarEnabled()} and
     * {@link #isVerticalScrollBarEnabled()}. When the animation is started,
     * this method returns true, and false otherwise. If the animation is
     * started, this method calls {@link #invalidate()} if the invalidate parameter
     * is set to true; in that case the caller
     * should not call {@link #invalidate()}.
     * </p>
     *
     * <p>
     * This method should be invoked everytime a subclass directly updates the
     * scroll parameters.
     * </p>
     *
     * @param startDelay the delay, in milliseconds, after which the animation
     *        should start; when the delay is 0, the animation starts
     *        immediately
     *
     * @param invalidate Wheter this method should call invalidate
     *
     * @return true if the animation is played, false otherwise
     *
     * @see #scrollBy(int, int)
     * @see #scrollTo(int, int)
     * @see #isHorizontalScrollBarEnabled()
     * @see #isVerticalScrollBarEnabled()
     * @see #setHorizontalScrollBarEnabled(boolean)
     * @see #setVerticalScrollBarEnabled(boolean)
     */
    protected boolean awakenScrollBars(int startDelay, boolean invalidate) {
        final ScrollabilityCache scrollCache = mScrollCache;

        if (scrollCache == null || !scrollCache.fadeScrollBars) {
            return false;
        }

        if (scrollCache.scrollBar == null) {
            scrollCache.scrollBar = new ScrollBarDrawable();
        }

        if (isHorizontalScrollBarEnabled() || isVerticalScrollBarEnabled()) {

            if (invalidate) {
                // Invalidate to show the scrollbars
                postInvalidateOnAnimation();
            }

            if (scrollCache.state == ScrollabilityCache.OFF) {
                // FIXME: this is copied from WindowManagerService.
                // We should get this value from the system when it
                // is possible to do so.
                final int KEY_REPEAT_FIRST_DELAY = 750;
                startDelay = Math.max(KEY_REPEAT_FIRST_DELAY, startDelay);
            }

            // Tell mScrollCache when we should start fading. This may
            // extend the fade start time if one was already scheduled
            long fadeStartTime = AnimationUtils.currentAnimationTimeMillis() + startDelay;
            scrollCache.fadeStartTime = fadeStartTime;
            scrollCache.state = ScrollabilityCache.ON;

            // Schedule our fader to run, unscheduling any old ones first
            if (mAttachInfo != null) {
                mAttachInfo.mHandler.removeCallbacks(scrollCache);
                mAttachInfo.mHandler.postAtTime(scrollCache, fadeStartTime);
            }

            return true;
        }

        return false;
    }
	
    /**
     * Sets the identifier for this view. The identifier does not have to be
     * unique in this view's hierarchy. The identifier should be a positive
     * number.
     *
     * @see #NO_ID
     * @see #getId()
     * @see #findViewById(int)
     *
     * @param id a number used to identify the view
     *
     */
    public void setId(int id){
    	mID = id;
    }
    
    /**
     * Returns this view's identifier.
     *
     * @return a positive integer used to identify the view or {@link #NO_ID}
     *         if the view has no ID
     *
     * @see #setId(int)
     * @see #findViewById(int)
     */
    public int getId(){
    	return mID;
    }
    
    /**
     * {@hide}
     *
     * @param isRoot true if the view belongs to the root namespace, false
     *        otherwise
     */
    public void setIsRootNamespace(boolean isRoot) {
        if (isRoot) {
            mPrivateFlags |= PFLAG_IS_ROOT_NAMESPACE;
        } else {
            mPrivateFlags &= ~PFLAG_IS_ROOT_NAMESPACE;
        }
    }

    /**
     * {@hide}
     *
     * @return true if the view belongs to the root namespace, false otherwise
     */
    public boolean isRootNamespace() {
        return (mPrivateFlags&PFLAG_IS_ROOT_NAMESPACE) != 0;
    }
    
    /**
     * Look for a child view with the given id.  If this view has the given
     * id, return this view.
     *
     * @param id The id to search for.
     * @return The view that has the given id in the hierarchy or null
     */
    public final View findViewById(int id) {
        if (id < 0) {
            return null;
        }
        return findViewTraversal(id);
    }

    /**
     * {@hide}
     * @param id the id of the view to be found
     * @return the view of the specified id, null if cannot be found
     */
    protected View findViewTraversal(int id) {
        if (id == mID) {
            return this;
        }
        return null;
    }
    
    /**
     * {@hide}
     * @param tag the tag of the view to be found
     * @return the view of specified tag, null if cannot be found
     */
    protected View findViewWithTagTraversal(Object tag) {
        if (tag != null && tag.equals(mTag)) {
            return this;
        }
        return null;
    }
    
    /**
     * {@hide}
     * @param predicate The predicate to evaluate.
     * @param childToSkip If not null, ignores this child during the recursive traversal.
     * @return The first view that matches the predicate or null.
     */
    protected View findViewByPredicateTraversal(Predicate<View> predicate, View childToSkip) {
        if (predicate.apply(this)) {
            return this;
        }
        return null;
    }
    
    /**
     * Look for a child view with the given tag.  If this view has the given
     * tag, return this view.
     *
     * @param tag The tag to search for, using "tag.equals(getTag())".
     * @return The View that has the given tag in the hierarchy or null
     */
    public final View findViewWithTag(Object tag) {
        if (tag == null) {
            return null;
        }
        return findViewWithTagTraversal(tag);
    }

    /**
     * {@hide}
     * Look for a child view that matches the specified predicate.
     * If this view matches the predicate, return this view.
     *
     * @param predicate The predicate to evaluate.
     * @return The first view that matches the predicate or null.
     */
    public final View findViewByPredicate(Predicate<View> predicate) {
        return findViewByPredicateTraversal(predicate, null);
    }

    /**
     * {@hide}
     * Look for a child view that matches the specified predicate,
     * starting with the specified view and its descendents and then
     * recusively searching the ancestors and siblings of that view
     * until this view is reached.
     *
     * This method is useful in cases where the predicate does not match
     * a single unique view (perhaps multiple views use the same id)
     * and we are trying to find the view that is "closest" in scope to the
     * starting view.
     *
     * @param start The view to start from.
     * @param predicate The predicate to evaluate.
     * @return The first view that matches the predicate or null.
     */
    public final View findViewByPredicateInsideOut(View start, Predicate<View> predicate) {
        View childToSkip = null;
        for (;;) {
            View view = start.findViewByPredicateTraversal(predicate, childToSkip);
            if (view != null || start == this) {
                return view;
            }

            ViewGroup parent = start.getParent();
            if (parent == null || !(parent instanceof View)) {
                return null;
            }

            childToSkip = start;
            start = (View) parent;
        }
    }
    
    /**
     * Set the enabled state of this view. The interpretation of the enabled
     * state varies by subclass.
     *
     * @param enabled True if this view is enabled, false otherwise.
     */
    public void setEnabled(boolean enabled) {
        if (enabled == isEnabled()) return;

        setFlags(enabled ? ENABLED : DISABLED, ENABLED_MASK);

        /*
         * The View most likely has to change its appearance, so refresh
         * the drawable state.
         */
        refreshDrawableState();

        // Invalidate too, since the default behavior for views is to be
        // be drawn at 50% alpha rather than to change the drawable.
        invalidate();
    }
    
    /**
     * Returns the enabled status for this view. The interpretation of the
     * enabled state varies by subclass.
     *
     * @return True if this view is enabled, false otherwise.
     */
    public boolean isEnabled() {
        return (mViewFlags & ENABLED_MASK) == ENABLED;
    }
    
    /**
     * Called by a parent to request that a child update its values for mScrollX
     * and mScrollY if necessary. This will typically be done if the child is
     * animating a scroll using a {@link com.aliyun.image.common.Scroller Scroller}
     * object.
     */
    public void computeScroll() {
    }
    
	public boolean dispatchTouchEvent(MotionEvent event) {
		if (!isEnabled())
			return false;
		
		boolean result = false;
		
		final int actionMasked = event.getActionMasked();
        if (actionMasked == MotionEvent.ACTION_DOWN) {
            // Defensive cleanup for new gesture
            stopNestedScroll();
        }

        //noinspection SimplifiableIfStatement
        ListenerInfo li = mListenerInfo;
        if (li != null && li.mOnTouchListener != null
                && (mViewFlags & ENABLED_MASK) == ENABLED
                && li.mOnTouchListener.onTouch(this, event)) {
            result = true;
        }

        if (!result && onTouchEvent(event)) {
            result = true;
        }
        
        // Clean up after nested scrolls if this is the end of a gesture;
        // also cancel it if we tried an ACTION_DOWN but we didn't want the rest
        // of the gesture.
        if (actionMasked == MotionEvent.ACTION_UP ||
                actionMasked == MotionEvent.ACTION_CANCEL ||
                (actionMasked == MotionEvent.ACTION_DOWN && !result)) {
            stopNestedScroll();
        }

        return result;
	}
    
    public boolean dispatchKeyEvent(KeyEvent event) {

    	// Give any attached key listener a first crack at the event.
        //noinspection SimplifiableIfStatement
        ListenerInfo li = mListenerInfo;
        if (li != null && li.mOnKeyListener != null && (mViewFlags & ENABLED_MASK) == ENABLED
                && li.mOnKeyListener.onKey(this, event.getKeyCode(), event)) {
            return true;
        }

        if (event.dispatch(this, null, this)) {
            return true;
        }
    	
    	return false;
    }
    
    /**
     * Called when the window containing this view gains or loses window focus.
     * ViewGroups should override to route to their children.
     *
     * @param hasFocus True if the window containing this view now has focus,
     *        false otherwise.
     */
    public void dispatchWindowFocusChanged(boolean hasFocus) {
        onWindowFocusChanged(hasFocus);
    }

    /**
     * Called when the window containing this view gains or loses focus.  Note
     * that this is separate from view focus: to receive key events, both
     * your view and its window must have focus.  If a window is displayed
     * on top of yours that takes input focus, then your own window will lose
     * focus but the view focus will remain unchanged.
     *
     * @param hasWindowFocus True if the window containing this view now has
     *        focus, false otherwise.
     */
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        if (!hasWindowFocus) {
            if (isPressed()) {
                setPressed(false);
            }
            removeLongPressCallback();
            removeTapCallback();
            onFocusLost();
        }
        refreshDrawableState();
    }

    /**
     * Returns true if this view is in a window that currently has window focus.
     * Note that this is not the same as the view itself having focus.
     *
     * @return True if this view is in a window that currently has window focus.
     */
    public boolean hasWindowFocus() {
        return mAttachInfo != null && mAttachInfo.mHasWindowFocus;
    }
    
    /**
     * Dispatch a view visibility change down the view hierarchy.
     * ViewGroups should override to route to their children.
     * @param changedView The view whose visibility changed. Could be 'this' or
     * an ancestor view.
     * @param visibility The new visibility of changedView: {@link #VISIBLE},
     * {@link #INVISIBLE} or {@link #GONE}.
     */
    protected void dispatchVisibilityChanged(View changedView,
            int visibility) {
        onVisibilityChanged(changedView, visibility);
    }

    /**
     * Called when the visibility of the view or an ancestor of the view is changed.
     * @param changedView The view whose visibility changed. Could be 'this' or
     * an ancestor view.
     * @param visibility The new visibility of changedView: {@link #VISIBLE},
     * {@link #INVISIBLE} or {@link #GONE}.
     */
    protected void onVisibilityChanged(View changedView, int visibility) {
        if (visibility == VISIBLE) {
            if (mAttachInfo != null) {
                initialAwakenScrollBars();
            } else {
                mPrivateFlags |= PFLAG_AWAKEN_SCROLL_BARS_ON_ATTACH;
            }
        }
    }
    
    /**
     * Dispatch a window visibility change down the view hierarchy.
     * ViewGroups should override to route to their children.
     *
     * @param visibility The new visibility of the window.
     *
     * @see #onWindowVisibilityChanged(int)
     */
    public void dispatchWindowVisibilityChanged(int visibility) {
        onWindowVisibilityChanged(visibility);
    }

    /**
     * Called when the window containing has change its visibility
     * (between {@link #GONE}, {@link #INVISIBLE}, and {@link #VISIBLE}).  Note
     * that this tells you whether or not your window is being made visible
     * to the window manager; this does <em>not</em> tell you whether or not
     * your window is obscured by other windows on the screen, even if it
     * is itself visible.
     *
     * @param visibility The new visibility of the window.
     */
    protected void onWindowVisibilityChanged(int visibility) {
        if (visibility == VISIBLE) {
            initialAwakenScrollBars();
        }
    }
    
    private void checkForLongClick(int delayOffset) {
        if ((mViewFlags & LONG_CLICKABLE) == LONG_CLICKABLE) {
            mHasPerformedLongPress = false;

            if (mPendingCheckForLongPress == null) {
                mPendingCheckForLongPress = new CheckForLongPress();
            }
            mPendingCheckForLongPress.rememberWindowAttachCount();
            postDelayed(mPendingCheckForLongPress,
                    ViewConfiguration.getLongPressTimeout() - delayOffset);
        }
    }
    
    /**
     * Call this view's OnClickListener, if it is defined.  Performs all normal
     * actions associated with clicking: reporting accessibility event, playing
     * a sound, etc.
     *
     * @return True there was an assigned OnClickListener that was called, false
     *         otherwise is returned.
     */
    public boolean performClick() {

        ListenerInfo li = mListenerInfo;
        if (li != null && li.mOnClickListener != null) {
            playSoundEffect(SoundEffectConstants.CLICK);
            li.mOnClickListener.onClick(this);
            return true;
        }

        return false;
    }
    
    /**
     * Call this view's OnLongClickListener, if it is defined. Invokes the context menu if the
     * OnLongClickListener did not consume the event.
     *
     * @return True if one of the above receivers consumed the event, false otherwise.
     */
    public boolean performLongClick() {
        boolean handled = false;
        ListenerInfo li = mListenerInfo;
        if (li != null && li.mOnLongClickListener != null) {
            handled = li.mOnLongClickListener.onLongClick(View.this);
        }
        if (handled) {
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        }
        return handled;
    }
    
    /**
     * Performs button-related actions during a touch down event.
     *
     * @param event The event.
     * @return True if the down was consumed.
     *
     * @hide
     */
    protected boolean performButtonActionOnTouchDown(MotionEvent event) {
        return false;
    }
    
    /**
     * Sets the padding. The view may add on the space required to display
     * the scrollbars, depending on the style and visibility of the scrollbars.
     * So the values returned from {@link #getPaddingLeft}, {@link #getPaddingTop},
     * {@link #getPaddingRight} and {@link #getPaddingBottom} may be different
     * from the values set in this call.
     *
     * @attr ref android.R.styleable#View_padding
     * @attr ref android.R.styleable#View_paddingBottom
     * @attr ref android.R.styleable#View_paddingLeft
     * @attr ref android.R.styleable#View_paddingRight
     * @attr ref android.R.styleable#View_paddingTop
     * @param left the left padding in pixels
     * @param top the top padding in pixels
     * @param right the right padding in pixels
     * @param bottom the bottom padding in pixels
     */
    public void setPadding(int left, int top, int right, int bottom) {
        resetResolvedPadding();

        mUserPaddingStart = UNDEFINED_PADDING;
        mUserPaddingEnd = UNDEFINED_PADDING;

        mUserPaddingLeftInitial = left;
        mUserPaddingRightInitial = right;

        mLeftPaddingDefined = true;
        mRightPaddingDefined = true;

        internalSetPadding(left, top, right, bottom);
    }
    
    /**
     * @hide
     */
    protected void internalSetPadding(int left, int top, int right, int bottom) {
        mUserPaddingLeft = left;
        mUserPaddingRight = right;
        mUserPaddingBottom = bottom;

        final int viewFlags = mViewFlags;
        boolean changed = false;

        // Common case is there are no scroll bars.
        if ((viewFlags & (SCROLLBARS_VERTICAL|SCROLLBARS_HORIZONTAL)) != 0) {
            if ((viewFlags & SCROLLBARS_VERTICAL) != 0) {
                final int offset = (viewFlags & SCROLLBARS_INSET_MASK) == 0
                        ? 0 : getVerticalScrollbarWidth();
                switch (mVerticalScrollbarPosition) {
                    case SCROLLBAR_POSITION_DEFAULT:
                        if (isLayoutRtl()) {
                            left += offset;
                        } else {
                            right += offset;
                        }
                        break;
                    case SCROLLBAR_POSITION_RIGHT:
                        right += offset;
                        break;
                    case SCROLLBAR_POSITION_LEFT:
                        left += offset;
                        break;
                }
            }
            if ((viewFlags & SCROLLBARS_HORIZONTAL) != 0) {
                bottom += (viewFlags & SCROLLBARS_INSET_MASK) == 0
                        ? 0 : getHorizontalScrollbarHeight();
            }
        }

        if (mPaddingLeft != left) {
            changed = true;
            mPaddingLeft = left;
        }
        if (mPaddingTop != top) {
            changed = true;
            mPaddingTop = top;
        }
        if (mPaddingRight != right) {
            changed = true;
            mPaddingRight = right;
        }
        if (mPaddingBottom != bottom) {
            changed = true;
            mPaddingBottom = bottom;
        }

        if (changed) {
            requestLayout();
        }
    }

    /**
     * Sets the relative padding. The view may add on the space required to display
     * the scrollbars, depending on the style and visibility of the scrollbars.
     * So the values returned from {@link #getPaddingStart}, {@link #getPaddingTop},
     * {@link #getPaddingEnd} and {@link #getPaddingBottom} may be different
     * from the values set in this call.
     *
     * @attr ref android.R.styleable#View_padding
     * @attr ref android.R.styleable#View_paddingBottom
     * @attr ref android.R.styleable#View_paddingStart
     * @attr ref android.R.styleable#View_paddingEnd
     * @attr ref android.R.styleable#View_paddingTop
     * @param start the start padding in pixels
     * @param top the top padding in pixels
     * @param end the end padding in pixels
     * @param bottom the bottom padding in pixels
     */
    public void setPaddingRelative(int start, int top, int end, int bottom) {
        resetResolvedPadding();

        mUserPaddingStart = start;
        mUserPaddingEnd = end;
        mLeftPaddingDefined = true;
        mRightPaddingDefined = true;

        switch(getLayoutDirection()) {
            case LAYOUT_DIRECTION_RTL:
                mUserPaddingLeftInitial = end;
                mUserPaddingRightInitial = start;
                internalSetPadding(end, top, start, bottom);
                break;
            case LAYOUT_DIRECTION_LTR:
            default:
                mUserPaddingLeftInitial = start;
                mUserPaddingRightInitial = end;
                internalSetPadding(start, top, end, bottom);
        }
    }
    
    Rect mTmpRect = new Rect();
    
    /**
     * Check if layout direction resolution can be done.
     *
     * @return true if layout direction resolution can be done otherwise return false.
     */
    public boolean canResolveLayoutDirection() {
        switch (getRawLayoutDirection()) {
            case LAYOUT_DIRECTION_INHERIT:
                if (mParent != null) {
                    try {
                        return mParent.canResolveLayoutDirection();
                    } catch (AbstractMethodError e) {
                        Log.e(VIEW_LOG_TAG, mParent.getClass().getSimpleName() +
                                " does not fully implement ViewParent", e);
                    }
                }
                return false;

            default:
                return true;
        }
    }
    
    /**
     * Resolve and cache the layout direction. LTR is set initially. This is implicitly supposing
     * that the parent directionality can and will be resolved before its children.
     *
     * @return true if resolution has been done, false otherwise.
     *
     * @hide
     */
    public boolean resolveLayoutDirection() {
        // Clear any previous layout direction resolution
        mPrivateFlags2 &= ~PFLAG2_LAYOUT_DIRECTION_RESOLVED_MASK;

        if (Build.VERSION.SDK_INT >= JELLY_BEAN_MR1) {
            // Set resolved depending on layout direction
            switch ((mPrivateFlags2 & PFLAG2_LAYOUT_DIRECTION_MASK) >>
                    PFLAG2_LAYOUT_DIRECTION_MASK_SHIFT) {
                case LAYOUT_DIRECTION_INHERIT:
                    // We cannot resolve yet. LTR is by default and let the resolution happen again
                    // later to get the correct resolved value
                    if (!canResolveLayoutDirection()) return false;

                    // Parent has not yet resolved, LTR is still the default
                    try {
                        if (!mParent.isLayoutDirectionResolved()) return false;

                        if (mParent.getLayoutDirection() == LAYOUT_DIRECTION_RTL) {
                            mPrivateFlags2 |= PFLAG2_LAYOUT_DIRECTION_RESOLVED_RTL;
                        }
                    } catch (AbstractMethodError e) {
                        Log.e(VIEW_LOG_TAG, mParent.getClass().getSimpleName() +
                                " does not fully implement ViewParent", e);
                    }
                    break;
                case LAYOUT_DIRECTION_RTL:
                    mPrivateFlags2 |= PFLAG2_LAYOUT_DIRECTION_RESOLVED_RTL;
                    break;
                case LAYOUT_DIRECTION_LOCALE:
                    if((LAYOUT_DIRECTION_RTL ==
                            TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()))) {
                        mPrivateFlags2 |= PFLAG2_LAYOUT_DIRECTION_RESOLVED_RTL;
                    }
                    break;
                default:
                    // Nothing to do, LTR by default
            }
        }

        // Set to resolved
        mPrivateFlags2 |= PFLAG2_LAYOUT_DIRECTION_RESOLVED;
        return true;
    }

    /**
     * Reset the resolved layout direction. Layout direction will be resolved during a call to
     * {@link #onMeasure(int, int)}.
     *
     * @hide
     */
    public void resetResolvedLayoutDirection() {
        // Reset the current resolved bits
        mPrivateFlags2 &= ~PFLAG2_LAYOUT_DIRECTION_RESOLVED_MASK;
    }

    /**
     * @return true if the layout direction is inherited.
     *
     * @hide
     */
    public boolean isLayoutDirectionInherited() {
        return (getRawLayoutDirection() == LAYOUT_DIRECTION_INHERIT);
    }

    /**
     * @return true if layout direction has been resolved.
     */
    public boolean isLayoutDirectionResolved() {
        return (mPrivateFlags2 & PFLAG2_LAYOUT_DIRECTION_RESOLVED) == PFLAG2_LAYOUT_DIRECTION_RESOLVED;
    }

    /**
     * Return if padding has been resolved
     *
     * @hide
     */
    boolean isPaddingResolved() {
        return (mPrivateFlags2 & PFLAG2_PADDING_RESOLVED) == PFLAG2_PADDING_RESOLVED;
    }
    
    /**
     * Resolves padding depending on layout direction, if applicable, and
     * recomputes internal padding values to adjust for scroll bars.
     *
     * @hide
     */
    public void resolvePadding() {
        final int resolvedLayoutDirection = getLayoutDirection();
        
        boolean isRtlCompatibilityMode = false;
        if (!isRtlCompatibilityMode) {
            // Post Jelly Bean MR1 case: we need to take the resolved layout direction into account.
            // If start / end padding are defined, they will be resolved (hence overriding) to
            // left / right or right / left depending on the resolved layout direction.
            // If start / end padding are not defined, use the left / right ones.
            if (mBackground != null && (!mLeftPaddingDefined || !mRightPaddingDefined)) {
                Rect padding = mTmpRect;
                mBackground.getPadding(padding);
                if (!mLeftPaddingDefined) {
                    mUserPaddingLeftInitial = padding.left;
                }
                if (!mRightPaddingDefined) {
                    mUserPaddingRightInitial = padding.right;
                }
            }
            switch (resolvedLayoutDirection) {
                case LAYOUT_DIRECTION_RTL:
                    if (mUserPaddingStart != UNDEFINED_PADDING) {
                        mUserPaddingRight = mUserPaddingStart;
                    } else {
                        mUserPaddingRight = mUserPaddingRightInitial;
                    }
                    if (mUserPaddingEnd != UNDEFINED_PADDING) {
                        mUserPaddingLeft = mUserPaddingEnd;
                    } else {
                        mUserPaddingLeft = mUserPaddingLeftInitial;
                    }
                    break;
                case LAYOUT_DIRECTION_LTR:
                default:
                    if (mUserPaddingStart != UNDEFINED_PADDING) {
                        mUserPaddingLeft = mUserPaddingStart;
                    } else {
                        mUserPaddingLeft = mUserPaddingLeftInitial;
                    }
                    if (mUserPaddingEnd != UNDEFINED_PADDING) {
                        mUserPaddingRight = mUserPaddingEnd;
                    } else {
                        mUserPaddingRight = mUserPaddingRightInitial;
                    }
            }

            mUserPaddingBottom = (mUserPaddingBottom >= 0) ? mUserPaddingBottom : mPaddingBottom;
        }

        internalSetPadding(mUserPaddingLeft, mPaddingTop, mUserPaddingRight, mUserPaddingBottom);
        onRtlPropertiesChanged(resolvedLayoutDirection);

        mPrivateFlags2 |= PFLAG2_PADDING_RESOLVED;
    }

    /**
     * Returns the top padding of this view.
     *
     * @return the top padding in pixels
     */
    public int getPaddingTop() {
        return mPaddingTop;
    }

    /**
     * Returns the bottom padding of this view. If there are inset and enabled
     * scrollbars, this value may include the space required to display the
     * scrollbars as well.
     *
     * @return the bottom padding in pixels
     */
    public int getPaddingBottom() {
        return mPaddingBottom;
    }

    /**
     * Returns the left padding of this view. If there are inset and enabled
     * scrollbars, this value may include the space required to display the
     * scrollbars as well.
     *
     * @return the left padding in pixels
     */
    public int getPaddingLeft() {
        if (!isPaddingResolved()) {
            resolvePadding();
        }
        return mPaddingLeft;
    }

    /**
     * Returns the right padding of this view. If there are inset and enabled
     * scrollbars, this value may include the space required to display the
     * scrollbars as well.
     *
     * @return the right padding in pixels
     */
    public int getPaddingRight() {
        if (!isPaddingResolved()) {
            resolvePadding();
        }
    	return mPaddingRight;
    }
    
    /**
     * Returns the start padding of this view depending on its resolved layout direction.
     * If there are inset and enabled scrollbars, this value may include the space
     * required to display the scrollbars as well.
     *
     * @return the start padding in pixels
     */
    public int getPaddingStart() {
        if (!isPaddingResolved()) {
            resolvePadding();
        }
        return (getLayoutDirection() == LAYOUT_DIRECTION_RTL) ?
                mPaddingRight : mPaddingLeft;
    }

    /**
     * Returns the end padding of this view depending on its resolved layout direction.
     * If there are inset and enabled scrollbars, this value may include the space
     * required to display the scrollbars as well.
     *
     * @return the end padding in pixels
     */
    public int getPaddingEnd() {
        if (!isPaddingResolved()) {
            resolvePadding();
        }
        return (getLayoutDirection() == LAYOUT_DIRECTION_RTL) ?
                mPaddingLeft : mPaddingRight;
    }
    
    /**
     * <p>Indicates whether or not this view's layout will be requested during
     * the next hierarchy layout pass.</p>
     *
     * @return true if the layout will be forced during next layout pass
     */
    public boolean isLayoutRequested() {
        return (mPrivateFlags & PFLAG_FORCE_LAYOUT) == PFLAG_FORCE_LAYOUT;
    }

	public void layout(int left, int top, int right, int bottom) {
    	if ((mPrivateFlags3 & PFLAG3_MEASURE_NEEDED_BEFORE_LAYOUT) != 0) {
            onMeasure(mOldWidthMeasureSpec, mOldHeightMeasureSpec);
            mPrivateFlags3 &= ~PFLAG3_MEASURE_NEEDED_BEFORE_LAYOUT;
        }
    	
        int oldL = mLeft;
        int oldT = mTop;
        int oldB = mBottom;
        int oldR = mRight;
    	
        boolean sizeChanged = setFrame(left, top, right, bottom);
        // We call onLayout no matter sizeChanged is true or not because the
        // orientation may change without changing the size of the View (for
        // example, rotate the device by 180 degrees), and we want to handle
        // orientation change in onLayout.
        if(sizeChanged || (mPrivateFlags & PFLAG_LAYOUT_REQUIRED) == PFLAG_LAYOUT_REQUIRED){
        	onLayout(sizeChanged, left, top, right, bottom);
        	mPrivateFlags &= ~PFLAG_LAYOUT_REQUIRED;
        	
            ListenerInfo li = mListenerInfo;
            if (li != null && li.mOnLayoutChangeListeners != null) {
                ArrayList<OnLayoutChangeListener> listenersCopy =
                        (ArrayList<OnLayoutChangeListener>)li.mOnLayoutChangeListeners.clone();
                int numListeners = listenersCopy.size();
                for (int i = 0; i < numListeners; ++i) {
                    listenersCopy.get(i).onLayoutChange(this, left, top, right, bottom, oldL, oldT, oldR, oldB);
                }
            }
        }
        
        mPrivateFlags &= ~PFLAG_FORCE_LAYOUT;
        mPrivateFlags3 |= PFLAG3_IS_LAID_OUT;
    }
    
    /**
     * Assign a size and position to this view.
     *
     * This is called from layout.
     *
     * @param left Left position, relative to parent
     * @param top Top position, relative to parent
     * @param right Right position, relative to parent
     * @param bottom Bottom position, relative to parent
     * @return true if the new size and position are different than the
     *         previous ones
     */
	protected boolean setFrame(int left, int top, int right, int bottom) {
		boolean changed = false;

		if (mLeft != left || mRight != right || mTop != top || mBottom != bottom) {
			changed = true;
			int oldWidth = mRight - mLeft;
			int oldHeight = mBottom - mTop;
			int newWidth = right - left;
			int newHeight = bottom - top;
			boolean sizeChanged = (newWidth != oldWidth)
					|| (newHeight != oldHeight);
			
			// Invalidate our old position
            invalidate(sizeChanged);

			mLeft = left;
			mTop = top;
			mRight = right;
			mBottom = bottom;
			mRenderNode.setLeftTopRightBottom(left, top, right, bottom);
			
	        mXCenter = mLeft + ((mRight - mLeft) >> 1);
	        mYCenter = mTop + ((mBottom - mTop) >> 1);

			mPrivateFlags |= PFLAG_HAS_BOUNDS;

			if (sizeChanged) {
                sizeChange(newWidth, newHeight, oldWidth, oldHeight);
            }
			
			mBackgroundSizeChanged = true;
		}
		return changed;
	}

	private void sizeChange(int newWidth, int newHeight, int oldWidth, int oldHeight) {
        onSizeChanged(newWidth, newHeight, oldWidth, oldHeight);
        if (mOverlay != null) {
            mOverlay.getOverlayView().setRight(newWidth);
            mOverlay.getOverlayView().setBottom(newHeight);
        }
    }

	/**
	 * This is called during layout when the size of this view has changed. If
	 * you were just added to the view hierarchy, you're called with the old
	 * values of 0.
	 * 
	 * @param w
	 *            Current width of this view.
	 * @param h
	 *            Current height of this view.
	 * @param oldw
	 *            Old width of this view.
	 * @param oldh
	 *            Old height of this view.
	 */
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {}
    
	/**
     * Called by draw to draw the child views. This may be overridden
     * by derived classes to gain control just before its children are drawn
     * (but after its own view has been drawn).
     * @param canvas the canvas on which to draw the view
     */
    protected void dispatchDraw(GLCanvas canvas) {
    }
    
    /**
     * <p>
     * This is called to find out how big a view should be. The parent
     * supplies constraint information in the width and height parameters.
     * </p>
     *
     * <p>
     * The actual measurement work of a view is performed in
     * {@link #onMeasure(int, int)}, called by this method. Therefore, only
     * {@link #onMeasure(int, int)} can and must be overridden by subclasses.
     * </p>
     *
     *
     * @param widthMeasureSpec Horizontal space requirements as imposed by the
     *        parent
     * @param heightMeasureSpec Vertical space requirements as imposed by the
     *        parent
     *
     * @see #onMeasure(int, int)
     */
    @SuppressLint("WrongCall")
    public final void measure(int widthMeasureSpec, int heightMeasureSpec) {
        if ((mPrivateFlags & PFLAG_FORCE_LAYOUT) == PFLAG_FORCE_LAYOUT ||
                widthMeasureSpec != mOldWidthMeasureSpec ||
                heightMeasureSpec != mOldHeightMeasureSpec) {

        	resolveRtlPropertiesIfNeeded();
        	
            // first clears the measured dimension flag
            mPrivateFlags &= ~PFLAG_MEASURED_DIMENSION_SET;
            
            // measure ourselves, this should set the measured dimension flag back
            onMeasure(widthMeasureSpec, heightMeasureSpec);

            // flag not set, setMeasuredDimension() was not invoked, we raise
            // an exception to warn the developer
            if ((mPrivateFlags & PFLAG_MEASURED_DIMENSION_SET) != PFLAG_MEASURED_DIMENSION_SET) {
                throw new IllegalStateException("" + this + "onMeasure() did not set the"
                        + " measured dimension by calling"
                        + " setMeasuredDimension()");
            }

            mPrivateFlags |= PFLAG_LAYOUT_REQUIRED;
        }

        mOldWidthMeasureSpec = widthMeasureSpec;
        mOldHeightMeasureSpec = heightMeasureSpec;
    }
    
//    protected void loadBackgroundTexture(int resourceId){
///*    	int width = MeasureSpec.getSize(widthMeasureSpec);
//    	int height = MeasureSpec.getSize(heightMeasureSpec);*/
//    	if(mSetBackgroundType != NO_BACKGROUND && mBackgroundTexture == null){
//    		switch(mSetBackgroundType){
//    		case SET_BACKGROUND_BITMAP:
//    			mBackgroundTexture = createTexture(mBackgroundBitmap, CREATE_BACKGROUND);				
//    			break;
//    		case SET_BACKGROUND_RESOURCE:
//
//    			break;
//    		}
//    	}
//    }
    
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
    }

    /**
     * <p>This mehod must be called by {@link #onMeasure(int, int)} to store the
     * measured width and measured height. Failing to do so will trigger an
     * exception at measurement time.</p>
     *
     * @param measuredWidth The measured width of this view.  May be a complex
     * bit mask as defined by {@link #MEASURED_SIZE_MASK} and
     * {@link #MEASURED_STATE_TOO_SMALL}.
     * @param measuredHeight The measured height of this view.  May be a complex
     * bit mask as defined by {@link #MEASURED_SIZE_MASK} and
     * {@link #MEASURED_STATE_TOO_SMALL}.
     */
    protected final void setMeasuredDimension(int measuredWidth, int measuredHeight) {
        mMeasuredWidth = measuredWidth;
        mMeasuredHeight = measuredHeight;

        mPrivateFlags |= PFLAG_MEASURED_DIMENSION_SET;
    }
    
    /**
     * Merge two states as returned by {@link #getMeasuredState()}.
     * @param curState The current state as returned from a view or the result
     * of combining multiple views.
     * @param newState The new view state to combine.
     * @return Returns a new integer reflecting the combination of the two
     * states.
     */
    public static int combineMeasuredStates(int curState, int newState) {
        return curState | newState;
    }
    
    /**
     * Version of {@link #resolveSizeAndState(int, int, int)}
     * returning only the {@link #MEASURED_SIZE_MASK} bits of the result.
     */
    public static int resolveSize(int size, int measureSpec) {
        return resolveSizeAndState(size, measureSpec, 0) & MEASURED_SIZE_MASK;
    }

    /**
     * Like {@link #getMeasuredWidthAndState()}, but only returns the
     * raw width component (that is the result is masked by
     * {@link #MEASURED_SIZE_MASK}).
     *
     * @return The raw measured width of this view.
     */
    public int getMeasuredWidth() {
        return mMeasuredWidth & MEASURED_SIZE_MASK;
    }
    
    /**
     * Return the full width measurement information for this view as computed
     * by the most recent call to {@link #measure(int, int)}.  This result is a bit mask
     * as defined by {@link #MEASURED_SIZE_MASK} and {@link #MEASURED_STATE_TOO_SMALL}.
     * This should be used during measurement and layout calculations only. Use
     * {@link #getWidth()} to see how wide a view is after layout.
     *
     * @return The measured width of this view as a bit mask.
     */
    public final int getMeasuredWidthAndState() {
        return mMeasuredWidth;
    }

    /**
     * Like {@link #getMeasuredHeightAndState()}, but only returns the
     * raw width component (that is the result is masked by
     * {@link #MEASURED_SIZE_MASK}).
     *
     * @return The raw measured height of this view.
     */
    public int getMeasuredHeight() {
        return mMeasuredHeight & MEASURED_SIZE_MASK;
    }
    
    /**
     * Return the full height measurement information for this view as computed
     * by the most recent call to {@link #measure(int, int)}.  This result is a bit mask
     * as defined by {@link #MEASURED_SIZE_MASK} and {@link #MEASURED_STATE_TOO_SMALL}.
     * This should be used during measurement and layout calculations only. Use
     * {@link #getHeight()} to see how wide a view is after layout.
     *
     * @return The measured width of this view as a bit mask.
     */
    public final int getMeasuredHeightAndState() {
        return mMeasuredHeight;
    }

    /**
     * Return only the state bits of {@link #getMeasuredWidthAndState()}
     * and {@link #getMeasuredHeightAndState()}, combined into one integer.
     * The width component is in the regular bits {@link #MEASURED_STATE_MASK}
     * and the height component is at the shifted bits
     * {@link #MEASURED_HEIGHT_STATE_SHIFT}>>{@link #MEASURED_STATE_MASK}.
     */
    public final int getMeasuredState() {
        return (mMeasuredWidth&MEASURED_STATE_MASK)
                | ((mMeasuredHeight>>MEASURED_HEIGHT_STATE_SHIFT)
                        & (MEASURED_STATE_MASK>>MEASURED_HEIGHT_STATE_SHIFT));
    }
    
    /**
     * Utility to reconcile a desired size and state, with constraints imposed
     * by a MeasureSpec.  Will take the desired size, unless a different size
     * is imposed by the constraints.  The returned value is a compound integer,
     * with the resolved size in the {@link #MEASURED_SIZE_MASK} bits and
     * optionally the bit {@link #MEASURED_STATE_TOO_SMALL} set if the resulting
     * size is smaller than the size the view wants to be.
     *
     * @param size How big the view wants to be
     * @param measureSpec Constraints imposed by the parent
     * @return Size information bit mask as defined by
     * {@link #MEASURED_SIZE_MASK} and {@link #MEASURED_STATE_TOO_SMALL}.
     */
    public static int resolveSizeAndState(int size, int measureSpec, int childMeasuredState) {
        int result = size;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize =  MeasureSpec.getSize(measureSpec);
        switch (specMode) {
        case MeasureSpec.UNSPECIFIED:
            result = size;
            break;
        case MeasureSpec.AT_MOST:
            if (specSize < size) {
                result = specSize | MEASURED_STATE_TOO_SMALL;
            } else {
                result = size;
            }
            break;
        case MeasureSpec.EXACTLY:
            result = specSize;
            break;
        }
        return result | (childMeasuredState&MEASURED_STATE_MASK);
    }
    
    /**
     * Returns the suggested minimum height that the view should use. This
     * returns the maximum of the view's minimum height
     * and the background's minimum height
     * ({@link android.graphics.drawable.Drawable#getMinimumHeight()}).
     * <p>
     * When being used in {@link #onMeasure(int, int)}, the caller should still
     * ensure the returned height is within the requirements of the parent.
     *
     * @return The suggested minimum height of the view.
     */
    protected int getSuggestedMinimumHeight() {
        return (mBackground == null) ? mMinHeight : max(mMinHeight, mBackground.getMinimumHeight());
    }

    /**
     * Returns the suggested minimum width that the view should use. This
     * returns the maximum of the view's minimum width)
     * and the background's minimum width
     *  ({@link android.graphics.drawable.Drawable#getMinimumWidth()}).
     * <p>
     * When being used in {@link #onMeasure(int, int)}, the caller should still
     * ensure the returned width is within the requirements of the parent.
     *
     * @return The suggested minimum width of the view.
     */
    protected int getSuggestedMinimumWidth() {
        return (mBackground == null) ? mMinWidth : max(mMinWidth, mBackground.getMinimumWidth());
    }
    
    /**
     * Returns the minimum height of the view.
     *
     * @return the minimum height the view will try to be.
     *
     * @see #setMinimumHeight(int)
     *
     * @attr ref android.R.styleable#View_minHeight
     */
    public int getMinimumHeight() {
        return mMinHeight;
    }

    /**
     * Sets the minimum height of the view. It is not guaranteed the view will
     * be able to achieve this minimum height (for example, if its parent layout
     * constrains it with less available height).
     *
     * @param minHeight The minimum height the view will try to be.
     *
     * @see #getMinimumHeight()
     *
     * @attr ref android.R.styleable#View_minHeight
     */
    public void setMinimumHeight(int minHeight) {
        mMinHeight = minHeight;
        requestLayout();
    }

    /**
     * Returns the minimum width of the view.
     *
     * @return the minimum width the view will try to be.
     *
     * @see #setMinimumWidth(int)
     *
     * @attr ref android.R.styleable#View_minWidth
     */
    public int getMinimumWidth() {
        return mMinWidth;
    }

    /**
     * Sets the minimum width of the view. It is not guaranteed the view will
     * be able to achieve this minimum width (for example, if its parent layout
     * constrains it with less available width).
     *
     * @param minWidth The minimum width the view will try to be.
     *
     * @see #getMinimumWidth()
     *
     * @attr ref android.R.styleable#View_minWidth
     */
    public void setMinimumWidth(int minWidth) {
        mMinWidth = minWidth;
        requestLayout();

    }

    /**
     * Called from layout when this view should
     * assign a size and position to each of its children.
     *
     * Derived classes with children should override
     * this method and call layout on each of
     * their children.
     * @param changed This is a new size or position for this view
     * @param left Left position, relative to parent
     * @param top Top position, relative to parent
     * @param right Right position, relative to parent
     * @param bottom Bottom position, relative to parent
     */
    protected void onLayout(boolean changeSize,
    		int left, int top, int right, int bottom) {
    }

    /**
     * <p>Indicate whether the horizontal scrollbar should be drawn or not. The
     * scrollbar is not drawn by default.</p>
     *
     * @return true if the horizontal scrollbar should be painted, false
     *         otherwise
     *
     * @see #setHorizontalScrollBarEnabled(boolean)
     */
    public boolean isHorizontalScrollBarEnabled() {
        return (mViewFlags & SCROLLBARS_HORIZONTAL) == SCROLLBARS_HORIZONTAL;
    }

    /**
     * <p>Define whether the horizontal scrollbar should be drawn or not. The
     * scrollbar is not drawn by default.</p>
     *
     * @param horizontalScrollBarEnabled true if the horizontal scrollbar should
     *                                   be painted
     *
     * @see #isHorizontalScrollBarEnabled()
     */
    public void setHorizontalScrollBarEnabled(boolean horizontalScrollBarEnabled) {
        if (isHorizontalScrollBarEnabled() != horizontalScrollBarEnabled) {
            mViewFlags ^= SCROLLBARS_HORIZONTAL;
            computeOpaqueFlags();
            resolvePadding();
        }
    }
    
    /**
     * Returns the strength, or intensity, of the top faded edge. The strength is
     * a value between 0.0 (no fade) and 1.0 (full fade). The default implementation
     * returns 0.0 or 1.0 but no value in between.
     *
     * Subclasses should override this method to provide a smoother fade transition
     * when scrolling occurs.
     *
     * @return the intensity of the top fade as a float between 0.0f and 1.0f
     */
    protected float getTopFadingEdgeStrength() {
        return computeVerticalScrollOffset() > 0 ? 1.0f : 0.0f;
    }

    /**
     * Returns the strength, or intensity, of the bottom faded edge. The strength is
     * a value between 0.0 (no fade) and 1.0 (full fade). The default implementation
     * returns 0.0 or 1.0 but no value in between.
     *
     * Subclasses should override this method to provide a smoother fade transition
     * when scrolling occurs.
     *
     * @return the intensity of the bottom fade as a float between 0.0f and 1.0f
     */
    protected float getBottomFadingEdgeStrength() {
        return computeVerticalScrollOffset() + computeVerticalScrollExtent() <
                computeVerticalScrollRange() ? 1.0f : 0.0f;
    }

    /**
     * Returns the strength, or intensity, of the left faded edge. The strength is
     * a value between 0.0 (no fade) and 1.0 (full fade). The default implementation
     * returns 0.0 or 1.0 but no value in between.
     *
     * Subclasses should override this method to provide a smoother fade transition
     * when scrolling occurs.
     *
     * @return the intensity of the left fade as a float between 0.0f and 1.0f
     */
    protected float getLeftFadingEdgeStrength() {
        return computeHorizontalScrollOffset() > 0 ? 1.0f : 0.0f;
    }

    /**
     * Returns the strength, or intensity, of the right faded edge. The strength is
     * a value between 0.0 (no fade) and 1.0 (full fade). The default implementation
     * returns 0.0 or 1.0 but no value in between.
     *
     * Subclasses should override this method to provide a smoother fade transition
     * when scrolling occurs.
     *
     * @return the intensity of the right fade as a float between 0.0f and 1.0f
     */
    protected float getRightFadingEdgeStrength() {
        return computeHorizontalScrollOffset() + computeHorizontalScrollExtent() <
                computeHorizontalScrollRange() ? 1.0f : 0.0f;
    }

    /**
     * <p>Indicate whether the vertical scrollbar should be drawn or not. The
     * scrollbar is not drawn by default.</p>
     *
     * @return true if the vertical scrollbar should be painted, false
     *         otherwise
     *
     * @see #setVerticalScrollBarEnabled(boolean)
     */
    public boolean isVerticalScrollBarEnabled() {
        return (mViewFlags & SCROLLBARS_VERTICAL) == SCROLLBARS_VERTICAL;
    }

    /**
     * <p>Define whether the vertical scrollbar should be drawn or not. The
     * scrollbar is not drawn by default.</p>
     *
     * @param verticalScrollBarEnabled true if the vertical scrollbar should
     *                                 be painted
     *
     * @see #isVerticalScrollBarEnabled()
     */
    public void setVerticalScrollBarEnabled(boolean verticalScrollBarEnabled) {
        if (isVerticalScrollBarEnabled() != verticalScrollBarEnabled) {
            mViewFlags ^= SCROLLBARS_VERTICAL;
            computeOpaqueFlags();
            resolvePadding();
        }
    }

    /**
     * @hide
     */
    protected void recomputePadding() {
        internalSetPadding(mUserPaddingLeft, mPaddingTop, mUserPaddingRight, mUserPaddingBottom);
    }

    /**
     * Define whether scrollbars will fade when the view is not scrolling.
     *
     * @param fadeScrollbars wheter to enable fading
     *
     * @attr ref android.R.styleable#View_fadeScrollbars
     */
    public void setScrollbarFadingEnabled(boolean fadeScrollbars) {
        initScrollCache();
        final ScrollabilityCache scrollabilityCache = mScrollCache;
        scrollabilityCache.fadeScrollBars = fadeScrollbars;
        if (fadeScrollbars) {
            scrollabilityCache.state = ScrollabilityCache.OFF;
        } else {
            scrollabilityCache.state = ScrollabilityCache.ON;
        }
    }

    /**
     *
     * Returns true if scrollbars will fade when this view is not scrolling
     *
     * @return true if scrollbar fading is enabled
     *
     * @attr ref android.R.styleable#View_fadeScrollbars
     */
    public boolean isScrollbarFadingEnabled() {
        return mScrollCache != null && mScrollCache.fadeScrollBars;
    }

    /**
     *
     * Returns the delay before scrollbars fade.
     *
     * @return the delay before scrollbars fade
     *
     * @attr ref android.R.styleable#View_scrollbarDefaultDelayBeforeFade
     */
    public int getScrollBarDefaultDelayBeforeFade() {
        return mScrollCache == null ? ViewConfiguration.getScrollDefaultDelay() :
                mScrollCache.scrollBarDefaultDelayBeforeFade;
    }

    /**
     * Define the delay before scrollbars fade.
     *
     * @param scrollBarDefaultDelayBeforeFade - the delay before scrollbars fade
     *
     * @attr ref android.R.styleable#View_scrollbarDefaultDelayBeforeFade
     */
    public void setScrollBarDefaultDelayBeforeFade(int scrollBarDefaultDelayBeforeFade) {
        getScrollCache().scrollBarDefaultDelayBeforeFade = scrollBarDefaultDelayBeforeFade;
    }

    /**
     *
     * Returns the scrollbar fade duration.
     *
     * @return the scrollbar fade duration
     *
     * @attr ref android.R.styleable#View_scrollbarFadeDuration
     */
    public int getScrollBarFadeDuration() {
        return mScrollCache == null ? ViewConfiguration.getScrollBarFadeDuration() :
                mScrollCache.scrollBarFadeDuration;
    }

    /**
     * Define the scrollbar fade duration.
     *
     * @param scrollBarFadeDuration - the scrollbar fade duration
     *
     * @attr ref android.R.styleable#View_scrollbarFadeDuration
     */
    public void setScrollBarFadeDuration(int scrollBarFadeDuration) {
        getScrollCache().scrollBarFadeDuration = scrollBarFadeDuration;
    }

    /**
     *
     * Returns the scrollbar size.
     *
     * @return the scrollbar size
     *
     * @attr ref android.R.styleable#View_scrollbarSize
     */
    public int getScrollBarSize() {
        return mScrollCache == null ? ViewConfiguration.get(mContext).getScaledScrollBarSize() :
                mScrollCache.scrollBarSize;
    }

    /**
     * Define the scrollbar size.
     *
     * @param scrollBarSize - the scrollbar size
     *
     * @attr ref android.R.styleable#View_scrollbarSize
     */
    public void setScrollBarSize(int scrollBarSize) {
        getScrollCache().scrollBarSize = scrollBarSize;
    }

    /**
     * <p>Specify the style of the scrollbars. The scrollbars can be overlaid or
     * inset. When inset, they add to the padding of the view. And the scrollbars
     * can be drawn inside the padding area or on the edge of the view. For example,
     * if a view has a background drawable and you want to draw the scrollbars
     * inside the padding specified by the drawable, you can use
     * SCROLLBARS_INSIDE_OVERLAY or SCROLLBARS_INSIDE_INSET. If you want them to
     * appear at the edge of the view, ignoring the padding, then you can use
     * SCROLLBARS_OUTSIDE_OVERLAY or SCROLLBARS_OUTSIDE_INSET.</p>
     * @param style the style of the scrollbars. Should be one of
     * SCROLLBARS_INSIDE_OVERLAY, SCROLLBARS_INSIDE_INSET,
     * SCROLLBARS_OUTSIDE_OVERLAY or SCROLLBARS_OUTSIDE_INSET.
     * @see #SCROLLBARS_INSIDE_OVERLAY
     * @see #SCROLLBARS_INSIDE_INSET
     * @see #SCROLLBARS_OUTSIDE_OVERLAY
     * @see #SCROLLBARS_OUTSIDE_INSET
     *
     * @attr ref android.R.styleable#View_scrollbarStyle
     */
    public void setScrollBarStyle(int style) {
        if (style != (mViewFlags & SCROLLBARS_STYLE_MASK)) {
            mViewFlags = (mViewFlags & ~SCROLLBARS_STYLE_MASK) | (style & SCROLLBARS_STYLE_MASK);
            computeOpaqueFlags();
            resolvePadding();
        }
    }

    /**
     * <p>Returns the current scrollbar style.</p>
     * @return the current scrollbar style
     * @see #SCROLLBARS_INSIDE_OVERLAY
     * @see #SCROLLBARS_INSIDE_INSET
     * @see #SCROLLBARS_OUTSIDE_OVERLAY
     * @see #SCROLLBARS_OUTSIDE_INSET
     *
     * @attr ref android.R.styleable#View_scrollbarStyle
     */
    public int getScrollBarStyle() {
        return mViewFlags & SCROLLBARS_STYLE_MASK;
    }

    /**
     * <p>Compute the horizontal range that the horizontal scrollbar
     * represents.</p>
     *
     * <p>The range is expressed in arbitrary units that must be the same as the
     * units used by {@link #computeHorizontalScrollExtent()} and
     * {@link #computeHorizontalScrollOffset()}.</p>
     *
     * <p>The default range is the drawing width of this view.</p>
     *
     * @return the total horizontal range represented by the horizontal
     *         scrollbar
     *
     * @see #computeHorizontalScrollExtent()
     * @see #computeHorizontalScrollOffset()
     * @see android.widget.ScrollBarDrawable
     */
    protected int computeHorizontalScrollRange() {
        return getWidth();
    }

    /**
     * <p>Compute the horizontal offset of the horizontal scrollbar's thumb
     * within the horizontal range. This value is used to compute the position
     * of the thumb within the scrollbar's track.</p>
     *
     * <p>The range is expressed in arbitrary units that must be the same as the
     * units used by {@link #computeHorizontalScrollRange()} and
     * {@link #computeHorizontalScrollExtent()}.</p>
     *
     * <p>The default offset is the scroll offset of this view.</p>
     *
     * @return the horizontal offset of the scrollbar's thumb
     *
     * @see #computeHorizontalScrollRange()
     * @see #computeHorizontalScrollExtent()
     * @see android.widget.ScrollBarDrawable
     */
    protected int computeHorizontalScrollOffset() {
        return mScrollX;
    }

    /**
     * <p>Compute the horizontal extent of the horizontal scrollbar's thumb
     * within the horizontal range. This value is used to compute the length
     * of the thumb within the scrollbar's track.</p>
     *
     * <p>The range is expressed in arbitrary units that must be the same as the
     * units used by {@link #computeHorizontalScrollRange()} and
     * {@link #computeHorizontalScrollOffset()}.</p>
     *
     * <p>The default extent is the drawing width of this view.</p>
     *
     * @return the horizontal extent of the scrollbar's thumb
     *
     * @see #computeHorizontalScrollRange()
     * @see #computeHorizontalScrollOffset()
     * @see android.widget.ScrollBarDrawable
     */
    protected int computeHorizontalScrollExtent() {
        return getWidth();
    }

    /**
     * <p>Compute the vertical range that the vertical scrollbar represents.</p>
     *
     * <p>The range is expressed in arbitrary units that must be the same as the
     * units used by {@link #computeVerticalScrollExtent()} and
     * {@link #computeVerticalScrollOffset()}.</p>
     *
     * @return the total vertical range represented by the vertical scrollbar
     *
     * <p>The default range is the drawing height of this view.</p>
     *
     * @see #computeVerticalScrollExtent()
     * @see #computeVerticalScrollOffset()
     * @see android.widget.ScrollBarDrawable
     */
    protected int computeVerticalScrollRange() {
        return getHeight();
    }

    /**
     * <p>Compute the vertical offset of the vertical scrollbar's thumb
     * within the horizontal range. This value is used to compute the position
     * of the thumb within the scrollbar's track.</p>
     *
     * <p>The range is expressed in arbitrary units that must be the same as the
     * units used by {@link #computeVerticalScrollRange()} and
     * {@link #computeVerticalScrollExtent()}.</p>
     *
     * <p>The default offset is the scroll offset of this view.</p>
     *
     * @return the vertical offset of the scrollbar's thumb
     *
     * @see #computeVerticalScrollRange()
     * @see #computeVerticalScrollExtent()
     * @see android.widget.ScrollBarDrawable
     */
    protected int computeVerticalScrollOffset() {
        return mScrollY;
    }

    /**
     * <p>Compute the vertical extent of the vertical scrollbar's thumb
     * within the vertical range. This value is used to compute the length
     * of the thumb within the scrollbar's track.</p>
     *
     * <p>The range is expressed in arbitrary units that must be the same as the
     * units used by {@link #computeVerticalScrollRange()} and
     * {@link #computeVerticalScrollOffset()}.</p>
     *
     * <p>The default extent is the drawing height of this view.</p>
     *
     * @return the vertical extent of the scrollbar's thumb
     *
     * @see #computeVerticalScrollRange()
     * @see #computeVerticalScrollOffset()
     * @see android.widget.ScrollBarDrawable
     */
    protected int computeVerticalScrollExtent() {
        return getHeight();
    }

    /**
     * Check if this view can be scrolled horizontally in a certain direction.
     *
     * @param direction Negative to check scrolling left, positive to check scrolling right.
     * @return true if this view can be scrolled in the specified direction, false otherwise.
     */
    public boolean canScrollHorizontally(int direction) {
        final int offset = computeHorizontalScrollOffset();
        final int range = computeHorizontalScrollRange() - computeHorizontalScrollExtent();
        if (range == 0) return false;
        if (direction < 0) {
            return offset > 0;
        } else {
            return offset < range - 1;
        }
    }

    /**
     * Check if this view can be scrolled vertically in a certain direction.
     *
     * @param direction Negative to check scrolling up, positive to check scrolling down.
     * @return true if this view can be scrolled in the specified direction, false otherwise.
     */
    public boolean canScrollVertically(int direction) {
        final int offset = computeVerticalScrollOffset();
        final int range = computeVerticalScrollRange() - computeVerticalScrollExtent();
        if (range == 0) return false;
        if (direction < 0) {
            return offset > 0;
        } else {
            return offset < range - 1;
        }
    }

    /**
     * <p>Request the drawing of the horizontal and the vertical scrollbar. The
     * scrollbars are painted only if they have been awakened first.</p>
     *
     * @param canvas the canvas on which to draw the scrollbars
     *
     * @see #awakenScrollBars(int)
     */
    protected final void onDrawScrollBars(GLCanvas canvas) {
        // scrollbars are drawn only when the animation is running
        final ScrollabilityCache cache = mScrollCache;
        if (cache != null) {

            int state = cache.state;

            if (state == ScrollabilityCache.OFF) {
                return;
            }

            boolean invalidate = false;

            if (state == ScrollabilityCache.FADING) {
                // We're fading -- get our fade interpolation
                if (cache.interpolatorValues == null) {
                    cache.interpolatorValues = new float[1];
                }

                float[] values = cache.interpolatorValues;

                // Stops the animation if we're done
                if (cache.scrollBarInterpolator.timeToValues(values) ==
                        Interpolator.Result.FREEZE_END) {
                    cache.state = ScrollabilityCache.OFF;
                } else {
                    cache.scrollBar.setAlpha(Math.round(values[0]));
                }

                // This will make the scroll bars inval themselves after
                // drawing. We only want this when we're fading so that
                // we prevent excessive redraws
                invalidate = true;
            } else {
                // We're just on -- but we may have been fading before so
                // reset alpha
                cache.scrollBar.setAlpha(255);
            }


            final int viewFlags = mViewFlags;

            final boolean drawHorizontalScrollBar =
                (viewFlags & SCROLLBARS_HORIZONTAL) == SCROLLBARS_HORIZONTAL;
            final boolean drawVerticalScrollBar =
                (viewFlags & SCROLLBARS_VERTICAL) == SCROLLBARS_VERTICAL
                && !isVerticalScrollBarHidden();

            if (drawVerticalScrollBar || drawHorizontalScrollBar) {
                final int width = mRight - mLeft;
                final int height = mBottom - mTop;

                final ScrollBarDrawable scrollBar = cache.scrollBar;

                final int scrollX = mScrollX;
                final int scrollY = mScrollY;
                final int inside = (viewFlags & SCROLLBARS_OUTSIDE_MASK) == 0 ? ~0 : 0;

                int left;
                int top;
                int right;
                int bottom;

                if (drawHorizontalScrollBar) {
                    int size = scrollBar.getSize(false);
                    if (size <= 0) {
                        size = cache.scrollBarSize;
                    }

                    scrollBar.setParameters(computeHorizontalScrollRange(),
                                            computeHorizontalScrollOffset(),
                                            computeHorizontalScrollExtent(), false);
                    final int verticalScrollBarGap = drawVerticalScrollBar ?
                            getVerticalScrollbarWidth() : 0;
                    top = scrollY + height - size - (mUserPaddingBottom & inside);
                    left = scrollX + (mPaddingLeft & inside);
                    right = scrollX + width - (mUserPaddingRight & inside) - verticalScrollBarGap;
                    bottom = top + size;
                    onDrawHorizontalScrollBar(canvas, scrollBar, left, top, right, bottom);
                    if (invalidate) {
                        invalidate(left, top, right, bottom);
                    }
                }

                if (drawVerticalScrollBar) {
                    int size = scrollBar.getSize(true);
                    if (size <= 0) {
                        size = cache.scrollBarSize;
                    }

                    scrollBar.setParameters(computeVerticalScrollRange(),
                                            computeVerticalScrollOffset(),
                                            computeVerticalScrollExtent(), true);
                    int verticalScrollbarPosition = mVerticalScrollbarPosition;
                    if (verticalScrollbarPosition == SCROLLBAR_POSITION_DEFAULT) {
                        verticalScrollbarPosition = isLayoutRtl() ?
                                SCROLLBAR_POSITION_LEFT : SCROLLBAR_POSITION_RIGHT;
                    }
                    switch (verticalScrollbarPosition) {
                        default:
                        case SCROLLBAR_POSITION_RIGHT:
                            left = scrollX + width - size - (mUserPaddingRight & inside);
                            break;
                        case SCROLLBAR_POSITION_LEFT:
                            left = scrollX + (mUserPaddingLeft & inside);
                            break;
                    }
                    top = scrollY + (mPaddingTop & inside);
                    right = left + size;
                    bottom = scrollY + height - (mUserPaddingBottom & inside);
                    onDrawVerticalScrollBar(canvas, scrollBar, left, top, right, bottom);
                    if (invalidate) {
                        invalidate(left, top, right, bottom);
                    }
                }
            }
        }
    }

    /**
     * Override this if the vertical scrollbar needs to be hidden in a subclass, like when
     * FastScroller is visible.
     * @return whether to temporarily hide the vertical scrollbar
     * @hide
     */
    protected boolean isVerticalScrollBarHidden() {
        return false;
    }

    /**
     * <p>Draw the horizontal scrollbar if
     * {@link #isHorizontalScrollBarEnabled()} returns true.</p>
     *
     * @param canvas the canvas on which to draw the scrollbar
     * @param scrollBar the scrollbar's drawable
     *
     * @see #isHorizontalScrollBarEnabled()
     * @see #computeHorizontalScrollRange()
     * @see #computeHorizontalScrollExtent()
     * @see #computeHorizontalScrollOffset()
     * @see android.widget.ScrollBarDrawable
     * @hide
     */
    protected void onDrawHorizontalScrollBar(GLCanvas canvas, Drawable scrollBar,
            int l, int t, int r, int b) {
        scrollBar.setBounds(l, t, r, b);
        scrollBar.draw(canvas);
    }

    /**
     * <p>Draw the vertical scrollbar if {@link #isVerticalScrollBarEnabled()}
     * returns true.</p>
     *
     * @param canvas the canvas on which to draw the scrollbar
     * @param scrollBar the scrollbar's drawable
     *
     * @see #isVerticalScrollBarEnabled()
     * @see #computeVerticalScrollRange()
     * @see #computeVerticalScrollExtent()
     * @see #computeVerticalScrollOffset()
     * @see android.widget.ScrollBarDrawable
     * @hide
     */
    protected void onDrawVerticalScrollBar(GLCanvas canvas, Drawable scrollBar,
            int l, int t, int r, int b) {
        scrollBar.setBounds(l, t, r, b);
        scrollBar.draw(canvas);
    }
    
    /**
     * Implement this to do your drawing.
     *
     * @param canvas the canvas on which the background will be drawn
     */
    protected void onDraw(GLCanvas canvas) {
    }

    /**
     * This is called when the view is attached to a window.  At this point it
     * has a Surface and will start drawing.  Note that this function is
     * guaranteed to be called before {@link #onDraw(android.graphics.Canvas)},
     * however it may be called any time before the first onDraw -- including
     * before or after {@link #onMeasure(int, int)}.
     *
     * @see #onDetachedFromWindow()
     */
    protected void onAttachedToWindow() {
    	if ((mPrivateFlags & PFLAG_AWAKEN_SCROLL_BARS_ON_ATTACH) != 0) {
            initialAwakenScrollBars();
            mPrivateFlags &= ~PFLAG_AWAKEN_SCROLL_BARS_ON_ATTACH;
        }
    	mPrivateFlags3 &= ~PFLAG3_IS_LAID_OUT;
    }
    
    /**
     * Returns true if this view is currently attached to a window.
     */
    public boolean isAttachedToWindow() {
        return mAttachInfo != null;
    }
    
    /**
     * Returns true if this view has been through at least one layout since it
     * was last attached to or detached from a window.
     */
    public boolean isLaidOut() {
        return (mPrivateFlags3 & PFLAG3_IS_LAID_OUT) == PFLAG3_IS_LAID_OUT;
    }

    /**
     * If this view doesn't do any drawing on its own, set this flag to
     * allow further optimizations. By default, this flag is not set on
     * View, but could be set on some View subclasses such as ViewGroup.
     *
     * Typically, if you override {@link #onDraw(android.graphics.Canvas)}
     * you should clear this flag.
     *
     * @param willNotDraw whether or not this View draw on its own
     */
    public void setWillNotDraw(boolean willNotDraw) {
        setFlags(willNotDraw ? WILL_NOT_DRAW : 0, DRAW_MASK);
    }

    /**
     * Returns whether or not this View draws on its own.
     *
     * @return true if this view has nothing to draw, false otherwise
     */
    public boolean willNotDraw() {
        return (mViewFlags & DRAW_MASK) == WILL_NOT_DRAW;
    }

    protected void onDetachedFromWindow() {
    }
    
    /**
     * This is a framework-internal mirror of onDetachedFromWindow() that's called
     * after onDetachedFromWindow().
     *
     * If you override this you *MUST* call super.onDetachedFromWindowInternal()!
     * The super method should be called at the end of the overriden method to ensure
     * subclasses are destroyed first
     *
     * @hide
     */
    protected void onDetachedFromWindowInternal() {
        mPrivateFlags &= ~PFLAG_CANCEL_NEXT_UP_EVENT;
        mPrivateFlags3 &= ~PFLAG3_IS_LAID_OUT;

        removeUnsetPressCallback();
        removeLongPressCallback();
        removePerformClickCallback();
        stopNestedScroll();

        // Anything that started animating right before detach should already
        // be in its final state when re-attached.
        jumpDrawablesToCurrentState();

        cleanupDraw();
        mCurrentAnimation = null;
    }
    
    private void cleanupDraw() {
        resetDisplayList();
        if (mAttachInfo != null) {
//            mAttachInfo.mViewRootImpl.cancelInvalidate(this);
        }
    }
    
    /**
     * Retrieve the {@link WindowId} for the window this view is
     * currently attached to.
     */
    public WindowId getWindowId() {
        if (mAttachInfo == null) {
            return null;
        }
        return mAttachInfo.mWindowId;
    }
    
    /**
     * @hide
     */
    public void dispatchStartTemporaryDetach() {
        onStartTemporaryDetach();
    }

    /**
     * This is called when a container is going to temporarily detach a child, with
     * {@link ViewGroup#detachViewFromParent(View) ViewGroup.detachViewFromParent}.
     * It will either be followed by {@link #onFinishTemporaryDetach()} or
     * {@link #onDetachedFromWindow()} when the container is done.
     */
    public void onStartTemporaryDetach() {
        removeUnsetPressCallback();
        mPrivateFlags |= PFLAG_CANCEL_NEXT_UP_EVENT;
    }

    /**
     * @hide
     */
    public void dispatchFinishTemporaryDetach() {
        onFinishTemporaryDetach();
    }

    /**
     * Called after {@link #onStartTemporaryDetach} when the container is done
     * changing the view.
     */
    public void onFinishTemporaryDetach() {
    }

    // This is for debugging only.
    // Dump the view hierarchy into log.
    void dumpTree(String prefix) {
        Log.d(VIEW_LOG_TAG, prefix + getClass().getSimpleName());
        if(this instanceof ViewGroup){
			ViewGroup parent = (ViewGroup)this;
			int childCount = parent.mChildrenCount;
	        for (int i = 0; i < childCount; ++i) {
	            parent.mChildren[i].dumpTree(prefix + "....");
	        }
        }
    }
    
    /**
     * If a user manually specified the next view id for a particular direction,
     * use the root to look up the view.
     * @param root The root view of the hierarchy containing this view.
     * @param direction One of FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, FOCUS_RIGHT, FOCUS_FORWARD,
     * or FOCUS_BACKWARD.
     * @return The user specified next view, or null if there is none.
     */
    View findUserSetNextFocus(View root, int direction) {
        switch (direction) {
            case FOCUS_LEFT:
                if (mNextFocusLeftId == View.NO_ID) return null;
                return findViewInsideOutShouldExist(root, mNextFocusLeftId);
            case FOCUS_RIGHT:
                if (mNextFocusRightId == View.NO_ID) return null;
                return findViewInsideOutShouldExist(root, mNextFocusRightId);
            case FOCUS_UP:
                if (mNextFocusUpId == View.NO_ID) return null;
                return findViewInsideOutShouldExist(root, mNextFocusUpId);
            case FOCUS_DOWN:
                if (mNextFocusDownId == View.NO_ID) return null;
                return findViewInsideOutShouldExist(root, mNextFocusDownId);
            case FOCUS_FORWARD:
                if (mNextFocusForwardId == View.NO_ID) return null;
                return findViewInsideOutShouldExist(root, mNextFocusForwardId);
            case FOCUS_BACKWARD: {
                if (mID == View.NO_ID) return null;
                final int id = mID;
                return root.findViewByPredicateInsideOut(this, new Predicate<View>() {
                    @Override
                    public boolean apply(View t) {
                        return t.mNextFocusForwardId == id;
                    }
                });
            }
        }
        return null;
    }

    private View findViewInsideOutShouldExist(View root, int id) {
        if (mMatchIdPredicate == null) {
            mMatchIdPredicate = new MatchIdPredicate();
        }
        mMatchIdPredicate.mId = id;
        View result = root.findViewByPredicateInsideOut(this, mMatchIdPredicate);
        if (result == null) {
            Log.w(VIEW_LOG_TAG, "couldn't find view with id " + id);
        }
        return result;
    }
    
    /**
     * Find and return all focusable views that are descendants of this view,
     * possibly including this view if it is focusable itself.
     *
     * @param direction The direction of the focus
     * @return A list of focusable views
     */
    public ArrayList<View> getFocusables(int direction) {
        ArrayList<View> result = new ArrayList<View>(24);
        addFocusables(result, direction);
        return result;
    }
    
    /**
     * Add any focusable views that are descendants of this view (possibly
     * including this view if it is focusable itself) to views.  If we are in touch mode,
     * only add views that are also focusable in touch mode.
     *
     * @param views Focusable views found so far
     * @param direction The direction of the focus
     */
    public void addFocusables(ArrayList<View> views, int direction) {
        addFocusables(views, direction, FOCUSABLES_TOUCH_MODE);
    }

    /**
     * Adds any focusable views that are descendants of this view (possibly
     * including this view if it is focusable itself) to views. This method
     * adds all focusable views regardless if we are in touch mode or
     * only views focusable in touch mode if we are in touch mode or
     * only views that can take accessibility focus if accessibility is enabeld
     * depending on the focusable mode paramater.
     *
     * @param views Focusable views found so far or null if all we are interested is
     *        the number of focusables.
     * @param direction The direction of the focus.
     * @param focusableMode The type of focusables to be added.
     *
     * @see #FOCUSABLES_ALL
     * @see #FOCUSABLES_TOUCH_MODE
     */
    public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
        if (views == null) {
            return;
        }
        if (!isFocusable()) {
            return;
        }
        
        if ((focusableMode & FOCUSABLES_TOUCH_MODE) == FOCUSABLES_TOUCH_MODE
                && isInTouchMode() && !isFocusableInTouchMode()) {
            return;
        }
        views.add(this);
    }
    
    /**
     * Invoked whenever this view loses focus, either by losing window focus or by losing
     * focus within its window. This method can be used to clear any state tied to the
     * focus. For instance, if a button is held pressed with the trackball and the window
     * loses focus, this method can be used to cancel the press.
     *
     * Subclasses of View overriding this method should always call super.onFocusLost().
     *
     * @see #onFocusChanged(boolean, int, android.graphics.Rect)
     * @see #onWindowFocusChanged(boolean)
     *
     * @hide pending API council approval
     */
    protected void onFocusLost() {
        resetPressedState();
    }
    
    private void resetPressedState() {
        if ((mViewFlags & ENABLED_MASK) == DISABLED) {
            return;
        }

        if (isPressed()) {
            setPressed(false);
        }
    }
    
    /**
     * Returns true if this view has focus
     *
     * @return True if this view has focus, false otherwise.
     */
    public boolean isFocused() {
        return (mPrivateFlags & PFLAG_FOCUSED) != 0;
    }
    
    /**
     * Returns whether this View is able to take focus.
     *
     * @return True if this view can take focus, or false otherwise.
     */
    public final boolean isFocusable() {
        return FOCUSABLE == (mViewFlags & FOCUSABLE_MASK);
    }
    
    /**
     * When a view is focusable, it may not want to take focus when in touch mode.
     * For example, a button would like focus when the user is navigating via a D-pad
     * so that the user can click on it, but once the user starts touching the screen,
     * the button shouldn't take focus
     * @return Whether the view is focusable in touch mode.
     * @attr ref android.R.styleable#View_focusableInTouchMode
     */
    public final boolean isFocusableInTouchMode() {
        return FOCUSABLE_IN_TOUCH_MODE == (mViewFlags & FOCUSABLE_IN_TOUCH_MODE);
    }
    
    /**
     * Return the scrolled left position of this view. This is the left edge of
     * the displayed part of your view. You do not need to draw any pixels
     * farther left, since those are outside of the frame of your view on
     * screen.
     *
     * @return The left edge of the displayed part of your view, in pixels.
     */
    public final int getScrollX() {
        return mScrollX;
    }

    /**
     * Return the scrolled top position of this view. This is the top edge of
     * the displayed part of your view. You do not need to draw any pixels above
     * it, since those are outside of the frame of your view on screen.
     *
     * @return The top edge of the displayed part of your view, in pixels.
     */
    public final int getScrollY() {
        return mScrollY;
    }
    
    /**
     * Find and return all touchable views that are descendants of this view,
     * possibly including this view if it is touchable itself.
     *
     * @return A list of touchable views
     */
    public ArrayList<View> getTouchables() {
        ArrayList<View> result = new ArrayList<View>();
        addTouchables(result);
        return result;
    }

    /**
     * Add any touchable views that are descendants of this view (possibly
     * including this view if it is touchable itself) to views.
     *
     * @param views Touchable views found so far
     */
    public void addTouchables(ArrayList<View> views) {
        final int viewFlags = mViewFlags;

        if (((viewFlags & CLICKABLE) == CLICKABLE || (viewFlags & LONG_CLICKABLE) == LONG_CLICKABLE)
                && (viewFlags & ENABLED_MASK) == ENABLED) {
            views.add(this);
        }
    }
    
    /**
     * Returns true if this view has focus iteself, or is the ancestor of the
     * view that has focus.
     *
     * @return True if this view has or contains focus, false otherwise.
     */
    public boolean hasFocus() {
        return (mPrivateFlags & PFLAG_FOCUSED) != 0;
    }

    /**
     * Returns true if this view is focusable or if it contains a reachable View
     * for which {@link #hasFocusable()} returns true. A "reachable hasFocusable()"
     * is a View whose parents do not block descendants focus.
     *
     * Only {@link #VISIBLE} views are considered focusable.
     *
     * @return True if the view is focusable or if the view contains a focusable
     *         View, false otherwise.
     *
     */
    public boolean hasFocusable() {
        return (mViewFlags & VISIBILITY_MASK) == VISIBLE && isFocusable();
    }
    
    /**
     * Gets the parent of this view. Note that the parent is a
     * ViewParent and not necessarily a View.
     *
     * @return Parent of this view.
     */
    public final ViewGroup getParent() {
        return mParent;
    }
    
    /**
     * <p>Return the offset of the widget's text baseline from the widget's top
     * boundary. If this widget does not support baseline alignment, this
     * method returns -1. </p>
     *
     * @return the offset of the baseline within the widget's bounds or -1
     *         if baseline alignment is not supported
     */
    public int getBaseline() {
        return -1;
    }
    
    /**
     * Set the background to a given Drawable, or remove the background. If the
     * background has padding, this View's padding is set to the background's
     * padding. However, when a background is removed, this View's padding isn't
     * touched. If setting the padding is desired, please use
     * {@link #setPadding(int, int, int, int)}.
     *
     * @param background The Drawable to use as the background, or null to remove the
     *        background
     */
    public void setBackground(Drawable background) {
        //noinspection deprecation
        setBackgroundDrawable(background);
    }

    /**
     * @deprecated use {@link #setBackground(Drawable)} instead
     */
    @Deprecated
    public void setBackgroundDrawable(Drawable background) {
        computeOpaqueFlags();

        if (background == mBackground) {
            return;
        }

        boolean requestLayout = false;

        mBackgroundResource = 0;

        /*
         * Regardless of whether we're setting a new background or not, we want
         * to clear the previous drawable.
         */
        if (mBackground != null) {
            mBackground.setCallback(null);
            unscheduleDrawable(mBackground);
        }

        if (background != null) {
            Rect padding = sThreadLocal.get();
            if (padding == null) {
                padding = new Rect();
                sThreadLocal.set(padding);
            }
            resetResolvedDrawables();
            background.setLayoutDirection(getLayoutDirection());
            if (background.getPadding(padding)) {
                resetResolvedPadding();
                switch (background.getLayoutDirection()) {
                    case LAYOUT_DIRECTION_RTL:
                        mUserPaddingLeftInitial = padding.right;
                        mUserPaddingRightInitial = padding.left;
                        internalSetPadding(padding.right, padding.top, padding.left, padding.bottom);
                        break;
                    case LAYOUT_DIRECTION_LTR:
                    default:
                        mUserPaddingLeftInitial = padding.left;
                        mUserPaddingRightInitial = padding.right;
                        internalSetPadding(padding.left, padding.top, padding.right, padding.bottom);
                }
                mLeftPaddingDefined = false;
                mRightPaddingDefined = false;
            }

            // Compare the minimum sizes of the old Drawable and the new.  If there isn't an old or
            // if it has a different minimum size, we should layout again
            if (mBackground == null
                    || mBackground.getMinimumHeight() != background.getMinimumHeight()
                    || mBackground.getMinimumWidth() != background.getMinimumWidth()) {
                requestLayout = true;
            }

            background.setCallback(this);
            if (background.isStateful()) {
                background.setState(getDrawableState());
            }
            background.setVisible(getVisibility() == VISIBLE, false);
            mBackground = background;

//            applyBackgroundTint();
//
//            if ((mPrivateFlags & PFLAG_SKIP_DRAW) != 0) {
//                mPrivateFlags &= ~PFLAG_SKIP_DRAW;
//                mPrivateFlags |= PFLAG_ONLY_DRAWS_BACKGROUND;
//                requestLayout = true;
//            }
        } else {
            /* Remove the background */
            mBackground = null;

//            if ((mPrivateFlags & PFLAG_ONLY_DRAWS_BACKGROUND) != 0) {
//                /*
//                 * This view ONLY drew the background before and we're removing
//                 * the background, so now it won't draw anything
//                 * (hence we SKIP_DRAW)
//                 */
//                mPrivateFlags &= ~PFLAG_ONLY_DRAWS_BACKGROUND;
//                mPrivateFlags |= PFLAG_SKIP_DRAW;
//            }

            /*
             * When the background is set, we try to apply its padding to this
             * View. When the background is removed, we don't touch this View's
             * padding. This is noted in the Javadocs. Hence, we don't need to
             * requestLayout(), the invalidate() below is sufficient.
             */

            // The old background's minimum size could have affected this
            // View's layout, so let's requestLayout
            requestLayout = true;
        }

        computeOpaqueFlags();

        if (requestLayout) {
            requestLayout();
        }

        mBackgroundSizeChanged = true;
        invalidate(true);
    }
    
    /**
     * Gets the background drawable
     *
     * @return The drawable used as the background for this view, if any.
     *
     * @see #setBackground(Drawable)
     *
     * @attr ref android.R.styleable#View_background
     */
    public Drawable getBackground() {
        return mBackground;
    }
    
    public void setBackgroundColor(int color) {
        if (mBackground instanceof ColorDrawable) {
            ((ColorDrawable) mBackground.mutate()).setColor(color);
            computeOpaqueFlags();
            mBackgroundResource = 0;
        } else {
            setBackground(new ColorDrawable(color));
        }
    }
    
    /**
     * Set the background to a given resource. The resource should refer to
     * a Drawable object or 0 to remove the background.
     * @param resid The identifier of the resource.
     *
     */
    public void setBackgroundResource(int resid) {
        if (resid == mBackgroundResource) {
            return;
        }

        Drawable d= null;
        if (resid != 0) {
            d = GLContext.get().getResources().getDrawable(resid);
        }
        setBackground(d);

        mBackgroundResource = resid;	
    }
    
    /**
     * Find the nearest view in the specified direction that can take focus.
     * This does not actually give focus to that view.
     *
     * @param direction One of FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, and FOCUS_RIGHT
     *
     * @return The nearest focusable in the specified direction, or null if none
     *         can be found.
     */
    public View focusSearch(int direction) {
        if (mParent != null) {
            return mParent.focusSearch(this, direction);
        } else {
            return null;
        }
    }
    
    /**
     * This method is the last chance for the focused view and its ancestors to
     * respond to an arrow key. This is called when the focused view did not
     * consume the key internally, nor could the view system find a new view in
     * the requested direction to give focus to.
     *
     * @param focused The currently focused view.
     * @param direction The direction focus wants to move. One of FOCUS_UP,
     *        FOCUS_DOWN, FOCUS_LEFT, and FOCUS_RIGHT.
     * @return True if the this view consumed this unhandled move.
     */
    public boolean dispatchUnhandledMove(View focused, int direction) {
        return false;
    }
    
    /**
     * Find the view in the hierarchy rooted at this view that currently has
     * focus.
     *
     * @return The view that currently has focus, or null if no focused view can
     *         be found.
     */
    public View findFocus() {
        return (mPrivateFlags & PFLAG_FOCUSED) != 0 ? this : null;
    }
    
    /**
     * Called internally by the view system when a new view is getting focus.
     * This is what clears the old focus.
     */
   void unFocus() {
        if ((mPrivateFlags & PFLAG_FOCUSED) != 0) {
            mPrivateFlags &= ~PFLAG_FOCUSED;

            onFocusChanged(false, 0, null);
            refreshDrawableState();
        }
    }
    
    /**
     * Called by the view system when the focus state of this view changes.
     * When the focus change event is caused by directional navigation, direction
     * and previouslyFocusedRect provide insight into where the focus is coming from.
     * When overriding, be sure to call up through to the super class so that
     * the standard focus handling will occur.
     *
     * @param gainFocus True if the View has focus; false otherwise.
     * @param direction The direction focus has moved when requestFocus()
     *                  is called to give this view focus. Values are
     *                  {@link #FOCUS_UP}, {@link #FOCUS_DOWN}, {@link #FOCUS_LEFT},
     *                  {@link #FOCUS_RIGHT}, {@link #FOCUS_FORWARD}, or {@link #FOCUS_BACKWARD}.
     *                  It may not always apply, in which case use the default.
     * @param previouslyFocusedRect The rectangle, in this view's coordinate
     *        system, of the previously focused view.  If applicable, this will be
     *        passed in as finer grained information about where the focus is coming
     *        from (in addition to direction).  Will be <code>null</code> otherwise.
     */
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        ListenerInfo li = mListenerInfo;
        if (li != null && li.mOnFocusChangeListener != null) {
            li.mOnFocusChangeListener.onFocusChange(this, gainFocus);
        }
        
    }
    
    /**
     * Set whether this view can receive the focus.
     *
     * Setting this to false will also ensure that this view is not focusable
     * in touch mode.
     *
     * @param focusable If true, this view can receive the focus.
     *
     */
    public void setFocusable(boolean focusable) {
        if (!focusable) {
            setFlags(0, FOCUSABLE_IN_TOUCH_MODE);
        }
        setFlags(focusable ? FOCUSABLE : NOT_FOCUSABLE, FOCUSABLE_MASK);
    }
    
    /**
     * Set whether this view can receive focus while in touch mode.
     *
     * Setting this to true will also ensure that this view is focusable.
     *
     * @param focusableInTouchMode If true, this view can receive the focus while
     *   in touch mode.
     *
     * @see #setFocusable(boolean)
     * @attr ref android.R.styleable#View_focusableInTouchMode
     */
    public void setFocusableInTouchMode(boolean focusableInTouchMode) {
        // Focusable in touch mode should always be set before the focusable flag
        // otherwise, setting the focusable flag will trigger a focusableViewAvailable()
        // which, in touch mode, will not successfully request focus on this view
        // because the focusable in touch mode flag is not set
        setFlags(focusableInTouchMode ? FOCUSABLE_IN_TOUCH_MODE : 0, FOCUSABLE_IN_TOUCH_MODE);
        if (focusableInTouchMode) {
            setFlags(FOCUSABLE, FOCUSABLE_MASK);
        }
    }
    
    /**
     * Returns whether the screen should remain on, corresponding to the current
     * value of {@link #KEEP_SCREEN_ON}.
     *
     * @return Returns true if {@link #KEEP_SCREEN_ON} is set.
     *
     * @see #setKeepScreenOn(boolean)
     *
     * @attr ref android.R.styleable#View_keepScreenOn
     */
    public boolean getKeepScreenOn() {
        return (mViewFlags & KEEP_SCREEN_ON) != 0;
    }

    /**
     * Controls whether the screen should remain on, modifying the
     * value of {@link #KEEP_SCREEN_ON}.
     *
     * @param keepScreenOn Supply true to set {@link #KEEP_SCREEN_ON}.
     *
     * @see #getKeepScreenOn()
     *
     * @attr ref android.R.styleable#View_keepScreenOn
     */
    public void setKeepScreenOn(boolean keepScreenOn) {
        setFlags(keepScreenOn ? KEEP_SCREEN_ON : 0, KEEP_SCREEN_ON);
    }

    /**
     * Gets the id of the view to use when the next focus is {@link #FOCUS_LEFT}.
     * @return The next focus ID, or {@link #NO_ID} if the framework should decide automatically.
     *
     * @attr ref android.R.styleable#View_nextFocusLeft
     */
    public int getNextFocusLeftId() {
        return mNextFocusLeftId;
    }

    /**
     * Sets the id of the view to use when the next focus is {@link #FOCUS_LEFT}.
     * @param nextFocusLeftId The next focus ID, or {@link #NO_ID} if the framework should
     * decide automatically.
     *
     * @attr ref android.R.styleable#View_nextFocusLeft
     */
    public void setNextFocusLeftId(int nextFocusLeftId) {
        mNextFocusLeftId = nextFocusLeftId;
    }

    /**
     * Gets the id of the view to use when the next focus is {@link #FOCUS_RIGHT}.
     * @return The next focus ID, or {@link #NO_ID} if the framework should decide automatically.
     *
     * @attr ref android.R.styleable#View_nextFocusRight
     */
    public int getNextFocusRightId() {
        return mNextFocusRightId;
    }

    /**
     * Sets the id of the view to use when the next focus is {@link #FOCUS_RIGHT}.
     * @param nextFocusRightId The next focus ID, or {@link #NO_ID} if the framework should
     * decide automatically.
     *
     * @attr ref android.R.styleable#View_nextFocusRight
     */
    public void setNextFocusRightId(int nextFocusRightId) {
        mNextFocusRightId = nextFocusRightId;
    }

    /**
     * Gets the id of the view to use when the next focus is {@link #FOCUS_UP}.
     * @return The next focus ID, or {@link #NO_ID} if the framework should decide automatically.
     *
     * @attr ref android.R.styleable#View_nextFocusUp
     */
    public int getNextFocusUpId() {
        return mNextFocusUpId;
    }

    /**
     * Sets the id of the view to use when the next focus is {@link #FOCUS_UP}.
     * @param nextFocusUpId The next focus ID, or {@link #NO_ID} if the framework should
     * decide automatically.
     *
     * @attr ref android.R.styleable#View_nextFocusUp
     */
    public void setNextFocusUpId(int nextFocusUpId) {
        mNextFocusUpId = nextFocusUpId;
    }

    /**
     * Gets the id of the view to use when the next focus is {@link #FOCUS_DOWN}.
     * @return The next focus ID, or {@link #NO_ID} if the framework should decide automatically.
     *
     * @attr ref android.R.styleable#View_nextFocusDown
     */
    public int getNextFocusDownId() {
        return mNextFocusDownId;
    }

    /**
     * Sets the id of the view to use when the next focus is {@link #FOCUS_DOWN}.
     * @param nextFocusDownId The next focus ID, or {@link #NO_ID} if the framework should
     * decide automatically.
     *
     * @attr ref android.R.styleable#View_nextFocusDown
     */
    public void setNextFocusDownId(int nextFocusDownId) {
        mNextFocusDownId = nextFocusDownId;
    }

    /**
     * Gets the id of the view to use when the next focus is {@link #FOCUS_FORWARD}.
     * @return The next focus ID, or {@link #NO_ID} if the framework should decide automatically.
     *
     * @attr ref android.R.styleable#View_nextFocusForward
     */
    public int getNextFocusForwardId() {
        return mNextFocusForwardId;
    }

    /**
     * Sets the id of the view to use when the next focus is {@link #FOCUS_FORWARD}.
     * @param nextFocusForwardId The next focus ID, or {@link #NO_ID} if the framework should
     * decide automatically.
     *
     * @attr ref android.R.styleable#View_nextFocusForward
     */
    public void setNextFocusForwardId(int nextFocusForwardId) {
        mNextFocusForwardId = nextFocusForwardId;
    }

    /**
     * Returns the visibility of this view and all of its ancestors
     *
     * @return True if this view and all of its ancestors are {@link #VISIBLE}
     */
    public boolean isShown() {
        View current = this;
        //noinspection ConstantConditions
        do {
            if ((current.mViewFlags & VISIBILITY_MASK) != VISIBLE) {
                return false;
            }
            ViewGroup parent = current.mParent;
            if (parent == null) {
                return false; // We are not attached to the view root
            }
            if (!(parent instanceof View)) {
                return true;
            }
            current = (View) parent;
        } while (current != null);

        return false;
    }

    /**
     * Set whether this view should have sound effects enabled for events such as
     * clicking and touching.
     *
     * <p>You may wish to disable sound effects for a view if you already play sounds,
     * for instance, a dial key that plays dtmf tones.
     *
     * @param soundEffectsEnabled whether sound effects are enabled for this view.
     * @see #isSoundEffectsEnabled()
     * @see #playSoundEffect(int)
     * @attr ref android.R.styleable#View_soundEffectsEnabled
     */
    public void setSoundEffectsEnabled(boolean soundEffectsEnabled) {
        setFlags(soundEffectsEnabled ? SOUND_EFFECTS_ENABLED: 0, SOUND_EFFECTS_ENABLED);
    }

    /**
     * @return whether this view should have sound effects enabled for events such as
     *     clicking and touching.
     *
     * @see #setSoundEffectsEnabled(boolean)
     * @see #playSoundEffect(int)
     * @attr ref android.R.styleable#View_soundEffectsEnabled
     */
    public boolean isSoundEffectsEnabled() {
        return SOUND_EFFECTS_ENABLED == (mViewFlags & SOUND_EFFECTS_ENABLED);
    }
    
    /**
     * Set whether this view should have haptic feedback for events such as
     * long presses.
     *
     * <p>You may wish to disable haptic feedback if your view already controls
     * its own haptic feedback.
     *
     * @param hapticFeedbackEnabled whether haptic feedback enabled for this view.
     * @see #isHapticFeedbackEnabled()
     * @see #performHapticFeedback(int)
     * @attr ref android.R.styleable#View_hapticFeedbackEnabled
     */
    public void setHapticFeedbackEnabled(boolean hapticFeedbackEnabled) {
        setFlags(hapticFeedbackEnabled ? HAPTIC_FEEDBACK_ENABLED: 0, HAPTIC_FEEDBACK_ENABLED);
    }

    /**
     * @return whether this view should have haptic feedback enabled for events
     * long presses.
     *
     * @see #setHapticFeedbackEnabled(boolean)
     * @see #performHapticFeedback(int)
     * @attr ref android.R.styleable#View_hapticFeedbackEnabled
     */
    public boolean isHapticFeedbackEnabled() {
        return HAPTIC_FEEDBACK_ENABLED == (mViewFlags & HAPTIC_FEEDBACK_ENABLED);
    }
    
    /**
     * Indicates whether this view reacts to click events or not.
     *
     * @return true if the view is clickable, false otherwise
     *
     * @see #setClickable(boolean)
     * @attr ref android.R.styleable#View_clickable
     */
    public boolean isClickable() {
        return (mViewFlags & CLICKABLE) == CLICKABLE;
    }

    /**
     * Enables or disables click events for this view. When a view
     * is clickable it will change its state to "pressed" on every click.
     * Subclasses should set the view clickable to visually react to
     * user's clicks.
     *
     * @param clickable true to make the view clickable, false otherwise
     *
     * @see #isClickable()
     * @attr ref android.R.styleable#View_clickable
     */
    public void setClickable(boolean clickable) {
        setFlags(clickable ? CLICKABLE : 0, CLICKABLE);
    }

    /**
     * Indicates whether this view reacts to long click events or not.
     *
     * @return true if the view is long clickable, false otherwise
     *
     * @see #setLongClickable(boolean)
     * @attr ref android.R.styleable#View_longClickable
     */
    public boolean isLongClickable() {
        return (mViewFlags & LONG_CLICKABLE) == LONG_CLICKABLE;
    }

    /**
     * Enables or disables long click events for this view. When a view is long
     * clickable it reacts to the user holding down the button for a longer
     * duration than a tap. This event can either launch the listener or a
     * context menu.
     *
     * @param longClickable true to make the view long clickable, false otherwise
     * @see #isLongClickable()
     * @attr ref android.R.styleable#View_longClickable
     */
    public void setLongClickable(boolean longClickable) {
        setFlags(longClickable ? LONG_CLICKABLE : 0, LONG_CLICKABLE);
    }
    
    /**
     * Sets the pressed state for this view and provides a touch coordinate for
     * animation hinting.
     *
     * @param pressed Pass true to set the View's internal state to "pressed",
     *            or false to reverts the View's internal state from a
     *            previously set "pressed" state.
     * @param x The x coordinate of the touch that caused the press
     * @param y The y coordinate of the touch that caused the press
     */
    private void setPressed(boolean pressed, float x, float y) {
        if (pressed) {
//            drawableHotspotChanged(x, y);
        }

        setPressed(pressed);
    }

    /**
     * Sets the pressed state for this view.
     *
     * @see #isClickable()
     * @see #setClickable(boolean)
     *
     * @param pressed Pass true to set the View's internal state to "pressed", or false to reverts
     *        the View's internal state from a previously set "pressed" state.
     */
    public void setPressed(boolean pressed) {
        final boolean needsRefresh = pressed != ((mPrivateFlags & PFLAG_PRESSED) == PFLAG_PRESSED);

        if (pressed) {
            mPrivateFlags |= PFLAG_PRESSED;
        } else {
            mPrivateFlags &= ~PFLAG_PRESSED;
        }

        if (needsRefresh) {
            refreshDrawableState();
        }
        dispatchSetPressed(pressed);
    }

    /**
     * Dispatch setPressed to all of this View's children.
     *
     * @see #setPressed(boolean)
     *
     * @param pressed The new pressed state
     */
    protected void dispatchSetPressed(boolean pressed) {
    }

    /**
     * Indicates whether the view is currently in pressed state. Unless
     * {@link #setPressed(boolean)} is explicitly called, only clickable views can enter
     * the pressed state.
     *
     * @see #setPressed(boolean)
     * @see #isClickable()
     * @see #setClickable(boolean)
     *
     * @return true if the view is currently pressed, false otherwise
     */
    public boolean isPressed() {
        return (mPrivateFlags & PFLAG_PRESSED) == PFLAG_PRESSED;
    }
    
    /**
     * Sets the TouchDelegate for this View.
     */
    public void setTouchDelegate(TouchDelegate delegate) {
        mTouchDelegate = delegate;
    }

    /**
     * Gets the TouchDelegate for this View.
     */
    public TouchDelegate getTouchDelegate() {
        return mTouchDelegate;
    }
    
    /**
     * Set flags controlling behavior of this view.
     *
     * @param flags Constant indicating the value which should be set
     * @param mask Constant indicating the bit range that should be changed
     */
    void setFlags(int flags, int mask) {
        int old = mViewFlags;
        mViewFlags = (mViewFlags & ~mask) | (flags & mask);
        int changed = mViewFlags ^ old;
        if (changed == 0) {
            return;
        }
        
        /* Check if the FOCUSABLE bit has changed */
        int privateFlags = mPrivateFlags;

        /* Check if the FOCUSABLE bit has changed */
        if (((changed & FOCUSABLE_MASK) != 0) &&
                ((privateFlags & PFLAG_HAS_BOUNDS) !=0)) {
            if (((old & FOCUSABLE_MASK) == FOCUSABLE)
                    && ((privateFlags & PFLAG_FOCUSED) != 0)) {
                /* Give up focus if we are no longer focusable */
                clearFocus();
            } else if (((old & FOCUSABLE_MASK) == NOT_FOCUSABLE)
                    && ((privateFlags & PFLAG_FOCUSED) == 0)) {
                /*
                 * Tell the view system that we are now available to take focus
                 * if no one else already has it.
                 */
                if (mParent != null) mParent.focusableViewAvailable(this);
            }
        }
        
        final int newVisibility = flags & VISIBILITY_MASK;
        if (newVisibility == VISIBLE) {
            if ((changed & VISIBILITY_MASK) != 0) {
                /*
                 * If this view is becoming visible, invalidate it in case it changed while
                 * it was not visible. Marking it drawn ensures that the invalidation will
                 * go through.
                 */
                invalidate(true);

                needGlobalAttributesUpdate(true);

                // a view becoming visible is worth notifying the parent
                // about in case nothing has focus.  even if this specific view
                // isn't focusable, it may contain something that is, so let
                // the root view try to give this focus if nothing else does.
                if ((mParent != null) && (mBottom > mTop) && (mRight > mLeft)) {
                    mParent.focusableViewAvailable(this);
                }
            }
        }
        
        /* Check if the GONE bit has changed */
        if ((changed & GONE) != 0) {
            needGlobalAttributesUpdate(false);
            requestLayout();

            if (((mViewFlags & VISIBILITY_MASK) == GONE)) {
                if (hasFocus()) clearFocus();
                if (mParent instanceof View) {
                    // GONE views noop invalidation, so invalidate the parent
                    ((View) mParent).invalidate(true);
                }
                // Mark the view drawn to ensure that it gets invalidated properly the next
                // time it is visible and gets invalidated
//                mPrivateFlags |= PFLAG_DRAWN;
            }
            if (mAttachInfo != null) {
//                mAttachInfo.mViewVisibilityChanged = true;
            }
        }
        
        if ((changed & VISIBILITY_MASK) != 0) {
            // If the view is invisible, cleanup its display list to free up resources
            if (newVisibility != VISIBLE) {
                cleanupDraw();
            }

            if (mParent != null) {
                mParent.onChildVisibilityChanged(this,
                        (changed & VISIBILITY_MASK), newVisibility);
                mParent.invalidate();
            }
            dispatchVisibilityChanged(this, newVisibility);
        }
    }
    
    /**
     * Called when this view wants to give up focus. If focus is cleared
     * {@link #onFocusChanged(boolean, int, android.graphics.Rect)} is called.
     * <p>
     * <strong>Note:</strong> When a View clears focus the framework is trying
     * to give focus to the first focusable View from the top. Hence, if this
     * View is the first from the top that can take focus, then all callbacks
     * related to clearing focus will be invoked after which the framework will
     * give focus to this view.
     * </p>
     */
    public void clearFocus() {
        if (DBG) {
            System.out.println(this + " clearFocus()");
        }

        clearFocusInternal(null, true, true);
    }

    /**
     * Clears focus from the view, optionally propagating the change up through
     * the parent hierarchy and requesting that the root view place new focus.
     *
     * @param propagate whether to propagate the change up through the parent
     *            hierarchy
     * @param refocus when propagate is true, specifies whether to request the
     *            root view place new focus
     */
    void clearFocusInternal(View focused, boolean propagate, boolean refocus) {
        if ((mPrivateFlags & PFLAG_FOCUSED) != 0) {
            mPrivateFlags &= ~PFLAG_FOCUSED;

            if (propagate && mParent != null) {
                mParent.clearChildFocus(this);
            }

            onFocusChanged(false, 0, null);
            refreshDrawableState();

            if (propagate && (!refocus || !rootViewRequestFocus())) {
                notifyGlobalFocusCleared(this);
            }
        }
    }
    
    void notifyGlobalFocusCleared(View oldFocus) {
        if (oldFocus != null && mAttachInfo != null) {
            mAttachInfo.mTreeObserver.dispatchOnGlobalFocusChange(oldFocus, null);
        }
    }

    boolean rootViewRequestFocus() {
        final View root = getRootView();
        return root != null && root.requestFocus();
    }

    /**
     * Called internally by the view system when a new view is getting focus.
     * This is what clears the old focus.
     * <p>
     * <b>NOTE:</b> The parent view's focused child must be updated manually
     * after calling this method. Otherwise, the view hierarchy may be left in
     * an inconstent state.
     */
    void unFocus(View focused) {
        if (DBG) {
            System.out.println(this + " unFocus()");
        }

        clearFocusInternal(focused, false, false);
    }
    
    /**
     * Call this to try to give focus to a specific view or to one of its
     * descendants.
     *
     * A view will not actually take focus if it is not focusable ({@link #isFocusable} returns
     * false), or if it is focusable and it is not focusable in touch mode
     * ({@link #isFocusableInTouchMode}) while the device is in touch mode.
     *
     * See also {@link #focusSearch(int)}, which is what you call to say that you
     * have focus, and you want your parent to look for the next one.
     *
     * This is equivalent to calling {@link #requestFocus(int, Rect)} with arguments
     * {@link #FOCUS_DOWN} and <code>null</code>.
     *
     * @return Whether this view or one of its descendants actually took focus.
     */
    public final boolean requestFocus() {
        return requestFocus(View.FOCUS_DOWN);
    }
    
    /**
     * Call this to try to give focus to a specific view or to one of its
     * descendants and give it a hint about what direction focus is heading.
     *
     * A view will not actually take focus if it is not focusable ({@link #isFocusable} returns
     * false), or if it is focusable and it is not focusable in touch mode
     * ({@link #isFocusableInTouchMode}) while the device is in touch mode.
     *
     * See also {@link #focusSearch(int)}, which is what you call to say that you
     * have focus, and you want your parent to look for the next one.
     *
     * This is equivalent to calling {@link #requestFocus(int, Rect)} with
     * <code>null</code> set for the previously focused rectangle.
     *
     * @param direction One of FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, and FOCUS_RIGHT
     * @return Whether this view or one of its descendants actually took focus.
     */
    public final boolean requestFocus(int direction) {
        return requestFocus(direction, null);
    }
    
    /**
     * Call this to try to give focus to a specific view or to one of its descendants
     * and give it hints about the direction and a specific rectangle that the focus
     * is coming from.  The rectangle can help give larger views a finer grained hint
     * about where focus is coming from, and therefore, where to show selection, or
     * forward focus change internally.
     *
     * A view will not actually take focus if it is not focusable ({@link #isFocusable} returns
     * false), or if it is focusable and it is not focusable in touch mode
     * ({@link #isFocusableInTouchMode}) while the device is in touch mode.
     *
     * A View will not take focus if it is not visible.
     *
     * A View will not take focus if one of its parents has
     *  equal to
     *
     * See also {@link #focusSearch(int)}, which is what you call to say that you
     * have focus, and you want your parent to look for the next one.
     *
     * You may wish to override this method if your custom {@link View} has an internal
     * {@link View} that it wishes to forward the request to.
     *
     * @param direction One of FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, and FOCUS_RIGHT
     * @param previouslyFocusedRect The rectangle (in this View's coordinate system)
     *        to give a finer grained hint about where focus is coming from.  May be null
     *        if there is no hint.
     * @return Whether this view or one of its descendants actually took focus.
     */
    public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
        return requestFocusNoSearch(direction, previouslyFocusedRect);
    }
    
    private boolean requestFocusNoSearch(int direction, Rect previouslyFocusedRect) {
        // need to be focusable
        if ((mViewFlags & FOCUSABLE_MASK) != FOCUSABLE ||
                (mViewFlags & VISIBILITY_MASK) != VISIBLE) {
            return false;
        }

        // need to be focusable in touch mode if in touch mode
        if (isInTouchMode() &&
            (FOCUSABLE_IN_TOUCH_MODE != (mViewFlags & FOCUSABLE_IN_TOUCH_MODE))) {
               return false;
        }

        // need to not have any parents blocking us
        if (hasAncestorThatBlocksDescendantFocus()) {
            return false;
        }

        handleFocusGainInternal(direction, previouslyFocusedRect);
        return true;
    }
    
    /**
     * @return Whether any ancestor of this view blocks descendant focus.
     */
    private boolean hasAncestorThatBlocksDescendantFocus() {
        final boolean focusableInTouchMode = isFocusableInTouchMode();
        ViewGroup ancestor = mParent;
        while (ancestor instanceof ViewGroup) {
        	 final ViewGroup vgAncestor = (ViewGroup) ancestor;
             if (vgAncestor.getDescendantFocusability() == ViewGroup.FOCUS_BLOCK_DESCENDANTS) {
                 return true;
             } else {
                 ancestor = vgAncestor.getParent();
             }
        }
        return false;
    }
    
    /**
     * Give this view focus. This will cause
     * {@link #onFocusChanged(boolean, int, android.graphics.Rect)} to be called.
     *
     * Note: this does not check whether this {@link View} should get focus, it just
     * gives it focus no matter what.  It should only be called internally by framework
     * code that knows what it is doing, namely {@link #requestFocus(int, Rect)}.
     *
     * @param direction values are {@link View#FOCUS_UP}, {@link View#FOCUS_DOWN},
     *        {@link View#FOCUS_LEFT} or {@link View#FOCUS_RIGHT}. This is the direction which
     *        focus moved when requestFocus() is called. It may not always
     *        apply, in which case use the default View.FOCUS_DOWN.
     * @param previouslyFocusedRect The rectangle of the view that had focus
     *        prior in this View's coordinate system.
     */
    void handleFocusGainInternal(int direction, Rect previouslyFocusedRect) {

        if ((mPrivateFlags & PFLAG_FOCUSED) == 0) {
            mPrivateFlags |= PFLAG_FOCUSED;

            View oldFocus = (mAttachInfo != null) ? getRootView().findFocus() : null;

            if (mParent != null) {
                mParent.requestChildFocus(this, this);
            }

            if (mAttachInfo != null) {
                mAttachInfo.mTreeObserver.dispatchOnGlobalFocusChange(oldFocus, this);
            }

            onFocusChanged(true, direction, previouslyFocusedRect);
            refreshDrawableState();
        }
    }
    
    /**
     * Scroll the view with standard behavior for scrolling beyond the normal
     * content boundaries. Views that call this method should override
     * {@link #onOverScrolled(int, int, boolean, boolean)} to respond to the
     * results of an over-scroll operation.
     *
     * Views can use this method to handle any touch or fling-based scrolling.
     *
     * @param deltaX Change in X in pixels
     * @param deltaY Change in Y in pixels
     * @param scrollX Current X scroll value in pixels before applying deltaX
     * @param scrollY Current Y scroll value in pixels before applying deltaY
     * @param scrollRangeX Maximum content scroll range along the X axis
     * @param scrollRangeY Maximum content scroll range along the Y axis
     * @param maxOverScrollX Number of pixels to overscroll by in either direction
     *          along the X axis.
     * @param maxOverScrollY Number of pixels to overscroll by in either direction
     *          along the Y axis.
     * @param isTouchEvent true if this scroll operation is the result of a touch event.
     * @return true if scrolling was clamped to an over-scroll boundary along either
     *          axis, false otherwise.
     */
    @SuppressWarnings({"UnusedParameters"})
    protected boolean overScrollBy(int deltaX, int deltaY,
            int scrollX, int scrollY,
            int scrollRangeX, int scrollRangeY,
            int maxOverScrollX, int maxOverScrollY,
            boolean isTouchEvent) {
        final int overScrollMode = mOverScrollMode;
        final boolean canScrollHorizontal =
                computeHorizontalScrollRange() > computeHorizontalScrollExtent();
        final boolean canScrollVertical =
                computeVerticalScrollRange() > computeVerticalScrollExtent();
        final boolean overScrollHorizontal = overScrollMode == OVER_SCROLL_ALWAYS ||
                (overScrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && canScrollHorizontal);
        final boolean overScrollVertical = overScrollMode == OVER_SCROLL_ALWAYS ||
                (overScrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS && canScrollVertical);

        int newScrollX = scrollX + deltaX;
        if (!overScrollHorizontal) {
            maxOverScrollX = 0;
        }

        int newScrollY = scrollY + deltaY;
        if (!overScrollVertical) {
            maxOverScrollY = 0;
        }

        // Clamp values if at the limits and record
        final int left = -maxOverScrollX;
        final int right = maxOverScrollX + scrollRangeX;
        final int top = -maxOverScrollY;
        final int bottom = maxOverScrollY + scrollRangeY;

        boolean clampedX = false;
        if (newScrollX > right) {
            newScrollX = right;
            clampedX = true;
        } else if (newScrollX < left) {
            newScrollX = left;
            clampedX = true;
        }

        boolean clampedY = false;
        if (newScrollY > bottom) {
            newScrollY = bottom;
            clampedY = true;
        } else if (newScrollY < top) {
            newScrollY = top;
            clampedY = true;
        }

        onOverScrolled(newScrollX, newScrollY, clampedX, clampedY);

        return clampedX || clampedY;
    }

    /**
     * Called by {@link #overScrollBy(int, int, int, int, int, int, int, int, boolean)} to
     * respond to the results of an over-scroll operation.
     *
     * @param scrollX New X scroll value in pixels
     * @param scrollY New Y scroll value in pixels
     * @param clampedX True if scrollX was clamped to an over-scroll boundary
     * @param clampedY True if scrollY was clamped to an over-scroll boundary
     */
    protected void onOverScrolled(int scrollX, int scrollY,
            boolean clampedX, boolean clampedY) {
        // Intentionally empty.
    }

    /**
     * Returns the over-scroll mode for this view. The result will be
     * one of {@link #OVER_SCROLL_ALWAYS} (default), {@link #OVER_SCROLL_IF_CONTENT_SCROLLS}
     * (allow over-scrolling only if the view content is larger than the container),
     * or {@link #OVER_SCROLL_NEVER}.
     *
     * @return This view's over-scroll mode.
     */
    public int getOverScrollMode() {
        return mOverScrollMode;
    }

    /**
     * Set the over-scroll mode for this view. Valid over-scroll modes are
     * {@link #OVER_SCROLL_ALWAYS} (default), {@link #OVER_SCROLL_IF_CONTENT_SCROLLS}
     * (allow over-scrolling only if the view content is larger than the container),
     * or {@link #OVER_SCROLL_NEVER}.
     *
     * Setting the over-scroll mode of a view will have an effect only if the
     * view is capable of scrolling.
     *
     * @param overScrollMode The new over-scroll mode for this view.
     */
    public void setOverScrollMode(int overScrollMode) {
        if (overScrollMode != OVER_SCROLL_ALWAYS &&
                overScrollMode != OVER_SCROLL_IF_CONTENT_SCROLLS &&
                overScrollMode != OVER_SCROLL_NEVER) {
            throw new IllegalArgumentException("Invalid overscroll mode " + overScrollMode);
        }
        mOverScrollMode = overScrollMode;
    }
    
    /**
     * Enable or disable nested scrolling for this view.
     *
     * <p>If this property is set to true the view will be permitted to initiate nested
     * scrolling operations with a compatible parent view in the current hierarchy. If this
     * view does not implement nested scrolling this will have no effect. Disabling nested scrolling
     * while a nested scroll is in progress has the effect of {@link #stopNestedScroll() stopping}
     * the nested scroll.</p>
     *
     * @param enabled true to enable nested scrolling, false to disable
     *
     * @see #isNestedScrollingEnabled()
     */
    public void setNestedScrollingEnabled(boolean enabled) {
        if (enabled) {
            mPrivateFlags3 |= PFLAG3_NESTED_SCROLLING_ENABLED;
        } else {
            stopNestedScroll();
            mPrivateFlags3 &= ~PFLAG3_NESTED_SCROLLING_ENABLED;
        }
    }

    /**
     * Returns true if nested scrolling is enabled for this view.
     *
     * <p>If nested scrolling is enabled and this View class implementation supports it,
     * this view will act as a nested scrolling child view when applicable, forwarding data
     * about the scroll operation in progress to a compatible and cooperating nested scrolling
     * parent.</p>
     *
     * @return true if nested scrolling is enabled
     *
     * @see #setNestedScrollingEnabled(boolean)
     */
    public boolean isNestedScrollingEnabled() {
        return (mPrivateFlags3 & PFLAG3_NESTED_SCROLLING_ENABLED) ==
                PFLAG3_NESTED_SCROLLING_ENABLED;
    }

    /**
     * Begin a nestable scroll operation along the given axes.
     *
     * <p>A view starting a nested scroll promises to abide by the following contract:</p>
     *
     * <p>The view will call startNestedScroll upon initiating a scroll operation. In the case
     * of a touch scroll this corresponds to the initial {@link MotionEvent#ACTION_DOWN}.
     * In the case of touch scrolling the nested scroll will be terminated automatically in
     * the same manner as {@link ViewParent#requestDisallowInterceptTouchEvent(boolean)}.
     * In the event of programmatic scrolling the caller must explicitly call
     * {@link #stopNestedScroll()} to indicate the end of the nested scroll.</p>
     *
     * <p>If <code>startNestedScroll</code> returns true, a cooperative parent was found.
     * If it returns false the caller may ignore the rest of this contract until the next scroll.
     * Calling startNestedScroll while a nested scroll is already in progress will return true.</p>
     *
     * <p>At each incremental step of the scroll the caller should invoke
     * {@link #dispatchNestedPreScroll(int, int, int[], int[]) dispatchNestedPreScroll}
     * once it has calculated the requested scrolling delta. If it returns true the nested scrolling
     * parent at least partially consumed the scroll and the caller should adjust the amount it
     * scrolls by.</p>
     *
     * <p>After applying the remainder of the scroll delta the caller should invoke
     * {@link #dispatchNestedScroll(int, int, int, int, int[]) dispatchNestedScroll}, passing
     * both the delta consumed and the delta unconsumed. A nested scrolling parent may treat
     * these values differently. See {@link ViewParent#onNestedScroll(View, int, int, int, int)}.
     * </p>
     *
     * @param axes Flags consisting of a combination of {@link #SCROLL_AXIS_HORIZONTAL} and/or
     *             {@link #SCROLL_AXIS_VERTICAL}.
     * @return true if a cooperative parent was found and nested scrolling has been enabled for
     *         the current gesture.
     *
     * @see #stopNestedScroll()
     * @see #dispatchNestedPreScroll(int, int, int[], int[])
     * @see #dispatchNestedScroll(int, int, int, int, int[])
     */
    public boolean startNestedScroll(int axes) {
        if (hasNestedScrollingParent()) {
            // Already in progress
            return true;
        }
        if (isNestedScrollingEnabled()) {
            ViewGroup p = getParent();
            View child = this;
            while (p != null) {
                try {
                    if (p.onStartNestedScroll(child, this, axes)) {
                        mNestedScrollingParent = p;
                        p.onNestedScrollAccepted(child, this, axes);
                        return true;
                    }
                } catch (AbstractMethodError e) {
                    Log.e(VIEW_LOG_TAG, "ViewParent " + p + " does not implement interface " +
                            "method onStartNestedScroll", e);
                    // Allow the search upward to continue
                }
                if (p instanceof View) {
                    child = (View) p;
                }
                p = p.getParent();
            }
        }
        return false;
    }

    /**
     * Stop a nested scroll in progress.
     *
     * <p>Calling this method when a nested scroll is not currently in progress is harmless.</p>
     *
     * @see #startNestedScroll(int)
     */
    public void stopNestedScroll() {
        if (mNestedScrollingParent != null) {
            mNestedScrollingParent.onStopNestedScroll(this);
            mNestedScrollingParent = null;
        }
    }

    /**
     * Returns true if this view has a nested scrolling parent.
     *
     * <p>The presence of a nested scrolling parent indicates that this view has initiated
     * a nested scroll and it was accepted by an ancestor view further up the view hierarchy.</p>
     *
     * @return whether this view has a nested scrolling parent
     */
    public boolean hasNestedScrollingParent() {
        return mNestedScrollingParent != null;
    }

    /**
     * Dispatch one step of a nested scroll in progress.
     *
     * <p>Implementations of views that support nested scrolling should call this to report
     * info about a scroll in progress to the current nested scrolling parent. If a nested scroll
     * is not currently in progress or nested scrolling is not
     * {@link #isNestedScrollingEnabled() enabled} for this view this method does nothing.</p>
     *
     * <p>Compatible View implementations should also call
     * {@link #dispatchNestedPreScroll(int, int, int[], int[]) dispatchNestedPreScroll} before
     * consuming a component of the scroll event themselves.</p>
     *
     * @param dxConsumed Horizontal distance in pixels consumed by this view during this scroll step
     * @param dyConsumed Vertical distance in pixels consumed by this view during this scroll step
     * @param dxUnconsumed Horizontal scroll distance in pixels not consumed by this view
     * @param dyUnconsumed Horizontal scroll distance in pixels not consumed by this view
     * @param offsetInWindow Optional. If not null, on return this will contain the offset
     *                       in local view coordinates of this view from before this operation
     *                       to after it completes. View implementations may use this to adjust
     *                       expected input coordinate tracking.
     * @return true if the event was dispatched, false if it could not be dispatched.
     * @see #dispatchNestedPreScroll(int, int, int[], int[])
     */
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed,
            int dxUnconsumed, int dyUnconsumed, int[] offsetInWindow) {
        if (isNestedScrollingEnabled() && mNestedScrollingParent != null) {
            if (dxConsumed != 0 || dyConsumed != 0 || dxUnconsumed != 0 || dyUnconsumed != 0) {
                int startX = 0;
                int startY = 0;
                if (offsetInWindow != null) {
                    getLocationInWindow(offsetInWindow);
                    startX = offsetInWindow[0];
                    startY = offsetInWindow[1];
                }

                mNestedScrollingParent.onNestedScroll(this, dxConsumed, dyConsumed,
                        dxUnconsumed, dyUnconsumed);

                if (offsetInWindow != null) {
                    getLocationInWindow(offsetInWindow);
                    offsetInWindow[0] -= startX;
                    offsetInWindow[1] -= startY;
                }
                return true;
            } else if (offsetInWindow != null) {
                // No motion, no dispatch. Keep offsetInWindow up to date.
                offsetInWindow[0] = 0;
                offsetInWindow[1] = 0;
            }
        }
        return false;
    }

    /**
     * Dispatch one step of a nested scroll in progress before this view consumes any portion of it.
     *
     * <p>Nested pre-scroll events are to nested scroll events what touch intercept is to touch.
     * <code>dispatchNestedPreScroll</code> offers an opportunity for the parent view in a nested
     * scrolling operation to consume some or all of the scroll operation before the child view
     * consumes it.</p>
     *
     * @param dx Horizontal scroll distance in pixels
     * @param dy Vertical scroll distance in pixels
     * @param consumed Output. If not null, consumed[0] will contain the consumed component of dx
     *                 and consumed[1] the consumed dy.
     * @param offsetInWindow Optional. If not null, on return this will contain the offset
     *                       in local view coordinates of this view from before this operation
     *                       to after it completes. View implementations may use this to adjust
     *                       expected input coordinate tracking.
     * @return true if the parent consumed some or all of the scroll delta
     * @see #dispatchNestedScroll(int, int, int, int, int[])
     */
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        if (isNestedScrollingEnabled() && mNestedScrollingParent != null) {
            if (dx != 0 || dy != 0) {
                int startX = 0;
                int startY = 0;
                if (offsetInWindow != null) {
                    getLocationInWindow(offsetInWindow);
                    startX = offsetInWindow[0];
                    startY = offsetInWindow[1];
                }

                if (consumed == null) {
                    if (mTempNestedScrollConsumed == null) {
                        mTempNestedScrollConsumed = new int[2];
                    }
                    consumed = mTempNestedScrollConsumed;
                }
                consumed[0] = 0;
                consumed[1] = 0;
                mNestedScrollingParent.onNestedPreScroll(this, dx, dy, consumed);

                if (offsetInWindow != null) {
                    getLocationInWindow(offsetInWindow);
                    offsetInWindow[0] -= startX;
                    offsetInWindow[1] -= startY;
                }
                return consumed[0] != 0 || consumed[1] != 0;
            } else if (offsetInWindow != null) {
                offsetInWindow[0] = 0;
                offsetInWindow[1] = 0;
            }
        }
        return false;
    }

    /**
     * Dispatch a fling to a nested scrolling parent.
     *
     * <p>This method should be used to indicate that a nested scrolling child has detected
     * suitable conditions for a fling. Generally this means that a touch scroll has ended with a
     * {@link VelocityTracker velocity} in the direction of scrolling that meets or exceeds
     * the {@link ViewConfiguration#getScaledMinimumFlingVelocity() minimum fling velocity}
     * along a scrollable axis.</p>
     *
     * <p>If a nested scrolling child view would normally fling but it is at the edge of
     * its own content, it can use this method to delegate the fling to its nested scrolling
     * parent instead. The parent may optionally consume the fling or observe a child fling.</p>
     *
     * @param velocityX Horizontal fling velocity in pixels per second
     * @param velocityY Vertical fling velocity in pixels per second
     * @param consumed true if the child consumed the fling, false otherwise
     * @return true if the nested scrolling parent consumed or otherwise reacted to the fling
     */
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        if (isNestedScrollingEnabled() && mNestedScrollingParent != null) {
            return mNestedScrollingParent.onNestedFling(this, velocityX, velocityY, consumed);
        }
        return false;
    }

    /**
     * Dispatch a fling to a nested scrolling parent before it is processed by this view.
     *
     * <p>Nested pre-fling events are to nested fling events what touch intercept is to touch
     * and what nested pre-scroll is to nested scroll. <code>dispatchNestedPreFling</code>
     * offsets an opportunity for the parent view in a nested fling to fully consume the fling
     * before the child view consumes it. If this method returns <code>true</code>, a nested
     * parent view consumed the fling and this view should not scroll as a result.</p>
     *
     * <p>For a better user experience, only one view in a nested scrolling chain should consume
     * the fling at a time. If a parent view consumed the fling this method will return false.
     * Custom view implementations should account for this in two ways:</p>
     *
     * <ul>
     *     <li>If a custom view is paged and needs to settle to a fixed page-point, do not
     *     call <code>dispatchNestedPreFling</code>; consume the fling and settle to a valid
     *     position regardless.</li>
     *     <li>If a nested parent does consume the fling, this view should not scroll at all,
     *     even to settle back to a valid idle position.</li>
     * </ul>
     *
     * <p>Views should also not offer fling velocities to nested parent views along an axis
     * where scrolling is not currently supported; a {@link android.widget.ScrollView ScrollView}
     * should not offer a horizontal fling velocity to its parents since scrolling along that
     * axis is not permitted and carrying velocity along that motion does not make sense.</p>
     *
     * @param velocityX Horizontal fling velocity in pixels per second
     * @param velocityY Vertical fling velocity in pixels per second
     * @return true if a nested scrolling parent consumed the fling
     */
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        if (isNestedScrollingEnabled() && mNestedScrollingParent != null) {
            return mNestedScrollingParent.onNestedPreFling(this, velocityX, velocityY);
        }
        return false;
    }

    /**
     * Hit rectangle in parent's coordinates
     *
     * @param outRect The hit rectangle of the view.
     */
    public void getHitRect(Rect outRect) {
        outRect.set(mLeft, mTop, mRight, mBottom);
    }
    
    /**
     * Determines whether the given point, in local coordinates is inside the view.
     */
    /*package*/ final boolean pointInView(float localX, float localY) {
        return localX >= 0 && localX < (mRight - mLeft)
                && localY >= 0 && localY < (mBottom - mTop);
    }

    /**
     * Utility method to determine whether the given point, in local coordinates,
     * is inside the view, where the area of the view is expanded by the slop factor.
     * This method is called while processing touch-move events to determine if the event
     * is still within the view.
     *
     * @hide
     */
    public boolean pointInView(float localX, float localY, float slop) {
        return localX >= -slop && localY >= -slop && localX < ((mRight - mLeft) + slop) &&
                localY < ((mBottom - mTop) + slop);
    }
    
    /**
     * When a view has focus and the user navigates away from it, the next view is searched for
     * starting from the rectangle filled in by this method.
     *
     * By default, the rectangle is the {@link #getDrawingRect(android.graphics.Rect)})
     * of the view.  However, if your view maintains some idea of internal selection,
     * such as a cursor, or a selected row or column, you should override this method and
     * fill in a more specific rectangle.
     *
     * @param r The rectangle to fill in, in this view's coordinates.
     */
    public void getFocusedRect(Rect r) {
        getDrawingRect(r);
    }

    /**
     * If some part of this view is not clipped by any of its parents, then
     * return that area in r in global (root) coordinates. To convert r to local
     * coordinates (without taking possible View rotations into account), offset
     * it by -globalOffset (e.g. r.offset(-globalOffset.x, -globalOffset.y)).
     * If the view is completely clipped or translated out, return false.
     *
     * @param r If true is returned, r holds the global coordinates of the
     *        visible portion of this view.
     * @param globalOffset If true is returned, globalOffset holds the dx,dy
     *        between this view and its root. globalOffet may be null.
     * @return true if r is non-empty (i.e. part of the view is visible at the
     *         root level.
     */
    public boolean getGlobalVisibleRect(Rect r, Point globalOffset) {
        int width = mRight - mLeft;
        int height = mBottom - mTop;
        if (width > 0 && height > 0) {
            r.set(0, 0, width, height);
            if (globalOffset != null) {
                globalOffset.set(-mScrollX, -mScrollY);
            }
            int[] location = new int[2];
            getLocationInWindow(location);
            r.offset(location[0], location[1]);
//            return mParent == null || mParent.getChildVisibleRect(this, r, globalOffset);
            return true;
        }
        return false;
    }

    public final boolean getGlobalVisibleRect(Rect r) {
        return getGlobalVisibleRect(r, null);
    }
    
    /**
     * Offset this view's vertical location by the specified number of pixels.
     *
     * @param offset the number of pixels to offset the view by
     */
    public void offsetTopAndBottom(int offset) {
        if (offset != 0) {
            mTop += offset;
            mBottom += offset;
            mRenderNode.setTop(mTop);
            mRenderNode.setBottom(mBottom);
           invalidate();
        }
    }

    /**
     * Offset this view's horizontal location by the specified amount of pixels.
     *
     * @param offset the number of pixels to offset the view by
     */
    public void offsetLeftAndRight(int offset) {
        if (offset != 0) {
            mLeft += offset;
            mRight += offset;
            mRenderNode.setLeft(mLeft);
            mRenderNode.setRight(mRight);
            invalidate();
        }
    }
    
    public ViewGroup.LayoutParams getLayoutParams() {
        return mLayoutParams;
    }
    
    /**
     * Set the layout parameters associated with this view. These supply
     * parameters to the <i>parent</i> of this view specifying how it should be
     * arranged. There are many subclasses of GLViewGroup.GLLayoutParams, and these
     * correspond to the different subclasses of GLViewGroup that are responsible
     * for arranging their children.
     *
     * @param params The layout parameters for this view, cannot be null
     */
    public void setLayoutParams(ViewGroup.LayoutParams params){
    	if (params == null) {
    		throw new NullPointerException("Layout parameters cannot be null");
    	}
    	mLayoutParams = params;
    	resolveLayoutParams();
//        mParent.onSetLayoutParams(this, params);
		requestLayout();
    }
    
    /**
     * Resolve the layout parameters depending on the resolved layout direction
     *
     * @hide
     */
    public void resolveLayoutParams() {
        if (mLayoutParams != null) {
            mLayoutParams.resolveLayoutDirection(getLayoutDirection());
        }
    }
    
    /**
     * Finalize inflating a view from XML.  This is called as the last phase
     * of inflation, after all child views have been added.
     *
     * <p>Even if the subclass overrides onFinishInflate, they should always be
     * sure to call the super method, so that we get called.
     */
    protected void onFinishInflate() {
    }
    
    /**
     * Returns the resources associated with this view.
     *
     * @return Resources object.
     */
    public GLResources getResources() {
        return GLContext.get().getResources();
    }
    
    /**
     * Call this to force a view to update its drawable state. This will cause
     * drawableStateChanged to be called on this view. Views that are interested
     * in the new state should call getDrawableState.
     *
     * @see #drawableStateChanged
     * @see #getDrawableState
     */
    public void refreshDrawableState() {
        mPrivateFlags |= PFLAG_DRAWABLE_STATE_DIRTY;
        drawableStateChanged();

        ViewGroup parent = mParent;
        if (parent != null) {
            parent.childDrawableStateChanged(this);
        }
    }
    
    /**
     * This function is called whenever the state of the view changes in such
     * a way that it impacts the state of drawables being shown.
     *
     * <p>Be sure to call through to the superclass when overriding this
     * function.
     *
     * @see Drawable#setState(int[])
     */
    protected void drawableStateChanged() {
        final Drawable d = mBackground;
        if (d != null && d.isStateful()) {
            d.setState(getDrawableState());
        }
        
        if (mStateListAnimator != null) {
            mStateListAnimator.setState(getDrawableState());
        }
    }
    
    /**
     * Return an array of resource IDs of the drawable states representing the
     * current state of the view.
     *
     * @return The current drawable state
     *
     * @see Drawable#setState(int[])
     * @see #drawableStateChanged()
     * @see #onCreateDrawableState(int)
     */
    public final int[] getDrawableState() {
        if ((mDrawableState != null) && ((mPrivateFlags & PFLAG_DRAWABLE_STATE_DIRTY) == 0)) {
            return mDrawableState;
        } else {
            mDrawableState = onCreateDrawableState(0);
            mPrivateFlags &= ~PFLAG_DRAWABLE_STATE_DIRTY;
            return mDrawableState;
        }
    }
    
    /**
     * Generate the new {@link android.graphics.drawable.Drawable} state for
     * this view. This is called by the view
     * system when the cached Drawable state is determined to be invalid.  To
     * retrieve the current state, you should use {@link #getDrawableState}.
     *
     * @param extraSpace if non-zero, this is the number of extra entries you
     * would like in the returned array in which you can place your own
     * states.
     *
     * @return Returns an array holding the current {@link Drawable} state of
     * the view.
     *
     * @see #mergeDrawableStates(int[], int[])
     */
    protected int[] onCreateDrawableState(int extraSpace) {
        if ((mViewFlags & DUPLICATE_PARENT_STATE) == DUPLICATE_PARENT_STATE &&
                mParent instanceof View) {
            return ((View) mParent).onCreateDrawableState(extraSpace);
        }

        int[] drawableState;

        int privateFlags = mPrivateFlags;

        int viewStateIndex = 0;
        if ((privateFlags & PFLAG_PRESSED) != 0) viewStateIndex |= VIEW_STATE_PRESSED;
        if ((mViewFlags & ENABLED_MASK) == ENABLED) viewStateIndex |= VIEW_STATE_ENABLED;
        if (isFocused()) viewStateIndex |= VIEW_STATE_FOCUSED;
        if ((privateFlags & PFLAG_SELECTED) != 0) viewStateIndex |= VIEW_STATE_SELECTED;
        if (hasWindowFocus()) viewStateIndex |= VIEW_STATE_WINDOW_FOCUSED;
        if ((privateFlags & PFLAG_ACTIVATED) != 0) viewStateIndex |= VIEW_STATE_ACTIVATED;
//        if (mAttachInfo != null && mAttachInfo.mHardwareAccelerationRequested &&
//                HardwareRenderer.isAvailable()) {
//            // This is set if HW acceleration is requested, even if the current
//            // process doesn't allow it.  This is just to allow app preview
//            // windows to better match their app.
//            viewStateIndex |= VIEW_STATE_ACCELERATED;
//        }
        if ((privateFlags & PFLAG_HOVERED) != 0) viewStateIndex |= VIEW_STATE_HOVERED;

        final int privateFlags2 = mPrivateFlags2;
        if ((privateFlags2 & PFLAG2_DRAG_CAN_ACCEPT) != 0) viewStateIndex |= VIEW_STATE_DRAG_CAN_ACCEPT;
        if ((privateFlags2 & PFLAG2_DRAG_HOVERED) != 0) viewStateIndex |= VIEW_STATE_DRAG_HOVERED;

        drawableState = VIEW_STATE_SETS[viewStateIndex];

        //noinspection ConstantIfStatement
//        if (false) {
//            Log.i("View", "drawableStateIndex=" + viewStateIndex);
//            Log.i("View", toString()
//                    + " pressed=" + ((privateFlags & PFLAG_PRESSED) != 0)
//                    + " en=" + ((mViewFlags & ENABLED_MASK) == ENABLED)
//                    + " fo=" + hasFocus()
//                    + " sl=" + ((privateFlags & PFLAG_SELECTED) != 0)
//                    + " wf=" + hasWindowFocus()
//                    + ": " + Arrays.toString(drawableState));
//        }

        if (extraSpace == 0) {
            return drawableState;
        }

        final int[] fullState;
        if (drawableState != null) {
            fullState = new int[drawableState.length + extraSpace];
            System.arraycopy(drawableState, 0, fullState, 0, drawableState.length);
        } else {
            fullState = new int[extraSpace];
        }

        return fullState;
    }
    
    /**
     * If your view subclass is displaying its own Drawable objects, it should
     * override this function and return true for any Drawable it is
     * displaying.  This allows animations for those drawables to be
     * scheduled.
     *
     * <p>Be sure to call through to the super class when overriding this
     * function.
     *
     * @param who The Drawable to verify.  Return true if it is one you are
     *            displaying, else return the result of calling through to the
     *            super class.
     *
     * @return boolean If true than the Drawable is being displayed in the
     *         view; else false and it is not allowed to animate.
     *
     * @see #unscheduleDrawable(android.graphics.drawable.Drawable)
     * @see #drawableStateChanged()
     */
    protected boolean verifyDrawable(Drawable who) {
        return who == mBackground;
    }
    
    /**
     * Merge your own state values in <var>additionalState</var> into the base
     * state values <var>baseState</var> that were returned by
     * {@link #onCreateDrawableState(int)}.
     *
     * @param baseState The base state values returned by
     * {@link #onCreateDrawableState(int)}, which will be modified to also hold your
     * own additional state values.
     *
     * @param additionalState The additional state values you would like
     * added to <var>baseState</var>; this array is not modified.
     *
     * @return As a convenience, the <var>baseState</var> array you originally
     * passed into the function is returned.
     *
     * @see #onCreateDrawableState(int)
     */
    protected static int[] mergeDrawableStates(int[] baseState, int[] additionalState) {
        final int N = baseState.length;
        int i = N - 1;
        while (i >= 0 && baseState[i] == 0) {
            i--;
        }
        System.arraycopy(additionalState, 0, baseState, i + 1, additionalState.length);
        return baseState;
    }
    
    /**
     * Returns the layout direction for this view.
     *
     * @return One of {@link #LAYOUT_DIRECTION_LTR},
     *   {@link #LAYOUT_DIRECTION_RTL},
     *   {@link #LAYOUT_DIRECTION_INHERIT} or
     *   {@link #LAYOUT_DIRECTION_LOCALE}.
     *
     * @attr ref android.R.styleable#View_layoutDirection
     *
     * @hide
     */
    public int getRawLayoutDirection() {
        return (mPrivateFlags2 & PFLAG2_LAYOUT_DIRECTION_MASK) >> PFLAG2_LAYOUT_DIRECTION_MASK_SHIFT;
    }

    /**
     * Set the layout direction for this view. This will propagate a reset of layout direction
     * resolution to the view's children and resolve layout direction for this view.
     *
     * @param layoutDirection the layout direction to set. Should be one of:
     *
     * {@link #LAYOUT_DIRECTION_LTR},
     * {@link #LAYOUT_DIRECTION_RTL},
     * {@link #LAYOUT_DIRECTION_INHERIT},
     * {@link #LAYOUT_DIRECTION_LOCALE}.
     *
     * Resolution will be done if the value is set to LAYOUT_DIRECTION_INHERIT. The resolution
     * proceeds up the parent chain of the view to get the value. If there is no parent, then it
     * will return the default {@link #LAYOUT_DIRECTION_LTR}.
     *
     * @attr ref android.R.styleable#View_layoutDirection
     */
    public void setLayoutDirection(int layoutDirection) {
        if (getRawLayoutDirection() != layoutDirection) {
            // Reset the current layout direction and the resolved one
            mPrivateFlags2 &= ~PFLAG2_LAYOUT_DIRECTION_MASK;
            resetRtlProperties();
            // Set the new layout direction (filtered)
            mPrivateFlags2 |=
                    ((layoutDirection << PFLAG2_LAYOUT_DIRECTION_MASK_SHIFT) & PFLAG2_LAYOUT_DIRECTION_MASK);
            // We need to resolve all RTL properties as they all depend on layout direction
            resolveRtlPropertiesIfNeeded();
            requestLayout();
            invalidate(true);
        }
    }
    
    /**
     * Reset resolution of all RTL related properties.
     *
     * @hide
     */
    public void resetRtlProperties() {
        resetResolvedLayoutDirection();
        resetResolvedPadding();
        resetResolvedDrawables();
    }
    
    /**
     * Called when layout direction has been resolved.
     *
     * The default implementation does nothing.
     *
     * @param layoutDirection The resolved layout direction.
     *
     * @see #LAYOUT_DIRECTION_LTR
     * @see #LAYOUT_DIRECTION_RTL
     *
     * @hide
     */
    public void onResolveDrawables(int layoutDirection) {
    }

    /**
     * @hide
     */
    protected void resetResolvedDrawables() {
        mPrivateFlags2 &= ~PFLAG2_DRAWABLE_RESOLVED;
    }

    private boolean isDrawablesResolved() {
        return (mPrivateFlags2 & PFLAG2_DRAWABLE_RESOLVED) == PFLAG2_DRAWABLE_RESOLVED;
    }
    
    /**
     * Reset the resolved layout direction.
     *
     * @hide
     */
    public void resetResolvedPadding() {
        mPrivateFlags2 &= ~PFLAG2_PADDING_RESOLVED;
    }
    
    /**
     * Resolve all RTL related properties.
     *
     * @return true if resolution of RTL properties has been done
     *
     * @hide
     */
    public boolean resolveRtlPropertiesIfNeeded() {
        if (!needRtlPropertiesResolution()) return false;

        // Order is important here: LayoutDirection MUST be resolved first
        if (!isLayoutDirectionResolved()) {
            resolveLayoutDirection();
            resolveLayoutParams();
        }
     	// Should resolve Drawables before Padding because we need the layout direction of the
        // Drawable to correctly resolve Padding.
        if (!isDrawablesResolved()) {
            resolveDrawables();
        }
        if (!isPaddingResolved()) {
            resolvePadding();
        }
        onRtlPropertiesChanged(getLayoutDirection());
        return true;
    }
    
    /**
     * Resolve the Drawables depending on the layout direction. This is implicitly supposing
     * that the View directionality can and will be resolved before its Drawables.
     *
     * Will call {@link View#onResolveDrawables} when resolution is done.
     *
     * @hide
     */
    protected void resolveDrawables() {
        // Drawables resolution may need to happen before resolving the layout direction (which is
        // done only during the measure() call).
        // If the layout direction is not resolved yet, we cannot resolve the Drawables except in
        // one case: when the raw layout direction has not been defined as LAYOUT_DIRECTION_INHERIT.
        // So, if the raw layout direction is LAYOUT_DIRECTION_LTR or LAYOUT_DIRECTION_RTL or
        // LAYOUT_DIRECTION_LOCALE, we can "cheat" and we don't need to wait for the layout
        // direction to be resolved as its resolved value will be the same as its raw value.
        if (!isLayoutDirectionResolved() &&
                getRawLayoutDirection() == View.LAYOUT_DIRECTION_INHERIT) {
            return;
        }

        final int layoutDirection = isLayoutDirectionResolved() ?
                getLayoutDirection() : getRawLayoutDirection();

        if (mBackground != null) {
            mBackground.setLayoutDirection(layoutDirection);
        }
        mPrivateFlags2 |= PFLAG2_DRAWABLE_RESOLVED;
        onResolveDrawables(layoutDirection);
    }
    
    /**
     * Return true if we are in RTL compatibility mode (either before Jelly Bean MR1 or
     * RTL not supported)
     */
    private boolean isRtlCompatibilityMode() {
        /*final int targetSdkVersion = getContext().getApplicationInfo().targetSdkVersion;
        return targetSdkVersion < JELLY_BEAN_MR1 || !hasRtlSupport();*/
    	return true;
    }
    
    /**
     * @return true if RTL properties need resolution.
     *
     */
    private boolean needRtlPropertiesResolution() {
        return (mPrivateFlags2 & ALL_RTL_PROPERTIES_RESOLVED) != ALL_RTL_PROPERTIES_RESOLVED;
    }

    /**
     * Called when any RTL property (layout direction or text direction or text alignment) has
     * been changed.
     *
     * Subclasses need to override this method to take care of cached information that depends on the
     * resolved layout direction, or to inform child views that inherit their layout direction.
     *
     * The default implementation does nothing.
     *
     * @param layoutDirection the direction of the layout
     *
     * @see #LAYOUT_DIRECTION_LTR
     * @see #LAYOUT_DIRECTION_RTL
     */
    public void onRtlPropertiesChanged(int layoutDirection) {
    }

    /**
     * Returns the resolved layout direction for this view.
     *
     * @return {@link #LAYOUT_DIRECTION_RTL} if the layout direction is RTL or returns
     * {@link #LAYOUT_DIRECTION_LTR} if the layout direction is not RTL.
     *
     * For compatibility, this will return {@link #LAYOUT_DIRECTION_LTR} if API version
     * is lower than {@link android.os.Build.VERSION_CODES#JELLY_BEAN_MR1}.
     *
     * @attr ref android.R.styleable#View_layoutDirection
     */
    public int getLayoutDirection() {
        final int targetSdkVersion = getContext().getApplicationInfo().targetSdkVersion;
        if (targetSdkVersion < JELLY_BEAN_MR1) {
            mPrivateFlags2 |= PFLAG2_LAYOUT_DIRECTION_RESOLVED;
            return LAYOUT_DIRECTION_RESOLVED_DEFAULT;
        }
        return ((mPrivateFlags2 & PFLAG2_LAYOUT_DIRECTION_RESOLVED_RTL) ==
                PFLAG2_LAYOUT_DIRECTION_RESOLVED_RTL) ? LAYOUT_DIRECTION_RTL : LAYOUT_DIRECTION_LTR;
    }

    /**
     * Indicates whether or not this view's layout is right-to-left. This is resolved from
     * layout attribute and/or the inherited value from the parent
     *
     * @return true if the layout is right-to-left.
     *
     * @hide
     */
    public boolean isLayoutRtl() {
        return (getLayoutDirection() == LAYOUT_DIRECTION_RTL);
    }
    
    /**
     * Indicates whether the view is currently tracking transient state that the
     * app should not need to concern itself with saving and restoring, but that
     * the framework should take special note to preserve when possible.
     *
     * <p>A view with transient state cannot be trivially rebound from an external
     * data source, such as an adapter binding item views in a list. This may be
     * because the view is performing an animation, tracking user selection
     * of content, or similar.</p>
     *
     * @return true if the view has transient state
     */
    public boolean hasTransientState() {
        return (mPrivateFlags2 & PFLAG2_HAS_TRANSIENT_STATE) == PFLAG2_HAS_TRANSIENT_STATE;
    }

    /**
     * Set whether this view is currently tracking transient state that the
     * framework should attempt to preserve when possible. This flag is reference counted,
     * so every call to setHasTransientState(true) should be paired with a later call
     * to setHasTransientState(false).
     *
     * <p>A view with transient state cannot be trivially rebound from an external
     * data source, such as an adapter binding item views in a list. This may be
     * because the view is performing an animation, tracking user selection
     * of content, or similar.</p>
     *
     * @param hasTransientState true if this view has transient state
     */
    public void setHasTransientState(boolean hasTransientState) {
        mTransientStateCount = hasTransientState ? mTransientStateCount + 1 :
                mTransientStateCount - 1;
        if (mTransientStateCount < 0) {
            mTransientStateCount = 0;
            Log.e(VIEW_LOG_TAG, "hasTransientState decremented below 0: " +
                    "unmatched pair of setHasTransientState calls");
        } else if ((hasTransientState && mTransientStateCount == 1) ||
                (!hasTransientState && mTransientStateCount == 0)) {
            // update flag if we've just incremented up from 0 or decremented down to 0
            mPrivateFlags2 = (mPrivateFlags2 & ~PFLAG2_HAS_TRANSIENT_STATE) |
                    (hasTransientState ? PFLAG2_HAS_TRANSIENT_STATE : 0);
            if (mParent != null) {
                try {
                    mParent.childHasTransientStateChanged(this, hasTransientState);
                } catch (AbstractMethodError e) {
                    Log.e(VIEW_LOG_TAG, mParent.getClass().getSimpleName() +
                            " does not fully implement ViewParent", e);
                }
            }
        }
    }
    
	/**
	 * 判断是否为scrollView控件
	 * @return  true 为可以滚动的控件，比如listview，或者scrollview
	 */
	public boolean isScrollView(){
		return false;
	}
    
    /**
     * Return the visible drawing bounds of your view. Fills in the output
     * rectangle with the values from getScrollX(), getScrollY(),
     * getWidth(), and getHeight(). These bounds do not account for any
     * transformation properties currently set on the view, such as
     * {@link #setScaleX(float)} or {@link #setRotation(float)}.
     *
     * @param outRect The (scrolled) drawing bounds of the view.
     */
    public void getDrawingRect(Rect outRect) {
        outRect.left = mScrollX;
        outRect.top = mScrollY;
        outRect.right = mScrollX + (mRight - mLeft);
        outRect.bottom = mScrollY + (mBottom - mTop);
    }
    
    /**
     * Register a callback to be invoked when a hardware key is pressed in this view.
     * Key presses in software input methods will generally not trigger the methods of
     * this listener.
     * @param l the key listener to attach to this view
     */
    public void setOnKeyListener(OnKeyListener l) {
        getListenerInfo().mOnKeyListener = l;
    }
    
    /**
     * Register a callback to be invoked when a touch event is sent to this view.
     * @param l the touch listener to attach to this view
     */
    public void setOnTouchListener(OnTouchListener l) {
        getListenerInfo().mOnTouchListener = l;
    }
    
    /**
     * Register a callback to be invoked when this view is clicked. If this view is not
     * clickable, it becomes clickable.
     *
     * @param l The callback that will run
     *
     * @see #setClickable(boolean)
     */
    public void setOnClickListener(OnClickListener l) {
        if (!isClickable()) {
            setClickable(true);
        }
        getListenerInfo().mOnClickListener = l;
    }
    
    /**
     * Return whether this view has an attached OnClickListener.  Returns
     * true if there is a listener, false if there is none.
     */
    public boolean hasOnClickListeners() {
        ListenerInfo li = mListenerInfo;
        return (li != null && li.mOnClickListener != null);
    }

    /**
     * Register a callback to be invoked when this view is clicked and held. If this view is not
     * long clickable, it becomes long clickable.
     *
     * @param l The callback that will run
     *
     * @see #setLongClickable(boolean)
     */
    public void setOnLongClickListener(OnLongClickListener l) {
        if (!isLongClickable()) {
            setLongClickable(true);
        }
        getListenerInfo().mOnLongClickListener = l;
    }
    
    /**
     * Register a callback to be invoked when focus of this view changed.
     *
     * @param l The callback that will run.
     */
    public void setOnFocusChangeListener(OnFocusChangeListener l) {
        getListenerInfo().mOnFocusChangeListener = l;
    }
    
    /**
     * Returns the focus-change callback registered for this view.
     *
     * @return The callback, or null if one is not registered.
     */
    public OnFocusChangeListener getOnFocusChangeListener() {
        ListenerInfo li = mListenerInfo;
        return li != null ? li.mOnFocusChangeListener : null;
    }
    
    /**
     * Returns whether the device is currently in touch mode.  Touch mode is entered
     * once the user begins interacting with the device by touch, and affects various
     * things like whether focus is always visible to the user.
     *
     * @return Whether the device is in touch mode.
     */
    public boolean isInTouchMode() {
        if (mAttachInfo != null) {
//            return mAttachInfo.mViewRootImpl.isInTouchMode();
        	return mAttachInfo.mInTouchMode;
        }
        return false;
    }

    /**
     * Handle a key event before it is processed by any input method
     * associated with the view hierarchy.  This can be used to intercept
     * key events in special situations before the IME consumes them; a
     * typical example would be handling the BACK key to update the application's
     * UI instead of allowing the IME to see it and close itself.
     *
     * @param keyCode The value in event.getKeyCode().
     * @param event Description of the key event.
     * @return If you handled the event, return true. If you want to allow the
     *         event to be handled by the next receiver, return false.
     */
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        return false;
    }

    /**
     * Default implementation of {@link KeyEvent.Callback#onKeyDown(int, KeyEvent)
     * KeyEvent.Callback.onKeyDown()}: perform press of the view
     * when {@link KeyEvent#KEYCODE_DPAD_CENTER} or {@link KeyEvent#KEYCODE_ENTER}
     * is released, if the view is enabled and clickable.
     *
     * <p>Key presses in software keyboards will generally NOT trigger this listener,
     * although some may elect to do so in some situations. Do not rely on this to
     * catch software key presses.
     *
     * @param keyCode A key code that represents the button pressed, from
     *                {@link android.view.KeyEvent}.
     * @param event   The KeyEvent object that defines the button action.
     */
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean result = false;

        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER/*KeyEvent.isConfirmKey(keyCode)*/) {
            if ((mViewFlags & ENABLED_MASK) == DISABLED) {
                return true;
            }
            // Long clickable items don't necessarily have to be clickable
            if (((mViewFlags & CLICKABLE) == CLICKABLE ||
                    (mViewFlags & LONG_CLICKABLE) == LONG_CLICKABLE) &&
                    (event.getRepeatCount() == 0)) {
                setPressed(true);
                checkForLongClick(0);
                return true;
            }
        }
        return result;
    }

    /**
     * Default implementation of {@link KeyEvent.Callback#onKeyLongPress(int, KeyEvent)
     * KeyEvent.Callback.onKeyLongPress()}: always returns false (doesn't handle
     * the event).
     * <p>Key presses in software keyboards will generally NOT trigger this listener,
     * although some may elect to do so in some situations. Do not rely on this to
     * catch software key presses.
     */
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        return false;
    }

    /**
     * Default implementation of {@link KeyEvent.Callback#onKeyUp(int, KeyEvent)
     * KeyEvent.Callback.onKeyUp()}: perform clicking of the view
     * when {@link KeyEvent#KEYCODE_DPAD_CENTER} or
     * {@link KeyEvent#KEYCODE_ENTER} is released.
     * <p>Key presses in software keyboards will generally NOT trigger this listener,
     * although some may elect to do so in some situations. Do not rely on this to
     * catch software key presses.
     *
     * @param keyCode A key code that represents the button pressed, from
     *                {@link android.view.KeyEvent}.
     * @param event   The KeyEvent object that defines the button action.
     */
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER/*KeyEvent.isConfirmKey(keyCode)*/) {
            if ((mViewFlags & ENABLED_MASK) == DISABLED) {
                return true;
            }
            if ((mViewFlags & CLICKABLE) == CLICKABLE && isPressed()) {
                setPressed(false);

                if (!mHasPerformedLongPress) {
                    // This is a tap, so remove the longpress check
                    removeLongPressCallback();
                    return performClick();
                }
            }
        }
        return false;
    }
	
    /**
     * Returns this GLView's tag.
     *
     * @return the Object stored in this view as a tag
     *
     * @see #setTag(Object)
     * @see #getTag(int)
     */
    public Object getTag() {
        return mTag;
    }

    /**
     * Sets the tag associated with this view. A tag can be used to mark
     * a view in its hierarchy and does not have to be unique within the
     * hierarchy. Tags can also be used to store data within a view without
     * resorting to another data structure.
     *
     * @param tag an Object to tag the view with
     *
     * @see #getTag()
     * @see #setTag(int, Object)
     */
    public void setTag(final Object tag) {
        mTag = tag;
    }
    
    /**
     * Returns the tag associated with this view and the specified key.
     *
     * @param key The key identifying the tag
     *
     * @return the Object stored in this view as a tag, or {@code null} if not
     *         set
     *
     * @see #setTag(int, Object)
     * @see #getTag()
     */
    public Object getTag(int key) {
        if (mKeyedTags != null) return mKeyedTags.get(key);
        return null;
    }
    
    /**
     * Sets a tag associated with this view and a key. A tag can be used
     * to mark a view in its hierarchy and does not have to be unique within
     * the hierarchy. Tags can also be used to store data within a view
     * without resorting to another data structure.
     *
     * The specified key should be an id declared in the resources of the
     * application to ensure it is unique (see the <a
     * href={@docRoot}guide/topics/resources/more-resources.html#Id">ID resource type</a>).
     * Keys identified as belonging to
     * the Android framework or not associated with any package will cause
     * an {@link IllegalArgumentException} to be thrown.
     *
     * @param key The key identifying the tag
     * @param tag An Object to tag the view with
     *
     * @throws IllegalArgumentException If they specified key is not valid
     *
     * @see #setTag(Object)
     * @see #getTag(int)
     */
    public void setTag(int key, final Object tag) {
        // If the package id is 0x00 or 0x01, it's either an undefined package
        // or a framework id
        /*if ((key >>> 24) < 2) {
            throw new IllegalArgumentException("The key must be an application-specific "
                    + "resource id.");
        }*/

        setKeyedTag(key, tag);
    }
    
    /**
     * Variation of {@link #setTag(int, Object)} that enforces the key to be a
     * framework id.
     *
     * @hide
     */
    public void setTagInternal(int key, Object tag) {
        /*if ((key >>> 24) != 0x1) {
            throw new IllegalArgumentException("The key must be a framework-specific "
                    + "resource id.");
        }*/

        setKeyedTag(key, tag);
    }

    private void setKeyedTag(int key, Object tag) {
        if (mKeyedTags == null) {
            mKeyedTags = new SparseArray<Object>(2);
        }

        mKeyedTags.put(key, tag);
    }

    /**
     * Sets the {@link View} description. It briefly describes the view and is
     * primarily used for accessibility support. Set this property to enable
     * better accessibility support for your application. This is especially
     * true for views that do not have textual representation (For example,
     * ImageButton).
     *
     * @param contentDescription The content description.
     *
     */
    public void setContentDescription(CharSequence contentDescription) {
        if (mContentDescription == null) {
            if (contentDescription == null) {
                return;
            }
        } else if (mContentDescription.equals(contentDescription)) {
            return;
        }
        mContentDescription = contentDescription;
    }

	@Override
	public boolean onKeyMultiple(int keyCode, int count, KeyEvent event) {
		return false;
	}
	
    ListenerInfo getListenerInfo() {
        if (mListenerInfo != null) {
            return mListenerInfo;
        }
        mListenerInfo = new ListenerInfo();
        return mListenerInfo;
    }
	
    ListenerInfo mListenerInfo;
	
    static class ListenerInfo {
        /**
         * Listener used to dispatch focus change events.
         * This field should be made private, so it is hidden from the SDK.
         * {@hide}
         */
        protected OnFocusChangeListener mOnFocusChangeListener;

        /**
         * Listeners for layout change events.
         */
        private ArrayList<OnLayoutChangeListener> mOnLayoutChangeListeners;

        /**
         * Listeners for attach events.
         */
        private CopyOnWriteArrayList<OnAttachStateChangeListener> mOnAttachStateChangeListeners;

        /**
         * Listener used to dispatch click events.
         * This field should be made private, so it is hidden from the SDK.
         * {@hide}
         */
        public OnClickListener mOnClickListener;

        /**
         * Listener used to dispatch long click events.
         * This field should be made private, so it is hidden from the SDK.
         * {@hide}
         */
        protected OnLongClickListener mOnLongClickListener;

        /**
         * Listener used to build the context menu.
         * This field should be made private, so it is hidden from the SDK.
         * {@hide}
         */
        protected OnCreateContextMenuListener mOnCreateContextMenuListener;

        private OnKeyListener mOnKeyListener;

        private OnTouchListener mOnTouchListener;

        private OnHoverListener mOnHoverListener;

        private OnGenericMotionListener mOnGenericMotionListener;

        private OnDragListener mOnDragListener;

        private OnSystemUiVisibilityChangeListener mOnSystemUiVisibilityChangeListener;
    }
    
    /**
     * Interface definition for a callback to be invoked when a hardware key event is
     * dispatched to this view. The callback will be invoked before the key event is
     * given to the view. This is only useful for hardware keyboards; a software input
     * method has no obligation to trigger this listener.
     */
    public interface OnKeyListener {
        /**
         * Called when a hardware key is dispatched to a view. This allows listeners to
         * get a chance to respond before the target view.
         * <p>Key presses in software keyboards will generally NOT trigger this method,
         * although some may elect to do so in some situations. Do not assume a
         * software input method has to be key-based; even if it is, it may use key presses
         * in a different way than you expect, so there is no way to reliably catch soft
         * input key presses.
         *
         * @param v The GLView the key has been dispatched to.
         * @param keyCode The code for the physical key that was pressed
         * @param event The KeyEvent object containing full information about
         *        the event.
         * @return True if the listener has consumed the event, false otherwise.
         */
        boolean onKey(View v, int keyCode, KeyEvent event);
    }
    /**
     * Interface definition for a callback to be invoked when the focus state of
     * a view changed.
     */
    public interface OnFocusChangeListener {
        /**
         * Called when the focus state of a view has changed.
         *
         * @param v The view whose state has changed.
         * @param hasFocus The new focus state of v.
         */
        void onFocusChange(View v, boolean hasFocus);
    }
    
    /**
     * Interface definition for a callback to be invoked when a touch event is
     * dispatched to this view. The callback will be invoked before the touch
     * event is given to the view.
     */
    public interface OnTouchListener {
        /**
         * Called when a touch event is dispatched to a view. This allows listeners to
         * get a chance to respond before the target view.
         *
         * @param v The view the touch event has been dispatched to.
         * @param event The MotionEvent object containing full information about
         *        the event.
         * @return True if the listener has consumed the event, false otherwise.
         */
        boolean onTouch(View v, MotionEvent event);
    }
    
    /**
     * Interface definition for a callback to be invoked when a view is clicked.
     */
    public interface OnClickListener {
        /**
         * Called when a view has been clicked.
         *
         * @param v The view that was clicked.
         */
        void onClick(View v);
    }
    
    /**
     * Interface definition for a callback to be invoked when a view has been clicked and held.
     */
    public interface OnLongClickListener {
        /**
         * Called when a view has been clicked and held.
         *
         * @param v The view that was clicked and held.
         *
         * @return true if the callback consumed the long click, false otherwise.
         */
        boolean onLongClick(View v);
    }
    
    @Override
    public String toString() {
    	return super.toString() + " , id = " + mID;
    }
    
    public void forceStopFocusAnimation(){
    	
    }    
        
    /**
     * Interface definition for a callback to be invoked when the layout bounds of a view
     * changes due to layout processing.
     */
    public interface OnLayoutChangeListener {
        /**
         * Called when the focus state of a view has changed.
         *
         * @param v The view whose state has changed.
         * @param left The new value of the view's left property.
         * @param top The new value of the view's top property.
         * @param right The new value of the view's right property.
         * @param bottom The new value of the view's bottom property.
         * @param oldLeft The previous value of the view's left property.
         * @param oldTop The previous value of the view's top property.
         * @param oldRight The previous value of the view's right property.
         * @param oldBottom The previous value of the view's bottom property.
         */
        void onLayoutChange(View v, int left, int top, int right, int bottom,
            int oldLeft, int oldTop, int oldRight, int oldBottom);
    }
    
    /**
     * Interface definition for a callback to be invoked when the status bar changes
     * visibility.  This reports <strong>global</strong> changes to the system UI
     * state, not what the application is requesting.
     *
     * @see View#setOnSystemUiVisibilityChangeListener(android.view.View.OnSystemUiVisibilityChangeListener)
     */
    public interface OnSystemUiVisibilityChangeListener {
        /**
         * Called when the status bar changes visibility because of a call to
         * {@link View#setSystemUiVisibility(int)}.
         *
         * @param visibility  Bitwise-or of flags {@link #SYSTEM_UI_FLAG_LOW_PROFILE},
         * {@link #SYSTEM_UI_FLAG_HIDE_NAVIGATION}, and {@link #SYSTEM_UI_FLAG_FULLSCREEN}.
         * This tells you the <strong>global</strong> state of these UI visibility
         * flags, not what your app is currently applying.
         */
        public void onSystemUiVisibilityChange(int visibility);
    }
    
    /**
     * Interface definition for a callback to be invoked when this view is attached
     * or detached from its window.
     */
    public interface OnAttachStateChangeListener {
        /**
         * Called when the view is attached to a window.
         * @param v The view that was attached
         */
        public void onViewAttachedToWindow(View v);
        /**
         * Called when the view is detached from a window.
         * @param v The view that was detached
         */
        public void onViewDetachedFromWindow(View v);
    }
    
    /**
     * Add a listener that will be called when the bounds of the view change due to
     * layout processing.
     *
     * @param listener The listener that will be called when layout bounds change.
     */
    public void addOnLayoutChangeListener(OnLayoutChangeListener listener) {
        ListenerInfo li = getListenerInfo();
        if (li.mOnLayoutChangeListeners == null) {
            li.mOnLayoutChangeListeners = new ArrayList<OnLayoutChangeListener>();
        }
        if (!li.mOnLayoutChangeListeners.contains(listener)) {
            li.mOnLayoutChangeListeners.add(listener);
        }
    }

    /**
     * Remove a listener for layout changes.
     *
     * @param listener The listener for layout bounds change.
     */
    public void removeOnLayoutChangeListener(OnLayoutChangeListener listener) {
        ListenerInfo li = mListenerInfo;
        if (li == null || li.mOnLayoutChangeListeners == null) {
            return;
        }
        li.mOnLayoutChangeListeners.remove(listener);
    }
    
    /**
     * Add a listener for attach state changes.
     *
     * This listener will be called whenever this view is attached or detached
     * from a window. Remove the listener using
     * {@link #removeOnAttachStateChangeListener(OnAttachStateChangeListener)}.
     *
     * @param listener Listener to attach
     * @see #removeOnAttachStateChangeListener(OnAttachStateChangeListener)
     */
    public void addOnAttachStateChangeListener(OnAttachStateChangeListener listener) {
        ListenerInfo li = getListenerInfo();
        if (li.mOnAttachStateChangeListeners == null) {
            li.mOnAttachStateChangeListeners
                    = new CopyOnWriteArrayList<OnAttachStateChangeListener>();
        }
        li.mOnAttachStateChangeListeners.add(listener);
    }

    /**
     * Remove a listener for attach state changes. The listener will receive no further
     * notification of window attach/detach events.
     *
     * @param listener Listener to remove
     * @see #addOnAttachStateChangeListener(OnAttachStateChangeListener)
     */
    public void removeOnAttachStateChangeListener(OnAttachStateChangeListener listener) {
        ListenerInfo li = mListenerInfo;
        if (li == null || li.mOnAttachStateChangeListeners == null) {
            return;
        }
        li.mOnAttachStateChangeListeners.remove(listener);
    }
    
    
    public Animation getAnimation(){
    	return mCurrentAnimation;
    }
    
    public void startAnimation(Animation animation){
        animation.setStartTime(Animation.START_ON_FIRST_FRAME);
        setAnimation(animation);
        invalidate(true);
    }
    
    public void cancelAnimation(){
    	if(mCurrentAnimation != null) mCurrentAnimation.cancel();
    }
    
    /**
     * Cancels any animations for this view.
     */
    public void clearAnimation() {
    	if (mCurrentAnimation != null) {
            mCurrentAnimation.detach();
            invalidate();
        }
        mCurrentAnimation = null;
    }
    
    public void setAnimation(Animation animation){
		mCurrentAnimation = animation;
		if (animation != null) {
			// If the screen is off assume the animation start time is now instead of
			// the next frame we draw. Keeping the START_ON_FIRST_FRAME start time
			// would cause the animation to start when the screen turns back on
			if (animation.getStartTime() == Animation.START_ON_FIRST_FRAME) {
//                animation.setStartTime(AnimationUtils.currentAnimationTimeMillis());
			}
			animation.reset();
		}
    }
    
    /**
     * Invoked by a parent ViewGroup to notify the start of the animation
     * currently associated with this view. If you override this method,
     * always call super.onAnimationStart();
     *
     * @see #setAnimation(android.view.animation.Animation)
     * @see #getAnimation()
     */
    protected void onAnimationStart() {
        mPrivateFlags |= PFLAG_ANIMATION_STARTED;
    }

    /**
     * Invoked by a parent ViewGroup to notify the end of the animation
     * currently associated with this view. If you override this method,
     * always call super.onAnimationEnd();
     *
     * @see #setAnimation(android.view.animation.Animation)
     * @see #getAnimation()
     */
    protected void onAnimationEnd() {
        mPrivateFlags &= ~PFLAG_ANIMATION_STARTED;
    }
    
    /**
     * Play a sound effect for this view.
     *
     * <p>The framework will play sound effects for some built in actions, such as
     * clicking, but you may wish to play these effects in your widget,
     * for instance, for internal navigation.
     *
     * <p>The sound effect will only be played if sound effects are enabled by the user, and
     * {@link #isSoundEffectsEnabled()} is true.
     *
     * @param soundConstant One of the constants defined in {@link SoundEffectConstants}
     */
    public void playSoundEffect(int soundConstant) {
        if (mAttachInfo == null || mAttachInfo.mRootCallbacks == null || !isSoundEffectsEnabled()) {
            return;
        }
        mAttachInfo.mRootCallbacks.playSoundEffect(soundConstant);
    }
    
    /**
     * BZZZTT!!1!
     *
     * <p>Provide haptic feedback to the user for this view.
     *
     * <p>The framework will provide haptic feedback for some built in actions,
     * such as long presses, but you may wish to provide feedback for your
     * own widget.
     *
     * <p>The feedback will only be performed if
     * {@link #isHapticFeedbackEnabled()} is true.
     *
     * @param feedbackConstant One of the constants defined in
     * {@link HapticFeedbackConstants}
     */
    public boolean performHapticFeedback(int feedbackConstant) {
        return performHapticFeedback(feedbackConstant, 0);
    }

    /**
     * BZZZTT!!1!
     *
     * <p>Like {@link #performHapticFeedback(int)}, with additional options.
     *
     * @param feedbackConstant One of the constants defined in
     * {@link HapticFeedbackConstants}
     * @param flags Additional flags as per {@link HapticFeedbackConstants}.
     */
    public boolean performHapticFeedback(int feedbackConstant, int flags) {
        if (mAttachInfo == null) {
            return false;
        }
        //noinspection SimplifiableIfStatement
        if ((flags & HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING) == 0
                && !isHapticFeedbackEnabled()) {
            return false;
        }
        return mAttachInfo.mRootCallbacks.performHapticFeedback(feedbackConstant,
                (flags & HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING) != 0);
    }
    
    /**
     * Request that the visibility of the status bar or other screen/window
     * decorations be changed.
     *
     * <p>This method is used to put the over device UI into temporary modes
     * where the user's attention is focused more on the application content,
     * by dimming or hiding surrounding system affordances.  This is typically
     * used in conjunction with {@link Window#FEATURE_ACTION_BAR_OVERLAY
     * Window.FEATURE_ACTION_BAR_OVERLAY}, allowing the applications content
     * to be placed behind the action bar (and with these flags other system
     * affordances) so that smooth transitions between hiding and showing them
     * can be done.
     *
     * <p>Two representative examples of the use of system UI visibility is
     * implementing a content browsing application (like a magazine reader)
     * and a video playing application.
     *
     * <p>The first code shows a typical implementation of a View in a content
     * browsing application.  In this implementation, the application goes
     * into a content-oriented mode by hiding the status bar and action bar,
     * and putting the navigation elements into lights out mode.  The user can
     * then interact with content while in this mode.  Such an application should
     * provide an easy way for the user to toggle out of the mode (such as to
     * check information in the status bar or access notifications).  In the
     * implementation here, this is done simply by tapping on the content.
     *
     * {@sample development/samples/ApiDemos/src/com/example/android/apis/view/ContentBrowserActivity.java
     *      content}
     *
     * <p>This second code sample shows a typical implementation of a View
     * in a video playing application.  In this situation, while the video is
     * playing the application would like to go into a complete full-screen mode,
     * to use as much of the display as possible for the video.  When in this state
     * the user can not interact with the application; the system intercepts
     * touching on the screen to pop the UI out of full screen mode.  See
     * {@link #fitSystemWindows(Rect)} for a sample layout that goes with this code.
     *
     * {@sample development/samples/ApiDemos/src/com/example/android/apis/view/VideoPlayerActivity.java
     *      content}
     *
     * @param visibility  Bitwise-or of flags {@link #SYSTEM_UI_FLAG_LOW_PROFILE},
     * {@link #SYSTEM_UI_FLAG_HIDE_NAVIGATION}, {@link #SYSTEM_UI_FLAG_FULLSCREEN},
     * {@link #SYSTEM_UI_FLAG_LAYOUT_STABLE}, {@link #SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION},
     * {@link #SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN}, {@link #SYSTEM_UI_FLAG_IMMERSIVE},
     * and {@link #SYSTEM_UI_FLAG_IMMERSIVE_STICKY}.
     */
    public void setSystemUiVisibility(int visibility) {
        if (visibility != mSystemUiVisibility) {
            mSystemUiVisibility = visibility;
            if (mParent != null && mAttachInfo != null && !mAttachInfo.mRecomputeGlobalAttributes) {
                mParent.recomputeViewAttributes(this);
            } else if (mAttachInfo != null && !mAttachInfo.mRecomputeGlobalAttributes) {
            	getViewRootImpl().recomputeViewAttributes(this);
            }
        }
    }

    /**
     * Returns the last {@link #setSystemUiVisibility(int)} that this view has requested.
     * @return  Bitwise-or of flags {@link #SYSTEM_UI_FLAG_LOW_PROFILE},
     * {@link #SYSTEM_UI_FLAG_HIDE_NAVIGATION}, {@link #SYSTEM_UI_FLAG_FULLSCREEN},
     * {@link #SYSTEM_UI_FLAG_LAYOUT_STABLE}, {@link #SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION},
     * {@link #SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN}, {@link #SYSTEM_UI_FLAG_IMMERSIVE},
     * and {@link #SYSTEM_UI_FLAG_IMMERSIVE_STICKY}.
     */
    public int getSystemUiVisibility() {
        return mSystemUiVisibility;
    }
    
    /**
     * Returns the current system UI visibility that is currently set for
     * the entire window.  This is the combination of the
     * {@link #setSystemUiVisibility(int)} values supplied by all of the
     * views in the window.
     */
    public int getWindowSystemUiVisibility() {
        return mAttachInfo != null ? mAttachInfo.mSystemUiVisibility : 0;
    }

    /**
     * Override to find out when the window's requested system UI visibility
     * has changed, that is the value returned by {@link #getWindowSystemUiVisibility()}.
     * This is different from the callbacks received through
     * {@link #setOnSystemUiVisibilityChangeListener(OnSystemUiVisibilityChangeListener)}
     * in that this is only telling you about the local request of the window,
     * not the actual values applied by the system.
     */
    public void onWindowSystemUiVisibilityChanged(int visible) {
    }

    /**
     * Dispatch callbacks to {@link #onWindowSystemUiVisibilityChanged(int)} down
     * the view hierarchy.
     */
    public void dispatchWindowSystemUiVisiblityChanged(int visible) {
        onWindowSystemUiVisibilityChanged(visible);
    }

    /**
     * Set a listener to receive callbacks when the visibility of the system bar changes.
     * @param l  The {@link OnSystemUiVisibilityChangeListener} to receive callbacks.
     */
    public void setOnSystemUiVisibilityChangeListener(OnSystemUiVisibilityChangeListener l) {
        getListenerInfo().mOnSystemUiVisibilityChangeListener = l;
        if (mParent != null && mAttachInfo != null && !mAttachInfo.mRecomputeGlobalAttributes) {
            mParent.recomputeViewAttributes(this);
        }
    }

    /**
     * Dispatch callbacks to {@link #setOnSystemUiVisibilityChangeListener} down
     * the view hierarchy.
     */
    public void dispatchSystemUiVisibilityChanged(int visibility) {
        ListenerInfo li = mListenerInfo;
        if (li != null && li.mOnSystemUiVisibilityChangeListener != null) {
            li.mOnSystemUiVisibilityChangeListener.onSystemUiVisibilityChange(
                    visibility & PUBLIC_STATUS_BAR_VISIBILITY_MASK);
        }
    }

    boolean updateLocalSystemUiVisibility(int localValue, int localChanges) {
        int val = (mSystemUiVisibility&~localChanges) | (localValue&localChanges);
        if (val != mSystemUiVisibility) {
            setSystemUiVisibility(val);
            return true;
        }
        return false;
    }
    
    /**
     * Private function to aggregate all per-view attributes in to the view
     * root.
     */
    void dispatchCollectViewAttributes(AttachInfo attachInfo, int visibility) {
        performCollectViewAttributes(attachInfo, visibility);
    }

    void performCollectViewAttributes(AttachInfo attachInfo, int visibility) {
        if ((visibility & VISIBILITY_MASK) == VISIBLE) {
            if ((mViewFlags & KEEP_SCREEN_ON) == KEEP_SCREEN_ON) {
                attachInfo.mKeepScreenOn = true;
            }
            attachInfo.mSystemUiVisibility |= mSystemUiVisibility;
            ListenerInfo li = mListenerInfo;
            if (li != null && li.mOnSystemUiVisibilityChangeListener != null) {
                attachInfo.mHasSystemUiListeners = true;
            }
        }
    }
    
    void needGlobalAttributesUpdate(boolean force) {
        final AttachInfo ai = mAttachInfo;
        if (ai != null && !ai.mRecomputeGlobalAttributes) {
            if (force || ai.mKeepScreenOn || (ai.mSystemUiVisibility != 0)
                    || ai.mHasSystemUiListeners) {
                ai.mRecomputeGlobalAttributes = true;
            }
        }
    }
    
    /**
     * A MeasureSpec encapsulates the layout requirements passed from parent to child.
     * Each MeasureSpec represents a requirement for either the width or the height.
     * A MeasureSpec is comprised of a size and a mode. There are three possible
     * modes:
     * <dl>
     * <dt>UNSPECIFIED</dt>
     * <dd>
     * The parent has not imposed any constraint on the child. It can be whatever size
     * it wants.
     * </dd>
     *
     * <dt>EXACTLY</dt>
     * <dd>
     * The parent has determined an exact size for the child. The child is going to be
     * given those bounds regardless of how big it wants to be.
     * </dd>
     *
     * <dt>AT_MOST</dt>
     * <dd>
     * The child can be as large as it wants up to the specified size.
     * </dd>
     * </dl>
     *
     * MeasureSpecs are implemented as ints to reduce object allocation. This class
     * is provided to pack and unpack the &lt;size, mode&gt; tuple into the int.
     */
    public static class MeasureSpec {
        private static final int MODE_SHIFT = 30;
        private static final int MODE_MASK  = 0x3 << MODE_SHIFT;

        /**
         * Measure specification mode: The parent has not imposed any constraint
         * on the child. It can be whatever size it wants.
         */
        public static final int UNSPECIFIED = 0 << MODE_SHIFT;

        /**
         * Measure specification mode: The parent has determined an exact size
         * for the child. The child is going to be given those bounds regardless
         * of how big it wants to be.
         */
        public static final int EXACTLY     = 1 << MODE_SHIFT;

        /**
         * Measure specification mode: The child can be as large as it wants up
         * to the specified size.
         */
        public static final int AT_MOST     = 2 << MODE_SHIFT;

        /**
         * Creates a measure specification based on the supplied size and mode.
         *
         * The mode must always be one of the following:
         * <ul>
         *  <li>{@link android.view.View.MeasureSpec#UNSPECIFIED}</li>
         *  <li>{@link android.view.View.MeasureSpec#EXACTLY}</li>
         *  <li>{@link android.view.View.MeasureSpec#AT_MOST}</li>
         * </ul>
         *
         * @param size the size of the measure specification
         * @param mode the mode of the measure specification
         * @return the measure specification based on size and mode
         */
        public static int makeMeasureSpec(int size, int mode) {
            return size + mode;
        }

        /**
         * Extracts the mode from the supplied measure specification.
         *
         * @param measureSpec the measure specification to extract the mode from
         * @return {@link android.view.View.MeasureSpec#UNSPECIFIED},
         *         {@link android.view.View.MeasureSpec#AT_MOST} or
         *         {@link android.view.View.MeasureSpec#EXACTLY}
         */
        public static int getMode(int measureSpec) {
            return (measureSpec & MODE_MASK);
        }

        /**
         * Extracts the size from the supplied measure specification.
         *
         * @param measureSpec the measure specification to extract the size from
         * @return the size in pixels defined in the supplied measure specification
         */
        public static int getSize(int measureSpec) {
            return (measureSpec & ~MODE_MASK);
        }

        /**
         * Returns a String representation of the specified measure
         * specification.
         *
         * @param measureSpec the measure specification to convert to a String
         * @return a String with the following format: "MeasureSpec: MODE SIZE"
         */
        public static String toString(int measureSpec) {
            int mode = getMode(measureSpec);
            int size = getSize(measureSpec);

            StringBuilder sb = new StringBuilder("MeasureSpec: ");

            if (mode == UNSPECIFIED)
                sb.append("UNSPECIFIED ");
            else if (mode == EXACTLY)
                sb.append("EXACTLY ");
            else if (mode == AT_MOST)
                sb.append("AT_MOST ");
            else
                sb.append(mode).append(" ");

            sb.append(size);
            return sb.toString();
        }
    }
    
    private final class CheckForLongPress implements Runnable {
        private int mOriginalWindowAttachCount;

        @Override
        public void run() {
            if (isPressed() && (mParent != null)
                    && mOriginalWindowAttachCount == mWindowAttachCount) {
                if (performLongClick()) {
                    mHasPerformedLongPress = true;
                }
            }
        }

        public void rememberWindowAttachCount() {
            mOriginalWindowAttachCount = mWindowAttachCount;
        }
    }

    private final class CheckForTap implements Runnable {
        public float x;
        public float y;

        @Override
        public void run() {
            mPrivateFlags &= ~PFLAG_PREPRESSED;
            setPressed(true, x, y);
            checkForLongClick(ViewConfiguration.getTapTimeout());
        }
    }
    
    private final class PerformClick implements Runnable {
        @Override
        public void run() {
            performClick();
        }
    }
    
    private final class UnsetPressedState implements Runnable {
        @Override
        public void run() {
            setPressed(false);
        }
    }
    
    public Context getContext(){
    	return mContext;
    }

    @Override
	public void invalidateDrawable(Drawable who) {
    	invalidate();
	}

	@Override
	public void scheduleDrawable(Drawable who, Runnable what, long when) {
		if (mAttachInfo != null) {
			mAttachInfo.mHandler.postAtTime(what, when);
		}
	}

	@Override
	public void unscheduleDrawable(Drawable who, Runnable what) {
		if (mAttachInfo != null) {
			mAttachInfo.mHandler.removeCallbacks(what);
		}
	}
	
	/**
     * Unschedule any events associated with the given Drawable.  This can be
     * used when selecting a new Drawable into a view, so that the previous
     * one is completely unscheduled.
     *
     * @param who The Drawable to unschedule.
     *
     * @see #drawableStateChanged
     */
    public void unscheduleDrawable(Drawable who) {
    }
	
	/**
     * A set of information given to a view when it is attached to its parent
     * window.
     */
    final static class AttachInfo {
        interface Callbacks {
            void playSoundEffect(int effectId);
            boolean performHapticFeedback(int effectId, boolean always);
        }

        boolean mInTouchMode;
        
        /**
         * Indicates that ViewAncestor should trigger a global layout change
         * the next time it performs a traversal
         */
        boolean mRecomputeGlobalAttributes;

        /**
         * Always report new attributes at next traversal.
         */
        boolean mForceReportNewAttributes;

        /**
         * Set during a traveral if any views want to keep the screen on.
         */
        boolean mKeepScreenOn;

        /**
         * Bitwise-or of all of the values that views have passed to setSystemUiVisibility().
         */
        int mSystemUiVisibility;

        /**
         * Hack to force certain system UI visibility flags to be cleared.
         */
        int mDisabledSystemUiVisibility;

        /**
         * Last global system UI visibility reported by the window manager.
         */
        int mGlobalSystemUiVisibility;

        /**
         * True if a view in this hierarchy has an OnSystemUiVisibilityChangeListener
         * attached.
         */
        boolean mHasSystemUiListeners;
        
        /**
         * Set to true if a view has been scrolled.
         */
        boolean mViewScrollChanged;
        
        View mRootView;
        
        /**
         * The view tree observer used to dispatch global events like
         * layout, pre-draw, touch mode change, etc.
         */
        final ViewTreeObserver mTreeObserver = new ViewTreeObserver();
        
        /**
         * Indicates whether the view's window currently has the focus.
         */
        boolean mHasWindowFocus;
        
        /**
         * Indicates the time at which drawing started to occur.
         */
        long mDrawingTime;

        /**
         * A Handler supplied by a view's {@link android.view.ViewRootImpl}. This
         * handler can be used to pump events in the UI events queue.
         */
        final Handler mHandler;
        
        final android.os.Handler mAndroidHandler;
        
        final Callbacks mRootCallbacks;
        
        final WindowId mWindowId;
        
        final GLRootView mViewRootImpl;

        /**
         * Creates a new set of attachment information with the specified
         * events handler and thread.
         *
         * @param handler the events handler the view must use
         */
        AttachInfo(GLRootView glRootView, WindowId windowId, Handler handler, android.os.Handler androidHandler, Callbacks effectPlayer) {
        	mViewRootImpl = glRootView;
        	mWindowId = windowId;
            mHandler = handler;
            mAndroidHandler = androidHandler;
            mRootCallbacks = effectPlayer;
        }
    }
    
    /**
     * <p>ScrollabilityCache holds various fields used by a View when scrolling
     * is supported. This avoids keeping too many unused fields in most
     * instances of View.</p>
     */
    private static class ScrollabilityCache implements Runnable {

        /**
         * Scrollbars are not visible
         */
        public static final int OFF = 0;

        /**
         * Scrollbars are visible
         */
        public static final int ON = 1;

        /**
         * Scrollbars are fading away
         */
        public static final int FADING = 2;

        public boolean fadeScrollBars;

        public int fadingEdgeLength;
        public int scrollBarDefaultDelayBeforeFade;
        public int scrollBarFadeDuration;

        public int scrollBarSize;
        public ScrollBarDrawable scrollBar;
        public float[] interpolatorValues;
        public View host;

        public final GLPaint paint;
        public final Matrix matrix;
        public BaseShader shader;

        public final Interpolator scrollBarInterpolator = new Interpolator(1, 2);

        private static final float[] OPAQUE = { 255 };
        private static final float[] TRANSPARENT = { 0.0f };

        /**
         * When fading should start. This time moves into the future every time
         * a new scroll happens. Measured based on SystemClock.uptimeMillis()
         */
        public long fadeStartTime;


        /**
         * The current state of the scrollbars: ON, OFF, or FADING
         */
        public int state = OFF;

        private int mLastColor;

        public ScrollabilityCache(ViewConfiguration configuration, View host) {
            fadingEdgeLength = configuration.getScaledFadingEdgeLength();
            scrollBarSize = configuration.getScaledScrollBarSize();
            scrollBarDefaultDelayBeforeFade = ViewConfiguration.getScrollDefaultDelay();
            scrollBarFadeDuration = ViewConfiguration.getScrollBarFadeDuration();

            paint = new GLPaint();
            matrix = new Matrix();
            // use use a height of 1, and then wack the matrix each time we
            // actually use it.
            shader = new LinearGradient(0, 0, 0, 1, 0xFF000000, 0, TileMode.CLAMP);
            paint.setShader(shader);

            this.host = host;
        }

        public void setFadeColor(int color) {
            if (color != mLastColor) {
                mLastColor = color;

                if (color != 0) {
                    shader = new LinearGradient(0, 0, 0, 1, color | 0xFF000000,
                            color & 0x00FFFFFF, TileMode.CLAMP);
                    paint.setShader(shader);
                    // Restore the default transfer mode (src_over)
                } else {
                    shader = new LinearGradient(0, 0, 0, 1, 0xFF000000, 0, TileMode.CLAMP);
                    paint.setShader(shader);
                }
            }
        }

        public void run() {
            long now = AnimationUtils.currentAnimationTimeMillis();
            if (now >= fadeStartTime) {

                // the animation fades the scrollbars out by changing
                // the opacity (alpha) from fully opaque to fully
                // transparent
                int nextFrame = (int) now;
                int framesCount = 0;

                Interpolator interpolator = scrollBarInterpolator;

                // Start opaque
                interpolator.setKeyFrame(framesCount++, nextFrame, OPAQUE);

                // End transparent
                nextFrame += scrollBarFadeDuration;
                interpolator.setKeyFrame(framesCount, nextFrame, TRANSPARENT);

                state = FADING;

                // Kick off the fade animation
                host.invalidate(true);
            }
        }
    }
	
	// TODO RenderNode begin
	/**
     * Indicates that this view was specifically invalidated, not just dirtied because some
     * child view was invalidated. The flag is used to determine when we need to recreate
     * a view's display list (as opposed to just returning a reference to its existing
     * display list).
     *
     * @hide
     */
    static final int PFLAG_INVALIDATED                 = 0x80000000;
    
    /** {@hide} */
    static final int PFLAG_DRAWING_CACHE_VALID         = 0x00008000;
    
    /**
     * Flag indicating that the view has been through at least one layout since it
     * was last attached to a window.
     */
    static final int PFLAG3_IS_LAID_OUT = 0x4;
    
    /**
     * Flag indicating that a call to measure() was skipped and should be done
     * instead when layout() is invoked.
     */
    static final int PFLAG3_MEASURE_NEEDED_BEFORE_LAYOUT = 0x8;
    
    /**
     * Flag indicating that nested scrolling is enabled for this view.
     * The view will optionally cooperate with views up its parent chain to allow for
     * integrated nested scrolling along the same axis.
     */
    static final int PFLAG3_NESTED_SCROLLING_ENABLED = 0x80;
    
    /**
     * Always allow a user to over-scroll this view, provided it is a
     * view that can scroll.
     *
     * @see #getOverScrollMode()
     * @see #setOverScrollMode(int)
     */
    public static final int OVER_SCROLL_ALWAYS = 0;

    /**
     * Allow a user to over-scroll this view only if the content is large
     * enough to meaningfully scroll, provided it is a view that can scroll.
     *
     * @see #getOverScrollMode()
     * @see #setOverScrollMode(int)
     */
    public static final int OVER_SCROLL_IF_CONTENT_SCROLLS = 1;

    /**
     * Never allow a user to over-scroll this view.
     *
     * @see #getOverScrollMode()
     * @see #setOverScrollMode(int)
     */
    public static final int OVER_SCROLL_NEVER = 2;
    
    /**
     * Special constant for {@link #setSystemUiVisibility(int)}: View has
     * requested the system UI (status bar) to be visible (the default).
     *
     * @see #setSystemUiVisibility(int)
     */
    public static final int SYSTEM_UI_FLAG_VISIBLE = 0;

    /**
     * Flag for {@link #setSystemUiVisibility(int)}: View has requested the
     * system UI to enter an unobtrusive "low profile" mode.
     *
     * <p>This is for use in games, book readers, video players, or any other
     * "immersive" application where the usual system chrome is deemed too distracting.
     *
     * <p>In low profile mode, the status bar and/or navigation icons may dim.
     *
     * @see #setSystemUiVisibility(int)
     */
    public static final int SYSTEM_UI_FLAG_LOW_PROFILE = 0x00000001;

    /**
     * Flag for {@link #setSystemUiVisibility(int)}: View has requested that the
     * system navigation be temporarily hidden.
     *
     * <p>This is an even less obtrusive state than that called for by
     * {@link #SYSTEM_UI_FLAG_LOW_PROFILE}; on devices that draw essential navigation controls
     * (Home, Back, and the like) on screen, <code>SYSTEM_UI_FLAG_HIDE_NAVIGATION</code> will cause
     * those to disappear. This is useful (in conjunction with the
     * {@link android.view.WindowManager.LayoutParams#FLAG_FULLSCREEN FLAG_FULLSCREEN} and
     * {@link android.view.WindowManager.LayoutParams#FLAG_LAYOUT_IN_SCREEN FLAG_LAYOUT_IN_SCREEN}
     * window flags) for displaying content using every last pixel on the display.
     *
     * <p>There is a limitation: because navigation controls are so important, the least user
     * interaction will cause them to reappear immediately.  When this happens, both
     * this flag and {@link #SYSTEM_UI_FLAG_FULLSCREEN} will be cleared automatically,
     * so that both elements reappear at the same time.
     *
     * @see #setSystemUiVisibility(int)
     */
    public static final int SYSTEM_UI_FLAG_HIDE_NAVIGATION = 0x00000002;

    /**
     * Flag for {@link #setSystemUiVisibility(int)}: View has requested to go
     * into the normal fullscreen mode so that its content can take over the screen
     * while still allowing the user to interact with the application.
     *
     * <p>This has the same visual effect as
     * {@link android.view.WindowManager.LayoutParams#FLAG_FULLSCREEN
     * WindowManager.LayoutParams.FLAG_FULLSCREEN},
     * meaning that non-critical screen decorations (such as the status bar) will be
     * hidden while the user is in the View's window, focusing the experience on
     * that content.  Unlike the window flag, if you are using ActionBar in
     * overlay mode with {@link Window#FEATURE_ACTION_BAR_OVERLAY
     * Window.FEATURE_ACTION_BAR_OVERLAY}, then enabling this flag will also
     * hide the action bar.
     *
     * <p>This approach to going fullscreen is best used over the window flag when
     * it is a transient state -- that is, the application does this at certain
     * points in its user interaction where it wants to allow the user to focus
     * on content, but not as a continuous state.  For situations where the application
     * would like to simply stay full screen the entire time (such as a game that
     * wants to take over the screen), the
     * {@link android.view.WindowManager.LayoutParams#FLAG_FULLSCREEN window flag}
     * is usually a better approach.  The state set here will be removed by the system
     * in various situations (such as the user moving to another application) like
     * the other system UI states.
     *
     * <p>When using this flag, the application should provide some easy facility
     * for the user to go out of it.  A common example would be in an e-book
     * reader, where tapping on the screen brings back whatever screen and UI
     * decorations that had been hidden while the user was immersed in reading
     * the book.
     *
     * @see #setSystemUiVisibility(int)
     */
    public static final int SYSTEM_UI_FLAG_FULLSCREEN = 0x00000004;

    /**
     * Flag for {@link #setSystemUiVisibility(int)}: When using other layout
     * flags, we would like a stable view of the content insets given to
     * {@link #fitSystemWindows(Rect)}.  This means that the insets seen there
     * will always represent the worst case that the application can expect
     * as a continuous state.  In the stock Android UI this is the space for
     * the system bar, nav bar, and status bar, but not more transient elements
     * such as an input method.
     *
     * The stable layout your UI sees is based on the system UI modes you can
     * switch to.  That is, if you specify {@link #SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN}
     * then you will get a stable layout for changes of the
     * {@link #SYSTEM_UI_FLAG_FULLSCREEN} mode; if you specify
     * {@link #SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN} and
     * {@link #SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION}, then you can transition
     * to {@link #SYSTEM_UI_FLAG_FULLSCREEN} and {@link #SYSTEM_UI_FLAG_HIDE_NAVIGATION}
     * with a stable layout.  (Note that you should avoid using
     * {@link #SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION} by itself.)
     *
     * If you have set the window flag {@link WindowManager.LayoutParams#FLAG_FULLSCREEN}
     * to hide the status bar (instead of using {@link #SYSTEM_UI_FLAG_FULLSCREEN}),
     * then a hidden status bar will be considered a "stable" state for purposes
     * here.  This allows your UI to continually hide the status bar, while still
     * using the system UI flags to hide the action bar while still retaining
     * a stable layout.  Note that changing the window fullscreen flag will never
     * provide a stable layout for a clean transition.
     *
     * <p>If you are using ActionBar in
     * overlay mode with {@link Window#FEATURE_ACTION_BAR_OVERLAY
     * Window.FEATURE_ACTION_BAR_OVERLAY}, this flag will also impact the
     * insets it adds to those given to the application.
     */
    public static final int SYSTEM_UI_FLAG_LAYOUT_STABLE = 0x00000100;

    /**
     * Flag for {@link #setSystemUiVisibility(int)}: View would like its window
     * to be layed out as if it has requested
     * {@link #SYSTEM_UI_FLAG_HIDE_NAVIGATION}, even if it currently hasn't.  This
     * allows it to avoid artifacts when switching in and out of that mode, at
     * the expense that some of its user interface may be covered by screen
     * decorations when they are shown.  You can perform layout of your inner
     * UI elements to account for the navigation system UI through the
     * {@link #fitSystemWindows(Rect)} method.
     */
    public static final int SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION = 0x00000200;

    /**
     * Flag for {@link #setSystemUiVisibility(int)}: View would like its window
     * to be layed out as if it has requested
     * {@link #SYSTEM_UI_FLAG_FULLSCREEN}, even if it currently hasn't.  This
     * allows it to avoid artifacts when switching in and out of that mode, at
     * the expense that some of its user interface may be covered by screen
     * decorations when they are shown.  You can perform layout of your inner
     * UI elements to account for non-fullscreen system UI through the
     * {@link #fitSystemWindows(Rect)} method.
     */
    public static final int SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN = 0x00000400;

    /**
     * Flag for {@link #setSystemUiVisibility(int)}: View would like to remain interactive when
     * hiding the navigation bar with {@link #SYSTEM_UI_FLAG_HIDE_NAVIGATION}.  If this flag is
     * not set, {@link #SYSTEM_UI_FLAG_HIDE_NAVIGATION} will be force cleared by the system on any
     * user interaction.
     * <p>Since this flag is a modifier for {@link #SYSTEM_UI_FLAG_HIDE_NAVIGATION}, it only
     * has an effect when used in combination with that flag.</p>
     */
    public static final int SYSTEM_UI_FLAG_IMMERSIVE = 0x00000800;

    /**
     * Flag for {@link #setSystemUiVisibility(int)}: View would like to remain interactive when
     * hiding the status bar with {@link #SYSTEM_UI_FLAG_FULLSCREEN} and/or hiding the navigation
     * bar with {@link #SYSTEM_UI_FLAG_HIDE_NAVIGATION}.  Use this flag to create an immersive
     * experience while also hiding the system bars.  If this flag is not set,
     * {@link #SYSTEM_UI_FLAG_HIDE_NAVIGATION} will be force cleared by the system on any user
     * interaction, and {@link #SYSTEM_UI_FLAG_FULLSCREEN} will be force-cleared by the system
     * if the user swipes from the top of the screen.
     * <p>When system bars are hidden in immersive mode, they can be revealed temporarily with
     * system gestures, such as swiping from the top of the screen.  These transient system bars
     * will overlay app’s content, may have some degree of transparency, and will automatically
     * hide after a short timeout.
     * </p><p>Since this flag is a modifier for {@link #SYSTEM_UI_FLAG_FULLSCREEN} and
     * {@link #SYSTEM_UI_FLAG_HIDE_NAVIGATION}, it only has an effect when used in combination
     * with one or both of those flags.</p>
     */
    public static final int SYSTEM_UI_FLAG_IMMERSIVE_STICKY = 0x00001000;
    
    /**
     * @hide
     *
     * Makes system ui transparent.
     */
    public static final int SYSTEM_UI_TRANSPARENT = 0x00008000;

    /**
     * @hide
     */
    public static final int PUBLIC_STATUS_BAR_VISIBILITY_MASK = 0x00003FFF;
    
    /**
     * Indicates that the view does not have a layer.
     *
     * @see #getLayerType()
     * @see #setLayerType(int, android.graphics.Paint)
     * @see #LAYER_TYPE_SOFTWARE
     * @see #LAYER_TYPE_HARDWARE
     */
    public static final int LAYER_TYPE_NONE = 0;

    /**
     * <p>Indicates that the view has a software layer. A software layer is backed
     * by a bitmap and causes the view to be rendered using Android's software
     * rendering pipeline, even if hardware acceleration is enabled.</p>
     *
     * <p>Software layers have various usages:</p>
     * <p>When the application is not using hardware acceleration, a software layer
     * is useful to apply a specific color filter and/or blending mode and/or
     * translucency to a view and all its children.</p>
     * <p>When the application is using hardware acceleration, a software layer
     * is useful to render drawing primitives not supported by the hardware
     * accelerated pipeline. It can also be used to cache a complex view tree
     * into a texture and reduce the complexity of drawing operations. For instance,
     * when animating a complex view tree with a translation, a software layer can
     * be used to render the view tree only once.</p>
     * <p>Software layers should be avoided when the affected view tree updates
     * often. Every update will require to re-render the software layer, which can
     * potentially be slow (particularly when hardware acceleration is turned on
     * since the layer will have to be uploaded into a hardware texture after every
     * update.)</p>
     *
     * @see #getLayerType()
     * @see #setLayerType(int, android.graphics.Paint)
     * @see #LAYER_TYPE_NONE
     * @see #LAYER_TYPE_HARDWARE
     */
    public static final int LAYER_TYPE_SOFTWARE = 1;

    /**
     * <p>Indicates that the view has a hardware layer. A hardware layer is backed
     * by a hardware specific texture (generally Frame Buffer Objects or FBO on
     * OpenGL hardware) and causes the view to be rendered using Android's hardware
     * rendering pipeline, but only if hardware acceleration is turned on for the
     * view hierarchy. When hardware acceleration is turned off, hardware layers
     * behave exactly as {@link #LAYER_TYPE_SOFTWARE software layers}.</p>
     *
     * <p>A hardware layer is useful to apply a specific color filter and/or
     * blending mode and/or translucency to a view and all its children.</p>
     * <p>A hardware layer can be used to cache a complex view tree into a
     * texture and reduce the complexity of drawing operations. For instance,
     * when animating a complex view tree with a translation, a hardware layer can
     * be used to render the view tree only once.</p>
     * <p>A hardware layer can also be used to increase the rendering quality when
     * rotation transformations are applied on a view. It can also be used to
     * prevent potential clipping issues when applying 3D transforms on a view.</p>
     *
     * @see #getLayerType()
     * @see #setLayerType(int, android.graphics.Paint)
     * @see #LAYER_TYPE_NONE
     * @see #LAYER_TYPE_SOFTWARE
     */
    public static final int LAYER_TYPE_HARDWARE = 2;
    
    /**
     * Flag to indicate that this view was marked INVALIDATED, or had its display list
     * invalidated, prior to the current drawing iteration. If true, the view must re-draw
     * its display list. This flag, used only when hw accelerated, allows us to clear the
     * flag while retaining this information until it's needed (at getDisplayList() time and
     * in drawChild(), when we decide to draw a view's children's display lists into our own).
     *
     * {@hide}
     */
    boolean mRecreateDisplayList = false;
	
    RenderNode mRenderNode = new RenderNode();
    
    /**
     * The view's overlay layer. Developers get a reference to the overlay via getOverlay()
     * and add/remove objects to/from the overlay directly through the Overlay methods.
     */
    ViewOverlay mOverlay;
    
    ViewPropertyAnimator mAnimator;
    
    /**
     * This method returns a ViewPropertyAnimator object, which can be used to animate
     * specific properties on this View.
     *
     * @return ViewPropertyAnimator The ViewPropertyAnimator associated with this View.
     */
    public ViewPropertyAnimator animate() {
        if (mAnimator == null) {
            mAnimator = new ViewPropertyAnimator(this);
        }
        return mAnimator;
    }
    
    /**
     * Sets the name of the View to be used to identify Views in Transitions.
     * Names should be unique in the View hierarchy.
     *
     * @param transitionName The name of the View to uniquely identify it for Transitions.
     */
    public final void setTransitionName(String transitionName) {
        mTransitionName = transitionName;
    }

    /**
     * Returns the name of the View to be used to identify Views in Transitions.
     * Names should be unique in the View hierarchy.
     *
     * <p>This returns null if the View has not been given a name.</p>
     *
     * @return The name used of the View to be used to identify Views in Transitions or null
     * if no name has been given.
     */
    public String getTransitionName() {
        return mTransitionName;
    }
    
    public void setAlpha(float alpha) {
    	if (mRenderNode.setAlpha(alpha)) {
    		invalidateViewProperty();
    	}
    }
    
    /**
     * This property is hidden and intended only for use by the Fade transition, which
     * animates it to produce a visual translucency that does not side-effect (or get
     * affected by) the real alpha property. This value is composited with the other
     * alpha value (and the AlphaAnimation value, when that is present) to produce
     * a final visual translucency result, which is what is passed into the DisplayList.
     *
     * @hide
     */
    public void setTransitionAlpha(float alpha) {
        setAlpha(alpha);
    }
    
    /**
     * This property is hidden and intended only for use by the Fade transition, which
     * animates it to produce a visual translucency that does not side-effect (or get
     * affected by) the real alpha property. This value is composited with the other
     * alpha value (and the AlphaAnimation value, when that is present) to produce
     * a final visual translucency result, which is what is passed into the DisplayList.
     *
     * @hide
     */
    public float getTransitionAlpha() {
        return mRenderNode.getAlpha();
    }
    
    public float getAlpha() {
    	return mRenderNode.getAlpha();
    }
    
    /**
     * Top position of this view relative to its parent.
     *
     * @return The top of this view, in pixels.
     */
    public final int getTop() {
        return mTop;
    }

    /**
     * Sets the top position of this view relative to its parent. This method is meant to be called
     * by the layout system and should not generally be called otherwise, because the property
     * may be changed at any time by the layout.
     *
     * @param top The top of this view, in pixels.
     */
    public final void setTop(int top) {
        if (top != mTop) {
            // Double-invalidation is necessary to capture view's old and new areas
            invalidate(true);

            int width = mRight - mLeft;
            int oldHeight = mBottom - mTop;

            mTop = top;
            mRenderNode.setTop(mTop);

            sizeChange(width, mBottom - mTop, width, oldHeight);
            
            invalidate(true);

            mBackgroundSizeChanged = true;
            invalidateParentIfNeeded();
        }
    }

    /**
     * Bottom position of this view relative to its parent.
     *
     * @return The bottom of this view, in pixels.
     */
    public final int getBottom() {
        return mBottom;
    }

    /**
     * Sets the bottom position of this view relative to its parent. This method is meant to be
     * called by the layout system and should not generally be called otherwise, because the
     * property may be changed at any time by the layout.
     *
     * @param bottom The bottom of this view, in pixels.
     */
    public final void setBottom(int bottom) {
        if (bottom != mBottom) {
            // Double-invalidation is necessary to capture view's old and new areas
            invalidate(true);

            int width = mRight - mLeft;
            int oldHeight = mBottom - mTop;

            mBottom = bottom;
            mRenderNode.setBottom(mBottom);

            sizeChange(width, mBottom - mTop, width, oldHeight);

            invalidate(true);

            mBackgroundSizeChanged = true;
            invalidateParentIfNeeded();
        }
    }

    /**
     * Left position of this view relative to its parent.
     *
     * @return The left edge of this view, in pixels.
     */
    public final int getLeft() {
        return mLeft;
    }

    /**
     * Sets the left position of this view relative to its parent. This method is meant to be called
     * by the layout system and should not generally be called otherwise, because the property
     * may be changed at any time by the layout.
     *
     * @param left The left of this view, in pixels.
     */
    public final void setLeft(int left) {
        if (left != mLeft) {
            // Double-invalidation is necessary to capture view's old and new areas
            invalidate(true);

            int oldWidth = mRight - mLeft;
            int height = mBottom - mTop;

            mLeft = left;
            mRenderNode.setLeft(left);

            sizeChange(mRight - mLeft, height, oldWidth, height);

            invalidate(true);

            mBackgroundSizeChanged = true;
            invalidateParentIfNeeded();
        }
    }

    /**
     * Right position of this view relative to its parent.
     *
     * @return The right edge of this view, in pixels.
     */
    public final int getRight() {
        return mRight;
    }

    /**
     * Sets the right position of this view relative to its parent. This method is meant to be called
     * by the layout system and should not generally be called otherwise, because the property
     * may be changed at any time by the layout.
     *
     * @param right The right of this view, in pixels.
     */
    public final void setRight(int right) {
        if (right != mRight) {
            // Double-invalidation is necessary to capture view's old and new areas
            invalidate(true);

            int oldWidth = mRight - mLeft;
            int height = mBottom - mTop;

            mRight = right;
            mRenderNode.setRight(mRight);

            sizeChange(mRight - mLeft, height, oldWidth, height);

            invalidate(true);
            
            mBackgroundSizeChanged = true;
            invalidateParentIfNeeded();
        }
    }
    
    /**
     * The visual x position of this view, in pixels. This is equivalent to the
     * {@link #setTranslationX(float) translationX} property plus the current
     * {@link #getLeft() left} property.
     *
     * @return The visual x position of this view, in pixels.
     */
    public float getX() {
        return mLeft + getTranslationX();
    }

    /**
     * Sets the visual x position of this view, in pixels. This is equivalent to setting the
     * {@link #setTranslationX(float) translationX} property to be the difference between
     * the x value passed in and the current {@link #getLeft() left} property.
     *
     * @param x The visual x position of this view, in pixels.
     */
    public void setX(float x) {
        setTranslationX(x - mLeft);
    }

    /**
     * The visual y position of this view, in pixels. This is equivalent to the
     * {@link #setTranslationY(float) translationY} property plus the current
     * {@link #getTop() top} property.
     *
     * @return The visual y position of this view, in pixels.
     */
    public float getY() {
        return mTop + getTranslationY();
    }

    /**
     * Sets the visual y position of this view, in pixels. This is equivalent to setting the
     * {@link #setTranslationY(float) translationY} property to be the difference between
     * the y value passed in and the current {@link #getTop() top} property.
     *
     * @param y The visual y position of this view, in pixels.
     */
    public void setY(float y) {
        setTranslationY(y - mTop);
    }
    
    /**
     * The visual z position of this view, in pixels. This is equivalent to the
     * {@link #setTranslationZ(float) translationZ} property plus the current
     * {@link #getElevation() elevation} property.
     *
     * @return The visual z position of this view, in pixels.
     */
    @ViewDebug.ExportedProperty(category = "drawing")
    public float getZ() {
        return /*getElevation() + */getTranslationZ();
    }

    /**
     * Sets the visual z position of this view, in pixels. This is equivalent to setting the
     * {@link #setTranslationZ(float) translationZ} property to be the difference between
     * the x value passed in and the current {@link #getElevation() elevation} property.
     *
     * @param z The visual z position of this view, in pixels.
     */
    public void setZ(float z) {
        setTranslationZ(z/* - getElevation()*/);
    }
    
    /**
     * Used to indicate that the parent of this view should be invalidated. This functionality
     * is used to force the parent to rebuild its display list (when hardware-accelerated),
     * which is necessary when various parent-managed properties of the view change, such as
     * alpha, translationX/Y, scrollX/Y, scaleX/Y, and rotation/X/Y. This method will propagate
     * an invalidation event to the parent.
     *
     * @hide
     */
    protected void invalidateParentIfNeeded() {
        if (mParent != null) {
            ((View) mParent).invalidate(true);
        }
    }
    
    /**
     * The horizontal location of this view relative to its {@link #getLeft() left} position.
     * This position is post-layout, in addition to wherever the object's
     * layout placed it.
     *
     * @return The horizontal position of this view relative to its left position, in pixels.
     */
    public float getTranslationX() {
        return mRenderNode.getTranslationX();
    }
    
    public void setTranslationX(float translationX) {
    	if (mRenderNode.setTranslationX(translationX)) {
    		invalidateViewProperty();
    	}
    }
    
    public void setLayerType(int layerType) {
    	if (mRenderNode.setLayerType(layerType)) {
    		invalidate();
    	}
    }
    
    public int getLayerType() {
    	return mRenderNode.getLayerType();
    }
    
    /**
     * The vertical location of this view relative to its {@link #getTop() top} position.
     * This position is post-layout, in addition to wherever the object's
     * layout placed it.
     *
     * @return The vertical position of this view relative to its top position,
     * in pixels.
     */
    public float getTranslationY() {
        return mRenderNode.getTranslationY();
    }
    
    public void setTranslationY(float translationY) {
    	if (mRenderNode.setTranslationY(translationY)) {
    		invalidateViewProperty();
    	}
    }
    
    /**
     * The depth location of this view relative to its {@link #getElevation() elevation}.
     *
     * @return The depth of this view relative to its elevation.
     */
    public float getTranslationZ() {
        return mRenderNode.getTranslationZ();
    }
    
    public void setTranslationZ(float translationZ) {
    	if (mRenderNode.setTranslationZ(translationZ)) {
    		invalidateViewProperty();
    	}
    }
    
    /**
     * The amount that the view is scaled in x around the pivot point, as a proportion of
     * the view's unscaled width. A value of 1, the default, means that no scaling is applied.
     *
     * <p>By default, this is 1.0f.
     *
     * @see #getPivotX()
     * @see #getPivotY()
     * @return The scaling factor.
     */
    public float getScaleX() {
        return mRenderNode.getScaleX();
    }
    
    public void setScaleX(float scaleX) {
    	if (mRenderNode.setScaleX(scaleX)) {
    		invalidateViewProperty();
    	}
    }
    
    /**
     * The amount that the view is scaled in y around the pivot point, as a proportion of
     * the view's unscaled height. A value of 1, the default, means that no scaling is applied.
     *
     * <p>By default, this is 1.0f.
     *
     * @see #getPivotX()
     * @see #getPivotY()
     * @return The scaling factor.
     */
    public float getScaleY() {
        return mRenderNode.getScaleY();
    }
    
    public void setScaleY(float scaleY) {
    	if (mRenderNode.setScaleY(scaleY)) {
    		invalidateViewProperty();
    	}
    }
    
    public void setScale(float scale) {
    	setScaleX(scale);
    	setScaleY(scale);
    }
    
    /**
     * The degrees that the view is rotated around the pivot point.
     *
     * @see #setRotation(float)
     * @see #getPivotX()
     * @see #getPivotY()
     *
     * @return The degrees of rotation.
     */
    public float getRotation() {
        return mRenderNode.getRotation();
    }
    
    public void setRotation(float rotation) {
    	if (mRenderNode.setRotation(rotation)) {
    		invalidateViewProperty();
    	}
    }
    
    /**
     * The degrees that the view is rotated around the vertical axis through the pivot point.
     *
     * @see #getPivotX()
     * @see #getPivotY()
     * @see #setRotationY(float)
     *
     * @return The degrees of Y rotation.
     */
    public float getRotationY() {
        return mRenderNode.getRotationY();
    }
    
    public void setRotationY(float rotationY) {
    	if (mRenderNode.setRotationY(rotationY)) {
    		invalidateViewProperty();
    	}
    }
    

    /**
     * The degrees that the view is rotated around the horizontal axis through the pivot point.
     *
     * @see #getPivotX()
     * @see #getPivotY()
     * @see #setRotationX(float)
     *
     * @return The degrees of X rotation.
     */
    public float getRotationX() {
        return mRenderNode.getRotationX();
    }
    
    public void setRotationX(float rotationX) {
    	if (mRenderNode.setRotationX(rotationX)) {
    		invalidateViewProperty();
    	}
    }
    
    /**
     * Returns the current StateListAnimator if exists.
     *
     * @return StateListAnimator or null if it does not exists
     * @see    #setStateListAnimator(android.animation.StateListAnimator)
     */
    public StateListAnimator getStateListAnimator() {
        return mStateListAnimator;
    }

    /**
     * Attaches the provided StateListAnimator to this View.
     * <p>
     * Any previously attached StateListAnimator will be detached.
     *
     * @param stateListAnimator The StateListAnimator to update the view
     * @see {@link android.animation.StateListAnimator}
     */
    public void setStateListAnimator(StateListAnimator stateListAnimator) {
        if (mStateListAnimator == stateListAnimator) {
            return;
        }
        if (mStateListAnimator != null) {
            mStateListAnimator.setTarget(null);
        }
        mStateListAnimator = stateListAnimator;
        if (stateListAnimator != null) {
            stateListAnimator.setTarget(this);
            if (isAttachedToWindow()) {
                stateListAnimator.setState(getDrawableState());
            }
        }
    }
    
    /**
     * Setting a solid background color for the drawing cache's bitmaps will improve
     * performance and memory usage. Note, though that this should only be used if this
     * view will always be drawn on top of a solid color.
     *
     * @param color The background color to use for the drawing cache's bitmap
     *
     * @see #setDrawingCacheEnabled(boolean)
     * @see #buildDrawingCache()
     * @see #getDrawingCache()
     */
    public void setDrawingCacheBackgroundColor(int color) {
        if (color != mDrawingCacheBackgroundColor) {
            mDrawingCacheBackgroundColor = color;
            mPrivateFlags &= ~PFLAG_DRAWING_CACHE_VALID;
        }
    }

    /**
     * @see #setDrawingCacheBackgroundColor(int)
     *
     * @return The background color to used for the drawing cache's bitmap
     */
    public int getDrawingCacheBackgroundColor() {
        return mDrawingCacheBackgroundColor;
    }
    
    /**
     * Override this if your view is known to always be drawn on top of a solid color background,
     * and needs to draw fading edges. Returning a non-zero color enables the view system to
     * optimize the drawing of the fading edges. If you do return a non-zero color, the alpha
     * should be set to 0xFF.
     *
     * @see #setVerticalFadingEdgeEnabled(boolean)
     * @see #setHorizontalFadingEdgeEnabled(boolean)
     *
     * @return The known solid color background for this view, or 0 if the color may vary
     */
    public int getSolidColor() {
        return 0;
    }
    
    public RenderNode getDisplayList() {
		updateDisplayListIfDirty();
		return mRenderNode;
	}
    
    private void resetDisplayList() {
        if (mRenderNode.isValid()) {
            mRenderNode.destroy();
        }
    }
    
    // FIXME
    private GLRootView mViewRootImpl;
    /**
     * <p>Calling this method is equivalent to calling <code>getDrawingCache(false)</code>.</p>
     *
     * @return A non-scaled bitmap representing this view or null if cache is disabled.
     *
     * @see #getDrawingCache(boolean)
     */
    public Bitmap getDrawingCache() {
    	if (mViewRootImpl != null && getWidth() > 0 && getHeight() > 0) {
    		return mViewRootImpl.buildDrawingCache(this);
    	}
        return null;
    }
	
	private void updateDisplayListIfDirty() {
		final RenderNode renderNode = mRenderNode;
		if ((mPrivateFlags & PFLAG_DRAWING_CACHE_VALID) == 0 
				|| !renderNode.isValid()
				|| mRecreateDisplayList) {
			// Don't need to recreate the display list, just need to tell our
            // children to restore/recreate theirs
            if (renderNode.isValid()
                    && !mRecreateDisplayList) {
                mPrivateFlags |= PFLAG_DRAWING_CACHE_VALID;
                dispatchGetDisplayList();

                return; // no work needed
            }
			GLCanvas canvas = renderNode.start(getWidth(), getHeight());
			computeScroll();
			
			canvas.translate(- mScrollX, - mScrollY);
			pdraw(canvas);
			canvas.translate(mScrollX, mScrollY);
			
			renderNode.end(canvas);
			mRecreateDisplayList = false;
		}
	}
	
	void dispatchRender(GLCanvas canvas) {
    	canvas.drawRenderNode(updateViewDisplayList());
    }
	
	/**
     * This method is used by ViewGroup to cause its children to restore or recreate their
     * display lists. It is called by getDisplayList() when the parent ViewGroup does not need
     * to recreate its own display list, which would happen if it went through the normal
     * draw/dispatchDraw mechanisms.
     *
     * @hide
     */
    protected void dispatchGetDisplayList() {}
	
	public RenderNode updateViewDisplayList() {
        mRecreateDisplayList = (mPrivateFlags & PFLAG_INVALIDATED)
                == PFLAG_INVALIDATED;
        mPrivateFlags &= ~PFLAG_INVALIDATED;
        getDisplayList();
        mRecreateDisplayList = false;
        return mRenderNode;
	}
	
	/**
	 * This is a view property invalidate.
	 * Only call a frame render, the render node will apply the transformation.
	 */
	void invalidateViewProperty() {
		if (mAttachInfo != null) {
			mAttachInfo.mViewRootImpl.requestRender();
		}
		damageParentCache();
	}
	
	void damageParentCache() {
		if (mParent != null) {
			if (mParent.getLayerType() == LAYER_TYPE_HARDWARE) {
				mParent.invalidate();
			}
			mParent.damageParentCache();
		}
	}
	
	/**
     * Mark the area defined by dirty as needing to be drawn. If the view is
     * visible, {@link #onDraw(android.graphics.Canvas)} will be called at some
     * point in the future.
     * <p>
     * This must be called from a UI thread. To call from a non-UI thread, call
     * {@link #postInvalidate()}.
     * <p>
     * <b>WARNING:</b> In API 19 and below, this method may be destructive to
     * {@code dirty}.
     *
     * @param dirty the rectangle representing the bounds of the dirty region
     */
    public void invalidate(Rect dirty) {
        final int scrollX = mScrollX;
        final int scrollY = mScrollY;
        invalidateInternal(dirty.left - scrollX, dirty.top - scrollY,
                dirty.right - scrollX, dirty.bottom - scrollY, true, false);
    }
    
    /**
     * Mark the area defined by the rect (l,t,r,b) as needing to be drawn. The
     * coordinates of the dirty rect are relative to the view. If the view is
     * visible, {@link #onDraw(android.graphics.Canvas)} will be called at some
     * point in the future.
     * <p>
     * This must be called from a UI thread. To call from a non-UI thread, call
     * {@link #postInvalidate()}.
     *
     * @param l the left position of the dirty region
     * @param t the top position of the dirty region
     * @param r the right position of the dirty region
     * @param b the bottom position of the dirty region
     */
    public void invalidate(int l, int t, int r, int b) {
        final int scrollX = mScrollX;
        final int scrollY = mScrollY;
        invalidateInternal(l - scrollX, t - scrollY, r - scrollX, b - scrollY, true, false);
    }
	
	public void invalidate() {
		invalidate(true);
    }
	
	Runnable mInvalidateRunnable = new Runnable() {
		@Override
		public void run() {
			invalidate();
		}
	};
	
	public void postInvalidate() {
		removeCallbacks(mInvalidateRunnable);
		post(mInvalidateRunnable);
	}
	
	public void postInvalidateOnAnimation() {
		postInvalidate();
	}
	
	/**
     * This is where the invalidate() work actually happens. A full invalidate()
     * causes the drawing cache to be invalidated, but this function can be
     * called with invalidateCache set to false to skip that invalidation step
     * for cases that do not need it (for example, a component that remains at
     * the same dimensions with the same content).
     *
     * @param invalidateCache Whether the drawing cache for this view should be
     *            invalidated as well. This is usually true for a full
     *            invalidate, but may be set to false if the View's contents or
     *            dimensions have not changed.
     */
    void invalidate(boolean invalidateCache) {
        invalidateInternal(0, 0, mRight - mLeft, mBottom - mTop, invalidateCache, true);
    }

    void invalidateInternal(int l, int t, int r, int b, boolean invalidateCache,
            boolean fullInvalidate) {
    	if (mAttachInfo == null) return;
    	if ((mPrivateFlags & PFLAG_INVALIDATED)
                == PFLAG_INVALIDATED) {
    		return;
		}
        if (invalidateCache) {
            mPrivateFlags |= PFLAG_INVALIDATED;
            mPrivateFlags &= ~PFLAG_DRAWING_CACHE_VALID;
        }
        if (mParent != null) {
        	mParent.invalidateChild(this, l, t, r, b);
        } else if (mAttachInfo != null) {
        	mAttachInfo.mViewRootImpl.requestRender();
    	}
    }
    
    /**
     * @return A handler associated with the thread running the View. This
     * handler can be used to pump events in the UI events queue.
     */
    public Handler getHandler() {
        final AttachInfo attachInfo = mAttachInfo;
        if (attachInfo != null) {
            return attachInfo.mHandler;
        }
        return null;
    }
    
    /**
     * <p>Causes the Runnable to be added to the message queue.
     * The runnable will be run on the user interface thread.</p>
     *
     * @param action The Runnable that will be executed.
     *
     * @return Returns true if the Runnable was successfully placed in to the
     *         message queue.  Returns false on failure, usually because the
     *         looper processing the message queue is exiting.
     *
     * @see #postDelayed
     * @see #removeCallbacks
     */
    public boolean post(Runnable action) {
        final AttachInfo attachInfo = mAttachInfo;
        if (attachInfo != null) {
            return attachInfo.mHandler.post(action);
        }
        return false;
    }
    
    public boolean postToAndroid(Runnable action) {
    	final AttachInfo attachInfo = mAttachInfo;
        if (attachInfo != null) {
            return attachInfo.mAndroidHandler.post(action);
        }
        return false;
    }
    
    public boolean removeCallbacksFromAndroid(Runnable action) {
    	final AttachInfo attachInfo = mAttachInfo;
        if (attachInfo != null) {
            attachInfo.mAndroidHandler.removeCallbacks(action);
            return true;
        }
        return false;
    }

    /**
     * <p>Causes the Runnable to be added to the message queue, to be run
     * after the specified amount of time elapses.
     * The runnable will be run on the user interface thread.</p>
     *
     * @param action The Runnable that will be executed.
     * @param delayMillis The delay (in milliseconds) until the Runnable
     *        will be executed.
     *
     * @return true if the Runnable was successfully placed in to the
     *         message queue.  Returns false on failure, usually because the
     *         looper processing the message queue is exiting.  Note that a
     *         result of true does not mean the Runnable will be processed --
     *         if the looper is quit before the delivery time of the message
     *         occurs then the message will be dropped.
     *
     * @see #post
     * @see #removeCallbacks
     */
    public boolean postDelayed(Runnable action, long delayMillis) {
        final AttachInfo attachInfo = mAttachInfo;
        if (attachInfo != null) {
            return attachInfo.mHandler.postDelayed(action, delayMillis);
        }
        return false;
    }

    /**
     * <p>Causes the Runnable to execute on the next animation time step.
     * The runnable will be run on the user interface thread.</p>
     *
     * @param action The Runnable that will be executed.
     *
     * @see #postOnAnimationDelayed
     * @see #removeCallbacks
     */
    public void postOnAnimation(Runnable action) {
    	post(action);
    }

    /**
     * <p>Causes the Runnable to execute on the next animation time step,
     * after the specified amount of time elapses.
     * The runnable will be run on the user interface thread.</p>
     *
     * @param action The Runnable that will be executed.
     * @param delayMillis The delay (in milliseconds) until the Runnable
     *        will be executed.
     *
     * @see #postOnAnimation
     * @see #removeCallbacks
     */
    public void postOnAnimationDelayed(Runnable action, long delayMillis) {
        postDelayed(action, delayMillis);
    }

    /**
     * <p>Removes the specified Runnable from the message queue.</p>
     *
     * @param action The Runnable to remove from the message handling queue
     *
     * @return true if this view could ask the Handler to remove the Runnable,
     *         false otherwise. When the returned value is true, the Runnable
     *         may or may not have been actually removed from the message queue
     *         (for instance, if the Runnable was not in the queue already.)
     *
     * @see #post
     * @see #postDelayed
     * @see #postOnAnimation
     * @see #postOnAnimationDelayed
     */
    public boolean removeCallbacks(Runnable action) {
        if (action != null) {
            final AttachInfo attachInfo = mAttachInfo;
            if (attachInfo != null) {
                attachInfo.mHandler.removeCallbacks(action);
            }
        }
        return true;
    }
    
    //
    // Properties
    //
    /**
     * A Property wrapper around the <code>alpha</code> functionality handled by the
     * {@link View#setAlpha(float)} and {@link View#getAlpha()} methods.
     */
    public static final Property<View, Float> ALPHA = new FloatProperty<View>("alpha") {
        @Override
        public void setValue(View object, float value) {
            object.setAlpha(value);
        }

        @Override
        public Float get(View object) {
            return object.getAlpha();
        }
    };

    /**
     * A Property wrapper around the <code>translationX</code> functionality handled by the
     * {@link View#setTranslationX(float)} and {@link View#getTranslationX()} methods.
     */
    public static final Property<View, Float> TRANSLATION_X = new FloatProperty<View>("translationX") {
        @Override
        public void setValue(View object, float value) {
            object.setTranslationX(value);
        }

                @Override
        public Float get(View object) {
            return object.getTranslationX();
        }
    };

    /**
     * A Property wrapper around the <code>translationY</code> functionality handled by the
     * {@link View#setTranslationY(float)} and {@link View#getTranslationY()} methods.
     */
    public static final Property<View, Float> TRANSLATION_Y = new FloatProperty<View>("translationY") {
        @Override
        public void setValue(View object, float value) {
            object.setTranslationY(value);
        }

        @Override
        public Float get(View object) {
            return object.getTranslationY();
        }
    };

    /**
     * A Property wrapper around the <code>translationZ</code> functionality handled by the
     * {@link View#setTranslationZ(float)} and {@link View#getTranslationZ()} methods.
     */
    public static final Property<View, Float> TRANSLATION_Z = new FloatProperty<View>("translationZ") {
        @Override
        public void setValue(View object, float value) {
            object.setTranslationZ(value);
        }

        @Override
        public Float get(View object) {
            return object.getTranslationZ();
        }
    };
    
    /**
     * A Property wrapper around the <code>x</code> functionality handled by the
     * {@link View#setX(float)} and {@link View#getX()} methods.
     */
    public static final Property<View, Float> X = new FloatProperty<View>("x") {
        @Override
        public void setValue(View object, float value) {
            object.setX(value);
        }

        @Override
        public Float get(View object) {
            return object.getX();
        }
    };

    /**
     * A Property wrapper around the <code>y</code> functionality handled by the
     * {@link View#setY(float)} and {@link View#getY()} methods.
     */
    public static final Property<View, Float> Y = new FloatProperty<View>("y") {
        @Override
        public void setValue(View object, float value) {
            object.setY(value);
        }

        @Override
        public Float get(View object) {
            return object.getY();
        }
    };

    /**
     * A Property wrapper around the <code>z</code> functionality handled by the
     * {@link View#setZ(float)} and {@link View#getZ()} methods.
     */
    public static final Property<View, Float> Z = new FloatProperty<View>("z") {
        @Override
        public void setValue(View object, float value) {
            object.setZ(value);
        }

        @Override
        public Float get(View object) {
            return object.getZ();
        }
    };

    /**
     * A Property wrapper around the <code>rotation</code> functionality handled by the
     * {@link View#setRotation(float)} and {@link View#getRotation()} methods.
     */
    public static final Property<View, Float> ROTATION = new FloatProperty<View>("rotation") {
        @Override
        public void setValue(View object, float value) {
            object.setRotation(value);
        }

        @Override
        public Float get(View object) {
            return object.getRotation();
        }
    };

    /**
     * A Property wrapper around the <code>rotationX</code> functionality handled by the
     * {@link View#setRotationX(float)} and {@link View#getRotationX()} methods.
     */
    public static final Property<View, Float> ROTATION_X = new FloatProperty<View>("rotationX") {
        @Override
        public void setValue(View object, float value) {
            object.setRotationX(value);
        }

        @Override
        public Float get(View object) {
            return object.getRotationX();
        }
    };

    /**
     * A Property wrapper around the <code>rotationY</code> functionality handled by the
     * {@link View#setRotationY(float)} and {@link View#getRotationY()} methods.
     */
    public static final Property<View, Float> ROTATION_Y = new FloatProperty<View>("rotationY") {
        @Override
        public void setValue(View object, float value) {
            object.setRotationY(value);
        }

        @Override
        public Float get(View object) {
            return object.getRotationY();
        }
    };

    /**
     * A Property wrapper around the <code>scaleX</code> functionality handled by the
     * {@link View#setScaleX(float)} and {@link View#getScaleX()} methods.
     */
    public static final Property<View, Float> SCALE_X = new FloatProperty<View>("scaleX") {
        @Override
        public void setValue(View object, float value) {
            object.setScaleX(value);
        }

        @Override
        public Float get(View object) {
            return object.getScaleX();
        }
    };

    /**
     * A Property wrapper around the <code>scaleY</code> functionality handled by the
     * {@link View#setScaleY(float)} and {@link View#getScaleY()} methods.
     */
    public static final Property<View, Float> SCALE_Y = new FloatProperty<View>("scaleY") {
        @Override
        public void setValue(View object, float value) {
            object.setScaleY(value);
        }

        @Override
        public Float get(View object) {
            return object.getScaleY();
        }
    };
    
    private class MatchIdPredicate implements Predicate<View> {
        public int mId;

        @Override
        public boolean apply(View view) {
            return (view.mID == mId);
        }
    }
    
}
