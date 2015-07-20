package com.glview.hwui.op;

import com.glview.hwui.GLCanvas;

public class ScaleOp extends StateOp {
	
	float mScaleX, mScaleY, mScaleZ;

	public ScaleOp() {
	}
	
	public static ScaleOp obtain(float scaleX, float scaleY, float scaleZ) {
		ScaleOp op = (ScaleOp) OpFactory.get().poll(ScaleOp.class);
		op.mScaleX = scaleX;
		op.mScaleY = scaleY;
		op.mScaleZ = scaleZ;
		return op;
	}

	@Override
	void applyState(GLCanvas canvas) {
		canvas.scale(mScaleX, mScaleY, mScaleZ);
	}

}
