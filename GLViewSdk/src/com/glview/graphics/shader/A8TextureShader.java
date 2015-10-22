package com.glview.graphics.shader;

import com.glview.libgdx.graphics.glutils.ShaderProgram;
import com.glview.libgdx.graphics.glutils.ShaderProgram.HandleInfo;

public class A8TextureShader extends BaseShader {
	
	public HandleInfo mTexSizeHandle;
	
	public A8TextureShader() {
		mTexSizeHandle = new HandleInfo("u_texSize");
		setA8Format(true);
	}

	@Override
	protected String generateVertexShader() {
		StringBuffer vertexShader = new StringBuffer();
		vertexShader.append("attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n"); //
		vertexShader.append("uniform vec4 u_texSize;\n");//
		vertexShader.append("attribute vec4 " + ShaderProgram.COLOR_ATTRIBUTE + ";\n"); //
		vertexShader.append("uniform mat4 u_projTrans;\n"); //
		vertexShader.append("varying vec2 v_texCoords;\n"); //
		vertexShader.append("\n"); //
		vertexShader.append("void main()\n"); //
		vertexShader.append("{\n"); //
		vertexShader.append("   v_texCoords = vec2(("+ ShaderProgram.POSITION_ATTRIBUTE + ".x - u_texSize.x)/u_texSize.z, ("+ ShaderProgram.POSITION_ATTRIBUTE + ".y - u_texSize.y)/u_texSize.w);\n");
		vertexShader.append("   gl_Position =  u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n"); //
		vertexShader.append("}\n");;
		return vertexShader.toString();
	}
	
	@Override
	public void setupTextureCoords(float x, float y, float width, float height) {
		getShaderProgram().setUniformf(mTexSizeHandle, x, y, width, height);
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
		fragmentShader.append("varying vec2 v_texCoords;\n"); //
		fragmentShader.append("uniform sampler2D u_texture;\n"); //
		fragmentShader.append("void main()\n");//
		fragmentShader.append("{\n"); //
		fragmentShader.append("  gl_FragColor = u_ColorTotal*texture2D(u_texture, v_texCoords)").append(generateTextureA8()).append(";\n"); //
		fragmentShader.append("}");
		return fragmentShader.toString();
	}

}
