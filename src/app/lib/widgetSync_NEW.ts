import { registerPlugin } from '@capacitor/core';

/**
 * Widget Sync Plugin Interface
 * 
 * Manages synchronization between the app and home screen widget.
 * Uses SharedPreferences as single source of truth.
 * Tracks current CYCLE pills (not daily), resetting only when user starts new cycle.
 */
export interface WidgetSyncPlugin {
  /**
   * Save pill count data for current cycle and update widget
   * 
   * Call this whenever pill count changes in current cycle:
   * - User marks pill as taken
   * - User untakes a pill
   * - User starts a new cycle (resets to 0)
   * - User changes pill type
   * 
   * @param data - Pill count data for current cycle
   */
  savePillCount(data: {
    pillsTakenCurrentCycle: number;
    totalPillsCurrentCycle: number;
    pillType?: string;
  }): Promise<void>;

  /**
   * Manually refresh widget
   * Reads current data from SharedPreferences and updates UI
   */
  refreshWidget(): Promise<void>;

  /**
   * Get current pill count from SharedPreferences
   * Useful for verifying sync status
   */
  getPillCount(): Promise<{
    pillsTakenCurrentCycle: number;
    totalPillsCurrentCycle: number;
    lastUpdated: number;
  }>;
}

// Register the plugin
const WidgetSync = registerPlugin<WidgetSyncPlugin>('WidgetSync', {
  web: () => import('./widgetSync.web').then(m => new m.WidgetSyncWeb()),
});

/**
 * Update widget with current cycle pill count
 * 
 * CRITICAL: Call this after EVERY pill status change
 * 
 * @param pillsTakenCurrentCycle - Number of pills taken in current cycle
 * @param totalPillsCurrentCycle - Total pills in current cycle (depends on pill type)
 */
export async function updateWidgetPillCount(
  pillsTakenCurrentCycle: number,
  totalPillsCurrentCycle: number,
  pillType?: string
): Promise<void> {
  try {
    console.log(`Updating widget: ${pillsTakenCurrentCycle} / ${totalPillsCurrentCycle} pills (current cycle, type: ${pillType})`);

    const payload: any = {
      pillsTakenCurrentCycle,
      totalPillsCurrentCycle,
    };

    if (pillType) {
      payload.pillType = pillType;
    }

    await WidgetSync.savePillCount(payload);
    
    console.log('Widget updated successfully');
  } catch (error) {
    console.error('Failed to update widget:', error);
    // Don't throw - widget update failure shouldn't break app
  }
}

/**
 * Manually refresh widget
 */
export async function refreshWidget(): Promise<void> {
  try {
    await WidgetSync.refreshWidget();
    console.log('Widget refreshed');
  } catch (error) {
    console.error('Failed to refresh widget:', error);
  }
}

/**
 * Get current widget data for debugging
 */
export async function getWidgetData(): Promise<{
  pillsTakenCurrentCycle: number;
  totalPillsCurrentCycle: number;
  lastUpdated: number;
} | null> {
  try {
    const data = await WidgetSync.getPillCount();
    console.log('Widget data:', data);
    return data;
  } catch (error) {
    console.error('Failed to get widget data:', error);
    return null;
  }
}

export default WidgetSync;
