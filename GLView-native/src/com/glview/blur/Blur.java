package com.glview.blur;

import com.glview.Lib;

public class Blur {

	static {
		Lib.init();
	}
	
	public final static native void blur(byte[] src, int w, int h, int stride, int radius, int cores, int core, int step);

}
