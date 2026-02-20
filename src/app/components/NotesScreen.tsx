import { useState, useMemo } from 'react';
import { Note } from '@/types/pill-types';
import { format, parseISO, startOfDay, isToday, isYesterday, isThisWeek, isThisMonth } from 'date-fns';
import { Plus, Edit2, Trash2, Calendar, Clock, Search, FileText, ArrowLeft } from 'lucide-react';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Textarea } from './ui/textarea';
import { Card } from './ui/card';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from './ui/dialog';
import { ScrollArea } from './ui/scroll-area';
import { Badge } from './ui/badge';

interface NotesScreenProps {
  notes: Note[];
  onAddNote: (note: Omit<Note, 'id' | 'createdAt' | 'updatedAt'>) => void;
  onEditNote: (id: string, content: string) => void;
  onDeleteNote: (id: string) => void;
  onBack: () => void;
}

export function NotesScreen({ notes, onAddNote, onEditNote, onDeleteNote, onBack }: NotesScreenProps) {
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [editingNote, setEditingNote] = useState<Note | null>(null);
  const [noteContent, setNoteContent] = useState('');
  const [noteDate, setNoteDate] = useState(format(new Date(), 'yyyy-MM-dd'));
  const [noteTime, setNoteTime] = useState(format(new Date(), 'HH:mm'));
  const [searchQuery, setSearchQuery] = useState('');

  // Filter and sort notes
  const filteredNotes = useMemo(() => {
    let filtered = notes;
    
    if (searchQuery) {
      filtered = notes.filter(note => 
        note.content.toLowerCase().includes(searchQuery.toLowerCase())
      );
    }
    
    return filtered.sort((a, b) => {
      const dateA = new Date(a.date).getTime();
      const dateB = new Date(b.date).getTime();
      if (dateA !== dateB) return dateB - dateA; // Most recent first
      return b.time.localeCompare(a.time); // Later time first for same date
    });
  }, [notes, searchQuery]);

  // Group notes by time period
  const groupedNotes = useMemo(() => {
    const groups: { [key: string]: Note[] } = {
      today: [],
      yesterday: [],
      thisWeek: [],
      thisMonth: [],
      older: [],
    };

    filteredNotes.forEach(note => {
      const noteDate = startOfDay(new Date(note.date));
      if (isToday(noteDate)) {
        groups.today.push(note);
      } else if (isYesterday(noteDate)) {
        groups.yesterday.push(note);
      } else if (isThisWeek(noteDate, { weekStartsOn: 1 })) {
        groups.thisWeek.push(note);
      } else if (isThisMonth(noteDate)) {
        groups.thisMonth.push(note);
      } else {
        groups.older.push(note);
      }
    });

    return groups;
  }, [filteredNotes]);

  const handleOpenDialog = (note?: Note) => {
    if (note) {
      setEditingNote(note);
      setNoteContent(note.content);
      setNoteDate(format(new Date(note.date), 'yyyy-MM-dd'));
      setNoteTime(note.time);
    } else {
      setEditingNote(null);
      setNoteContent('');
      setNoteDate(format(new Date(), 'yyyy-MM-dd'));
      setNoteTime(format(new Date(), 'HH:mm'));
    }
    setIsDialogOpen(true);
  };

  const handleSaveNote = () => {
    if (!noteContent.trim()) return;

    if (editingNote) {
      onEditNote(editingNote.id, noteContent);
    } else {
      onAddNote({
        date: new Date(noteDate),
        time: noteTime,
        content: noteContent,
      });
    }

    setIsDialogOpen(false);
    setNoteContent('');
    setEditingNote(null);
  };

  const getRelativeDateLabel = (date: Date) => {
    const noteDate = startOfDay(date);
    if (isToday(noteDate)) return 'Today';
    if (isYesterday(noteDate)) return 'Yesterday';
    if (isThisWeek(noteDate, { weekStartsOn: 1 })) return format(date, 'EEEE');
    return format(date, 'MMM dd, yyyy');
  };

  const renderNoteGroup = (title: string, notes: Note[]) => {
    if (notes.length === 0) return null;

    return (
      <div key={title} className="mb-6">
        <h3 className="text-sm font-semibold text-gray-500 mb-3 px-4">{title}</h3>
        <div className="space-y-2 px-4">
          {notes.map(note => (
            <Card key={note.id} className="p-4 hover:shadow-md transition-shadow">
              <div className="flex items-start justify-between gap-3">
                <div className="flex-1">
                  <div className="flex items-center gap-2 mb-2">
                    <Badge variant="outline" className="text-xs">
                      <Calendar className="w-3 h-3 mr-1" />
                      {getRelativeDateLabel(new Date(note.date))}
                    </Badge>
                    <Badge variant="outline" className="text-xs">
                      <Clock className="w-3 h-3 mr-1" />
                      {note.time}
                    </Badge>
                  </div>
                  <p className="text-sm text-gray-700 whitespace-pre-wrap">{note.content}</p>
                  <p className="text-xs text-gray-400 mt-2">
                    {format(new Date(note.updatedAt), 'MMM dd, yyyy \'at\' h:mm a')}
                  </p>
                </div>
                <div className="flex gap-1">
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => handleOpenDialog(note)}
                    className="h-8 w-8 p-0"
                  >
                    <Edit2 className="w-4 h-4" />
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => {
                      if (confirm('Delete this note?')) {
                        onDeleteNote(note.id);
                      }
                    }}
                    className="h-8 w-8 p-0 text-red-500 hover:text-red-700"
                  >
                    <Trash2 className="w-4 h-4" />
                  </Button>
                </div>
              </div>
            </Card>
          ))}
        </div>
      </div>
    );
  };

  return (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="p-4 border-b">
        <div className="flex items-center gap-3 mb-4">
          <button
            onClick={onBack}
            className="p-2 hover:bg-gray-100 rounded-full transition-colors"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div className="flex-1">
            <h2 className="text-xl font-bold">Notes</h2>
            <p className="text-sm text-gray-500">
              {notes.length} {notes.length === 1 ? 'note' : 'notes'}
            </p>
          </div>
          <Button
            onClick={() => handleOpenDialog()}
            size="sm"
            className="bg-gradient-to-r from-pink-500 to-orange-400 hover:from-pink-600 hover:to-orange-500"
          >
            <Plus className="w-4 h-4 mr-2" />
            Add
          </Button>
        </div>

        {/* Search */}
        <div className="relative">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <Input
            type="text"
            placeholder="Search notes..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="pl-10"
          />
        </div>
      </div>

      {/* Notes List */}
      <ScrollArea className="flex-1">
        {filteredNotes.length === 0 ? (
          <div className="flex flex-col items-center justify-center h-full p-8 text-center">
            <FileText className="w-16 h-16 text-gray-300 mb-4" />
            <h3 className="text-lg font-semibold text-gray-700 mb-2">
              {searchQuery ? 'No notes found' : 'No notes yet'}
            </h3>
            <p className="text-sm text-gray-500 mb-4">
              {searchQuery 
                ? 'Try a different search term' 
                : 'Add notes to track your pill-taking experience'}
            </p>
            {!searchQuery && (
              <Button
                onClick={() => handleOpenDialog()}
                className="bg-gradient-to-r from-pink-500 to-orange-400 hover:from-pink-600 hover:to-orange-500"
              >
                <Plus className="w-4 h-4 mr-2" />
                Add Your First Note
              </Button>
            )}
          </div>
        ) : (
          <div className="py-4">
            {renderNoteGroup('Today', groupedNotes.today)}
            {renderNoteGroup('Yesterday', groupedNotes.yesterday)}
            {renderNoteGroup('This Week', groupedNotes.thisWeek)}
            {renderNoteGroup('This Month', groupedNotes.thisMonth)}
            {renderNoteGroup('Older', groupedNotes.older)}
          </div>
        )}
      </ScrollArea>

      {/* Add/Edit Note Dialog */}
      <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>
              {editingNote ? 'Edit Note' : 'Add Note'}
            </DialogTitle>
            <DialogDescription>
              {editingNote 
                ? 'Update your note details' 
                : 'Add a note about your pill-taking experience'}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="text-sm font-medium mb-2 block">Date</label>
                <Input
                  type="date"
                  value={noteDate}
                  onChange={(e) => setNoteDate(e.target.value)}
                  disabled={!!editingNote}
                />
              </div>
              <div>
                <label className="text-sm font-medium mb-2 block">Time</label>
                <Input
                  type="time"
                  value={noteTime}
                  onChange={(e) => setNoteTime(e.target.value)}
                  disabled={!!editingNote}
                />
              </div>
            </div>
            <div>
              <label className="text-sm font-medium mb-2 block">Note</label>
              <Textarea
                value={noteContent}
                onChange={(e) => setNoteContent(e.target.value)}
                placeholder="How are you feeling? Any side effects?"
                rows={4}
                className="resize-none"
              />
            </div>
          </div>
          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setIsDialogOpen(false)}
            >
              Cancel
            </Button>
            <Button
              onClick={handleSaveNote}
              disabled={!noteContent.trim()}
              className="bg-gradient-to-r from-pink-500 to-orange-400 hover:from-pink-600 hover:to-orange-500"
            >
              {editingNote ? 'Update' : 'Add'} Note
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
