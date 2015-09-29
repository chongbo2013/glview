package com.rendernode.test.view;

import android.content.Context;

import com.glview.content.GLContext;
import com.glview.graphics.Bitmap;
import com.glview.graphics.Rect;
import com.glview.graphics.drawable.BitmapDrawable;
import com.glview.hwui.GLCanvas;
import com.glview.view.View;
import com.rendernode.test.R;

public class BatchView extends View {

	Bitmap mBitmap;
	
	Rect mRect = new Rect(0, 0, 50, 50);
	
	public BatchView(Context context) {
		super(context);
		mBitmap = ((BitmapDrawable) GLContext.get().getResources().getDrawable(R.drawable.ic_launcher)).getBitmap();
	}
	
	@Override
	protected void onDraw(GLCanvas canvas) {
		super.onDraw(canvas);
		for (int i = 0;i < 100; i ++) {
			canvas.drawBitmapBatch(mBitmap, null, mRect, null);
		}
	}

}
