/**
 * EXAMPLE: How to Integrate the Widget in Your App
 * 
 * This file shows practical examples of when and how to update the widget
 * Copy these patterns into your actual app code
 */

import { 
  updateWidgetPillCount,
  resetDailyPillCount,
  getWidgetData 
} from '@/app/lib/widgetSync';

// ============================================
// EXAMPLE 1: Mark Pill as Taken
// ============================================

async function handleMarkPillTaken(pillId: string, allPills: Pill[]) {
  // 1. Update your app's state
  const updatedPills = allPills.map(pill => 
    pill.id === pillId 
      ? { ...pill, status: 'taken', takenAt: new Date() }
      : pill
  );
  
  // Save to your storage (localStorage, etc.)
  savePillsToStorage(updatedPills);
  
  // 2. Calculate TODAY's counts
  const today = new Date().toDateString();
  const todaysPills = updatedPills.filter(p => 
    new Date(p.date).toDateString() === today
  );
  
  const pillsTakenToday = todaysPills.filter(p => p.status === 'taken').length;
  const totalPillsToday = todaysPills.length;
  
  // 3. Update widget IMMEDIATELY
  await updateWidgetPillCount(pillsTakenToday, totalPillsToday);
  
  // Widget now shows updated count!
}

// ============================================
// EXAMPLE 2: Untake a Pill
// ============================================

async function handleUntakePill(pillId: string, allPills: Pill[]) {
  // 1. Update app state
  const updatedPills = allPills.map(pill => 
    pill.id === pillId 
      ? { ...pill, status: 'not_taken', takenAt: null }
      : pill
  );
  
  savePillsToStorage(updatedPills);
  
  // 2. Recalculate counts
  const today = new Date().toDateString();
  const todaysPills = updatedPills.filter(p => 
    new Date(p.date).toDateString() === today
  );
  
  const pillsTakenToday = todaysPills.filter(p => p.status === 'taken').length;
  const totalPillsToday = todaysPills.length;
  
  // 3. Update widget
  await updateWidgetPillCount(pillsTakenToday, totalPillsToday);
}

// ============================================
// EXAMPLE 3: Add New Pill
// ============================================

async function handleAddPill(newPill: Pill, allPills: Pill[]) {
  // 1. Add pill
  const updatedPills = [...allPills, newPill];
  savePillsToStorage(updatedPills);
  
  // 2. If it's for today, update widget
  const today = new Date().toDateString();
  const pillDate = new Date(newPill.date).toDateString();
  
  if (pillDate === today) {
    const todaysPills = updatedPills.filter(p => 
      new Date(p.date).toDateString() === today
    );
    
    const pillsTakenToday = todaysPills.filter(p => p.status === 'taken').length;
    const totalPillsToday = todaysPills.length;
    
    await updateWidgetPillCount(pillsTakenToday, totalPillsToday);
  }
}

// ============================================
// EXAMPLE 4: Delete Pill
// ============================================

async function handleDeletePill(pillId: string, allPills: Pill[]) {
  // 1. Remove pill
  const pillToDelete = allPills.find(p => p.id === pillId);
  const updatedPills = allPills.filter(p => p.id !== pillId);
  savePillsToStorage(updatedPills);
  
  // 2. If it was for today, update widget
  const today = new Date().toDateString();
  const pillDate = new Date(pillToDelete.date).toDateString();
  
  if (pillDate === today) {
    const todaysPills = updatedPills.filter(p => 
      new Date(p.date).toDateString() === today
    );
    
    const pillsTakenToday = todaysPills.filter(p => p.status === 'taken').length;
    const totalPillsToday = todaysPills.length;
    
    await updateWidgetPillCount(pillsTakenToday, totalPillsToday);
  }
}

// ============================================
// EXAMPLE 5: Daily Reset (Midnight)
// ============================================

function setupMidnightReset() {
  const scheduleMidnightReset = () => {
    const now = new Date();
    const midnight = new Date(now);
    midnight.setHours(24, 0, 0, 0); // Next midnight
    
    const msUntilMidnight = midnight.getTime() - now.getTime();
    
    console.log(`Scheduling reset in ${msUntilMidnight}ms (${msUntilMidnight / 1000 / 60} minutes)`);
    
    setTimeout(async () => {
      console.log('🌙 Midnight reset triggered');
      
      // Reset widget count to 0
      await resetDailyPillCount();
      
      // Also reset your app's daily tracking if needed
      resetAppDailyTracking();
      
      // Schedule next reset
      scheduleMidnightReset();
    }, msUntilMidnight);
  };
  
  scheduleMidnightReset();
}

// Call on app startup
// setupMidnightReset();

// ============================================
// EXAMPLE 6: App Initialization
// ============================================

async function initializeAppWithWidget(allPills: Pill[]) {
  // Calculate current day's pills
  const today = new Date().toDateString();
  const todaysPills = allPills.filter(p => 
    new Date(p.date).toDateString() === today
  );
  
  const pillsTakenToday = todaysPills.filter(p => p.status === 'taken').length;
  const totalPillsToday = todaysPills.length;
  
  // Sync widget with current state
  await updateWidgetPillCount(pillsTakenToday, totalPillsToday);
  
  // Setup midnight reset
  setupMidnightReset();
  
  console.log(`✅ App initialized: ${pillsTakenToday} / ${totalPillsToday} pills taken`);
}

// ============================================
// EXAMPLE 7: React Component Integration
// ============================================

import { useEffect, useState } from 'react';

function PillTrackerComponent() {
  const [pills, setPills] = useState<Pill[]>([]);
  
  // Initialize widget on mount
  useEffect(() => {
    const loadPills = async () => {
      const loadedPills = await loadPillsFromStorage();
      setPills(loadedPills);
      
      // Sync widget
      const today = new Date().toDateString();
      const todaysPills = loadedPills.filter(p => 
        new Date(p.date).toDateString() === today
      );
      
      const pillsTaken = todaysPills.filter(p => p.status === 'taken').length;
      const totalPills = todaysPills.length;
      
      await updateWidgetPillCount(pillsTaken, totalPills);
    };
    
    loadPills();
    setupMidnightReset();
  }, []);
  
  // Update widget whenever pills change
  useEffect(() => {
    const syncWidget = async () => {
      const today = new Date().toDateString();
      const todaysPills = pills.filter(p => 
        new Date(p.date).toDateString() === today
      );
      
      const pillsTaken = todaysPills.filter(p => p.status === 'taken').length;
      const totalPills = todaysPills.length;
      
      await updateWidgetPillCount(pillsTaken, totalPills);
    };
    
    syncWidget();
  }, [pills]); // Runs whenever pills state changes
  
  const handleTakePill = (pillId: string) => {
    setPills(prevPills => 
      prevPills.map(p => 
        p.id === pillId 
          ? { ...p, status: 'taken', takenAt: new Date() }
          : p
      )
    );
    // Widget will auto-update via useEffect above!
  };
  
  return (
    <div>
      {/* Your UI here */}
    </div>
  );
}

// ============================================
// EXAMPLE 8: Debug Widget Sync
// ============================================

async function debugWidgetSync(allPills: Pill[]) {
  console.log('🔍 Debugging widget sync...');
  
  // Check what app thinks
  const today = new Date().toDateString();
  const todaysPills = allPills.filter(p => 
    new Date(p.date).toDateString() === today
  );
  const appCount = todaysPills.filter(p => p.status === 'taken').length;
  const appTotal = todaysPills.length;
  
  console.log(`App thinks: ${appCount} / ${appTotal}`);
  
  // Check what widget has
  const widgetData = await getWidgetData();
  if (widgetData) {
    console.log(`Widget has: ${widgetData.pillsTakenToday} / ${widgetData.totalPillsToday}`);
    console.log(`Last updated: ${new Date(widgetData.lastUpdated)}`);
    
    if (appCount !== widgetData.pillsTakenToday || appTotal !== widgetData.totalPillsToday) {
      console.log('⚠️ MISMATCH! Re-syncing...');
      await updateWidgetPillCount(appCount, appTotal);
      console.log('✅ Re-synced!');
    } else {
      console.log('✅ In sync!');
    }
  }
}

// ============================================
// EXAMPLE 9: Handle App Coming to Foreground
// ============================================

// If your app supports background/foreground
function handleAppComingToForeground(allPills: Pill[]) {
  // Check if it's a new day
  const lastCheckDate = localStorage.getItem('lastCheckDate');
  const today = new Date().toDateString();
  
  if (lastCheckDate !== today) {
    console.log('📅 New day detected!');
    
    // Reset for new day
    resetDailyPillCount();
    localStorage.setItem('lastCheckDate', today);
  }
  
  // Always sync widget when coming to foreground
  const todaysPills = allPills.filter(p => 
    new Date(p.date).toDateString() === today
  );
  
  const pillsTaken = todaysPills.filter(p => p.status === 'taken').length;
  const totalPills = todaysPills.length;
  
  updateWidgetPillCount(pillsTaken, totalPills);
}

// ============================================
// HELPER TYPES (for reference)
// ============================================

interface Pill {
  id: string;
  date: Date;
  status: 'taken' | 'not_taken' | 'missed';
  takenAt?: Date;
  name?: string;
}

// Placeholder functions (implement based on your app)
function savePillsToStorage(pills: Pill[]) { /* Your storage logic */ }
function loadPillsFromStorage(): Promise<Pill[]> { /* Your storage logic */ return Promise.resolve([]); }
function resetAppDailyTracking() { /* Your reset logic */ }
