package com.glview.hwui;

import com.glview.graphics.Rect;
import com.glview.graphics.Region;
import com.glview.pool.Pool;
import com.glview.pool.Poolable;
import com.glview.util.MatrixUtil;

public class Snapshot implements Poolable {
	
	static Pool<Snapshot> sPool = new Pool<Snapshot>(false);
	
	/**
     * Indicates that the clip region was modified. When this
     * snapshot is restored so must the clip.
     */
	public final static int kFlagClipSet = 0x1;
    /**
     * Indicates that this snapshot was created when saving
     * a new layer.
     */
	public final static int kFlagIsLayer = 0x2;
    /**
     * Indicates that this snapshot is a special type of layer
     * backed by an FBO. This flag only makes sense when the
     * flag kFlagIsLayer is also set.
     *
     * Viewport has been modified to fit the new Fbo, and must be
     * restored when this snapshot is restored.
     */
	public final static int kFlagIsFboLayer = 0x4;
    /**
     * Indicates that this snapshot or an ancestor snapshot is
     * an FBO layer.
     */
	public final static int kFlagFboTarget = 0x8;
	
	Snapshot previous;
	int flags;
	
	public static Snapshot obtain(Snapshot previous, int saveFlags) {
		Snapshot snapshot = (Snapshot) sPool.poll(Snapshot.class);
		snapshot.save(previous, saveFlags);
		return snapshot;
	}

	private void save(Snapshot s, int saveFlags) {
		flags = 0;
		
		this.previous = s;
		alpha = previous.alpha;
		if ((saveFlags & GLCanvas.SAVE_FLAG_MATRIX) != 0) {
			System.arraycopy(s.transform, 0, mTransformRoot, 0, mTransformRoot.length);
			transform = mTransformRoot;
		} else {
			transform = s.transform;
		}
		
		if ((saveFlags & GLCanvas.SAVE_FLAG_CLIP) != 0) {
			mClipRectRoot.set(s.clipRect);
			clipRect = mClipRectRoot;
		} else {
			clipRect = s.clipRect;
		}
		
		if ((s.flags & kFlagFboTarget) != 0) {
	        flags |= kFlagFboTarget;
	    }
	}
	
	public float[] getTransformation() {
		return transform;
	}
	
	float mTransformRoot[] = new float[48];
	Rect mClipRectRoot = new Rect();
	
	float alpha = 1;
    float transform[] = mTransformRoot;
    Rect clipRect = mClipRectRoot;
    
    static ThreadLocal<Rect> sThreadLocalRect = new ThreadLocal<Rect>() {
    	protected Rect initialValue() {
    		return new Rect();
    	};
    };
    
    boolean clip(float left, float top, float right, float bottom, Region.Op op) {
    	Rect r = sThreadLocalRect.get();
    	r.set((int) left, (int) top, (int) right, (int) bottom);
        MatrixUtil.mapRect(transform, r);
        return clipTransformed(r, op);
    }
    
    boolean clipTransformed(Rect r, Region.Op op) {
        boolean clipped = false;

        switch (op) {
            case INTERSECT: {
                    clipped = clipRect.intersect(r);
                    if (!clipped) {
                        clipRect.setEmpty();
                        clipped = true;
                }
                break;
            }
            case REPLACE: {
                setClip(r.left, r.top, r.right, r.bottom);
                clipped = true;
                break;
            }
            default: {
                break;
            }
        }

        if (clipped) {
            flags |= kFlagClipSet;
        }

        return clipped;
    }

    void setClip(int left, int top, int right, int bottom) {
    	clipRect.set(left, top, right, bottom);
        flags |= kFlagClipSet;
    }
    
    public void recycle() {
    	previous = null;
    	sPool.push(this);
    }
}
