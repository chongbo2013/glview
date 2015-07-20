package com.glview.graphics.shader;

import com.glview.libgdx.graphics.glutils.ShaderProgram;
import com.glview.libgdx.graphics.glutils.ShaderProgram.HandleInfo;

public class SimpleTextureShader extends BaseShader{
    public HandleInfo mTexCoordAttrHandle;
    public HandleInfo mColorAttrHandle;
    public HandleInfo mTotalAlphaUniHandle;
    
    
	public SimpleTextureShader() {
		mTexCoordAttrHandle = new HandleInfo(ShaderProgram.TEXCOORD_ATTRIBUTE);
		mColorAttrHandle    = new HandleInfo(ShaderProgram.COLOR_ATTRIBUTE);
		//uniform vec4 u_ColorTotal; // render color r
		
	}

	@Override
	protected String generateVertexShader() {
		StringBuffer vertexShader = new StringBuffer();
		vertexShader.append("attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n"); //
		vertexShader.append("attribute vec4 " + ShaderProgram.COLOR_ATTRIBUTE + ";\n"); //
		vertexShader.append("attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + ";\n"); //
		vertexShader.append("uniform mat4 u_projTrans;\n"); //
		vertexShader.append("uniform vec4 v_color;\n");//
		vertexShader.append("varying vec2 v_texCoords;\n"); //
		vertexShader.append("\n"); //
		vertexShader.append("void main()\n"); //
		vertexShader.append("{\n"); //
		vertexShader.append("   v_color = " + ShaderProgram.COLOR_ATTRIBUTE + ";\n"); //
		vertexShader.append("   v_texCoords = "+ ShaderProgram.TEXCOORD_ATTRIBUTE + "; \n");
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
		fragmentShader.append("varying lowp vec4 v_color;\n"); //
		fragmentShader.append("varying vec2 v_texCoords;\n"); //
		fragmentShader.append("uniform sampler2D u_texture;\n"); //
		fragmentShader.append("void main()\n");//
		fragmentShader.append("{\n"); //
		fragmentShader.append("  gl_FragColor = texture2D(u_texture, v_texCoords)*v_color;\n"); //
		fragmentShader.append("}");
		return fragmentShader.toString();
	}
}
