package com.glview.hwui;

import com.glview.graphics.Bitmap;
import com.glview.hwui.op.DisplayListData;
import com.glview.hwui.op.RenderNodeOp;

/**
 * 
 * 
 * enum DirtyPropertyMask {
        GENERIC         = 1 << 1,
        TRANSLATION_X   = 1 << 2,
        TRANSLATION_Y   = 1 << 3,
        TRANSLATION_Z   = 1 << 4,
        SCALE_X         = 1 << 5,
        SCALE_Y         = 1 << 6,
        ROTATION        = 1 << 7,
        ROTATION_X      = 1 << 8,
        ROTATION_Y      = 1 << 9,
        X               = 1 << 10,
        Y               = 1 << 11,
        Z               = 1 << 12,
        ALPHA           = 1 << 13,
        DISPLAY_LIST    = 1 << 14,
    };
 * 
 * 
 * @author lijing.lj
 */
public class RenderNode {
	
	boolean mValid = false;
	
	/**
	 * The cached display list which needs to sync to {@link #mDisplayListData}.
	 * @see #replay(GLCanvas)
	 * @see #mNeedsDisplayListDataSync
	 */
	DisplayListData mStageDisplayListData = null;
	/**
	 * Recorded operations that will be replayed, use {@link StatefullBaseCanvas} to do the real gl operations.
	 * @see #replay(GLCanvas)
	 */
	DisplayListData mDisplayListData = null;
	boolean mNeedsDisplayListDataSync = false;
	final byte[] mDisplayListDataLock = new byte[0];
	final byte[] mStageDisplayListDataLock = new byte[0];
	
	RenderProperties mRenderProperties = new RenderProperties(this);
	
	public RenderNode() {
	}
	
	public boolean isValid() {
		return mValid;
	}
	
	public void destroy() {
		if (!mValid) return;
		mValid = false;
		recycleDisplayListData(true);
	}
	
	/**
	 * Recycle the current display list when {@link #destroy()} called
	 * or the display list will be recreated.
	 * @see #replay(GLCanvas)
	 * 
	 * @param destory
	 */
	void recycleDisplayListData(boolean destory) {
		DisplayListData displayListData = mDisplayListData;
		synchronized (mDisplayListDataLock) {
			mDisplayListData = null;
		}
		recycleDisplayListData(displayListData, destory);
	}
	
	void recycleDisplayListData(DisplayListData displayListData, boolean destory) {
		if (displayListData != null) {
			CanvasOp op = displayListData.mCanvasOps;
			while (op != null) {
				if (destory && (op instanceof RenderNodeOp)) {
					((RenderNodeOp) op).destroyRenderNode();
				}
				CanvasOp next = op.mNext;
				op.recycle();
				op = next;
			}
			displayListData.recycle();
		}
	}

	/**
	 * Start recording, obtain a GLRecordingCanvas.
	 * @param w
	 * @param h
	 * @return
	 */
	public GLCanvas start(int w, int h) {
		mRenderProperties.setSize(w, h);
		return GLRecordingCanvas.obtain(this);
	}

	/**
	 * End recording, the display list needs to be recreate.
	 * Set {@link #mNeedsDisplayListDataSync} to true to notice
	 * render thread that the display list will be sync.
	 * @param endCanvas
	 */
	public void end(GLCanvas endCanvas) {
		if (!(endCanvas instanceof GLRecordingCanvas)) {
            throw new IllegalArgumentException("Passed an invalid canvas to end!");
        }
		GLRecordingCanvas canvas = (GLRecordingCanvas) endCanvas;
		DisplayListData op = canvas.endRecording();
		synchronized (mStageDisplayListDataLock) {
			if (mStageDisplayListData != null) {
				recycleDisplayListData(mStageDisplayListData, false);
			}
			mStageDisplayListData = op;
			mNeedsDisplayListDataSync = true;
		}
		canvas.recycle();
		mValid = true;
	}
	
	public float getTranslationX() {
		return mRenderProperties.getTranslationX();
	}

	public boolean setTranslationX(float translationX) {
		return mRenderProperties.setTranslationX(translationX);
	}

	public float getTranslationY() {
		return mRenderProperties.getTranslationY();
	}

	public boolean setTranslationY(float translationY) {
		return mRenderProperties.setTranslationY(translationY);
	}

	public float getTranslationZ() {
		return mRenderProperties.getTranslationZ();
	}

	public boolean setTranslationZ(float translationZ) {
		return mRenderProperties.setTranslationZ(translationZ);
	}
	
	public float getLeft() {
		return mRenderProperties.getLeft();
	}

	public boolean setLeft(float left) {
		return mRenderProperties.setLeft(left);
	}

	public float getTop() {
		return mRenderProperties.getTop();
	}

	public boolean setTop(float top) {
		return mRenderProperties.setTop(top);
	}
	
	public float getRight() {
		return mRenderProperties.getRight();
	}
	
	public boolean setRight(float right) {
		return mRenderProperties.setRight(right);
	}
	
	public float getBottom() {
		return mRenderProperties.getBottom();
	}
	
	public boolean setBottom(float bottom) {
		return mRenderProperties.setBottom(bottom);
	}
	
	public boolean setLeftTopRightBottom(float left, float top, float right, float bottom) {
		return setLeft(left) | setTop(top) | setRight(right) | setBottom(bottom);
	}
	
	public float getScaleX() {
		return mRenderProperties.getScaleX();
	}
	
	public boolean setScaleX(float scaleX) {
		return mRenderProperties.setScaleX(scaleX);
	}
	
	public float getScaleY() {
		return mRenderProperties.getScaleY();
	}
	
	public boolean setScaleY(float scaleY) {
		return mRenderProperties.setScaleY(scaleY);
	}
	
	public float getRotation() {
		return mRenderProperties.getRotation();
	}
	
	public boolean setRotation(float rotation) {
		return mRenderProperties.setRotation(rotation);
	}
	
	public float getRotationX() {
		return mRenderProperties.getRotationX();
	}
	
	public boolean setRotationX(float rotationX) {
		return mRenderProperties.setRotationX(rotationX);
	}
	
	public float getRotationY() {
		return mRenderProperties.getRotationY();
	}
	
	public boolean setRotationY(float rotationY) {
		return mRenderProperties.setRotationY(rotationY);
	}
	
	public float getAlpha() {
		return mRenderProperties.getAlpha();
	}
	
	public boolean setAlpha(float alpha) {
		return mRenderProperties.setAlpha(alpha);
	}
	
	public int getLayerType() {
		return mRenderProperties.getLayerType();
	}
	
	public boolean setLayerType(int layerType) {
		return mRenderProperties.setLayerType(layerType);
	}
	
	public RenderProperties properties() {
		return mRenderProperties;
	}
	
	/*
	 * This method runs in RenderThread.
	 */
	void replay(GLCanvas canvas) {
		if (mRenderProperties.skipRender()) return;
		
		// apply properties transform
		mRenderProperties.applyRenderProperties(canvas);
		// DisplayList has been changed by GLThread, sync it to RenderThread
		if (mNeedsDisplayListDataSync) {
			recycleDisplayListData(false);
			
			mDisplayListData = mStageDisplayListData;
			synchronized (mStageDisplayListDataLock) {
				mStageDisplayListData = null;
				mNeedsDisplayListDataSync = false;
			}
			mRenderProperties.mNeedsLayerSync = true;
		}
		
		mRenderProperties.updateRenderLayer(canvas);
		
		render(canvas);
		// restore properties transform
		mRenderProperties.restoreRenderProperties(canvas);
	}
	
	private void render(GLCanvas canvas) {
		if (!mRenderProperties.renderLayer(canvas)) {
			renderWithoutLayer(canvas);
		}
	}
	
	void renderWithoutLayer(GLCanvas canvas) {
		synchronized (mDisplayListDataLock) {
			DisplayListData displayListData = mDisplayListData;
			if (displayListData != null) {
				CanvasOp op = displayListData.mCanvasOps;
				while (op != null) {
					op.replay(canvas);
					op = op.mNext;
				}
			}
		}
	}
	
	public Bitmap buildDrawingCache(GLCanvas canvas) {
		try {
			if (mRenderProperties.skipRender()) return null;
			// DisplayList has been changed by GLThread, sync it to RenderThread
			if (mNeedsDisplayListDataSync) {
				recycleDisplayListData(false);
				
				mDisplayListData = mStageDisplayListData;
				synchronized (mStageDisplayListDataLock) {
					mStageDisplayListData = null;
					mNeedsDisplayListDataSync = false;
				}
				mRenderProperties.mNeedsLayerSync = true;
			}
			return mRenderProperties.buildDrawingCache(canvas);
		} finally {
			canvas.restoreToCount(0);
		}
	}
}
