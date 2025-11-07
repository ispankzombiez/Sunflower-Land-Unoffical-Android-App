package com.sunflowerland.mobile;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.sunflowerland.mobile.models.FarmItem;
import androidx.preference.PreferenceManager;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Extracts auction schedule data for notifications
 * - Extracts only FUTURE auctions (where startAt > currentTime)
 * - Each auction = one individual notification (no grouping)
 * - Optimizes by only scheduling the NEXT upcoming auction
 * - Implements expiration flag when all auctions are in the past
 */
public class AuctionScheduleExtractor {
    private static final String TAG = "AuctionScheduleExtractor";
    private Context context;

    public AuctionScheduleExtractor(Context context) {
        this.context = context;
    }

    /**
     * Ensure raw auction data file exists as a placeholder
     */
    private void ensureRawAuctionFileExists() {
        try {
            File filesDir = context.getFilesDir();
            File auctionFile = new File(filesDir, "auction_schedule_raw.json");

            // Create file if it doesn't exist
            if (!auctionFile.exists()) {
                auctionFile.createNewFile();
                Log.d(TAG, "Created placeholder file: " + auctionFile.getAbsolutePath());
            }
        } catch (IOException e) {
            Log.e(TAG, "Error creating raw auction file: " + e.getMessage(), e);
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in ensureRawAuctionFileExists: " + e.getMessage(), e);
        }
    }

    /**
     * Read raw auction data from app assets
     * Returns null if file is empty or doesn't exist
     */
    private JsonObject readRawAuctionFromFile() {
        try {
            // Read directly from assets
            java.io.InputStream inputStream = context.getAssets().open("auction_schedule_raw.json");
            StringBuilder content = new StringBuilder();
            try (java.io.InputStreamReader reader = new java.io.InputStreamReader(inputStream)) {
                char[] buffer = new char[1024];
                int read;
                while ((read = reader.read(buffer)) != -1) {
                    content.append(buffer, 0, read);
                }
            }
            
            String jsonString = content.toString().trim();
            if (jsonString.isEmpty()) {
                Log.d(TAG, "Auction file from assets is empty");
                DebugLog.log("❌ Auction file from assets is empty");
                return null;
            }

            Log.d(TAG, "Read " + jsonString.length() + " characters from auction file");
            DebugLog.log("✅ Read auction file: " + jsonString.length() + " chars, First 100 chars: " + jsonString.substring(0, Math.min(100, jsonString.length())));

            // Parse JSON
            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(jsonString, JsonObject.class);
            Log.d(TAG, "Successfully parsed JSON from auction file");
            DebugLog.log("✅ Successfully parsed JSON from auction file");
            return jsonObject;

        } catch (Exception e) {
            Log.w(TAG, "Error reading auction file from assets: " + e.getMessage());
            DebugLog.error("Error reading auction file from assets", e);
            return null;
        }
    }

    /**
     * Main extraction method - returns list of FarmItems for auction notifications
     * 
     * AUCTION SCHEDULE IS INDEPENDENT FROM API:
     * - Reads ONLY from raw file (never uses API data)
     * - Tracks file changes to detect new auction schedules
     * - Sets expired=true when no future auctions remain
     * - Only resumes processing when file is updated with new schedule
     */
    public List<FarmItem> extractAuctionNotifications(JsonObject farmData) {
        List<FarmItem> items = new ArrayList<>();

        // Read raw auction data from assets
        JsonObject auctionData = readRawAuctionFromFile();
        
        // File is empty or invalid
        if (auctionData == null) {
            Log.d(TAG, "Auction file from assets is empty or invalid - no auctions to process");
            DebugLog.log("⚠️ Auction file is null/empty - marking as expired");
            markAuctionScheduleExpired(true);
            return items;
        }

        // Check if this is new data by looking for isNew flag
        boolean isNewData = false;
        if (auctionData.has("isNew")) {
            try {
                isNewData = auctionData.get("isNew").getAsBoolean();
                Log.d(TAG, "isNew flag found: " + isNewData);
                DebugLog.log("📌 isNew flag in file: " + isNewData);
            } catch (Exception e) {
                Log.w(TAG, "Error reading isNew flag: " + e.getMessage());
            }
        }

        // If data is marked as new, clear the expired flag and resume processing
        if (isNewData) {
            Log.d(TAG, "New auction data detected - clearing expired flag");
            DebugLog.log("✅ NEW DATA DETECTED - resuming auction processing");
            markAuctionScheduleExpired(false);
        } else if (isAuctionScheduleExpired()) {
            // If not new data and schedule is expired, skip processing
            Log.d(TAG, "Auction schedule is expired and no new data flag - skipping");
            DebugLog.log("⏸️  Auction schedule is expired - skipping processing");
            return items;
        } else {
            DebugLog.log("Auction schedule is ACTIVE (not expired)");
        }

        if (!auctionData.has("auctions")) {
            Log.d(TAG, "No auctions data found in file");
            DebugLog.log("⚠️ JSON has no 'auctions' key - marking as expired");
            markAuctionScheduleExpired(true);
            return items;
        }

        try {
            // Handle both structures:
            // 1. Nested: { "auctions": { "auctions": [...] } }
            // 2. Flat: { "isNew": true, "auctions": [...] }
            JsonArray auctionsArray;
            Object auctionsObj = auctionData.get("auctions");
            
            if (auctionsObj instanceof JsonArray) {
                // Flat structure - direct array
                auctionsArray = (JsonArray) auctionsObj;
            } else if (auctionsObj instanceof JsonObject) {
                // Nested structure - object containing array
                JsonObject auctionsContainer = (JsonObject) auctionsObj;
                auctionsArray = auctionsContainer.getAsJsonArray("auctions");
            } else {
                Log.e(TAG, "Unexpected auction structure type: " + auctionsObj.getClass().getName());
                DebugLog.log("❌ Unexpected auction structure");
                return items;
            }
            
            long currentTime = System.currentTimeMillis();

            Log.d(TAG, "Processing " + auctionsArray.size() + " total auctions from file");
            DebugLog.log("📋 Processing " + auctionsArray.size() + " total auctions from file");

            // Extract only FUTURE auctions
            for (int i = 0; i < auctionsArray.size(); i++) {
                JsonObject auction = auctionsArray.get(i).getAsJsonObject();

                try {
                    long startAt = auction.get("startAt").getAsLong();
                    long endAt = auction.get("endAt").getAsLong();

                    // Only include if startAt is in the future
                    if (startAt > currentTime) {
                        FarmItem item = extractAuctionToFarmItem(auction, startAt, endAt);
                        if (item != null) {
                            items.add(item);
                            Log.d(TAG, "Added auction: " + item.getName() + 
                                  ", startAt=" + formatTimestamp(startAt));
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error processing auction entry: " + e.getMessage());
                }
            }

            // Check if we found any future auctions
            if (items.isEmpty()) {
                Log.d(TAG, "No future auctions found - marking schedule as expired");
                DebugLog.log("⚠️ No FUTURE auctions found (all in past) - marking as expired");
                markAuctionScheduleExpired(true);
            } else {
                Log.d(TAG, "Found " + items.size() + " future auctions");
                DebugLog.log("✅ Found " + items.size() + " future auction(s) ready to schedule");
                // Schedule is active, make sure expired flag is clear
                markAuctionScheduleExpired(false);
            }

            // If we detected new data, mark it as processed in the file
            if (isNewData) {
                Log.d(TAG, "Marking new data as processed by setting isNew=false");
                DebugLog.log("📝 Marking data as processed (isNew=false)");
                updateIsNewFlagInFile(false);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error extracting auction data: " + e.getMessage(), e);
            DebugLog.error("Error extracting auction data", e);
            markAuctionScheduleExpired(true);
        }

        return items;
    }

    /**
     * Convert a single auction JSON object to a FarmItem
     * Stores auction metadata in the details field for later use
     */
    private FarmItem extractAuctionToFarmItem(JsonObject auction, long startAt, long endAt) {
        try {
            String auctionId = auction.get("auctionId").getAsString();
            long sfl = auction.has("sfl") ? auction.get("sfl").getAsLong() : 0;

            // Get the name based on type (wearable, collectible, or nft)
            String auctionName = getAuctionName(auction);
            if (auctionName == null) {
                Log.w(TAG, "Could not determine auction item name");
                return null;
            }

            // Extract ingredients (for icon selection logic)
            String ingredientsJson = "";
            if (auction.has("ingredients")) {
                ingredientsJson = auction.getAsJsonObject("ingredients").toString();
            }

            // Create FarmItem with category "auction" and timestamp = startAt
            FarmItem item = new FarmItem(auctionId, "auction", auctionName, 1, startAt);

            // Store metadata in details: startAt|endAt|sfl|ingredientsJson
            String details = startAt + "|" + endAt + "|" + sfl + "|" + ingredientsJson;
            item.setDetails(details);

            return item;

        } catch (Exception e) {
            Log.w(TAG, "Error converting auction to FarmItem: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get the auction item name based on type
     * Looks for wearable, collectible, or nft fields
     */
    private String getAuctionName(JsonObject auction) {
        try {
            if (auction.has("wearable")) {
                return auction.get("wearable").getAsString();
            }
            if (auction.has("collectible")) {
                return auction.get("collectible").getAsString();
            }
            if (auction.has("nft")) {
                return auction.get("nft").getAsString();
            }
        } catch (Exception e) {
            Log.w(TAG, "Error getting auction name: " + e.getMessage());
        }
        return null;
    }

    /**
     * Mark auction schedule as expired when no future auctions exist
     * This prevents unnecessary scanning until data is updated
     */
    private void markAuctionScheduleExpired(boolean expired) {
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            prefs.edit().putBoolean("auctionScheduleExpired", expired).apply();
            Log.d(TAG, "Auction schedule expired flag set to: " + expired);
        } catch (Exception e) {
            Log.w(TAG, "Error setting auction schedule expired flag: " + e.getMessage());
        }
    }

    /**
     * Check if auction schedule is expired
     */
    public boolean isAuctionScheduleExpired() {
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            return prefs.getBoolean("auctionScheduleExpired", false);
        } catch (Exception e) {
            Log.w(TAG, "Error reading auction schedule expired flag: " + e.getMessage());
            return false;
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
     * Check if the auction file has changed since last check
     * Used to detect when a new auction schedule is uploaded
     */
    private boolean hasAuctionFileChanged() {
        try {
            File filesDir = context.getFilesDir();
            File auctionFile = new File(filesDir, "auction_schedule_raw.json");

            if (!auctionFile.exists()) {
                Log.d(TAG, "Auction file does not exist");
                return false;
            }

            // Get current file modification time
            long currentModTime = auctionFile.lastModified();
            
            // Get stored modification time
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            long lastModTime = prefs.getLong("auctionFileLastModified", 0);

            Log.d(TAG, "Current file modTime: " + currentModTime + ", Last stored: " + lastModTime);

            // File has changed if current modTime is newer than stored
            if (currentModTime > lastModTime) {
                Log.d(TAG, "Auction file was updated!");
                // Store the new modification time
                prefs.edit().putLong("auctionFileLastModified", currentModTime).apply();
                return true;
            }

            Log.d(TAG, "Auction file unchanged");
            return false;

        } catch (Exception e) {
            Log.w(TAG, "Error checking if auction file changed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Update the isNew flag in the auction file
     * After processing new data, set it to false to mark it as processed
     * Uses safe string replacement to preserve JSON structure
     */
    private void updateIsNewFlagInFile(boolean value) {
        try {
            File filesDir = context.getFilesDir();
            File auctionFile = new File(filesDir, "auction_schedule_raw.json");

            if (!auctionFile.exists()) {
                Log.w(TAG, "Cannot update isNew flag - file does not exist");
                return;
            }

            // Read current content
            StringBuilder content = new StringBuilder();
            try (FileReader reader = new FileReader(auctionFile)) {
                char[] buffer = new char[1024];
                int read;
                while ((read = reader.read(buffer)) != -1) {
                    content.append(buffer, 0, read);
                }
            }

            String jsonString = content.toString().trim();
            if (jsonString.isEmpty()) {
                Log.w(TAG, "Auction file is empty, cannot update isNew flag");
                return;
            }

            // Safe string replacement: only replace isNew value, not anything else
            // Replace both "isNew": true and "isNew": false with "isNew": false
            String updatedJson = jsonString;
            
            // First try to replace "isNew": true (with various whitespace patterns)
            updatedJson = updatedJson.replaceAll("\"isNew\"\\s*:\\s*true\\b", "\"isNew\": false");
            
            // If isNew doesn't exist in the file, add it at the beginning (after opening brace)
            if (!jsonString.contains("\"isNew\"")) {
                // Add isNew flag right after the opening brace
                updatedJson = updatedJson.replaceFirst("\\{\\s*", "{\"isNew\": false,");
            } else {
                // Just make sure it's set to false (if it somehow got set to true again)
                updatedJson = updatedJson.replaceAll("\"isNew\"\\s*:\\s*false\\b", "\"isNew\": false");
            }
            
            // Write back to file
            try (FileWriter writer = new FileWriter(auctionFile)) {
                writer.write(updatedJson);
            }

            Log.d(TAG, "Updated isNew flag in file to: false");
            DebugLog.log("✏️  Updated isNew flag in file to: false");

        } catch (Exception e) {
            Log.w(TAG, "Error updating isNew flag: " + e.getMessage());
            DebugLog.log("⚠️  Error updating isNew flag: " + e.getMessage());
        }
    }
}
