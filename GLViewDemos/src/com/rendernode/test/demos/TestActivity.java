package com.rendernode.test.demos;

import android.os.Bundle;

import com.rendernode.test.view.BatchView;

public class TestActivity extends BaseActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setGLContentView(new BatchView(this));
	}
}
