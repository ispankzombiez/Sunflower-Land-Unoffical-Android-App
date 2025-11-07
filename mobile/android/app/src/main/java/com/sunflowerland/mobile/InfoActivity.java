package com.sunflowerland.mobile;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ImageButton;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Html;
import android.text.Spanned;

public class InfoActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        ScrollView scrollView = new ScrollView(this);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(16, 96, 16, 16);
        
        // Main info text
        TextView textView = new TextView(this);
        textView.setTextSize(14);
        textView.setLineSpacing(1.5f, 1.5f);
        
        String htmlContent = "This app was developed solely by community member: <b><u><a href=\"https://sunflower-land.com/play/#/visit/1128976301583508\">iSPANK</a></u></b><br/><br/>" +
                "This app is designed as an access point and notification delivery system for the online game Sunflower-Land.com.<br/><br/>" +
                "I am not directly affiliated with the Sunflower-Land Team.<br/>" +
                "I do not claim any connection, or any rights to their software, hardware, or website.<br/>" +
                "This app has been developed 100% without any involvement from the official team.<br/><br/>" +
                "This app is supplied free of charge, no strings attached.<br/>" +
                "However, if you would like to donate to the further developement of this app, you can do so by sending to this address:";
        
        Spanned spannedText = Html.fromHtml(htmlContent, Html.FROM_HTML_MODE_LEGACY);
        textView.setText(spannedText);
        textView.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
        
        container.addView(textView);
        
        // Ethereum address display with copy button
        LinearLayout addressContainer = new LinearLayout(this);
        addressContainer.setOrientation(LinearLayout.HORIZONTAL);
        addressContainer.setPadding(0, 16, 0, 0);
        
        TextView addressView = new TextView(this);
        addressView.setText("0xa1B7...EafE");
        addressView.setTextSize(14);
        addressView.setTypeface(android.graphics.Typeface.MONOSPACE);
        LinearLayout.LayoutParams addressParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            60  // Match button height
        );
        addressParams.setMargins(0, 0, 12, 0);
        addressView.setGravity(android.view.Gravity.CENTER_VERTICAL);
        addressView.setLayoutParams(addressParams);
        
        // Use ImageButton for proper icon display
        ImageButton copyButton = new ImageButton(this);
        copyButton.setImageResource(R.drawable.copy_icon);
        copyButton.setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"));
        copyButton.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
        copyButton.setPadding(8, 8, 8, 8);
        
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
            60, // button width
            60  // button height
        );
        copyButton.setLayoutParams(btnParams);
        
        final String fullAddress = "0xa1B7786847410C677fadB526D476d787EA4DEafE";
        copyButton.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Ethereum Address", fullAddress);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(InfoActivity.this, "Address copied to clipboard", Toast.LENGTH_SHORT).show();
        });
        
        addressContainer.addView(addressView);
        addressContainer.addView(copyButton);
        
        container.addView(addressContainer);
        
        scrollView.addView(container);
        setContentView(scrollView);
    }
}
