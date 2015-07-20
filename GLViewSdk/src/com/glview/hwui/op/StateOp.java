package com.glview.hwui.op;

import com.glview.hwui.CanvasOp;
import com.glview.hwui.GLCanvas;

public abstract class StateOp extends CanvasOp {

	@Override
	public final void replay(GLCanvas canvas) {
		applyState(canvas);
	}
	
	@Override
	protected void recycleInner() {
	}
	
	abstract void applyState(GLCanvas canvas);
}
