package com.glview.util;

import android.graphics.Matrix;

import com.glview.graphics.Rect;
import com.glview.graphics.RectF;

public class MatrixUtil {
	
	/**
     * This class is uninstantiable.
     */
    private MatrixUtil() {
        // This space intentionally left blank.
    }

    /**
     * mapPoint用到的buffer，避免频繁申请
     * ThreadLocal解决多线程的线程安全问题
     */
	final static ThreadLocal<float[]> sMapPointsBuffer = new ThreadLocal<float[]>() {
		protected float[] initialValue() {
			return new float[2];
		};
	};
	
	public static float[] mapPoint(float[] m, float x, float y) {
		final float[] r = sMapPointsBuffer.get();
        return mapPoint(m, r, x, y);
	}
	
	public static float[] mapPoint(float[] m, float[] dst, float x, float y) {
		final float[] r = dst;
        // Multiply m and (x1 y1 0 1) to produce (x3 y3 z3 w3). z3 is unused.
        float x3 = m[0] * x + m[4] * y + m[12];
        float y3 = m[1] * x + m[5] * y + m[13];
        float w3 = m[3] * x + m[7] * y + m[15];
        r[0] = x3 / w3;
        r[1] = y3 / w3;
        return r;
	}
	
	/**
	 * {@link Matrix#mapRect(RectF, RectF)}
	 * @param m      the matrix
	 * @param src
	 * @param dest
	 * @return Boolean
	 */
	public static boolean mapRect(float[] m, RectF src, RectF dest) {
		if (src.isEmpty()) return false;
		float[] r = mapPoint(m, src.left, src.top);
		dest.left = r[0];
		dest.top = r[1];
		r = mapPoint(m, src.right, src.bottom);
		dest.right = r[0];
		dest.bottom = r[1];
		dest.sort();
		return true;
	}
	
	public static boolean mapRect(float[] m, RectF rect) {
		return mapRect(m, rect, rect);
	}
	
	/**
	 * {@link Matrix#mapRect(RectF, RectF)}
	 * @param m
	 * @param src
	 * @param dest
	 * @return Boolean
	 */
	public static boolean mapRect(float[] m, Rect src, Rect dest) {
		if (src.isEmpty()) return false;
		float[] r = mapPoint(m, src.left, src.top);
		dest.left = (int) (r[0] + 0.5);
		dest.top = (int) (r[1] + 0.5);
		r = mapPoint(m, src.right, src.bottom);
		dest.right = (int) (r[0] + 0.5);
		dest.bottom = (int) (r[1] + 0.5);
		dest.sort();
		return true;
	}
	
	public static boolean mapRect(float[] m, Rect rect) {
		return mapRect(m, rect, rect);
	}
	
}
