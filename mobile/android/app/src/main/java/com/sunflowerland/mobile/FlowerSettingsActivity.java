package com.sunflowerland.mobile;

import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;

public class FlowerSettingsActivity extends BaseCategorySettingsActivity {
    @Override
    protected String getCategoryKey() {
        return "flower";
    }
    
    @Override
    protected String getCategoryTitle() {
        return "Flower Notifications";
    }
    
    @Override
    protected String[] getItems() {
        return new String[]{
            "Red Pansy", "Yellow Pansy", "Purple Pansy", "White Pansy", "Blue Pansy",
            "Red Cosmos", "Yellow Cosmos", "Purple Cosmos", "White Cosmos", "Blue Cosmos", "Prism Petal",
            "Red Balloon Flower", "Yellow Balloon Flower", "Purple Balloon Flower", "White Balloon Flower", "Blue Balloon Flower",
            "Red Daffodil", "Yellow Daffodil", "Purple Daffodil", "White Daffodil", "Blue Daffodil", "Celestial Frostbloom",
            "Red Edelweiss", "Yellow Edelweiss", "Purple Edelweiss", "White Edelweiss", "Blue Edelweiss",
            "Red Gladiolus", "Yellow Gladiolus", "Purple Gladiolus", "White Gladiolus", "Blue Gladiolus",
            "Red Lavender", "Yellow Lavender", "Purple Lavender", "White Lavender", "Blue Lavender",
            "Red Carnation", "Yellow Carnation", "Purple Carnation", "White Carnation", "Blue Carnation",
            "Red Clover", "Yellow Clover", "Purple Clover", "White Clover", "Blue Clover",
            "Red Lotus", "Yellow Lotus", "Purple Lotus", "White Lotus", "Blue Lotus", "Primula Enigma"
        };
    }
    
    @Override
    protected PreferenceFragmentCompat createFragment() {
        return new FlowerFragment();
    }
    
    public static class FlowerFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.flower_preferences, rootKey);
        }
    }
}
