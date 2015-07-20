package com.glview.hwui;

import java.util.ArrayList;
import java.util.List;

import com.glview.graphics.Path;
import com.glview.graphics.PointF;
import com.glview.graphics.Path.Verb;

public class PathTessellator {
	
	final static int MAX_POOL_SIZE = 10000;
	
	static ThreadLocal<List<PointF>> sThreadLocal = new ThreadLocal<List<PointF>>() {
		@Override
		protected List<PointF> initialValue() {
			return new ArrayList<PointF>();
		}
	};
	
	public static void tessellatePath(Path path, GLPaint paint) {
		boolean forceClose = paint.getStyle() != GLPaint.Style.STROKE;
	}

	static boolean approximatePathOutlineVertices(Path path, boolean forceClose,
	        float sqrInvScaleX, float sqrInvScaleY, float thresholdSquared,
	        List<PointF> outputVertices) {
    	PointF lastPoint = obtainPoint();
    	int srcPts = 0;
    	PointF fMoveTo = new PointF();
    	PointF pt;
    	for (Verb verb : path.fVerbs()) {
    		switch (verb) {
            case kMove_Verb:
            	pt = obtainPoint();
            	pt.set(path.fPointFs().get(srcPts));
            	lastPoint.set(pt);
            	fMoveTo.set(lastPoint);
            	srcPts += 1;
            	pushToVector(outputVertices, pt.x, pt.y);
                break;
            case kLine_Verb:
            	pt = obtainPoint();
            	pt.set(path.fPointFs().get(srcPts));
            	lastPoint.set(pt);
            	srcPts += 1;
            	pushToVector(outputVertices, pt.x, pt.y);
                break;
            case kQuad_Verb:
                srcPts += 2;
                break;
            case kCubic_Verb:
                srcPts += 3;
                break;
            case kClose_Verb:
            	lastPoint.set(fMoveTo);
            	break;
            default:
            	break;
    		}
    	}
		return false;
	}
	
	static void pushToVector(List<PointF> vertices, float x, float y) {
		PointF pt = obtainPoint();
		pt.set(x, y);
		vertices.add(pt);
	}
	
	static PointF obtainPoint() {
		List<PointF> pts = sThreadLocal.get();
		int size = pts.size();
		if (size > 0) {
			return pts.remove(size - 1);
		}
		return new PointF();
	}
	
	static void recyclePoint(PointF pt) {
		List<PointF> pts = sThreadLocal.get();
		if (pts.size() >= MAX_POOL_SIZE) {
			return;
		}
		pts.add(pt);
	}
}
