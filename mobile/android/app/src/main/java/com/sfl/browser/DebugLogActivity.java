package com.sfl.browser;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.ScrollView;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Environment;
import android.widget.Toast;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Activity to display the general debug log file
 * Shows all app processing steps logged by DebugLog utility
 * Allows viewing, clearing, and exporting the log
 */
public class DebugLogActivity extends AppCompatActivity {
    private TextView debugLogTextView;
    private ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug_log);

        // Ensure DebugLog is initialized in this activity context
        DebugLog.init(this);

        // Set up the action bar title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("General Debug Log");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        debugLogTextView = findViewById(R.id.debug_log_text);
        scrollView = findViewById(R.id.debug_log_scroll);
        Button clearButton = findViewById(R.id.clear_debug_log_button);
        Button exportButton = findViewById(R.id.export_debug_log_button);

        // Load and display the debug log
        loadDebugLog();

        // Clear button handler
        if (clearButton != null) {
            clearButton.setOnClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(DebugLogActivity.this)
                    .setTitle("Clear Debug Log")
                    .setMessage("Are you sure you want to clear the debug log?")
                    .setPositiveButton("Clear", (dialog, which) -> {
                        DebugLog.log("🗑️ [MANUAL CLEAR] User clicked 'Clear Debug Log' button - clearing all logs");
                        DebugLog.clearLog(DebugLogActivity.this);
                        loadDebugLog();
                        android.widget.Toast.makeText(DebugLogActivity.this, "Debug log cleared", android.widget.Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .show();
            });
        }

        // Export button handler
        if (exportButton != null) {
            exportButton.setOnClickListener(v -> exportLogToFile());
        }
    }

    /**
     * Load and display the debug log file
     */
    private void loadDebugLog() {
        String logContent = DebugLog.getDebugLog(this);
        
        if (logContent == null || logContent.isEmpty() || logContent.contains("not found")) {
            debugLogTextView.setText("(No debug log entries yet)\n\nDebug logs will appear here as the app processes farm data.");
        } else {
            debugLogTextView.setText(logContent);
            // Scroll to bottom to show latest entries
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        }
    }

    /**
     * Export the debug log to a file in the Download folder
     */
    private void exportLogToFile() {
        try {
            // Get log content
            String logContent = DebugLog.getDebugLog(this);
            if (logContent == null || logContent.isEmpty() || logContent.contains("not found")) {
                Toast.makeText(this, "No log content to export", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create filename with timestamp
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(new Date());
            String filename = "sunflower_debug_" + timestamp + ".txt";

            // Get Download folder
            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadDir.exists()) {
                downloadDir.mkdirs();
            }

            // Create file
            File logFile = new File(downloadDir, filename);

            // Write log content to file
            try (FileWriter writer = new FileWriter(logFile)) {
                writer.write(logContent);
            }

            Toast.makeText(this, "Log exported to Downloads/" + filename, Toast.LENGTH_LONG).show();
            android.util.Log.i("DebugLogActivity", "✅ Log exported to: " + logFile.getAbsolutePath());

        } catch (IOException e) {
            Toast.makeText(this, "Failed to export log: " + e.getMessage(), Toast.LENGTH_LONG).show();
            android.util.Log.e("DebugLogActivity", "❌ Error exporting log: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
