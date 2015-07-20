package com.rendernode.test.view;

import android.content.Context;
import android.graphics.Color;

import com.glview.content.GLContext;
import com.glview.graphics.Bitmap;
import com.glview.graphics.drawable.BitmapDrawable;
import com.glview.graphics.mesh.CircleMesh;
import com.glview.hwui.GLCanvas;
import com.glview.hwui.GLPaint;
import com.glview.view.View;
import com.rendernode.test.R;
import com.rendernode.test.mesh.TestMesh;
import com.rendernode.test.mesh.TestMesh1;

public class MeshView extends View {
	
	CircleMesh mCircleMesh = new CircleMesh(500, 500);
	CircleMesh mCircle1Mesh = new CircleMesh(50, 50);
	CircleMesh mCircle2Mesh = new CircleMesh(50, 50);
	CircleMesh mCircle3Mesh = new CircleMesh(50, 50);
	
	TestMesh mTestMesh = new TestMesh(100, 100);
	
	TestMesh1 mTestMesh1 = new TestMesh1();
	
	GLPaint mPaint = new GLPaint();
	
	Bitmap mBitmap;

	public MeshView(Context context) {
		super(context);
		mPaint.setColor(Color.BLUE);
//		mPaint.setShader(new DefaultTextureShader());
//		mPaint.setStyle(Style.STROKE);
		
		mBitmap = ((BitmapDrawable) GLContext.get().getResources().getDrawable(R.drawable.bitmap1)).getBitmap();//new Bitmap(((BitmapDrawable) context.getDrawable(R.drawable.bitmap1)).getBitmap());
	}

	@Override
	protected void onDraw(GLCanvas canvas) {
		super.onDraw(canvas);
		/*for (int i = 0; i < 1; i ++) {
			canvas.drawMesh(mCircleMesh, mPaint);
		}*/
//		canvas.drawBitmap(mBitmap, 200, 500, mPaint);
//		canvas.drawBitmap(mBitmap, 1000, 100, mPaint);
		
		canvas.drawBitmapMesh(mBitmap, mCircleMesh, mPaint);
		
		canvas.translate(500, 0);
		canvas.drawMesh(mTestMesh, mPaint);
		
		canvas.translate(500, 0);
		canvas.drawMesh(mTestMesh1, mPaint);
	}

}
