package com.sfl.browser;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

public abstract class BaseCategorySettingsActivity extends AppCompatActivity {
    // Utility to turn on all toggles for a category
    public static void turnOnAllToggles(android.content.Context context, String categoryKey, String[] items) {
        android.content.SharedPreferences prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context);
        android.content.SharedPreferences.Editor editor = prefs.edit();
        for (String item : items) {
            String key = categoryKey + "_" + item.toLowerCase().replace(" ", "_");
            editor.putBoolean(key, true);
        }
        editor.apply();
    }
    // Utility to turn off all toggles for a category
    public static void turnOffAllToggles(android.content.Context context, String categoryKey, String[] items) {
        // Use the default SharedPreferences (same as PreferenceManager.getDefaultSharedPreferences)
        android.content.SharedPreferences prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context);
        android.content.SharedPreferences.Editor editor = prefs.edit();
        for (String item : items) {
            String key = categoryKey + "_" + item.toLowerCase().replace(" ", "_");
            editor.putBoolean(key, false);
        }
        editor.apply();
    }
    
    protected abstract String getCategoryKey();
    protected abstract String getCategoryTitle();
    protected abstract String[] getItems();
    
    protected PreferenceFragmentCompat createFragment() {
        return new CategoryFragment();
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        android.util.Log.d("BaseCategorySettings", "onCreate called for: " + getCategoryTitle());
        
        try {
            setContentView(R.layout.activity_settings);
            
            // Set up the toolbar
            Toolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
                android.util.Log.d("BaseCategorySettings", "Toolbar found and set");
            } else {
                android.util.Log.e("BaseCategorySettings", "Toolbar is null!");
            }
            
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
                getSupportActionBar().setTitle(getCategoryTitle());
                android.util.Log.d("BaseCategorySettings", "ActionBar configured with back button");
            } else {
                android.util.Log.e("BaseCategorySettings", "ActionBar is null!");
            }
            
            if (savedInstanceState == null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.settings_container, createFragment())
                        .commit();
            }
        } catch (Exception e) {
            android.util.Log.e("BaseCategorySettings", "Error in onCreate", e);
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
    
    public class CategoryFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            android.util.Log.d("CategoryFragment", "onCreatePreferences called");
            
            // Load the base XML
            setPreferencesFromResource(R.xml.category_preferences, rootKey);
            
            BaseCategorySettingsActivity activity = (BaseCategorySettingsActivity) getActivity();
            if (activity == null) {
                android.util.Log.e("CategoryFragment", "Activity is null!");
                return;
            }
            
            String[] items = activity.getItems();
            String categoryKey = activity.getCategoryKey();
            
            android.util.Log.d("CategoryFragment", "Creating preferences for category: " + categoryKey + " with " + items.length + " items");
            
            PreferenceScreen screen = getPreferenceScreen();
            if (screen == null) {
                android.util.Log.e("CategoryFragment", "PreferenceScreen is null!");
                return;
            }
            
            for (String item : items) {
                String key = categoryKey + "_" + item.toLowerCase().replace(" ", "_");
                
                SwitchPreferenceCompat pref = new SwitchPreferenceCompat(requireContext());
                pref.setKey(key);
                pref.setTitle(item);
                pref.setDefaultValue(true);
                
                screen.addPreference(pref);
                android.util.Log.d("CategoryFragment", "Added preference: " + key + " (" + item + ")");
            }
            
            android.util.Log.d("CategoryFragment", "Finished adding all preferences. Total count: " + screen.getPreferenceCount());
        }
    }
}
