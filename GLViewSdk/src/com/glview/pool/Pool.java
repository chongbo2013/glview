package com.glview.pool;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Pool<T extends Poolable> {
	
	private boolean mMultiple;
	private int mMaxPoolSize = -1;
	
	private Map<Class<? extends Poolable>, List<T>> mPoll = new HashMap<Class<? extends Poolable>, List<T>>();
	
	private List<T> mSinglePool;
	
	public Pool(boolean multiple) {
		this(multiple, -1);
	}
	
	public Pool(boolean multiple, int maxPoolSize) {
		mMultiple = multiple;
		mMaxPoolSize = maxPoolSize;
	}
	
	public synchronized Poolable poll(Class<? extends Poolable> cls) {
		return poll(cls, true);
	}

	public synchronized Poolable poll(Class<? extends Poolable> cls, boolean autoCreate) {
		List<T> l = ensureTypeList(cls);
		Poolable poolable = l.size() > 0 ? l.remove(l.size() - 1) : null;
		if (poolable == null && autoCreate) {
			poolable = create(cls);
		}
		return poolable;
	}
	
	public synchronized boolean push(T poolable) {
		if (poolable == null) return false;
		List<T> l = ensureTypeList(poolable.getClass());
		if (mMaxPoolSize > 0 && l.size() >= mMaxPoolSize) {
			return false;
		}
		l.add(poolable);
		return true;
	}
	
	public synchronized void recycle() {
		mPoll.clear();
	}
	
	private List<T> ensureTypeList(Class<? extends Poolable> cls) {
		if (!mMultiple) {
			if (mSinglePool == null) {
				if (mMaxPoolSize > 0) {
					mSinglePool = new ArrayList<T>(mMaxPoolSize);
				} else {
					mSinglePool = new ArrayList<T>();
				}
			}
			return mSinglePool;
		}
		List<T> l = mPoll.get(cls);
		if (l == null) {
			if (mMaxPoolSize > 0) {
				l = new ArrayList<T>(mMaxPoolSize);
			} else {
				l = new ArrayList<T>();
			}
			mPoll.put(cls, l);
		}
		return l;
	}
	
	private Poolable create(Class<? extends Poolable> cls) {
		try {
			Constructor<?> c = cls.getConstructor();
			c.setAccessible(true);
			return (Poolable) c.newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}
}
