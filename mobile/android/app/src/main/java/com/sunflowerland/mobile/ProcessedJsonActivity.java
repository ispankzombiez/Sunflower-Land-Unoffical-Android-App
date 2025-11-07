package com.sunflowerland.mobile;

import android.os.Bundle;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class ProcessedJsonActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        ScrollView scrollView = new ScrollView(this);
        TextView textView = new TextView(this);
        textView.setPadding(16, 96, 16, 16);
        textView.setTextSize(12);
        
        String logContent = readLogFile("future_notifications.json");
        textView.setText(logContent);
        
        scrollView.addView(textView);
        setContentView(scrollView);
    }
    
    private String readLogFile(String filename) {
        try {
            File logFile = new File(getFilesDir(), filename);
            if (!logFile.exists()) {
                return "No log file found: " + filename;
            }
            
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(logFile));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            
            return sb.toString();
        } catch (IOException e) {
            return "Error reading log: " + e.getMessage();
        }
    }
}
