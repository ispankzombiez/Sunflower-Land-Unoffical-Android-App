package com.sunflowerland.mobile;

import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;

public class AnimalSettingsActivity extends BaseCategorySettingsActivity {
    @Override
    protected String getCategoryKey() {
        return "animal";
    }
    
    @Override
    protected String getCategoryTitle() {
        return "Animal Notifications";
    }
    
    @Override
    protected String[] getItems() {
        return new String[]{
            "Chicken", "Sheep", "Cow"
        };
    }
    
    @Override
    protected PreferenceFragmentCompat createFragment() {
        return new AnimalFragment();
    }
    
    public static class AnimalFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.animal_preferences, rootKey);
        }
    }
}
