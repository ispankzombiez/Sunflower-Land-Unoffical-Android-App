import { LocalNotifications } from '@capacitor/local-notifications';

// Growth times in milliseconds for each crop type
const CROP_GROWTH_TIMES = {
  'Sunflower': 1 * 60 * 1000, // 1 minute
  'Potato': 5 * 60 * 1000, // 5 minutes
  'Rhubarb': 10 * 60 * 1000, // 10 minutes
  'Pumpkin': 30 * 60 * 1000, // 30 minutes
  'Zucchini': 30 * 60 * 1000, // 30 minutes
  'Carrot': 60 * 60 * 1000, // 1 hour
  'Yam': 60 * 60 * 1000, // 1 hour
  'Cabbage': 2 * 60 * 60 * 1000, // 2 hours
  'Broccoli': 2 * 60 * 60 * 1000, // 2 hours
  'Soybean': 3 * 60 * 60 * 1000, // 3 hours
  'Beetroot': 4 * 60 * 60 * 1000, // 4 hours
  'Pepper': 4 * 60 * 60 * 1000, // 4 hours
  'Cauliflower': 8 * 60 * 60 * 1000, // 8 hours
  'Parsnip': 12 * 60 * 60 * 1000, // 12 hours
  'Eggplant': 16 * 60 * 60 * 1000, // 16 hours
  'Corn': 20 * 60 * 60 * 1000, // 20 hours
  'Onion': 20 * 60 * 60 * 1000, // 20 hours
  'Radish': 24 * 60 * 60 * 1000, // 24 hours
  'Wheat': 24 * 60 * 60 * 1000, // 24 hours
  'Turnip': 24 * 60 * 60 * 1000, // 24 hours
  'Kale': 36 * 60 * 60 * 1000, // 36 hours
  'Artichoke': 36 * 60 * 60 * 1000, // 36 hours
  'Barley': 48 * 60 * 60 * 1000, // 48 hours
};

const FLOWER_GROWTH_TIMES = {
  'Red Pansy': 30 * 60 * 1000, // 30 minutes
  'Yellow Pansy': 30 * 60 * 1000,
  'Purple Pansy': 30 * 60 * 1000,
  'White Pansy': 30 * 60 * 1000,
  'Blue Pansy': 30 * 60 * 1000,
  'Red Cosmos': 60 * 60 * 1000, // 1 hour
  'Yellow Cosmos': 60 * 60 * 1000,
  'Purple Cosmos': 60 * 60 * 1000,
  'White Cosmos': 60 * 60 * 1000,
  'Blue Cosmos': 60 * 60 * 1000,
  'Red Balloon Flower': 2 * 60 * 60 * 1000, // 2 hours
  'Yellow Balloon Flower': 2 * 60 * 60 * 1000,
  'Purple Balloon Flower': 2 * 60 * 60 * 1000,
  'White Balloon Flower': 2 * 60 * 60 * 1000,
  'Blue Balloon Flower': 2 * 60 * 60 * 1000,
  'Red Daffodil': 3 * 60 * 60 * 1000, // 3 hours
  'Yellow Daffodil': 3 * 60 * 60 * 1000,
  'Purple Daffodil': 3 * 60 * 60 * 1000,
  'White Daffodil': 3 * 60 * 60 * 1000,
  'Blue Daffodil': 3 * 60 * 60 * 1000,
  'Red Lotus': 4 * 60 * 60 * 1000, // 4 hours
  'Yellow Lotus': 4 * 60 * 60 * 1000,
  'Purple Lotus': 4 * 60 * 60 * 1000,
  'White Lotus': 4 * 60 * 60 * 1000,
  'Blue Lotus': 4 * 60 * 60 * 1000,
  'Red Carnation': 2 * 60 * 60 * 1000, // 2 hours
  'Yellow Carnation': 2 * 60 * 60 * 1000,
  'Purple Carnation': 2 * 60 * 60 * 1000,
  'White Carnation': 2 * 60 * 60 * 1000,
  'Blue Carnation': 2 * 60 * 60 * 1000,
  'Red Edelweiss': 5 * 60 * 60 * 1000, // 5 hours
  'Yellow Edelweiss': 5 * 60 * 60 * 1000,
  'Purple Edelweiss': 5 * 60 * 60 * 1000,
  'Red Gladiolus': 5 * 60 * 60 * 1000,
  'Yellow Gladiolus': 5 * 60 * 60 * 1000,
  'Purple Gladiolus': 5 * 60 * 60 * 1000,
  'White Gladiolus': 5 * 60 * 60 * 1000,
  'Blue Gladiolus': 5 * 60 * 60 * 1000,
  'Red Lavender': 6 * 60 * 60 * 1000, // 6 hours
  'Purple Lavender': 6 * 60 * 60 * 1000,
  'White Lavender': 6 * 60 * 60 * 1000,
  'Blue Lavender': 6 * 60 * 60 * 1000,
  'Yellow Clover': 7 * 60 * 60 * 1000, // 7 hours
  'Purple Clover': 7 * 60 * 60 * 1000,
  'White Clover': 7 * 60 * 60 * 1000,
  'Blue Clover': 7 * 60 * 60 * 1000,
  'Prism Petal': 8 * 60 * 60 * 1000, // 8 hours
  'Celestial Frostbloom': 8 * 60 * 60 * 1000,
  'Primula Enigma': 8 * 60 * 60 * 1000,
};

// Resource growth times (in milliseconds)
const RESOURCE_GROWTH_TIMES = {
  'tree': 2 * 60 * 60 * 1000, // 2 hours
  'stone': 4 * 60 * 60 * 1000, // 4 hours
  'iron': 8 * 60 * 60 * 1000, // 8 hours
  'gold': 24 * 60 * 60 * 1000, // 24 hours
  'crimstone': 24 * 60 * 60 * 1000, // 24 hours
  'sunstone': 3 * 24 * 60 * 60 * 1000, // 3 days (72 hours)
  'oil': 24 * 60 * 60 * 1000, // 24 hours
  'lava': 24 * 60 * 60 * 1000, // 24 hours
};

// Time before completion to trigger notification (5 seconds in milliseconds)
const NOTIFICATION_ADVANCE_TIME = 5 * 1000;

class NotificationService {
  constructor() {
    this.scheduledNotifications = new Map();
    this.checkInterval = null;
    this.lastFarmDataFetch = null;
    this.testNotificationSent = false;
  }

  async initialize() {
    try {
      console.log('🔔 Initializing Notification Service...');
      
      // Request notification permissions - this will show the permission dialog
      console.log('📱 Requesting notification permissions...');
      const permission = await LocalNotifications.requestPermissions();
      console.log('📱 Notification permissions response:', permission);
      console.log('📱 Full permission object:', JSON.stringify(permission));
      
      // Check if we actually got permission
      if (permission.notifications === 'granted') {
        console.log('✅ Notification permissions GRANTED');
      } else if (permission.notifications === 'denied') {
        console.warn('⚠️ Notification permissions DENIED by user');
        alert('Notifications are disabled. Please enable notifications in settings for this app to work properly.');
        return; // Don't try to send notifications if denied
      } else if (permission.notifications === 'prompt') {
        console.warn('⚠️ Notification permissions PROMPT - user may need to accept');
      }
      
      // Log device timezone for debugging
      const tzOffset = this.getTimezoneOffset();
      console.log(`🌍 Device timezone offset: ${new Date().getTimezoneOffset()} minutes`);
      
      // Clear any old scheduled notifications
      try {
        await LocalNotifications.cancelAll();
        console.log('🧹 Cleared old notifications');
      } catch (error) {
        console.warn('⚠️ Error clearing old notifications:', error);
      }
      
      // Send test notification immediately to verify system works
      console.log('🧪 About to send test notification...');
      await this.sendTestNotification();
      console.log('🧪 Test notification sent');
      
      // Start checking for ready items
      this.startNotificationCheck();
      console.log('⏱️ Notification check started (every 10 seconds)');
    } catch (error) {
      console.error('❌ Failed to initialize notifications:', error);
      console.error('❌ Error stack:', error.stack);
    }
  }

  getTimezoneOffset() {
    // Get the device's timezone offset in milliseconds
    // This is the difference between UTC and local time
    return new Date().getTimezoneOffset() * 60 * 1000;
  }

  formatTimeInLocalTimezone(utcTimestamp) {
    // Convert UTC timestamp to local time for display
    const date = new Date(utcTimestamp);
    return date.toLocaleString();
  }

  startNotificationCheck() {
    // Check every 10 seconds for items ready to notify
    if (this.checkInterval) clearInterval(this.checkInterval);
    this.checkInterval = setInterval(() => this.checkForReadyItems(), 10000);
  }

  async fetchFarmData() {
    try {
      console.log('📥 Fetching farm data from API...');
      const startTime = Date.now();
      
      const response = await fetch('https://api.sunflower-land.com/session', {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
        },
      });

      if (!response.ok) {
        throw new Error(`API error: ${response.status}`);
      }

      const data = await response.json();
      const fetchTime = Date.now() - startTime;
      
      console.log(`✅ Farm data fetched successfully in ${fetchTime}ms`);
      console.log('📊 Farm data structure:', {
        hasFarm: !!data.farm,
        hasUser: !!data.user,
        farmKeys: data.farm ? Object.keys(data.farm) : [],
        timestamp: new Date().toLocaleString(),
      });
      
      // Store the last fetch timestamp
      this.lastFarmDataFetch = Date.now();
      
      return data;
    } catch (error) {
      console.error('❌ Failed to fetch farm data:', error);
      console.error('   Error details:', {
        message: error.message,
        stack: error.stack,
        timestamp: new Date().toLocaleString(),
      });
      return null;
    }
  }

  extractReadyItems(farmData) {
    // Get current time in UTC (Date.now() is always UTC)
    const nowUTC = Date.now();
    
    // Get device timezone offset in milliseconds
    const tzOffsetMs = new Date().getTimezoneOffset() * 60 * 1000;
    
    // Current time in device's local timezone
    const nowLocal = nowUTC - tzOffsetMs;
    
    // For comparison, use UTC time (API timestamps are in UTC)
    const now = nowUTC;
    
    const readyItems = [];

    if (!farmData || !farmData.farm) return readyItems;

    // DEBUG: Log timezone info
    console.log(`Timezone offset: ${new Date().getTimezoneOffset()} minutes | UTC: ${nowUTC} | Local: ${nowLocal}`);

    // Extract crops
    if (farmData.farm.crops) {
      console.log(`🌾 Checking ${Object.keys(farmData.farm.crops).length} crop plots...`);
      Object.entries(farmData.farm.crops).forEach(([key, plot]) => {
        if (plot.crop) {
          const growthTime = CROP_GROWTH_TIMES[plot.crop.name] || 60 * 60 * 1000;
          
          // Convert plantedAt from seconds to milliseconds if needed
          const plantedAtMs = plot.crop.plantedAt > 100000000000 ? plot.crop.plantedAt : plot.crop.plantedAt * 1000;
          
          const readyAt = plantedAtMs + growthTime + (plot.crop.boostedTime || 0);
          const timeUntilReady = readyAt - now;

          console.log(`   📍 Plot ${key}: ${plot.crop.name}`);
          console.log(`      Planted: ${this.formatTimeInLocalTimezone(plantedAtMs)}`);
          console.log(`      Growth time: ${(growthTime / 1000).toFixed(0)}s`);
          console.log(`      Ready at: ${this.formatTimeInLocalTimezone(readyAt)}`);
          console.log(`      Time until ready: ${timeUntilReady}ms (${(timeUntilReady / 1000).toFixed(1)}s)`);
          console.log(`      In notification window? ${timeUntilReady > 0 && timeUntilReady <= NOTIFICATION_ADVANCE_TIME ? '✅ YES' : '❌ NO'}`);

          if (timeUntilReady > 0 && timeUntilReady <= NOTIFICATION_ADVANCE_TIME) {
            readyItems.push({
              type: 'crop',
              name: plot.crop.name,
              readyAt,
              timeUntilReady,
              id: `crop_${key}`,
            });
          }
        }
      });
    }

    // Extract greenhouse plants
    if (farmData.farm.greenhouse && farmData.farm.greenhouse.pots) {
      Object.entries(farmData.farm.greenhouse.pots).forEach(([key, pot]) => {
        if (pot.plant) {
          const growthTime = CROP_GROWTH_TIMES[pot.plant.name] || 60 * 60 * 1000;
          
          // Convert plantedAt from seconds to milliseconds if needed
          const plantedAtMs = pot.plant.plantedAt > 100000000000 ? pot.plant.plantedAt : pot.plant.plantedAt * 1000;
          
          const readyAt = plantedAtMs + growthTime;
          const timeUntilReady = readyAt - now;

          if (timeUntilReady > 0 && timeUntilReady <= NOTIFICATION_ADVANCE_TIME) {
            readyItems.push({
              type: 'greenhouse',
              name: `Greenhouse ${pot.plant.name}`,
              readyAt,
              timeUntilReady,
              id: `greenhouse_${key}`,
            });
          }
        }
      });
    }

    // Extract animals (awakeAt)
    if (farmData.farm.animals) {
      Object.entries(farmData.farm.animals).forEach(([key, animal]) => {
        if (animal.awakeAt) {
          const timeUntilReady = animal.awakeAt - now;
          if (timeUntilReady > 0 && timeUntilReady <= NOTIFICATION_ADVANCE_TIME) {
            readyItems.push({
              type: 'animal',
              name: `${animal.type} waking up`,
              readyAt: animal.awakeAt,
              timeUntilReady,
              id: `animal_${key}`,
            });
          }
        }
      });
    }

    // Extract cooking from buildings
    this.extractBuildingCrafts(farmData.farm, readyItems, now);

    // Extract resource nodes
    this.extractResourceNodes(farmData.farm, readyItems, now);

    // Extract composters
    this.extractComposters(farmData.farm, readyItems, now);

    // Extract beehives (when attachedUntil expires)
    if (farmData.farm.beehives) {
      Object.entries(farmData.farm.beehives).forEach(([key, hive]) => {
        if (hive.flowers && hive.flowers.length > 0) {
          hive.flowers.forEach((flower, idx) => {
            const timeUntilReady = flower.attachedUntil - now;
            if (timeUntilReady > 0 && timeUntilReady <= NOTIFICATION_ADVANCE_TIME) {
              readyItems.push({
                type: 'beehive',
                name: 'Flower attachment expires',
                readyAt: flower.attachedUntil,
                timeUntilReady,
                id: `beehive_${key}_${idx}`,
              });
            }
          });
        }
      });
    }

    return readyItems;
  }

  extractBuildingCrafts(farm, readyItems, now) {
    if (!farm.buildings) return;

    const buildingTypes = [
      'Kitchen', 'Bakery', 'Deli', 'Smoothie Shack', 'Fire Pit',
      'Crafting Box', 'Brewery', 'Hooch House', 'Jam House',
      'Cheese House', 'Oil House', 'Potion House', 'Feed House',
      'Turbo Composter', 'Premium Composter', 'Compost Bin'
    ];

    buildingTypes.forEach(buildingType => {
      if (farm.buildings[buildingType]) {
        farm.buildings[buildingType].forEach((building, idx) => {
          if (building.crafting && Array.isArray(building.crafting)) {
            building.crafting.forEach((craft, craftIdx) => {
              if (craft.readyAt) {
                const timeUntilReady = craft.readyAt - now;
                if (timeUntilReady > 0 && timeUntilReady <= NOTIFICATION_ADVANCE_TIME) {
                  readyItems.push({
                    type: 'craft',
                    name: `${craft.name} (${buildingType})`,
                    readyAt: craft.readyAt,
                    timeUntilReady,
                    id: `craft_${buildingType}_${idx}_${craftIdx}`,
                  });
                }
              }
            });
          }
        });
      }
    });
  }

  extractComposters(farm, readyItems, now) {
    if (!farm.buildings) return;

    const composterTypes = ['Compost Bin', 'Turbo Composter', 'Premium Composter'];

    composterTypes.forEach(composterType => {
      if (farm.buildings[composterType]) {
        farm.buildings[composterType].forEach((composter, idx) => {
          if (composter.producing && composter.producing.readyAt) {
            const timeUntilReady = composter.producing.readyAt - now;
            if (timeUntilReady > 0 && timeUntilReady <= NOTIFICATION_ADVANCE_TIME) {
              const items = Object.keys(composter.producing.items || {}).join(', ');
              readyItems.push({
                type: 'compost',
                name: `${composterType} producing ${items}`,
                readyAt: composter.producing.readyAt,
                timeUntilReady,
                id: `compost_${composterType}_${idx}`,
              });
            }
          }
        });
      }
    });
  }

  extractResourceNodes(farm, readyItems, now) {
    // Trees
    if (farm.trees) {
      Object.entries(farm.trees).forEach(([key, tree]) => {
        if (tree.wood && tree.wood.choppedAt) {
          const readyAt = tree.wood.choppedAt + RESOURCE_GROWTH_TIMES.tree;
          const timeUntilReady = readyAt - now;
          if (timeUntilReady > 0 && timeUntilReady <= NOTIFICATION_ADVANCE_TIME) {
            readyItems.push({
              type: 'resource',
              name: 'Tree ready to chop',
              readyAt,
              timeUntilReady,
              id: `tree_${key}`,
            });
          }
        }
      });
    }

    // Stones
    if (farm.stones) {
      Object.entries(farm.stones).forEach(([key, stone]) => {
        if (stone.stone && stone.stone.minedAt) {
          const readyAt = stone.stone.minedAt + RESOURCE_GROWTH_TIMES.stone;
          const timeUntilReady = readyAt - now;
          if (timeUntilReady > 0 && timeUntilReady <= NOTIFICATION_ADVANCE_TIME) {
            readyItems.push({
              type: 'resource',
              name: 'Stone ready to mine',
              readyAt,
              timeUntilReady,
              id: `stone_${key}`,
            });
          }
        }
      });
    }

    // Iron
    if (farm.iron) {
      Object.entries(farm.iron).forEach(([key, iron]) => {
        if (iron.iron && iron.iron.minedAt) {
          const readyAt = iron.iron.minedAt + RESOURCE_GROWTH_TIMES.iron;
          const timeUntilReady = readyAt - now;
          if (timeUntilReady > 0 && timeUntilReady <= NOTIFICATION_ADVANCE_TIME) {
            readyItems.push({
              type: 'resource',
              name: 'Iron ready to mine',
              readyAt,
              timeUntilReady,
              id: `iron_${key}`,
            });
          }
        }
      });
    }

    // Gold
    if (farm.gold) {
      Object.entries(farm.gold).forEach(([key, gold]) => {
        if (gold.gold && gold.gold.minedAt) {
          const readyAt = gold.gold.minedAt + RESOURCE_GROWTH_TIMES.gold;
          const timeUntilReady = readyAt - now;
          if (timeUntilReady > 0 && timeUntilReady <= NOTIFICATION_ADVANCE_TIME) {
            readyItems.push({
              type: 'resource',
              name: 'Gold ready to mine',
              readyAt,
              timeUntilReady,
              id: `gold_${key}`,
            });
          }
        }
      });
    }

    // Crimstones
    if (farm.crimstones) {
      Object.entries(farm.crimstones).forEach(([key, crimstone]) => {
        if (crimstone.crimstone && crimstone.crimstone.minedAt) {
          const readyAt = crimstone.crimstone.minedAt + RESOURCE_GROWTH_TIMES.crimstone;
          const timeUntilReady = readyAt - now;
          if (timeUntilReady > 0 && timeUntilReady <= NOTIFICATION_ADVANCE_TIME) {
            readyItems.push({
              type: 'resource',
              name: 'Crimstone ready to mine',
              readyAt,
              timeUntilReady,
              id: `crimstone_${key}`,
            });
          }
        }
      });
    }

    // Oil Reserves
    if (farm.oil) {
      Object.entries(farm.oil).forEach(([key, oilNode]) => {
        if (oilNode.oil && oilNode.oil.minedAt) {
          const readyAt = oilNode.oil.minedAt + RESOURCE_GROWTH_TIMES.oil;
          const timeUntilReady = readyAt - now;
          if (timeUntilReady > 0 && timeUntilReady <= NOTIFICATION_ADVANCE_TIME) {
            readyItems.push({
              type: 'resource',
              name: 'Oil ready to drill',
              readyAt,
              timeUntilReady,
              id: `oil_${key}`,
            });
          }
        }
      });
    }

    // Lava Pits
    if (farm.lava) {
      Object.entries(farm.lava).forEach(([key, lavaNode]) => {
        if (lavaNode.lava && lavaNode.lava.minedAt) {
          const readyAt = lavaNode.lava.minedAt + RESOURCE_GROWTH_TIMES.lava;
          const timeUntilReady = readyAt - now;
          if (timeUntilReady > 0 && timeUntilReady <= NOTIFICATION_ADVANCE_TIME) {
            readyItems.push({
              type: 'resource',
              name: 'Lava ready to drill',
              readyAt,
              timeUntilReady,
              id: `lava_${key}`,
            });
          }
        }
      });
    }

    // Sunstones
    if (farm.sunstones) {
      Object.entries(farm.sunstones).forEach(([key, sunstone]) => {
        if (sunstone.sunstone && sunstone.sunstone.minedAt) {
          const readyAt = sunstone.sunstone.minedAt + RESOURCE_GROWTH_TIMES.sunstone;
          const timeUntilReady = readyAt - now;
          if (timeUntilReady > 0 && timeUntilReady <= NOTIFICATION_ADVANCE_TIME) {
            readyItems.push({
              type: 'resource',
              name: 'Sunstone ready to mine',
              readyAt,
              timeUntilReady,
              id: `sunstone_${key}`,
            });
          }
        }
      });
    }
  }

  async checkForReadyItems() {
    try {
      const now = Date.now();
      console.log(`\n🔍 [CHECK ${new Date(now).toLocaleTimeString()}] Checking for ready items...`);
      
      const farmData = await this.fetchFarmData();
      if (!farmData) {
        console.warn('⚠️ No farm data received - API call failed');
        return;
      }

      const readyItems = this.extractReadyItems(farmData);
      
      console.log(`📊 Ready items check complete: ${readyItems.length} items found`);
      
      if (readyItems.length > 0) {
        console.log(`� ITEMS READY TO HARVEST:`);
        readyItems.forEach(item => {
          console.log(`  ✅ ${item.name}`);
          console.log(`     Ready at: ${this.formatTimeInLocalTimezone(item.readyAt)}`);
          console.log(`     Time until ready: ${item.timeUntilReady}ms (${(item.timeUntilReady / 1000).toFixed(1)}s)`);
          console.log(`     Item ID: ${item.id}`);
        });
      } else {
        console.log('💤 No items ready yet');
      }

      for (const item of readyItems) {
        // Check if we've already scheduled this notification
        if (!this.scheduledNotifications.has(item.id)) {
          console.log(`📢 [NEW NOTIFICATION] Scheduling for: ${item.name}`);
          // Schedule notification for when item is ready
          this.scheduleNotification(item);
          this.scheduledNotifications.set(item.id, true);
        } else {
          console.log(`⏭️ [ALREADY SCHEDULED] ${item.name}`);
        }
      }
    } catch (error) {
      console.error('❌ Error checking for ready items:', error);
    }
  }

  async scheduleNotification(item) {
    try {
      // The item.timeUntilReady is how many milliseconds until the item is ready
      // We want to notify when it's ready, so we wait timeUntilReady milliseconds
      const delayMs = item.timeUntilReady;
      
      // Create a Date object for when to trigger the notification
      const notificationTime = new Date(Date.now() + delayMs);
      
      console.log(`⏰ SCHEDULING NOTIFICATION:`);
      console.log(`   Item: ${item.name}`);
      console.log(`   Item ready at (UTC): ${item.readyAt} (${this.formatTimeInLocalTimezone(item.readyAt)})`);
      console.log(`   Delay from now: ${delayMs}ms (${(delayMs / 1000).toFixed(1)}s)`);
      console.log(`   Will notify at: ${notificationTime.toLocaleString()}`);

      // Use LocalNotifications.schedule() with a delay
      // The delay is calculated from current time
      const id = Math.floor(Math.random() * 1000000);
      
      await LocalNotifications.schedule({
        notifications: [
          {
            id: id,
            title: '🌻 Farm Ready!',
            body: `${item.name} is ready!`,
            largeBody: `Your ${item.name} has completed and is ready to harvest or collect.`,
            schedule: {
              at: notificationTime, // Schedule for the exact time it will be ready
            },
            autoCancel: true,
            smallIcon: 'ic_stat_icon_config_sample',
          },
        ],
      });

      console.log(`✅ Notification scheduled with ID: ${id}`);
    } catch (error) {
      console.error('❌ Failed to schedule notification:', error);
      console.error('   Error details:', error.message);
    }
  }

  async sendTestNotification() {
    try {
      if (this.testNotificationSent) {
        console.log('📨 Test notification already sent');
        return;
      }
      
      console.log('📨 Attempting to send TEST notification...');
      console.log('📨 Checking Capacitor availability:', {
        hasWindow: typeof window !== 'undefined',
        hasCapacitor: typeof window?.Capacitor !== 'undefined',
        hasPlugins: typeof window?.Capacitor?.Plugins !== 'undefined',
        hasLocalNotifications: typeof window?.Capacitor?.Plugins?.LocalNotifications !== 'undefined',
        pluginsList: Object.keys(window?.Capacitor?.Plugins || {})
      });
      
      const testId = 999999;
      const testTime = new Date(Date.now() + 10000); // 10 seconds from now
      
      try {
        await LocalNotifications.schedule({
          notifications: [
            {
              id: testId,
              title: '🧪 TEST NOTIFICATION',
              body: 'If you see this, notifications are working!',
              largeBody: 'This is a test notification sent when the app started. Notification system is operational!',
              schedule: {
                at: testTime,
              },
              autoCancel: true,
              smallIcon: 'ic_stat_icon_config_sample',
            },
          ],
        });
        
        this.testNotificationSent = true;
        console.log(`✅ TEST notification scheduled for ${testTime.toLocaleString()}`);
      } catch (scheduleError) {
        console.error('❌ Schedule failed:', scheduleError);
        console.log('📨 Method 2: Trying immediate notification...');
        
        // Try immediate notification as fallback
        try {
          await LocalNotifications.schedule({
            notifications: [
              {
                id: testId,
                title: '🧪 TEST NOTIFICATION (IMMEDIATE)',
                body: 'If you see this, notifications are working!',
                schedule: {
                  at: new Date(Date.now() + 500), // 500ms
                },
              },
            ],
          });
          
          this.testNotificationSent = true;
          console.log('✅ TEST notification sent immediately');
        } catch (immediateError) {
          console.error('❌ Immediate notification also failed:', immediateError);
        }
      }
    } catch (error) {
      console.error('❌ Failed to send test notification:', error);
      console.error('❌ Error message:', error.message);
      console.error('❌ Error code:', error.code);
    }
  }

  stop() {
    if (this.checkInterval) {
      clearInterval(this.checkInterval);
      this.checkInterval = null;
    }
  }
}

// Export singleton instance
export const notificationService = new NotificationService();
