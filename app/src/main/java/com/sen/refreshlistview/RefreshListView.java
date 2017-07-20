package com.sen.refreshlistview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class RefreshListView extends ListView {

	private int downY;
	private View header;
	private int headerHeight;

	private static final int PULLREFRESH_STATE = 1;// 下拉刷新状态
	private static final int RELEASE_STATE = 2;// 松开刷新状态
	private static final int REFRESHING_STATE = 3;// 正在刷新状态
	private int current_state = PULLREFRESH_STATE;// 当前刷新状态
	private ProgressBar progress;
	private ImageView arrow;
	private TextView state;
	private TextView time;
	private RotateAnimation up;
	private RotateAnimation down;
	private OnRefreshListener mListener;
	private View footer;
	private int footerHeight;

	public RefreshListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		initHeader();
		initAnimation();
		initFooter();
	}

	private void initFooter() {
		footer = View.inflate(getContext(), R.layout.refresh_footer, null);
		footer.measure(0, 0);
		footerHeight = footer.getMeasuredHeight();
		footer.setPadding(0, 0, 0, -footerHeight);
		// 添加脚布局
		addFooterView(footer);
		// 监听Listview的滚动状态
		this.setOnScrollListener(new MyOnScrollListener());
	}

	private void initAnimation() {
		up = new RotateAnimation(0, -180, Animation.RELATIVE_TO_SELF, 0.5f,
				Animation.RELATIVE_TO_SELF, 0.5f);
		up.setDuration(200);
		up.setFillAfter(true);
		down = new RotateAnimation(-180, -360, Animation.RELATIVE_TO_SELF,
				0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		down.setDuration(200);
		down.setFillAfter(true);
	}

	private void initHeader() {
		header = View.inflate(getContext(), R.layout.refresh_header, null);
		progress = (ProgressBar) header.findViewById(R.id.progress);
		arrow = (ImageView) header.findViewById(R.id.arrow);
		state = (TextView) header.findViewById(R.id.state);
		time = (TextView) header.findViewById(R.id.time);

		// 隐藏头布局
		// System.out.println(header.getHeight());getHeight()只有在控件layout布局后，才能获取到
		// 主动测量控件，获取测量的宽高
		header.measure(0, 0);// 把布局中的宽高给测量出来
		// 获取测量的宽高

		headerHeight = header.getMeasuredHeight();
		header.setPadding(0, -headerHeight, 0, 0);
		// 把布局添加到Listview的头上
		this.addHeaderView(header);
	}

	// 处理触摸事件
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		// System.out.println(getFirstVisiblePosition());
		switch (ev.getAction()) {
		case MotionEvent.ACTION_DOWN:
			downY = (int) ev.getY();
			break;
		case MotionEvent.ACTION_MOVE:
			int moveY = (int) ev.getY();
			// 计算手指移动的距离
			int diffY = moveY - downY;
			// 只有当Listview中的第一个条目完全展示时，header.setPadding才有效果，才能自己处理事件
			if (getFirstVisiblePosition() != 0) {
				// 如果自己不处理事件，每次移动需要给downY重新赋值
				downY = (int) ev.getY();
				break;
			}
			// 只处理从上往下的事件
			if (diffY > 0) {
				// 计算头布局距离顶部的padding值
				int topPadding = diffY - headerHeight;
				// 根据toppadding值是否大于0 头布局是否完全展示，判断状态的切换
				if (topPadding >= 0 && current_state != RELEASE_STATE) {// 头布局完全展示，切换到松开刷新
																		// ，如果已经是松开刷新状态，就不用再切换
					current_state = RELEASE_STATE;
					System.out.println("切换到松开刷新");
					switchState();
				} else if (topPadding < 0 && current_state != PULLREFRESH_STATE) {// 头布局没有完全展示，切换到下拉刷新
					current_state = PULLREFRESH_STATE;
					System.out.println("切换到下拉刷新");
					switchState();
				}

				header.setPadding(0, topPadding, 0, 0);
				return true;// 自己处理的从上往下的触摸事件，需要消费掉
			}
			break;
		case MotionEvent.ACTION_UP:
			// 手指抬起时，根据当前的状态判断是否切换到正在刷新
			if (current_state == PULLREFRESH_STATE) {// 抬起时，是下拉刷新，头布局没有完全展示，不切换到正在刷新
				// 隐藏头布局
				header.setPadding(0, -headerHeight, 0, 0);
			} else if (current_state == RELEASE_STATE) {// 抬起时，是松开刷新，切换到正在刷新
				current_state = REFRESHING_STATE;
				// 让头布局正好完全展示
				header.setPadding(0, 0, 0, 0);
				System.out.println("切换到正在刷新");
				switchState();
				// 当处于正在刷新状态时，回调监听器的onRefreshing
				if (mListener != null) {
					mListener.onRefreshing();
				}
			}
			break;

		default:
			break;
		}
		return super.onTouchEvent(ev);
	}

	// 下拉刷新完成后，恢复状态，隐藏头布局
	public void refreshFinished() {
		header.setPadding(0, -headerHeight, 0, 0);
		state.setText("下拉刷新");
		progress.setVisibility(View.INVISIBLE);
		arrow.setVisibility(View.VISIBLE);
		current_state = PULLREFRESH_STATE;
	}

	// 切换状态时，更新界面
	private void switchState() {
		switch (current_state) {
		case PULLREFRESH_STATE:
			state.setText("下拉刷新");
			progress.setVisibility(View.INVISIBLE);
			arrow.setVisibility(View.VISIBLE);
			arrow.startAnimation(down);
			break;
		case RELEASE_STATE:
			state.setText("松开刷新");
			arrow.startAnimation(up);
			break;
		case REFRESHING_STATE:
			// 由于动画设置了setFillAfter 控件就停留在结束时的效果
			arrow.clearAnimation();
			state.setText("正在刷新");
			progress.setVisibility(View.VISIBLE);
			arrow.setVisibility(View.INVISIBLE);
			break;

		default:
			break;
		}
	}

	// 对外暴露接口
	public interface OnRefreshListener {
		// 正在刷新时，回调
		void onRefreshing();
		// 加载更多时，回调
		void onLoadingMore();
	}

	// 提供传递监听器的方法
	public void setOnRefreshListener(OnRefreshListener listener) {
		this.mListener = listener;
	}
	private boolean isLoadMore = false;// 当前加载更多脚布局是否显示
	
	// 加载更多完成后，恢复状态
	public void loadMoreFinished(){
		isLoadMore = false;
		footer.setPadding(0, 0, 0, -footerHeight);
	}
	class MyOnScrollListener implements OnScrollListener {
		// 状态发生变化时调用
		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			// 当处于停止或惯性停止状态时
			if (scrollState == OnScrollListener.SCROLL_STATE_IDLE
					|| scrollState == OnScrollListener.SCROLL_STATE_FLING) {
//				System.out.println(getLastVisiblePosition()+":" + getCount());
				// 而且Listview最后一个条目完全展示
				if(getLastVisiblePosition()==getCount()-1&&!isLoadMore){
					isLoadMore = true;
					// 显示加载更多布局
					footer.setPadding(0, 0, 0, 0);
					System.out.println("加载更多了");
					// 自动显示加载更多布局
					setSelection(getCount());
					// 当处于加载更多时，调用监听器的onLoadingMore方法
					if(mListener!=null){
						mListener.onLoadingMore();
					}
				}
			}
		}

		@Override
		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {

		}

	}
}
