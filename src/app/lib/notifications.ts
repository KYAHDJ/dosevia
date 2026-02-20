import { LocalNotifications } from '@capacitor/local-notifications';
import { Capacitor } from '@capacitor/core';

const ALARM_CHANNEL_ID = 'dosevia-critical-alarm';
const NOTIFICATION_IDS = {
  EARLY_30MIN: 100,
  EARLY_1MIN: 101,
  MAIN_ALARM: 1,
  ESCALATION_BASE: 1000,
  MISSED_PILL_WARNING: 2000,
  PILL_BUYING_REMINDER: 3000,
};

/**
 * ICON MAPPING - Maps emoji icons to Android drawable resources
 */
const ICON_MAP: Record<string, string> = {
  '💊': 'ic_stat_pill',
  '🔔': 'ic_stat_bell',
  '❤️': 'ic_stat_heart',
  '⭐': 'ic_stat_star',
  '📅': 'ic_stat_calendar',
  '⚠️': 'ic_stat_alert',
  '✅': 'ic_stat_check',
  '⏰': 'ic_stat_clock',
  '⚡': 'ic_stat_zap',
  '📊': 'ic_stat_activity',
  '☕': 'ic_stat_coffee',
  '☀️': 'ic_stat_sun',
  '🌙': 'ic_stat_moon',
  '✨': 'ic_stat_sparkles',
  '⭕': 'ic_stat_circle',
  'pill': 'ic_stat_pill', // Default
};

/**
 * Get Android drawable resource name from emoji
 */
function getIconResource(iconEmoji: string): string {
  const resource = ICON_MAP[iconEmoji] || ICON_MAP['pill'];
  console.log(`🎨 Icon mapping: ${iconEmoji} → ${resource}`);
  return resource;
}

/**
 * Get sound resource from filename
 * Returns the filename (without extension) for Android raw resources
 * OR undefined to use default system sound
 */
function getSoundResource(soundFileName: string | undefined): string | undefined {
  if (!soundFileName || soundFileName === 'default' || soundFileName === 'Default' || soundFileName === '') {
    console.log('🔊 Using default system alarm sound');
    return undefined; // This makes Android use default alarm sound
  }

  // The filename is already sanitized from AudioPicker (e.g., "dosevia_1234567_myalarm")
  // This should match a file in android/app/src/main/res/raw/ OR app storage
  console.log(`🔊 Using custom sound: ${soundFileName}`);
  return soundFileName;
}

/**
 * Initialize notification channels with MAXIMUM priority
 */
export async function initNotifications() {
  if (Capacitor.getPlatform() !== 'android') {
    console.log('⚠️ Notifications only supported on Android');
    return;
  }

  try {
    // Request permissions
    const perm = await LocalNotifications.requestPermissions();
    console.log('📱 Notification permission:', perm.display);

    if (perm.display !== 'granted') {
      console.error('❌ Notification permission denied');
      return;
    }

    // Create CRITICAL alarm channel
    await LocalNotifications.createChannel({
      id: ALARM_CHANNEL_ID,
      name: 'Medication Alarms',
      description: 'Critical medication reminders with sound and vibration',
      importance: 5, // MAXIMUM importance (CRITICAL)
      sound: undefined, // Let system use default alarm sound
      vibration: true,
      visibility: 1, // PUBLIC - show on lock screen
      lights: true,
      lightColor: '#f609bc',
    });

    console.log('✅ Notification channel created');
  } catch (error) {
    console.error('❌ Error initializing notifications:', error);
  }
}

/**
 * Schedule daily alarm with custom icon and sound
 */
export async function scheduleDailyAlarm(
  hour: number,
  minute: number,
  title: string,
  body: string,
  notificationIcon: string = '💊',
  soundFileName?: string, // The filename saved from AudioPicker
  vibrate: boolean = true,
  isPillAlreadyTaken: boolean = false
) {
  try {
    await cancelAllAlarms();

    if (isPillAlreadyTaken) {
      console.log('✅ Pill already taken - no alarms scheduled');
      return;
    }

    const now = new Date();
    const mainTrigger = new Date();
    mainTrigger.setHours(hour, minute, 0, 0);

    if (mainTrigger <= now) {
      mainTrigger.setDate(mainTrigger.getDate() + 1);
    }

    const iconResource = getIconResource(notificationIcon);
    const soundResource = getSoundResource(soundFileName);

    console.log(`⏰ Scheduling alarms for ${mainTrigger.toLocaleString()}`);
    console.log(`🎨 Using icon: ${notificationIcon} → ${iconResource}`);
    console.log(`🔊 Using sound: ${soundFileName || 'default system alarm'} → ${soundResource || 'system default'}`);

    const notifications = [];

    // 1. Early warning - 30 minutes before
    const early30MinTrigger = new Date(mainTrigger.getTime() - 30 * 60 * 1000);
    if (early30MinTrigger > now) {
      notifications.push({
        id: NOTIFICATION_IDS.EARLY_30MIN,
        title: '⏰ Medication Reminder - 30 Minutes',
        body: 'Your medication time is in 30 minutes. Get ready!',
        channelId: ALARM_CHANNEL_ID,
        schedule: {
          at: early30MinTrigger,
          allowWhileIdle: true,
        },
        sound: soundResource,
        smallIcon: iconResource,
        actionTypeId: '',
        autoCancel: false,
        ongoing: false,
        extra: { 
          type: 'early_30min',
          playSound: true,
        },
      });
      console.log('📢 Scheduled 30-min early warning at', early30MinTrigger.toLocaleTimeString());
    }

    // 2. Early warning - 1 minute before
    const early1MinTrigger = new Date(mainTrigger.getTime() - 60 * 1000);
    if (early1MinTrigger > now) {
      notifications.push({
        id: NOTIFICATION_IDS.EARLY_1MIN,
        title: '⏰ Medication Reminder - 1 Minute',
        body: 'Your medication time is in 1 minute! Get ready.',
        channelId: ALARM_CHANNEL_ID,
        schedule: {
          at: early1MinTrigger,
          allowWhileIdle: true,
        },
        sound: soundResource,
        smallIcon: iconResource,
        actionTypeId: '',
        autoCancel: false,
        ongoing: false,
        extra: { 
          type: 'early_1min',
          playSound: true,
        },
      });
      console.log('📢 Scheduled 1-min early warning at', early1MinTrigger.toLocaleTimeString());
    }

    // 3. Main alarm at exact time
    notifications.push({
      id: NOTIFICATION_IDS.MAIN_ALARM,
      title: `🔔 ${title}`,
      body,
      channelId: ALARM_CHANNEL_ID,
      schedule: {
        at: mainTrigger,
        allowWhileIdle: true,
      },
      sound: soundResource,
      smallIcon: iconResource,
      actionTypeId: '',
      ongoing: false,
      autoCancel: false,
      extra: {
        type: 'main_alarm',
        playSound: true,
      },
    });
    console.log('🔔 Scheduled main alarm at', mainTrigger.toLocaleTimeString());

    // 4. Escalating then AGGRESSIVE repeating alarms
    // Pattern: Start gentle, then become relentless
    // Phase 1: Escalate from 30s → 10s
    // Phase 2: Continue AGGRESSIVELY at 10s intervals
    
    const escalationPattern = [
      30, 30,      // Start: 2 alarms at 30 seconds
      25, 25,      // Getting closer: 2 alarms at 25 seconds
      20, 20,      // More urgent: 2 alarms at 20 seconds
      15, 15,      // Very urgent: 2 alarms at 15 seconds
      10, 10, 10,  // Critical: 3 alarms at 10 seconds
    ];
    
    // After escalation, continue AGGRESSIVELY at 10 seconds
    const AGGRESSIVE_INTERVAL = 10;
    const AGGRESSIVE_COUNT = 90; // 90 more alarms at 10s = 15 minutes
    
    const allIntervals = [
      ...escalationPattern,
      ...Array(AGGRESSIVE_COUNT).fill(AGGRESSIVE_INTERVAL)
    ];

    let cumulativeDelay = 0;
    for (let i = 0; i < allIntervals.length; i++) {
      const interval = allIntervals[i];
      cumulativeDelay += interval * 1000; // Convert to milliseconds

      const escalationTrigger = new Date(mainTrigger.getTime() + cumulativeDelay);

      if (escalationTrigger > now) {
        const isAggressivePhase = i >= escalationPattern.length;
        
        notifications.push({
          id: NOTIFICATION_IDS.ESCALATION_BASE + i,
          title: isAggressivePhase ? `🚨 CRITICAL: ${title}` : `🔔 URGENT: ${title}`,
          body: `⏰ Reminder #${i + 1}: ${body}`,
          channelId: ALARM_CHANNEL_ID,
          schedule: {
            at: escalationTrigger,
            allowWhileIdle: true,
          },
          sound: soundResource,
          smallIcon: iconResource,
          actionTypeId: '',
          ongoing: isAggressivePhase, // Persistent in aggressive phase
          autoCancel: false,
          extra: {
            type: isAggressivePhase ? 'aggressive_alarm' : 'escalating_alarm',
            attemptNumber: i + 1,
            intervalSeconds: interval,
            playSound: true,
            phase: isAggressivePhase ? 'aggressive' : 'escalating',
          },
        });
      }
    }
    
    const escalatingCount = notifications.filter(n => n.extra?.type === 'escalating_alarm').length;
    const aggressiveCount = notifications.filter(n => n.extra?.type === 'aggressive_alarm').length;
    console.log(`📢 Scheduled ${escalatingCount} escalating alarms (30s → 10s)`);
    console.log(`🚨 Scheduled ${aggressiveCount} AGGRESSIVE alarms (every 10s)`);

    // Schedule all notifications at once
    if (notifications.length > 0) {
      await LocalNotifications.schedule({ notifications });
      console.log(`✅ Total notifications scheduled: ${notifications.length}`);
      console.log(`   - Early warnings: ${notifications.filter(n => n.extra?.type?.includes('early')).length}`);
      console.log(`   - Main alarm: 1`);
      console.log(`   - Escalating alarms: ${escalatingCount}`);
    }
  } catch (error) {
    console.error('❌ Error scheduling alarms:', error);
  }
}

/**
 * Cancel all alarms (called when pill is marked as taken)
 */
export async function cancelAllAlarms() {
  try {
    const notificationIds = [
      { id: NOTIFICATION_IDS.EARLY_30MIN },
      { id: NOTIFICATION_IDS.EARLY_1MIN },
      { id: NOTIFICATION_IDS.MAIN_ALARM },
      { id: NOTIFICATION_IDS.MISSED_PILL_WARNING },
      { id: NOTIFICATION_IDS.PILL_BUYING_REMINDER },
    ];

    // Add all escalating alarm IDs (100 alarms for continuous reminders)
    for (let i = 0; i < 100; i++) {
      notificationIds.push({ id: NOTIFICATION_IDS.ESCALATION_BASE + i });
    }

    await LocalNotifications.cancel({ notifications: notificationIds });
    console.log('🔕 All alarms canceled (total:', notificationIds.length, 'notifications)');
  } catch (error) {
    console.error('❌ Error canceling alarms:', error);
  }
}

/**
 * Schedule missed pill warning with custom icon and sound
 * CRITICAL: Continuous vibration for missed pills
 */
export async function scheduleMissedPillWarning(
  hour: number,
  minute: number,
  notificationIcon: string = '💊',
  soundFileName?: string
) {
  try {
    const now = new Date();
    const pillTime = new Date();
    pillTime.setHours(hour, minute, 0, 0);
    
    if (pillTime <= now) {
      pillTime.setDate(pillTime.getDate() + 1);
    }
    
    const warningTime = new Date(pillTime.getTime() - 5 * 60 * 60 * 1000);
    
    if (warningTime > now) {
      const iconResource = getIconResource(notificationIcon);
      const soundResource = getSoundResource(soundFileName);

      await LocalNotifications.schedule({
        notifications: [
          {
            id: NOTIFICATION_IDS.MISSED_PILL_WARNING,
            title: '🚨 URGENT: Missed Pill Warning',
            body: 'You haven\'t taken today\'s pill yet! Take it now to avoid missing your dose.',
            channelId: ALARM_CHANNEL_ID,
            schedule: {
              at: warningTime,
              allowWhileIdle: true,
            },
            sound: soundResource,
            smallIcon: iconResource,
            actionTypeId: '',
            autoCancel: false,
            ongoing: true, // Makes it persistent
            extra: {
              type: 'missed_pill_warning',
              playSound: true,
              vibrate: true,
            },
          },
        ],
      });
      
      console.log(`⚠️ Scheduled CRITICAL missed pill warning at ${warningTime.toLocaleTimeString()}`);
    } else {
      console.log('⚠️ Warning time already passed, not scheduling');
    }
  } catch (error) {
    console.error('❌ Error scheduling missed pill warning:', error);
  }
}

/**
 * Check for missed pills - placeholder for future use
 */
export function checkAndMarkMissedPills(days: any[], setDays: any) {
  // Implementation in App.tsx
}

/**
 * Schedule pill buying reminder with repeating notifications
 * Repeats 5 times at 20-second intervals
 */
export async function schedulePillBuyingReminder(
  reminderDate: Date,
  hour: number,
  minute: number,
  notificationIcon: string = '🛒',
  soundFileName?: string
) {
  try {
    // Set the exact time on the reminder date
    const scheduledTime = new Date(reminderDate);
    scheduledTime.setHours(hour, minute, 0, 0);

    const now = new Date();
    
    // Only schedule if the reminder date is in the future
    if (scheduledTime <= now) {
      console.log('📅 Pill buying reminder date is in the past, not scheduling');
      return;
    }

    const iconResource = getIconResource(notificationIcon);
    const soundResource = getSoundResource(soundFileName);

    const notifications = [];

    // Main reminder
    notifications.push({
      id: NOTIFICATION_IDS.PILL_BUYING_REMINDER,
      title: '🛒 Time to Buy More Pills',
      body: 'You need to purchase your next pack of pills. Don\'t forget!',
      channelId: ALARM_CHANNEL_ID,
      schedule: {
        at: scheduledTime,
        allowWhileIdle: true,
      },
      sound: soundResource,
      smallIcon: iconResource,
      actionTypeId: '',
      autoCancel: false,
      ongoing: false,
      extra: {
        type: 'pill_buying_reminder',
        playSound: true,
        attemptNumber: 1,
      },
    });

    // Repeating reminders - 5 times at 20-second intervals
    for (let i = 1; i <= 5; i++) {
      const repeatTime = new Date(scheduledTime.getTime() + (i * 20 * 1000));
      
      notifications.push({
        id: NOTIFICATION_IDS.PILL_BUYING_REMINDER + i,
        title: `🛒 REMINDER #${i + 1}: Buy Pills`,
        body: 'Please purchase your next pack of pills soon!',
        channelId: ALARM_CHANNEL_ID,
        schedule: {
          at: repeatTime,
          allowWhileIdle: true,
        },
        sound: soundResource,
        smallIcon: iconResource,
        actionTypeId: '',
        autoCancel: false,
        ongoing: false,
        extra: {
          type: 'pill_buying_reminder_repeat',
          playSound: true,
          attemptNumber: i + 1,
        },
      });
    }

    await LocalNotifications.schedule({ notifications });

    console.log(`🛒 Scheduled pill buying reminder for ${scheduledTime.toLocaleDateString()} at ${scheduledTime.toLocaleTimeString()}`);
    console.log(`📢 Scheduled 5 repeat reminders (every 20 seconds)`);
  } catch (error) {
    console.error('❌ Error scheduling pill buying reminder:', error);
  }
}

/**
 * Cancel pill buying reminder and all its repeats
 */
export async function cancelPillBuyingReminder() {
  try {
    const notificationIds = [{ id: NOTIFICATION_IDS.PILL_BUYING_REMINDER }];
    
    // Add all repeat notification IDs
    for (let i = 1; i <= 5; i++) {
      notificationIds.push({ id: NOTIFICATION_IDS.PILL_BUYING_REMINDER + i });
    }

    await LocalNotifications.cancel({ notifications: notificationIds });
    console.log('🔕 Canceled pill buying reminder and all repeats');
  } catch (error) {
    console.error('❌ Error canceling pill buying reminder:', error);
  }
}
