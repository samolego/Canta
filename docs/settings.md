---
title: Canta - Settings
description: Configure Canta settings for optimal use
---
# Settings

Canta offers several configuration options to customize your experience. This page explains each setting and how it affects app functionality.

## Available Settings

<div class="screenshot-container">
  <img src="/images/phoneScreenshots/screenshot-settings.png" alt="Settings screen" class="phone-screenshot">
  <div class="screenshot-caption">
    Canta settings screen
  </div>
</div>

### Auto-update Bloat List

**Description:** When enabled, Canta automatically checks for and downloads the latest app classification data from the [Universal Debloater Alliance](https://github.com/Universal-Debloater-Alliance/universal-android-preinstalled-lists) repository.

- **Enabled (Default):** Ensures you have the most up-to-date information about apps, including newly identified bloatware and revised safety recommendations.
- **Disabled:** Canta will use only the locally stored bloat list data without checking for updates.

::: tip
Enable this setting to ensure you have the most accurate information about which apps are safe to remove.
:::

### Confirm Before Uninstall

**Description:** Shows a confirmation dialog before uninstalling selected apps.

- **Enabled (Default):** Displays a confirmation dialog showing the number of apps you're about to uninstall, helping prevent accidental removals.
- **Disabled:** Immediately proceeds with uninstallation when you tap the trash icon, without asking for confirmation.

::: warning
We recommend keeping this enabled to avoid accidentally uninstalling important apps.
:::

## Advanced Settings

Canta includes advanced settings for power users who need more control over the app's behavior.

### Allow Unsafe Selections

**Description:** Controls whether you can select apps that are marked with the 🟣 **Unsafe** badge for uninstallation.
You can bypass this restriction by using presets, too.

- **Disabled (Default):** Apps marked as unsafe cannot be selected for uninstallation, protecting you from accidentally removing critical system components.
- **Enabled:** Allows selection of unsafe apps, giving experienced users full control over what they can uninstall.

::: danger IMPORTANT
Enabling this setting allows you to select apps that could break vital system functionality. Only enable this if you understand the risks and know exactly what you're doing. There's a high chance you'll experience bootlooping!
:::

### Bloat List URL

**Description:** Specifies the source URL where Canta downloads app classification data, badges, and descriptions.

- **Default:** Points to the Universal Debloater Alliance repository
- **Custom:** You can specify an alternative source that follows the same data format

This setting allows organizations or advanced users to maintain their own app classification databases.

### Commits URL

**Description:** Defines where Canta checks for updates to the bloat list data.

- **Default:** Points to the commits API of the Universal Debloater Alliance repository
- **Custom:** Can be changed to track updates from alternative sources

This works in conjunction with the "Auto-update Bloat List" setting to determine when new data is available.

::: tip ADVANCED USAGE
The Bloat List URL and Commits URL settings are primarily intended for developers testing custom app databases.
:::

## Hidden Features

### Select All

For advanced users, Canta includes a hidden "Select All" feature that can be enabled by tapping the version number in Settings multiple times. This feature adds a "Select All" option when having "recommended" filter applied.

::: warning CAUTION
Use this feature carefully, as mass uninstallation could affect device functionality.
:::
