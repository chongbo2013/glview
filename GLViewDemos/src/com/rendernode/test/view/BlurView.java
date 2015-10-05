package com.rendernode.test.view;

import android.content.Context;

import com.glview.animation.ObjectAnimator;
import com.glview.animation.ValueAnimator;
import com.glview.animation.ValueAnimator.AnimatorUpdateListener;
import com.glview.graphics.Bitmap;
import com.glview.graphics.drawable.BitmapDrawable;
import com.glview.graphics.shader.BlurShader;
import com.glview.hwui.GLCanvas;
import com.glview.hwui.GLPaint;
import com.glview.view.View;
import com.rendernode.test.R;

public class BlurView extends View {
	
	BlurShader mShader = new BlurShader();
	GLPaint mPaint = new GLPaint();
	
	Bitmap mBitmap;
	ValueAnimator mAnimator;

	public BlurView(Context context) {
		super(context);
		
		mPaint.setShader(mShader);
		
		mBitmap = ((BitmapDrawable) getResources().getDrawable(R.drawable.bitmap1)).getBitmap();
	}
	
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		mAnimator = ObjectAnimator.ofFloat(mShader, "radius", 1, 10).setDuration(1000);
		mAnimator.setRepeatCount(ValueAnimator.INFINITE);
		mAnimator.setRepeatMode(ValueAnimator.REVERSE);
		mAnimator.addUpdateListener(new AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				invalidate();
			}
		});
		mAnimator.start();
	}
	
	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		mAnimator.end();
		mAnimator = null;
	}
	
	@Override
	protected void onDraw(GLCanvas canvas) {
		super.onDraw(canvas);
		
		canvas.drawBitmap(mBitmap, 0, 0, mPaint);
	}

}
