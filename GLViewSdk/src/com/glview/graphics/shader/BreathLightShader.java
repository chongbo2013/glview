package com.glview.graphics.shader;

import com.glview.libgdx.graphics.glutils.ShaderProgram;
import com.glview.libgdx.graphics.glutils.ShaderProgram.HandleInfo;

//加载顶点Shader与片元Shader的工具类
public class BreathLightShader extends SimpleTextureShader{
    HandleInfo mAlphaAniUniHandle;
    HandleInfo mDrawInnerrUniHandle;
    
	public BreathLightShader() {
		mAlphaAniUniHandle   = new HandleInfo("u_alphaAni");
		mDrawInnerrUniHandle = new HandleInfo("u_drawinnerr");
	}
	
	
	
	/*public void useShaderDrawMesh(GLCanvas canvas, UploadedTexture texture, int x, int y, int w, int h, Rect inner, float progressL, float progressT, float progressR, float progressB){
   		canvas.setShader(this);
   		mShaderProgram.setUniformf(mDrawInnerrUniHandle,  inner.left - x, inner.top - y, inner.right - x, inner.bottom - y);
		mShaderProgram.setUniformf(mAlphaAniUniHandle, progressL, progressT, progressR, progressB);
	}	
	
	public void useShaderDrawTexture(GLCanvas canvas, UploadedTexture texture, int x, int y, int w, int h, Rect inner, float progressL, float progressT, float progressR, float progressB){
   		canvas.setShader(this);
   		mShaderProgram.setUniformf(mDrawInnerrUniHandle,  inner.left - x, inner.top - y, inner.right - x, inner.bottom - y);
		mShaderProgram.setUniformf(mAlphaAniUniHandle, progressL, progressT, progressR, progressB);	
	}*/

	/*
	@Override
	public String createVertexShader() {
		String vertexShader = "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" //
				+ "attribute vec4 " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" //
				+ "attribute vec4 " + ShaderProgram.TEXCOORD_ATTRIBUTE + ";\n" //
				+ "uniform mat4 u_projTrans;\n" //
				+ "uniform vec4 u_ColorTotal;\n" //
				+ "uniform vec4 u_drawinnerr;\n"
				+ "varying vec4 v_color;\n" //
				+ "varying vec4 v_pos;\n" //
				+ "varying vec2 v_texCoords;\n" //
				+ "varying vec4 v_poslp;\n" //
				+ "varying vec4 v_posrb;\n" //
				+ "\n" //
				+ "void main()\n" //
				+ "{\n" //
				+ "   v_color = u_ColorTotal*" + ShaderProgram.COLOR_ATTRIBUTE + ";\n" //
				+ "   vec4 a_tex4V = vec4("+ ShaderProgram.TEXCOORD_ATTRIBUTE + ".x," + ShaderProgram.TEXCOORD_ATTRIBUTE + ".y, 0, 1); \n"
				+ "   vec4 a_tex4VT = a_tex4V;\n" //
				+ "   v_texCoords = vec2(a_tex4VT.x, a_tex4VT.y); \n"
				+ "   gl_Position =  u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" //
				+ "   v_poslp = u_projTrans*vec4(u_drawinnerr.x, u_drawinnerr.y, 0, 1.0);\n"
				+ "   v_posrb = u_projTrans*vec4(u_drawinnerr.z, u_drawinnerr.w, 0, 1.0);\n"
				+ "   v_pos = gl_Position;\n"
				+ "}\n";
		return vertexShader;
	}

	@Override
	public String createFragmentShader() {
		String fragmentShader = 
				"#define LOWP lowp\n" //
				+ "precision mediump float;\n" //
				+ "uniform vec4 u_alphaAni;\n" //
				+ "uniform mat4 u_projTrans;\n" //			
				+ "varying LOWP vec4 v_color;\n" //
				+ "varying vec4 v_pos;\n" //
				+ "varying vec2 v_texCoords;\n" //
				+ "varying vec4 v_poslp;\n" //
				+ "varying vec4 v_posrb;\n" //
				+ "uniform sampler2D u_texture;\n" //
				+ "float xCenter;\n" //
				+ "float yCenter;\n" //
				+ "float radi;\n" //
				+ "float alpha;\n" //
				+ "vec2  pt;\n" //
				+ "float alphaInterpolate(float startp, float endp, float stratAlpha, float endAlpha, float p)\n"
				+"{\n"
				+"      float lamta = 0.0;\n"
				+"      if(p < startp)\n"
				+"      {\n"
				+"           return stratAlpha;   \n"
				+"      }\n"			
				+"      if(p > endp)\n"
				+"      {\n"
				+"           return endAlpha;   \n"
				+"      }\n"
				+"      if(startp == endp)\n"
				+"      {\n"
				+"           return endAlpha;   \n"
				+"      }\n"			
				+"      lamta = (p - startp)/(endp - startp);\n"
				+"      return stratAlpha + (endAlpha - stratAlpha)*lamta;\n"
				+"}\n"
				+ "float alphaHVLine(float startp, float endp, float stratAlpha, float endAlpha, float p)\n"
				+"{\n"
				+"      if(p < (startp + endp)*0.5)\n"
				+"      {\n"
				+"           return alphaInterpolate(startp, (startp + endp)*0.5, stratAlpha, 1.0, p);   \n"
				+"      }\n"			
				+"      return alphaInterpolate((startp + endp)*0.5, endp, 1.0, endAlpha, p);  \n"
				+"}\n"
				+ "void main()\n"//
				+ "{\n" //
				+ "          vec4 rb = v_posrb;\n"
				+ "          vec4 lp = v_poslp;\n"
				+ "          alpha = 0.0;\n"
				+ "          pt.x = v_pos.x;\n"
				+ "          pt.y = v_pos.y;\n"
				+ "          if(pt.y > lp.y)\n"
				+ "			 {\n"	
				+ "             alpha = alphaHVLine(lp.x, rb.x, u_alphaAni.x, u_alphaAni.y, pt.x);\n"
				+ "			 }\n"	
				+ "          else if(pt.y < rb.y)\n"
				+ "			 {\n"
				+ "             alpha = alphaHVLine(lp.x, rb.x, u_alphaAni.z, u_alphaAni.w, pt.x);\n"
				+ "			 }\n"
				+ "          else if(pt.x < lp.x)\n"
				+ "			 {\n"	
				+ "             alpha = alphaHVLine(rb.y, lp.y, u_alphaAni.z, u_alphaAni.x, pt.y);\n"
				+ "			 }\n"	
				+ "          else if(pt.x > rb.x)\n"
				+ "			 {\n"
				+ "             alpha = alphaHVLine(rb.y, lp.y, u_alphaAni.w, u_alphaAni.y, pt.y);\n"
				+ "			 }\n"
				+ "          else\n"
				+ "			 {\n"
				+ "             alpha = 1.0;\n"
				+ "			 }\n"	
				+ "    		 gl_FragColor = v_color * texture2D(u_texture, v_texCoords.xy)*alpha;\n"
				+ "}";
		return fragmentShader;
	}
	*/
	
	
	@Override
	protected String generateVertexShader() {
		String vertexShader = "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" //
				+ "attribute vec4 " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" //
				+ "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + ";\n" //
				+ "uniform mat4 u_projTrans;\n" //
				+ "uniform vec4 u_ColorTotal;\n" //
				+ "uniform vec4 u_drawinnerr;\n"
				+ "varying vec4 v_color;\n" //
				+ "varying vec4 v_pos;\n" //
				+ "varying vec2 v_texCoords;\n" //
				+ "varying vec4 v_poslp;\n" //
				+ "varying vec4 v_posrb;\n" //
				+ "\n" //
				+ "void main()\n" //
				+ "{\n" //
				+ "   v_color = u_ColorTotal*" + ShaderProgram.COLOR_ATTRIBUTE + ";\n" //
				+ "   v_texCoords = "+ ShaderProgram.TEXCOORD_ATTRIBUTE + "; \n"
				+ "   gl_Position =  u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" //
				+ "   v_poslp = u_projTrans*vec4(u_drawinnerr.x, u_drawinnerr.y, 0, 1.0);\n"
				+ "   v_posrb = u_projTrans*vec4(u_drawinnerr.z, u_drawinnerr.w, 0, 1.0);\n"
				+ "   v_pos = gl_Position;\n"
				+ "}\n";
		return vertexShader;
	}

	@Override
	protected String generateFragmentShader() {
		String fragmentShader = 
				"#define LOWP lowp\n" //
				+ "precision mediump float;\n" //
				+ "uniform vec4 u_alphaAni;\n" //
				+ "uniform mat4 u_projTrans;\n" //			
				+ "varying LOWP vec4 v_color;\n" //
				+ "varying vec4 v_pos;\n" //
				+ "varying vec2 v_texCoords;\n" //
				+ "varying vec4 v_poslp;\n" //
				+ "varying vec4 v_posrb;\n" //
				+ "uniform sampler2D u_texture;\n" //
				+ "float xCenter;\n" //
				+ "float yCenter;\n" //
				+ "float radi;\n" //
				+ "float alpha;\n" //
				+ "vec2  pt;\n" //
				+ "float alphaInterpolate(float startp, float endp, float stratAlpha, float endAlpha, float p)\n"
				+"{\n"
				+"      float lamta = 0.0;\n"
				+"      if(p < startp)\n"
				+"      {\n"
				+"           return stratAlpha;   \n"
				+"      }\n"			
				+"      if(p > endp)\n"
				+"      {\n"
				+"           return endAlpha;   \n"
				+"      }\n"
				+"      if(startp == endp)\n"
				+"      {\n"
				+"           return endAlpha;   \n"
				+"      }\n"			
				+"      lamta = (p - startp)/(endp - startp);\n"
				+"      return stratAlpha + (endAlpha - stratAlpha)*lamta;\n"
				+"}\n"
				+ "float alphaHVLine(float startp, float endp, float stratAlpha, float endAlpha, float p)\n"
				+"{\n"
				+"      if(p < (startp + endp)*0.5)\n"
				+"      {\n"
				+"           return alphaInterpolate(startp, (startp + endp)*0.5, stratAlpha, 1.0, p);   \n"
				+"      }\n"			
				+"      return alphaInterpolate((startp + endp)*0.5, endp, 1.0, endAlpha, p);  \n"
				+"}\n"
				+ "void main()\n"//
				+ "{\n" //
				+ "          vec4 rb = v_posrb;\n"
				+ "          vec4 lp = v_poslp;\n"
				+ "          alpha = 0.0;\n"
				+ "          pt.x = v_pos.x;\n"
				+ "          pt.y = v_pos.y;\n"
				+ "          if(pt.y > lp.y)\n"
				+ "			 {\n"	
				+ "             alpha = alphaHVLine(lp.x, rb.x, u_alphaAni.x, u_alphaAni.y, pt.x);\n"
				+ "			 }\n"	
				+ "          else if(pt.y < rb.y)\n"
				+ "			 {\n"
				+ "             alpha = alphaHVLine(lp.x, rb.x, u_alphaAni.z, u_alphaAni.w, pt.x);\n"
				+ "			 }\n"
				+ "          else if(pt.x < lp.x)\n"
				+ "			 {\n"	
				+ "             alpha = alphaHVLine(rb.y, lp.y, u_alphaAni.z, u_alphaAni.x, pt.y);\n"
				+ "			 }\n"	
				+ "          else if(pt.x > rb.x)\n"
				+ "			 {\n"
				+ "             alpha = alphaHVLine(rb.y, lp.y, u_alphaAni.w, u_alphaAni.y, pt.y);\n"
				+ "			 }\n"
				+ "          else\n"
				+ "			 {\n"
				+ "             alpha = 1.0;\n"
				+ "			 }\n"	
				+ "    		 gl_FragColor = v_color * texture2D(u_texture, v_texCoords.xy)*alpha;\n"
				+ "}";
		return fragmentShader;
	}
	
}
