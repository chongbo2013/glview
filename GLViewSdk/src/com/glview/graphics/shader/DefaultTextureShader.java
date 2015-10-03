package com.glview.graphics.shader;

import com.glview.libgdx.graphics.glutils.ShaderProgram;
import com.glview.libgdx.graphics.glutils.ShaderProgram.HandleInfo;

public class DefaultTextureShader extends BaseShader {
	
	public HandleInfo mTexSizeHandle;
	
	boolean mHasTexcoordsAttr = false;
	boolean mHasColorAttr = false;
	boolean mHasTotalColor = true;
	
	public DefaultTextureShader() {
		mTexSizeHandle = new HandleInfo("u_texSize");
	}
	
	public void setHasTexcoordsAttr(boolean hasTexcoordsAttr) {
		if (mHasTexcoordsAttr != hasTexcoordsAttr) {
			mHasTexcoordsAttr = hasTexcoordsAttr;
			invalidate();
		}
	}
	
	public void setHasColorAttr(boolean hasColorAttr) {
		if (mHasColorAttr != hasColorAttr) {
			mHasColorAttr = hasColorAttr;
			invalidate();
		}
	}
	
	public void setHasTotalColor(boolean hasTotalColor) {
		if (mHasTotalColor != hasTotalColor) {
			mHasTotalColor = hasTotalColor;
			invalidate();
		}
	}
	
	@Override
	protected String generateVertexShader() {
		StringBuffer vertexShader = new StringBuffer();
		vertexShader.append("attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n"); //
		if (mHasTexcoordsAttr) {
			vertexShader.append("attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + ";\n"); //
		} else {
			vertexShader.append("uniform vec4 u_texSize;\n");//
		}
		if (mHasColorAttr) {
			vertexShader.append("attribute vec4 " + ShaderProgram.COLOR_ATTRIBUTE + ";\n"); //
			vertexShader.append("varying vec4 v_color;\n");//
		}
		vertexShader.append("uniform mat4 u_projTrans;\n"); //
		vertexShader.append("varying vec2 v_texCoords;\n"); //
		vertexShader.append("\n"); //
		vertexShader.append("void main()\n"); //
		vertexShader.append("{\n"); //
		if (mHasColorAttr) {
			vertexShader.append("   v_color = " + ShaderProgram.COLOR_ATTRIBUTE + ";\n"); //
		}
		if (mHasTexcoordsAttr) {
			vertexShader.append("   v_texCoords = "+ ShaderProgram.TEXCOORD_ATTRIBUTE + "; \n");
		} else {
			vertexShader.append("   v_texCoords = vec2(("+ ShaderProgram.POSITION_ATTRIBUTE + ".x - u_texSize.x)/u_texSize.z, ("+ ShaderProgram.POSITION_ATTRIBUTE + ".y - u_texSize.y)/u_texSize.w);\n");
		}
		vertexShader.append("   gl_Position =  u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n"); //
		vertexShader.append("}\n");;
		return vertexShader.toString();
	}

	@Override
	protected String generateFragmentShader() {
		StringBuffer fragmentShader = new StringBuffer();
		fragmentShader.append("#ifdef GL_ES\n"); //
		fragmentShader.append("#define LOWP lowp\n"); //
		fragmentShader.append("precision mediump float;\n"); //
		fragmentShader.append("#else\n"); //
		fragmentShader.append("#define LOWP \n"); //
		fragmentShader.append("#endif\n"); //
		if (mHasColorAttr) {
			fragmentShader.append("varying lowp vec4 v_color;\n"); //
		}
		fragmentShader.append("varying vec2 v_texCoords;\n"); //
		fragmentShader.append("uniform sampler2D u_texture;\n"); //
		if (mHasTotalColor) {
			fragmentShader.append("uniform vec4 u_ColorTotal; \n");//
		}
		fragmentShader.append("void main()\n");//
		fragmentShader.append("{\n"); //
		if (mHasTotalColor) {
			if (mHasColorAttr) {
				fragmentShader.append("  gl_FragColor = u_ColorTotal*v_color*texture2D(u_texture, v_texCoords)").append(generateTextureA8()).append(";\n"); //
			} else {
				fragmentShader.append("  gl_FragColor = u_ColorTotal*texture2D(u_texture, v_texCoords)").append(generateTextureA8()).append(";\n"); //
			}
		} else {
			if (mHasColorAttr) {
				fragmentShader.append("  gl_FragColor = v_color*texture2D(u_texture, v_texCoords)").append(generateTextureA8()).append(";\n"); //
			} else {
				fragmentShader.append("  gl_FragColor = texture2D(u_texture, v_texCoords)").append(generateTextureA8()).append(";\n"); //
			}
		}
		fragmentShader.append("}");
		return fragmentShader.toString();
	}
	
	@Override
	public void setupColor(float r, float g, float b, float a) {
		if (mHasTotalColor) {
			super.setupColor(r, g, b, a);
		}
	}
	
	@Override
	public void setupTextureCoords(float x, float y, float width, float height) {
		if (!mHasTexcoordsAttr) {
			getShaderProgram().setUniformf(mTexSizeHandle, x, y, width, height);
		}
	}

}
