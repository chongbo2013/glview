package com.glview.hwui;

import android.opengl.Matrix;

import com.glview.App;
import com.glview.graphics.Bitmap;
import com.glview.graphics.Rect;
import com.glview.graphics.RectF;
import com.glview.graphics.drawable.ninepatch.NinePatch;
import com.glview.graphics.mesh.BasicMesh;
import com.glview.graphics.shader.BaseShader;
import com.glview.graphics.shader.DefaultColorShader;
import com.glview.graphics.shader.DefaultTextureShader;
import com.glview.hwui.GLPaint.Style;
import com.glview.hwui.font.FontRenderer;
import com.glview.libgdx.graphics.Mesh;
import com.glview.libgdx.graphics.VertexAttribute;
import com.glview.libgdx.graphics.VertexAttributes.Usage;
import com.glview.libgdx.graphics.glutils.ShaderProgram;
import com.glview.libgdx.graphics.opengl.GL20;
import com.glview.util.MatrixUtil;
import com.glview.util.Utils;

/**
 * This canvas use OpenGL ES 2.0
 * 
 * @author lijing.lj
 */
class GL20Canvas extends StatefullBaseCanvas implements InnerGLCanvas {

	private static final String TAG = "GL20Canvas";

	private static final int OFFSET_FILL_RECT = 0;
	private static final int OFFSET_DRAW_LINE = 6;
	private static final int OFFSET_DRAW_RECT = 8;

	private static final int COUNT_DRAW_TXETURE = 6;
	private static final int COUNT_DRAW_LINE = 2;
	private static final int COUNT_DRAW_RECT = 4;
	private static final int COUNT_TOTAL = COUNT_DRAW_TXETURE + COUNT_DRAW_LINE
			+ COUNT_DRAW_RECT;

	private final RectF mDrawTextureSourceRect = new RectF();
	private final RectF mDrawTextureTargetRect = new RectF();

	final float mFinalMatrix[] = new float[16];

	boolean tmpRet;

	Mesh mMesh;
	float mVertices[];
	Batch mBatch;
	FontRenderer mFontRenderer;

	ShaderManager mShaderManager;

	GL20 mGL;
	RenderState mRenderState;

	Caches mCaches;

	public GL20Canvas(RenderState renderState) {
		mCaches = Caches.getInstance();
		mGL = App.getGL20();
		mRenderState = renderState;
		
		initialize();

		mShaderManager = new ShaderManager();
		mBatch = new Batch(this);
		mFontRenderer = FontRenderer.instance();
		initialMesh();
	}

	private void initialMesh() {
		mMesh = new Mesh(false, 4, COUNT_TOTAL, new VertexAttribute(
				Usage.Position, 2, ShaderProgram.POSITION_ATTRIBUTE), // position
				// new VertexAttribute(Usage.ColorPacked, 4,
				// ShaderProgram.COLOR_ATTRIBUTE),
				new VertexAttribute(Usage.TextureCoordinates, 2,
						ShaderProgram.TEXCOORD_ATTRIBUTE));// texture position
		initialVertices();
		mMesh.setVertices(mVertices);
		setIndex();
	}

	private void setIndex() {
		short indices[] = new short[COUNT_TOTAL];
		indices[0] = (short) (0);
		indices[1] = (short) (1);
		indices[2] = (short) (2);
		indices[3] = (short) (1);
		indices[4] = (short) (3);
		indices[5] = (short) (2);

		indices[6] = (short) (0);
		indices[7] = (short) (3);

		indices[8] = (short) (0);
		indices[9] = (short) (1);
		indices[10] = (short) (3);
		indices[11] = (short) (2);

		mMesh.setIndices(indices);
	}

	private void setVerticesXY(float left, float top, float right, float bottom) {
		// 1
		mVertices[0] = left;
		mVertices[1] = top;
		// 2
		mVertices[4] = left;
		mVertices[5] = bottom;
		// 3
		mVertices[8] = right;
		mVertices[9] = top;
		// 4
		mVertices[12] = right;
		mVertices[13] = bottom;
	}

	private void setVerticesUV(float left, float top, float right, float bottom) {
		// 1
		mVertices[2] = left;
		mVertices[3] = top;
		// 2
		mVertices[6] = left;
		mVertices[7] = bottom;
		// 3
		mVertices[10] = right;
		mVertices[11] = top;
		// 4
		mVertices[14] = right;
		mVertices[15] = bottom;
	}

	private void initialVertices() {
		mVertices = new float[4 * 4];
		setVerticesXY(0, 0, 1, 1);
		setVerticesUV(0, 0, 1, 1);
	}

	public void setSize(int width, int height) {
		super.setSize(width, height);

		Utils.assertTrue(width >= 0 && height >= 0);

		// //////////////////////////
		// gl10--->gl20 start
		// 设置视窗大小及位置
		mRenderState.setViewport(width, height);

		setCameraAndProject(0, 0);

		final float matrix[] = currentSnapshot().transform;
		Matrix.setIdentityM(matrix, 0);

		Matrix.translateM(matrix, 0, 0, height, 0);
		Matrix.scaleM(matrix, 0, 1, -1, 1);

		// 裁剪平面
		//App.getGL20().glEnable(GL20.GL_CULL_FACE);
		//App.getGL20().glCullFace(GL20.GL_BACK);
	}
	
	private void setCameraAndProject(float centerX, float centerY) {
		float near = 1;
		float camera = 500;
		float far = 5000;
		
		// 调用此方法产生摄像机9参数位置矩阵
		setCamera(centerX, centerY, camera, centerX, centerY, 0f, 0f, 1.0f, 0.0f);
		setProjectFrustum(- centerX / camera, mWidth / camera - centerX / camera, - centerY / camera, mHeight / camera - centerY / camera, near, far);
	}
	
	@Override
	public void translate(float x, float y) {
		flushBatch();
		super.translate(x, y);
	}
	
	@Override
	public void translate(float x, float y, float z) {
		flushBatch();
		if (z != 0) {
			float[] center = MatrixUtil.mapPoint(currentSnapshot().transform, 0, 0);
			setCameraAndProject(center[0], center[1]);
			mRenderState.setDepthEnabled(true);
		}
		super.translate(x, y, z);
	}
	
	@Override
	public void rotate(float degrees, float x, float y, float z) {
		if (degrees == 0) return;
		flushBatch();
		if (x != 0 || y != 0) {
			float[] center = MatrixUtil.mapPoint(currentSnapshot().transform, 0, 0);
			setCameraAndProject(center[0], center[1]);
			mRenderState.setDepthEnabled(true);
		}
		super.rotate(degrees, x, y, z);
	}
	
	@Override
	public void rotate(float degrees) {
		flushBatch();
		super.rotate(degrees);
	}
	
	@Override
	public void scale(float sx, float sy, float sz) {
		flushBatch();
		super.scale(sx, sy, sz);
	}
	
	@Override
	public void restore() {
		flushBatch();
		super.restore();
	}
	
	@Override
	public void restoreToCount(int saveCount) {
		flushBatch();
		super.restoreToCount(saveCount);
	}

	private void initialize() {
	}

	@Override
	public void beginFrame() {
		setupDraw();
		mGL.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		mGL.glClearDepthf(1f);
		mGL.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		mFontRenderer.setGLCanvas(this);
		
		mRenderState.setDepthEnabled(false);
		mRenderState.setBlendEnabled(false);
		dirtyClip();
		Caches.getInstance().disableScissor();
//		mRenderState.setBlendEnabled(true);
	}

	@Override
	public void endFrame() {
		flushBatch();
		flushFont();
		mFontRenderer.end(this);
		mFontRenderer.setGLCanvas(null);
	}

	private void setupDraw() {
		if (mDirtyClip) {
			if (mCaches.scissorEnabled) {
	            setScissorFromClip();
	        }
		}
	}
	
	@Override
	public void drawRect(float left, float top, float right, float bottom,
			GLPaint paint) {
		paint = getGLPaint(paint);
		if (paint.getStyle() == Style.FILL) {
			fillRect(left, top, right, bottom, paint);
			return;
		}
		int color = paint.getColor();
		mRenderState.setLineWidth(paint.getStrokeWidth());
		setVerticesXY(left, top, right, bottom);
		drawColor(color, GL20.GL_LINE_LOOP, OFFSET_DRAW_RECT, COUNT_DRAW_RECT,
				paint);
	}

	private GLPaint getGLPaint(GLPaint paint) {
		if (paint != null)
			return paint;
		return mRenderState.getDefaultPaint();
	}

	public void drawLine(float x1, float y1, float x2, float y2, GLPaint paint) {
		paint = getGLPaint(paint);
		int color = paint.getColor();
		mRenderState.setLineWidth(paint.getStrokeWidth());
		setVerticesXY(x1, y1, x2, y2);
		drawColor(color, GL20.GL_LINE_STRIP, OFFSET_DRAW_LINE, COUNT_DRAW_LINE,
				paint);
	}

	private void fillRect(float left, float top, float right, float bottom,
			GLPaint paint) {
		int color = paint.getColor();
		setVerticesXY(left, top, right, bottom);
		drawColor(color, GL20.GL_TRIANGLES, OFFSET_FILL_RECT,
				COUNT_DRAW_TXETURE, paint);
	}

	private void drawColor(int color, int mode, int offset, int count,
			GLPaint paint) {
		
		setupDraw();
		
		flushBatch();
		
		// If a shader is set, preserve only the alpha
		if (paint.getShader() != null) {
	        color |= 0x00ffffff;
	    }
		
		float alpha = currentSnapshot().alpha * paint.getAlpha() / 255;
		float prealpha = ((color >>> 24) & 0xFF) * alpha / 255;
		float colorR = Math.round(((color >> 16) & 0xFF) * prealpha) * 1.0f / 255;
		float colorG = Math.round(((color >> 8) & 0xFF) * prealpha) * 1.0f / 255;
		float colorB = Math.round((color & 0xFF) * prealpha) * 1.0f / 255;
		float colorA = Math.round(255 * prealpha) * 1.0f / 255;
		
		BaseShader useShader = mShaderManager.setupColorShader(colorR, colorG, colorB, colorA, paint, false);
		mMesh.setVertices(mVertices);
		mMesh.render(useShader.getShaderProgram(), mode, offset, count, true);
		mRenderState.setDepthMask(true);
	}

	private void textureRect(float x, float y, float width, float height, boolean hasAlpha,
			GLPaint paint) {
		
		setupDraw();
		
		flushBatch();
		
		setVerticesXY(x, y, x + width, y + height);
		mMesh.setVertices(mVertices);

		BaseShader useShader = mShaderManager.setupTextureShader(paint, x, y,
				width, height, hasAlpha, true, false);
		mMesh.render(useShader.getShaderProgram(), GL20.GL_TRIANGLES,
				OFFSET_FILL_RECT, COUNT_DRAW_TXETURE, true);
		mRenderState.setDepthMask(true);
	}
	
	@Override
	public void drawTexture(Texture texture, float x, float y, float width, float height, GLPaint paint) {
		drawTexture(texture, x, y, width, height, true, paint);
	}
	
	public void drawTexture(Texture texture, float x, float y, float width, float height, boolean hasAlpha, GLPaint paint) {
		if (texture != null && texture.mId > 0) {
			mCaches.bindTexture(texture);
			setVerticesUV(0, 0, 1, 1);
			textureRect(x, y, width, height, hasAlpha, getGLPaint(paint));
		}
	}

	@Override
	public void drawBitmap(Bitmap bitmap, float x, float y, GLPaint paint) {
		Texture texture = mCaches.textureCache.get(bitmap);
		if (texture == null)
			return;
		drawTexture(texture, x, y, bitmap.getWidth(), bitmap.getHeight(), bitmap.hasAlpha(), paint);
	}

	@Override
	public void drawBitmap(Bitmap bitmap, RectF source, RectF target,
			GLPaint paint) {
		paint = getGLPaint(paint);
		Texture texture = mCaches.textureCache.get(bitmap);
		if (texture == null)
			return;

		// Copy the input to avoid changing it.
		if (source != null && !source.isEmpty()) {
			mDrawTextureSourceRect.set(source);
		} else {
			mDrawTextureSourceRect.set(0, 0, bitmap.getWidth(),
					bitmap.getHeight());
		}
		mDrawTextureTargetRect.set(target);

		mCaches.bindTexture(texture);

		convertCoordinate(mDrawTextureSourceRect, texture);
		setVerticesUV(mDrawTextureSourceRect.left, mDrawTextureSourceRect.top,
				mDrawTextureSourceRect.right, mDrawTextureSourceRect.bottom);

		textureRect(mDrawTextureTargetRect.left, mDrawTextureTargetRect.top,
				mDrawTextureTargetRect.width(),
				mDrawTextureTargetRect.height(), bitmap.hasAlpha(), paint);
	}

	@Override
	public void drawBitmap(Bitmap bitmap, Rect source, Rect target,
			GLPaint paint) {
		paint = getGLPaint(paint);
		Texture texture = mCaches.textureCache.get(bitmap);
		if (texture == null)
			return;

		// Copy the input to avoid changing it.
		if (source != null && !source.isEmpty()) {
			mDrawTextureSourceRect.set(source);
		} else {
			mDrawTextureSourceRect.set(0, 0, bitmap.getWidth(),
					bitmap.getHeight());
		}
		mDrawTextureTargetRect.set(target);

		mCaches.bindTexture(texture);

		convertCoordinate(mDrawTextureSourceRect, texture);
		setVerticesUV(mDrawTextureSourceRect.left, mDrawTextureSourceRect.top,
				mDrawTextureSourceRect.right, mDrawTextureSourceRect.bottom);

		textureRect(mDrawTextureTargetRect.left, mDrawTextureTargetRect.top,
				mDrawTextureTargetRect.width(),
				mDrawTextureTargetRect.height(), bitmap.hasAlpha(), paint);
	}
	
	@Override
	public void drawBitmapBatch(Bitmap bitmap, Rect source, Rect target,
			GLPaint paint) {
		paint = getGLPaint(paint);
		if (source == null || source.isEmpty()) {
			mBatch.drawBitmap(bitmap, target.left, target.top, target.width(), target.height(), 0, 0, bitmap.getWidth(), bitmap.getHeight(), currentSnapshot().alpha, paint);
		} else {
			mBatch.drawBitmap(bitmap, target.left, target.top, target.width(), target.height(), source.left, source.top, source.width(), source.height(), currentSnapshot().alpha, paint);
		}
	}
	
	@Override
	public void applyMatrix(BaseShader shader, float[] transform) {
		// 将最终变换矩阵传入shader程序
		float[] m = getFinalMatrix(mFinalMatrix, transform == null ? currentSnapshot().transform : transform);
		shader.setupViewModelMatrices(m);
	}

	// This function changes the source coordinate to the texture coordinates.
	// It also clips the source and target coordinates if it is beyond the
	// bound of the texture.
	private static void convertCoordinate(RectF source, Texture texture) {

		int width = texture.getWidth();
		int height = texture.getHeight();

		float invTexWidth = 1.0f / width;
		float invTexHeight = 1.0f / height;
		// Convert to texture coordinates
		source.left *= invTexWidth;
		source.right *= invTexWidth;
		source.top *= invTexHeight;
		source.bottom *= invTexHeight;
	}

	@Override
	public void drawPatch(NinePatch patch, Rect rect, GLPaint paint) {
		paint = getGLPaint(paint);
		Texture texture = mCaches.textureCache.get(patch.getBitmap());
		if (texture == null)
			return;
		Mesh mesh = mCaches.patchCache.get(rect.width(), rect.height(), patch);
		if (mesh == null)
			return;

		setupDraw();
		
		flushBatch();
		
		mCaches.bindTexture(texture);

		translate(rect.left, rect.top);
		
		BaseShader useShader = mShaderManager.setupTextureShader(paint, 0, 0,
				texture.getWidth(), texture.getHeight(), patch.getBitmap().hasAlpha(), true, false);

		mesh.render(useShader.getShaderProgram(), GL20.GL_TRIANGLE_STRIP, 0,
				mesh.getNumIndices(), true);
		mRenderState.setDepthMask(true);
		// 恢复现状
		translate(-rect.left, -rect.top);
	}

	@Override
	public void drawMesh(BasicMesh basicMesh, GLPaint paint) {
		paint = getGLPaint(paint);
		Mesh mesh = mCaches.meshCache.get(basicMesh);
		if (mesh == null)
			return;
		
		setupDraw();
		
		flushBatch();
		
		mRenderState.setLineWidth(paint.getStrokeWidth());
		
		int color = paint.getColor();
		float alpha = currentSnapshot().alpha * paint.getAlpha() / 255;
		float prealpha = ((color >>> 24) & 0xFF) * alpha / 255;
		float colorR = Math.round(((color >> 16) & 0xFF) * prealpha) * 1.0f / 255;
		float colorG = Math.round(((color >> 8) & 0xFF) * prealpha) * 1.0f / 255;
		float colorB = Math.round((color & 0xFF) * prealpha) * 1.0f / 255;
		float colorA = Math.round(255 * prealpha) * 1.0f / 255;
		
		BaseShader useShader = mShaderManager.setupColorShader(colorR, colorG, colorB, colorA, paint, basicMesh.hasColorAttr());
		if (paint.getStyle() == Style.FILL) {
			mesh.render(useShader.getShaderProgram(), basicMesh.getDrawMode());
		} else {
			mesh.render(useShader.getShaderProgram(), GL20.GL_LINE_LOOP);
		}
		mRenderState.setDepthMask(true);
	}

	@Override
	public void drawBitmapMesh(Bitmap bitmap, BasicMesh basicMesh, GLPaint paint) {
		paint = getGLPaint(paint);
		Texture texture = mCaches.textureCache.get(bitmap);
		if (texture == null)
			return;

		Mesh mesh = mCaches.meshCache.get(basicMesh);
		if (mesh == null)
			return;
		setupDraw();

		flushBatch();

		mCaches.bindTexture(texture);
		BaseShader useShader = mShaderManager.setupTextureShader(paint, 0, 0,
				texture.getWidth(), texture.getHeight(), bitmap.hasAlpha(),
				basicMesh.hasTexCoordsAttr(), basicMesh.hasColorAttr());

		mesh.render(useShader.getShaderProgram(), basicMesh.getDrawMode());
		mRenderState.setDepthMask(true);
	}

	public void setTextureTarget(int target) {
		mRenderState.setTextureTarget(target);
	}
	
	@Override
	public void drawText(CharSequence text, int start, int end, float x, float y,
			GLPaint paint, boolean drawDefer) {
		setupDraw();
		mFontRenderer.renderText(this, text, start, end, x, y, currentSnapshot().alpha, getGLPaint(paint), currentSnapshot().clipRect, currentSnapshot().transform, !drawDefer);
	}

	class ShaderManager {

		private DefaultTextureShader mDefaultTextureShader;
		private DefaultTextureShader mTextureCoordsShader;
		private DefaultTextureShader mColorAttrTextureShader;
		private DefaultTextureShader mColorAttrTextureCoordsShader;

		private DefaultColorShader mDefaultColorShader;
		private DefaultColorShader mColorAttrShader;

		public ShaderManager() {
			mDefaultTextureShader = new DefaultTextureShader();
			mDefaultTextureShader.setHasColorAttr(false);
			mDefaultTextureShader.setHasTexcoordsAttr(false);
			mTextureCoordsShader = new DefaultTextureShader();
			mTextureCoordsShader.setHasColorAttr(false);
			mTextureCoordsShader.setHasTexcoordsAttr(true);
			
			mColorAttrTextureShader = new DefaultTextureShader();
			mColorAttrTextureShader.setHasColorAttr(true);
			mColorAttrTextureShader.setHasTexcoordsAttr(false);
			mColorAttrTextureCoordsShader = new DefaultTextureShader();
			mColorAttrTextureCoordsShader.setHasColorAttr(true);
			mColorAttrTextureCoordsShader.setHasTexcoordsAttr(true);
			
			mDefaultColorShader = new DefaultColorShader();
			mDefaultColorShader.setHasColorAttr(false);
			mColorAttrShader = new DefaultColorShader();
			mColorAttrShader.setHasColorAttr(true);
		}

		public BaseShader getDefaultTextureShader(boolean texCoords, boolean colorAttr) {
			if (texCoords) {
				if (colorAttr) {
					return mColorAttrTextureCoordsShader;
				} else {
					return mTextureCoordsShader;
				}
			} else {
				if (colorAttr) {
					return mColorAttrTextureShader;
				} else {
					return mDefaultTextureShader;
				}
			}
		}

		public BaseShader getDefaultColorShader(boolean colorAttr) {
			if (colorAttr) {
				return mColorAttrShader;
			} else {
				return mDefaultColorShader;
			}
		}

		public BaseShader setupTextureShader(GLPaint paint, float x, float y,
				float width, float height, boolean hasAlpha, boolean texCoords, boolean colorAttr) {
			BaseShader useShader = paint.getShader();
			float alpha = currentSnapshot().alpha * paint.getAlpha() / 255;
			int color = paint.getColor();
			if (useShader == null) {
				useShader = getDefaultTextureShader(texCoords, colorAttr);
			}
			useShader.setHasTexture(true);

			mCaches.useProgram(useShader.getShaderProgram());
			// 设置alpha值
			float prealpha = ((color >>> 24) & 0xFF) * alpha / 255;
			float colorR = Math.round(((color >> 16) & 0xFF) * prealpha) * 1.0f / 255;
			float colorG = Math.round(((color >> 8) & 0xFF) * prealpha) * 1.0f / 255;
			float colorB = Math.round((color & 0xFF) * prealpha) * 1.0f / 255;
			float colorA = Math.round(255 * prealpha) * 1.0f / 255;
			if (hasAlpha) {
				mRenderState.setBlendEnabled(true);
			} else {
				mRenderState.setColorMode(colorA);
			}
			useShader.setupColor(colorR, colorG, colorB, colorA);

			useShader.setupTextureCoords(x, y, width, height);
			// 将最终变换矩阵传入shader程序
			float[] m = getFinalMatrix(mFinalMatrix, currentSnapshot().transform);
			useShader.setupViewModelMatrices(m);

			useShader.setupCustomValues();
			return useShader;
		}

		public BaseShader setupColorShader(float r, float g, float b, float a, GLPaint paint, boolean colorAttr) {
			BaseShader useShader = paint.getShader();
			if (useShader == null) {
				useShader = getDefaultColorShader(colorAttr);
			}
			useShader.setHasTexture(false);

			mCaches.useProgram(useShader.getShaderProgram());
			
			mRenderState.setColorMode(a);
			useShader.setupColor(r, g, b, a);
			// 将最终变换矩阵传入shader程序
			float[] m = getFinalMatrix(mFinalMatrix, currentSnapshot().transform);
			useShader.setupViewModelMatrices(m);
			useShader.setupCustomValues();
			return useShader;
		}
	}
	
	protected void onSnapshotRestored(Snapshot removed, Snapshot restore) {
		super.onSnapshotRestored(removed, restore);
		boolean restoreViewport = (removed.flags & Snapshot.kFlagIsFboLayer) != 0;
		boolean restoreClip = (removed.flags & Snapshot.kFlagClipSet) != 0;

	    if (restoreViewport) {
	        mRenderState.setViewport(getWidth(), getHeight());
	    }
	    
	    if (restoreClip) {
	    	flushFont();
	    	dirtyClip();
	    }
	};
	
	///////////////////////////////////////////////////////////////////////////////
	//Clipping
	///////////////////////////////////////////////////////////////////////////////
	void setScissorFromClip() {
	    Rect clip = currentClipRect();
	    if (mCaches.setScissor(clip.left, clip.top, clip.width(), clip.height())) {
	    }
	    mDirtyClip = false;
	}
	
	@Override
	public void clipRect(float left, float top, float right, float bottom) {
		flushBatch();
		flushFont();
		super.clipRect(left, top, right, bottom);
		if (mDirtyClip) {
			Caches.getInstance().enableScissor();
		}
	}
	
	private void flushBatch() {
		mBatch.flush();
	}
	
	private void flushFont() {
		mFontRenderer.flushBatch();
	}
}
