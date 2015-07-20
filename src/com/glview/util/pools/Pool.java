package com.glview.util.pools;

public interface Pool<T extends Poolable<T>> {
    public abstract T acquire();
    public abstract void release(T element);
}
