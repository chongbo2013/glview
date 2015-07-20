package com.glview.hwui;


public abstract class Task implements Runnable {
	
	private boolean mTaskDone = true;
	private boolean mCanceled = true;
	
	/**
	 * A sync task, main thread may be blocked by the handler thread.
	 */
	private boolean mSyncTask = false;
	
	public boolean isTaskDone() {
		return mTaskDone;
	}
	
	public boolean isRunning() {
		return !mTaskDone || mCanceled;
	}
	
	void setSync(boolean sync) {
		mSyncTask = sync;
	}
	
	void startTask() {
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
	
	final void finishTask() {
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
	
	final void cancelTask() {
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
