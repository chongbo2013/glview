package com.glview.hwui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLSurface;

import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.SurfaceHolder;

import com.glview.App;
import com.glview.animation.Animator;
import com.glview.animation.AnimatorListenerAdapter;
import com.glview.animation.ValueAnimator;
import com.glview.animation.ValueAnimator.AnimatorUpdateListener;
import com.glview.graphics.Bitmap;
import com.glview.hwui.font.FontRenderer;
import com.glview.hwui.task.Task;
import com.glview.hwui.task.TaskHandler;
import com.glview.thread.Looper;
import com.glview.util.FPSUtils;
import com.glview.view.GLRootView;

/**
 * This is the context of our RenderThread.
 * It holds EGLContext and Canvas instance, and the state that ensure
 * our drawing is enable {@link #isEnable()}.
 * 
 * @author lijing.lj
 */
class CanvasContext {
	
	private final static String TAG = "CanvasContext";
	private final static boolean DEBUG = true;
	private final static boolean DEBUG_FRAME = false;
	private final static boolean DEBUG_FPS = true;
	private final static boolean DEBUG_ANIMATING = false;
	
	/**
	 * EGL Management.
	 * Manage the EGL Context.
	 */
	static EglManager sEglManager;

    EGLSurface mEglSurface;
    
    /**
     * Viewport width and height.
     */
    int mWidth = -1, mHeight = -1;
    
    GLCanvas mCanvas;
    
    /**
     * The root node, we begin our frame with this node.
     * It contains the real CanvasOp list.
     * @see RenderNode#replay(GLCanvas)
     */
    RenderNode mRootNode;
    
    FPSUtils mFpsUtils = null;
    
    RenderState mRenderState;
    
    SurfaceHolder mSurfaceHolder;
    
    boolean mExited = false;
    
    TaskHandler mHandler;
    
    /**
	 * This task is used to schedule the RT-driven animation's draw frame.
	 * It's running in render thread.
	 * @see #scheduleAnimatingDrawTask()
	 * @see #unscheduleAnimatingDrawTask()
	 */
	AnimatingDrawTask mAnimatingDrawTask = new AnimatingDrawTask();
	boolean mAnimatingDrawRequested = false;
	
	boolean mAnimating = false;
	
	AnimationStartTask mAnimationStartTask = new AnimationStartTask();
	AnimationStopTask mAnimationStopTask = new AnimationStopTask();
	
	/**
	 * The RT-driven animations. These animations run in the render thread.
	 * We will schedule a frame draw in {@link #mUpdateListener}.
	 */
    List<Animator> mAnimators = new ArrayList<Animator>();
    AnimatorListenerAdapter mListener = new AnimatorListenerAdapter() {
    	@Override
    	public void onAnimationStart(Animator animation) {
    		if (DEBUG_ANIMATING) Log.d(TAG, "onAnimationStart, scheduleAnimatingDrawTask. animation=" + animation);
    		animationStarted(animation);
    	}
    	@Override
    	public void onAnimationCancel(Animator animation) {
    		if (DEBUG_ANIMATING) Log.d(TAG, "onAnimationCancel. animation=" + animation);
    		animationStopped(animation);
    	}
    	@Override
    	public void onAnimationEnd(Animator animation) {
    		if (DEBUG_ANIMATING) Log.d(TAG, "onAnimationEnd. animation=" + animation);
    		animationStopped(animation);
    	}
	};
	
	/**
     * If we are in animating (RenderThread-driven animation), we should call
     * the method {@link #scheduleAnimatingDrawTask()} to schedule a draw operation
     * in {@link AnimatorUpdateListener#onAnimationUpdate(ValueAnimator)}.
     * This flag is just running in {@link RenderThread}.
     */
	AnimatorUpdateListener mUpdateListener = new AnimatorUpdateListener() {
		@Override
		public void onAnimationUpdate(ValueAnimator animation) {
			if (DEBUG_ANIMATING) Log.d(TAG, "onAnimationUpdate. animation=" + animation);
			scheduleAnimatingDrawTask();
		}
	};
	
    public CanvasContext(RenderNode rootRenderNode) {
    	mHandler = new TaskHandler(Looper.myLooper());
    	ensureEglManager();
    	mRootNode = rootRenderNode;
    	if (DEBUG_FPS) mFpsUtils = new FPSUtils(this);
    }
    
    public static void ensureEglManager() {
    	// now we only support OpenGL 2.0.
    	if (sEglManager == null) sEglManager = new EglManager();
    	sEglManager.initializeEgl();
    }
    
    /**
     * Initialize the EGL surface.
     * Called by {@link GLRootView#surfaceCreated(android.view.SurfaceHolder)}
     * @param surface
     */
	public void initialize(Object surface) {
		if (surface != mSurfaceHolder) {
			if (surface instanceof SurfaceHolder) {
				mSurfaceHolder = (SurfaceHolder) surface;
			} else {
				mSurfaceHolder = null;
			}
		}
        if (createSurface(surface)) {
        	if (mCanvas == null) {
        		mCanvas = createGLCanvas();
        	}
        }
	}
	
	private GLCanvas createGLCanvas() {
		if (!sEglManager.hasEglContext()) {
			Log.w(TAG, "Why u reach here!");
			throw new IllegalStateException("No egl context exists.");
		}
		if (mRenderState == null) {
			mRenderState = new RenderState(App.getGL20());
		}
		return new GL20Canvas(mRenderState);
	}
	
	/**
	 * Set the canvas's size.
	 * Called by {@link GLSurfaceView#surfaceChanged(android.view.SurfaceHolder, int, int, int)}. 
	 * @param width
	 * @param height
	 */
	void setSize(Object surface, int width, int height) {
		if (DEBUG) Log.d(TAG, "setSize called in CanvasContext, width=" + width + ", height=" + height);
		if (mEglSurface == null || mEglSurface == EGL10.EGL_NO_SURFACE) {
			initialize(surface);
		}
		if (ensureCurrentSurface()) {
			mWidth = width;
			mHeight = height;
			mCanvas.setSize(mWidth, mHeight);
		}
	}
	
	/**
	 * If is able to draw.
	 * @return
	 */
	boolean isEnable() {
		return mEglSurface != null && mEglSurface != EGL10.EGL_NO_SURFACE && mWidth > 0 && mHeight > 0 && sEglManager.hasEglContext();
	}
	
	private boolean ensureCurrentSurface() {
    	return sEglManager.makeCurrent(mEglSurface);
    }
	
	/**
	 * Draw one frame, called by RenderPolicy {@link RenderPolicy#innerDraw()}
	 * @return
	 */
	public boolean draw() {
		if (isEnable() && ensureCurrentSurface()) {
			if (DEBUG_FRAME) Log.d(TAG, "One frame begin!");
			mCanvas.beginFrame();
			mCanvas.drawRenderNode(mRootNode);
			mCanvas.endFrame();
			swapBuffers();
			if (DEBUG_FPS) mFpsUtils.fps();
			if (DEBUG_FRAME) Log.d(TAG, "One frame end!");
			return true;
		}
		return false;
	}
	
	private boolean createSurface(Object surface) {
		if (DEBUG) Log.d(TAG, "createSurface called in CanvasContext, surface=" + surface);
		destroySurface();
        mEglSurface = sEglManager.createSurface(surface);
        return mEglSurface != null;
    }
    
    void destroy(boolean full) {
    	if (DEBUG) Log.d(TAG, "destroy called in CanvasContext, full=" + full);
        destroySurface();
        mSurfaceHolder = null;
        if (full) {
        	mRootNode.destroy();
        	mCanvas = null;
        	mHandler.removeCallbacksAndMessages(null);
	        clearAnimations();
	        mExited = true;
        }
    }
    
    void destroyHardwareResources() {
    	Caches.getInstance().clear();
    	destroyContext();
    	ensureEglManager();
    }
    
    void swapBuffers() {
    	if (DEBUG_FRAME) Log.d(TAG, "swapBuffers called in CanvasContext.");
    	if (!sEglManager.swapBuffers(mEglSurface)) {
    		if (DEBUG_FRAME) Log.d(TAG, "swapBuffers failed, maybe the surface is no longer valid.");
    		destroySurface();
    	}
    }
    
    void destroySurface() {
    	if (mEglSurface != null) {
    		if (DEBUG) Log.d(TAG, "destroy EGLSurface called in CanvasContext.");
    		sEglManager.destroySurface(mEglSurface);
    		mEglSurface = null;
    	}
    }
    
    void destroyContext() {
    	if (DEBUG) Log.d(TAG, "destroy EGLContext called in CanvasContext.");
    	sEglManager.destroy();
    }
    
    public void startAnimation(List<Animator> animators) {
		mAnimationStartTask.mAnimators = animators;
		mHandler.postAtFrontOfQueueAndWait(mAnimationStartTask);
	}
	
	public void stopAnimation(List<Animator> animators) {
		mAnimationStopTask.mAnimators = animators;
		mHandler.postAtFrontOfQueueAndWait(mAnimationStopTask);
	}
	
	public Bitmap buildDrawingCache(RenderNode renderNode) {
		if (mCanvas != null) {
			return renderNode.buildDrawingCache(mCanvas);
		}
		return null;
	}
	
	private void scheduleAnimatingDrawTask() {
		if (mExited) return;
		if (!mAnimatingDrawRequested) {
			mAnimatingDrawRequested = true;
			mHandler.post(mAnimatingDrawTask);
		}
	}
	
	private void unscheduleAnimatingDrawTask() {
		mAnimatingDrawRequested = false;
		mHandler.remove(mAnimatingDrawTask);
	}
	
	private void animationStarted(Animator animation) {
		scheduleAnimatingDrawTask();
	}
	
	private void animationStopped(Animator animation) {
		mAnimators.remove(animation);
		if (mAnimators.size() == 0) {
			Log.i(TAG, "running animations is empty.");
		}
	}
	
	private void clearAnimations() {
		for (Iterator<Animator> iter = mAnimators.iterator(); iter.hasNext(); ) {
			iter.next().cancel();
		}
	}
	
	/*
	 * Run in render thread.
	 */
	class AnimatingDrawTask extends Task {
		@Override
		public void doTask() {
			mAnimatingDrawRequested = false;
			draw();
		}
	}
	
	class AnimationStartTask extends Task {
		List<Animator> mAnimators;
		@Override
		public void doTask() {
			if (mAnimators != null) {
				for (Animator animator : mAnimators) {
					animator.addListener(mListener);
					((ValueAnimator) animator).addUpdateListener(mUpdateListener);
					animator.start();
				}
			}
			mAnimators = null;
		}
	}
	
	class AnimationStopTask extends Task {
		List<Animator> mAnimators;
		@Override
		public void doTask() {
			if (mAnimators != null) {
				for (Animator animator : mAnimators) {
					animator.cancel();
				}
			}
			mAnimators = null;
		}
	}
	
	/**
     * Level for {@link #onTrimMemory(int)}: the process is nearing the end
     * of the background LRU list, and if more memory isn't found soon it will
     * be killed.
     */
    static final int TRIM_MEMORY_COMPLETE = 80;
    
    /**
     * Level for {@link #onTrimMemory(int)}: the process is around the middle
     * of the background LRU list; freeing memory can help the system keep
     * other processes running later in the list for better overall performance.
     */
    static final int TRIM_MEMORY_MODERATE = 60;
    
    /**
     * Level for {@link #onTrimMemory(int)}: the process has gone on to the
     * LRU list.  This is a good opportunity to clean up resources that can
     * efficiently and quickly be re-built if the user returns to the app.
     */
    static final int TRIM_MEMORY_BACKGROUND = 40;
    
    /**
     * Level for {@link #onTrimMemory(int)}: the process had been showing
     * a user interface, and is no longer doing so.  Large allocations with
     * the UI should be released at this point to allow memory to be better
     * managed.
     */
    static final int TRIM_MEMORY_UI_HIDDEN = 20;

    /**
     * Level for {@link #onTrimMemory(int)}: the process is not an expendable
     * background process, but the device is running extremely low on memory
     * and is about to not be able to keep any background processes running.
     * Your running process should free up as many non-critical resources as it
     * can to allow that memory to be used elsewhere.  The next thing that
     * will happen after this is {@link #onLowMemory()} called to report that
     * nothing at all can be kept in the background, a situation that can start
     * to notably impact the user.
     */
    static final int TRIM_MEMORY_RUNNING_CRITICAL = 15;

    /**
     * Level for {@link #onTrimMemory(int)}: the process is not an expendable
     * background process, but the device is running low on memory.
     * Your running process should free up unneeded resources to allow that
     * memory to be used elsewhere.
     */
    static final int TRIM_MEMORY_RUNNING_LOW = 10;


    /**
     * Level for {@link #onTrimMemory(int)}: the process is not an expendable
     * background process, but the device is running moderately low on memory.
     * Your running process may want to release some unneeded resources for
     * use elsewhere.
     */
    static final int TRIM_MEMORY_RUNNING_MODERATE = 5;
    
    static TaskHandler sTaskHandler = new TaskHandler(RenderThread.getRenderThreadLooper());
    static void trimMemory(final int level) {
    	sTaskHandler.postAndWait(new Task() {
			@Override
			public void doTask() {
				ensureEglManager();
				Log.v(TAG, "trimMemory level=" + level);
				if (level >= TRIM_MEMORY_COMPLETE) {
					FontRenderer.instance().release();
					Caches.getInstance().clear();
				} if (level >= TRIM_MEMORY_MODERATE) {
					FontRenderer.instance().release();
					Caches.getInstance().textureCache.clear();
				} else if (level >= TRIM_MEMORY_UI_HIDDEN) {
					FontRenderer.instance().release();
					Caches.getInstance().textureCache.flush();
				} else {
					if (level == TRIM_MEMORY_RUNNING_MODERATE) {
						Caches.getInstance().textureCache.flush();
					} else {
						Caches.getInstance().clear();
					}
				}
			}
		});
    }
}
