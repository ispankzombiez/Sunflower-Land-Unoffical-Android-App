package com.sunflowerland.mobile;

import android.content.Intent;
import android.os.Build;
import androidx.core.content.ContextCompat;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

public class SettingsFragment extends PreferenceFragmentCompat {
    
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load preferences first
        setPreferencesFromResource(R.xml.preferences, rootKey);

        // Debug/Dev: View Notification Log
        Preference viewLogPref = findPreference("view_notification_log");
        if (viewLogPref != null) {
            viewLogPref.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(getActivity(), NotificationLogActivity.class);
                startActivity(intent);
                return true;
            });
        }

        // Debug/Dev: View Raw JSON
        Preference viewRawJsonPref = findPreference("view_raw_json");
        if (viewRawJsonPref != null) {
            viewRawJsonPref.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(getActivity(), RawJsonActivity.class);
                startActivity(intent);
                return true;
            });
        }

        // Debug/Dev: View Processed JSON
        Preference viewProcessedJsonPref = findPreference("view_processed_json");
        if (viewProcessedJsonPref != null) {
            viewProcessedJsonPref.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(getActivity(), ProcessedJsonActivity.class);
                startActivity(intent);
                return true;
            });
        }

        // Debug/Dev: View General Debug Log (Processing Log)
        Preference viewDebugLogPref = findPreference("view_processing_log");
        if (viewDebugLogPref != null) {
            viewDebugLogPref.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(getActivity(), DebugLogActivity.class);
                startActivity(intent);
                return true;
            });
        }

        // Start Notification Worker
        Preference startManagerPref = findPreference("Start_Notification_Manager");
        if (startManagerPref != null) {
            startManagerPref.setOnPreferenceClickListener(preference -> {
                SharedPreferences prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext());
                String farmId = prefs.getString("farm_id", "");
                String apiKey = prefs.getString("api_key", "");
                if (farmId == null || farmId.isEmpty()) {
                    android.widget.Toast.makeText(requireContext(), "assign farm id", android.widget.Toast.LENGTH_LONG).show();
                    DebugLog.warning("Start Worker button pressed but farm_id is empty");
                    return true;
                }
                if (apiKey == null || apiKey.isEmpty()) {
                    android.widget.Toast.makeText(requireContext(), "assign farm api key", android.widget.Toast.LENGTH_LONG).show();
                    DebugLog.warning("Start Worker button pressed but api_key is empty");
                    return true;
                }
                
                // Use WorkManagerHelper to schedule the notification worker with "manual" source (immediate + periodic)
                DebugLog.log("User clicked 'Start Notification Worker' button");
                if (WorkManagerHelper.scheduleNotificationWorkerWithSource(requireContext(), "manual")) {
                    android.widget.Toast.makeText(requireContext(), "Notification Worker Started (immediate + periodic)", android.widget.Toast.LENGTH_SHORT).show();
                    DebugLog.log("Notification Worker scheduled successfully (manual source)");
                } else {
                    android.widget.Toast.makeText(requireContext(), "Failed to start Notification Worker", android.widget.Toast.LENGTH_SHORT).show();
                    DebugLog.error("Failed to schedule Notification Worker", null);
                }
                return true;
            });
        }

        // Stop Notification Worker
        Preference stopManagerPref = findPreference("Stop_Notification_Manager");
        if (stopManagerPref != null) {
            stopManagerPref.setOnPreferenceClickListener(preference -> {
                // Use WorkManagerHelper to cancel all notification workers
                DebugLog.log("User clicked 'Stop Notification Worker' button");
                if (WorkManagerHelper.cancelNotificationWorker(requireContext())) {
                    new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Notification Worker Stopped")
                        .setMessage("Background notifications are now disabled.")
                        .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                        .show();
                    DebugLog.log("Notification Worker stopped successfully");
                } else {
                    android.widget.Toast.makeText(requireContext(), "Failed to stop Notification Worker", android.widget.Toast.LENGTH_SHORT).show();
                    DebugLog.error("Failed to stop Notification Worker", null);
                }
                return true;
            });
        }

        // Set up master button to turn ON all toggles in category screens with confirmation
        Preference masterButtonOn = findPreference("notifications_master_on");
        if (masterButtonOn != null) {
            masterButtonOn.setOnPreferenceClickListener(preference -> {
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setMessage("Are you sure you want to turn all notifications to ON?")
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .setPositiveButton("Confirm", (dialog, which) -> {
                        // Turn on the Daily Reset toggle
                        SharedPreferences prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext());
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean("category_daily_reset", true);
                        editor.putBoolean("category_crop_machine", true);
                        editor.putBoolean("category_crafting", true);
                        editor.putBoolean("marketplace_listings_enabled", true);
                        editor.putBoolean("floating_island_enabled", true);
                        editor.putBoolean("category_auction", true);
                        editor.putBoolean("category_pet_sleep", true);
                        editor.putBoolean("category_animal_sick", true);
                        editor.apply();
                        
                        com.sunflowerland.mobile.BaseCategorySettingsActivity.turnOnAllToggles(requireContext(), "crop", new String[]{
                            "Sunflower", "Potato", "Rhubarb", "Pumpkin", "Zucchini", "Carrot", "Yam", "Cabbage", "Broccoli", "Soybean", "Beetroot", "Pepper", "Cauliflower", "Parsnip", "Eggplant", "Corn", "Onion", "Radish", "Wheat", "Turnip", "Kale", "Artichoke", "Barley"
                        });
                        com.sunflowerland.mobile.BaseCategorySettingsActivity.turnOnAllToggles(requireContext(), "fruit", new String[]{
                            "Apple", "Orange", "Blueberry", "Banana", "Tomato", "Lemon", "Celestine", "Lunara", "Duskberry"
                        });
                        com.sunflowerland.mobile.BaseCategorySettingsActivity.turnOnAllToggles(requireContext(), "greenhouse_crop", new String[]{
                            "Rice", "Olive", "Grape"
                        });
                        com.sunflowerland.mobile.BaseCategorySettingsActivity.turnOnAllToggles(requireContext(), "resource", new String[]{
                            "Tree", "Stone", "Iron", "Gold", "Crimstone", "Oil", "Sunstone", "Obsidian"
                        });
                        com.sunflowerland.mobile.BaseCategorySettingsActivity.turnOnAllToggles(requireContext(), "cooking", new String[]{
                            "Pumpkin Soup", "Roasted Cauliflower", "Sauerkraut", "Radish Pie", "Boiled Eggs", "Bumpkin Broth", "Mashed Potato", "Goblin's Brunch", "Cauliflower Burger", "Club Sandwich", "Bumpkin Roast", "Goblin's Treat", "Fruit Salad", "Kale Stew", "Reindeer Carrot", "Pancakes", "Roast Veggies", "Bumpkin ganoush", "Chowder", "Kale & mushroom omelette", "Cabbers n mash", "Fancy fries", "Fermented fish", "Apple Pie", "Blueberry Jam", "Fermented Carrots", "Sauerkraut (batch)", "Fancy fries (batch)", "Orange Juice", "Apple Juice", "Purple Smoothie", "Bumpkin Detox", "Power Smoothie", "Bumpkin Fitness Blend", "Rapid Root Juice", "Belly Buster", "Carrot Juice", "Seafood Basket", "Fish Burger", "Fish n Chips", "Fish Omelette", "Fried Calamari", "Fried Tofu", "Gumbo", "Ocean's Delight", "Sushi Roll", "The Lot", "Tofu Scramble", "Honey Cake", "Sunflower Cake", "Potato Cake", "Pumpkin Cake", "Carrot Cake", "Cabbage Cake", "Beetroot Cake", "Cauliflower Cake", "Parsnip Cake", "Radish Cake", "Wheat Cake", "Eggplant Cake", "Corn Cake", "Bumpkin Salad"
                        });
                        com.sunflowerland.mobile.BaseCategorySettingsActivity.turnOnAllToggles(requireContext(), "animal", new String[]{
                            "Chicken", "Sheep", "Cow"
                        });
                        com.sunflowerland.mobile.BaseCategorySettingsActivity.turnOnAllToggles(requireContext(), "flower", new String[]{
                            "Sunpetal", "Bloom", "Lily", "Red Pansy", "Yellow Pansy", "Purple Pansy", "White Pansy", "Blue Pansy", "Red Cosmos", "Yellow Cosmos", "Purple Cosmos", "White Cosmos", "Blue Cosmos", "Prism Petal", "Red Balloon Flower", "Yellow Balloon Flower", "Purple Balloon Flower", "White Balloon Flower", "Blue Balloon Flower", "Red Carnation", "Yellow Carnation", "Purple Carnation", "White Carnation", "Blue Carnation", "Red Clover", "Yellow Clover", "Purple Clover", "White Clover", "Blue Clover", "Red Daffodil", "Yellow Daffodil", "Purple Daffodil", "White Daffodil", "Blue Daffodil", "Celestial Frostbloom", "Red Edelweiss", "Yellow Edelweiss", "Purple Edelweiss", "White Edelweiss", "Blue Edelweiss", "Red Gladiolus", "Yellow Gladiolus", "Purple Gladiolus", "White Gladiolus", "Blue Gladiolus", "Red Lavender", "Yellow Lavender", "Purple Lavender", "White Lavender", "Blue Lavender", "Red Lotus", "Yellow Lotus", "Purple Lotus", "White Lotus", "Blue Lotus", "Primula Enigma"
                        });
                        com.sunflowerland.mobile.BaseCategorySettingsActivity.turnOnAllToggles(requireContext(), "composter", new String[]{
                            "Composter", "Turbo Composter", "Premium Composter"
                        });
                        // Crafting now uses a simple boolean toggle (category_crafting) with no per-item toggles
                        requireActivity().recreate();
                    })
                    .show();
                return true;
            });
        }

        // Set up category click listeners to open detail screens
        Preference cropsPref = findPreference("category_crops");
        if (cropsPref != null) {
            cropsPref.setOnPreferenceClickListener(preference -> {
                try {
                    Intent intent = new Intent(getActivity(), CropSettingsActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    android.util.Log.e("SettingsFragment", "Failed to start CropSettingsActivity", e);
                }
                return true;
            });
        }

        Preference fruitsPref = findPreference("category_fruits");
        if (fruitsPref != null) {
            fruitsPref.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(getActivity(), FruitSettingsActivity.class);
                startActivity(intent);
                return true;
            });
        }

        Preference greenhousePref = findPreference("category_greenhouse_crops");
        if (greenhousePref != null) {
            greenhousePref.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(getActivity(), GreenhouseSettingsActivity.class);
                startActivity(intent);
                return true;
            });
        }

        Preference resourcesPref = findPreference("category_resources");
        if (resourcesPref != null) {
            resourcesPref.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(getActivity(), ResourceSettingsActivity.class);
                startActivity(intent);
                return true;
            });
        }

        Preference flowersPref = findPreference("category_flowers");
        if (flowersPref != null) {
            flowersPref.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(getActivity(), FlowerSettingsActivity.class);
                startActivity(intent);
                return true;
            });
        }

        Preference cookingPref = findPreference("category_cooking");
        if (cookingPref != null) {
            cookingPref.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(getActivity(), CookingSettingsActivity.class);
                startActivity(intent);
                return true;
            });
        }

        Preference compostersPref = findPreference("category_composters");
        if (compostersPref != null) {
            compostersPref.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(getActivity(), ComposterSettingsActivity.class);
                startActivity(intent);
                return true;
            });
        }

        Preference animalsPref = findPreference("category_animals");
        if (animalsPref != null) {
            animalsPref.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(getActivity(), AnimalSettingsActivity.class);
                startActivity(intent);
                return true;
            });
        }

        // Crafting category is now a simple boolean toggle (category_crafting), no per-item toggles needed
        
        // Set up master button to turn off all toggles in category screens with confirmation
        Preference masterButton = findPreference("notifications_master");
        if (masterButton != null) {
            masterButton.setOnPreferenceClickListener(preference -> {
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setMessage("Are you sure you want to turn all notifications to off?")
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .setPositiveButton("Confirm", (dialog, which) -> {
                        // Turn off the Daily Reset toggle
                        SharedPreferences prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext());
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean("category_daily_reset", false);
                        editor.putBoolean("category_crop_machine", false);
                        editor.putBoolean("category_crafting", false);
                        editor.putBoolean("marketplace_listings_enabled", false);
                        editor.putBoolean("floating_island_enabled", false);
                        editor.putBoolean("category_auction", false);
                        editor.putBoolean("category_pet_sleep", false);
                        editor.putBoolean("category_animal_sick", false);
                        editor.apply();
                        
                        com.sunflowerland.mobile.BaseCategorySettingsActivity.turnOffAllToggles(requireContext(), "crop", new String[]{
                            "Sunflower", "Potato", "Rhubarb", "Pumpkin", "Zucchini", "Carrot", "Yam", "Cabbage", "Broccoli", "Soybean", "Beetroot", "Pepper", "Cauliflower", "Parsnip", "Eggplant", "Corn", "Onion", "Radish", "Wheat", "Turnip", "Kale", "Artichoke", "Barley"
                        });
                        com.sunflowerland.mobile.BaseCategorySettingsActivity.turnOffAllToggles(requireContext(), "fruit", new String[]{
                            "Apple", "Orange", "Blueberry", "Banana", "Tomato", "Lemon", "Celestine", "Lunara", "Duskberry"
                        });
                        com.sunflowerland.mobile.BaseCategorySettingsActivity.turnOffAllToggles(requireContext(), "greenhouse_crop", new String[]{
                            "Rice", "Olive", "Grape"
                        });
                        com.sunflowerland.mobile.BaseCategorySettingsActivity.turnOffAllToggles(requireContext(), "resource", new String[]{
                            "Tree", "Stone", "Iron", "Gold", "Crimstone", "Oil", "Sunstone", "Obsidian"
                        });
                        com.sunflowerland.mobile.BaseCategorySettingsActivity.turnOffAllToggles(requireContext(), "cooking", new String[]{
                            "Pumpkin Soup", "Roasted Cauliflower", "Sauerkraut", "Radish Pie", "Boiled Eggs", "Bumpkin Broth", "Mashed Potato", "Goblin's Brunch", "Cauliflower Burger", "Club Sandwich", "Bumpkin Roast", "Goblin's Treat", "Fruit Salad", "Kale Stew", "Reindeer Carrot", "Pancakes", "Roast Veggies", "Bumpkin ganoush", "Chowder", "Kale & mushroom omelette", "Cabbers n mash", "Fancy fries", "Fermented fish", "Apple Pie", "Blueberry Jam", "Fermented Carrots", "Sauerkraut (batch)", "Fancy fries (batch)", "Orange Juice", "Apple Juice", "Purple Smoothie", "Bumpkin Detox", "Power Smoothie", "Bumpkin Fitness Blend", "Rapid Root Juice", "Belly Buster", "Carrot Juice", "Seafood Basket", "Fish Burger", "Fish n Chips", "Fish Omelette", "Fried Calamari", "Fried Tofu", "Gumbo", "Ocean's Delight", "Sushi Roll", "The Lot", "Tofu Scramble", "Honey Cake", "Sunflower Cake", "Potato Cake", "Pumpkin Cake", "Carrot Cake", "Cabbage Cake", "Beetroot Cake", "Cauliflower Cake", "Parsnip Cake", "Radish Cake", "Wheat Cake", "Eggplant Cake", "Corn Cake", "Bumpkin Salad"
                        });
                        com.sunflowerland.mobile.BaseCategorySettingsActivity.turnOffAllToggles(requireContext(), "animal", new String[]{
                            "Chicken", "Sheep", "Cow"
                        });
                        com.sunflowerland.mobile.BaseCategorySettingsActivity.turnOffAllToggles(requireContext(), "flower", new String[]{
                            "Sunpetal", "Bloom", "Lily", "Red Pansy", "Yellow Pansy", "Purple Pansy", "White Pansy", "Blue Pansy", "Red Cosmos", "Yellow Cosmos", "Purple Cosmos", "White Cosmos", "Blue Cosmos", "Prism Petal", "Red Balloon Flower", "Yellow Balloon Flower", "Purple Balloon Flower", "White Balloon Flower", "Blue Balloon Flower", "Red Carnation", "Yellow Carnation", "Purple Carnation", "White Carnation", "Blue Carnation", "Red Clover", "Yellow Clover", "Purple Clover", "White Clover", "Blue Clover", "Red Daffodil", "Yellow Daffodil", "Purple Daffodil", "White Daffodil", "Blue Daffodil", "Celestial Frostbloom", "Red Edelweiss", "Yellow Edelweiss", "Purple Edelweiss", "White Edelweiss", "Blue Edelweiss", "Red Gladiolus", "Yellow Gladiolus", "Purple Gladiolus", "White Gladiolus", "Blue Gladiolus", "Red Lavender", "Yellow Lavender", "Purple Lavender", "White Lavender", "Blue Lavender", "Red Lotus", "Yellow Lotus", "Purple Lotus", "White Lotus", "Blue Lotus", "Primula Enigma"
                        });
                        com.sunflowerland.mobile.BaseCategorySettingsActivity.turnOffAllToggles(requireContext(), "composter", new String[]{
                            "Composter", "Turbo Composter", "Premium Composter"
                        });
                        // Crafting now uses a simple boolean toggle (category_crafting) with no per-item toggles
                        requireActivity().recreate();
                    })
                    .show();
                return true;
            });
        }
        
        // Info/About button
        Preference infoPref = findPreference("info_button");
        if (infoPref != null) {
            infoPref.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(getActivity(), InfoActivity.class);
                startActivity(intent);
                return true;
            });
        }
        
        // Category toggles are now SwitchPreferenceCompat and do not need click listeners
    }
    
}
