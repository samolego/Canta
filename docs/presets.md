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
- **Creation Date**: When the preset was created
- **Version**: Format version for compatibility

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
    {
      "packageName": "com.samsung.android.bixby.agent"
    },
    {
      "packageName": "com.samsung.android.app.spage"
    }
  ]
}
```

---

Presets make Canta even more powerful by allowing you to systematically manage app removal across devices. Whether you're setting up multiple devices, sharing your expertise, or simply want to backup your uninstall preferences, presets provide a robust solution for that case.
