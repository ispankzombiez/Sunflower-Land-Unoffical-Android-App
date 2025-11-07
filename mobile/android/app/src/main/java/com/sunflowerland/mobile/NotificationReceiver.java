package com.sunflowerland.mobile;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;
import android.util.Log;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "sunflower_notifications";
    private static final String CHANNEL_NAME = "Sunflower Land";
    private static final String CHANNEL_DESC = "Notifications for crops, cooking, and other farm activities";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("NOTIFICATION_DEBUG", "📬 NotificationReceiver triggered!");
        
        int notificationId = intent.getIntExtra("notificationId", 0);
        String title = intent.getStringExtra("title");
        String body = intent.getStringExtra("body");
        String itemName = intent.getStringExtra("itemName");
        String category = intent.getStringExtra("category");
        String groupId = intent.getStringExtra("groupId");
        String details = intent.getStringExtra("details");
        int count = intent.getIntExtra("count", 1);

        Log.i("NOTIFICATION_DEBUG", String.format("📬 Showing notification: ID=%d, Item=%s, Category=%s, Count=%d, GroupId=%s", notificationId, itemName, category, count, groupId));

        // Always show notification for system/Notification Manager or API Response
        boolean isApiResponse = "system".equals(category) && "API Response".equals(itemName);
        
        // Log the preference check for debugging
        Log.d("NOTIFICATION_DEBUG", "Checking preferences: category=" + category + ", itemName=" + itemName + ", isSystemOrApiResponse=" + isApiResponse);
        
        if (!("system".equals(category) && ("Notification Manager".equals(itemName) || "API Response".equals(itemName)))) {
            boolean enabled = NotificationPreferences.areNotificationsEnabled(context, category, itemName);
            Log.d("NOTIFICATION_DEBUG", "NotificationPreferences.areNotificationsEnabled returned: " + enabled);
            if (!enabled) {
                Log.i("NOTIFICATION_DEBUG", "🔕 Notification disabled by user preferences for " + category + "/" + itemName);
                return;
            }
        }

        // Create notification channel (required for Android O and above)
        createNotificationChannel(context);

        // Create intent to open the app when notification is clicked
        Intent appIntent;
        
        // Check if "Only Notifications" mode is enabled
        android.content.SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean onlyNotificationsMode = prefs.getBoolean("only_notifications", false);
        
        if (onlyNotificationsMode) {
            // If in notification-only mode, open SettingsActivity instead
            appIntent = new Intent(context, SettingsActivity.class);
        } else {
            // Normal mode - open MainActivity
            appIntent = new Intent(context, MainActivity.class);
        }
        
        appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            appIntent,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );
        
        // Build notification - use system defaults for sound and vibration
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(getSmallIconForDevice(context))
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        if (isApiResponse) {
            // Use standard notification for API response
            builder.setContentTitle(title != null ? title : "Got Api");
            builder.setContentText(body != null ? body : "");
            builder.setStyle(new NotificationCompat.BigTextStyle().bigText(body != null ? body : ""));
        } else {
            // Get the custom icon for this item
            // For marketplace notifications, use the marketplace icon instead of the item icon
            // For auction notifications, use special logic based on sfl/ingredients
            // For composter notifications, use the composter type icon
            // For sick animal notifications, use chicken icon
            String iconItemName;
            if ("marketplace".equals(category)) {
                iconItemName = "Marketplace";
            } else if ("auction".equals(category)) {
                // Auction icon logic: Check sfl value, then ingredients
                iconItemName = getAuctionIconName(details);
            } else if ("composters".equals(category)) {
                // Composter icon uses the composter type name (Composter, Turbo Composter, Premium Composter)
                iconItemName = itemName;
            } else if ("animal_sick".equals(category)) {
                // Sick animal icon is always the chicken icon
                iconItemName = "Animals just got sick!";
            } else {
                iconItemName = itemName;
            }
            int customIconResId = getIconForItem(context, iconItemName);
            
            // Determine notification title and text based on groupId and category
            String notificationTitle;
            String notificationText;
            
            if ("marketplace".equals(category)) {
                // Marketplace listing sold notification
                // Format: "{amount} {name} Sold!" and "{sfl} $Flower"
                notificationTitle = count + " " + itemName + " Sold!";
                String sflText = body != null ? body : "Listing sold";
                // Extract just the number from "6.9999 SFL" and format as "6.9999 $Flower"
                if (sflText.contains("SFL")) {
                    String sflAmount = sflText.replace(" SFL", "").trim();
                    notificationText = sflAmount + " $Flower";
                } else {
                    notificationText = sflText + " $Flower";
                }
            } else if ("floating_island".equals(category)) {
                // Floating Island schedule notification
                // Format: "Floating Island is Live!" and "Ends at HH:MM AM/PM"
                notificationTitle = "Floating Island is Live!";
                // details contains "startAt|endAt" (pipe-separated timestamps)
                if (details != null && details.contains("|")) {
                    String[] times = details.split("\\|");
                    if (times.length > 1) {
                        long endAt = Long.parseLong(times[1]);
                        notificationText = "Ends at " + formatEndTime(endAt);
                    } else {
                        notificationText = "Island is available!";
                    }
                } else if ("Love Island Shop".equals(itemName)) {
                    // Shop change notification
                    notificationTitle = "New Love Island Items!";
                    notificationText = details != null ? details : "Shop items updated";
                } else {
                    notificationText = "Island is available!";
                }
            } else if (groupId != null && groupId.contains("animal love at")) {
                // Love notification
                notificationTitle = "Your " + itemName + " need love!";
                notificationText = formatCountAndName(count, itemName);
            } else if ("Sunstones".equals(category)) {
                // Sunstone notification - show mines left
                notificationTitle = "Sunstone is ready!";
                notificationText = count + " mines left";
            } else if ("Daily Reset".equals(category)) {
                // Daily reset notification
                notificationTitle = "Daily Reset!";
                notificationText = "Your farm has been reset. Time to start a new day!";
                
                // Clear debug log at 00:00 UTC (when daily reset notification fires)
                DebugLog.clearLog(context);
                DebugLog.log("=== Daily Reset notification at 00:00 UTC - Debug log cleared ===");
            } else if ("composters".equals(category) && details != null && !details.isEmpty()) {
                // Composter notification - show produced items
                notificationTitle = itemName + " is ready!";
                notificationText = details;
            } else if ("cooking".equals(category) && details != null && !details.isEmpty()) {
                // Cooking notification (grouped by building) - show food items
                notificationTitle = itemName + " is done cooking!";
                notificationText = details;
            } else if ("crafting".equals(category)) {
                // Crafting box notification
                notificationTitle = itemName + " is ready!";
                notificationText = "Crafting complete";
                Log.i("NOTIFICATION_DEBUG", "Crafting notification for " + itemName);
            } else if ("auction".equals(category)) {
                // Auction schedule notification
                // Format: "{name} {currency} Auction is live!" and "Ends at {TIME}"
                notificationTitle = formatAuctionDisplayName(itemName, details) + " is live!";
                // details contains "endAt|sfl|ingredientsJson"
                if (details != null && details.contains("|")) {
                    String[] parts = details.split("\\|", 3);
                    if (parts.length > 0) {
                        try {
                            long endAt = Long.parseLong(parts[0]);
                            notificationText = "Ends at " + formatEndTime(endAt);
                        } catch (NumberFormatException e) {
                            notificationText = "Ends at unknown time";
                        }
                    } else {
                        notificationText = "Ends at unknown time";
                    }
                } else {
                    notificationText = "Auction is live!";
                }
                Log.i("NOTIFICATION_DEBUG", "Auction notification for " + itemName);
            } else if ("animal_sick".equals(category)) {
                // Sick animal notification
                // Format: "Animals just got sick!" and "{animal list with counts}"
                notificationTitle = "Animals just got sick!";
                notificationText = itemName; // itemName already contains the formatted list (e.g., "2 Chickens, 1 Cow")
            } else {
                // Production/standard notification
                notificationTitle = itemName + " is ready!";
                notificationText = formatCountAndName(count, itemName);
            }
            
            // Create custom notification layout
            android.widget.RemoteViews customView = new android.widget.RemoteViews(
                context.getPackageName(),
                R.layout.notification_custom
            );
            customView.setImageViewResource(R.id.notification_icon, customIconResId);
            customView.setTextViewText(R.id.notification_title, notificationTitle);
            customView.setTextViewText(R.id.notification_text, notificationText);
            Log.d("NOTIFICATION_DEBUG", "Custom view set - icon resId: " + customIconResId + ", title: " + notificationTitle + ", text: " + notificationText);
            builder.setStyle(new androidx.core.app.NotificationCompat.DecoratedCustomViewStyle())
                   .setCustomContentView(customView);
        }

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            notificationManager.notify(notificationId, builder.build());
            Log.i("NOTIFICATION_DEBUG", "✅ Notification shown successfully");
        } else {
            Log.e("NOTIFICATION_DEBUG", "❌ NotificationManager is null");
        }
    }
    
    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = 
                    context.getSystemService(NotificationManager.class);
            
            if (notificationManager != null) {
                // Check if channel already exists
                NotificationChannel existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID);
                if (existingChannel != null) {
                    Log.d("NotificationReceiver", "Notification channel already exists");
                    return;
                }
                
                // Create notification channel with system defaults for sound/vibration
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription(CHANNEL_DESC);
                
                notificationManager.createNotificationChannel(channel);
                Log.d("NotificationReceiver", "Created notification channel with system defaults");
            }
        }
    }
    
    private int getIconForItem(Context context, String itemName) {
        if (itemName == null) {
            Log.w("NOTIFICATION_DEBUG", "getIconForItem called with null itemName");
            return android.R.drawable.ic_dialog_info; // Default fallback
        }
        
        Log.d("NOTIFICATION_DEBUG", "Looking up icon for: " + itemName);
        
        // Special case mappings for specific items
        switch (itemName) {
            case "Compost Bin":
                return context.getResources().getIdentifier("ic_sprout_mix", "drawable", context.getPackageName());
            case "Composter":
                return context.getResources().getIdentifier("ic_sprout_mix", "drawable", context.getPackageName());
            case "Turbo Composter":
                return context.getResources().getIdentifier("ic_fruitful_blend", "drawable", context.getPackageName());
            case "Premium Composter":
                return context.getResources().getIdentifier("ic_rapid_root", "drawable", context.getPackageName());
            case "Marketplace":
                return context.getResources().getIdentifier("ic_marketplace", "drawable", context.getPackageName());
            case "Floating Island":
                return context.getResources().getIdentifier("ic_marketplace", "drawable", context.getPackageName());
            case "Love Island Shop":
                return context.getResources().getIdentifier("ic_marketplace", "drawable", context.getPackageName());
            case "Flower Token":
                return context.getResources().getIdentifier("ic_flower_token", "drawable", context.getPackageName());
            case "Gem":
                return context.getResources().getIdentifier("ic_gem", "drawable", context.getPackageName());
            case "Pet Cookie":
                return context.getResources().getIdentifier("ic_pet_cookie", "drawable", context.getPackageName());
            case "Animals just got sick!":
                // Use chicken icon for sick animal notifications
                return context.getResources().getIdentifier("ic_chicken", "drawable", context.getPackageName());
        }
        
        // Map item names to drawable resource names (lowercase, no spaces)
        String resourceName = "ic_" + itemName.toLowerCase().replace(" ", "_");
        Log.d("NOTIFICATION_DEBUG", "Constructed resource name: " + resourceName + " for item: " + itemName);
        
        // Try to get the resource ID
        int resId = context.getResources().getIdentifier(resourceName, "drawable", context.getPackageName());
        
        if (resId != 0) {
            Log.i("NOTIFICATION_DEBUG", "✅ Found custom icon for " + itemName + ": " + resourceName + " (resId: " + resId + ")");
            return resId;
        } else {
            Log.w("NOTIFICATION_DEBUG", "⚠️ No custom icon found for " + itemName + ", resource name was: " + resourceName + ", using default");
            return android.R.drawable.ic_dialog_info; // Fallback to default
        }
    }
    
    /**
     * Determine auction icon based on sfl value and ingredients
     * Icon logic:
     * 1. If sfl == 1 -> use "Flower Token" (ic_flower_token)
     * 2. Else -> get first ingredient key from ingredientsJson
     *    - If "Gem" -> use "Gem" (ic_gem)
     *    - If "Pet Cookie" -> use "Pet Cookie" (ic_pet_cookie)
     */
    private String getAuctionIconName(String details) {
        if (details == null || details.isEmpty()) {
            return "Gem";  // Default fallback
        }
        
        try {
            // details format: "endAt|sfl|ingredientsJson"
            String[] parts = details.split("\\|", 3);
            if (parts.length < 2) {
                return "Gem";  // Default fallback
            }
            
            long sfl = Long.parseLong(parts[1]);
            
            // If sfl == 1, use Flower Token
            if (sfl == 1) {
                Log.d("NOTIFICATION_DEBUG", "Auction icon: Flower Token (sfl==1)");
                return "Flower Token";
            }
            
            // Otherwise, parse ingredients to get first key
            if (parts.length > 2 && !parts[2].isEmpty()) {
                String ingredientsJson = parts[2];
                // Parse first key from JSON object: {"Gem":X} or {"Pet Cookie":X}
                if (ingredientsJson.contains("\"Gem\"")) {
                    Log.d("NOTIFICATION_DEBUG", "Auction icon: Gem (found in ingredients)");
                    return "Gem";
                } else if (ingredientsJson.contains("\"Pet Cookie\"")) {
                    Log.d("NOTIFICATION_DEBUG", "Auction icon: Pet Cookie (found in ingredients)");
                    return "Pet Cookie";
                }
            }
            
            // Default fallback
            Log.d("NOTIFICATION_DEBUG", "Auction icon: Gem (default fallback)");
            return "Gem";
            
        } catch (Exception e) {
            Log.w("NOTIFICATION_DEBUG", "Error determining auction icon: " + e.getMessage());
            return "Gem";  // Default fallback
        }
    }

    /**
     * Get the currency display name for an auction
     * Determines currency type from sfl and ingredients
     * Returns: "$Flower", "Gem", or "Pet Cookie"
     */
    private String getAuctionCurrencyName(String details) {
        if (details == null || details.isEmpty()) {
            return "Gem";  // Default fallback
        }
        
        try {
            // details format: "endAt|sfl|ingredientsJson"
            String[] parts = details.split("\\|", 3);
            if (parts.length < 2) {
                return "Gem";  // Default fallback
            }
            
            long sfl = Long.parseLong(parts[1]);
            
            // If sfl > 0, use $Flower
            if (sfl > 0) {
                return "$Flower";
            }
            
            // Otherwise, parse ingredients to get first key
            if (parts.length > 2 && !parts[2].isEmpty()) {
                String ingredientsJson = parts[2];
                // Parse first key from JSON object: {"Gem":X} or {"Pet Cookie":X}
                if (ingredientsJson.contains("\"Gem\"")) {
                    return "Gem";
                } else if (ingredientsJson.contains("\"Pet Cookie\"")) {
                    return "Pet Cookie";
                }
            }
            
            // Default fallback
            return "Gem";
            
        } catch (Exception e) {
            Log.w("NOTIFICATION_DEBUG", "Error determining auction currency: " + e.getMessage());
            return "Gem";  // Default fallback
        }
    }

    /**
     * Format auction display name: "{itemName} {currency} Auction"
     * Example: "Coin Aura $Flower Auction"
     */
    private String formatAuctionDisplayName(String itemName, String details) {
        String currencyName = getAuctionCurrencyName(details);
        return itemName + " " + currencyName + " Auction";
    }

    private String formatCountAndName(int count, String itemName) {
        if (itemName == null) {
            return String.valueOf(count);
        }
        
        // Pluralize the item name if count > 1
        String displayName = itemName;
        if (count > 1 && !itemName.endsWith("s")) {
            displayName = itemName + "s";
        }
        
        return count + " " + displayName;
    }

    /**
     * Determine the appropriate small icon based on device type
     * Google devices (Pixel, etc.) need different handling than Samsung
     */
    private int getSmallIconForDevice(Context context) {
        String manufacturer = Build.MANUFACTURER != null ? Build.MANUFACTURER.toLowerCase() : "";
        String model = Build.MODEL != null ? Build.MODEL.toLowerCase() : "";
        
        Log.d("NOTIFICATION_DEBUG", "Device - Manufacturer: " + manufacturer + ", Model: " + model);
        
        // Check if this is a Google device (Pixel, Nexus, etc.)
        if (isGoogleDevice(manufacturer, model)) {
            Log.d("NOTIFICATION_DEBUG", "Google device detected - using monochrome icon");
            // For Google devices, use the monochrome icon
            return R.drawable.ic_notification;
        } else {
            Log.d("NOTIFICATION_DEBUG", "Non-Google device detected - using launcher icon");
            // For other devices (Samsung, etc.), use the app launcher icon
            return R.mipmap.ic_launcher;
        }
    }

    /**
     * Detect if device is a Google-manufactured device
     */
    private boolean isGoogleDevice(String manufacturer, String model) {
        return manufacturer.contains("google") || 
               model.contains("pixel") || 
               model.contains("nexus") ||
               model.startsWith("x") || // Pixel devices start with 'x' (e.g., Xperia is Sony, but need to check)
               manufacturer.contains("huawei") == false && 
               manufacturer.contains("samsung") == false &&
               manufacturer.contains("lg") == false &&
               model.contains("fold") == false; // Samsung Fold/Flip
    }

    /**
     * Format timestamp to user-friendly end time (e.g., "2:30 PM")
     */
    private String formatEndTime(long timestamp) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("h:mm a");
            return sdf.format(new java.util.Date(timestamp));
        } catch (Exception e) {
            Log.w("NOTIFICATION_DEBUG", "Error formatting end time: " + e.getMessage());
            return "later";
        }
    }
}
