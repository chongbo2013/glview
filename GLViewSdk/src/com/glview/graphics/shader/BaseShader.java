package com.glview.graphics.shader;

import java.lang.ref.WeakReference;

import com.glview.hwui.Caches;
import com.glview.libgdx.graphics.glutils.ShaderProgram;
import com.glview.libgdx.graphics.glutils.ShaderProgram.HandleInfo;

public abstract class BaseShader {
	final static String MATRIX_UNIFORM = "u_projTrans";
	final static String TEXTURE_CORD_MATRIX_UNIFORM = "u_tcProj";
	final static String COLOR_UNIFORM = "u_ColorTotal";
	final static String TEXTURE_UNIFORM = "u_texture";

	WeakReference<ShaderProgram> mShaderProgram = null;
	
	protected boolean mHasTexture = false;

	protected HandleInfo mPositionAttrHandle;// position
	protected HandleInfo mColorTotalUniHandle;// alpha
	protected HandleInfo mProjTransUniHandle;// view model
	protected HandleInfo mTextureUniHandle;// view model
	
	protected int key = -1;
	
	protected String mVertexShader, mFragmentShader;
	
	protected boolean mA8Format = false;

	public BaseShader() {
		mPositionAttrHandle = new HandleInfo(ShaderProgram.POSITION_ATTRIBUTE);
		mColorTotalUniHandle = new HandleInfo(COLOR_UNIFORM);
		mProjTransUniHandle = new HandleInfo(MATRIX_UNIFORM);
		mTextureUniHandle = new HandleInfo(TEXTURE_UNIFORM);
	}

	public final String getVertexShader() {
		if (mVertexShader == null) {
			mVertexShader = generateVertexShader();
		}
		return mVertexShader;
	}

	public final String getFragmentShader() {
		if (mFragmentShader == null) {
			mFragmentShader = generateFragmentShader();
		}
		return mFragmentShader;
	}
	
	protected abstract String generateVertexShader();
	
	protected abstract String generateFragmentShader();
	
	protected final String generateTextureA8() {
		if (isA8Format()) {
			return ".a";
		} else {
			return "";
		}
	}
	
	/**
	 * @hide
	 * @param hasTexture
	 */
	public void setHasTexture(boolean hasTexture) {
		mHasTexture = hasTexture;
	}
	
	public void setA8Format(boolean a8Format) {
		if (a8Format != mA8Format) {
			mA8Format = a8Format;
			invalidate();
		}
	}
	
	public boolean isA8Format() {
		return mA8Format;
	}
	
	public void invalidate() {
		if (mVertexShader != null && mFragmentShader != null) {
			Caches.getInstance().programCache.remove(this);
		}
		mShaderProgram = null;
		mVertexShader = null;
		mFragmentShader = null;
		key = -1;
	}
	
	public final int getKey() {
		if (key == -1) {
			key = getVertexShader().hashCode() * 31 + getFragmentShader().hashCode();
		}
		return key;
	}
	
	@Override
	public int hashCode() {
		return getKey();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof BaseShader)) {
			return false;
		}
		BaseShader other = (BaseShader) o;
		return getVertexShader().equals(other.getVertexShader()) && getFragmentShader().equals(other.getFragmentShader());
	}

	/**
	 * 
	 */
	public void setupCustomValues() {
	}

	/**
	 * Don't change the ShaderProgram outside of the RenderThread.
	 * @hide
	 * @return
	 */
	public ShaderProgram getShaderProgram() {
		if (mShaderProgram == null || mShaderProgram.get() == null || !mShaderProgram.get().isCompiled()) {
			mShaderProgram = new WeakReference<ShaderProgram>(Caches.getInstance().programCache.get(this));
		}
		return mShaderProgram.get();
	}

	public void setupViewModelMatrices(float[] matrix) {
		getShaderProgram().setUniformMatrix4fv(mProjTransUniHandle, matrix, 0,
				matrix.length);
	}

	public void setupColor(float r, float g, float b, float a) {
		getShaderProgram().setUniformf(mColorTotalUniHandle, r, g, b, a);
	}
	
	public void setupTextureCoords(float x, float y, float width, float height) {
	}

	public void setupTexture(int index) {
		getShaderProgram().setUniformi(mTextureUniHandle, index);
	}

}
