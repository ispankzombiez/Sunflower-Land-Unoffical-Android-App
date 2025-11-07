package com.sunflowerland.mobile;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * OneTime worker for immediate farm data processing.
 * Executes once as soon as WorkManager can schedule it (typically within 1-5 seconds).
 * 
 * Used when:
 * - User clicks "Start Notification Manager" button
 * - App startup if workers not already scheduled
 * 
 * After this executes, PeriodicWorkers take over for future scheduled runs.
 */
public class ImmediateNotificationWorker extends Worker {
    private static final String TAG = "ImmediateNotificationWorker";

    public ImmediateNotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Ensure DebugLog is initialized in this worker process
        DebugLog.init(getApplicationContext());
        
        Log.d(TAG, "=== IMMEDIATE Execution - One-Time Farm Data Processing ===");
        DebugLog.log("=== IMMEDIATE Execution - One-Time Farm Data Processing ===");
        
        try {
            // Process farm data using cached settings from input data
            FarmDataProcessor.processFarmDataFromWorker(getApplicationContext(), getInputData());
            
            Log.d(TAG, "✅ IMMEDIATE execution completed successfully");
            DebugLog.log("✅ IMMEDIATE execution completed successfully");
            
            // Flush buffered logs to disk (single write operation)
            DebugLog.flush(getApplicationContext());
            
            return Result.success();
        } catch (RuntimeException e) {
            // API failure - retry instead of failing
            if (e.getMessage() != null && e.getMessage().contains("API call failed")) {
                Log.w(TAG, "⚠️  IMMEDIATE execution - API call failed, will retry...");
                DebugLog.log("⚠️  IMMEDIATE execution - API call failed, will retry...");
                DebugLog.flush(getApplicationContext());
                
                // Retry on API failure (even though this is one-time)
                return Result.retry();
            } else {
                // Other runtime exceptions = actual errors, don't retry
                Log.e(TAG, "❌ IMMEDIATE execution error: " + e.getMessage(), e);
                DebugLog.error("❌ IMMEDIATE execution error", e);
                DebugLog.flush(getApplicationContext());
                return Result.failure();
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ IMMEDIATE execution error: " + e.getMessage(), e);
            DebugLog.error("❌ IMMEDIATE execution error", e);
            
            // Flush logs before failing
            DebugLog.flush(getApplicationContext());
            
            // Don't retry one-time execution for other exceptions
            return Result.failure();
        }
    }
}
