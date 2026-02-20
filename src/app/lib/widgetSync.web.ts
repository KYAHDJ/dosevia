export class WidgetSyncWeb {
  async updateWidgets(): Promise<void> {
    console.log('Widget update not supported on web');
  }

  async savePillData(): Promise<void> {
    console.log('Widget data save not supported on web');
  }

  async requestPinWidget(): Promise<void> {
    console.log('Widget pin request not supported on web');
  }
}
