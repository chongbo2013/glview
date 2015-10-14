package com.glview.hwui.op;

import com.glview.hwui.GLCanvas;
import com.glview.hwui.GLPaint;

public class DrawTextOp extends DrawOp {

	CharSequence mText;
	float mX, mY;
	int mStart, mEnd;
	boolean mDrawDeffer;
	
	public DrawTextOp() {
	}
	
	public static DrawTextOp obtain(CharSequence text, int start, int end, float x, float y, GLPaint paint, boolean drawDeffer) {
		DrawTextOp op = (DrawTextOp) OpFactory.get().poll(DrawTextOp.class);
		op.mText = text;
		op.mX = x;
		op.mY = y;
		op.mStart = start;
		op.mEnd = end;
		op.mDrawDeffer = drawDeffer;
		op.setPaint(paint);
		return op;
	}

	@Override
	void applyDraw(GLCanvas canvas) {
		canvas.drawText(mText, mStart, mEnd, mX, mY, mPaint, mDrawDeffer);
	}
	
	@Override
	protected void recycleInner() {
		super.recycleInner();
		mText = null;
	}

}
