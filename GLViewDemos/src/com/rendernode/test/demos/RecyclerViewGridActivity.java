package com.rendernode.test.demos;

import android.os.Bundle;

import com.glview.support.v7.widget.GridLayoutManager;
import com.glview.support.v7.widget.RecyclerView;
import com.glview.support.v7.widget.RecyclerView.OnScrollListener;
import com.glview.view.View;
import com.glview.view.ViewGroup;
import com.rendernode.test.R;
import com.rendernode.test.adapter.RecyclerViewAdapter;
import com.rendernode.test.adapter.RecyclerViewAdapter.OnItemClickListener;

public class RecyclerViewGridActivity extends BaseActivity {

	RecyclerView mRecyclerView;
	
	RecyclerViewAdapter mAdapter;
	GridLayoutManager mLayoutManager;
	
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
		mAdapter = new RecyclerViewAdapter();
		mRecyclerView.setAdapter(mAdapter);
		mLayoutManager = new GridLayoutManager(this, 3);
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
		default:
			break;
		}
	}
}
