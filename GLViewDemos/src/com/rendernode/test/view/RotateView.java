package com.rendernode.test.view;

import android.content.Context;

import com.glview.animation.ObjectAnimator;
import com.glview.animation.ValueAnimator;
import com.glview.content.GLContext;
import com.glview.graphics.drawable.Drawable;
import com.glview.hwui.GLCanvas;
import com.glview.view.View;
import com.rendernode.test.R;

public class RotateView extends View {
	
	Drawable mDrawable;

	public RotateView(Context context) {
		super(context);
		mDrawable = GLContext.get().getResources().getDrawable(R.drawable.bitmap2);
	}
	
	@Override
	protected void onLayout(boolean changeSize, int left, int top, int right,
			int bottom) {
		super.onLayout(changeSize, left, top, right, bottom);
		mDrawable.setBounds(0, 0, getWidth(), getHeight());
	}
	
	@Override
	protected void onDraw(GLCanvas canvas) {
		/*canvas.save();
		super.onDraw(canvas);
		canvas.translate(getWidth() / 2,  getHeight() / 2);
		canvas.rotate(70, 0, 1, 0);
		canvas.translate(- getWidth() / 2,  - getHeight() / 2);
		mDrawable.draw(canvas);
		canvas.restore();*/
		super.onDraw(canvas);
		mDrawable.draw(canvas);
	}
	
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		ObjectAnimator animator = ObjectAnimator.ofFloat(this, "rotationY", 0, 360);
		animator.setDuration(10000);
		animator.setRepeatCount(ValueAnimator.INFINITE);
		animator.setRepeatMode(ValueAnimator.REVERSE);
		animator.start();
		animator = ObjectAnimator.ofFloat(this, "scale", 1f, 0.3f);
		animator.setDuration(3000);
		animator.setRepeatCount(ValueAnimator.INFINITE);
		animator.setRepeatMode(ValueAnimator.REVERSE);
//		animator.start();
//		setRotationY(45);
//		setRotationX(45);
	}

}
