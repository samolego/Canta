# Features

## Core Features

### Safe Uninstallation
No permanent device damage risk can be done when uninstalling apps with Canta. The uninstallation process happens for current user
only, so APKs remain on device. In case you uninstall a critical system component, you can risk a bootloop.
In that case, you will need to perform a factory reset and the device will not be bricked.

::: info
Canta calls Android System APIs directly to uninstall apps, similar to how ADB does it.
:::

### System Requirements
* Android 9.0 (SDK 28) or higher
* No root required
* Elevates permissions using [Shizuku](https://shizuku.rikka.app/)

## Cool Features

### App Management
* Filter apps by type (system apps, user apps) or badge
* Search functionality
* Batch selection for multiple uninstalls
* See app info and size details

### App Badges
* 🟢 **Safe to Remove** - Non-essential apps. Still review them, though.
* 🟡 **Advanced** - May affect some functionality.
* 🔴 **Expert** - Can break important functionality.
* 🟣 **Unsafe** - Apps that can break vital system components.

### User Interface
App uses a modern Material Design 3 interface that adapts to your device's theme preferences.
The interface provides detailed information about each app while maintaining simplicity in its controls.
Finding specific apps is effortless thanks to the quick search functionality and comprehensive filtering options.

<div class="screenshot-container">
  <img src="/images/phoneScreenshots/screenshot-main.png" alt="Main screen" class="phone-screenshot">
  <div class="screenshot-caption">
      Canta Home Screen
  </div>
</div>

## Privacy & Security

### Privacy-Focused
Canta can operate without internet connection and collects no data whatsoever.
There's no analytics or tracking built into the app.

::: warning NOTE
Badges information and app descriptions are fetched from GitHub repository if connected to the internet.
No data is uploaded whatsoever.
:::

### Open Source
Canta is licensed under LGPL-3.0, with its complete source code available on [GitHub](https://github.com/samolego/Canta). This transparency ensures you can verify the app's functionality and contribute to its development if you wish.
