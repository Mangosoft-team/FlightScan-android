package tech.mangosoft.flightscan;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class SearchKayak extends AppCompatActivity {

    List<String> resources = new LinkedList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_kayak);

        WebView.setWebContentsDebuggingEnabled(true);

        WebView myWebView = (WebView) findViewById(R.id.webview);
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        myWebView.addJavascriptInterface(new WebAppInterface(this), "Android");

        myWebView.setWebViewClient(new WebViewClient() {

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest url){
                String urlString = url.getUrl().toString();
                if (!(!urlString.contains("px/client") && !urlString.contains("mixpanel555"))) {
                    resources.add(url.getUrl().toString());
                    try {
                        InputStream is = getAssets().open("script.js");
                        return new WebResourceResponse("text/javascript", "UTF-8", is);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                injectScriptFile(view, "script.js"); // see below ...

                // test if the script was loaded
/*               view.evaluateJavascript("(function() { return searchFormSubmission(); })();", new ValueCallback<String>() {
                   @Override
                   public void onReceiveValue(String s) {
                       System.out.println(s);
                   }
               });
               */

            }

            private void injectScriptFile(WebView view, String scriptFile) {
                InputStream input;
                try {
                    input = getAssets().open(scriptFile);
                    byte[] buffer = new byte[input.available()];
                    input.read(buffer);
                    input.close();

                    // String-ify the script byte-array using BASE64 encoding !!!
                    String encoded = Base64.encodeToString(buffer, Base64.NO_WRAP);
                    view.evaluateJavascript("javascript:(function() {" +
                            "var parent = document.getElementsByTagName('head').item(0);" +
                            "var script = document.createElement('script');" +
                            "script.type = 'text/javascript';" +
                            // Tell the browser to BASE64-decode the string into your script !!!
                            "script.innerHTML = window.atob('" + encoded + "');" +
                            "let as = parent.appendChild(script);" +
//                            "return  setTimeout(function() {searchFormSubmission(); }, 500); "+
                            "})()", new ValueCallback<String>(){
                            @Override
                            public void onReceiveValue(String s) {
                                System.out.println(s);
                            }
                    });
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });

       // myWebView.loadUrl("http://www.kayak.com");
        myWebView.loadUrl("https://www.skyscanner.net");

    }

    protected void onFinishInflate() {

    }

    public String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e("error", ex.toString());
        }
        return null;
    }



    protected class WebAppInterface {
        Context mContext;

        /** Instantiate the interface and set the context */
        WebAppInterface(Context c) {
            mContext = c;
        }

        /** Show a toast from the web page */
        @JavascriptInterface
        public void showToast(String toast) {
            Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
        }
    }

}
