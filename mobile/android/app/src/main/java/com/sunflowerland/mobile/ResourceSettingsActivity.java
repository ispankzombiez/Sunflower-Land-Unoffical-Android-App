package com.sunflowerland.mobile;

import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;

public class ResourceSettingsActivity extends BaseCategorySettingsActivity {
    @Override
    protected String getCategoryKey() {
        return "resource";
    }
    
    @Override
    protected String getCategoryTitle() {
        return "Resource Notifications";
    }
    
    @Override
    protected String[] getItems() {
        return new String[]{
            "Tree", "Stone", "Iron", "Gold", "Crimstone", "Oil", "Sunstone", "Obsidian"
        };
    }
    
    @Override
    protected PreferenceFragmentCompat createFragment() {
        return new ResourceFragment();
    }
    
    public static class ResourceFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.resource_preferences, rootKey);
        }
    }
}
