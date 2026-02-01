// Based on real birth control pill regimens used worldwide
export type PillType = 
  // Standard 28-day cycles (most common)
  | '21+7'      // 21 active + 7 placebo (most common traditional pack)
  | '24+4'      // 24 active + 4 placebo (shorter placebo interval)
  | '26+2'      // 26 active + 2 placebo (very short placebo interval)
  | '28-day'    // 28 active, no placebo (continuous)
  
  // Extended cycle regimens
  | '84+7'      // 84 active + 7 placebo (91-day pack, period every 3 months)
  | '84+7-low'  // 84 active + 7 low-dose estrogen (Seasonique-type)
  | '365-day'   // 365 active, no placebo (Lybrel-type, no periods)
  
  // Progestin-only pills (minipills)
  | '28-pop'    // 28 progestin-only (all active, no placebo, must take same time daily)
  
  // Flexible extended regimens
  | 'flexible'  // User-controlled cycle length (24-120 days active, then 4-day break)
  | 'custom';   // Custom configuration

export type PillStatus = 'not_taken' | 'taken' | 'missed';

export interface DayData {
  day: number;
  status: PillStatus;
  isPlacebo: boolean;
  date: Date;
  takenAt?: string; // ISO timestamp when pill was taken
  isLowDose?: boolean; // For pills like Seasonique that have low-dose estrogen instead of placebo
}

export interface ReminderSettings {
  placeboReminder: boolean;
  dailyReminderTime: string;
  pillBuyingDaysBefore: number;
  pillBuyingReminderTime: string;
  appActive: boolean;
  repeatInterval: number;
  notificationSound: string;
  soundFileUri?: string; // URI of custom sound file
  playSoundAlways: boolean;
  vibrateAlways: boolean;
  notificationTitle: string;
  notificationSubtitle: string;
  notificationIcon: string;
}
