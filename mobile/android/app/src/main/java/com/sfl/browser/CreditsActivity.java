package com.sfl.browser;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;

public class CreditsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credits);
        
        // Set up the toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // Enable back button in action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Credits");
        }
        
        LinearLayout container = findViewById(R.id.credits_container);
        container.setPadding(0, 0, 0, 0);
        
        // Title
        TextView titleView = new TextView(this);
        titleView.setTextSize(18);
        titleView.setPadding(0, 0, 0, 16);
        String titleHtml = "<h2 style=\"color: #c9b26d; text-align: center; margin: 0 0 16px 0;\">👥 Contributors</h2>";
        Spanned titleSpanned = Html.fromHtml(titleHtml, Html.FROM_HTML_MODE_LEGACY);
        titleView.setText(titleSpanned);
        container.addView(titleView);
        
        // Divider
        TextView divider1 = new TextView(this);
        divider1.setHeight(2);
        divider1.setBackgroundColor(android.graphics.Color.parseColor("#bfa14a"));
        divider1.setPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            2
        );
        dividerParams.setMargins(0, 0, 0, 16);
        divider1.setLayoutParams(dividerParams);
        container.addView(divider1);
        
        // Andrei contributor
        TextView contributorAndrei = new TextView(this);
        contributorAndrei.setTextSize(16);
        contributorAndrei.setLineSpacing(1.5f, 1.5f);
        contributorAndrei.setPadding(0, 0, 0, 16);
        String contributorAndreiHtml = "<p style=\"color: #e6e6e6; margin: 0 0 8px 0;\"><b style=\"color: #bfa14a;\">🌐 Andrei</b></p>" +
                "<p style=\"color: #e6c97a; margin: 0 0 8px 0;\">Creator and operator of SFL World and SFL Wiki</p>" +
                "<p style=\"color: #e6e6e6; margin: 0;\">Farm: <a href=\"https://sunflower-land.com/play/#/visit/114779\" style=\"color: #c9b26d;\">https://sunflower-land.com/play/#/visit/114779</a></p>" +
                "<p style=\"color: #e6e6e6; margin: 4px 0 0 0;\">SFL World: <a href=\"https://sfl.world/\" style=\"color: #c9b26d;\">https://sfl.world/</a></p>" +
                "<p style=\"color: #e6e6e6; margin: 4px 0 0 0;\">SFL Wiki: <a href=\"https://wiki.sfl.world/\" style=\"color: #c9b26d;\">https://wiki.sfl.world/</a></p>";
        Spanned contributorAndreiSpanned = Html.fromHtml(contributorAndreiHtml, Html.FROM_HTML_MODE_LEGACY);
        contributorAndrei.setText(contributorAndreiSpanned);
        contributorAndrei.setMovementMethod(new SunflowerLandLinkHandler(this));
        container.addView(contributorAndrei);
        
        // Divider
        TextView dividerAndrei = new TextView(this);
        dividerAndrei.setHeight(2);
        dividerAndrei.setBackgroundColor(android.graphics.Color.parseColor("#bfa14a"));
        dividerAndrei.setPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams dividerParamsAndrei = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            2
        );
        dividerParamsAndrei.setMargins(0, 0, 0, 16);
        dividerAndrei.setLayoutParams(dividerParamsAndrei);
        container.addView(dividerAndrei);
        
        // Kohirabbit contributor
        TextView contributor1 = new TextView(this);
        contributor1.setTextSize(16);
        contributor1.setLineSpacing(1.5f, 1.5f);
        contributor1.setPadding(0, 0, 0, 16);
        String contributor1Html = "<p style=\"color: #e6e6e6; margin: 0 0 8px 0;\"><b style=\"color: #bfa14a;\">🎨 kohirabbit</b></p>" +
                "<p style=\"color: #e6c97a; margin: 0 0 8px 0;\">App Icon Design</p>" +
                "<p style=\"color: #e6e6e6; margin: 0;\">Farm: <a href=\"https://sunflower-land.com/play/#/visit/61992\" style=\"color: #c9b26d;\">https://sunflower-land.com/play/#/visit/61992</a></p>" +
                "<p style=\"color: #e6e6e6; margin: 4px 0 0 0;\">Portfolio: <a href=\"https://kohirabbit.carrd.co/\" style=\"color: #c9b26d;\">https://kohirabbit.carrd.co/</a></p>";
        Spanned contributor1Spanned = Html.fromHtml(contributor1Html, Html.FROM_HTML_MODE_LEGACY);
        contributor1.setText(contributor1Spanned);
        contributor1.setMovementMethod(new SunflowerLandLinkHandler(this));
        container.addView(contributor1);
        
        // Divider
        TextView divider2 = new TextView(this);
        divider2.setHeight(2);
        divider2.setBackgroundColor(android.graphics.Color.parseColor("#bfa14a"));
        divider2.setPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams dividerParams2 = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            2
        );
        dividerParams2.setMargins(0, 0, 0, 16);
        divider2.setLayoutParams(dividerParams2);
        container.addView(divider2);
        
        // Maschs contributor
        TextView contributor2 = new TextView(this);
        contributor2.setTextSize(16);
        contributor2.setLineSpacing(1.5f, 1.5f);
        contributor2.setPadding(0, 0, 0, 16);
        String contributor2Html = "<p style=\"color: #e6e6e6; margin: 0 0 8px 0;\"><b style=\"color: #bfa14a;\">💻 Maschs</b></p>" +
                "<p style=\"color: #e6c97a; margin: 0 0 8px 0;\">Code and API Tutoring</p>" +
                "<p style=\"color: #e6e6e6; margin: 0;\">Farm: <a href=\"https://sunflower-land.com/play/#/visit/137396\" style=\"color: #c9b26d;\">https://sunflower-land.com/play/#/visit/137396</a></p>";
        Spanned contributor2Spanned = Html.fromHtml(contributor2Html, Html.FROM_HTML_MODE_LEGACY);
        contributor2.setText(contributor2Spanned);
        contributor2.setMovementMethod(new SunflowerLandLinkHandler(this));
        container.addView(contributor2);
        
        // Final divider
        TextView divider3 = new TextView(this);
        divider3.setHeight(2);
        divider3.setBackgroundColor(android.graphics.Color.parseColor("#bfa14a"));
        divider3.setPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams dividerParams3 = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            2
        );
        dividerParams3.setMargins(0, 16, 0, 16);
        divider3.setLayoutParams(dividerParams3);
        container.addView(divider3);
        
        // Thank you message
        TextView thankYouView = new TextView(this);
        thankYouView.setTextSize(14);
        thankYouView.setLineSpacing(1.5f, 1.5f);
        thankYouView.setPadding(0, 0, 0, 0);
        String thankYouHtml = "<p style=\"color: #bfa14a; text-align: center; margin: 0;\"><b>Thank you to all our contributors!</b></p>";
        Spanned thankYouSpanned = Html.fromHtml(thankYouHtml, Html.FROM_HTML_MODE_LEGACY);
        thankYouView.setText(thankYouSpanned);
        container.addView(thankYouView);
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
