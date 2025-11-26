package com.sfl.browser.clustering;

import android.util.Log;
import com.sfl.browser.models.FarmItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Clusters animals (Chicken, Cow, Sheep, etc.) by type with a 5-minute time window.
 * Handles both production notifications ("animal awake at") and love notifications ("animal love at").
 * 
 * Groups animals of the same type that are ready within 5 minutes of each other.
 * Love notifications are kept separate from production notifications.
 * Each group is assigned a unique groupId and represents multiple animals ready around the same time.
 * 
 * Example: 3 chickens ready at 14:32:15, 14:32:30, 14:36:00
 * Result: First 2 grouped as "2 Chicken" (within 5 min), 3rd as separate "1 Chicken" (outside window)
 */
public class AnimalClusterer extends CategoryClusterer {
    private static final String TAG = "AnimalClusterer";
    private static final long CLUSTERING_WINDOW = 5 * 60 * 1000; // 5 minutes = 300,000 ms

    @Override
    public List<NotificationGroup> cluster(List<FarmItem> items) {
        Log.d(TAG, "Clustering " + items.size() + " animal item(s) with 5-minute window");
        List<NotificationGroup> groups = new ArrayList<>();

        if (items == null || items.isEmpty()) {
            Log.d(TAG, "No items to cluster");
            return groups;
        }

        // Separate production notifications from love notifications
        List<FarmItem> productionItems = new ArrayList<>();
        List<FarmItem> loveItems = new ArrayList<>();
        
        for (FarmItem item : items) {
            if ("animals_love".equals(item.getCategory())) {
                loveItems.add(item);
            } else if ("animals".equals(item.getCategory())) {
                productionItems.add(item);
            }
        }
        
        // Cluster production items
        if (!productionItems.isEmpty()) {
            groups.addAll(clusterItemsByTypeAndTime(productionItems, "animal awake at"));
        }
        
        // Cluster love items separately
        if (!loveItems.isEmpty()) {
            groups.addAll(clusterItemsByTypeAndTime(loveItems, "animal love at"));
        }

        Log.d(TAG, "Created " + groups.size() + " notification group(s)");
        return groups;
    }

    /**
     * Helper method to cluster items by animal type and time
     */
    private List<NotificationGroup> clusterItemsByTypeAndTime(List<FarmItem> items, String notificationId) {
        List<NotificationGroup> groups = new ArrayList<>();
        
        // Group items by animal type first
        Map<String, List<FarmItem>> itemsByType = new HashMap<>();
        for (FarmItem item : items) {
            itemsByType.computeIfAbsent(item.getName(), k -> new ArrayList<>()).add(item);
        }

        // Within each type group, apply 5-minute time window clustering
        for (String animalType : itemsByType.keySet()) {
            List<FarmItem> typeGroup = itemsByType.get(animalType);
            Log.d(TAG, "Processing " + animalType + " with " + typeGroup.size() + " item(s) for " + notificationId);

            // Sort by timestamp (should already be sorted, but ensure it)
            typeGroup.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));

            // Cluster by 5-minute time windows
            List<List<FarmItem>> timeClusters = new ArrayList<>();
            List<Long> clusterStartTimes = new ArrayList<>();
            
            for (FarmItem item : typeGroup) {
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
                    "animals",         // category
                    animalType,        // name
                    quantity,          // quantity
                    earliestReadyTime   // earliestReadyTime
                );
                group.groupId = notificationId;
                group.groupId = notificationId + "_" + generateGroupId();
                
                groups.add(group);
                Log.d(TAG, "Created cluster: " + quantity + " " + animalType + 
                      " ready at " + group.earliestReadyTime + " (" + notificationId + ")");
            }
        }
        
        return groups;
    }

    /**
     * Generates a unique group ID
     */
    private String generateGroupId() {
        return "ani_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 10000);
    }
}
