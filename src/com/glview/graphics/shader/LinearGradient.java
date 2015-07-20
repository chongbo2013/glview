package com.glview.graphics.shader;

import com.glview.libgdx.graphics.glutils.ShaderProgram;
import com.glview.libgdx.graphics.glutils.ShaderProgram.HandleInfo;

public class LinearGradient extends DefaultTextureShader {
	
	private static final int TYPE_COLORS_AND_POSITIONS = 1;
    private static final int TYPE_COLOR_START_AND_COLOR_END = 2;

    /**
     * Type of the LinearGradient: can be either TYPE_COLORS_AND_POSITIONS or
     * TYPE_COLOR_START_AND_COLOR_END.
     */
    private int mType;
	
	private float mX0;
    private float mY0;
    private float mX1;
    private float mY1;
    private int[] mColors;
    private float[] mPositions;
    private int mColor0;
    private int mColor1;
    
    private TileMode mTileMode;
    
    HandleInfo mStartColorHandle;
    HandleInfo mEndColorHandle;
    
    HandleInfo mStartPointHandle;
    HandleInfo mEndPointHandle;
    
    public LinearGradient(float x0, float y0, float x1, float y1, int colors[], float positions[],
            TileMode tile) {
        if (colors.length < 2) {
            throw new IllegalArgumentException("needs >= 2 number of colors");
        }
        if (positions != null && colors.length != positions.length) {
            throw new IllegalArgumentException("color and position arrays must be of equal length");
        }
        mType = TYPE_COLORS_AND_POSITIONS;
        mX0 = x0;
        mY0 = y0;
        mX1 = x1;
        mY1 = y1;
        mColors = colors;
        mPositions = positions;
        mTileMode = tile;
    }
    
    public LinearGradient(float x0, float y0, float x1, float y1,
			int color0, int color1) {
    	this(x0, y0, x1, y1, color0, color1, TileMode.CLAMP);
    }
    
	public LinearGradient(float x0, float y0, float x1, float y1,
			int color0, int color1, TileMode tile) {
		mType = TYPE_COLOR_START_AND_COLOR_END;
		
		this.mX0 = x0;
		this.mY0 = y0;
		this.mX1 = x1;
		this.mY1 = y1;
		this.mColor0 = color0;
		this.mColor1 = color1;
		
		mStartColorHandle = new HandleInfo("u_startColor");
		mEndColorHandle = new HandleInfo("u_endColor");
		mStartPointHandle = new HandleInfo("u_startPoint");
		mEndPointHandle = new HandleInfo("u_endPoint");
	}
	
	public void setPosition(float x0, float y0, float x1, float y1) {
		this.mX0 = x0;
		this.mY0 = y0;
		this.mX1 = x1;
		this.mY1 = y1;
	}
	
	public void setColors(int color0, int color1) {
		this.mColor0 = color0;
		this.mColor1 = color1;
	}
	
	@Override
	public void setHasTexture(boolean hasTexture) {
		if (mHasTexture != hasTexture) {
			super.setHasTexture(hasTexture);
			// recreate this shader.
			invalidate();
		}
	}

	protected String generateVertexShader() {
		StringBuffer vertexShader = new StringBuffer();
		vertexShader.append("attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n"); //
		vertexShader.append("uniform mat4 u_projTrans;\n"); //
		vertexShader.append("uniform vec2 u_startPoint;\n");//
		vertexShader.append("uniform vec2 u_endPoint;\n"); //
		if (mHasTexture) {
			vertexShader.append("varying vec2 v_texCoords;\n"); //
		}
		vertexShader.append("varying float linear;\n"); //
		vertexShader.append("\n"); //
		vertexShader.append("void main()\n"); //
		vertexShader.append("{\n"); //
		if (mHasTexture) {
			vertexShader.append("   v_texCoords = vec2(("+ ShaderProgram.POSITION_ATTRIBUTE + ".x - u_texSize.x)/u_texSize.z, ("+ ShaderProgram.POSITION_ATTRIBUTE + ".y - u_texSize.y)/u_texSize.w);\n");
		}
		vertexShader.append("   vec2 a = u_endPoint - u_startPoint;\n");
		vertexShader.append("   vec2 b = vec2(" + ShaderProgram.POSITION_ATTRIBUTE + ".x, " + ShaderProgram.POSITION_ATTRIBUTE + ".y) - u_startPoint;\n");
		vertexShader.append("   float length1 = length(b * a / length(a));\n");
		vertexShader.append("   linear = length1 / length(a);\n");
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
		fragmentShader.append("uniform vec4 u_startColor;\n"); //
		fragmentShader.append("uniform vec4 u_endColor;\n"); //
		fragmentShader.append("uniform vec4 u_ColorTotal;\n"); //
		if (mHasTexture) {
			fragmentShader.append("uniform sampler2D u_texture;\n"); //
		}
		fragmentShader.append("varying float linear;\n");//
		fragmentShader.append("void main()\n");//
		fragmentShader.append("{\n"); //
		fragmentShader.append("  vec4 gradientColor = mix(u_startColor, u_endColor, clamp(linear, 0.0, 1.0));\n"); //
		if (mHasTexture) {
			fragmentShader.append("  gl_FragColor = texture2D(u_texture, v_texCoords)*gradientColor*u_ColorTotal;\n"); //
		} else {
			fragmentShader.append("  gl_FragColor = gradientColor * u_ColorTotal;\n"); //
		}
		fragmentShader.append("}");
		return fragmentShader.toString();
	}
	
	@Override
	public void setupCustomValues() {
		float prealpha = ((mColor0 >>> 24)&0xFF)*1.0f/255;
        float colorR = Math.round(((mColor0 >> 16) & 0xFF) * prealpha)*1.0f/255;
		float colorG = Math.round(((mColor0 >> 8) & 0xFF) * prealpha)*1.0f/255;
		float colorB = Math.round((mColor0 & 0xFF) * prealpha)*1.0f/255;
		float colorA = Math.round(255 * prealpha)*1.0f/255;
		getShaderProgram().setUniformf(mStartColorHandle, colorR, colorG, colorB, colorA);
		
		prealpha = ((mColor1 >>> 24)&0xFF)*1.0f/255;
        colorR = Math.round(((mColor1 >> 16) & 0xFF) * prealpha)*1.0f/255;
		colorG = Math.round(((mColor1 >> 8) & 0xFF) * prealpha)*1.0f/255;
		colorB = Math.round((mColor1 & 0xFF) * prealpha)*1.0f/255;
		colorA = Math.round(255 * prealpha)*1.0f/255;
		getShaderProgram().setUniformf(mEndColorHandle, colorR, colorG, colorB, colorA);
		
		getShaderProgram().setUniformf(mStartPointHandle, mX0, mY0);
		getShaderProgram().setUniformf(mEndPointHandle, mX1, mY1);
	}
	
	@Override
	public void setupTextureCoords(float x, float y, float width, float height) {
		if (mHasTexture) {
			super.setupTextureCoords(x, y, width, height);
		}
	}
	
	@Override
	public void setupColor(float r, float g, float b, float a) {
		super.setupColor(r, g, b, a);
	}

}
