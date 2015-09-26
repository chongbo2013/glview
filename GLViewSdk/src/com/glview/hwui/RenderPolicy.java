package com.glview.hwui;

import java.util.List;

import android.util.Log;
import android.util.LogPrinter;
import android.util.Printer;
import android.view.Surface;

import com.glview.animation.Animator;
import com.glview.graphics.Bitmap;
import com.glview.hwui.task.Task;
import com.glview.hwui.task.TaskHandler;
import com.glview.view.GLRootView;

public final class RenderPolicy {
	
	private final static String TAG = "RenderPolicy";
	private final static boolean DEBUG = true;
	private final static boolean DEBUG_LOOPER = false;
	private final static boolean DEBUG_DRAW = false;
	
	Printer pw = new LogPrinter(Log.DEBUG, TAG);
	
	CanvasContext mCanvasContext = null;
	
	RenderNode mRootNode;
	
	/**
	 * This is the render thread's message handler.
	 * It handle out render thread's messages.
	 * @see #RenderPolicy(RenderNode)
	 * @see #initialize(Surface)
	 * @see #destroy(boolean)
	 * @see #syncAndDrawFrame()
	 */
	TaskHandler mHandler;
	
	/**
	 * If the render thread {@link #mHandler} has been exited.
	 * We do nothing after it's exited.
	 */
	boolean mExited = false;
	
	/**
	 * Draw frame task, it's a sync task, called from the GLThread {@link GLRootView#onDrawFrame()}.
	 * Our GLThread may be blocked by the render thread.
	 * @see #syncAndDrawFrame()
	 */
	DrawTask mDrawTask = new DrawTask();
	
	public RenderPolicy(RenderNode rootRenderNode) {
		mRootNode = rootRenderNode;
		init();
	}
	
	private void init() {
		mExited = false;
		mHandler = new TaskHandler(RenderThread.getRenderThreadLooper());
		mHandler.postAndWait(new PolicyTask(PolicyTask.TASK_CREATE_CONTEXT, 0, 0, mRootNode));
	}
	
	public void initialize(Object surface) {
		if (mExited) {
			init();
		}
		mHandler.postAndWait(new PolicyTask(PolicyTask.TASK_INITIALIZE, 0, 0, surface));
	}
	
	public void startAnimation(List<Animator> animators) {
		if (mExited) return;
		mCanvasContext.startAnimation(animators);
	}
	
	public void stopAnimation(List<Animator> animators) {
		if (mExited) return;
		mCanvasContext.stopAnimation(animators);
	}
	
	public Bitmap buildDrawingCache(RenderNode renderNode) {
		if (mExited) return null;
		BuildDrawingCacheTask task = new BuildDrawingCacheTask(renderNode);
		mHandler.postAndWait(task);
		return task.mDrawingCache;
	}
	
	public void setSize(Object surface, int width, int height) {
		if (mExited) return;
		mHandler.postAndWait(new PolicyTask(PolicyTask.TASK_SETSIZE, width, height, surface));
	}
	
	public void destroy(boolean full) {
		if (DEBUG) Log.d(TAG, "destroy called full=" + full);
		mHandler.removeCallbacksAndMessages(null);
		mHandler.postAtFrontOfQueueAndWait(new PolicyTask(PolicyTask.TASK_DESTROY, 0, 0, full));
		if (full) {
			mExited = true;
			mHandler = null;
		}
		if (DEBUG) Log.d(TAG, "destroy called end");
	}
	
	public void syncAndDrawFrame() {
		if (mExited) return;
		if (isEnable()) {
			if (DEBUG_DRAW) Log.d(TAG, "sync and draw frame begin!");
			mHandler.postAndWait(mDrawTask);
			if (DEBUG_DRAW) Log.d(TAG, "sync and draw frame end!");
//			scheduleAnimatingDrawTask();
		}
	}
	
	public boolean isEnable() {
		return mCanvasContext != null && mCanvasContext.isEnable();
	}
	
	private void innerDraw(boolean fromGLThread) {
		if (DEBUG_LOOPER) mHandler.getLooper().dump(pw, "looper");
		/*if (mCanvasContext.draw()) {
			if (mAnimating && mThread.checkThreadState()) {
				scheduleAnimatingDrawTask();
			}
		}*/
		if (mCanvasContext.draw() && fromGLThread) {
//			mHandler.remove(mDrawTask);
		}
	}
	
	class PolicyTask extends Task {
		final static int TASK_CREATE_CONTEXT = 0;
		final static int TASK_INITIALIZE = 1;
		final static int TASK_SETSIZE = 2;
		final static int TASK_DESTROY = 3;
		
		int what;
		int arg1;
		int arg2;
		Object obj;
		
		PolicyTask(int what, int arg1, int arg2, Object obj) {
			this.what = what;
			this.arg1 = arg1;
			this.arg2 = arg2;
			this.obj = obj;
		}

		@Override
		public void doTask() {
			if (DEBUG) Log.d(TAG, "PolicyTask doTask what=" + what + ", arg1=" + arg1 + ", arg2=" + arg2 + ", obj=" + obj + ", tid=" + Thread.currentThread().getId());
			switch (what) {
			case TASK_CREATE_CONTEXT:
				mCanvasContext = new CanvasContext((RenderNode) obj);
				break;
			case TASK_INITIALIZE:
				mCanvasContext.initialize(obj);
				break;
			case TASK_SETSIZE:
				mCanvasContext.setSize(obj, arg1, arg2);
				break;
			case TASK_DESTROY:
				mCanvasContext.destroy((Boolean) obj);
				break;
			default:
				break;
			}
		}
	}
	
	class DrawTask extends Task {
		@Override
		public void doTask() {
			innerDraw(true);
		}
	}
	
	class BuildDrawingCacheTask extends Task {
		RenderNode mRenderNode;
		Bitmap mDrawingCache;
		public BuildDrawingCacheTask(RenderNode renderNode) {
			mRenderNode = renderNode;
		}
		@Override
		public void doTask() {
			if (mRenderNode != null) {
				mDrawingCache = mCanvasContext.buildDrawingCache(mRenderNode);
			}
		}
	}
	
	public static void trimMemory(int level) {
		CanvasContext.trimMemory(level);
	}
}
