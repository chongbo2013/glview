package com.glview.hwui.op;

import com.glview.graphics.Bitmap;
import com.glview.graphics.Rect;
import com.glview.hwui.GLCanvas;
import com.glview.hwui.GLPaint;

public class DrawBitmapBatchRectOp extends AbsDrawBitmapOp {

	Rect mSource = new Rect(), mTarget = new Rect();
	
	public DrawBitmapBatchRectOp() {
	}
	
	public static DrawBitmapBatchRectOp obtain(Bitmap bitmap, Rect source, Rect target, GLPaint paint) {
		DrawBitmapBatchRectOp op = (DrawBitmapBatchRectOp) OpFactory.get().poll(DrawBitmapBatchRectOp.class);
		op.setBitmap(bitmap);
		if (source == null) {
			op.mSource.setEmpty();
		} else {
			op.mSource.set(source);
		}
		if (target == null) {
			op.mTarget.setEmpty();
		} else {
			op.mTarget.set(target);
		}
		op.setPaint(paint);
		return op;
	}

	@Override
	void applyDraw(GLCanvas canvas) {
		canvas.drawBitmapBatch(getBitmap(), mSource, mTarget, mPaint);
	}
	
	@Override
	protected void recycleInner() {
		super.recycleInner();
	}

}
