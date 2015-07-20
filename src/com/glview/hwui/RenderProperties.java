package com.glview.hwui;

import com.glview.graphics.Bitmap;


final public class RenderProperties {
	
	int mWidth, mHeight;
	
	// These RenderProperties are apply to canvas when replay called, and dose not need to recreate the RenderNode.
	float mTranslationX, mTranslationY, mTranslationZ;
	float mLeft, mTop, mRight, mBottom;
	float mScaleX = 1, mScaleY = 1;
	float mRotation, mRotationX, mRotationY;
	float mPivotX, mPivotY;
	float mAlpha = 1;
	
	Layer mLayer = null;
	
	int mLayerType = Layer.LAYER_TYPE_NONE;
	boolean mNeedsLayerSync = false;
	
	final RenderNode mRenderNode;
	
	public RenderProperties(RenderNode renderNode) {
		mRenderNode = renderNode;
	}
	
	public void setSize(int w, int h) {
		mWidth = w;
		mHeight = h;
		mPivotX = w / 2f;
		mPivotY = h / 2f;
	}
	
	void updateNodeMatrix() {
	}
	
	boolean skipRender() {
		return mWidth <= 0 || mHeight <= 0;
	}
	
	public float getTranslationX() {
		return mTranslationX;
	}

	public boolean setTranslationX(float translationX) {
		if (mTranslationX != translationX) {
			mTranslationX = translationX;
			updateNodeMatrix();
			return true;
		}
		return false;
	}

	public float getTranslationY() {
		return mTranslationY;
	}

	public boolean setTranslationY(float translationY) {
		if (mTranslationY != translationY) {
			mTranslationY = translationY;
			updateNodeMatrix();
			return true;
		}
		return false;
	}

	public float getTranslationZ() {
		return mTranslationZ;
	}

	public boolean setTranslationZ(float translationZ) {
		if (mTranslationZ != translationZ) {
			mTranslationZ = translationZ;
			updateNodeMatrix();
			return true;
		}
		return false;
	}
	
	public float getLeft() {
		return mLeft;
	}
	
	public boolean setLeft(float left) {
		if (mLeft != left) {
			mLeft = left;
			updateNodeMatrix();
			return true;
		}
		return false;
	}
	
	public float getRight() {
		return mRight;
	}
	
	public boolean setRight(float right) {
		if (mRight != right) {
			mRight = right;
			updateNodeMatrix();
			return true;
		}
		return false;
	}
	
	public float getBottom() {
		return mBottom;
	}
	
	public boolean setBottom(float bottom) {
		if (mBottom != bottom) {
			mBottom = bottom;
			updateNodeMatrix();
			return true;
		}
		return false;
	}
	
	public float getTop() {
		return mTop;
	}
	
	public boolean setTop(float top) {
		if (mTop != top) {
			mTop = top;
			updateNodeMatrix();
			return true;
		}
		return false;
	}
	
	public float getScaleX() {
		return mScaleX;
	}
	
	public boolean setScaleX(float scaleX) {
		if (mScaleX != scaleX) {
			mScaleX = scaleX;
			updateNodeMatrix();
			return true;
		}
		return false;
	}
	
	public float getScaleY() {
		return mScaleY;
	}
	
	public boolean setScaleY(float scaleY) {
		if (mScaleY != scaleY) {
			mScaleY =scaleY;
			updateNodeMatrix();
			return true;
		}
		return false;
	}
	
	public float getRotation() {
		return mRotation;
	}
	
	public boolean setRotation(float rotation) {
		if (mRotation != rotation) {
			mRotation = rotation;
			updateNodeMatrix();
			return true;
		}
		return false;
	}
	
	public float getRotationX() {
		return mRotationX;
	}
	
	public boolean setRotationX(float rotationX) {
		if (mRotationX != rotationX) {
			mRotationX = rotationX;
			updateNodeMatrix();
			return true;
		}
		return false;
	}
	
	public float getRotationY() {
		return mRotationY;
	}
	
	public boolean setRotationY(float rotationY) {
		if (mRotationY != rotationY) {
			mRotationY = rotationY;
			updateNodeMatrix();
			return true;
		}
		return false;
	}
	
	public float getAlpha() {
		return mAlpha;
	}
	
	public boolean setAlpha(float alpha) {
		if (mAlpha != alpha) {
			if (alpha < 0) {
				alpha = 0;
			}
			if (alpha > 1) {
				alpha = 1;
			}
			mAlpha = alpha;
			return true;
		}
		return false;
	}
	
	public boolean setLayerType(int layerType) {
		if (mLayerType != layerType) {
			mLayerType = layerType;
			mNeedsLayerSync = true;
			return true;
		}
		return false;
	}
	
	public int getLayerType() {
		return mLayerType;
	}
	
	/**
	 * Apply render node transformation to canvas at the beginning of replay.
	 * @param canvas
	 */
	void applyRenderProperties(GLCanvas canvas) {
		canvas.save();
		// translate
		final float x = mLeft + mTranslationX;
		final float y = mTop + mTranslationY;
		final float z = mTranslationZ;
		if (x != 0.0f || y != 0.0f || z != 0.0f) {
			canvas.translate(x, y, z);
		}
		// scale/rotate
		boolean hasScale = mScaleX != 1.0f || mScaleY != 1.0f;
		boolean hasRotate = mRotation != 0.0f || mRotationX != 0.0f || mRotationY != 0.0f;
		if (hasScale || hasRotate) {
			canvas.translate(mPivotX, mPivotY);
			if (hasScale) {
				canvas.scale(mScaleX, mScaleY, 1.0f);
			}
			if (mRotation != 0.0f) {
				canvas.rotate(mRotation, 0, 0, 1.0f);
			}
			if (mRotationX != 0.0f) {
				canvas.rotate(mRotationX, 1.0f, 0, 0);
			}
			if (mRotationY != 0.0f) {
				canvas.rotate(mRotationY, 0, 1.0f, 0);
			}
			canvas.translate(- mPivotX, - mPivotY);
		}
		canvas.multiplyAlpha(mAlpha);
	}
	
	void restoreRenderProperties(GLCanvas canvas) {
		canvas.restore();
	}
	
	void updateRenderLayer(GLCanvas canvas) {
		if (!(canvas instanceof GL20Canvas)) {
			// We only support OpenGL 2.0
			return;
		}
		if (mNeedsLayerSync) {
			mNeedsLayerSync = false;
			if (mLayer != null) {
				if (mLayerType != Layer.LAYER_TYPE_HARDWARE) {
					mLayer.destroy();
					mLayer = null;
					return;
				}
				if ((mLayer.getWidth() != mWidth || mLayer.getHeight() != mHeight) && !mLayer.resize(mWidth, mHeight)) {
					mLayer.destroy();
					mLayer = null;
				}
			}
			if (mLayerType == Layer.LAYER_TYPE_HARDWARE && mLayer == null && mWidth > 0 && mHeight > 0) {
				mLayer = new Layer(mWidth, mHeight);
			}
			if (mLayer != null) {
				mLayer.invalidate();
			}
		}
	}
	
	boolean renderLayer(GLCanvas canvas) {
		if (mLayer != null) {
			return mLayer.render(canvas, mRenderNode);
		}
		return false;
	}
	
	Bitmap buildDrawingCache(GLCanvas canvas) {
		Layer layer = mLayer;
		if (layer == null && mWidth > 0 && mHeight > 0) {
			layer = new Layer(mWidth, mHeight);
		}
		if (layer != null) {
			Bitmap bitmap = layer.buildDrawingCache(canvas, mRenderNode);
			if (mLayer == null) {
				layer.destroy();
			}
			return bitmap;
		}
		return null;
	}
}

