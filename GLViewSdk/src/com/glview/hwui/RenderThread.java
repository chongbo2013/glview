package com.glview.hwui;

import android.os.Build;
import android.os.Process;
import android.os.StrictMode;
import android.os.StrictMode.ThreadPolicy;
import android.util.Log;

import com.glview.App;
import com.glview.libgdx.graphics.opengl.AndroidGL20;
import com.glview.thread.Looper;

class RenderThread extends Thread {

	final static String TAG = "RenderThread";
	final static boolean DEBUG = false;
	
	private static Looper sMainLooper;
	
	private static RenderThread sMainThread;
	
	private Looper mLooper;
	
	private RenderThread() {
	}
	
	private static void ensureRenderThread() {
		if (sMainThread == null || !sMainThread.isAlive()) {
			sMainThread = new RenderThread();
			sMainThread.start();
			sMainLooper = sMainThread.getLooper();
		}
	}
	
	public static Looper getRenderThreadLooper() {
		ensureRenderThread();
		return sMainLooper;
	}
	
	@Override
	public synchronized void start() {
		super.start();
	}
	
	boolean checkThreadState() {
		return Looper.myLooper() == mLooper;
	}
	
	@Override
	public void run() {
		setName("RenderThread " + getId());
		Log.i(TAG, "starting tid=" + getId());
		init();
		// 主线程，调用prepare(false)，线程不允许被quit
//		try {
//			Log.i(TAG, "Try to prepare the looper with declaredMethod 'private static void android.os.Looper.prepare(boolean)' to set quitAllowed=false");
//			Method m = Looper.class.getDeclaredMethod("prepare", boolean.class);
//			m.setAccessible(true);
//			m.invoke(null, false);
//			m.setAccessible(false);
//		} catch(Exception e) {
//			Log.w(TAG, "Reflection failed, use public prepare", e);
//		}
//		if (Looper.myLooper() == null) {
//			Looper.prepare();
//		}
		Looper.prepare1();
		App.setGL20(new AndroidGL20());
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
		Looper.loop();
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
