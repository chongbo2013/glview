package com.glview.hwui.op;

import com.glview.graphics.mesh.BasicMesh;
import com.glview.hwui.GLCanvas;
import com.glview.hwui.GLPaint;

public class DrawMeshOp extends DrawOp {

	BasicMesh mMesh;
	
	public DrawMeshOp() {
	}
	
	public static DrawMeshOp obtain(BasicMesh mesh, GLPaint paint) {
		DrawMeshOp op = (DrawMeshOp) OpFactory.get().poll(DrawMeshOp.class);
		op.mMesh = mesh;
		op.setPaint(paint);
		return op;
	}

	@Override
	void applyDraw(GLCanvas canvas) {
		canvas.drawMesh(mMesh, mPaint);
	}
	
	@Override
	protected void recycleInner() {
		super.recycleInner();
		mMesh = null;
	}

}
