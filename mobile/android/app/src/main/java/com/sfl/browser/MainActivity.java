package com.sfl.browser;

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
import android.graphics.Canvas;
import android.webkit.WebResourceResponse;
import java.io.ByteArrayInputStream;
import android.os.Handler;
import android.os.Looper;
import android.webkit.WebResourceRequest;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.Manifest;
import android.view.View;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import android.text.TextWatcher;
import android.text.Editable;
import android.os.Build;
import android.view.WindowManager;
import androidx.core.view.WindowCompat;
import android.view.GestureDetector;
import android.view.MotionEvent;

public class MainActivity extends BridgeActivity {

    /**
     * Applies the orientation setting from preferences or a given value.
     * @param value The orientation value ("auto", "portrait", "landscape"). If null, reads from preferences.
     */
    public void applyOrientationSetting(String value) {
        String orientationValue = value;
        if (orientationValue == null) {
            android.content.SharedPreferences prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this);
            orientationValue = prefs.getString("orientation", "portrait");
        }
        int requestedOrientation;
        switch (orientationValue) {
            case "landscape":
                requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                break;
            case "portrait":
                requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                break;
            case "auto":
            default:
                requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
                break;
        }
        setRequestedOrientation(requestedOrientation);
    }

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

    /**
     * Show or hide the system navigation bar (soft keys) in an API-version-aware way.
     * When hiding, the bars can be revealed transiently by swipe.
     */
    private void setSystemNavHidden(boolean hide) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                final android.view.WindowInsetsController insetsController = getWindow().getInsetsController();
                if (insetsController != null) {
                    if (hide) {
                        // Hide both navigation and status bars when requested
                        insetsController.hide(android.view.WindowInsets.Type.navigationBars() | android.view.WindowInsets.Type.statusBars());
                        insetsController.setSystemBarsBehavior(android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                    } else {
                        // Show both navigation and status bars again
                        insetsController.show(android.view.WindowInsets.Type.navigationBars() | android.view.WindowInsets.Type.statusBars());
                    }
                }
            } else {
                final View decor = getWindow().getDecorView();
                int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                if (hide) {
                    flags |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
                }
                decor.setSystemUiVisibility(flags);
            }

        // Remember the current hidden state so the WindowInsets listener can honor it
        try { systemNavHidden = hide; } catch (Exception ignored) { }
        } catch (Exception e) {
            Log.w("MainActivity", "Failed to set system navigation visibility", e);
        }

        // Re-apply insets so our WindowInsets listener recalculates WebView padding/margin
        try {
            View root = findViewById(getResources().getIdentifier("root_coordinator", "id", getPackageName()));
            if (root != null) root.requestApplyInsets();
        } catch (Exception ignored) { }

        // Immediately adjust WebView padding/margin and root top padding for the hide case so UI updates without waiting
        try {
            uiHandler.post(() -> {
                try {
                    if (mainWebView != null) {
                        if (hide) {
                            // Remove bottom inset so WebView can extend to screen bottom
                            mainWebView.setPadding(mainWebView.getPaddingLeft(), mainWebView.getPaddingTop(), mainWebView.getPaddingRight(), 0);
                            try {
                                android.view.ViewGroup.LayoutParams lp = mainWebView.getLayoutParams();
                                if (lp instanceof android.widget.LinearLayout.LayoutParams) {
                                    android.widget.LinearLayout.LayoutParams llp = (android.widget.LinearLayout.LayoutParams) lp;
                                    if (llp.bottomMargin != 0) {
                                        llp.bottomMargin = 0;
                                        mainWebView.setLayoutParams(llp);
                                    }
                                }
                            } catch (Exception ignore) { }
                            // Also clear the root view top padding so the status bar hidden state
                            // immediately allows content to occupy the top of the screen.
                            try {
                                View root = findViewById(getResources().getIdentifier("root_coordinator", "id", getPackageName()));
                                if (root != null) {
                                    int left = root.getPaddingLeft();
                                    int right = root.getPaddingRight();
                                    int bottom = root.getPaddingBottom();
                                    if (root.getPaddingTop() != 0) {
                                        root.setPadding(left, 0, right, bottom);
                                    }
                                }
                            } catch (Exception ignore) { }
                        } else {
                            // When showing nav again, request insets so listener reapplies proper padding/margin
                            View root = findViewById(getResources().getIdentifier("root_coordinator", "id", getPackageName()));
                            if (root != null) root.requestApplyInsets();
                        }
                    }
                } catch (Exception e) {
                    Log.w("MainActivity", "Error adjusting WebView padding/margin post nav-change", e);
                }
            });
        } catch (Exception ignored) { }
    }
    private boolean deepLinkHandlersAttached = false;
    private WebView mainWebView = null;
    // Cache one WebView per tab so switching tabs preserves state instead of reloading
    private WebView[] tabWebViews = new WebView[3];
    // Parent container and insertion index where webviews are attached/replaced
    private android.view.ViewGroup webviewParent = null;
    private int webviewIndexInParent = -1;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 100;
    private static final int AUDIO_PERMISSION_REQUEST_CODE = 101;
    
    private android.widget.LinearLayout browserControlsBar;
    private android.widget.ImageButton btnBack, btnForward;
    private android.widget.EditText urlBar;
    private android.content.SharedPreferences prefs;
    private android.content.SharedPreferences.OnSharedPreferenceChangeListener prefListener;

    private GestureDetector gestureDetector;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private String currentUrlShown = null;
    // Track whether the user is actively editing the url bar to avoid clobbering input
    private boolean urlBarUserEditing = false;
    // Track whether we've hidden the system navigation/status bars so inset handling can respect it
    private boolean systemNavHidden = false;
    // Tab state: 1 = home_page, 2 = home_page_2, 3 = home_page_3
    private int currentTab = 1;
    private static final int MAX_TABS = 3;
    // Track last programmatic URL we asked the WebView to load (for debugging and to avoid overrides)
    private String lastProgrammaticUrl = null;

    private int threeFingerTapCount = 0;
    private long lastThreeFingerTapTime = 0;
    private static final int TRIPLE_TAP_TIMEOUT_MS = 1000;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Initialize preferences once at the top
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        
        // Check if "Only Notifications" mode is enabled - redirect before any UI is loaded
        boolean onlyNotificationsMode = prefs.getBoolean("only_notifications", true);
        boolean bypassOnlyNotifications = getIntent().getBooleanExtra("bypass_notifications_only", false);
        if (onlyNotificationsMode && !bypassOnlyNotifications) {
            Log.d("MainActivity", "Only Notifications mode enabled - redirecting to SettingsActivity");
            // Start SettingsActivity and finish MainActivity
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
            finish();
            return;
        }
        
        // Show debug toast for exact alarm permission if enabled
        boolean showDebugToast = prefs.getBoolean("show_debug_toast", false);
        if (showDebugToast && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            android.app.AlarmManager alarmManager = (android.app.AlarmManager) getSystemService(ALARM_SERVICE);
            boolean canExact = alarmManager != null && alarmManager.canScheduleExactAlarms();
            android.widget.Toast.makeText(this, "Exact Alarms Permission: " + canExact, android.widget.Toast.LENGTH_LONG).show();
        }
        super.onCreate(savedInstanceState);

        // Set up layout and browser controls UI
        setContentView(R.layout.activity_main);
        try {
            View root = findViewById(R.id.root_coordinator);
            if (root != null) {
                root.setFocusable(true);
                root.setFocusableInTouchMode(true);
                root.requestFocus();
            }
            // Hide keyboard on startup to avoid EditText grabbing focus
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        } catch (Exception ignore) { }
        mainWebView = findViewById(R.id.main_webview);
        
        // tabWebViews[0] will be set to the bridge WebView in setupJavaScriptInterface()
        // tabWebViews[1] and [2] will be created on-demand in createTabWebView()
        
        browserControlsBar = findViewById(R.id.browser_controls_bar);
        btnBack = findViewById(R.id.btn_back);
        btnForward = findViewById(R.id.btn_forward);
        urlBar = findViewById(R.id.url_bar);
        
        android.widget.Button btnTabSwitcher = findViewById(R.id.btn_tab_switcher);
        android.widget.ImageButton btnMenu = findViewById(R.id.btn_menu);

        // Wire browser control buttons
        btnBack.setOnClickListener(v -> { if (mainWebView != null && mainWebView.canGoBack()) mainWebView.goBack(); });
        btnForward.setOnClickListener(v -> { if (mainWebView != null && mainWebView.canGoForward()) mainWebView.goForward(); });
        
        // Tab switcher button - show tab overview
        btnTabSwitcher.setOnClickListener(v -> {
            showTabOverview();
        });
        
        // Menu button (3-dot)
        btnMenu.setOnClickListener(v -> {
            android.widget.PopupMenu popup = new android.widget.PopupMenu(MainActivity.this, v);
            popup.getMenu().add(0, 1, 0, "Refresh");
            popup.getMenu().add(0, 2, 1, "Settings");
            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1) {
                    // Call the dedicated refresh method that handles both bridge and regular WebViews
                    refreshCurrentTab();
                    return true;
                } else if (item.getItemId() == 2) {
                    Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                    startActivity(settingsIntent);
                    return true;
                }
                return false;
            });
            popup.show();
        });

        // URL bar behavior
        urlBar.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) { urlBar.setTextSize(18f); urlBar.setMaxLines(2); }
            else { urlBar.setTextSize(14f); urlBar.setMaxLines(1); }
            // When focus is lost, assume user finished editing
            if (!hasFocus) urlBarUserEditing = false;
        });
        urlBar.setOnEditorActionListener((v, actionId, event) -> {
            String url = urlBar.getText().toString().trim();
            if (!url.isEmpty()) {
                if (!url.startsWith("http://") && !url.startsWith("https://")) url = "https://" + url;
                if (mainWebView != null) mainWebView.loadUrl(url);
                urlBar.clearFocus();
                urlBarUserEditing = false;
            }
            return true;
        });

        // Track user edits so we don't overwrite while typing
        urlBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try { urlBarUserEditing = urlBar.isFocused(); } catch (Exception ignored) { }
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        // Show/hide browser controls according to preference
        boolean hideControls = prefs.getBoolean("no_browser_controls", false);
        if (browserControlsBar != null) browserControlsBar.setVisibility(hideControls ? View.GONE : View.VISIBLE);

        // Listen for preference changes to update UI and home page dynamically
        prefListener = (sharedPreferences, key) -> {
            if ("home_page".equals(key)) {
                String url = sharedPreferences.getString("home_page", "https://sunflower-land.com/play/?ref=iSPANK");
                // Only reload if the active tab is tab 1
                if (mainWebView != null && currentTab == 1) {
                    Log.d("MainActivity", "Preference changed: home_page -> reloading because currentTab==1: " + url);
                    loadTabUrl(1, true);
                } else {
                    Log.d("MainActivity", "Preference changed: home_page (ignored because currentTab!=1)");
                }
            } else if ("home_page_2".equals(key)) {
                String url = sharedPreferences.getString("home_page_2", "https://sfl.world");
                if (mainWebView != null && currentTab == 2) {
                    Log.d("MainActivity", "Preference changed: home_page_2 -> reloading for currentTab==2: " + url);
                    loadTabUrl(2, true);
                } else {
                    Log.d("MainActivity", "Preference changed: home_page_2 (ignored because currentTab!=2)");
                }
            } else if ("home_page_3".equals(key)) {
                String url = sharedPreferences.getString("home_page_3", "https://wiki.sfl.world");
                if (mainWebView != null && currentTab == 3) {
                    Log.d("MainActivity", "Preference changed: home_page_3 -> reloading for currentTab==3: " + url);
                    loadTabUrl(3, true);
                } else {
                    Log.d("MainActivity", "Preference changed: home_page_3 (ignored because currentTab!=3)");
                }
            } else if ("no_browser_controls".equals(key)) {
                boolean hide = sharedPreferences.getBoolean("no_browser_controls", false);
                if (browserControlsBar != null) browserControlsBar.setVisibility(hide ? View.GONE : View.VISIBLE);
            } else if ("android_buttons".equals(key)) {
                boolean show = sharedPreferences.getBoolean("android_buttons", true);
                // Show or hide system navigation bar based on toggle
                setSystemNavHidden(!show);
            }
        };
        prefs.registerOnSharedPreferenceChangeListener(prefListener);

        // Load home page from settings (use centralized loader) - THIS IS TAB 1, THE ACTIVE TAB
        if (mainWebView != null) {
            loadTabUrl(currentTab, true);
            try {
                // Ensure the URL bar is not focused on startup so WebViewClient updates can update it.
                if (urlBar != null) {
                    urlBar.clearFocus();
                    android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                    if (imm != null) imm.hideSoftInputFromWindow(urlBar.getWindowToken(), 0);
                }
                // Prefer WebView to have focus so navigation events can update the UI
                mainWebView.requestFocus(android.view.View.FOCUS_DOWN);
            } catch (Exception ignore) { }
        }

        // Pre-create and load tabs 2 and 3 in the background to ensure they persist and are ready
        // Use Handler to post to UI thread after a delay so tab 1 loads first
        android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        for (int bgTab = 2; bgTab <= MAX_TABS; bgTab++) {
            final int tabNum = bgTab;
            handler.postDelayed(() -> {
                try {
                    if (tabWebViews[tabNum - 1] == null) {
                        tabWebViews[tabNum - 1] = createTabWebView(tabNum);
                        String url = getHomeUrlForTab(tabNum);
                        if (url != null && !url.isEmpty()) {
                            tabWebViews[tabNum - 1].loadUrl(url);
                            Log.d("MainActivity", "Pre-loaded tab " + tabNum + " with URL: " + url);
                        }
                    }
                } catch (Exception e) {
                    Log.w("MainActivity", "Error pre-loading tab " + tabNum, e);
                }
            }, 500 * (tabNum - 1)); // 500ms for tab 2, 1000ms for tab 3
        }

        // Ensure WebView content appears above the system navigation bar (soft keys)
        try {
            int navBarHeight = 0;
            int resId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
            if (resId > 0) {
                navBarHeight = getResources().getDimensionPixelSize(resId);
            }
            if (mainWebView != null && navBarHeight > 0) {
                // Add bottom padding equal to navigation bar height so content isn't obscured
                mainWebView.setPadding(
                    mainWebView.getPaddingLeft(),
                    mainWebView.getPaddingTop(),
                    mainWebView.getPaddingRight(),
                    navBarHeight
                );
                mainWebView.setClipToPadding(false);
                try {
                    android.view.ViewGroup.LayoutParams lp = mainWebView.getLayoutParams();
                    if (lp instanceof android.widget.LinearLayout.LayoutParams) {
                        android.widget.LinearLayout.LayoutParams llp = (android.widget.LinearLayout.LayoutParams) lp;
                        if (llp.bottomMargin != navBarHeight) {
                            llp.bottomMargin = navBarHeight;
                            mainWebView.setLayoutParams(llp);
                        }
                    }
                } catch (Exception ignore) { }
                Log.d("MainActivity", "Applied navigation bar bottom padding/margin: " + navBarHeight);
            }
        } catch (Exception e) {
            Log.w("MainActivity", "Could not apply navigation bar inset padding", e);
        }

        // Apply orientation setting on startup
        applyOrientationSetting(null);
        
        // Initialize debug log system
        DebugLog.init(this);
        
        Log.i("NOTIFICATION_DEBUG", "=== Sunflower Land App Starting ===");
        Log.i("NOTIFICATION_DEBUG", "Process ID: " + android.os.Process.myPid());
        Log.i("NOTIFICATION_DEBUG", "App Package: " + getPackageName());

        // Prompt for exact alarm permission if needed (Android 12+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            android.app.AlarmManager alarmManager = (android.app.AlarmManager) getSystemService(ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                runOnUiThread(() -> {
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Allow Exact Alarms")
                        .setMessage("To ensure notifications are delivered on time, please allow this app to use exact alarms.")
                        .setNegativeButton("Close", (dialog, which) -> dialog.dismiss())
                        .setPositiveButton("Open", (dialog, which) -> {
                            try {
                                android.content.Intent intent = new android.content.Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                                startActivity(intent);
                            } catch (Exception e) {
                                android.widget.Toast.makeText(this, "Unable to open settings.", android.widget.Toast.LENGTH_SHORT).show();
                            }
                        })
                        .show();
                });
            }
        }

        // Setup JavaScript interface immediately (don't defer)
        // This configures WebView settings and clients needed for the page to load
        setupJavaScriptInterface();
        
        // Apply WindowInsets listener so we can ensure WebView sits above system navigation
        try {
            final View rootView = findViewById(getResources().getIdentifier("root_coordinator", "id", getPackageName()));
            if (rootView != null) {
                ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                    try {
                        WindowInsetsCompat compat = insets;
                        Insets navInsets = compat.getInsets(WindowInsetsCompat.Type.navigationBars());
                        int bottomInset = navInsets.bottom;

                        // Apply top inset (status bar) as padding to the root view so the entire
                        // app UI is slightly offset downward to account for the notification/status bar.
                        Insets statusInsets = compat.getInsets(WindowInsetsCompat.Type.statusBars());
                        // If we've explicitly hidden system nav/status bars via the toggle, ignore top inset
                        int topInset = systemNavHidden ? 0 : statusInsets.top;
                        try {
                            if (v != null) {
                                int left = v.getPaddingLeft();
                                int right = v.getPaddingRight();
                                int bottom = v.getPaddingBottom();
                                if (v.getPaddingTop() != topInset) {
                                    v.setPadding(left, topInset, right, bottom);
                                    Log.d("MainActivity", "WindowInsets applied top padding: " + topInset);
                                }
                            }
                        } catch (Exception ignore) { }

                        if (mainWebView != null) {
                            // Apply bottom inset to WebView so content is not obscured by nav bar
                            if (bottomInset > 0) {
                                mainWebView.setPadding(
                                    mainWebView.getPaddingLeft(),
                                    mainWebView.getPaddingTop(),
                                    mainWebView.getPaddingRight(),
                                    bottomInset
                                );
                                mainWebView.setClipToPadding(false);
                                try {
                                    android.view.ViewGroup.LayoutParams lp = mainWebView.getLayoutParams();
                                    if (lp instanceof android.widget.LinearLayout.LayoutParams) {
                                        android.widget.LinearLayout.LayoutParams llp = (android.widget.LinearLayout.LayoutParams) lp;
                                        if (llp.bottomMargin != bottomInset) {
                                            llp.bottomMargin = bottomInset;
                                            mainWebView.setLayoutParams(llp);
                                        }
                                    }
                                } catch (Exception ignore) { }
                                Log.d("MainActivity", "WindowInsets applied bottom padding/margin: " + bottomInset);
                            } else {
                                // If no bottom inset, ensure any previous bottom margin/padding remains or is cleared
                                // (we intentionally avoid forcing removal here; setSystemNavHidden handles hide-case)
                            }
                        }
                    } catch (Exception e) {
                        Log.w("MainActivity", "Error applying window insets to WebView/root", e);
                    }
                    return insets;
                });
                // Request insets now so listener runs at least once
                rootView.requestApplyInsets();
            }
        } catch (Exception e) {
            Log.w("MainActivity", "WindowInsets handler not applied", e);
        }

        // Respect the initial `android_buttons` preference to show/hide system nav
        try {
            boolean showButtons = prefs.getBoolean("android_buttons", true);
            setSystemNavHidden(!showButtons);
        } catch (Exception ignored) { }

        handleIntent(getIntent());

        // Attach deep-link interceptors immediately - CRITICAL for WebView functionality
        attachDeepLinkHandlers();
        
        // Auto-start notification manager immediately (it runs in separate process, won't interfere)
        autoStartNotificationManager();
        
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }
        });

    }

    /**
     * Override dispatchTouchEvent to intercept 3-finger touches and prevent system gesture interception.
     * This ensures 3-finger swipes are handled by our gesture handler and not intercepted by Android's
     * system gestures (like split-screen or gesture navigation).
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        try {
            // If this is a 3-finger touch, block ALL event propagation to system handlers
            if (event.getPointerCount() >= 3) {
                int action = event.getActionMasked();
                if (action == MotionEvent.ACTION_DOWN || 
                    action == MotionEvent.ACTION_POINTER_DOWN || 
                    action == MotionEvent.ACTION_MOVE ||
                    action == MotionEvent.ACTION_UP ||
                    action == MotionEvent.ACTION_POINTER_UP ||
                    action == MotionEvent.ACTION_CANCEL) {
                    
                    // Log for debugging
                    if (action == MotionEvent.ACTION_MOVE) {
                        Log.d("MainActivity", "3-finger MOVE at (" + event.getX() + "," + event.getY() + ")");
                    }
                    
                    // Send to WebView first, consume result
                    boolean handledByWebView = super.dispatchTouchEvent(event);
                    
                    // Consume the event to prevent system gesture handling
                    return true;
                }
            }
        } catch (Exception e) {
            Log.w("MainActivity", "Error in dispatchTouchEvent", e);
        }
        return super.dispatchTouchEvent(event);
    }

    private String getHomeUrlForTab(int tab) {
        try {
            android.content.SharedPreferences sp = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this);
            switch (tab) {
                case 2:
                    return sp.getString("home_page_2", null);
                case 3:
                    return sp.getString("home_page_3", null);
                case 1:
                default:
                    return sp.getString("home_page", null);
            }
        } catch (Exception e) {
            Log.w("MainActivity", "Error reading home URL for tab " + tab, e);
            return null;
        }
    }

    /**
     * Load the configured home URL for a tab and apply the same behavior used at startup.
     * This centralizes tab loading so tab 2/3 behave the same as tab 1.
     */
    private void loadTabUrl(int tab) {
        // Backwards-compatible overload: default to forcing a load (existing behavior)
        loadTabUrl(tab, true);
    }

    private void loadTabUrl(int tab, boolean forceReload) {
        uiHandler.post(() -> {
            try {
                // Read prefs directly for diagnostics
                android.content.SharedPreferences sp = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this);
                String p1 = sp.getString("home_page", "<missing>");
                String p2 = sp.getString("home_page_2", "<missing>");
                String p3 = sp.getString("home_page_3", "<missing>");
                String url = getHomeUrlForTab(tab);
                if (url == null || url.trim().isEmpty()) {
                    Log.w("MainActivity", "No configured home URL for tab " + tab + " - skipping load");
                    return;
                }
                Log.d("MainActivity", "loadTabUrl(tab=" + tab + ") -> " + url + " (lastProgrammaticUrl=" + lastProgrammaticUrl + ") forceReload=" + forceReload);
                Log.d("MainActivity", "Stored prefs: home_page=" + p1 + ", home_page_2=" + p2 + ", home_page_3=" + p3);

                // Special handling for tab 1 (the Capacitor bridge WebView)
                // NEVER swap it out - it stays in the container always
                if (tab == 1) {
                    try {
                        if (tabWebViews[0] == null && mainWebView != null) {
                            tabWebViews[0] = mainWebView;
                        }
                        
                        if (tabWebViews[0] != null) {
                            // Tab 1 is always visible - just update its content if needed
                            if (forceReload) {
                                try {
                                    String cur = null;
                                    try { cur = tabWebViews[0].getUrl(); } catch (Exception ignore) { cur = null; }
                                    if (cur == null || !cur.equals(url)) {
                                        Log.d("MainActivity", "Loading tab 1 URL: " + url);
                                        try { tabWebViews[0].loadUrl(url); } catch (Exception ignore) { }
                                    }
                                } catch (Exception ignore) { }
                            }
                            mainWebView = tabWebViews[0]; // Ensure mainWebView is always pointing to tab 1
                        }
                    } catch (Exception e) {
                        Log.w("MainActivity", "Error loading tab 1", e);
                    }
                } else {
                    // For tabs 2 and 3, swap them in/out of the container
                    try {
                        WebView desired = null;
                        try { desired = tabWebViews[tab - 1]; } catch (Exception ignored) { desired = null; }

                        if (webviewParent != null) {
                            if (desired == null) {
                                // Lazily create + configure the WebView for this tab
                                desired = createTabWebView(tab);
                                tabWebViews[tab - 1] = desired;
                                try { desired.loadUrl(url); } catch (Exception ignore) { }
                            } else {
                                // If forceReload is true, attempt to load the configured URL; otherwise preserve existing tab state
                                if (forceReload) {
                                    try {
                                        String cur = null;
                                        try { cur = desired.getUrl(); } catch (Exception ignore) { cur = null; }
                                        if (cur == null || !cur.equals(url)) {
                                            try { desired.loadUrl(url); } catch (Exception ignore) { }
                                        }
                                    } catch (Exception ignore) { }
                                }
                            }

                            // Swap into parent if not already visible
                            // But NEVER swap out tab 1 (the bridge WebView)
                            if (desired != mainWebView) {
                                try {
                                    // Remove the currently visible non-tab-1 WebView
                                    if (mainWebView != null && mainWebView != tabWebViews[0] && mainWebView.getParent() == webviewParent) {
                                        webviewParent.removeView(mainWebView);
                                    }
                                    
                                    if (desired.getParent() instanceof android.view.ViewGroup) {
                                        ((android.view.ViewGroup) desired.getParent()).removeView(desired);
                                    }
                                    
                                    // Ensure proper layout parameters for the WebView
                                    android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(
                                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                                        0
                                    );
                                    layoutParams.weight = 1;
                                    desired.setLayoutParams(layoutParams);
                                    
                                    webviewParent.addView(desired, webviewIndexInParent);
                                    mainWebView = desired;
                                    
                                    // Keep tab array reference in sync
                                    try { tabWebViews[tab - 1] = desired; } catch (Exception ignored) { }
                                    
                                    Log.d("MainActivity", "Swapped tab " + tab + " WebView into container");
                                } catch (Exception e) {
                                    Log.w("MainActivity", "Failed to swap WebView for tab " + tab, e);
                                }
                            }

                            lastProgrammaticUrl = url;
                        } else {
                            // No swap container available — fall back to single-WebView behavior
                            if (mainWebView != null) {
                                try { mainWebView.stopLoading(); } catch (Exception ignore) { }
                                try {
                                    lastProgrammaticUrl = url;
                                    mainWebView.loadUrl(url);
                                } catch (Exception e) {
                                    Log.w("MainActivity", "Failed to load URL for tab " + tab + ": " + url, e);
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.w("MainActivity", "Error handling tab " + tab, e);
                    }
                }

                try {
                    if (urlBar != null && !urlBarUserEditing) uiHandler.post(() -> { try { urlBar.setText(url); } catch (Exception ignore) {} });
                } catch (Exception ignore) { }
                try {
                    // Ensure keyboard is hidden and focus is on WebView like startup
                    if (urlBar != null) {
                        urlBar.clearFocus();
                        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                        if (imm != null) imm.hideSoftInputFromWindow(urlBar.getWindowToken(), 0);
                    }
                    if (mainWebView != null) mainWebView.requestFocus(android.view.View.FOCUS_DOWN);
                } catch (Exception ignore) { }
                try { if (btnBack != null && mainWebView != null) btnBack.setEnabled(mainWebView.canGoBack()); } catch (Exception ignore) {}
                try { if (btnForward != null && mainWebView != null) btnForward.setEnabled(mainWebView.canGoForward()); } catch (Exception ignore) {}

            } catch (Exception e) {
                Log.w("MainActivity", "Error in loadTabUrl for tab " + tab, e);
            }
        });
    }

    private void switchTab(boolean up) {
        uiHandler.post(() -> {
            try {
                if (up) {
                    currentTab = (currentTab % MAX_TABS) + 1;
                } else {
                    currentTab = (currentTab - 1);
                    if (currentTab < 1) currentTab = MAX_TABS;
                }
                Log.d("MainActivity", "Switching to tab " + currentTab);
                
                // Load the tab's content (which swaps the WebView into the container)
                loadTabUrl(currentTab, false);
            } catch (Exception e) {
                Log.w("MainActivity", "Error switching tab", e);
            }
        });
    }

    /**
     * Refresh the currently active tab using the appropriate method for that tab's type
     */
    private void refreshCurrentTab() {
        try {
            Log.d("MainActivity", "=== REFRESH CLICKED ===");
            Log.d("MainActivity", "currentTab: " + currentTab);
            Log.d("MainActivity", "tabWebViews[0]: " + (tabWebViews[0] != null ? tabWebViews[0].getClass().getSimpleName() : "NULL"));
            Log.d("MainActivity", "mainWebView: " + (mainWebView != null ? mainWebView.getClass().getSimpleName() : "NULL"));
            Log.d("MainActivity", "Are they the same? " + (tabWebViews[0] == mainWebView));
            
            // For tab 1 (bridge WebView), use the dedicated refresh method
            if (currentTab == 1) {
                Log.d("MainActivity", "Refreshing TAB 1");
                
                // Try to get the bridge WebView - try multiple sources
                WebView bridgeWv = null;
                
                // First try tabWebViews[0]
                if (tabWebViews[0] != null) {
                    bridgeWv = tabWebViews[0];
                    Log.d("MainActivity", "Using tabWebViews[0]");
                }
                
                // Fallback to mainWebView
                if (bridgeWv == null && mainWebView != null) {
                    bridgeWv = mainWebView;
                    Log.d("MainActivity", "Using mainWebView as fallback");
                }
                
                // Last resort: try to get from bridge directly
                if (bridgeWv == null && this.bridge != null) {
                    try {
                        Object bridgeObj = this.bridge.getWebView();
                        if (bridgeObj instanceof WebView) {
                            bridgeWv = (WebView) bridgeObj;
                            tabWebViews[0] = bridgeWv;  // Update the cache
                            Log.d("MainActivity", "Got WebView from bridge directly");
                        }
                    } catch (Exception e) {
                        Log.w("MainActivity", "Could not get WebView from bridge", e);
                    }
                }
                
                if (bridgeWv != null) {
                    Log.d("MainActivity", "Calling refreshBridgeWebView");
                    refreshBridgeWebView(bridgeWv);
                } else {
                    Log.e("MainActivity", "CRITICAL: Could not find bridge WebView from ANY source!");
                }
            } else {
                Log.d("MainActivity", "Refreshing TAB " + currentTab + " (non-bridge)");
                // For tabs 2 and 3, ensure they're visible then reload
                // First swap them into view
                loadTabUrl(currentTab, false);
                
                // Then reload after a short delay
                uiHandler.postDelayed(() -> {
                    try {
                        WebView tab = tabWebViews[currentTab - 1];
                        if (tab != null) {
                            Log.d("MainActivity", "Reloading tab " + currentTab);
                            tab.reload();
                        } else {
                            Log.e("MainActivity", "ERROR: Tab " + currentTab + " WebView is NULL");
                        }
                    } catch (Exception e) {
                        Log.e("MainActivity", "Error reloading tab " + currentTab, e);
                    }
                }, 100);
            }
            
        } catch (Exception e) {
            Log.e("MainActivity", "CRITICAL ERROR in refreshCurrentTab", e);
        }
    }

    /**
     * Refresh the Capacitor bridge WebView using multiple strategies
     * Since Capacitor intentionally doesn't expose reload, we need creative solutions
     */
    private void refreshBridgeWebView(WebView wv) {
        try {
            Log.d("MainActivity", "Refreshing bridge WebView (tab 1)");
            
            if (wv == null) {
                Log.e("MainActivity", "Bridge WebView is NULL - cannot refresh");
                return;
            }
            
            // STRATEGY 1: Inject JavaScript to reload the page from the browser side
            // This is the most Capacitor-friendly approach
            try {
                Log.d("MainActivity", "Strategy 1: JavaScript window.location.reload()");
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    wv.evaluateJavascript(
                        "(function() { " +
                        "  console.log('Reloading page via JavaScript'); " +
                        "  window.location.reload(true); " +
                        "})()",
                        result -> Log.d("MainActivity", "JavaScript reload result: " + result)
                    );
                    Log.d("MainActivity", "Strategy 1 SUCCESS: JavaScript reload injected");
                    return;
                }
            } catch (Exception e) {
                Log.w("MainActivity", "Strategy 1 failed: " + e.getMessage());
            }
            
            // STRATEGY 2: Try WebView.reload() - this might work on the actual underlying WebView
            try {
                Log.d("MainActivity", "Strategy 2: Direct WebView.reload()");
                wv.reload();
                Log.d("MainActivity", "Strategy 2 SUCCESS: reload() called");
                return;
            } catch (Exception e) {
                Log.w("MainActivity", "Strategy 2 failed: " + e.getMessage());
            }
            
            // STRATEGY 3: Load the current URL again (forces page reload)
            try {
                Log.d("MainActivity", "Strategy 3: Re-navigate to current URL");
                String currentUrl = wv.getUrl();
                if (currentUrl != null && !currentUrl.isEmpty() && !currentUrl.equals("about:blank")) {
                    Log.d("MainActivity", "Current URL: " + currentUrl + ", re-navigating...");
                    wv.loadUrl(currentUrl);
                    Log.d("MainActivity", "Strategy 3 SUCCESS: Re-navigated to current URL");
                    return;
                }
            } catch (Exception e) {
                Log.w("MainActivity", "Strategy 3 failed: " + e.getMessage());
            }
            
            // STRATEGY 4: Inject reload with cache-busting
            try {
                Log.d("MainActivity", "Strategy 4: JavaScript reload with cache buster");
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                    long timestamp = System.currentTimeMillis();
                    String jsCode = "(function() { " +
                        "  var url = window.location.href; " +
                        "  var separator = url.indexOf('?') > -1 ? '&' : '?'; " +
                        "  var bustedUrl = url + separator + '_t=' + " + timestamp + "; " +
                        "  console.log('Reloading with busted URL: ' + bustedUrl); " +
                        "  window.location.href = bustedUrl; " +
                        "})()";
                    wv.evaluateJavascript(jsCode, null);
                    Log.d("MainActivity", "Strategy 4 SUCCESS: Cache-buster reload injected");
                    return;
                }
            } catch (Exception e) {
                Log.w("MainActivity", "Strategy 4 failed: " + e.getMessage());
            }
            
            // STRATEGY 5: Clear cache and then navigate
            try {
                Log.d("MainActivity", "Strategy 5: Clear cache + navigate");
                wv.clearCache(true);
                String currentUrl = wv.getUrl();
                if (currentUrl != null && !currentUrl.isEmpty()) {
                    wv.loadUrl(currentUrl);
                    Log.d("MainActivity", "Strategy 5 SUCCESS: Cache cleared and URL re-loaded");
                    return;
                }
            } catch (Exception e) {
                Log.w("MainActivity", "Strategy 5 failed: " + e.getMessage());
            }
            
            Log.e("MainActivity", "All refresh strategies FAILED for bridge WebView");
            
        } catch (Exception e) {
            Log.e("MainActivity", "Critical error in refreshBridgeWebView", e);
        }
    }

    /**
     * Create and configure a WebView instance for a tab. This mirrors the important
     * settings we apply to the bridge WebView so tab WebViews behave similarly.
     */
    private WebView createTabWebView(int tab) {
        try {
            WebView wv = new WebView(this);
            // match layout of placeholder
            android.view.ViewGroup.LayoutParams lp = new android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
            );
            wv.setLayoutParams(lp);

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
            
            // Smart caching strategy: Cache aggressively for tabs 2 and 3
            // Always use LOAD_CACHE_ELSE_NETWORK for tabs 2 and 3 to save bandwidth
            // These tabs (sfl.world and wiki) benefit from caching
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            boolean aggressiveCaching = prefs.getBoolean("aggressive_caching", false);
            ws.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
            Log.d("MainActivity", "Tab " + tab + ": Using LOAD_CACHE_ELSE_NETWORK");
            
            if (aggressiveCaching) {
                // In aggressive caching mode: aggressively cache content to minimize bandwidth
                // Still allow network updates when the website requests them
                ws.setBlockNetworkLoads(false);      // Allow network, but prefer cache
                ws.setBlockNetworkImage(false);      // Allow image network requests
                // Cache images more aggressively - don't expire as quickly
                ws.setLoadsImagesAutomatically(true);
            } else {
                // Normal mode: standard caching behavior
                ws.setBlockNetworkLoads(false);
                ws.setBlockNetworkImage(false); // Allow images (will be cached per server headers)
                ws.setLoadsImagesAutomatically(true);
            }
            
            ws.setLoadWithOverviewMode(true);
            ws.setUseWideViewPort(true);
            ws.setBuiltInZoomControls(true);
            ws.setDisplayZoomControls(false);
            ws.setSupportZoom(true);
            ws.setGeolocationEnabled(true);
            
            // Enable network optimization
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                try {
                    ws.setForceDark(WebSettings.FORCE_DARK_OFF); // Avoid rendering delays from dark mode conversion
                } catch (Exception ignore) { }
            }
            
            try {
                String defaultUserAgent = ws.getUserAgentString();
                String customUserAgent = defaultUserAgent.replace("; wv", "").replace("Version/4.0 ", "");
                if (!customUserAgent.contains("Chrome/")) customUserAgent = customUserAgent + " Chrome/120.0.0.0";
                ws.setUserAgentString(customUserAgent);
            } catch (Exception ignored) { }

            wv.setWebChromeClient(new android.webkit.WebChromeClient() {
                @Override
                public boolean onConsoleMessage(android.webkit.ConsoleMessage cm) {
                    String message = cm.message();
                    Log.d("WebView Console", String.format("[%s:%d] %s", cm.sourceId(), cm.lineNumber(), message));
                    return true;
                }
            });

            wv.setWebViewClient(new android.webkit.WebViewClient() {
                @Override
                public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                    Log.d("MainActivity", "(tab) onPageStarted: " + url);
                    // handle deep links similarly
                    try {
                        if (url != null && (url.contains("keys.coinbase.com") || url.contains("coinbase.com") || url.contains("login.coinbase"))) {
                            try {
                                view.stopLoading();
                                Intent extIntent = new Intent(Intent.ACTION_VIEW);
                                extIntent.setData(Uri.parse(url));
                                extIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                try { extIntent.setPackage("com.coinbase.android"); startActivity(extIntent); } catch (Exception e) { extIntent.setPackage(null); startActivity(extIntent); }
                            } catch (Exception e) { Log.e("MainActivity", "Failed to handle Coinbase URL in tab webview", e); }
                        }
                    } catch (Exception ignore) { }

                    // Update URL bar unless user is editing it
                    try {
                        if (urlBar != null && !urlBarUserEditing) {
                            uiHandler.post(() -> { try { urlBar.setText(url); } catch (Exception ignore) {} });
                        }
                    } catch (Exception ignore) { }
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    Log.d("MainActivity", "(tab) Page load finished: " + url);
                    try {
                        if (urlBar != null && !urlBarUserEditing) uiHandler.post(() -> { try { urlBar.setText(url); } catch (Exception ignore) {} });
                        if (btnBack != null) btnBack.setEnabled(view.canGoBack());
                        if (btnForward != null) btnForward.setEnabled(view.canGoForward());
                        
                        // Preload other tabs after the current tab finishes loading
                        // Only preload if this is tab 1 (the first tab)
                        if (currentTab == 1) {
                            Log.d("MainActivity", "Tab 1 loaded - preloading tabs 2 and 3 in background");
                            preloadOtherTabs();
                        }
                    } catch (Exception ignore) { }
                }
            });

                // Attach gesture handlers so this tab supports three-finger gestures like the bridge WebView
                try { attachGestureHandlers(wv); } catch (Exception ignored) {}

                return wv;
        } catch (Exception e) {
            Log.w("MainActivity", "Failed to create tab WebView", e);
            return null;
        }
    }

        private void attachGestureHandlers(WebView wv) {
            try {
                wv.setOnTouchListener(new View.OnTouchListener() {
                    private int threeFingerTapCount = 0;
                    private long lastThreeFingerTapTime = 0;
                    private static final int TRIPLE_TAP_TIMEOUT_MS = 1000;
                    private float threeFingerStartAvgX = -1f;
                    private boolean threeFingerSwipeTriggered = false;
                    private long lastThreeFingerSwipeTime = 0L;
                    private final int SWIPE_COOLDOWN_MS = 600; // ms
                    private final int SWIPE_THRESHOLD_PX = 200;

                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        int action = event.getActionMasked();
                        
                        // If swipe was triggered, consume all subsequent events until all fingers are up
                        if (threeFingerSwipeTriggered) {
                            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL || event.getPointerCount() < 3) {
                                threeFingerSwipeTriggered = false;
                                threeFingerStartAvgX = -1f;
                            }
                            return true; // Consume the event
                        }
                        
                        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                            if (event.getPointerCount() == 3) {
                                // Disallow parent ViewGroup from intercepting (prevents system split-screen gesture)
                                if (v.getParent() != null) {
                                    v.getParent().requestDisallowInterceptTouchEvent(true);
                                }
                                // Also disallow the webview container parent
                                if (webviewParent != null) {
                                    webviewParent.requestDisallowInterceptTouchEvent(true);
                                }
                                // And try the root coordinator layout
                                try {
                                    View rootView = findViewById(getResources().getIdentifier("root_coordinator", "id", getPackageName()));
                                    if (rootView != null && rootView.getParent() instanceof android.view.ViewGroup) {
                                        ((android.view.ViewGroup) rootView.getParent()).requestDisallowInterceptTouchEvent(true);
                                    }
                                } catch (Exception ignore) { }
                                
                                long now = System.currentTimeMillis();
                                if (now - lastThreeFingerTapTime > TRIPLE_TAP_TIMEOUT_MS) threeFingerTapCount = 0;
                                threeFingerTapCount++;
                                lastThreeFingerTapTime = now;
                                if (threeFingerTapCount == 3) {
                                    openSettingsPage();
                                    threeFingerTapCount = 0;
                                    threeFingerStartAvgX = -1f;
                                    threeFingerSwipeTriggered = false;
                                    return true; // Consume triple-tap event
                                }
                            }
                            if (event.getPointerCount() >= 3) {
                                try {
                                    float sum = 0f;
                                    for (int i = 0; i < 3; i++) sum += event.getX(i);
                                    threeFingerStartAvgX = sum / 3f;
                                    threeFingerSwipeTriggered = false;
                                } catch (Exception ignore) { threeFingerStartAvgX = -1f; }
                            }
                        }

                        if (action == MotionEvent.ACTION_MOVE) {
                            if (event.getPointerCount() >= 3 && threeFingerStartAvgX >= 0 && !threeFingerSwipeTriggered) {
                                try {
                                    float sum = 0f;
                                    for (int i = 0; i < 3; i++) sum += event.getX(i);
                                    float curAvg = sum / 3f;
                                    float dx = curAvg - threeFingerStartAvgX;
                                    if (Math.abs(dx) > SWIPE_THRESHOLD_PX) {
                                        long nowSwipe = System.currentTimeMillis();
                                        if (nowSwipe - lastThreeFingerSwipeTime > SWIPE_COOLDOWN_MS) {
                                            threeFingerSwipeTriggered = true;
                                            lastThreeFingerSwipeTime = nowSwipe;
                                            
                                            // Disallow ALL parent ViewGroups from intercepting
                                            if (v.getParent() != null) {
                                                v.getParent().requestDisallowInterceptTouchEvent(true);
                                            }
                                            if (webviewParent != null) {
                                                webviewParent.requestDisallowInterceptTouchEvent(true);
                                            }
                                            try {
                                                View rootView = findViewById(getResources().getIdentifier("root_coordinator", "id", getPackageName()));
                                                if (rootView != null && rootView.getParent() instanceof android.view.ViewGroup) {
                                                    ((android.view.ViewGroup) rootView.getParent()).requestDisallowInterceptTouchEvent(true);
                                                }
                                            } catch (Exception ignore) { }
                                            
                                            // Show tab overview on 3-finger swipe
                                            MainActivity.this.showTabOverview();
                                            
                                            // Send ACTION_CANCEL to clear WebView's touch state
                                            try {
                                                MotionEvent cancelEvent = MotionEvent.obtain(event);
                                                cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
                                                ((WebView) v).onTouchEvent(cancelEvent);
                                                cancelEvent.recycle();
                                            } catch (Exception ignore) { }
                                            
                                            return true; // Consume the event
                                        } else {
                                            threeFingerSwipeTriggered = true;
                                            return true; // Consume cooldown event
                                        }
                                    }
                                } catch (Exception ignore) { }
                            }
                        }

                        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_CANCEL) {
                            try {
                                if (event.getPointerCount() < 3) {
                                    threeFingerStartAvgX = -1f;
                                    threeFingerSwipeTriggered = false;
                                    // Allow parent ViewGroup to intercept again (normal behavior)
                                    if (v.getParent() != null) {
                                        v.getParent().requestDisallowInterceptTouchEvent(false);
                                    }
                                }
                            } catch (Exception ignore) { threeFingerStartAvgX = -1f; threeFingerSwipeTriggered = false; }
                        }
                        return false;
                    }
                });
            } catch (Exception e) {
                Log.w("MainActivity", "Failed to attach gesture handlers", e);
            }
        }

    /**
     * Attach gesture handlers to the webview container (parent ViewGroup) instead of individual WebViews.
     * This ensures gesture detection works regardless of which tab is currently displayed.
     */
    private void attachGestureHandlersToContainer(android.view.ViewGroup container) {
        try {
            container.setOnTouchListener(new View.OnTouchListener() {
                private int threeFingerTapCount = 0;
                private long lastThreeFingerTapTime = 0;
                private static final int TRIPLE_TAP_TIMEOUT_MS = 1000;
                private float threeFingerStartAvgX = -1f;
                private boolean threeFingerSwipeTriggered = false;
                private long lastThreeFingerSwipeTime = 0L;
                private final int SWIPE_COOLDOWN_MS = 600; // ms
                private final int SWIPE_THRESHOLD_PX = 200;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    int action = event.getActionMasked();
                    
                    // If swipe was triggered, consume all subsequent events until all fingers are up
                    if (threeFingerSwipeTriggered) {
                        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL || event.getPointerCount() < 3) {
                            threeFingerSwipeTriggered = false;
                            threeFingerStartAvgX = -1f;
                        }
                        return true; // Consume the event
                    }
                    
                    if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                        if (event.getPointerCount() == 3) {
                            // Disallow parent ViewGroup from intercepting (prevents system split-screen gesture)
                            if (v.getParent() != null) {
                                v.getParent().requestDisallowInterceptTouchEvent(true);
                            }
                            
                            long now = System.currentTimeMillis();
                            if (now - lastThreeFingerTapTime > TRIPLE_TAP_TIMEOUT_MS) threeFingerTapCount = 0;
                            threeFingerTapCount++;
                            lastThreeFingerTapTime = now;
                            if (threeFingerTapCount == 3) {
                                openSettingsPage();
                                threeFingerTapCount = 0;
                                threeFingerStartAvgX = -1f;
                                threeFingerSwipeTriggered = false;
                                return true; // Consume triple-tap event
                            }
                        }
                        if (event.getPointerCount() >= 3) {
                            try {
                                float sum = 0f;
                                for (int i = 0; i < 3; i++) sum += event.getX(i);
                                threeFingerStartAvgX = sum / 3f;
                                threeFingerSwipeTriggered = false;
                            } catch (Exception ignore) { threeFingerStartAvgX = -1f; }
                        }
                    }

                    if (action == MotionEvent.ACTION_MOVE) {
                        if (event.getPointerCount() >= 3 && threeFingerStartAvgX >= 0 && !threeFingerSwipeTriggered) {
                            try {
                                float sum = 0f;
                                for (int i = 0; i < 3; i++) sum += event.getX(i);
                                float curAvg = sum / 3f;
                                float dx = curAvg - threeFingerStartAvgX;
                                if (Math.abs(dx) > SWIPE_THRESHOLD_PX) {
                                    long nowSwipe = System.currentTimeMillis();
                                    if (nowSwipe - lastThreeFingerSwipeTime > SWIPE_COOLDOWN_MS) {
                                        threeFingerSwipeTriggered = true;
                                        lastThreeFingerSwipeTime = nowSwipe;
                                        
                                        // Disallow ALL parent ViewGroups from intercepting
                                        if (v.getParent() != null) {
                                            v.getParent().requestDisallowInterceptTouchEvent(true);
                                        }
                                        
                                        Log.d("MainActivity", "Container gesture detected: dx=" + dx + ", showing tab overview");
                                        // Show tab overview on 3-finger swipe
                                        MainActivity.this.showTabOverview();
                                        
                                        return true; // Consume the event
                                    } else {
                                        threeFingerSwipeTriggered = true;
                                        return true; // Consume cooldown event
                                    }
                                }
                            } catch (Exception ignore) { }
                        }
                    }

                    if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_CANCEL) {
                        try {
                            if (event.getPointerCount() < 3) {
                                threeFingerStartAvgX = -1f;
                                threeFingerSwipeTriggered = false;
                                // Allow parent ViewGroup to intercept again (normal behavior)
                                if (v.getParent() != null) {
                                    v.getParent().requestDisallowInterceptTouchEvent(false);
                                }
                            }
                        } catch (Exception ignore) { threeFingerStartAvgX = -1f; threeFingerSwipeTriggered = false; }
                    }
                    return false;
                }
            });
        } catch (Exception e) {
            Log.w("MainActivity", "Failed to attach gesture handlers to container", e);
        }
    }

    /**
     * Preload tabs 2 and 3 in the background after tab 1 has finished loading.
     * This ensures smooth tab switching without black screens or long load times.
     */
    private void preloadOtherTabs() {
        // Run on a background thread to avoid blocking the UI
        new Thread(() -> {
            try {
                Thread.sleep(500); // Small delay to let tab 1 settle
                
                // Preload tab 2
                String url2 = getHomeUrlForTab(2);
                if (url2 != null && !url2.trim().isEmpty() && tabWebViews[1] == null) {
                    Log.d("MainActivity", "Preloading tab 2: " + url2);
                    uiHandler.post(() -> {
                        try {
                            WebView tab2 = createTabWebView(2);
                            tabWebViews[1] = tab2;
                            tab2.loadUrl(url2);
                        } catch (Exception e) {
                            Log.e("MainActivity", "Failed to preload tab 2", e);
                        }
                    });
                }
                
                // Small delay between preloading tabs to avoid overwhelming network
                Thread.sleep(300);
                
                // Preload tab 3
                String url3 = getHomeUrlForTab(3);
                if (url3 != null && !url3.trim().isEmpty() && tabWebViews[2] == null) {
                    Log.d("MainActivity", "Preloading tab 3: " + url3);
                    uiHandler.post(() -> {
                        try {
                            WebView tab3 = createTabWebView(3);
                            tabWebViews[2] = tab3;
                            tab3.loadUrl(url3);
                        } catch (Exception e) {
                            Log.e("MainActivity", "Failed to preload tab 3", e);
                        }
                    });
                }
            } catch (Exception e) {
                Log.e("MainActivity", "Error during tab preloading", e);
            }
        }).start();
    }
        // Start polling the WebView URL to keep the address bar in sync (helps with SPA pushState updates)
        // URL synchronization will be handled by WebViewClient lifecycle callbacks (simple, reliable)

    @Override
    public void onDestroy() {
        try {
            if (prefs != null && prefListener != null) prefs.unregisterOnSharedPreferenceChangeListener(prefListener);
        } catch (Exception ignore) { }
        super.onDestroy();
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
                
                // IMPORTANT: Set tab 1 reference immediately so loadTabUrl doesn't create a duplicate
                try { tabWebViews[0] = wv; } catch (Exception ignored) { }
                
                try {
                    // If our layout contains a placeholder WebView (`R.id.main_webview`), replace it with the bridge WebView
                    View placeholder = findViewById(getResources().getIdentifier("main_webview", "id", getPackageName()));
                    String placeholderUrl = null;
                    if (placeholder instanceof WebView) {
                        try {
                            placeholderUrl = ((WebView) placeholder).getUrl();
                        } catch (Exception ignore) { }
                    }
                    if (placeholder != null && placeholder.getParent() instanceof android.view.ViewGroup) {
                        android.view.ViewGroup parent = (android.view.ViewGroup) placeholder.getParent();
                        int index = parent.indexOfChild(placeholder);
                        // Remove placeholder and insert bridge WebView in its place
                            parent.removeView(placeholder);
                            try {
                                // detach wv from any existing parent first
                                if (wv.getParent() instanceof android.view.ViewGroup) {
                                    ((android.view.ViewGroup) wv.getParent()).removeView(wv);
                                }
                            } catch (Exception ignore) { }
                            // Remember parent/index so we can swap tab WebViews without rebuilding layout
                            webviewParent = parent;
                            webviewIndexInParent = index;
                            parent.addView(wv, index);
                            // Tab 1 is already set in tabWebViews[0] above
                            
                            // Attach gesture handlers to the webviewParent container so they work
                            // regardless of which tab WebView is currently displayed
                            try {
                                attachGestureHandlersToContainer(webviewParent);
                            } catch (Exception e) {
                                Log.w("MainActivity", "Failed to attach gesture handlers to container", e);
                            }

                        // Prefer loading the active tab's configured home URL into the bridge WebView
                        try {
                            String desired = getHomeUrlForTab(currentTab);
                            if (desired != null && (wv.getUrl() == null || wv.getUrl().isEmpty() || !wv.getUrl().equals(desired))) {
                                Log.d("MainActivity", "Attaching bridge WebView - loading desired tab URL: " + desired);
                                try { wv.loadUrl(desired); } catch (Exception ignore) { }
                            }
                        } catch (Exception e) {
                            // Fallback: if something goes wrong, and placeholder had a URL, attempt to load it
                            try {
                                if (placeholderUrl != null && (wv.getUrl() == null || wv.getUrl().isEmpty() || !wv.getUrl().equals(placeholderUrl))) {
                                    wv.loadUrl(placeholderUrl);
                                }
                            } catch (Exception ignore) { }
                        }
                    }
                } catch (Exception e) {
                    Log.w("MainActivity", "Could not attach bridge WebView into layout placeholder", e);
                }

                // Attach gesture detection to WebView
                attachGestureHandlers(wv);

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
                
                // Tab 1: Use LOAD_DEFAULT (normal caching) unless aggressive caching is enabled
                // This ensures the game loads properly without stale cache issues
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                boolean aggressiveCaching = prefs.getBoolean("aggressive_caching", false);
                if (aggressiveCaching) {
                    ws.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
                    Log.d("MainActivity", "Tab 1: Using LOAD_CACHE_ELSE_NETWORK (aggressive caching enabled)");
                } else {
                    ws.setCacheMode(WebSettings.LOAD_DEFAULT);
                    Log.d("MainActivity", "Tab 1: Using LOAD_DEFAULT (normal mode)");
                }
                
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
                try {
                    // Keep a lightweight JS bridge for logging only. Do not update the address bar from JS.
                    wv.addJavascriptInterface(new Object() {
                        @android.webkit.JavascriptInterface
                        public void jsLog(final String msg) {
                            Log.i("JS_INTERFACE", "jsLog: " + msg);
                        }
                    }, "AndroidNative");
                } catch (Exception e) {
                    Log.w("MainActivity", "Failed to add JavascriptInterface", e);
                }
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
                        
                        // Inject wallet provider early, before page scripts run
                        try {
                            String walletScript = injectWalletProvider();
                            if (walletScript != null && !walletScript.isEmpty()) {
                                Log.d("MainActivity", "Injecting wallet provider at page start...");
                                view.evaluateJavascript(walletScript, null);
                            }
                        } catch (Exception e) {
                            Log.e("MainActivity", "Error injecting wallet at page start: " + e.getMessage());
                        }
                        
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
                        // Update URL bar unless user is editing it
                        try {
                            if (urlBar != null) {
                                Log.d("MainActivity", "Attempting to update urlBar in onPageStarted; urlBarUserEditing=" + urlBarUserEditing + " url=" + url);
                                if (!urlBarUserEditing) {
                                    uiHandler.post(() -> {
                                        try { urlBar.setText(url); }
                                        catch (Exception e) { Log.e("MainActivity", "Failed to set urlBar in onPageStarted", e); }
                                    });
                                }
                            }
                        } catch (Exception ignore) { }
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
                                    // Inject wallet provider based on user's default wallet selection
                                    String walletScript = injectWalletProvider();
                                    if (walletScript != null && !walletScript.isEmpty()) {
                                        Log.d("MainActivity", "Injecting wallet provider...");
                                        view.evaluateJavascript(walletScript, null);
                                    }
                                    
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
                                        "})();" +
                                        // (No SPA->native notifier: address bar is synced via WebViewClient callbacks only)
                                        "})();";

                                    Log.i("NOTIFICATION_DEBUG", "🚀 Injecting link interceptor JavaScript...");
                                    view.evaluateJavascript(script, null);
                                    Log.i("NOTIFICATION_DEBUG", "✅ Link interceptor injection completed");
                                } catch (Exception e) {
                                    Log.e("MainActivity", "Error injecting link interceptor script: " + e.getMessage(), e);
                                }
                            }
                        }, 100);  // 100ms delay to allow page to render first
                        // After page finished, update URL bar from the real WebView URL and update nav buttons
                        try {
                            if (urlBar != null) {
                                Log.d("MainActivity", "Attempting to update urlBar in onPageFinished; urlBarUserEditing=" + urlBarUserEditing + " url=" + url);
                                if (!urlBarUserEditing) {
                                    String current = (url != null && !url.isEmpty()) ? url : (mainWebView != null ? mainWebView.getUrl() : null);
                                    if (current != null) {
                                        uiHandler.post(() -> {
                                            try { urlBar.setText(current); }
                                            catch (Exception e) { Log.e("MainActivity", "Failed to set urlBar in onPageFinished", e); }
                                        });
                                    }
                                }
                            }
                            if (btnBack != null) btnBack.setEnabled(mainWebView != null && mainWebView.canGoBack());
                            if (btnForward != null) btnForward.setEnabled(mainWebView != null && mainWebView.canGoForward());
                        } catch (Exception ignore) { }

                        // NOTE: Deliberately do NOT perform JS polling/fallback here.
                        // Address bar updates are driven only by WebViewClient lifecycle
                        // callbacks (onPageStarted/onPageFinished) and by the user
                        // manually editing the `urlBar`. This prevents unexpected
                        // overwrites while the user is typing and keeps the WebView
                        // as the authoritative source of URL changes.
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
                                final String decoded = Uri.decode(msg);
                                Log.i("JS_CONSOLE", "(unified) " + decoded);
                                // Note: do not update the address bar from bridge log messages.
                            }
                            return true;
                        }
                        
                        // Handle wallet manager bridge URL (UNIFIED CLIENT)
                        if (url.startsWith("https://__native_bridge__/openWalletManager")) {
                            Log.d("MainActivity", "Opening Wallet Manager from game");
                            try {
                                Intent walletIntent = new Intent(MainActivity.this, 
                                    Class.forName("com.sfl.browser.WalletManagerActivity"));
                                startActivityForResult(walletIntent, 1001); // REQUEST_CODE for wallet connection
                            } catch (ClassNotFoundException e) {
                                Log.e("MainActivity", "Failed to launch WalletManagerActivity", e);
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
                                final String decoded = Uri.decode(msg);
                                Log.i("JS_CONSOLE", decoded);
                                // Do not update the address bar from bridge log messages.
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
        // Periodically clear cache to manage data usage
        clearOldCache();
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
        
        // Handle opening a URL in tab 1 (from link in Tutorial or other activities)
        if (intent != null && intent.hasExtra("load_url_in_tab_1")) {
            String urlToLoad = intent.getStringExtra("load_url_in_tab_1");
            Log.d("MainActivity", "handleIntent: Loading URL in tab 1: " + urlToLoad);
            if (urlToLoad != null) {
                uiHandler.post(() -> {
                    try {
                        // If we're not already on tab 1, switch to it
                        if (currentTab != 1) {
                            Log.d("MainActivity", "Switching from tab " + currentTab + " to tab 1");
                            currentTab = 1;
                            loadTabUrl(1, false);
                        }
                        // Load the URL in the main WebView (which should now be tab 1)
                        if (mainWebView != null) {
                            mainWebView.loadUrl(urlToLoad);
                            Log.d("MainActivity", "Loaded URL in tab 1: " + urlToLoad);
                        }
                    } catch (Exception e) {
                        Log.e("MainActivity", "Error loading URL in tab 1", e);
                    }
                });
            }
            return;
        }
        
        String action = intent.getAction();
        Uri data = intent.getData();
        Log.d("MainActivity", "Intent data: " + (data != null ? data.toString() : "null"));

        // Handle opening notification settings from the foreground notification click
        if ("com.sfl.browser.OPEN_NOTIFICATION_SETTINGS".equals(action)) {
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

    /**
     * Handle activity results from WalletManagerActivity
     * Receives wallet connection results and notifies the game
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        // REQUEST_CODE 1 is for tab grid overlay
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            int selectedTab = data.getIntExtra("selected_tab", currentTab);
            if (selectedTab >= 1 && selectedTab <= MAX_TABS && selectedTab != currentTab) {
                currentTab = selectedTab;
                Log.d("MainActivity", "Tab switched to " + currentTab + " from grid overlay");
                loadTabUrl(currentTab, false);
                try {
                    if (urlBar != null && !urlBarUserEditing) {
                        try { urlBar.setText(getHomeUrlForTab(currentTab)); } catch (Exception ignore) {}
                    }
                } catch (Exception ignore) { }
            }
            return;
        }
        
        // REQUEST_CODE 1001 is for wallet connection
        if (requestCode == 1001) {
            if (resultCode == RESULT_OK && data != null) {
                String walletId = data.getStringExtra("wallet_id");
                String walletName = data.getStringExtra("wallet_name");
                String address = data.getStringExtra("address");
                String chainId = data.getStringExtra("chain_id");
                
                Log.d("MainActivity", "Wallet connection result: " + walletName + " - " + address);
                
                // Notify game JavaScript about successful wallet connection
                if (mainWebView != null) {
                    String jsPayload = "window.dispatchEvent(new CustomEvent('walletConnected', { " +
                            "detail: { " +
                            "walletId: '" + walletId + "'," +
                            "walletName: '" + walletName + "'," +
                            "address: '" + (address != null ? address : "") + "'," +
                            "chainId: '" + (chainId != null ? chainId : "") + "'" +
                            "}}));";
                    
                    mainWebView.evaluateJavascript(jsPayload, null);
                }
            } else {
                Log.d("MainActivity", "Wallet connection cancelled");
                
                // Notify game JavaScript about cancellation
                if (mainWebView != null) {
                    mainWebView.evaluateJavascript(
                            "window.dispatchEvent(new CustomEvent('walletConnectionCancelled', {}));",
                            null);
                }
            }
        }
    }

    /**
     * Injects the selected wallet provider into the page JavaScript.
     * This tells the game which wallet is available.
     * Currently only supports MetaMask.
     */
    private String injectWalletProvider() {
        String walletId = com.sfl.browser.wallet.WalletPreferenceManager.getDefaultWalletId(this);
        String walletName = com.sfl.browser.wallet.WalletPreferenceManager.getDefaultWalletName(this);

        if (walletId == null || walletId.isEmpty()) {
            Log.d("MainActivity", "No default wallet selected");
            return "";
        }

        Log.d("MainActivity", "Injecting wallet provider: " + walletName + " (" + walletId + ")");

        if ("metamask".equals(walletId)) {
            // Inject MetaMask provider with proper structure
            return "(function() {" +
                    "console.log('Injecting MetaMask wallet provider');" +
                    "if (!window.ethereum) {" +
                    "  window.ethereum = {" +
                    "    isMetaMask: true," +
                    "    isConnected: function() { return true; }," +
                    "    request: function(req) { return Promise.resolve({}); }," +
                    "    on: function() {}," +
                    "    off: function() {}," +
                    "    removeListener: function() {}," +
                    "    send: function(payload, callback) { callback(null, {}); }," +
                    "    sendAsync: function(payload, callback) { callback(null, {}); }" +
                    "  };" +
                    "  console.log('MetaMask provider injected');" +
                    "}" +
                    "})();";
        }

        return "";
    }

    private void openSettingsPage() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    /**
     * Show tab overview - displays all 3 tabs scaled down in a grid for easy switching
     */
    private void showTabOverview() {
        try {
            // First, ensure all tabs are loaded
            for (int i = 0; i < MAX_TABS; i++) {
                if (tabWebViews[i] == null) {
                    tabWebViews[i] = createTabWebView(i + 1);
                    String url = getHomeUrlForTab(i + 1);
                    tabWebViews[i].loadUrl(url);
                    Log.d("MainActivity", "Preloading tab " + (i + 1) + ": " + url);
                }
            }

            // Create a full-screen overlay container
            android.widget.FrameLayout overlayContainer = new android.widget.FrameLayout(MainActivity.this);
            overlayContainer.setLayoutParams(new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            ));
            overlayContainer.setBackgroundColor(0xFF1a1a1a);
            overlayContainer.setTag("tab_overview_overlay");

            // Create a scroll view to hold tab previews
            android.widget.ScrollView scrollView = new android.widget.ScrollView(MainActivity.this);
            android.widget.FrameLayout.LayoutParams scrollParams = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            );
            scrollView.setLayoutParams(scrollParams);

            // Get screen dimensions for cards (2 per row)
            android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            int screenWidth = metrics.widthPixels;
            int cardWidth = (screenWidth - 32) / 2; // 2 columns with padding
            int cardHeight = (int) (cardWidth * 1.6f); // Portrait ratio (taller than wide)

            // Create a grid layout to hold tab cards in 2 columns
            android.widget.LinearLayout gridContainer = new android.widget.LinearLayout(MainActivity.this);
            gridContainer.setOrientation(android.widget.LinearLayout.VERTICAL);
            gridContainer.setClipChildren(true);
            gridContainer.setClipToPadding(true);
            android.widget.ScrollView.LayoutParams gridParams = new android.widget.ScrollView.LayoutParams(
                android.widget.ScrollView.LayoutParams.MATCH_PARENT,
                android.widget.ScrollView.LayoutParams.WRAP_CONTENT
            );
            gridContainer.setLayoutParams(gridParams);
            gridContainer.setPadding(8, 8, 8, 8);

            // Create 2 rows for 3 tabs (first row has 2, second row has 1)
            for (int row = 0; row < 2; row++) {
                android.widget.LinearLayout rowLayout = new android.widget.LinearLayout(MainActivity.this);
                rowLayout.setOrientation(android.widget.LinearLayout.HORIZONTAL);
                rowLayout.setClipChildren(true);
                rowLayout.setClipToPadding(true);
                android.widget.LinearLayout.LayoutParams rowParams = new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                );
                rowParams.setMargins(0, 4, 0, 4);
                rowLayout.setLayoutParams(rowParams);

                // Add up to 2 tabs per row
                for (int col = 0; col < 2; col++) {
                    int tabIndex = row * 2 + col;
                    if (tabIndex >= MAX_TABS) break;

                    final int finalTabIndex = tabIndex;
                    final int tabNumber = tabIndex + 1;

                    // Create container for the card (WebView + label)
                    android.widget.LinearLayout cardLayout = new android.widget.LinearLayout(MainActivity.this);
                    cardLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
                    cardLayout.setClipChildren(true);
                    cardLayout.setClipToPadding(true);
                    android.widget.LinearLayout.LayoutParams cardParams = new android.widget.LinearLayout.LayoutParams(
                        cardWidth,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    cardParams.setMargins(4, 0, 4, 0);
                    cardLayout.setLayoutParams(cardParams);
                    cardLayout.setBackgroundColor(0xFF2a2a2a);

                    // Create container for tab preview (will hold WebView)
                    android.widget.FrameLayout previewContainer = new android.widget.FrameLayout(MainActivity.this);
                    android.widget.LinearLayout.LayoutParams previewParams = new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        cardHeight
                    );
                    previewContainer.setLayoutParams(previewParams);
                    previewContainer.setClipChildren(true);
                    previewContainer.setClipToPadding(true);
                    previewContainer.setBackgroundColor(0xFF333333);

                    // Display actual tab content at scaled size (not a separate WebView)
                    if (tabWebViews[tabIndex] != null) {
                        WebView actualTabWebView = tabWebViews[tabIndex];
                        
                        // Save original dimensions
                        int originalWidth = actualTabWebView.getMeasuredWidth();
                        int originalHeight = actualTabWebView.getMeasuredHeight();
                        
                        // Temporarily remove from parent if needed
                        android.view.ViewGroup originalParent = (android.view.ViewGroup) actualTabWebView.getParent();
                        if (originalParent != null) {
                            originalParent.removeView(actualTabWebView);
                        }
                        
                        // Size the WebView much larger than the preview
                        int scaledWebViewWidth = (int) (cardWidth * 3.33f);
                        int scaledWebViewHeight = (int) (cardHeight * 3.33f);
                        
                        android.widget.FrameLayout.LayoutParams previewWebViewParams = new android.widget.FrameLayout.LayoutParams(
                            scaledWebViewWidth,
                            scaledWebViewHeight
                        );
                        previewWebViewParams.gravity = android.view.Gravity.TOP | android.view.Gravity.LEFT;
                        previewContainer.addView(actualTabWebView, previewWebViewParams);
                        
                        // Scale down to fit preview
                        actualTabWebView.setScaleX(0.3f);
                        actualTabWebView.setScaleY(0.3f);
                        actualTabWebView.setPivotX(0);
                        actualTabWebView.setPivotY(0);
                        
                        // Disable interactions
                        actualTabWebView.setEnabled(false);
                        actualTabWebView.setClickable(false);
                        actualTabWebView.setLongClickable(false);
                        actualTabWebView.setFocusable(false);
                        actualTabWebView.setFocusableInTouchMode(false);
                        actualTabWebView.setOnTouchListener((v, event) -> true);
                    }
                    
                    // Create a transparent touch blocker overlay on top of the WebView preview
                    android.view.View touchBlockerPreview = new android.view.View(MainActivity.this);
                    android.widget.FrameLayout.LayoutParams blockerPreviewParams = new android.widget.FrameLayout.LayoutParams(
                        cardWidth,
                        cardHeight
                    );
                    blockerPreviewParams.gravity = android.view.Gravity.TOP | android.view.Gravity.LEFT;
                    touchBlockerPreview.setLayoutParams(blockerPreviewParams);
                    touchBlockerPreview.setBackgroundColor(0x00000000); // Fully transparent
                    
                    // Set click listener on the touch blocker to select this tab
                    touchBlockerPreview.setOnClickListener(v -> {
                        Log.d("MainActivity", "Tab " + tabNumber + " clicked");
                        currentTab = tabNumber;
                        
                        // Restore all tab WebViews to original state (undo scaling)
                        for (int i = 0; i < MAX_TABS; i++) {
                            if (tabWebViews[i] != null) {
                                tabWebViews[i].setScaleX(1.0f);
                                tabWebViews[i].setScaleY(1.0f);
                                tabWebViews[i].setEnabled(true);
                                tabWebViews[i].setClickable(true);
                                tabWebViews[i].setFocusable(true);
                                tabWebViews[i].setFocusableInTouchMode(true);
                                // Re-attach gesture handlers instead of clearing them
                                attachGestureHandlers(tabWebViews[i]);
                            }
                        }
                        
                        // Remove overlay first
                        android.view.View parent = findViewById(getResources().getIdentifier("root_coordinator", "id", getPackageName()));
                        if (parent instanceof android.view.ViewGroup) {
                            ((android.view.ViewGroup) parent).removeView(overlayContainer);
                        }
                        
                        // Move selected tab to main container
                        if (tabWebViews[finalTabIndex].getParent() != null) {
                            ((android.view.ViewGroup) tabWebViews[finalTabIndex].getParent()).removeView(tabWebViews[finalTabIndex]);
                        }
                        if (webviewParent != null) {
                            webviewParent.removeView(mainWebView);
                            
                            // Ensure proper layout parameters for the tab WebView
                            android.widget.LinearLayout.LayoutParams layoutParams = new android.widget.LinearLayout.LayoutParams(
                                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                                0
                            );
                            layoutParams.weight = 1;
                            tabWebViews[finalTabIndex].setLayoutParams(layoutParams);
                            
                            webviewParent.addView(tabWebViews[finalTabIndex], webviewIndexInParent);
                            Log.d("MainActivity", "Added tab " + tabNumber + " to main container with proper layout params");
                        }
                        mainWebView = tabWebViews[finalTabIndex];
                        
                        try {
                            if (urlBar != null && !urlBarUserEditing) {
                                try { urlBar.setText(mainWebView.getUrl()); } catch (Exception ignore) {}
                            }
                        } catch (Exception ignore) { }
                        
                        Log.d("MainActivity", "Now showing tab " + tabNumber);
                    });                    previewContainer.addView(touchBlockerPreview);

                    cardLayout.addView(previewContainer);

                    // Add tab title at the bottom (will show page title)
                    android.widget.TextView tabLabel = new android.widget.TextView(MainActivity.this);
                    String pageTitle = "Tab " + tabNumber;
                    if (tabWebViews[tabIndex] != null) {
                        String title = tabWebViews[tabIndex].getTitle();
                        if (title != null && !title.isEmpty()) {
                            pageTitle = title;
                        }
                    }
                    tabLabel.setText(pageTitle);
                    tabLabel.setTextColor(0xFFC9B26D);
                    tabLabel.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
                    tabLabel.setTypeface(null, android.graphics.Typeface.BOLD);
                    tabLabel.setPadding(8, 6, 8, 6);
                    tabLabel.setMaxLines(2);
                    tabLabel.setEllipsize(android.text.TextUtils.TruncateAt.END);
                    tabLabel.setGravity(android.view.Gravity.CENTER);
                    android.widget.LinearLayout.LayoutParams labelParams = new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                    cardLayout.addView(tabLabel, labelParams);

                    rowLayout.addView(cardLayout);
                }

                gridContainer.addView(rowLayout);
            }

            scrollView.addView(gridContainer);
            overlayContainer.addView(scrollView);

            // Add overlay to root view
            android.view.View rootView = findViewById(getResources().getIdentifier("root_coordinator", "id", getPackageName()));
            if (rootView instanceof android.view.ViewGroup) {
                ((android.view.ViewGroup) rootView).addView(overlayContainer);
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error showing tab overview", e);
        }
    }

    /**
     * Periodically clear old cache to manage data usage.
     * Cache is cleared every 7 days to balance between performance and data usage.
     * Uses LOAD_DEFAULT cache mode which respects server Cache-Control headers.
     */
    private void clearOldCache() {
        try {
            long lastCacheClear = prefs.getLong("last_cache_clear_time", 0);
            long currentTime = System.currentTimeMillis();
            long SEVEN_DAYS_MS = 7 * 24 * 60 * 60 * 1000L;
            
            // Clear cache if it hasn't been cleared in 7 days
            if (currentTime - lastCacheClear > SEVEN_DAYS_MS) {
                Log.d("MainActivity", "Cache is stale (> 7 days) - clearing to manage data usage");
                
                // Clear WebView cache
                if (mainWebView != null) {
                    try {
                        mainWebView.clearCache(false); // false = keep only disk cache, clear memory
                    } catch (Exception e) {
                        Log.w("MainActivity", "Error clearing mainWebView cache", e);
                    }
                }
                
                // Clear cache for all tab WebViews
                for (int i = 0; i < tabWebViews.length; i++) {
                    if (tabWebViews[i] != null) {
                        try {
                            tabWebViews[i].clearCache(false);
                        } catch (Exception e) {
                            Log.w("MainActivity", "Error clearing tab " + (i+1) + " WebView cache", e);
                        }
                    }
                }
                
                // Update timestamp
                prefs.edit().putLong("last_cache_clear_time", currentTime).apply();
                Log.d("MainActivity", "Cache cleared successfully");
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error in clearOldCache", e);
        }
    }
}