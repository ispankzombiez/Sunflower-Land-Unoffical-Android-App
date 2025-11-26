package com.sfl.browser.clustering;

import com.sfl.browser.models.FarmItem;
import java.util.List;

/**
 * Abstract base class for category-specific clustering strategies
 * Each category can have different grouping rules
 */
public abstract class CategoryClusterer {
    
    /**
     * Cluster FarmItems according to category-specific rules
     * @param items List of FarmItems from the category extractor
     * @return List of NotificationGroups ready for notification
     */
    public abstract List<NotificationGroup> cluster(List<FarmItem> items);
    
    /**
     * Generate a unique cluster ID for tracking in SharedPreferences
     * Override to customize tracking key generation
     */
    public String generateClusterId(NotificationGroup group) {
        long timeBucket = (group.earliestReadyTime / 60000) * 60000;
        return group.category + "_" + group.name + "_" + timeBucket;
    }
}
