package com.affirm.android;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

abstract class AffirmActivity extends AppCompatActivity implements AffirmWebChromeClient.Callbacks {

    ViewGroup container;
    WebView webView;
    View progressIndicator;

    abstract void initViews();

    abstract void beforeOnCreate();

    abstract void initData(@Nullable Bundle savedInstanceState);

    abstract void onAttached();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        beforeOnCreate();
        super.onCreate(savedInstanceState);

        setContentView(R.layout.affirm_activity_webview);
        container = getWindow().getDecorView().findViewById(android.R.id.content);
        webView = findViewById(R.id.webview);
        progressIndicator = findViewById(R.id.progressIndicator);

        initViews();

        initData(savedInstanceState);

        onAttached();
    }

    @Override
    protected void onDestroy() {
        container.removeView(webView);
        webView.removeAllViews();
        webView.clearCache(true);
        webView.destroyDrawingCache();
        webView.clearHistory();
        webView.destroy();
        webView = null;
        super.onDestroy();
    }

    @Override
    public void chromeLoadCompleted() {
        progressIndicator.setVisibility(View.GONE);
    }
}
