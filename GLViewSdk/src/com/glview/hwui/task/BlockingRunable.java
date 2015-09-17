package com.glview.hwui.task;

import com.glview.thread.Looper;

import android.os.SystemClock;


/**
 * Runs the specified task synchronously.
 * <p>
 * If the current thread is the same as the handler thread, then the runnable
 * runs immediately without being enqueued.  Otherwise, posts the runnable
 * to the handler and waits for it to complete before returning.
 * </p><p>
 * This method is dangerous!  Improper use can result in deadlocks.
 * Never call this method while any locks are held or use it in a
 * possibly re-entrant manner.
 * </p><p>
 * This method is occasionally useful in situations where a background thread
 * must synchronously await completion of a task that must run on the
 * handler's thread.  However, this problem is often a symptom of bad design.
 * Consider improving the design (if possible) before resorting to this method.
 * </p><p>
 * One example of where you might want to use this method is when you just
 * set up a Handler thread and need to perform some initialization steps on
 * it before continuing execution.
 * </p><p>
 * If timeout occurs then this method returns <code>false</code> but the runnable
 * will remain posted on the handler and may already be in progress or
 * complete at a later time.
 * </p><p>
 * When using this method, be sure to use {@link Looper#quitSafely} when
 * quitting the looper.  Otherwise {@link #runWithScissors} may hang indefinitely.
 * (TODO: We should fix this by making MessageQueue aware of blocking runnables.)
 * </p>
 *
 * @param task The Runnable that will be executed synchronously.
 *
 */
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
            throw new IllegalArgumentException("task must not be null");
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

        return waitForTaskDone(timeout);
    }
    
    public boolean postAtFrontOfQueueAndWait(TaskHandler handler, long timeout) {
        if (!handler.postAtFrontOfQueue(this)) {
            return false;
        }

        return waitForTaskDone(timeout);
    }
    
    private boolean waitForTaskDone(long timeout) {
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
