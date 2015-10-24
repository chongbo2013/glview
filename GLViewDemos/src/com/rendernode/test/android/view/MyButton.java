package com.rendernode.test.android.view;

import com.glview.util.FPSUtils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.widget.Button;

public class MyButton extends Button {
	
	TextPaint mPaint;
	TextPaint mPaint1;

	public MyButton(Context context) {
		super(context);
		init();
	}

	public MyButton(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public MyButton(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		init();
	}
	
	static Typeface sTypeface;
	
	void init() {
		if (sTypeface == null) {
			sTypeface = Typeface.createFromAsset(getContext().getAssets(), "fonts/DroidSansFallback.ttf");
		}
		setTypeface(sTypeface);
		mPaint = new TextPaint();
		mPaint.setTextSize(100);
		mPaint.setColor(Color.RED);
		mPaint1 = new TextPaint();
		mPaint1.setTextSize(100);
		mPaint1.setColor(Color.RED);
		mPaint1.setAntiAlias(true);
	}
	
	FPSUtils fps = new FPSUtils(this);
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
//		canvas.drawText("Test111", 0, 100, mPaint);
//		canvas.drawText("Test111", 0, 300, mPaint1);
		invalidate();
		fps.fps();
	}

}
