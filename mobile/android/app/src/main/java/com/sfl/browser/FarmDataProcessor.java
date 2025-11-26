package com.sfl.browser;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;
import androidx.work.Data;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sfl.browser.clustering.CategoryClusterer;
import com.sfl.browser.clustering.ClustererFactory;
import com.sfl.browser.clustering.NotificationGroup;
import com.sfl.browser.clustering.PetSleepClusterer;
import com.sfl.browser.models.FarmItem;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Utility class for farm data processing pipeline.
 * Extracted from NotificationWorker to be reused by both:
 * - OneTimeWorkRequest (immediate execution)
 * - PeriodicWorkRequest (future scheduled runs)
 * 
 * Handles:
 * 1. API call to fetch raw farm data
 * 2. Parse and extract farm items by category
 * 3. Cluster items by readiness time
 * 4. Schedule notifications for ready items
 * 5. Log all processing steps
 */
public class FarmDataProcessor {
    private static final String TAG = "FarmDataProcessor";
    private static final String API_BASE_URL = "https://api.sunflower-land.com/community/farms/";

    /**
     * Process farm data using input data from WorkRequest
     * @param context Application context
     * @param inputData WorkRequest input data containing farm_id, api_key, etc.
     */
    public static void processFarmDataFromWorker(Context context, Data inputData) {
        try {
            String farmId = inputData.getString("farm_id");
            String apiKey = inputData.getString("api_key");
            String source = inputData.getString("source");
            int workerId = inputData.getInt("worker_id", -1);

            processFarmData(context, farmId, apiKey, source, workerId);
        } catch (Exception e) {
            Log.e(TAG, "Error processing farm data from worker input: " + e.getMessage(), e);
            DebugLog.error("Error in processFarmDataFromWorker", e);
            writeErrorLog(context, "Worker input processing failed: " + e.getMessage());
        }
    }

    /**
     * Process farm data using provided credentials
     * @param context Application context
     * @param farmId Farm ID from settings
     * @param apiKey API key from settings
     * @param source Source identifier ("manual", "auto", "immediate", etc.)
     * @param workerId Worker ID (-1 for non-worker calls)
     */
    public static void processFarmData(Context context, String farmId, String apiKey, String source, int workerId) {
        try {
            String workerTag = workerId >= 0 ? "Worker #" + workerId : "Processor";
            Log.d(TAG, "=== " + workerTag + " - Starting Farm Data Processing Pipeline (source: " + source + ") ===");
            DebugLog.log("🚀 processFarmData CALLED with source: " + source);
            DebugLog.log("=== " + workerTag + " - Starting Farm Data Processing Pipeline (source: " + source + ") ===");

            if (farmId == null || farmId.isEmpty() || apiKey == null || apiKey.isEmpty()) {
                Log.e(TAG, "Farm ID or API key not found");
                DebugLog.error("Farm ID or API key not configured", null);
                writeErrorLog(context, "Farm ID or API key not configured");
                return;
            }

            // OPTIMIZATION: Check if we just fetched data within the last 30 seconds
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            long lastAPICallTime = prefs.getLong("lastAPICallTime", 0);
            long currentTime = System.currentTimeMillis();
            long timeSinceLastCall = currentTime - lastAPICallTime;

            if (timeSinceLastCall < 30000) {  // 30 seconds
                Log.d(TAG, "Skipping API call - last call was " + (timeSinceLastCall / 1000) + " seconds ago (cache window: 30 seconds)");
                DebugLog.logStep("Cache Check", "Skipping API call - last call was " + (timeSinceLastCall / 1000) + " seconds ago");
                return;
            }

            // Build API URL
            String apiUrl = API_BASE_URL + farmId;
            Log.d(TAG, "API URL: " + apiUrl);
            DebugLog.logStep("API Setup", "URL: " + apiUrl);

            // Step 1: API Call
            Log.d(TAG, "Step 1: Fetching raw JSON from API...");
            DebugLog.logStep("Step 1", "Fetching raw JSON from API");
            String rawJSON = APIClient.fetchRawJSON(apiUrl, apiKey);
            if (rawJSON == null || rawJSON.isEmpty()) {
                Log.e(TAG, "Failed to fetch raw JSON from API");
                DebugLog.error("Step 1 Failed: API call returned no data", null);
                writeErrorLog(context, "API call failed - no response received");
                throw new RuntimeException("API call failed - no valid response received. Will retry in 20 seconds.");
            }

            // Record successful API call time
            prefs.edit().putLong("lastAPICallTime", currentTime).apply();
            saveRawJSON(context, rawJSON);
            Log.d(TAG, "Step 1 Complete: Raw JSON saved");
            DebugLog.logStep("Step 1", "Complete: Raw JSON saved successfully");

            // Step 2: Parse JSON and extract all categories
            Log.d(TAG, "Step 2: Parsing JSON and extracting farm items...");
            DebugLog.logStep("Step 2", "Parsing JSON and extracting farm items");
            JsonObject parsedJson = JsonParser.parseString(rawJSON).getAsJsonObject();
            JsonObject farmObject = parsedJson.getAsJsonObject("farm");

            List<FarmItem> crops = CategoryExtractors.extractCrops(farmObject);
            List<FarmItem> fruits = CategoryExtractors.extractFruits(farmObject);
            
            // Extract greenhouse crops if enabled
            List<FarmItem> greenhouseCrops = new ArrayList<>();
            if (prefs.getBoolean("category_greenhouse_crops", true)) {
                greenhouseCrops = CategoryExtractors.extractGreenhouseCrops(farmObject);
            }
            
            List<FarmItem> resources = CategoryExtractors.extractResources(farmObject);
            List<FarmItem> animals = CategoryExtractors.extractAnimals(farmObject);
            List<FarmItem> cooking = CategoryExtractors.extractCooking(farmObject);
            List<FarmItem> composters = CategoryExtractors.extractComposters(farmObject);
            List<FarmItem> flowers = CategoryExtractors.extractFlowers(farmObject);
            List<FarmItem> craftingBox = CategoryExtractors.extractCraftingBox(farmObject);
            List<FarmItem> beehives = CategoryExtractors.extractBeehives(farmObject);
            List<FarmItem> cropMachine = CategoryExtractors.extractCropMachine(farmObject);
            List<FarmItem> sunstones = CategoryExtractors.extractSunstones(farmObject);

            // Extract skill cooldowns if enabled
            List<FarmItem> skillCooldowns = new ArrayList<>();
            if (prefs.getBoolean("category_skill_cooldown", true)) {
                JsonObject bumpkinObject = farmObject.getAsJsonObject("bumpkin");
                if (bumpkinObject != null) {
                    skillCooldowns = SkillExtractors.extractSkillCooldowns(bumpkinObject);
                }
            }

            // Extract daily reset if enabled
            List<FarmItem> dailyReset = new ArrayList<>();
            if (prefs.getBoolean("category_daily_reset", true)) {
                dailyReset = CategoryExtractors.extractDailyReset(farmObject);
            }

            // Extract marketplace sold listings
            List<MarketplaceListingsExtractor.SoldListing> soldListings = new ArrayList<>();
            if (prefs.getBoolean("marketplace_listings_enabled", true)) {
                MarketplaceListingsExtractor marketplaceExtractor = new MarketplaceListingsExtractor(context);
                soldListings = marketplaceExtractor.extractSoldListings(farmObject);
            }

            // Extract floating island notifications
            List<FarmItem> floatingIsland = new ArrayList<>();
            if (prefs.getBoolean("floating_island_enabled", true)) {
                floatingIsland = CategoryExtractors.extractFloatingIsland(farmObject, context);
            }

            // Extract auction schedule at the end of API extraction
            // This reads from the independent raw file, independent from API data
            List<FarmItem> auctions = new ArrayList<>();
            if (prefs.getBoolean("auction_schedule_enabled", true)) {
                try {
                    AuctionScheduleExtractor auctionExtractor = new AuctionScheduleExtractor(context);
                    auctions = auctionExtractor.extractAuctionNotifications(farmObject);
                    DebugLog.log("Auction extraction complete: " + auctions.size() + " future auction(s) found");
                } catch (Exception auctionError) {
                    Log.e(TAG, "Error during auction extraction: " + auctionError.getMessage(), auctionError);
                    DebugLog.error("Error during auction extraction", auctionError);
                    DebugLog.log("⚠️ Auction extraction failed - continuing without auctions");
                    // Don't break the pipeline - just skip auctions this run
                    auctions = new ArrayList<>();
                }
            } else {
                DebugLog.log("Auction schedule processing disabled in preferences");
            }

            // Merge auction data with other extracted data for unified processing
            List<FarmItem> allExtractedItems = new ArrayList<>();
            allExtractedItems.addAll(crops);
            allExtractedItems.addAll(fruits);
            allExtractedItems.addAll(greenhouseCrops);
            allExtractedItems.addAll(resources);
            allExtractedItems.addAll(animals);
            allExtractedItems.addAll(cooking);
            allExtractedItems.addAll(composters);
            allExtractedItems.addAll(flowers);
            allExtractedItems.addAll(craftingBox);
            allExtractedItems.addAll(beehives);
            allExtractedItems.addAll(cropMachine);
            allExtractedItems.addAll(sunstones);
            allExtractedItems.addAll(skillCooldowns);
            allExtractedItems.addAll(dailyReset);
            allExtractedItems.addAll(floatingIsland);
            allExtractedItems.addAll(auctions);

            Log.d(TAG, "Step 2 Complete: Extracted " + crops.size() + " crop(s), " + fruits.size() + " fruit(s), " + greenhouseCrops.size() + " greenhouse crop(s), " + resources.size() + " resource(s), " + animals.size() + " animal(s), " + auctions.size() + " auction(s), total " + allExtractedItems.size() + " item(s)");
            DebugLog.logStep("Step 2", "Complete: Extracted items - Total: " + allExtractedItems.size() + ", Auctions: " + auctions.size());

            // Step 3: Cluster items by category
            Log.d(TAG, "Step 3: Clustering items by readiness time...");
            DebugLog.logStep("Step 3", "Clustering items by readiness time");
            List<NotificationGroup> allGroups = new ArrayList<>();

            // Cluster all standard categories
            CategoryClusterer cropClusterer = ClustererFactory.getClusterer("crops", context);
            List<NotificationGroup> cropGroups = cropClusterer.cluster(crops);
            allGroups.addAll(cropGroups);

            CategoryClusterer fruitClusterer = ClustererFactory.getClusterer("fruits", context);
            List<NotificationGroup> fruitGroups = fruitClusterer.cluster(fruits);
            allGroups.addAll(fruitGroups);

            CategoryClusterer greenhouseCropClusterer = ClustererFactory.getClusterer("greenhouse_crops", context);
            List<NotificationGroup> greenhouseCropGroups = greenhouseCropClusterer.cluster(greenhouseCrops);
            allGroups.addAll(greenhouseCropGroups);

            CategoryClusterer resourceClusterer = ClustererFactory.getClusterer("resources", context);
            List<NotificationGroup> resourceGroups = resourceClusterer.cluster(resources);
            allGroups.addAll(resourceGroups);

            CategoryClusterer animalClusterer = ClustererFactory.getClusterer("animals", context);
            List<NotificationGroup> animalGroups = animalClusterer.cluster(animals);
            allGroups.addAll(animalGroups);

            CategoryClusterer cookingClusterer = ClustererFactory.getClusterer("cooking", context);
            List<NotificationGroup> cookingGroups = cookingClusterer.cluster(cooking);
            allGroups.addAll(cookingGroups);

            CategoryClusterer composterClusterer = ClustererFactory.getClusterer("composters", context);
            List<NotificationGroup> composterGroups = composterClusterer.cluster(composters);
            allGroups.addAll(composterGroups);

            CategoryClusterer flowerClusterer = ClustererFactory.getClusterer("flowers", context);
            List<NotificationGroup> flowerGroups = flowerClusterer.cluster(flowers);
            allGroups.addAll(flowerGroups);

            CategoryClusterer craftingBoxClusterer = ClustererFactory.getClusterer("crafting_box", context);
            List<NotificationGroup> craftingBoxGroups = craftingBoxClusterer.cluster(craftingBox);
            allGroups.addAll(craftingBoxGroups);

            CategoryClusterer beehiveClusterer = ClustererFactory.getClusterer("beehives", context);
            List<NotificationGroup> beehiveGroups = beehiveClusterer.cluster(beehives);
            allGroups.addAll(beehiveGroups);

            CategoryClusterer cropMachineClusterer = ClustererFactory.getClusterer("crop_machine", context);
            List<NotificationGroup> cropMachineGroups = cropMachineClusterer.cluster(cropMachine);
            allGroups.addAll(cropMachineGroups);

            CategoryClusterer sunstoneClusterer = ClustererFactory.getClusterer("sunstones", context);
            List<NotificationGroup> sunstoneGroups = sunstoneClusterer.cluster(sunstones);
            allGroups.addAll(sunstoneGroups);

            CategoryClusterer skillCooldownClusterer = ClustererFactory.getClusterer("skill_cooldown", context);
            List<NotificationGroup> skillCooldownGroups = skillCooldownClusterer.cluster(skillCooldowns);
            allGroups.addAll(skillCooldownGroups);

            CategoryClusterer dailyResetClusterer = ClustererFactory.getClusterer("daily_reset", context);
            List<NotificationGroup> dailyResetGroups = dailyResetClusterer.cluster(dailyReset);
            allGroups.addAll(dailyResetGroups);

            // Convert sold marketplace listings to notification groups
            List<NotificationGroup> marketplaceGroups = convertSoldListingsToNotifications(soldListings);
            allGroups.addAll(marketplaceGroups);
            Log.d(TAG, "  Marketplace: Created " + marketplaceGroups.size() + " group(s)");

            CategoryClusterer floatingIslandClusterer = ClustererFactory.getClusterer("floating_island", context);
            List<NotificationGroup> floatingIslandGroups = floatingIslandClusterer.cluster(floatingIsland);
            allGroups.addAll(floatingIslandGroups);

            // Handle auctions separately (one at a time, no clustering)
            // Process auctions: Only schedule ONE at a time, the soonest upcoming
            Log.d(TAG, "Processing " + auctions.size() + " auction(s)");
            DebugLog.log("Auction Processing: Found " + auctions.size() + " auction(s) in file");
            
            if (!auctions.isEmpty()) {
                // Find the soonest auction
                FarmItem nextAuction = auctions.stream()
                    .min((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()))
                    .orElse(null);

                if (nextAuction != null) {
                    long nextAuctionStartAt = nextAuction.getTimestamp();

                    // Check if we should schedule this auction
                    String lastScheduledAuctionId = getLastScheduledAuctionId(context);
                    long lastScheduledAuctionStart = getLastScheduledAuctionStart(context);

                    Log.d(TAG, "Next auction: " + nextAuction.getName() + " (ID: " + nextAuction.getId() + ") at " + formatTimestamp(nextAuctionStartAt));
                    Log.d(TAG, "Last scheduled: ID=" + lastScheduledAuctionId + ", StartAt=" + formatTimestamp(lastScheduledAuctionStart) + ", CurrentTime=" + formatTimestamp(currentTime));
                    DebugLog.log("Next auction: " + nextAuction.getName() + " (ID: " + nextAuction.getId() + ") at " + formatTimestamp(nextAuctionStartAt));
                    DebugLog.log("Last scheduled: ID=" + lastScheduledAuctionId + " | Current: " + formatTimestamp(currentTime));

                    // Create single notification group for this auction
                    // Always add to display list (even if already scheduled)
                    NotificationGroup group = new NotificationGroup();
                    group.category = "auction";
                    group.name = formatAuctionDisplayName(nextAuction.getName(), nextAuction.getDetails());
                    group.quantity = 1;
                    group.earliestReadyTime = nextAuctionStartAt;

                    // Parse details for icon/metadata
                    String details = nextAuction.getDetails();
                    if (details != null && !details.isEmpty()) {
                        String[] parts = details.split("\\|", 4);
                        if (parts.length >= 2) {
                            group.details = parts[1] + "|" + parts[2] + "|" + (parts.length > 3 ? parts[3] : "");
                        } else {
                            group.details = details;
                        }
                    }

                    group.groupId = "auction_" + nextAuction.getId();
                    allGroups.add(group);

                    // Only update stored info if:
                    // 1. Different auction than last scheduled, OR
                    // 2. Same auction but its time has passed (ready to fire)
                    if (nextAuctionStartAt != lastScheduledAuctionStart || currentTime >= nextAuctionStartAt) {
                        Log.d(TAG, "SCHEDULING NEW AUCTION: Different from last (" + (nextAuctionStartAt != lastScheduledAuctionStart) + ") OR time passed (" + (currentTime >= nextAuctionStartAt) + ")");
                        DebugLog.log("✅ SCHEDULING NEW AUCTION (Different: " + (nextAuctionStartAt != lastScheduledAuctionStart) + " | TimePassed: " + (currentTime >= nextAuctionStartAt) + ")");
                        
                        // Store this auction as scheduled
                        storeLastScheduledAuctionId(context, nextAuction.getId());
                        storeLastScheduledAuctionStart(context, nextAuctionStartAt);

                        Log.d(TAG, "✅ AUCTION ADDED TO NOTIFICATION LIST: " + nextAuction.getName());
                        DebugLog.log("✅ AUCTION ADDED TO NOTIFICATION LIST: " + nextAuction.getName());
                    } else {
                        Log.d(TAG, "⏭️  Auction already scheduled and time hasn't passed - keeping existing");
                        DebugLog.log("⏭️  Auction already scheduled (ID: " + lastScheduledAuctionId + ") - keeping existing (displaying in log)");
                    }
                } else {
                    Log.d(TAG, "No next auction found even though list has " + auctions.size() + " items");
                    DebugLog.log("❌ No next auction found even though list has " + auctions.size() + " items");
                }
            } else {
                Log.d(TAG, "No auctions to process");
                DebugLog.log("No auctions to process");
            }

            Log.d(TAG, "Step 3 Complete: Created " + allGroups.size() + " total notification group(s)");
            DebugLog.logStep("Step 3", "Complete: Created " + allGroups.size() + " notification group(s)");

            // Step 3.5: Process pet sleep notifications
            Log.d(TAG, "Step 3.5: Processing pet sleep notifications...");
            DebugLog.logStep("Step 3.5", "Processing pet sleep notifications");
            try {
                boolean petSleepEnabled = prefs.getBoolean("category_pet_sleep", true);
                if (petSleepEnabled) {
                    JsonObject petsData = farmObject.getAsJsonObject("pets");
                    if (petsData != null && !petsData.entrySet().isEmpty()) {
                        PetSleepClusterer petSleepClusterer = new PetSleepClusterer();
                        List<NotificationGroup> petSleepGroups = petSleepClusterer.clusterPetSleep(petsData);
                        allGroups.addAll(petSleepGroups);
                        Log.d(TAG, "Step 3.5: Added " + petSleepGroups.size() + " pet sleep notification group(s)");
                        DebugLog.logStep("Step 3.5", "Complete: Added " + petSleepGroups.size() + " pet sleep group(s)");
                    } else {
                        Log.d(TAG, "Step 3.5: No pets data found");
                        DebugLog.logStep("Step 3.5", "No pets data found");
                    }
                } else {
                    Log.d(TAG, "Step 3.5: Pet sleep notifications are disabled");
                    DebugLog.logStep("Step 3.5", "Pet sleep notifications disabled");
                }
            } catch (Exception e) {
                Log.w(TAG, "Error processing pet sleep notifications: " + e.getMessage(), e);
                DebugLog.log("⚠️ Warning: Pet sleep processing failed: " + e.getMessage());
            }

            // Step 4: Save processed JSON
            Log.d(TAG, "Step 4: Saving processed data...");
            DebugLog.logStep("Step 4", "Saving processed data");
            saveProcessedJSON(context, crops, fruits, resources, animals, cooking, composters, flowers, craftingBox, beehives, cropMachine, sunstones, dailyReset);
            Log.d(TAG, "Step 4 Complete: Processed data saved");
            DebugLog.logStep("Step 4", "Complete: Processed data saved");

            // Step 5: Schedule notifications using AlarmManager
            Log.d(TAG, "Step 5: Scheduling notifications for " + allGroups.size() + " group(s)...");
            DebugLog.logStep("Step 5", "Scheduling notifications for " + allGroups.size() + " group(s)");
            AlarmScheduler scheduler = new AlarmScheduler(context);
            scheduler.cancelAllPendingAlarms();
            scheduler.clearScheduledTracking();
            scheduler.scheduleNotificationAlarms(allGroups);
            Log.d(TAG, "Step 5 Complete: Notifications scheduled");
            DebugLog.logStep("Step 5", "Complete: Notifications scheduled");

            // Step 6: Write scheduled notifications log
            Log.d(TAG, "Step 6: Writing scheduled notifications log...");
            DebugLog.logStep("Step 6", "Writing scheduled notifications log");
            
            // Sort groups by earliest ready time (chronological order - soonest first)
            allGroups.sort((g1, g2) -> Long.compare(g1.earliestReadyTime, g2.earliestReadyTime));
            
            writeScheduledNotificationsLog(context, allGroups);
            Log.d(TAG, "Step 6 Complete: Log written");
            DebugLog.logStep("Step 6", "Complete: Log written");

            Log.d(TAG, "=== " + workerTag + " - Farm Data Processing Pipeline Complete ===");
            DebugLog.log("=== " + workerTag + " - Farm Data Processing Pipeline Complete ===");
        } catch (Exception e) {
            Log.e(TAG, "Error in processFarmData: " + e.getMessage(), e);
            DebugLog.error("Pipeline error in processFarmData", e);
            writeErrorLog(context, "Processing pipeline failed: " + e.getMessage());
        }
    }

    /**
     * Save raw JSON response from API for debugging
     */
    private static void saveRawJSON(Context context, String rawJSON) {
        try {
            File file = new File(context.getFilesDir(), "farm_api_raw.json");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(rawJSON);
                writer.flush();
            }
            Log.d(TAG, "Raw JSON saved: " + file.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Error saving raw JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Save processed farm data as JSON for debugging
     */
    private static void saveProcessedJSON(Context context, List<FarmItem> crops, List<FarmItem> fruits,
                                          List<FarmItem> resources, List<FarmItem> animals, List<FarmItem> cooking,
                                          List<FarmItem> composters, List<FarmItem> flowers, List<FarmItem> craftingBox,
                                          List<FarmItem> beehives, List<FarmItem> cropMachine, List<FarmItem> sunstones,
                                          List<FarmItem> dailyReset) {
        try {
            Gson gson = new Gson();
            JsonObject processedData = new JsonObject();
            processedData.add("crops", gson.toJsonTree(crops));
            processedData.add("fruits", gson.toJsonTree(fruits));
            processedData.add("resources", gson.toJsonTree(resources));
            processedData.add("animals", gson.toJsonTree(animals));

            File file = new File(context.getFilesDir(), "processed_data.json");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(gson.toJson(processedData));
                writer.flush();
            }
            Log.d(TAG, "Processed JSON saved: " + file.getAbsolutePath());
        } catch (Exception e) {
            Log.e(TAG, "Error saving processed JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Write scheduled notifications log in plain English format
     */
    private static void writeScheduledNotificationsLog(Context context, List<NotificationGroup> groups) {
        try {
            Log.d(TAG, "writeScheduledNotificationsLog: Starting with " + groups.size() + " groups");
            File file = new File(context.getFilesDir(), "notification_summary.log");
            Log.d(TAG, "writeScheduledNotificationsLog: File path: " + file.getAbsolutePath());
            
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
                writer.write("=== Currently Scheduled Notifications ===\n");
                writer.write("Generated at: " + getCurrentTimestamp() + "\n");
                writer.write("---\n\n");

                if (groups.isEmpty()) {
                    writer.write("(No upcoming notifications scheduled)\n");
                    Log.d(TAG, "writeScheduledNotificationsLog: No groups to write");
                } else {
                    long currentTime = System.currentTimeMillis();
                    Log.d(TAG, "writeScheduledNotificationsLog: Writing " + groups.size() + " groups");

                    for (NotificationGroup group : groups) {
                        long timeRemaining = group.earliestReadyTime - currentTime;
                        String readyTime = formatTimeForNotification(group.earliestReadyTime);
                        String timeRemainingStr;

                        if (timeRemaining <= 0) {
                            timeRemainingStr = "now";
                        } else {
                            long minutes = timeRemaining / (60 * 1000);
                            long hours = minutes / 60;
                            long remainingMinutes = minutes % 60;

                            if (hours > 0) {
                                timeRemainingStr = hours + "h " + remainingMinutes + "m";
                            } else {
                                timeRemainingStr = remainingMinutes + "m";
                            }
                        }

                        String logEntry = "[" + readyTime + "] " + group.quantity + " " + group.name +
                                        " (ready in " + timeRemainingStr + ")\n";
                        writer.write(logEntry);
                        Log.d(TAG, "writeScheduledNotificationsLog: Wrote entry: " + logEntry.trim());
                    }
                }
                writer.flush();
                Log.d(TAG, "writeScheduledNotificationsLog: File successfully written");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error writing notification log: " + e.getMessage(), e);
            DebugLog.error("Error writing notification log", e);
        }
    }

    /**
     * Write error log
     */
    private static void writeErrorLog(Context context, String errorMessage) {
        try {
            File file = new File(context.getFilesDir(), "notification_summary.log");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
                writer.write("=== Error Log ===\n");
                writer.write("Error at: " + getCurrentTimestamp() + "\n");
                writer.write("Message: " + errorMessage + "\n");
                writer.flush();
            }
            Log.e(TAG, "Error log written: " + errorMessage);
        } catch (Exception e) {
            Log.e(TAG, "Error writing error log: " + e.getMessage(), e);
        }
    }

    /**
     * Return current timestamp in full format
     */
    private static String getCurrentTimestamp() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy h:mm:ss a", Locale.US);
            return sdf.format(new Date());
        } catch (Exception e) {
            return "00/00/0000 00:00:00";
        }
    }

    /**
     * Format timestamp for notification display
     */
    private static String formatTimeForNotification(long timestamp) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.US);
            return sdf.format(new Date(timestamp));
        } catch (Exception e) {
            return "Unknown";
        }
    }

    /**
     * Get the ID of the currently scheduled auction
     */
    private static String getLastScheduledAuctionId(Context context) {
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
    private static void storeLastScheduledAuctionId(Context context, String auctionId) {
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            prefs.edit().putString("lastScheduledAuctionId", auctionId).apply();
            Log.d(TAG, "Stored lastScheduledAuctionId: " + auctionId);
        } catch (Exception e) {
            Log.w(TAG, "Error storing lastScheduledAuctionId: " + e.getMessage());
        }
    }

    /**
     * Get the timestamp of the currently scheduled auction's start time
     */
    private static long getLastScheduledAuctionStart(Context context) {
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            return prefs.getLong("lastScheduledAuctionStart", -1);
        } catch (Exception e) {
            Log.w(TAG, "Error reading lastScheduledAuctionStart: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Store the timestamp of the currently scheduled auction's start time
     */
    private static void storeLastScheduledAuctionStart(Context context, long startAt) {
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
    private static String formatTimestamp(long timestamp) {
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
    private static String getAuctionCurrencyName(String details) {
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
     * Example: "Pet $Flower Auction"
     */
    private static String formatAuctionDisplayName(String itemName, String details) {
        String currencyName = getAuctionCurrencyName(details);
        return itemName + " " + currencyName + " Auction";
    }

    /**
     * Convert sold marketplace listings to notification groups
     */
    private static List<NotificationGroup> convertSoldListingsToNotifications(List<MarketplaceListingsExtractor.SoldListing> soldListings) {
        List<NotificationGroup> groups = new ArrayList<>();
        
        for (MarketplaceListingsExtractor.SoldListing sold : soldListings) {
            try {
                NotificationGroup group = new NotificationGroup();
                group.category = "marketplace";
                group.name = sold.itemName;
                group.quantity = (int) sold.amount;  // Cast long to int
                group.details = sold.amount + " " + sold.itemName + " for " + String.format("%.4f", sold.sfl) + " SFL";
                group.earliestReadyTime = sold.fulfilledAt; // Use fulfilled timestamp as the "ready time"
                group.groupId = "marketplace_" + sold.listingId; // Unique ID based on listing ID
                
                groups.add(group);
                Log.d(TAG, "Created marketplace notification: " + sold.amount + " " + sold.itemName + " sold for " + sold.sfl + " SFL");
            } catch (Exception e) {
                Log.w(TAG, "Error converting sold listing to notification: " + e.getMessage());
            }
        }
        
        return groups;
    }
}
