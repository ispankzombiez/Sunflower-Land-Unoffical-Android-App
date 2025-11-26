package com.sfl.browser;

import android.os.Bundle;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

/**
 * Displays the notification log in chronological order (soonest ready times first)
 */
public class NotificationLogActivity extends AppCompatActivity {
    private android.os.Handler handler;
    private Runnable updateRunnable;
    private TextView textView;
    private ScrollView scrollView;
    private static final int UPDATE_INTERVAL_MS = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        scrollView = new ScrollView(this);
        textView = new TextView(this);
        textView.setPadding(16, 96, 16, 16);
        textView.setTextSize(12);

        scrollView.addView(textView);
        setContentView(scrollView);

        handler = new android.os.Handler();
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                String logContent = readAndSortLogFile("notification_summary.log");
                textView.setText(logContent);
                handler.postDelayed(this, UPDATE_INTERVAL_MS);
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(updateRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(updateRunnable);
    }
    
    /**
     * Read log file and sort notifications chronologically (soonest first)
     */
    private long lastLogReadTime = 0;
    private long lastLogFileModified = 0;
    private List<NotificationEntry> cachedNotifications = new ArrayList<>();
    private String cachedGeneratedAt = "";
    private static final int LOG_READ_INTERVAL_MS = 5000;

    private String readAndSortLogFile(String filename) {
        long now = System.currentTimeMillis();
        File logFile = new File(getFilesDir(), filename);
        long fileLastModified = logFile.exists() ? logFile.lastModified() : 0;
        boolean needReload = false;
        if (!logFile.exists()) {
            return "No log file found: " + filename;
        }
        if (cachedNotifications.isEmpty() || fileLastModified != lastLogFileModified) {
            needReload = true;
        } else if (now - lastLogReadTime > LOG_READ_INTERVAL_MS) {
            // Only update countdowns, not the delivery time
            lastLogReadTime = now;
        }
        if (needReload) {
            try {
                List<String> lines = new ArrayList<>();
                BufferedReader reader = new BufferedReader(new FileReader(logFile));
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
                reader.close();
                String generatedAt = "";
                List<NotificationEntry> notifications = new ArrayList<>();
                long generatedTimestamp = 0L;
                for (String logLine : lines) {
                    if (logLine.startsWith("Generated at:")) {
                        generatedAt = logLine;
                        String ts = logLine.replace("Generated at:", "").trim();
                        try {
                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            generatedTimestamp = sdf.parse(ts).getTime();
                        } catch (Exception ignore) {}
                    } else if (logLine.startsWith("[")) {
                        NotificationEntry entry = parseNotificationLine(logLine, generatedTimestamp);
                        if (entry != null && entry.getRemainingMs() > 0) {
                            notifications.add(entry);
                        }
                    }
                }
                Collections.sort(notifications, (a, b) -> Long.compare(a.getRemainingMs(), b.getRemainingMs()));
                cachedNotifications = notifications;
                cachedGeneratedAt = generatedAt;
                lastLogReadTime = now;
                lastLogFileModified = fileLastModified;
            } catch (IOException e) {
                return "Error reading log: " + e.getMessage();
            }
        }
        // Now, update countdowns based on current time, but never update delivery time
        StringBuilder result = new StringBuilder();
        result.append("📋 UPCOMING NOTIFICATIONS\n");
        result.append("════════════════════════════════════════\n\n");
        result.append(cachedGeneratedAt).append("\n\n");
        if (cachedNotifications.isEmpty()) {
            result.append("No upcoming notifications scheduled.\n");
        } else {
            for (NotificationEntry entry : cachedNotifications) {
                result.append(entry.formatForDisplayStaticTime(System.currentTimeMillis())).append("\n\n");
            }
        }
        return result.toString();
    }
    
    /**
     * Parse a notification line to extract remaining time for sorting
     * Format: "[h:mm a] quantity name (ready in Xm)" or "(ready in Xh Xm)" or "(now)"
     */
    private NotificationEntry parseNotificationLine(String line, long generatedTimestamp) {
        try {
            // Extract time from "[HH:mm:ss]"
            int timeStart = line.indexOf("[");
            int timeEnd = line.indexOf("]");
            if (timeStart == -1 || timeEnd == -1) {
                return null;
            }
            String readyTimeRaw = line.substring(timeStart + 1, timeEnd); // Expecting HH:mm:ss as written in the log
            // Parse readyTimeRaw as HH:mm:ss to get the target time (today or next occurrence)
            // But for display, just use as-is

            // Extract remaining time from "(ready in ...)"
            int readyInStart = line.indexOf("(ready in ");
            if (readyInStart == -1) {
                return null;
            }
            int readyInEnd = line.indexOf(")", readyInStart);
            if (readyInEnd == -1) {
                return null;
            }
            String readyInStr = line.substring(readyInStart + 10, readyInEnd).trim();
            long offsetMs = 0;
            if (readyInStr.equals("now")) {
                offsetMs = 0;
            } else {
                // Parse "Xh Ym" or "Xm" format
                String[] parts = readyInStr.split("\\s+");
                for (String part : parts) {
                    if (part.endsWith("h")) {
                        long hours = Long.parseLong(part.substring(0, part.length() - 1));
                        offsetMs += hours * 60 * 60 * 1000;
                    } else if (part.endsWith("m")) {
                        long minutes = Long.parseLong(part.substring(0, part.length() - 1));
                        offsetMs += minutes * 60 * 1000;
                    }
                }
            }
            // Extract quantity and name
            String afterTime = line.substring(timeEnd + 1).trim();
            int parenStart = afterTime.indexOf("(ready in");
            String itemInfo = afterTime.substring(0, parenStart).trim();
            String[] parts = itemInfo.split("\\s+", 2);
            int quantity = 1;
            String itemName = itemInfo;
            if (parts.length == 2) {
                try {
                    quantity = Integer.parseInt(parts[0]);
                    itemName = parts[1];
                } catch (NumberFormatException e) {
                    itemName = itemInfo;
                    quantity = 1;
                }
            }
            // Calculate target time
            long targetTime = generatedTimestamp > 0 ? generatedTimestamp + offsetMs : System.currentTimeMillis() + offsetMs;
            return new NotificationEntry(line, targetTime, itemName, quantity, readyTimeRaw);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Helper class to hold notification entry data
     */
    private static class NotificationEntry {
        String originalLine;
        long targetTime;
        String itemName;
        int quantity;
        String deliveryTime;

        NotificationEntry(String line, long targetTime, String name, int qty, String deliveryTimeRaw) {
            this.originalLine = line;
            this.targetTime = targetTime;
            this.itemName = name;
            this.quantity = qty;
            // Convert deliveryTimeRaw (HH:mm:ss) to 12-hour format with AM/PM
            String formatted = deliveryTimeRaw;
            try {
                java.text.SimpleDateFormat inFormat = new java.text.SimpleDateFormat("HH:mm:ss");
                java.text.SimpleDateFormat outFormat = new java.text.SimpleDateFormat("h:mm:ss a");
                java.util.Date date = inFormat.parse(deliveryTimeRaw);
                formatted = outFormat.format(date);
            } catch (Exception ignored) {}
            this.deliveryTime = formatted;
        }

        long getRemainingMs() {
            return targetTime - System.currentTimeMillis();
        }

        String formatForDisplayStaticTime(long now) {
            long remainingMs = targetTime - now;
            String timeRemaining;
            if (remainingMs <= 0) {
                timeRemaining = "0s";
            } else {
                long hours = remainingMs / (60 * 60 * 1000);
                long minutes = (remainingMs % (60 * 60 * 1000)) / (60 * 1000);
                long seconds = (remainingMs % (60 * 1000)) / 1000;
                if (hours > 0) {
                    timeRemaining = String.format("%dh %dm %ds", hours, minutes, seconds);
                } else if (minutes > 0) {
                    timeRemaining = String.format("%dm %ds", minutes, seconds);
                } else {
                    timeRemaining = String.format("%ds", seconds);
                }
            }
            // Show the static delivery time (does not update)
            return String.format("%s - %d %s - (%s)",
                deliveryTime, quantity, itemName, timeRemaining);
        }
    }
}

