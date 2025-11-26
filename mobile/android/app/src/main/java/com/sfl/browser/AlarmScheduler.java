package com.sfl.browser;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import com.sfl.browser.clustering.NotificationGroup;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Handles scheduling of AlarmManager intents for persistent, system-managed notifications.
 * 
 * Design:
 * 1. App extracts crops and calculates readyTime
 * 2. App calls scheduleNotificationAlarms() to register alarms with the system
 * 3. System wakes the device at readyTime and sends broadcast to NotificationReceiver
 * 4. NotificationReceiver posts the notification (works even if app is closed)
 * 
 * Benefits:
 * - Notifications persist even if app is killed
 * - System manages delivery at exact readyTime
 * - No app-triggered delivery needed
 * - Battery efficient: system handles wakeup and scheduling
 * 
 * Deduplication:
 * - Tracks scheduled group IDs to prevent duplicate alarms
 * - Clears duplicates if same groupId with earlier readyTime is scheduled again
 */
public class AlarmScheduler {
    private static final String TAG = "AlarmScheduler";
    private static final String ACTION_FARM_NOTIFICATION = "com.sfl.browser.FARM_NOTIFICATION";
    private static final int NOTIFICATION_ID_BASE = 5000; // Base ID to avoid conflicts
    private static final String PREFS_NAME = "alarm_scheduler_prefs";
    private static final String PREFS_KEY_SCHEDULED = "scheduled_group_ids";
    
    private Context context;
    private AlarmManager alarmManager;
    private SharedPreferences prefs;
    
    public AlarmScheduler(Context context) {
        this.context = context;
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Schedules alarms for a list of notification groups.
     * Prevents duplicate notifications by tracking already-scheduled group IDs.
     * Each alarm will fire at the group's earliestReadyTime.
     * 
     * @param groups List of NotificationGroup objects to schedule
     */
    public void scheduleNotificationAlarms(List<NotificationGroup> groups) {
        if (groups == null) {
            Log.d(TAG, "No groups to schedule");
            return;
        }
        
        if (groups.isEmpty()) {
            Log.d(TAG, "No groups to schedule");
            return;
        }
        
        Log.d(TAG, "Processing " + groups.size() + " notification group(s)...");
        
        // Get previously scheduled group IDs to prevent duplicates
        Set<String> scheduledIds = prefs.getStringSet(PREFS_KEY_SCHEDULED, new HashSet<>());
        Set<String> newScheduledIds = new HashSet<>();
        
        for (NotificationGroup group : groups) {
            try {
                long currentTime = System.currentTimeMillis();
                long readyTime = group.earliestReadyTime;
                String groupId = group.groupId;
                
                // For marketplace notifications, deliver immediately (they're for past sales)
                // For other notifications, skip if readyTime has already passed
                if (readyTime <= currentTime && !"marketplace".equals(group.category)) {
                    Log.d(TAG, "Skipping " + group.name + " - ready time already passed");
                    continue;
                }
                
                // Check if this exact group is already scheduled to prevent duplicates
                if (scheduledIds.contains(groupId)) {
                    Log.d(TAG, "Skipping " + group.name + " (ID: " + groupId + ") - already scheduled to prevent duplicates");
                    newScheduledIds.add(groupId);
                    continue;
                }
                
                // For marketplace notifications, fire immediately
                if ("marketplace".equals(group.category)) {
                    Log.d(TAG, "Firing marketplace notification immediately: " + group.name);
                    deliverNotificationNow(group);
                } else {
                    // Schedule the alarm for future notifications
                    scheduleAlarmForGroup(group);
                }
                newScheduledIds.add(groupId);
                
            } catch (Exception e) {
                Log.e(TAG, "Error processing group: " + e.getMessage(), e);
            }
        }
        
        // Update the set of scheduled group IDs
        prefs.edit().putStringSet(PREFS_KEY_SCHEDULED, newScheduledIds).apply();
        Log.d(TAG, "Scheduled " + newScheduledIds.size() + " new alarm(s). Total scheduled: " + newScheduledIds.size());
    }
    
    /**
     * Schedules a single alarm for a notification group
     */
    private void scheduleAlarmForGroup(NotificationGroup group) {
        try {
            long currentTime = System.currentTimeMillis();
            long readyTime = group.earliestReadyTime;
            
            // Generate unique notification ID based on groupId hash
            int notificationId = NOTIFICATION_ID_BASE + Math.abs(group.groupId.hashCode() % 1000);
            
            Log.d(TAG, "Scheduling alarm for: " + group.name + " (category: " + group.category + 
                  ", ID: " + notificationId + ", readyTime: " + readyTime + ", delayMs: " + (readyTime - currentTime) + ")");
            
            // Create intent with notification data
            Intent intent = new Intent(context, NotificationReceiver.class);
            intent.setAction(ACTION_FARM_NOTIFICATION);
            intent.putExtra("notificationId", notificationId);
            intent.putExtra("itemName", group.name);
            intent.putExtra("category", group.category);
            intent.putExtra("count", group.quantity);
            intent.putExtra("groupId", group.groupId);
            intent.putExtra("details", group.details);  // Pass optional details
            
            // Set title and body - use custom format for marketplace
            if ("marketplace".equals(group.category)) {
                intent.putExtra("title", group.quantity + " " + group.name + " Sold!");
                // Extract SFL value from details (format: "50 Milk for 6.9999 SFL")
                String sflBody = "Listing sold"; // fallback
                if (group.details != null && group.details.contains(" for ")) {
                    String[] parts = group.details.split(" for ");
                    if (parts.length > 1) {
                        sflBody = parts[1]; // e.g., "6.9999 SFL"
                    }
                }
                intent.putExtra("body", sflBody);
            } else {
                intent.putExtra("title", group.quantity + " " + group.name + " Ready");
                intent.putExtra("body", "Ready to harvest/collect");
            }
            
            Log.d(TAG, "Intent extras set: title=" + intent.getStringExtra("title") + 
                  ", category=" + intent.getStringExtra("category") + 
                  ", itemName=" + intent.getStringExtra("itemName"));
            
            // Create PendingIntent with FLAG_IMMUTABLE for security
            // Use FLAG_UPDATE_CURRENT to replace any existing alarm for this ID
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    notificationId,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                );

                // Create a broadcast PendingIntent for notification click
                Intent clickIntent = new Intent(context, NotificationReceiver.class);
                clickIntent.setAction("com.sfl.browser.ACTION_NOTIFICATION_CLICK");
                clickIntent.putExtra("notificationId", notificationId);
                clickIntent.putExtra("title", intent.getStringExtra("title"));
                clickIntent.putExtra("body", intent.getStringExtra("body"));
                clickIntent.putExtra("itemName", intent.getStringExtra("itemName"));
                clickIntent.putExtra("category", intent.getStringExtra("category"));
                clickIntent.putExtra("groupId", intent.getStringExtra("groupId"));
                clickIntent.putExtra("details", intent.getStringExtra("details"));
                clickIntent.putExtra("count", intent.getIntExtra("count", 1));
                PendingIntent clickPendingIntent = PendingIntent.getBroadcast(
                    context,
                    notificationId + 10000, // ensure unique requestCode for click
                    clickIntent,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                );
            
            Log.d(TAG, "PendingIntent created: " + (pendingIntent != null ? "SUCCESS" : "FAILED"));
            
            // Schedule alarm to fire at readyTime with maximum precision
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    // API 31+ requires SCHEDULE_EXACT_ALARM permission check
                    if (alarmManager != null && alarmManager.canScheduleExactAlarms()) {
                        // Priority: Use setExactAndAllowWhileIdle first (most reliable)
                        alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                readyTime,
                                pendingIntent
                        );
                        long delayMs = readyTime - currentTime;
                        Log.d(TAG, "✅ Scheduled EXACT alarm (setExactAndAllowWhileIdle) for " + group.name + 
                              " - fires in " + (delayMs / 1000) + " seconds (ID: " + notificationId + ")");
                    } else if (alarmManager != null) {
                        // Fallback: Use setAndAllowWhileIdle for Doze compatibility
                        alarmManager.setAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                readyTime,
                                pendingIntent
                        );
                        long delayMs = readyTime - currentTime;
                        Log.d(TAG, "⚠️  Scheduled alarm (setAndAllowWhileIdle - exact not available) for " + group.name + 
                              " - fires in " + (delayMs / 1000) + " seconds (ID: " + notificationId + ")");
                    }
                } else {
                    // Pre-API 31 - use setExactAndAllowWhileIdle for best precision
                    if (alarmManager != null) {
                        alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                readyTime,
                                pendingIntent
                        );
                        long delayMs = readyTime - currentTime;
                        Log.d(TAG, "✅ Scheduled EXACT alarm (pre-API 31) for " + group.name + 
                              " - fires in " + (delayMs / 1000) + " seconds (ID: " + notificationId + ")");
                    }
                }
                
            } catch (SecurityException e) {
                Log.e(TAG, "❌ SecurityException scheduling alarm: " + e.getMessage() + 
                      " (missing SCHEDULE_EXACT_ALARM permission?)");
                // Fallback to inexact alarm
                if (alarmManager != null) {
                    alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            readyTime,
                            pendingIntent
                    );
                    long delayMs = readyTime - currentTime;
                    Log.d(TAG, "⚠️  Scheduled alarm with fallback (inexact) for " + group.name + 
                          " - fires in " + (delayMs / 1000) + " seconds (ID: " + notificationId + ")");
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error scheduling alarm for " + group.name + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Delivers a notification immediately without scheduling an alarm
     * Used for marketplace notifications that have already occurred
     */
    private void deliverNotificationNow(NotificationGroup group) {
        try {
            long currentTime = System.currentTimeMillis();
            long readyTime = group.earliestReadyTime;
            
            // Generate notification ID based on group ID
            int notificationId = NOTIFICATION_ID_BASE + Math.abs(group.groupId.hashCode() % 1000);
            
            Intent intent = new Intent(context, NotificationReceiver.class);
            intent.setAction(ACTION_FARM_NOTIFICATION);
            intent.putExtra("notificationId", notificationId);
            intent.putExtra("itemName", group.name);
            intent.putExtra("category", group.category);
            intent.putExtra("count", group.quantity);
            intent.putExtra("groupId", group.groupId);
            intent.putExtra("details", group.details);  // Pass optional details
            
            // Set title and body for marketplace notification
            intent.putExtra("title", group.quantity + " " + group.name + " Sold!");
            // Extract SFL value from details (format: "50 Milk for 6.9999 SFL")
            String sflBody = "Listing sold"; // fallback
            if (group.details != null && group.details.contains(" for ")) {
                String[] parts = group.details.split(" for ");
                if (parts.length > 1) {
                    sflBody = parts[1]; // e.g., "6.9999 SFL"
                }
            }
            intent.putExtra("body", sflBody);
            
            Log.d(TAG, "Intent extras set for immediate delivery: title=" + intent.getStringExtra("title") + 
                  ", body=" + intent.getStringExtra("body") +
                  ", category=" + intent.getStringExtra("category") + 
                  ", itemName=" + intent.getStringExtra("itemName"));
            
            // Send broadcast immediately to trigger notification
            context.sendBroadcast(intent);
            Log.d(TAG, "🎉 Delivered marketplace notification immediately for " + group.name + 
                  " (ID: " + notificationId + ")");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error delivering notification immediately for " + group.name + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Clears the scheduled alarms tracking (call this when clearing/resetting notifications)
     */
    public void clearScheduledTracking() {
        prefs.edit().remove(PREFS_KEY_SCHEDULED).apply();
        Log.d(TAG, "Cleared scheduled alarms tracking");
    }
    
    /**
     * Cancels all pending alarms from the system.
     * This is important to remove old stale alarms that may have been scheduled in previous runs.
     * We iterate through a range of possible notification IDs and cancel each one.
     */
    public void cancelAllPendingAlarms() {
        try {
            Log.d(TAG, "Attempting to cancel all pending alarms...");
            
            // We need to cancel all possible notification IDs that could have been scheduled
            // The IDs range from NOTIFICATION_ID_BASE to NOTIFICATION_ID_BASE + 1000
            for (int i = 0; i < 1000; i++) {
                int notificationId = NOTIFICATION_ID_BASE + i;
                
                Intent intent = new Intent(context, NotificationReceiver.class);
                intent.setAction(ACTION_FARM_NOTIFICATION);
                
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        context,
                        notificationId,
                        intent,
                        PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
                );
                
                // If the pending intent exists, cancel it
                if (pendingIntent != null && alarmManager != null) {
                    alarmManager.cancel(pendingIntent);
                    pendingIntent.cancel();
                    Log.d(TAG, "Cancelled alarm with ID: " + notificationId);
                }
            }
            
            Log.d(TAG, "Completed cancellation of all pending alarms (attempted 1000 IDs)");
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling pending alarms: " + e.getMessage(), e);
        }
    }
}
