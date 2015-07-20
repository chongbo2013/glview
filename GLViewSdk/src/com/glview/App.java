package com.glview;

import com.glview.libgdx.graphics.opengl.GL;
import com.glview.libgdx.graphics.opengl.GL11;
import com.glview.libgdx.graphics.opengl.GL20;

public class App {
	
	static ThreadLocal<GL20> sGL20ThreadInstance = new ThreadLocal<GL20>();
	
	public static GL20 getGL20() {
		return sGL20ThreadInstance.get();
	}
	
	public static GL getGL() {
		return sGL20ThreadInstance.get();
	}
	
	public static void setGL20(GL20 gl) {
		sGL20ThreadInstance.set(gl);
	}
	
	public static GL11 getGL11() {
		return null;
	}

}
