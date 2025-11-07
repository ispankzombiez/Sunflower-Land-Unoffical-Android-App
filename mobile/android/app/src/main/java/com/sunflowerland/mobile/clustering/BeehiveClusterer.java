package com.sunflowerland.mobile.clustering;

import android.util.Log;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sunflowerland.mobile.models.FarmItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BeehiveClusterer extends CategoryClusterer {
    private static final String TAG = "BeehiveClusterer";
    
    // 1-minute clustering window for honey fullness alerts
    private static final long CLUSTER_WINDOW_MS = 60_000;
    
    // Swarm alerts are tracked separately by UUID (not clustered)
    // Fullness alerts are clustered by 1-minute window
    
    @Override
    public List<NotificationGroup> cluster(List<FarmItem> items) {
        List<NotificationGroup> groups = new ArrayList<>();
        
        // Separate swarm alerts from fullness alerts
        List<FarmItem> swarmAlerts = new ArrayList<>();
        List<FarmItem> fullnessAlerts = new ArrayList<>();
        
        for (FarmItem item : items) {
            if ("Beehive Swarm".equals(item.getCategory())) {
                swarmAlerts.add(item);
            } else if ("Beehive Full".equals(item.getCategory())) {
                fullnessAlerts.add(item);
            }
        }
        
        // Process swarm alerts - each gets its own notification (will be tracked by state machine)
        for (FarmItem swarmItem : swarmAlerts) {
            NotificationGroup group = createSwarmNotificationGroup(swarmItem);
            groups.add(group);
            Log.d(TAG, "Created swarm notification for " + swarmItem.getName());
        }
        
        // Process fullness alerts - cluster by 1-minute window
        List<NotificationGroup> fullnessGroups = clusterFullnessAlerts(fullnessAlerts);
        groups.addAll(fullnessGroups);
        
        return groups;
    }
    
    private NotificationGroup createSwarmNotificationGroup(FarmItem swarmItem) {
        NotificationGroup group = new NotificationGroup();
        group.category = "beehive";
        group.name = "Swarm"; // This will be used for state tracking key: beehive_{uuid}_swarm_notified
        group.details = swarmItem.getName(); // Display "Beehive 1", "Beehive 2", etc.
        group.quantity = 1;
        group.earliestReadyTime = swarmItem.getTimestamp();
        // Store UUID in details for state machine lookup
        Log.d(TAG, "Swarm alert for: " + swarmItem.getBuildingName());
        return group;
    }
    
    private List<NotificationGroup> clusterFullnessAlerts(List<FarmItem> fullnessItems) {
        List<NotificationGroup> groups = new ArrayList<>();
        
        if (fullnessItems.isEmpty()) {
            return groups;
        }
        
        // Group by time clusters (1-minute window)
        Map<String, List<FarmItem>> clusters = new HashMap<>();
        
        for (FarmItem item : fullnessItems) {
            String clusterId = generateClusterIdForTimestamp(item.getTimestamp());
            clusters.computeIfAbsent(clusterId, k -> new ArrayList<>()).add(item);
        }
        
        // Create notification groups from clusters
        for (List<FarmItem> cluster : clusters.values()) {
            NotificationGroup group = createFullnessNotificationGroup(cluster);
            groups.add(group);
            Log.d(TAG, "Created fullness cluster with " + cluster.size() + " beehive(s)");
        }
        
        return groups;
    }
    
    private String generateClusterIdForTimestamp(long timestamp) {
        // Group by 1-minute window
        long clusterKey = (timestamp / CLUSTER_WINDOW_MS) * CLUSTER_WINDOW_MS;
        return String.valueOf(clusterKey);
    }
    
    private NotificationGroup createFullnessNotificationGroup(List<FarmItem> clusterItems) {
        NotificationGroup group = new NotificationGroup();
        group.category = "beehive";
        group.name = "Full";
        
        // Build display names: "Beehive 1, Beehive 2" etc.
        StringBuilder displayNames = new StringBuilder();
        long latestReadyTime = 0;
        
        for (int i = 0; i < clusterItems.size(); i++) {
            FarmItem item = clusterItems.get(i);
            if (i > 0) displayNames.append(", ");
            displayNames.append(item.getName()); // e.g., "Beehive 1"
            
            if (item.getTimestamp() > latestReadyTime) {
                latestReadyTime = item.getTimestamp();
            }
        }
        
        group.details = displayNames.toString();
        group.quantity = clusterItems.size();
        group.earliestReadyTime = latestReadyTime; // Use latest (furthest) fullness time
        
        return group;
    }
}
