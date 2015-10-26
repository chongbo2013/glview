package com.rendernode.test.demos;

import android.os.Bundle;
import android.util.Log;

import com.glview.stackblur.BlurProcess;
import com.glview.stackblur.JavaBlurProcess;
import com.glview.stackblur.NativeBlurProcess;
import com.rendernode.test.view.FreeTypeView;

public class TestActivity extends BaseActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setGLContentView(new FreeTypeView(this));
		
		int w = 30;
		int h = 30;
		int radius = 15;
		BlurProcess blur;
		
		byte[] b = new byte[w * h];
		
		blur = new JavaBlurProcess();
		Log.d("lijing", "java blur start");
		for (int i = 0; i < 300; i ++) {
			blur.blur(b, w, h, w, radius);
		}
		Log.d("lijing", "java blur end");
		
		blur = new NativeBlurProcess();
        Log.d("lijing", "native blur start");
		for (int i = 0; i < 300; i ++) {
			blur.blur(b, w, h, w, radius);
		}
		Log.d("lijing", "native blur end");
//		Bitmap overlay = ((BitmapDrawable) getResources().getDrawable(R.drawable.bitmap2)).getBitmap();//BitmapFactory.decodeResource(getResources(), R.drawable.bitmap2);
//		Log.d("lijing", "rs blur start " + overlay.getWidth() + ", " + overlay.getHeight());
//		RenderScript rs = RenderScript.create(this);
//		Allocation overlayAlloc = Allocation.createFromBitmap(
//                rs, overlay, Allocation.MipmapControl.MIPMAP_NONE,
//                Allocation.USAGE_SCRIPT);
//        ScriptIntrinsicBlur sblur = ScriptIntrinsicBlur.create(
//                rs, Element.U8_4(rs));
//        sblur.setInput(overlayAlloc);
//        sblur.setRadius(radius);
//
//		sblur.forEach(overlayAlloc);
//		Log.d("lijing", "rs blur end");
//        overlayAlloc.copyTo(overlay);
        
//        getWindow().setBackgroundDrawable(new BitmapDrawable(overlay));
	}
	
}
