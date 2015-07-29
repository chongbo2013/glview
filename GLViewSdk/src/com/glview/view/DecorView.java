package com.glview.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.SoundEffectConstants;

import com.glview.graphics.Rect;
import com.glview.widget.FrameLayout;

public class DecorView extends FrameLayout {
	
	final Rect mTempRect = new Rect(); // used in the transaction to not thrash the heap.

	public DecorView(Context context) {
		super(context);
	}

	public DecorView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public DecorView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public DecorView(Context context, AttributeSet attrs, int defStyleAttr,
			int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}
	
	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (super.dispatchKeyEvent(event)) {
			return true;
		}
		
		// Handle automatic focus changes.
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int direction = 0;
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    if (event.hasNoModifiers()) {
                        direction = View.FOCUS_LEFT;
                    }
                    break;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if (event.hasNoModifiers()) {
                        direction = View.FOCUS_RIGHT;
                    }
                    break;
                case KeyEvent.KEYCODE_DPAD_UP:
                    if (event.hasNoModifiers()) {
                        direction = View.FOCUS_UP;
                    }
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    if (event.hasNoModifiers()) {
                        direction = View.FOCUS_DOWN;
                    }
                    break;
                case KeyEvent.KEYCODE_TAB:
                    if (event.hasNoModifiers()) {
                        direction = View.FOCUS_FORWARD;
                    } else if (event.hasModifiers(KeyEvent.META_SHIFT_ON)) {
                        direction = View.FOCUS_BACKWARD;
                    }
                    break;
            }
            if (direction != 0) {
                View focused = findFocus();
                if (focused != null) {
                    View v = focused.focusSearch(direction);
                    if (v != null && v != focused) {
                        // do the math the get the interesting rect
                        // of previous focused into the coord system of
                        // newly focused view
                        focused.getFocusedRect(mTempRect);
                        offsetDescendantRectToMyCoords(
                                    focused, mTempRect);
                        offsetRectIntoDescendantCoords(
                                    v, mTempRect);
                            
                        if (v.requestFocus(direction, mTempRect)) {
                            playSoundEffect(SoundEffectConstants
                                    .getContantForFocusDirection(direction));
                            return true;
                        }
                    }

                    // Give the focused view a last chance to handle the dpad key.
                    if (dispatchUnhandledMove(focused, direction)) {
                        return true;
                    }
                } else {
                    // find the best view to give focus to in this non-touch-mode with no-focus
                    View v = focusSearch(null, direction);
                    if (v != null && v.requestFocus(direction)) {
                        return true;
                    }
                }
            }
        }
        
        return false;
	}

}
