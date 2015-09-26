package com.glview.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;

import com.glview.R;
import com.glview.content.GLContext;
import com.glview.graphics.Bitmap;
import com.glview.graphics.drawable.BitmapDrawable;
import com.glview.graphics.drawable.Drawable;
import com.glview.hwui.GLCanvas;
import com.glview.view.View;

public class ImageView extends View {
	
	private final static String TAG = "ImageView";
	
	protected RectF mImageDrawRect; //临时方案，最好通过控制matrix来进行操作
	
	private int mResource = 0;
	Drawable mDrawable;
	
	int mImageResource;
	boolean mSetImageBitmap = true;
	boolean mSetPreloadTexture;
	
	private Matrix mMatrix;
	
	private int[] mState = null;
    private boolean mMergeState = false;
    private int mLevel = 0;
    private int mDrawableWidth;
    private int mDrawableHeight;
	private Matrix mDrawMatrix = null;
	
	// Avoid allocations...
    private RectF mTempSrc = new RectF();
    private RectF mTempDst = new RectF();
	
    private ScaleType mScaleType = ScaleType.HORIZONTAL_CENTER;
    
    private boolean mAdjustViewBounds = false;

    private int mMaxWidth = Integer.MAX_VALUE;
    private int mMaxHeight = Integer.MAX_VALUE;
    
    private boolean mHaveFrame = false;
    
    public final static int CREATE_IMAGE = 2;
	
    // AdjustViewBounds behavior will be in compatibility mode for older apps.
    private boolean mAdjustViewBoundsCompat = false;
    
    private static final ScaleType[] sScaleTypeArray = {
        ScaleType.MATRIX,
        ScaleType.FIT_XY,
        ScaleType.FIT_START,
        ScaleType.FIT_CENTER,
        ScaleType.FIT_END,
        ScaleType.CENTER,
        ScaleType.CENTER_CROP,
        ScaleType.CENTER_INSIDE,
        ScaleType.HORIZONTAL_CENTER
    };
    
    public ImageView(Context context) {
        super(context);
        initImageView();
    }

    public ImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        
        initImageView();
        
        final TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.ImageView, defStyleAttr, defStyleRes);

        Drawable d = GLContext.get().getResources().getDrawable(a.getResourceId(R.styleable.ImageView_src, 0));//a.getDrawable(R.styleable.ImageView_src);
        
        if (d != null) {
            setImageDrawable(d);
        }
        
        setAdjustViewBounds(a.getBoolean(R.styleable.ImageView_adjustViewBounds, false));
        
        setMaxWidth(a.getDimensionPixelSize(
        		R.styleable.ImageView_maxWidth, Integer.MAX_VALUE));
        
        setMaxHeight(a.getDimensionPixelSize(
        		R.styleable.ImageView_maxHeight, Integer.MAX_VALUE));
        
        int index = a.getInt(R.styleable.ImageView_scaleType, -1);
        if (index >= 0) {
            setScaleType(sScaleTypeArray[index]);
        }
        
        a.recycle();
        
        //need inflate syntax/reader for matrix
    }
    
    private void initImageView() {
        mMatrix     = new Matrix();
        mScaleType  = ScaleType.HORIZONTAL_CENTER;
        mAdjustViewBoundsCompat = mContext.getApplicationInfo().targetSdkVersion <=
                Build.VERSION_CODES.JELLY_BEAN_MR1;
        mImageDrawRect = new RectF();
    }
    
    /**
     * True when ImageView is adjusting its bounds
     * to preserve the aspect ratio of its drawable
     *
     * @return whether to adjust the bounds of this view
     * to presrve the original aspect ratio of the drawable
     *
     * @see #setAdjustViewBounds(boolean)
     *
     * @attr ref android.R.styleable#ImageView_adjustViewBounds
     */
    public boolean getAdjustViewBounds() {
        return mAdjustViewBounds;
    }

    /**
     * Set this to true if you want the ImageView to adjust its bounds
     * to preserve the aspect ratio of its drawable.
     *
     * <p><strong>Note:</strong> If the application targets API level 17 or lower,
     * adjustViewBounds will allow the drawable to shrink the view bounds, but not grow
     * to fill available measured space in all cases. This is for compatibility with
     * legacy {@link android.view.View.MeasureSpec MeasureSpec} and
     * {@link android.widget.RelativeLayout RelativeLayout} behavior.</p>
     *
     * @param adjustViewBounds Whether to adjust the bounds of this view
     * to preserve the original aspect ratio of the drawable.
     * 
     * @see #getAdjustViewBounds()
     *
     * @attr ref android.R.styleable#ImageView_adjustViewBounds
     */
    public void setAdjustViewBounds(boolean adjustViewBounds) {
        mAdjustViewBounds = adjustViewBounds;
        if (adjustViewBounds) {
            setScaleType(ScaleType.FIT_CENTER);
        }
    }
    
    /**
     * The maximum width of this view.
     *
     * @return The maximum width of this view
     *
     * @see #setMaxWidth(int)
     *
     * @attr ref android.R.styleable#ImageView_maxWidth
     */
    public int getMaxWidth() {
        return mMaxWidth;
    }

    /**
     * An optional argument to supply a maximum width for this view. Only valid if
     * {@link #setAdjustViewBounds(boolean)} has been set to true. To set an image to be a maximum
     * of 100 x 100 while preserving the original aspect ratio, do the following: 1) set
     * adjustViewBounds to true 2) set maxWidth and maxHeight to 100 3) set the height and width
     * layout params to WRAP_CONTENT.
     * 
     * <p>
     * Note that this view could be still smaller than 100 x 100 using this approach if the original
     * image is small. To set an image to a fixed size, specify that size in the layout params and
     * then use {@link #setScaleType(android.widget.ImageView.ScaleType)} to determine how to fit
     * the image within the bounds.
     * </p>
     * 
     * @param maxWidth maximum width for this view
     *
     * @see #getMaxWidth()
     *
     */
    public void setMaxWidth(int maxWidth) {
        mMaxWidth = maxWidth;
    }

    /**
     * The maximum height of this view.
     *
     * @return The maximum height of this view
     *
     * @see #setMaxHeight(int)
     *
     */
    public int getMaxHeight() {
        return mMaxHeight;
    }

    /**
     * An optional argument to supply a maximum height for this view. Only valid if
     * {@link #setAdjustViewBounds(boolean)} has been set to true. To set an image to be a
     * maximum of 100 x 100 while preserving the original aspect ratio, do the following: 1) set
     * adjustViewBounds to true 2) set maxWidth and maxHeight to 100 3) set the height and width
     * layout params to WRAP_CONTENT.
     * 
     * <p>
     * Note that this view could be still smaller than 100 x 100 using this approach if the original
     * image is small. To set an image to a fixed size, specify that size in the layout params and
     * then use {@link #setScaleType(android.widget.ImageView.ScaleType)} to determine how to fit
     * the image within the bounds.
     * </p>
     * 
     * @param maxHeight maximum height for this view
     *
     * @see #getMaxHeight()
     *
     */
    public void setMaxHeight(int maxHeight) {
        mMaxHeight = maxHeight;
    }
    
    private static final Matrix.ScaleToFit[] sS2FArray = {
        Matrix.ScaleToFit.FILL,
        Matrix.ScaleToFit.START,
        Matrix.ScaleToFit.CENTER,
        Matrix.ScaleToFit.END
    };

    private static Matrix.ScaleToFit scaleTypeToScaleToFit(ScaleType st)  {
        // ScaleToFit enum to their corresponding Matrix.ScaleToFit values
        return sS2FArray[st.nativeInt - 1];
    } 
    
    
	@Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		resolveUri();
		
        int w;
        int h;
        
        // Desired aspect ratio of the view's contents (not including padding)
        float desiredAspect = 0.0f;
        
        // We are allowed to change the view's width
        boolean resizeWidth = false;
        
        // We are allowed to change the view's height
        boolean resizeHeight = false;
        
        final int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);

        if (mDrawable == null) {
            // If no texture, its intrinsic size is 0.
        	mDrawableWidth = -1;
        	mDrawableHeight = -1;
            w = h = 0;
        } else {
    		w = mDrawableWidth;
    		h = mDrawableHeight;
        	
  
            if (w <= 0) w = 1;
            if (h <= 0) h = 1;
            
         // We are supposed to adjust view bounds to match the aspect
            // ratio of our drawable. See if that is possible.
            if (mAdjustViewBounds) {
                resizeWidth = widthSpecMode != MeasureSpec.EXACTLY;
                resizeHeight = heightSpecMode != MeasureSpec.EXACTLY;
                
                desiredAspect = (float) w / (float) h;
            }
        }
        
        int pleft = mPaddingLeft;
        int pright = mPaddingRight;
        int ptop = mPaddingTop;
        int pbottom = mPaddingBottom;

        int widthSize;
        int heightSize;
        
        if (resizeWidth || resizeHeight) {
            /* If we get here, it means we want to resize to match the
                drawables aspect ratio, and we have the freedom to change at
                least one dimension. 
            */

            // Get the max possible width given our constraints
            widthSize = resolveAdjustedSize(w + pleft + pright, mMaxWidth, widthMeasureSpec);

            // Get the max possible height given our constraints
            heightSize = resolveAdjustedSize(h + ptop + pbottom, mMaxHeight, heightMeasureSpec);

            if (desiredAspect != 0.0f) {
                // See what our actual aspect ratio is
                float actualAspect = (float)(widthSize - pleft - pright) /
                                        (heightSize - ptop - pbottom);
                
                if (Math.abs(actualAspect - desiredAspect) > 0.0000001) {
                    
                    boolean done = false;
                    
                    // Try adjusting width to be proportional to height
                    if (resizeWidth) {
                        int newWidth = (int)(desiredAspect * (heightSize - ptop - pbottom)) +
                                pleft + pright;

                        // Allow the width to outgrow its original estimate if height is fixed.
                        if (!resizeHeight && !mAdjustViewBoundsCompat) {
                            widthSize = resolveAdjustedSize(newWidth, mMaxWidth, widthMeasureSpec);
                        }

                        if (newWidth <= widthSize) {
                            widthSize = newWidth;
                            done = true;
                        } 
                    }
                    
                    // Try adjusting height to be proportional to width
                    if (!done && resizeHeight) {
                        int newHeight = (int)((widthSize - pleft - pright) / desiredAspect) +
                                ptop + pbottom;

                        // Allow the height to outgrow its original estimate if width is fixed.
                        if (!resizeWidth && !mAdjustViewBoundsCompat) {
                            heightSize = resolveAdjustedSize(newHeight, mMaxHeight,
                                    heightMeasureSpec);
                        }

                        if (newHeight <= heightSize) {
                            heightSize = newHeight;
                        }
                    }
                }
            }
        } else {
            /* We are either don't want to preserve the drawables aspect ratio,
               or we are not allowed to change view dimensions. Just measure in
               the normal way.
            */
            w += pleft + pright;
            h += ptop + pbottom;
                
            w = Math.max(w, getSuggestedMinimumWidth());
            h = Math.max(h, getSuggestedMinimumHeight());

            widthSize = resolveSizeAndState(w, widthMeasureSpec, 0);
            heightSize = resolveSizeAndState(h, heightMeasureSpec, 0);
        }

        setMeasuredDimension(widthSize, heightSize);
    }
	
	private int resolveAdjustedSize(int desiredSize, int maxSize,
			int measureSpec) {
		int result = desiredSize;
		int specMode = MeasureSpec.getMode(measureSpec);
		int specSize = MeasureSpec.getSize(measureSpec);
		switch (specMode) {
		case MeasureSpec.UNSPECIFIED:
			/*
			 * Parent says we can be as big as we want. Just don't be larger
			 * than max size imposed on ourselves.
			 */
			result = Math.min(desiredSize, maxSize);
			break;
		case MeasureSpec.AT_MOST:
			// Parent says we can be as big as we want, up to specSize.
			// Don't be larger than specSize, and don't be larger than
			// the max size imposed on ourselves.
			result = Math.min(Math.min(desiredSize, specSize), maxSize);
			break;
		case MeasureSpec.EXACTLY:
			// No choice. Do what we are told.
			result = specSize;
			break;
		}
		return result;
	}
	
	@Override
	protected boolean setFrame(int left, int top, int right, int bottom) {
        boolean changed = super.setFrame(left, top, right, bottom);
        mHaveFrame = true;
        configureBounds();
        return changed;
	}
	
	//TODO:继续完善下面的代码，目前以mImageDrawRect作为显示区域，后面可以采用更合理的方案
    private void configureBounds() {
        if (mDrawable == null || !mHaveFrame) {
            return;
        }

        int dwidth  = mDrawableWidth;
        int dheight = mDrawableHeight;

        int vwidth = getWidth() - mPaddingLeft - mPaddingRight;
        int vheight = getHeight() - mPaddingTop - mPaddingBottom;

        boolean fits = (dwidth < 0 || vwidth == dwidth) &&
                       (dheight < 0 || vheight == dheight);
        
        if (dwidth <= 0 || dheight <= 0 || ScaleType.FIT_XY == mScaleType) {
            /* If the drawable has no intrinsic size, or we're told to
                scaletofit, then we just fill our entire view.
            */
//        	mDrawable.setBounds(0, 0, vwidth, vheight);
            mDrawMatrix = null;
        	mImageDrawRect.set(0, 0, vwidth, vheight);
        } else {
            // We need to do the scaling ourself, so have the drawable
            // use its native size.
//            mDrawable.setBounds(0, 0, dwidth, dheight);
            mImageDrawRect.set(0, 0, dwidth, dheight);

            if (ScaleType.MATRIX == mScaleType) {
                // Use the specified matrix as-is.
                if (mMatrix.isIdentity()) {
                    mDrawMatrix = null;
                } else {
                    mDrawMatrix = mMatrix;
                }
            } else if (fits) {
                // The bitmap fits exactly, no transform needed.
                mDrawMatrix = null;
            } else if(ScaleType.HORIZONTAL_CENTER == mScaleType){
            	mDrawMatrix = null;
            	mImageDrawRect.set((int) ((vwidth - dwidth) * 0.5f + 0.5f), 0,
            			(int) ((vwidth + dwidth) * 0.5f + 0.5f), dheight);
            } else if (ScaleType.CENTER == mScaleType) {
                // Center bitmap in view, no scaling.
                mDrawMatrix = mMatrix;
                mDrawMatrix.setTranslate((int) ((vwidth - dwidth) * 0.5f + 0.5f),
                                         (int) ((vheight - dheight) * 0.5f + 0.5f));
            } else if (ScaleType.CENTER_CROP == mScaleType) {
                mDrawMatrix = mMatrix;

                float scale;
                float dx = 0, dy = 0;

                if (dwidth * vheight > vwidth * dheight) {
                    scale = (float) vheight / (float) dheight; 
                    dx = (vwidth - dwidth * scale) * 0.5f;
                } else {
                    scale = (float) vwidth / (float) dwidth;
                    dy = (vheight - dheight * scale) * 0.5f;
                }
                mDrawMatrix.setScale(scale, scale);
                mDrawMatrix.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
            } else if (ScaleType.CENTER_INSIDE == mScaleType) {
                mDrawMatrix = mMatrix;
                float scale;
                float dx;
                float dy;
                
                if (dwidth <= vwidth && dheight <= vheight) {
                    scale = 1.0f;
                } else {
                    scale = Math.min((float) vwidth / (float) dwidth,
                            (float) vheight / (float) dheight);
                }
                
                dx = (int) ((vwidth - dwidth * scale) * 0.5f + 0.5f);
                dy = (int) ((vheight - dheight * scale) * 0.5f + 0.5f);

                mDrawMatrix.setScale(scale, scale);
                mDrawMatrix.postTranslate(dx, dy);
            } else {
                // Generate the required transform.
                mTempSrc.set(0, 0, dwidth, dheight);
                mTempDst.set(0, 0, vwidth, vheight);
                
                mDrawMatrix = mMatrix;
                mDrawMatrix.setRectToRect(mTempSrc, mTempDst, scaleTypeToScaleToFit(mScaleType));
            }
        }
        if (mDrawMatrix != null) {
        	mDrawMatrix.mapRect(mImageDrawRect);
        }
        mDrawable.setBounds((int) mImageDrawRect.left, (int) mImageDrawRect.top, (int) mImageDrawRect.right, (int) mImageDrawRect.bottom);
    }
	
    /**
     * Sets a drawable as the content of this ImageView.
     *
     * <p class="note">This does Bitmap reading and decoding on the UI
     * thread, which can cause a latency hiccup.  If that's a concern,
     * consider using {@link #setImageDrawable(android.graphics.drawable.Drawable)} or
     * {@link #setImageBitmap(android.graphics.Bitmap)} and
     * {@link android.graphics.BitmapFactory} instead.</p>
     *
     * @param resId the resource identifier of the the drawable
     *
     */
    public void setImageResource(int resId) {
    	if (mResource != resId) {
            final int oldWidth = mDrawableWidth;
            final int oldHeight = mDrawableHeight;

            updateDrawable(null);
            mResource = resId;

            resolveUri();
            
            if (oldWidth != mDrawableWidth || oldHeight != mDrawableHeight) {
                requestLayout();
            }
            invalidate();
        }
    }

    private void resolveUri() {
        if (mDrawable != null) {
            return;
        }

        Drawable d = null;

        if (mResource != 0) {
            try {
                d = getResources().getDrawable(mResource);
            } catch (Exception e) {
                Log.w("ImageView", "Unable to find resource: " + mResource, e);
            }
        }
        updateDrawable(d);
    }
    
    /**
     * Sets a Bitmap as the content of this ImageView.
     * 
     * @param bm The bitmap to set
     */
    public void setImageBitmap(Bitmap bm) {
        // if this is used frequently, may handle bitmaps explicitly
        // to reduce the intermediate drawable object
        setImageDrawable(new BitmapDrawable(mContext.getResources(), bm));
    }
    
    public void setImageState(int[] state, boolean merge) {
        mState = state;
        mMergeState = merge;
        if (mDrawable != null) {
            refreshDrawableState();
            resizeFromDrawable();
        }
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        resizeFromDrawable();
    }

    /**
     * Sets the image level, when it is constructed from a 
     * {@link android.graphics.drawable.LevelListDrawable}.
     *
     * @param level The new level for the image.
     */
    public void setImageLevel(int level) {
        mLevel = level;
        if (mDrawable != null) {
            mDrawable.setLevel(level);
            resizeFromDrawable();
        }
    }
    
    /**
     * Sets a drawable as the content of this ImageView.
     * 
     * @param drawable The drawable to set
     */
    public void setImageDrawable(Drawable drawable) {
        if (mDrawable != drawable) {
            mResource = 0;

            final int oldWidth = mDrawableWidth;
            final int oldHeight = mDrawableHeight;

            updateDrawable(drawable);

            if (oldWidth != mDrawableWidth || oldHeight != mDrawableHeight) {
                requestLayout();
            }
            invalidate();
        }
    }
    
    @Override
    public int[] onCreateDrawableState(int extraSpace) {
        if (mState == null) {
            return super.onCreateDrawableState(extraSpace);
        } else if (!mMergeState) {
            return mState;
        } else {
            return mergeDrawableStates(
                    super.onCreateDrawableState(extraSpace + mState.length), mState);
        }
    }
    
    private void updateDrawable(Drawable d) {
        if (mDrawable != null) {
            mDrawable.setCallback(null);
            unscheduleDrawable(mDrawable);
        }

        mDrawable = d;

        if (d != null) {
            d.setCallback(this);
            d.setLayoutDirection(getLayoutDirection());
            if (d.isStateful()) {
                d.setState(getDrawableState());
            }
            d.setVisible(getVisibility() == VISIBLE, true);
            d.setLevel(mLevel);
            mDrawableWidth = d.getIntrinsicWidth();
            mDrawableHeight = d.getIntrinsicHeight();
//            applyImageTint();
//            applyColorMod();
            configureBounds();
        } else {
            mDrawableWidth = mDrawableHeight = -1;
        }
    }
    
    private void resizeFromDrawable() {
        Drawable d = mDrawable;
        if (d != null) {
            int w = d.getIntrinsicWidth();
            if (w < 0) w = mDrawableWidth;
            int h = d.getIntrinsicHeight();
            if (h < 0) h = mDrawableHeight;
            if (w != mDrawableWidth || h != mDrawableHeight) {
                mDrawableWidth = w;
                mDrawableHeight = h;
                requestLayout();
            }
        }
    }
    
    @Override
    protected void onDraw(GLCanvas canvas) {
    	super.onDraw(canvas);

        if (mDrawable == null) {
            return; // couldn't resolve the URI
        }

        if (mDrawableWidth == 0 || mDrawableHeight == 0) {
            return;     // nothing to draw (empty bounds)
        }

        if (mPaddingTop == 0 && mPaddingLeft == 0) {
            mDrawable.draw(canvas);
        } else {
            canvas.translate(mPaddingLeft, mPaddingTop);
            mDrawable.draw(canvas);
            canvas.translate(- mPaddingLeft, - mPaddingTop);
        }	
    }
    
    /**
     * Options for scaling the bounds of an image to the bounds of this view.
     */
    public enum ScaleType {
        /**
         * Scale using the image matrix when drawing. The image matrix can be set using
         * {@link ImageView#setImageMatrix(Matrix)}. From XML, use this syntax:
         * <code>android:scaleType="matrix"</code>.
         */
        MATRIX      (0),
        /**
         * Scale the image using {@link Matrix.ScaleToFit#FILL}.
         * From XML, use this syntax: <code>android:scaleType="fitXY"</code>.
         */
        FIT_XY      (1),
        /**
         * Scale the image using {@link Matrix.ScaleToFit#START}.
         * From XML, use this syntax: <code>android:scaleType="fitStart"</code>.
         */
        FIT_START   (2),
        /**
         * Scale the image using {@link Matrix.ScaleToFit#CENTER}.
         * From XML, use this syntax:
         * <code>android:scaleType="fitCenter"</code>.
         */
        FIT_CENTER  (3),
        /**
         * Scale the image using {@link Matrix.ScaleToFit#END}.
         * From XML, use this syntax: <code>android:scaleType="fitEnd"</code>.
         */
        FIT_END     (4),
        /**
         * Center the image in the view, but perform no scaling.
         * From XML, use this syntax: <code>android:scaleType="center"</code>.
         */
        CENTER      (5),
        /**
         * Scale the image uniformly (maintain the image's aspect ratio) so
         * that both dimensions (width and height) of the image will be equal
         * to or larger than the corresponding dimension of the view
         * (minus padding). The image is then centered in the view.
         * From XML, use this syntax: <code>android:scaleType="centerCrop"</code>.
         */
        CENTER_CROP (6),
        /**
         * Scale the image uniformly (maintain the image's aspect ratio) so
         * that both dimensions (width and height) of the image will be equal
         * to or less than the corresponding dimension of the view
         * (minus padding). The image is then centered in the view.
         * From XML, use this syntax: <code>android:scaleType="centerInside"</code>.
         */
        CENTER_INSIDE (7),
        
        HORIZONTAL_CENTER (8);
        
        ScaleType(int ni) {
            nativeInt = ni;
        }
        final int nativeInt;
    }
    
    /**
     * Controls how the image should be resized or moved to match the size
     * of this ImageView.
     * 
     * @param scaleType The desired scaling mode.
     * 
     */
    public void setScaleType(ScaleType scaleType) {
        if (scaleType == null) {
            throw new NullPointerException();
        }

        if (mScaleType != scaleType) {
            mScaleType = scaleType;           
            
        }
    }
    
    public ScaleType getScaleType(){
    	return mScaleType;
    }
    
    public void setImageMatrix(Matrix matrix) {
        // collaps null and identity to just null
        if (matrix != null && matrix.isIdentity()) {
            matrix = null;
        }
        
        // don't invalidate unless we're actually changing our matrix
        if (matrix == null && !mMatrix.isIdentity() ||
                matrix != null && !mMatrix.equals(matrix)) {
            mMatrix.set(matrix);
            configureBounds();
            invalidate();
        }
    }
    
}
