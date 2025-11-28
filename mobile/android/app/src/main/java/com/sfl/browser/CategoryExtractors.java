package com.sfl.browser;

import android.util.Log;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sfl.browser.models.FarmItem;
import com.sfl.browser.models.SickAnimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CategoryExtractors {
    // Map to store flower bed finish times (id -> finishTime)
    private static Map<String, Long> flowerBedFinishTimes = new HashMap<>();
    private static final String TAG = "CategoryExtractors";

    /**
     * Extracts crops from raw API response
     * Navigates to farm.crops object (which contains plots with IDs as keys)
     * Each plot contains a nested "crop" object with name and plantedAt timestamp
     * 
     * Calculation: readyTime = plantedAt + Constants.CROP_GROWTH_TIMES.get(cropName)
     * 
     * @param farmData JsonObject farm object from API response (already extracted from root)
     * @return List<FarmItem> sorted by readyTime ascending (earliest first)
     */
    public static List<FarmItem> extractCrops(JsonObject farmData) {
        Log.d(TAG, "Extracting crops...");
        List<FarmItem> crops = new ArrayList<>();

        try {
            if (farmData == null || !farmData.has("crops")) {
                Log.w(TAG, "No crops data found in farm object");
                return crops;
            }

            // Get crops object (contains plots with numeric string keys: "1", "2", "3", etc.)
            JsonObject cropsObject = farmData.getAsJsonObject("crops");
            Log.d(TAG, "Found " + cropsObject.size() + " crop plot(s)");

            // Iterate over each crop plot by key
            for (String cropPlotId : cropsObject.keySet()) {
                try {
                    JsonObject plotData = cropsObject.getAsJsonObject(cropPlotId);
                    
                    // Crop data is nested inside a "crop" object
                    if (!plotData.has("crop")) {
                        Log.w(TAG, "Plot " + cropPlotId + " missing 'crop' field");
                        continue;
                    }
                    
                    JsonObject cropData = plotData.getAsJsonObject("crop");
                    
                    // Extract crop name
                    String name = getJsonString(cropData, "name");
                    if (name == null || name.isEmpty()) {
                        Log.w(TAG, "Plot " + cropPlotId + " missing crop name");
                        continue;
                    }

                    // Extract plantedAt timestamp (in milliseconds)
                    long plantedAt = 0;
                    if (cropData.has("plantedAt") && !cropData.get("plantedAt").isJsonNull()) {
                        plantedAt = cropData.get("plantedAt").getAsLong();
                    } else {
                        Log.w(TAG, "Plot " + cropPlotId + " (" + name + ") missing plantedAt");
                        continue;
                    }

                    // Calculate readyTime = plantedAt + baseTime from Constants
                    Long baseTime = Constants.CROP_GROWTH_TIMES.get(name);
                    if (baseTime == null || baseTime <= 0) {
                        Log.w(TAG, "Plot " + cropPlotId + ": Unknown crop '" + name + "' or invalid baseTime");
                        continue;
                    }
                    
                    long readyTime = plantedAt + baseTime;
                    
                    // Only include crops that will be ready in the future (not already passed)
                    long currentTime = System.currentTimeMillis();
                    if (readyTime <= currentTime) {
                        Log.d(TAG, "Skipping crop: 1 " + name + 
                              " (ready=" + formatTimestamp(readyTime) + 
                              " - already passed)");
                        continue;
                    }
                    
                    // Each crop plot counts as amount=1
                    FarmItem item = new FarmItem("crops", name, 1, readyTime);
                    crops.add(item);
                    Log.d(TAG, "Added crop: 1 " + name + 
                          " (planted=" + formatTimestamp(plantedAt) + 
                          ", baseTime=" + (baseTime / 1000) + "s" +
                          ", ready=" + formatTimestamp(readyTime) + ")");
                    
                } catch (Exception e) {
                    Log.w(TAG, "Error processing crop plot " + cropPlotId + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting crops: " + e.getMessage(), e);
        }

        // Sort by timestamp ascending (earliest first)
        sortByTimestamp(crops);
        Log.d(TAG, "Extracted " + crops.size() + " crop(s)");
        return crops;
    }

    /**
     * Extracts fruits from raw API response
     * Navigates to farm.fruitPatches object (which contains patches with IDs as keys)
     * Each patch contains a nested "fruit" object with name, plantedAt, and harvestedAt timestamps
     * 
     * Calculation: readyTime = max(plantedAt, harvestedAt) + Constants.FRUIT_GROWTH_TIMES.get(fruitName)
     * If harvestedAt is later than plantedAt, use harvestedAt (fruit was already harvested once)
     * Otherwise use plantedAt (first harvest)
     * 
     * @param farmData JsonObject farm object from API response (already extracted from root)
     * @return List<FarmItem> sorted by readyTime ascending (earliest first), only future items
     */
    public static List<FarmItem> extractFruits(JsonObject farmData) {
        Log.d(TAG, "Extracting fruits...");
        List<FarmItem> fruits = new ArrayList<>();

        try {
            if (farmData == null || !farmData.has("fruitPatches")) {
                Log.w(TAG, "No fruit patches data found in farm object");
                return fruits;
            }

            // Get fruitPatches object (contains patches with IDs as keys)
            JsonObject patchesObject = farmData.getAsJsonObject("fruitPatches");
            Log.d(TAG, "Found " + patchesObject.size() + " fruit patch(es)");

            // Iterate over each fruit patch by key
            for (String patchId : patchesObject.keySet()) {
                try {
                    JsonObject patchData = patchesObject.getAsJsonObject(patchId);
                    
                    // Fruit data is nested inside a "fruit" object
                    if (!patchData.has("fruit")) {
                        Log.w(TAG, "Patch " + patchId + " missing 'fruit' field");
                        continue;
                    }
                    
                    JsonObject fruitData = patchData.getAsJsonObject("fruit");
                    
                    // Extract fruit name
                    String name = getJsonString(fruitData, "name");
                    if (name == null || name.isEmpty()) {
                        Log.w(TAG, "Patch " + patchId + " missing fruit name");
                        continue;
                    }

                    // Extract plantedAt timestamp (in milliseconds)
                    long plantedAt = 0;
                    if (fruitData.has("plantedAt") && !fruitData.get("plantedAt").isJsonNull()) {
                        plantedAt = fruitData.get("plantedAt").getAsLong();
                    } else {
                        Log.w(TAG, "Patch " + patchId + " (" + name + ") missing plantedAt");
                        continue;
                    }

                    // Extract harvestedAt timestamp (may be 0 if never harvested)
                    long harvestedAt = 0;
                    if (fruitData.has("harvestedAt") && !fruitData.get("harvestedAt").isJsonNull()) {
                        harvestedAt = fruitData.get("harvestedAt").getAsLong();
                    }

                    // Use the later of plantedAt or harvestedAt as the base time
                    // If fruit was harvested, we calculate next ready time from harvestedAt
                    // Otherwise from plantedAt
                    long baseTimestamp = Math.max(plantedAt, harvestedAt);
                    
                    // Calculate readyTime = baseTimestamp + baseTime from Constants
                    Long baseTime = Constants.FRUIT_GROWTH_TIMES.get(name);
                    if (baseTime == null || baseTime <= 0) {
                        Log.w(TAG, "Patch " + patchId + ": Unknown fruit '" + name + "' or invalid baseTime");
                        continue;
                    }
                    
                    long readyTime = baseTimestamp + baseTime;
                    
                    // Only include fruits that will be ready in the future (not already passed)
                    long currentTime = System.currentTimeMillis();
                    if (readyTime <= currentTime) {
                        Log.d(TAG, "Skipping fruit: 1 " + name + 
                              " (ready=" + formatTimestamp(readyTime) + 
                              " - already passed)");
                        continue;
                    }
                    
                    // Each fruit patch counts as amount=1
                    FarmItem item = new FarmItem("fruits", name, 1, readyTime);
                    fruits.add(item);
                    Log.d(TAG, "Added fruit: 1 " + name + 
                          " (base=" + formatTimestamp(baseTimestamp) + 
                          ", baseTime=" + (baseTime / 1000) + "s" +
                          ", ready=" + formatTimestamp(readyTime) + ")");
                    
                } catch (Exception e) {
                    Log.w(TAG, "Error processing fruit patch " + patchId + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting fruits: " + e.getMessage(), e);
        }

        // Sort by timestamp ascending (earliest first)
        sortByTimestamp(fruits);
        Log.d(TAG, "Extracted " + fruits.size() + " fruit(s)");
        return fruits;
    }

    /**
     * Extracts greenhouse crops from raw API response
     * 
     * Greenhouse crops (Olive, Rice, Grape) are stored in farm.greenhouse object
     * Similar structure to fruitPatches: contains patches with IDs as keys
     * Each patch contains a nested "crop" object with name, plantedAt, and harvestedAt timestamps
     * 
     * Calculation: readyTime = max(plantedAt, harvestedAt) + Constants.GREENHOUSE_CROP_GROWTH_TIMES.get(cropName)
     * 
     * @param farmData JsonObject farm object from API response
     * @return List<FarmItem> sorted by readyTime ascending (earliest first), only future items
     */
    public static List<FarmItem> extractGreenhouseCrops(JsonObject farmData) {
        Log.d(TAG, "Extracting greenhouse crops...");
        List<FarmItem> greenhouseCrops = new ArrayList<>();

        try {
            if (farmData == null || !farmData.has("greenhouse")) {
                Log.d(TAG, "No greenhouse data found in farm object");
                return greenhouseCrops;
            }

            // Get greenhouse object
            JsonObject greenhouseObject = farmData.getAsJsonObject("greenhouse");
            
            // Check if pots exist
            if (!greenhouseObject.has("pots")) {
                Log.d(TAG, "No pots found in greenhouse object");
                return greenhouseCrops;
            }
            
            JsonObject potsObject = greenhouseObject.getAsJsonObject("pots");
            Log.d(TAG, "Found greenhouse pots with " + potsObject.size() + " pot(s)");

            // Iterate over each greenhouse pot by key (1, 2, 3, 4, etc.)
            for (String potId : potsObject.keySet()) {
                try {
                    JsonObject potData = potsObject.getAsJsonObject(potId);
                    
                    // Plant data is nested inside a "plant" object (not "crop")
                    if (!potData.has("plant")) {
                        Log.d(TAG, "Greenhouse pot " + potId + " missing 'plant' field");
                        continue;
                    }
                    
                    JsonObject plantData = potData.getAsJsonObject("plant");
                    
                    // Extract crop name
                    String name = getJsonString(plantData, "name");
                    if (name == null || name.isEmpty()) {
                        Log.w(TAG, "Greenhouse pot " + potId + " missing plant name");
                        continue;
                    }
                    
                    // Only include valid greenhouse crops (Olive, Rice, Grape)
                    if (!isValidGreenhouseCrop(name)) {
                        Log.d(TAG, "Greenhouse pot " + potId + ": Skipping non-greenhouse crop '" + name + "'");
                        continue;
                    }

                    // Extract plantedAt timestamp (in milliseconds)
                    long plantedAt = 0;
                    if (plantData.has("plantedAt") && !plantData.get("plantedAt").isJsonNull()) {
                        plantedAt = plantData.get("plantedAt").getAsLong();
                    } else {
                        Log.w(TAG, "Greenhouse pot " + potId + " (" + name + ") missing plantedAt");
                        continue;
                    }

                    // Greenhouse crops don't have harvestedAt, so we just use plantedAt as base
                    long baseTimestamp = plantedAt;
                    
                    // Calculate readyTime = baseTimestamp + baseTime from Constants
                    Long baseTime = Constants.GREENHOUSE_CROP_GROWTH_TIMES.get(name);
                    if (baseTime == null || baseTime <= 0) {
                        Log.w(TAG, "Greenhouse pot " + potId + ": Unknown greenhouse crop '" + name + "' or invalid baseTime");
                        continue;
                    }
                    
                    long readyTime = baseTimestamp + baseTime;
                    
                    // Only include crops that will be ready in the future (not already passed)
                    long currentTime = System.currentTimeMillis();
                    if (readyTime <= currentTime) {
                        Log.d(TAG, "Skipping greenhouse crop: 1 " + name + 
                              " (ready=" + formatTimestamp(readyTime) + 
                              " - already passed)");
                        continue;
                    }
                    
                    // Each greenhouse pot counts as amount=1
                    FarmItem item = new FarmItem("greenhouse_crops", name, 1, readyTime);
                    greenhouseCrops.add(item);
                    Log.d(TAG, "Added greenhouse crop: 1 " + name + 
                          " (base=" + formatTimestamp(baseTimestamp) + 
                          ", baseTime=" + (baseTime / 1000) + "s" +
                          ", ready=" + formatTimestamp(readyTime) + ")");
                    
                } catch (Exception e) {
                    Log.w(TAG, "Error processing greenhouse pot " + potId + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting greenhouse crops: " + e.getMessage(), e);
        }

        // Sort by timestamp ascending (earliest first)
        sortByTimestamp(greenhouseCrops);
        Log.d(TAG, "Extracted " + greenhouseCrops.size() + " greenhouse crop(s)");
        return greenhouseCrops;
    }
    
    /**
     * Helper method to validate if a crop name is a valid greenhouse crop
     */
    private static boolean isValidGreenhouseCrop(String cropName) {
        return cropName != null && (cropName.equals("Olive") || cropName.equals("Rice") || cropName.equals("Grape"));
    }

    /**
     * Extracts resources from raw API response
     * Resources include: Trees, Stones, Iron, Gold, Crimstone, Oil, Sunstone
     * 
     * Each resource type (farm.trees, farm.stones, farm.iron, etc.) is a JsonObject 
     * with resource IDs as keys. Each resource contains a nested "wood" or "stone" object
     * with harvest/mine timestamp and boostedTime.
     * 
     * For trees: uses wood.choppedAt + Constants.RESOURCE_REPLENISH_TIMES["Tree"]
     * For others: uses stone.minedAt + Constants.RESOURCE_REPLENISH_TIMES[type]
     * 
     * @param farmData JsonObject farm object from API response
     * @return List<FarmItem> sorted by readyTime ascending (earliest first), only future items
     */
    public static List<FarmItem> extractResources(JsonObject farmData) {
        Log.d(TAG, "Extracting resources...");
        List<FarmItem> resources = new ArrayList<>();

        if (farmData == null) {
            Log.w(TAG, "farmData is null");
            return resources;
        }

        try {
            // Map of resource types and their key names in the farm object
            String[] resourceTypes = {"trees", "stones", "iron", "gold", "crimstones", "oilReserves", "sunstones"};
            
            for (String resourceType : resourceTypes) {
                try {
                    if (!farmData.has(resourceType)) {
                        Log.d(TAG, "No " + resourceType + " data in farm object");
                        continue;
                    }

                    JsonObject resourcesObject = farmData.getAsJsonObject(resourceType);
                    
                    // Convert plural type to singular for Constants lookup and notification name
                    String singularType;
                    if ("crimstones".equals(resourceType)) {
                        singularType = "Crimstone";
                    } else if ("oilReserves".equals(resourceType)) {
                        singularType = "Oil";
                    } else if ("sunstones".equals(resourceType)) {
                        singularType = "Sunstone";
                    } else if ("STONE".equals(resourceType.toUpperCase())) {
                        singularType = "Stone";
                    } else if ("TREE".equals(resourceType.toUpperCase())) {
                        singularType = "Tree";
                    } else {
                        // Generic plural-to-singular conversion: stones -> Stone, iron -> Iron
                        String temp = resourceType.endsWith("s") ? 
                            resourceType.substring(0, resourceType.length() - 1) : 
                            resourceType;
                        singularType = temp.substring(0, 1).toUpperCase() + temp.substring(1).toLowerCase();
                    }
                    
                    Log.d(TAG, "Processing " + resourceType + " (looking up: " + singularType + 
                          ", found " + resourcesObject.size() + " resource(s))");
                    
                    // Get replenish time from Constants
                    Long replenishTime = Constants.RESOURCE_REPLENISH_TIMES.get(singularType);
                    if (replenishTime == null || replenishTime <= 0) {
                        Log.w(TAG, "Unknown resource type or invalid replenish time: " + singularType);
                        continue;
                    }
                    
                    // Iterate over each resource in this type
                    for (String resourceId : resourcesObject.keySet()) {
                        try {
                            JsonObject resourceData = resourcesObject.getAsJsonObject(resourceId);
                            
                            // Determine which field to use (wood for trees, oil for oilReserves, stone for others)
                            String harvestFieldName;
                            if ("trees".equals(resourceType)) {
                                harvestFieldName = "wood";
                            } else if ("oilReserves".equals(resourceType)) {
                                harvestFieldName = "oil";
                            } else {
                                harvestFieldName = "stone";
                            }
                            
                            if (!resourceData.has(harvestFieldName)) {
                                Log.w(TAG, resourceType + " resource " + resourceId + 
                                      " missing '" + harvestFieldName + "' field");
                                continue;
                            }
                            
                            JsonObject harvestData = resourceData.getAsJsonObject(harvestFieldName);
                            
                            // Extract harvest/mine timestamp
                            long harvestedAt = 0;
                            String timestampField;
                            if ("trees".equals(resourceType)) {
                                timestampField = "choppedAt";
                            } else if ("oilReserves".equals(resourceType)) {
                                timestampField = "drilledAt";
                            } else {
                                timestampField = "minedAt";
                            }
                            
                            if (harvestData.has(timestampField) && 
                                !harvestData.get(timestampField).isJsonNull()) {
                                harvestedAt = harvestData.get(timestampField).getAsLong();
                            } else {
                                Log.w(TAG, resourceType + " resource " + resourceId + 
                                      " missing " + timestampField);
                                continue;
                            }
                            
                            // Calculate readyTime = harvestedAt + replenishTime
                            long readyTime = harvestedAt + replenishTime;
                            
                            // Only include resources that will be ready in the future
                            long currentTime = System.currentTimeMillis();
                            if (readyTime <= currentTime) {
                                Log.d(TAG, "Skipping " + singularType + ": 1 " + singularType + 
                                      " (ready=" + formatTimestamp(readyTime) + 
                                      " - already passed)");
                                continue;
                            }
                            
                            // Count this resource as amount=1
                            FarmItem item = new FarmItem("resource", singularType, 1, readyTime);
                            resources.add(item);
                            Log.d(TAG, "Added resource: 1 " + singularType + 
                                  " (harvested=" + formatTimestamp(harvestedAt) + 
                                  ", replenishTime=" + (replenishTime / 1000) + "s" +
                                  ", ready=" + formatTimestamp(readyTime) + ")");
                            
                        } catch (Exception e) {
                            Log.w(TAG, "Error processing " + resourceType + " resource " + resourceId + 
                                  ": " + e.getMessage());
                        }
                    }
                    
                } catch (Exception e) {
                    Log.w(TAG, "Error processing resource type " + resourceType + ": " + e.getMessage());
                }
            }
            
            // Extract from Lava Pits (different structure - has readyAt directly, not stone.minedAt)
            if (farmData.has("lavaPits")) {
                try {
                    JsonObject lavaPits = farmData.getAsJsonObject("lavaPits");
                    Log.d(TAG, "Processing " + lavaPits.size() + " lava pit(s)");
                    
                    long currentTime = System.currentTimeMillis();
                    Long replenishTime = Constants.RESOURCE_REPLENISH_TIMES.get("Lavapit");
                    if (replenishTime == null || replenishTime <= 0) {
                        Log.w(TAG, "Unknown or invalid replenish time for Lavapit");
                        return resources;
                    }
                    
                    for (String lavaPitId : lavaPits.keySet()) {
                        try {
                            JsonObject lavaPitData = lavaPits.getAsJsonObject(lavaPitId);
                            
                            // Lava pits have readyAt directly (already calculated by API)
                            long readyAt = 0;
                            if (lavaPitData.has("readyAt") && !lavaPitData.get("readyAt").isJsonNull()) {
                                readyAt = lavaPitData.get("readyAt").getAsLong();
                            } else {
                                Log.w(TAG, "Lava pit " + lavaPitId + " missing readyAt");
                                continue;
                            }
                            
                            // Only include lava pits that will be ready in the future
                            if (readyAt <= currentTime) {
                                Log.d(TAG, "Skipping lava pit: 1 Obsidian (ready=" + formatTimestamp(readyAt) + 
                                      " - already passed)");
                                continue;
                            }
                            
                            // Each lava pit produces Obsidian
                            FarmItem item = new FarmItem("resource", "Obsidian", 1, readyAt);
                            resources.add(item);
                            Log.d(TAG, "Added resource: 1 Obsidian from lava pit (ready=" + formatTimestamp(readyAt) + ")");
                            
                        } catch (Exception e) {
                            Log.w(TAG, "Error processing lava pit " + lavaPitId + ": " + e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error processing lava pits: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting resources: " + e.getMessage(), e);
        }

        // Sort by timestamp ascending (earliest first)
        sortByTimestamp(resources);
        Log.d(TAG, "Extracted " + resources.size() + " resource(s)");
        return resources;
    }

    /**
     * Extracts animals from raw API response
     * Animals include Chickens (in henHouse) and Cows/Sheep (in barn)
     * 
     * Structure: farm.henHouse.animals and farm.barn.animals
     * Each animal has: type, awakeAt, asleepAt, experience, etc.
     * 
     * For production readiness: readyTime = awakeAt + Constants.ANIMAL_PRODUCTION_TIMES[type]
     * awakeAt is when animal wakes up and can produce (egg/milk/wool)
     * 
     * @param farmData JsonObject farm object from API response
     * @return List<FarmItem> sorted by readyTime ascending (earliest first), only future items
     */
    public static List<FarmItem> extractAnimals(JsonObject farmData) {
        Log.d(TAG, "Extracting animals...");
        List<FarmItem> animals = new ArrayList<>();

        if (farmData == null) {
            Log.w(TAG, "farmData is null");
            return animals;
        }

        try {
            long currentTime = System.currentTimeMillis();
            
            // Extract from Hen House
            if (farmData.has("henHouse")) {
                try {
                    JsonObject henHouse = farmData.getAsJsonObject("henHouse");
                    if (henHouse.has("animals")) {
                        JsonObject henHouseAnimals = henHouse.getAsJsonObject("animals");
                        Log.d(TAG, "Processing " + henHouseAnimals.size() + " henHouse animal(s)");
                        
                        // Maps to group animals by type for counting
                        Map<String, List<Long>> animalsByType = new HashMap<>();
                        Map<String, List<Long>> loveNotificationsByType = new HashMap<>();
                        
                        for (String animalId : henHouseAnimals.keySet()) {
                            try {
                                JsonObject animal = henHouseAnimals.getAsJsonObject(animalId);
                                
                                String type = getJsonString(animal, "type");
                                if (type == null) {
                                    Log.w(TAG, "HenHouse animal " + animalId + " missing type");
                                    continue;
                                }
                                
                                long awakeAt = 0;
                                if (animal.has("awakeAt") && !animal.get("awakeAt").isJsonNull()) {
                                    awakeAt = animal.get("awakeAt").getAsLong();
                                } else {
                                    Log.w(TAG, "HenHouse animal " + animalId + " (" + type + ") missing awakeAt");
                                    continue;
                                }
                                
                                long asleepAt = 0;
                                if (animal.has("asleepAt") && !animal.get("asleepAt").isJsonNull()) {
                                    asleepAt = animal.get("asleepAt").getAsLong();
                                }
                                
                                // Extract production notification (when animal wakes up)
                                // For animals, use awakeAt directly as the notification time
                                if (awakeAt <= currentTime) {
                                    Log.d(TAG, "Skipping " + type + " (AWAKE): already passed");
                                } else {
                                    // Group by type for later counting
                                    animalsByType.computeIfAbsent(type, k -> new ArrayList<>()).add(awakeAt);
                                    Log.d(TAG, "Added animal (AWAKE): 1 " + type + 
                                          " at " + formatTimestamp(awakeAt));
                                }
                                
                                // Extract love notification
                                long loveTime = 0;
                                long lovedAt = 0;
                                if (animal.has("lovedAt") && !animal.get("lovedAt").isJsonNull()) {
                                    lovedAt = animal.get("lovedAt").getAsLong();
                                }
                                
                                // Calculate loveTime using game logic: max of both thresholds
                                // Animal needs love when BOTH conditions are true (asleepAt + 1/3 cycle AND lovedAt + 1/3 cycle)
                                long oneThirdCycle = (awakeAt - asleepAt) / 3;
                                long sleepThreshold = asleepAt + oneThirdCycle;
                                long loveThreshold = lovedAt + oneThirdCycle;
                                loveTime = Math.max(sleepThreshold, loveThreshold);
                                
                                // Only include love notification if it occurs before awakeAt
                                if (loveTime < awakeAt && loveTime > currentTime) {
                                    loveNotificationsByType.computeIfAbsent(type, k -> new ArrayList<>()).add(loveTime);
                                    Log.d(TAG, "Added animal (LOVE): 1 " + type + 
                                          " at " + formatTimestamp(loveTime));
                                } else if (loveTime >= awakeAt) {
                                    Log.d(TAG, "Skipping " + type + " (LOVE): occurs after awakeAt");
                                } else if (loveTime <= currentTime) {
                                    Log.d(TAG, "Skipping " + type + " (LOVE): already passed");
                                }
                                
                            } catch (Exception e) {
                                Log.w(TAG, "Error processing henHouse animal " + animalId + ": " + e.getMessage());
                            }
                        }
                        
                        // Convert grouped animals to FarmItems (production notifications)
                        for (String type : animalsByType.keySet()) {
                            List<Long> readyTimes = animalsByType.get(type);
                            for (Long readyTime : readyTimes) {
                                FarmItem item = new FarmItem("animals", type, 1, readyTime);
                                animals.add(item);
                            }
                        }
                        
                        // Convert grouped love notifications to FarmItems
                        for (String type : loveNotificationsByType.keySet()) {
                            List<Long> loveTimes = loveNotificationsByType.get(type);
                            for (Long loveTime : loveTimes) {
                                // Use a special marker in the name to identify this as a love notification
                                FarmItem item = new FarmItem("animals_love", type, 1, loveTime);
                                animals.add(item);
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error processing henHouse: " + e.getMessage());
                }
            }
            
            // Extract from Barn
            if (farmData.has("barn")) {
                try {
                    JsonObject barn = farmData.getAsJsonObject("barn");
                    if (barn.has("animals")) {
                        JsonObject barnAnimals = barn.getAsJsonObject("animals");
                        Log.d(TAG, "Processing " + barnAnimals.size() + " barn animal(s)");
                        
                        // Maps to group animals by type for counting
                        Map<String, List<Long>> animalsByType = new HashMap<>();
                        Map<String, List<Long>> loveNotificationsByType = new HashMap<>();
                        
                        for (String animalId : barnAnimals.keySet()) {
                            try {
                                JsonObject animal = barnAnimals.getAsJsonObject(animalId);
                                
                                String type = getJsonString(animal, "type");
                                if (type == null) {
                                    Log.w(TAG, "Barn animal " + animalId + " missing type");
                                    continue;
                                }
                                
                                long awakeAt = 0;
                                if (animal.has("awakeAt") && !animal.get("awakeAt").isJsonNull()) {
                                    awakeAt = animal.get("awakeAt").getAsLong();
                                } else {
                                    Log.w(TAG, "Barn animal " + animalId + " (" + type + ") missing awakeAt");
                                    continue;
                                }
                                
                                long asleepAt = 0;
                                if (animal.has("asleepAt") && !animal.get("asleepAt").isJsonNull()) {
                                    asleepAt = animal.get("asleepAt").getAsLong();
                                }
                                
                                // Extract production notification (when animal wakes up)
                                // For animals, use awakeAt directly as the notification time
                                if (awakeAt <= currentTime) {
                                    Log.d(TAG, "Skipping " + type + " (AWAKE): already passed");
                                } else {
                                    // Group by type for later counting
                                    animalsByType.computeIfAbsent(type, k -> new ArrayList<>()).add(awakeAt);
                                    Log.d(TAG, "Added animal (AWAKE): 1 " + type + 
                                          " at " + formatTimestamp(awakeAt));
                                }
                                
                                // Extract love notification
                                long loveTime = 0;
                                long lovedAt = 0;
                                if (animal.has("lovedAt") && !animal.get("lovedAt").isJsonNull()) {
                                    lovedAt = animal.get("lovedAt").getAsLong();
                                }
                                
                                // Calculate loveTime using game logic: max of both thresholds
                                // Animal needs love when BOTH conditions are true (asleepAt + 1/3 cycle AND lovedAt + 1/3 cycle)
                                long oneThirdCycle = (awakeAt - asleepAt) / 3;
                                long sleepThreshold = asleepAt + oneThirdCycle;
                                long loveThreshold = lovedAt + oneThirdCycle;
                                loveTime = Math.max(sleepThreshold, loveThreshold);
                                
                                // Only include love notification if it occurs before awakeAt
                                if (loveTime < awakeAt && loveTime > currentTime) {
                                    loveNotificationsByType.computeIfAbsent(type, k -> new ArrayList<>()).add(loveTime);
                                    Log.d(TAG, "Added animal (LOVE): 1 " + type + 
                                          " at " + formatTimestamp(loveTime));
                                } else if (loveTime >= awakeAt) {
                                    Log.d(TAG, "Skipping " + type + " (LOVE): occurs after awakeAt");
                                } else if (loveTime <= currentTime) {
                                    Log.d(TAG, "Skipping " + type + " (LOVE): already passed");
                                }
                                
                            } catch (Exception e) {
                                Log.w(TAG, "Error processing barn animal " + animalId + ": " + e.getMessage());
                            }
                        }
                        
                        // Convert grouped animals to FarmItems (production notifications)
                        for (String type : animalsByType.keySet()) {
                            List<Long> readyTimes = animalsByType.get(type);
                            for (Long readyTime : readyTimes) {
                                FarmItem item = new FarmItem("animals", type, 1, readyTime);
                                animals.add(item);
                            }
                        }
                        
                        // Convert grouped love notifications to FarmItems
                        for (String type : loveNotificationsByType.keySet()) {
                            List<Long> loveTimes = loveNotificationsByType.get(type);
                            for (Long loveTime : loveTimes) {
                                // Use a special marker in the name to identify this as a love notification
                                FarmItem item = new FarmItem("animals_love", type, 1, loveTime);
                                animals.add(item);
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error processing barn: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting animals: " + e.getMessage(), e);
        }

        // Sort by timestamp ascending (earliest first)
        sortByTimestamp(animals);
        Log.d(TAG, "Extracted " + animals.size() + " animal(s)");
        return animals;
    }

    /**
     * Extracts sick animals from barn and henHouse for sick animal notifications
     * Collects state information for all animals to detect sickness
     * 
     * Returns a list of SickAnimal objects (type, id, state)
     * Does NOT modify the main animal extraction - this is purely for tracking sickness state
     * 
     * @param farmData JsonObject farm object from API response
     * @return List<SickAnimal> with animal type, id, and current state
     */
    public static List<com.sfl.browser.models.SickAnimal> extractSickAnimals(JsonObject farmData) {
        DebugLog.log("🐔 Sick Animals: Extracting animal states for sick detection...");
        List<com.sfl.browser.models.SickAnimal> sickAnimals = new ArrayList<>();

        if (farmData == null) {
            DebugLog.log("🐔 Sick Animals: farmData is null");
            return sickAnimals;
        }

        try {
            long currentTime = System.currentTimeMillis();
            
            // Extract from Hen House
            if (farmData.has("henHouse")) {
                try {
                    JsonObject henHouse = farmData.getAsJsonObject("henHouse");
                    if (henHouse.has("animals")) {
                        JsonObject henHouseAnimals = henHouse.getAsJsonObject("animals");
                        DebugLog.log("🐔 Sick Animals: Processing " + henHouseAnimals.size() + " henHouse animal(s)");
                        
                        for (String animalId : henHouseAnimals.keySet()) {
                            try {
                                JsonObject animal = henHouseAnimals.getAsJsonObject(animalId);
                                
                                String type = getJsonString(animal, "type");
                                if (type == null) {
                                    continue;
                                }
                                
                                String state = getJsonString(animal, "state");
                                if (state == null) {
                                    state = "idle"; // Default state if not specified
                                }
                                
                                com.sfl.browser.models.SickAnimal sickAnimal = 
                                    new com.sfl.browser.models.SickAnimal(type, animalId, state, currentTime);
                                sickAnimals.add(sickAnimal);
                                
                                if ("sick".equals(state)) {
                                    DebugLog.log("🐔 Sick Animals: Found SICK henHouse animal - " + type + " (ID: " + animalId + ")");
                                }
                                
                            } catch (Exception e) {
                                DebugLog.log("⚠️ Sick Animals: Error processing henHouse animal " + animalId + ": " + e.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    DebugLog.log("⚠️ Sick Animals: Error processing henHouse: " + e.getMessage());
                }
            }
            
            // Extract from Barn
            if (farmData.has("barn")) {
                try {
                    JsonObject barn = farmData.getAsJsonObject("barn");
                    if (barn.has("animals")) {
                        JsonObject barnAnimals = barn.getAsJsonObject("animals");
                        DebugLog.log("🐔 Sick Animals: Processing " + barnAnimals.size() + " barn animal(s)");
                        
                        for (String animalId : barnAnimals.keySet()) {
                            try {
                                JsonObject animal = barnAnimals.getAsJsonObject(animalId);
                                
                                String type = getJsonString(animal, "type");
                                if (type == null) {
                                    continue;
                                }
                                
                                String state = getJsonString(animal, "state");
                                if (state == null) {
                                    state = "idle"; // Default state if not specified
                                }
                                
                                com.sfl.browser.models.SickAnimal sickAnimal = 
                                    new com.sfl.browser.models.SickAnimal(type, animalId, state, currentTime);
                                sickAnimals.add(sickAnimal);
                                
                                if ("sick".equals(state)) {
                                    DebugLog.log("🐔 Sick Animals: Found SICK barn animal - " + type + " (ID: " + animalId + ")");
                                }
                                
                            } catch (Exception e) {
                                DebugLog.log("⚠️ Sick Animals: Error processing barn animal " + animalId + ": " + e.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    DebugLog.log("⚠️ Sick Animals: Error processing barn: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            DebugLog.log("❌ Sick Animals: Error extracting animal states: " + e.getMessage());
        }

        DebugLog.log("🐔 Sick Animals: Extracted " + sickAnimals.size() + " total animal state(s)");
        return sickAnimals;
    }

    /**
     * Extracts cooking items from raw API response
     * 
     * Structure: farm.buildings[buildingName] where buildingName is one of:
     * Fire Pit, Bakery, Kitchen, Deli, Smoothie Shack
     * Each building contains: crafting array with items that have readyAt timestamp
     * 
     * Each cooking item has:
     * - name: cooking item name (e.g., "Boiled Eggs", "Kale Stew")
     * - readyAt: timestamp when cooking completes (already calculated by game)
     * - amount: quantity being cooked
     * - boost: oil boost applied
     * - skills: active skills
     * 
     * @param farmData JsonObject farm object from API response
     * @return List<FarmItem> sorted by readyTime ascending (earliest first), only future items
     */
    public static List<FarmItem> extractCooking(JsonObject farmData) {
        Log.d(TAG, "Extracting cooking...");
        List<FarmItem> cooking = new ArrayList<>();

        if (farmData == null) {
            Log.w(TAG, "farmData is null");
            return cooking;
        }

        try {
            if (!farmData.has("buildings")) {
                Log.w(TAG, "No buildings data found in farm object");
                return cooking;
            }

            JsonObject buildings = farmData.getAsJsonObject("buildings");
            
            // List of all cooking building types
            String[] cookingBuildings = {
                "Fire Pit", "Bakery", "Kitchen", "Deli", "Smoothie Shack"
            };
            
            long currentTime = System.currentTimeMillis();
            
            for (String buildingType : cookingBuildings) {
                try {
                    if (!buildings.has(buildingType)) {
                        Log.d(TAG, "No " + buildingType + " found");
                        continue;
                    }
                    
                    JsonArray buildingArray = buildings.getAsJsonArray(buildingType);
                    Log.d(TAG, "Processing " + buildingArray.size() + " " + buildingType + " building(s)");
                    
                    for (JsonElement buildingElement : buildingArray) {
                        try {
                            JsonObject building = buildingElement.getAsJsonObject();
                            
                            // Check if building has crafting array
                            if (!building.has("crafting") || building.get("crafting").isJsonNull()) {
                                continue;
                            }
                            
                            JsonArray craftingArray = building.getAsJsonArray("crafting");
                            
                            // Each crafting item in the array
                            for (JsonElement craftingElement : craftingArray) {
                                try {
                                    JsonObject craftingItem = craftingElement.getAsJsonObject();
                                    
                                    // Extract cooking item name
                                    String name = getJsonString(craftingItem, "name");
                                    if (name == null || name.isEmpty()) {
                                        Log.w(TAG, buildingType + ": Crafting item missing name");
                                        continue;
                                    }
                                    
                                    // Extract readyAt (already calculated by game, use directly)
                                    long readyAt = 0;
                                    if (craftingItem.has("readyAt") && !craftingItem.get("readyAt").isJsonNull()) {
                                        readyAt = craftingItem.get("readyAt").getAsLong();
                                    } else {
                                        Log.w(TAG, buildingType + " (" + name + "): Missing readyAt");
                                        continue;
                                    }
                                    
                                    // Extract amount being cooked
                                    int amount = 1;
                                    if (craftingItem.has("amount") && !craftingItem.get("amount").isJsonNull()) {
                                        amount = craftingItem.get("amount").getAsInt();
                                    }
                                    
                                    // Only include items that will be ready in the future
                                    if (readyAt <= currentTime) {
                                        Log.d(TAG, "Skipping " + buildingType + " item: " + amount + " " + name + 
                                              " (ready=" + formatTimestamp(readyAt) + " - already passed)");
                                        continue;
                                    }
                                    
                                    FarmItem item = new FarmItem("cooking", name, amount, readyAt);
                                    item.setBuildingName(buildingType);  // Store building name for clustering logic
                                    cooking.add(item);
                                    Log.d(TAG, "Added " + buildingType + " item: " + amount + " " + name + 
                                          " (ready=" + formatTimestamp(readyAt) + ")");
                                    
                                } catch (Exception e) {
                                    Log.w(TAG, "Error processing " + buildingType + " crafting item: " + e.getMessage());
                                }
                            }
                            
                        } catch (Exception e) {
                            Log.w(TAG, "Error processing " + buildingType + " building: " + e.getMessage());
                        }
                    }
                    
                } catch (Exception e) {
                    Log.w(TAG, "Error processing " + buildingType + " building type: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting cooking: " + e.getMessage(), e);
        }

        // Sort by timestamp ascending (earliest first)
        sortByTimestamp(cooking);
        Log.d(TAG, "Extracted " + cooking.size() + " cooking item(s)");
        return cooking;
    }

    /**
     * Extracts composters from raw API response
     * 
     * Structure: farm.buildings[buildingName] where buildingName is one of:
     * Compost Bin, Turbo Composter, Premium Composter
     * Each building contains: producing object with readyAt timestamp and items
     * 
     * Each composter has:
     * - producing.items: Object with item names as keys and quantities as values
     * - producing.readyAt: Timestamp when production completes (already calculated by game)
     * - producing.startedAt: When production started (optional)
     * 
     * @param farmData JsonObject farm object from API response
     * @return List<FarmItem> sorted by readyTime ascending (earliest first), only future items
     */
    public static List<FarmItem> extractComposters(JsonObject farmData) {
        Log.d(TAG, "Extracting composters...");
        List<FarmItem> composters = new ArrayList<>();

        if (farmData == null) {
            Log.w(TAG, "farmData is null");
            return composters;
        }

        try {
            if (!farmData.has("buildings")) {
                Log.w(TAG, "No buildings data found in farm object");
                return composters;
            }

            JsonObject buildings = farmData.getAsJsonObject("buildings");
            
            // List of all composter building types
            String[] composterBuildings = {
                "Compost Bin", "Turbo Composter", "Premium Composter"
            };
            
            long currentTime = System.currentTimeMillis();
            
            for (String buildingType : composterBuildings) {
                try {
                    if (!buildings.has(buildingType)) {
                        Log.d(TAG, "No " + buildingType + " found");
                        continue;
                    }
                    
                    JsonArray buildingArray = buildings.getAsJsonArray(buildingType);
                    Log.d(TAG, "Processing " + buildingArray.size() + " " + buildingType + " composter(s)");
                    
                    for (JsonElement buildingElement : buildingArray) {
                        try {
                            JsonObject composter = buildingElement.getAsJsonObject();
                            
                            // Check if composter has producing object
                            if (!composter.has("producing") || composter.get("producing").isJsonNull()) {
                                continue;
                            }
                            
                            JsonObject producing = composter.getAsJsonObject("producing");
                            
                            // Extract readyAt timestamp (already calculated by game, use directly)
                            long readyAt = 0;
                            if (producing.has("readyAt") && !producing.get("readyAt").isJsonNull()) {
                                readyAt = producing.get("readyAt").getAsLong();
                            } else {
                                Log.w(TAG, buildingType + ": Missing readyAt in producing");
                                continue;
                            }
                            
                            // Extract items being produced
                            if (!producing.has("items") || producing.get("items").isJsonNull()) {
                                Log.w(TAG, buildingType + ": Missing items in producing");
                                continue;
                            }
                            
                            JsonObject itemsObject = producing.getAsJsonObject("items");
                            StringBuilder itemsList = new StringBuilder();
                            int totalQuantity = 0;
                            
                            // Build items list and calculate total quantity
                            java.util.List<String> itemNames = new java.util.ArrayList<>();
                            for (String itemName : itemsObject.keySet()) {
                                int quantity = itemsObject.get(itemName).getAsInt();
                                totalQuantity += quantity;
                                itemNames.add(quantity + " " + itemName);
                            }
                            
                            // Join items with commas
                            for (int i = 0; i < itemNames.size(); i++) {
                                itemsList.append(itemNames.get(i));
                                if (i < itemNames.size() - 1) {
                                    itemsList.append(", ");
                                }
                            }
                            
                            // Only include composters that will be ready in the future
                            if (readyAt <= currentTime) {
                                Log.d(TAG, "Skipping " + buildingType + ": " + itemsList.toString() + 
                                      " (ready=" + formatTimestamp(readyAt) + " - already passed)");
                                continue;
                            }
                            
                            // Create FarmItem with building name for clustering context
                            FarmItem item = new FarmItem("composters", buildingType, totalQuantity, readyAt);
                            item.setBuildingName(buildingType);
                            item.setDetails(itemsList.toString());  // Store the produced items
                            composters.add(item);
                            Log.d(TAG, "Added " + buildingType + ": " + itemsList.toString() + 
                                  " (ready=" + formatTimestamp(readyAt) + ")");
                            
                        } catch (Exception e) {
                            Log.w(TAG, "Error processing " + buildingType + " composter: " + e.getMessage());
                        }
                    }
                    
                } catch (Exception e) {
                    Log.w(TAG, "Error processing " + buildingType + " building type: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting composters: " + e.getMessage(), e);
        }

        // Sort by timestamp ascending (earliest first)
        sortByTimestamp(composters);
        Log.d(TAG, "Extracted " + composters.size() + " composter(s)");
        return composters;
    }

    /**
     * Extracts flowers from raw API response
     * 
     * Structure: farm.flowers.flowerBeds
     * Each flower bed contains a flower object with:
     * - name: flower type (e.g., "Yellow Cosmos", "Blue Gladiolus")
     * - plantedAt: timestamp when planted
     * 
     * Calculation: readyTime = plantedAt + Constants.FLOWER_GROWTH_TIMES[flowerName]
     * Growth times vary by flower type (typically 1-7 days)
     * 
     * @param farmData JsonObject farm object from API response
     * @return List<FarmItem> sorted by readyTime ascending (earliest first), only future items
     */
    public static List<FarmItem> extractFlowers(JsonObject farmData) {
        Log.d(TAG, "Extracting flowers...");
        List<FarmItem> flowers = new ArrayList<>();

    // Clear the map before each extraction
    flowerBedFinishTimes.clear();

        if (farmData == null) {
            Log.w(TAG, "farmData is null");
            return flowers;
        }

        try {
            if (!farmData.has("flowers")) {
                Log.w(TAG, "No flowers data found in farm object");
                return flowers;
            }

            JsonObject flowersData = farmData.getAsJsonObject("flowers");
            
            // Check for flowerBeds object
            if (!flowersData.has("flowerBeds")) {
                Log.w(TAG, "No flowerBeds found in flowers object");
                return flowers;
            }

            JsonObject flowerBeds = flowersData.getAsJsonObject("flowerBeds");
            Log.d(TAG, "Found " + flowerBeds.size() + " flower bed(s)");

            long currentTime = System.currentTimeMillis();

            // Iterate over each flower bed by key
            for (String bedId : flowerBeds.keySet()) {
                try {
                    JsonObject bedData = flowerBeds.getAsJsonObject(bedId);

                    // Check if bed has flower object
                    if (!bedData.has("flower") || bedData.get("flower").isJsonNull()) {
                        continue;
                    }

                    JsonObject flowerData = bedData.getAsJsonObject("flower");

                    // Extract flower name
                    String name = getJsonString(flowerData, "name");
                    if (name == null || name.isEmpty()) {
                        Log.w(TAG, "Flower bed " + bedId + " missing name");
                        continue;
                    }

                    // Extract plantedAt timestamp (in milliseconds)
                    long plantedAt = 0;
                    if (flowerData.has("plantedAt") && !flowerData.get("plantedAt").isJsonNull()) {
                        plantedAt = flowerData.get("plantedAt").getAsLong();
                    } else {
                        Log.w(TAG, "Flower bed " + bedId + " (" + name + ") missing plantedAt");
                        continue;
                    }

                    // Calculate readyTime = plantedAt + baseTime from Constants
                    Long baseTime = Constants.FLOWER_GROWTH_TIMES.get(name);
                    if (baseTime == null || baseTime <= 0) {
                        Log.w(TAG, "Flower bed " + bedId + ": Unknown flower '" + name + "' or invalid baseTime");
                        continue;
                    }

                    long finishTime = plantedAt + baseTime;
                    // Store finish time for this flower bed
                    flowerBedFinishTimes.put(bedId, finishTime);

                    long readyTime = finishTime;

                    // Only include flowers that will be ready in the future (not already passed)
                    if (readyTime <= currentTime) {
                        Log.d(TAG, "Skipping flower: 1 " + name +
                              " (ready=" + formatTimestamp(readyTime) +
                              " - already passed)");
                        continue;
                    }

                    // Each flower bed counts as amount=1
                    FarmItem item = new FarmItem("flowers", name, 1, readyTime);
                    flowers.add(item);
                    Log.d(TAG, "Added flower: 1 " + name +
                          " (planted=" + formatTimestamp(plantedAt) + 
                          ", baseTime=" + (baseTime / 1000 / 60 / 60 / 24) + " days" +
                          ", ready=" + formatTimestamp(readyTime) + ")");
                    
                } catch (Exception e) {
                    Log.w(TAG, "Error processing flower bed " + bedId + ": " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting flowers: " + e.getMessage(), e);
        }

        // Sort by timestamp ascending (earliest first)
        sortByTimestamp(flowers);
        Log.d(TAG, "Extracted " + flowers.size() + " flower(s)");
        return flowers;
    }

    /**
     * Sorts FarmItems by timestamp in ascending order (earliest first)
     */
    private static void sortByTimestamp(List<FarmItem> items) {
        Collections.sort(items, new Comparator<FarmItem>() {
            @Override
            public int compare(FarmItem o1, FarmItem o2) {
                return Long.compare(o1.getTimestamp(), o2.getTimestamp());
            }
        });
    }

    /**
     * Safely extracts a string value from JsonObject
     */
    private static String getJsonString(JsonObject obj, String key) {
        try {
            if (obj.has(key) && !obj.get(key).isJsonNull()) {
                return obj.get(key).getAsString();
            }
        } catch (Exception e) {
            Log.w(TAG, "Error reading string field: " + key + ", " + e.getMessage());
        }
        return null;
    }

    /**
     * Safely extracts an integer value from JsonObject with default fallback
     */
    private static int getJsonInt(JsonObject obj, String key, int defaultValue) {
        try {
            if (obj.has(key) && !obj.get(key).isJsonNull()) {
                return obj.get(key).getAsInt();
            }
        } catch (Exception e) {
            Log.w(TAG, "Error reading int field: " + key + ", " + e.getMessage());
        }
        return defaultValue;
    }

    /**
     * Parses ISO 8601 formatted timestamp string to Unix timestamp (milliseconds)
     * Format: "2025-11-01T15:30:00Z" or "2025-11-01T15:30:00.000Z"
     */
    private static long parseISO8601(String isoString) {
        try {
            if (isoString == null || isoString.isEmpty()) {
                return 0;
            }
            // Handle both formats with and without milliseconds
            String normalized = isoString.replace("Z", "+00:00");
            if (isoString.contains("T")) {
                // Use SimpleDateFormat for parsing
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
                Date date = sdf.parse(isoString.substring(0, 19)); // Take only YYYY-MM-DDTHH:MM:SS
                return date.getTime();
            }
        } catch (Exception e) {
            Log.w(TAG, "Error parsing ISO8601 timestamp: " + isoString + ", " + e.getMessage());
        }
        return 0;
    }

    /**
     * Formats Unix timestamp to human-readable MM/DD HH:MM:SS format
     */
    public static List<FarmItem> extractCraftingBox(JsonObject farmObject) {
        List<FarmItem> items = new ArrayList<>();
        
        try {
            if (!farmObject.has("craftingBox")) {
                Log.d(TAG, "No craftingBox found");
                return items;
            }
            
            JsonObject craftingBox = farmObject.getAsJsonObject("craftingBox");
            
            // Check if status is "crafting" and readyAt is in the future
            if (!craftingBox.has("status") || !craftingBox.has("readyAt")) {
                Log.d(TAG, "Crafting box missing status or readyAt");
                return items;
            }
            
            String status = craftingBox.get("status").getAsString();
            long readyAt = craftingBox.get("readyAt").getAsLong();
            long currentTime = System.currentTimeMillis();
            
            // Only include if currently crafting and readyAt is in the future
            if ("crafting".equals(status) && readyAt > currentTime) {
                // Get the collectible name
                String collectibleName = "Unknown";
                if (craftingBox.has("item")) {
                    JsonObject item = craftingBox.getAsJsonObject("item");
                    if (item.has("collectible")) {
                        collectibleName = item.get("collectible").getAsString();
                    }
                }
                
                // Create notification item using proper constructor
                FarmItem farmItem = new FarmItem("crafting", collectibleName, 1, readyAt);
                
                items.add(farmItem);
                Log.d(TAG, "Added crafting box item: " + collectibleName + " ready at " + formatTimestamp(readyAt));
            } else {
                Log.d(TAG, "Crafting box status: " + status + ", readyAt: " + formatTimestamp(readyAt) + " (current: " + formatTimestamp(currentTime) + ")");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting crafting box: " + e.getMessage(), e);
        }
        
        return items;
    }

    /**
     * Extracts beehive notifications (both swarm alerts and honey fullness alerts)
     * Beehives are numbered 1, 2, 3, etc. for display purposes
     */
    public static List<FarmItem> extractBeehives(JsonObject farmObject) {
        List<FarmItem> items = new ArrayList<>();
        
        try {
            if (!farmObject.has("beehives")) {
                Log.d(TAG, "No beehives found in farm data");
                return items;
            }
            
            JsonObject beehives = farmObject.getAsJsonObject("beehives");
            long currentTime = System.currentTimeMillis();
            int beehiveIndex = 0; // For numbering 1, 2, 3...
            
            for (String uuid : beehives.keySet()) {
                beehiveIndex++;
                JsonObject beehive = beehives.getAsJsonObject(uuid);
                String displayNumber = String.valueOf(beehiveIndex);
                
                // SWARM ALERT: Check if swarm is true
                if (beehive.has("swarm") && beehive.get("swarm").getAsBoolean()) {
                    // Create swarm alert with special marker to track state changes
                    FarmItem swarmItem = new FarmItem("Beehive Swarm", "Beehive " + displayNumber, 1, currentTime);
                    swarmItem.setBuildingName(uuid); // Store UUID for state tracking
                    items.add(swarmItem);
                    Log.d(TAG, "Added beehive swarm alert: Beehive " + displayNumber + " (uuid: " + uuid + ")");
                }
                
                // HONEY FULLNESS ALERT: Calculate when honey will be full
                if (beehive.has("honey") && beehive.has("flowers")) {
                    try {
                        JsonObject honey = beehive.getAsJsonObject("honey");
                        JsonArray flowers = beehive.getAsJsonArray("flowers");
                        
                        if (honey.has("updatedAt") && flowers.size() > 0) {
                            long honeyUpdatedAt = honey.get("updatedAt").getAsLong();
                            double alreadyProduced = honey.has("produced") ? honey.get("produced").getAsDouble() : 0.0;

                            // Find the attached flower bed id for this beehive (assume first flower in array has the id)
                            JsonObject firstFlower = flowers.get(0).getAsJsonObject();
                            String flowerBedId = firstFlower.has("id") ? firstFlower.get("id").getAsString() : null;
                            Long finishTime = (flowerBedId != null) ? flowerBedFinishTimes.get(flowerBedId) : null;

                            // Only produce honey if the finish time is in the future
                            if (finishTime == null || finishTime <= currentTime) {
                                Log.d(TAG, "Beehive " + displayNumber + " has no flower currently attached (by finishTime), skipping honey calculation.");
                                continue;
                            }

                            // Use the first flower's rate for honey production
                            double flowerRate = firstFlower.has("rate") ? firstFlower.get("rate").getAsDouble() : 1.0;
                            long flowerAttachedUntil = finishTime;

                            // Calculate honey produced since last update (from honey.updatedAt)
                            long productionStartTime = honeyUpdatedAt;
                            long productionEndTime = Math.min(currentTime, flowerAttachedUntil);
                            long timeSinceStart = Math.max(0, productionEndTime - productionStartTime);
                            double honeyProducedNow = timeSinceStart * flowerRate;

                            double currentHoney = alreadyProduced + honeyProducedNow;
                            double remainingCapacity = 86400000.0 - currentHoney;

                            if (remainingCapacity > 0) {
                                long millisecondsToFull = Math.round(remainingCapacity / flowerRate);
                                long fullnessTime = currentTime + millisecondsToFull;

                                // Only include if fullness will occur BEFORE flower detaches
                                if (fullnessTime > currentTime && fullnessTime < flowerAttachedUntil) {
                                    FarmItem fullItem = new FarmItem("Beehive Full", "Beehive " + displayNumber, 1, fullnessTime);
                                    fullItem.setBuildingName(uuid); // Store UUID for reference
                                    items.add(fullItem);
                                    Log.d(TAG, "Added beehive fullness alert: Beehive " + displayNumber + " (uuid: " + uuid + ") full at " + formatTimestamp(fullnessTime) +
                                            " (current: " + String.format("%.0f", currentHoney) + "ml, rate: " + flowerRate + "ml/ms, flower detaches at: " + formatTimestamp(flowerAttachedUntil) + ")");
                                } else {
                                    Log.d(TAG, "Beehive " + displayNumber + " won't fill before flower detaches: fullAt=" + formatTimestamp(fullnessTime) +
                                            " detachAt=" + formatTimestamp(flowerAttachedUntil));
                                }
                            } else {
                                Log.d(TAG, "Beehive " + displayNumber + " already full or over capacity: " + String.format("%.0f", currentHoney) + "ml");
                            }
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Error processing beehive honey for uuid " + uuid + ": " + e.getMessage());
                    }
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting beehives: " + e.getMessage(), e);
        }
        
        return items;
    }

    /**
     * Extracts Crop Machine queue items - each queued crop will be ready at readyAt
     * Groups items by crop name for notifications
     */
    public static List<FarmItem> extractCropMachine(JsonObject farmObject) {
        List<FarmItem> items = new ArrayList<>();
        
        try {
            if (!farmObject.has("buildings")) {
                Log.d(TAG, "No buildings found in farm data");
                return items;
            }
            
            JsonObject buildings = farmObject.getAsJsonObject("buildings");
            if (!buildings.has("Crop Machine")) {
                Log.d(TAG, "No Crop Machine buildings found");
                return items;
            }
            
            JsonArray cropMachines = buildings.getAsJsonArray("Crop Machine");
            long currentTime = System.currentTimeMillis();
            
            // Iterate through each Crop Machine instance
            for (int machineIdx = 0; machineIdx < cropMachines.size(); machineIdx++) {
                JsonObject machine = cropMachines.get(machineIdx).getAsJsonObject();
                
                // Check for queue
                if (!machine.has("queue")) {
                    Log.d(TAG, "Crop Machine " + machineIdx + " has no queue");
                    continue;
                }
                
                JsonArray queue = machine.getAsJsonArray("queue");
                
                // Extract each queued item
                for (int queueIdx = 0; queueIdx < queue.size(); queueIdx++) {
                    try {
                        JsonObject queueItem = queue.get(queueIdx).getAsJsonObject();
                        
                        if (!queueItem.has("readyAt") || !queueItem.has("crop")) {
                            Log.d(TAG, "Queue item missing readyAt or crop name");
                            continue;
                        }
                        
                        long readyAt = queueItem.get("readyAt").getAsLong();
                        String cropName = queueItem.get("crop").getAsString();
                        long seedAmount = queueItem.has("seeds") ? queueItem.get("seeds").getAsLong() : 0;
                        
                        // Only include if readyAt is in the future
                        if (readyAt > currentTime) {
                            // Create item: category=Crop Machine, name=crop name, amount=seed count, timestamp=readyAt
                            FarmItem cropItem = new FarmItem("Crop Machine", cropName, (int) seedAmount, readyAt);
                            items.add(cropItem);
                            Log.d(TAG, "Added Crop Machine item: " + cropName + " (" + seedAmount + " seeds) ready at " + formatTimestamp(readyAt));
                        } else {
                            Log.d(TAG, "Crop Machine item already ready or in past: " + cropName + " at " + formatTimestamp(readyAt));
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Error processing Crop Machine queue item " + queueIdx + ": " + e.getMessage());
                    }
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting Crop Machine: " + e.getMessage(), e);
        }
        
        return items;
    }

    /**
     * Extracts Sunstone mining alerts - sunstones are ready when minedAt + 3 days
     * Each sunstone takes 3 days (259200000 ms) to be ready to harvest
     */
    public static List<FarmItem> extractSunstones(JsonObject farmObject) {
        List<FarmItem> items = new ArrayList<>();
        
        try {
            if (!farmObject.has("sunstones")) {
                Log.d(TAG, "No sunstones found in farm data");
                return items;
            }
            
            JsonObject sunstones = farmObject.getAsJsonObject("sunstones");
            long currentTime = System.currentTimeMillis();
            
            // Sunstone growth time: 3 days = 259200000 ms
            long sunstoneGrowthTime = 3L * 24 * 60 * 60 * 1000;
            
            Log.d(TAG, "Found " + sunstones.size() + " sunstone node(s)");
            
            for (String uuid : sunstones.keySet()) {
                try {
                    JsonObject sunstone = sunstones.getAsJsonObject(uuid);
                    
                    if (!sunstone.has("stone")) {
                        Log.d(TAG, "Sunstone " + uuid + " missing stone data");
                        continue;
                    }
                    
                    JsonObject stone = sunstone.getAsJsonObject("stone");
                    
                    if (!stone.has("minedAt")) {
                        Log.d(TAG, "Sunstone " + uuid + " missing minedAt");
                        continue;
                    }
                    
                    long minedAt = stone.get("minedAt").getAsLong();
                    long readyAt = minedAt + sunstoneGrowthTime;
                    
                    // Only include if ready time is in the future
                    if (readyAt > currentTime) {
                        // Create FarmItem with UUID as ID to track individual sunstones
                        FarmItem sunstoneItem = new FarmItem(uuid, "Sunstones", "Sunstone", 1, readyAt);
                        items.add(sunstoneItem);
                        Log.d(TAG, "Added sunstone [UUID: " + uuid + "] ready at " + formatTimestamp(readyAt) + " (mined at " + formatTimestamp(minedAt) + ")");
                    } else {
                        Log.d(TAG, "Sunstone [UUID: " + uuid + "] already ready or in past: ready at " + formatTimestamp(readyAt));
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error processing sunstone " + uuid + ": " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting sunstones: " + e.getMessage(), e);
        }
        
        return items;
    }

    /**
     * Extracts daily reset notification - fires every day at 00:00 UTC
     * Always creates one notification for today's reset if enabled
     */
    public static List<FarmItem> extractDailyReset(JsonObject farmObject) {
        List<FarmItem> items = new ArrayList<>();
        
        try {
            long currentTime = System.currentTimeMillis();
            
            // Calculate next daily reset at 00:00 UTC
            // Get current time in UTC
            java.util.Calendar utcCalendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
            utcCalendar.setTimeInMillis(currentTime);
            
            // Set to 00:00:00 UTC tomorrow
            utcCalendar.add(java.util.Calendar.DAY_OF_MONTH, 1);
            utcCalendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
            utcCalendar.set(java.util.Calendar.MINUTE, 0);
            utcCalendar.set(java.util.Calendar.SECOND, 0);
            utcCalendar.set(java.util.Calendar.MILLISECOND, 0);
            
            long nextResetTime = utcCalendar.getTimeInMillis();
            
            // Create daily reset item
            FarmItem resetItem = new FarmItem("Daily Reset", "Daily Reset", 1, nextResetTime);
            items.add(resetItem);
            
            Log.d(TAG, "Created daily reset notification for " + formatTimestamp(nextResetTime) + " UTC");
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting daily reset: " + e.getMessage(), e);
        }
        
        return items;
    }

    public static String formatTimestamp(long timestamp) {
        try {
            Date date = new Date(timestamp);
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm:ss", Locale.US);
            return sdf.format(date);
        } catch (Exception e) {
            Log.w(TAG, "Error formatting timestamp: " + timestamp);
            return "Unknown";
        }
    }

    /**
     * Extract floating island notifications (schedule and shop changes)
     * Note: This method requires context, so it's delegated to FloatingIslandExtractor
     * which is called directly from NotificationManagerService
     */
    public static List<FarmItem> extractFloatingIsland(JsonObject farmData, android.content.Context context) {
        FloatingIslandExtractor extractor = new FloatingIslandExtractor(context);
        return extractor.extractFloatingIslandNotifications(farmData);
    }
}
