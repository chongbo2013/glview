package com.rendernode.test.demos;

import android.os.Bundle;
import android.util.Log;

import com.glview.view.View;
import com.glview.view.ViewTreeObserver.OnGlobalFocusChangeListener;
import com.rendernode.test.R;

public class StateListActivity extends BaseActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setGLContentView(R.layout.activity_state_list);
	}
	
	@Override
	public void onAttached(View content) {
		super.onAttached(content);
		content.getViewTreeObserver().addOnGlobalFocusChangeListener(new OnGlobalFocusChangeListener() {
			@Override
			public void onGlobalFocusChanged(View oldFocus, View newFocus) {
			}
		});
	}
}
