package com.glview.hwui;

import com.glview.graphics.Bitmap;
import com.glview.libgdx.graphics.opengl.GL20;


/**
 * HardwareLayer
 * @author lijing.lj
 */
public final class Layer {
	public final static int LAYER_TYPE_NONE = 0;
	public final static int LAYER_TYPE_SOFTWARE = 1;
	public final static int LAYER_TYPE_HARDWARE = 2;
	
	Layer mNextUpdate;
	
	boolean mLayerValid = false;
	
	Texture mTexture;
	
	LayerRenderer mLayerRenderer;
	
	int mWidth, mHeight;
	
	public Layer(int width, int height) {
		mWidth = width;
		mHeight = height;
		mTexture = new Texture();
		mTexture.setWidth(width);
		mTexture.setHeight(height);
		mTexture.setFormat(GL20.GL_RGBA);
		mTexture.setType(GL20.GL_UNSIGNED_BYTE);
	}
	
	public int getWidth() {
		return mWidth;
	}
	
	public int getHeight() {
		return mHeight;
	}
	
	public boolean resize(int w, int h) {
		if (w > 0 && h > 0) {
			mWidth = w;
			mHeight = h;
			mTexture.setWidth(w);
			mTexture.setHeight(h);
			Caches.getInstance().textureCache.generateTexture(mTexture);
			invalidate();
			return true;
		}
		return false;
	}
	
	public boolean isValid() {
		return mLayerValid;
	}
	
	void ensureLayerRenderer(GLCanvas canvas) {
		if (mLayerRenderer == null) {
			mLayerRenderer = (canvas instanceof GL20Canvas) ? new LayerRenderer((GL20Canvas) canvas) : null;
		}
	}
	
	public void invalidate() {
		mLayerValid = false;
	}
	
	public void destroy() {
		invalidate();
		Caches.getInstance().deleteTexture(mTexture);
	}
	
	public void destroyHardwareResource() {
		mLayerValid = false;
		if (mTexture != null) {
			Caches.getInstance().deleteTexture(mTexture);
		}
	}
	
	boolean render(GLCanvas canvas, RenderNode renderNode) {
		if (!isValid()) {
			updateLayer(canvas, renderNode);
		}
		if (isValid() && canvas instanceof GL20Canvas) {
			((GL20Canvas) canvas).drawTexture(mTexture, 0, 0, mWidth, mHeight, null);
			return true;
		}
		return false;
	}
	
	void updateLayer(GLCanvas canvas, RenderNode renderNode) {
		ensureLayerRenderer(canvas);
		if (mLayerRenderer == null) return;
		canvas.save();
		((StatefullBaseCanvas) canvas).saveViewport();
		if (mTexture.mId <= 0) {
			Caches.getInstance().textureCache.generateTexture(mTexture);
		}
		Caches.getInstance().bindTexture(mTexture);
		mLayerRenderer.setSize(mWidth, mHeight);
		mLayerValid = mLayerRenderer.updateTextureLayer(this, mTexture, renderNode);
		canvas.restore();
	}
	
	Bitmap buildDrawingCache(GLCanvas canvas, RenderNode renderNode) {
		ensureLayerRenderer(canvas);
		if (mLayerRenderer == null) return null;
		canvas.save();
		((StatefullBaseCanvas) canvas).saveViewport();
		if (mTexture.mId <= 0) {
			Caches.getInstance().textureCache.generateTexture(mTexture);
		}
		Caches.getInstance().bindTexture(mTexture);
		mLayerRenderer.setSize(mWidth, mHeight);
		Bitmap bitmap = mLayerRenderer.buildDrawingCache(this, mTexture, renderNode);
		canvas.restore();
		return bitmap;
	}
}
