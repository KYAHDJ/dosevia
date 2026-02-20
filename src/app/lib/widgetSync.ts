import { registerPlugin } from '@capacitor/core';
import { DayData, PillType } from '@/types/pill-types';

export interface WidgetSyncPlugin {
  updateWidgets(): Promise<void>;
  savePillCount(data: {
    pillsTakenCurrentCycle: number;
    totalPillsCurrentCycle: number;
  }): Promise<void>;
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


export function getTotalPillsForType(pillType: PillType): number {
  switch (pillType) {
    case '21+7': return 28;
    case '24+4': return 28;
    case '26+2': return 28;
    case '28-day': return 28;
    case '28-pop': return 28;
    case '84+7': return 91;
    case '84+7-low': return 91;
    case '365-day': return 365;
    default:
      return 28; // safe fallback
  }
}




export async function syncWidgetData(
  days: DayData[],
  startDate: Date,
  pillType: PillType
) {
  try {
    // Calculate statistics for CURRENT CYCLE (all pills in days array)
    const takenCount = days.filter(d => d.status === 'taken').length;
    const totalCount = getTotalPillsForType(pillType);


    console.log('Syncing widget with cycle data:', {
      pillType,
      totalPills: totalCount,
      takenPills: takenCount,
      startDate: startDate.toISOString()
    });

    // Update the new cycle-based widget with pillType
    await updateWidgetPillCount(takenCount, totalCount, pillType);

    console.log('Widget data synced successfully');
  } catch (error) {
    console.error('Failed to sync widget data:', error);
  }
}

/**
 * Update widget with current cycle pill count
 * Call this after every pill status change
 */
export async function updateWidgetPillCount(
  pillsTakenCurrentCycle: number,
  totalPillsCurrentCycle: number,
  pillType?: PillType
): Promise<void> {
  try {
    console.log(`Updating widget: ${pillsTakenCurrentCycle} / ${totalPillsCurrentCycle} pills (type: ${pillType})`);
    
    const payload: any = {
      pillsTakenCurrentCycle,
      totalPillsCurrentCycle,
    };
    
    // Include pillType if provided
    if (pillType) {
      payload.pillType = pillType;
    }
    
    await WidgetSync.savePillCount(payload);
    
    console.log('Widget updated successfully');
  } catch (error) {
    console.error('Failed to update widget:', error);
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
