# Privacy Policy for Canta

## Introduction
This Privacy Policy describes how Canta ("we", "our", or "app") handles information.
Canta is a FOSS (Free and Open Source Software) debloater app powered by Shizuku that allows users to uninstall applications without root access.

## Information Collection and Use
Canta does not collect any personal information, and it has no accounts or tracking.
By default, the only thing Canta sends over the network is a request to download the package list (see below).
There is one **optional**, opt-in feature that sends anonymous package statistics; it is off unless you turn it on.

### Network connections
Canta connects to GitHub (`raw.githubusercontent.com` and `api.github.com`) to download and update the [Universal Debloater Alliance](https://github.com/Universal-Debloater-Alliance/universal-android-preinstalled-lists) (UAD) package list (app descriptions and badges).
These requests are ordinary requests to GitHub and are subject to [GitHub's privacy statement](https://docs.github.com/en/site-policy/privacy-policies/github-general-privacy-statement).
You can turn auto-updating off in Settings.

### Package analytics (optional, opt-in)
Package analytics is **off** unless you choose "Contribute" during onboarding or enable it in Settings.
When enabled, Canta sends the following to a [Supabase](https://supabase.com) project hosted in the EU (Frankfurt, AWS eu-central-1):

- For **system packages only**: the package name, the event (*uninstalled*, *survived a reboot*, *reinstalled*, or *voided* when an uninstalled package came back without you reinstalling it in Canta, for example through a system update, so the uninstall says nothing about the reboot), the ISO week of the uninstall, and a coarse batch-size bucket (1, 2-10 or 11+ packages uninstalled together).
- The platform Canta runs on (android, desktop or web).
- A coarse **device description**, sent once per request: the phone brand (mapped by the server to a fixed list of major manufacturers, or "other"), the ROM family and its major version (for example HyperOS 1, One UI 6, or "stock"), and the Android API level (for example 34). Example: `{"brand":"xiaomi","rom":"hyperos","romVersion":1,"sdk":34}`. For uninstalls that are reported after a reboot, the description recorded at uninstall time is used.

Canta does **not** send device IDs, install IDs, account information, your user-installed apps, your device model, build number, fingerprint, serial number, security patch level or your locale.

Events are sent when you uninstall or reinstall a package, when Canta starts after a reboot, and, only if analytics is on, once in the background after the phone has booted (Canta asks Android to tell it when the boot completed, for the "RECEIVE_BOOT_COMPLETED" permission), to record that a package survived the reboot or came back. For this, Canta compares the build fingerprint of your phone with the one stored at uninstall time, only on your device, to tell a system update from other causes; the fingerprint itself is never sent. Likewise, on a computer a hash of the connected phone's serial number is stored locally only, so that uninstalls are only checked against the phone they happened on; it is never sent. With analytics off, that background check does nothing.

What the server stores:

- The scores are built from aggregate counters per (package, event, week, batch bucket, device context). These counters contain no device IDs, install IDs or account information.
- To prevent spam, the server computes a keyed hash of your IP address (IPv6 addresses are first grouped by /48) using a salt that is rotated and deleted daily. The hash is used for a daily request limit and to accept only one report per event type and package per day from the same IP (a second keyed hash of the IP group, event type and package). These hashes and the daily counters that go with them are deleted daily together with the salt. The raw IP address is not stored in Canta's database. Because the salt is kept in the same database until it is deleted, someone with access to the database could, for up to 24 hours, check whether a known IP address reported a known package. Once the day's salt is deleted, that is no longer possible.
- For abuse detection, per-package daily report totals (no IP address, no device data) are kept for 60 days.
- Supabase's own infrastructure may log request metadata (including the IP address) for a short period for operational purposes. See [Supabase's privacy policy](https://supabase.com/privacy).

Requests also include a small proof-of-work computation (anti-spam) that is done on your device.

Aggregated community scores (per package: a verdict, the number of reports rounded down to hundreds, and the share of the most common ROM; survival and reinstall rates are not published) are public and shown in the app to contributors. Only packages with at least 30 reports are published. Scores are also computed per ROM version (for example HyperOS 1) and published only when that ROM version has at least 30 reports for the package; the app shows the one for your own ROM next to the overall one.

You can disable package analytics at any time in Settings. Disabling it also deletes any locally queued data that has not been sent yet.

The server source code, including the exact database logic, is in this repository under [`server/supabase/migrations/`](https://github.com/samolego/Canta/tree/master/server/supabase/migrations).

### Crash reports
If Canta crashes (Android and desktop), it saves a short report on your device: app version, platform, OS version, time and the stack trace.
It is stored locally only and is never sent automatically.
The next time you open Canta you can copy it, open a prefilled GitHub issue with it (only if you choose to), or dismiss it, which deletes it.

## App Permissions
Canta requires the following permissions:

- **Shizuku API access**: Required to perform app uninstallation functions without root access. The Shizuku API allows Canta to execute commands with elevated privileges.
- **Package visibility**: Required to see and interact with other installed applications on your device.

These permissions are used solely for the core functionality of the app and not for data collection.

## Third-Party Libraries & Data Sources
Canta uses the following third-party libraries:

- **Shizuku**: An API that enables apps to perform operations that typically require elevated permissions. We do not share any data with Shizuku developers. For more information about Shizuku's privacy practices, please visit [https://shizuku.rikka.app/](https://shizuku.rikka.app/).

Canta also uses **Universal Android Debloater**'s information about apps (app descriptions and badges), downloaded from GitHub as described above.

The optional package analytics are processed by **Supabase** (EU region), as described above.

## Data Storage
Canta stores information on your device, e.g. app descriptions, versions, your settings, presets, queued analytics events (if enabled) and the last crash report.
This local data is **not** accessible to us or any third parties and is not sent anywhere, except for the analytics data described above if you opted in.

## Changes to This Privacy Policy
If needed, we may update our Privacy Policy from time to time.
Thus, you are advised to review this page periodically for any changes.
We will notify you of any changes by posting the new Privacy Policy on this page.

## Contact Us
If you have any questions or suggestions about this Privacy Policy, do not hesitate to contact us through our GitHub repository at [https://github.com/samolego/Canta](https://github.com/samolego/Canta).
