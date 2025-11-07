package com.sunflowerland.mobile;

import java.util.HashMap;
import java.util.Map;

/**
 * Constants for Sunflower Land game mechanics
 * Growth times extracted from game source code
 */
public class Constants {
    
    /**
     * Crop growth times in milliseconds
     * Based on harvestSeconds from game source
     */
    public static final Map<String, Long> CROP_GROWTH_TIMES = new HashMap<String, Long>() {{
        // Basic Crops (up to 30 minutes)
        put("Sunflower", 1L * 60 * 1000);           // 1 minute
        put("Potato", 5L * 60 * 1000);              // 5 minutes
        put("Rhubarb", 10L * 60 * 1000);            // 10 minutes
        put("Pumpkin", 30L * 60 * 1000);            // 30 minutes
        put("Zucchini", 30L * 60 * 1000);           // 30 minutes
        
        // Medium Crops (1-4 hours)
        put("Carrot", 60L * 60 * 1000);             // 1 hour
        put("Yam", 60L * 60 * 1000);                // 1 hour
        put("Cabbage", 2L * 60 * 60 * 1000);        // 2 hours
        put("Broccoli", 2L * 60 * 60 * 1000);       // 2 hours
        put("Soybean", 3L * 60 * 60 * 1000);        // 3 hours
        put("Beetroot", 4L * 60 * 60 * 1000);       // 4 hours
        put("Pepper", 4L * 60 * 60 * 1000);         // 4 hours
        
        // Advanced Crops (8-20 hours)
        put("Cauliflower", 8L * 60 * 60 * 1000);    // 8 hours
        put("Parsnip", 12L * 60 * 60 * 1000);       // 12 hours
        put("Eggplant", 16L * 60 * 60 * 1000);      // 16 hours
        put("Corn", 20L * 60 * 60 * 1000);          // 20 hours
        put("Onion", 20L * 60 * 60 * 1000);         // 20 hours
        
        // Overnight Crops (24+ hours)
        put("Radish", 24L * 60 * 60 * 1000);        // 24 hours
        put("Wheat", 24L * 60 * 60 * 1000);         // 24 hours
        put("Turnip", 24L * 60 * 60 * 1000);        // 24 hours
        put("Kale", 36L * 60 * 60 * 1000);          // 36 hours
        put("Artichoke", 36L * 60 * 60 * 1000);     // 36 hours
        put("Barley", 48L * 60 * 60 * 1000);        // 48 hours
        
        // Greenhouse Crops
        put("Rice", 32L * 60 * 60 * 1000);          // 32 hours (1 day 8 hours)
        put("Olive", 44L * 60 * 60 * 1000);         // 44 hours (1 day 20 hours)
        put("Grape", 12L * 60 * 60 * 1000);         // 12 hours
    }};
    
    /**
     * Fruit growth times in milliseconds
     * Fruits have multiple harvests - this is time until first harvest
     */
    public static final Map<String, Long> FRUIT_GROWTH_TIMES = new HashMap<String, Long>() {{
        put("Tomato", 2L * 60 * 60 * 1000);          // 2 hours
        put("Lemon", 4L * 60 * 60 * 1000);           // 4 hours
        put("Blueberry", 6L * 60 * 60 * 1000);       // 6 hours
        put("Orange", 8L * 60 * 60 * 1000);          // 8 hours
        put("Apple", 12L * 60 * 60 * 1000);          // 12 hours
        put("Banana", 12L * 60 * 60 * 1000);         // 12 hours
        put("Celestine", 6L * 60 * 60 * 1000);       // 6 hours
        put("Lunara", 12L * 60 * 60 * 1000);         // 12 hours
        put("Duskberry", 24L * 60 * 60 * 1000);      // 24 hours
    }};
    
    /**
     * Greenhouse crop growth times in milliseconds
     * Greenhouse crops (Olive, Rice, Grape) grow in the greenhouse building
     * They have longer growth times than regular crops
     */
    public static final Map<String, Long> GREENHOUSE_CROP_GROWTH_TIMES = new HashMap<String, Long>() {{
        put("Rice", 32L * 60 * 60 * 1000);           // 32 hours (1 day 8 hours)
        put("Olive", 44L * 60 * 60 * 1000);          // 44 hours (1 day 20 hours)
        put("Grape", 12L * 60 * 60 * 1000);          // 12 hours
    }};
    
    /**
     * Flower growth times in milliseconds
     */
    public static final Map<String, Long> FLOWER_GROWTH_TIMES = new HashMap<String, Long>() {{
        // Pansies - 1 day
        put("Red Pansy", 1L * 24 * 60 * 60 * 1000);
        put("Yellow Pansy", 1L * 24 * 60 * 60 * 1000);
        put("Purple Pansy", 1L * 24 * 60 * 60 * 1000);
        put("White Pansy", 1L * 24 * 60 * 60 * 1000);
        put("Blue Pansy", 1L * 24 * 60 * 60 * 1000);
        
        // Cosmos - 1 day
        put("Red Cosmos", 1L * 24 * 60 * 60 * 1000);
        put("Yellow Cosmos", 1L * 24 * 60 * 60 * 1000);
        put("Purple Cosmos", 1L * 24 * 60 * 60 * 1000);
        put("White Cosmos", 1L * 24 * 60 * 60 * 1000);
        put("Blue Cosmos", 1L * 24 * 60 * 60 * 1000);
        put("Prism Petal", 1L * 24 * 60 * 60 * 1000);
        
        // Balloon Flowers - 2 days
        put("Red Balloon Flower", 2L * 24 * 60 * 60 * 1000);
        put("Yellow Balloon Flower", 2L * 24 * 60 * 60 * 1000);
        put("Purple Balloon Flower", 2L * 24 * 60 * 60 * 1000);
        put("White Balloon Flower", 2L * 24 * 60 * 60 * 1000);
        put("Blue Balloon Flower", 2L * 24 * 60 * 60 * 1000);
        
        // Daffodils - 2 days
        put("Red Daffodil", 2L * 24 * 60 * 60 * 1000);
        put("Yellow Daffodil", 2L * 24 * 60 * 60 * 1000);
        put("Purple Daffodil", 2L * 24 * 60 * 60 * 1000);
        put("White Daffodil", 2L * 24 * 60 * 60 * 1000);
        put("Blue Daffodil", 2L * 24 * 60 * 60 * 1000);
        put("Celestial Frostbloom", 2L * 24 * 60 * 60 * 1000);
        
        // Edelweiss - 3 days
        put("Red Edelweiss", 3L * 24 * 60 * 60 * 1000);
        put("Yellow Edelweiss", 3L * 24 * 60 * 60 * 1000);
        put("Purple Edelweiss", 3L * 24 * 60 * 60 * 1000);
        put("White Edelweiss", 3L * 24 * 60 * 60 * 1000);
        put("Blue Edelweiss", 3L * 24 * 60 * 60 * 1000);
        
        // Gladiolus - 3 days
        put("Red Gladiolus", 3L * 24 * 60 * 60 * 1000);
        put("Yellow Gladiolus", 3L * 24 * 60 * 60 * 1000);
        put("Purple Gladiolus", 3L * 24 * 60 * 60 * 1000);
        put("White Gladiolus", 3L * 24 * 60 * 60 * 1000);
        put("Blue Gladiolus", 3L * 24 * 60 * 60 * 1000);
        
        // Lavender - 3 days
        put("Red Lavender", 3L * 24 * 60 * 60 * 1000);
        put("Yellow Lavender", 3L * 24 * 60 * 60 * 1000);
        put("Purple Lavender", 3L * 24 * 60 * 60 * 1000);
        put("White Lavender", 3L * 24 * 60 * 60 * 1000);
        put("Blue Lavender", 3L * 24 * 60 * 60 * 1000);
        
        // Carnations - 5 days
        put("Red Carnation", 5L * 24 * 60 * 60 * 1000);
        put("Yellow Carnation", 5L * 24 * 60 * 60 * 1000);
        put("Purple Carnation", 5L * 24 * 60 * 60 * 1000);
        put("White Carnation", 5L * 24 * 60 * 60 * 1000);
        put("Blue Carnation", 5L * 24 * 60 * 60 * 1000);
        
        // Clover - 3 days
        put("Red Clover", 3L * 24 * 60 * 60 * 1000);
        put("Yellow Clover", 3L * 24 * 60 * 60 * 1000);
        put("Purple Clover", 3L * 24 * 60 * 60 * 1000);
        put("White Clover", 3L * 24 * 60 * 60 * 1000);
        put("Blue Clover", 3L * 24 * 60 * 60 * 1000);
        
        // Lotus - 5 days
        put("Red Lotus", 5L * 24 * 60 * 60 * 1000);
        put("Yellow Lotus", 5L * 24 * 60 * 60 * 1000);
        put("Purple Lotus", 5L * 24 * 60 * 60 * 1000);
        put("White Lotus", 5L * 24 * 60 * 60 * 1000);
        put("Blue Lotus", 5L * 24 * 60 * 60 * 1000);
        put("Primula Enigma", 5L * 24 * 60 * 60 * 1000);
    }};
    
    /**
     * Cooking times in milliseconds
     */
    public static final Map<String, Long> COOKING_TIMES = new HashMap<String, Long>() {{
        // Basic foods
        put("Mashed Potato", 30L * 1000);                    // 30 seconds
        put("Pumpkin Soup", 3L * 60 * 1000);                 // 3 minutes
        put("Reindeer Carrot", 5L * 60 * 1000);              // 5 minutes
        put("Mushroom Soup", 10L * 60 * 1000);               // 10 minutes
        put("Popcorn", 12L * 60 * 1000);                     // 12 minutes
        put("Bumpkin Broth", 20L * 60 * 1000);               // 20 minutes
        put("Cabbers n Mash", 40L * 60 * 1000);              // 40 minutes
        put("Boiled Eggs", 60L * 60 * 1000);                 // 1 hour
        put("Kale Stew", 2L * 60 * 60 * 1000);               // 2 hours
        put("Kale Omelette", (long)(3.5 * 60 * 60 * 1000));  // 3.5 hours
        put("Gumbo", 4L * 60 * 60 * 1000);                   // 4 hours
        put("Rapid Roast", 10L * 1000);                      // 10 seconds
        put("Fried Tofu", 90L * 60 * 1000);                  // 90 minutes
        put("Rice Bun", 300L * 60 * 1000);                   // 300 minutes (5 hours)
        put("Antipasto", 180L * 60 * 1000);                  // 180 minutes (3 hours)
        put("Pizza Margherita", 20L * 60 * 60 * 1000);       // 20 hours
        
        // More foods
        put("Sunflower Crunch", 10L * 60 * 1000);            // 10 minutes
        put("Mushroom Jacket Potatoes", 10L * 60 * 1000);    // 10 minutes
        put("Fruit Salad", 30L * 60 * 1000);                 // 30 minutes
        put("Pancakes", 60L * 60 * 1000);                    // 1 hour
        put("Roast Veggies", 2L * 60 * 60 * 1000);           // 2 hours
        put("Cauliflower Burger", 3L * 60 * 60 * 1000);      // 3 hours
        put("Club Sandwich", 3L * 60 * 60 * 1000);           // 3 hours
        put("Bumpkin Salad", (long)(3.5 * 60 * 60 * 1000));  // 3.5 hours
        put("Bumpkin Ganoush", 5L * 60 * 60 * 1000);         // 5 hours
        put("Goblin's Treat", 6L * 60 * 60 * 1000);          // 6 hours
        put("Chowder", 8L * 60 * 60 * 1000);                 // 8 hours
        put("Bumpkin Roast", 12L * 60 * 60 * 1000);          // 12 hours
        put("Goblin Brunch", 12L * 60 * 60 * 1000);          // 12 hours
        put("Beetroot Blaze", 30L * 1000);                   // 30 seconds
        put("Steamed Red Rice", 4L * 60 * 60 * 1000);        // 4 hours
        put("Tofu Scramble", 3L * 60 * 60 * 1000);           // 3 hours
        put("Fried Calamari", 5L * 60 * 60 * 1000);          // 5 hours
        put("Fish Burger", 2L * 60 * 60 * 1000);             // 2 hours
        put("Fish Omelette", 5L * 60 * 60 * 1000);           // 5 hours
        put("Ocean's Olive", 2L * 60 * 60 * 1000);           // 2 hours
        put("Seafood Basket", 5L * 60 * 60 * 1000);          // 5 hours
        put("Fish n Chips", 4L * 60 * 60 * 1000);            // 4 hours
        put("Sushi Roll", 60L * 60 * 1000);                  // 1 hour
        put("Caprese Salad", 3L * 60 * 60 * 1000);           // 3 hours
        put("Spaghetti al Limone", 15L * 60 * 60 * 1000);    // 15 hours
        
        // Cakes
        put("Apple Pie", 240L * 60 * 1000);                  // 240 minutes (4 hours)
        put("Orange Cake", 240L * 60 * 1000);                // 240 minutes (4 hours)
        put("Kale & Mushroom Pie", 240L * 60 * 1000);        // 240 minutes (4 hours)
        put("Sunflower Cake", (long)(6.5 * 60 * 60 * 1000)); // 6.5 hours
        put("Honey Cake", 8L * 60 * 60 * 1000);              // 8 hours
        put("Potato Cake", (long)(10.5 * 60 * 60 * 1000));   // 10.5 hours
        put("Pumpkin Cake", (long)(10.5 * 60 * 60 * 1000));  // 10.5 hours
        put("Cornbread", 12L * 60 * 60 * 1000);              // 12 hours
        put("Carrot Cake", 13L * 60 * 60 * 1000);            // 13 hours
        put("Cabbage Cake", 15L * 60 * 60 * 1000);           // 15 hours
        put("Beetroot Cake", 22L * 60 * 60 * 1000);          // 22 hours
        put("Cauliflower Cake", 22L * 60 * 60 * 1000);       // 22 hours
        put("Parsnip Cake", 24L * 60 * 60 * 1000);           // 24 hours
        put("Eggplant Cake", 24L * 60 * 60 * 1000);          // 24 hours
        put("Radish Cake", 24L * 60 * 60 * 1000);            // 24 hours
        put("Wheat Cake", 24L * 60 * 60 * 1000);             // 24 hours
        put("Lemon Cheesecake", 30L * 60 * 60 * 1000);       // 30 hours
        
        // Preserves & fermented
        put("Blueberry Jam", 12L * 60 * 60 * 1000);          // 12 hours
        put("Fermented Carrots", 24L * 60 * 60 * 1000);      // 24 hours
        put("Sauerkraut", 24L * 60 * 60 * 1000);             // 24 hours
        put("Fancy Fries", 24L * 60 * 60 * 1000);            // 24 hours
        put("Fermented Fish", 24L * 60 * 60 * 1000);         // 24 hours
        put("Shroom Syrup", 10L * 1000);                     // 10 seconds
        put("Cheese", 20L * 60 * 1000);                      // 20 minutes
        put("Blue Cheese", 3L * 60 * 60 * 1000);             // 3 hours
        put("Honey Cheddar", 12L * 60 * 60 * 1000);          // 12 hours
        
        // Juices & smoothies
        put("Purple Smoothie", 30L * 60 * 1000);             // 30 minutes
        put("Orange Juice", 45L * 60 * 1000);                // 45 minutes
        put("Apple Juice", 60L * 60 * 1000);                 // 1 hour
        put("Power Smoothie", (long)(1.5 * 60 * 60 * 1000)); // 1.5 hours
        put("Bumpkin Detox", 2L * 60 * 60 * 1000);           // 2 hours
        put("Banana Blast", 3L * 60 * 60 * 1000);            // 3 hours
        put("Grape Juice", 3L * 60 * 60 * 1000);             // 3 hours
        put("The Lot", (long)(3.5 * 60 * 60 * 1000));        // 3.5 hours
        put("Carrot Juice", 60L * 60 * 1000);                // 1 hour
        put("Quick Juice", 30L * 60 * 1000);                 // 30 minutes
        put("Slow Juice", 24L * 60 * 60 * 1000);             // 24 hours
        put("Sour Shake", 60L * 60 * 1000);                  // 1 hour
    }};
    
    /**
     * Animal production times in milliseconds (time until next egg/wool/milk)
     */
    public static final Map<String, Long> ANIMAL_PRODUCTION_TIMES = new HashMap<String, Long>() {{
        put("Chicken", 24L * 60 * 60 * 1000);        // 24 hours - egg production (Hen House)
        put("Cow", 24L * 60 * 60 * 1000);            // 24 hours - milk production (Barn)
        put("Sheep", 24L * 60 * 60 * 1000);          // 24 hours - wool production (Barn)
    }};
    
    /**
     * Resource replenishment times in milliseconds (time until respawn/regeneration)
     */
    public static final Map<String, Long> RESOURCE_REPLENISH_TIMES = new HashMap<String, Long>() {{
        put("Tree", 2L * 60 * 60 * 1000);            // 2 hours
        put("Stone", 4L * 60 * 60 * 1000);           // 4 hours
        put("Iron", 8L * 60 * 60 * 1000);            // 8 hours
        put("Gold", 24L * 60 * 60 * 1000);           // 24 hours
        put("Crimstone", 24L * 60 * 60 * 1000);      // 24 hours
        put("Oil", 20L * 60 * 60 * 1000);            // 20 hours
        put("Sunstone", 3L * 24 * 60 * 60 * 1000);   // 3 days
        put("Lavapit", 3L * 24 * 60 * 60 * 1000);    // 3 days
        put("Obsidian", 3L * 24 * 60 * 60 * 1000);   // 3 days (comes from lavapits)
    }};
    
    /**
     * Composter completion times in milliseconds
     */
    public static final Map<String, Long> COMPOSTER_TIMES = new HashMap<String, Long>() {{
        put("Compost Bin", 6L * 60 * 60 * 1000);             // 6 hours
        put("Compost Bin Egg Boost", 2L * 60 * 60 * 1000);   // 2 hours (with egg boost)
        put("Turbo Composter", 8L * 60 * 60 * 1000);         // 8 hours
        put("Turbo Composter Egg Boost", 3L * 60 * 60 * 1000); // 3 hours (with egg boost)
        put("Premium Composter", 12L * 60 * 60 * 1000);      // 12 hours
        put("Premium Composter Egg Boost", 4L * 60 * 60 * 1000); // 4 hours (with egg boost)
    }};
    
    /**
     * Crafting Box completion times in milliseconds
     */
    public static final Map<String, Long> CRAFTING_BOX_TIMES = new HashMap<String, Long>() {{
        // Dolls
        put("Doll", 2L * 60 * 60 * 1000);                    // 2 hours
        put("Angler Doll", 8L * 60 * 60 * 1000);             // 8 hours
        put("Bigfin Doll", 8L * 60 * 60 * 1000);             // 8 hours
        put("Bloom Doll", 8L * 60 * 60 * 1000);              // 8 hours
        put("Buzz Doll", 8L * 60 * 60 * 1000);               // 8 hours
        put("Cluck Doll", 8L * 60 * 60 * 1000);              // 8 hours
        put("Cosmo Doll", 8L * 60 * 60 * 1000);              // 8 hours
        put("Crude Doll", 8L * 60 * 60 * 1000);              // 8 hours
        put("Dune Doll", 8L * 60 * 60 * 1000);               // 8 hours
        put("Ember Doll", 8L * 60 * 60 * 1000);              // 8 hours
        put("Frosty Doll", 8L * 60 * 60 * 1000);             // 8 hours
        put("Gilded Doll", 8L * 60 * 60 * 1000);             // 8 hours
        put("Grubby Doll", 8L * 60 * 60 * 1000);             // 8 hours
        put("Harvest Doll", 8L * 60 * 60 * 1000);            // 8 hours
        put("Juicy Doll", 8L * 60 * 60 * 1000);              // 8 hours
        put("Lumber Doll", 8L * 60 * 60 * 1000);             // 8 hours
        put("Lunar Doll", 8L * 60 * 60 * 1000);              // 8 hours
        put("Moo Doll", 8L * 60 * 60 * 1000);                // 8 hours
        put("Mouse Doll", 8L * 60 * 60 * 1000);              // 8 hours
        put("Nefari Doll", 8L * 60 * 60 * 1000);             // 8 hours
        put("Shadow Doll", 8L * 60 * 60 * 1000);             // 8 hours
        put("Sizzle Doll", 8L * 60 * 60 * 1000);             // 8 hours
        put("Solar Doll", 8L * 60 * 60 * 1000);              // 8 hours
        put("Wolly Doll", 8L * 60 * 60 * 1000);              // 8 hours
        
        // Beds
        put("Fisher Bed", 8L * 60 * 60 * 1000);              // 8 hours
        put("Basic Bed", 8L * 60 * 60 * 1000);               // 8 hours
        put("Floral Bed", 8L * 60 * 60 * 1000);              // 8 hours
        put("Desert Bed", 8L * 60 * 60 * 1000);              // 8 hours
        put("Sturdy Bed", 8L * 60 * 60 * 1000);              // 8 hours
        put("Cow Bed", 8L * 60 * 60 * 1000);                 // 8 hours
        put("Pirate Bed", 8L * 60 * 60 * 1000);              // 8 hours
        put("Royal Bed", 8L * 60 * 60 * 1000);               // 8 hours
    }};
    
    /**
     * Crop Machine completion times in milliseconds
     * NOTE: This is for seed packs with timestamps. Format needs investigation via logs.
     * TODO: Revisit after examining farm data to determine exact data format
     */
    public static final Map<String, Long> CROP_MACHINE_TIMES = new HashMap<String, Long>() {{
        // Placeholder - will populate once we understand the data format from logs
    }};
    
    /**
     * Get a unique notification ID for a crop type
     * Uses hash of crop name to ensure consistent IDs across app restarts
     */
    public static int getNotificationIdForCrop(String cropName) {
        // Use hashCode to get a consistent ID for each crop type
        // Add offset to avoid conflicts with other notification types
        return 1000 + Math.abs(cropName.hashCode() % 10000);
    }
    
    /**
     * Get growth time for a crop in milliseconds
     * @param cropName The name of the crop
     * @return Growth time in milliseconds, or null if crop not found
     */
    public static Long getCropGrowthTime(String cropName) {
        return CROP_GROWTH_TIMES.get(cropName);
    }
    
    /**
     * Check if a crop is a basic crop (harvest <= 30 minutes)
     */
    public static boolean isBasicCrop(String cropName) {
        Long growthTime = CROP_GROWTH_TIMES.get(cropName);
        if (growthTime == null) return false;
        return growthTime <= 30L * 60 * 1000;
    }
    
    /**
     * Check if a crop is an overnight crop (harvest >= 24 hours)
     */
    public static boolean isOvernightCrop(String cropName) {
        Long growthTime = CROP_GROWTH_TIMES.get(cropName);
        if (growthTime == null) return false;
        return growthTime >= 24L * 60 * 60 * 1000;
    }
    
    /**
     * Get a human-readable time string for a duration in milliseconds
     */
    public static String formatDuration(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        } else if (minutes > 0) {
            return minutes + "m";
        } else {
            return seconds + "s";
        }
    }
}
