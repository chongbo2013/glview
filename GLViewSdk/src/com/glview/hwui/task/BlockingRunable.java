package com.glview.hwui.task;

import android.os.SystemClock;



class BlockingRunable implements Runnable {
	
	// sometimes we store linked lists of these things
    /*package*/ BlockingRunable next;

    private static final Object sPoolSync = new Object();
    private static BlockingRunable sPool;
    private static int sPoolSize = 0;

    private static final int MAX_POOL_SIZE = 50;
    
    private Task task;
    private boolean done;
    
    static BlockingRunable obtain() {
        synchronized (sPoolSync) {
            if (sPool != null) {
            	BlockingRunable m = sPool;
                sPool = m.next;
                m.next = null;
                sPoolSize--;
                return m;
            }
        }
        return new BlockingRunable();
    }

    public static BlockingRunable obtain(Task task) {
    	if (task == null) {
            throw new IllegalArgumentException("runnable must not be null");
        }
    	BlockingRunable m = obtain();
    	m.done = false;
    	m.task = task;
        return m;
    }
    
    /**
     * Return a Message instance to the global pool.
     * <p>
     * You MUST NOT touch the Message after calling this function because it has
     * effectively been freed.  It is an error to recycle a message that is currently
     * enqueued or that is in the process of being delivered to a Handler.
     * </p>
     */
    public void recycle() {
    	done = false;
    	task = null;
    	synchronized (sPoolSync) {
            if (sPoolSize < MAX_POOL_SIZE) {
                next = sPool;
                sPool = this;
                sPoolSize++;
            }
        }
    }

    @Override
    public void run() {
        try {
            task.run();
        } finally {
            synchronized (this) {
            	done = true;
                notifyAll();
            }
        }
    }

    public boolean postAndWait(TaskHandler handler, long timeout) {
        if (!handler.post(this)) {
            return false;
        }

        synchronized (this) {
            if (timeout > 0) {
                final long expirationTime = SystemClock.uptimeMillis() + timeout;
                while (!done) {
                    long delay = expirationTime - SystemClock.uptimeMillis();
                    if (delay <= 0) {
                        return false; // timeout
                    }
                    try {
                        wait(delay);
                    } catch (InterruptedException ex) {
                    }
                }
            } else {
                while (!done) {
                    try {
                        wait();
                    } catch (InterruptedException ex) {
                    }
                }
            }
        }
        return true;
    }

}
