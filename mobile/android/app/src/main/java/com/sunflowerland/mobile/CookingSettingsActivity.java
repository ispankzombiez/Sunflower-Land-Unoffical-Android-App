package com.sunflowerland.mobile;

import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;

public class CookingSettingsActivity extends BaseCategorySettingsActivity {
    @Override
    protected String getCategoryKey() {
        return "cooking";
    }
    
    @Override
    protected String getCategoryTitle() {
        return "Cooking Notifications";
    }
    
    @Override
    protected String[] getItems() {
        return new String[]{
            // Fire Pit
            "Antipasto", "Boiled Eggs", "Bumpkin Broth", "Cabbers n Mash",
            "Fried Tofu", "Gumbo", "Kale Omelette", "Kale Stew",
            "Mashed Potato", "Mushroom Soup", "Pizza Margherita", "Popcorn",
            "Pumpkin Soup", "Rapid Roast", "Reindeer Carrot", "Rice Bun",
            // Kitchen
            "Caprese Salad", "Cauliflower Burger", "Chowder", "Club Sandwich",
            "Fish Burger", "Fish n Chips", "Fish Omelette", "Fried Calamari",
            "Fruit Salad", "Goblin Brunch", "Goblin's Treat", "Mushroom Jacket Potatoes",
            "Ocean's Olive", "Pancakes", "Roast Veggies", "Seafood Basket",
            "Spaghetti al Limone", "Steamed Red Rice", "Sushi Roll", "Sunflower Crunch",
            "Tofu Scramble", "Bumpkin Ganoush", "Bumpkin Roast", "Bumpkin Salad",
            // Bakery
            "Apple Pie", "Beetroot Cake", "Cabbage Cake", "Carrot Cake",
            "Cauliflower Cake", "Cornbread", "Eggplant Cake", "Honey Cake",
            "Kale & Mushroom Pie", "Lemon Cheesecake", "Orange Cake", "Parsnip Cake",
            "Potato Cake", "Pumpkin Cake", "Radish Cake", "Sunflower Cake", "Wheat Cake",
            // Deli
            "Blueberry Jam", "Blue Cheese", "Cheese", "Fancy Fries",
            "Fermented Carrots", "Fermented Fish", "Honey Cheddar", "Sauerkraut", "Shroom Syrup",
            // Smoothie Shack
            "Apple Juice", "Banana Blast", "Bumpkin Detox", "Carrot Juice",
            "Grape Juice", "Orange Juice", "Power Smoothie", "Purple Smoothie",
            "Quick Juice", "Slow Juice", "Sour Shake", "The Lot"
        };
    }
    
    @Override
    protected PreferenceFragmentCompat createFragment() {
        return new CookingFragment();
    }
    
    public static class CookingFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.cooking_preferences, rootKey);
        }
    }
}
