package com.sunflowerland.mobile;

import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.content.Context;
import com.getcapacitor.BridgeActivity;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import java.util.List;
import android.util.Log;
import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;

import android.os.Message;
import android.graphics.Bitmap;
import android.webkit.WebResourceResponse;
import java.io.ByteArrayInputStream;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebResourceRequest;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import android.Manifest;
import android.view.View;
import android.os.Build;
import android.view.WindowManager;
import androidx.core.view.WindowCompat;

public class MainActivity extends BridgeActivity {

    // Removed duplicate onCreate and onResume methods to resolve redefinition errors.

    private void hideSystemUIDelayed() {
        // Re-apply immersive mode after a short delay to ensure it sticks
        final View decorView = getWindow().getDecorView();
        decorView.postDelayed(new Runnable() {
            @Override
            public void run() {
                hideSystemUI();
            }
        }, 200);
    }

    private void hideSystemUI() {
        // Immersive sticky mode: hides navigation and status bars, but shows them when the keyboard is open or on swipe
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            final android.view.WindowInsetsController insetsController = getWindow().getInsetsController();
            if (insetsController != null) {
                insetsController.hide(android.view.WindowInsets.Type.navigationBars() | android.view.WindowInsets.Type.statusBars());
                insetsController.setSystemBarsBehavior(
                        android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
            );
        }
    }
    private boolean deepLinkHandlersAttached = false;
    private WebView mainWebView = null;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 100;
    private static final int AUDIO_PERMISSION_REQUEST_CODE = 101;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize debug log system
        DebugLog.init(this);
        
        Log.i("NOTIFICATION_DEBUG", "=== Sunflower Land App Starting ===");
        Log.i("NOTIFICATION_DEBUG", "Process ID: " + android.os.Process.myPid());
        Log.i("NOTIFICATION_DEBUG", "App Package: " + getPackageName());
        
        // Check if "Only Notifications" mode is enabled
        android.content.SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean onlyNotificationsMode = prefs.getBoolean("only_notifications", false);
        
        if (onlyNotificationsMode) {
            Log.d("MainActivity", "Only Notifications mode enabled - redirecting to SettingsActivity");
            // Start SettingsActivity and finish MainActivity
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
            finish();
            return;
        }

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Requesting POST_NOTIFICATIONS permission");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE);
            } else {
                Log.d("MainActivity", "POST_NOTIFICATIONS permission already granted");
            }
        }

        // Setup JavaScript interface immediately (don't defer)
        // This configures WebView settings and clients needed for the page to load
        setupJavaScriptInterface();
        
        handleIntent(getIntent());

        // Attach deep-link interceptors immediately - CRITICAL for WebView functionality
        attachDeepLinkHandlers();
        
        // Auto-start notification manager immediately (it runs in separate process, won't interfere)
        autoStartNotificationManager();
        
        // OPTIMIZATION: Defer UI dialogs and heavy JavaScript injection to run AFTER WebView loads
        // This prevents blocking the browser from loading
        this.getWindow().getDecorView().post(new Runnable() {
            @Override
            public void run() {
                try {
                    // Show first-launch dialog after UI is ready (only shows on first launch due to flag)
                    showFirstLaunchDialog();
                } catch (Exception e) {
                    Log.e("MainActivity", "Error in deferred initialization: " + e.getMessage(), e);
                }
            }
        });
    }
    
    private void setupJavaScriptInterface() {
        try {
            Object webViewObj = null;
            try {
                webViewObj = this.bridge.getWebView();
            } catch (Exception inner) {
                webViewObj = null;
            }

            WebView wv = null;
            if (webViewObj instanceof WebView) {
                wv = (WebView) webViewObj;
            } else if (webViewObj != null) {
                try {
                    Object engine = null;
                    try {
                        engine = webViewObj.getClass().getMethod("getEngine").invoke(webViewObj);
                    } catch (NoSuchMethodException nsme) {
                        // ignore
                    }
                    if (engine != null) {
                        Object view = null;
                        try {
                            view = engine.getClass().getMethod("getView").invoke(engine);
                        } catch (NoSuchMethodException nsme2) {
                            // ignore
                        }
                        if (view instanceof WebView) {
                            wv = (WebView) view;
                        }
                    }

                    if (wv == null) {
                        try {
                            Object view2 = webViewObj.getClass().getMethod("getView").invoke(webViewObj);
                            if (view2 instanceof WebView) {
                                wv = (WebView) view2;
                            }
                        } catch (NoSuchMethodException nsme3) {
                            // ignore
                        }
                    }
                } catch (Exception ex) {
                    Log.w("MainActivity", "Reflection to obtain WebView failed", ex);
                }
            }

            if (wv != null) {
                // Store WebView reference for back button functionality
                this.mainWebView = wv;
                
                getWindow().setFlags(
                    android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                );

                wv.setLayerType(WebView.LAYER_TYPE_HARDWARE, null);

                WebSettings ws = wv.getSettings();
                ws.setJavaScriptEnabled(true);
                ws.setDomStorageEnabled(true);
                ws.setDatabaseEnabled(true);
                ws.setAllowFileAccess(true);
                ws.setAllowContentAccess(true);
                ws.setAllowUniversalAccessFromFileURLs(true);
                ws.setAllowFileAccessFromFileURLs(true);
                ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
                ws.setJavaScriptCanOpenWindowsAutomatically(true);
                ws.setMediaPlaybackRequiresUserGesture(false);
                
                // Use LOAD_DEFAULT to respect server cache headers
                // This allows the game/website to control caching via HTTP headers
                // (Cache-Control, ETag, Last-Modified, etc.)
                // Only cache what the game/site explicitly requests to be cached
                ws.setCacheMode(WebSettings.LOAD_DEFAULT);
                
                ws.setLoadWithOverviewMode(true);
                ws.setUseWideViewPort(true);
                
                // Set User-Agent to make Google accept this as a secure browser
                String defaultUserAgent = ws.getUserAgentString();
                String customUserAgent = defaultUserAgent.replace("; wv", "").replace("Version/4.0 ", "");
                if (!customUserAgent.contains("Chrome/")) {
                    // Add Chrome version if not present to satisfy Google's requirements
                    customUserAgent = customUserAgent + " Chrome/120.0.0.0";
                }
                ws.setUserAgentString(customUserAgent);
                Log.d("MainActivity", "Set User-Agent: " + customUserAgent);
                
                // Additional settings to make WebView appear as a standard browser
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ws.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
                }
                
                // Enable geolocation and other browser features
                ws.setGeolocationEnabled(true);
                ws.setBuiltInZoomControls(true);
                ws.setDisplayZoomControls(false);
                ws.setSupportZoom(true);
                
                // Security settings that help with Google OAuth
                ws.setSaveFormData(false);
                ws.setSavePassword(false);
                
                // Enable storage APIs
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ws.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
                }
                
                // Enable remote debugging and console logging
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    WebView.setWebContentsDebuggingEnabled(true);
                }
                
                // Add JavaScript interface for WebView functionality
                Log.i("NOTIFICATION_DEBUG", "� WebView configured and ready");
                wv.setWebChromeClient(new android.webkit.WebChromeClient() {
                    @Override
                    public boolean onConsoleMessage(android.webkit.ConsoleMessage cm) {
                        String message = cm.message();
                        Log.d("WebView Console", String.format("[%s:%d] %s", cm.sourceId(), cm.lineNumber(), message));
                        
                        // Log our API monitoring messages to NOTIFICATION_DEBUG
                        if (message.contains("[SUNFLOWER]")) {
                            Log.i("NOTIFICATION_DEBUG", "🌻 " + message);
                        }
                        
                        // Specifically log game-related data with timestamps
                        String lowerMsg = message.toLowerCase();
                        if (lowerMsg.contains("soybean") || lowerMsg.contains("orange") || lowerMsg.contains("tomato") || 
                            lowerMsg.contains("rice") || lowerMsg.contains("cow") || lowerMsg.contains("sheep") ||
                            lowerMsg.contains("timestamp") || lowerMsg.contains("readyat") || lowerMsg.contains("completedat") ||
                            lowerMsg.contains("harvestat") || lowerMsg.contains("finishedat") || lowerMsg.contains("crops") ||
                            lowerMsg.contains("animals") || lowerMsg.contains("farm")) {
                            Log.e("GAME_DATA", "🎮 FARM DATA: " + message);
                        }
                        return true;
                    }

                    @Override
                    public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                        // Intercept popup/new-window requests (e.g., window.open) and capture the URL
                        WebView newWebView = new WebView(view.getContext());
                        newWebView.setWebViewClient(new android.webkit.WebViewClient() {
                            @Override
                            public void onPageStarted(WebView w, String url, Bitmap favicon) {
                                Log.d("MainActivity", "Popup requested URL: " + url);
                                if (tryHandleDeepLink(url)) {
                                    // Handled externally - stop loading in the temp view
                                    try {
                                        w.stopLoading();
                                    } catch (Exception e) { }
                                } else {
                                    // If not a deep link, load in the main view
                                    view.loadUrl(url);
                                }
                                // Cleanup: destroy temp webview
                                try {
                                    w.destroy();
                                } catch (Exception ex) { }
                            }
                        });
                        WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                        transport.setWebView(newWebView);
                        resultMsg.sendToTarget();
                        return true;
                    }
                    
                    @Override
                    public void onReceivedTitle(WebView view, String title) {
                        super.onReceivedTitle(view, title);
                        if (title != null && title.startsWith("FARM_DATA:")) {
                            String jsonData = title.substring(10);
                            Log.e("🎮 EXTRACTED_DATA", "Farm storage data: " + jsonData);
                        }
                    }
                });
                wv.setWebViewClient(new android.webkit.WebViewClient() {
                    @Override
                    public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                        Log.d("MainActivity", ">>> onPageStarted: " + url);
                        // Intercept Coinbase URLs before they load
                        if (url.contains("keys.coinbase.com") || url.contains("coinbase.com") || url.contains("login.coinbase")) {
                            Log.d("MainActivity", "*** INTERCEPTED COINBASE URL in onPageStarted: " + url);
                            try {
                                view.stopLoading();
                                Intent extIntent = new Intent(Intent.ACTION_VIEW);
                                extIntent.setData(Uri.parse(url));
                                extIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                try {
                                    extIntent.setPackage("com.coinbase.android");
                                    startActivity(extIntent);
                                } catch (Exception e) {
                                    Log.d("MainActivity", "Coinbase app not available, opening in browser");
                                    extIntent.setPackage(null);
                                    startActivity(extIntent);
                                }
                            } catch (Exception e) {
                                Log.e("MainActivity", "Failed to handle Coinbase URL in onPageStarted", e);
                            }
                        }
                    }
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        Log.d("MainActivity", "Page load finished: " + url);
                        
                        // OPTIMIZATION: Defer JavaScript injection to not block page rendering
                        // Post to handler with small delay to let page render first
                        new Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    // Inject console logger + intercept non-http(s) links and window.open to route via a safe bridge URL
                                    String script = "(function(){" +
                                        "console.log('WebView ready - installing native link interceptor');" +
                                        "window.onerror = function(msg, url, line) { console.error('JS Error:', msg, 'at', url, ':', line); return false; };" +
                                        // Intercept window.location setter
                                        "var proto = Object.getPrototypeOf(window.location); " +
                                        "var desc = Object.getOwnPropertyDescriptor(proto, 'href'); " +
                                        "if(desc && desc.set) { " +
                                        "  var originalSetter = desc.set; " +
                                        "  Object.defineProperty(proto, 'href', { " +
                                        "    set: function(val) { " +
                                        "      console.log('Intercepted window.location.href: ' + val); " +
                                        "      if(val && (val.indexOf('http') !== 0 || val.indexOf('keys.coinbase.com') !== -1 || val.indexOf('coinbase.com/connect') !== -1)) { " +
                                        "        console.log('Blocked URL detected, routing through bridge: ' + val); " +
                                        "        window.location.href = 'https://__native_bridge__/open?u=' + encodeURIComponent(val); " +
                                        "        return; " +
                                        "      } " +
                                        "      originalSetter.call(this, val); " +
                                        "    } " +
                                        "  }); " +
                                        "}" +
                                        // override window.open - especially for Coinbase popup auth
                                        "var _open = window.open; window.open = function(url, target, features){ try{ console.log('window.open called with URL: ' + url); if(url && (url.indexOf('keys.coinbase.com') !== -1 || url.indexOf('coinbase.com') !== -1 || url.indexOf('login.coinbase.com') !== -1)){ console.log('Detected Coinbase popup window.open, launching Coinbase app directly'); setTimeout(function(){ window.location.href = 'https://__native_bridge__/open?u=' + encodeURIComponent(url); }, 10); return { close: function(){}, opener: window, closed: false }; } if(url && (url.indexOf('http') !== 0 && url.indexOf('//') !== 0)){ console.log('Detected non-http scheme: ' + url); setTimeout(function(){ window.location.href = 'https://__native_bridge__/open?u=' + encodeURIComponent(url); }, 10); return { close: function(){}, opener: window, closed: false }; } }catch(e){ console.error('Error in window.open override: ' + e); } return _open.apply(this, arguments); };" +
                                        "document.addEventListener('click', function(e){ var a = e.target; while(a && a.tagName!=='A'){ a = a.parentElement; } if(a && a.href){ try{ var href = a.getAttribute('href'); console.log('Click intercepted on link: ' + href); if(href && (href.indexOf('http')!==0 || href.indexOf('keys.coinbase.com') !== -1 || href.indexOf('coinbase.com/connect') !== -1)){ e.preventDefault(); console.log('Blocked URL via click handler, redirecting: ' + href); window.location.href = 'https://__native_bridge__/open?u=' + encodeURIComponent(href); return false; } }catch(err){} } }, true);" +
                                        "var _fetch = window.fetch; window.fetch = function(url){  try{ if(url && (typeof url === 'string' && (url.indexOf('keys.coinbase.com') !== -1 || url.indexOf('coinbase.com/connect') !== -1))){ console.log('Blocked fetch to Coinbase: ' + url); window.location.href = 'https://__native_bridge__/open?u=' + encodeURIComponent(url); return Promise.reject('Blocked'); } }catch(e){} return _fetch.apply(this, arguments); };" +
                                        "document.addEventListener('submit', function(e){ var action = e.target.action || ''; console.log('Form submit detected: ' + action); if(action && (action.indexOf('keys.coinbase.com') !== -1 || action.indexOf('coinbase.com/connect') !== -1)){ e.preventDefault(); console.log('Blocked form submission to Coinbase: ' + action); window.location.href = 'https://__native_bridge__/open?u=' + encodeURIComponent(action); } }, true);" +
                                        "})();";

                                    Log.i("NOTIFICATION_DEBUG", "🚀 Injecting link interceptor JavaScript...");
                                    view.evaluateJavascript(script, null);
                                    Log.i("NOTIFICATION_DEBUG", "✅ Link interceptor injection completed");
                                } catch (Exception e) {
                                    Log.e("MainActivity", "Error injecting link interceptor script: " + e.getMessage(), e);
                                }
                            }
                        }, 100);  // 100ms delay to allow page to render first
                    }
                    @Override
                    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                        Log.e("MainActivity", "WebView error: code=" + errorCode + " desc=" + description + " url=" + failingUrl);
                        Log.e("MainActivity", "WebView error: code=" + errorCode + " desc=" + description + " url=" + failingUrl);
                        // Older API fallback: try to handle unknown scheme errors by launching external handler
                        if (failingUrl != null && (failingUrl.startsWith("metamask:") || failingUrl.startsWith("wc:"))) {
                            if (tryHandleDeepLink(failingUrl)) {
                                // prevent showing WebView error page
                                view.loadUrl("about:blank");
                            }
                        }
                    }

                    @Override
                    public void onReceivedError(WebView view, android.webkit.WebResourceRequest request, android.webkit.WebResourceError error) {
                        String failingUrl = request.getUrl().toString();
                        Log.e("MainActivity", "WebView request error: " + failingUrl + " -> " + error.getDescription());
                        if (failingUrl != null && (failingUrl.startsWith("metamask:") || failingUrl.startsWith("wc:"))) {
                            if (tryHandleDeepLink(failingUrl)) {
                                view.loadUrl("about:blank");
                            }
                        }
                    }
                    @Override
                    public void onReceivedSslError(WebView view, android.webkit.SslErrorHandler handler, android.net.http.SslError error) {
                        Log.e("MainActivity", "SSL Error: " + error);
                        handler.cancel(); // Reject SSL errors - do not proceed with untrusted certificates
                    }
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        Log.d("MainActivity", "Should override URL (string): " + url);
                        
                        // Intercept Coinbase URLs immediately and don't load them in WebView
                        if (url.contains("keys.coinbase.com") || url.contains("coinbase.com/connect") || url.contains("login.coinbase.com")) {
                            Log.d("MainActivity", "Intercepted Coinbase URL in string handler, opening externally: " + url);
                            try {
                                Intent extIntent = new Intent(Intent.ACTION_VIEW);
                                extIntent.setData(Uri.parse(url));
                                extIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                extIntent.setPackage("com.coinbase.android");
                                startActivity(extIntent);
                            } catch (Exception e) {
                                Log.e("MainActivity", "Failed to open Coinbase app, trying browser", e);
                                try {
                                    Intent browserIntent = new Intent(Intent.ACTION_VIEW);
                                    browserIntent.setData(Uri.parse(url));
                                    browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(browserIntent);
                                } catch (Exception e2) {
                                    Log.e("MainActivity", "Failed to open Coinbase URL", e2);
                                }
                            }
                            return true;
                        }
                        
                        boolean handled = tryHandleDeepLink(url);
                        Log.d("MainActivity", "shouldOverrideUrlLoading result: " + handled + " for URL: " + url);
                        return handled;
                    }

                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                        String url = request.getUrl().toString();
                        Log.d("MainActivity", "Should override URL (request): " + url);
                        
                        // Handle native bridge URL for deep link opening (UNIFIED CLIENT)
                        if (url.startsWith("https://__native_bridge__/open")) {
                            Uri u = request.getUrl();
                            String target = u.getQueryParameter("u");
                            if (target != null) {
                                String decoded = Uri.decode(target);
                                Log.d("MainActivity", "(unified) Native bridge requested open: " + decoded);
                                if (tryHandleDeepLink(decoded)) return true;
                            }
                            return true;
                        }
                        
                        // Handle notification bridge URL (UNIFIED CLIENT)
                        if (url.startsWith("https://__native_bridge__/notification")) {
                            Uri u = request.getUrl();
                            String title = u.getQueryParameter("title");
                            String body = u.getQueryParameter("body");
                            String delayStr = u.getQueryParameter("delay");
                            
                            if (title != null && body != null && delayStr != null) {
                                try {
                                    long delay = Long.parseLong(delayStr);
                                    Log.i("NOTIFICATION_DEBUG", "🔔 (unified) Scheduling notification: " + title + " - " + body + " in " + (delay/1000) + "s");
                                    
                                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                        Log.i("NOTIFICATION_DEBUG", "📲 (unified) Sending notification now: " + title);
                                        if (mainWebView != null) {
                                            mainWebView.evaluateJavascript(
                                                "if(window.Capacitor && window.Capacitor.Plugins && window.Capacitor.Plugins.LocalNotifications) {" +
                                                "  window.Capacitor.Plugins.LocalNotifications.schedule({" +
                                                "    notifications: [{" +
                                                "      id: " + System.currentTimeMillis() + "," +
                                                "      title: '" + title.replace("'", "\\'") + "'," +
                                                "      body: '" + body.replace("'", "\\'") + "'," +
                                                "      schedule: { at: new Date(Date.now() + 1000) }" +
                                                "    }]" +
                                                "  }).then(() => console.log('✅ (unified) Farm notification sent'))" +
                                                "   .catch(e => console.error('❌ (unified) Farm notification failed:', e));" +
                                                "}", null);
                                        }
                                    }, delay);
                                    
                                } catch (NumberFormatException e) {
                                    Log.e("NOTIFICATION_DEBUG", "❌ (unified) Invalid delay parameter: " + delayStr);
                                }
                            }
                            return true;
                        }
                        
                        // Handle console log bridge URL (UNIFIED CLIENT)
                        if (url.startsWith("https://__native_bridge__/log")) {
                            Uri u = request.getUrl();
                            String msg = u.getQueryParameter("msg");
                            if (msg != null) {
                                Log.i("JS_CONSOLE", "(unified) " + Uri.decode(msg));
                            }
                            return true;
                        }
                        
                        // Intercept Coinbase URLs and open in external app/browser instead of loading internally
                        if (url.contains("keys.coinbase.com") || url.contains("coinbase.com/connect")) {
                            Log.d("MainActivity", "Intercepted Coinbase URL, opening externally");
                            try {
                                Intent extIntent = new Intent(Intent.ACTION_VIEW);
                                extIntent.setData(Uri.parse(url));
                                extIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                // Try to launch with Coinbase app if available
                                PackageManager pm = getPackageManager();
                                try {
                                    extIntent.setPackage("com.coinbase.android"); // Coinbase package
                                    startActivity(extIntent);
                                } catch (Exception e) {
                                    // If Coinbase app not available, open in browser
                                    Log.d("MainActivity", "Coinbase app not available, opening in browser");
                                    extIntent.setPackage(null);
                                    startActivity(extIntent);
                                }
                            } catch (Exception e) {
                                Log.e("MainActivity", "Failed to handle Coinbase URL", e);
                            }
                            return true;
                        }
                        
                        // Intercept our internal bridge URL used by injected JS
                        if (url.startsWith("https://__native_bridge__/open")) {
                            Uri u = request.getUrl();
                            String target = u.getQueryParameter("u");
                            if (target != null) {
                                String decoded = Uri.decode(target);
                                Log.d("MainActivity", "Native bridge requested open: " + decoded);
                                if (tryHandleDeepLink(decoded)) return true;
                            }
                            return true;
                        }
                        
                        // Handle crops data bridge URL
                        if (url.startsWith("https://__native_bridge__/crops")) {
                            Uri u = request.getUrl();
                            String dataJson = u.getQueryParameter("data");
                            
                            if (dataJson != null) {
                                try {
                                    Log.i("NOTIFICATION_DEBUG", "🌾 Received crops data: " + dataJson);
                                    
                                    // Parse crop data: Format is ["CropName@plantedAt#cropId", ...]
                                    String decoded = Uri.decode(dataJson);
                                    decoded = decoded.replace("[", "").replace("]", "").replace("\"", "");
                                    String[] crops = decoded.split(",");
                                    
                                    long now = System.currentTimeMillis();
                                    int notificationCount = 0;
                                    
                                    for (String cropStr : crops) {
                                        if (cropStr.trim().isEmpty()) continue;
                                        
                                        String[] parts = cropStr.split("@");
                                        if (parts.length >= 2) {
                                            String cropName = parts[0].trim();
                                            String[] timeParts = parts[1].split("#");
                                            long plantedAt = Long.parseLong(timeParts[0].trim());
                                            
                                            Long growthTime = Constants.getCropGrowthTime(cropName);
                                            if (growthTime != null) {
                                                long readyAt = plantedAt + growthTime;
                                                long timeUntilReady = readyAt - now;
                                                
                                                if (timeUntilReady > 0 && timeUntilReady < 86400000) { // Within 24 hours
                                                    notificationCount++;
                                                    int notifId = (int)(plantedAt % 100000);
                                                    
                                                    String durationStr = Constants.formatDuration(timeUntilReady);
                                                    Log.i("NOTIFICATION_DEBUG", String.format(
                                                        "🌱 Scheduling %s notification in %s (ID: %d)",
                                                        cropName, durationStr, notifId
                                                    ));
                                                    
                                                    scheduleNotification(notifId, 
                                                        "🌻 " + cropName + " Ready!", 
                                                        "Your " + cropName + " is ready to harvest!", 
                                                        timeUntilReady);
                                                } else if (timeUntilReady < 0) {
                                                    Log.i("NOTIFICATION_DEBUG", String.format(
                                                        "⏰ %s already ready (planted %s ago)",
                                                        cropName, Constants.formatDuration(-timeUntilReady)
                                                    ));
                                                } else {
                                                    Log.i("NOTIFICATION_DEBUG", String.format(
                                                        "⏱️  %s too far in future (%s)",
                                                        cropName, Constants.formatDuration(timeUntilReady)
                                                    ));
                                                }
                                            } else {
                                                Log.w("NOTIFICATION_DEBUG", "❓ Unknown crop type: " + cropName);
                                            }
                                        }
                                    }
                                    
                                    Log.i("NOTIFICATION_DEBUG", "✅ Scheduled " + notificationCount + " crop notifications");
                                    
                                } catch (Exception e) {
                                    Log.e("NOTIFICATION_DEBUG", "❌ Error processing crops: " + e.getMessage());
                                    e.printStackTrace();
                                }
                            }
                            return true;
                        }
                        
                        // Handle notification bridge URL
                        if (url.startsWith("https://__native_bridge__/notification")) {
                            Uri u = request.getUrl();
                            String title = u.getQueryParameter("title");
                            String body = u.getQueryParameter("body");
                            String delayStr = u.getQueryParameter("delay");
                            
                            if (title != null && body != null && delayStr != null) {
                                try {
                                    long delay = Long.parseLong(delayStr);
                                    Log.i("NOTIFICATION_DEBUG", "🔔 Scheduling notification: " + title + " - " + body + " in " + (delay/1000) + "s");
                                    
                                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                        Log.i("NOTIFICATION_DEBUG", "📲 Sending notification now: " + title);
                                        getBridge().getWebView().evaluateJavascript(
                                            "if(window.Capacitor && window.Capacitor.Plugins && window.Capacitor.Plugins.LocalNotifications) {" +
                                            "  window.Capacitor.Plugins.LocalNotifications.schedule({" +
                                            "    notifications: [{" +
                                            "      id: " + System.currentTimeMillis() + "," +
                                            "      title: '" + title.replace("'", "\\'") + "'," +
                                            "      body: '" + body.replace("'", "\\'") + "'," +
                                            "      schedule: { at: new Date(Date.now() + 1000) }" +
                                            "    }]" +
                                            "  }).then(() => console.log('✅ Farm notification sent'))" +
                                            "   .catch(e => console.error('❌ Farm notification failed:', e));" +
                                            "}", null);
                                    }, delay);
                                    
                                } catch (NumberFormatException e) {
                                    Log.e("NOTIFICATION_DEBUG", "❌ Invalid delay parameter: " + delayStr);
                                }
                            }
                            return true;
                        }
                        
                        // Handle console log bridge URL
                        if (url.startsWith("https://__native_bridge__/log")) {
                            Uri u = request.getUrl();
                            String msg = u.getQueryParameter("msg");
                            if (msg != null) {
                                Log.i("JS_CONSOLE", Uri.decode(msg));
                            }
                            return true;
                        }
                        
                        boolean handled = tryHandleDeepLink(url);
                        Log.d("MainActivity", "shouldOverrideUrlLoading result: " + handled + " for URL: " + url);
                        return handled;
                    }
                    @Override
                    public void onLoadResource(WebView view, String url) {
                        Log.d("MainActivity", "Loading resource: " + url);
                    }

                    @Override
                    public android.webkit.WebResourceResponse shouldInterceptRequest(WebView view, android.webkit.WebResourceRequest request) {
                        String url = request.getUrl().toString();
                        
                        // ALWAYS LOG WHEN THIS METHOD IS CALLED
                        Log.i("NOTIFICATION_DEBUG", "INTERCEPT_CALLED: " + url);
                        Log.d("MainActivity", "shouldInterceptRequest(request): " + url);
                        
                        // LOG ALL NETWORK REQUESTS TO SEE EVERYTHING
                        String requestMethod = request.getMethod();
                        Log.i("NOTIFICATION_DEBUG", "ALL_NETWORK: " + requestMethod + " " + url);
                        
                        // Log ANY Sunflower Land API calls
                        if (url.contains("sunflower-land.com")) {
                            Log.i("NOTIFICATION_DEBUG", "🌍 SUNFLOWER API CALL DETECTED: " + url);
                        }
                        
                        // Monitor session API calls specifically
                        if (url.contains("api.sunflower-land.com/session")) {
                            Log.i("NOTIFICATION_DEBUG", "🎯 SESSION API DETECTED: TRUE - " + url);
                            Log.i("NOTIFICATION_DEBUG", "🌐 API CALL INTERCEPTED: TRUE");
                            // Log the POST request details
                            String method = request.getMethod();
                            Log.i("NOTIFICATION_DEBUG", "🔍 SESSION METHOD: " + method);
                            Log.i("NOTIFICATION_DEBUG", "� SESSION HEADERS: " + request.getRequestHeaders().toString());

                        }
                        
                        // Intercept Coinbase URLs and prevent internal loading
                        if (url.contains("keys.coinbase.com") || url.contains("coinbase.com/connect")) {
                            Log.d("MainActivity", "*** INTERCEPTED COINBASE URL in shouldInterceptRequest: " + url);
                            Log.d("MainActivity", "Routing to external app");
                            try {
                                Intent extIntent = new Intent(Intent.ACTION_VIEW);
                                extIntent.setData(Uri.parse(url));
                                extIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                try {
                                    extIntent.setPackage("com.coinbase.android"); // Coinbase package
                                    startActivity(extIntent);
                                } catch (Exception e) {
                                    Log.d("MainActivity", "Coinbase app not available, opening in browser");
                                    extIntent.setPackage(null);
                                    startActivity(extIntent);
                                }
                            } catch (Exception e) {
                                Log.e("MainActivity", "Failed to handle Coinbase URL in shouldInterceptRequest", e);
                            }
                            // Return empty response to prevent WebView from loading
                            return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("".getBytes()));
                        }
                        
                        if (url != null) {
                            String scheme = request.getUrl().getScheme();
                            if (scheme != null && !scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https") && tryHandleDeepLink(url)) {
                                // Return an empty response to prevent WebView from attempting to load the unknown scheme
                                return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("".getBytes()));
                            }
                        }
                        return super.shouldInterceptRequest(view, request);
                    }

                    @Override
                    public android.webkit.WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                        Log.d("MainActivity", "shouldInterceptRequest(string): " + url);
                        if (url != null) {
                            String scheme = Uri.parse(url).getScheme();
                            if (scheme != null && !scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https") && tryHandleDeepLink(url)) {
                                return new WebResourceResponse("text/plain", "utf-8", new ByteArrayInputStream("".getBytes()));
                            }
                        }
                        return super.shouldInterceptRequest(view, url);
                    }
                });

                Log.d("MainActivity", "WebView Debug:"
                    + "\nHardware Acceleration: " + wv.isHardwareAccelerated()
                    + "\nLayer Type: " + wv.getLayerType()
                    + "\nJS Enabled: " + ws.getJavaScriptEnabled()
                    + "\nDOM Storage: " + ws.getDomStorageEnabled()
                    + "\nDatabase: " + ws.getDatabaseEnabled());
            } else {
                Log.w("MainActivity", "Could not obtain underlying WebView instance to configure settings");
            }
        } catch (Exception e) {
            Log.w("MainActivity", "Failed to configure WebView settings", e);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.d("MainActivity", "onNewIntent called with action: " + (intent != null ? intent.getAction() : "null"));
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void scheduleNotification(int id, String title, String body, long delayMs) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                WebView webView = getBridge().getWebView();
                if (webView != null) {
                    webView.evaluateJavascript(
                        "if(window.Capacitor && window.Capacitor.Plugins && window.Capacitor.Plugins.LocalNotifications) {" +
                        "  window.Capacitor.Plugins.LocalNotifications.schedule({" +
                        "    notifications: [{" +
                        "      id: " + id + "," +
                        "      title: '" + title.replace("'", "\\'") + "'," +
                        "      body: '" + body.replace("'", "\\'") + "'," +
                        "      schedule: { at: new Date(Date.now() + 1000) }" +
                        "    }]" +
                        "  }).then(() => console.log('✅ Notification sent: " + title + "'))" +
                        "   .catch(e => console.error('❌ Notification failed:', e));" +
                        "}", null);
                }
            } catch (Exception e) {
                Log.e("NOTIFICATION_DEBUG", "❌ Error sending notification: " + e.getMessage());
            }
        }, delayMs);
    }

    /**
     * Auto-start the notification manager if autostart is enabled and service is not already running
     */
    private void autoStartNotificationManager() {
        try {
            android.content.SharedPreferences prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this);
            boolean autostartEnabled = prefs.getBoolean("autostart_manager", false);
            
            DebugLog.log("MainActivity.autoStartNotificationManager() - autostart_enabled=" + autostartEnabled);
            
            if (autostartEnabled) {
                // Check if farm ID and API key are configured
                String farmId = prefs.getString("farm_id", "");
                String apiKey = prefs.getString("api_key", "");
                
                if (farmId != null && !farmId.isEmpty() && apiKey != null && !apiKey.isEmpty()) {
                    // Check if WorkManager is already scheduled
                    if (WorkManagerHelper.isNotificationWorkerScheduled(this)) {
                        Log.d("MainActivity", "✅ WorkManager already scheduled - skipping duplicate schedule");
                        DebugLog.log("WorkManager already scheduled - skipping duplicate");
                        WorkManagerHelper.logCurrentRefreshInterval(this);
                    } else {
                        // Schedule WorkManager with "auto" source (30-second delay for app load)
                        Log.d("MainActivity", "Scheduling WorkManager for the first time (auto-start from MainActivity)");
                        DebugLog.log("App startup: Scheduling WorkManager with 30-second delay (auto-start)");
                        if (WorkManagerHelper.scheduleNotificationWorkerWithSource(this, "auto")) {
                            Log.d("MainActivity", "✅ WorkManager scheduled successfully on app startup (with 30-second delay)");
                            DebugLog.log("✅ WorkManager scheduled successfully (auto-start)");
                        } else {
                            Log.e("MainActivity", "❌ Failed to schedule WorkManager on app startup");
                            DebugLog.error("Failed to schedule WorkManager on app startup", null);
                        }
                    }
                } else {
                    Log.d("MainActivity", "Auto-start skipped: Farm ID or API key not configured");
                    DebugLog.warning("Auto-start skipped: Farm ID or API key not configured");
                }
            } else {
                DebugLog.log("Auto-start disabled by user preference");
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error in auto-start: " + e.getMessage(), e);
            DebugLog.error("Error in autoStartNotificationManager", e);
        }
    }

    /**
     * Check if we have all required permissions for starting the foreground service
     */
    private boolean hasRequiredPermissionsForForegroundService() {
        // Check POST_NOTIFICATIONS permission (required for showing notifications)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Missing POST_NOTIFICATIONS permission");
                return false;
            }
        }
        
        // Check SCHEDULE_EXACT_ALARM permission (required for scheduling notifications)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SCHEDULE_EXACT_ALARM)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Missing SCHEDULE_EXACT_ALARM permission");
                return false;
            }
        }
        
        // Check FOREGROUND_SERVICE_DATA_SYNC permission (required for foreground service on Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, "android.permission.FOREGROUND_SERVICE_DATA_SYNC")
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Missing FOREGROUND_SERVICE_DATA_SYNC permission");
                return false;
            }
        }
        
        Log.d("MainActivity", "All required permissions are granted");
        return true;
    }

    /**
     * Open the notification settings page
     */
    private void openNotificationSettings() {
        try {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            settingsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(settingsIntent);
            Log.d("MainActivity", "Opened notification settings");
        } catch (Exception e) {
            Log.e("MainActivity", "Error opening notification settings: " + e.getMessage(), e);
        }
    }

    private void handleIntent(Intent intent) {
        Log.d("MainActivity", "handleIntent called with action: " + (intent != null ? intent.getAction() : "null"));
        String action = intent.getAction();
        Uri data = intent.getData();
        Log.d("MainActivity", "Intent data: " + (data != null ? data.toString() : "null"));

        // Handle opening notification settings from the foreground notification click
        if ("com.sunflowerland.mobile.OPEN_NOTIFICATION_SETTINGS".equals(action)) {
            Log.d("MainActivity", "Opening notification settings from foreground notification");
            openNotificationSettings();
            return;
        }

        if (Intent.ACTION_VIEW.equals(action) && data != null) {
            String scheme = data.getScheme();
            String uri = data.toString();
            
            // All wallet schemes should be routed through tryHandleDeepLink first
            // This ensures we route them to the correct wallet app if available
            if (tryHandleDeepLink(uri)) {
                Log.d("MainActivity", "Successfully routed wallet scheme to external app");
                return;
            }

            // Separate handling for WalletConnect, MetaMask, and Sunflowerland deep links
            String escaped = uri.replace("'", "\\'");
            String js = null;
            if ("wc".equals(scheme)) {
                Log.d("MainActivity", "Routing wc:// scheme to WebView for WalletConnect handling");
                js = "window.dispatchEvent(new CustomEvent('walletconnectDeepLink', { detail: '" + escaped + "' }));";
            } else if ("metamask".equals(scheme)) {
                Log.d("MainActivity", "Routing metamask:// scheme to WebView for MetaMask callback handling");
                js = "window.dispatchEvent(new CustomEvent('metamaskDeepLink', { detail: '" + escaped + "' }));";
            } else if ("sunflowerland".equals(scheme)) {
                Log.d("MainActivity", "Routing sunflowerland:// deep link to WebView for login callback");
                js = "window.dispatchEvent(new CustomEvent('sunflowerlandDeepLink', { detail: '" + escaped + "' }));";
            } else {
                // For other wallet schemes that couldn't be routed externally,
                // try to pass them through the WebView in case the game/web can handle them
                Log.d("MainActivity", "Unhandled wallet scheme, attempting WebView fallback: " + scheme);
                js = "window.dispatchEvent(new CustomEvent('walletDeepLink', { detail: '" + escaped + "' }));";
            }

            if (js != null) {
                Log.d("MainActivity", "Forwarding deep link to WebView: " + uri);
                bridge.eval(js, new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        // no-op
                    }
                });
            }
        }
    }

    // Try to handle deep link URLs by launching an external wallet app (not this app).
    private boolean tryHandleDeepLink(String url) {
        if (url == null) return false;

        try {
            Uri parsed = Uri.parse(url);
            String scheme = parsed.getScheme();
            if (scheme == null) return false;

            // If it's a normal http(s) URL, don't handle here
            if (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https")) return false;

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            PackageManager pm = getPackageManager();
            List<ResolveInfo> handlers = pm.queryIntentActivities(intent, 0);
            String myPackage = getPackageName();
            Log.d("MainActivity", "Query handlers for scheme " + scheme + ": found " + (handlers != null ? handlers.size() : 0) + " handlers");

            // Known wallet scheme -> package mappings
            // This is a comprehensive list of popular wallet apps and their package names
            java.util.HashMap<String, String> schemeToPackage = new java.util.HashMap<>();
            
            // Ronin/Axie Infinity ecosystem
            schemeToPackage.put("roninwallet", "com.skymavis.genesis");
            
            // Binance
            schemeToPackage.put("bnc", "com.binance.us");
            schemeToPackage.put("bncus", "com.binance.us");
            schemeToPackage.put("binance", "com.binance.us");
            
            // MetaMask
            schemeToPackage.put("metamask", "io.metamask");
            
            // Trust Wallet
            schemeToPackage.put("trust", "com.wallet.crypto.trustapp");
            schemeToPackage.put("trustwallet", "com.wallet.crypto.trustapp");
            
            // Coinbase
            schemeToPackage.put("coinbase", "com.coinbase.android");
            
            // Rainbow Wallet
            schemeToPackage.put("rainbow", "me.rainbow");
            
            // Argent
            schemeToPackage.put("argent", "im.argent");
            
            // Ledger Live
            schemeToPackage.put("ledger", "com.ledger.live");
            schemeToPackage.put("ledgerlive", "com.ledger.live");
            
            // Zerion
            schemeToPackage.put("zerion", "io.zerion.android");
            
            // dYdX
            schemeToPackage.put("dydx", "com.dydx");
            
            // Uniswap
            schemeToPackage.put("uniswap", "com.uniswap.mobile");
            
            // Phantom (Solana wallet, also supports EVM)
            schemeToPackage.put("phantom", "app.phantom");
            
            // Solflare (Solana wallet)
            schemeToPackage.put("solflare", "com.solflare.mobile");
            
            // OKX Wallet
            schemeToPackage.put("okx", "com.okinc.okex.gp");
            schemeToPackage.put("okex", "com.okinc.okex.gp");
            schemeToPackage.put("okexwallet", "com.okinc.okex.gp");
            
            // Keplr (Cosmos wallet)
            schemeToPackage.put("keplr", "com.chainapsis.keplr");
            
            // Exodus
            schemeToPackage.put("exodus", "com.exodus");
            
            // Rabby Wallet
            schemeToPackage.put("rabby", "com.debank.rabbymobile");
            schemeToPackage.put("debank", "com.debank.rabbymobile");
            
            // 1inch Wallet
            schemeToPackage.put("1inch", "io.oneinch.app");
            
            // BitPay Wallet
            schemeToPackage.put("bitpay", "com.bitpay");
            
            // Best Wallet
            schemeToPackage.put("bestwallet", "io.bestwallet");
            
            // Fireblocks
            schemeToPackage.put("fireblocks", "com.fireblocks.mobile");
            
            // Bitget Wallet
            schemeToPackage.put("bitget", "com.bitget.wallet");
            
            // TokenPocket
            schemeToPackage.put("tokenpocket", "com.tokenpocket.tp");
            
            // SafePal
            schemeToPackage.put("safepal", "io.safepal.wallet");
            
            // Xportal
            schemeToPackage.put("xportal", "com.xportal.app");
            
            // Crypto.com
            schemeToPackage.put("crypto", "com.crypto.app");
            
            // Bitcoin.com Wallet
            schemeToPackage.put("bitcoincom", "com.bitcoin.wallet");
            schemeToPackage.put("bitcoin", "com.bitcoin.wallet");
            
            // Bitfrost Wallet
            schemeToPackage.put("bitfrost", "com.bitfrost.wallet");
            
            // Rakuten Wallet
            schemeToPackage.put("rakuten", "com.rakuten.wallet");
            
            // Bybit Wallet
            schemeToPackage.put("bybit", "com.bybit.wallet");
            
            // Blockchain.com
            schemeToPackage.put("blockchaincom", "com.blockchain.wallet");
            schemeToPackage.put("blockchain", "com.blockchain.wallet");
            
            // Trezor Suite
            schemeToPackage.put("trezor", "io.trezor.suite");
            
            // imToken
            schemeToPackage.put("imtoken", "io.imtoken.app");
            
            // Safe (formerly Gnosis Safe)
            schemeToPackage.put("safe", "io.gnosis.safe");
            
            // Gemini
            schemeToPackage.put("gemini", "com.gemini");
            
            // Ctrl Wallet
            schemeToPackage.put("ctrl", "com.ctrlwallet");
            
            // Arculus Wallet
            schemeToPackage.put("arculus", "com.arculus.wallet");
            
            // Generic wallet fallback
            schemeToPackage.put("wallet", "io.metamask");
            schemeToPackage.put("walletconnect", "io.metamask");
            
            // Preferred keywords in priority order
            // These are used to match wallet packages by name when exact scheme mapping isn't available
            ArrayList<String> preferredKeywords = new ArrayList<>(Arrays.asList(
                "ronin",
                "skymavis",
                "binance",
                "trust",
                "trustwallet",
                "rainbow",
                "argent",
                "ledger",
                "zerion",
                "dydx",
                "coinbase",
                "toshi",
                "uniswap",
                "phantom",
                "solflare",
                "okx",
                "keplr",
                "exodus",
                "rabby",
                "debank",
                "1inch",
                "bitpay",
                "bestwallet",
                "fireblocks",
                "bitget",
                "tokenpocket",
                "safepal",
                "xportal",
                "bitcoincom",
                "bitcoin",
                "bitfrost",
                "rakuten",
                "bybit",
                "blockchaincom",
                "blockchain",
                "trezor",
                "imtoken",
                "safe",
                "gemini",
                "ctrl",
                "arculus",
                "telegram",
                "crypto",
                "crypto.com",
                "robinhood",
                "wallet",
                "walletconnect",
                "metamask" // MetaMask is last priority since it handles many schemes
            ));
            
            // Try exact package match for scheme first (only if we have the exact mapping)
            String targetPackage = schemeToPackage.get(scheme.toLowerCase());
            Log.d("MainActivity", "URL scheme: " + scheme + " (lowercase: " + scheme.toLowerCase() + "), Direct package lookup: " + targetPackage);
            Log.d("MainActivity", "Full URL: " + url);
            
            // If no exact mapping, filter handlers by keywords to find the best match
            if (targetPackage == null && handlers != null && !handlers.isEmpty()) {
                ArrayList<String> candidatePkgs = new ArrayList<>();
                for (ResolveInfo ri : handlers) {
                    String pkg = ri.activityInfo.packageName;
                    if (!myPackage.equals(pkg)) candidatePkgs.add(pkg);
                }
                Log.d("MainActivity", "Available handlers for scheme " + scheme + ": " + candidatePkgs.toString());
                
                // Try to find best match by keyword
                outer:
                for (String kw : preferredKeywords) {
                    for (String pkg : candidatePkgs) {
                        if (pkg.toLowerCase().contains(kw.toLowerCase())) {
                            targetPackage = pkg;
                            Log.d("MainActivity", "Matched keyword '" + kw + "' to package: " + targetPackage);
                            break outer;
                        }
                    }
                }
                
                // If no keyword match, use first available
                if (targetPackage == null && !candidatePkgs.isEmpty()) {
                    targetPackage = candidatePkgs.get(0);
                    Log.d("MainActivity", "No keyword match, using first handler: " + targetPackage);
                }
            }
            
            // If no handler was found, try fallback methods
            if (targetPackage == null) {

                if (handlers != null && !handlers.isEmpty()) {
                    ArrayList<String> candidateLog = new ArrayList<>();
                    ArrayList<String> candidatePkgs = new ArrayList<>();
                    for (ResolveInfo ri : handlers) {
                        String pkg = ri.activityInfo.packageName;
                        candidateLog.add(pkg);
                        if (!myPackage.equals(pkg)) candidatePkgs.add(pkg);
                    }
                    Log.d("MainActivity", "Candidate handler packages: " + candidateLog.toString());

                    // Try preferred keywords
                    outer:
                    for (String kw : preferredKeywords) {
                        for (String pkg : candidatePkgs) {
                            if (pkg.toLowerCase().contains(kw.toLowerCase())) {
                                targetPackage = pkg;
                                break outer;
                            }
                        }
                    }

                    if (targetPackage == null && !candidatePkgs.isEmpty()) {
                        targetPackage = candidatePkgs.get(0);
                    }
                }
            }

            // Launch the intent with the target package if found
            if (targetPackage != null) {
                intent.setPackage(targetPackage);
                Log.d("MainActivity", "Launching " + targetPackage + " with URL: " + url);
                try {
                    startActivity(intent);
                    return true;
                } catch (Exception e) {
                    Log.e("MainActivity", "Failed to launch " + targetPackage, e);
                }
            }

            // If no direct handler: try parsing intent:// URIs
            if (url.startsWith("intent://")) {
                try {
                    Intent parsedIntent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
                    if (parsedIntent.getPackage() != null && !parsedIntent.getPackage().equals(myPackage)) {
                        startActivity(parsedIntent);
                        return true;
                    }
                } catch (Exception ex) {
                    Log.w("MainActivity", "Failed to parse intent:// URI", ex);
                }
            }

            // If the URL contains an embedded WalletConnect URI (common for wallet bridges), extract and try universal links
            String embedded = parsed.getQueryParameter("uri");
            String toEncode = null;
            if (embedded != null && (embedded.startsWith("wc:") || embedded.contains("wc%3A") || embedded.contains("wc:"))) {
                // If it's percent-encoded (wc%3A...), decode it first
                try {
                    String decoded = Uri.decode(embedded);
                    if (decoded.startsWith("wc:")) toEncode = decoded;
                    else toEncode = embedded; // fallback
                } catch (Exception ex) {
                    toEncode = embedded;
                }
            } else if (url.startsWith("wc:") || url.startsWith("metamask:")) {
                toEncode = url;
            }

            if (toEncode != null) {
                try {
                    String encoded = URLEncoder.encode(toEncode, "UTF-8");
                    ArrayList<String> appLinks = new ArrayList<>(Arrays.asList(
                        "https://metamask.app.link/wc?uri=",
                        "https://link.trustwallet.com/wc?uri=",
                        "https://walletconnect.com/wc?uri="
                    ));
                    for (String prefix : appLinks) {
                        try {
                            Intent metaIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(prefix + encoded));
                            metaIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(metaIntent);
                            return true;
                        } catch (Exception ex) {
                            Log.w("MainActivity", "Universal link fallback failed for " + prefix, ex);
                        }
                    }
                } catch (UnsupportedEncodingException uee) {
                    Log.w("MainActivity", "Failed to encode WC URI", uee);
                }
            }

            // Final fallback: Route to the specific wallet app's Play Store page based on scheme
            // Build a reverse mapping of scheme -> package for Play Store lookup
            java.util.HashMap<String, String> schemeToPlayStoreId = new java.util.HashMap<>();
            schemeToPlayStoreId.put("roninwallet", "com.skymavis.genesis");
            schemeToPlayStoreId.put("bnc", "com.binance.us");
            schemeToPlayStoreId.put("bncus", "com.binance.us");
            schemeToPlayStoreId.put("binance", "com.binance.us");
            schemeToPlayStoreId.put("metamask", "io.metamask");
            schemeToPlayStoreId.put("trust", "com.wallet.crypto.trustapp");
            schemeToPlayStoreId.put("trustwallet", "com.wallet.crypto.trustapp");
            schemeToPlayStoreId.put("coinbase", "com.coinbase.android");
            schemeToPlayStoreId.put("rainbow", "me.rainbow");
            schemeToPlayStoreId.put("argent", "im.argent");
            schemeToPlayStoreId.put("ledger", "com.ledger.live");
            schemeToPlayStoreId.put("ledgerlive", "com.ledger.live");
            schemeToPlayStoreId.put("zerion", "io.zerion.android");
            schemeToPlayStoreId.put("dydx", "com.dydx");
            schemeToPlayStoreId.put("uniswap", "com.uniswap.mobile");
            schemeToPlayStoreId.put("phantom", "app.phantom");
            schemeToPlayStoreId.put("solflare", "com.solflare.mobile");
            schemeToPlayStoreId.put("okx", "com.okinc.okex.gp");
            schemeToPlayStoreId.put("okex", "com.okinc.okex.gp");
            schemeToPlayStoreId.put("okexwallet", "com.okinc.okex.gp");
            schemeToPlayStoreId.put("keplr", "com.chainapsis.keplr");
            schemeToPlayStoreId.put("exodus", "com.exodus");
            schemeToPlayStoreId.put("rabby", "com.debank.rabbymobile");
            schemeToPlayStoreId.put("debank", "com.debank.rabbymobile");
            schemeToPlayStoreId.put("1inch", "io.oneinch.app");
            schemeToPlayStoreId.put("bitpay", "com.bitpay");
            schemeToPlayStoreId.put("bestwallet", "io.bestwallet");
            schemeToPlayStoreId.put("fireblocks", "com.fireblocks.mobile");
            schemeToPlayStoreId.put("bitget", "com.bitget.wallet");
            schemeToPlayStoreId.put("tokenpocket", "com.tokenpocket.tp");
            schemeToPlayStoreId.put("safepal", "io.safepal.wallet");
            schemeToPlayStoreId.put("xportal", "com.xportal.app");
            schemeToPlayStoreId.put("crypto", "com.crypto.app");
            schemeToPlayStoreId.put("bitcoincom", "com.bitcoin.wallet");
            schemeToPlayStoreId.put("bitcoin", "com.bitcoin.wallet");
            schemeToPlayStoreId.put("bitfrost", "com.bitfrost.wallet");
            schemeToPlayStoreId.put("rakuten", "com.rakuten.wallet");
            schemeToPlayStoreId.put("bybit", "com.bybit.wallet");
            schemeToPlayStoreId.put("blockchaincom", "com.blockchain.wallet");
            schemeToPlayStoreId.put("blockchain", "com.blockchain.wallet");
            schemeToPlayStoreId.put("trezor", "io.trezor.suite");
            schemeToPlayStoreId.put("imtoken", "io.imtoken.app");
            schemeToPlayStoreId.put("safe", "io.gnosis.safe");
            schemeToPlayStoreId.put("gemini", "com.gemini");
            schemeToPlayStoreId.put("ctrl", "com.ctrlwallet");
            schemeToPlayStoreId.put("arculus", "com.arculus.wallet");
            
            // Only open Play Store if we have a specific app for this scheme
            String playStoreId = schemeToPlayStoreId.get(scheme.toLowerCase());
            if (playStoreId != null) {
                Log.d("MainActivity", "App not found for scheme " + scheme + ", opening Play Store for: " + playStoreId);
                try {
                    Intent store = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + playStoreId));
                    store.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(store);
                    return true;
                } catch (Exception ex) {
                    Log.w("MainActivity", "Failed to open Play Store for " + playStoreId, ex);
                }
            } else {
                Log.w("MainActivity", "No Play Store entry found for scheme: " + scheme);
            }

            Log.w("MainActivity", "No external handler found for deep link: " + url);
        } catch (Exception e) {
            Log.e("MainActivity", "Error handling deep link: " + url, e);
        }

        return false;
    }

    // Attempt to locate the Capacitor WebView instance and attach our clients again.
    private void attachDeepLinkHandlers() {
        if (deepLinkHandlersAttached) return;
        try {
            Object webViewObj = null;
            try { webViewObj = this.bridge.getWebView(); } catch (Exception e) { webViewObj = null; }

            WebView wv = null;
            if (webViewObj instanceof WebView) {
                wv = (WebView) webViewObj;
            } else if (webViewObj != null) {
                try {
                    Object engine = null;
                    try { engine = webViewObj.getClass().getMethod("getEngine").invoke(webViewObj); } catch (NoSuchMethodException nsme) { }
                    if (engine != null) {
                        try { Object view = engine.getClass().getMethod("getView").invoke(engine); if (view instanceof WebView) wv = (WebView)view; } catch (Exception ex) { }
                    }
                    if (wv == null) {
                        try { Object view2 = webViewObj.getClass().getMethod("getView").invoke(webViewObj); if (view2 instanceof WebView) wv = (WebView)view2; } catch (Exception ex) {}
                    }
                } catch (Exception ex) { Log.w("MainActivity", "attachDeepLinkHandlers reflection failed", ex); }
            }

            if (wv != null) {
                Log.d("MainActivity", "Attaching WebChromeClient (WebViewClient already set in setupJavaScriptInterface)");
                
                // WebViewClient is already unified in setupJavaScriptInterface - only set WebChromeClient here for popup handling
                wv.setWebChromeClient(new android.webkit.WebChromeClient() {
                    @Override
                    public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                        // Create a new WebView for the popup window
                        WebView popupWebView = new WebView(view.getContext());
                        popupWebView.getSettings().setJavaScriptEnabled(true);
                        
                        popupWebView.setWebViewClient(new android.webkit.WebViewClient() {
                            @Override
                            public boolean shouldOverrideUrlLoading(WebView w, WebResourceRequest request) {
                                String url = request.getUrl().toString();
                                Log.d("MainActivity", "POPUP WebView loading: " + url);
                                
                                // Block Coinbase URLs from loading in the popup
                                if (url.contains("keys.coinbase.com") || url.contains("coinbase.com") || url.contains("login.coinbase")) {
                                    Log.d("MainActivity", "BLOCKED: Coinbase URL in popup: " + url);
                                    return true;  // Block the load
                                }
                                
                                return false;  // Allow other URLs
                            }
                        });
                        
                        // Attach the popup to the transport so it opens as a separate window
                        WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                        transport.setWebView(popupWebView);
                        resultMsg.sendToTarget();
                        return true;
                    }
                });

                deepLinkHandlersAttached = true;
            } else {
                Log.d("MainActivity", "attachDeepLinkHandlers: WebView instance not found yet");
            }
        } catch (Exception e) {
            Log.w("MainActivity", "attachDeepLinkHandlers failed", e);
        }
    }
    
    @Override
    public void onBackPressed() {
        // Check if WebView can go back in history
        if (mainWebView != null && mainWebView.canGoBack()) {
            Log.d("MainActivity", "Going back in WebView history");
            mainWebView.goBack();
        } else {
            // If no WebView history, perform default back behavior (exit app)
            Log.d("MainActivity", "No WebView history, performing default back action");
            super.onBackPressed();
        }
    }
    
    // Method to manually extract all game data
    public void extractGameData() {
        if (mainWebView != null) {
            Log.d("GAME_EXTRACT", "Manually extracting game data...");
            String extractScript = 
                "console.log('🔥 MANUAL DATA EXTRACTION 🔥');" +
                "try {" +
                "  var allData = {};" +
                "  allData.localStorage = {};" +
                "  for(var i = 0; i < localStorage.length; i++) {" +
                "    var k = localStorage.key(i);" +
                "    allData.localStorage[k] = localStorage.getItem(k);" +
                "  }" +
                "  allData.sessionStorage = {};" +
                "  for(var i = 0; i < sessionStorage.length; i++) {" +
                "    var k = sessionStorage.key(i);" +
                "    allData.sessionStorage[k] = sessionStorage.getItem(k);" +
                "  }" +
                "  allData.cookies = document.cookie;" +
                "  console.log('🔥 ALL_GAME_DATA:', JSON.stringify(allData));" +
                "} catch(e) { console.error('Extract failed:', e); }";
            
            mainWebView.evaluateJavascript(extractScript, null);
        }
    }

    /**
     * Show first-launch dialog on first app open
     * Dialog informs user to set Farm ID and API key in settings
     */
    private void showFirstLaunchDialog() {
        android.content.SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean hasSeenDialog = prefs.getBoolean("has_seen_first_launch_dialog", false);
        
        if (!hasSeenDialog) {
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
            builder.setMessage("Set your Farm ID and Farm API-key in settings to receive push notifications to your device")
                   .setCancelable(false)
                   .setNegativeButton("Settings", (dialog, id) -> {
                       // Mark dialog as seen
                       android.content.SharedPreferences.Editor editor = prefs.edit();
                       editor.putBoolean("has_seen_first_launch_dialog", true);
                       editor.apply();
                       
                       // Go to settings
                       Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                       startActivity(settingsIntent);
                   })
                   .setPositiveButton("Continue", (dialog, id) -> {
                       // Mark dialog as seen
                       android.content.SharedPreferences.Editor editor = prefs.edit();
                       editor.putBoolean("has_seen_first_launch_dialog", true);
                       editor.apply();
                       
                       dialog.cancel();
                   });
            
            androidx.appcompat.app.AlertDialog alert = builder.create();
            alert.show();
        }
    }
}

