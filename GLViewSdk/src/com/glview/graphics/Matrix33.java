package com.glview.graphics;

public class Matrix33 {
	
	private static ThreadLocal<Matrix33> sThreadLocal = new ThreadLocal<Matrix33>() {
		protected Matrix33 initialValue() {
			return new Matrix33();
		};
	};
	private static ThreadLocal<Matrix33> sConcatTmp = new ThreadLocal<Matrix33>() {
		protected Matrix33 initialValue() {
			return new Matrix33();
		};
	};

	private final float[] fMat = new float[9];
	
	private final static int kMScaleX = 0;
	private final static int kMSkewX = 1;
	private final static int kMTransX = 2;
	private final static int kMSkewY = 3;
	private final static int kMScaleY = 4;
	private final static int kMTransY = 5;
	private final static int kMPersp0 = 6;
	private final static int kMPersp1 = 7;
	private final static int kMPersp2 = 8;
	
	public Matrix33() {
		reset();
	}
	
	private void loadIdentity() {
		fMat[kMScaleX] = fMat[kMScaleY] = fMat[kMPersp2] = 1;
	    fMat[kMSkewX]  = fMat[kMSkewY] =
	    fMat[kMTransX] = fMat[kMTransY] =
	    fMat[kMPersp0] = fMat[kMPersp1] = 0;
	}
	
	public void reset() {
		loadIdentity();
	}
	
	public void load(Matrix33 v) {
		System.arraycopy(v.fMat, 0, fMat, 0, fMat.length);
	}
	
	public void setConcat(Matrix33 a, Matrix33 b) {
		Matrix33 tmp = sConcatTmp.get();
		tmp.reset();
		
		tmp.fMat[kMScaleX] = rowcol3(a.fMat, 0, b.fMat, 0);
        tmp.fMat[kMSkewX]  = rowcol3(a.fMat, 0, b.fMat, 1);
        tmp.fMat[kMTransX] = rowcol3(a.fMat, 0, b.fMat, 2);
        tmp.fMat[kMSkewY]  = rowcol3(a.fMat, 3, b.fMat, 0);
        tmp.fMat[kMScaleY] = rowcol3(a.fMat, 3, b.fMat, 1);
        tmp.fMat[kMTransY] = rowcol3(a.fMat, 3, b.fMat, 2);
        tmp.fMat[kMPersp0] = rowcol3(a.fMat, 6, b.fMat, 0);
        tmp.fMat[kMPersp1] = rowcol3(a.fMat, 6, b.fMat, 1);
        tmp.fMat[kMPersp2] = rowcol3(a.fMat, 6, b.fMat, 2);

        normalize_perspective(tmp.fMat);
	    
	    load(tmp);
	}
	
	public void postConcat(Matrix33 mat) {
        this.setConcat(mat, this);
	}
	
	public void preConcat(Matrix33 mat) {
        this.setConcat(this, mat);
	}
	
	public void setScale(float sx, float sy, float px, float py) {
		if (1 == sx && 1 == sy) {
	        reset();
	    } else {
	    	fMat[kMScaleX] = sx;
	        fMat[kMScaleY] = sy;
	        fMat[kMTransX] = px - sx * px;
	        fMat[kMTransY] = py - sy * py;
	        fMat[kMPersp2] = 1;

	        fMat[kMSkewX]  = fMat[kMSkewY] =
	        fMat[kMPersp0] = fMat[kMPersp1] = 0;
	    }
	}
	
	public void setScale(float sx, float sy) {
		if (1 == sx && 1 == sy) {
	        reset();
	    } else {
	        fMat[kMScaleX] = sx;
	        fMat[kMScaleY] = sy;
	        fMat[kMPersp2] = 1;

	        fMat[kMTransX] = fMat[kMTransY] =
	        fMat[kMSkewX]  = fMat[kMSkewY] =
	        fMat[kMPersp0] = fMat[kMPersp1] = 0;
	    }
	}
	
	public void preScale(float sx, float sy) {
		if (1 == sx && 1 == sy) {
	        return;
	    }
		
		// the assumption is that these multiplies are very cheap, and that
	    // a full concat and/or just computing the matrix type is more expensive.
	    // Also, the fixed-point case checks for overflow, but the float doesn't,
	    // so we can get away with these blind multiplies.

	    fMat[kMScaleX] *= sx;
	    fMat[kMSkewY]  *= sx;
	    fMat[kMPersp0] *= sx;

	    fMat[kMSkewX]  *= sy;
	    fMat[kMScaleY] *= sy;
	    fMat[kMPersp1] *= sy;
	}
	
	public void preScale(float sx, float sy, float px, float py) {
		if (1 == sx && 1 == sy) {
	        return;
	    }
		
		Matrix33 m = sThreadLocal.get();
		m.setScale(sx, sy, px, py);
		preConcat(m);
	}
	
	public void postScale(float sx, float sy, float px, float py) {
		if (1 == sx && 1 == sy) {
	        return;
	    }
		Matrix33 m = sThreadLocal.get();
		m.setScale(sx, sy, px, py);
		postConcat(m);
	}
	
	public void postScale(float sx, float sy) {
		if (1 == sx && 1 == sy) {
	        return;
	    }
		Matrix33 m = sThreadLocal.get();
		m.setScale(sx, sy);
		postConcat(m);
	}
	
	public void setTranslate(float dx, float dy) {
		if (dx != 0 || dy != 0) {
	        fMat[kMTransX] = dx;
	        fMat[kMTransY] = dy;

	        fMat[kMScaleX] = fMat[kMScaleY] = fMat[kMPersp2] = 1;
	        fMat[kMSkewX]  = fMat[kMSkewY] =
	        fMat[kMPersp0] = fMat[kMPersp1] = 0;

	    } else {
	        reset();
	    }
	}
	
	public void preTranslate(float dx, float dy) {
		if (dx == 0 && dy == 0) {
			return;
		}
		Matrix33 m = sThreadLocal.get();
        m.setTranslate(dx, dy);
        preConcat(m);
	}
	
	public void postTranslate(float dx, float dy) {
		if (dx == 0 && dy == 0) {
			return;
		}
		Matrix33 m = sThreadLocal.get();
        m.setTranslate(dx, dy);
        postConcat(m);
	}
	
	public void setSinCos(float sinV, float cosV) {
		fMat[kMScaleX]  = cosV;
	    fMat[kMSkewX]   = -sinV;
	    fMat[kMTransX]  = 0;

	    fMat[kMSkewY]   = sinV;
	    fMat[kMScaleY]  = cosV;
	    fMat[kMTransY]  = 0;

	    fMat[kMPersp0] = fMat[kMPersp1] = 0;
	    fMat[kMPersp2] = 1;
	}
	
	public void setSinCos(float sinV, float cosV, float px, float py) {
		float oneMinusCosV = 1 - cosV;

	    fMat[kMScaleX]  = cosV;
	    fMat[kMSkewX]   = -sinV;
	    fMat[kMTransX]  = sdot(sinV, py, oneMinusCosV, px);

	    fMat[kMSkewY]   = sinV;
	    fMat[kMScaleY]  = cosV;
	    fMat[kMTransY]  = sdot(-sinV, px, oneMinusCosV, py);

	    fMat[kMPersp0] = fMat[kMPersp1] = 0;
	    fMat[kMPersp2] = 1;
	}
	
	public void setRotate(float degrees, float px, float py) {
		float a = degrees * (float) (Math.PI / 180.0f);
		float sinV = (float) Math.sin(a);
		float cosV = (float) Math.cos(a);
		setSinCos(sinV, cosV, px, py);
	}
	
	public void setRotate(float degrees) {
		float a = degrees * (float) (Math.PI / 180.0f);
        float sinV = (float) Math.sin(a);
        float cosV = (float) Math.cos(a);
        setSinCos(sinV, cosV);
	}
	
	public void preRotate(float degrees, float px, float py) {
		Matrix33 m = sThreadLocal.get();
	    m.setRotate(degrees, px, py);
	    preConcat(m);
	}

	public void preRotate(float degrees) {
		Matrix33 m = sThreadLocal.get();
	    m.setRotate(degrees);
	    preConcat(m);
	}

	public void postRotate(float degrees, float px, float py) {
		Matrix33 m = sThreadLocal.get();
	    m.setRotate(degrees, px, py);
	    postConcat(m);
	}

	public void postRotate(float degrees) {
		Matrix33 m = sThreadLocal.get();
	    m.setRotate(degrees);
	    postConcat(m);
	}
	
	public void setSkew(float sx, float sy) {
	    fMat[kMScaleX]  = 1;
	    fMat[kMSkewX]   = sx;
	    fMat[kMTransX]  = 0;

	    fMat[kMSkewY]   = sy;
	    fMat[kMScaleY]  = 1;
	    fMat[kMTransY]  = 0;

	    fMat[kMPersp0] = fMat[kMPersp1] = 0;
	    fMat[kMPersp2] = 1;
	}
	
	public void setSkew(float sx, float sy, float px, float py) {
	    fMat[kMScaleX]  = 1;
	    fMat[kMSkewX]   = sx;
	    fMat[kMTransX]  = -sx * py;

	    fMat[kMSkewY]   = sy;
	    fMat[kMScaleY]  = 1;
	    fMat[kMTransY]  = -sy * px;

	    fMat[kMPersp0] = fMat[kMPersp1] = 0;
	    fMat[kMPersp2] = 1;
	}
	
	public void preSkew(float sx, float sy, float px, float py) {
		Matrix33 m = sThreadLocal.get();
	    m.setSkew(sx, sy, px, py);
	    preConcat(m);
	}

	public void preSkew(float sx, float sy) {
		Matrix33 m = sThreadLocal.get();
	    m.setSkew(sx, sy);
	    preConcat(m);
	}

	public void postSkew(float sx, float sy, float px, float py) {
		Matrix33 m = sThreadLocal.get();
	    m.setSkew(sx, sy, px, py);
	    postConcat(m);
	}

	public void postSkew(float sx, float sy) {
		Matrix33 m = sThreadLocal.get();
	    m.setSkew(sx, sy);
	    postConcat(m);
	}
	
	static float muladdmul(float a, float b, float c, float d) {
	    return a * b + c * d;
	}
	
	static float sdot(float a, float b, float c, float d) {
	    return a * b + c * d;
	}

	static float sdot(float a, float b, float c, float d,
			float e, float f) {
	    return a * b + c * d + e * f;
	}

	static float scross(float a, float b, float c, float d) {
	    return a * b - c * d;
	}
	
	static float rowcol3(float row[], int rOffset, float col[], int cOffset) {
	    return row[rOffset + 0] * col[cOffset + 0] + row[rOffset + 1] * col[cOffset + 3] + row[rOffset + 2] * col[cOffset + 6];
	}
	
	static void normalize_perspective(float[] mat) {
	    if (Math.abs(mat[kMPersp2]) > 1) {
	        for (int i = 0; i < 9; i++)
	            mat[i] = mat[i] * 0.5f;
	    }
	}
	
	public void mapPoints(PointF[] pts, int length) {
		if (pts != null) {
			for (int i = 0; i < pts.length && i < length; i ++) {
				PointF pt = pts[i];
				mapPoint(pt, pt.x, pt.y);
			}
		}
	}
	
	public PointF mapPoint(PointF dst, float x, float y) {
	    float dx = x * fMat[kMScaleX] + y * fMat[kMSkewX] + fMat[kMTransX];
	    float dy = x * fMat[kMSkewY] + y * fMat[kMScaleY] + fMat[kMTransY];
	    float dz = x * fMat[kMPersp0] + y * fMat[kMPersp1] + fMat[kMPersp2];
	    if (dz != 0) dz = 1.0f / dz;

	    dst.x = dx * dz;
	    dst.y = dy * dz;
	    return dst;
	}
}
