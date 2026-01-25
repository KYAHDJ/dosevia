export type PillType = '21+7' | '24+4' | '28-day';

export type PillStatus = 'not_taken' | 'taken' | 'missed';

export interface DayData {
  day: number;
  status: PillStatus;
  isPlacebo: boolean;
  date: Date;
}

export interface ReminderSettings {
  placeboReminder: boolean;
  dailyReminderTime: string;
  pillBuyingDaysBefore: number;
  pillBuyingReminderTime: string;
  appActive: boolean;
  repeatInterval: number;
  notificationSound: string;
  playSoundAlways: boolean;
  vibrateAlways: boolean;
  notificationTitle: string;
  notificationSubtitle: string;
  notificationIcon: string;
}
