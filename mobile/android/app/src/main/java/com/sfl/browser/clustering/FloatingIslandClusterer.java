package com.sfl.browser.clustering;

import android.content.Context;
import android.util.Log;
import com.sfl.browser.models.FarmItem;
import java.util.ArrayList;
import java.util.List;

/**
 * Clusters floating island notifications (schedule and shop changes)
 * - Schedule: Creates one group per schedule entry (startAt is the notification time)
 * - Shop changes: Creates one group per shop change notification
 */
public class FloatingIslandClusterer extends CategoryClusterer {
    private static final String TAG = "FloatingIslandClusterer";

    @Override
    public List<NotificationGroup> cluster(List<FarmItem> items) {
        Log.d(TAG, "Clustering " + items.size() + " floating island item(s)");
        List<NotificationGroup> groups = new ArrayList<>();

        if (items == null || items.isEmpty()) {
            Log.d(TAG, "No items to cluster");
            return groups;
        }

        for (FarmItem item : items) {
            try {
                NotificationGroup group = new NotificationGroup();
                group.category = "floating_island";
                group.name = item.getName();
                group.quantity = item.getAmount();
                group.groupId = "floating_island_" + item.getTimestamp() + "_" + Math.random();
                group.earliestReadyTime = item.getTimestamp();
                
                // Store details for later use (either startAt|endAt for schedule, or shop item list)
                group.details = item.getDetails() != null ? item.getDetails() : "";
                
                groups.add(group);
                Log.d(TAG, "Created notification group: " + group.name + 
                      " at " + formatTimestamp(item.getTimestamp()));
            } catch (Exception e) {
                Log.w(TAG, "Error clustering item: " + e.getMessage());
            }
        }

        Log.d(TAG, "Created " + groups.size() + " notification group(s)");
        return groups;
    }

    private String formatTimestamp(long timestamp) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MM/dd HH:mm:ss");
            return sdf.format(new java.util.Date(timestamp));
        } catch (Exception e) {
            return String.valueOf(timestamp);
        }
    }
}
