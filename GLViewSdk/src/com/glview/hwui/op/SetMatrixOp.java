package com.glview.hwui.op;

import com.glview.hwui.GLCanvas;

public class SetMatrixOp extends StateOp {
	
	float[] mMatrix = new float[16];
	
	public SetMatrixOp() {
	}
	
	public static SetMatrixOp obtain(float[] matrix, int offset) {
		SetMatrixOp op = (SetMatrixOp) OpFactory.get().poll(SetMatrixOp.class);
		return op;
	}

	@Override
	void applyState(GLCanvas canvas) {
	}

	@Override
	protected void recycleInner() {
		super.recycleInner();
	}

}
