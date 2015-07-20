package com.rendernode.test.demos;

import android.content.Context;
import android.os.Bundle;

import com.glview.animation.ObjectAnimator;
import com.glview.animation.ValueAnimator;
import com.glview.view.Gravity;
import com.glview.view.View;
import com.glview.widget.FrameLayout;
import com.glview.widget.FrameLayout.LayoutParams;
import com.glview.widget.LinearLayout;
import com.rendernode.test.view.RotateView;

public class RotateViewActivity extends BaseActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setGLContentView(new FrameLayout(this));
	}
	
	@Override
	public void onAttached(View content) {
		super.onAttached(content);
		LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER);
		
		LinearLayout l = new MyLinearLayout(this);
		((FrameLayout) content).addView(l, lp);
		
		LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(300, 300);
//		LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
		
		View v = new RotateView(this);
		l.addView(v, lp1);
//		v.setAlpha(0.5f);
		v = new RotateView(this);
		l.addView(v, lp1);
		v.setAlpha(0.5f);
		v = new RotateView(this);
		l.addView(v, lp1);
		v.setAlpha(0.5f);
		ObjectAnimator animator = ObjectAnimator.ofFloat(l, "rotationX", 0, 1000);
		animator.setDuration(5000);
		animator.setRepeatCount(ValueAnimator.INFINITE);
		animator.setRepeatMode(ValueAnimator.REVERSE);
//		animator.start();
		animator = ObjectAnimator.ofFloat(l, "rotationY", 0, 360);
		animator.setDuration(10000);
		animator.setRepeatCount(ValueAnimator.INFINITE);
		animator.setRepeatMode(ValueAnimator.REVERSE);
		animator.start();
		animator = ObjectAnimator.ofFloat(l, "scale", 1f, 0.3f);
		animator.setDuration(3000);
		animator.setRepeatCount(ValueAnimator.INFINITE);
		animator.setRepeatMode(ValueAnimator.REVERSE);
//		animator.start();
//		l.setRotationX(45);
	}
	
	class MyLinearLayout extends LinearLayout {

		public MyLinearLayout(Context context) {
			super(context);
//			setChildrenDrawingOrderEnabled(true);
		}
		
		@Override
		protected int getChildDrawingOrder(int childCount, int zOrder) {
			if (zOrder == 1) return 0;
			if (zOrder == 0) return 1;
			return super.getChildDrawingOrder(childCount, zOrder);
		}
	}
}
