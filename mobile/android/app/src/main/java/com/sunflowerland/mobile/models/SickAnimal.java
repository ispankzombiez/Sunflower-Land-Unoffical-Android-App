package com.sunflowerland.mobile.models;

/**
 * Represents a sick animal for tracking purposes
 * Used to detect NEW sicknesses and avoid duplicate notifications
 */
public class SickAnimal {
    public String type;      // "Cow", "Sheep", "Chicken"
    public String id;        // Animal ID from API
    public String state;     // "sick", "idle", etc.
    public long detectedAt;  // Timestamp when sickness was detected

    public SickAnimal() {
    }

    public SickAnimal(String type, String id, String state, long detectedAt) {
        this.type = type;
        this.id = id;
        this.state = state;
        this.detectedAt = detectedAt;
    }

    @Override
    public String toString() {
        return type + " (" + id + "): " + state;
    }

    /**
     * Create a unique key for this animal (for deduplication)
     */
    public String getKey() {
        return type + ":" + id;
    }
}
