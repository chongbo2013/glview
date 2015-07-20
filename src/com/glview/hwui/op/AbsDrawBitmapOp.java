package com.glview.hwui.op;

import com.glview.graphics.Bitmap;

public abstract class AbsDrawBitmapOp extends DrawOp {

	private Bitmap mBitmap;
	
	protected void setBitmap(Bitmap bitmap) {
		mBitmap = bitmap;
	}
	
	protected Bitmap getBitmap() {
		return mBitmap;
	}
	
	@Override
	protected void recycleInner() {
		super.recycleInner();
		mBitmap = null;
	}
	
}
