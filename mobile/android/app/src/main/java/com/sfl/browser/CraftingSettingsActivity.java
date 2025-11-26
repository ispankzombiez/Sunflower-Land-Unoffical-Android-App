package com.sfl.browser;

import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;

public class CraftingSettingsActivity extends BaseCategorySettingsActivity {
    @Override
    protected String getCategoryKey() {
        return "crafting";
    }
    
    @Override
    protected String getCategoryTitle() {
        return "Crafting Box Notifications";
    }
    
    @Override
    protected String[] getItems() {
        return new String[]{
            "Doll", "Angler Doll", "Bigfin Doll", "Bloom Doll", "Buzz Doll", "Cluck Doll", "Cosmo Doll", "Crude Doll", "Dune Doll", "Ember Doll", "Frosty Doll", "Gilded Doll", "Grubby Doll", "Harvest Doll", "Juicy Doll", "Lumber Doll", "Lunar Doll", "Moo Doll", "Mouse Doll", "Nefari Doll", "Shadow Doll", "Sizzle Doll", "Solar Doll", "Wolly Doll",
            "Fisher Bed", "Basic Bed", "Floral Bed", "Desert Bed", "Sturdy Bed", "Cow Bed", "Pirate Bed", "Royal Bed"
        };
    }
    
    @Override
    protected PreferenceFragmentCompat createFragment() {
        return new CraftingFragment();
    }
    
    public static class CraftingFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.crafting_preferences, rootKey);
        }
    }
}
