package com.glview.hwui.op;

import com.glview.graphics.Rect;
import com.glview.hwui.GLCanvas;

public class ClipRectOp extends StateOp {

	Rect mRect = new Rect();
	
	public ClipRectOp() {
	}
	
	public static ClipRectOp obtain(Rect rect) {
		ClipRectOp op = (ClipRectOp) OpFactory.get().poll(ClipRectOp.class);
		op.mRect.set(rect);
		return op;
	}

	@Override
	void applyState(GLCanvas canvas) {
		canvas.clipRect(mRect);
	}

}
