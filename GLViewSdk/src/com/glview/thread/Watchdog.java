package com.glview.thread;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import android.os.Process;
import android.os.SystemClock;
import android.util.Log;

public class Watchdog extends Thread {
	
	static final String TAG = "GLWatchdog";
	
	// Set this to true to use debug default values.
    static final boolean DB = false;

    static final long DEFAULT_TIMEOUT = DB ? 10*1000 : 60*1000;
    static final long CHECK_INTERVAL = DEFAULT_TIMEOUT / 2;
    
	// These are temporally ordered: larger values as lateness increases
    static final int COMPLETED = 0;
    static final int WAITING = 1;
    static final int WAITED_HALF = 2;
    static final int OVERDUE = 3;
	
	static Watchdog sWatchdog;
	
	public static Watchdog getInstance() {
        if (sWatchdog == null) {
            sWatchdog = new Watchdog();
            sWatchdog.start();
        }

        return sWatchdog;
    }
	
	/* This handler will be used to post message back onto the main thread */
    final HashMap<Object, HandlerChecker> mHandlerCheckers = new HashMap<Object, HandlerChecker>();
	
	/**
     * Used for checking status of handle threads and scheduling monitor callbacks.
     */
    public final class HandlerChecker implements Runnable {
    	
    	private final Handler mHandler;
        private final String mName;
        private final long mWaitMax;
        private final ArrayList<Monitor> mMonitors = new ArrayList<Monitor>();
        private boolean mCompleted;
        private Monitor mCurrentMonitor;
        private long mStartTime;

        HandlerChecker(Handler handler, String name, long waitMaxMillis) {
            mHandler = handler;
            mName = name;
            mWaitMax = waitMaxMillis;
            mCompleted = true;
        }
        
        public void scheduleCheckLocked() {
            if (mMonitors.size() == 0) {
            	try {
            		Method m = mHandler.getLooper().getClass().getDeclaredMethod("isIdling");
            		m.setAccessible(true);
            		boolean isIdling = (Boolean)m.invoke(mHandler.getLooper());
            		if (isIdling) {
            			// If the target looper is or just recently was idling, then
                        // there is no reason to enqueue our checker on it since that
                        // is as good as it not being deadlocked.  This avoid having
                        // to do a context switch to check the thread.  Note that we
                        // only do this if mCheckReboot is false and we have no
                        // monitors, since those would need to be executed at this point.
                        mCompleted = true;
                        return;
            		}
            	} catch (Throwable throwable) {
            	}
            }

            if (!mCompleted) {
                // we already have a check in flight, so no need
                return;
            }

            mCompleted = false;
            mCurrentMonitor = null;
            mStartTime = SystemClock.uptimeMillis();
            mHandler.removeCallbacks(this);
            mHandler.postAtFrontOfQueue(this);
        }
        
        public boolean isOverdueLocked() {
            return (!mCompleted) && (SystemClock.uptimeMillis() > mStartTime + mWaitMax);
        }
        
        public int getCompletionStateLocked() {
            if (mCompleted) {
                return COMPLETED;
            } else {
                long latency = SystemClock.uptimeMillis() - mStartTime;
                if (latency < mWaitMax/2) {
                    return WAITING;
                } else if (latency < mWaitMax) {
                    return WAITED_HALF;
                }
            }
            return OVERDUE;
        }

        public Thread getThread() {
            return mHandler.getLooper().getThread();
        }

        public String getName() {
            return mName;
        }
        
        public String describeBlockedStateLocked() {
            if (mCurrentMonitor == null) {
                return "Blocked in handler on " + mName + " (" + getThread().getName() + ")";
            } else {
                return "Blocked in monitor " + mCurrentMonitor.getClass().getName()
                        + " on " + mName + " (" + getThread().getName() + ")";
            }
        }

        @Override
        public void run() {
            final int size = mMonitors.size();
            for (int i = 0 ; i < size ; i++) {
                synchronized (Watchdog.this) {
                    mCurrentMonitor = mMonitors.get(i);
                }
                mCurrentMonitor.monitor();
            }

            synchronized (Watchdog.this) {
                mCompleted = true;
                mCurrentMonitor = null;
            }
        }
    	
    }
    
    public interface Monitor {
        void monitor();
    }
    
    private Watchdog() {
    	super("GLWatchdog");
    	setDaemon(true);
    }
    
    public void addThread(Object token, Handler thread, String name) {
        addThread(token, thread, name, DEFAULT_TIMEOUT);
    }

    public void addThread(Object token, Handler thread, String name, long timeoutMillis) {
        synchronized (this) {
            /*if (isAlive()) {
                throw new RuntimeException("Threads can't be added once the Watchdog is running");
            }*/
            mHandlerCheckers.put(token, new HandlerChecker(thread, name, timeoutMillis));
        }
    }
    
    public void removeThread(Object token) {
    	synchronized (this) {
    		HandlerChecker handlerChecker = mHandlerCheckers.remove(token);
    		if (handlerChecker != null) {
    			handlerChecker.mHandler.removeCallbacksAndMessages(null);
    		}
    	}
    }
    
    private int evaluateCheckerCompletionLocked() {
        int state = COMPLETED;
        for (Entry<Object, HandlerChecker> entry : mHandlerCheckers.entrySet()) {
            HandlerChecker hc = entry.getValue();
            state = Math.max(state, hc.getCompletionStateLocked());
        }
        return state;
    }

    private ArrayList<HandlerChecker> getBlockedCheckersLocked() {
        ArrayList<HandlerChecker> checkers = new ArrayList<HandlerChecker>();
        for (Entry<Object, HandlerChecker> entry : mHandlerCheckers.entrySet()) {
            HandlerChecker hc = entry.getValue();
            if (hc.isOverdueLocked()) {
                checkers.add(hc);
            }
        }
        return checkers;
    }

    private String describeCheckersLocked(ArrayList<HandlerChecker> checkers) {
        StringBuilder builder = new StringBuilder(128);
        for (int i=0; i<checkers.size(); i++) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(checkers.get(i).describeBlockedStateLocked());
        }
        return builder.toString();
    }
    
    @Override
    public void run() {
        boolean waitedHalf = false;
        while (true) {
            final ArrayList<HandlerChecker> blockedCheckers;
            final String subject;
            synchronized (this) {
                long timeout = CHECK_INTERVAL;
                // Make sure we (re)spin the checkers that have become idle within
                // this wait-and-check interval
                for (Entry<Object, HandlerChecker> entry : mHandlerCheckers.entrySet()) {
                    HandlerChecker hc = entry.getValue();
                    hc.scheduleCheckLocked();
                }

                // NOTE: We use uptimeMillis() here because we do not want to increment the time we
                // wait while asleep. If the device is asleep then the thing that we are waiting
                // to timeout on is asleep as well and won't have a chance to run, causing a false
                // positive on when to kill things.
                long start = SystemClock.uptimeMillis();
                while (timeout > 0) {
                    try {
                        wait(timeout);
                    } catch (InterruptedException e) {
                        Log.wtf(TAG, e);
                    }
                    timeout = CHECK_INTERVAL - (SystemClock.uptimeMillis() - start);
                }

                final int waitState = evaluateCheckerCompletionLocked();
                if (waitState == COMPLETED) {
                    // The monitors have returned; reset
                    waitedHalf = false;
                    continue;
                } else if (waitState == WAITING) {
                    // still waiting but within their configured intervals; back off and recheck
                    continue;
                } else if (waitState == WAITED_HALF) {
                    if (!waitedHalf) {
                        // We've waited half the deadlock-detection interval.  Pull a stack
                        // trace and wait another half.
                    	dumpStackTraces();
                        waitedHalf = true;
                    }
                    continue;
                }

                // something is overdue!
                blockedCheckers = getBlockedCheckersLocked();
                subject = describeCheckersLocked(blockedCheckers);
                Log.wtf(TAG, subject);
            }

            dumpStackTraces();
            
            // Give some extra time to make sure the stack traces get written.
            // The system's been hanging for a minute, another second or two won't hurt much.
            SystemClock.sleep(2000);

            // FIXME
            if (false) {
                Process.killProcess(Process.myPid());
                System.exit(10);
            }

            waitedHalf = false;
        }
    }
    
    void dumpStackTraces() {
    	try {
    		Map<Thread, StackTraceElement[]> stackTraces = Thread.getAllStackTraces();
    		for (Entry<Thread, StackTraceElement[]> entry : stackTraces.entrySet()) {
    			Thread t = entry.getKey();
    			StackTraceElement[] elements = entry.getValue();
    			Log.w(TAG, "Thread:" + t.getName() + "----------state:" + t.getState() + "---------------priority:" + t.getPriority());
    			for (StackTraceElement element : elements) {
    				Log.w(TAG, "\t" + element.getClassName() + "." + element.getMethodName() + "(" + element.getLineNumber() + ")");
    			}
    			Log.w(TAG, "\n");
    		}
    	} catch (Throwable throwable) {
    		Log.w(TAG, "in dumpStackTraces", throwable);
    	}
    }

}
