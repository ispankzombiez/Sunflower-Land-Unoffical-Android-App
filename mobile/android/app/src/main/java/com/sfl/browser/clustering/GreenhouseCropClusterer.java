package com.sfl.browser.clustering;

import android.util.Log;
import com.sfl.browser.models.FarmItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Clustering strategy for GREENHOUSE CROPS
 * 
 * Rules:
 * 1. Group by crop name (all Olive together, all Rice together, all Grape together)
 * 2. Within each name group, cluster by 1-minute time windows
 * 3. Items within 60 seconds of the earliest item in cluster stay together
 * 4. Each cluster = 1 notification showing total quantity
 * 5. Notification time = earliest ready time in cluster
 */
public class GreenhouseCropClusterer extends CategoryClusterer {
    private static final String TAG = "GreenhouseCropClusterer";
    private static final long ONE_MINUTE_MS = 60000;  // 1 minute in milliseconds
    
    @Override
    public List<NotificationGroup> cluster(List<FarmItem> items) {
        Log.d(TAG, "Clustering " + items.size() + " greenhouse crop items");
        List<NotificationGroup> groups = new ArrayList<>();
        
        if (items.isEmpty()) {
            return groups;
        }
        
        // Step 1: Group all items by crop name
        Map<String, List<FarmItem>> byName = groupByName(items);
        Log.d(TAG, "Grouped into " + byName.size() + " greenhouse crop name(s)");
        
        // Step 2: For each crop name, cluster by 1-minute windows
        for (Map.Entry<String, List<FarmItem>> entry : byName.entrySet()) {
            String cropName = entry.getKey();
            List<FarmItem> nameGroup = entry.getValue();
            
            // Sort by ready time ascending
            nameGroup.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
            
            Log.d(TAG, "Clustering " + cropName + ": " + nameGroup.size() + " patch(es)");
            
            // Cluster this name group by 1-minute windows
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
                    Log.d(TAG, "    Added item (+" + (timeSinceClusterStart / 1000) + "s)");
                } else {
                    // Outside window, create cluster and start new one
                    NotificationGroup cluster = createCluster(currentCluster);
                    clusters.add(cluster);
                    Log.d(TAG, "  Cluster end: total=" + currentCluster.size());
                    
                    // Start new cluster
                    currentCluster = new ArrayList<>();
                    currentCluster.add(item);
                    clusterStartTime = item.getTimestamp();
                    Log.d(TAG, "  Cluster start: " + item.getName() + " @ " + item.getTimestamp());
                }
            }
        }
        
        // Don't forget the last cluster
        if (!currentCluster.isEmpty()) {
            NotificationGroup cluster = createCluster(currentCluster);
            clusters.add(cluster);
            Log.d(TAG, "  Cluster end: total=" + currentCluster.size());
        }
        
        return clusters;
    }
    
    /**
     * Create a NotificationGroup from a cluster of items
     */
    private NotificationGroup createCluster(List<FarmItem> cluster) {
        // Use first item's timestamp (earliest = cluster ready time)
        FarmItem earliest = cluster.get(0);
        
        // Sum quantities
        int totalQuantity = cluster.stream().mapToInt(FarmItem::getAmount).sum();
        
        // All items in this cluster are the same name, so use that name
        String itemName = earliest.getName();
        
        // Create group ID from crop name + timestamp
        String groupId = itemName.toLowerCase() + "_" + earliest.getTimestamp();
        
        NotificationGroup group = new NotificationGroup();
        group.earliestReadyTime = earliest.getTimestamp();
        group.quantity = totalQuantity;
        group.name = itemName;
        group.category = "greenhouse_crops";
        group.groupId = groupId;
        group.details = null;  // No additional details needed for greenhouse crops
        
        Log.d(TAG, "    Created group: " + totalQuantity + "x " + itemName + 
              " ready at " + earliest.getTimestamp());
        
        return group;
    }
}
