import { ArrowLeft, Calendar, CheckCircle2, XCircle, AlertCircle } from 'lucide-react';
import { DayData } from '@/types/pill-types';
import { format } from 'date-fns';

interface HistoryScreenProps {
  days: DayData[];
  onBack: () => void;
}

export function HistoryScreen({ days, onBack }: HistoryScreenProps) {
  const takenCount = days.filter(d => d.status === 'taken').length;
  const missedCount = days.filter(d => d.status === 'missed').length;
  const notTakenCount = days.filter(d => d.status === 'not_taken').length;
  const adherenceRate = ((takenCount / days.length) * 100).toFixed(1);

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
          <h1 className="text-xl font-semibold text-white">History</h1>
        </div>
      </div>

      {/* Content */}
      <div className="max-w-2xl mx-auto px-4 py-6 space-y-6 pb-20">
        {/* Summary Cards */}
        <div className="grid grid-cols-2 gap-4">
          <div className="bg-white rounded-xl p-4 shadow-sm">
            <div className="flex items-center gap-2 mb-2">
              <CheckCircle2 className="w-5 h-5 text-green-600" />
              <p className="text-sm font-medium text-gray-600">Taken</p>
            </div>
            <p className="text-3xl font-bold text-gray-900">{takenCount}</p>
          </div>

          <div className="bg-white rounded-xl p-4 shadow-sm">
            <div className="flex items-center gap-2 mb-2">
              <AlertCircle className="w-5 h-5" style={{ color: '#fab86d' }} />
              <p className="text-sm font-medium text-gray-600">Missed</p>
            </div>
            <p className="text-3xl font-bold text-gray-900">{missedCount}</p>
          </div>

          <div className="bg-white rounded-xl p-4 shadow-sm col-span-2">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-sm font-medium text-gray-600 mb-1">Adherence Rate</p>
                <p className="text-3xl font-bold" style={{ color: '#f609bc' }}>{adherenceRate}%</p>
              </div>
              <Calendar className="w-12 h-12" style={{ color: 'rgba(246, 9, 188, 0.2)' }} />
            </div>
          </div>
        </div>

        {/* Daily Log */}
        <section>
          <h2 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-3 px-1">
            Daily Log
          </h2>
          <div className="bg-white rounded-xl shadow-sm overflow-hidden divide-y divide-gray-100">
            {days.map((day) => (
              <div key={day.day} className="flex items-center gap-3 px-4 py-3">
                <div className="flex-shrink-0">
                  {day.status === 'taken' && (
                    <CheckCircle2 className="w-6 h-6 text-green-600" />
                  )}
                  {day.status === 'missed' && (
                    <AlertCircle className="w-6 h-6 text-amber-600" />
                  )}
                  {day.status === 'not_taken' && (
                    <XCircle className="w-6 h-6 text-gray-300" />
                  )}
                </div>
                <div className="flex-1">
                  <p className="font-medium text-gray-900">
                    Day {day.day} {day.isPlacebo && <span className="text-sm text-gray-500">(Placebo)</span>}
                  </p>
                  <p className="text-sm text-gray-500">
                    {format(day.date, 'EEEE, MMMM d, yyyy')}
                  </p>
                </div>
                <div className="text-right">
                  <span
                    className={`text-xs font-medium px-2 py-1 rounded-full ${
                      day.status === 'taken'
                        ? 'bg-green-100 text-green-700'
                        : day.status === 'missed'
                        ? 'bg-amber-100 text-amber-700'
                        : 'bg-gray-100 text-gray-500'
                    }`}
                  >
                    {day.status === 'taken' && 'Taken'}
                    {day.status === 'missed' && 'Missed'}
                    {day.status === 'not_taken' && 'Pending'}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </section>
      </div>
    </div>
  );
}