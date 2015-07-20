package com.glview.widget;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

public class Toast {
	
	/**
     * Show the view or text notification for a short period of time.  This time
     * could be user-definable.  This is the default.
     * @see #setDuration
     */
    public static final int LENGTH_SHORT = 0;

    /**
     * Show the view or text notification for a long period of time.  This time
     * could be user-definable.
     * @see #setDuration
     */
    public static final int LENGTH_LONG = 1;
	
	/**
	 * Android's main handler.
	 */
    static Handler sHandler = new Handler(Looper.getMainLooper());

	public static void showShortToast(final Context context, final String text) {
		showToast(context, text, LENGTH_SHORT);
	}
	
	public static void showLongToast(final Context context, final String text) {
		showToast(context, text, LENGTH_LONG);
	}
	
	public static void showShortToast(final Context context, final int text) {
		showToast(context, text, LENGTH_SHORT);
	}
	
	public static void showLongToast(final Context context, final int text) {
		showToast(context, text, LENGTH_LONG);
	}
	
	public static void showToast(final Context context, final String text, final int duration) {
		Runnable r = new Runnable() {
			@Override
			public void run() {
				android.widget.Toast.makeText(context, text, duration).show();
			}
		};
		runOnAndroidUIThread(r);
	}
	
	public static void showToast(final Context context, final int text, final int duration) {
		Runnable r = new Runnable() {
			@Override
			public void run() {
				android.widget.Toast.makeText(context, text, duration).show();
			}
		};
		runOnAndroidUIThread(r);
	}
	
	private static void runOnAndroidUIThread(Runnable r) {
		if (Looper.myLooper() == sHandler.getLooper()) {
			r.run();
		} else {
			sHandler.post(r);
		}
	}
}
