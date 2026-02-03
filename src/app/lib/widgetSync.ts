import { registerPlugin } from '@capacitor/core';
import { DayData, PillType } from '@/types/pill-types';

export interface WidgetSyncPlugin {
  updateWidgets(): Promise<void>;
  savePillData(data: {
    days: string;
    startDate: string;
    pillType: string;
    takenCount: number;
    missedCount: number;
    totalCount: number;
  }): Promise<void>;
  requestPinWidget(options: { widgetType: string }): Promise<void>;
}

const WidgetSync = registerPlugin<WidgetSyncPlugin>('WidgetSync', {
  web: () => import('./widgetSync.web').then(m => new m.WidgetSyncWeb()),
});

/**
 * Sync widget data with the native widgets
 * This should be called whenever pill data changes
 */
export async function syncWidgetData(
  days: DayData[],
  startDate: Date,
  pillType: PillType
) {
  try {
    // Calculate statistics
    const takenCount = days.filter(d => d.status === 'taken').length;
    const missedCount = days.filter(d => d.status === 'missed').length;
    const totalCount = days.length;

    // Format dates properly for Android (yyyy-MM-dd)
    const formatDate = (date: Date) => {
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      return `${year}-${month}-${day}`;
    };

    // Prepare data for native layer
    const daysData = days.map(day => ({
      day: day.day,
      status: day.status,
      isPlacebo: day.isPlacebo,
      date: formatDate(day.date),
      takenAt: day.takenAt,
      isLowDose: day.isLowDose || false,
    }));

    const startDateFormatted = formatDate(startDate);

    await WidgetSync.savePillData({
      days: JSON.stringify(daysData),
      startDate: startDateFormatted,
      pillType: pillType,
      takenCount,
      missedCount,
      totalCount,
    });

    console.log('Widget data synced successfully', {
      startDate: startDateFormatted,
      totalDays: daysData.length,
      takenCount,
      missedCount
    });
  } catch (error) {
    console.error('Failed to sync widget data:', error);
  }
}

/**
 * Request to pin a widget to the home screen
 */
export async function requestPinWidget(widgetType: 'small' | 'medium' | 'large') {
  try {
    await WidgetSync.requestPinWidget({ widgetType });
  } catch (error) {
    console.error('Failed to request pin widget:', error);
  }
}

/**
 * Force update all widgets
 */
export async function forceUpdateWidgets() {
  try {
    await WidgetSync.updateWidgets();
  } catch (error) {
    console.error('Failed to force update widgets:', error);
  }
}
