package com.sfl.browser;

import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;

public class ComposterSettingsActivity extends BaseCategorySettingsActivity {
    @Override
    protected String getCategoryKey() {
        return "composter";
    }
    
    @Override
    protected String getCategoryTitle() {
        return "Composter Notifications";
    }
    
    @Override
    protected String[] getItems() {
        return new String[]{
            "Compost Bin", "Turbo Composter", "Premium Composter"
        };
    }
    
    @Override
    protected PreferenceFragmentCompat createFragment() {
        return new ComposterFragment();
    }
    
    public static class ComposterFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.composter_preferences, rootKey);
        }
    }
}
