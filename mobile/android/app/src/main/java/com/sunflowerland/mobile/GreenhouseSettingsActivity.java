package com.sunflowerland.mobile;

import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;

public class GreenhouseSettingsActivity extends BaseCategorySettingsActivity {
    @Override
    protected String getCategoryKey() {
        return "greenhouse_crop";
    }
    
    @Override
    protected String getCategoryTitle() {
        return "Greenhouse Crop Notifications";
    }
    
    @Override
    protected String[] getItems() {
        return new String[]{
            "Olive", "Grape", "Rice"
        };
    }
    
    @Override
    protected PreferenceFragmentCompat createFragment() {
        return new GreenhouseFragment();
    }
    
    public static class GreenhouseFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.greenhouse_preferences, rootKey);
        }
    }
}
