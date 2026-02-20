#!/bin/bash

# Dosevia Widget Migration Script
# This script helps migrate from old 3-widget system to new single widget

set -e  # Exit on error

echo "🎯 Dosevia Widget Migration Script"
echo "===================================="
echo ""

# Check if we're in the right directory
if [ ! -f "capacitor.config.json" ]; then
    echo "❌ Error: Not in project root. Please run from dosevia-main directory."
    exit 1
fi

echo "📁 Current directory: $(pwd)"
echo ""

# Create backup directory
BACKUP_DIR="widget_backup_$(date +%Y%m%d_%H%M%S)"
mkdir -p "$BACKUP_DIR"
echo "✅ Created backup directory: $BACKUP_DIR"
echo ""

# Backup old widget files
echo "📦 Backing up old widget files..."

if [ -f "android/app/src/main/java/com/dosevia/app/DoseviaSmallWidget.kt" ]; then
    cp "android/app/src/main/java/com/dosevia/app/DoseviaSmallWidget.kt" "$BACKUP_DIR/"
    echo "  ✓ Backed up DoseviaSmallWidget.kt"
fi

if [ -f "android/app/src/main/java/com/dosevia/app/DoseviaMediumWidget.kt" ]; then
    cp "android/app/src/main/java/com/dosevia/app/DoseviaMediumWidget.kt" "$BACKUP_DIR/"
    echo "  ✓ Backed up DoseviaMediumWidget.kt"
fi

if [ -f "android/app/src/main/java/com/dosevia/app/DoseviaLargeWidget.kt" ]; then
    cp "android/app/src/main/java/com/dosevia/app/DoseviaLargeWidget.kt" "$BACKUP_DIR/"
    echo "  ✓ Backed up DoseviaLargeWidget.kt"
fi

if [ -f "android/app/src/main/java/com/dosevia/app/WidgetSyncPlugin.kt" ]; then
    cp "android/app/src/main/java/com/dosevia/app/WidgetSyncPlugin.kt" "$BACKUP_DIR/"
    echo "  ✓ Backed up WidgetSyncPlugin.kt"
fi

if [ -f "src/app/lib/widgetSync.ts" ]; then
    cp "src/app/lib/widgetSync.ts" "$BACKUP_DIR/"
    echo "  ✓ Backed up widgetSync.ts"
fi

if [ -f "android/app/src/main/AndroidManifest.xml" ]; then
    cp "android/app/src/main/AndroidManifest.xml" "$BACKUP_DIR/"
    echo "  ✓ Backed up AndroidManifest.xml"
fi

echo ""
echo "✅ Backup complete!"
echo ""

# Delete old widget files
echo "🗑️  Removing old widget files..."

rm -f "android/app/src/main/java/com/dosevia/app/DoseviaSmallWidget.kt"
rm -f "android/app/src/main/java/com/dosevia/app/DoseviaMediumWidget.kt"
rm -f "android/app/src/main/java/com/dosevia/app/DoseviaLargeWidget.kt"
echo "  ✓ Deleted old widget providers"

rm -f "android/app/src/main/res/layout/widget_small.xml"
rm -f "android/app/src/main/res/layout/widget_medium.xml"
rm -f "android/app/src/main/res/layout/widget_large.xml"
echo "  ✓ Deleted old widget layouts"

rm -f "android/app/src/main/res/xml/widget_info_small.xml"
rm -f "android/app/src/main/res/xml/widget_info_medium.xml"
rm -f "android/app/src/main/res/xml/widget_info_large.xml"
echo "  ✓ Deleted old widget info files"

rm -f "android/app/src/main/res/drawable/widget_small_background.xml"
rm -f "android/app/src/main/res/drawable/widget_medium_background.xml"
rm -f "android/app/src/main/res/drawable/widget_large_background.xml"
echo "  ✓ Deleted old widget backgrounds"

echo ""
echo "✅ Old files removed!"
echo ""

# Install new files
echo "📥 Installing new widget files..."

if [ -f "android/app/src/main/java/com/dosevia/app/WidgetSyncPlugin_NEW.kt" ]; then
    mv "android/app/src/main/java/com/dosevia/app/WidgetSyncPlugin_NEW.kt" \
       "android/app/src/main/java/com/dosevia/app/WidgetSyncPlugin.kt"
    echo "  ✓ Installed new WidgetSyncPlugin.kt"
fi

if [ -f "src/app/lib/widgetSync_NEW.ts" ]; then
    mv "src/app/lib/widgetSync_NEW.ts" "src/app/lib/widgetSync.ts"
    echo "  ✓ Installed new widgetSync.ts"
fi

if [ -f "android/app/src/main/AndroidManifest_NEW.xml" ]; then
    mv "android/app/src/main/AndroidManifest_NEW.xml" \
       "android/app/src/main/AndroidManifest.xml"
    echo "  ✓ Installed new AndroidManifest.xml"
fi

echo ""
echo "✅ New files installed!"
echo ""

# Check for widget description in strings.xml
echo "🔍 Checking strings.xml..."

STRINGS_FILE="android/app/src/main/res/values/strings.xml"
if grep -q "widget_description" "$STRINGS_FILE"; then
    echo "  ✓ Widget description already exists"
else
    echo "  ⚠️  Need to add widget_description to strings.xml"
    echo ""
    echo "Add this line to $STRINGS_FILE:"
    echo '    <string name="widget_description">Shows pills taken today</string>'
fi

echo ""

# Summary
echo "✅ Migration Complete!"
echo ""
echo "📋 Summary:"
echo "  ✓ Old widget files backed up to: $BACKUP_DIR"
echo "  ✓ Old widget files removed"
echo "  ✓ New widget files installed"
echo ""
echo "📝 Next Steps:"
echo "  1. Review WIDGET_IMPLEMENTATION_GUIDE.md"
echo "  2. Add widget_description to strings.xml (if needed)"
echo "  3. Update your app code to call updateWidgetPillCount()"
echo "  4. Run: npx cap sync android"
echo "  5. Build and test!"
echo ""
echo "🎉 Done!"
