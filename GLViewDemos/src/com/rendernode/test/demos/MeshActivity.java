package com.rendernode.test.demos;

import android.os.Bundle;

import com.rendernode.test.view.MeshView;

public class MeshActivity extends BaseActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setGLContentView(new MeshView(this));
	}
}
