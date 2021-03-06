package zhangfei.example.mydouban.Utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

import zhangfei.example.mydouban.engine.DoubanUserInfoProvider;

import com.google.gdata.client.douban.DoubanService;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.util.ServiceException;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

/**
 * @author tmac 自定义的异常处理类 将获取的错误信息以私有日记的形式发表到豆瓣服务器
 */
public class MyCrashHandler implements UncaughtExceptionHandler {
	private Context context;
	// 保证MyCrashHandler 只有一个实例
	// 2.静态的成员变量
	private static MyCrashHandler myCrashHandler;

	// 1.私有化构造方法
	private MyCrashHandler() {
	}

	// 3.暴露出一个静态方法
	public static synchronized MyCrashHandler getInstance() {
		if (myCrashHandler == null) {
			myCrashHandler = new MyCrashHandler();
		}
		return myCrashHandler;
	}

	public void init(Context context) {
		this.context = context;
	}

	// 程序异常时调用的方法

	public void uncaughtException(Thread thread, Throwable ex) {
		System.out.println("====程序出错====");
		StringBuilder sb = new StringBuilder();
		PackageManager pm = context.getPackageManager();
		// 获取重要的手机及程序信息

		PackageInfo info;
		try {
			info = pm.getPackageInfo(context.getPackageName(), 0);
			// 1.获取当前应用程序的版本号.
			sb.append("程序的版本号->" + info.versionName);
			sb.append("\n");
			sb.append("~~~~~手机的硬件信息~~~~~~");
			sb.append("\n");
			// 2.获取手机的硬件信息.
			Field[] fields = Build.class.getDeclaredFields();
			for (int i = 0; i < fields.length; i++) {
				// 暴力反射，获取私有的字段信息
				fields[i].setAccessible(true);
				String name = fields[i].getName();
				sb.append("name=" + name);
				sb.append("\t");
				String vaule = fields[i].get(null).toString();
				sb.append("value=" + vaule);
				sb.append("\n");
			}
			sb.append("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
			sb.append("\n");

		} catch (NameNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// 3.获取程序错误的堆栈信息 .
		sb.append("~~~~~程序错误的堆栈信息~~~~~~");
		sb.append("\n");
		StringWriter writer = new StringWriter();
		PrintWriter printWriter = new PrintWriter(writer);
		// 指定错误信息的输出对象
		ex.printStackTrace(printWriter);
		String result = writer.toString();
		sb.append(result);

		sb.append("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
		sb.append("\n");
		// 4.把错误信息 提交到服务器
		System.out.println(sb.toString());

		// 获取当前系统时间作为标题的一部分
		SimpleDateFormat dateFormat = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss");
		final String title = "mydouban错误日志 "
				+ dateFormat.format(new Date(System.currentTimeMillis()));
		final String content = sb.toString();

		// // 当网络可用时才发送
		if (isNetworkAvail()) {
			// 发表日志是耗时操作，要放在asynctask中执行
			new Thread() {

				@Override
				public void run() {
					super.run();

					DoubanUserInfoProvider infoProvider = new DoubanUserInfoProvider(
							context);
					DoubanService myService = infoProvider
							.getBackDoubanService();
					try {
						myService.createNote(new PlainTextConstruct(title),
								new PlainTextConstruct(content), "private",
								"no");

					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();

					}

					try {
						sleep(500);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					// 完成自杀的操作
					android.os.Process.killProcess(android.os.Process.myPid());
				}

			}.start();
		}
		// 下面的方法失败
		// new AsyncTask<Void, Void, Boolean>() {

		Log.e("error", sb.toString());

	}

	/**
	 * check out whether the network is available.
	 * 
	 * @return true if the network is available.
	 */
	public boolean isNetworkAvail() {
		ConnectivityManager cman = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = cman.getActiveNetworkInfo();

		return (info != null && info.isConnected());
	}

}
