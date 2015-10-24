package com.glview.hwui;

import android.opengl.Matrix;

import com.glview.graphics.Rect;
import com.glview.graphics.Region;

abstract class StatefullBaseCanvas extends AbsGLCanvas {

	protected final float[] mTempMatrix = new float[32];
	
	protected Snapshot mFirstSnapshot = new Snapshot();
	protected Snapshot mSnapshot = mFirstSnapshot;
	protected int mSaveCount = 1;
	protected boolean mDirtyClip = false;
	
	int mWidth;
	int mHeight;
	
    @Override
    public void setSize(int width, int height) {
    	super.setSize(width, height);
    	mWidth = width;
    	mHeight = height;
    	mFirstSnapshot.setClip(0, 0, width, height);
    }
    
    //设置摄像机
    public void setCamera
    (
    		float cx,	//摄像机位置x
    		float cy,   //摄像机位置y
    		float cz,   //摄像机位置z
    		float tx,   //摄像机目标点x
    		float ty,   //摄像机目标点y
    		float tz,   //摄像机目标点z
    		float upx,  //摄像机UP向量X分量
    		float upy,  //摄像机UP向量Y分量
    		float upz   //摄像机UP向量Z分量		
    )
    {
    	Matrix.setLookAtM
        (
        		currentSnapshot().transform, 
        		16, 
        		cx,
        		cy,
        		cz,
        		tx,
        		ty,
        		tz,
        		upx,
        		upy,
        		upz
        );
    }
    
    //设置正交投影参数
    public void setProjectOrtho
    (
    	float left,		//near面的left
    	float right,    //near面的right
    	float bottom,   //near面的bottom
    	float top,      //near面的top
    	float near,		//near面距离
    	float far       //far面距离
    )
    {    	
    	Matrix.orthoM(currentSnapshot().transform, 32, left, right, bottom, top, near, far);
    }
    
    
    
    
    public void setProjectFrustum
    (
    	float left,		//near面的left
    	float right,    //near面的right
    	float bottom,   //near面的bottom
    	float top,      //near面的top
    	float near,		//near面距离
    	float far       //far面距离
    )
    {
    	/*
    	 * android4.0的Matrix.frustumM方法实现有问题，会出现坐标往左偏一半的问题
    	 * 参考4.4上面的代码自己来实现透视投影
    	 */
//    	Matrix.frustumM(mProjMatrix, 0, left, right, bottom, top, near, far);
    	frustumM(currentSnapshot().transform, 32, left, right, bottom, top, near, far);
    }
    
    /**
     * Defines a projection matrix in terms of six clip planes.
     *
     * @param m the float array that holds the output perspective matrix
     * @param offset the offset into float array m where the perspective
     *        matrix data is written
     * @param left
     * @param right
     * @param bottom
     * @param top
     * @param near
     * @param far
     */
    public static void frustumM(float[] m, int offset,
            float left, float right, float bottom, float top,
            float near, float far) {
        if (left == right) {
            throw new IllegalArgumentException("left == right");
        }
        if (top == bottom) {
            throw new IllegalArgumentException("top == bottom");
        }
        if (near == far) {
            throw new IllegalArgumentException("near == far");
        }
        if (near <= 0.0f) {
            throw new IllegalArgumentException("near <= 0.0f");
        }
        if (far <= 0.0f) {
            throw new IllegalArgumentException("far <= 0.0f");
        }
        final float r_width  = 1.0f / (right - left);
        final float r_height = 1.0f / (top - bottom);
        final float r_depth  = 1.0f / (near - far);
        final float x = 2.0f * (near * r_width);
        final float y = 2.0f * (near * r_height);
        final float A = (right + left) * r_width;
        final float B = (top + bottom) * r_height;
        final float C = (far + near) * r_depth;
        final float D = 2.0f * (far * near * r_depth);
        m[offset + 0] = x;
        m[offset + 5] = y;
        m[offset + 8] = A;
        m[offset +  9] = B;
        m[offset + 10] = C;
        m[offset + 14] = D;
        m[offset + 11] = -1.0f;
        m[offset +  1] = 0.0f;
        m[offset +  2] = 0.0f;
        m[offset +  3] = 0.0f;
        m[offset +  4] = 0.0f;
        m[offset +  6] = 0.0f;
        m[offset +  7] = 0.0f;
        m[offset + 12] = 0.0f;
        m[offset + 13] = 0.0f;
        m[offset + 15] = 0.0f;
    }
    
  //获取具体物体的总变换矩阵
    public float[] getFinalMatrix(float[] outPut, float[] spec)
    {
    	float[] mMVPMatrix = outPut;
    	Matrix.multiplyMM(mMVPMatrix, 0, currentSnapshot().transform, 16, spec, 0);
        Matrix.multiplyMM(mMVPMatrix, 0, currentSnapshot().transform, 32, mMVPMatrix, 0);
        return mMVPMatrix;
    }
    
    @Override
    public int getWidth() {
    	return mWidth;
    }
    
    @Override
    public int getHeight() {
    	return mHeight;
    }
    
    Rect currentClipRect() {
        return currentSnapshot().clipRect;
    }
    
    Snapshot currentSnapshot() {
    	return mSnapshot;
    }
    
	@Override
    public void translate(float x, float y) {
    	translate(x, y, 0);
    }
    
    @Override
    public void translate(float x, float y, float z) {
    	if (z == 0) {
    		float[] m = mSnapshot.transform;
            m[12] += m[0] * x + m[4] * y;
            m[13] += m[1] * x + m[5] * y;
            m[14] += m[2] * x + m[6] * y;
            m[15] += m[3] * x + m[7] * y;
    	} else {
    		Matrix.translateM(mSnapshot.transform, 0, x, y, z);
    	}
    }

    @Override
    public void scale(float sx, float sy, float sz) {
        Matrix.scaleM(mSnapshot.transform, 0, sx, sy, sz);
    }
    
    @Override
    public void rotate(float degrees, float x, float y, float z) {
        if (degrees == 0) return;
        float[] temp = mTempMatrix;
        Matrix.setRotateM(temp, 0, degrees, x, y, z);
        Matrix.multiplyMM(temp, 16, mSnapshot.transform, 0, temp, 0);
        System.arraycopy(temp, 16, mSnapshot.transform, 0, 16);
    }
    
    @Override
    public void multiplyMatrix(float[] matrix, int offset) {
    	float[] temp = mTempMatrix;
        Matrix.multiplyMM(temp, 0, mSnapshot.transform, 0, matrix, offset);
        System.arraycopy(temp, 0, mSnapshot.transform, 0, 16);
    }
    
    public void setAlpha(float alpha) {
        mSnapshot.alpha = alpha < 0 ? 0 : (alpha > 1 ? 1 : alpha);
    }

    public float getAlpha() {
        return mSnapshot.alpha;
    }

    
    public void multiplyAlpha(float alpha) {
    	mSnapshot.alpha *= alpha < 0 ? 0 : (alpha > 1 ? 1 : alpha);    		
    }
    
	@Override
	public int save(int saveFlags) {
		return saveSnapshot(saveFlags);
	}
	
	public void saveViewport() {
		currentSnapshot().flags |= Snapshot.kFlagIsFboLayer;
	}
	
	private int saveSnapshot(int saveFlags) {
		Snapshot snapshot = Snapshot.obtain(mSnapshot, saveFlags);
		mSnapshot = snapshot;
		
		return mSaveCount++;
	}
	
	@Override
	public void clipRect(Rect r) {
		clipRect(r.left, r.top, r.right, r.bottom);
	}
	
	@Override
	public void clipRect(float left, float top, float right, float bottom) {
		mDirtyClip |= currentSnapshot().clip(left, top, right, bottom, Region.Op.INTERSECT);
	}
	
	protected void dirtyClip() {
		mDirtyClip = true;
	}
	
	@Override
    public void restore() {
		if (mSaveCount > 1) {
			restoreSnapshot();
		}
    }
	
	public void restoreToCount(int saveCount) {
		if (saveCount < 1) saveCount = 1;

	    while (mSaveCount > saveCount) {
	        restoreSnapshot();
	    }
	}
	
	private void restoreSnapshot() {
		Snapshot toRemove = mSnapshot;
	    Snapshot toRestore = mSnapshot.previous;

	    mSaveCount--;
	    mSnapshot = toRestore;

	    // subclass handles restore implementation
	    onSnapshotRestored(toRemove, toRestore);
	    toRemove.recycle();
	}
	
	protected void onSnapshotRestored(Snapshot removed, Snapshot toRestore) {
	}
	
	public void drawRenderNode(RenderNode renderNode) {
		renderNode.replay(this);
	}
}
