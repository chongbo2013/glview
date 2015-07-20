package com.glview.hwui.cache;

import java.util.HashMap;
import java.util.Map;

import com.glview.graphics.shader.BaseShader;
import com.glview.libgdx.graphics.glutils.ShaderProgram;
import com.glview.view.GLRootView.CalledFromWrongThreadException;

public class ProgramCache {
	
	private Map<BaseShader, ShaderDescription> mCaches = new HashMap<BaseShader, ProgramCache.ShaderDescription>();
	
	Thread mTargetThread;
	
	public ProgramCache() {
		mTargetThread = Thread.currentThread();
	}
	
	public ShaderProgram get(BaseShader shader) {
		String vertexShader = shader.getVertexShader();
		String fragmentShader = shader.getFragmentShader();
		ShaderDescription program = mCaches.get(shader);
		if (program == null) {
			program = new ShaderDescription();
			program.baseShader = shader;
			program.program = new ShaderProgram(vertexShader, fragmentShader);
			mCaches.put(shader, program);
		}
		program.baseShader = shader;
		program.program.compile();
		return program.program;
	}
	
	public void clear() {
		if (Thread.currentThread() != mTargetThread) {
			throw new CalledFromWrongThreadException("Called from wrong thread.");
		}
		for (BaseShader shader : mCaches.keySet()) {
			ShaderDescription program = mCaches.get(shader);
			if (program != null) {
				program.program.dispose();
			}
		}
		mCaches.clear();
	}
	
	public void remove(BaseShader shader) {
		if (Thread.currentThread() != mTargetThread) {
			throw new CalledFromWrongThreadException("Called from wrong thread.");
		}
		ShaderDescription program = mCaches.remove(shader);
		if (program != null) {
			program.program.dispose();
		}
	}
	
	static class ShaderDescription {
		ShaderProgram program;
		BaseShader baseShader;
	}

}
