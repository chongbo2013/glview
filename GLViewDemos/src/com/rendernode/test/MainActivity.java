package com.rendernode.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class MainActivity extends ListActivity {
	
	final static String TAG = "Main";

	static Map<String, List<Map<String, Object>>> sTestActivitys = null;
	
	final static String DEFAULT_PACKAGE = "com.rendernode.test.demos";
	
	String mPackageName;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "-----load data begin");
        loadData();
        Log.d(TAG, "-----load data end");
        mPackageName = getIntent().getStringExtra("package");
        if (TextUtils.isEmpty(mPackageName)) {
        	mPackageName = DEFAULT_PACKAGE;
        }
        setTitle(mPackageName);
        if (sTestActivitys.get(mPackageName) != null) {
        	setListAdapter(new SimpleAdapter(this, sTestActivitys.get(mPackageName), android.R.layout.simple_list_item_1, new String[]{"title"}, new int[]{android.R.id.text1}));
        } else {
        	new AlertDialog.Builder(this)
        	.setTitle("Warning!")
        	.setMessage("该目录下没有可调试的activity")
        	.setPositiveButton("ok", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
			}).show();
        }
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
    	List<Map<String, Object>> list = sTestActivitys.get(mPackageName);
    	Map<String, Object> item = list.get(position);
    	Intent intent;
    	if (item.containsKey("package")) {
    		intent = new Intent(this, MainActivity.class);
    		intent.putExtra("package", mPackageName + "." + item.get("name"));
    	} else {
    		intent = new Intent();
    		intent.setClassName(this, mPackageName + "." + item.get("name"));
    	}
    	startActivity(intent);
    }
    
    void loadData() {
    	if (sTestActivitys != null) {
    		return;
    	}
    	sTestActivitys = new HashMap<String, List<Map<String,Object>>>();
    	List<String> tmp = new ArrayList<String>();
    	try {
	    	for (ActivityInfo activityInfo : getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_ACTIVITIES).activities) {
	    		String name = activityInfo.name;
	    		Log.d(TAG, "-------------------------" + name);
	    		try {
	    			if (name.startsWith(DEFAULT_PACKAGE)) {
	    				String packageName = name.substring(0, name.lastIndexOf("."));
	    				List<Map<String, Object>> packageItems = sTestActivitys.get(packageName);
	    				if (packageItems == null) {
	    					packageItems = new ArrayList<Map<String,Object>>();
	    					sTestActivitys.put(packageName, packageItems);
	    				}
	    				Map<String, Object> item = new HashMap<String, Object>();
	    				item.put("name", name.substring(name.lastIndexOf(".") + 1));
	    				item.put("title", activityInfo.loadLabel(getPackageManager()));
	    				packageItems.add(item);
	    				Collections.sort(packageItems, mComparator);
	    				while (!packageName.equals(DEFAULT_PACKAGE)) {
	    					boolean exists = tmp.contains(packageName);
	    					if (!exists) {
	    						tmp.add(packageName);
	    					}
	    					String packageN = packageName.substring(packageName.lastIndexOf(".") + 1);
	    					packageName = packageName.substring(0, packageName.lastIndexOf("."));
	    					packageItems = sTestActivitys.get(packageName);
		    				if (packageItems == null) {
		    					packageItems = new ArrayList<Map<String,Object>>();
		    					sTestActivitys.put(packageName, packageItems);
		    				}
		    				if (!exists) {
		    					item = new HashMap<String, Object>();
			    				item.put("name", packageN);
			    				item.put("title", packageN);
			    				item.put("package", "true");
			    				packageItems.add(item);
			    				Collections.sort(packageItems, mComparator);
		    				}
	    				}
	    			}
	    		} catch (Exception e) {}
	    	}
    	} catch (Exception e){}
    }
    
    Comparator<Map<String, Object>> mComparator = new Comparator<Map<String,Object>>() {
		@Override
		public int compare(Map<String, Object> lhs, Map<String, Object> rhs) {
			if (lhs.containsKey("package")) {
				return -1;
			}
			if (rhs.containsKey("package")) {
				return 1;
			}
			String name1 = (String) lhs.get("name");
			String name2 = (String) rhs.get("name");
			if (name1 != null && name2 != null) {
				return name1.compareTo(name2);
			}
			return 0;
		}
	};
}
