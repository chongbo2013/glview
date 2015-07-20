package com.rendernode.test;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.AttributeSet;

import com.glview.graphics.Bitmap;
import com.glview.graphics.drawable.BitmapDrawable;
import com.glview.graphics.shader.LinearGradient;
import com.glview.hwui.GLCanvas;
import com.glview.hwui.GLPaint;
import com.glview.widget.ImageView;

public class MyGLView extends ImageView {
	
	GLPaint mPaint = new GLPaint();

	BitmapDrawable mDrawable;
	
	public MyGLView(Context context) {
		this(context, null);
	}

	public MyGLView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public MyGLView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}
	
	void init() {
		mPaint.setColor(0xffffffff);
		mPaint.setShader(new LinearGradient(0, 0, 0, 100, Color.RED, Color.TRANSPARENT));
		mDrawable = new BitmapDrawable(new Bitmap(BitmapFactory.decodeResource(getContext().getResources(), R.drawable.ic_launcher)));
		setImageDrawable(mDrawable);
	}
	
	@Override
	protected void onDraw(GLCanvas canvas) {
		super.onDraw(canvas);
		canvas.drawRect(0, 0, getWidth(), getHeight(), mPaint);
//		canvas.drawBitmap(mDrawable.getBitmap(), 0, 0, mPaint);
		/*mDrawable.setBounds(0, 0, getWidth(), getHeight());
		mDrawable.draw(canvas);*/
	}

}
