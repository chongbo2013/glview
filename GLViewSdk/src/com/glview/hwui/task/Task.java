package com.glview.hwui.task;


public abstract class Task implements Runnable {
	
	private boolean mTaskDone = true;
	private boolean mCanceled = false;
	
	/**
	 * A sync task, main thread may be blocked by the handler thread.
	 */
	private boolean mSyncTask = false;
	
	public synchronized boolean isTaskDone() {
		return mTaskDone;
	}
	
	public synchronized boolean isRunning() {
		return !mTaskDone && !mCanceled;
	}
	
	synchronized void setSync(boolean sync) {
		mSyncTask = sync;
	}
	
	synchronized void startTask() {
		mTaskDone = false;
		mCanceled = false;
	}
	
	@Override
	public final void run() {
		try {
			doTask();
		} finally {
			finishTask();
		}
	}
	
	final synchronized void finishTask() {
		if (!mTaskDone) {
			mTaskDone = true;
			// If this is a sync task, notify the main thread which is blocked by me.
			if (mSyncTask) {
				synchronized (this) {
					notifyAll();
				}
			}
		}
	}
	
	final synchronized void cancelTask() {
		if (!mCanceled) {
			mCanceled = true;
			// If this is a sync task, notify the main thread which is blocked by me.
			if (mSyncTask) {
				synchronized (this) {
					notifyAll();
				}
			}
		}
	}
	
	abstract public void doTask();
}
