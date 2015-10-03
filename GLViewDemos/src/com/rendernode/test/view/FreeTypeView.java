package com.rendernode.test.view;

import android.content.Context;
import android.graphics.Color;

import com.glview.hwui.GLCanvas;
import com.glview.hwui.GLPaint;
import com.glview.widget.FrameLayout;
import com.glview.widget.TextView;

public class FreeTypeView extends FrameLayout {
	
	String s = "这是啥玩意儿啊我去哦";
	
	GLPaint mPaint = new GLPaint();
	
	public FreeTypeView(Context context) {
		super(context);
		mPaint.setColor(Color.RED);
		TextView tv = new TextView(context);
		tv.setTextSize(100);
		tv.setText(s);
		addView(tv);
	}
	
	@Override
	protected void dispatchDraw(GLCanvas canvas) {
		super.dispatchDraw(canvas);
		mPaint.setTextSize(100);
		canvas.drawText(s, 0, 0, mPaint);
		
		mPaint.setTextSize(50);
		canvas.drawText(s, 0, 200, mPaint);
	}

}
