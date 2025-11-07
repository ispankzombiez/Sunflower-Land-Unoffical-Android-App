package com.sunflowerland.mobile;

import android.app.ActivityManager;
import android.content.Context;
import android.util.Log;

public class ServiceUtils {
    private static final String TAG = "ServiceUtils";
    
    /**
     * Check if the NotificationManagerService is currently running
     */
    public static boolean isNotificationManagerServiceRunning(Context context) {
        try {
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager != null) {
                for (ActivityManager.RunningServiceInfo serviceInfo : activityManager.getRunningServices(Integer.MAX_VALUE)) {
                    if (NotificationManagerService.class.getName().equals(serviceInfo.service.getClassName())) {
                        Log.d(TAG, "NotificationManagerService is running");
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking if service is running: " + e.getMessage(), e);
        }
        Log.d(TAG, "NotificationManagerService is NOT running");
        return false;
    }
}
