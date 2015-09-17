package com.glview.hwui.task;


public abstract class Task implements Runnable {
	
	public Task() {
	}
	
	@Override
	public final void run() {
		doTask();
	}
	
	abstract public void doTask();
}
