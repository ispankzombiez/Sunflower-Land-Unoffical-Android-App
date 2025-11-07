package com.sunflowerland.mobile.clustering;

import android.util.Log;
import com.sunflowerland.mobile.models.FarmItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SunstoneClusterer extends CategoryClusterer {
    private static final String TAG = "SunstoneClusterer";
    
    // 1-minute clustering window for sunstone ready times
    private static final long CLUSTER_WINDOW_MS = 60_000;
    
    @Override
    public List<NotificationGroup> cluster(List<FarmItem> items) {
        List<NotificationGroup> groups = new ArrayList<>();
        
        if (items.isEmpty()) {
            return groups;
        }
        
        // Group by 1-minute time windows
        Map<String, List<FarmItem>> clusters = new HashMap<>();
        
        for (FarmItem item : items) {
            String clusterId = generateClusterIdForTimestamp(item.getTimestamp());
            clusters.computeIfAbsent(clusterId, k -> new ArrayList<>()).add(item);
        }
        
        // Create notification groups from clusters
        for (List<FarmItem> cluster : clusters.values()) {
            NotificationGroup group = createNotificationGroup(cluster);
            groups.add(group);
            Log.d(TAG, "Created cluster with " + cluster.size() + " sunstone(s)");
        }
        
        return groups;
    }
    
    private NotificationGroup createNotificationGroup(List<FarmItem> cluster) {
        NotificationGroup group = new NotificationGroup();
        group.category = "sunstones";
        group.name = "Sunstone";
        
        // Calculate quantity and latest ready time
        long totalCount = cluster.size();
        long latestReadyTime = 0;
        
        for (FarmItem item : cluster) {
            if (item.getTimestamp() > latestReadyTime) {
                latestReadyTime = item.getTimestamp();
            }
        }
        
        group.quantity = (int) totalCount;
        group.earliestReadyTime = latestReadyTime;
        
        if (totalCount == 1) {
            group.details = "1 sunstone ready";
        } else {
            group.details = totalCount + " sunstones ready";
        }
        
        return group;
    }
    
    private String generateClusterIdForTimestamp(long timestamp) {
        // Group by 1-minute window
        long clusterKey = (timestamp / CLUSTER_WINDOW_MS) * CLUSTER_WINDOW_MS;
        return String.valueOf(clusterKey);
    }
}
