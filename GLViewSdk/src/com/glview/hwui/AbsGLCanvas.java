package com.glview.hwui;

import com.glview.graphics.Bitmap;
import com.glview.graphics.Rect;
import com.glview.graphics.RectF;
import com.glview.graphics.drawable.ninepatch.NinePatch;
import com.glview.graphics.mesh.BasicMesh;
import com.glview.graphics.shader.BaseShader;

/**
 * @author lijing.lj
 */
public abstract class AbsGLCanvas implements GLCanvas {
	
	@Override
	public void setSize(int width, int height) {
	}
	
	@Override
	public int getWidth() {
		return 0;
	}
	
	@Override
	public int getHeight() {
		return 0;
	}
	
	@Override
	public void getMatrix(float[] matrix) {
	}
	
	@Override
	public void setMatrix(float[] matrix) {
	}
	
	@Override
	public int save() {
		return save(SAVE_FLAG_ALPHA | SAVE_FLAG_MATRIX);
	}
	
	@Override
	public int save(int saveFlags) {
		return 0;
	}
	
	@Override
	public void restore() {
	}
	
	@Override
	public void restoreToCount(int saveCount) {
	}
	
	@Override
	public void translate(float x, float y) {
    }
    
	@Override
    public void translate(float x, float y, float z) {
    }

	@Override
    public void scale(float sx, float sy, float sz) {
    }
	
	@Override
	public void rotate(float degrees) {
		rotate(degrees, 0, 0, 1);
	}
    
	@Override
    public void rotate(float degrees, float x, float y, float z) {
    }
    
    @Override
    public void multiplyMatrix(float matrix[], int offset) {
    }
    
    @Override
    public void setAlpha(float alpha) {
    }
    
    @Override
    public void multiplyAlpha(float alpha) {
    }
	
    @Override
    public void drawLine(float x1, float y1, float x2, float y2, GLPaint paint) {
    }
    
    @Override
    public void drawRect(RectF rect, GLPaint paint) {
    	drawRect(rect.left, rect.top, rect.right, rect.bottom, paint);
    }
    
    @Override
    public void drawRect(Rect rect, GLPaint paint) {
    	drawRect(rect.left, rect.top, rect.right, rect.bottom, paint);
    }
    
    @Override
	public void drawRect(float left, float top, float right, float bottom, GLPaint paint) {
	}
    
    @Override
    public void drawOval(RectF oval, GLPaint paint) {
    	drawOval(oval.left, oval.top, oval.right, oval.bottom, paint);
    }
    
    @Override
    public void drawOval(float left, float top, float right, float bottom,
    		GLPaint paint) {
    }
	
	@Override
	public void drawBitmap(Bitmap bitmap, float x, float y, GLPaint paint) {
	}
	
	@Override
	public void drawBitmap(Bitmap bitmap, RectF source, RectF target,
			GLPaint paint) {
	}
	
	@Override
	public void drawBitmap(Bitmap bitmap, Rect source, Rect target,
			GLPaint paint) {
	}
	
	@Override
	public void drawBitmapBatch(Bitmap bitmap, Rect source, Rect target,
			GLPaint paint) {
	}
	
	@Override
	public void drawPatch(NinePatch patch, Rect rect, GLPaint paint) {
	}
	
	@Override
	public void drawMesh(BasicMesh mesh, GLPaint paint) {
	}
	
	@Override
	public void drawBitmapMesh(Bitmap bitmap, BasicMesh mesh, GLPaint paint) {
	}
	
	@Override
	public void drawText(CharSequence text, float x, float y, GLPaint paint) {
		drawText(text, 0, text.length(), x, y, paint, true);
	}
	
	@Override
	public void drawText(CharSequence text, int start, int end, float x,
			float y, GLPaint paint) {
		drawText(text, start, end, x, y, paint, true);
	}
	
	@Override
	public void drawText(CharSequence text, int start, int end, float x,
			float y, GLPaint paint, boolean drawDefer) {
	}
	
	@Override
	public void drawCircle(float cx, float cy, float radius, GLPaint paint) {
	}
	
	@Override
	public void drawRoundRect(RectF rect, float rx, float ry, GLPaint paint) {
	}
	
	@Override
	public void drawRoundRect(float left, float top, float right, float bottom,
			float rx, float ry, GLPaint paint) {
	}

	@Override
	public void drawRenderNode(RenderNode renderNode) {
	}
	
	@Override
	public void clipRect(Rect r) {
	}
	
	@Override
	public void clipRect(float left, float top, float right, float bottom) {
	}

	@Override
	public void beginFrame() {
	}

	@Override
	public void endFrame() {
	}
	
	@Override
	public void applyMatrix(BaseShader shader, float[] transform) {
	}
}
