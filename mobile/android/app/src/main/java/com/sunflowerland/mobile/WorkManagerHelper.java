package com.sunflowerland.mobile;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Helper class to manage WorkManager scheduling for notification processing.
 * Implements staggered scheduling to achieve custom refresh rates while respecting
 * WorkManager's 15-minute minimum constraint.
 * 
 * Staggered Scheduling Strategy:
 * - If refresh_time = 5 minutes, schedule 3 workers with 0, 5, 10-minute offsets
 * - Each worker runs every 15 minutes (WorkManager minimum)
 * - Combined effect: API checks every 5 minutes
 * 
 * Features:
 * - Schedule multiple staggered workers based on refresh_time
 * - Cancel all scheduled workers
 * - Check if workers are scheduled
 * - Comprehensive logging for debugging
 */
public class WorkManagerHelper {
    private static final String TAG = "WorkManagerHelper";
    private static final String WORK_ID_PREFIX = "farm_notification_worker_";
    private static final String WORK_TAG = "farm_notification_work";
    private static final long WORKMANAGER_MIN_INTERVAL_MINUTES = 15;

    /**
     * Schedule staggered notification workers based on refresh_time setting.
     * 
     * @param context Application context
     * @return true if successfully scheduled, false otherwise
     */
    public static boolean scheduleNotificationWorker(Context context) {
        return scheduleNotificationWorkerWithSource(context, "manual");
    }

    /**
     * Schedule hybrid notification system: OneTime immediate + Periodic future runs.
     * 
     * One-Time Worker (Immediate Execution):
     * - Executes as soon as WorkManager can schedule (typically 1-5 seconds)
     * - Used for manual "Start" button to give instant feedback
     * - Fetches latest data and updates logs immediately
     * 
     * Periodic Workers (Future Scheduled Runs):
     * - Multiple staggered workers for custom refresh rates
     * - Respects user's refresh_time setting
     * - Takes over after first immediate execution
     *
     * @param context Application context
     * @param source "manual" (user clicked button) or "auto" (MainActivity auto-start)
     * @return true if successfully scheduled, false otherwise
     */
    public static boolean scheduleNotificationWorkerWithSource(Context context, String source) {
        try {
            // Cancel all existing workers first to avoid duplicates
            cancelAllNotificationWorkers(context);

            // Get refresh time from settings (default 300 seconds = 5 minutes)
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            String refreshTimeStr = prefs.getString("refresh_time", "300");
            long refreshTimeSeconds = 300;

            try {
                refreshTimeSeconds = Long.parseLong(refreshTimeStr);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Invalid refresh_time value: " + refreshTimeStr + ", using default 300 seconds");
                refreshTimeSeconds = 300;
            }

            // Calculate how many staggered workers we need
            // Workers needed = ceiling(15 minutes / refresh interval)
            long refreshTimeMinutes = refreshTimeSeconds / 60;
            int workersNeeded = (int) Math.ceil((double) WORKMANAGER_MIN_INTERVAL_MINUTES / Math.max(1, refreshTimeMinutes));

            Log.d(TAG, "📊 Hybrid WorkManager Setup:");
            Log.d(TAG, "   Source: " + source);
            Log.d(TAG, "   Refresh time from settings: " + refreshTimeSeconds + " seconds (" + refreshTimeMinutes + " minutes)");
            Log.d(TAG, "   WorkManager minimum interval: " + WORKMANAGER_MIN_INTERVAL_MINUTES + " minutes");
            Log.d(TAG, "   Periodic workers needed: " + workersNeeded);

            // Prepare cached settings as input data
            Data.Builder inputDataBuilder = new Data.Builder();
            inputDataBuilder.putString("farm_id", prefs.getString("farm_id", ""));
            inputDataBuilder.putString("api_key", prefs.getString("api_key", ""));
            inputDataBuilder.putLong("refresh_time_seconds", refreshTimeSeconds);
            inputDataBuilder.putString("source", source);

            // STEP 1: Schedule ONE-TIME immediate worker (only for manual start)
            if (source.equals("manual")) {
                Log.d(TAG, "   📍 Scheduling ONE-TIME immediate execution worker...");
                Data immediateInputData = inputDataBuilder
                        .putInt("worker_id", -1)  // -1 indicates immediate worker
                        .build();

                OneTimeWorkRequest immediateWork = new OneTimeWorkRequest.Builder(ImmediateNotificationWorker.class)
                        .setInputData(immediateInputData)
                        .addTag(WORK_TAG)
                        .build();

                WorkManager.getInstance(context).enqueue(immediateWork);
                Log.d(TAG, "      ✅ ONE-TIME worker enqueued");
                Log.d(TAG, "      First API call: IMMEDIATELY (as soon as WorkManager schedules)");
            }

            // STEP 2: Schedule PERIODIC staggered workers for future runs
            Log.d(TAG, "   📍 Scheduling " + workersNeeded + " PERIODIC staggered worker(s)...");

            // Determine initial delay for first periodic worker
            long baseInitialDelaySeconds = source.equals("manual") ? 
                    (WORKMANAGER_MIN_INTERVAL_MINUTES * 60) :  // Manual: periodic starts after 15 min (after immediate runs once)
                    30;  // Auto-start: first periodic starts in 30 seconds

            for (int i = 0; i < workersNeeded; i++) {
                // Calculate initial delay for this worker
                long workerDelayMinutes = i * refreshTimeMinutes;
                long totalInitialDelaySeconds = baseInitialDelaySeconds + (workerDelayMinutes * 60);

                String workerId = WORK_ID_PREFIX + i;

                // Set worker ID in input data
                Data workerInputData = inputDataBuilder
                        .putInt("worker_id", i)
                        .build();

                // Create periodic work request
                PeriodicWorkRequest notificationWork = new PeriodicWorkRequest.Builder(
                        NotificationWorker.class,
                        WORKMANAGER_MIN_INTERVAL_MINUTES,
                        TimeUnit.MINUTES
                )
                .setInputData(workerInputData)
                .addTag(WORK_TAG)
                .setInitialDelay(totalInitialDelaySeconds, TimeUnit.SECONDS)
                .build();

                // Enqueue with KEEP policy
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                        workerId,
                        ExistingPeriodicWorkPolicy.KEEP,
                        notificationWork
                );

                Log.d(TAG, "      ✅ Periodic Worker " + i + " scheduled");
                Log.d(TAG, "         Work ID: " + workerId);
                Log.d(TAG, "         Initial delay: " + totalInitialDelaySeconds + " seconds (" + (totalInitialDelaySeconds / 60) + "m " + (totalInitialDelaySeconds % 60) + "s)");
                Log.d(TAG, "         Interval: " + WORKMANAGER_MIN_INTERVAL_MINUTES + " minutes");
            }

            Log.d(TAG, "✅ Hybrid WorkManager scheduled successfully");
            Log.d(TAG, "   Source: " + source);
            Log.d(TAG, "   Execution pattern:");
            if (source.equals("manual")) {
                Log.d(TAG, "      - Immediate: ONE-TIME worker (1-5 seconds)");
                Log.d(TAG, "      - Then: Periodic workers every " + refreshTimeMinutes + " minutes (starting after 15min)");
            } else {
                Log.d(TAG, "      - Periodic workers only (starting in 30 seconds)");
                Log.d(TAG, "      - Then: Every " + refreshTimeMinutes + " minutes");
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "❌ Error scheduling WorkManager task: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Cancel all scheduled notification workers.
     * Stops all future periodic executions of staggered workers.
     *
     * @param context Application context
     * @return true if successfully cancelled, false otherwise
     */
    public static boolean cancelNotificationWorker(Context context) {
        try {
            cancelAllNotificationWorkers(context);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "❌ Error cancelling WorkManager tasks: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Internal method to cancel all staggered workers.
     * Queries WorkManager for all workers with the WORK_TAG and cancels them.
     *
     * @param context Application context
     */
    private static void cancelAllNotificationWorkers(Context context) {
        try {
            WorkManager workManager = WorkManager.getInstance(context);
            
            // Cancel up to 20 workers (should be more than enough)
            for (int i = 0; i < 20; i++) {
                String workerId = WORK_ID_PREFIX + i;
                workManager.cancelUniqueWork(workerId);
            }
            
            Log.d(TAG, "✅ All WorkManager workers cancelled successfully");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error cancelling all WorkManager workers: " + e.getMessage(), e);
        }
    }

    /**
     * Check if any notification worker is currently scheduled.
     * Queries WorkManager to see if at least one worker is in the queue.
     *
     * @param context Application context
     * @return true if at least one worker is scheduled, false otherwise
     */
    public static boolean isNotificationWorkerScheduled(Context context) {
        try {
            WorkManager workManager = WorkManager.getInstance(context);
            
            // Check if worker 0 exists (first worker indicator)
            List<WorkInfo> workInfos = workManager
                    .getWorkInfosForUniqueWork(WORK_ID_PREFIX + 0)
                    .get();

            boolean isScheduled = workInfos != null && !workInfos.isEmpty();

            if (isScheduled) {
                WorkInfo workInfo = workInfos.get(0);
                String state = workInfo.getState().toString();
                Log.d(TAG, "✅ WorkManager is currently scheduled");
                Log.d(TAG, "   First worker state: " + state);
            } else {
                Log.d(TAG, "ℹ️  WorkManager is NOT currently scheduled");
            }

            return isScheduled;
        } catch (Exception e) {
            Log.e(TAG, "❌ Error checking WorkManager status: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get detailed status of the scheduled workers.
     * Useful for debugging and logging current state.
     *
     * @param context Application context
     * @return Status string describing current work state
     */
    public static String getWorkStatus(Context context) {
        try {
            List<WorkInfo> workInfos = WorkManager.getInstance(context)
                    .getWorkInfosForUniqueWork(WORK_ID_PREFIX + 0)
                    .get();

            if (workInfos == null || workInfos.isEmpty()) {
                return "NOT_SCHEDULED";
            }

            WorkInfo workInfo = workInfos.get(0);
            return workInfo.getState().toString();
        } catch (Exception e) {
            Log.e(TAG, "Error getting work status: " + e.getMessage());
            return "ERROR";
        }
    }

    /**
     * Log the current refresh interval and worker configuration.
     * Useful for verifying the staggered workers are set up correctly.
     *
     * @param context Application context
     */
    public static void logCurrentRefreshInterval(Context context) {
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            String refreshTimeStr = prefs.getString("refresh_time", "300");
            long refreshTimeSeconds = 300;

            try {
                refreshTimeSeconds = Long.parseLong(refreshTimeStr);
            } catch (NumberFormatException e) {
                Log.w(TAG, "Invalid refresh_time in settings: " + refreshTimeStr);
            }

            long refreshTimeMinutes = refreshTimeSeconds / 60;
            int workersNeeded = (int) Math.ceil((double) WORKMANAGER_MIN_INTERVAL_MINUTES / Math.max(1, refreshTimeMinutes));

            Log.d(TAG, "📊 Current Refresh Configuration:");
            Log.d(TAG, "   Settings value: " + refreshTimeSeconds + " seconds (" + refreshTimeMinutes + " minutes)");
            Log.d(TAG, "   Staggered workers: " + workersNeeded);
            Log.d(TAG, "   Each worker interval: " + WORKMANAGER_MIN_INTERVAL_MINUTES + " minutes");
            Log.d(TAG, "   Effective refresh rate: every " + refreshTimeMinutes + " minutes");
            Log.d(TAG, "   Initial delays: 0");
            for (int i = 1; i < workersNeeded; i++) {
                Log.d(TAG, "                    " + (i * refreshTimeMinutes) + " minutes");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error logging refresh interval: " + e.getMessage());
        }
    }
}
