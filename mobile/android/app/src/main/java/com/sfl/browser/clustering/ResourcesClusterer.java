package com.sfl.browser.clustering;

import android.util.Log;
import com.sfl.browser.models.FarmItem;
import com.sfl.browser.clustering.NotificationGroup;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Clusters resources (Tree, Stone, Iron, Gold, Crimstone, Oil, Sunstone, etc.)
 * by name with a 5-minute time window.
 * 
 * Groups resources of the same type that are ready within 5 minutes of each other.
 * Each group is assigned a unique groupId and represents multiple resources ready around the same time.
 * 
 * Example: 3 trees ready at 14:32:15, 14:32:30, 14:36:00
 * Result: First 2 grouped together as "2 Tree" (within 5 min), 3rd as separate "1 Tree" (outside window)
 */
public class ResourcesClusterer extends CategoryClusterer {
    private static final String TAG = "ResourcesClusterer";
    private static final long CLUSTERING_WINDOW = 5 * 60 * 1000; // 5 minutes = 300,000 ms

    @Override
    public List<NotificationGroup> cluster(List<FarmItem> items) {
        Log.d(TAG, "Clustering " + items.size() + " resource(s) with 5-minute window");
        List<NotificationGroup> groups = new ArrayList<>();

        if (items == null || items.isEmpty()) {
            Log.d(TAG, "No items to cluster");
            return groups;
        }

        // Group items by resource name first
        Map<String, List<FarmItem>> itemsByName = new HashMap<>();
        for (FarmItem item : items) {
            itemsByName.computeIfAbsent(item.getName(), k -> new ArrayList<>()).add(item);
        }

        // Within each name group, apply 1-minute time window clustering
        for (String resourceName : itemsByName.keySet()) {
            List<FarmItem> nameGroup = itemsByName.get(resourceName);
            Log.d(TAG, "Processing " + resourceName + " with " + nameGroup.size() + " item(s)");

            // Sort by timestamp (should already be sorted, but ensure it)
            nameGroup.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));

            // Cluster by 5-minute time windows
            List<List<FarmItem>> timeClusters = new ArrayList<>();
            List<Long> clusterStartTimes = new ArrayList<>();
            
            for (FarmItem item : nameGroup) {
                // Try to find an existing cluster where this item fits
                boolean added = false;
                for (int i = 0; i < timeClusters.size(); i++) {
                    List<FarmItem> cluster = timeClusters.get(i);
                    long clusterStartTime = clusterStartTimes.get(i);
                    long timeDifference = item.getTimestamp() - clusterStartTime;
                    
                    // If within 5-minute window, add to this cluster
                    if (timeDifference <= CLUSTERING_WINDOW) {
                        cluster.add(item);
                        added = true;
                        break;
                    }
                }
                
                // If not added to any cluster, create new cluster
                if (!added) {
                    List<FarmItem> newCluster = new ArrayList<>();
                    newCluster.add(item);
                    timeClusters.add(newCluster);
                    clusterStartTimes.add(item.getTimestamp());
                }
            }

            // Convert each time cluster to a NotificationGroup
            for (List<FarmItem> cluster : timeClusters) {
                long earliestReadyTime = cluster.get(0).getTimestamp();
                int quantity = cluster.size();
                
                NotificationGroup group = new NotificationGroup(
                    "resources",           // category
                    resourceName,          // name
                    quantity,              // quantity
                    earliestReadyTime      // earliestReadyTime
                );
                group.groupId = generateGroupId();
                
                groups.add(group);
                Log.d(TAG, "Created cluster: " + quantity + " " + resourceName + 
                      " ready at " + group.earliestReadyTime);
            }
        }

        Log.d(TAG, "Created " + groups.size() + " notification group(s)");
        return groups;
    }

    /**
     * Generates a unique group ID
     */
    private String generateGroupId() {
        return "res_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
    }
}
