package com.rendernode.test.demos.transition;

import android.os.Bundle;

import com.glview.transition.Scene;
import com.glview.transition.TransitionInflater;
import com.glview.transition.TransitionManager;
import com.glview.view.View;
import com.glview.view.ViewGroup;
import com.rendernode.test.R;
import com.rendernode.test.demos.BaseActivity;

public class SceneTransitionActivity extends BaseActivity {

	ViewGroup mLayout;
	
	private TransitionManager mTransitionManager;
	private Scene mScene1;
	private Scene mScene2;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setGLContentView(R.layout.activity_scene_transition);
	}
	
	@Override
	public void onAttached(View content) {
		super.onAttached(content);
		mLayout = (ViewGroup) content.findViewById(R.id.container);
		mTransitionManager = TransitionInflater.from(this).inflateTransitionManager(R.transition.transition_manager, mLayout);
		mScene1 = Scene.getSceneForLayout(mLayout, R.layout.layout_transition_scene1, this);
		mScene2 = Scene.getSceneForLayout(mLayout,  R.layout.layout_transition_scene2, this);
	}
	
	public void goToScene1(View v) {
		mTransitionManager.transitionTo(mScene1);
	}
	
	public void goToScene2(View v) {
		mTransitionManager.transitionTo(mScene2);
	}
}
