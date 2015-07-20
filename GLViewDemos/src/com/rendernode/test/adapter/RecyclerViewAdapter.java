package com.rendernode.test.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.glview.support.v7.widget.RecyclerView;
import com.glview.support.v7.widget.RecyclerView.Adapter;
import com.glview.view.LayoutInflater;
import com.glview.view.View;
import com.glview.view.View.OnClickListener;
import com.glview.view.ViewGroup;
import com.glview.widget.TextView;
import com.rendernode.test.R;
import com.rendernode.test.adapter.RecyclerViewAdapter.CustomViewHolder;

public class RecyclerViewAdapter extends Adapter<CustomViewHolder> {

	List<String> mData = new ArrayList<String>();
	
	public RecyclerViewAdapter() {
		for (int i = 0; i < 10; i ++) {
			mData.add("Text " + i);
		}
	}
	
	@Override
	public int getItemCount() {
		return mData.size();
	}
	
	@Override
	public CustomViewHolder onCreateViewHolder(ViewGroup parent, int arg1) {
		View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_card_view, parent, false);
		return new CustomViewHolder(v);
	}
	
	public static interface OnItemClickListener {
		public void onItemClick(View v, int position);
	}
	
	OnItemClickListener mOnItemClickListener;
	
	public void setOnItemClickListener(OnItemClickListener l) {
		mOnItemClickListener = l;
	}

	@Override
	public void onBindViewHolder(final CustomViewHolder viewHolder, int position) {
		viewHolder.mTextView.setText(mData.get(position));
		if (mOnItemClickListener != null) {
			viewHolder.itemView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mOnItemClickListener.onItemClick(v, viewHolder.getPosition());
				}
			});
		}
		
	}
	
	public void addItem(int position) {
		mData.add(position, "Text " + new Random().nextInt(1000));
		notifyItemInserted(position);
	}
	
	public void removeItem(int position) {
		mData.remove(position);
		notifyItemRemoved(position);
	}

	class CustomViewHolder extends RecyclerView.ViewHolder {
		
		TextView mTextView;

		public CustomViewHolder(View itemView) {
			super(itemView);
			mTextView = (TextView) itemView.findViewById(R.id.text);
		}
	}
}
