package com.sunflowerland.mobile;

import android.content.Context;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * General debug log utility for app-wide logging.
 * 
 * Features:
 * - Immediate file writing (logs written as they happen, not buffered)
 * - Auto-clears every 24 hours at 00:00 UTC (when daily reset notification fires)
 * - Thread-safe logging
 * - Includes timestamps for all entries
 * 
 * Usage:
 *   DebugLog.init(context)  // Call once at app startup
 *   DebugLog.log("WorkManager: Processing started")
 *   DebugLog.logStep("Step 1", "API call initiated")
 *   DebugLog.error("Failed to process", exception)
 */
public class DebugLog {
    private static final String TAG = "DebugLog";
    private static final String DEBUG_LOG_FILE = "general_debug.log";
    private static final Object LOCK = new Object();
    
    // Static context reference (initialized at app startup)
    private static Context appContext = null;

    /**
     * Initialize DebugLog with application context
     * Must be called once at app startup
     */
    public static void init(Context context) {
        synchronized (LOCK) {
            if (appContext == null) {
                appContext = context.getApplicationContext();
                Log.d(TAG, "DebugLog initialized");
            }
        }
    }

    /**
     * Log a general message with timestamp (written immediately to file)
     */
    public static void log(String message) {
        synchronized (LOCK) {
            try {
                Log.d(TAG, "IMMEDIATE LOG: " + message);  // Also log to Android logcat
                writeToFile(formatMessage("INFO", message));
            } catch (Exception e) {
                Log.e(TAG, "Error writing log: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Log a processing step (written immediately to file)
     */
    public static void logStep(String step, String message) {
        synchronized (LOCK) {
            try {
                String stepLog = "[" + step + "] " + message;
                Log.d(TAG, "IMMEDIATE STEP: " + stepLog);  // Also log to Android logcat
                writeToFile(formatMessage("STEP", stepLog));
            } catch (Exception e) {
                Log.e(TAG, "Error writing log: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Log an error with optional exception details (written immediately to file)
     */
    public static void error(String message, Exception e) {
        synchronized (LOCK) {
            try {
                String errorMsg = message;
                if (e != null) {
                    errorMsg += " [" + e.getClass().getSimpleName() + ": " + e.getMessage() + "]";
                }
                Log.e(TAG, "IMMEDIATE ERROR: " + errorMsg, e);  // Also log to Android logcat
                writeToFile(formatMessage("ERROR", errorMsg));
            } catch (Exception ex) {
                Log.e(TAG, "Error writing error log: " + ex.getMessage(), ex);
            }
        }
    }

    /**
     * Log a warning (written immediately to file)
     */
    public static void warning(String message) {
        synchronized (LOCK) {
            try {
                Log.w(TAG, "IMMEDIATE WARNING: " + message);  // Also log to Android logcat
                writeToFile(formatMessage("WARN", message));
            } catch (Exception e) {
                Log.e(TAG, "Error writing log: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Write a single log entry to file immediately
     */
    private static void writeToFile(String message) throws IOException {
        if (appContext == null) {
            Log.w(TAG, "⚠️  DebugLog.appContext is NULL - cannot write: " + message);
            return;
        }
        File logFile = new File(appContext.getFilesDir(), DEBUG_LOG_FILE);
        Log.d(TAG, "📝 Writing to log file: " + logFile.getAbsolutePath() + " | File exists: " + logFile.exists() + " | Size: " + logFile.length());
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.append(message).append("\n");
            writer.flush();
            Log.d(TAG, "✅ Successfully wrote to log file");
        } catch (IOException io) {
            Log.e(TAG, "❌ IOException writing to log file: " + io.getMessage(), io);
            throw io;
        }
    }

    /**
     * Flush method - now a no-op since logs are written immediately
     * Kept for compatibility with existing code
     */
    public static void flush(Context context) {
        // Logs are now written immediately, nothing to flush
        // This method kept for backward compatibility
    }

    /**
     * Get current buffer size (always 0 now since no buffering)
     */
    public static int getBufferSize() {
        return 0;  // No buffering anymore
    }

    /**
     * Clear the debug log file (called every 24 hours at 00:00 UTC)
     */
    public static void clearLog(Context context) {
        synchronized (LOCK) {
            try {
                // Log WHO is calling clearLog by getting the stack trace
                StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                String caller = "Unknown";
                if (stackTrace.length > 2) {
                    caller = stackTrace[2].getClassName() + "." + stackTrace[2].getMethodName() + ":" + stackTrace[2].getLineNumber();
                }
                Log.d(TAG, "⚠️  DEBUG LOG BEING CLEARED by: " + caller);
                
                File logFile = new File(context.getFilesDir(), DEBUG_LOG_FILE);
                if (logFile.exists()) {
                    logFile.delete();
                }
                
                // Log the clear action
                StringBuilder logBuffer = new StringBuilder();
                logBuffer.append(formatMessage("SYSTEM", "Debug log cleared (called from: " + caller + ")")).append("\n");
                
                File newLogFile = new File(context.getFilesDir(), DEBUG_LOG_FILE);
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(newLogFile, true))) {
                    writer.append(logBuffer.toString());
                    writer.flush();
                }
                
                Log.d(TAG, "Debug log cleared successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error clearing debug log: " + e.getMessage());
            }
        }
    }

    /**
     * Get the entire debug log content
     */
    public static String getDebugLog(Context context) {
        synchronized (LOCK) {
            try {
                File logFile = new File(context.getFilesDir(), DEBUG_LOG_FILE);
                Log.d(TAG, "📖 Reading log file: " + logFile.getAbsolutePath() + " | Exists: " + logFile.exists() + " | Size: " + logFile.length());
                
                if (!logFile.exists()) {
                    Log.w(TAG, "Log file does not exist");
                    return "[Debug log not found]";
                }

                StringBuilder sb = new StringBuilder();
                try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(logFile))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                }
                Log.d(TAG, "✅ Successfully read " + sb.length() + " bytes from log file");
                return sb.toString();
            } catch (Exception e) {
                return "[Error reading debug log: " + e.getMessage() + "]";
            }
        }
    }

    /**
     * Format a log message with timestamp and level
     */
    private static String formatMessage(String level, String message) {
        String timestamp = getCurrentTimestamp();
        return "[" + timestamp + "] [" + level + "] " + message;
    }

    /**
     * Return current timestamp in MM/dd HH:mm:ss format
     */
    private static String getCurrentTimestamp() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm:ss", Locale.US);
            return sdf.format(new Date());
        } catch (Exception e) {
            return "00/00 00:00:00";
        }
    }
}
