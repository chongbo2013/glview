package com.rendernode.test.view;

import android.content.Context;
import android.util.AttributeSet;

import com.glview.animation.ValueAnimator;
import com.glview.animation.ValueAnimator.AnimatorUpdateListener;
import com.glview.content.GLContext;
import com.glview.graphics.Bitmap;
import com.glview.graphics.Rect;
import com.glview.graphics.drawable.BitmapDrawable;
import com.glview.graphics.shader.RippleShader;
import com.glview.hwui.GLCanvas;
import com.glview.hwui.GLPaint;
import com.glview.view.View;
import com.rendernode.test.R;

public class RippleShaderView extends View {
	
	GLPaint mPaint = new GLPaint();
	RippleShader mRippleShader;
	
	Bitmap mBitmap;
	
	Rect mSourceRect = new Rect();
	Rect mTargetRect = new Rect();
	
	ValueAnimator mAnimator;

	public RippleShaderView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public RippleShaderView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}
	
	void init() {
		mBitmap = ((BitmapDrawable) GLContext.get().getResources().getDrawable(R.drawable.bitmap2)).getBitmap();
		mSourceRect.set(0, 0, mBitmap.getWidth(), mBitmap.getHeight());
		mRippleShader = new RippleShader(mBitmap.getWidth(), mBitmap.getHeight(), true);
		mPaint.setShader(mRippleShader);
	}
	
	@Override
	protected void onLayout(boolean changeSize, int left, int top, int right,
			int bottom) {
		super.onLayout(changeSize, left, top, right, bottom);
		mTargetRect.set(0, 0, getWidth(), getHeight());
	}
	
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		mAnimator = ValueAnimator.ofFloat(0, 1);
		mAnimator.setRepeatCount(ValueAnimator.INFINITE);
		mAnimator.setRepeatMode(ValueAnimator.RESTART);
		mAnimator.setDuration(3000);
		mAnimator.addUpdateListener(new AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				float value = (Float) animation.getAnimatedValue();
				mRippleShader.setProgress(value);
				invalidate();
			}
		});
		mAnimator.start();
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if (mAnimator != null) {
			mAnimator.end();
			mAnimator = null;
		}
	}
	
	@Override
	protected void onDraw(GLCanvas canvas) {
//		canvas.drawBitmap(mBitmap, 0, 0, mPaint);
		canvas.drawBitmap(mBitmap, mSourceRect, mTargetRect, mPaint);
	}

}
