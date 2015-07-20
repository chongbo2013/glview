package com.glview.hwui.op;

import com.glview.hwui.GLCanvas;
import com.glview.hwui.GLPaint;

public class DrawLineOp extends DrawOp {
	
	protected float mX1, mY1, mX2, mY2;
	
	public DrawLineOp() {
	}

	public static DrawLineOp obtain(float x1, float y1, float x2, float y2, GLPaint paint) {
		DrawLineOp op = (DrawLineOp) OpFactory.get().poll(DrawLineOp.class);
		op.mX1 = x1;
		op.mY1 = y1;
		op.mX2 = x2;
		op.mY2 = y2;
		op.setPaint(paint);
		return op;
	}

	@Override
	void applyDraw(GLCanvas canvas) {
		canvas.drawRect(mX1, mY1, mX2, mY2, mPaint);
	}

}
