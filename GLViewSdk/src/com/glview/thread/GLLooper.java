package com.glview.thread;

import android.os.Build;
import android.os.Process;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.util.Log;

/**
 * GLView的MainLooper，使用该线程{@link GLHandlerThread}
 * 通过{@link #getGLLooper()} 取得GLThread主线程
 * @author lijing.lj
 */
final class GLLooper {
	
	private final static String TAG = "GLLooper";
	
	private static Looper sGLMainLooper;
	
	private static GLHandlerThread sGLMainThread;
	
	/**
	 * 线程允许的异常次数
	 * 0表示不允许出现任何异常，否则直接crash
	 * @see GLHandlerThread#intoLoop()
	 */
	private final static int MAX_LOOP_EXCEPTION_ALLOWED = 0;
	
	private static void ensureGLThread() {
		if (sGLMainThread == null || !sGLMainThread.isAlive()) {
			sGLMainThread = new GLHandlerThread(true);
			sGLMainThread.start();
			sGLMainLooper = sGLMainThread.getLooper();
			Watchdog.getInstance().addThread(sGLMainLooper, new Handler(sGLMainLooper), "GLMainThread");
		}
	}

	/**
	 * MainLooper
	 * @return
	 */
	public synchronized static Looper getGLMainLooper() {
		ensureGLThread();
		return sGLMainLooper;
//		return Looper.getMainLooper();
	}
	
	static class GLHandlerThread extends Thread {
		
		boolean mMainLooper;
		
		Looper mLooper;
		
		public GLHandlerThread() {
			this(false);
		}
		
		public GLHandlerThread(boolean mainLooper) {
			mMainLooper = mainLooper;
		}
		
		@Override
		public void run() {
			setName("GLThread " + getId());
			Log.i(TAG, "starting tid=" + getId());
			init();
			if (mMainLooper) {
				// 主线程，调用prepare(false)，线程不允许被quit
				/*try {
					Log.i(TAG, "Try to prepare the looper with declaredMethod 'private static void android.os.Looper.prepare(boolean)' to set quitAllowed=false");
					Method m = Looper.class.getDeclaredMethod("prepare", boolean.class);
					m.setAccessible(true);
					m.invoke(null, false);
					m.setAccessible(false);
				} catch(Exception e) {
					Log.w(TAG, "Reflection failed, use public prepare", e);
				}*/
				Looper.prepareMainLooper();
			}
			if (Looper.myLooper() == null) {
				Looper.prepare();
			}
			synchronized (this) {
	            mLooper = Looper.myLooper();
	            notifyAll();
	        }
			intoLoop();
		}
		
		void init() {
			Process.setThreadPriority(Process.THREAD_PRIORITY_DISPLAY);
			// disable network access in main thread
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				StrictMode.setThreadPolicy(new ThreadPolicy.Builder().detectNetwork().penaltyDeathOnNetwork().build());
			}
		}
		
		void intoLoop() {
			int exceptionCount = 0;
			while (true) {
				try {
					Looper.loop();
				} /*catch (NetworkOnMainThreadException e) {
					Log.w(TAG, "GLHandlerThread catch NetworkOnMainThreadException, crash the process, fix it", e);
					throw e;
				} */catch(Throwable e) {
					Log.w(TAG, "GLHandlerThread catch some exception", e);
					if (++ exceptionCount > MAX_LOOP_EXCEPTION_ALLOWED) {
						Log.w(TAG, "Beyond the max allowed count, crash the process. exceptionCount:" + exceptionCount + ", MAX_LOOP_EXCEPTION_ALLOWED:" + MAX_LOOP_EXCEPTION_ALLOWED);
						throw new RuntimeException(e);
					}
					// maybe we can continue the loop
					Log.w(TAG, "Maybe we can continue the loop. exceptionCount:" + exceptionCount + ", MAX_LOOP_EXCEPTION_ALLOWED:" + MAX_LOOP_EXCEPTION_ALLOWED);
					continue;
				}
				// no exception， safe quit
				return;
			}
		}
		
		/**
	     * This method returns the Looper associated with this thread. If this thread not been started
	     * or for any reason is isAlive() returns false, this method will return null. If this thread 
	     * has been started, this method will block until the looper has been initialized.  
	     * @return The looper.
	     */
	    public Looper getLooper() {
	        if (!isAlive()) {
	            return null;
	        }
	        // If the thread has been started, wait until the looper has been created.
	        synchronized (this) {
	            while (isAlive() && mLooper == null) {
	                try {
	                    wait();
	                } catch (InterruptedException e) {
	                }
	            }
	        }
	        return mLooper;
	    }
	}
}
