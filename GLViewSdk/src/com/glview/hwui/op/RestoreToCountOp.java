package com.glview.hwui.op;

import com.glview.hwui.GLCanvas;

public class RestoreToCountOp extends StateOp {
	
	private int mSaveCount;

	public RestoreToCountOp() {
	}
	
	public static RestoreToCountOp obtain(int saveCount) {
		RestoreToCountOp op = (RestoreToCountOp) OpFactory.get().poll(RestoreToCountOp.class);
		op.mSaveCount = saveCount;
		return op;
	}

	@Override
	void applyState(GLCanvas canvas) {
		canvas.restoreToCount(mSaveCount);
	}

}
