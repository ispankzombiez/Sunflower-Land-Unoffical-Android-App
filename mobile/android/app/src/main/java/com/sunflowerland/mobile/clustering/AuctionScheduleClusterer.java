package com.sunflowerland.mobile.clustering;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import androidx.preference.PreferenceManager;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sunflowerland.mobile.models.FarmItem;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Clustering strategy for auction schedule items
 * 
 * Rules:
 * 1. Each auction gets its OWN individual notification (no grouping)
 * 2. OPTIMIZATION: Only schedule the NEXT upcoming auction (earliest startAt)
 * 3. Store lastScheduledAuctionStart timestamp for optimization
 * 4. Skip processing if current time < lastScheduledAuctionStart
 * 5. Only reschedule when lastScheduledAuctionStart time has passed
 * 
 * This optimization prevents unnecessary processing of all auctions on every API call
 */
public class AuctionScheduleClusterer extends CategoryClusterer {
    private static final String TAG = "AuctionScheduleClusterer";
    private Context context;

    public AuctionScheduleClusterer(Context context) {
        this.context = context;
    }

    @Override
    public List<NotificationGroup> cluster(List<FarmItem> items) {
        Log.d(TAG, "Clustering " + items.size() + " auction items (only next auction)");
        List<NotificationGroup> groups = new ArrayList<>();

        if (items.isEmpty()) {
            Log.d(TAG, "No auction items to cluster");
            return groups;
        }

        // Find ONLY the next upcoming auction (earliest startAt)
        FarmItem nextAuction = findNextAuction(items);
        if (nextAuction == null) {
            Log.d(TAG, "No next auction found");
            return groups;
        }

        long nextAuctionStartAt = nextAuction.getTimestamp();
        String nextAuctionId = nextAuction.getId();
        long currentTime = System.currentTimeMillis();

        // Get the currently scheduled auction (if any)
        String lastScheduledAuctionId = getLastScheduledAuctionId();
        long lastScheduledStartAt = getLastScheduledAuctionStart();

        Log.d(TAG, "Next auction ID: " + nextAuctionId + ", startAt: " + formatTimestamp(nextAuctionStartAt));
        Log.d(TAG, "Last scheduled ID: " + lastScheduledAuctionId + ", startAt: " + formatTimestamp(lastScheduledStartAt));

        // Check if same auction is already scheduled
        if (nextAuctionId.equals(lastScheduledAuctionId)) {
            // Same auction - only schedule if its time has passed
            if (currentTime < nextAuctionStartAt) {
                Log.d(TAG, "Same auction already scheduled, time hasn't passed yet - skipping");
                return groups;
            } else {
                Log.d(TAG, "Same auction's time has passed - will reschedule");
            }
        } else {
            // Different auction (or first time scheduling)
            Log.d(TAG, "Different auction than last scheduled - will schedule this one");
        }

        // Create notification group for the next auction
        NotificationGroup group = createNotificationGroup(nextAuction);
        groups.add(group);

        // Store the scheduled auction info
        storeLastScheduledAuctionId(nextAuctionId);
        storeLastScheduledAuctionStart(nextAuctionStartAt);

        Log.d(TAG, "Created notification group for next auction: " + group.name + 
              " starting at " + formatTimestamp(nextAuctionStartAt));

        return groups;
    }

    /**
     * Find the next upcoming auction (earliest startAt timestamp)
     */
    private FarmItem findNextAuction(List<FarmItem> items) {
        FarmItem nextAuction = null;
        long earliestStartAt = Long.MAX_VALUE;

        for (FarmItem item : items) {
            long startAt = item.getTimestamp();
            if (startAt < earliestStartAt) {
                earliestStartAt = startAt;
                nextAuction = item;
            }
        }

        return nextAuction;
    }

    /**
     * Create a single notification group for an auction
     * Extracts metadata from the details field (set by AuctionScheduleExtractor)
     * 
     * Details format: startAt|endAt|sfl|ingredientsJson
     */
    private NotificationGroup createNotificationGroup(FarmItem auction) {
        long startAt = auction.getTimestamp();
        long endAt = startAt;  // Default value
        long sfl = 0;
        String ingredientsJson = "";

        // Parse details field if present
        if (auction.getDetails() != null && !auction.getDetails().isEmpty()) {
            String[] parts = auction.getDetails().split("\\|", 4);
            if (parts.length >= 2) {
                try {
                    startAt = Long.parseLong(parts[0]);
                    endAt = Long.parseLong(parts[1]);
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Error parsing startAt/endAt: " + e.getMessage());
                }
            }
            if (parts.length >= 3) {
                try {
                    sfl = Long.parseLong(parts[2]);
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Error parsing sfl: " + e.getMessage());
                }
            }
            if (parts.length >= 4) {
                ingredientsJson = parts[3];
            }
        }

        NotificationGroup group = new NotificationGroup();
        group.category = "auction";
        group.name = formatAuctionDisplayName(auction.getName(), auction.getDetails());
        group.quantity = 1;  // Each auction is one item
        group.earliestReadyTime = startAt;

        // Store auction metadata in details for NotificationReceiver to use
        // Format: endAt|sfl|ingredientsJson
        group.details = endAt + "|" + sfl + "|" + ingredientsJson;

        // Generate unique groupId using auction ID (from FarmItem.id)
        group.groupId = generateClusterId("auction", auction.getId(), startAt);

        Log.d(TAG, "Created auction notification group: " + group.name + 
              " (sfl=" + sfl + ", startAt=" + formatTimestamp(startAt) + ", id=" + group.groupId + ")");

        return group;
    }

    /**
     * Generate unique cluster ID for this auction
     * Uses auction ID to ensure each auction has unique ID
     */
    private String generateClusterId(String category, String auctionId, long startAt) {
        return category + "_" + auctionId;
    }

    /**
     * Get the ID of the currently scheduled auction
     */
    private String getLastScheduledAuctionId() {
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            return prefs.getString("lastScheduledAuctionId", "");
        } catch (Exception e) {
            Log.w(TAG, "Error reading lastScheduledAuctionId: " + e.getMessage());
            return "";
        }
    }

    /**
     * Store the ID of the currently scheduled auction
     */
    private void storeLastScheduledAuctionId(String auctionId) {
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            prefs.edit().putString("lastScheduledAuctionId", auctionId).apply();
            Log.d(TAG, "Stored lastScheduledAuctionId: " + auctionId);
        } catch (Exception e) {
            Log.w(TAG, "Error storing lastScheduledAuctionId: " + e.getMessage());
        }
    }

    /**
     * Get the stored timestamp of the last scheduled auction's startAt
     */
    private long getLastScheduledAuctionStart() {
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            return prefs.getLong("lastScheduledAuctionStart", -1);
        } catch (Exception e) {
            Log.w(TAG, "Error reading lastScheduledAuctionStart: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Store the timestamp of the currently scheduled auction's startAt
     * Used for optimization to skip re-clustering
     */
    private void storeLastScheduledAuctionStart(long startAt) {
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            prefs.edit().putLong("lastScheduledAuctionStart", startAt).apply();
            Log.d(TAG, "Stored lastScheduledAuctionStart: " + formatTimestamp(startAt));
        } catch (Exception e) {
            Log.w(TAG, "Error storing lastScheduledAuctionStart: " + e.getMessage());
        }
    }

    /**
     * Format timestamp for logging
     */
    private String formatTimestamp(long timestamp) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        } catch (Exception e) {
            return String.valueOf(timestamp);
        }
    }

    /**
     * Get the currency display name for an auction
     * Determines currency type from sfl and ingredients
     * Returns: "$Flower", "Gem", or "Pet Cookie"
     * 
     * Details format: "startAt|endAt|sfl|ingredientsJson"
     */
    private String getAuctionCurrencyName(String details) {
        if (details == null || details.isEmpty()) {
            return "Gem";  // Default fallback
        }
        
        try {
            // details format: "startAt|endAt|sfl|ingredientsJson"
            String[] parts = details.split("\\|", 4);
            if (parts.length < 3) {
                return "Gem";  // Default fallback
            }
            
            long sfl = Long.parseLong(parts[2]);
            
            // If sfl > 0, use $Flower
            if (sfl > 0) {
                return "$Flower";
            }
            
            // Otherwise, parse ingredients to get first key
            if (parts.length > 3 && !parts[3].isEmpty()) {
                String ingredientsJson = parts[3];
                // Parse first key from JSON object: {"Gem":X} or {"Pet Cookie":X}
                if (ingredientsJson.contains("\"Gem\"")) {
                    return "Gem";
                } else if (ingredientsJson.contains("\"Pet Cookie\"")) {
                    return "Pet Cookie";
                }
            }
            
            // Default fallback
            return "Gem";
            
        } catch (Exception e) {
            Log.w(TAG, "Error determining auction currency: " + e.getMessage());
            return "Gem";  // Default fallback
        }
    }

    /**
     * Format auction display name: "{itemName} {currency} Auction"
     * Example: "Coin Aura $Flower Auction"
     */
    private String formatAuctionDisplayName(String itemName, String details) {
        String currencyName = getAuctionCurrencyName(details);
        return itemName + " " + currencyName + " Auction";
    }
}
