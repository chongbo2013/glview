package com.rendernode.test;

import android.os.Bundle;
import android.view.View;

import com.glview.app.GLActivity;
import com.glview.view.ViewGroup;
import com.glview.view.LayoutInflater;

public class Main1Activity extends GLActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        ViewGroup vg = (ViewGroup) LayoutInflater.from(this).inflate(R.layout.gl_layout, null);
        
        setGLContentView(vg);
        
        vg.getChildAt(1).setTranslationZ(-1);
    }

	@Override
	public void onAttached(com.glview.view.View content) {
	}
}
