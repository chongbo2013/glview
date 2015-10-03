package com.glview.hwui.font;

import java.nio.ByteBuffer;
import java.util.Vector;

import android.support.v4.util.LongSparseArray;

import com.badlogic.gdx.graphics.g2d.freetype.FreeType;
import com.badlogic.gdx.graphics.g2d.freetype.FreeType.Face;
import com.badlogic.gdx.graphics.g2d.freetype.FreeType.SizeMetrics;
import com.glview.graphics.Typeface;
import com.glview.hwui.GLCanvas;
import com.glview.hwui.GLPaint;
import com.glview.hwui.packer.PackerRect;
import com.glview.libgdx.graphics.opengl.GL20;

public class FontRenderer {
	
	static ThreadLocal<FontRenderer> sThreadLocal = new ThreadLocal<FontRenderer>() {
		@Override
		protected FontRenderer initialValue() {
			return new FontRenderer();
		}
	};
	
	private FontRenderer() {}
	
	public static FontRenderer instance() {
		return sThreadLocal.get();
	}
	
	private boolean mInitialized;
	
	int mSmallCacheWidth = 512;
	int mSmallCacheHeight = 512;
	int mLargeCacheWidth = 1024;
	int mLargeCacheHeight = 1024;

    Vector<CacheTexture> mACacheTextures = new Vector<CacheTexture>();
    Vector<CacheTexture> mRGBACacheTextures = new Vector<CacheTexture>();
    
    LongSparseArray<FontRect> mCacheRects = new LongSparseArray<FontRect>();
    
    LongSparseArray<FontData> mFontDatas = new LongSparseArray<FontData>();
    
    GLCanvas mCanvas = null;
    
    public void release() {
    	clearCacheTextures(mACacheTextures);
    	clearCacheTextures(mRGBACacheTextures);
    	mCacheRects.clear();
    	mFontDatas.clear();
    	mInitialized = false;
    }
    
	// We don't want to allocate anything unless we actually draw text
	void checkInit() {
	    if (mInitialized) {
	        return;
	    }

	    initTextTexture();

	    mInitialized = true;
	}
	
	void initTextTexture() {
	    clearCacheTextures(mACacheTextures);
	    clearCacheTextures(mRGBACacheTextures);
//
//	    mUploadTexture = false;
	    mACacheTextures.add(createCacheTexture(mSmallCacheWidth, mSmallCacheHeight,
	    		GL20.GL_ALPHA, true));
	    mACacheTextures.add(createCacheTexture(mLargeCacheWidth, mLargeCacheHeight >> 1,
	    		GL20.GL_ALPHA, false));
	    mACacheTextures.add(createCacheTexture(mLargeCacheWidth, mLargeCacheHeight >> 1,
	            GL20.GL_ALPHA, false));
	    mACacheTextures.add(createCacheTexture(mLargeCacheWidth, mLargeCacheHeight,
	    		GL20.GL_ALPHA, false));
//	    mRGBACacheTextures.add(createCacheTexture(mSmallCacheWidth, mSmallCacheHeight,
//	    		GL20.GL_RGBA, false));
//	    mRGBACacheTextures.add(createCacheTexture(mLargeCacheWidth, mLargeCacheHeight >> 1,
//	    		GL20.GL_RGBA, false));
	}
	
	void clearCacheTextures(Vector<CacheTexture> cacheTextures) {
	    for (int i = 0; i < cacheTextures.size(); i++) {
	        cacheTextures.get(i).release();
	    }
	    cacheTextures.clear();
	}
	
	CacheTexture createCacheTexture(int width, int height, int format,
	        boolean allocate) {
	    CacheTexture cacheTexture = new CacheTexture(this, width, height, format);

	    if (allocate) {
	        cacheTexture.allocateTexture();
	        cacheTexture.allocateMesh();
	    }

	    return cacheTexture;
	}
	
	public void flushBatch() {
		for (CacheTexture cacheTexture : mACacheTextures) {
			if (cacheTexture.mFontBatch != null) {
				cacheTexture.mFontBatch.flush();
			}
		}
	}
	
	public void setGLCanvas(GLCanvas canvas) {
		mCanvas = canvas;
	}
	
	public GLCanvas getGLCanvas() {
		return mCanvas;
	}
 	
	public void renderText(GLCanvas canvas, CharSequence text, int start, int end, float x, float y,
			float alpha, GLPaint paint) {
		checkInit();
		Typeface typeface = paint.getTypeface();
		Face face = typeface.face();
		int textSize = paint.getTextSize();
		if (textSize < 5) return;
		face.setPixelSizes(0, textSize);
		long k = typeface.index() << 10 | textSize;
		FontData fontData = mFontDatas.get(k);
		if (fontData == null) {
			fontData = new FontData();
			SizeMetrics fontMetrics = face.getSize().getMetrics();
			fontData.ascent = FreeType.toInt(fontMetrics.getAscender());
			fontData.descent = FreeType.toInt(fontMetrics.getDescender());
			fontData.lineHeight = FreeType.toInt(fontMetrics.getHeight());
			if (face.loadChar(' ', FreeType.FT_LOAD_DEFAULT)) {
				fontData.spaceWidth = FreeType.toInt(face.getGlyph().getMetrics().getHoriAdvance());
			} else {
				fontData.spaceWidth = FreeType.toInt(face.getMaxAdvanceWidth());
			}
		}
		
		float baseline = y + fontData.ascent;
		
		for (int index = 0; index < end - start; index ++) {
			char c = text.charAt(index + start);
			if (c == ' ') {
				x += fontData.spaceWidth;
				continue;
			}
			int charIndex = face.getCharIndex(c);
			if (charIndex == 0) {
				c = 0;
				charIndex = face.getCharIndex(c);
			}
			if (charIndex == 0) continue;
			long key = charIndex << 20 | typeface.index() << 10 | textSize;
			FontRect r = mCacheRects.get(key);
			if (r == null) {
				if (!face.loadChar(c, FreeType.FT_LOAD_DEFAULT)) {
					continue;
				}
				FreeType.GlyphSlot slot = face.getGlyph();
				FreeType.Glyph mainGlyph = slot.getGlyph();
				try {
					mainGlyph.toBitmap(FreeType.FT_RENDER_MODE_NORMAL);
				} catch (RuntimeException e) {
					mainGlyph.dispose();
					continue;
				}
				FreeType.Bitmap mainBitmap = mainGlyph.getBitmap();
				int w = mainBitmap.getWidth();
				int h = mainBitmap.getRows();
				for (CacheTexture cacheTexture : mACacheTextures) {
					PackerRect rect = cacheTexture.mPacker.insert(w, h);
					if (rect != null) {
						r = new FontRect(cacheTexture, rect, mainGlyph.getLeft(), mainGlyph.getTop());
						if (cacheTexture.getPixelBuffer() == null) {
							cacheTexture.allocateTexture();
						}
						ByteBuffer byteBuffer = cacheTexture.getPixelBuffer().map();
						ByteBuffer buffer = mainBitmap.getBuffer();
						int pitch = mainBitmap.getPitch();
						for (int i = rect.rect().top; i < rect.rect().bottom; i ++) {
							for (int j = rect.rect().left; j < rect.rect().right; j ++) {
								byteBuffer.put(i * cacheTexture.mWidth + j, buffer.get((i - rect.rect().top) * pitch + j - rect.rect().left));
							}
						}
						cacheTexture.mDirtyRect.union(rect.rect());
						cacheTexture.setDirty(true);
						mCacheRects.put(key, r);
						break;
					}
				}
				mainGlyph.dispose();
			}
			if (r != null) {
				r.mTexture.allocateMesh();
				r.mTexture.mFontBatch.draw(x + r.mLeft, baseline - r.mRect.height(), r.mRect.width(), r.mRect.height(), r.mRect.rect().left, r.mRect.rect().top, r.mRect.width(), r.mRect.height(), alpha, paint);
				x += r.mRect.width() + r.mLeft;
			} else {
			}
		}
	}
	
	private static class FontData {
		int ascent;
		int descent;
		int lineHeight;
		int spaceWidth;
	}

}
