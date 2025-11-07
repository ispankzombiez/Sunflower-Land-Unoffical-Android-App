package com.sunflowerland.mobile.clustering;

import android.content.Context;
import android.util.Log;

/**
 * Factory for getting the appropriate clusterer for each category
 * Centralizes clustering logic by category type
 */
public class ClustererFactory {
    private static final String TAG = "ClustererFactory";
    
    public static CategoryClusterer getClusterer(String category, Context context) {
        switch (category.toLowerCase()) {
            case "crops":
                Log.d(TAG, "Using CropClusterer for category: " + category);
                return new CropClusterer();
            
            case "fruits":
                Log.d(TAG, "Using FruitClusterer for category: " + category);
                return new FruitClusterer();
            
            case "greenhouse_crops":
                Log.d(TAG, "Using GreenhouseCropClusterer for category: " + category);
                return new GreenhouseCropClusterer();
            
            case "resources":
                Log.d(TAG, "Using ResourcesClusterer for category: " + category);
                return new ResourcesClusterer();
            
            case "animals":
                Log.d(TAG, "Using AnimalClusterer for category: " + category);
                return new AnimalClusterer();
            
            case "cooking":
                Log.d(TAG, "Using CookingClusterer for category: " + category);
                return new CookingClusterer(context);
            
            case "composters":
                Log.d(TAG, "Using ComposterClusterer for category: " + category);
                return new ComposterClusterer();
            
            case "flowers":
                Log.d(TAG, "Using FlowerClusterer for category: " + category);
                return new FlowerClusterer();
            
            case "crafting":
                Log.d(TAG, "Using CraftingBoxClusterer for category: " + category);
                return new CraftingBoxClusterer();
            
            case "beehive":
                Log.d(TAG, "Using BeehiveClusterer for category: " + category);
                return new BeehiveClusterer();
            
            case "cropMachine":
                Log.d(TAG, "Using CropMachineClusterer for category: " + category);
                return new CropMachineClusterer();
            
            case "sunstones":
                Log.d(TAG, "Using SunstoneClusterer for category: " + category);
                return new SunstoneClusterer();
            
            case "dailyReset":
                Log.d(TAG, "Using DefaultClusterer for category: " + category);
                return new DefaultClusterer();
            
            case "floating_island":
                Log.d(TAG, "Using FloatingIslandClusterer for category: " + category);
                return new FloatingIslandClusterer();
            
            default:
                Log.w(TAG, "Unknown category: " + category + ", using DefaultClusterer");
                return new DefaultClusterer();
        }
    }
}