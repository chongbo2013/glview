package com.rendernode.test.view;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import com.glview.animation.Animator;
import com.glview.animation.AnimatorListenerAdapter;
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

public class SlideshowView extends View {
	
	final static String TAG = "SlideshowView";
	
	GLPaint mPaint = new GLPaint();
	GLPaint mPaint2 = new GLPaint();
	RippleShader mRippleShader;
	
	int mCurrentImage = 0;
	List<Bitmap> mBitmaps = new ArrayList<Bitmap>();
	List<Rect> mSourceRects = new ArrayList<Rect>();
	Rect mTargetRect = new Rect();
	
	ValueAnimator mAnimator;

	public SlideshowView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public SlideshowView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}
	
	private void init() {
		mBitmaps.add(((BitmapDrawable) GLContext.get().getResources().getDrawable(R.drawable.bitmap1)).getBitmap());
		mBitmaps.add(((BitmapDrawable) GLContext.get().getResources().getDrawable(R.drawable.bitmap2)).getBitmap());
		mBitmaps.add(((BitmapDrawable) GLContext.get().getResources().getDrawable(R.drawable.bitmap3)).getBitmap());
		mBitmaps.add(((BitmapDrawable) GLContext.get().getResources().getDrawable(R.drawable.bitmap4)).getBitmap());
		for (Bitmap bitmap : mBitmaps) {
			mSourceRects.add(new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()));
		}
		mRippleShader = new RippleShader();
		mPaint.setShader(mRippleShader);
		mPaint2.setShader(mRippleShader);
	}
	
	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		Log.d(TAG, "onAttachedToWindow---" + this);
		mAnimator = ValueAnimator.ofFloat(0, 1);
		mAnimator.setRepeatCount(ValueAnimator.INFINITE);
		mAnimator.setRepeatMode(ValueAnimator.RESTART);
		mAnimator.setDuration(3000);
		mAnimator.addUpdateListener(new AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				float value = (Float) animation.getAnimatedValue();
				mRippleShader.setProgress(value);
				mPaint.setAlpha((int) (255 * (1 - value)));
				mPaint2.setAlpha((int) (255 * value));
				invalidate();
			}
		});
		mAnimator.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationRepeat(Animator animation) {
				mCurrentImage ++;
				if (mCurrentImage >= mBitmaps.size()) {
					mCurrentImage = 0;
				}
			}
		});
		mAnimator.start();
	}
	
	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		Log.d(TAG, "onDetachedFromWindow---" + this);
		if (mAnimator != null) {
			mAnimator.end();
			mAnimator = null;
		}
	}
	
	@Override
	protected void onLayout(boolean changeSize, int left, int top, int right,
			int bottom) {
		super.onLayout(changeSize, left, top, right, bottom);
		mTargetRect.set(0, 0, right, bottom);
	}
	
	@Override
	protected void onDraw(GLCanvas canvas) {
		int next = mCurrentImage + 1;
		if (next >= mBitmaps.size()) {
			next = 0;
		}
		canvas.drawBitmap(mBitmaps.get(next), mSourceRects.get(next), mTargetRect, mPaint2);
		canvas.drawBitmap(mBitmaps.get(mCurrentImage), mSourceRects.get(mCurrentImage), mTargetRect, mPaint);
	}

}
