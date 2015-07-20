package com.glview.util.pools;


class SynchronizedPool<T extends Poolable<T>> implements Pool<T> {
    private final Pool<T> mPool;
    private final Object mLock;

    public SynchronizedPool(Pool<T> pool) {
        mPool = pool;
        mLock = this;
    }

    public SynchronizedPool(Pool<T> pool, Object lock) {
        mPool = pool;
        mLock = lock;
    }

    public T acquire() {
        synchronized (mLock) {
            return mPool.acquire();
        }
    }

    public void release(T element) {
        synchronized (mLock) {
            mPool.release(element);
        }
    }
}
