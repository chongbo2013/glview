package com.glview.graphics.shader;

import com.glview.libgdx.graphics.glutils.ShaderProgram;
import com.glview.libgdx.graphics.glutils.ShaderProgram.HandleInfo;

public class BlurShader extends BaseShader {
	
	float  mRadius;
	public HandleInfo mRadiusHandle;
	public HandleInfo mTexSizeHandle;

	public BlurShader() {
		mRadiusHandle = new HandleInfo("u_radius");
		mTexSizeHandle = new HandleInfo("u_texSize");
		
	}
	
	public void setRadius(float radius) {
		mRadius = (int) radius;
	}

	@Override
	protected String generateVertexShader() {
		StringBuffer vertexShader = new StringBuffer();
		vertexShader.append("attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n"); //
		vertexShader.append("uniform vec4 u_texSize;\n");//
		vertexShader.append("uniform mat4 u_projTrans;\n"); //
		vertexShader.append("varying vec2 v_texCoords;\n"); //
		vertexShader.append("varying vec2 v_texSize;\n"); //
		vertexShader.append("\n"); //
		vertexShader.append("void main()\n"); //
		vertexShader.append("{\n"); //
		vertexShader.append("   v_texCoords = vec2(("+ ShaderProgram.POSITION_ATTRIBUTE + ".x - u_texSize.x)/u_texSize.z, ("+ ShaderProgram.POSITION_ATTRIBUTE + ".y - u_texSize.y)/u_texSize.w);\n");
		vertexShader.append("   v_texSize = vec2(u_texSize.z, u_texSize.w);\n");
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
		fragmentShader.append("varying vec2 v_texCoords;\n"); //
		fragmentShader.append("uniform sampler2D u_texture;\n"); //
		fragmentShader.append("uniform vec4 u_ColorTotal; \n");//
		fragmentShader.append("uniform float u_radius;\n");
		fragmentShader.append("varying vec2 v_texSize;\n");//
		fragmentShader.append("float gaussian(float x)\n");//
		fragmentShader.append("{\n");//
		fragmentShader.append("  return 0.01 * pow(2.71828183, -x*x/(1.0) );  \n");//
		fragmentShader.append("}\n");//
		fragmentShader.append("void main()\n");//
		fragmentShader.append("{\n"); //
		fragmentShader.append("  vec4 sum = vec4(0.0);\n");
		fragmentShader.append("  float i, j, gaussTotal=0.0, temp;\n");
		fragmentShader.append("  for(i = -u_radius; i <= u_radius; i+=1.0){\n");
		fragmentShader.append("    for(j = -u_radius; j <= u_radius; j+=1.0){\n");
//		fragmentShader.append("      temp = gaussian(sqrt( i*i + j*j ));\n");
		fragmentShader.append("      temp = 0.25/u_radius/u_radius;\n");
		fragmentShader.append("      gaussTotal += temp;\n");
		fragmentShader.append("      sum += temp * texture2D(u_texture, v_texCoords + vec2(i,j)/v_texSize);\n");
		fragmentShader.append("    }\n");
		fragmentShader.append("  }\n");
		fragmentShader.append("  gl_FragColor = u_ColorTotal*(sum/gaussTotal);\n"); //
		fragmentShader.append("}");
		return fragmentShader.toString();
	}
	
	@Override
	public void setupCustomValues() {
		getShaderProgram().setUniformf(mRadiusHandle, mRadius);
	}
	
	@Override
	public void setupTextureCoords(float x, float y, float width, float height) {
		getShaderProgram().setUniformf(mTexSizeHandle, x, y, width, height);
	}

}
