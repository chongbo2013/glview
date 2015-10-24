package com.glview.hwui;

import java.nio.IntBuffer;

import android.util.Log;

import com.glview.App;
import com.glview.hwui.cache.FboCache;
import com.glview.hwui.cache.MeshCache;
import com.glview.hwui.cache.PatchCache;
import com.glview.hwui.cache.ProgramCache;
import com.glview.hwui.cache.Stencil;
import com.glview.hwui.cache.TextureCache;
import com.glview.libgdx.graphics.glutils.ShaderProgram;
import com.glview.libgdx.graphics.opengl.GL20;
import com.glview.thread.Looper;
import com.glview.utils.BufferUtils;

/**
 * @hide
 * @author lijing.lj
 */
public final class Caches {
	
	static ThreadLocal<Caches> sThreadLocal = new ThreadLocal<Caches>() {
		@Override
		protected Caches initialValue() {
			return new Caches();
		}
	};
	
	// Must define as many texture units as specified by REQUIRED_TEXTURE_UNITS_COUNT
	static final int gTextureUnits[] = {
	    GL20.GL_TEXTURE0,
	    GL20.GL_TEXTURE1,
	    GL20.GL_TEXTURE2
	};
	
	final GL20 mGL;
	
	int maxTextureSize;
	
	private Caches() {
		mGL = App.getGL20();
		
		scissorEnabled = mGL.glIsEnabled(GL20.GL_SCISSOR_TEST);
		IntBuffer intbuf = BufferUtils.newIntBuffer(1);
		mGL.glGetIntegerv(GL20.GL_MAX_TEXTURE_SIZE, intbuf);
		maxTextureSize = intbuf.get(0);
	}
	
	public FboCache fboCache = new FboCache();
	public TextureCache textureCache = new TextureCache();
	public PatchCache patchCache = new PatchCache();
	public MeshCache meshCache = new MeshCache();
	public ProgramCache programCache = new ProgramCache();
	
	public Stencil stencil = new Stencil();
	public Extensions extensions = new Extensions();
	
	IntBuffer mBuffer = BufferUtils.newIntBuffer(1);
	
	public static Caches getInstance() {
		if (Looper.myLooper() == RenderThread.getRenderThreadLooper()) {
			CanvasContext.ensureEglManager();
			return sThreadLocal.get();
		} else {
			throw new IllegalStateException("Only call from RenderThread.");
		}
	}
	
	Texture mBindedTexture = null;
	
	public void bindTexture(Texture texture) {
		if (mBindedTexture != texture) {
			mBindedTexture = texture;
			mGL.glBindTexture(GL20.GL_TEXTURE_2D, texture.mId);
		}
	}
	
	public void unbindTexture(Texture texture) {
		if (mBindedTexture == texture) {
			mBindedTexture = null;
			mGL.glBindTexture(GL20.GL_TEXTURE_2D, 0);
		}
	}
	
	public void deleteTexture(Texture texture) {
		unbindTexture(texture);
		if (texture.mId <= 0) return;
		mBuffer.put(0, texture.mId);
		mBuffer.position(0);
		mGL.glDeleteTextures(1, mBuffer);
		texture.mId = 0;
	}

	ShaderProgram mCurrentProgram = null;
	public void useProgram(ShaderProgram program) {
		if (mCurrentProgram != program) {
			if (program != null) {
				mGL.glUseProgram(program.getProgram());
			} else {
				mGL.glUseProgram(0);
			}
			
			mCurrentProgram = program;
		} else if (mCurrentProgram != null && !mCurrentProgram.isCompiled()) {
			mGL.glUseProgram(mCurrentProgram.getProgram());
		}
	}
	
	public void clear() {
		meshCache.clear();
		textureCache.clear();
		patchCache.clear();
		programCache.clear();
	}
	
	///////////////////////////////////////////////////////////////////////////////
	//Scissor
	///////////////////////////////////////////////////////////////////////////////
	boolean scissorEnabled;
	int mScissorX;
	int mScissorY;
	int mScissorWidth;
	int mScissorHeight;
	
	boolean setScissor(int x, int y, int width, int height) {
	    if (scissorEnabled && (x != mScissorX || y != mScissorY ||
	            width != mScissorWidth || height != mScissorHeight)) {

	        if (x < 0) {
	            width += x;
	            x = 0;
	        }
	        if (y < 0) {
	            height += y;
	            y = 0;
	        }
	        if (width < 0) {
	            width = 0;
	        }
	        if (height < 0) {
	            height = 0;
	        }
	        mGL.glScissor(x, y, width, height);

	        mScissorX = x;
	        mScissorY = y;
	        mScissorWidth = width;
	        mScissorHeight = height;

	        return true;
	    }
	    return false;
	}
	
	boolean enableScissor() {
	    if (!scissorEnabled) {
	    	mGL.glEnable(GL20.GL_SCISSOR_TEST);
	        scissorEnabled = true;
	        resetScissor();
	        return true;
	    }
	    return false;
	}

	boolean disableScissor() {
	    if (scissorEnabled) {
	        mGL.glDisable(GL20.GL_SCISSOR_TEST);
	        scissorEnabled = false;
	        return true;
	    }
	    return false;
	}

	void setScissorEnabled(boolean enabled) {
	    if (scissorEnabled != enabled) {
	        if (enabled) mGL.glEnable(GL20.GL_SCISSOR_TEST);
	        else mGL.glDisable(GL20.GL_SCISSOR_TEST);
	        scissorEnabled = enabled;
	    }
	}
	
	void resetScissor() {
	    mScissorX = mScissorY = mScissorWidth = mScissorHeight = 0;
	}
}
