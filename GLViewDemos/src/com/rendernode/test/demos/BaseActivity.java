package com.rendernode.test.demos;

import android.os.Bundle;
import android.view.KeyEvent;

import com.glview.app.GLActivity;
import com.glview.view.View;

public class BaseActivity extends GLActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setDebugEnable(true);
	}
	
	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		return super.dispatchKeyEvent(event);
	}

	@Override
	public void onAttached(View content) {
		
	}

}
