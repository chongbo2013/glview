package com.rendernode.test.demos;

import android.os.Bundle;

import com.glview.support.v7.widget.LinearLayoutManager;
import com.glview.support.v7.widget.RecyclerView;
import com.glview.support.v7.widget.RecyclerView.OnScrollListener;
import com.glview.transition.Slide;
import com.glview.transition.TransitionManager;
import com.glview.view.Gravity;
import com.glview.view.View;
import com.glview.view.ViewGroup;
import com.glview.widget.Button;
import com.rendernode.test.R;
import com.rendernode.test.adapter.RecyclerViewAdapter;
import com.rendernode.test.adapter.RecyclerViewAdapter.OnItemClickListener;

public class RecyclerViewActivity extends BaseActivity {
	
	RecyclerView mRecyclerView;
	
	RecyclerViewAdapter mAdapter;
	LinearLayoutManager mLayoutManager;
	
	Button mDelete;
	boolean shown = false;
	ViewGroup mRoot;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setGLContentView(R.layout.activity_recycler_view);
	}
	
	@Override
	public void onAttached(View content) {
		super.onAttached(content);
		
		mRoot = (ViewGroup) content.findViewById(R.id.root);
		mRecyclerView = (RecyclerView) content.findViewById(R.id.recycler_view);
		mDelete = (Button) content.findViewById(R.id.delete);
		mAdapter = new RecyclerViewAdapter();
		mRecyclerView.setAdapter(mAdapter);
		mLayoutManager = new LinearLayoutManager(this);
		mRecyclerView.setLayoutManager(mLayoutManager);
		mAdapter.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(View v, int position) {
				mAdapter.removeItem(position);
			}
		});
		mRecyclerView.setOnScrollListener(new OnScrollListener() {
			@Override
			public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
				super.onScrolled(recyclerView, dx, dy);
				if (dy > 0) {
					if (!shown) {
						TransitionManager.beginDelayedTransition(mRoot);
						mDelete.setVisibility(View.VISIBLE);
						shown = true;
					}
				} else if (shown) {
					TransitionManager.beginDelayedTransition(mRoot);
					mDelete.setVisibility(View.INVISIBLE);
					shown = false;
				}
			}
		});
	}
	
	public void click(View v) {
		switch (v.getId()) {
		case R.id.add: {
			int pos = mLayoutManager.findFirstCompletelyVisibleItemPosition();
			mAdapter.addItem(pos);
			break;
		}
		case R.id.delete: {
			int pos = mLayoutManager.findFirstCompletelyVisibleItemPosition();
			mAdapter.removeItem(pos);
			break;
		}
		default:
			break;
		}
	}
}
