import { Preferences } from '@capacitor/preferences';

export async function saveData<T>(key: string, value: T) {
  await Preferences.set({
    key,
    value: JSON.stringify(value),
  });
}

export async function loadData<T>(key: string): Promise<T | null> {
  const { value } = await Preferences.get({ key });
  return value ? JSON.parse(value) : null;
}
