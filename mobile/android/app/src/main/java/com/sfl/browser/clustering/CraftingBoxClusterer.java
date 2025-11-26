package com.sfl.browser.clustering;

import android.util.Log;
import com.sfl.browser.models.FarmItem;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Clustering strategy for crafting box items
 * 
 * Rules:
 * 1. Group by collectible name
 * 2. Items ready within 1-minute window = same group
 * 3. Each unique (name, timeWindow) combination = one notification
 * 4. Total quantity = sum of all items in that group
 * 5. Ready time = latest timestamp in the group (when all items in group are done)
 * 
 * This allows multiple notifications for same collectible if crafted at different times.
 */
public class CraftingBoxClusterer extends CategoryClusterer {
    private static final String TAG = "CraftingBoxClusterer";
    private static final long CLUSTERING_WINDOW = 60_000; // 1 minute in milliseconds

    @Override
    public List<NotificationGroup> cluster(List<FarmItem> items) {
        Log.d(TAG, "Clustering " + items.size() + " crafting box items");
        List<NotificationGroup> groups = new ArrayList<>();

        if (items.isEmpty()) {
            return groups;
        }

        // Map: collectible name -> list of items for that collectible
        Map<String, List<FarmItem>> itemsByName = new HashMap<>();

        for (FarmItem item : items) {
            String name = item.getName();  // Name stores the collectible name
            itemsByName.computeIfAbsent(name, k -> new ArrayList<>()).add(item);
        }

        // For each collectible, create time-based clusters
        for (String collectibleName : itemsByName.keySet()) {
            List<FarmItem> nameItems = itemsByName.get(collectibleName);

            // Sort by timestamp
            nameItems.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));

            // Group by time window
            List<FarmItem> currentCluster = new ArrayList<>();
            long clusterStartTime = -1;

            for (FarmItem item : nameItems) {
                if (clusterStartTime == -1) {
                    // Start new cluster
                    clusterStartTime = item.getTimestamp();
                    currentCluster.add(item);
                } else if (item.getTimestamp() - clusterStartTime <= CLUSTERING_WINDOW) {
                    // Add to current cluster
                    currentCluster.add(item);
                } else {
                    // Cluster boundary crossed, create notification group and start new cluster
                    if (!currentCluster.isEmpty()) {
                        groups.add(createNotificationGroup(collectibleName, currentCluster));
                    }
                    currentCluster = new ArrayList<>();
                    currentCluster.add(item);
                    clusterStartTime = item.getTimestamp();
                }
            }

            // Don't forget last cluster
            if (!currentCluster.isEmpty()) {
                groups.add(createNotificationGroup(collectibleName, currentCluster));
            }
        }

        Log.d(TAG, "Created " + groups.size() + " notification groups from " + items.size() + " items");
        return groups;
    }

    private NotificationGroup createNotificationGroup(String collectibleName, List<FarmItem> clusterItems) {
        // Calculate metrics for this cluster
        long latestReadyTime = clusterItems.stream()
            .mapToLong(FarmItem::getTimestamp)
            .max()
            .orElse(System.currentTimeMillis());
        
        long earliestReadyTime = clusterItems.stream()
            .mapToLong(FarmItem::getTimestamp)
            .min()
            .orElse(System.currentTimeMillis());

        int totalQuantity = clusterItems.stream()
            .mapToInt(FarmItem::getAmount)
            .sum();

        NotificationGroup group = new NotificationGroup();
        group.category = "crafting";
        group.name = collectibleName;
        group.details = totalQuantity + " " + collectibleName;
        group.quantity = totalQuantity;
        group.earliestReadyTime = latestReadyTime;
        
        // Generate unique groupId for deduplication in AlarmScheduler
        group.groupId = generateClusterId(group);

        Log.d(TAG, "Created notification group: " + totalQuantity + " " + collectibleName + 
            " ready at " + formatTimestamp(latestReadyTime) + " (id=" + group.groupId + ")");

        return group;
    }

    private String formatTimestamp(long timestamp) {
        try {
            Date date = new Date(timestamp);
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm:ss", Locale.US);
            return sdf.format(date);
        } catch (Exception e) {
            Log.w(TAG, "Error formatting timestamp: " + timestamp);
            return "Unknown";
        }
    }
}
