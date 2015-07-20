package com.glview.hwui.op;

import com.glview.graphics.Bitmap;
import com.glview.hwui.GLCanvas;
import com.glview.hwui.GLPaint;

public class DrawBitmapOp extends AbsDrawBitmapOp {

	float mX, mY;
	
	public DrawBitmapOp() {
	}
	
	public static DrawBitmapOp obtain(Bitmap bitmap, float x, float y, GLPaint paint) {
		DrawBitmapOp op = (DrawBitmapOp) OpFactory.get().poll(DrawBitmapOp.class);
		op.setBitmap(bitmap);
		op.mX = x;
		op.mY = y;
		op.setPaint(paint);
		return op;
	}

	@Override
	void applyDraw(GLCanvas canvas) {
		canvas.drawBitmap(getBitmap(), mX, mY, mPaint);
	}
	
	@Override
	protected void recycleInner() {
		super.recycleInner();
	}

}
