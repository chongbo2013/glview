package com.rendernode.test.demos;

import android.os.Bundle;

import com.rendernode.test.R;

public class ListViewActivity extends BaseActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setGLContentView(R.layout.gl_layout_list);
	}
}
