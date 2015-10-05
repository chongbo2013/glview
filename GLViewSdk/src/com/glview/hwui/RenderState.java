package com.glview.hwui;

import com.glview.libgdx.graphics.opengl.GL20;

class RenderState {
	
	public static final float OPAQUE_ALPHA = 0.95f;
	
	GL20 mGL;
	
	private int mTextureTarget = GL20.GL_TEXTURE_2D;
    private boolean mBlendEnabled = false;
    private boolean mDepthEnabled = false;
    private boolean mDepthMask = true;
    private float mLineWidth = 1.0f;
	
    GLPaint mDefaultPaint = new GLPaint();
    
    int mViewportWidth, mViewportHeight;
    
	public RenderState(GL20 gl) {
		mGL = gl;
		// 设置屏幕背景色RGBA
		mGL.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		mGL.glClearStencil(0);

		// Enable used features
		mGL.glEnable(GL20.GL_DITHER);

		// 关闭深度测试
		mGL.glDisable(GL20.GL_DEPTH_TEST);
		mGL.glDepthFunc(GL20.GL_LEQUAL);

		mGL.glEnable(GL20.GL_BLEND);
		mGL.glBlendFunc(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);

		// We use 565 or 8888 format, so set the alignment to 2 bytes/pixel.
		mGL.glPixelStorei(GL20.GL_UNPACK_ALIGNMENT, 2);
	}
	
	public void setViewport(int width, int height) {
	    mViewportWidth = width;
	    mViewportHeight = height;
	    mGL.glViewport(0, 0, mViewportWidth, mViewportHeight);
	}
	
	public GLPaint getDefaultPaint() {
		return mDefaultPaint;
	}

	int mFrameBuffer = 0;
	
	public int getFrameBuffer() {
		return mFrameBuffer;
	}
	
	public void bindFrameBuffer(int buffer) {
		if (mFrameBuffer != buffer) {
			mFrameBuffer = buffer;
			mGL.glBindFramebuffer(GL20.GL_FRAMEBUFFER, mFrameBuffer);
		}
	}
	
	public void setLineWidth(float width) {
        if (mLineWidth == width) return;
        mLineWidth = width;
        mGL.glLineWidth(width);
    }
	
	public void setColorMode(float alpha) {
        setBlendEnabled(alpha < OPAQUE_ALPHA);
    }
	
	// target is a value like GL_TEXTURE_2D. If target = 0, texturing is disabled.
    public void setTextureTarget(int target) {
        if (mTextureTarget == target) return;
        if (mTextureTarget != 0) {
        	mGL.glDisable(mTextureTarget);
        }
        mTextureTarget = target;
        if (mTextureTarget != 0) {
        	mGL.glEnable(mTextureTarget);
        }
    }
	
	public void setBlendEnabled(boolean enabled) { 	
        if (mBlendEnabled == enabled) {
        	return;
        }
        setDepthMask(!enabled);
        mBlendEnabled = enabled;
//        if (enabled) {
//        	mGL.glEnable(GL20.GL_BLEND);
//        } else {
//        	mGL.glDisable(GL20.GL_BLEND);
//        }
    }
	
	public void setDepthEnabled(boolean enabled) {
		if (mDepthEnabled == enabled) {
			return;
		}
		mDepthEnabled = enabled;
		if (enabled) {
        	mGL.glEnable(GL20.GL_DEPTH_TEST);
        } else {
        	mGL.glDisable(GL20.GL_DEPTH_TEST);
        }
	}
	
	public void setDepthMask(boolean mask) {
		if (mDepthMask == mask) {
			return;
		}
		mDepthMask = mask;
		mGL.glDepthMask(mask);
	}
}
