import React from 'react';
import { X, Smartphone, BarChart3, Calendar } from 'lucide-react';
import { Card } from './ui/card';
import { Button } from './ui/button';

interface WidgetsScreenProps {
  onClose: () => void;
}

export function WidgetsScreen({ onClose }: WidgetsScreenProps) {
  const widgets = [
    {
      id: 'small',
      name: 'Compact Widget',
      description: 'Shows pills taken today (1/28)',
      size: '2x1 or 2x2',
      icon: Smartphone,
      color: 'bg-gradient-to-br from-pink-500 to-pink-600',
      features: [
        'Quick status view',
        'Checkmark when complete',
        'Minimal space usage',
      ],
    },
    {
      id: 'medium',
      name: 'Statistics Widget',
      description: 'Detailed pill statistics',
      size: '4x2',
      icon: BarChart3,
      color: 'bg-white border-2 border-gray-200',
      features: [
        'Total pills count',
        'Pills taken',
        'Pills missed',
      ],
    },
    {
      id: 'large',
      name: 'Calendar Widget',
      description: 'Full calendar view of pill pack',
      size: '4x4',
      icon: Calendar,
      color: 'bg-gradient-to-br from-pink-500 to-pink-600',
      features: [
        'Calendar layout',
        'Smart date alignment',
        'Color-coded status',
        'Pulsing missed pills',
      ],
    },
  ];

  const handleAddWidget = (widgetId: string) => {
    // Show instructions for adding widget
    alert(
      `To add the ${widgetId} widget:\n\n` +
      '1. Long press on your home screen\n' +
      '2. Tap "Widgets"\n' +
      '3. Find "Dosevia"\n' +
      '4. Drag the widget you want to your home screen\n\n' +
      'All widgets will sync automatically!'
    );
  };

  return (
    <div className="fixed inset-0 bg-white z-50 overflow-y-auto">
      {/* Header */}
      <div className="sticky top-0 bg-white border-b z-10">
        <div className="max-w-7xl mx-auto px-4 py-4">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">Home Screen Widgets</h1>
              <p className="text-sm text-gray-500 mt-1">
                Add widgets to your home screen for quick access
              </p>
            </div>
            <button
              onClick={onClose}
              className="p-2 rounded-full hover:bg-gray-100 transition-colors"
            >
              <X className="w-6 h-6 text-gray-600" />
            </button>
          </div>
        </div>
      </div>

      {/* Content */}
      <div className="max-w-7xl mx-auto px-4 py-8">
        {/* Instructions */}
        <Card className="p-6 mb-8 bg-blue-50 border-blue-200">
          <h2 className="text-lg font-semibold text-blue-900 mb-2">
            How to Add Widgets
          </h2>
          <ol className="list-decimal list-inside space-y-2 text-blue-800">
            <li>Long press on your Android home screen</li>
            <li>Tap "Widgets" from the menu</li>
            <li>Scroll to find "Dosevia" widgets</li>
            <li>Drag your preferred widget to the home screen</li>
            <li>The widget will automatically sync with your pill data</li>
          </ol>
        </Card>

        {/* Widget Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {widgets.map((widget) => (
            <Card key={widget.id} className="overflow-hidden hover:shadow-lg transition-shadow">
              {/* Widget Preview */}
              <div className={`${widget.color} h-48 flex items-center justify-center p-4`}>
                <widget.icon 
                  className={`w-24 h-24 ${
                    widget.id === 'medium' ? 'text-pink-500' : 'text-white'
                  }`} 
                />
              </div>

              {/* Widget Info */}
              <div className="p-6">
                <div className="flex items-start justify-between mb-2">
                  <h3 className="text-xl font-bold text-gray-900">
                    {widget.name}
                  </h3>
                  <span className="text-xs font-medium text-gray-500 bg-gray-100 px-2 py-1 rounded">
                    {widget.size}
                  </span>
                </div>
                
                <p className="text-gray-600 mb-4">
                  {widget.description}
                </p>

                {/* Features */}
                <div className="mb-4">
                  <p className="text-sm font-semibold text-gray-700 mb-2">Features:</p>
                  <ul className="space-y-1">
                    {widget.features.map((feature, index) => (
                      <li key={index} className="text-sm text-gray-600 flex items-start">
                        <span className="text-pink-500 mr-2">•</span>
                        {feature}
                      </li>
                    ))}
                  </ul>
                </div>

                {/* Add Button */}
                <Button
                  onClick={() => handleAddWidget(widget.id)}
                  className="w-full bg-pink-500 hover:bg-pink-600 text-white"
                >
                  How to Add
                </Button>
              </div>
            </Card>
          ))}
        </div>

        {/* Additional Info */}
        <Card className="p-6 mt-8">
          <h2 className="text-lg font-semibold text-gray-900 mb-4">
            Widget Features
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm text-gray-600">
            <div>
              <h3 className="font-semibold text-gray-800 mb-2">📊 Real-time Sync</h3>
              <p>Widgets update automatically when you mark pills as taken or missed in the app.</p>
            </div>
            <div>
              <h3 className="font-semibold text-gray-800 mb-2">🎨 Color Coded</h3>
              <p>Pills are color-coded: gray for taken, red for missed, and white for upcoming.</p>
            </div>
            <div>
              <h3 className="font-semibold text-gray-800 mb-2">📅 Calendar Smart</h3>
              <p>The large widget aligns pills to the correct days of the week based on your start date.</p>
            </div>
            <div>
              <h3 className="font-semibold text-gray-800 mb-2">🔄 Always Updated</h3>
              <p>Widgets refresh every 30 minutes and whenever you update your pill status.</p>
            </div>
          </div>
        </Card>
      </div>
    </div>
  );
}
