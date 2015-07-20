package com.glview.util;

import android.util.Log;

public class FPSUtils {
	
	Object mTarget;
	
	public FPSUtils(Object target) {
		mTarget = target;
	}
	
	int mTotalCount;
	int mSecondCount = 0;
	int mFrameCount = 0;
	long mFrameCountingStart = 0;
    
    public void fps() {
        long now = System.nanoTime();
        ++mFrameCount;
        if (mFrameCountingStart == 0) {
            mFrameCountingStart = now;
        } else if ((now - mFrameCountingStart) > 1000000000) {
            Log.d("FPS", "fps: " + (double) mFrameCount
                    * 1000000000 / (now - mFrameCountingStart) + ", target:" + mTarget + ", tid=" + Thread.currentThread().getId());
            mFrameCountingStart = now;
            mFrameCount = 0;
            
            Log.d("FPS", "mTotalCount: " + mTotalCount + ", mSecondCount: " + (++ mSecondCount));
        }
        ++mTotalCount;
	}
}
