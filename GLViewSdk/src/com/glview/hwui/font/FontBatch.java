package com.glview.hwui.font;

import android.opengl.Matrix;

import com.glview.graphics.shader.BaseShader;
import com.glview.graphics.shader.DefaultTextureShader;
import com.glview.hwui.Caches;
import com.glview.hwui.GLPaint;
import com.glview.libgdx.graphics.Mesh;
import com.glview.libgdx.graphics.VertexAttribute;
import com.glview.libgdx.graphics.VertexAttributes.Usage;
import com.glview.libgdx.graphics.glutils.ShaderProgram;
import com.glview.libgdx.graphics.math.NumberUtils;
import com.glview.libgdx.graphics.opengl.GL20;
import com.glview.libgdx.graphics.utils.Disposable;
import com.glview.util.MatrixUtil;

class FontBatch implements Disposable {
	
	private Mesh mMesh;
	
	final float[] mVertices;
	int mIndex = 0;
	final CacheTexture mTexture;
	float mInvTexWidth, mInvTexHeight;
	
	final FontRenderer mFontRenderer;
	
	DefaultTextureShader mDefaultShader;
	BaseShader mCustomShader;
	
	Caches mCaches;
	
	float[] mIdentityMatrix = new float[16];
	float[] mTmpPoint = new float[2];
	float mPackedColor;
	float mCachedAlpha;
	int mCachedColor;
	
	public FontBatch(FontRenderer fontRenderer, CacheTexture texture) {
		this(fontRenderer, texture, 2048);
	}
	
	public FontBatch(FontRenderer fontRenderer, CacheTexture texture, int size) {
		// 32767 is max index, so 32767 / 6 - (32767 / 6 % 3) = 5460.
		if (size > 5460) throw new IllegalArgumentException("Can't have more than 5460 sprites per batch: " + size);
		mFontRenderer = fontRenderer;
		mTexture = texture;
		mInvTexWidth = 1.0f / texture.getWidth();
		mInvTexHeight = 1.0f / texture.getHeight();
		
		Matrix.setIdentityM(mIdentityMatrix, 0);
		
		mCaches = Caches.getInstance();
		
		mMesh = new Mesh(false, size * 4, size * 6, new VertexAttribute(
				Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE), // position
				 new VertexAttribute(Usage.ColorPacked, 4,
						 ShaderProgram.COLOR_ATTRIBUTE),
				new VertexAttribute(Usage.TextureCoordinates, 2,
						ShaderProgram.TEXCOORD_ATTRIBUTE));// texture position
		
		mVertices = new float[size * 20];
		
		int len = size * 6;
		short[] indices = new short[len];
		short j = 0;
		for (int i = 0; i < len; i += 6, j += 4) {
			indices[i] = j;
			indices[i + 1] = (short)(j + 1);
			indices[i + 2] = (short)(j + 2);
			indices[i + 3] = (short)(j + 2);
			indices[i + 4] = (short)(j + 3);
			indices[i + 5] = j;
		}
		mMesh.setIndices(indices);
		mDefaultShader = new DefaultTextureShader();
		mDefaultShader.setHasTexcoordsAttr(true);
		mDefaultShader.setHasColorAttr(true);
		mDefaultShader.setHasTotalColor(false);
		mDefaultShader.setA8Format(true);
	}
	
	@Override
	public void dispose() {
		mMesh.dispose();
	}
	
	public boolean full() {
		return this.mIndex == this.mVertices.length;
	}
	
	public void draw(float x, float y, float width, float height, float srcX, float srcY, float srcWidth, float srcHeight, float[] transformation, float alpha, int color, GLPaint paint) {
		float[] vertices = this.mVertices;

		if (this.mIndex == vertices.length) //
			flush();

		final float u = srcX * mInvTexWidth;
		final float v = srcY * mInvTexHeight;
		final float u2 = (srcX + srcWidth) * mInvTexWidth;
		final float v2 = (srcY + srcHeight) * mInvTexHeight;
		
		final float fx2 = x + width;
		final float fy2 = y + height;
		
		float[] pt = MatrixUtil.mapPoint(transformation, mTmpPoint, x, y);
		float x1 = pt[0], y1 = pt[1];
		pt = MatrixUtil.mapPoint(transformation, mTmpPoint, x, fy2);
		float x2 = pt[0], y2 = pt[1];
		pt = MatrixUtil.mapPoint(transformation, mTmpPoint, fx2, fy2);
		float x3 = pt[0], y3 = pt[1];
		pt = MatrixUtil.mapPoint(transformation, mTmpPoint, fx2, y);
		float x4 = pt[0], y4 = pt[1];

		if (alpha != mCachedAlpha || color != mCachedColor) {
			mCachedAlpha = alpha;
			mCachedColor = color;
			mPackedColor = NumberUtils.intToFloatColor(packColor(alpha, color));
		}
		float c = mPackedColor;
		int idx = this.mIndex;
		vertices[idx++] = x1;
		vertices[idx++] = y1;
		vertices[idx++] = c;
		vertices[idx++] = u;
		vertices[idx++] = v;

		vertices[idx++] = x2;
		vertices[idx++] = y2;
		vertices[idx++] = c;
		vertices[idx++] = u;
		vertices[idx++] = v2;

		vertices[idx++] = x3;
		vertices[idx++] = y3;
		vertices[idx++] = c;
		vertices[idx++] = u2;
		vertices[idx++] = v2;

		vertices[idx++] = x4;
		vertices[idx++] = y4;
		vertices[idx++] = c;
		vertices[idx++] = u2;
		vertices[idx++] = v;
		this.mIndex = idx;
	}
	
	public void draw(float x, float y, float width, float height, float srcX, float srcY, float srcWidth, float srcHeight, float alpha, int color, GLPaint paint) {
		float[] vertices = this.mVertices;

		if (this.mIndex == vertices.length) //
			flush();

		final float u = srcX * mInvTexWidth;
		final float v = srcY * mInvTexHeight;
		final float u2 = (srcX + srcWidth) * mInvTexWidth;
		final float v2 = (srcY + srcHeight) * mInvTexHeight;
		
		final float fx2 = x + width;
		final float fy2 = y + height;
		
		float c = NumberUtils.intToFloatColor(packColor(alpha, color));
		int idx = this.mIndex;
		vertices[idx++] = x;
		vertices[idx++] = y;
		vertices[idx++] = c;
		vertices[idx++] = u;
		vertices[idx++] = v;

		vertices[idx++] = x;
		vertices[idx++] = fy2;
		vertices[idx++] = c;
		vertices[idx++] = u;
		vertices[idx++] = v2;

		vertices[idx++] = fx2;
		vertices[idx++] = fy2;
		vertices[idx++] = c;
		vertices[idx++] = u2;
		vertices[idx++] = v2;

		vertices[idx++] = fx2;
		vertices[idx++] = y;
		vertices[idx++] = c;
		vertices[idx++] = u2;
		vertices[idx++] = v;
		this.mIndex = idx;
	}
	
	public void flush() {
		if (mIndex == 0) return;
		mTexture.upload();
		int count = mIndex / 20 * 6;

		mCaches.bindTexture(mTexture.mTexture);
		Mesh mesh = this.mMesh;
		mesh.setVertices(mVertices, 0, mIndex);
		mesh.getIndicesBuffer().position(0);
		mesh.getIndicesBuffer().limit(count);
		
		ShaderProgram program = mDefaultShader.getShaderProgram();
		mCaches.useProgram(program);
		mDefaultShader.setupColor(1, 1, 1, 1);
		if (mFontRenderer.getGLCanvas() != null) {
			mFontRenderer.getGLCanvas().applyMatrix(mDefaultShader, mIdentityMatrix);
		}
		mDefaultShader.setupCustomValues();
		
		mesh.render(program, GL20.GL_TRIANGLES, 0, count);
		mIndex = 0;
	}
	
	int packColor(float alpha, int color) {
		float prealpha = ((color >>> 24) & 0xFF) * alpha / 255;
		float colorR = Math.round(((color >> 16) & 0xFF)) * prealpha / 255;
		float colorG = Math.round(((color >> 8) & 0xFF)) * prealpha / 255;
		float colorB = Math.round((color & 0xFF)) * prealpha / 255;
		float colorA = Math.round(255 * prealpha) * 1.0f / 255;
		int intBits = (int)(255 * colorA) << 24 | (int)(255 * colorB) << 16 | (int)(255 * colorG) << 8 | (int)(255 * colorR);
		return intBits;
	}

}
