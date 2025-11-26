package com.sfl.browser.clustering;

import android.util.Log;
import com.sfl.browser.models.FarmItem;
import java.util.ArrayList;
import java.util.List;

public class SkillClusterer extends CategoryClusterer {
    private static final String TAG = "SkillClusterer";

    @Override
    public List<NotificationGroup> cluster(List<FarmItem> items) {
        Log.d(TAG, "Clustering " + items.size() + " skill items");
        List<NotificationGroup> groups = new ArrayList<>();

        // Create one notification group per skill (no grouping/clustering)
        for (FarmItem item : items) {
            NotificationGroup group = new NotificationGroup();
            group.category = "skill_cooldown";
            group.name = item.getName();
            group.quantity = 1;
            group.earliestReadyTime = item.getTimestamp();
            group.groupId = item.getName(); // Use skill name as unique ID for state tracking
            
            groups.add(group);
            Log.d(TAG, "Created notification for skill: " + item.getName() + " ready at " + item.getTimestamp());
        }

        return groups;
    }
}
