package com.rendernode.test.demos;

import android.graphics.PixelFormat;
import android.os.Bundle;

import com.glview.animation.ObjectAnimator;
import com.glview.animation.ValueAnimator;
import com.glview.animation.ValueAnimator.AnimatorUpdateListener;
import com.glview.graphics.Bitmap;
import com.glview.view.Gravity;
import com.glview.view.View;
import com.glview.view.ViewTreeObserver.OnPreDrawListener;
import com.glview.widget.FrameLayout;
import com.glview.widget.FrameLayout.LayoutParams;
import com.glview.widget.ImageView;
import com.glview.widget.ImageView.ScaleType;
import com.glview.widget.LinearLayout;
import com.rendernode.test.R;

public class HardwareLayerActivity extends BaseActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setGLContentView(new FrameLayout(this));
		getSurfaceView().setZOrderOnTop(true);
		getSurfaceView().getHolder().setFormat(PixelFormat.TRANSLUCENT);
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
		v = new ImageView(this);
		v.setImageResource(R.drawable.bitmap1);
		v.setScaleType(ScaleType.FIT_XY);
//		v.setLayerType(View.LAYER_TYPE_HARDWARE);
		l.addView(v, lp1);
		v = new ImageView(this);
		v.setImageResource(R.drawable.bitmap1);
		v.setScaleType(ScaleType.FIT_XY);
		l.addView(v, lp1);
//		l.setLayerType(View.LAYER_TYPE_HARDWARE);
		ObjectAnimator animator = ObjectAnimator.ofFloat(l.getChildAt(1), "rotation", 0, 1000);
		animator.setDuration(5000);
		animator.setRepeatCount(ValueAnimator.INFINITE);
		animator.setRepeatMode(ValueAnimator.REVERSE);
//		animator.start();
		animator.addUpdateListener(new AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
//				l.getChildAt(0).invalidate();
			}
		});
		final ImageView cache = new ImageView(this);
		cache.setScaleType(ScaleType.FIT_XY);
//		cache.setBackgroundColor(Color.RED);
		lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		((FrameLayout) content).addView(cache, lp);
		l.getViewTreeObserver().addOnPreDrawListener(new OnPreDrawListener() {
			@Override
			public boolean onPreDraw() {
				Bitmap bitmap = l.getDrawingCache();
				cache.setImageBitmap(bitmap);
				l.getViewTreeObserver().removeOnPreDrawListener(this);
				return false;
			}
		});
	}
}
