package com.sfl.browser;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import com.sfl.browser.clustering.CategoryClusterer;
import com.sfl.browser.clustering.ClustererFactory;
import com.sfl.browser.clustering.NotificationGroup;
import com.sfl.browser.models.FarmItem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * NotificationTestingActivity - Tests the actual notification system without disturbing the API/scheduled notifications
 * 
 * Design:
 * - Generates test FarmItem objects that match real data structure from CategoryExtractors
 * - Routes through real CategoryClusterer (same as production notifications)
 * - Schedules through AlarmScheduler with 5-second delay for testing
 * - Each option directly reflects what can be notified from actual game data
 * - NO modifications to production notification pipeline
 * 
 * Real Notification Categories (from CategoryExtractors):
 * - "crops": All crops (23 types)
 * - "fruits": All fruit trees (9 types)
 * - "greenhouse_crops": Greenhouse plants (Rice, Olive, Grape)
 * - "resources": Mining resources (8 types)
 * - "animals": Animal production (Chicken, Cow, Sheep only)
 * - "animals_love": Animal love needed (Chicken, Cow, Sheep only)
 * - "cooking": Cooking recipes (50+ items)
 * - "composters": Compost Bin, Turbo Composter, Premium Composter
 * - "flowers": All flowers (30+ types)
 * - "crafting": Crafting box completions (24 dolls + 8 beds)
 * - "Beehive Swarm": When a beehive is about to swarm
 * - "Beehive Full": When honey is full
 * - "Crop Machine": Crop machine queue items
 * - "sunstones": Sunstone production
 * - "Daily Reset": Daily farm reset
 * - "floating_island": Floating island activities
 */
public class NotificationTestingActivity extends AppCompatActivity {
    
    private static final String TAG = "NotificationTestingActivity";
    private Spinner notificationTypeSpinner;
    private Button sendTestNotificationButton;
    
    // Data structure: display name -> [category, itemName, amount]
    private Map<String, TestOption> testOptions = new HashMap<>();
    
    private static class TestOption {
        String category;
        String itemName;
        int amount;
        
        TestOption(String category, String itemName) {
            this(category, itemName, 1);
        }
        
        TestOption(String category, String itemName, int amount) {
            this.category = category;
            this.itemName = itemName;
            this.amount = amount;
        }
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_testing);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Notification Testing");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        initializeTestOptions();
        
        notificationTypeSpinner = findViewById(R.id.notificationTypeSpinner);
        sendTestNotificationButton = findViewById(R.id.sendTestNotificationButton);
        
        // Create adapter from sorted option keys
        List<String> optionNames = new ArrayList<>(testOptions.keySet());
        optionNames.sort(String::compareTo);
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_item,
            optionNames.toArray(new String[0])
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        notificationTypeSpinner.setAdapter(adapter);
        
        sendTestNotificationButton.setOnClickListener(v -> sendTestNotification());
    }
    
    /**
     * Initialize all test notification options based on real game items from Constants
     */
    private void initializeTestOptions() {
        // === CROPS (from Constants.CROP_GROWTH_TIMES) ===
        addOption("Crop - Sunflower", "crops", "Sunflower");
        addOption("Crop - Potato", "crops", "Potato");
        addOption("Crop - Rhubarb", "crops", "Rhubarb");
        addOption("Crop - Pumpkin", "crops", "Pumpkin");
        addOption("Crop - Zucchini", "crops", "Zucchini");
        addOption("Crop - Carrot", "crops", "Carrot");
        addOption("Crop - Yam", "crops", "Yam");
        addOption("Crop - Cabbage", "crops", "Cabbage");
        addOption("Crop - Broccoli", "crops", "Broccoli");
        addOption("Crop - Soybean", "crops", "Soybean");
        addOption("Crop - Beetroot", "crops", "Beetroot");
        addOption("Crop - Pepper", "crops", "Pepper");
        addOption("Crop - Cauliflower", "crops", "Cauliflower");
        addOption("Crop - Parsnip", "crops", "Parsnip");
        addOption("Crop - Eggplant", "crops", "Eggplant");
        addOption("Crop - Corn", "crops", "Corn");
        addOption("Crop - Onion", "crops", "Onion");
        addOption("Crop - Radish", "crops", "Radish");
        addOption("Crop - Wheat", "crops", "Wheat");
        addOption("Crop - Turnip", "crops", "Turnip");
        addOption("Crop - Kale", "crops", "Kale");
        addOption("Crop - Artichoke", "crops", "Artichoke");
        addOption("Crop - Barley", "crops", "Barley");
        
        // === FRUITS (from Constants.FRUIT_GROWTH_TIMES) ===
        addOption("Fruit - Tomato", "fruits", "Tomato");
        addOption("Fruit - Lemon", "fruits", "Lemon");
        addOption("Fruit - Blueberry", "fruits", "Blueberry");
        addOption("Fruit - Orange", "fruits", "Orange");
        addOption("Fruit - Apple", "fruits", "Apple");
        addOption("Fruit - Banana", "fruits", "Banana");
        addOption("Fruit - Celestine", "fruits", "Celestine");
        addOption("Fruit - Lunara", "fruits", "Lunara");
        addOption("Fruit - Duskberry", "fruits", "Duskberry");
        
        // === GREENHOUSE CROPS ===
        addOption("Greenhouse - Rice", "greenhouse_crops", "Rice");
        addOption("Greenhouse - Olive", "greenhouse_crops", "Olive");
        addOption("Greenhouse - Grape", "greenhouse_crops", "Grape");
        
        // === RESOURCES ===
        addOption("Resource - Tree", "resources", "Tree");
        addOption("Resource - Stone", "resources", "Stone");
        addOption("Resource - Iron", "resources", "Iron");
        addOption("Resource - Gold", "resources", "Gold");
        addOption("Resource - Crimstone", "resources", "Crimstone");
        addOption("Resource - Oil", "resources", "Oil");
        addOption("Resource - Sunstone", "resources", "Sunstone");
        addOption("Resource - Obsidian", "resources", "Obsidian");
        
        // === ANIMALS - PRODUCTION (only Chicken, Cow, Sheep) ===
        addOption("Animal - Chicken Ready", "animals", "Chicken");
        addOption("Animal - Cow Ready", "animals", "Cow");
        addOption("Animal - Sheep Ready", "animals", "Sheep");
        
        // === ANIMALS - LOVE (only Chicken, Cow, Sheep) ===
        addOption("Animal - Chicken Needs Love", "animals_love", "Chicken");
        addOption("Animal - Cow Needs Love", "animals_love", "Cow");
        addOption("Animal - Sheep Needs Love", "animals_love", "Sheep");
        
        // === FLOWERS (from Constants.FLOWER_GROWTH_TIMES) ===
        // Pansies (1 day)
        addOption("Flower - Red Pansy", "flowers", "Red Pansy");
        addOption("Flower - Yellow Pansy", "flowers", "Yellow Pansy");
        addOption("Flower - Purple Pansy", "flowers", "Purple Pansy");
        addOption("Flower - White Pansy", "flowers", "White Pansy");
        addOption("Flower - Blue Pansy", "flowers", "Blue Pansy");
        // Cosmos (1 day)
        addOption("Flower - Red Cosmos", "flowers", "Red Cosmos");
        addOption("Flower - Yellow Cosmos", "flowers", "Yellow Cosmos");
        addOption("Flower - Purple Cosmos", "flowers", "Purple Cosmos");
        addOption("Flower - White Cosmos", "flowers", "White Cosmos");
        addOption("Flower - Blue Cosmos", "flowers", "Blue Cosmos");
        addOption("Flower - Prism Petal", "flowers", "Prism Petal");
        // Balloon Flowers (2 days)
        addOption("Flower - Red Balloon Flower", "flowers", "Red Balloon Flower");
        addOption("Flower - Yellow Balloon Flower", "flowers", "Yellow Balloon Flower");
        addOption("Flower - Purple Balloon Flower", "flowers", "Purple Balloon Flower");
        addOption("Flower - White Balloon Flower", "flowers", "White Balloon Flower");
        addOption("Flower - Blue Balloon Flower", "flowers", "Blue Balloon Flower");
        // Daffodils (2 days)
        addOption("Flower - Red Daffodil", "flowers", "Red Daffodil");
        addOption("Flower - Yellow Daffodil", "flowers", "Yellow Daffodil");
        addOption("Flower - Purple Daffodil", "flowers", "Purple Daffodil");
        addOption("Flower - White Daffodil", "flowers", "White Daffodil");
        addOption("Flower - Blue Daffodil", "flowers", "Blue Daffodil");
        addOption("Flower - Celestial Frostbloom", "flowers", "Celestial Frostbloom");
        // Edelweiss (3 days)
        addOption("Flower - Red Edelweiss", "flowers", "Red Edelweiss");
        addOption("Flower - Yellow Edelweiss", "flowers", "Yellow Edelweiss");
        addOption("Flower - Purple Edelweiss", "flowers", "Purple Edelweiss");
        addOption("Flower - White Edelweiss", "flowers", "White Edelweiss");
        addOption("Flower - Blue Edelweiss", "flowers", "Blue Edelweiss");
        // Gladiolus (3 days)
        addOption("Flower - Red Gladiolus", "flowers", "Red Gladiolus");
        addOption("Flower - Yellow Gladiolus", "flowers", "Yellow Gladiolus");
        addOption("Flower - Purple Gladiolus", "flowers", "Purple Gladiolus");
        addOption("Flower - White Gladiolus", "flowers", "White Gladiolus");
        addOption("Flower - Blue Gladiolus", "flowers", "Blue Gladiolus");
        // Lavender (3 days)
        addOption("Flower - Red Lavender", "flowers", "Red Lavender");
        addOption("Flower - Yellow Lavender", "flowers", "Yellow Lavender");
        addOption("Flower - Purple Lavender", "flowers", "Purple Lavender");
        addOption("Flower - White Lavender", "flowers", "White Lavender");
        addOption("Flower - Blue Lavender", "flowers", "Blue Lavender");
        // Carnation (5 days)
        addOption("Flower - Red Carnation", "flowers", "Red Carnation");
        addOption("Flower - Yellow Carnation", "flowers", "Yellow Carnation");
        addOption("Flower - Purple Carnation", "flowers", "Purple Carnation");
        addOption("Flower - White Carnation", "flowers", "White Carnation");
        addOption("Flower - Blue Carnation", "flowers", "Blue Carnation");
        // Clover (3 days)
        addOption("Flower - Red Clover", "flowers", "Red Clover");
        addOption("Flower - Yellow Clover", "flowers", "Yellow Clover");
        addOption("Flower - Purple Clover", "flowers", "Purple Clover");
        addOption("Flower - White Clover", "flowers", "White Clover");
        addOption("Flower - Blue Clover", "flowers", "Blue Clover");
        // Lotus (5 days)
        addOption("Flower - Red Lotus", "flowers", "Red Lotus");
        addOption("Flower - Yellow Lotus", "flowers", "Yellow Lotus");
        addOption("Flower - Purple Lotus", "flowers", "Purple Lotus");
        addOption("Flower - White Lotus", "flowers", "White Lotus");
        addOption("Flower - Blue Lotus", "flowers", "Blue Lotus");
        addOption("Flower - Primula Enigma", "flowers", "Primula Enigma");
        
        // === COOKING (from Constants.COOKING_TIMES) ===
        // Basic foods
        addOption("Cooking - Mashed Potato", "cooking", "Mashed Potato");
        addOption("Cooking - Pumpkin Soup", "cooking", "Pumpkin Soup");
        addOption("Cooking - Reindeer Carrot", "cooking", "Reindeer Carrot");
        addOption("Cooking - Mushroom Soup", "cooking", "Mushroom Soup");
        addOption("Cooking - Popcorn", "cooking", "Popcorn");
        addOption("Cooking - Bumpkin Broth", "cooking", "Bumpkin Broth");
        addOption("Cooking - Cabbers n Mash", "cooking", "Cabbers n Mash");
        addOption("Cooking - Boiled Eggs", "cooking", "Boiled Eggs");
        addOption("Cooking - Kale Stew", "cooking", "Kale Stew");
        addOption("Cooking - Kale Omelette", "cooking", "Kale Omelette");
        addOption("Cooking - Gumbo", "cooking", "Gumbo");
        addOption("Cooking - Rapid Roast", "cooking", "Rapid Roast");
        addOption("Cooking - Fried Tofu", "cooking", "Fried Tofu");
        addOption("Cooking - Rice Bun", "cooking", "Rice Bun");
        addOption("Cooking - Antipasto", "cooking", "Antipasto");
        addOption("Cooking - Pizza Margherita", "cooking", "Pizza Margherita");
        addOption("Cooking - Sunflower Crunch", "cooking", "Sunflower Crunch");
        addOption("Cooking - Mushroom Jacket Potatoes", "cooking", "Mushroom Jacket Potatoes");
        addOption("Cooking - Fruit Salad", "cooking", "Fruit Salad");
        addOption("Cooking - Pancakes", "cooking", "Pancakes");
        addOption("Cooking - Roast Veggies", "cooking", "Roast Veggies");
        addOption("Cooking - Cauliflower Burger", "cooking", "Cauliflower Burger");
        addOption("Cooking - Club Sandwich", "cooking", "Club Sandwich");
        addOption("Cooking - Bumpkin Salad", "cooking", "Bumpkin Salad");
        addOption("Cooking - Bumpkin Ganoush", "cooking", "Bumpkin Ganoush");
        addOption("Cooking - Goblin's Treat", "cooking", "Goblin's Treat");
        addOption("Cooking - Chowder", "cooking", "Chowder");
        addOption("Cooking - Bumpkin Roast", "cooking", "Bumpkin Roast");
        addOption("Cooking - Goblin Brunch", "cooking", "Goblin Brunch");
        addOption("Cooking - Beetroot Blaze", "cooking", "Beetroot Blaze");
        addOption("Cooking - Steamed Red Rice", "cooking", "Steamed Red Rice");
        addOption("Cooking - Tofu Scramble", "cooking", "Tofu Scramble");
        addOption("Cooking - Fried Calamari", "cooking", "Fried Calamari");
        addOption("Cooking - Fish Burger", "cooking", "Fish Burger");
        addOption("Cooking - Fish Omelette", "cooking", "Fish Omelette");
        addOption("Cooking - Ocean's Olive", "cooking", "Ocean's Olive");
        addOption("Cooking - Seafood Basket", "cooking", "Seafood Basket");
        addOption("Cooking - Fish n Chips", "cooking", "Fish n Chips");
        addOption("Cooking - Sushi Roll", "cooking", "Sushi Roll");
        addOption("Cooking - Caprese Salad", "cooking", "Caprese Salad");
        addOption("Cooking - Spaghetti al Limone", "cooking", "Spaghetti al Limone");
        // Cakes
        addOption("Cooking - Apple Pie", "cooking", "Apple Pie");
        addOption("Cooking - Orange Cake", "cooking", "Orange Cake");
        addOption("Cooking - Kale & Mushroom Pie", "cooking", "Kale & Mushroom Pie");
        addOption("Cooking - Sunflower Cake", "cooking", "Sunflower Cake");
        addOption("Cooking - Honey Cake", "cooking", "Honey Cake");
        addOption("Cooking - Potato Cake", "cooking", "Potato Cake");
        addOption("Cooking - Pumpkin Cake", "cooking", "Pumpkin Cake");
        addOption("Cooking - Cornbread", "cooking", "Cornbread");
        addOption("Cooking - Carrot Cake", "cooking", "Carrot Cake");
        addOption("Cooking - Cabbage Cake", "cooking", "Cabbage Cake");
        addOption("Cooking - Beetroot Cake", "cooking", "Beetroot Cake");
        addOption("Cooking - Cauliflower Cake", "cooking", "Cauliflower Cake");
        addOption("Cooking - Parsnip Cake", "cooking", "Parsnip Cake");
        addOption("Cooking - Eggplant Cake", "cooking", "Eggplant Cake");
        addOption("Cooking - Radish Cake", "cooking", "Radish Cake");
        addOption("Cooking - Wheat Cake", "cooking", "Wheat Cake");
        addOption("Cooking - Lemon Cheesecake", "cooking", "Lemon Cheesecake");
        // Preserves & fermented
        addOption("Cooking - Blueberry Jam", "cooking", "Blueberry Jam");
        addOption("Cooking - Fermented Carrots", "cooking", "Fermented Carrots");
        addOption("Cooking - Sauerkraut", "cooking", "Sauerkraut");
        addOption("Cooking - Fancy Fries", "cooking", "Fancy Fries");
        addOption("Cooking - Fermented Fish", "cooking", "Fermented Fish");
        addOption("Cooking - Shroom Syrup", "cooking", "Shroom Syrup");
        addOption("Cooking - Cheese", "cooking", "Cheese");
        addOption("Cooking - Blue Cheese", "cooking", "Blue Cheese");
        addOption("Cooking - Honey Cheddar", "cooking", "Honey Cheddar");
        // Juices & smoothies
        addOption("Cooking - Purple Smoothie", "cooking", "Purple Smoothie");
        addOption("Cooking - Orange Juice", "cooking", "Orange Juice");
        addOption("Cooking - Apple Juice", "cooking", "Apple Juice");
        addOption("Cooking - Power Smoothie", "cooking", "Power Smoothie");
        addOption("Cooking - Bumpkin Detox", "cooking", "Bumpkin Detox");
        addOption("Cooking - Banana Blast", "cooking", "Banana Blast");
        addOption("Cooking - Grape Juice", "cooking", "Grape Juice");
        addOption("Cooking - The Lot", "cooking", "The Lot");
        addOption("Cooking - Carrot Juice", "cooking", "Carrot Juice");
        addOption("Cooking - Quick Juice", "cooking", "Quick Juice");
        addOption("Cooking - Slow Juice", "cooking", "Slow Juice");
        addOption("Cooking - Sour Shake", "cooking", "Sour Shake");
        
        // === COMPOSTERS ===
        addOption("Composter - Compost Bin", "composters", "Compost Bin");
        addOption("Composter - Turbo Composter", "composters", "Turbo Composter");
        addOption("Composter - Premium Composter", "composters", "Premium Composter");
        
        // === CRAFTING - DOLLS (from Constants.CRAFTING_BOX_TIMES) ===
        addOption("Crafting - Doll", "crafting", "Doll");
        addOption("Crafting - Angler Doll", "crafting", "Angler Doll");
        addOption("Crafting - Bigfin Doll", "crafting", "Bigfin Doll");
        addOption("Crafting - Bloom Doll", "crafting", "Bloom Doll");
        addOption("Crafting - Buzz Doll", "crafting", "Buzz Doll");
        addOption("Crafting - Cluck Doll", "crafting", "Cluck Doll");
        addOption("Crafting - Cosmo Doll", "crafting", "Cosmo Doll");
        addOption("Crafting - Crude Doll", "crafting", "Crude Doll");
        addOption("Crafting - Dune Doll", "crafting", "Dune Doll");
        addOption("Crafting - Ember Doll", "crafting", "Ember Doll");
        addOption("Crafting - Frosty Doll", "crafting", "Frosty Doll");
        addOption("Crafting - Gilded Doll", "crafting", "Gilded Doll");
        addOption("Crafting - Grubby Doll", "crafting", "Grubby Doll");
        addOption("Crafting - Harvest Doll", "crafting", "Harvest Doll");
        addOption("Crafting - Juicy Doll", "crafting", "Juicy Doll");
        addOption("Crafting - Lumber Doll", "crafting", "Lumber Doll");
        addOption("Crafting - Lunar Doll", "crafting", "Lunar Doll");
        addOption("Crafting - Moo Doll", "crafting", "Moo Doll");
        addOption("Crafting - Mouse Doll", "crafting", "Mouse Doll");
        addOption("Crafting - Nefari Doll", "crafting", "Nefari Doll");
        addOption("Crafting - Shadow Doll", "crafting", "Shadow Doll");
        addOption("Crafting - Sizzle Doll", "crafting", "Sizzle Doll");
        addOption("Crafting - Solar Doll", "crafting", "Solar Doll");
        addOption("Crafting - Wolly Doll", "crafting", "Wolly Doll");
        
        // === CRAFTING - BEDS ===
        addOption("Crafting - Fisher Bed", "crafting", "Fisher Bed");
        addOption("Crafting - Basic Bed", "crafting", "Basic Bed");
        addOption("Crafting - Floral Bed", "crafting", "Floral Bed");
        addOption("Crafting - Desert Bed", "crafting", "Desert Bed");
        addOption("Crafting - Sturdy Bed", "crafting", "Sturdy Bed");
        addOption("Crafting - Cow Bed", "crafting", "Cow Bed");
        addOption("Crafting - Pirate Bed", "crafting", "Pirate Bed");
        addOption("Crafting - Royal Bed", "crafting", "Royal Bed");
        
        // === BEEHIVES ===
        addOption("Beehive - Fullness Alert", "Beehive Full", "Beehive 1");
        addOption("Beehive - Swarm Alert", "Beehive Swarm", "Beehive 1");
        
        // === CROP MACHINE - with seed amounts ===
        addOption("Crop Machine - Sunflower (10 seeds)", "Crop Machine", "Sunflower", 10);
        addOption("Crop Machine - Potato (5 seeds)", "Crop Machine", "Potato", 5);
        addOption("Crop Machine - Corn (20 seeds)", "Crop Machine", "Corn", 20);
        
        // === OTHER BUILDINGS ===
        addOption("Sunstone - Production Ready", "sunstones", "Sunstone");
        addOption("Daily Reset", "Daily Reset", "Daily Reset");
        addOption("Floating Island - Event", "floating_island", "Floating Island");
    }
    
    private void addOption(String displayName, String category, String itemName) {
        testOptions.put(displayName, new TestOption(category, itemName, 1));
    }
    
    private void addOption(String displayName, String category, String itemName, int amount) {
        testOptions.put(displayName, new TestOption(category, itemName, amount));
    }
    
    private void sendTestNotification() {
        try {
            String selectedDisplay = (String) notificationTypeSpinner.getSelectedItem();
            if (selectedDisplay == null) {
                Toast.makeText(this, "No notification type selected", Toast.LENGTH_SHORT).show();
                return;
            }
            
            TestOption option = testOptions.get(selectedDisplay);
            if (option == null) {
                Toast.makeText(this, "Unknown option: " + selectedDisplay, Toast.LENGTH_SHORT).show();
                return;
            }
            
            Log.i(TAG, "Sending test notification: " + selectedDisplay + 
                  " (category: " + option.category + ", item: " + option.itemName + ", amount: " + option.amount + ")");
            
            // Create FarmItem - will be adjusted to fire in 5 seconds
            long now = System.currentTimeMillis();
            FarmItem testItem = new FarmItem(option.category, option.itemName, option.amount, now + 10000);
            
            // Process through real notification pipeline
            processTestNotification(testItem);
            
        } catch (Exception e) {
            Log.e(TAG, "Error: " + e.getMessage(), e);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Process test item through real notification pipeline
     */
    private void processTestNotification(FarmItem testItem) {
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String category = testItem.getCategory();
            
            // Check if category is enabled
            String categoryKey = "category_" + category;
            boolean categoryEnabled = prefs.getBoolean(categoryKey, true);
            
            if (!categoryEnabled) {
                Log.w(TAG, "Category '" + category + "' is disabled in settings");
                Toast.makeText(this, "Category '" + category + "' is disabled in settings", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Adjust timestamp to fire in ~5 seconds from now
            long adjustedTime = System.currentTimeMillis() + 5000;
            testItem.setTimestamp(adjustedTime);
            
            Log.d(TAG, "Adjusted timestamp to: " + adjustedTime + " (5 seconds from now)");
            
            // Get the real clusterer for this category
            CategoryClusterer clusterer = ClustererFactory.getClusterer(category, this);
            if (clusterer == null) {
                Log.w(TAG, "No clusterer found for category: " + category);
                Toast.makeText(this, "No clusterer for: " + category, Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Cluster the item (simulates what production notifications do)
            List<NotificationGroup> groups = clusterer.cluster(Arrays.asList(testItem));
            
            Log.i(TAG, "Created " + groups.size() + " notification group(s)");
            
            // Schedule through real AlarmScheduler
            AlarmScheduler scheduler = new AlarmScheduler(this);
            scheduler.scheduleNotificationAlarms(groups);
            
            Toast.makeText(this, "Test notification scheduled! Should appear in ~5 seconds.", Toast.LENGTH_SHORT).show();
            Log.i(TAG, "Successfully scheduled test notification through AlarmScheduler");
            
        } catch (Exception e) {
            Log.e(TAG, "Error in processTestNotification: " + e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
