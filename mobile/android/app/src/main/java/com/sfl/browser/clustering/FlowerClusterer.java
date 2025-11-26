package com.sfl.browser.clustering;

import android.util.Log;
import com.sfl.browser.models.FarmItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Clustering strategy for flower items
 * 
 * Rules:
 * 1. Group by flower name
 * 2. Items ready within 1-minute window = same group
 * 3. Each unique (name, timeWindow) combination = one notification
 * 4. Total quantity = sum of all flower beds with that name ready in same window
 * 5. Ready time = earliest flower's timestamp in the group
 * 
 * This allows multiple notifications for same flower type if they're ready at different times.
 */
public class FlowerClusterer extends CategoryClusterer {
    private static final String TAG = "FlowerClusterer";
    private static final long CLUSTERING_WINDOW = 60_000; // 1 minute in milliseconds

    @Override
    public List<NotificationGroup> cluster(List<FarmItem> items) {
        Log.d(TAG, "Clustering " + items.size() + " flower items");
        List<NotificationGroup> groups = new ArrayList<>();

        if (items.isEmpty()) {
            return groups;
        }

        // Map: flowerName -> list of items with that name
        Map<String, List<FarmItem>> itemsByName = new HashMap<>();

        for (FarmItem item : items) {
            String name = item.getName();
            itemsByName.computeIfAbsent(name, k -> new ArrayList<>()).add(item);
        }

        // For each flower name, create time-based clusters
        for (String flowerName : itemsByName.keySet()) {
            List<FarmItem> itemsWithName = itemsByName.get(flowerName);

            // Sort by timestamp
            itemsWithName.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));

            // Group by time window
            List<List<FarmItem>> timeClusters = new ArrayList<>();
            List<FarmItem> currentCluster = new ArrayList<>();
            long clusterStartTime = 0;

            for (FarmItem item : itemsWithName) {
                if (currentCluster.isEmpty()) {
                    currentCluster.add(item);
                    clusterStartTime = item.getTimestamp();
                } else {
                    // Check if within clustering window of cluster start time (not first item)
                    long timeSinceClusterStart = item.getTimestamp() - clusterStartTime;
                    if (timeSinceClusterStart <= CLUSTERING_WINDOW) {
                        currentCluster.add(item);
                    } else {
                        // Start a new cluster
                        timeClusters.add(currentCluster);
                        currentCluster = new ArrayList<>();
                        currentCluster.add(item);
                        clusterStartTime = item.getTimestamp();
                    }
                }
            }

            // Don't forget the last cluster
            if (!currentCluster.isEmpty()) {
                timeClusters.add(currentCluster);
            }

            // Create one notification group per time cluster
            for (List<FarmItem> cluster : timeClusters) {
                int totalQuantity = 0;
                long earliestTime = Long.MAX_VALUE;

                for (FarmItem item : cluster) {
                    totalQuantity += item.getAmount();
                    earliestTime = Math.min(earliestTime, item.getTimestamp());
                }

                NotificationGroup group = new NotificationGroup(
                    "flowers",
                    flowerName,
                    totalQuantity,
                    earliestTime
                );

                group.groupId = generateClusterId(group);
                groups.add(group);

                Log.d(TAG, "  Group: " + totalQuantity + " " + flowerName +
                      " ready at " + earliestTime + " (id=" + group.groupId + ")");
            }
        }

        Log.d(TAG, "Created " + groups.size() + " notification group(s)");
        return groups;
    }
}
