package com.sfl.browser;

import com.google.gson.JsonObject;
import com.sfl.browser.models.FarmItem;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SkillExtractors {
    private static final String TAG = "SkillExtractors";
    
    // Skill cooldown times in milliseconds
    private static final Map<String, Long> SKILL_COOLDOWNS = new HashMap<String, Long>() {{
        put("Instant Growth", 3 * 24 * 60 * 60 * 1000L);           // 3 days
        put("Tree Blitz", 1 * 24 * 60 * 60 * 1000L);              // 1 day
        put("Instant Gratification", 4 * 24 * 60 * 60 * 1000L);   // 4 days
        put("Barnyard Rouse", 5 * 24 * 60 * 60 * 1000L);          // 5 days
        put("Petal Blessed", 4 * 24 * 60 * 60 * 1000L);           // 4 days
        put("Greenhouse Guru", 4 * 24 * 60 * 60 * 1000L);         // 4 days
        put("Grease Lightning", 4 * 24 * 60 * 60 * 1000L);        // 4 days
    }};
    
    /**
     * Extracts skill cooldown notifications from bumpkin data
     * Only returns skills that:
     * 1. Have been used (exist in previousPowerUseAt)
     * 2. Are in the skills section (player has learned them)
     * 3. Will refresh in the future
     */
    public static List<FarmItem> extractSkillCooldowns(JsonObject bumpkinObject) {
        List<FarmItem> items = new ArrayList<>();
        
        try {
            if (!bumpkinObject.has("previousPowerUseAt") || !bumpkinObject.has("skills")) {
                Log.d(TAG, "No previousPowerUseAt or skills section found in bumpkin data");
                return items;
            }
            
            JsonObject previousPowerUseAt = bumpkinObject.getAsJsonObject("previousPowerUseAt");
            JsonObject skills = bumpkinObject.getAsJsonObject("skills");
            long currentTime = System.currentTimeMillis();
            
            // Check each skill that has been used
            for (String skillName : previousPowerUseAt.keySet()) {
                // Only process skills that the player has learned
                if (!skills.has(skillName)) {
                    Log.d(TAG, "Skill " + skillName + " not in skills section, skipping");
                    continue;
                }
                
                // Get the cooldown time for this skill
                Long cooldownMs = SKILL_COOLDOWNS.get(skillName);
                if (cooldownMs == null) {
                    Log.w(TAG, "Unknown skill cooldown for: " + skillName);
                    continue;
                }
                
                // Get when the skill was last used
                long lastUsedTime = previousPowerUseAt.get(skillName).getAsLong();
                
                // Calculate when the skill will be ready
                long readyTime = lastUsedTime + cooldownMs;
                
                // Only create a notification if the skill will be ready in the future
                if (readyTime > currentTime) {
                    FarmItem skillItem = new FarmItem("skill_cooldown", skillName, 1, readyTime);
                    items.add(skillItem);
                    
                    long timeUntilReady = readyTime - currentTime;
                    long daysUntilReady = timeUntilReady / (24 * 60 * 60 * 1000);
                    long hoursUntilReady = (timeUntilReady % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000);
                    
                    Log.d(TAG, "Added skill cooldown notification: " + skillName + 
                              " - Ready in " + daysUntilReady + "d " + hoursUntilReady + "h");
                } else {
                    Log.d(TAG, "Skill already ready: " + skillName);
                }
            }
            
            Log.d(TAG, "Extracted " + items.size() + " skill cooldown notification(s)");
            
        } catch (Exception e) {
            Log.e(TAG, "Error extracting skill cooldowns: " + e.getMessage(), e);
        }
        
        return items;
    }
}
