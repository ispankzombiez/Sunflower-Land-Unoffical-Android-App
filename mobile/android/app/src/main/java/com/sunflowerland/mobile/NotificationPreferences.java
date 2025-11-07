package com.sunflowerland.mobile;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.preference.PreferenceManager;

public class NotificationPreferences {
    private static final String TAG = "NotificationPreferences";
    
    public static boolean areNotificationsEnabled(Context context, String category, String itemName) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        
        Log.d(TAG, "========== Checking notification preferences ==========");
        Log.d(TAG, "Category: " + category + ", Item: " + itemName);
        
        // Check master toggle
        boolean masterEnabled = prefs.getBoolean("notifications_master", true);
        Log.d(TAG, "Master toggle (notifications_master): " + masterEnabled);
        if (!masterEnabled) {
            Log.d(TAG, "❌ Master toggle disabled - blocking all notifications");
            return false;
        }
        
        // Check category toggle (special case for marketplace and floating_island)
        String categoryKey;
        if ("marketplace".equals(category)) {
            categoryKey = "marketplace_listings_enabled";
        } else if ("floating_island".equals(category)) {
            categoryKey = "floating_island_enabled";
        } else {
            categoryKey = "category_" + category.toLowerCase();
        }
        
        boolean categoryEnabled = prefs.getBoolean(categoryKey, true);
        Log.d(TAG, "Category toggle (" + categoryKey + "): " + categoryEnabled);
        if (!categoryEnabled) {
            Log.d(TAG, "❌ Category '" + category + "' disabled - blocking notification");
            return false;
        }
        
        // Check specific item toggle (marketplace, crafting, auction, floating_island, and animal_sick don't have per-item toggles)
        if ("marketplace".equals(category) || "crafting".equals(category) || "auction".equals(category) || "floating_island".equals(category) || "animal_sick".equals(category)) {
            Log.d(TAG, "✅ Final result for " + category + "/" + itemName + ": true (no per-item toggles for " + category + ")");
            return true;
        }
        
        String itemKey = category.toLowerCase() + "_" + itemName.toLowerCase().replace(" ", "_");
        boolean itemEnabled = prefs.getBoolean(itemKey, true);
        Log.d(TAG, "Item toggle (" + itemKey + "): " + itemEnabled);
        
        Log.d(TAG, "✅ Final result for " + category + "/" + itemName + ": " + itemEnabled);
        return itemEnabled;
    }
    
    public static boolean shouldGroupCookingByBuilding(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("cooking_group_by_building", false);
    }
}
