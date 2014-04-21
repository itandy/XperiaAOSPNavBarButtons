package com.gzplanet.xposed.xperianavbarbuttons;

import java.util.ArrayList;
import java.util.Arrays;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.commonsware.cwac.tlv.TouchListView;

public class ReorderActivity extends ListActivity {
	private IconicAdapter mAdapter = null;
	private ArrayList<String> mItems;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.reorder);
		
		Intent intent = getIntent();
		String[] items = intent.getStringExtra("order_list").split(",");
		mItems = new ArrayList<String>(Arrays.asList(items));

		TouchListView tlv = (TouchListView) getListView();
		mAdapter = new IconicAdapter();
		setListAdapter(mAdapter);

		tlv.setDropListener(onDrop);
		tlv.setRemoveListener(onRemove);
	}

	@Override
	public void onBackPressed() {
		StringBuilder list = new StringBuilder();

		for (int i = 0; i < mItems.size(); i++) {
			list.append(mItems.get(i));
			if (i != mItems.size() - 1)
				list.append(",");
		}
		
		Intent intent = new Intent();
		intent.putExtra("order_list", list.toString());
		setResult(RESULT_OK, intent);
		finish();
	}

	private TouchListView.DropListener onDrop = new TouchListView.DropListener() {
		@Override
		public void drop(int from, int to) {
			String item = mAdapter.getItem(from);

			mAdapter.remove(item);
			mAdapter.insert(item, to);
		}
	};

	private TouchListView.RemoveListener onRemove = new TouchListView.RemoveListener() {
		@Override
		public void remove(int which) {
			mAdapter.remove(mAdapter.getItem(which));
		}
	};

	class IconicAdapter extends ArrayAdapter<String> {
		IconicAdapter() {
			super(ReorderActivity.this, R.layout.sort_list_row, mItems);
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;

			if (row == null) {
				LayoutInflater inflater = getLayoutInflater();

				row = inflater.inflate(R.layout.sort_list_row, parent, false);
			}

			TextView label = (TextView) row.findViewById(R.id.label);

			label.setText(mItems.get(position));

			return (row);
		}
	}

}
