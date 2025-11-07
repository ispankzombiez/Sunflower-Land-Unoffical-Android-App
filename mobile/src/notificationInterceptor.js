/**
 * Notification Interceptor for Sunflower Land
 * 
 * This script intercepts API responses from api.sunflower-land.com/session
 * and automatically schedules notifications 5 seconds before items are ready.
 * 
 * It works by:
 * 1. Intercepting fetch() calls to the session API
 * 2. Extracting timestamps from the response
 * 3. Calculating when items will be ready
 * 4. Scheduling notifications via Capacitor's LocalNotifications
 */

import { LocalNotifications } from '@capacitor/local-notifications';

class NotificationInterceptor {
  constructor() {
    this.scheduledNotifications = new Map();
    this.cropGrowthTimes = {
      'Sunflower': 1 * 60 * 1000, // 1 minute
      'Potato': 5 * 60 * 1000,
      'Rhubarb': 10 * 60 * 1000,
      'Pumpkin': 30 * 60 * 1000,
      'Zucchini': 30 * 60 * 1000,
      'Carrot': 60 * 60 * 1000,
      'Yam': 60 * 60 * 1000,
      'Cabbage': 2 * 60 * 60 * 1000,
      'Broccoli': 2 * 60 * 60 * 1000,
      'Soybean': 3 * 60 * 60 * 1000,
      'Beetroot': 4 * 60 * 60 * 1000,
      'Pepper': 4 * 60 * 60 * 1000,
      'Cauliflower': 8 * 60 * 60 * 1000,
      'Parsnip': 12 * 60 * 60 * 1000,
      'Eggplant': 16 * 60 * 60 * 1000,
      'Corn': 20 * 60 * 60 * 1000,
      'Onion': 20 * 60 * 60 * 1000,
      'Radish': 24 * 60 * 60 * 1000,
      'Wheat': 24 * 60 * 60 * 1000,
      'Turnip': 24 * 60 * 60 * 1000,
      'Kale': 36 * 60 * 60 * 1000,
      'Artichoke': 36 * 60 * 60 * 1000,
      'Barley': 48 * 60 * 60 * 1000,
      'Rice': 14 * 60 * 60 * 1000,
      'Grape': 10 * 60 * 60 * 1000,
      'Tomato': 4 * 60 * 60 * 1000,
      'Orange': 8 * 60 * 60 * 1000,
    };
    
    this.resourceGrowthTimes = {
      'tree': 2 * 60 * 60 * 1000,
      'stone': 4 * 60 * 60 * 1000,
      'iron': 8 * 60 * 60 * 1000,
      'gold': 24 * 60 * 60 * 1000,
      'crimstone': 24 * 60 * 60 * 1000,
      'sunstone': 3 * 24 * 60 * 60 * 1000,
      'oil': 24 * 60 * 60 * 1000,
      'lava': 24 * 60 * 60 * 1000,
    };
  }

  async initialize() {
    console.log('🔔 Initializing Notification Interceptor...');
    
    try {
      // Request permissions
      const permission = await LocalNotifications.requestPermissions();
      console.log('📱 Notification permissions:', permission.notifications);
      
      if (permission.notifications !== 'granted') {
        console.warn('⚠️ Notifications not granted');
        return;
      }
      
      // Intercept the original fetch function
      this.interceptFetch();
      console.log('✅ Fetch interceptor installed');
      
    } catch (error) {
      console.error('❌ Failed to initialize interceptor:', error);
    }
  }

  interceptFetch() {
    const originalFetch = window.fetch;
    const self = this;

    window.fetch = function(...args) {
      const url = args[0];
      
      // Only intercept the session API endpoint
      if (typeof url === 'string' && url.includes('api.sunflower-land.com/session')) {
        console.log('🌐 Intercepted API call to:', url);
        
        // Call the original fetch
        return originalFetch.apply(this, args).then(response => {
          // Clone the response so we can read it without consuming the original
          const clonedResponse = response.clone();
          
          // Read the JSON
          clonedResponse.json().then(data => {
            console.log('📊 Received farm data from API');
            self.processFarmData(data);
          }).catch(err => console.error('❌ Error parsing farm data:', err));
          
          // Return the original response to the website
          return response;
        });
      }
      
      // For all other requests, just pass through
      return originalFetch.apply(this, args);
    };
  }

  processFarmData(farmData) {
    console.log('🔍 Processing farm data for ready items...');
    
    if (!farmData || !farmData.farm) {
      console.warn('⚠️ No farm data structure found');
      return;
    }

    const now = Date.now();
    const readyItems = [];

    // Check crops
    if (farmData.farm.crops) {
      Object.entries(farmData.farm.crops).forEach(([key, plot]) => {
        if (plot.crop) {
          const item = this.checkCropReadiness(plot.crop, key, now);
          if (item) readyItems.push(item);
        }
      });
    }

    // Check animals
    if (farmData.farm.animals) {
      Object.entries(farmData.farm.animals).forEach(([key, animal]) => {
        const item = this.checkAnimalReadiness(animal, key, now);
        if (item) readyItems.push(item);
      });
    }

    // Check resource nodes
    if (farmData.farm.trees) {
      Object.entries(farmData.farm.trees).forEach(([key, tree]) => {
        const item = this.checkResourceReadiness(tree, 'tree', key, now);
        if (item) readyItems.push(item);
      });
    }

    if (farmData.farm.stones) {
      Object.entries(farmData.farm.stones).forEach(([key, stone]) => {
        const item = this.checkResourceReadiness(stone, 'stone', key, now);
        if (item) readyItems.push(item);
      });
    }

    // Add more resource types as needed
    const resourceTypes = ['iron', 'gold', 'crimstone', 'oil', 'lava', 'sunstones'];
    resourceTypes.forEach(type => {
      if (farmData.farm[type]) {
        Object.entries(farmData.farm[type]).forEach(([key, resource]) => {
          const item = this.checkResourceReadiness(resource, type.replace('s$', ''), key, now);
          if (item) readyItems.push(item);
        });
      }
    });

    // Schedule notifications for ready items
    if (readyItems.length > 0) {
      console.log(`🚨 Found ${readyItems.length} items ready or nearly ready`);
      readyItems.forEach(item => this.scheduleNotification(item));
    } else {
      console.log('💤 No items ready yet');
    }
  }

  checkCropReadiness(crop, plotId, now) {
    const growthTime = this.cropGrowthTimes[crop.name] || 60 * 60 * 1000;
    const plantedAtMs = crop.plantedAt > 100000000000 ? crop.plantedAt : crop.plantedAt * 1000;
    const readyAt = plantedAtMs + growthTime + (crop.boostedTime || 0);
    const timeUntilReady = readyAt - now;
    const NOTIFICATION_WINDOW = 5 * 1000; // 5 seconds before ready

    if (timeUntilReady > 0 && timeUntilReady <= NOTIFICATION_WINDOW) {
      console.log(`✅ Crop ready soon: ${crop.name} in ${(timeUntilReady / 1000).toFixed(1)}s`);
      return {
        type: 'crop',
        name: crop.name,
        readyAt,
        timeUntilReady,
        id: `crop_${plotId}`,
      };
    }
    return null;
  }

  checkAnimalReadiness(animal, animalId, now) {
    if (!animal.awakeAt) return null;
    
    const timeUntilReady = animal.awakeAt - now;
    const NOTIFICATION_WINDOW = 5 * 1000;

    if (timeUntilReady > 0 && timeUntilReady <= NOTIFICATION_WINDOW) {
      return {
        type: 'animal',
        name: `${animal.type} ready`,
        readyAt: animal.awakeAt,
        timeUntilReady,
        id: `animal_${animalId}`,
      };
    }
    return null;
  }

  checkResourceReadiness(resource, resourceType, resourceId, now) {
    if (!resource[resourceType] || !resource[resourceType].minedAt) return null;
    
    const growthTime = this.resourceGrowthTimes[resourceType] || 24 * 60 * 60 * 1000;
    const readyAt = resource[resourceType].minedAt + growthTime;
    const timeUntilReady = readyAt - now;
    const NOTIFICATION_WINDOW = 5 * 1000;

    if (timeUntilReady > 0 && timeUntilReady <= NOTIFICATION_WINDOW) {
      const friendlyName = resourceType.charAt(0).toUpperCase() + resourceType.slice(1);
      console.log(`✅ Resource ready soon: ${friendlyName} in ${(timeUntilReady / 1000).toFixed(1)}s`);
      return {
        type: 'resource',
        name: friendlyName,
        readyAt,
        timeUntilReady,
        id: `${resourceType}_${resourceId}`,
      };
    }
    return null;
  }

  async scheduleNotification(item) {
    // Check if already scheduled
    if (this.scheduledNotifications.has(item.id)) {
      console.log(`⏭️ Already scheduled: ${item.name}`);
      return;
    }

    try {
      const notifyTime = new Date(Date.now() + item.timeUntilReady);
      const notificationId = Math.floor(Math.random() * 1000000);

      await LocalNotifications.schedule({
        notifications: [
          {
            id: notificationId,
            title: '🌾 Farm Ready!',
            body: `${item.name} is ready!`,
            largeBody: `Your ${item.name} has finished growing and is ready to harvest.`,
            schedule: {
              at: notifyTime,
            },
            autoCancel: true,
            smallIcon: 'ic_stat_icon_config_sample',
          },
        ],
      });

      this.scheduledNotifications.set(item.id, true);
      console.log(`📢 Notification scheduled for: ${item.name} at ${notifyTime.toLocaleTimeString()}`);
    } catch (error) {
      console.error('❌ Failed to schedule notification:', error);
    }
  }
}

// Export as global so it can be accessed
window.notificationInterceptor = new NotificationInterceptor();

// Initialize when the script loads
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', () => {
    window.notificationInterceptor.initialize();
  });
} else {
  window.notificationInterceptor.initialize();
}
