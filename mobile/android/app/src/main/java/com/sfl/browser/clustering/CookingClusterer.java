package com.sfl.browser.clustering;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import com.sfl.browser.models.FarmItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Clustering strategy for cooking items
 * 
 * Mode 1 (Default - toggle OFF):
 * - NO clustering - each cooking item gets its own notification at its readyAt time
 * - One notification per item
 * 
 * Mode 2 (toggle ON - "group by building"):
 * - Group by building name
 * - Use LATEST (maximum) timestamp from each building
 * - One notification per building showing all items ready at or before that time
 * - Example: "Kitchen is empty! 3 Sunflower Crunch, 1 Roast Veggies"
 */
public class CookingClusterer extends CategoryClusterer {
    private static final String TAG = "CookingClusterer";
    private Context context;

    public CookingClusterer(Context context) {
        this.context = context;
    }

    @Override
    public List<NotificationGroup> cluster(List<FarmItem> items) {
        Log.d(TAG, "Clustering " + items.size() + " cooking items");
        List<NotificationGroup> groups = new ArrayList<>();

        if (items.isEmpty()) {
            return groups;
        }

        // Check if "group by building" toggle is enabled
        boolean groupByBuilding = false;
        if (context != null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            groupByBuilding = prefs.getBoolean("cooking_group_by_building", false);
        }

        if (groupByBuilding) {
            groups = clusterByBuilding(items);
        } else {
            groups = clusterNoGrouping(items);
        }

        Log.d(TAG, "Created " + groups.size() + " notification group(s) (groupByBuilding=" + groupByBuilding + ")");
        return groups;
    }

    /**
     * Mode 1: No grouping - each cooking item gets its own notification
     */
    private List<NotificationGroup> clusterNoGrouping(List<FarmItem> items) {
        List<NotificationGroup> groups = new ArrayList<>();

        for (FarmItem item : items) {
            NotificationGroup group = new NotificationGroup(
                "cooking",
                item.getName(),
                item.getAmount(),
                item.getTimestamp()
            );

            group.groupId = generateClusterId(group);
            groups.add(group);

            Log.d(TAG, "  Group (no clustering): " + item.getAmount() + " " + item.getName() +
                  " from " + item.getBuildingName() + 
                  " ready at " + item.getTimestamp() + " (id=" + group.groupId + ")");
        }

        return groups;
    }

    /**
     * Mode 2: Group by building, use latest timestamp from each building
     * Items with same name are grouped together with sum of quantities
     */
    private List<NotificationGroup> clusterByBuilding(List<FarmItem> items) {
        List<NotificationGroup> groups = new ArrayList<>();

        // Map: buildingName -> map of itemName -> total quantity
        Map<String, Map<String, Integer>> itemsByBuildingAndName = new HashMap<>();
        // Map: buildingName -> latest timestamp
        Map<String, Long> latestTimestampByBuilding = new HashMap<>();

        for (FarmItem item : items) {
            String buildingName = item.getBuildingName();
            if (buildingName == null || buildingName.isEmpty()) {
                buildingName = "Unknown";
            }

            // Track latest timestamp for this building
            long currentLatest = latestTimestampByBuilding.getOrDefault(buildingName, 0L);
            latestTimestampByBuilding.put(buildingName, Math.max(currentLatest, item.getTimestamp()));

            // Group items by name
            Map<String, Integer> buildingItems = itemsByBuildingAndName.computeIfAbsent(buildingName, k -> new HashMap<>());
            String itemName = item.getName();
            buildingItems.put(itemName, buildingItems.getOrDefault(itemName, 0) + item.getAmount());
        }

        // For each building, create one notification group with latest timestamp
        for (String buildingName : itemsByBuildingAndName.keySet()) {
            Map<String, Integer> itemsByName = itemsByBuildingAndName.get(buildingName);
            long latestTimestamp = latestTimestampByBuilding.get(buildingName);

            // Calculate total quantity and build items list
            int totalQuantity = 0;
            List<String> itemLines = new ArrayList<>();
            
            for (String itemName : itemsByName.keySet()) {
                int quantity = itemsByName.get(itemName);
                totalQuantity += quantity;
                // Only show quantity if more than 1
                if (quantity > 1) {
                    itemLines.add(quantity + " " + itemName);
                } else {
                    itemLines.add(itemName);
                }
            }

            StringBuilder itemsList = new StringBuilder();
            for (int i = 0; i < itemLines.size(); i++) {
                itemsList.append(itemLines.get(i));
                if (i < itemLines.size() - 1) {
                    itemsList.append(", ");
                }
            }

            NotificationGroup group = new NotificationGroup(
                "cooking",
                buildingName,
                totalQuantity,
                latestTimestamp
            );
            
            // Store the items list as a detail in the group for notification display
            group.details = itemsList.toString();

            group.groupId = generateClusterId(group);
            groups.add(group);

            Log.d(TAG, "  Group (by building): " + buildingName + 
                  " with " + itemsByName.size() + " item type(s): " + itemsList.toString() +
                  " ready at " + latestTimestamp + " (id=" + group.groupId + ")");
        }

        return groups;
    }
}
