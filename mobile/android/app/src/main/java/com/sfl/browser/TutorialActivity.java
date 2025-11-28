package com.sfl.browser;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.text.Html;
import android.text.method.LinkMovementMethod;

public class TutorialActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);
        
        // Set up the toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        // Enable back button in action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Tutorial");
        }
        
        // Set tutorial content
        TextView tutorialContent = findViewById(R.id.tutorial_content);
        String tutorial = getTutorialContent();
        tutorialContent.setText(Html.fromHtml(tutorial, Html.FROM_HTML_MODE_LEGACY));
        // Make links clickable with custom handler for sunflower-land links
        tutorialContent.setMovementMethod(new SunflowerLandLinkHandler(this));
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
    
    private String getTutorialContent() {
        return "<h2 style=\"color: #c9b26d; text-align: center; margin: 20px 0;\">🌻 SFL Browser Tutorial</h2>" +
                "<p style=\"text-align: center; color: #e6c97a;\"><i>Your companion app for Sunflower Land</i></p>" +
                "<hr style=\"border: none; height: 1px; background: #bfa14a;\">" +
                
                "<h3 style=\"color: #e6c97a; border-left: 4px solid #bfa14a; padding-left: 10px; margin-top: 20px;\">🚀 Getting Started</h3>" +
                "<p>SFL Browser is an unofficial Android app designed to enhance your Sunflower Land experience with real-time notifications and quick access to game tools.</p>" +
                
                "<h3 style=\"color: #e6c97a; border-left: 4px solid #bfa14a; padding-left: 10px; margin-top: 20px;\">⚙️ App Modes</h3>" +
                "<p><b style=\"color: #bfa14a;\">Notifications Only Mode (Default):</b> When enabled, the app opens directly to the Settings page. This mode disables the web browser and keeps only the notification system active. You can toggle this mode in Settings.</p>" +
                "<p><b style=\"color: #bfa14a;\">Browser Mode:</b> When Notifications Only is disabled, the app opens to the web browser, allowing you to play Sunflower Land directly within the app.</p>" +
                "<hr style=\"border: none; height: 1px; background: #bfa14a;\">" +
                
                "<h3 style=\"color: #e6c97a; border-left: 4px solid #bfa14a; padding-left: 10px; margin-top: 20px;\">🔔 Setting Up Notifications</h3>" +
                "<p>To enable push notifications:</p>" +
                "<ol>" +
                "<li>Open the app - it will prompt you for notification permission</li>" +
                "<li>Go to Settings</li>" +
                "<li>Enter your Farm ID (found in-game: Settings → 3 dots → top of game options panel)</li>" +
                "<li>Enter your Farm API Key (found in-game: Settings → 3 dots → General → API Key)</li>" +
                "<li>Click \"Start Worker\" to begin receiving notifications</li>" +
                "</ol>" +
                
                "<h3 style=\"color: #e6c97a; border-left: 4px solid #bfa14a; padding-left: 10px; margin-top: 20px;\">📋 Notification Categories</h3>" +
                "<p>In Settings, you can customize which categories send you notifications:</p>" +
                "<ul>" +
                "<li><b style=\"color: #bfa14a;\">🌾 Crops:</b> Get notified when crops are ready to harvest</li>" +
                "<li><b style=\"color: #bfa14a;\">🐄 Animals:</b> Receive alerts for animal care needs</li>" +
                "<li><b style=\"color: #bfa14a;\">⛏️ Resources:</b> Be notified when resources are ready to collect</li>" +
                "<li><b style=\"color: #bfa14a;\">🍳 Cooking:</b> Get alerts for cooking completion</li>" +
                "<li><b style=\"color: #bfa14a;\">🌸 Flowers:</b> Receive flower harvesting notifications</li>" +
                "<li><b style=\"color: #bfa14a;\">🍎 Fruits:</b> Get alerts for fruit harvesting</li>" +
                "<li><b style=\"color: #bfa14a;\">➕ And more:</b> Many other categories available to customize</li>" +
                "</ul>" +
                "<p>Toggle each category on or off to customize your notification experience.</p>" +
                "<hr style=\"border: none; height: 1px; background: #bfa14a;\">" +
                
                "<h3 style=\"color: #e6c97a; border-left: 4px solid #bfa14a; padding-left: 10px; margin-top: 20px;\">🎮 Accessing the Game</h3>" +
                "<p>When Notifications Only is disabled:</p>" +
                "<ul>" +
                "<li>The app opens to the game browser by default</li>" +
                "<li>Three tab buttons at the bottom allow quick switching between different pages</li>" +
                "<li>Use the browser controls (back, forward, reload) to navigate</li>" +
                "<li>Enter URLs in the address bar to visit different pages</li>" +
                "<li>Customize the default URLs for each tab in Settings</li>" +
                "<li>Use the tab switcher button (grid icon) to view all open tabs at once</li>" +
                "</ul>" +
                
                "<h3 style=\"color: #e6c97a; border-left: 4px solid #bfa14a; padding-left: 10px; margin-top: 20px;\">🎨 Browser Controls</h3>" +
                "<p><b style=\"color: #bfa14a;\">Browser Controls Toggle:</b> In Settings, you can hide or show browser navigation controls for a cleaner game view.</p>" +
                "<p>When hidden, only the game content is visible, but you can still access Settings by triple-tapping with 3 fingers.</p>" +
                
                "<h3 style=\"color: #e6c97a; border-left: 4px solid #bfa14a; padding-left: 10px; margin-top: 20px;\">📑 Tab Switcher & Overview</h3>" +
                "<p><b style=\"color: #bfa14a;\">Tab Switcher Button:</b> Click the grid icon button at the bottom right of the screen to open the tab overview. This displays all three open tabs as live previews in a 2x2 grid layout, allowing you to quickly select which tab to view.</p>" +
                "<p><b style=\"color: #bfa14a;\">3-Finger Swipe Gesture:</b> Swipe left or right with 3 fingers anywhere on the screen to quickly open the tab overview without using the button. This is a quick way to switch between tabs without touching the interface.</p>" +
                "<p><b style=\"color: #bfa14a;\">Tab Persistence:</b> All tabs remain active and retain their page state and scroll position, so you can seamlessly switch between them.</p>" +
                
                "<h3 style=\"color: #e6c97a; border-left: 4px solid #bfa14a; padding-left: 10px; margin-top: 20px;\">📱 Screen Orientation</h3>" +
                "<p>In Settings, choose between Portrait and Landscape orientation for your preferred gameplay experience.</p>" +
                
                "<h3 style=\"color: #e6c97a; border-left: 4px solid #bfa14a; padding-left: 10px; margin-top: 20px;\">📲 App to Open</h3>" +
                "<p>When Notifications Only mode is enabled, you can set which app notifications will open when clicked. By default, it's set to open SFL Browser.</p>" +
                "<hr style=\"border: none; height: 1px; background: #bfa14a;\">" +
                
                "<h3 style=\"color: #e6c97a; border-left: 4px solid #bfa14a; padding-left: 10px; margin-top: 20px;\">⚡ Starting and Stopping the Worker</h3>" +
                "<p>The notification worker runs in the background to check for updates. You can manually:</p>" +
                "<ul>" +
                "<li><b style=\"color: #bfa14a;\">Start Worker:</b> Manually enable the background notification service</li>" +
                "<li><b style=\"color: #bfa14a;\">Stop Worker:</b> Manually disable the background notification service</li>" +
                "<li><b style=\"color: #bfa14a;\">Auto Start Worker:</b> Enable automatic starting when the app launches (toggleable in Settings)</li>" +
                "</ul>" +
                "<hr style=\"border: none; height: 1px; background: #bfa14a;\">" +
                
                "<h3 style=\"color: #e6c97a; border-left: 4px solid #bfa14a; padding-left: 10px; margin-top: 20px;\">ℹ️ About This App</h3>" +
                "<p>SFL Browser is an unofficial third-party app created by community members. It is not affiliated with, endorsed by, or connected to Sunflower Land or its development team.</p>" +
                "<p>The source code is fully open-source and available on GitHub for transparency and community contributions.</p>" +
                
                "<h3 style=\"color: #e6c97a; border-left: 4px solid #bfa14a; padding-left: 10px; margin-top: 20px;\">💡 Tips & Tricks</h3>" +
                "<ul>" +
                "<li>Enable \"Auto Start Worker\" to automatically begin checking for notifications when you open the app</li>" +
                "<li>Use the three tabs in browser mode to quickly switch between your Farm, the SFL Wiki, and other resources</li>" +
                "<li>Use the tab switcher button (grid icon) to view and select between all open tabs, or swipe left or right with 3 fingers anywhere on the screen to quickly open the tab overview</li>" +
                "<li>Customize notification categories based on your playstyle - don't get overwhelmed by too many alerts</li>" +
                "<li>Triple-tap with 3 fingers to access Settings even when browser controls are hidden</li>" +
                "<li>Keep your API Key secure - it is sensitive account information. This app only uses it on your behalf to fetch your game data and does not share it with third parties.</li>" +
                "</ul>" +
                "<hr style=\"border: none; height: 1px; background: #bfa14a;\">" +
                
                "<h3 style=\"color: #e6c97a; border-left: 4px solid #bfa14a; padding-left: 10px; margin-top: 20px;\">❓ Need Help?</h3>" +
                "<p>For support, questions, or to report issues, join the official Sunflower Land Discord:</p>" +
                "<p><a href=\"https://discord.com/channels/880987707214544966/1314031342182338651\"><b style=\"color: #c9b26d;\">→ SFL Browser Support Channel</b></a></p>" +
                "<p>You can also find the project source code on GitHub for additional information and to contribute.</p>" +
                "<p style=\"text-align: center; margin-top: 30px; color: #bfa14a;\"><b>Thank you for using SFL Browser!</b></p>";
    }
}
