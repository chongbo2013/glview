package com.glview.hwui.op;

import com.glview.graphics.Bitmap;
import com.glview.graphics.mesh.BasicMesh;
import com.glview.hwui.GLCanvas;
import com.glview.hwui.GLPaint;

public class DrawBitmapMeshOp extends AbsDrawBitmapOp {

	BasicMesh mMesh;
	
	public DrawBitmapMeshOp() {
	}
	
	public static DrawBitmapMeshOp obtain(Bitmap bitmap, BasicMesh mesh, GLPaint paint) {
		DrawBitmapMeshOp op = (DrawBitmapMeshOp) OpFactory.get().poll(DrawBitmapMeshOp.class);
		op.setBitmap(bitmap);
		op.mMesh = mesh;
		op.setPaint(paint);
		return op;
	}

	@Override
	void applyDraw(GLCanvas canvas) {
		canvas.drawBitmapMesh(getBitmap(), mMesh, mPaint);
	}
	
	@Override
	protected void recycleInner() {
		super.recycleInner();
		mMesh = null;
	}

}
