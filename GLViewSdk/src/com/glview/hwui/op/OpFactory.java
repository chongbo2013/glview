package com.glview.hwui.op;

import com.glview.hwui.CanvasOp;
import com.glview.pool.Pool;

public final class OpFactory extends Pool<CanvasOp> {
	
	private static OpFactory sInstance;
	
	private OpFactory() {
		super(true);
	}
	
	public synchronized static OpFactory get() {
		if (sInstance == null) {
			sInstance = new OpFactory();
		}
		return sInstance;
	}

}
