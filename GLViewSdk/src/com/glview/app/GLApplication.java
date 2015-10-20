package com.glview.app;

import android.app.Application;

import com.glview.content.GLContext;
import com.glview.hwui.RenderPolicy;

public class GLApplication extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
		GLContext.initialize(getApplicationContext());
	}
	
	@Override
	public void onTrimMemory(int level) {
		super.onTrimMemory(level);
		RenderPolicy.trimMemory(level);
	}
	
	@Override
	public void onLowMemory() {
		super.onLowMemory();
		RenderPolicy.trimMemory(TRIM_MEMORY_COMPLETE);
	}
}
