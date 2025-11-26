package com.sfl.browser;

import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;

public class SkillSettingsActivity extends BaseCategorySettingsActivity {
    @Override
    protected String getCategoryKey() {
        return "skill";
    }
    
    @Override
    protected String getCategoryTitle() {
        return "Skill Cooldown Notifications";
    }
    
    @Override
    protected String[] getItems() {
        return new String[]{
            "Instant Growth", "Tree Blitz", "Instant Gratification", "Barnyard Rouse", 
            "Petal Blessed", "Greenhouse Guru", "Grease Lightning"
        };
    }
    
    @Override
    protected PreferenceFragmentCompat createFragment() {
        return new SkillFragment();
    }
    
    public static class SkillFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.skill_preferences, rootKey);
        }
    }
}
