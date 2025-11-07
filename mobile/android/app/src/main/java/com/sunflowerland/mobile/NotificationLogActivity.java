package com.sunflowerland.mobile;

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
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Displays the notification log in chronological order (soonest ready times first)
 */
public class NotificationLogActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        ScrollView scrollView = new ScrollView(this);
        TextView textView = new TextView(this);
        textView.setPadding(16, 96, 16, 16);
        textView.setTextSize(12);
        
        String logContent = readAndSortLogFile("notification_summary.log");
        textView.setText(logContent);
        
        scrollView.addView(textView);
        setContentView(scrollView);
    }
    
    /**
     * Read log file and sort notifications chronologically (soonest first)
     */
    private String readAndSortLogFile(String filename) {
        try {
            File logFile = new File(getFilesDir(), filename);
            if (!logFile.exists()) {
                return "No log file found: " + filename;
            }
            
            // Read all lines
            List<String> lines = new ArrayList<>();
            BufferedReader reader = new BufferedReader(new FileReader(logFile));
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            reader.close();
            
            // Find header and notification entries
            StringBuilder header = new StringBuilder();
            List<NotificationEntry> notifications = new ArrayList<>();
            
            boolean inHeader = true;
            for (String logLine : lines) {
                if (inHeader) {
                    if (logLine.startsWith("[")) {
                        inHeader = false;
                        // Parse this line as notification
                        NotificationEntry entry = parseNotificationLine(logLine);
                        if (entry != null) {
                            notifications.add(entry);
                        }
                    } else if (!logLine.equals("(No upcoming notifications scheduled)")) {
                        header.append(logLine).append("\n");
                    }
                } else if (logLine.startsWith("[")) {
                    // Parse as notification
                    NotificationEntry entry = parseNotificationLine(logLine);
                    if (entry != null) {
                        notifications.add(entry);
                    }
                } else if (!logLine.trim().isEmpty()) {
                    // Additional info line
                }
            }
            
            // Sort notifications by ready time (soonest first)
            Collections.sort(notifications, (a, b) -> Long.compare(a.readyTimeMs, b.readyTimeMs));
            
            // Rebuild output with sorted notifications
            StringBuilder result = new StringBuilder();
            result.append(header);
            
            if (notifications.isEmpty()) {
                result.append("(No upcoming notifications scheduled)\n");
            } else {
                for (NotificationEntry entry : notifications) {
                    result.append(entry.originalLine).append("\n");
                }
            }
            
            return result.toString();
        } catch (IOException e) {
            return "Error reading log: " + e.getMessage();
        }
    }
    
    /**
     * Parse a notification line to extract time for sorting
     * Format: "[h:mm a] quantity name (ready in Xm)"
     */
    private NotificationEntry parseNotificationLine(String line) {
        try {
            // Extract time from "[h:mm a]"
            int timeStart = line.indexOf("[");
            int timeEnd = line.indexOf("]");
            if (timeStart == -1 || timeEnd == -1) {
                return null;
            }
            
            String timeStr = line.substring(timeStart + 1, timeEnd);
            
            // Parse the time string to get milliseconds
            // We assume it's today's time, so we calculate from current day start
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.US);
            long readyTimeMs = sdf.parse(timeStr).getTime();
            
            // Adjust for today's date
            long currentTimeMs = System.currentTimeMillis();
            long todayStartMs = (currentTimeMs / (24 * 60 * 60 * 1000)) * (24 * 60 * 60 * 1000);
            long todayReadyTimeMs = todayStartMs + (readyTimeMs % (24 * 60 * 60 * 1000));
            
            // If time is in the past today, assume it's tomorrow
            if (todayReadyTimeMs < currentTimeMs) {
                todayReadyTimeMs += 24 * 60 * 60 * 1000;
            }
            
            return new NotificationEntry(line, todayReadyTimeMs);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Helper class to hold notification entry data
     */
    private static class NotificationEntry {
        String originalLine;
        long readyTimeMs;
        
        NotificationEntry(String line, long timeMs) {
            this.originalLine = line;
            this.readyTimeMs = timeMs;
        }
    }
}

