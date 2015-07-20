package com.glview.hwui.op;

import com.glview.hwui.CanvasOp;
import com.glview.hwui.GLCanvas;
import com.glview.hwui.GLPaint;

public abstract class DrawOp extends CanvasOp {
	
	protected GLPaint mPaint = new GLPaint();
	
	public DrawOp() {
	}
	
	protected void setPaint(GLPaint paint) {
		mPaint.set(paint);
	}

	@Override
	public final void replay(GLCanvas canvas) {
		applyDraw(canvas);
	}
	
	@Override
	protected void recycleInner() {
		mPaint.reset();
	}
	
	abstract void applyDraw(GLCanvas canvas);

}
