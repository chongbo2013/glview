package com.glview.util.pools;

import android.util.Log;

/**
 * @hide
 */
class FinitePool<T extends Poolable<T>> implements Pool<T> {
    private static final String LOG_TAG = "FinitePool";

    /**
     * Factory used to create new pool objects
     */
    private final PoolableManager<T> mManager;
    /**
     * Maximum number of objects in the pool
     */
    private final int mLimit;
    /**
     * If true, mLimit is ignored
     */
    private final boolean mInfinite;

    /**
     * Next object to acquire
     */
    private T mRoot;
    /**
     * Number of objects in the pool
     */
    private int mPoolCount;

    FinitePool(PoolableManager<T> manager) {
        mManager = manager;
        mLimit = 0;
        mInfinite = true;
    }

    FinitePool(PoolableManager<T> manager, int limit) {
        if (limit <= 0) throw new IllegalArgumentException("The pool limit must be > 0");

        mManager = manager;
        mLimit = limit;
        mInfinite = false;
    }

    public T acquire() {
        T element;

        if (mRoot != null) {
            element = mRoot;
            mRoot = element.getNextPoolable();
            mPoolCount--;
        } else {
            element = mManager.newInstance();
        }

        if (element != null) {
            element.setNextPoolable(null);
            element.setPooled(false);
            mManager.onAcquired(element);            
        }

        return element;
    }

    public void release(T element) {
        if (!element.isPooled()) {
            if (mInfinite || mPoolCount < mLimit) {
                mPoolCount++;
                element.setNextPoolable(mRoot);
                element.setPooled(true);
                mRoot = element;
            }
            mManager.onReleased(element);
        } else {
            Log.w(LOG_TAG, "Element is already in pool: " + element);
        }
    }
}
