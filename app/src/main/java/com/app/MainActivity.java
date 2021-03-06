package com.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

	private WebView contentWebView = null;
	private TextView msgView = null;
    Vibrator m_vib = null;

    private void sendNotify(){
        //定义NotificationManager
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);
        //定义通知栏展现的内容信息
        int icon = R.drawable.icon;
        CharSequence tickerText = "我的通知栏标题";
        long when = System.currentTimeMillis();
        Notification notification = new Notification(icon, tickerText, when);

        //定义下拉通知栏时要展现的内容信息
        Context context = getApplicationContext();
        CharSequence contentTitle = "我的通知栏标展开标题";
        CharSequence contentText = "我的通知栏展开详细内容";
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);
        notification.setLatestEventInfo(context, contentTitle, contentText,
                contentIntent);

        //用mNotificationManager的notify方法通知用户生成标题栏消息通知
        mNotificationManager.notify(1, notification);
    }
	@SuppressLint("SetJavaScriptEnabled")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);
		contentWebView = (WebView) findViewById(R.id.webview);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            contentWebView.getSettings().setAllowUniversalAccessFromFileURLs(true);
		//msgView = (TextView) findViewById(R.id.msg);
		// using javascript
		contentWebView.getSettings().setJavaScriptEnabled(true);
		//
		contentWebView.loadUrl("file:///android_asset/MobileBootstrap/forms.html");

        contentWebView.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);   //在当前的webview中跳转到新的url
                return true;
            }
        });
        //点击后退按钮,让WebView后退一页(也可以覆写Activity的onKeyDown方法)
        contentWebView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_BACK && contentWebView.canGoBack()) {  //表示按返回键时的操作
                        contentWebView.goBack();   //后退
                        //webview.goForward();//前进
                        return true;    //已处理
                    }
                }
                return false;
            }
        });

        //Button button = (Button) findViewById(R.id.button);
		//button.setOnClickListener(btnClickListener);
		//contentWebView.addJavascriptInterface(this, "wst");

        m_vib = (Vibrator)this.getSystemService(Service.VIBRATOR_SERVICE);


        AsyncHttpServer server = new AsyncHttpServer();
        final List<WebSocket> _sockets = new ArrayList<WebSocket>();
        server.get("/", new HttpServerRequestCallback() {
            @Override
            public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
                response.send("Hello!!!");
            }
        });
        // listen on port 5000
        server.listen(5000);
        // browsing http://localhost:5000 will return Hello!!!

        server.websocket("/live", new AsyncHttpServer.WebSocketRequestCallback() {
            @Override
            public void onConnected(final WebSocket webSocket, AsyncHttpServerRequest request) {
                _sockets.add(webSocket);

                //Use this to clean up any references to your websocket
                webSocket.setClosedCallback(new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        try {
                            if (ex != null)
                                Log.e("WebSocket", "Error");
                        } finally {
                            _sockets.remove(webSocket);
                        }
                    }
                });

                webSocket.setStringCallback(new WebSocket.StringCallback() {
                    @Override
                    public void onStringAvailable(String s) {
                        //if ("Hello Server".equals(s))
                        m_vib.vibrate(1000);
                        sendNotify();
                        int _height = getWindowManager().getDefaultDisplay().getHeight();
                        webSocket.send("Get height: " + Integer.toString(_height));
                    }
                });

            }
        });
        //..Sometime later, broadcast!
        for (WebSocket socket : _sockets)
            socket.send("Fireball!");
    }


    OnClickListener btnClickListener = new Button.OnClickListener() {
		public void onClick(View v) {
			//no args.
			contentWebView.loadUrl("javascript:javacalljs()");
			//with args.
			contentWebView.loadUrl("javascript:javacalljswithargs(" + "'hello world'" + ")");
		}
	};

	public void startFunction() {
		Toast.makeText(this, "js invoke java func.", Toast.LENGTH_SHORT).show();
        Vibrator vib = (Vibrator)this.getSystemService(Service.VIBRATOR_SERVICE);
        vib.vibrate(1000);
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				msgView.setText(msgView.getText() + "\njs invoke java func.");

			}
		});
	}

	public void startFunction(final String str) {
		Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				msgView.setText(msgView.getText() + "\njs invoke javs func with args." + str);

			}
		});
	}
}
