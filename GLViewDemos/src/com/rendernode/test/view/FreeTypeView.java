package com.rendernode.test.view;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import android.content.Context;
import android.graphics.Bitmap.Config;
import android.util.Log;

import com.badlogic.gdx.graphics.g2d.freetype.FreeType;
import com.badlogic.gdx.graphics.g2d.freetype.FreeType.Bitmap;
import com.badlogic.gdx.graphics.g2d.freetype.FreeType.Face;
import com.badlogic.gdx.graphics.g2d.freetype.FreeType.GlyphSlot;
import com.badlogic.gdx.graphics.g2d.freetype.FreeType.Library;
import com.glview.hwui.GLCanvas;
import com.glview.view.View;

public class FreeTypeView extends View {
	
	com.glview.graphics.Bitmap mBitmap;

	public FreeTypeView(Context context) {
		super(context);
		
		Library library = FreeType.initFreeType();
		byte[] buffer = new byte[1024];
		try {
			InputStream is = context.getAssets().open("font/lantingxihei.TTF");
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			int bytesRead;
			while ((bytesRead = is.read(buffer)) != -1) {
				os.write(buffer, 0, bytesRead);
			}
			Face face = library.newMemoryFace(os.toByteArray(), os.size(), 0);
			face.setPixelSizes(0, 200);
			face.loadChar('我', FreeType.FT_LOAD_DEFAULT);
			GlyphSlot slot = face.getGlyph();
			FreeType.Glyph mainGlyph = slot.getGlyph();
			mainGlyph.toBitmap(FreeType.FT_RENDER_MODE_NORMAL);
			Bitmap mainBitmap = mainGlyph.getBitmap();
			ByteBuffer src = mainBitmap.getBuffer();
			android.graphics.Bitmap b = android.graphics.Bitmap.createBitmap(mainBitmap.getWidth(), mainBitmap.getRows(), Config.ARGB_8888);
			int[] colors = new int[mainBitmap.getWidth() * mainBitmap.getRows()];
			int srcPitch = mainBitmap.getPitch();
			int width = mainBitmap.getWidth();
			for (int y = 0; y < mainBitmap.getRows(); y++) {
				int ySrcPitch = y * srcPitch;
				int yWidth = y * width;
				for (int x = 0; x < width; x++) {
					//use the color value of the foreground color, blend alpha
					byte alpha = src.get(ySrcPitch + x);
					colors[yWidth + x] = android.graphics.Color.argb(alpha, 255, 255, 255);;
				}
			}
			b.setPixels(colors, 0, mainBitmap.getWidth(), 0, 0, mainBitmap.getWidth(), mainBitmap.getRows());
			mBitmap = new com.glview.graphics.Bitmap(b);
		} catch (IOException e) {
		}
	}
	
	@Override
	protected void onDraw(GLCanvas canvas) {
		if (mBitmap != null) {
			canvas.translate(100, 100);
			canvas.drawBitmap(mBitmap, 0, 0, null);
		}
	}

}
