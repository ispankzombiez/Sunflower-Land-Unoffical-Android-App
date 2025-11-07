package com.sunflowerland.mobile.clustering;

import android.util.Log;
import com.sunflowerland.mobile.models.FarmItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Clustering strategy for composter items
 * 
 * Rules:
 * 1. Group by composter type (Compost Bin, Turbo Composter, Premium Composter)
 * 2. Items ready within 1-minute window = same group
 * 3. Each unique (type, timeWindow) combination = one notification
 * 4. Total quantity = sum of all items in that group
 * 5. Ready time = earliest timestamp in the group
 * 
 * This allows multiple notifications for same composter type if productions finish at different times.
 */
public class ComposterClusterer extends CategoryClusterer {
    private static final String TAG = "ComposterClusterer";
    private static final long CLUSTERING_WINDOW = 60_000; // 1 minute in milliseconds

    @Override
    public List<NotificationGroup> cluster(List<FarmItem> items) {
        Log.d(TAG, "Clustering " + items.size() + " composter items");
        List<NotificationGroup> groups = new ArrayList<>();

        if (items.isEmpty()) {
            return groups;
        }

        // Map: composterType -> list of items from that composter type
        Map<String, List<FarmItem>> itemsByType = new HashMap<>();

        for (FarmItem item : items) {
            String type = item.getName();  // Name stores the composter type (Compost Bin, Turbo Composter, Premium Composter)
            itemsByType.computeIfAbsent(type, k -> new ArrayList<>()).add(item);
        }

        // For each composter type, create time-based clusters
        for (String composterType : itemsByType.keySet()) {
            List<FarmItem> typeItems = itemsByType.get(composterType);

            // Sort by timestamp
            typeItems.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));

            // Group by time window
            List<List<FarmItem>> timeClusters = new ArrayList<>();
            List<FarmItem> currentCluster = new ArrayList<>();
            long clusterStartTime = 0;

            for (FarmItem item : typeItems) {
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
                StringBuilder detailsBuilder = new StringBuilder();

                for (int i = 0; i < cluster.size(); i++) {
                    FarmItem item = cluster.get(i);
                    totalQuantity += item.getAmount();
                    earliestTime = Math.min(earliestTime, item.getTimestamp());
                    
                    // Append item details if available
                    if (item.getDetails() != null && !item.getDetails().isEmpty()) {
                        detailsBuilder.append(item.getDetails());
                        if (i < cluster.size() - 1) {
                            detailsBuilder.append(", ");
                        }
                    }
                }

                NotificationGroup group = new NotificationGroup(
                    "composters",
                    composterType,
                    totalQuantity,
                    earliestTime
                );

                group.groupId = generateClusterId(group);
                group.details = detailsBuilder.toString();  // Store the produced items
                groups.add(group);

                Log.d(TAG, "  Group: " + composterType + " with " + totalQuantity + " total items" +
                      " ready at " + earliestTime + " (id=" + group.groupId + ")");
            }
        }

        Log.d(TAG, "Created " + groups.size() + " notification group(s)");
        return groups;
    }
}
