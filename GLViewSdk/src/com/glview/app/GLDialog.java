package com.glview.app;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.SurfaceView;

import com.glview.content.GLContext;
import com.glview.thread.Handler;
import com.glview.thread.Looper;
import com.glview.view.GLRootView;
import com.glview.view.View;

public class GLDialog extends Dialog  implements GLRootView.Callback {
	
	GLRootView mGLRootView;
	
	Handler mHandler = new Handler(Looper.getMainLooper());

	public GLDialog(Context context) {
		super(context);
		GLContext.initialize(context);
	}

	public GLDialog(Context context, int theme) {
		super(context, theme);
		GLContext.initialize(context);
	}

	public GLDialog(Context context, boolean cancelable,
			OnCancelListener cancelListener) {
		super(context, cancelable, cancelListener);
		GLContext.initialize(context);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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
			setContentView(new GLRootView(getContext()));
		}
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
}
