package com.glview.hwui.op;

import com.glview.hwui.GLCanvas;

public class SaveOp extends StateOp {

	int mSaveFlags;
	
	public SaveOp() {
	}
	
	public static SaveOp obtain(int saveFlags) {
		SaveOp op = (SaveOp) OpFactory.get().poll(SaveOp.class);
		op.mSaveFlags = saveFlags;
		return op;
	}

	@Override
	void applyState(GLCanvas canvas) {
		canvas.save(mSaveFlags);
	}

	@Override
	protected void recycleInner() {
	}

}
