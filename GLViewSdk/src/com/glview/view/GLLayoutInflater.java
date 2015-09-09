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

package com.glview.view;

import java.util.WeakHashMap;

import android.content.Context;
import android.util.AttributeSet;

class GLLayoutInflater extends LayoutInflater {
	
	static WeakHashMap<Context, GLLayoutInflater> sLayoutInflaters = new WeakHashMap<Context, GLLayoutInflater>();
	
    private static final String[] sClassPrefixList = {
//        "android.widget.",
//        "android.webkit."
    	"com.glview.widget."
    };
    
    static GLLayoutInflater instance(Context context) {
    	GLLayoutInflater instance = sLayoutInflaters.get(context);
    	if (instance == null) {
    		instance = new GLLayoutInflater(context);
    		sLayoutInflaters.put(context, instance);
    	}
    	return instance;
    }
    
    static void removeInstance(Context context) {
    	sLayoutInflaters.remove(context);
    }
    
    private GLLayoutInflater(Context context) {
        super(context);
    }
    
    private GLLayoutInflater(LayoutInflater original, Context newContext) {
        super(original, newContext);
    }
    
    /** Override onCreateView to instantiate names that correspond to the
        widgets known to the Widget factory. If we don't find a match,
        call through to our super class.
    */
    @Override protected View onCreateView(String name, AttributeSet attrs) throws ClassNotFoundException {
        for (String prefix : sClassPrefixList) {
            try {
            	View view = createView(name, prefix, attrs);
                if (view != null) {
                    return view;
                }
            } catch (ClassNotFoundException e) {
                // In this case we want to let the base class take a crack
                // at it.
            }
        }

        return super.onCreateView(name, attrs);
    }
    
    public LayoutInflater cloneInContext(Context newContext) {
        return new GLLayoutInflater(this, newContext);
    }
}

