package tech.mangosoft.flightscan;

import android.content.Context;
import android.os.Build;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.ProtocolException;
import java.net.SocketException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class SearchKayak extends AppCompatActivity {

    private int appState = APP_STATE_LOADING;
    public static int APP_STATE_LOADING = 0;
    public static int APP_STATE_SEARCHING = 1;
    public static int APP_STATE_LOOKING_FOR_RESULTS = 2;
    public static int APP_STATE_RESULTS_FOUND = 3;
    public static int APP_STATE_DATE_ORIGIN_SELECTION = 4;
    public static int APP_STATE_DATE_RETURN_SELECTION = 5;

    private String lastUrl = null;

    private String from = "pari";
    private String to = "new y";
    private String datefrom = "2018-04-23";
    private String datereturn = "2018-04-28";

    List<String> resources = new LinkedList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        setContentView(R.layout.activity_search_kayak);

        //get current location from web service

        JSONObject location = getJSONObjectData("https://freegeoip.net/json/");


        WebView.setWebContentsDebuggingEnabled(true);

        WebView myWebView = (WebView) findViewById(R.id.webview);
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        android.webkit.CookieManager cookieManager = CookieManager.getInstance();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookieManager.removeAllCookies(new ValueCallback<Boolean>() {
                // a callback which is executed when the cookies have been removed
                @Override
                public void onReceiveValue(Boolean aBoolean) {
                    Log.d("COOKIE", "Cookie removed: " + aBoolean);
                }
            });
        }
        else cookieManager.removeAllCookie();

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

                appState = APP_STATE_SEARCHING;

                if (url.contains("transport/flights")) {
                    appState = APP_STATE_LOOKING_FOR_RESULTS;
                }

                if (!url.equals(lastUrl)) {
                    injectScriptFile(view, "script.js");
                }

                // test if the script was loaded
/*               view.evaluateJavascript("(function() { return searchFormSubmission(); })();", new ValueCallback<String>() {
                   @Override
                   public void onReceiveValue(String s) {
                       System.out.println(s);
                   }
               });
               */

                lastUrl = url;
            }

            private void injectScriptFile(WebView view, String scriptFile) {
                InputStream input;
                try {
                    input = getAssets().open(scriptFile);
                    byte[] buffer = new byte[input.available()];
                    input.read(buffer);
                    input.close();

                    String execution = null;

                    if (appState == APP_STATE_SEARCHING) {
                        execution = "searchFormSubmission('" + from + "', '" + to +"', '" + datefrom + "', '" + datereturn + "' )";
                    } else if (appState == APP_STATE_LOOKING_FOR_RESULTS) {
                        execution = "executeCheckFoundResults()";
                    }



                    // String-ify the script byte-array using BASE64 encoding !!!
                    String encoded = Base64.encodeToString(buffer, Base64.NO_WRAP);
                    view.evaluateJavascript("javascript:(function() {" +
                            "var parent = document.getElementsByTagName('head').item(0);" +
                            "var script = document.createElement('script');" +
                            "script.type = 'text/javascript';" +
                            // Tell the browser to BASE64-decode the string into your script !!!
                            "script.innerHTML = window.atob('" + encoded + "');" +
                            "let as = parent.appendChild(script);" +
                            ((execution != null) ? "return  setTimeout(function() {" + execution + "; }, 2000); " : "" )+
                            "})()", new ValueCallback<String>(){
                            @Override
                            public void onReceiveValue(String s) {
                                System.out.println(s);
                            }
                    });
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    Toast.makeText(getApplicationContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        final Map<String, String> additionalHeaders = new HashMap<>();
        additionalHeaders.put("Accept-Language", "en-US");
        additionalHeaders.put("X-Requested-With", "");
        try {
            additionalHeaders.put("Geo-Country",location.getString("country_code"));
            additionalHeaders.put("Geo-Position",location.getString("latitude") + "," + location.getString("longitude"));
            additionalHeaders.put("X-Geo-Location","city=" + location.getString("city") +"; country="+location.getString("country_code"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // myWebView.loadUrl("http://www.kayak.com");
        myWebView.loadUrl("https://www.skyscanner.net", additionalHeaders);

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
            Toast.makeText(getApplicationContext(), "Error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
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

        @JavascriptInterface
        public void uploadParsedResults(String jsonString) {
            Toast.makeText(mContext, jsonString, Toast.LENGTH_LONG).show();
            try {
                JSONArray data = new JSONArray(jsonString);
                System.out.println(data.length());
                System.out.println(jsonString);
                sendJSONPDataToServer(jsonString);
                appState = APP_STATE_RESULTS_FOUND;
            } catch (JSONException e) {
                Toast.makeText(mContext, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();

            }
        }

        private void  sendJSONPDataToServer(String data) {
            URL url = null;
            try {
                url = new URL("http://194.31.46.184:9090/flightsapi/flight/multiple");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(100000);
                conn.setConnectTimeout(150000);
                conn.setRequestMethod("POST");
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.addRequestProperty("Accept", "application/json");
                conn.addRequestProperty("Content-Type", "application/json");

                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));
                //writer.write(getQuery(params));
                writer.write(data);
                writer.flush();
                writer.close();
                os.close();

                conn.connect();
                String encoding = conn.getContentEncoding();

                BufferedReader streamReader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder responseStrBuilder = new StringBuilder();

                String inputStr;
                while ((inputStr = streamReader.readLine()) != null)
                    responseStrBuilder.append(inputStr);
                String s = responseStrBuilder.toString();
                JSONArray obj = null;
                try {
                    obj = new JSONArray(responseStrBuilder.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                if (obj != null) {
                    obj.length();
                    System.out.println("Response: " + obj.length() + " elements were saved" );
                    Toast.makeText(mContext, "Error: " + "Response: " + obj.length() + " elements were saved" , Toast.LENGTH_LONG).show();
                } else {
                    //error - nothing got
                    Toast.makeText(mContext, "Error: " + "Response code: " +  conn.getResponseCode(), Toast.LENGTH_LONG).show();
                    System.out.println("Response code: " +  conn.getResponseCode());
                }



            } catch (MalformedURLException e) {
                System.out.println("Response was: " + e.getMessage());
                Toast.makeText(mContext, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            } catch (UnsupportedEncodingException e) {
                System.out.println("Response was: " + e.getMessage());
                Toast.makeText(mContext, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            } catch (ProtocolException e) {
                System.out.println("Response was: " + e.getMessage());
                Toast.makeText(mContext, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                System.out.println("Response was: " + e.getMessage());
                Toast.makeText(mContext, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }


    }

    private JSONObject getJSONObjectData(String urlString) {
        URL url = null;
        JSONObject obj = null;
        try {
            url = new URL(urlString);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
            conn.setReadTimeout(100000);
            conn.setConnectTimeout(150000);
            conn.setRequestMethod("GET");
            conn.addRequestProperty("Accept", "application/json");
            conn.addRequestProperty("Content-Type", "application/json");
            conn.connect();
            String encoding = conn.getContentEncoding();

            BufferedReader streamReader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            StringBuilder responseStrBuilder = new StringBuilder();

            String inputStr;
            while ((inputStr = streamReader.readLine()) != null)
                responseStrBuilder.append(inputStr);
            String s = responseStrBuilder.toString();
            try {
                obj = new JSONObject(responseStrBuilder.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (obj != null) {
                obj.length();
                System.out.println("Response: " + obj.length() + " elements in JSON were received\n" + s );
            } else {
                //error - nothing got
                System.out.println("Response code: " +  conn.getResponseCode());
            }



        } catch (MalformedURLException e) {
            System.err.println("Response was: " + e.getMessage());
        } catch (UnsupportedEncodingException e) {
            System.err.println("Response was: " + e.getMessage());
        } catch (ProtocolException e) {
            System.err.println("Response was: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Response was: " + e.getMessage());
        }

        return obj;
    }

}
