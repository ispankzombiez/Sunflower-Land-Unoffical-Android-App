package com.sfl.browser.clustering;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sfl.browser.models.FarmItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * BeehiveClusterer - Handles both swarm alerts and fullness alerts independently
 * 
 * Swarm Logic:
 * - Tracks which beehives have already been notified about swarms (persisted in SharedPreferences)
 * - Only creates notification when swarm changes from false -> true (AND not previously notified)
 * - Clears the "notified" flag when: a) beehive reaches full, OR b) user collects honey
 * - Multiple swarms are grouped into single notification: "Multiple swarms incoming!"
 * 
 * Fullness Logic:
 * - Completely independent from swarm logic
 * - Creates individual notification for each beehive
 * - Does NOT check swarm status
 * - Each beehive gets its own notification with exact fullness time
 */
public class BeehiveClusterer extends CategoryClusterer {
    private static final String TAG = "BeehiveClusterer";
    private static final String PREFS_NAME = "beehive_swarm_tracking";
    private static final String PREFS_KEY_NOTIFIED = "notified_swarms"; // Set of UUIDs already notified
    private static final String PREFS_KEY_COLLECTED = "collected_beehives"; // For tracking collected honey
    
    private Context context;
    
    public BeehiveClusterer() {
        this.context = null;
    }
    
    /**
     * Override cluster to accept Context for preference access
     * Called from ClustererFactory which has context
     */
    public void setContext(Context context) {
        this.context = context;
    }
    
    @Override
    public List<NotificationGroup> cluster(List<FarmItem> items) {
        List<NotificationGroup> groups = new ArrayList<>();
        
        // Separate swarm alerts from fullness alerts
        List<FarmItem> swarmAlerts = new ArrayList<>();
        List<FarmItem> fullnessAlerts = new ArrayList<>();
        Map<String, String> displayNumberByUuid = new HashMap<>();
        
        for (FarmItem item : items) {
            if ("Beehive Swarm".equals(item.getCategory())) {
                swarmAlerts.add(item);
                displayNumberByUuid.put(item.getBuildingName(), item.getName()); // Store "Beehive 1", "Beehive 2", etc.
            } else if ("Beehive Full".equals(item.getCategory())) {
                fullnessAlerts.add(item);
                displayNumberByUuid.put(item.getBuildingName(), item.getName());
            }
        }
        
        // Process swarms independently
        if (!swarmAlerts.isEmpty()) {
            List<NotificationGroup> swarmGroups = processSwarmAlerts(swarmAlerts);
            groups.addAll(swarmGroups);
        }
        
        // Process fullness independently
        if (!fullnessAlerts.isEmpty()) {
            List<NotificationGroup> fullnessGroups = processFullnessAlerts(fullnessAlerts);
            groups.addAll(fullnessGroups);
        }
        
        // If beehives reached full, reset their swarm notification status
        if (!fullnessAlerts.isEmpty()) {
            resetSwarmNotificationForCollectedBeehives(fullnessAlerts);
        }
        
        Log.d(TAG, "Clusterer created " + groups.size() + " total notification group(s): " + 
              swarmAlerts.size() + " swarm(s), " + fullnessAlerts.size() + " fullness");
        
        return groups;
    }
    
    /**
     * Process swarm alerts - only notify if not previously notified about this swarm
     * Multiple swarms are grouped into single notification
     */
    private List<NotificationGroup> processSwarmAlerts(List<FarmItem> swarmItems) {
        List<NotificationGroup> groups = new ArrayList<>();
        
        if (context == null) {
            Log.w(TAG, "Context is null, cannot track swarm notifications (notification will still fire but not persisted)");
            // Still create notification even without context
            return createSwarmNotificationGroups(swarmItems, new HashSet<>());
        }
        
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> notifiedUuids = new HashSet<>(prefs.getStringSet(PREFS_KEY_NOTIFIED, new HashSet<>()));
        
        // Filter to only swarms NOT previously notified
        List<FarmItem> newSwarms = new ArrayList<>();
        for (FarmItem swarm : swarmItems) {
            String uuid = swarm.getBuildingName();
            if (!notifiedUuids.contains(uuid)) {
                newSwarms.add(swarm);
                notifiedUuids.add(uuid); // Mark as notified
                Log.d(TAG, "NEW SWARM detected for uuid: " + uuid + " (" + swarm.getName() + ")");
            } else {
                Log.d(TAG, "Swarm already notified for uuid: " + uuid + " (" + swarm.getName() + ") - skipping");
            }
        }
        
        // Save updated notified set
        if (!newSwarms.isEmpty()) {
            prefs.edit().putStringSet(PREFS_KEY_NOTIFIED, notifiedUuids).apply();
            Log.d(TAG, "Saved " + notifiedUuids.size() + " notified swarm uuid(s)");
        }
        
        // Create notifications for new swarms only
        if (!newSwarms.isEmpty()) {
            groups.addAll(createSwarmNotificationGroups(newSwarms, notifiedUuids));
        } else {
            Log.d(TAG, "No new swarms to notify (all have been previously notified)");
        }
        
        return groups;
    }
    
    /**
     * Create swarm notification(s) - groups all swarms into one or multiple notifications
     */
    private List<NotificationGroup> createSwarmNotificationGroups(List<FarmItem> swarmItems, Set<String> notifiedUuids) {
        List<NotificationGroup> groups = new ArrayList<>();
        
        if (swarmItems.isEmpty()) {
            return groups;
        }
        
        if (swarmItems.size() == 1) {
            // Single swarm
            FarmItem swarm = swarmItems.get(0);
            NotificationGroup group = new NotificationGroup();
            group.category = "beehive";
            group.name = "Swarm"; // Will display as "Swarm" in logs
            group.details = "Swarm incoming! " + swarm.getName(); // Main display
            group.quantity = 1;
            group.earliestReadyTime = swarm.getTimestamp(); // Current time
            group.groupId = "beehive_swarm_" + swarm.getBuildingName(); // Unique per hive
            groups.add(group);
            Log.d(TAG, "Created single swarm notification for " + swarm.getName() + " (groupId: " + group.groupId + ")");
        } else {
            // Multiple swarms - group into single notification
            NotificationGroup group = new NotificationGroup();
            group.category = "beehive";
            group.name = "Swarms"; // Will display as "Swarms" in logs
            
            // Build list: "Beehive 1, Beehive 3, Beehive 5"
            List<String> hiveNames = new ArrayList<>();
            StringBuilder uuidList = new StringBuilder();
            for (FarmItem swarm : swarmItems) {
                hiveNames.add(swarm.getName());
                if (uuidList.length() > 0) uuidList.append("|");
                uuidList.append(swarm.getBuildingName());
            }
            group.details = "Multiple swarms incoming! " + String.join(", ", hiveNames); // Main display
            group.quantity = swarmItems.size();
            group.earliestReadyTime = System.currentTimeMillis();
            group.groupId = "beehive_swarms_grouped_" + uuidList.toString(); // Include all UUIDs
            groups.add(group);
            Log.d(TAG, "Created grouped swarm notification for " + swarmItems.size() + " beehive(s): " + group.details);
        }
        
        return groups;
    }
    
    /**
     * Process fullness alerts - each beehive gets individual notification
     * Completely independent from swarm logic
     */
    private List<NotificationGroup> processFullnessAlerts(List<FarmItem> fullnessItems) {
        List<NotificationGroup> groups = new ArrayList<>();
        
        for (FarmItem fullness : fullnessItems) {
            NotificationGroup group = new NotificationGroup();
            group.category = "beehive";
            group.name = "Full"; // Will appear in logs as "Full"
            group.details = fullness.getName() + " full - ready to collect"; // Main text: "Beehive 1 full - ready to collect"
            group.quantity = 1;
            group.earliestReadyTime = fullness.getTimestamp();
            
            // Create unique groupId for each beehive fullness
            group.groupId = "beehive_" + fullness.getBuildingName() + "_full";
            
            groups.add(group);
            Log.d(TAG, "Created individual fullness notification for " + fullness.getName() + 
                  " at " + fullness.getTimestamp() + " (groupId: " + group.groupId + ")");
        }
        
        return groups;
    }
    
    /**
     * When a beehive reaches "full" status, reset its swarm notification flag
     * This allows a new swarm to be detected and notified when it occurs next time
     */
    private void resetSwarmNotificationForCollectedBeehives(List<FarmItem> fullnessItems) {
        if (context == null || fullnessItems.isEmpty()) {
            return;
        }
        
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> notifiedUuids = new HashSet<>(prefs.getStringSet(PREFS_KEY_NOTIFIED, new HashSet<>()));
        boolean changed = false;
        
        for (FarmItem fullness : fullnessItems) {
            String uuid = fullness.getBuildingName();
            if (notifiedUuids.contains(uuid)) {
                notifiedUuids.remove(uuid);
                changed = true;
                Log.d(TAG, "RESET swarm notification flag for uuid: " + uuid + " (" + fullness.getName() + 
                      ") - because beehive reached FULL status. Next swarm will be notified.");
            }
        }
        
        if (changed) {
            prefs.edit().putStringSet(PREFS_KEY_NOTIFIED, notifiedUuids).apply();
            Log.d(TAG, "Updated notified swarms set (removed " + fullnessItems.size() + " beehive(s))");
        }
    }
}
