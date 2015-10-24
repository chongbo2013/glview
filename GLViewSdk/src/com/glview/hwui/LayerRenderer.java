package com.glview.hwui;

import java.nio.IntBuffer;

import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.opengl.Matrix;

import com.glview.graphics.Bitmap;
import com.glview.libgdx.graphics.opengl.GL20;
import com.glview.utils.BufferUtils;


public class LayerRenderer extends GL20Canvas {
	
	private static int MAX_LAYER_SIZE = (1024 * 1024);
	
	static ThreadLocal<IntBuffer> sThreadLocal = new ThreadLocal<IntBuffer>() {
		@Override
		protected IntBuffer initialValue() {
			return BufferUtils.newIntBuffer(1);
		}
	};
	
	public LayerRenderer(GL20Canvas canvas) {
		super(canvas.mRenderState);
	}
	
	@Override
	public void setSize(int width, int height) {
		super.setSize(width, height);
		final float matrix[] = currentSnapshot().transform;
		Matrix.setIdentityM(matrix, 0);
	}
	
	public boolean updateTextureLayer(Layer layer, Texture texture, RenderNode renderNode) {
		final int size = texture.getWidth() * texture.getHeight();
		if (size > MAX_LAYER_SIZE) return false;
		final GL20 gl = mGL;
		final IntBuffer handle = sThreadLocal.get();
		handle.position(0);
		gl.glGenFramebuffers(1, handle);
		int framebufferHandle = handle.get(0);
		gl.glBindFramebuffer(GL20.GL_FRAMEBUFFER, framebufferHandle);
		
		gl.glFramebufferTexture2D(GL20.GL_FRAMEBUFFER, GL20.GL_COLOR_ATTACHMENT0, GL20.GL_TEXTURE_2D,
				texture.mId, 0);
		
		int result = gl.glCheckFramebufferStatus(GL20.GL_FRAMEBUFFER);
		
		if (result != GL20.GL_FRAMEBUFFER_COMPLETE) {
			deleteFrameBuffer(framebufferHandle);
			return false;
		}
		int preFrameBuffer = mRenderState.getFrameBuffer();
		mRenderState.bindFrameBuffer(framebufferHandle);
		beginFrame();
		renderNode.renderWithoutLayer(this);
		endFrame();
		mRenderState.bindFrameBuffer(preFrameBuffer);
		
		deleteFrameBuffer(framebufferHandle);
		return true;
	}
	
	public Bitmap buildDrawingCache(Layer layer, Texture texture, RenderNode renderNode) {
		final int size = texture.getWidth() * texture.getHeight();
		if (size > MAX_LAYER_SIZE) return null;
		final GL20 gl = mGL;
		final IntBuffer handle = sThreadLocal.get();
		handle.position(0);
		gl.glGenFramebuffers(1, handle);
		int framebufferHandle = handle.get(0);
		gl.glBindFramebuffer(GL20.GL_FRAMEBUFFER, framebufferHandle);
		
		gl.glFramebufferTexture2D(GL20.GL_FRAMEBUFFER, GL20.GL_COLOR_ATTACHMENT0, GL20.GL_TEXTURE_2D,
				texture.mId, 0);
		
		int result = gl.glCheckFramebufferStatus(GL20.GL_FRAMEBUFFER);
		
		if (result != GL20.GL_FRAMEBUFFER_COMPLETE) {
			deleteFrameBuffer(framebufferHandle);
			return null;
		}
		int preFrameBuffer = mRenderState.getFrameBuffer();
		mRenderState.bindFrameBuffer(framebufferHandle);
		beginFrame();
		renderNode.renderWithoutLayer(this);
		endFrame();
		Bitmap bitmap = null;
		
		int[] pixels = new int[size];
		gl.glReadPixels(0, 0, texture.getWidth(), texture.getHeight(), GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, IntBuffer.wrap(pixels));
		adaptBitmapFormat(pixels);
		bitmap = Bitmap.createBitmap(pixels, texture.getWidth(), texture.getHeight(), Config.ARGB_8888);
		mRenderState.bindFrameBuffer(preFrameBuffer);
		
		deleteFrameBuffer(framebufferHandle);
		return bitmap;
	}
	
	/**
	 * FIXME 耗时操作啊
	 * @param corlorArr
	 */
	private void adaptBitmapFormat(int corlorArr[]){
    	int vColor;
		int count = corlorArr.length;
		int r;
		int g;
		int b;
		int a;
		for(int i = 0; i < count; i++){
			vColor = corlorArr[i];
			
			a = Color.alpha(vColor);
			b = Color.red(vColor);
		 	g = Color.green(vColor);
		 	r = Color.blue(vColor);
		 	vColor = Color.argb(a, r, g, b);
		 	corlorArr[i] = vColor;
		}
		
    }
	
	private void deleteFrameBuffer(int framebufferHandle) {
		final IntBuffer handle = sThreadLocal.get();
		handle.put(0, framebufferHandle);
		handle.position(0);
		mGL.glDeleteFramebuffers(1, handle);
	}

}
