package com.sfl.browser.clustering;

import android.util.Log;
import com.sfl.browser.models.FarmItem;
import java.util.ArrayList;
import java.util.List;

/**
 * Default clustering strategy
 * 
 * Rules:
 * 1. Group by name only (no time-based clustering)
 * 2. Each unique name = one notification
 * 3. Total quantity = sum of all items with that name
 * 4. Ready time = earliest item's timestamp
 * 
 * Used for categories that don't have special clustering rules yet
 */
public class DefaultClusterer extends CategoryClusterer {
    private static final String TAG = "DefaultClusterer";
    
    @Override
    public List<NotificationGroup> cluster(List<FarmItem> items) {
        Log.d(TAG, "Clustering " + items.size() + " items (default strategy)");
        List<NotificationGroup> groups = new ArrayList<>();
        
        if (items.isEmpty()) {
            return groups;
        }
        
        // Group by name and aggregate
        java.util.Map<String, FarmItem> aggregated = new java.util.HashMap<>();
        java.util.Map<String, Integer> counts = new java.util.HashMap<>();
        
        for (FarmItem item : items) {
            String key = item.getName();
            
            if (!aggregated.containsKey(key)) {
                aggregated.put(key, item);
                counts.put(key, 0);
            }
            
            counts.put(key, counts.get(key) + item.getAmount());
        }
        
        // Create one notification group per unique name
        for (String name : aggregated.keySet()) {
            FarmItem item = aggregated.get(name);
            int totalQuantity = counts.get(name);
            
            NotificationGroup group = new NotificationGroup(
                item.getCategory(),
                name,
                totalQuantity,
                item.getTimestamp()
            );
            
            group.groupId = generateClusterId(group);
            groups.add(group);
            
            Log.d(TAG, "  Group: " + totalQuantity + " " + name + 
                  " ready at " + item.getTimestamp() + " (id=" + group.groupId + ")");
        }
        
        Log.d(TAG, "Created " + groups.size() + " notification group(s)");
        return groups;
    }
}
