package com.sunflowerland.mobile.clustering;

import android.util.Log;
import com.sunflowerland.mobile.models.FarmItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Clustering strategy for CROPS
 * 
 * Rules:
 * 1. Group by crop name (all Beetroot together, all Sunflower together, etc.)
 * 2. Within each name group, cluster by 1-minute time windows
 * 3. Items within 60 seconds of the earliest item in cluster stay together
 * 4. Each cluster = 1 notification showing total quantity
 * 5. Notification time = earliest ready time in cluster
 */
public class CropClusterer extends CategoryClusterer {
    private static final String TAG = "CropClusterer";
    private static final long ONE_MINUTE_MS = 60000;  // 1 minute in milliseconds
    
    @Override
    public List<NotificationGroup> cluster(List<FarmItem> items) {
        Log.d(TAG, "Clustering " + items.size() + " crop items");
        List<NotificationGroup> groups = new ArrayList<>();
        
        if (items.isEmpty()) {
            return groups;
        }
        
        // Step 1: Group all items by crop name
        Map<String, List<FarmItem>> byName = groupByName(items);
        Log.d(TAG, "Grouped into " + byName.size() + " crop name(s)");
        
        // Step 2: For each crop name, cluster by 2-minute windows
        for (Map.Entry<String, List<FarmItem>> entry : byName.entrySet()) {
            String cropName = entry.getKey();
            List<FarmItem> nameGroup = entry.getValue();
            
            // Sort by ready time ascending
            nameGroup.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
            
            Log.d(TAG, "Clustering " + cropName + ": " + nameGroup.size() + " plot(s)");
            
            // Cluster this name group by 2-minute windows
            List<NotificationGroup> nameClusters = clusterByTimeWindow(nameGroup);
            groups.addAll(nameClusters);
        }
        
        Log.d(TAG, "Created " + groups.size() + " notification group(s)");
        return groups;
    }
    
    /**
     * Group items by crop name
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
        
        NotificationGroup group = new NotificationGroup(
            firstItem.getCategory(),
            firstItem.getName(),
            items.size(),
            firstItem.getTimestamp()  // Earliest time (items are sorted)
        );
        
        // Generate tracking ID
        group.groupId = generateClusterId(group);
        
        Log.d(TAG, "    Created group: " + group.quantity + " " + group.name + 
              " ready at " + group.earliestReadyTime + " (id=" + group.groupId + ")");
        
        return group;
    }
}
