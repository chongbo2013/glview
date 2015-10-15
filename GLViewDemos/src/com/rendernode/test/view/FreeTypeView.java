package com.rendernode.test.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import com.glview.hwui.GLCanvas;
import com.glview.hwui.GLPaint;
import com.glview.text.Layout;
import com.glview.text.StaticLayout;
import com.glview.widget.FrameLayout;
import com.glview.widget.TextView;

public class FreeTypeView extends FrameLayout {
	
	String s = "Helloeverybody,thisislijingspeaking,whoareyoune";
	
	GLPaint mPaint = new GLPaint();
	Paint mAndroidPaint = new Paint();
	
	Layout mLayout, mLayout1;
	
	public FreeTypeView(Context context) {
		super(context);
		mPaint.setColor(Color.RED);
		mPaint.setTextSize(100);
		TextView tv = new TextView(context);
		tv.setTextSize(100);
		tv.setText(s);
//		addView(tv);
		mAndroidPaint.setTextSize(100);
		
//		BoringLayout.Metrics metrics = BoringLayout.isBoring(s, mPaint);
//		if (metrics != null) {
//			mLayout = BoringLayout.make(s, mPaint, 1000, Layout.Alignment.ALIGN_NORMAL, 1, 0, metrics, false, TextUtils.TruncateAt.END, 1000, true);
//		}
		mLayout = new StaticLayout(s, mPaint, 1000, Layout.Alignment.ALIGN_OPPOSITE, 1f, 0, false, true);
		
		mLayout1 = new StaticLayout("啊哦heihei", mPaint, 1000, Layout.Alignment.ALIGN_OPPOSITE, 1f, 0, false, true);
	}
	
	@Override
	protected void dispatchDraw(GLCanvas canvas) {
		super.dispatchDraw(canvas);
		mPaint.setTextSize(100);
		Log.d("lijing", "glview=" + mPaint.measureText(s));
		Log.d("lijing", "glview=" + mPaint.getFontMetricsInt());
		
		Log.d("lijing", "android=" + mAndroidPaint.measureText(s));
		Log.d("lijing", "android=" + mAndroidPaint.getFontMetricsInt());
		
//		canvas.drawText(s, 0, 0, mPaint);
		if (mLayout != null) {
			mLayout.draw(canvas);
			mLayout1.draw(canvas);
		}
//		mPaint.setTextSize(50);
//		canvas.drawText(s, 0, 200, mPaint);
//		GLPaint paint = mPaint;
//		Typeface typeface = paint.getTypeface();
//		Face face = typeface.face();
//		int textSize = paint.getTextSize();
//		if (textSize < 5) return;
//		face.setPixelSizes(0, textSize);
//		for (int i = 0; i < s.length(); i ++) {
//			char c = s.charAt(i);
//			if (!face.loadChar(c, FreeType.FT_LOAD_DEFAULT)) {
//				continue;
//			}
//			FreeType.GlyphSlot slot = face.getGlyph();
//			FreeType.Glyph mainGlyph = slot.getGlyph();
//			
//			Log.d("lijing", "---c=" + c);
//			Log.d("lijing", "---xAdvance=" + FreeType.toInt(slot.getAdvanceX()));
//			Log.d("lijing", "---HAdvance=" + slot.getMetrics().getHoriAdvance());
//			mainGlyph.toBitmap(FreeType.FT_RENDER_MODE_NORMAL);
//			Log.d("lijing", "---width=" + mainGlyph.getBitmap().getWidth());
//		}
	}

}
