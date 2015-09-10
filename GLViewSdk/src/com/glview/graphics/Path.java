/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.glview.graphics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * The Path class encapsulates compound (multiple contour) geometric paths
 * consisting of straight line segments, quadratic curves, and cubic curves.
 * It can be drawn with canvas.drawPath(path, paint), either filled or stroked
 * (based on the paint's Style), or it can be used for clipping or to draw
 * text on a path.
 */
public final class Path {
	
	private PointF mTempPoint = new PointF();
	
	/**
     * Create an empty path
     */
	public Path() {
	}
	
	/**
     * Create a new path, copying the contents from the src path.
     *
     * @param src The path to copy from when initializing the new path
     */
	public Path(Path src) {
		fPoints.addAll(src.fPoints);
		fVerbs.addAll(src.fVerbs);
		fLastMoveToIndex = src.fLastMoveToIndex;
	}
	
	/**
     * Clear any lines and curves from the path, making it empty.
     * This does NOT change the fill-type setting.
     */
	public void reset() {
		fPoints.clear();
		fVerbs.clear();
		fLastMoveToIndex = ~0;
	}
	
	/** Replace the contents of this with the contents of src.
	    */
    public void set(Path src) {
        if (this != src) {
        	reset();
        	fPoints.addAll(src.fPoints);
    		fVerbs.addAll(src.fVerbs);
    		fLastMoveToIndex = src.fLastMoveToIndex;
        }
    }
	
	/**
     * Returns true if the path is empty (contains no lines or curves)
     *
     * @return true if the path is empty (contains no lines or curves)
     */
    public boolean isEmpty() {
        return countPoints() == 0;
    }
	
	public PointF getLastPt() {
		return getLastPt(mTempPoint);
	}
	
	public PointF getLastPt(PointF dst) {
		int count = countPoints();
		if (count > 0) {
			dst.set(fPoints.get(count - 1));
		} else {
			dst.set(0, 0);
		}
		return dst;
	}
	
	/**
     * Set the beginning of the next contour to the point (x,y).
     *
     * @param x The x-coordinate of the start of a new contour
     * @param y The y-coordinate of the start of a new contour
     */
	public void moveTo(float x, float y) {
		fLastMoveToIndex = countPoints();
		int p = growForVerb(Verb.kMove_Verb);
		fPoints.get(p).set(x, y);
	}
	
	/**
     * Set the beginning of the next contour relative to the last point on the
     * previous contour. If there is no previous contour, this is treated the
     * same as moveTo().
     *
     * @param dx The amount to add to the x-coordinate of the end of the
     *           previous contour, to specify the start of a new contour
     * @param dy The amount to add to the y-coordinate of the end of the
     *           previous contour, to specify the start of a new contour
     */
	public void rMoveTo(float dx, float dy) {
		PointF lastPoint = getLastPt();
		moveTo(lastPoint.x + dx, lastPoint.y + dy);
	}
	
	/**
     * Add a line from the last point to the specified point (x,y).
     * If no moveTo() call has been made for this contour, the first point is
     * automatically set to (0,0).
     *
     * @param x The x-coordinate of the end of a line
     * @param y The y-coordinate of the end of a line
     */
	public void lineTo(float x, float y) {
		this.injectMoveToIfNeeded();
		int p = growForVerb(Verb.kLine_Verb);
		fPoints.get(p).set(x, y);
	}
	
	/**
     * Same as lineTo, but the coordinates are considered relative to the last
     * point on this contour. If there is no previous point, then a moveTo(0,0)
     * is inserted automatically.
     *
     * @param dx The amount to add to the x-coordinate of the previous point on
     *           this contour, to specify a line
     * @param dy The amount to add to the y-coordinate of the previous point on
     *           this contour, to specify a line
     */
	public void rLineTo(float dx, float dy) {
		this.injectMoveToIfNeeded();
		PointF lastPoint = getLastPt();
		lineTo(lastPoint.x + dx, lastPoint.y + dy);
	}
	
	/**
     * Add a quadratic bezier from the last point, approaching control point
     * (x1,y1), and ending at (x2,y2). If no moveTo() call has been made for
     * this contour, the first point is automatically set to (0,0).
     *
     * @param x1 The x-coordinate of the control point on a quadratic curve
     * @param y1 The y-coordinate of the control point on a quadratic curve
     * @param x2 The x-coordinate of the end point on a quadratic curve
     * @param y2 The y-coordinate of the end point on a quadratic curve
     */
    public void quadTo(float x1, float y1, float x2, float y2) {
    	this.injectMoveToIfNeeded();
    	int p = growForVerb(Verb.kQuad_Verb);
    	fPoints.get(p).set(x1, y1);
    	fPoints.get(p + 1).set(x2, y2);
    }

    /**
     * Same as quadTo, but the coordinates are considered relative to the last
     * point on this contour. If there is no previous point, then a moveTo(0,0)
     * is inserted automatically.
     *
     * @param dx1 The amount to add to the x-coordinate of the last point on
     *            this contour, for the control point of a quadratic curve
     * @param dy1 The amount to add to the y-coordinate of the last point on
     *            this contour, for the control point of a quadratic curve
     * @param dx2 The amount to add to the x-coordinate of the last point on
     *            this contour, for the end point of a quadratic curve
     * @param dy2 The amount to add to the y-coordinate of the last point on
     *            this contour, for the end point of a quadratic curve
     */
    public void rQuadTo(float dx1, float dy1, float dx2, float dy2) {
    	this.injectMoveToIfNeeded();  // This can change the result of this->getLastPt().
        PointF pt = getLastPt();
        quadTo(pt.x + dx1, pt.y + dy1, pt.x + dx2, pt.y + dy2);
    }
    
    /**
     * Add a cubic bezier from the last point, approaching control points
     * (x1,y1) and (x2,y2), and ending at (x3,y3). If no moveTo() call has been
     * made for this contour, the first point is automatically set to (0,0).
     *
     * @param x1 The x-coordinate of the 1st control point on a cubic curve
     * @param y1 The y-coordinate of the 1st control point on a cubic curve
     * @param x2 The x-coordinate of the 2nd control point on a cubic curve
     * @param y2 The y-coordinate of the 2nd control point on a cubic curve
     * @param x3 The x-coordinate of the end point on a cubic curve
     * @param y3 The y-coordinate of the end point on a cubic curve
     */
    public void cubicTo(float x1, float y1, float x2, float y2,
                        float x3, float y3) {
    	this.injectMoveToIfNeeded();
    	int p = growForVerb(Verb.kCubic_Verb);
    	fPoints.get(p).set(x1, y1);
    	fPoints.get(p + 1).set(x2, y2);
    	fPoints.get(p + 2).set(x3, y3);
    }

    /**
     * Same as cubicTo, but the coordinates are considered relative to the
     * current point on this contour. If there is no previous point, then a
     * moveTo(0,0) is inserted automatically.
     */
    public void rCubicTo(float x1, float y1, float x2, float y2,
                         float x3, float y3) {
    	this.injectMoveToIfNeeded();  // This can change the result of this->getLastPt().
        PointF pt = getLastPt();
        cubicTo(pt.x + x1, pt.y + y1, pt.x + x2, pt.y + y2, pt.x + x3, pt.y + y3);
    }
	
    /**
     * Append the specified arc to the path as a new contour. If the start of
     * the path is different from the path's current last point, then an
     * automatic lineTo() is added to connect the current contour to the
     * start of the arc. However, if the path is empty, then we call moveTo()
     * with the first point of the arc.
     *
     * @param oval        The bounds of oval defining shape and size of the arc
     * @param startAngle  Starting angle (in degrees) where the arc begins
     * @param sweepAngle  Sweep angle (in degrees) measured clockwise, treated
     *                    mod 360.
     * @param forceMoveTo If true, always begin a new contour with the arc
     */
    public void arcTo(RectF oval, float startAngle, float sweepAngle,
                      boolean forceMoveTo) {
        arcTo(oval.left, oval.top, oval.right, oval.bottom, startAngle, sweepAngle, forceMoveTo);
    }

    /**
     * Append the specified arc to the path as a new contour. If the start of
     * the path is different from the path's current last point, then an
     * automatic lineTo() is added to connect the current contour to the
     * start of the arc. However, if the path is empty, then we call moveTo()
     * with the first point of the arc.
     *
     * @param oval        The bounds of oval defining shape and size of the arc
     * @param startAngle  Starting angle (in degrees) where the arc begins
     * @param sweepAngle  Sweep angle (in degrees) measured clockwise
     */
    public void arcTo(RectF oval, float startAngle, float sweepAngle) {
        arcTo(oval.left, oval.top, oval.right, oval.bottom, startAngle, sweepAngle, false);
    }
    
    ThreadLocal<PointF[]> kBuildQuadArcThreadLocal = new ThreadLocal<PointF[]>() {
    	protected PointF[] initialValue() {
			return new PointF[] { new PointF(), new PointF(), new PointF(),
					new PointF(), new PointF(), new PointF(), new PointF(),
					new PointF(), new PointF(), new PointF(), new PointF(),
					new PointF(), new PointF(), new PointF(), new PointF(),
					new PointF(), new PointF() };
    	};
    };
    /**
     * Append the specified arc to the path as a new contour. If the start of
     * the path is different from the path's current last point, then an
     * automatic lineTo() is added to connect the current contour to the
     * start of the arc. However, if the path is empty, then we call moveTo()
     * with the first point of the arc.
     *
     * @param startAngle  Starting angle (in degrees) where the arc begins
     * @param sweepAngle  Sweep angle (in degrees) measured clockwise, treated
     *                    mod 360.
     * @param forceMoveTo If true, always begin a new contour with the arc
     */
    public void arcTo(float left, float top, float right, float bottom, float startAngle,
            float sweepAngle, boolean forceMoveTo) {
    	if (right < left || bottom < top) return;
    	PointF pts[] = kBuildQuadArcThreadLocal.get();
    	RectF oval = new RectF(left, top, right, bottom);
    	int count = build_arc_points(oval, startAngle, sweepAngle, pts);
    	if ((count & 1) != 1) {
    		return;
    	}
    	if (countVerbs() == 0) {
            forceMoveTo = true;
        }

    	if (forceMoveTo) {
        	this.moveTo(pts[0].x, pts[0].y);
        } else {
        	this.lineTo(pts[0].x, pts[0].y);
        }
        for (int i = 1; i < count; i += 2) {
            this.quadTo(pts[i].x, pts[i].y, pts[i + 1].x, pts[i + 1].y);
        }
    }
    
    private int build_arc_points(RectF oval, float startAngle,
            float sweepAngle, PointF[] pts) {
    	if (0 == sweepAngle &&
	        (0 == startAngle || 360f == startAngle)) {
	        // Chrome uses this path to move into and out of ovals. If not
	        // treated as a special case the moves can distort the oval's
	        // bounding box (and break the circle special case).
	        pts[0].set(oval.right, oval.centerY());
	        return 1;
	    } else if (0 == oval.width() && 0 == oval.height()) {
	        // Chrome will sometimes create 0 radius round rects. Having degenerate
	        // quad segments in the path prevents the path from being recognized as
	        // a rect.
	        // TODO: optimizing the case where only one of width or height is zero
	        // should also be considered. This case, however, doesn't seem to be
	        // as common as the single point case.
	        pts[0].set(oval.right, oval.top);
	        return 1;
	    }
    	Vector start = new Vector(), stop = new Vector();
    	float a = startAngle * (float) (Math.PI / 180.0f);
		start.y = (float) Math.sin(a);
		if (Math.abs(start.y) <= FloatNearlyZero) {
			start.y = 0f;
		}
		start.x = (float) Math.cos(a);
		if (Math.abs(start.x) <= FloatNearlyZero) {
			start.x = 0f;
		}
		float stopDegrees = (startAngle + sweepAngle) * (float) (Math.PI / 180.0f);
		stop.y = (float) Math.sin(stopDegrees);
		if (Math.abs(stop.y) <= FloatNearlyZero) {
			stop.y = 0f;
		}
		stop.x = (float) Math.cos(stopDegrees);
		if (Math.abs(stop.x) <= FloatNearlyZero) {
			stop.x = 0f;
		}
		
		/*  If the sweep angle is nearly (but less than) 360, then due to precision
        loss in radians-conversion and/or sin/cos, we may end up with coincident
        vectors, which will fool SkBuildQuadArc into doing nothing (bad) instead
        of drawing a nearly complete circle (good).
             e.g. canvas.drawArc(0, 359.99, ...)
             -vs- canvas.drawArc(0, 359.9, ...)
        We try to detect this edge case, and tweak the stop vector
     */
		if (start.equals(stop)) {
	        float sw = Math.abs(sweepAngle);
	        if (sw < 360f && sw > 359f) {
	            float stopRad = stopDegrees;
	            // make a guess at a tiny angle (in radians) to tweak by
	            float deltaRad = sweepAngle > 0 ? (1f/512) : (-1f/512);
	            // not sure how much will be enough, so we use a loop
	            do {
	                stopRad -= deltaRad;
	                stop.y = (float) Math.sin(stopRad);
	        		if (Math.abs(stop.y) <= FloatNearlyZero) {
	        			stop.y = 0f;
	        		}
	        		stop.x = (float) Math.cos(stopRad);
	        		if (Math.abs(stop.x) <= FloatNearlyZero) {
	        			stop.x = 0f;
	        		}
	            } while (start.equals(stop));
	        }
	    }
		
		Matrix matrix = new Matrix();
		matrix.setScale(oval.width() / 2, oval.height() / 2);
	    matrix.postTranslate(oval.centerX(), oval.centerY());
	    
    	return BuildQuadArc(start, stop, sweepAngle > 0 ? Direction.CW :
    		Direction.CCW, matrix, pts);
    }
    
    private final static float SK_ScalarTanPIOver8 = 0.414213562f;
    private final static float SK_ScalarRoot2Over2 = 0.707106781f;
    private final static float SK_ScalarSqrt2 = 1.41421356f;
    private final static float SK_ScalarPI = 3.14159265f;
    private final static float CUBIC_ARC_FACTOR = ((SK_ScalarSqrt2 - 1) * 4 / 3);
    private final static float FloatNearlyZero = 1f / (1 << 12);
    private final static float SK_MID_RRECT_OFFSET = (float) ((1f + SK_ScalarTanPIOver8 + (1f / (Math.pow(2, 23))) * 4) / 2);
    
    final static PointF gQuadCirclePts[] = new PointF[] {
    	// The mid point of the quadratic arc approximation is half way between the two
    	// control points. The float epsilon adjustment moves the on curve point out by
    	// two bits, distributing the convex test error between the round rect
    	// approximation and the convex cross product sign equality test.
    	new PointF(1f, 0f),
    	new PointF(1f, SK_ScalarTanPIOver8),
    	new PointF(SK_MID_RRECT_OFFSET, SK_MID_RRECT_OFFSET),
    	new PointF(SK_ScalarTanPIOver8, 1f),
    	
    	new PointF(0f, 1f),
    	new PointF(-SK_ScalarTanPIOver8, 1f),
    	new PointF(-SK_MID_RRECT_OFFSET,  SK_MID_RRECT_OFFSET),
    	new PointF(-1f,  SK_ScalarTanPIOver8),
    	
    	new PointF(-1f, 0f),
    	new PointF(-1f, -SK_ScalarTanPIOver8),
    	new PointF(-SK_MID_RRECT_OFFSET,  -SK_MID_RRECT_OFFSET),
    	new PointF(-SK_ScalarTanPIOver8,  - 1f),
    	
    	new PointF(0f, -1f),
    	new PointF(SK_ScalarTanPIOver8, -1f),
    	new PointF(SK_MID_RRECT_OFFSET,  -SK_MID_RRECT_OFFSET),
    	new PointF(1f,  -SK_ScalarTanPIOver8),
    	
    	new PointF(1f,  0)};
    
    int BuildQuadArc(Vector uStart, Vector uStop,
            Direction dir, Matrix userMatrix,
            PointF quadPoints[]) {
		// rotate by x,y so that uStart is (1.0)
		float x = uStart.dot(uStop);
		float y = uStart.crs(uStop);

		float absX = Math.abs(x);
		float absY = Math.abs(y);

		int pointCount;

		// check for (effectively) coincident vectors
		// this can happen if our angle is nearly 0 or nearly 180 (y == 0)
		// ... we use the dot-prod to distinguish between 0 and 180 (x > 0)
		if (absY <= FloatNearlyZero
				&& x > 0
				&& ((y >= 0 && Direction.CW == dir) || (y <= 0 && Direction.CCW == dir))) {

			// just return the start-point
			quadPoints[0].set(1f, 0);
			pointCount = 1;
		} else {
			if (dir == Direction.CCW) {
				y = -y;
			}
			// what octant (quadratic curve) is [xy] in?
			int oct = 0;
			boolean sameSign = true;

			if (0 == y) {
				oct = 4; // 180
			} else if (0 == x) {
				oct = y > 0 ? 2 : 6; // 90 : 270
			} else {
				if (y < 0) {
					oct += 4;
				}
				if ((x < 0) != (y < 0)) {
					oct += 2;
					sameSign = false;
				}
				if ((absX < absY) == sameSign) {
					oct += 1;
				}
			}

			int wholeCount = oct << 1;
			System.arraycopy(gQuadCirclePts, 0, quadPoints, 0, wholeCount + 1);

			// TODO
			// PointF arc = &gQuadCirclePts[wholeCount];
			// if (truncate_last_curve(arc, x, y, quadPoints[wholeCount + 1])) {
			// wholeCount += 2;
			// }
			pointCount = wholeCount + 1;
		}

		// now handle counter-clockwise and the initial unitStart rotation
		Matrix matrix = new Matrix();
		matrix.setSinCos(uStart.y, uStart.x);
		if (dir == Direction.CCW) {
			matrix.preScale(1f, -1f);
		}
		if (userMatrix != null) {
			matrix.postConcat(userMatrix);
		}
		matrix.mapPoints(quadPoints, pointCount);
		return pointCount;
}
    
	/**
     * Close the current contour. If the current point is not equal to the
     * first point of the contour, a line segment is automatically added.
     */
	public void close() {
		int count = countVerbs();
	    if (count > 0) {
	    	switch (fVerbs.get(count - 1)) {
	    	case kLine_Verb:
            case kQuad_Verb:
            case kConic_Verb:
            case kCubic_Verb:
            case kMove_Verb: {
            	growForVerb(Verb.kClose_Verb);
                break;
            }
            case kClose_Verb:
                // don't add a close if it's the first verb or a repeat
                break;
            default:
                break;
			}
	    }
	    fLastMoveToIndex ^= (~fLastMoveToIndex >> (8 * 4 - 1));
	}

	/**
     * Specifies how closed shapes (e.g. rects, ovals) are oriented when they
     * are added to a path.
     */
    public enum Direction {
        /** clockwise */
        CW,    // must match enum in SkPath.h
        /** counter-clockwise */
        CCW;    // must match enum in SkPath.h

        Direction() {
        }
    }
    
    /**
     * Add a closed rectangle contour to the path
     *
     * @param rect The rectangle to add as a closed contour to the path
     * @param dir  The direction to wind the rectangle's contour
     */
    public void addRect(RectF rect, Direction dir) {
        addRect(rect.left, rect.top, rect.right, rect.bottom, dir);
    }

    /**
     * Add a closed rectangle contour to the path
     *
     * @param left   The left side of a rectangle to add to the path
     * @param top    The top of a rectangle to add to the path
     * @param right  The right side of a rectangle to add to the path
     * @param bottom The bottom of a rectangle to add to the path
     * @param dir    The direction to wind the rectangle's contour
     */
    public void addRect(float left, float top, float right, float bottom, Direction dir) {
		moveTo(left, top);
		if (dir == Direction.CCW) {
			lineTo(left, bottom);
			lineTo(right, bottom);
			lineTo(right, top);
		} else {
			lineTo(right, top);
			lineTo(right, bottom);
			lineTo(left, bottom);
		}
		close();
    }
    
    /**
     * Add a closed oval contour to the path
     *
     * @param oval The bounds of the oval to add as a closed contour to the path
     * @param dir  The direction to wind the oval's contour
     */
    public void addOval(RectF oval, Direction dir) {
        addOval(oval.left, oval.top, oval.right, oval.bottom, dir);
    }

    /**
     * Add a closed oval contour to the path
     *
     * @param dir The direction to wind the oval's contour
     */
    public void addOval(float left, float top, float right, float bottom, Direction dir) {
    	float    cx = (right + left) * .5f;
    	float    cy = (bottom + top) * .5f;
    	float    rx = (right - left) * .5f;
    	float    ry = (bottom - top) * .5f;

    	float    sx = (rx * SK_ScalarTanPIOver8);
    	float    sy = (ry * SK_ScalarTanPIOver8);
    	float    mx = (rx * SK_ScalarRoot2Over2);
    	float    my = (ry * SK_ScalarRoot2Over2);
    	
    	/*
	        To handle imprecision in computing the center and radii, we revert to
	        the provided bounds when we can (i.e. use oval.fLeft instead of cx-rx)
	        to ensure that we don't exceed the oval's bounds *ever*, since we want
	        to use oval for our fast-bounds, rather than have to recompute it.
	    */
	    final float L = left;      // cx - rx
	    final float T = top;       // cy - ry
	    final float R = right;     // cx + rx
	    final float B = bottom;    // cy + ry
	    
	    this.moveTo(R, cy);
	    if (dir == Direction.CCW) {
	        this.quadTo(      R, cy - sy, cx + mx, cy - my);
	        this.quadTo(cx + sx,       T, cx     ,       T);
	        this.quadTo(cx - sx,       T, cx - mx, cy - my);
	        this.quadTo(      L, cy - sy,       L, cy     );
	        this.quadTo(      L, cy + sy, cx - mx, cy + my);
	        this.quadTo(cx - sx,       B, cx     ,       B);
	        this.quadTo(cx + sx,       B, cx + mx, cy + my);
	        this.quadTo(      R, cy + sy,       R, cy     );
	    } else {
	        this.quadTo(      R, cy + sy, cx + mx, cy + my);
	        this.quadTo(cx + sx,       B, cx     ,       B);
	        this.quadTo(cx - sx,       B, cx - mx, cy + my);
	        this.quadTo(      L, cy + sy,       L, cy     );
	        this.quadTo(      L, cy - sy, cx - mx, cy - my);
	        this.quadTo(cx - sx,       T, cx     ,       T);
	        this.quadTo(cx + sx,       T, cx + mx, cy - my);
	        this.quadTo(      R, cy - sy,       R, cy     );
	    }
	    this.close();
    }
    
    /**
     * Add a closed circle contour to the path
     *
     * @param x   The x-coordinate of the center of a circle to add to the path
     * @param y   The y-coordinate of the center of a circle to add to the path
     * @param radius The radius of a circle to add to the path
     * @param dir    The direction to wind the circle's contour
     */
    public void addCircle(float x, float y, float radius, Direction dir) {
    	if (radius > 0) {
    		addOval(x - radius, y - radius, x + radius, y + radius, dir);
        }
    }
    
    /**
     * Add a closed round-rectangle contour to the path
  *
  * @param rect The bounds of a round-rectangle to add to the path
  * @param rx   The x-radius of the rounded corners on the round-rectangle
  * @param ry   The y-radius of the rounded corners on the round-rectangle
  * @param dir  The direction to wind the round-rectangle's contour
  */
 public void addRoundRect(RectF rect, float rx, float ry, Direction dir) {
     addRoundRect(rect.left, rect.top, rect.right, rect.bottom, rx, ry, dir);
 }

 /**
  * Add a closed round-rectangle contour to the path
  *
  * @param rx   The x-radius of the rounded corners on the round-rectangle
  * @param ry   The y-radius of the rounded corners on the round-rectangle
  * @param dir  The direction to wind the round-rectangle's contour
  */
 public void addRoundRect(float left, float top, float right, float bottom, float rx, float ry,
         Direction dir) {
	 float w = right - left;
	    float halfW = w * .5f;
	    float h = bottom - top;
	    float halfH = h * .5f;

	    if (halfW <= 0 || halfH <= 0) {
	        return;
	    }

	    boolean skip_hori = rx >= halfW;
	    boolean skip_vert = ry >= halfH;

	    if (skip_hori && skip_vert) {
	        this.addOval(left, top, right, bottom, dir);
	        return;
	    }

	    if (skip_hori) {
	        rx = halfW;
	    } else if (skip_vert) {
	        ry = halfH;
	    }
	    float sx = (rx * CUBIC_ARC_FACTOR);
	    float sy = (ry * CUBIC_ARC_FACTOR);

	    moveTo(right - rx, top);                  // top-right
	    if (dir == Direction.CCW) {
	        if (!skip_hori) {
	            lineTo(left + rx, top);           // top
	        }
	        cubicTo(left + rx - sx, top,
	                      left, top + ry - sy,
	                      left, top + ry);          // top-left
	        if (!skip_vert) {
	            lineTo(left, bottom - ry);        // left
	        }
	        cubicTo(left, bottom - ry + sy,
	                      left + rx - sx, bottom,
	                      left + rx, bottom);       // bot-left
	        if (!skip_hori) {
	            lineTo(right - rx, bottom);       // bottom
	        }
	        cubicTo(right - rx + sx, bottom,
	                      right, bottom - ry + sy,
	                      right, bottom - ry);      // bot-right
	        if (!skip_vert) {
	            lineTo(right, top + ry);          // right
	        }
	        cubicTo(right, top + ry - sy,
	                      right - rx + sx, top,
	                      right - rx, top);         // top-right
	    } else {
	        cubicTo(right - rx + sx, top,
	                      right, top + ry - sy,
	                      right, top + ry);         // top-right
	        if (!skip_vert) {
	            lineTo(right, bottom - ry);       // right
	        }
	        cubicTo(right, bottom - ry + sy,
	                      right - rx + sx, bottom,
	                      right - rx, bottom);      // bot-right
	        if (!skip_hori) {
	            lineTo(left + rx, bottom);        // bottom
	        }
	        cubicTo(left + rx - sx, bottom,
	                      left, bottom - ry + sy,
	                      left, bottom - ry);       // bot-left
	        if (!skip_vert) {
	            lineTo(left, top + ry);           // left
	        }
	        cubicTo(left, top + ry - sy,
	                      left + rx - sx, top,
	                      left + rx, top);          // top-left
	        if (!skip_hori) {
	            lineTo(right - rx, top);          // top
	        }
	    }
	    close();
 }

 /**
  * Add a closed round-rectangle contour to the path. Each corner receives
  * two radius values [X, Y]. The corners are ordered top-left, top-right,
  * bottom-right, bottom-left
  *
  * @param rect The bounds of a round-rectangle to add to the path
  * @param radii Array of 8 values, 4 pairs of [X,Y] radii
  * @param dir  The direction to wind the round-rectangle's contour
  */
 public void addRoundRect(RectF rect, float[] radii, Direction dir) {
     if (rect == null) {
         throw new NullPointerException("need rect parameter");
     }
     addRoundRect(rect.left, rect.top, rect.right, rect.bottom, radii, dir);
 }

 /**
  * Add a closed round-rectangle contour to the path. Each corner receives
  * two radius values [X, Y]. The corners are ordered top-left, top-right,
  * bottom-right, bottom-left
  *
  * @param radii Array of 8 values, 4 pairs of [X,Y] radii
  * @param dir  The direction to wind the round-rectangle's contour
  */
 public void addRoundRect(float left, float top, float right, float bottom, float[] radii,
         Direction dir) {
     if (radii.length < 8) {
         throw new ArrayIndexOutOfBoundsException("radii[] needs 8 values");
     }
     // TODO
 }
    
    void injectMoveToIfNeeded() {
        if (fLastMoveToIndex < 0) {
        	float x, y;
        	if (countVerbs() == 0) {
        		x = y = 0f;
        	} else {
        		PointF pt = fPoints.get(~fLastMoveToIndex);
        		x = pt.x;
        		y = pt.y;
        	}
        	moveTo(x, y);
        }
    }
    
    static void addMove(List<PointF> segmentPoints, List<Float> lengths,
    		PointF point) {
    	float length = 0;
    	if (!lengths.isEmpty()) {
    		length = lengths.get(lengths.size() - 1);
    	}
    	segmentPoints.add(new PointF(point));
    	lengths.add(length);
    }
    
    static void addLine(List<PointF> segmentPoints, List<Float> lengths,
    		PointF toPoint) {
    	if (segmentPoints.isEmpty()) {
    		segmentPoints.add(new PointF());
    		lengths.add(.0f);
    	} else if (segmentPoints.get(segmentPoints.size() - 1).equals(toPoint)) {
    		return; // Empty line
    	}
    	PointF l = segmentPoints.get(segmentPoints.size() - 1);
    	float length = lengths.get(lengths.size() - 1) + PointF.length(l.x - toPoint.x, l.y - toPoint.y);
    	segmentPoints.add(new PointF(toPoint));
    	lengths.add(length);
    }
    
    static ThreadLocal<PointF> sThreadLocal = new ThreadLocal<PointF>(){
    	@Override
    	protected PointF initialValue() {
    		return new PointF();
    	}
    };
    
    static float cubicCoordinateCalculation(float t, float p0, float p1, float p2, float p3) {
        float oneMinusT = 1 - t;
        float oneMinusTSquared = oneMinusT * oneMinusT;
        float oneMinusTCubed = oneMinusTSquared * oneMinusT;
        float tSquared = t * t;
        float tCubed = tSquared * t;
        return (oneMinusTCubed * p0) + (3 * oneMinusTSquared * t * p1)
                + (3 * oneMinusT * tSquared * p2) + (tCubed * p3);
    }

    static PointF cubicBezierCalculation(float t, PointF[] points) {
        float x = cubicCoordinateCalculation(t, points[0].x, points[1].x,
            points[2].x, points[3].x);
        float y = cubicCoordinateCalculation(t, points[0].y, points[1].y,
            points[2].y, points[3].y);
        PointF pt = sThreadLocal.get();
        pt.set(x, y);
        return pt;
    }
    
    static float quadraticCoordinateCalculation(float t, float p0, float p1, float p2) {
        float oneMinusT = 1 - t;
        return oneMinusT * ((oneMinusT * p0) + (t * p1)) + t * ((oneMinusT * p1) + (t * p2));
    }

    static PointF quadraticBezierCalculation(float t, PointF[] points) {
        float x = quadraticCoordinateCalculation(t, points[0].x, points[1].x, points[2].x);
        float y = quadraticCoordinateCalculation(t, points[0].y, points[1].y, points[2].y);
        PointF pt = sThreadLocal.get();
        pt.set(x, y);
        return pt;
    }
    
    // Subdivide a section of the Bezier curve, set the mid-point and the mid-t value.
    // Returns true if further subdivision is necessary as defined by errorSquared.
    static boolean subdividePoints(PointF[] points, boolean cubicBezierCalculation,
            float t0, PointF p0, float t1, PointF p1,
            float midT, PointF midPoint, float errorSquared) {
        midT = (t1 + t0) / 2;
        float midX = (p1.x + p0.x) / 2;
        float midY = (p1.y + p0.y) / 2;

        PointF pt;
        if (!cubicBezierCalculation) {
        	pt = quadraticBezierCalculation(midT, points);
    	} else {
    		pt = cubicBezierCalculation(midT, points);
    	}
        if (midPoint == null) {
        	midPoint = pt;
        } else {
        	midPoint.set(pt);
        }
        
        float xError = midPoint.x - midX;
        float yError = midPoint.y - midY;
        float midErrorSquared = (xError * xError) + (yError * yError);
        return midErrorSquared > errorSquared;
    }
    
    // Divides Bezier curves until linear interpolation is very close to accurate, using
    // errorSquared as a metric. Cubic Bezier curves can have an inflection point that improperly
    // short-circuit subdivision. If you imagine an S shape, the top and bottom points being the
    // starting and end points, linear interpolation would mark the center where the curve places
    // the point. It is clearly not the case that we can linearly interpolate at that point.
    // doubleCheckDivision forces a second examination between subdivisions to ensure that linear
    // interpolation works.
    static void addBezier(PointF[] points,
            boolean cubicBezierCalculation, List<PointF> segmentPoints,
            List<Float> lengths, float errorSquared, boolean doubleCheckDivision) {
    	
    	TreeMap<Float, PointF> tToPoint = new TreeMap<Float, PointF>();
    	
    	if (!cubicBezierCalculation) {
    		tToPoint.put(0f, new PointF(quadraticBezierCalculation(0f, points)));
    		tToPoint.put(1f, new PointF(quadraticBezierCalculation(1f, points)));
    	} else {
    		tToPoint.put(0f, new PointF(cubicBezierCalculation(0f, points)));
    		tToPoint.put(1f, new PointF(cubicBezierCalculation(1f, points)));
    	}
    	
    	Entry<Float, PointF> iter = tToPoint.pollFirstEntry();
    	Entry<Float, PointF> next = tToPoint.firstEntry();
    	float t0, t1;
    	PointF p1;
    	while (next != null) {
    		addLine(segmentPoints, lengths, iter.getValue());
    		boolean needsSubdivision = true;
    		t0 = iter.getKey();
            t1 = next.getKey();
    		p1 = next.getValue();
            do {
                float midT;
                PointF midPoint = new PointF();
                midT = (t1 + t0) / 2;
                needsSubdivision = subdividePoints(points, cubicBezierCalculation, t0,
                        iter.getValue(), t1, p1, midT, midPoint, errorSquared);
                if (!needsSubdivision && doubleCheckDivision) {
                    needsSubdivision = subdividePoints(points, cubicBezierCalculation, iter.getKey(),
                            iter.getValue(), midT, midPoint, 0, null, errorSquared);
                    if (needsSubdivision) {
                        // Found an inflection point. No need to double-check.
                        doubleCheckDivision = false;
                    }
                }
                if (needsSubdivision) {
                    tToPoint.put(midT, midPoint);
                    t1 = midT;
                    p1 = midPoint;
                }
            } while (needsSubdivision);
            
    		iter = tToPoint.pollFirstEntry();
    		next = tToPoint.firstEntry();
    	}
    }
    
    static void createVerbSegments(Verb verb, PointF[] points,
    		List<PointF> segmentPoints, List<Float> lengths, float errorSquared) {
    	switch (verb) {
        case kMove_Verb:
        	addMove(segmentPoints, lengths, points[0]);
            break;
        case kClose_Verb:
        	addLine(segmentPoints, lengths, points[0]);
            break;
        case kLine_Verb:
        	addLine(segmentPoints, lengths, points[1]);
            break;
        case kQuad_Verb:
        	addBezier(points, false, segmentPoints, lengths, errorSquared, false);
            break;
        case kCubic_Verb:
        	addBezier(points, true, segmentPoints, lengths, errorSquared, true);
            break;
        default:
            // Leave element as NULL, Conic sections are not supported.
            break;
    	}
    }
    
    /**
     * Approximate the <code>Path</code> with a series of line segments.
     * This returns float[] with the array containing point components.
     * There are three components for each point, in order:
     * <ul>
     *     <li>Fraction along the length of the path that the point resides</li>
     *     <li>The x coordinate of the point</li>
     *     <li>The y coordinate of the point</li>
     * </ul>
     * <p>Two points may share the same fraction along its length when there is
     * a move action within the Path.</p>
     *
     * @param acceptableError The acceptable error for a line on the
     *                        Path. Typically this would be 0.5 so that
     *                        the error is less than half a pixel.
     * @return An array of components for points approximating the Path.
     * @hide
     */
    public float[] approximate(float acceptableError) {
    	PointF pts[] = new PointF[] {new PointF(), new PointF(), new PointF(), new PointF()};
    	PointF lastPoint = new PointF();
    	List<PointF> segmentPoints = new ArrayList<PointF>();
    	List<Float> lengths = new ArrayList<Float>();
        float errorSquared = acceptableError * acceptableError;
    	int srcPts = 0;
    	PointF fMoveTo = new PointF();;
    	for (Verb verb : fVerbs) {
    		switch (verb) {
            case kMove_Verb:
            	pts[0].set(fPoints.get(srcPts));
            	lastPoint.set(pts[0]);
            	fMoveTo.set(lastPoint);
            	srcPts += 1;
                break;
            case kLine_Verb:
            	pts[0].set(lastPoint);
            	pts[1].set(fPoints.get(srcPts));
            	lastPoint.set(pts[1]);
            	srcPts += 1;
                break;
            case kQuad_Verb:
            	pts[0].set(lastPoint);;
            	pts[1].set(fPoints.get(srcPts));
            	pts[2].set(fPoints.get(srcPts + 1));
                lastPoint.set(pts[2]);
                srcPts += 2;
                break;
            case kCubic_Verb:
            	pts[0].set(lastPoint);;
            	pts[1].set(fPoints.get(srcPts));
                pts[2].set(fPoints.get(srcPts + 1));
                pts[3].set(fPoints.get(srcPts + 2));
                lastPoint.set(pts[3]);
                srcPts += 3;
                break;
            case kClose_Verb:
            	lastPoint.set(fMoveTo);
            	pts[0].set(fMoveTo);
            	break;
            default:
            	break;
    		}
    		createVerbSegments(verb, pts, segmentPoints, lengths, errorSquared);
    	}
    	if (segmentPoints.isEmpty()) {
            int numVerbs = countVerbs();
            if (numVerbs == 1) {
                addMove(segmentPoints, lengths, fPoints.get(0));
            } else {
                // Invalid or empty path. Fall back to point(0,0)
                addMove(segmentPoints, lengths, new PointF());
            }
        }
    	float totalLength = lengths.get(lengths.size() - 1);
        if (totalLength == 0) {
            // Lone Move instructions should still be able to animate at the same value.
            segmentPoints.add(segmentPoints.get(segmentPoints.size() - 1));
            lengths.add(1f);
            totalLength = 1;
        }

        int numPoints = segmentPoints.size();
        int approximationArraySize = numPoints * 3;

        float[] approximation = new float[approximationArraySize];

        int approximationIndex = 0;
        for (int i = 0; i < numPoints; i++) {
            PointF point = segmentPoints.get(i);
            approximation[approximationIndex++] = lengths.get(i) / totalLength;
            approximation[approximationIndex++] = point.x;
            approximation[approximationIndex++] = point.y;
        }

        return approximation;
    }
    
    private final List<PointF> fPoints = new ArrayList<PointF>();
    private final List<Verb> fVerbs = new ArrayList<Verb>();
    private int fLastMoveToIndex = ~0;
    
    int countVerbs() {
    	return fVerbs.size();
    }
    
    int countPoints() {
    	return fPoints.size();
    }
    
    public final List<PointF> fPointFs() {
    	return fPoints;
    }
    
    public final List<Verb> fVerbs() {
    	return fVerbs;
    }
    
    private int growForVerb(Verb verb) {
    	int pCnt = 0;
    	switch (verb) {
    	case kMove_Verb:
    	case kLine_Verb:
    		pCnt = 1;
    		break;
    	case kQuad_Verb:
            pCnt = 2;
            break;
        case kConic_Verb:
            pCnt = 2;
            break;
        case kCubic_Verb:
            pCnt = 3;
            break;
        case kClose_Verb:
            pCnt = 0;
            break;
		default:
			break;
    	}
    	int ret = countPoints();
    	for (int i = 0; i < pCnt; i ++) {
    		fPoints.add(new PointF());
    	}
    	fVerbs.add(verb);
    	return ret;
    }
    
    public enum Verb {
        kMove_Verb,     //!< iter.next returns 1 point
        kLine_Verb,     //!< iter.next returns 2 points
        kQuad_Verb,     //!< iter.next returns 3 points
        kConic_Verb,    //!< iter.next returns 3 points + iter.conicWeight()
        kCubic_Verb,    //!< iter.next returns 4 points
        kClose_Verb,    //!< iter.next returns 1 point (contour's moveTo pt)
        kDone_Verb,     //!< iter.next returns 0 points
    };
}
