package com.glview.graphics.shader;

import com.glview.libgdx.graphics.glutils.ShaderProgram;

public class DefaultColorShader extends BaseShader{
	
	boolean mHasColorAttr = false;
	
	public DefaultColorShader() {
	}
	
	public void setHasColorAttr(boolean hasColorAttr) {
		if (mHasColorAttr != hasColorAttr) {
			mHasColorAttr = hasColorAttr;
			invalidate();
		}
	}
	
	@Override
	protected String generateVertexShader() {
		StringBuffer vertexShader = new StringBuffer();
		vertexShader.append("attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n"); //
		if (mHasColorAttr) {
			vertexShader.append("attribute vec4 " + ShaderProgram.COLOR_ATTRIBUTE + ";\n"); //
			vertexShader.append("varying vec4 v_color;\n");//
		}
		vertexShader.append("uniform mat4 u_projTrans;\n"); //
		vertexShader.append("\n"); //
		vertexShader.append("void main()\n"); //
		vertexShader.append("{\n"); //
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
		fragmentShader.append("uniform vec4 u_ColorTotal; \n");//
		fragmentShader.append("void main()\n");//
		fragmentShader.append("{\n"); //
		if (mHasColorAttr) {
			fragmentShader.append("  gl_FragColor = u_ColorTotal*v_color;\n"); //
		} else {
			fragmentShader.append("  gl_FragColor = u_ColorTotal;\n"); //
		}
		fragmentShader.append("}");
		return fragmentShader.toString();
	}
	
}
