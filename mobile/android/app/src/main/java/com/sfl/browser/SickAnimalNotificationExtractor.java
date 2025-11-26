package com.sfl.browser;

import com.sfl.browser.clustering.NotificationGroup;
import com.sfl.browser.models.SickAnimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extractor for sick animal notifications.
 * Detects newly sick animals and creates aggregated notifications.
 */
public class SickAnimalNotificationExtractor {

    /**
     * Create a notification group for newly sick animals.
     * Aggregates by animal type with counts.
     *
     * @param newlySickAnimals List of newly sick animals from SickAnimalTracker
     * @return NotificationGroup containing the aggregated sick animal notification, or null if no new sicknesses
     */
    public static NotificationGroup createSickAnimalNotification(List<SickAnimal> newlySickAnimals) {
        if (newlySickAnimals == null || newlySickAnimals.isEmpty()) {
            return null;
        }

        // Aggregate by animal type with counts
        Map<String, Integer> typeCounts = new HashMap<>();
        for (SickAnimal animal : newlySickAnimals) {
            typeCounts.put(animal.type, typeCounts.getOrDefault(animal.type, 0) + 1);
        }

        // Build the notification body with counts
        StringBuilder bodyBuilder = new StringBuilder();
        for (String type : typeCounts.keySet()) {
            int count = typeCounts.get(type);
            if (bodyBuilder.length() > 0) {
                bodyBuilder.append(", ");
            }

            // Format: "2 Chickens" or just "Chicken" if count is 1
            if (count > 1) {
                bodyBuilder.append(count).append(" ").append(getPluralForm(type));
            } else {
                bodyBuilder.append(type);
            }
        }

        String title = "Animals just got sick!";
        String body = bodyBuilder.toString();

        DebugLog.log("🐔 SICK ANIMAL NOTIFICATION: " + title + " - " + body);

        // Create NotificationGroup with category = "animal_sick"
        NotificationGroup group = new NotificationGroup();
        group.category = "animal_sick";
        group.name = body; // Store the formatted body in name field
        group.quantity = newlySickAnimals.size();
        group.earliestReadyTime = System.currentTimeMillis();
        group.groupId = "animal_sick_" + System.currentTimeMillis(); // Unique ID for tracking
        group.details = null;

        return group;
    }

    /**
     * Get the plural form of an animal type.
     *
     * @param animalType The singular animal type (e.g., "Chicken", "Cow", "Sheep")
     * @return The plural form
     */
    private static String getPluralForm(String animalType) {
        switch (animalType) {
            case "Chicken":
                return "Chickens";
            case "Cow":
                return "Cows";
            case "Sheep":
                return "Sheep";
            default:
                return animalType + "s";
        }
    }
}
