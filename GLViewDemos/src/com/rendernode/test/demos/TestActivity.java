package com.rendernode.test.demos;

import android.os.Bundle;

import com.glview.view.View;
import com.rendernode.test.view.BlurView;

public class TestActivity extends BaseActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setGLContentView(new BlurView(this));
	}
	
	@Override
	public void onAttached(View content) {
		super.onAttached(content);
	}
}
