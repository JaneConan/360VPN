package com.secure.vpnclient.demo;

import com.secure.libsportal.sdk.demo.R;
import com.secure.sportal.sdk.SPVPNClient;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;

@SuppressLint("SetJavaScriptEnabled")
public class DemoWebViewActivity extends Activity implements View.OnClickListener
{
    private View     mQuitBtn;
    private EditText mUrlText;
    private View     mGoBtn;
    private WebView  mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        setContentView(R.layout.activity_webview);

        mQuitBtn = findViewById(R.id.browser_btn_quit);
        mUrlText = (EditText) findViewById(R.id.browser_txt_url);
        mGoBtn = findViewById(R.id.browser_btn_go);
        mWebView = (WebView) findViewById(R.id.browser_webview);

        mQuitBtn.setOnClickListener(this);
        mGoBtn.setOnClickListener(this);

        mWebView.getSettings().setLoadWithOverviewMode(true);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.getSettings().setBuiltInZoomControls(true);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        mWebView.clearCache(true);

        mWebView.setWebViewClient(new ProxyWebViewClient());
        mWebView.setWebChromeClient(new ProxyChromeClient());

        // allow https redirect to http
        // http://stackoverflow.com/questions/28626433/android-webview-blocks-redirect-from-https-to-http
        if (Build.VERSION.SDK_INT >= 21)
        {
            mWebView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        // 为webview设置代理服务器
        // 也可以不用设置代理服务器，SDK底层自动Hook连接
        SPVPNClient.setWebViewProxy(mWebView);

        final String url = getIntent().getStringExtra("service_url");
        if (!TextUtils.isEmpty(url))
        {
            mUrlText.setText(url);
            mWebView.postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    mWebView.loadUrl(url);
                }
            }, 100);
        }
    }

    @Override
    public void onBackPressed()
    {
        if (mWebView.canGoBack())
        {
            mWebView.goBack();
        }
        else
        {
            super.onBackPressed();
        }
    }

    @Override
    public void onClick(View v)
    {
        if (v.getId() == R.id.browser_btn_go)
        {
            mWebView.loadUrl(mUrlText.getText().toString());
        }
        else if (v.getId() == R.id.browser_btn_quit)
        {
            onBackPressed();
        }
    }

    private class ProxyWebViewClient extends WebViewClient
    {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url)
        {
            // return super.shouldOverrideUrlLoading(view, url);
            mUrlText.setText(url);
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error)
        {
            Log.d("tun", "onReceivedSslError error=" + error);
            // super.onReceivedSslError(view, handler, error);
            handler.proceed();
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon)
        {
            mUrlText.setText(url);
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url)
        {
            super.onPageFinished(view, url);
            // mEditTitle.setText(view.getTitle());
        }
    }

    private class ProxyChromeClient extends WebChromeClient
    {
        @Override
        public void onProgressChanged(WebView view, int newProgress)
        {
            super.onProgressChanged(view, newProgress);
            // mProgressLoad.setProgress(newProgress);
        }

        @Override
        public void onReceivedTitle(WebView view, String title)
        {
            super.onReceivedTitle(view, title);
            // mEditTitle.setText(title);
        }
    }
}
