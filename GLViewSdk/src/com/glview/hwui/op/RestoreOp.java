package com.glview.hwui.op;

import com.glview.hwui.GLCanvas;

public class RestoreOp extends StateOp {

	public RestoreOp() {
	}
	
	public static RestoreOp obtain() {
		RestoreOp op = (RestoreOp) OpFactory.get().poll(RestoreOp.class);
		return op;
	}

	@Override
	void applyState(GLCanvas canvas) {
		canvas.restore();
	}

}
