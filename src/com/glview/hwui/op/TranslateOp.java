package com.glview.hwui.op;

import com.glview.hwui.GLCanvas;

public class TranslateOp extends StateOp {

	protected float mTranslationX, mTranslationY, mTranslationZ;
	
	public TranslateOp() {
	}
	
	public static TranslateOp obtain(float translationX, float translationY, float translationZ) {
		TranslateOp op = (TranslateOp) OpFactory.get().poll(TranslateOp.class);
		op.mTranslationX = translationX;
		op.mTranslationY = translationY;
		op.mTranslationZ = translationZ;
		return op;
	}

	@Override
	void applyState(GLCanvas canvas) {
		canvas.translate(mTranslationX, mTranslationY, mTranslationZ);
	}

}
