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
        if (colors.length == 2 && positions == null) {
        	initColorStartAndColorEnd(x0, y0, x1, y1, colors[0], colors[1], tile);
        } else {
        	mType = TYPE_COLORS_AND_POSITIONS;
            mX0 = x0;
            mY0 = y0;
            mX1 = x1;
            mY1 = y1;
            mColors = colors;
            if (positions == null) {
            	float dis = 1f / (mColors.length - 1);
            	positions = new float[mColors.length];
            	for (int i = 0; i < mColors.length; i ++) {
            		positions[i] = dis * i;
            	}
            }
            mPositions = positions;
            mTileMode = tile;
            
            mStartPointHandle = new HandleInfo("u_startPoint");
    		mEndPointHandle = new HandleInfo("u_endPoint");
        }
    }
    
    public LinearGradient(float x0, float y0, float x1, float y1,
			int color0, int color1) {
    	this(x0, y0, x1, y1, color0, color1, TileMode.CLAMP);
    }
    
	public LinearGradient(float x0, float y0, float x1, float y1,
			int color0, int color1, TileMode tile) {
		initColorStartAndColorEnd(x0, y0, x1, y1, color0, color1, tile);
	}
	
	private void initColorStartAndColorEnd(float x0, float y0, float x1, float y1,
			int color0, int color1, TileMode tile) {
		mType = TYPE_COLOR_START_AND_COLOR_END;
		
		this.mX0 = x0;
		this.mY0 = y0;
		this.mX1 = x1;
		this.mY1 = y1;
		this.mColor0 = color0;
		this.mColor1 = color1;
		mTileMode = tile;
		
		mStartColorHandle = new HandleInfo("u_startColor");
		mEndColorHandle = new HandleInfo("u_endColor");
		mStartPointHandle = new HandleInfo("u_startPoint");
		mEndPointHandle = new HandleInfo("u_endPoint");
	}
	
	boolean isSimplemGradient() {
		return mType == TYPE_COLOR_START_AND_COLOR_END;
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
		if (isSimplemGradient()) {
			fragmentShader.append("uniform vec4 u_startColor;\n"); //
			fragmentShader.append("uniform vec4 u_endColor;\n"); //
		}
		fragmentShader.append("uniform vec4 u_ColorTotal;\n"); //
		if (mHasTexture) {
			fragmentShader.append("uniform sampler2D u_texture;\n"); //
		}
		fragmentShader.append("varying float linear;\n");//
		generateLinearWrap(fragmentShader);
		fragmentShader.append("void main()\n");//
		fragmentShader.append("{\n"); //
		generateLinearColor(fragmentShader);
		if (mHasTexture) {
			fragmentShader.append("  gl_FragColor = gradientColor*u_ColorTotal*texture2D(u_texture, v_texCoords);\n"); //
		} else {
			fragmentShader.append("  gl_FragColor = gradientColor * u_ColorTotal;\n"); //
		}
		fragmentShader.append("}");
		return fragmentShader.toString();
	}
	
	private void generateLinearColor(StringBuffer s) {
		s.append(" float al = clamp(wrap(linear), 0.0, 1.0);\n");
		s.append(" vec4 gradientColor;\n");
		if (isSimplemGradient()) {
			s.append("  gradientColor = mix(u_startColor, u_endColor, al);\n");
		} else {
			for (int i = 0; i < mColors.length - 1; i ++) {
				s.append(((i > 0) ? " else" : "") + " if (al >= " + mPositions[i] + " && al <= " + mPositions[i + 1] + ") {\n");
				float dis = mPositions[i + 1] - mPositions[i];
				float[] startColor = swapColor(mColors[i]);
				float[] endColor = swapColor(mColors[i + 1]);
				if (dis <= 0) {
					s.append(" gradientColor = vec4(" + startColor[0] + ", " + startColor[1] + ", " + startColor[2] + ", " + startColor[3] + ");\n");
				} else {
					s.append(" gradientColor = mix(vec4(" + startColor[0]
							+ ", " + startColor[1] + ", " + startColor[2]
							+ ", " + startColor[3] + "), vec4(" + endColor[0]
							+ ", " + endColor[1] + ", " + endColor[2] + ", "
							+ endColor[3] + "), clamp((al - " + mPositions[i] + ") / " + dis + ", 0.0, 1.0));\n");
				}
				s.append(" }\n");
			}
		}
	}
	
	private float[] swapColor(int color) {
		float prealpha = ((color >>> 24)&0xFF)*1.0f/255;
		float colorR = Math.round(((color >> 16) & 0xFF) * prealpha)*1.0f/255;
		float colorG = Math.round(((color >> 8) & 0xFF) * prealpha)*1.0f/255;
		float colorB = Math.round((color & 0xFF) * prealpha)*1.0f/255;
		float colorA = Math.round(255 * prealpha)*1.0f/255;
		return new float[] {colorR, colorG, colorB, colorA};
	}
	
	private void generateLinearWrap(StringBuffer s) {
		s.append("highp float wrap(highp float linear) {\n");
		if (mTileMode == TileMode.MIRROR) {
			s.append("    highp float mod2 = mod(linear, 2.0);\n");
			s.append("    if (mod2 > 1.0) mod2 = 2.0 - mod2;\n");
		}
		s.append("    return ");
		switch (mTileMode) {
		case CLAMP:
			s.append("linear");
			break;
		case REPEAT:
			s.append("mod(linear, 1.0)");
			break;
		case MIRROR:
			s.append("mod2");
			break;
		default:
			break;
		}
		s.append(";\n}\n");
	}
	
	@Override
	public void setupCustomValues() {
		if (isSimplemGradient()) {
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
			
		} else {
		}
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
