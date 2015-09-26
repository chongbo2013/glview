package com.glview.view;

import java.util.List;

import com.glview.animation.Animator;
import com.glview.graphics.Bitmap;
import com.glview.hwui.GLCanvas;
import com.glview.hwui.RenderNode;
import com.glview.hwui.RenderPolicy;

class GLRenderer {
	
	RenderPolicy mRenderer = null;
	
	RenderNode mRootNode = new RenderNode();
	boolean mRootNodeNeedsUpdate = true;
	
	int mWidth, mHeight;
	
	private GLRenderer() {
		mRenderer = new RenderPolicy(mRootNode);
	}

	public static GLRenderer createRender() {
		return new GLRenderer();
	}
	
	public void initialize(Object surface) {
		mRenderer.initialize(surface);
	}
	
	public void startRTAnimation(List<Animator> animators) {
		mRenderer.startAnimation(animators);
	}
	
	public void stopRTAnimation(List<Animator> animators) {
		mRenderer.stopAnimation(animators);
	}
	
	public Bitmap buildDrawingCache(View v) {
		return mRenderer.buildDrawingCache(v.getDisplayList());
	}
	
	public void setSize(Object surface, int width, int height) {
		mWidth = width;
		mHeight = height;
		mRenderer.setSize(surface, width, height);
	}
	
	public void destroy(boolean full) {
		mRenderer.destroy(full);
	}
	
	public boolean isEnable() {
		return mRenderer.isEnable();
	}
	
	private void updateRootDisplayList(View view) {
		RenderNode renderNode = updateViewTreeDisplayList(view);
		
		if (mRootNodeNeedsUpdate || !mRootNode.isValid()) {
			GLCanvas canvas =  mRootNode.start(mWidth, mHeight);
			canvas.drawRenderNode(renderNode);
			mRootNode.end(canvas);
			mRootNodeNeedsUpdate = false;
		}
	}
	
	public void invalidateRoot() {
		mRootNodeNeedsUpdate = true;
	}
	
	private RenderNode updateViewTreeDisplayList(View view) {
        /*view.mPrivateFlags |= View.PFLAG_DRAWN;
        view.mRecreateDisplayList = (view.mPrivateFlags & View.PFLAG_INVALIDATED)
                == View.PFLAG_INVALIDATED;
        view.mPrivateFlags &= ~View.PFLAG_INVALIDATED;
        view.getDisplayList();
        view.mRecreateDisplayList = false;*/
		return view.updateViewDisplayList();
    }
	
	public void draw(View view) {
		updateRootDisplayList(view);
		/*
		 * draw
		 */
		syncAndDrawFrame();
	}
	
	void syncAndDrawFrame() {
		mRenderer.syncAndDrawFrame();
	}
}
