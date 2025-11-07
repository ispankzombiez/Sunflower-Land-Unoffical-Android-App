package com.sunflowerland.mobile.models;

public class FarmItem {
    private String id;            // Unique identifier (UUID for sunstones, plot ID for crops, etc.)
    private String category;      // "crops", "fruits", "resources", etc.
    private String name;          // "Corn", "Sunflower", etc.
    private int amount;           // Quantity of this item
    private long timestamp;       // Unix timestamp when ready/planted/mined/etc
    private String readyAt;       // ISO timestamp when ready (if applicable)
    private String plantedAt;     // ISO timestamp when planted (if applicable)
    private String chopedAt;      // ISO timestamp when chopped (if applicable)
    private String minedAt;       // ISO timestamp when mined (if applicable)
    private String buildingName;  // Building name (only for cooking items, e.g., "Fire Pit", "Bakery")
    private String details;       // Extra details (e.g., composter items "4 Sprout Mix, 1 Earthworm")

    // Constructor
    public FarmItem(String category, String name, int amount, long timestamp) {
        this.id = null;
        this.category = category;
        this.name = name;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    // Constructor with ID
    public FarmItem(String id, String category, String name, int amount, long timestamp) {
        this.id = id;
        this.category = category;
        this.name = name;
        this.amount = amount;
        this.timestamp = timestamp;
    }

    // Full constructor with all fields
    public FarmItem(String category, String name, int amount, long timestamp,
                    String readyAt, String plantedAt, String chopedAt, String minedAt) {
        this.id = null;
        this.category = category;
        this.name = name;
        this.amount = amount;
        this.timestamp = timestamp;
        this.readyAt = readyAt;
        this.plantedAt = plantedAt;
        this.chopedAt = chopedAt;
        this.minedAt = minedAt;
    }

    // Full constructor with ID
    public FarmItem(String id, String category, String name, int amount, long timestamp,
                    String readyAt, String plantedAt, String chopedAt, String minedAt) {
        this.id = id;
        this.category = category;
        this.name = name;
        this.amount = amount;
        this.timestamp = timestamp;
        this.readyAt = readyAt;
        this.plantedAt = plantedAt;
        this.chopedAt = chopedAt;
        this.minedAt = minedAt;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getAmount() { return amount; }
    public void setAmount(int amount) { this.amount = amount; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getReadyAt() { return readyAt; }
    public void setReadyAt(String readyAt) { this.readyAt = readyAt; }

    public String getPlantedAt() { return plantedAt; }
    public void setPlantedAt(String plantedAt) { this.plantedAt = plantedAt; }

    public String getChopedAt() { return chopedAt; }
    public void setChopedAt(String chopedAt) { this.chopedAt = chopedAt; }

    public String getMinedAt() { return minedAt; }
    public void setMinedAt(String minedAt) { this.minedAt = minedAt; }

    public String getBuildingName() { return buildingName; }
    public void setBuildingName(String buildingName) { this.buildingName = buildingName; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    @Override
    public String toString() {
        return "FarmItem{" +
                "category='" + category + '\'' +
                ", name='" + name + '\'' +
                ", amount=" + amount +
                ", timestamp=" + timestamp +
                '}';
    }
}
