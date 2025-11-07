package com.sunflowerland.mobile.clustering;

import android.util.Log;
import com.sunflowerland.mobile.models.FarmItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Clustering strategy for FRUITS
 * 
 * Rules:
 * 1. Group by fruit name (all Lemon together, all Orange together, etc.)
 * 2. Within each name group, cluster by 1-minute time windows
 * 3. Items within 60 seconds of the earliest item in cluster stay together
 * 4. Each cluster = 1 notification showing total quantity
 * 5. Notification time = earliest ready time in cluster
 */
public class FruitClusterer extends CategoryClusterer {
    private static final String TAG = "FruitClusterer";
    private static final long ONE_MINUTE_MS = 60000;  // 1 minute in milliseconds
    
    @Override
    public List<NotificationGroup> cluster(List<FarmItem> items) {
        Log.d(TAG, "Clustering " + items.size() + " fruit items");
        List<NotificationGroup> groups = new ArrayList<>();
        
        if (items.isEmpty()) {
            return groups;
        }
        
        // Step 1: Group all items by fruit name
        Map<String, List<FarmItem>> byName = groupByName(items);
        Log.d(TAG, "Grouped into " + byName.size() + " fruit name(s)");
        
        // Step 2: For each fruit name, cluster by 1-minute windows
        for (Map.Entry<String, List<FarmItem>> entry : byName.entrySet()) {
            String fruitName = entry.getKey();
            List<FarmItem> nameGroup = entry.getValue();
            
            // Sort by ready time ascending
            nameGroup.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
            
            Log.d(TAG, "Clustering " + fruitName + ": " + nameGroup.size() + " patch(es)");
            
            // Cluster this name group by 1-minute windows
            List<NotificationGroup> nameClusters = clusterByTimeWindow(nameGroup);
            groups.addAll(nameClusters);
        }
        
        Log.d(TAG, "Created " + groups.size() + " notification group(s)");
        return groups;
    }
    
    /**
     * Group items by fruit name
     */
    private Map<String, List<FarmItem>> groupByName(List<FarmItem> items) {
        Map<String, List<FarmItem>> byName = new HashMap<>();
        
        for (FarmItem item : items) {
            String key = item.getName();
            byName.computeIfAbsent(key, k -> new ArrayList<>()).add(item);
        }
        
        return byName;
    }
    
    /**
     * Cluster items by 1-minute time windows
     * Items are already sorted by timestamp
     * 
     * Example with 1-minute window:
     * Item 1: 10:00:00
     * Item 2: 10:00:30 (30 seconds later) → SAME cluster
     * Item 3: 10:02:00 (120 seconds from start) → NEW cluster
     */
    private List<NotificationGroup> clusterByTimeWindow(List<FarmItem> sortedItems) {
        List<NotificationGroup> clusters = new ArrayList<>();
        
        if (sortedItems.isEmpty()) {
            return clusters;
        }
        
        List<FarmItem> currentCluster = new ArrayList<>();
        long clusterStartTime = 0;
        
        for (FarmItem item : sortedItems) {
            if (currentCluster.isEmpty()) {
                // Start new cluster
                currentCluster.add(item);
                clusterStartTime = item.getTimestamp();
                Log.d(TAG, "  Cluster start: " + item.getName() + " @ " + item.getTimestamp());
            } else {
                long timeSinceClusterStart = item.getTimestamp() - clusterStartTime;
                
                if (timeSinceClusterStart <= ONE_MINUTE_MS) {
                    // Within 1-minute window, add to current cluster
                    currentCluster.add(item);
                    Log.d(TAG, "    Added to cluster: " + item.getName() + " (+" + timeSinceClusterStart + "ms)");
                } else {
                    // Outside 1-minute window, save current cluster and start new one
                    clusters.add(createNotificationGroup(currentCluster));
                    currentCluster = new ArrayList<>();
                    currentCluster.add(item);
                    clusterStartTime = item.getTimestamp();
                    Log.d(TAG, "  New cluster: " + item.getName() + " @ " + item.getTimestamp());
                }
            }
        }
        
        // Don't forget the last cluster
        if (!currentCluster.isEmpty()) {
            clusters.add(createNotificationGroup(currentCluster));
        }
        
        return clusters;
    }
    
    /**
     * Convert a list of items into a single NotificationGroup
     */
    private NotificationGroup createNotificationGroup(List<FarmItem> items) {
        if (items.isEmpty()) {
            return null;
        }
        
        // All items in cluster have same category and name
        FarmItem firstItem = items.get(0);
        
        NotificationGroup group = new NotificationGroup();
        group.category = firstItem.getCategory();
        group.name = firstItem.getName();
        group.quantity = items.size();
        
        // Earliest ready time in cluster
        group.earliestReadyTime = items.get(0).getTimestamp();
        
        // Generate unique ID from grouping
        group.groupId = generateClusterId(group);
        
        Log.d(TAG, "    Created group: " + group.quantity + " " + group.name + 
              " (groupId: " + group.groupId + ")");
        
        return group;
    }
}
