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
        // Load preferences from XML first so findPreference(...) returns actual objects
        setPreferencesFromResource(R.xml.preferences, rootKey);

        // --- Only Notifications Toggle ---
        SwitchPreferenceCompat onlyNotificationsPref = findPreference("only_notifications");

                // --- No Browser Controls Toggle ---
                SwitchPreferenceCompat noBrowserControlsPref = findPreference("no_browser_controls");
                if (noBrowserControlsPref != null) {
                    SharedPreferences prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext());
                    if (!prefs.contains("no_browser_controls")) {
                        prefs.edit().putBoolean("no_browser_controls", true).apply();
                    }
                    noBrowserControlsPref.setOnPreferenceChangeListener((preference, newValue) -> {
                        boolean enabled = Boolean.TRUE.equals(newValue);
                        android.util.Log.d("SettingsFragment", "No browser controls toggled: " + enabled);
                        // TODO: Wire this to browser UI logic
                        return true;
                    });
                }


                // --- Home Page Editable Field ---
                Preference homePagePref = findPreference("home_page");
                if (homePagePref != null) {
                    SharedPreferences prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext());
                    final String DEFAULT_URL = "https://sunflower-land.com/play/?ref=iSPANK";
                    if (!prefs.contains("home_page")) {
                        prefs.edit().putString("home_page", DEFAULT_URL).apply();
                        homePagePref.setSummary(DEFAULT_URL);
                    } else {
                        String url = prefs.getString("home_page", DEFAULT_URL);
                        homePagePref.setSummary(url);
                    }
                    homePagePref.setOnPreferenceChangeListener((preference, newValue) -> {
                        String url = (newValue != null) ? newValue.toString().trim() : "";
                        if (url.isEmpty()) url = DEFAULT_URL;
                        // Ensure a scheme is present so WebView.loadUrl behaves predictably
                        if (!url.startsWith("http://") && !url.startsWith("https://")) {
                            url = "https://" + url;
                        }
                        prefs.edit().putString("home_page", url).apply();
                        homePagePref.setSummary(url);
                        android.util.Log.d("SettingsFragment", "Home page set to: " + url);
                        return true;
                    });
                }
                // --- Home Page 2 Editable Field ---
                Preference homePage2Pref = findPreference("home_page_2");
                if (homePage2Pref != null) {
                    SharedPreferences prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext());
                    final String DEFAULT_URL2 = "https://sfl.world";
                    if (!prefs.contains("home_page_2")) {
                        prefs.edit().putString("home_page_2", DEFAULT_URL2).apply();
                        homePage2Pref.setSummary(DEFAULT_URL2);
                    } else {
                        String url = prefs.getString("home_page_2", DEFAULT_URL2);
                        homePage2Pref.setSummary(url);
                    }
                    homePage2Pref.setOnPreferenceChangeListener((preference, newValue) -> {
                        String url = (newValue != null) ? newValue.toString().trim() : "";
                        if (url.isEmpty()) url = DEFAULT_URL2;
                        if (!url.startsWith("http://") && !url.startsWith("https://")) {
                            url = "https://" + url;
                        }
                        prefs.edit().putString("home_page_2", url).apply();
                        homePage2Pref.setSummary(url);
                        android.util.Log.d("SettingsFragment", "Home page 2 set to: " + url);
                        return true;
                    });
                }

                // --- Home Page 3 Editable Field ---
                Preference homePage3Pref = findPreference("home_page_3");
                if (homePage3Pref != null) {
                    SharedPreferences prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext());
                    final String DEFAULT_URL3 = "https://wiki.sfl.world";
                    if (!prefs.contains("home_page_3")) {
                        prefs.edit().putString("home_page_3", DEFAULT_URL3).apply();
                        homePage3Pref.setSummary(DEFAULT_URL3);
                    } else {
                        String url = prefs.getString("home_page_3", DEFAULT_URL3);
                        homePage3Pref.setSummary(url);
                    }
                    homePage3Pref.setOnPreferenceChangeListener((preference, newValue) -> {
                        String url = (newValue != null) ? newValue.toString().trim() : "";
                        if (url.isEmpty()) url = DEFAULT_URL3;
                        if (!url.startsWith("http://") && !url.startsWith("https://")) {
                            url = "https://" + url;
                        }
                        prefs.edit().putString("home_page_3", url).apply();
                        homePage3Pref.setSummary(url);
                        android.util.Log.d("SettingsFragment", "Home page 3 set to: " + url);
                        return true;
                    });
                }
        // (preferences already loaded above)

        // Debug/Dev: Diagnostics
        Preference diagnosticsPref = findPreference("open_diagnostics");
        if (diagnosticsPref != null) {
            diagnosticsPref.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(requireContext(), DiagnosticsActivity.class);
                startActivity(intent);
                return true;
            });
        }

        // Now wire up 'app to open' visibility and logic
        Preference appToOpenPref = findPreference("app_to_open");
        if (appToOpenPref != null && onlyNotificationsPref != null) {
            // Set default if blank or unset
            SharedPreferences prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(requireContext());
            String currentPackage = prefs.getString("app_to_open", null);
            if (currentPackage == null || currentPackage.trim().isEmpty()) {
                prefs.edit().putString("app_to_open", requireContext().getPackageName()).apply();
            }
            // Hide by default unless notifications only is true
            boolean notificationsOnly = onlyNotificationsPref.isChecked();
            appToOpenPref.setVisible(notificationsOnly);
            onlyNotificationsPref.setOnPreferenceChangeListener((preference, newValue) -> {
                boolean enabled = Boolean.TRUE.equals(newValue);
                appToOpenPref.setVisible(enabled);
                return true;
            });

            // Show all launchable apps with icons in a custom dialog
            appToOpenPref.setOnPreferenceClickListener(preference -> {
                final android.content.pm.PackageManager pm = requireContext().getPackageManager();
                final String[][] walletApps = new String[][] {
                    {"Phantom", "app.phantom"},
                    {"MetaMask", "io.metamask"},
                    {"Ronin Wallet", "com.skymavis.genesis"},
                    {"Rabby Wallet", "com.debank.rabbymobile"},
                    {"Ledger Wallet", "com.ledger.live"},
                    {"Coinbase", "com.coinbase.android"},
                    {"OKX Wallet", "com.okx.wallet"},
                    {"Trust Wallet", "com.wallet.crypto.trustapp"}
                };
                java.util.List<String> displayNames = new java.util.ArrayList<>();
                java.util.List<String> packageNames = new java.util.ArrayList<>();
                java.util.List<android.graphics.drawable.Drawable> icons = new java.util.ArrayList<>();
                for (String[] wallet : walletApps) {
                    String label = wallet[0];
                    String pkg = wallet[1];
                    try {
                        Intent launchIntent = pm.getLaunchIntentForPackage(pkg);
                        if (launchIntent != null) {
                            displayNames.add(label);
                            packageNames.add(pkg);
                            try {
                                icons.add(pm.getApplicationIcon(pkg));
                            } catch (Exception e) {
                                icons.add(null);
                            }
                        }
                    } catch (Exception e) {
                        // Not installed or not launchable, skip
                    }
                }
                displayNames.add("Custom...");
                icons.add(null);
                android.widget.ListAdapter adapter = new android.widget.BaseAdapter() {
                    @Override public int getCount() { return displayNames.size(); }
                    @Override public Object getItem(int i) { return displayNames.get(i); }
                    @Override public long getItemId(int i) { return i; }
                    @Override public android.view.View getView(int i, android.view.View convertView, android.view.ViewGroup parent) {
                        android.view.LayoutInflater inflater = android.view.LayoutInflater.from(requireContext());
                        android.view.View view = inflater.inflate(android.R.layout.select_dialog_item, parent, false);
                        android.widget.TextView text = view.findViewById(android.R.id.text1);
                        text.setText(displayNames.get(i));
                        android.graphics.drawable.Drawable icon = icons.get(i);
                        if (icon != null) {
                            int iconSize = (int) (text.getLineHeight() * 1.2f);
                            icon.setBounds(0, 0, iconSize, iconSize);
                            text.setCompoundDrawables(icon, null, null, null);
                            text.setCompoundDrawablePadding(16);
                        }
                        return view;
                    }
                };
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Select App to Open")
                        .setAdapter(adapter, (dialog, which) -> {
                            if (which < packageNames.size()) {
                                String pkg = packageNames.get(which);
                                String label = displayNames.get(which);
                                prefs.edit().putString("app_to_open", pkg).apply();
                                appToOpenPref.setSummary(label + " (" + pkg + ")");
                            } else {
                                // Custom option
                                final android.widget.EditText input = new android.widget.EditText(requireContext());
                                input.setHint("package.name or package/name");
                                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                                        .setTitle("Enter Package Name")
                                        .setView(input)
                                        .setPositiveButton("OK", (d, w) -> {
                                            String value = input.getText().toString().trim();
                                            if (!value.isEmpty()) {
                                                prefs.edit().putString("app_to_open", value).apply();
                                                appToOpenPref.setSummary("Custom: " + value);
                                            }
                                        })
                                        .setNegativeButton("Cancel", null)
                                        .show();
                            }
                        })
                        .show();
                return true;
            });
        }

        // Debug/Dev: View Notification Log
        Preference viewLogPref = findPreference("view_notification_log");
        if (viewLogPref != null) {
            viewLogPref.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(getActivity(), NotificationLogActivity.class);
                startActivity(intent);
                return true;
            });
        }

        // Debug/Dev: Notification Testing
        Preference notificationTestingPref = findPreference("notification_testing");
        if (notificationTestingPref != null) {
            notificationTestingPref.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(getActivity(), NotificationTestingActivity.class);
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

        // Handle "Only Notifications" mode toggle (already handled above for visibility logic)

        // Handle "Open Game (One Time)" button
        Preference openWebViewPref = findPreference("open_webview");
        if (openWebViewPref != null) {
            openWebViewPref.setOnPreferenceClickListener(preference -> {
                // Start MainActivity temporarily
                Intent mainIntent = new Intent(getActivity(), MainActivity.class);
                mainIntent.putExtra("bypass_notifications_only", true);
                startActivity(mainIntent);
                return true;
            });
        }

        // Handle "Default Wallet" button
        Preference defaultWalletPref = findPreference("default_wallet");
        if (defaultWalletPref != null) {
            updateDefaultWalletSummary(defaultWalletPref);
            defaultWalletPref.setOnPreferenceClickListener(preference -> {
                showWalletSelectionDialog();
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
                        editor.putBoolean("category_skill_cooldown", true);
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
                        com.sunflowerland.mobile.BaseCategorySettingsActivity.turnOnAllToggles(requireContext(), "skill", new String[]{
                            "Instant Growth", "Tree Blitz", "Instant Gratification", "Barnyard Rouse", "Petal Blessed", "Greenhouse Guru", "Grease Lightning"
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

        Preference skillsPref = findPreference("category_skills");
        if (skillsPref != null) {
            skillsPref.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(getActivity(), SkillSettingsActivity.class);
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
                        editor.putBoolean("category_skill_cooldown", false);
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

    @Override
    public void onResume() {
        super.onResume();
        // Listen for orientation preference changes
        androidx.preference.Preference orientationPref = findPreference("orientation");
        if (orientationPref != null) {
            orientationPref.setOnPreferenceChangeListener((preference, newValue) -> {
                // Notify MainActivity to update orientation
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).applyOrientationSetting((String) newValue);
                }
                return true;
            });
        }
    }

    /**
     * Update the summary of the default wallet preference to show the selected wallet.
     */
    private void updateDefaultWalletSummary(Preference pref) {
        String walletName = com.sunflowerland.mobile.wallet.WalletPreferenceManager.getDefaultWalletName(requireContext());
        if (walletName != null) {
            pref.setSummary("Selected: " + walletName);
        } else {
            pref.setSummary("Select your default wallet for the game");
        }
    }

    /**
     * Show a dialog to select the default wallet.
     * Currently shows MetaMask only.
     */
    private void showWalletSelectionDialog() {
        // Currently only MetaMask is supported
        String[] walletNames = {"MetaMask"};
        String[] walletIds = {"metamask"};

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Select Default Wallet")
            .setItems(walletNames, (dialog, which) -> {
                String selectedId = walletIds[which];
                String selectedName = walletNames[which];
                
                // Save the selection
                com.sunflowerland.mobile.wallet.WalletPreferenceManager.setDefaultWallet(
                    requireContext(),
                    selectedId,
                    selectedName
                );
                
                // Update the summary
                Preference pref = findPreference("default_wallet");
                if (pref != null) {
                    updateDefaultWalletSummary(pref);
                }
                
                android.widget.Toast.makeText(
                    requireContext(),
                    "Default wallet set to: " + selectedName,
                    android.widget.Toast.LENGTH_SHORT
                ).show();
            })
            .show();
    }
    
}
