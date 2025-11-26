package com.sfl.browser;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sfl.browser.models.SickAnimal;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for tracking sick animal state changes.
 * Persists sick animal states to detect NEW sicknesses vs. existing sicknesses.
 */
public class SickAnimalTracker {
    private static final String PREFS_NAME = "sick_animal_tracking";
    private static final String KEY_SICK_ANIMALS = "sick_animals_state";

    private final SharedPreferences prefs;
    private final Gson gson;

    public SickAnimalTracker(Context context) {
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    /**
     * Load the previously tracked sick animal states.
     *
     * @return Map of "type_id" -> state (e.g., "Chicken_5" -> "sick")
     */
    public Map<String, String> loadPreviousState() {
        String json = prefs.getString(KEY_SICK_ANIMALS, "{}");
        Type type = new TypeToken<Map<String, String>>() {}.getType();
        return gson.fromJson(json, type);
    }

    /**
     * Save the current sick animal states for future comparison.
     *
     * @param currentSickAnimals List of current SickAnimal objects
     */
    public void saveCurrentState(List<SickAnimal> currentSickAnimals) {
        Map<String, String> stateMap = new HashMap<>();
        for (SickAnimal animal : currentSickAnimals) {
            String key = animal.getKey(); // "type_id"
            stateMap.put(key, animal.state);
        }
        String json = gson.toJson(stateMap);
        prefs.edit().putString(KEY_SICK_ANIMALS, json).apply();
    }

    /**
     * Compare current sick states with previous states to detect NEW sicknesses.
     * NEW sickness = animal was idle/not tracked and is now sick.
     *
     * @param currentSickAnimals List of current SickAnimal objects
     * @return List of newly sick animals (were not sick before, now are)
     */
    public List<SickAnimal> getNewlySickAnimals(List<SickAnimal> currentSickAnimals) {
        Map<String, String> previousState = loadPreviousState();
        List<SickAnimal> newlySick = new ArrayList<>();

        for (SickAnimal animal : currentSickAnimals) {
            String key = animal.getKey();
            String previousStatus = previousState.getOrDefault(key, "idle");

            // NEW sickness: was idle/not tracked and is now sick
            if ("idle".equals(previousStatus) && "sick".equals(animal.state)) {
                newlySick.add(animal);
                DebugLog.log("🐔 NEW SICKNESS: " + animal.type + " #" + animal.id + " became sick");
            }
        }

        return newlySick;
    }

    /**
     * Clear all tracked state (useful for testing or resetting).
     */
    public void clearTrackedState() {
        prefs.edit().remove(KEY_SICK_ANIMALS).apply();
    }
}
