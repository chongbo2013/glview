package com.glview;

import com.glview.libgdx.graphics.opengl.GL;
import com.glview.libgdx.graphics.opengl.GL20;

public class App {
	
	private static GL20 sGL20Instance;
	
	public static GL20 getGL20() {
		return sGL20Instance;
	}
	
	public static GL getGL() {
		return sGL20Instance;
	}
	
	/**
	 * Dont call. 
	 * @hide
	 * @param gl
	 */
	public static void setGL20(GL20 gl) {
		sGL20Instance = gl;
	}

}
