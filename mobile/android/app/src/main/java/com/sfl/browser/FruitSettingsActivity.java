package com.sfl.browser;

import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;

public class FruitSettingsActivity extends BaseCategorySettingsActivity {
    @Override
    protected String getCategoryKey() {
        return "fruit";
    }
    
    @Override
    protected String getCategoryTitle() {
        return "Fruit Notifications";
    }
    
    @Override
    protected String[] getItems() {
        return new String[]{
            "Tomato", "Lemon", "Blueberry", "Orange", "Apple", "Banana", "Celestine", "Lunara", "Duskberry"
        };
    }
    
    @Override
    protected PreferenceFragmentCompat createFragment() {
        return new FruitFragment();
    }
    
    public static class FruitFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.fruit_preferences, rootKey);
        }
    }
}
