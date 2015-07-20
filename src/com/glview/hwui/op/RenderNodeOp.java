package com.glview.hwui.op;

import com.glview.hwui.GLCanvas;
import com.glview.hwui.RenderNode;

public class RenderNodeOp extends DrawOp {

	RenderNode mRenderNode;
	
	public RenderNodeOp() {
	}
	
	public static RenderNodeOp obtain(RenderNode renderNode) {
		RenderNodeOp op = (RenderNodeOp) OpFactory.get().poll(RenderNodeOp.class);
		op.mRenderNode = renderNode;
		return op;
	}
	
	@Override
	void applyDraw(GLCanvas canvas) {
		canvas.drawRenderNode(mRenderNode);
	}

	@Override
	protected void recycleInner() {
		mRenderNode = null;
	}
	
	public void destroyRenderNode() {
		if (mRenderNode != null) {
			mRenderNode.destroy();
			mRenderNode = null;
		}
	}

}
