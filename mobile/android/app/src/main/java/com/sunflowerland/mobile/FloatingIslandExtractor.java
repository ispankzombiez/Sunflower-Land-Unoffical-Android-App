package com.sunflowerland.mobile;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.sunflowerland.mobile.models.FarmItem;
import androidx.preference.PreferenceManager;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Extracts floating island schedule and shop data for notifications
 * - Schedule: Creates notifications for when floating island becomes available (startAt)
 * - Shop: Detects changes to shop items and creates notification when items change
 */
public class FloatingIslandExtractor {
    private static final String TAG = "FloatingIslandExtractor";
    private static final String SNAPSHOT_FILE = "floating_island_snapshot.json";
    private Context context;

    /**
     * Inner class to represent a schedule entry
     */
    public static class ScheduleEntry {
        public long startAt;
        public long endAt;
    }

    /**
     * Inner class to represent a shop item
     */
    public static class ShopItem {
        public String name;
        public long loveCost;

        public ShopItem(String name, long cost) {
            this.name = name;
            this.loveCost = cost;
        }
    }

    public FloatingIslandExtractor(Context context) {
        this.context = context;
    }

    /**
     * Main extraction method - returns list of FarmItems for notifications
     */
    public List<FarmItem> extractFloatingIslandNotifications(JsonObject farmData) {
        List<FarmItem> items = new ArrayList<>();

        if (farmData == null || !farmData.has("floatingIsland")) {
            DebugLog.log("🏝️ Floating Island: No floatingIsland data found in farm response");
            return items;
        }

        try {
            JsonObject floatingIsland = farmData.getAsJsonObject("floatingIsland");
            long currentTime = System.currentTimeMillis();
            DebugLog.log("🏝️ Floating Island: Processing floating island notifications (currentTime: " + currentTime + ")");

            // Extract schedule notifications
            List<FarmItem> scheduleItems = extractScheduleNotifications(floatingIsland, currentTime);
            items.addAll(scheduleItems);
            DebugLog.log("🏝️ Floating Island: Extracted " + scheduleItems.size() + " schedule notification(s)");

            // Extract shop change notifications
            List<FarmItem> shopItems = extractShopChangeNotifications(floatingIsland, currentTime);
            items.addAll(shopItems);
            DebugLog.log("🏝️ Floating Island: Extracted " + shopItems.size() + " shop change notification(s)");

        } catch (Exception e) {
            DebugLog.log("❌ Floating Island: Error extracting floating island data: " + e.getMessage());
        }

        return items;
    }

    /**
     * Extract schedule entries and create notifications for startAt times
     */
    private List<FarmItem> extractScheduleNotifications(JsonObject floatingIsland, long currentTime) {
        List<FarmItem> items = new ArrayList<>();

        try {
            if (!floatingIsland.has("schedule")) {
                DebugLog.log("🏝️ Floating Island: No schedule found in floatingIsland data");
                return items;
            }

            JsonArray schedule = floatingIsland.getAsJsonArray("schedule");
            DebugLog.log("🏝️ Floating Island: Processing " + schedule.size() + " schedule entries (currentTime: " + currentTime + ")");

            for (int i = 0; i < schedule.size(); i++) {
                JsonObject entry = schedule.get(i).getAsJsonObject();

                long startAt = entry.get("startAt").getAsLong();
                long endAt = entry.get("endAt").getAsLong();

                DebugLog.log("🏝️ Floating Island: Schedule entry " + (i+1) + ": startAt=" + formatTimestamp(startAt) + 
                      " (" + startAt + "), endAt=" + formatTimestamp(endAt) + ", currentTime=" + currentTime);

                // Only include if startAt is in the future
                if (startAt > currentTime) {
                    // Store both startAt and endAt in the details so we can format the notification later
                    String details = startAt + "|" + endAt; // pipe-separated for easy parsing
                    FarmItem item = new FarmItem("floating_island", "Floating Island", 1, startAt);
                    item.setDetails(details);
                    items.add(item);
                    DebugLog.log("✅ Floating Island: Added schedule: startAt=" + formatTimestamp(startAt) + 
                          ", endAt=" + formatTimestamp(endAt));
                } else {
                    DebugLog.log("⏭️ Floating Island: Skipping schedule entry - startAt already passed (diff: " + (currentTime - startAt) + "ms)");
                }
            }
        } catch (Exception e) {
            DebugLog.log("❌ Floating Island: Error extracting schedule notifications: " + e.getMessage());
        }

        return items;
    }

    /**
     * Extract shop items and detect changes
     */
    private List<FarmItem> extractShopChangeNotifications(JsonObject floatingIsland, long currentTime) {
        List<FarmItem> items = new ArrayList<>();

        try {
            if (!floatingIsland.has("shop")) {
                DebugLog.log("🏝️ Floating Island: No shop found");
                return items;
            }

            JsonObject shop = floatingIsland.getAsJsonObject("shop");
            List<ShopItem> currentItems = new ArrayList<>();

            // Parse current shop items
            for (String key : shop.keySet()) {
                JsonObject itemData = shop.getAsJsonObject(key);
                String itemName = itemData.get("name").getAsString();
                
                long loveCost = 0;
                if (itemData.has("cost")) {
                    JsonObject cost = itemData.getAsJsonObject("cost");
                    if (cost.has("items")) {
                        JsonObject items_obj = cost.getAsJsonObject("items");
                        if (items_obj.has("Love Charm")) {
                            loveCost = items_obj.get("Love Charm").getAsLong();
                        }
                    }
                }

                currentItems.add(new ShopItem(itemName, loveCost));
                DebugLog.log("🏝️ Floating Island: Found shop item: " + itemName + " - " + loveCost + " Love Charm");
            }

            // Check if items have changed
            if (hasShopChanged(currentItems)) {
                // Create notification with all current items
                String shopDetails = formatShopItems(currentItems);
                FarmItem item = new FarmItem("floating_island_shop", "Love Island Shop", 1, currentTime);
                item.setDetails(shopDetails);
                items.add(item);
                DebugLog.log("✅ Floating Island: Shop items changed! Creating notification");

                // Save new snapshot
                saveShopSnapshot(currentItems);
            } else {
                DebugLog.log("🏝️ Floating Island: Shop items unchanged");
            }
        } catch (Exception e) {
            DebugLog.log("❌ Floating Island: Error extracting shop notifications: " + e.getMessage());
        }

        return items;
    }

    /**
     * Check if shop items have changed compared to snapshot
     */
    private boolean hasShopChanged(List<ShopItem> currentItems) {
        List<ShopItem> previousItems = loadShopSnapshot();
        
        // If no previous snapshot, this is first time
        if (previousItems == null) {
            DebugLog.log("🏝️ Floating Island: No previous snapshot - shop is new or first check");
            return true;
        }

        // If sizes differ, items changed
        if (previousItems.size() != currentItems.size()) {
            DebugLog.log("🏝️ Floating Island: Shop size changed from " + previousItems.size() + " to " + currentItems.size());
            return true;
        }

        // Check each item
        for (ShopItem current : currentItems) {
            boolean found = false;
            for (ShopItem previous : previousItems) {
                if (current.name.equals(previous.name) && current.loveCost == previous.loveCost) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                DebugLog.log("🏝️ Floating Island: Item not found in previous snapshot: " + current.name);
                return true;
            }
        }

        return false;
    }

    /**
     * Format shop items for notification display
     */
    private String formatShopItems(List<ShopItem> items) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            ShopItem item = items.get(i);
            sb.append(item.name).append(" - ").append(item.loveCost).append(" Love Charm");
            if (i < items.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Load shop snapshot from file
     */
    private List<ShopItem> loadShopSnapshot() {
        try {
            File snapshotFile = new File(context.getFilesDir(), SNAPSHOT_FILE);
            if (!snapshotFile.exists()) {
                DebugLog.log("🏝️ Floating Island: No snapshot file found");
                return null;
            }

            Gson gson = new Gson();
            FileReader reader = new FileReader(snapshotFile);
            ShopItem[] items = gson.fromJson(reader, ShopItem[].class);
            reader.close();

            List<ShopItem> itemList = new ArrayList<>();
            if (items != null) {
                for (ShopItem item : items) {
                    itemList.add(item);
                }
            }
            DebugLog.log("🏝️ Floating Island: Loaded " + itemList.size() + " items from snapshot");
            return itemList;
        } catch (Exception e) {
            DebugLog.log("❌ Floating Island: Error loading snapshot: " + e.getMessage());
            return null;
        }
    }

    /**
     * Save shop snapshot to file
     */
    private void saveShopSnapshot(List<ShopItem> items) {
        try {
            File snapshotFile = new File(context.getFilesDir(), SNAPSHOT_FILE);
            Gson gson = new Gson();
            FileWriter writer = new FileWriter(snapshotFile);
            gson.toJson(items.toArray(new ShopItem[0]), writer);
            writer.close();
            DebugLog.log("✅ Floating Island: Saved snapshot with " + items.size() + " items");
        } catch (IOException e) {
            DebugLog.log("❌ Floating Island: Error saving snapshot: " + e.getMessage());
        }
    }

    /**
     * Format timestamp as human-readable string
     */
    private String formatTimestamp(long timestamp) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        } catch (Exception e) {
            return String.valueOf(timestamp);
        }
    }
}
