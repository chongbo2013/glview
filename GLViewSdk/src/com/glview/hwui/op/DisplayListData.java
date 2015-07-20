package com.glview.hwui.op;

import com.glview.hwui.CanvasOp;

public class DisplayListData extends NonOp {
	
	public CanvasOp mCanvasOps;
	
	public boolean mHasDrawOp = false;
	
	public DisplayListData() {
	}
	
	public static DisplayListData obtain() {
		return (DisplayListData) OpFactory.get().poll(DisplayListData.class);
	}

	@Override
	protected void recycleInner() {
		mCanvasOps = null;
		mHasDrawOp = false;
	}

}
