package com.sfl.browser;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageButton;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;

public class InfoActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        
        // Set up the toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // Enable back button in action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Info");
        }
        
        LinearLayout container = findViewById(R.id.info_container);
        container.setPadding(0, 0, 0, 0);
        
        // Main info text (before address)
        TextView textView = new TextView(this);
        textView.setTextSize(16);
        textView.setLineSpacing(1.5f, 1.5f);
        textView.setPadding(0, 16, 0, 16);
        String htmlContent1 = "<h3 style=\"color: #c9b26d; text-align: center; margin: 0 0 16px 0;\">👨‍💻 Developer Info</h3>" +
            "<p style=\"color: #e6e6e6; margin: 8px 0;\">This app was developed solely by community member: <b style=\"color: #bfa14a;\"><u>iSPANK</u></b></p>" +
            "<p style=\"color: #e6e6e6; margin: 8px 0;\">This app is designed as an access point and notification delivery system for the online game Sunflower-Land.com.</p>" +
            "<p style=\"color: #e6e6e6; margin: 8px 0;\">I am not directly affiliated with the Sunflower-Land Team. I do not claim any connection, or any rights to their software, hardware, or website.</p>" +
            "<p style=\"color: #e6e6e6; margin: 8px 0;\">This app has been developed 100% without any involvement from the official team.</p>" +
            "<p style=\"color: #e6e6e6; margin: 8px 0;\">This app is supplied free of charge, no strings attached.</p>" +
            "<p style=\"color: #e6c97a; margin: 16px 0 8px 0;\"><b>However, if you would like to help the developer further develop this app, you can do so by:</b></p>";
        Spanned spannedText1 = Html.fromHtml(htmlContent1, Html.FROM_HTML_MODE_LEGACY);
        textView.setText(spannedText1);
        textView.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
        container.addView(textView);
        
        // Horizontal wrapper for "Donate to this address:" + address + button on same line
        LinearLayout donateLineContainer = new LinearLayout(this);
        donateLineContainer.setOrientation(LinearLayout.HORIZONTAL);
        donateLineContainer.setPadding(0, 8, 0, 16);
        
        // "Donate to this address:" label
        TextView donateLabel = new TextView(this);
        donateLabel.setTextSize(14);
        String donateLabelHtml = "<b style=\"color: #bfa14a;\">💰 Donating to this address:</b> ";
        Spanned donateLabelSpanned = Html.fromHtml(donateLabelHtml, Html.FROM_HTML_MODE_LEGACY);
        donateLabel.setText(donateLabelSpanned);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        labelParams.setMargins(0, 0, 0, 0);
        donateLabel.setLayoutParams(labelParams);
        donateLineContainer.addView(donateLabel);
        
        // Ethereum address display with copy button
        LinearLayout addressContainer = new LinearLayout(this);
        addressContainer.setOrientation(LinearLayout.HORIZONTAL);
        addressContainer.setPadding(0, 0, 0, 0);
        
        TextView addressView = new TextView(this);
        addressView.setText("0xa1...EafE");
        addressView.setTextSize(14);
        addressView.setTextColor(android.graphics.Color.parseColor("#c9b26d"));
        addressView.setTypeface(android.graphics.Typeface.MONOSPACE);
        LinearLayout.LayoutParams addressParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        addressParams.setMargins(0, 0, 12, 0);
        addressView.setLayoutParams(addressParams);
        
        // Use ImageButton for proper icon display
        ImageButton copyButton = new ImageButton(this);
        copyButton.setImageResource(R.drawable.copy_icon);
        copyButton.setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"));
        copyButton.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
        copyButton.setPadding(8, 8, 8, 8);
        
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
            40, // button width
            40  // button height
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
        
        donateLineContainer.addView(addressContainer);
        container.addView(donateLineContainer);
        
        // Second text section after address
        TextView textView3 = new TextView(this);
        textView3.setTextSize(16);
        textView3.setLineSpacing(1.5f, 1.5f);
        textView3.setPadding(0, 16, 0, 0);
        
        String htmlContent3 = "<p style=\"color: #e6c97a; margin: 8px 0; text-align: center;\">(Preferred currencies: POL, USDC, Base ETH, $Flower. But anything is accepted.)</p>" +
                "<hr style=\"border: none; height: 1px; background: #bfa14a; margin: 16px 0;\" />" +
                "<p style=\"color: #e6e6e6; margin: 12px 0;\"><b style=\"color: #bfa14a;\">🎮 Follow/Help/Cheer the Dev in-game:</b> <br/><a href=\"https://sunflower-land.com/play/#/visit/1128976301583508\" style=\"color: #c9b26d;\">iSPANK's Farm</a></p>" +
                "<p style=\"color: #e6e6e6; margin: 12px 0;\"><b style=\"color: #bfa14a;\">⭐ Leave a 5 star review on</b> <br/><a href=\"https://play.google.com/store/apps/details?id=com.sfl.browser\" style=\"color: #c9b26d;\">Google Play Store</a></p>" +
                "<hr style=\"border: none; height: 1px; background: #bfa14a; margin: 16px 0;\" />" +
                "<p style=\"color: #bfa14a; text-align: center; margin: 20px 0; font-size: 14sp;\"><b>Thank you for supporting this project!</b></p>";
        
        Spanned spannedText3 = Html.fromHtml(htmlContent3, Html.FROM_HTML_MODE_LEGACY);
        textView3.setText(spannedText3);
        textView3.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
        
        container.addView(textView3);
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
