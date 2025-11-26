package com.sfl.browser.clustering;

import android.util.Log;
import com.sfl.browser.models.FarmItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CropMachineClusterer extends CategoryClusterer {
    private static final String TAG = "CropMachineClusterer";
    
    // 1-minute clustering window for crop machine items
    private static final long CLUSTER_WINDOW_MS = 60_000;
    
    @Override
    public List<NotificationGroup> cluster(List<FarmItem> items) {
        List<NotificationGroup> groups = new ArrayList<>();
        
        if (items.isEmpty()) {
            return groups;
        }
        
        // Group by crop name, then by 1-minute time windows
        Map<String, Map<String, List<FarmItem>>> cropClusters = new HashMap<>();
        
        for (FarmItem item : items) {
            String cropName = item.getName();
            String timeClusterId = generateClusterIdForTimestamp(item.getTimestamp());
            
            cropClusters.computeIfAbsent(cropName, k -> new HashMap<>())
                    .computeIfAbsent(timeClusterId, k -> new ArrayList<>())
                    .add(item);
        }
        
        // Create notification groups from clusters
        for (Map.Entry<String, Map<String, List<FarmItem>>> cropEntry : cropClusters.entrySet()) {
            String cropName = cropEntry.getKey();
            
            for (List<FarmItem> cluster : cropEntry.getValue().values()) {
                NotificationGroup group = createNotificationGroup(cropName, cluster);
                groups.add(group);
                Log.d(TAG, "Created cluster for " + cropName + " with " + cluster.size() + " item(s)");
            }
        }
        
        return groups;
    }
    
    private NotificationGroup createNotificationGroup(String cropName, List<FarmItem> cluster) {
        NotificationGroup group = new NotificationGroup();
        group.category = "cropMachine";
        group.name = cropName;
        
        // Calculate total seeds and latest ready time
        long totalSeeds = 0;
        long latestReadyTime = 0;
        
        for (FarmItem item : cluster) {
            totalSeeds += item.getAmount();
            if (item.getTimestamp() > latestReadyTime) {
                latestReadyTime = item.getTimestamp();
            }
        }
        
        group.quantity = (int) totalSeeds;
        group.earliestReadyTime = latestReadyTime;
        group.details = cluster.size() + " batch(es) of " + cropName;
        
        return group;
    }
    
    private String generateClusterIdForTimestamp(long timestamp) {
        // Group by 1-minute window
        long clusterKey = (timestamp / CLUSTER_WINDOW_MS) * CLUSTER_WINDOW_MS;
        return String.valueOf(clusterKey);
    }
}
