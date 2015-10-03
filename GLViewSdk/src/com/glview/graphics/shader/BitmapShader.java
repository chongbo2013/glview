package com.glview.graphics.shader;

import com.glview.graphics.Bitmap;
import com.glview.libgdx.graphics.glutils.ShaderProgram;
import com.glview.libgdx.graphics.glutils.ShaderProgram.HandleInfo;

public class BitmapShader extends DefaultTextureShader {
	
	Bitmap mBitmap;
	TileMode mTileModeX;
	TileMode mTileModeY;
	
	HandleInfo mTexSize1 = new HandleInfo("u_texSize1");
	
	public BitmapShader(Bitmap bitmap, TileMode tileModeX, TileMode tileModeY) {
		mBitmap = bitmap;
		mTileModeX = tileModeX;
		mTileModeY = tileModeY;
	}

	@Override
	protected String generateVertexShader() {
		StringBuffer vertexShader = new StringBuffer();
		vertexShader.append("attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n"); //
		vertexShader.append("uniform mat4 u_projTrans;\n"); //
		vertexShader.append("uniform vec4 u_texSize;\n");//
		vertexShader.append("uniform vec2 u_texSize1;");
		vertexShader.append("varying vec2 v_texCoords;\n"); //
		vertexShader.append("\n"); //
		vertexShader.append("void main()\n"); //
		vertexShader.append("{\n"); //
		vertexShader.append("   v_texCoords = vec2(("+ ShaderProgram.POSITION_ATTRIBUTE + ".x - u_texSize.x)/u_texSize1.x, ("+ ShaderProgram.POSITION_ATTRIBUTE + ".y - u_texSize.y)/u_texSize1.y);\n");
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
		fragmentShader.append("uniform vec4 u_ColorTotal; \n");//
		generateTextureWrap(fragmentShader);
		fragmentShader.append("void main()\n");//
		fragmentShader.append("{\n"); //
		fragmentShader.append("  vec2 texCoords = wrap(v_texCoords);\n");
		fragmentShader.append("  gl_FragColor = u_ColorTotal*texture2D(u_texture, texCoords);\n"); //
		fragmentShader.append("}");
		return fragmentShader.toString();
	}
	
	private void generateTextureWrap(StringBuffer s) {
		s.append("highp vec2 wrap(highp vec2 texCoords) {\n");
		if (mTileModeX == TileMode.MIRROR) {
			s.append("    highp float xMod2 = mod(texCoords.x, 2.0);\n");
			s.append("    if (xMod2 > 1.0) xMod2 = 2.0 - xMod2;\n");
		}
		if (mTileModeY == TileMode.MIRROR) {
			s.append("    highp float yMod2 = mod(texCoords.y, 2.0);\n");
			s.append("    if (yMod2 > 1.0) yMod2 = 2.0 - yMod2;\n");
		}
		s.append("    return vec2(");
		switch (mTileModeX) {
		case CLAMP:
			s.append("texCoords.x");
			break;
		case REPEAT:
			s.append("mod(texCoords.x, 1.0)");
			break;
		case MIRROR:
			s.append("xMod2");
			break;
		default:
			break;
		}
		s.append(",");
		switch (mTileModeY) {
		case CLAMP:
			s.append("texCoords.y");
			break;
		case REPEAT:
			s.append("mod(texCoords.y, 1.0)");
			break;
		case MIRROR:
			s.append("yMod2");
			break;
		default:
			break;
		}
		s.append(");\n}\n");
	}
	
	@Override
	public void setupCustomValues() {
		getShaderProgram().setUniformf(mTexSize1, mBitmap.getWidth(), mBitmap.getHeight());
	}

}
