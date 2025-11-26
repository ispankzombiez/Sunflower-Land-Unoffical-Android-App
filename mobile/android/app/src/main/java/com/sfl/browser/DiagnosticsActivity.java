package com.sfl.browser;

import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.content.Context;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import androidx.work.WorkManager;
import androidx.work.WorkInfo;
import java.util.List;
import com.sfl.browser.ServiceUtils;
import com.sfl.browser.WorkManagerHelper;

public class DiagnosticsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Diagnostics");
        ScrollView scrollView = new ScrollView(this);
        TextView textView = new TextView(this);
        textView.setTextIsSelectable(true);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        int topPadding = (int) (100 * getResources().getDisplayMetrics().density);
        textView.setPadding(padding, topPadding, padding, padding);
        // ...existing code...
        StringBuilder sb = new StringBuilder();
        sb.append("Diagnostics\n\n");

        // Device info
        sb.append("Device: ").append(android.os.Build.MANUFACTURER).append(" ").append(android.os.Build.MODEL).append("\n");
        sb.append("Android Version: ").append(android.os.Build.VERSION.RELEASE).append(" (API ").append(android.os.Build.VERSION.SDK_INT).append(")\n");
        sb.append("Time: ").append(java.text.DateFormat.getDateTimeInstance().format(new java.util.Date())).append("\n");
        try {
            int targetSdk = getApplicationContext().getApplicationInfo().targetSdkVersion;
            sb.append("Target SDK: ").append(targetSdk).append("\n");
        } catch (Exception e) {
            sb.append("Target SDK: unknown\n");
        }
        sb.append("\n");

        // Permissions
        sb.append("Permissions:\n");
        sb.append("  POST_NOTIFICATIONS: ").append(androidx.core.content.ContextCompat.checkSelfPermission(this, "android.permission.POST_NOTIFICATIONS") == android.content.pm.PackageManager.PERMISSION_GRANTED).append("\n");
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            sb.append("  SCHEDULE_EXACT_ALARM: ");
            android.app.AlarmManager alarmManager = (android.app.AlarmManager) getSystemService(ALARM_SERVICE);
            boolean canExact = alarmManager != null && alarmManager.canScheduleExactAlarms();
            sb.append(canExact).append("\n");
        }

        // Battery optimization
        String packageName = getPackageName();
        android.os.PowerManager pm = (android.os.PowerManager) getSystemService(POWER_SERVICE);
        boolean ignoringBatteryOpt = false;
        if (pm != null) {
            try {
                ignoringBatteryOpt = pm.isIgnoringBatteryOptimizations(packageName);
            } catch (Exception e) { }
        }
        sb.append("Battery Optimization: ").append(ignoringBatteryOpt ? "IGNORED" : "ENABLED").append("\n");

        // Power saving mode
        boolean powerSave = pm != null && pm.isPowerSaveMode();
        sb.append("Power Saving Mode: ").append(powerSave ? "ON (may delay alarms)" : "OFF").append("\n");

        // Do Not Disturb (DND) mode
        android.app.NotificationManager notificationManager = (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        boolean dndEnabled = false;
        if (notificationManager != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                dndEnabled = notificationManager.getCurrentInterruptionFilter() != android.app.NotificationManager.INTERRUPTION_FILTER_ALL;
            }
        }
        sb.append("Do Not Disturb: ").append(dndEnabled ? "ON" : "OFF").append("\n");

        // Number of active notification workers
        try {
            int workerCount = 0;
            int maxWorkers = 20;
            for (int i = 0; i < maxWorkers; i++) {
                java.util.List<androidx.work.WorkInfo> workInfos = androidx.work.WorkManager.getInstance(this)
                        .getWorkInfosForUniqueWork("farm_notification_worker_" + i)
                        .get();
                if (workInfos != null && !workInfos.isEmpty()) {
                    androidx.work.WorkInfo.State state = workInfos.get(0).getState();
                    if (state == androidx.work.WorkInfo.State.ENQUEUED || state == androidx.work.WorkInfo.State.RUNNING) {
                        workerCount++;
                    }
                }
            }
            sb.append("Number of Workers: ").append(workerCount).append("\n");
        } catch (Exception e) {
            sb.append("Number of Workers: error\n");
        }

        // Help blurb
        sb.append("\n");
        sb.append("---\n");
        sb.append("Trouble getting notifications?\n");
        sb.append("\n");
        sb.append("• Check battery optimization and background restrictions for this app in your device settings.\n");
        sb.append("• On most devices: Settings > Apps > Sunflower Land > Battery > Battery Optimization or Background Restriction.\n");
        sb.append("• Ensure Do Not Disturb is off if you want notifications.\n");
        sb.append("• On some devices, look for 'Auto-start' or 'App launch' and allow the app to run in the background.\n");
        sb.append("• If using Data Saver, whitelist this app.\n");
        sb.append("\n");
        textView.setText(sb.toString());
        scrollView.addView(textView);
        setContentView(scrollView);
    }
}
