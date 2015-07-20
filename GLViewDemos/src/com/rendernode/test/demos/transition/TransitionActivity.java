package com.rendernode.test.demos.transition;

import android.os.Bundle;

import com.glview.transition.TransitionManager;
import com.glview.view.Gravity;
import com.glview.view.View;
import com.glview.view.ViewGroup;
import com.glview.widget.FrameLayout;
import com.rendernode.test.R;
import com.rendernode.test.demos.BaseActivity;

public class TransitionActivity extends BaseActivity {
	
	ViewGroup mContainer;
	
	View mView1, mView2, mView3;
	View mView4;
	
	boolean expanded = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setGLContentView(R.layout.activity_transition);
	}
	
	@Override
	public void onAttached(View content) {
		super.onAttached(content);
		mContainer = (ViewGroup) content.findViewById(R.id.container);
		mView1 = content.findViewById(R.id.text1);
		mView2 = content.findViewById(R.id.text2);
		mView3 = content.findViewById(R.id.text3);
		mView4 = content.findViewById(R.id.image);
	}
	
	public void click(View v) {
		TransitionManager.beginDelayedTransition(mContainer);
		if (!expanded) {
			((FrameLayout.LayoutParams) mView1.getLayoutParams()).gravity = Gravity.CENTER;
			((FrameLayout.LayoutParams) mView2.getLayoutParams()).gravity = Gravity.CENTER_HORIZONTAL;
			((FrameLayout.LayoutParams) mView3.getLayoutParams()).gravity = Gravity.CENTER_VERTICAL;
			mView4.setVisibility(View.VISIBLE);
			expanded = true;
		} else {
			((FrameLayout.LayoutParams) mView1.getLayoutParams()).gravity = Gravity.START;
			((FrameLayout.LayoutParams) mView2.getLayoutParams()).gravity = Gravity.START;
			((FrameLayout.LayoutParams) mView3.getLayoutParams()).gravity = Gravity.START;
			mView4.setVisibility(View.GONE);
			expanded = false;
		}
		mContainer.requestLayout();
	}
}