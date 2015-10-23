package com.glview.stackblur;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public interface BlurProcess {
	
	static final int EXECUTOR_THREADS = Runtime.getRuntime().availableProcessors();
	static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(EXECUTOR_THREADS);
	
    public byte[] blur(byte[] original, int w, int h, int stride, float radius);
    public int[] blur(int[] original, int w, int h, int stride, float radius);
}
