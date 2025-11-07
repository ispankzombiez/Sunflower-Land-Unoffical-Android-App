package com.sunflowerland.mobile;

import android.app.Service;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.os.Handler;
import android.Manifest;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.ExistingPeriodicWorkPolicy;
import java.util.concurrent.TimeUnit;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sunflowerland.mobile.clustering.CategoryClusterer;
import com.sunflowerland.mobile.clustering.ClustererFactory;
import com.sunflowerland.mobile.clustering.NotificationGroup;
import com.sunflowerland.mobile.models.FarmItem;
import com.sunflowerland.mobile.models.SickAnimal;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class NotificationManagerService extends Service {
    private static final String TAG = "NotificationManagerService";
    private static final String CHANNEL_ID_ITEMS = "farm_items";
    private static final String API_BASE_URL = "https://api.sunflower-land.com/community/farms/";
    private volatile boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            Log.d(TAG, "Service started - onStartCommand called");
            
            // Check if this is a WorkManager-triggered call to process farm data
            if (intent != null && intent.getAction() != null && 
                intent.getAction().equals("com.sunflowerland.mobile.PROCESS_FARM_DATA")) {
                Log.d(TAG, "WorkManager triggered farm data processing");
                processFarmData();
                return START_NOT_STICKY;
            }
            
            if (!isRunning) {
                isRunning = true;
                createNotificationChannel();
                Log.d(TAG, "Notification channel created");
                
                // Get refresh time from settings (default 300 seconds = 5 minutes)
                SharedPreferences prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this);
                String refreshTimeStr = prefs.getString("refresh_time", "300");
                long refreshTimeMinutes = 5; // default
                try {
                    long refreshTimeSeconds = Long.parseLong(refreshTimeStr);
                    // Convert seconds to minutes (WorkManager minimum is 15 minutes)
                    refreshTimeMinutes = Math.max(15, refreshTimeSeconds / 60);
                    Log.d(TAG, "Refresh time from settings: " + refreshTimeSeconds + " seconds -> " + refreshTimeMinutes + " minutes for WorkManager");
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Invalid refresh_time value: " + refreshTimeStr + ", using default 5 minutes");
                }
                
                // Schedule WorkManager periodic task with dynamic refresh interval
                scheduleWorkManagerTask(refreshTimeMinutes);
                
                // Note: We no longer maintain a foreground notification
                // WorkManager handles all scheduling and execution
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onStartCommand", e);
            isRunning = false;
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    /**
     * Schedule the NotificationWorker to run periodically with the refresh interval from settings
     * @param refreshTimeMinutes Refresh interval in minutes (minimum 15 due to WorkManager constraints)
     */
    private void scheduleWorkManagerTask(long refreshTimeMinutes) {
        try {
            Log.d(TAG, "Scheduling WorkManager task with " + refreshTimeMinutes + " minute interval");
            
            // Create a periodic work request with the refresh interval
            PeriodicWorkRequest notificationWork = new PeriodicWorkRequest.Builder(
                    NotificationWorker.class,
                    refreshTimeMinutes,
                    TimeUnit.MINUTES
            )
            .addTag("farm_notification_work")
            .setInitialDelay(1, TimeUnit.MINUTES)  // Start first check after 1 minute
            .build();
            
            // Enqueue with KEEP policy - if already enqueued, keep the existing one
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                    "farm_notification_worker",
                    ExistingPeriodicWorkPolicy.KEEP,
                    notificationWork
            );
            
            Log.d(TAG, "✅ WorkManager task scheduled successfully with " + refreshTimeMinutes + " minute interval");
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling WorkManager task: " + e.getMessage(), e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");
        isRunning = false;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Not a bound service
        return null;
    }

    private void createNotificationChannel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Create channel for farm items
                NotificationChannel itemsChannel = new NotificationChannel(
                        CHANNEL_ID_ITEMS,
                        "Farm Items",
                        NotificationManager.IMPORTANCE_DEFAULT
                );
                itemsChannel.setDescription("Notifications for farm items ready to harvest/use");
                NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (manager != null) {
                    manager.createNotificationChannel(itemsChannel);
                    Log.d(TAG, "Items notification channel created");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating notification channel", e);
        }
    }

    /**
     * Main processing pipeline: API call -> Raw JSON save -> Extract data -> Cluster -> Create notifications
     */
    private void processFarmData() {
        try {
            Log.d(TAG, "=== Starting Farm Data Processing Pipeline ===");
            
            // Get farm ID and API key from SharedPreferences
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String farmId = prefs.getString("farm_id", "");
            String apiKey = prefs.getString("api_key", "");
            
            if (farmId == null || farmId.isEmpty() || apiKey == null || apiKey.isEmpty()) {
                Log.e(TAG, "Farm ID or API key not configured");
                writeErrorLog("Farm ID or API key not configured in settings");
                return;
            }
            
            // OPTIMIZATION: Check if we just fetched data within the last 30 seconds
            // This prevents redundant API calls on rapid re-opens
            long lastAPICallTime = prefs.getLong("lastAPICallTime", 0);
            long currentTime = System.currentTimeMillis();
            long timeSinceLastCall = currentTime - lastAPICallTime;
            
            if (timeSinceLastCall < 30000) {  // 30 seconds
                Log.d(TAG, "Skipping API call - last call was " + (timeSinceLastCall / 1000) + " seconds ago (cache window: 30 seconds)");
                return;
            }
            
            // Build API URL: https://api.sunflower-land.com/community/farms/{farmId}
            String apiUrl = API_BASE_URL + farmId;
            Log.d(TAG, "API URL: " + apiUrl);
            
            // Step 1: API Call and Raw JSON Save
            Log.d(TAG, "Step 1: Fetching raw JSON from API...");
            String rawJSON = APIClient.fetchRawJSON(apiUrl, apiKey);
            if (rawJSON == null || rawJSON.isEmpty()) {
                Log.e(TAG, "Failed to fetch raw JSON from API");
                writeErrorLog("API call failed - no response received");
                return;
            }
            
            // Record the time of this successful API call
            prefs.edit().putLong("lastAPICallTime", currentTime).apply();
            
            // Save raw JSON
            saveRawJSON(rawJSON);
            Log.d(TAG, "Step 1 Complete: Raw JSON saved");

            // Step 2: Parse JSON and extract all categories
            Log.d(TAG, "Step 2: Parsing JSON and extracting crops, fruits, greenhouse crops, resources, animals, cooking, composters, flowers, beehives, crop machine, sunstones & daily reset...");
            JsonObject parsedJson = JsonParser.parseString(rawJSON).getAsJsonObject();
            JsonObject farmObject = parsedJson.getAsJsonObject("farm");
            List<FarmItem> crops = CategoryExtractors.extractCrops(farmObject);
            List<FarmItem> fruits = CategoryExtractors.extractFruits(farmObject);
            List<FarmItem> greenhouseCrops = CategoryExtractors.extractGreenhouseCrops(farmObject);
            List<FarmItem> resources = CategoryExtractors.extractResources(farmObject);
            List<FarmItem> animals = CategoryExtractors.extractAnimals(farmObject);
            List<FarmItem> cooking = CategoryExtractors.extractCooking(farmObject);
            List<FarmItem> composters = CategoryExtractors.extractComposters(farmObject);
            List<FarmItem> flowers = CategoryExtractors.extractFlowers(farmObject);
            List<FarmItem> craftingBox = CategoryExtractors.extractCraftingBox(farmObject);
            List<FarmItem> beehives = CategoryExtractors.extractBeehives(farmObject);
            List<FarmItem> cropMachine = CategoryExtractors.extractCropMachine(farmObject);
            List<FarmItem> sunstones = CategoryExtractors.extractSunstones(farmObject);
            
            // Extract daily reset if enabled in preferences
            List<FarmItem> dailyReset = new ArrayList<>();
            if (prefs.getBoolean("category_daily_reset", true)) {
                dailyReset = CategoryExtractors.extractDailyReset(farmObject);
            }
            
            // Extract marketplace sold listings
            List<MarketplaceListingsExtractor.SoldListing> soldListings = new ArrayList<>();
            if (prefs.getBoolean("marketplace_listings_enabled", true)) {
                MarketplaceListingsExtractor marketplaceExtractor = new MarketplaceListingsExtractor(this);
                soldListings = marketplaceExtractor.extractSoldListings(farmObject);
            }
            
            // Extract floating island notifications (schedule and shop changes)
            List<FarmItem> floatingIsland = new ArrayList<>();
            if (prefs.getBoolean("floating_island_enabled", true)) {
                floatingIsland = CategoryExtractors.extractFloatingIsland(farmObject, this);
            }

            // Extract sick animals for health monitoring
            List<SickAnimal> sickAnimals = CategoryExtractors.extractSickAnimals(farmObject);

            Log.d(TAG, "Step 2 Complete: Extracted " + crops.size() + " crop(s), " + fruits.size() + " fruit(s), " + resources.size() + " resource(s), " + animals.size() + " animal(s), " + cooking.size() + " cooking item(s), " + composters.size() + " composter(s), " + flowers.size() + " flower(s), " + craftingBox.size() + " crafting box item(s), " + beehives.size() + " beehive item(s), " + cropMachine.size() + " crop machine item(s), " + sunstones.size() + " sunstone(s), " + dailyReset.size() + " daily reset(s), " + soldListings.size() + " sold listing(s), " + floatingIsland.size() + " floating island item(s), " + sickAnimals.size() + " sick animal(s)");

            // Step 3: Cluster crops, fruits, resources, animals, and cooking by category-specific rules
            Log.d(TAG, "Step 3: Clustering crops, fruits, resources, animals & cooking...");
            List<NotificationGroup> allGroups = new ArrayList<>();
            
            CategoryClusterer cropClusterer = ClustererFactory.getClusterer("crops", this);
            List<NotificationGroup> cropGroups = cropClusterer.cluster(crops);
            allGroups.addAll(cropGroups);
            Log.d(TAG, "  Crops: Created " + cropGroups.size() + " group(s)");
            
            CategoryClusterer fruitClusterer = ClustererFactory.getClusterer("fruits", this);
            List<NotificationGroup> fruitGroups = fruitClusterer.cluster(fruits);
            allGroups.addAll(fruitGroups);
            Log.d(TAG, "  Fruits: Created " + fruitGroups.size() + " group(s)");
            
            CategoryClusterer greenhouseCropClusterer = ClustererFactory.getClusterer("greenhouse_crops", this);
            List<NotificationGroup> greenhouseCropGroups = greenhouseCropClusterer.cluster(greenhouseCrops);
            allGroups.addAll(greenhouseCropGroups);
            Log.d(TAG, "  Greenhouse Crops: Created " + greenhouseCropGroups.size() + " group(s)");
            
            CategoryClusterer resourceClusterer = ClustererFactory.getClusterer("resources", this);
            List<NotificationGroup> resourceGroups = resourceClusterer.cluster(resources);
            allGroups.addAll(resourceGroups);
            Log.d(TAG, "  Resources: Created " + resourceGroups.size() + " group(s)");
            
            CategoryClusterer animalClusterer = ClustererFactory.getClusterer("animals", this);
            List<NotificationGroup> animalGroups = animalClusterer.cluster(animals);
            allGroups.addAll(animalGroups);
            Log.d(TAG, "  Animals: Created " + animalGroups.size() + " group(s)");
            
            CategoryClusterer cookingClusterer = ClustererFactory.getClusterer("cooking", this);
            List<NotificationGroup> cookingGroups = cookingClusterer.cluster(cooking);
            allGroups.addAll(cookingGroups);
            Log.d(TAG, "  Cooking: Created " + cookingGroups.size() + " group(s)");
            
            CategoryClusterer composterClusterer = ClustererFactory.getClusterer("composters", this);
            List<NotificationGroup> composterGroups = composterClusterer.cluster(composters);
            allGroups.addAll(composterGroups);
            Log.d(TAG, "  Composters: Created " + composterGroups.size() + " group(s)");
            
            CategoryClusterer flowerClusterer = ClustererFactory.getClusterer("flowers", this);
            List<NotificationGroup> flowerGroups = flowerClusterer.cluster(flowers);
            allGroups.addAll(flowerGroups);
            Log.d(TAG, "  Flowers: Created " + flowerGroups.size() + " group(s)");
            
            CategoryClusterer craftingBoxClusterer = ClustererFactory.getClusterer("crafting", this);
            List<NotificationGroup> craftingBoxGroups = craftingBoxClusterer.cluster(craftingBox);
            allGroups.addAll(craftingBoxGroups);
            Log.d(TAG, "  Crafting Box: Created " + craftingBoxGroups.size() + " group(s)");
            
            CategoryClusterer beehiveClusterer = ClustererFactory.getClusterer("beehive", this);
            List<NotificationGroup> beehiveGroups = beehiveClusterer.cluster(beehives);
            allGroups.addAll(beehiveGroups);
            Log.d(TAG, "  Beehives: Created " + beehiveGroups.size() + " group(s)");
            
            CategoryClusterer cropMachineClusterer = ClustererFactory.getClusterer("cropMachine", this);
            List<NotificationGroup> cropMachineGroups = cropMachineClusterer.cluster(cropMachine);
            
            // Check if crop machine notifications are enabled
            boolean cropMachineEnabled = prefs.getBoolean("category_crop_machine", true);
            
            if (cropMachineEnabled) {
                allGroups.addAll(cropMachineGroups);
                Log.d(TAG, "  Crop Machine: Created " + cropMachineGroups.size() + " group(s)");
            } else {
                Log.d(TAG, "  Crop Machine: Notifications disabled - skipping " + cropMachineGroups.size() + " group(s)");
            }
            
            CategoryClusterer sunstoneClusterer = ClustererFactory.getClusterer("sunstones", this);
            List<NotificationGroup> sunstoneGroups = sunstoneClusterer.cluster(sunstones);
            allGroups.addAll(sunstoneGroups);
            Log.d(TAG, "  Sunstones: Created " + sunstoneGroups.size() + " group(s)");
            
            CategoryClusterer dailyResetClusterer = ClustererFactory.getClusterer("dailyReset", this);
            List<NotificationGroup> dailyResetGroups = dailyResetClusterer.cluster(dailyReset);
            allGroups.addAll(dailyResetGroups);
            Log.d(TAG, "  Daily Reset: Created " + dailyResetGroups.size() + " group(s)");
            
            // Convert sold listings to notification groups
            List<NotificationGroup> marketplaceGroups = convertSoldListingsToNotifications(soldListings);
            allGroups.addAll(marketplaceGroups);
            Log.d(TAG, "  Marketplace: Created " + marketplaceGroups.size() + " group(s)");
            
            // Cluster floating island notifications
            CategoryClusterer floatingIslandClusterer = ClustererFactory.getClusterer("floating_island", this);
            List<NotificationGroup> floatingIslandGroups = floatingIslandClusterer.cluster(floatingIsland);
            allGroups.addAll(floatingIslandGroups);
            Log.d(TAG, "  Floating Island: Created " + floatingIslandGroups.size() + " group(s)");

            // Handle sick animal notifications with state tracking
            if (prefs.getBoolean("category_animal_sick", true)) {
                SickAnimalTracker tracker = new SickAnimalTracker(this);
                List<SickAnimal> newlySickAnimals = tracker.getNewlySickAnimals(sickAnimals);
                
                if (!newlySickAnimals.isEmpty()) {
                    NotificationGroup sickAnimalGroup = SickAnimalNotificationExtractor.createSickAnimalNotification(newlySickAnimals);
                    if (sickAnimalGroup != null) {
                        allGroups.add(sickAnimalGroup);
                        Log.d(TAG, "  Sick Animals: Created 1 group with " + newlySickAnimals.size() + " newly sick animal(s)");
                    }
                } else {
                    Log.d(TAG, "  Sick Animals: No newly sick animals detected");
                }
                
                // Always update the tracker with current state for next run
                tracker.saveCurrentState(sickAnimals);
            } else {
                Log.d(TAG, "  Sick Animals: Notifications disabled");
            }
            
            Log.d(TAG, "Step 3 Complete: Created " + allGroups.size() + " total notification group(s)");

            // Step 4: Write processing log
            Log.d(TAG, "Step 4: Writing processing log...");
            writeProcessingLog(crops, fruits, greenhouseCrops, resources, animals, cooking, composters, flowers, craftingBox, beehives, cropMachine, sunstones, dailyReset, floatingIsland);
            Log.d(TAG, "Step 4 Complete: Processing log written");

            // Step 5: Save processed JSON
            Log.d(TAG, "Step 5: Saving processed JSON...");
            saveProcessedJSON(crops, fruits, resources, animals, cooking, composters, flowers, craftingBox, beehives, cropMachine, sunstones, dailyReset);
            Log.d(TAG, "Step 5 Complete: Processed JSON saved");

            // Step 6: Schedule AlarmManager intents for system-managed notifications
            Log.d(TAG, "Step 6: Scheduling alarms with system for " + allGroups.size() + " group(s)...");
            AlarmScheduler scheduler = new AlarmScheduler(this);
            
            // CRITICAL: Cancel all old pending alarms from the system
            // This prevents stale notifications from firing (e.g., old "tree ready" from days ago)
            // Old alarms persist in the system even after the app is updated
            Log.d(TAG, "Cancelling all old pending alarms to prevent stale notifications...");
            scheduler.cancelAllPendingAlarms();
            Log.d(TAG, "All old pending alarms cancelled");
            
            // Clear old scheduled tracking so we re-evaluate all alarms on this run
            // This prevents past readyTimes from being skipped due to cached IDs
            scheduler.clearScheduledTracking();
            Log.d(TAG, "Cleared scheduled tracking to re-evaluate all alarms on this run");
            
            // Schedule only the current/future notifications
            scheduler.scheduleNotificationAlarms(allGroups);
            Log.d(TAG, "Step 6 Complete: Alarms scheduled with system");

            // Step 7: Write scheduled notifications log (plain English readout)
            Log.d(TAG, "Step 7: Writing scheduled notifications log...");
            writeScheduledNotificationsLog(allGroups);
            Log.d(TAG, "Step 7 Complete: Notifications log written");

            Log.d(TAG, "=== Farm Data Processing Pipeline Complete ===");
        } catch (Exception e) {
            Log.e(TAG, "Error in processFarmData: " + e.getMessage(), e);
            writeErrorLog("Processing pipeline failed: " + e.getMessage());
        }
    }

    /**
     * Saves raw JSON response to farm_api_raw.json (overwrites previous)
     */
    private void saveRawJSON(String rawJSON) {
        try {
            File file = new File(getFilesDir(), "farm_api_raw.json");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
                writer.write(rawJSON);
                writer.flush();
            }
            Log.d(TAG, "Raw JSON saved: " + file.getAbsolutePath() + " (" + rawJSON.length() + " bytes)");
        } catch (IOException e) {
            Log.e(TAG, "Error saving raw JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Writes processing log with summary of extracted items
     */
    private void writeProcessingLog(List<FarmItem> crops, List<FarmItem> fruits, List<FarmItem> greenhouseCrops, List<FarmItem> resources, List<FarmItem> animals, List<FarmItem> cooking, List<FarmItem> composters, List<FarmItem> flowers, List<FarmItem> craftingBox, List<FarmItem> beehives, List<FarmItem> cropMachine, List<FarmItem> sunstones, List<FarmItem> dailyReset, List<FarmItem> floatingIsland) {
        try {
            File file = new File(getFilesDir(), "processing_log.txt");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
                // Write header with timestamp
                String timestamp = getCurrentTimestamp();
                writer.write("=== Processing Log ===\n");
                writer.write("Processed at: " + timestamp + "\n");
                writer.write("---\n\n");

                // Write crops summary
                if (crops.isEmpty()) {
                    writer.write("Crops: No crops found\n");
                } else {
                    StringBuilder cropsLine = new StringBuilder("Crops: ");
                    for (int i = 0; i < crops.size(); i++) {
                        FarmItem crop = crops.get(i);
                        String formattedTime = CategoryExtractors.formatTimestamp(crop.getTimestamp());
                        cropsLine.append(crop.getAmount()).append(" ").append(crop.getName())
                                .append(" [").append(formattedTime).append("]");
                        if (i < crops.size() - 1) {
                            cropsLine.append(", ");
                        }
                    }
                    writer.write(cropsLine.toString());
                    writer.write("\n");
                }

                // Write fruits summary
                if (fruits.isEmpty()) {
                    writer.write("Fruits: No fruits found\n");
                } else {
                    StringBuilder fruitsLine = new StringBuilder("Fruits: ");
                    for (int i = 0; i < fruits.size(); i++) {
                        FarmItem fruit = fruits.get(i);
                        String formattedTime = CategoryExtractors.formatTimestamp(fruit.getTimestamp());
                        fruitsLine.append(fruit.getAmount()).append(" ").append(fruit.getName())
                                .append(" [").append(formattedTime).append("]");
                        if (i < fruits.size() - 1) {
                            fruitsLine.append(", ");
                        }
                    }
                    writer.write(fruitsLine.toString());
                    writer.write("\n");
                }

                // Write greenhouse crops summary
                if (greenhouseCrops.isEmpty()) {
                    writer.write("Greenhouse Crops: No greenhouse crops found\n");
                } else {
                    StringBuilder greenhouseCropsLine = new StringBuilder("Greenhouse Crops: ");
                    for (int i = 0; i < greenhouseCrops.size(); i++) {
                        FarmItem crop = greenhouseCrops.get(i);
                        String formattedTime = CategoryExtractors.formatTimestamp(crop.getTimestamp());
                        greenhouseCropsLine.append(crop.getAmount()).append(" ").append(crop.getName())
                                .append(" [").append(formattedTime).append("]");
                        if (i < greenhouseCrops.size() - 1) {
                            greenhouseCropsLine.append(", ");
                        }
                    }
                    writer.write(greenhouseCropsLine.toString());
                    writer.write("\n");
                }

                // Write resources summary
                if (resources.isEmpty()) {
                    writer.write("Resources: No resources found\n");
                } else {
                    StringBuilder resourcesLine = new StringBuilder("Resources: ");
                    for (int i = 0; i < resources.size(); i++) {
                        FarmItem resource = resources.get(i);
                        String formattedTime = CategoryExtractors.formatTimestamp(resource.getTimestamp());
                        resourcesLine.append(resource.getAmount()).append(" ").append(resource.getName())
                                .append(" [").append(formattedTime).append("]");
                        if (i < resources.size() - 1) {
                            resourcesLine.append(", ");
                        }
                    }
                    writer.write(resourcesLine.toString());
                    writer.write("\n");
                }

                // Write animals summary
                if (animals.isEmpty()) {
                    writer.write("Animals: No animals found\n");
                } else {
                    StringBuilder animalsLine = new StringBuilder("Animals: ");
                    for (int i = 0; i < animals.size(); i++) {
                        FarmItem animal = animals.get(i);
                        String formattedTime = CategoryExtractors.formatTimestamp(animal.getTimestamp());
                        animalsLine.append(animal.getAmount()).append(" ").append(animal.getName())
                                .append(" [").append(formattedTime).append("]");
                        if (i < animals.size() - 1) {
                            animalsLine.append(", ");
                        }
                    }
                    writer.write(animalsLine.toString());
                    writer.write("\n");
                }

                // Write cooking summary
                if (cooking.isEmpty()) {
                    writer.write("Cooking: No cooking items found\n");
                } else {
                    StringBuilder cookingLine = new StringBuilder("Cooking: ");
                    for (int i = 0; i < cooking.size(); i++) {
                        FarmItem item = cooking.get(i);
                        String formattedTime = CategoryExtractors.formatTimestamp(item.getTimestamp());
                        cookingLine.append(item.getAmount()).append(" ").append(item.getName())
                                .append(" [").append(formattedTime).append("]");
                        if (i < cooking.size() - 1) {
                            cookingLine.append(", ");
                        }
                    }
                    writer.write(cookingLine.toString());
                    writer.write("\n");
                }

                // Write composters summary
                if (composters.isEmpty()) {
                    writer.write("Composters: No composters found\n");
                } else {
                    StringBuilder compostersLine = new StringBuilder("Composters: ");
                    for (int i = 0; i < composters.size(); i++) {
                        FarmItem composter = composters.get(i);
                        String formattedTime = CategoryExtractors.formatTimestamp(composter.getTimestamp());
                        compostersLine.append(composter.getAmount()).append(" items in ").append(composter.getName())
                                .append(" [").append(formattedTime).append("]");
                        if (i < composters.size() - 1) {
                            compostersLine.append(", ");
                        }
                    }
                    writer.write(compostersLine.toString());
                    writer.write("\n");
                }

                // Write flowers summary
                if (flowers.isEmpty()) {
                    writer.write("Flowers: No flowers found\n");
                } else {
                    StringBuilder flowersLine = new StringBuilder("Flowers: ");
                    for (int i = 0; i < flowers.size(); i++) {
                        FarmItem flower = flowers.get(i);
                        String formattedTime = CategoryExtractors.formatTimestamp(flower.getTimestamp());
                        flowersLine.append(flower.getAmount()).append(" ").append(flower.getName())
                                .append(" [").append(formattedTime).append("]");
                        if (i < flowers.size() - 1) {
                            flowersLine.append(", ");
                        }
                    }
                    writer.write(flowersLine.toString());
                    writer.write("\n");
                }

                // Write crafting box summary
                if (craftingBox.isEmpty()) {
                    writer.write("Crafting Box: No items found\n");
                } else {
                    StringBuilder craftingBoxLine = new StringBuilder("Crafting Box: ");
                    for (int i = 0; i < craftingBox.size(); i++) {
                        FarmItem item = craftingBox.get(i);
                        String formattedTime = CategoryExtractors.formatTimestamp(item.getTimestamp());
                        craftingBoxLine.append(item.getAmount()).append(" ").append(item.getName())
                                .append(" [").append(formattedTime).append("]");
                        if (i < craftingBox.size() - 1) {
                            craftingBoxLine.append(", ");
                        }
                    }
                    writer.write(craftingBoxLine.toString());
                    writer.write("\n");
                }

                // Write beehives summary
                if (beehives.isEmpty()) {
                    writer.write("Beehives: No alerts found\n");
                } else {
                    StringBuilder beehivesLine = new StringBuilder("Beehives: ");
                    for (int i = 0; i < beehives.size(); i++) {
                        FarmItem item = beehives.get(i);
                        String formattedTime = CategoryExtractors.formatTimestamp(item.getTimestamp());
                        beehivesLine.append(item.getCategory()).append(" - ").append(item.getName())
                                .append(" [").append(formattedTime).append("]");
                        if (i < beehives.size() - 1) {
                            beehivesLine.append(", ");
                        }
                    }
                    writer.write(beehivesLine.toString());
                    writer.write("\n");
                }

                // Write crop machine summary
                if (cropMachine.isEmpty()) {
                    writer.write("Crop Machine: No items found\n");
                } else {
                    StringBuilder cropMachineLineBuilder = new StringBuilder("Crop Machine: ");
                    for (int i = 0; i < cropMachine.size(); i++) {
                        FarmItem item = cropMachine.get(i);
                        String formattedTime = CategoryExtractors.formatTimestamp(item.getTimestamp());
                        cropMachineLineBuilder.append(item.getAmount()).append(" seeds of ").append(item.getName())
                                .append(" [").append(formattedTime).append("]");
                        if (i < cropMachine.size() - 1) {
                            cropMachineLineBuilder.append(", ");
                        }
                    }
                    writer.write(cropMachineLineBuilder.toString());
                    writer.write("\n");
                }

                // Write sunstones summary
                if (sunstones.isEmpty()) {
                    writer.write("Sunstones: No items found\n");
                } else {
                    StringBuilder sunstonesLine = new StringBuilder("Sunstones: ");
                    for (int i = 0; i < sunstones.size(); i++) {
                        FarmItem item = sunstones.get(i);
                        String formattedTime = CategoryExtractors.formatTimestamp(item.getTimestamp());
                        sunstonesLine.append(item.getAmount()).append(" ").append(item.getName())
                                .append(" [").append(formattedTime).append("]");
                        if (i < sunstones.size() - 1) {
                            sunstonesLine.append(", ");
                        }
                    }
                    writer.write(sunstonesLine.toString());
                    writer.write("\n");
                }

                // Write daily reset summary
                if (dailyReset.isEmpty()) {
                    writer.write("Daily Reset: No reset scheduled\n");
                } else {
                    StringBuilder dailyResetLine = new StringBuilder("Daily Reset: ");
                    for (int i = 0; i < dailyReset.size(); i++) {
                        FarmItem item = dailyReset.get(i);
                        String formattedTime = CategoryExtractors.formatTimestamp(item.getTimestamp());
                        dailyResetLine.append("Next reset at ").append(formattedTime).append(" UTC");
                        if (i < dailyReset.size() - 1) {
                            dailyResetLine.append(", ");
                        }
                    }
                    writer.write(dailyResetLine.toString());
                    writer.write("\n");
                }

                // Write floating island summary
                if (floatingIsland.isEmpty()) {
                    writer.write("Floating Island: No events scheduled\n");
                } else {
                    StringBuilder floatingIslandLine = new StringBuilder("Floating Island: ");
                    for (int i = 0; i < floatingIsland.size(); i++) {
                        FarmItem item = floatingIsland.get(i);
                        String formattedTime = CategoryExtractors.formatTimestamp(item.getTimestamp());
                        floatingIslandLine.append(item.getName())
                                .append(" [").append(formattedTime).append("]");
                        if (i < floatingIsland.size() - 1) {
                            floatingIslandLine.append(", ");
                        }
                    }
                    writer.write(floatingIslandLine.toString());
                    writer.write("\n");
                }

                writer.flush();
            }
            Log.d(TAG, "Processing log saved: " + file.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Error writing processing log: " + e.getMessage(), e);
        }
    }

    /**
     * Saves processed items as JSON (chronologically ordered, earliest first)
     */
    private void saveProcessedJSON(List<FarmItem> crops, List<FarmItem> fruits, List<FarmItem> resources, List<FarmItem> animals, List<FarmItem> cooking, List<FarmItem> composters, List<FarmItem> flowers, List<FarmItem> craftingBox, List<FarmItem> beehives, List<FarmItem> cropMachine, List<FarmItem> sunstones, List<FarmItem> dailyReset) {
        try {
            File file = new File(getFilesDir(), "future_notifications.json");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
                // Combine all items
                List<FarmItem> allItems = new ArrayList<>();
                allItems.addAll(crops);
                allItems.addAll(fruits);
                allItems.addAll(resources);
                allItems.addAll(animals);
                allItems.addAll(cooking);
                allItems.addAll(composters);
                allItems.addAll(flowers);
                allItems.addAll(craftingBox);
                allItems.addAll(beehives);
                allItems.addAll(cropMachine);
                allItems.addAll(sunstones);
                allItems.addAll(dailyReset);
                
                // Sort by timestamp
                allItems.sort((a, b) -> Long.compare(a.getTimestamp(), b.getTimestamp()));
                
                Gson gson = new Gson();
                String json = gson.toJson(allItems);
                writer.write(json);
                writer.flush();
            }
            Log.d(TAG, "Processed JSON saved: " + file.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Error saving processed JSON: " + e.getMessage(), e);
        }
    }

    /**
     * Logs alarm scheduling summary for debugging purposes
     * (AlarmScheduler handles actual system notification delivery)
     */
    private void logAlarmScheduling(List<NotificationGroup> groups) {
        try {
            File logFile = new File(getFilesDir(), "notification_summary.log");
            
            try (BufferedWriter logWriter = new BufferedWriter(new FileWriter(logFile, true))) {
                // Write header
                logWriter.write("\n=== Alarm Scheduling Summary ===\n");
                logWriter.write("Generated at: " + getCurrentTimestamp() + "\n");
                logWriter.write("Note: Notifications are delivered by Android system at readyTime\n");
                logWriter.write("(not by this app - even if app is closed)\n");
                logWriter.write("---\n");

                int alarmsScheduled = 0;

                // Log each scheduled alarm
                for (NotificationGroup group : groups) {
                    String formattedTime = formatTimeForNotification(group.earliestReadyTime);
                    String logEntry = "[ALARM SCHEDULED] " + group.quantity + " " + group.name + 
                                    " - will notify at " + formattedTime + "\n";
                    logWriter.write(logEntry);
                    alarmsScheduled++;
                }

                logWriter.flush();
                Log.d(TAG, "Alarm scheduling summary logged: " + alarmsScheduled + " alarm(s) scheduled");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error logging alarm scheduling: " + e.getMessage(), e);
        }
    }

    /**
     * Writes error log
     */
    private void writeErrorLog(String errorMessage) {
        try {
            File file = new File(getFilesDir(), "notification_summary.log");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
                writer.write("=== Error Log ===\n");
                writer.write("Error at: " + getCurrentTimestamp() + "\n");
                writer.write("Message: " + errorMessage + "\n");
                writer.flush();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error writing error log: " + e.getMessage(), e);
        }
    }

    /**
     * Returns current timestamp in MM/DD HH:MM:SS format
     */
    private String getCurrentTime() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm:ss", Locale.US);
            return sdf.format(new Date());
        } catch (Exception e) {
            return "00/00 00:00:00";
        }
    }

    /**
     * Returns current timestamp in full format
     */
    private String getCurrentTimestamp() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy h:mm:ss a", Locale.US);
            return sdf.format(new Date());
        } catch (Exception e) {
            return "00/00/0000 00:00:00";
        }
    }

    /**
     * Formats timestamp for notification display (HH:MM AM/PM format)
     */
    private String formatTimeForNotification(long timestamp) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.US);
            return sdf.format(new Date(timestamp));
        } catch (Exception e) {
            return "Unknown";
        }
    }

    /**
     * Writes currently scheduled notifications in plain English format
     * Sorted chronologically (soonest first)
     * Completely overwrites file (no appending)
     */
    private void writeScheduledNotificationsLog(List<NotificationGroup> groups) {
        try {
            // Sort groups by earliest ready time (soonest first)
            List<NotificationGroup> sortedGroups = new ArrayList<>(groups);
            sortedGroups.sort((a, b) -> Long.compare(a.earliestReadyTime, b.earliestReadyTime));
            
            File file = new File(getFilesDir(), "notification_summary.log");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
                // Write header
                writer.write("=== Currently Scheduled Notifications ===\n");
                writer.write("Generated at: " + getCurrentTimestamp() + "\n");
                writer.write("---\n\n");
                
                if (sortedGroups.isEmpty()) {
                    writer.write("(No upcoming notifications scheduled)\n");
                } else {
                    long currentTime = System.currentTimeMillis();
                    
                    for (NotificationGroup group : sortedGroups) {
                        long timeRemaining = group.earliestReadyTime - currentTime;
                        String readyTime = formatTimeForNotification(group.earliestReadyTime);
                        
                        // Calculate friendly time remaining
                        String timeRemainingStr;
                        if (timeRemaining <= 0) {
                            timeRemainingStr = "now";
                        } else {
                            long minutes = timeRemaining / (60 * 1000);
                            long hours = minutes / 60;
                            long remainingMinutes = minutes % 60;
                            
                            if (hours > 0) {
                                timeRemainingStr = "in " + hours + "h " + remainingMinutes + "m";
                            } else {
                                timeRemainingStr = "in " + minutes + "m";
                            }
                        }
                        
                        // Determine notification type for animals
                        String displayCategory = group.category;
                        if ("animals".equals(group.category) && group.groupId != null && group.groupId.contains("animal love at")) {
                            displayCategory = "animals (LOVE)";
                        } else if ("animals".equals(group.category)) {
                            displayCategory = "animals (AWAKE)";
                        }
                        
                        // Format: [Category] Quantity Name - Ready at Time (time remaining)
                        String logEntry = String.format("[%s] %d %s - Ready at %s (%s)\n",
                                displayCategory, group.quantity, group.name, readyTime, timeRemainingStr);
                        writer.write(logEntry);
                    }
                }
                
                writer.flush();
            }
            Log.d(TAG, "Scheduled notifications log written: " + file.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Error writing scheduled notifications log: " + e.getMessage(), e);
        }
    }
    
    /**
     * Convert sold marketplace listings to notification groups
     */
    private List<NotificationGroup> convertSoldListingsToNotifications(List<MarketplaceListingsExtractor.SoldListing> soldListings) {
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

    /**
     * Static wrapper for processFarmData that can be called from WorkManager worker
     * Creates a temporary instance and calls the instance method
     */
    public static void processFarmDataStatic(Context context) {
        try {
            Log.d("NotificationWorker", "Calling processFarmData via static wrapper");
            // Start the service with an intent to trigger onStartCommand
            // This ensures proper Android lifecycle initialization
            Intent intent = new Intent(context, NotificationManagerService.class);
            intent.setAction("com.sunflowerland.mobile.PROCESS_FARM_DATA");
            context.startService(intent);
            Log.d("NotificationWorker", "NotificationManagerService started successfully");
        } catch (Exception e) {
            Log.e("NotificationWorker", "Error in static processFarmData: " + e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}


