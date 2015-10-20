package com.glview.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.SurfaceView;

import com.glview.content.GLContext;
import com.glview.hwui.RenderPolicy;
import com.glview.thread.Handler;
import com.glview.thread.Looper;
import com.glview.view.GLRootView;
import com.glview.view.LayoutInflater;
import com.glview.view.View;

public abstract class GLActivity extends Activity implements GLRootView.Callback {
	
	GLRootView mGLRootView;
	
	Handler mHandler = new Handler(Looper.getMainLooper());
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		GLContext.initialize(this);
	}
	
	public void setDebugEnable(boolean enable) {
		ensureGLRootView();
		mGLRootView.setDebugEnable(enable);
	}
	
	public void setGLContentView(int layout) {
		ensureGLRootView();
		mGLRootView.setContentView(layout);
	}
	
	public void setGLContentView(View content) {
		ensureGLRootView();
		mGLRootView.setContentView(content);
	}
	
	public SurfaceView getRootView() {
		ensureGLRootView();
		return mGLRootView;
	}
	
	private void ensureGLRootView() {
		if (mGLRootView == null) {
			setContentView(new GLRootView(this));
		}
	}
	
	public SurfaceView getSurfaceView() {
		ensureGLRootView();
		return mGLRootView;
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mGLRootView != null) {
			mGLRootView.removeCallback(this);
		}
		LayoutInflater.destory(this);
	}
	
	private void addCallback() {
		if (mGLRootView != null) {
			mGLRootView.addCallback(this);
		}
	}
	
	@Override
	public void setContentView(int layoutResID) {
		super.setContentView(layoutResID);
		mGLRootView = (GLRootView) findViewById(GLRootView.GL_ROOT_VIEW_ID);
		addCallback();
	}
	
	@Override
	public void setContentView(android.view.View view) {
		super.setContentView(view);
		mGLRootView = (GLRootView) findViewById(GLRootView.GL_ROOT_VIEW_ID);
		addCallback();
	}
	
	@Override
	public void setContentView(android.view.View view, android.view.ViewGroup.LayoutParams params) {
		super.setContentView(view, params);
		mGLRootView = (GLRootView) findViewById(GLRootView.GL_ROOT_VIEW_ID);
		addCallback();
	}
	
	@Override
	public void onAttached(View content) {
	}
	
	public void runOnGLThread(Runnable r) {
		if (Looper.myLooper() == mHandler.getLooper()) {
			r.run();
		} else {
			mHandler.post(r);
		}
	}
	
	@Override
	public void onLowMemory() {
		super.onLowMemory();
		RenderPolicy.trimMemory(TRIM_MEMORY_COMPLETE);
	}
	
	@Override
	public void onTrimMemory(int level) {
		super.onTrimMemory(level);
		RenderPolicy.trimMemory(level);
	}
}
