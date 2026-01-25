import { ArrowLeft, TrendingUp, Calendar, Target } from 'lucide-react';
import { DayData } from '@/types/pill-types';

interface StatsScreenProps {
  days: DayData[];
  startDate: Date;
  onBack: () => void;
}

export function StatsScreen({ days, startDate, onBack }: StatsScreenProps) {
  const takenCount = days.filter(d => d.status === 'taken').length;
  const missedCount = days.filter(d => d.status === 'missed').length;
  const adherenceRate = ((takenCount / days.length) * 100).toFixed(1);
  
  const daysSinceStart = Math.floor(
    (new Date().getTime() - startDate.getTime()) / (1000 * 60 * 60 * 24)
  );
  const cyclesCompleted = Math.floor(daysSinceStart / 28);

  // Calculate weekly adherence (last 7 days)
  const last7Days = days.slice(-7);
  const weeklyTaken = last7Days.filter(d => d.status === 'taken').length;
  const weeklyAdherence = ((weeklyTaken / 7) * 100).toFixed(1);

  return (
    <div className="min-h-screen bg-gradient-to-br from-pink-50 via-orange-50 to-yellow-50">
      {/* Header */}
      <div 
        className="border-b sticky top-0 z-10"
        style={{
          background: 'linear-gradient(135deg, #f609bc, #fab86d)',
          borderColor: 'rgba(255, 255, 255, 0.3)',
        }}
      >
        <div className="max-w-2xl mx-auto px-4 py-4 flex items-center">
          <button
            onClick={onBack}
            className="mr-3 p-2 -ml-2 hover:bg-white/20 rounded-full transition-colors"
          >
            <ArrowLeft className="w-5 h-5 text-white" />
          </button>
          <h1 className="text-xl font-semibold text-white">Statistics</h1>
        </div>
      </div>

      {/* Content */}
      <div className="max-w-2xl mx-auto px-4 py-6 space-y-6 pb-20">
        {/* Overall Adherence */}
        <div 
          className="rounded-2xl p-6 text-white shadow-lg"
          style={{
            background: 'linear-gradient(135deg, #f609bc, #fab86d)',
          }}
        >
          <div className="flex items-center gap-2 mb-3">
            <Target className="w-6 h-6" />
            <p className="text-sm font-medium opacity-90">Overall Adherence</p>
          </div>
          <div className="flex items-baseline gap-2">
            <p className="text-5xl font-bold">{adherenceRate}</p>
            <p className="text-2xl opacity-90">%</p>
          </div>
          <p className="text-sm opacity-80 mt-2">
            {takenCount} of {days.length} pills taken
          </p>
        </div>

        {/* Stats Grid */}
        <div className="grid grid-cols-2 gap-4">
          <div className="bg-white rounded-xl p-4 shadow-sm">
            <div className="flex items-center gap-2 mb-2">
              <TrendingUp className="w-5 h-5 text-green-600" />
              <p className="text-sm font-medium text-gray-600">This Week</p>
            </div>
            <p className="text-3xl font-bold text-gray-900">{weeklyAdherence}%</p>
            <p className="text-xs text-gray-500 mt-1">{weeklyTaken}/7 days</p>
          </div>

          <div className="bg-white rounded-xl p-4 shadow-sm">
            <div className="flex items-center gap-2 mb-2">
              <Calendar className="w-5 h-5" style={{ color: '#f609bc' }} />
              <p className="text-sm font-medium text-gray-600">Cycles</p>
            </div>
            <p className="text-3xl font-bold text-gray-900">{cyclesCompleted}</p>
            <p className="text-xs text-gray-500 mt-1">Completed</p>
          </div>
        </div>

        {/* Breakdown */}
        <section>
          <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3 px-1">
            Current Cycle Breakdown
          </h2>
          <div className="bg-white rounded-xl shadow-sm p-6 space-y-4">
            {/* Taken */}
            <div>
              <div className="flex justify-between items-center mb-2">
                <span className="text-sm font-medium text-gray-700">Taken</span>
                <span className="text-sm font-semibold text-gray-900">{takenCount} pills</span>
              </div>
              <div className="w-full h-2 bg-gray-200 rounded-full overflow-hidden">
                <div
                  className="h-full bg-green-500 rounded-full"
                  style={{ width: `${(takenCount / days.length) * 100}%` }}
                />
              </div>
            </div>

            {/* Missed */}
            <div>
              <div className="flex justify-between items-center mb-2">
                <span className="text-sm font-medium text-gray-700">Missed</span>
                <span className="text-sm font-semibold text-gray-900">{missedCount} pills</span>
              </div>
              <div className="w-full h-2 bg-gray-200 rounded-full overflow-hidden">
                <div
                  className="h-full rounded-full"
                  style={{ 
                    width: `${(missedCount / days.length) * 100}%`,
                    background: 'linear-gradient(90deg, #fab86d, #f59e0b)',
                  }}
                />
              </div>
            </div>

            {/* Not Taken */}
            <div>
              <div className="flex justify-between items-center mb-2">
                <span className="text-sm font-medium text-gray-700">Remaining</span>
                <span className="text-sm font-semibold text-gray-900">
                  {days.filter(d => d.status === 'not_taken').length} pills
                </span>
              </div>
              <div className="w-full h-2 bg-gray-200 rounded-full overflow-hidden">
                <div
                  className="h-full bg-gray-400 rounded-full"
                  style={{
                    width: `${(days.filter(d => d.status === 'not_taken').length / days.length) * 100}%`,
                  }}
                />
              </div>
            </div>
          </div>
        </section>

        {/* Insights */}
        <section>
          <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3 px-1">
            Insights
          </h2>
          <div className="bg-white rounded-xl shadow-sm p-5 space-y-3">
            <div className="flex gap-3">
              <div 
                className="flex-shrink-0 w-10 h-10 rounded-full flex items-center justify-center"
                style={{
                  background: 'rgba(246, 9, 188, 0.1)',
                }}
              >
                <Calendar className="w-5 h-5" style={{ color: '#f609bc' }} />
              </div>
              <div>
                <p className="font-medium text-gray-900">Consistency</p>
                <p className="text-sm text-gray-600">
                  {weeklyAdherence === '100.0'
                    ? 'Perfect adherence this week! Keep it up!'
                    : weeklyTaken >= 5
                    ? 'Great job maintaining your routine!'
                    : 'Try to improve consistency for better protection.'}
                </p>
              </div>
            </div>

            {missedCount > 0 && (
              <div className="flex gap-3">
                <div 
                  className="flex-shrink-0 w-10 h-10 rounded-full flex items-center justify-center"
                  style={{
                    background: 'rgba(250, 184, 109, 0.2)',
                  }}
                >
                  <Target className="w-5 h-5" style={{ color: '#fab86d' }} />
                </div>
                <div>
                  <p className="font-medium text-gray-900">Missed Pills</p>
                  <p className="text-sm text-gray-600">
                    You've missed {missedCount} pill{missedCount > 1 ? 's' : ''} this cycle. Consult your
                    healthcare provider if you miss multiple pills.
                  </p>
                </div>
              </div>
            )}
          </div>
        </section>
      </div>
    </div>
  );
}