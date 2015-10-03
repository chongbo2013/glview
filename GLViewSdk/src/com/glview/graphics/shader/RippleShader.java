package com.glview.graphics.shader;

import com.glview.libgdx.graphics.glutils.ShaderProgram.HandleInfo;

public class RippleShader extends DefaultTextureShader {
	
	float mRatio;
	float[] mPosition = new float[2];
	
	float mProgress = 0f;
	
	HandleInfo mProgressHandleInfo;
	
	float mWaveLength = 0.4f;
	float mAmp = 0.2f;
	float mRatius = 1.0f;
	
	public RippleShader() {
		this(0, 0, false);
	}
	
	public RippleShader(float width, float height, boolean circle) {
		if (circle) {
			mRatio = width / height;
		} else {
			mRatio = 1f;
		}
		mPosition[0] = 0.5f;
		mPosition[1] = 0.5f;
		
		mProgressHandleInfo = new HandleInfo("u_progress");
	}
	
	public void setProgress(float progress) {
		mProgress = progress;
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
		fragmentShader.append("uniform vec4 u_ColorTotal;\n"); //
		fragmentShader.append("uniform float u_progress;\n"); //
		fragmentShader.append("const float c_wavelength = " + mWaveLength + ";\n"); //波浪的宽度
		fragmentShader.append("const float c_amps = c_wavelength / 6.283;\n"); //3.1415 * 2.0
		fragmentShader.append("const float c_amp = " + mAmp + ";\n"); //计算振幅的参数
		fragmentShader.append("const float c_radius = " + mRatius + ";\n"); //最大的波浪半径
		fragmentShader.append("\n"); //
		fragmentShader.append("vec2 generateWaterRipples(float x, float y, float radius, vec2 pos) {\n"); //
		fragmentShader.append((mRatio != 1f ? "  x *= " + mRatio +  ";\n" : "")); //
		fragmentShader.append((mRatio != 1f ? "  pos.x *= " + mRatio +  ";\n" : "")); //
		fragmentShader.append("  vec2 delta = vec2(x, y) - pos;\n"); //
		fragmentShader.append("  float dist = length(delta);\n"); //
		fragmentShader.append("  if (dist < radius) { \n"); //如果在半径以内，计算偏移
		fragmentShader.append("    float dist1 = radius - dist;\n"); //
		fragmentShader.append("    float amount = c_amp * (1.0 - u_progress);\n"); //振幅比例随着时间慢慢变小
		fragmentShader.append("    amount *= sin(dist1 / c_amps);\n"); //计算该点的振幅 3.1415 * 2.0
//		fragmentShader.append("    amount *= pow(dist1 / radius, 2.0);\n"); //计算能量损失，离中心点越远，振幅越小
		fragmentShader.append("    return delta * amount;\n"); //
		fragmentShader.append("  }\n"); //
		fragmentShader.append("  return vec2(0.0, 0.0);\n"); //
		fragmentShader.append("}\n"); //
		fragmentShader.append("\n"); //
		fragmentShader.append("void main()\n");//
		fragmentShader.append("{\n");//
		fragmentShader.append("  vec2 texCoords = v_texCoords;\n"); //
		fragmentShader.append("  texCoords += generateWaterRipples(" + mPosition[0] + ", " + mPosition[1] + ", u_progress * c_radius, texCoords);\n"); //
//		fragmentShader.append("  if (texCoords.x < 0.0 || v_texCoords.y < 0.0 || texCoords.x > 1.0 || texCoords.y > 1.0)\n"); //
//		fragmentShader.append("    gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);\n"); //
//		fragmentShader.append("  else\n"); //
		fragmentShader.append("  gl_FragColor = u_ColorTotal*texture2D(u_texture, texCoords);\n"); //
		fragmentShader.append("}");
		return fragmentShader.toString();
	}

	@Override
	public void setupCustomValues() {
		getShaderProgram().setUniformf(mProgressHandleInfo, mProgress);
	}

}
