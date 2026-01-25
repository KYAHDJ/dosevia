import { LocalNotifications } from '@capacitor/local-notifications';

export async function scheduleDailyReminder(time: string) {
  // time format: "9:00 PM" or "21:00"
  const [hour, minute] = parseTime(time);

  await LocalNotifications.cancel({ notifications: [{ id: 1 }] });

  await LocalNotifications.schedule({
    notifications: [
      {
        id: 1,
        title: 'Time to take your pill 💊',
        body: 'Don’t forget your daily dose',
        schedule: {
          repeats: true,
          on: {
            hour,
            minute,
          },
        },
      },
    ],
  });
}

export async function cancelDailyReminder() {
  await LocalNotifications.cancel({ notifications: [{ id: 1 }] });
}

function parseTime(time: string): [number, number] {
  // supports "9:00 PM" and "21:00"
  if (time.includes('AM') || time.includes('PM')) {
    const [t, modifier] = time.split(' ');
    let [hours, minutes] = t.split(':').map(Number);
    if (modifier === 'PM' && hours < 12) hours += 12;
    if (modifier === 'AM' && hours === 12) hours = 0;
    return [hours, minutes];
  }

  const [hours, minutes] = time.split(':').map(Number);
  return [hours, minutes];
}
