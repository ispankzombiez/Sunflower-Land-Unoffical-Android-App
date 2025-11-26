package com.sfl.browser;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * PeriodicWorkRequest worker for scheduled farm data processing.
 * Executes at regular intervals as determined by refresh_time setting.
 * 
 * Multiple instances of this worker can be staggered to achieve custom refresh rates:
 * - refresh_time = 5 min → 3 staggered workers every 15 min = effective 5-min checks
 * - refresh_time = 10 min → 2 staggered workers every 15 min = effective 10-min checks
 * - refresh_time = 15+ min → 1 worker every N minutes
 * 
 * This worker handles:
 * 1. API call to fetch raw farm data
 * 2. Parse and extract farm items by category
 * 3. Cluster items by readiness time
 * 4. Schedule notifications for ready items
 * 5. Log all processing steps
 */
public class NotificationWorker extends Worker {
    private static final String TAG = "NotificationWorker";

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        int workerId = getInputData().getInt("worker_id", -1);
        
        // Ensure DebugLog is initialized in this worker process
        DebugLog.init(getApplicationContext());
        
        Log.d(TAG, "=== PERIODIC Execution - Worker #" + workerId + " Starting ===");
        DebugLog.log("=== PERIODIC Execution - Worker #" + workerId + " Starting ===");
        
        try {
            // Process farm data using cached settings from input data
            FarmDataProcessor.processFarmDataFromWorker(getApplicationContext(), getInputData());
            
            Log.d(TAG, "✅ Worker #" + workerId + " - PERIODIC execution completed successfully");
            DebugLog.log("✅ Worker #" + workerId + " - PERIODIC execution completed successfully");
            
            // Flush buffered logs to disk (single write operation)
            DebugLog.flush(getApplicationContext());
            
            return Result.success();
        } catch (RuntimeException e) {
            // API failure - log and retry with custom backoff
            if (e.getMessage() != null && e.getMessage().contains("API call failed")) {
                Log.w(TAG, "⚠️  Worker #" + workerId + " - API call failed, retrying in ~30 seconds...");
                DebugLog.log("⚠️  Worker #" + workerId + " - API call failed, retrying in ~30 seconds...");
                DebugLog.flush(getApplicationContext());
                
                // Return retry with automatic exponential backoff
                return Result.retry();
            } else {
                // Other runtime exceptions = actual errors
                Log.e(TAG, "❌ Worker #" + workerId + " - Processing error: " + e.getMessage(), e);
                DebugLog.error("❌ Worker #" + workerId + " - Processing error", e);
                DebugLog.flush(getApplicationContext());
                return Result.retry();
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Worker #" + workerId + " - PERIODIC execution error: " + e.getMessage(), e);
            DebugLog.error("❌ Worker #" + workerId + " - PERIODIC execution error", e);
            
            // Flush logs before retry
            DebugLog.flush(getApplicationContext());
            
            // WorkManager will automatically retry with exponential backoff
            return Result.retry();
        }
    }
}

