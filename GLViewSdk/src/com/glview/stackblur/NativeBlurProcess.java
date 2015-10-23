package com.glview.stackblur;

import java.util.ArrayList;
import java.util.concurrent.Callable;

import com.glview.blur.Blur;

/**
 * Blur using Java code.
 *
 * This is a compromise between Gaussian Blur and Box blur
 * It creates much better looking blurs than Box Blur, but is
 * 7x faster than my Gaussian Blur implementation.

 * I called it Stack Blur because this describes best how this
 * filter works internally: it creates a kind of moving stack
 * of colors whilst scanning through the image. Thereby it
 * just has to add one new block of color to the right side
 * of the stack and remove the leftmost color. The remaining
 * colors on the topmost layer of the stack are either added on
 * or reduced by one, depending on if they are on the right or
 * on the left side of the stack.
 *
 * @author Enrique López Mañas <eenriquelopez@gmail.com>
 * http://www.neo-tech.es
 *
 * Author of the original algorithm: Mario Klingemann <mario.quasimondo.com>
 *
 * Based heavily on http://vitiy.info/Code/stackblur.cpp
 * See http://vitiy.info/stackblur-algorithm-multi-threaded-blur-for-cpp/
 *
 * @copyright: Enrique López Mañas
 * @license: Apache License 2.0
 */
public class NativeBlurProcess implements BlurProcess {
	
	@Override
	public byte[] blur(byte[] original, int w, int h, int stride, float radius) {
		if (stride < w) stride = w;
		byte[] currentPixels = original;
		int cores = EXECUTOR_THREADS;

		ArrayList<A8BlurTask> horizontal = new ArrayList<A8BlurTask>(cores);
		ArrayList<A8BlurTask> vertical = new ArrayList<A8BlurTask>(cores);
		for (int i = 0; i < cores; i++) {
			horizontal.add(new A8BlurTask(currentPixels, w, h, stride, (int) radius, cores, i, 1));
			vertical.add(new A8BlurTask(currentPixels, w, h, stride, (int) radius, cores, i, 2));
		}

		try {
			EXECUTOR.invokeAll(horizontal);
		} catch (InterruptedException e) {
			return null;
		}

		try {
			EXECUTOR.invokeAll(vertical);
		} catch (InterruptedException e) {
			return null;
		}
		return currentPixels;
	}
	
	@Override
	public int[] blur(int[] original, int w, int h, int stride, float radius) {
		int[] currentPixels = new int[w * h];
		System.arraycopy(original, 0, currentPixels, 0, currentPixels.length);
		int cores = EXECUTOR_THREADS;

		ArrayList<RGBABlurTask> horizontal = new ArrayList<RGBABlurTask>(cores);
		ArrayList<RGBABlurTask> vertical = new ArrayList<RGBABlurTask>(cores);
		for (int i = 0; i < cores; i++) {
			horizontal.add(new RGBABlurTask(currentPixels, w, h, (int) radius, cores, i, 1));
			vertical.add(new RGBABlurTask(currentPixels, w, h, (int) radius, cores, i, 2));
		}

		try {
			EXECUTOR.invokeAll(horizontal);
		} catch (InterruptedException e) {
			return null;
		}

		try {
			EXECUTOR.invokeAll(vertical);
		} catch (InterruptedException e) {
			return null;
		}

		return currentPixels;
	}
	
	private static class RGBABlurTask implements Callable<Void> {
		private final int[] _src;
		private final int _w;
		private final int _h;
		private final int _radius;
		private final int _totalCores;
		private final int _coreIndex;
		private final int _round;

		public RGBABlurTask(int[] src, int w, int h, int radius, int totalCores, int coreIndex, int round) {
			_src = src;
			_w = w;
			_h = h;
			_radius = radius;
			_totalCores = totalCores;
			_coreIndex = coreIndex;
			_round = round;
		}

		@Override public Void call() throws Exception {
//			blurIteration(_src, _w, _h, _radius, _totalCores, _coreIndex, _round);
			return null;
		}

	}
	
	private static class A8BlurTask implements Callable<Void> {
		private final byte[] _src;
		private final int _w;
		private final int _h;
		private final int _stride;
		private final int _radius;
		private final int _totalCores;
		private final int _coreIndex;
		private final int _round;

		public A8BlurTask(byte[] src, int w, int h, int stride, int radius, int totalCores, int coreIndex, int round) {
			_src = src;
			_w = w;
			_h = h;
			_stride = stride;
			_radius = radius;
			_totalCores = totalCores;
			_coreIndex = coreIndex;
			_round = round;
		}

		@Override public Void call() throws Exception {
			Blur.blur(_src, _w, _h, _stride, _radius, _totalCores, _coreIndex, _round);
			return null;
		}

	}
}
