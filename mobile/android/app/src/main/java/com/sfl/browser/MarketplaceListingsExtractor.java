package com.sfl.browser;

import android.content.Context;
import android.util.Log;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Detects marketplace listings that have been sold since last poll
 * 
 * System:
 * 1. Load previous snapshot of listings (marketplace_listings_snapshot.json)
 * 2. Compare current listings against snapshot by unique ID
 * 3. Find listings that now have fulfilledAt but didn't before = JUST SOLD
 * 4. Extract sale details (amount, item name, SFL price)
 * 5. Save current state as new snapshot for next poll
 */
public class MarketplaceListingsExtractor {
    private static final String TAG = "MarketplaceListingsExtractor";
    private static final String SNAPSHOT_FILE = "marketplace_listings_snapshot.json";
    
    private Context context;
    
    public MarketplaceListingsExtractor(Context context) {
        this.context = context;
    }
    
    /**
     * Main entry point: detect newly-sold listings
     */
    public List<SoldListing> extractSoldListings(JsonObject farmObject) {
        List<SoldListing> soldListings = new ArrayList<>();
        
        try {
            // Get current listings
            JsonObject currentListings = getCurrentListings(farmObject);
            if (currentListings == null || currentListings.entrySet().isEmpty()) {
                Log.d(TAG, "No current listings found");
                return soldListings;
            }
            
            // Load previous snapshot
            Map<String, Boolean> previousSnapshot = loadSnapshot();
            
            // Compare and detect newly-sold listings
            for (String listingId : currentListings.keySet()) {
                try {
                    JsonObject listing = currentListings.getAsJsonObject(listingId);
                    
                    boolean currentlyFulfilled = listing.has("fulfilledAt") && !listing.get("fulfilledAt").isJsonNull();
                    boolean previouslyFulfilled = previousSnapshot.getOrDefault(listingId, false);
                    
                    // Detect state change: was NOT fulfilled before, NOW is fulfilled
                    if (currentlyFulfilled && !previouslyFulfilled) {
                        Log.d(TAG, "🎉 NEWLY SOLD listing detected: " + listingId);
                        SoldListing sold = parseSoldListing(listingId, listing);
                        if (sold != null) {
                            soldListings.add(sold);
                        }
                    }
                    
                } catch (Exception e) {
                    Log.w(TAG, "Error processing listing " + listingId + ": " + e.getMessage());
                }
            }
            
            // Save current state as snapshot for next poll
            Map<String, Boolean> currentSnapshot = createSnapshot(currentListings);
            saveSnapshot(currentSnapshot);
            
            Log.d(TAG, "Extracted " + soldListings.size() + " newly-sold listing(s)");
            
        } catch (Exception e) {
            Log.e(TAG, "Error in extractSoldListings: " + e.getMessage(), e);
        }
        
        return soldListings;
    }
    
    /**
     * Get the listings section from farm object
     */
    private JsonObject getCurrentListings(JsonObject farmObject) {
        try {
            if (!farmObject.has("trades")) {
                return null;
            }
            
            JsonObject trades = farmObject.getAsJsonObject("trades");
            if (!trades.has("listings")) {
                return null;
            }
            
            return trades.getAsJsonObject("listings");
        } catch (Exception e) {
            Log.e(TAG, "Error getting current listings: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Parse a sold listing into SoldListing object
     */
    private SoldListing parseSoldListing(String listingId, JsonObject listing) {
        try {
            // Extract item name and amount
            String itemName = "Unknown";
            long amount = 0;
            
            if (listing.has("items")) {
                JsonObject items = listing.getAsJsonObject("items");
                if (!items.entrySet().isEmpty()) {
                    // Get first (usually only) item
                    String firstItemKey = items.keySet().iterator().next();
                    itemName = firstItemKey;
                    amount = items.get(firstItemKey).getAsLong();
                }
            }
            
            // Extract SFL price
            double sfl = 0;
            if (listing.has("sfl")) {
                sfl = listing.get("sfl").getAsDouble();
            }
            
            // Extract fulfilled timestamp
            long fulfilledAt = 0;
            if (listing.has("fulfilledAt")) {
                fulfilledAt = listing.get("fulfilledAt").getAsLong();
            }
            
            SoldListing sold = new SoldListing(listingId, itemName, amount, sfl, fulfilledAt);
            Log.d(TAG, "Parsed sold listing: " + amount + " " + itemName + " for " + sfl + " SFL (ID: " + listingId + ")");
            
            return sold;
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing sold listing " + listingId + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Load previous snapshot from file
     * Map<listingId, isFulfilled>
     */
    private Map<String, Boolean> loadSnapshot() {
        Map<String, Boolean> snapshot = new HashMap<>();
        
        try {
            File file = new File(context.getFilesDir(), SNAPSHOT_FILE);
            if (!file.exists()) {
                Log.d(TAG, "Snapshot file doesn't exist yet (first run)");
                return snapshot;
            }
            
            StringBuilder json = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    json.append(line);
                }
            }
            
            JsonObject snapshotObj = JsonParser.parseString(json.toString()).getAsJsonObject();
            for (String listingId : snapshotObj.keySet()) {
                boolean isFulfilled = snapshotObj.get(listingId).getAsBoolean();
                snapshot.put(listingId, isFulfilled);
            }
            
            Log.d(TAG, "Loaded snapshot with " + snapshot.size() + " listing(s)");
            
        } catch (Exception e) {
            Log.w(TAG, "Error loading snapshot: " + e.getMessage());
        }
        
        return snapshot;
    }
    
    /**
     * Create snapshot of current state
     * Map<listingId, isFulfilled>
     */
    private Map<String, Boolean> createSnapshot(JsonObject currentListings) {
        Map<String, Boolean> snapshot = new HashMap<>();
        
        for (String listingId : currentListings.keySet()) {
            try {
                JsonObject listing = currentListings.getAsJsonObject(listingId);
                boolean isFulfilled = listing.has("fulfilledAt") && !listing.get("fulfilledAt").isJsonNull();
                snapshot.put(listingId, isFulfilled);
            } catch (Exception e) {
                Log.w(TAG, "Error creating snapshot for " + listingId + ": " + e.getMessage());
            }
        }
        
        return snapshot;
    }
    
    /**
     * Save snapshot to file
     */
    private void saveSnapshot(Map<String, Boolean> snapshot) {
        try {
            File file = new File(context.getFilesDir(), SNAPSHOT_FILE);
            
            JsonObject snapshotObj = new JsonObject();
            for (String listingId : snapshot.keySet()) {
                snapshotObj.addProperty(listingId, snapshot.get(listingId));
            }
            
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
                writer.write(snapshotObj.toString());
                writer.flush();
            }
            
            Log.d(TAG, "Saved snapshot with " + snapshot.size() + " listing(s)");
            
        } catch (Exception e) {
            Log.e(TAG, "Error saving snapshot: " + e.getMessage());
        }
    }
    
    /**
     * Represents a sold listing
     */
    public static class SoldListing {
        public String listingId;
        public String itemName;
        public long amount;
        public double sfl;
        public long fulfilledAt;
        
        public SoldListing(String listingId, String itemName, long amount, double sfl, long fulfilledAt) {
            this.listingId = listingId;
            this.itemName = itemName;
            this.amount = amount;
            this.sfl = sfl;
            this.fulfilledAt = fulfilledAt;
        }
    }
}
