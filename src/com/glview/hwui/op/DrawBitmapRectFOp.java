package com.glview.hwui.op;

import com.glview.graphics.Bitmap;
import com.glview.graphics.RectF;
import com.glview.hwui.GLCanvas;
import com.glview.hwui.GLPaint;

public class DrawBitmapRectFOp extends AbsDrawBitmapOp {

	RectF mSource = new RectF(), mTarget = new RectF();
	
	public DrawBitmapRectFOp() {
	}
	
	public static DrawBitmapRectFOp obtain(Bitmap bitmap, RectF source, RectF target, GLPaint paint) {
		DrawBitmapRectFOp op = (DrawBitmapRectFOp) OpFactory.get().poll(DrawBitmapRectFOp.class);
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
		canvas.drawBitmap(getBitmap(), mSource, mTarget, mPaint);
	}
	
	@Override
	protected void recycleInner() {
		super.recycleInner();
	}

}
