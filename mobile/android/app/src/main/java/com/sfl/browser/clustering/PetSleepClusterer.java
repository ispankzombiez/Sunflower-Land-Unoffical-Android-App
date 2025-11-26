package com.sfl.browser.clustering;

import android.util.Log;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Clustering strategy for pet sleep notifications
 * 
 * Rules:
 * 1. Extract all pets from farm.pets section (all categories)
 * 2. For each pet, calculate asleepAt = pettedAt + 2 hours
 * 3. Group pets that sleep within 1-minute window (±30 seconds)
 * 4. Single pet in window: "{name} went to sleep!"
 * 5. Multiple pets in window: "multiple pets went to sleep!"
 * 
 * This clusterer processes independently from other notification types
 */
public class PetSleepClusterer {
    private static final String TAG = "PetSleepClusterer";
    private static final long SLEEP_DELAY_MS = 2 * 60 * 60 * 1000; // 2 hours in milliseconds
    private static final long GROUPING_WINDOW_MS = 60 * 1000; // 1 minute grouping window

    /**
     * Cluster pet sleep data from farm.pets
     * @param petsData JsonObject containing pets data (the "pets" section from farm)
     * @return List of NotificationGroups for pet sleep events
     */
    public List<NotificationGroup> clusterPetSleep(JsonObject petsData) {
        Log.d(TAG, "Starting pet sleep clustering");
        List<NotificationGroup> groups = new ArrayList<>();

        if (petsData == null || petsData.entrySet().isEmpty()) {
            Log.d(TAG, "No pets data to process");
            return groups;
        }

        // Extract all pets from all categories
        List<PetSleepData> allPets = extractAllPets(petsData);
        Log.d(TAG, "Extracted " + allPets.size() + " total pets");

        if (allPets.isEmpty()) {
            Log.d(TAG, "No pets found with pettedAt data");
            return groups;
        }

        // Calculate sleep times for all pets
        List<PetSleepData> petsWithSleepTime = calculateSleepTimes(allPets);

        // Group pets by sleep time (within 1-minute window)
        Map<Long, List<PetSleepData>> petsByTimeWindow = groupPetsByTimeWindow(petsWithSleepTime);

        // Create notification groups from time windows
        for (Map.Entry<Long, List<PetSleepData>> entry : petsByTimeWindow.entrySet()) {
            long asleepAt = entry.getKey();
            List<PetSleepData> petsInWindow = entry.getValue();

            NotificationGroup group = createNotificationGroup(petsInWindow, asleepAt);
            groups.add(group);

            Log.d(TAG, "Created notification group: " + group.name + " at " + formatTimestamp(asleepAt));
        }

        Log.d(TAG, "Pet sleep clustering complete: " + groups.size() + " notification groups");
        return groups;
    }

    /**
     * Extract all pets from all categories in pets data
     */
    private List<PetSleepData> extractAllPets(JsonObject petsData) {
        List<PetSleepData> allPets = new ArrayList<>();

        for (String categoryName : petsData.keySet()) {
            // Skip metadata keys
            if (categoryName.equals("requestsGeneratedAt") || categoryName.equals("nfts")) {
                continue;
            }

            JsonElement categoryElement = petsData.get(categoryName);
            if (!categoryElement.isJsonObject()) {
                continue;
            }

            JsonObject category = categoryElement.getAsJsonObject();
            Log.d(TAG, "Processing pet category: " + categoryName);

            // Extract pets from this category
            for (String petName : category.keySet()) {
                JsonElement petElement = category.get(petName);
                if (!petElement.isJsonObject()) {
                    continue;
                }

                JsonObject petData = petElement.getAsJsonObject();

                // Extract name and pettedAt
                String name = null;
                long pettedAt = -1;

                if (petData.has("name")) {
                    name = petData.get("name").getAsString();
                }

                if (petData.has("pettedAt")) {
                    pettedAt = petData.get("pettedAt").getAsLong();
                }

                if (name != null && pettedAt > 0) {
                    PetSleepData petSleepData = new PetSleepData(name, pettedAt, categoryName);
                    allPets.add(petSleepData);
                    Log.d(TAG, "Extracted pet: " + name + " from category: " + categoryName + 
                          " pettedAt: " + formatTimestamp(pettedAt));
                } else {
                    Log.w(TAG, "Incomplete pet data for: " + petName + " (name=" + name + 
                          ", pettedAt=" + pettedAt + ")");
                }
            }
        }

        // Also extract NFT pets from the 'nfts' section if present
        if (petsData.has("nfts") && petsData.get("nfts").isJsonObject()) {
            JsonObject nfts = petsData.getAsJsonObject("nfts");
            for (String nftId : nfts.keySet()) {
                JsonElement nftElement = nfts.get(nftId);
                if (!nftElement.isJsonObject()) {
                    continue;
                }
                JsonObject nftPet = nftElement.getAsJsonObject();
                String name = null;
                long pettedAt = -1;
                if (nftPet.has("name")) {
                    name = nftPet.get("name").getAsString();
                }
                if (nftPet.has("pettedAt")) {
                    pettedAt = nftPet.get("pettedAt").getAsLong();
                }
                if (name != null && pettedAt > 0) {
                    PetSleepData petSleepData = new PetSleepData(name, pettedAt, "nfts");
                    allPets.add(petSleepData);
                    Log.d(TAG, "Extracted NFT pet: " + name + " from nfts category" + 
                          " pettedAt: " + formatTimestamp(pettedAt));
                }
            }
        }

        return allPets;
    }

    /**
     * Calculate sleep times (pettedAt + 2 hours) for all pets
     */
    private List<PetSleepData> calculateSleepTimes(List<PetSleepData> pets) {
        for (PetSleepData pet : pets) {
            pet.asleepAt = pet.pettedAt + SLEEP_DELAY_MS;
        }
        return pets;
    }

    /**
     * Group pets by sleep time window (within 1 minute of each other)
     * Returns a TreeMap sorted by time for consistent ordering
     */
    private Map<Long, List<PetSleepData>> groupPetsByTimeWindow(List<PetSleepData> pets) {
        // First, sort pets by asleepAt time
        pets.sort((p1, p2) -> Long.compare(p1.asleepAt, p2.asleepAt));

        // Group by time window
        Map<Long, List<PetSleepData>> windows = new TreeMap<>();

        for (PetSleepData pet : pets) {
            boolean added = false;

            // Check if pet fits in an existing window
            for (Map.Entry<Long, List<PetSleepData>> entry : windows.entrySet()) {
                long windowTime = entry.getKey();
                long timeDifference = Math.abs(pet.asleepAt - windowTime);

                if (timeDifference <= GROUPING_WINDOW_MS) {
                    entry.getValue().add(pet);
                    added = true;
                    Log.d(TAG, "Added " + pet.name + " to existing window at " + 
                          formatTimestamp(windowTime) + " (diff: " + timeDifference + "ms)");
                    break;
                }
            }

            // If pet doesn't fit in any window, create new window
            if (!added) {
                List<PetSleepData> newWindow = new ArrayList<>();
                newWindow.add(pet);
                windows.put(pet.asleepAt, newWindow);
                Log.d(TAG, "Created new time window at " + formatTimestamp(pet.asleepAt) + 
                      " for pet: " + pet.name);
            }
        }

        return windows;
    }

    /**
     * Create a notification group from pets in a time window
     */
    private NotificationGroup createNotificationGroup(List<PetSleepData> petsInWindow, long asleepAt) {
        NotificationGroup group = new NotificationGroup();
        group.category = "pet_sleep";
        group.earliestReadyTime = asleepAt;

        if (petsInWindow.size() == 1) {
            // Single pet: "{name} went to sleep!"
            PetSleepData pet = petsInWindow.get(0);
            group.name = pet.name + " went to sleep!";
            group.quantity = 1;
            group.details = pet.name; // Store pet name for later use
            group.groupId = generateGroupId("pet_single", pet.name, asleepAt);
            Log.d(TAG, "Single pet sleep notification: " + group.name);
        } else {
            // Multiple pets: "multiple pets went to sleep!"
            group.name = "multiple pets went to sleep!";
            group.quantity = petsInWindow.size();
            
            // Store pet names in details for reference
            StringBuilder petNames = new StringBuilder();
            for (int i = 0; i < petsInWindow.size(); i++) {
                if (i > 0) petNames.append(", ");
                petNames.append(petsInWindow.get(i).name);
            }
            group.details = petNames.toString();
            group.groupId = generateGroupId("pet_multiple", petNames.toString(), asleepAt);
            Log.d(TAG, "Multiple pets sleep notification: " + group.name + " (" + group.quantity + 
                  " pets: " + group.details + ")");
        }

        return group;
    }

    /**
     * Generate unique group ID for pet sleep notification
     */
    private String generateGroupId(String type, String identifier, long asleepAt) {
        // Round to nearest minute for consistent grouping
        long minuteBucket = (asleepAt / 60000) * 60000;
        return "pet_sleep_" + type + "_" + identifier.hashCode() + "_" + minuteBucket;
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
     * Internal class to hold pet sleep data during processing
     */
    private static class PetSleepData {
        String name;
        long pettedAt;
        long asleepAt;
        String category;

        PetSleepData(String name, long pettedAt, String category) {
            this.name = name;
            this.pettedAt = pettedAt;
            this.category = category;
            this.asleepAt = -1; // Will be calculated
        }

        @Override
        public String toString() {
            return name + " (pettedAt: " + pettedAt + ", asleepAt: " + asleepAt + ")";
        }
    }
}
