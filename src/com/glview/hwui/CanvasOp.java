package com.glview.hwui;

import com.glview.hwui.op.OpFactory;
import com.glview.pool.Poolable;

/**
 * Record operations by canvas {@link GLRecordingCanvas}, 
 * and replayed in render thread {@link RenderThread}.
 * Called by RenderNode {@link RenderNode#replay(GLCanvas)}.
 * 
 * @author lijing.lj
 */
public abstract class CanvasOp implements Poolable {
	
	/*
	 * Used to build the display list.
	 */
	public CanvasOp mNext;
	
	/*
	 * Replay the recorded operation, this method do the real drawing.
	 */
	abstract public void replay(GLCanvas canvas);
	
	abstract protected void recycleInner();
	
	/*
	 * The display list has been changed or destroyed, so we recycle unused resources.
	 */
	public final void recycle() {
		mNext = null;
		recycleInner();
		// Cache to CanvasOpFactory, so it can be reuse.
		OpFactory.get().push(this);
	}
}
