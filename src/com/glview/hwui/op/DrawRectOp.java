package com.glview.hwui.op;

import com.glview.hwui.GLCanvas;
import com.glview.hwui.GLPaint;

public class DrawRectOp extends DrawOp {
	
	protected float mLeft, mTop, mRight, mBottom;
	
	public DrawRectOp() {
	}

	public static DrawRectOp obtain(float left, float top, float right, float bottom, GLPaint paint) {
		DrawRectOp op = (DrawRectOp) OpFactory.get().poll(DrawRectOp.class);
		op.mLeft = left;
		op.mTop = top;
		op.mRight = right;
		op.mBottom = bottom;
		op.setPaint(paint);
		return op;
	}

	@Override
	void applyDraw(GLCanvas canvas) {
		canvas.drawRect(mLeft, mTop, mRight, mBottom, mPaint);
	}

}
