package zhangfei.example.mydouban;

import zhangfei.example.mydouban.engine.DoubanUserInfoProvider;

import com.google.gdata.client.douban.DoubanService;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * 
 * 抽象类 , 先定义好统一的方法，其他activity继承就可以用了。
 * 
 * @author tmac
 *
 */
public abstract class BaseMyActivity extends Activity {
	private static final int GET_USERNAME_SUCCESS = 1;
	public DoubanService myService;
	private NetConnChangedReceiver mNetReceiver;
	private SharedPreferences sp;
	public Context mContextFP;
	private ImageButton wIbtn_back;
	public TextView mTv_user_fromP;
	public RelativeLayout mRl_loading_fromP;
	public ProgressBar mPb_loadingFP;
	public TextView mTv_loadingFP;
	private Handler handler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case GET_USERNAME_SUCCESS:
				String username = (String) msg.obj;

				if (username != null) {
					mTv_user_fromP.setText("(" + username + ")");
				}
				break;

			default:
				break;
			}

		}

	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		DoubanUserInfoProvider infoProvider = new DoubanUserInfoProvider(
				getApplicationContext());
		sp = getSharedPreferences("config", Context.MODE_PRIVATE);
		myService = infoProvider.getBackDoubanService();
		/*
		 * 应该让子控件去找
		 */
		// mTv_user=(TextView) findViewById(R.id.tv_titlebar_user);
		setupView();
		setupListener();
		fillData();
		ctrlTitleBar();
		super.onCreate(savedInstanceState);
	}

	private void ctrlTitleBar() {
		showUser();
		wIbtn_back = (ImageButton) findViewById(R.id.back_button);

		wIbtn_back.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();

			}
		});
	}

	public abstract void setupView();

	public abstract void setupListener();

	public abstract void fillData();

	public void showLoading() {
		// set anim for the loading relativelayout
		AlphaAnimation aa = new AlphaAnimation(0.4f, 1.0f);
		aa.setDuration(400);
		ScaleAnimation sa = new ScaleAnimation(0.6f, 1.0f, 0.6f, 1.0f,
				Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
				0.5f);
		sa.setDuration(400);

		AnimationSet animset = new AnimationSet(false);
		animset.addAnimation(aa);
		animset.addAnimation(sa);

		if (mRl_loading_fromP != null) {
			mRl_loading_fromP.setAnimation(animset);
		}
	}

	public void hideLoading() {
		AlphaAnimation aa = new AlphaAnimation(1.0f, 0.0f);
		aa.setDuration(400);
		ScaleAnimation sa = new ScaleAnimation(1.0f, 0.6f, 1.0f, 0.6f,
				Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
				0.5f);
		sa.setDuration(400);

		AnimationSet animset = new AnimationSet(false);
		animset.addAnimation(aa);
		animset.addAnimation(sa);
		if (mRl_loading_fromP != null) {
			mRl_loading_fromP.setAnimation(animset);
		}
	}

	@Override
	protected void onStart() {
		// 完成NetConnChangedReceiver的初始化
		mNetReceiver = new NetConnChangedReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
		filter.setPriority(1000);
		registerReceiver(mNetReceiver, filter);
		super.onStart();
	}

	@Override
	protected void onStop() {

		unregisterReceiver(mNetReceiver);
		super.onStop();
	}

	class NetConnChangedReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			// abortBroadcast();
			// ConnectivityManager connectivityManager = (ConnectivityManager)
			// context
			// .getSystemService(Context.CONNECTIVITY_SERVICE);
			// NetworkInfo wifiNetInfo = connectivityManager
			// .getNetworkInfo(ConnectivityManager.TYPE_WIFI);
			// NetworkInfo mobNetInfo = connectivityManager
			// .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
			if (isNetworkAvail()) {
				// 网络改变了 ，从无到有
				showUser();
				// change the loading status
				// showLoading();
				// mRl_loading_fromP.setVisibility(View.INVISIBLE);
			} else {
				// 将账户信息改为离线
				String text = "(" + "<font color='red'>离线</font>" + ")";
				mTv_user_fromP.setText(Html.fromHtml(text));
				// change the loading status
				hideLoading();
				if (mRl_loading_fromP != null) {
					mRl_loading_fromP.setVisibility(View.VISIBLE);
					if (mPb_loadingFP != null && mTv_loadingFP != null) {
						mPb_loadingFP.setVisibility(View.GONE);
						mTv_loadingFP.setText("加载失败，请返回重试");
					}
				}
			}

		}

	}

	/**
	 * 如果用户已经授权就显示用户名信息
	 */
	public void showUser() {
		if (isUserAuthoroized()) {
			// 显示xxx已登录
			/*
			 * 获取服务器上的数据是耗时操作，要在worker thread中执行
			 */
			new Thread() {

				@Override
				public void run() {
					super.run();
					DoubanUserInfoProvider infoProvider = new DoubanUserInfoProvider(
							getApplicationContext());
					Message msg = Message.obtain();
					try {
						String username = infoProvider.getUserName();
						msg.what = GET_USERNAME_SUCCESS;
						msg.obj = username;
						handler.sendMessage(msg);
					} catch (Exception e) {
						// msg.what = GET_USERNAME_FAILED;
						// handler.sendMessage(msg);
						e.printStackTrace();
					}

				}

			}.start();

		} else {
			// 显示未授权的状态

		}
	}

	/**
	 * check out whether the network is available.
	 * 
	 * @return true if the network is available.
	 */
	public boolean isNetworkAvail() {
		ConnectivityManager cman = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo info = cman.getActiveNetworkInfo();

		return (info != null && info.isConnected());
	}

	/**
	 * 判断是否得到了用户授权
	 * 
	 * @return
	 */
	private boolean isUserAuthoroized() {
		String tokenaccess = sp.getString("tokenaccess", null);
		String tokensecret = sp.getString("tokensecret", null);
		if (tokenaccess == null || tokensecret == null
				|| "".equals(tokenaccess)) {
			return false;
		} else {
			return true;
		}
	}

	public void showToast(String text) {
		Toast.makeText(this, text, 0).show();
	}

}
