package com.glview.hwui.cache;

import com.glview.App;
import com.glview.libgdx.graphics.opengl.GL20;

public class Stencil {
	
	private final static boolean DEBUG_STENCIL = false;
	
	private final static int STENCIL_WRITE_VALUE = DEBUG_STENCIL ? 0xff : 0x1;
	private final static int STENCIL_MASK_VALUE = DEBUG_STENCIL ? 0xff : 0x1;
	
	private final static int STENCIL_BUFFER_SIZE = 8;
	
	enum StencilState {
		kDisabled,
        kTest,
        kWrite
	}
	
	final GL20 mGL;
	
	StencilState mState = StencilState.kDisabled;
	
	public Stencil() {
		mGL = App.getGL20();
	}
	
	public int getStencilSize() {
	    return STENCIL_BUFFER_SIZE;
	}
	
	public void clear() {
		mGL.glClearStencil(0);
		mGL.glClear(GL20.GL_STENCIL_BUFFER_BIT);
	}
	
	public void enableTest() {
	    if (mState != StencilState.kTest) {
	        enable();
	        mGL.glStencilFunc(GL20.GL_EQUAL, STENCIL_WRITE_VALUE, STENCIL_MASK_VALUE);
	        // We only want to test, let's keep everything
	        mGL.glStencilOp(GL20.GL_KEEP, GL20.GL_KEEP, GL20.GL_KEEP);
	        mGL.glColorMask(true, true, true, true);
	        mState = StencilState.kTest;
	    }
	}
	
	public void enableWrite() {
	    if (mState != StencilState.kWrite) {
	        enable();
	        mGL.glStencilFunc(GL20.GL_ALWAYS, STENCIL_WRITE_VALUE, STENCIL_MASK_VALUE);
	        // The test always passes so the first two values are meaningless
	        mGL.glStencilOp(GL20.GL_KEEP, GL20.GL_KEEP, GL20.GL_REPLACE);
	        mGL.glColorMask(false, false, false, false);
	        mState = StencilState.kWrite;
	    }
	}

	public void enableDebugTest(int value, boolean greater) {
	    enable();
	    mGL.glStencilFunc(greater ? GL20.GL_LESS : GL20.GL_EQUAL, value, 0xffffffff);
	    // We only want to test, let's keep everything
	    mGL.glStencilOp(GL20.GL_KEEP, GL20.GL_KEEP, GL20.GL_KEEP);
	    mState = StencilState.kTest;
	}

	public void enableDebugWrite() {
	    if (mState != StencilState.kWrite) {
	        enable();
	        mGL.glStencilFunc(GL20.GL_ALWAYS, 0x1, 0xffffffff);
	        // The test always passes so the first two values are meaningless
	        mGL.glStencilOp(GL20.GL_KEEP, GL20.GL_KEEP, GL20.GL_INCR);
	        mGL.glColorMask(true, true, true, true);
	        mState = StencilState.kWrite;
	    }
	}

	public void enable() {
	    if (mState == StencilState.kDisabled) {
	    	mGL.glEnable(GL20.GL_STENCIL_TEST);
	    }
	}

	public void disable() {
	    if (mState != StencilState.kDisabled) {
	    	mGL.glDisable(GL20.GL_STENCIL_TEST);
	        mState = StencilState.kDisabled;
	    }
	}

}
