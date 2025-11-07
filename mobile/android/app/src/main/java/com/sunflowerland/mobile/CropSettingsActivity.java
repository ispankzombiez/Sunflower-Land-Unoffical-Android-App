package com.sunflowerland.mobile;

import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;

public class CropSettingsActivity extends BaseCategorySettingsActivity {
    @Override
    protected String getCategoryKey() {
        return "crop";
    }
    
    @Override
    protected String getCategoryTitle() {
        return "Crop Notifications";
    }
    
    @Override
    protected String[] getItems() {
        return new String[]{
            "Sunflower", "Potato", "Rhubarb", "Pumpkin", "Zucchini",
            "Carrot", "Yam", "Cabbage", "Broccoli", "Soybean", "Beetroot", "Pepper",
            "Cauliflower", "Parsnip", "Eggplant", "Corn", "Onion",
            "Radish", "Wheat", "Turnip", "Kale", "Artichoke", "Barley",
            "Rice", "Olive", "Grape"
        };
    }
    
    @Override
    protected PreferenceFragmentCompat createFragment() {
        return new CropFragment();
    }
    
    public static class CropFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.crop_preferences, rootKey);
        }
    }
}
