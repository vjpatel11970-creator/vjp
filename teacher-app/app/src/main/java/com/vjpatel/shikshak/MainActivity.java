package com.vjpatel.shikshak;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/** શિક્ષક એપ (Android): શાળાની સાઇટનું શિક્ષક પાનું (?teacher=1) પૂર્ણ-સ્ક્રીનમાં ખોલે છે. */
public class MainActivity extends Activity {

    private static final String SITE_HOST = "vjpatel11970-creator.github.io";
    private static final String START_URL = "https://vjpatel11970-creator.github.io/vjp/?teacher=1";

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.parseColor("#1B5E4B"));

        webView = new WebView(this);
        webView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(webView);

        WebSettings st = webView.getSettings();
        st.setJavaScriptEnabled(true);
        st.setDomStorageEnabled(true);
        st.setCacheMode(WebSettings.LOAD_DEFAULT);
        st.setAllowFileAccess(true);
        st.setAllowContentAccess(false);
        st.setSupportMultipleWindows(false);
        st.setUserAgentString(st.getUserAgentString() + " VJPTeacherApp/1");

        webView.addJavascriptInterface(new Bridge(), "AndroidApp");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                String scheme = u.getScheme() == null ? "" : u.getScheme();
                if (("https".equals(scheme) || "http".equals(scheme)) && SITE_HOST.equals(u.getHost())) {
                    return false; // પોતાની સાઇટ: એપની અંદર જ ખુલે
                }
                if ("file".equals(scheme)) {
                    return false;
                }
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, u)); // બાકી બધું (PDF/Drive/YouTube/ફોન) બહારની એપમાં
                } catch (Exception ignored) {
                }
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                // WebView માં window.print() ચાલતું નથી, એટલે તેને Android ના પ્રિન્ટ/PDF સાથે જોડીએ
                view.evaluateJavascript(
                        "window.print=function(){try{AndroidApp.print();}catch(e){}};", null);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    view.loadUrl("file:///android_asset/offline.html");
                }
            }
        });

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.loadUrl(START_URL);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) webView.saveState(outState);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }

    /** પેજમાંથી બોલાવી શકાય તેવા ફંક્શન (ફક્ત print). */
    private class Bridge {
        @JavascriptInterface
        public void print() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (webView == null) return;
                    PrintManager pm = (PrintManager) getSystemService(Context.PRINT_SERVICE);
                    String job = "પરિણામ";
                    PrintDocumentAdapter adapter = webView.createPrintDocumentAdapter(job);
                    pm.print(job, adapter, new PrintAttributes.Builder().build());
                }
            });
        }
    }
}
