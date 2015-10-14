package com.rendernode.test.android.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.widget.Button;

public class MyButton extends Button {
	
	TextPaint mPaint;

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

	public MyButton(Context context, AttributeSet attrs, int defStyleAttr,
			int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		init();
	}
	
	void init() {
		mPaint = new TextPaint();
		mPaint.setTextSize(100);
		mPaint.setColor(Color.RED);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		canvas.drawText("Test111", 100, 100, mPaint);
	}

}
