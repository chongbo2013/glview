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
public interface GLCanvas {
	
	public static final int SAVE_FLAG_ALL = 0xFFFFFFFF;
    public static final int SAVE_FLAG_ALPHA = 0x01;
    public static final int SAVE_FLAG_MATRIX = 0x02;
    public static final int SAVE_FLAG_CLIP = 0x04;
	
	public void setSize(int width, int height);
	
	int getWidth();
	
	int getHeight();
	
	void beginFrame();
	
	void endFrame();
	
	void getMatrix(float[] matrix);
	
	void setMatrix(float[] matrix);
	
	public int save();
	
	public int save(int saveFlags);
	
	public void restore();
	
	public void restoreToCount(int saveCount);
	
	public void translate(float x, float y);
    
    public void translate(float x, float y, float z);

    public void scale(float sx, float sy, float sz);
    
    public void rotate(float degrees);
    
    public void rotate(float degrees, float x, float y, float z);
    
    public void multiplyMatrix(float matrix[], int offset);
    
    public void setAlpha(float alpha);
    
    public void multiplyAlpha(float alpha);
	
    public void drawLine(float x1, float y1, float x2, float y2, GLPaint paint);
    
    public void drawRect(RectF rect, GLPaint paint);
    
    public void drawRect(Rect rect, GLPaint paint);
    
	public void drawRect(float left, float top, float right, float bottom, GLPaint paint);
	
	public void drawOval(RectF oval, GLPaint paint);
	
	public void drawOval(float left, float top, float right, float bottom, GLPaint paint);
	
	public void drawBitmap(Bitmap bitmap, float x, float y, GLPaint paint);
	
	public void drawBitmap(Bitmap bitmap, RectF source, RectF target, GLPaint paint);
	
	public void drawBitmap(Bitmap bitmap, Rect source, Rect target, GLPaint paint);
	
	public void drawBitmapBatch(Bitmap bitmap, Rect source, Rect target, GLPaint paint);
	
	public void drawPatch(NinePatch patch, Rect rect, GLPaint paint);
	
	public void drawMesh(BasicMesh mesh, GLPaint paint);
	
	public void drawBitmapMesh(Bitmap bitmap, BasicMesh mesh, GLPaint paint);

    public void drawText(CharSequence text, float x, float y, GLPaint paint);

    public void drawText(CharSequence text, int start, int end, float x, float y,
            GLPaint paint);
    
    public void drawText(CharSequence text, int start, int end, float x, float y,
            GLPaint paint, boolean drawDefer);
	
	public void drawCircle(float cx, float cy, float radius, GLPaint paint);
	
	public void drawRoundRect(RectF rect, float rx, float ry, GLPaint paint);
	
	public void drawRoundRect(float left, float top, float right, float bottom, float rx, float ry, GLPaint paint);
	
	public void drawRenderNode(RenderNode renderNode);
	
	public void clipRect(Rect r);
	
	public void clipRect(float left, float top, float right, float bottom);
	
	public void applyMatrix(BaseShader shader, float[] transform);
	
}
