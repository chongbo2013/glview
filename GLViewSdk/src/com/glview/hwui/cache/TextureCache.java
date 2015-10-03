package com.glview.hwui.cache;

import javax.microedition.khronos.opengles.GL11;

import android.opengl.GLUtils;
import android.os.Build;
import android.support.v4.util.LruCache;
import android.util.Log;

import com.glview.App;
import com.glview.content.GLContext;
import com.glview.graphics.Bitmap;
import com.glview.hwui.Caches;
import com.glview.hwui.GLId;
import com.glview.hwui.Texture;
import com.glview.libgdx.graphics.opengl.GL;
import com.glview.libgdx.graphics.opengl.GL20;

public class TextureCache {
	
	final static String TAG = "TextureCache";
	
	final static int MB = 1024 * 1024;
	final static int DEFAULT_TEXTURE_CACHE_SIZE = 24 * MB; //24MB
	final static int LARGE_TEXTURE_CACHE_SIZE = 32 * MB; //32MB
	final static int LARGER_TEXTURE_CACHE_SIZE = 64 * MB; //64MB
	final static float DEFAULT_TEXTURE_CACHE_FLUSH_RATE = 0.6f;
	
	TextureLruCache mCache;
	
	int[] mBuffer = new int[1];
	
	float mFlushRate;
	int mSize;
	
	public TextureCache() {
		final boolean isLargeHeap = GLContext.get().isLargeHeap();
		mSize = DEFAULT_TEXTURE_CACHE_SIZE;
		final int memoryClass = isLargeHeap ? GLContext.get().getLargeMemoryClass() : GLContext.get().getMemoryClass();
		if (memoryClass <= 48) {
			mSize = memoryClass / 2 * MB;
		} else if (memoryClass >= 256) {
			mSize = LARGER_TEXTURE_CACHE_SIZE;
		} else if (memoryClass >= 128) {
			mSize = LARGE_TEXTURE_CACHE_SIZE;
		}
		Log.d(TAG, "TextureCache size=" + mSize);
		mFlushRate = DEFAULT_TEXTURE_CACHE_FLUSH_RATE;
		mCache = new TextureLruCache(mSize);
	}
	
	public Texture get(Bitmap bitmap) {
		if (bitmap == null) return null;
		
		try {
			Texture texture = mCache.get(bitmap);
			boolean sizeChanged = false;
			if (texture == null) {
				if (!canMakeTextureFromBitmap(bitmap)) {
					return null;
				}
				texture = create(bitmap);
				if (!generateTexture(bitmap, texture, false)) {
					return null;
				}
				sizeChanged = true;
			} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1 && texture.mGenerationId != bitmap.getGenerationId()) {
				if (bitmap.getRowBytes() * bitmap.getHeight() != texture.getByteCount()) {
					sizeChanged = true;
					mCache.remove(bitmap);
				}
				if (!generateTexture(bitmap, texture, true)) {
					mCache.remove(bitmap);
					return null;
				}
			}
			if (sizeChanged) {
				if (mCache.maxSize() < texture.getByteCount()) {
					mCache.resize(texture.getByteCount());
				} else if (mCache.maxSize() > mSize) {
					mCache.resize(mSize);
				}
				mCache.put(bitmap, texture);
			}
			return texture;
		} catch (Throwable throwable) {
			Log.w(TAG, "getTexture fail", throwable);
		}
		return null;
	}
	
	boolean canMakeTextureFromBitmap(Bitmap bitmap) {
		if (bitmap.isRecycled() || bitmap.getBitmap() == null) {
			return false;
		}
		return true;
	}
	
	Texture create(Bitmap bitmap) {
		Texture texture = new Texture();
		texture.setWidth(bitmap.getWidth());
		texture.setHeight(bitmap.getHeight());
		android.graphics.Bitmap aBitmap = bitmap.getBitmap();
		if (aBitmap != null && !aBitmap.isRecycled()) {
			int format = GLUtils.getInternalFormat(aBitmap);
			int type = GLUtils.getType(aBitmap);
			texture.setFormat(format);
			texture.setType(type);
		}
		return texture;
	}
	
	void deleteTexture(Texture texture) {
		Caches.getInstance().deleteTexture(texture);
	}
	
	boolean hasNpot() {
		return Caches.getInstance().extensions.hasNPot();
	}
	
	public void generateTexture(Texture texture) {
		final GL gl = App.getGL();
		boolean regenerate = false;
		if (texture.mId > 0) {
			regenerate = true;
		} else {
			GLId.glGenTextures(1, mBuffer, 0);
			texture.mId = mBuffer[0];
		}
		Caches.getInstance().bindTexture(texture);
		gl.glTexImage2D(GL11.GL_TEXTURE_2D, 0, texture.getFormat(),
				texture.getWidth(), texture.getHeight(),
				0, texture.getFormat(), texture.getType(), null);
		if (!regenerate) {
			gl.glTexParameterf(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_S,
					GL20.GL_CLAMP_TO_EDGE);
			gl.glTexParameterf(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_T,
					GL20.GL_CLAMP_TO_EDGE);
			gl.glTexParameterf(GL20.GL_TEXTURE_2D,
					GL20.GL_TEXTURE_MIN_FILTER, GL20.GL_LINEAR);
			gl.glTexParameterf(GL20.GL_TEXTURE_2D,
					GL20.GL_TEXTURE_MAG_FILTER, GL20.GL_LINEAR);
		}
	}
	
	boolean generateTexture(Bitmap bitmap, Texture texture, boolean regenerate) {
		android.graphics.Bitmap aBitmap = bitmap.getBitmap();
		if (aBitmap == null || aBitmap.isRecycled()) {
			return false;
		}
		
		final boolean canMipMap = hasNpot();
		
		final boolean resize = !regenerate
				|| bitmap.getWidth() != texture.getWidth()
				|| bitmap.getHeight() != texture.getHeight()
				|| (regenerate && canMipMap && texture.isMipMap() && bitmap.hasMipMap());
		
		if (!regenerate) {
			if (texture.mId > 0) {
				deleteTexture(texture);
			}
			GLId.glGenTextures(1, mBuffer, 0);
			texture.mId = mBuffer[0];
		}
		texture.mGenerationId = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1 ? bitmap.getGenerationId() : 0;
		texture.setWidth(bitmap.getWidth());
		texture.setHeight(bitmap.getHeight());
		texture.setByteCount(bitmap.getRowBytes() * bitmap.getHeight());

		Caches.getInstance().bindTexture(texture);
		
		GL gl = App.getGL();
		
		
		if (resize) {
			GLUtils.texImage2D(GL20.GL_TEXTURE_2D, 0, texture.getFormat(), aBitmap, texture.getType(), 0);
		} else {
			GLUtils.texSubImage2D(GL20.GL_TEXTURE_2D, 0, 0, 0, aBitmap, texture.getFormat(), texture.getType());
		}
		
		if (canMipMap) {
			texture.setMipMap(bitmap.hasMipMap());
			if (texture.isMipMap() && (gl instanceof GL20)) {
				((GL20) gl).glGenerateMipmap(GL20.GL_TEXTURE_2D);
			}
		}
		
		if (!regenerate) {
			gl.glTexParameterf(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_S,
					GL20.GL_CLAMP_TO_EDGE);
			gl.glTexParameterf(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_T,
					GL20.GL_CLAMP_TO_EDGE);
			gl.glTexParameterf(GL20.GL_TEXTURE_2D,
					GL20.GL_TEXTURE_MIN_FILTER, GL20.GL_LINEAR);
			gl.glTexParameterf(GL20.GL_TEXTURE_2D,
					GL20.GL_TEXTURE_MAG_FILTER, GL20.GL_LINEAR);
		}
		bitmap.freeBitmap();
		return true;
	}
	
	public void clear() {
		mCache.evictAll();
	}
	
	public void flush() {
		if (mFlushRate > 1f || mCache.size() == 0) {
			return;
		}
		if (mFlushRate <= 0.0f) {
	        clear();
	        return;
	    }
		int targetSize = (int) (mSize * mFlushRate);
		if (mCache.size() > targetSize) {
			mCache.trimToSize(targetSize);
		}
	}
	
	int getEntrySize(Bitmap bitmap) {
		return bitmap.getRowBytes() * bitmap.getHeight();
	}
	
	class TextureLruCache extends LruCache<Object, Texture> {

		public TextureLruCache(int maxSize) {
			super(maxSize);
		}
		
		@Override
		protected int sizeOf(Object key, Texture value) {
			return value.getByteCount();
		}
		
		@Override
		protected void entryRemoved(boolean evicted, Object bitmap,
				Texture oldValue, Texture newValue) {
			if (oldValue != newValue) {
				deleteTexture(oldValue);
			}
		}
		
		@Override
		protected Texture create(Object bitmap) {
			return null;
		}
		
		@Override
		protected void finalize() throws Throwable {
			super.finalize();
			evictAll();
		}
	}
	
}
