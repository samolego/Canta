---
title: Canta - Presets
description: Learn how to use presets to manage app uninstall lists across devices.
---

# Presets

Presets are a powerful feature in Canta that allows you to create, manage, and share collections of apps for uninstallation. This feature is particularly useful for applying the same set of app removals across multiple devices or sharing your bloatware removal configurations with others.

## What are presets?

A preset is a saved collection of apps that you want to uninstall. Each preset contains:

- **Name**: A descriptive name for your preset (e.g., "Samsung Bloatware")
- **Description**: Optional details about what the preset removes
- **App List**: The specific apps (package names) to be uninstalled
- **Folders** (optional): Filesystem paths to delete when the preset runs
- **Self-removal** (optional): Whether the preset uninstalls Canta itself at the end
- **Creation Date**: When the preset was created
- **Version**: Format version for compatibility

## Deleting folders

Presets can also delete folders on the device when they run — useful for
wiping leftover data after debloating. In the preset editor, type one
absolute path per line or use **Pick a folder…** to select one in the system
file picker. For safety, only paths inside user storage are allowed:
`/sdcard`, `/storage`, `/data/media` and `/data/local/tmp`. Paths outside
these roots (e.g. `/system`, `/data/data`, `/data/system`) are refused, both
in the editor and when importing a preset.

On Android, deleting shared-storage folders needs the **"All files access"**
permission (`MANAGE_EXTERNAL_STORAGE`). Canta asks for it the first time you
try to add a folder to a preset (focusing the path field or tapping "Pick a
folder…"), not at app startup. If it isn't granted, folder deletion in a run
fails with a clear message (uninstalls still work — they use Shizuku, not the
storage permission). On API 28/29 the classic storage permission is used
instead.

## Removing Canta after a run

Enabling "Uninstall Canta itself after the preset runs" makes a run remove
Canta as its very last action — after the apps are uninstalled and the
folders are deleted. Canta is never removed through the preset's plain app
list: if a preset contains Canta's own package it is skipped unless this
option is enabled.

## Shortcuts and headless runs

From a preset's menu, **Run now** executes it immediately (with a
confirmation dialog; if "require authentication" is enabled in settings, the
system credential prompt appears first). **Add shortcut** pins a home-screen
shortcut that runs the preset without opening Canta's interface. The shortcut
first shows a slide-to-confirm prompt so a stray tap can't trigger a
destructive run; the Shizuku and authentication checks then apply as usual.

Both the shortcut and the in-app run honor the authentication setting, and
all deletions go through the same safety checks as a normal preset run.

## Creating presets

### From Uninstalled Apps

The easiest way to create a preset is from apps you've already uninstalled:

1. **Uninstall the apps** you want to include in your preset
2. Navigate to the **Presets** tab in Canta
3. Tap the **Create Preset** button
4. Enter a **name** for your preset (required)
5. Add an optional **description** explaining what the preset removes
6. Tap **Save**

::: tip
The preset will automatically capture all currently uninstalled apps on your device. Make sure you've uninstalled exactly the apps you want before creating the preset.
:::

## Managing Presets

### Viewing Your Presets

When you navigate to the Presets tab, you'll see a comprehensive list of all your saved presets. Each preset entry displays the preset name and description, the creation date showing when it was originally made, the number of apps included in that preset, and an action menu that provides various management options for each individual preset.

### Applying Presets

To use a preset on your current device:

1. Find the preset you want to apply
2. Tap the **Apply Preset** option
3. Canta will select all available apps from the preset
4. Review the selected apps in the main app list
5. Uninstall the selected apps as usual

::: warning Important
When applying presets, only apps that exist on your current device will be selected. Apps that aren't installed or don't exist on your device will be skipped.
:::

## Import & Export


Sharing and importing presets is simple and flexible in Canta. To **export** a preset, find it in your presets list and tap the **Share** option to copy it as JSON to your clipboard, which you can then paste and share with others. To **import** presets, tap **Import Preset** and choose either the **Clipboard** tab to import directly from copied JSON data, or the **Text** tab to manually paste preset data into the text field before tapping **Import**.

### JSON Format

Presets are exported in JSON format for easy sharing. Here's what a preset JSON looks like:

```json
{
  "name": "Samsung Bloatware",
  "description": "Removes Samsung's duplicate apps and unnecessary services",
  "createdDate": 1699123456789,
  "version": "1.0",
  "apps": [
    "com.samsung.android.bixby.agent",
    "com.samsung.android.app.spage"
  ]
}
```

---

Presets make Canta even more powerful by allowing you to systematically manage app removal across devices. Whether you're setting up multiple devices, sharing your expertise, or simply want to backup your uninstall preferences, presets provide a robust solution for that case.
