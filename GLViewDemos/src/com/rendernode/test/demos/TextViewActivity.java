package com.rendernode.test.demos;

import com.glview.view.View;
import com.glview.view.ViewGroup;
import com.glview.view.LayoutInflater;
import com.rendernode.test.R;

import android.os.Bundle;

public class TextViewActivity extends BaseActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		View v = LayoutInflater.from(this).inflate(R.layout.gl_layout_text, (ViewGroup) null, false);
		setGLContentView(v);
	}
}
