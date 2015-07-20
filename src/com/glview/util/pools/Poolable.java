package com.glview.util.pools;

public interface Poolable<T> {
    void setNextPoolable(T element);
    T getNextPoolable();
    boolean isPooled();
    void setPooled(boolean isPooled);
}
