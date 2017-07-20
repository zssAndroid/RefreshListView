package com.sen.refreshlistview;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class MainActivity extends Activity {

	private RefreshListView listview;
	private ArrayList<String> arrayList;
	private MyAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		listview = (RefreshListView) findViewById(R.id.listview);
		arrayList = new ArrayList<String>();
		for(int i=0;i<20;i++){
			arrayList.add("数据：" + i);
		}
		adapter = new MyAdapter();
		listview.setAdapter(adapter);
		// 监听下拉刷新状态
		listview.setOnRefreshListener(new MyListener());
	}
	class MyListener implements RefreshListView.OnRefreshListener {

		@Override
		public void onRefreshing() {
			// 处理业务
			new Thread(){
				public void run() {
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					runOnUiThread(new Runnable() {
						
						@Override
						public void run() {
//							arrayList.add(0, "我是拉出来的");
							arrayList.clear();
							for(int i=0;i<20;i++){
								arrayList.add("刷新的数据：" + i);
							}
							adapter.notifyDataSetChanged();
							// 刷新完成后，调用恢复下拉刷新控件的方法
							listview.refreshFinished();
						}
					});
				};
			}.start();
		}

		@Override
		public void onLoadingMore() {
			// 处理业务
						new Thread(){
							public void run() {
								try {
									Thread.sleep(3000);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								runOnUiThread(new Runnable() {
									
									@Override
									public void run() {
										arrayList.add("我是加载出来的");
										arrayList.add("我是加载出来的");
										adapter.notifyDataSetChanged();
										// 加载更多完成后，调用控件恢复状态的方法
										listview.loadMoreFinished();
									}
								});
							};
						}.start();
		}
		
	}
	class MyAdapter extends BaseAdapter{

		@Override
		public int getCount() {
			return arrayList.size();
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView textView = new TextView(getApplicationContext());
			textView.setTextSize(20);
			textView.setTextColor(Color.BLACK);
			textView.setText(arrayList.get(position));
			return textView;
		}
		
	}

}
