package com.glview.hwui.op;

import com.glview.graphics.Rect;
import com.glview.graphics.drawable.ninepatch.NinePatch;
import com.glview.hwui.GLCanvas;
import com.glview.hwui.GLPaint;

public class DrawPatchOp extends DrawOp {
	
	NinePatch mPatch;
	Rect mRect = new Rect();

	public DrawPatchOp() {
	}
	
	public static DrawPatchOp obtain(NinePatch patch, Rect rect, GLPaint paint) {
		DrawPatchOp op = (DrawPatchOp) OpFactory.get().poll(DrawPatchOp.class);
		op.mPatch = patch;
		if (rect == null) {
			op.mRect.setEmpty();
		} else {
			op.mRect.set(rect);
		}
		op.setPaint(paint);
		return op;
	}

	@Override
	void applyDraw(GLCanvas canvas) {
		canvas.drawPatch(mPatch, mRect, mPaint);
	}
	
	@Override
	protected void recycleInner() {
		super.recycleInner();
		mPatch = null;
	}

}
