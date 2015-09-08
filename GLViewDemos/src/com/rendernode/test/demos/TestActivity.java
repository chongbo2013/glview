package com.rendernode.test.demos;

import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Log;

import com.glview.graphics.Matrix33;
import com.glview.graphics.PointF;
import com.glview.util.MatrixUtil;
import com.rendernode.test.R;

public class TestActivity extends BaseActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setGLContentView(R.layout.gl_layout_test);
		
		Matrix mat = new Matrix();
		mat.postTranslate(100, 100);
		mat.postTranslate(100, 100);
		mat.preScale(1.5f, 2);
		float[] r = new float[]{50f, 50f};
		mat.mapPoints(r);
		Log.d("lijing", "----android---r0=" + r[0]);
		Log.d("lijing", "-------r1=" + r[1]);
		
		Matrix33 mat33 = new Matrix33();
		mat33.postTranslate(100, 100);
		mat33.postTranslate(100, 100);
		mat33.preScale(1.5f, 2);
		Log.d("lijing", "-----Matrix33--pt=" + mat33.mapPoint(new PointF(), 50, 50));
		
		float[] m = new float[16];
		android.opengl.Matrix.setIdentityM(m, 0);
		android.opengl.Matrix.translateM(m, 0, 100, 100, 0);
		android.opengl.Matrix.translateM(m, 0, 100, 100, 0);
		android.opengl.Matrix.scaleM(m, 0, 1.5f, 2, 1);
//		Matrix1.rotateM(m, 0, 180, 0, 0, 1);
//		Matrix.rotateM(m, 0, 90, 0, 0, 1);
		r = MatrixUtil.mapPoint(m, 50f, 50f);
		Log.d("lijing", "----opengl---r0=" + r[0]);
		Log.d("lijing", "-------r1=" + r[1]);
	}
}
