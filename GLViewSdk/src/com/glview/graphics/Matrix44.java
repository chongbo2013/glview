package com.glview.graphics;

public class Matrix44 {
	
	private static ThreadLocal<Matrix44> sThreadLocal = new ThreadLocal<Matrix44>() {
		protected Matrix44 initialValue() {
			return new Matrix44();
		};
	};
	private static ThreadLocal<Matrix44> sConcatTmp = new ThreadLocal<Matrix44>() {
		protected Matrix44 initialValue() {
			return new Matrix44();
		};
	};

	private final float[] data = new float[16];
	
	private final static int kScaleX = 0;
	private final static int kSkewY = 1;
	private final static int kPerspective0 = 3;
	private final static int kSkewX = 4;
	private final static int kScaleY = 5;
	private final static int kPerspective1 = 7;
	private final static int kScaleZ = 10;
	private final static int kTranslateX = 12;
	private final static int kTranslateY = 13;
	private final static int kTranslateZ = 14;
	private final static int kPerspective2 = 15;
	
	public Matrix44() {
		reset();
	}
	
	private void loadIdentity() {
		data[kScaleX]       = 1.0f;
	    data[kSkewY]        = 0.0f;
	    data[2]             = 0.0f;
	    data[kPerspective0] = 0.0f;

	    data[kSkewX]        = 0.0f;
	    data[kScaleY]       = 1.0f;
	    data[6]             = 0.0f;
	    data[kPerspective1] = 0.0f;

	    data[8]             = 0.0f;
	    data[9]             = 0.0f;
	    data[kScaleZ]       = 1.0f;
	    data[11]            = 0.0f;

	    data[kTranslateX]   = 0.0f;
	    data[kTranslateY]   = 0.0f;
	    data[kTranslateZ]   = 0.0f;
	    data[kPerspective2] = 1.0f;
	}
	
	private float get(int i, int j) {
        return data[i * 4 + j];
    }

	private void set(int i, int j, float v) {
        data[i * 4 + j] = v;
    }
	
	public void reset() {
		loadIdentity();
	}
	
	public void load(Matrix44 v) {
		System.arraycopy(v.data, 0, data, 0, data.length);
	}
	
	public void setConcat(Matrix44 a, Matrix44 b) {
		Matrix44 tmp = sConcatTmp.get();
		tmp.reset();
		
	    for (int i = 0 ; i < 4 ; i++) {
	        float x = 0;
	        float y = 0;
	        float z = 0;
	        float w = 0;

	        for (int j = 0 ; j < 4 ; j++) {
	            float e = a.get(i, j);
	            x += b.get(j, 0) * e;
	            y += b.get(j, 1) * e;
	            z += b.get(j, 2) * e;
	            w += b.get(j, 3) * e;
	        }

	        tmp.set(i, 0, x);
	        tmp.set(i, 1, y);
	        tmp.set(i, 2, z);
	        tmp.set(i, 3, w);
	    }
	    load(tmp);
	}
	
	public void postConcat(Matrix44 mat) {
        this.setConcat(mat, this);
	}
	
	public void preConcat(Matrix44 mat) {
        this.setConcat(this, mat);
	}
	
	public void setScale(float sx, float sy, float sz) {
		loadIdentity();

	    data[kScaleX] = sx;
	    data[kScaleY] = sy;
	    data[kScaleZ] = sz;
	}
	
	public void preScale(float sx, float sy, float sz) {
		if (1 == sx && 1 == sy && 1 == sz) {
	        return;
	    }
		for (int i = 0 ; i < 4 ; i ++) {
			data[     i] *= sx;
			data[ 4 + i] *= sy;
			data[ 8 + i] *= sz;
        }
	}
	
	public void postScale(float sx, float sy, float sz) {
		if (1 == sx && 1 == sy && 1 == sz) {
	        return;
	    }
		Matrix44 m = sThreadLocal.get();
		m.setScale(sx, sy, sz);
		postConcat(m);
	}
	
	public void setTranslate(float dx, float dy, float dz) {
		loadIdentity();
		
		data[kTranslateX] = dx;
        data[kTranslateY] = dy;
        data[kTranslateZ] = dz;
	}
	
	public void preTranslate(float dx, float dy, float dz) {
		if (dx == 0 && dy == 0 && dz == 0) {
			return;
		}
		Matrix44 m = sThreadLocal.get();
        m.setTranslate(dx, dy, dz);
        preConcat(m);
	}
	
	public void postTranslate(float dx, float dy, float dz) {
		if (dx == 0 && dy == 0 && dz == 0) {
			return;
		}
		Matrix44 m = sThreadLocal.get();
        m.setTranslate(dx, dy, dz);
        postConcat(m);
	}
	
	public void setRotate(float degrees) {
		setRotate(degrees, 0, 0, 1);
	}
	
	public void setRotate(float a, float x, float y, float z) {
		data[3] = 0;
        data[7] = 0;
        data[11]= 0;
        data[12]= 0;
        data[13]= 0;
        data[14]= 0;
        data[15]= 1;
        a *= (float) (Math.PI / 180.0f);
        float s = (float) Math.sin(a);
        float c = (float) Math.cos(a);
        if (1.0f == x && 0.0f == y && 0.0f == z) {
            data[5] = c;   data[10]= c;
            data[6] = s;   data[9] = -s;
            data[1] = 0;   data[2] = 0;
            data[4] = 0;   data[8] = 0;
            data[0] = 1;
        } else if (0.0f == x && 1.0f == y && 0.0f == z) {
            data[0] = c;   data[10]= c;
            data[8] = s;   data[2] = -s;
            data[1] = 0;   data[4] = 0;
            data[6] = 0;   data[9] = 0;
            data[5] = 1;
        } else if (0.0f == x && 0.0f == y && 1.0f == z) {
            data[0] = c;   data[5] = c;
            data[1] = s;   data[4] = -s;
            data[2] = 0;   data[6] = 0;
            data[8] = 0;   data[9] = 0;
            data[10]= 1;
        } else {
            float len = length(x, y, z);
            if (1.0f != len) {
                float recipLen = 1.0f / len;
                x *= recipLen;
                y *= recipLen;
                z *= recipLen;
            }
            float nc = 1.0f - c;
            float xy = x * y;
            float yz = y * z;
            float zx = z * x;
            float xs = x * s;
            float ys = y * s;
            float zs = z * s;
            data[ 0] = x*x*nc +  c;
            data[ 4] =  xy*nc - zs;
            data[ 8] =  zx*nc + ys;
            data[ 1] =  xy*nc + zs;
            data[ 5] = y*y*nc +  c;
            data[ 9] =  yz*nc - xs;
            data[ 2] =  zx*nc - ys;
            data[ 6] =  yz*nc + xs;
            data[10] = z*z*nc +  c;
        }
	}
	
	public void preRotate(float degrees) {
		Matrix44 m = sThreadLocal.get();
		m.setRotate(degrees);
		preConcat(m);
	}
	
	public void postRotate(float degrees) {
		Matrix44 m = sThreadLocal.get();
		m.setRotate(degrees);
		postConcat(m);
	}
	
	public void preRotate(float a, float x, float y, float z) {
		Matrix44 m = sThreadLocal.get();
		m.setRotate(a, x, y, z);
		preConcat(m);
	}
	
	public void postRotate(float a, float x, float y, float z) {
		Matrix44 m = sThreadLocal.get();
		m.setRotate(a, x, y, z);
		postConcat(m);
	}
	
	/**
     * Computes the length of a vector.
     *
     * @param x x coordinate of a vector
     * @param y y coordinate of a vector
     * @param z z coordinate of a vector
     * @return the length of a vector
     */
    public static float length(float x, float y, float z) {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }
	
	public PointF mapPoint(PointF dst, float x, float y) {
	    float dx = x * data[kScaleX] + y * data[kSkewX] + data[kTranslateX];
	    float dy = x * data[kSkewY] + y * data[kScaleY] + data[kTranslateY];
	    float dz = x * data[kPerspective0] + y * data[kPerspective1] + data[kPerspective2];
	    if (dz != 0) dz = 1.0f / dz;

	    dst.x = dx * dz;
	    dst.y = dy * dz;
	    return dst;
	}
}
