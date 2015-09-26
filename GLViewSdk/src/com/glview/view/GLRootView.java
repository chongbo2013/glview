package com.glview.view;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Rect;
import android.media.AudioManager;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.util.LogPrinter;
import android.util.Printer;
import android.view.InputEvent;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.glview.animation.Animator;
import com.glview.graphics.Bitmap;
import com.glview.hwui.task.Task;
import com.glview.hwui.task.TaskHandler;
import com.glview.thread.Handler;
import com.glview.thread.Looper;
import com.glview.thread.Message;
import com.glview.view.View.AttachInfo;
import com.glview.view.ViewGroup.LayoutParams;
import com.glview.view.animation.AnimationUtils;

/**
 * @hide
 * @author lijing.lj
 */
final public class GLRootView extends SurfaceView
	implements SurfaceHolder.Callback, SurfaceHolder.Callback2, android.view.ViewTreeObserver.OnTouchModeChangeListener {
	
	protected final static String TAG = "GLRootView";
	final static boolean DEBUG = true;
	final static boolean DEBUG_GL_MESSAGE = false;
	final static boolean DEBUG_LOOPER = false;
	
    private static final int FLAG_NEED_LAYOUT = 0x00000002;
    
    public static final int GL_ROOT_VIEW_ID = 10190511;
    
    /**
     * The renderer only renders
     * when the surface is created, or when {@link #requestRender} is called.
     *
     * @see #getRenderMode()
     * @see #setRenderMode(int)
     * @see #requestRender()
     */
    public final static int RENDERMODE_WHEN_DIRTY = 0;
    /**
     * The renderer is called
     * continuously to re-render the scene.
     *
     * @see #getRenderMode()
     * @see #setRenderMode(int)
     */
    public final static int RENDERMODE_CONTINUOUSLY = 1;
    
	Printer pw = new LogPrinter(Log.DEBUG, TAG);
	
	GLRenderer mRenderer;
	
	protected GLHandler mGLHandler = new GLHandler();
	
	Thread mThread;
	
	boolean mRenderRequested = false;
	boolean mRenderPrepared = false;
	
	boolean mAttatched = false;
	boolean mFirst;
	
	// This is the top-level view of the window, containing the window decor.
    private ViewGroup mDecor;
    
    private int mRenderMode = RENDERMODE_WHEN_DIRTY;
    
	private int mFlags = FLAG_NEED_LAYOUT;
	
	Handler mHandler = new Handler(Looper.getMainLooper());
	
	android.os.Handler mAndroidHandler = new android.os.Handler(android.os.Looper.getMainLooper());
	
	AttachInfo mAttachInfo;
	WindowId mWindowId = new WindowId();
	
	List<Callback> mCallbacks = new ArrayList<Callback>();
	
	/**
     * see {@link PlaySound#playSoundEffect(int)}
     */
    AudioManager mAudioManager;

	public GLRootView(Context context) {
		super(context);
		init();
	}

	public GLRootView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	private void init() {
		mFirst = true;
		mAttachInfo = new AttachInfo(this, mWindowId, mHandler, mAndroidHandler, new RootCallbacks());
		mThread = mHandler.getLooper().getThread();
		
		getHolder().addCallback(this);
		setId(GL_ROOT_VIEW_ID);
		setFocusable(true);
		setFocusableInTouchMode(true);
	}
	
	public void setDebugEnable(boolean enable) {
		if (enable) {
			mRenderMode = RENDERMODE_CONTINUOUSLY;
		} else {
			mRenderMode = RENDERMODE_WHEN_DIRTY;
		}
	}
	
	GLRenderer getRenderer() {
		if (mRenderer == null) {
			mRenderer = GLRenderer.createRender();
		}
		return mRenderer;
	}

	@Override
	public void surfaceRedrawNeeded(SurfaceHolder holder) {
	}

	/**
     * This method is part of the SurfaceHolder.Callback interface, we get
     * a surface to draw our UI.
     */
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		if (DEBUG) Log.d(TAG, "surfaceCreated called.");
		mGLHandler.sendSyncMessage(GLHandler.MSG_SURFACE_CREATED, 0, 0, holder);
	}

	/**
     * This method is part of the SurfaceHolder.Callback interface, now we
     * are identified of the size of the surface, so we can set out viewport.
     */
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		if (DEBUG) Log.d(TAG, String.format("surfaceChanged called; format=%s, width=%s, height=%s.", format, width, height));
		mGLHandler.sendSyncMessage(GLHandler.MSG_SURFACE_CHANGED, width, height, holder);
	}
	
	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		if (DEBUG) Log.d(TAG, String.format("onLayout called; left=%s, top=%s, right=%s, bottom=%s.", left, top, right, bottom));
		if (changed) mGLHandler.sendSyncMessage(GLHandler.MSG_REQUEST_LAYOUT, 0, 0, null);
	}

	/**
     * This method is part of the SurfaceHolder.Callback interface, we lost
     * the surface, it has been destroyed, we can not do any drawing.
     * Maybe it's just because our window is invisible right now, we should
     * hold the resources which can be recycled in method {@link #onDetachedFromWindow()}
     */
	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (DEBUG) Log.d(TAG, "surfaceDestroyed called.");
		mGLHandler.sendSyncMessage(GLHandler.MSG_SURFACE_DESTROYED, 0, 0, null);
	}
	
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		mAttatched = true;
		if (DEBUG) Log.d(TAG, "onAttachedToWindow called.");
		getViewTreeObserver().addOnTouchModeChangeListener(this);
		mGLHandler.sendSyncMessage(GLHandler.MSG_ATTACHED_TO_WINDOW, 0, 0, null);
	}
	
	/**
	 * The SurfaceView is detached, we consider that it's out of its lifecircle,
	 * so it's time to recycle unused resources..
	 */
	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		mAttatched = false;
		if (DEBUG) Log.d(TAG, "onDetachedFromWindow called.");
		getViewTreeObserver().removeOnTouchModeChangeListener(this);
		mGLHandler.sendSyncMessage(GLHandler.MSG_DETACHED_FROM_WINDOW, 0, 0, null);
	}
	
	public void setContentView(View view) {
		if (DEBUG) Log.d(TAG, "setContentView called. view=" + view);
		mGLHandler.sendSyncMessage(GLHandler.MSG_SET_CONTENT_VIEW, 0, 0, view);
	}
	
	public void setContentView(int resId) {
		if (DEBUG) Log.d(TAG, "setContentView called. resId=" + resId);
		mGLHandler.sendSyncMessage(GLHandler.MSG_SET_CONTENT_VIEW, resId, 0, null);
	}
	
	void attachContentView(int resId) {
		if (resId > 0) {
			if (mDecor == null) {
				installDecor();
			}
			View v = LayoutInflater.from(getContext()).inflate(resId, mDecor, false);
			if (v != null) {
				attachContentView(v);
			}
		}
	}
	
	void attachContentView(View view) {
		if (mDecor == null) {
			installDecor();
		} else {
			mDecor.removeAllViews();
		}
		mDecor.addView(view);
		mAttachInfo.mRootView = view;
		dispatchAttach();
	}
	
	void dispatchAttach() {
		for (Callback callback : mCallbacks) {
			callback.onAttached(mDecor);
		}
	}
	
	private void installDecor() {
        if (mDecor == null) {
            mDecor = new DecorView(getContext());
            mDecor.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
            mDecor.setIsRootNamespace(true);
            mAttachInfo.mRootView = mDecor;
            if (mAttatched) {
    			attachToRoot();
    		}
        }
	}
	
	public void addCallback(Callback callback) {
		if (!mCallbacks.contains(callback)) {
			mCallbacks.add(callback);
		}
	}
	
	public void removeCallback(Callback callback) {
		mCallbacks.remove(callback);
	}
	
	void detachFromRoot() {
		if (mDecor != null && mDecor.isAttachedToWindow()) {
			mDecor.dispatchDetachedFromWindow();
		}
	}
	
	void attachToRoot() {
		if (mDecor != null && !mDecor.isAttachedToWindow()) {
			mDecor.dispatchAttachedToWindow(mAttachInfo, 0);
			requestLayoutGLContentView();
		}
	}
	
	public void requestRender() {
		checkThread();
		scheduleRender();
	}
	
	void checkThread() {
        if (mThread != Thread.currentThread()) {
            throw new CalledFromWrongThreadException(
                    "Only the original thread that created a view hierarchy can touch its views.");
        }
    }
	
	/**
	 * @hide
	 * Start  some RenderThread-driven animations, these animations runs in RenderThread.
	 * Only called from ViewPropertyAnimatorRT.
	 * @see ViewPropertyAnimatorRT#startAnimation(ViewPropertyAnimator)
	 * 
	 * @param animationStarter
	 */
	void startRTAnimation(List<Animator> animators) {
		getRenderer().startRTAnimation(animators);
	}
	
	/**
	 * @hide
	 * Stop some RenderThread-driven animations.
	 * @param animators
	 */
	void stopRTAnimation(List<Animator> animators) {
		getRenderer().stopRTAnimation(animators);
	}
	
	Bitmap buildDrawingCache(View v) {
		return getRenderer().buildDrawingCache(v);
	}
	
	void scheduleRender() {
		mRenderRequested = true;
		if (getRenderer().isEnable() && !mRenderPrepared) {
			mRenderRequested = false;
			mRenderPrepared = true;
			mGLHandler.post(mRenderRunnable);
		}
	}
	
	void unscheduleRender() {
		mRenderRequested = false;
		mRenderPrepared = false;
		mGLHandler.remove(mRenderRunnable);
	}
	
	Task mRenderRunnable = new Task() {
		@Override
		public void doTask() {
			mRenderPrepared = false;
			if (getRenderer().isEnable()) {
				onDrawFrame();
			}
		}
	};
	
	protected void onDrawFrame() {
		checkThread();
		
		collectViewAttributes();
		mAttachInfo.mDrawingTime = AnimationUtils.currentAnimationTimeMillis();
		// if need layout
		if ( (mFlags & FLAG_NEED_LAYOUT) == FLAG_NEED_LAYOUT ) {
            layoutContentPane();
            mAttachInfo.mTreeObserver.dispatchOnGlobalLayout();
        }
		
		if (mFirst) {
			boolean isInTouchMode = isInTouchMode();
			mAttachInfo.mInTouchMode = !isInTouchMode;
			ensureTouchMode(isInTouchMode);
		}
		
		boolean cancelDraw = mAttachInfo.mTreeObserver.dispatchOnPreDraw();
		if (!cancelDraw) {
			if (mAttachInfo.mViewScrollChanged) {
	            mAttachInfo.mViewScrollChanged = false;
	            mAttachInfo.mTreeObserver.dispatchOnScrollChanged();
	        }
			mAttachInfo.mTreeObserver.dispatchOnDraw();
			if (mDecor != null) {
				getRenderer().draw(mDecor);
			}
		} else {
			// Try again
			scheduleRender();
		}
		
		if (mRenderMode == RENDERMODE_CONTINUOUSLY) {
			requestRender();
		}
		
		mFirst = false;
	}
	
	private boolean collectViewAttributes() {
        if (mAttachInfo.mRecomputeGlobalAttributes) {
        	mAttachInfo.mRecomputeGlobalAttributes = false;
        	mAttachInfo.mRecomputeGlobalAttributes = false;
            boolean oldScreenOn = mAttachInfo.mKeepScreenOn;
            mAttachInfo.mKeepScreenOn = false;
            mAttachInfo.mSystemUiVisibility = 0;
            mAttachInfo.mHasSystemUiListeners = false;
            mDecor.dispatchCollectViewAttributes(mAttachInfo, 0);
            mAttachInfo.mSystemUiVisibility &= ~mAttachInfo.mDisabledSystemUiVisibility;
            if (mAttachInfo.mKeepScreenOn != oldScreenOn
                    || mAttachInfo.mSystemUiVisibility != getSystemUiVisibility()) {
            	removeCallbacks(mViewAttributesRunnable);
            	post(mViewAttributesRunnable);
            	mDecor.dispatchSystemUiVisibilityChanged(mAttachInfo.mSystemUiVisibility);
            }
        }
        return false;
    }
	
	Runnable mViewAttributesRunnable = new Runnable() {
		@Override
		public void run() {
			setKeepScreenOn(mAttachInfo.mKeepScreenOn);
			setSystemUiVisibility(mAttachInfo.mSystemUiVisibility);
		}
	};
	
	/**
	 * Recycle all the resources.
	 * Called by method {@link #onDetachedFromWindow()}.
	 */
	void destroy() {
		getRenderer().destroy(true);
	}

	public void requestLayoutGLContentView() {
		checkThread();
		if (mDecor == null) return;
        // "View" system will invoke onLayout() for initialization(bug ?), we
        // have to ignore it since the GLThread is not ready yet.
        mFlags |= FLAG_NEED_LAYOUT;
        requestRender();
	}
	
	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		if (!isEnabled()) return false;
		if (mDecor != null) {
			MotionEvent me = MotionEvent.obtain(event);
			me.offsetLocation(getLeft(), getTop());
			mInputEventTask.mInputEvent = me;
			mGLHandler.postAndWait(mInputEventTask);
			me.recycle();
			return mInputEventTask.mResult;
		}
		return super.dispatchTouchEvent(event);
	}
	
	InputEventTask mInputEventTask = new InputEventTask();
	
	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (!isEnabled()) return false;
		if (mDecor != null) {
			mInputEventTask.mInputEvent = event;
			mGLHandler.postAndWait(mInputEventTask);
			return mInputEventTask.mResult;
		}
		return super.dispatchKeyEvent(event);
	}
	
	@Override
	protected void onFocusChanged(final boolean gainFocus, final int direction,
			Rect previouslyFocusedRect) {
		super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
	}
	
	private void layoutContentPane() {
        mFlags &= ~FLAG_NEED_LAYOUT;

        int w = getWidth();
        int h = getHeight();
        
        if (mDecor != null 
        		&& mDecor.getVisibility() != View.GONE 
        		&& w != 0 && h != 0) {
        	
        	int rootWidthSpec;
        	int rootHeightSpec;
        	LayoutParams lp = mDecor.getLayoutParams();
        	if (lp != null) {
        		if (lp.width == LayoutParams.FILL_PARENT || lp.width == LayoutParams.MATCH_PARENT) {
        			rootWidthSpec = MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY);
        		} else if (lp.width == LayoutParams.WRAP_CONTENT) {
        			rootWidthSpec = MeasureSpec.makeMeasureSpec(w, MeasureSpec.AT_MOST);
        		} else {
        			rootWidthSpec = MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY);
        		}
        		if (lp.height == LayoutParams.FILL_PARENT || lp.height == LayoutParams.MATCH_PARENT) {
        			rootHeightSpec = MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY);
        		} else if (lp.height == LayoutParams.WRAP_CONTENT) {
        			rootHeightSpec = MeasureSpec.makeMeasureSpec(h, MeasureSpec.AT_MOST);
        		} else {
        			rootHeightSpec = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
        		}
        	} else {
        		rootWidthSpec = MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY);
        		rootHeightSpec = MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY);
        	}
        	mDecor.measure(rootWidthSpec, rootHeightSpec);
        	mDecor.layout(0, 0, mDecor.getMeasuredWidth(), getMeasuredHeight());
        }
    } 
	
	public View getContentPane() {
		return mDecor;
	}
	
	public void recomputeViewAttributes(View child) {
		checkThread();
        if (mDecor == child) {
            mAttachInfo.mRecomputeGlobalAttributes = true;
            scheduleRender();
        }
	}
	
	class InputEventTask extends Task {
		InputEvent mInputEvent;
		boolean mResult;
		@Override
		public void doTask() {
			if (mDecor != null && mInputEvent != null) {
				if (mInputEvent instanceof KeyEvent) {
					mResult = mDecor.dispatchKeyEvent((KeyEvent) mInputEvent);
				} else if (mInputEvent instanceof MotionEvent) {
					mResult = mDecor.dispatchTouchEvent((MotionEvent) mInputEvent);
				} else {
					mResult = false;
				}
			} else {
				mResult = false;
			}
			mInputEvent = null;
		}
	}
	
	public static final class CalledFromWrongThreadException extends RuntimeException {
		private static final long serialVersionUID = 5556515200079720110L;
		public CalledFromWrongThreadException(String msg) {
            super(msg);
        }
    }
	
	private AudioManager getAudioManager() {
        if (mAudioManager == null) {
            mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
        }
        return mAudioManager;
    }
	
	private class RootCallbacks implements AttachInfo.Callbacks {
		@Override
		public void playSoundEffect(int effectId) {
			 try {
		            final AudioManager audioManager = getAudioManager();
		            switch (effectId) {
		                case SoundEffectConstants.CLICK:
		                    audioManager.playSoundEffect(AudioManager.FX_KEY_CLICK);
		                    return;
		                case SoundEffectConstants.NAVIGATION_DOWN:
		                    audioManager.playSoundEffect(AudioManager.FX_FOCUS_NAVIGATION_DOWN);
		                    return;
		                case SoundEffectConstants.NAVIGATION_LEFT:
		                    audioManager.playSoundEffect(AudioManager.FX_FOCUS_NAVIGATION_LEFT);
		                    return;
		                case SoundEffectConstants.NAVIGATION_RIGHT:
		                    audioManager.playSoundEffect(AudioManager.FX_FOCUS_NAVIGATION_RIGHT);
		                    return;
		                case SoundEffectConstants.NAVIGATION_UP:
		                    audioManager.playSoundEffect(AudioManager.FX_FOCUS_NAVIGATION_UP);
		                    return;
		                default:
		                    throw new IllegalArgumentException("unknown effect id " + effectId +
		                            " not defined in " + SoundEffectConstants.class.getCanonicalName());
		            }
		        } catch (IllegalStateException e) {
		            // Exception thrown by getAudioManager() when mView is null
		            Log.e(TAG, "FATAL EXCEPTION when attempting to play sound effect: " + e);
		            e.printStackTrace();
		        }
	    }

		@Override
		public boolean performHapticFeedback(int effectId, boolean always) {
			return GLRootView.this.performHapticFeedback(effectId);
		}
	}

	// It's now depend on android handler.
	// We consider to striper the handler from android later.
	protected class GLHandler extends TaskHandler {
		final static int MSG_SURFACE_CREATED = 1;
		final static int MSG_SURFACE_CHANGED = 2;
		final static int MSG_SURFACE_DESTROYED = 3;
		
		final static int MSG_DETACHED_FROM_WINDOW = 4;
		final static int MSG_ATTACHED_TO_WINDOW = 5;
		
		final static int MSG_REQUEST_LAYOUT = 6;
		
		final static int MSG_SET_CONTENT_VIEW = 7;
		
		SyncMessageTask mSyncMessageTask = new SyncMessageTask();
		
		public GLHandler() {
			super(Looper.getMainLooper());
		}
		
		class SyncMessageTask extends Task {
			Message mMessage;
			SyncMessageTask() {
			}
			@Override
			public void doTask() {
				if (mMessage != null) {
					dispatchMessage(mMessage);
					mMessage.recycle();
				}
				mMessage = null;
			}
		}
		
		@Override
		public String getMessageName(Message msg) {
			switch (msg.what) {
			case MSG_SURFACE_CREATED:
				return "MSG_SURFACE_CREATED";
			case MSG_SURFACE_CHANGED:
				return "MSG_SURFACE_CHANGED";
			case MSG_SURFACE_DESTROYED:
				return "MSG_SURFACE_DESTROYED";
			case MSG_ATTACHED_TO_WINDOW:
				return "MSG_ATTACHED_TO_WINDOW";
			case MSG_DETACHED_FROM_WINDOW:
				return "MSG_DETACHED_FROM_WINDOW";
			case MSG_REQUEST_LAYOUT:
				return "MSG_REQUEST_LAYOUT";
			case MSG_SET_CONTENT_VIEW:
				return "MSG_SET_CONTENT_VIEW";
			default:
				break;
			}
			return super.getMessageName(msg);
		}
		
		@Override
		public void handleMessage(Message msg) {
			if (DEBUG_GL_MESSAGE) Log.d(TAG, "GLHandler handleMessage, message=" + getMessageName(msg));
			if (DEBUG_LOOPER) getLooper().dump(pw, "looper");
			switch (msg.what) {
			case MSG_SURFACE_CREATED:
				getRenderer().initialize(msg.obj);
				scheduleRender();
				break;
			case MSG_SURFACE_CHANGED:
				getRenderer().setSize(msg.obj, msg.arg1, msg.arg2);
				scheduleRender();
				break;
			case MSG_SURFACE_DESTROYED:
				getRenderer().destroy(false);
				unscheduleRender();
				break;
			case MSG_ATTACHED_TO_WINDOW:
				attachToRoot();
				break;
			case MSG_DETACHED_FROM_WINDOW:
				detachFromRoot();
				destroy();
				unscheduleRender();
				break;
			case MSG_REQUEST_LAYOUT:
				requestLayoutGLContentView();
				break;
			case MSG_SET_CONTENT_VIEW:
				if (msg.obj != null) {
					attachContentView((View) msg.obj);
				} else {
					attachContentView(msg.arg1);
				}
				break;
			default:
				break;
			}
		}
		
		/**
		 * This may block the current thread if current thread is not this handler thread.
		 * Methods like {@link GLSurfaceView#surfaceCreated(SurfaceHolder)}
		 * {@link GLSurfaceView#surfaceChanged(SurfaceHolder, int, int, int)}
		 * {@link GLSurfaceView#surfaceDestroyed(SurfaceHolder)}
		 * {@link GLSurfaceView#onDetachedFromWindow()} should use this to send sync messages.
		 * @param what
		 * @param arg1
		 * @param arg2
		 * @param obj
		 */
		void sendSyncMessage(int what, int arg1, int arg2, Object obj) {
			// Post a sync message. 
			// Shouldn't call this frequently, or should we use a task poll?
			// This method is only called from the Android main thread until now, so we can cache the task object in a local member.
			final SyncMessageTask task = mSyncMessageTask;//new SyncMessageTask();
			task.mMessage = obtainMessage(what, arg1, arg2, obj);
			postAndWait(task);
		}
		
		Message sendLocalMessage(int what, int arg1, int arg2, Object obj) {
			Message message = obtainMessage(what, arg1, arg2, obj);
			runOnGLThread(message);
			return message;
		}
		
		void runOnGLThread(Message msg) {
			if (isCurrentThread()) {
				dispatchMessage(msg);
			} else {
				sendMessage(msg);
			}
		}
	}
	
	public static interface Callback {
		public void onAttached(View content);
	}

	@Override
	public void onTouchModeChanged(boolean isInTouchMode) {
		ensureTouchMode(isInTouchMode);
	}
	
	/**
     * Something in the current window tells us we need to change the touch mode.  For
     * example, we are not in touch mode, and the user touches the screen.
     *
     * If the touch mode has changed, tell the window manager, and handle it locally.
     *
     * @param inTouchMode Whether we want to be in touch mode.
     * @return True if the touch mode changed and focus changed was changed as a result
     */
    boolean ensureTouchMode(boolean inTouchMode) {
    	if (mAttachInfo.mInTouchMode == inTouchMode) {
    		return false;
    	}
    	mTouchModeChangedTask.mIsInTouchMode = inTouchMode;
		mGLHandler.postAndWait(mTouchModeChangedTask);
		return true;
    }
	
	TouchModeChangedTask mTouchModeChangedTask = new TouchModeChangedTask();
	
	class TouchModeChangedTask extends Task {
		boolean mIsInTouchMode;
		@Override
		public void doTask() {
			mAttachInfo.mInTouchMode = mIsInTouchMode;
			mAttachInfo.mTreeObserver.dispatchOnTouchModeChanged(mIsInTouchMode);
			if (mIsInTouchMode) {
				enterTouchMode();
			} else {
				leaveTouchMode();
			}
		}
	}
	
	private boolean enterTouchMode() {
        if (mDecor != null && mDecor.hasFocus()) {
            // note: not relying on mFocusedView here because this could
            // be when the window is first being added, and mFocused isn't
            // set yet.
            final View focused = mDecor.findFocus();
            if (focused != null && !focused.isFocusableInTouchMode()) {
                // There's nothing to focus. Clear and propagate through the
                // hierarchy, but don't attempt to place new focus.
                focused.clearFocusInternal(null, true, false);
                return true;
            }
        }
        return false;
    }
	
	private boolean leaveTouchMode() {
        if (mDecor != null) {
            if (mDecor.hasFocus()) {
                View focusedView = mDecor.findFocus();
                if (!(focusedView instanceof ViewGroup)) {
                    // some view has focus, let it keep it
                    return false;
                } else if (((ViewGroup) focusedView).getDescendantFocusability() !=
                        ViewGroup.FOCUS_AFTER_DESCENDANTS) {
                    // some view group has focus, and doesn't prefer its children
                    // over itself for focus, so let them keep it.
                    return false;
                }
            }

            // find the best view to give focus to in this brave new non-touch-mode
            // world
            final View focused = focusSearch(null, View.FOCUS_DOWN);
            if (focused != null) {
                return focused.requestFocus(View.FOCUS_DOWN);
            }
        }
        return false;
    }
	
	/**
     * {@inheritDoc}
     */
    public View focusSearch(View focused, int direction) {
        checkThread();
        if (!(mDecor instanceof ViewGroup)) {
            return null;
        }
        return FocusFinder.getInstance().findNextFocus((ViewGroup) mDecor, focused, direction);
    }
	
	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus) {
		super.onWindowFocusChanged(hasWindowFocus);
		mWindowFocusChangedTask.mHasWindowFocus = hasWindowFocus;
		mGLHandler.postAndWait(mWindowFocusChangedTask);
	}
	
	WindowFocusChangedTask mWindowFocusChangedTask = new WindowFocusChangedTask();
	
	class WindowFocusChangedTask extends Task {
		boolean mHasWindowFocus;
		@Override
		public void doTask() {
			mAttachInfo.mHasWindowFocus = mHasWindowFocus;
			if (mDecor != null) {
				mDecor.dispatchWindowFocusChanged(mHasWindowFocus);
			}
		}
	}
	
	@Override
	protected void onWindowVisibilityChanged(int visibility) {
		super.onWindowVisibilityChanged(visibility);
		mWindowVisibilityChangedTask.mVisibility = visibility;
		mGLHandler.postAndWait(mWindowVisibilityChangedTask);
	}
	
	WindowVisibilityChangedTask mWindowVisibilityChangedTask = new WindowVisibilityChangedTask();
	
	class WindowVisibilityChangedTask extends Task {
		int mVisibility;
		@Override
		public void doTask() {
			if (mDecor != null) {
				mDecor.dispatchWindowVisibilityChanged(mVisibility);
			}
		}
	}
	
}
