package com.glview.hwui;

import com.glview.graphics.Bitmap;
import com.glview.graphics.Rect;
import com.glview.graphics.RectF;
import com.glview.graphics.drawable.ninepatch.NinePatch;
import com.glview.graphics.mesh.BasicMesh;
import com.glview.hwui.op.ClipRectOp;
import com.glview.hwui.op.DisplayListData;
import com.glview.hwui.op.DrawBitmapBatchRectOp;
import com.glview.hwui.op.DrawBitmapMeshOp;
import com.glview.hwui.op.DrawBitmapOp;
import com.glview.hwui.op.DrawBitmapRectFOp;
import com.glview.hwui.op.DrawBitmapRectOp;
import com.glview.hwui.op.DrawLineOp;
import com.glview.hwui.op.DrawMeshOp;
import com.glview.hwui.op.DrawOp;
import com.glview.hwui.op.DrawPatchOp;
import com.glview.hwui.op.DrawRectOp;
import com.glview.hwui.op.DrawTextOp;
import com.glview.hwui.op.RenderNodeOp;
import com.glview.hwui.op.RestoreOp;
import com.glview.hwui.op.RestoreToCountOp;
import com.glview.hwui.op.RotateOp;
import com.glview.hwui.op.SaveOp;
import com.glview.hwui.op.ScaleOp;
import com.glview.hwui.op.StateOp;
import com.glview.hwui.op.TranslateOp;
import com.glview.pool.Pool;
import com.glview.pool.Poolable;

/**
 * This canvas is used to record our OpenGL operations,
 * we will replay these operations in the {@link RenderThread}
 * @see RenderNode#replay(GLCanvas)
 * 
 * @author lijing.lj
 */
class GLRecordingCanvas extends AbsGLCanvas implements Poolable {
	
	static Pool<GLRecordingCanvas> sPoll = new Pool<GLRecordingCanvas>(false);
	
	DisplayListData mDisplayListData = null;
	CanvasOp mLastCanvasNode = null;
	
	float mTranslateX = 0, mTranslateY = 0, mTranslateZ = 0;
	boolean mHasDeferredTranslate = false;
	int mRestoreSaveCount = -1;
	
	private GLRecordingCanvas() {}
	
	static GLRecordingCanvas obtain(RenderNode renderNode) {
		GLRecordingCanvas canvas = (GLRecordingCanvas) sPoll.poll(GLRecordingCanvas.class, false);
		if (canvas == null) {
			canvas = new GLRecordingCanvas();
		}
		return canvas;
	}
	
	void recycle() {
		mDisplayListData = null;
		mLastCanvasNode = null;
		mTranslateX = mTranslateY = mTranslateZ = 0;
		sPoll.push(this);
	}
	
	DisplayListData endRecording() {
		flush();
		return mDisplayListData;
	}
	
	void flush() {
		flushRestoreToCount();
		flushTranslate();
	}
	
	void ensureDisplayListData() {
		if (mDisplayListData == null) {
			mDisplayListData = DisplayListData.obtain();
		}
	}
	
	/**
	 * Record the CanvasOp to display list
	 * @param op
	 */
	private void addOpAndUpdateChunk(CanvasOp op) {
		ensureDisplayListData();
		if (mDisplayListData.mCanvasOps == null) {
			mDisplayListData.mCanvasOps = op;
		}
		if (mLastCanvasNode == null) {
			mLastCanvasNode = op;
		} else {
			mLastCanvasNode.mNext = op;
			mLastCanvasNode = op;
		}
	}
	
	private void flushAndAddOp(CanvasOp op) {
		flushRestoreToCount();
		flushTranslate();
		addOpAndUpdateChunk(op);
	}
	
	void addStateOp(StateOp op) {
		flushAndAddOp(op);
	}
	
	void addDrawOp(DrawOp op) {
		flushAndAddOp(op);
		mDisplayListData.mHasDrawOp = true;
	}
	
	void addRenderNodeOp(RenderNodeOp op) {
		addDrawOp(op);
	}
	
	void flushTranslate() {
		if (mHasDeferredTranslate) {
			if (mTranslateX != 0.0f || mTranslateY != 0.0f || mTranslateZ != 0.0f) {
				addOpAndUpdateChunk(TranslateOp.obtain(mTranslateX, mTranslateY, mTranslateZ));
	            mTranslateX = mTranslateY = mTranslateZ = 0.0f;
	        }
	        mHasDeferredTranslate = false;
		}
	}
	
	void flushRestoreToCount() {
	    if (mRestoreSaveCount >= 0) {
	        addOpAndUpdateChunk(RestoreToCountOp.obtain(mRestoreSaveCount));
	        mRestoreSaveCount = -1;
	    }
	}
	
	@Override
	public void translate(float x, float y) {
		translate(x, y, 0);
	}
	
	@Override
	public void translate(float x, float y, float z) {
		mHasDeferredTranslate = true;
	    mTranslateX += x;
	    mTranslateY += y;
	    mTranslateZ += z;
	    flushRestoreToCount();
	}
	
	@Override
	public void scale(float sx, float sy, float sz) {
		addStateOp(ScaleOp.obtain(sx, sy, sz));
	}
	
	@Override
	public void rotate(float degrees, float x, float y, float z) {
		addStateOp(RotateOp.obtain(degrees, x, y, z));
	}
	
	@Override
	public void clipRect(Rect r) {
		addStateOp(ClipRectOp.obtain(r));
	}
	
	@Override
	public void clipRect(float left, float top, float right, float bottom) {
		addStateOp(ClipRectOp.obtain(left, top, right, bottom));
	}
	
	@Override
	public int save(int saveFlags) {
		addStateOp(SaveOp.obtain(saveFlags));
		return 0;
	}
	
	@Override
	public void restore() {
		addStateOp(RestoreOp.obtain());
	}
	
	@Override
	public void restoreToCount(int saveCount) {
		mRestoreSaveCount = saveCount;
	    flushTranslate();
	}
	
	@Override
	public void drawRenderNode(RenderNode renderNode) {
		addRenderNodeOp(RenderNodeOp.obtain(renderNode));
	}
	
	@Override
	public void drawLine(float x1, float y1, float x2, float y2, GLPaint paint) {
		addDrawOp(DrawLineOp.obtain(x1, y1, x2, y2, paint));
	}
	
	@Override
	public void drawRect(float left, float top, float right, float bottom,
			GLPaint paint) {
		addDrawOp(DrawRectOp.obtain(left, top, right, bottom, paint));
	}
	
	@Override
	public void drawBitmap(Bitmap bitmap, float x, float y, GLPaint paint) {
		addDrawOp(DrawBitmapOp.obtain(bitmap, x, y, paint));
	}
	
	@Override
	public void drawBitmap(Bitmap bitmap, RectF source, RectF target,
			GLPaint paint) {
		addDrawOp(DrawBitmapRectFOp.obtain(bitmap, source, target, paint));
	}
	
	@Override
	public void drawBitmap(Bitmap bitmap, Rect source, Rect target,
			GLPaint paint) {
		addDrawOp(DrawBitmapRectOp.obtain(bitmap, source, target, paint));
	}
	
	@Override
	public void drawBitmapBatch(Bitmap bitmap, Rect source, Rect target,
			GLPaint paint) {
		addDrawOp(DrawBitmapBatchRectOp.obtain(bitmap, source, target, paint));
	}
	
	@Override
	public void drawPatch(NinePatch patch, Rect rect, GLPaint paint) {
		addDrawOp(DrawPatchOp.obtain(patch, rect, paint));
	}
	
	@Override
	public void drawMesh(BasicMesh mesh, GLPaint paint) {
		addDrawOp(DrawMeshOp.obtain(mesh, paint));
	}
	
	@Override
	public void drawBitmapMesh(Bitmap bitmap, BasicMesh mesh, GLPaint paint) {
		addDrawOp(DrawBitmapMeshOp.obtain(bitmap, mesh, paint));
	}
	
	@Override
	public void drawText(CharSequence text, int start, int end, float x,
			float y, GLPaint paint, boolean drawDefer) {
		addDrawOp(DrawTextOp.obtain(text, start, end, x, y, paint, drawDefer));
	}
}
