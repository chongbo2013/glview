package com.glview.hwui.op;

import com.glview.hwui.GLCanvas;

public class RotateOp extends StateOp {
	
	float mDegrees;
	float mX, mY, mZ;

	public RotateOp() {
	}
	
	public static RotateOp obtain(float degrees, float x, float y, float z) {
		RotateOp op = (RotateOp) OpFactory.get().poll(RotateOp.class);
		op.mDegrees = degrees;
		op.mX = x;
		op.mY = y;
		op.mZ = z;
		return op;
	}

	@Override
	void applyState(GLCanvas canvas) {
		canvas.rotate(mDegrees, mX, mY, mZ);
	}

}
