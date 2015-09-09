package com.rendernode.test.demos;

import android.os.Bundle;

import com.glview.animation.ObjectAnimator;
import com.glview.animation.ValueAnimator;
import com.glview.animation.ValueAnimator.AnimatorUpdateListener;
import com.glview.graphics.Path;
import com.glview.graphics.Path.Direction;
import com.glview.view.Gravity;
import com.glview.view.View;
import com.glview.widget.FrameLayout;
import com.glview.widget.FrameLayout.LayoutParams;
import com.glview.widget.ImageView;
import com.glview.widget.ImageView.ScaleType;
import com.glview.widget.LinearLayout;
import com.rendernode.test.R;

public class PathMotionActivity extends BaseActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setGLContentView(new FrameLayout(this));
	}
	
	@Override
	public void onAttached(View content) {
		super.onAttached(content);
		LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER);
		
		final LinearLayout l = new LinearLayout(this);
		((FrameLayout) content).addView(l, lp);
		
		LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(300, 300);
//		LinearLayout.LayoutParams lp1 = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
		
		ImageView v = new ImageView(this);
		v.setImageResource(R.drawable.bitmap1);
		v.setScaleType(ScaleType.FIT_XY);
		l.addView(v, lp1);
		Path path = new Path();
//		path.addRect(0, 0, 500, 500, Direction.CCW);
		path.moveTo(0, 0);
//		path.addOval(0, 0, 100, 500, Direction.CW);
		path.arcTo(0, 0, 300, 300, 0, 90, false);
//		path.addRoundRect(-300, -200, 300, 200, 100, 100, Direction.CCW);
//		path.add
//		path.quadTo(300, 300, 100, - 300);
//		path.cubicTo(500, 500, -500, 20, -500, 300);
//		path.close();
//		path.lineTo(100, 100);
//		path.lineTo(100, 500);
//		ObjectAnimator animator = ObjectAnimator.ofFloat(l.getChildAt(1), "rotation", 0, 1000);
		ObjectAnimator animator = ObjectAnimator.ofFloat(v, View.TRANSLATION_X, View.TRANSLATION_Y, path);
		animator.setDuration(5000);
		animator.setRepeatCount(ValueAnimator.INFINITE);
		animator.setRepeatMode(ValueAnimator.RESTART);
		animator.start();
		animator.addUpdateListener(new AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
//				l.getChildAt(0).invalidate();
			}
		});
	}
}
