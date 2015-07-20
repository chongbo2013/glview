package com.rendernode.test.demos;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.rendernode.test.R;

public class RotateView1Activity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(getContentView());
	}
	
	public View getContentView() {
		FrameLayout fl = new FrameLayout(this);
		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, Gravity.CENTER);
		
		LinearLayout l = new LinearLayout(this);
		fl.addView(l, lp);
		
//		LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(300, 300);
		LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
		ImageView iv = new ImageView(this);
//		iv.setRotationX(30);
		iv.setImageResource(R.drawable.bitmap1);
		l.addView(iv, lp1);
		iv = new ImageView(this);
//		iv.setRotationX(30);
		iv.setImageResource(R.drawable.bitmap1);
//		l.addView(iv, lp1);
		iv = new ImageView(this);
//		iv.setRotationX(30);
		iv.setImageResource(R.drawable.bitmap1);
//		l.addView(iv, lp1);
		ObjectAnimator animator = ObjectAnimator.ofFloat(l, "rotationX", 0, 1000);
		animator.setDuration(5000);
		animator.setRepeatCount(ValueAnimator.INFINITE);
		animator.setRepeatMode(ValueAnimator.REVERSE);
		animator.start();
		animator = ObjectAnimator.ofFloat(l, "rotationY", 0, 1000);
		animator.setDuration(10000);
		animator.setRepeatCount(ValueAnimator.INFINITE);
		animator.setRepeatMode(ValueAnimator.REVERSE);
//		animator.start();
		animator = ObjectAnimator.ofFloat(l, "scale", 1f, 0.3f);
		animator.setDuration(3000);
		animator.setRepeatCount(ValueAnimator.INFINITE);
		animator.setRepeatMode(ValueAnimator.REVERSE);
//		animator.start();
//		l.setRotationX(45);
		
		return fl;
	}
}
