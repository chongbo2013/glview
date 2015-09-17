package com.glview.hwui.task;

import android.util.Log;

import com.glview.thread.Handler;
import com.glview.thread.Looper;

/**
 * A task handler that can run sync tasks.
 * @author lijing.lj
 */
public class TaskHandler extends Handler {

	final static String TAG = "TaskHandler";
	final static boolean DEBUG = false;
	
	public TaskHandler(Looper looper) {
		super(looper);
	}
	
	public boolean isCurrentThread() {
		return getLooper().getThread() == Thread.currentThread();
	}
	
	public boolean post(Task task) {
		if (DEBUG) Log.d(TAG, "Post a task. task=" + task);
		return super.post(task);
	}
	
	public boolean postAtFrontOfQueue(Task task) {
		if (DEBUG) Log.d(TAG, "Post a task at front of queue. task=" + task);
		return super.postAtFrontOfQueue(task);
	}
	
	public void remove(Task task) {
		super.removeCallbacks(task);
	}
	
	/**
     * Runs the specified task synchronously.
     * <p>
     * If the current thread is the same as the handler thread, then the runnable
     * runs immediately without being enqueued.  Otherwise, posts the runnable
     * to the handler and waits for it to complete before returning.
     * </p><p>
     * This method is dangerous!  Improper use can result in deadlocks.
     * Never call this method while any locks are held or use it in a
     * possibly re-entrant manner.
     * </p><p>
     * This method is occasionally useful in situations where a background thread
     * must synchronously await completion of a task that must run on the
     * handler's thread.  However, this problem is often a symptom of bad design.
     * Consider improving the design (if possible) before resorting to this method.
     * </p><p>
     * One example of where you might want to use this method is when you just
     * set up a Handler thread and need to perform some initialization steps on
     * it before continuing execution.
     * </p><p>
     * If timeout occurs then this method returns <code>false</code> but the runnable
     * will remain posted on the handler and may already be in progress or
     * complete at a later time.
     * </p><p>
     * When using this method, be sure to use {@link Looper#quitSafely} when
     * quitting the looper.  Otherwise {@link #runWithScissors} may hang indefinitely.
     * (TODO: We should fix this by making MessageQueue aware of blocking runnables.)
     * </p>
     *
     * @param task The Runnable that will be executed synchronously.
     *
     */
	public boolean postAndWait(Task task) {
		if (DEBUG) Log.d(TAG, "Post a sync task. task=" + task);
		if (isCurrentThread()) {
			task.run();
			return true;
		} else {
			BlockingRunable blockingRunable = BlockingRunable.obtain(task);
			boolean r =  blockingRunable.postAndWait(this, -1);
			blockingRunable.recycle();
			return r;
		}
	}
	
	public boolean postAtFrontOfQueueAndWait (Task task) {
		if (DEBUG) Log.d(TAG, "Post a sync task at front of queue. task=" + task);
		if (isCurrentThread()) {
			task.run();
			return true;
		} else {
			BlockingRunable blockingRunable = BlockingRunable.obtain(task);
			boolean r =  blockingRunable.postAtFrontOfQueueAndWait(this, -1);
			blockingRunable.recycle();
			return r;
		}
	}

}
