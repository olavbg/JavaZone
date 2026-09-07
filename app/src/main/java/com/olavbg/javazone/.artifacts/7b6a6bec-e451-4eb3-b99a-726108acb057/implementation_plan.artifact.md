# Implement Reliable Local Notifications

This plan outlines the steps to ensure local notifications for session reminders are reliable and correctly integrated into the JavaZone app. This includes handling lead time changes, boot rescheduling, and ensuring exact alarms are used where possible.

## User Review Required

> [!NOTE]
> The app already has a `ReminderManager` and `SessionReminderReceiver`. The proposed changes focus on wiring them up correctly to handle lead time changes and ensuring they are triggered reliably.

> [!IMPORTANT]
> To use exact alarms on Android 12+, the user might need to grant the "Alarms & Reminders" permission if the system doesn't grant it by default. We will add a check for this.

## Proposed Changes

### Notification Components

#### [MODIFY] [ReminderManager.kt](file:///C:/dev/JavaZone/app/src/main/java/com/olavbg/javazone/notifications/ReminderManager.kt)
- Add a helper to check if exact alarm permission is granted.
- Ensure the `PendingIntent` uses `FLAG_IMMUTABLE` (already there) and consistent request codes.

#### [MODIFY] [SessionReminderReceiver.kt](file:///C:/dev/JavaZone/app/src/main/java/com/olavbg/javazone/notifications/SessionReminderReceiver.kt)
- Ensure the notification channel is created at the right time.
- Improve notification priority and categories for better visibility.

#### [MODIFY] [BootReceiver.kt](file:///C:/dev/JavaZone/app/src/main/java/com/olavbg/javazone/notifications/BootReceiver.kt)
- Update to fetch the user's preferred lead time from `SettingsRepository` when rescheduling alarms after a reboot.

### Data & Repository

#### [MODIFY] [SessionRepository.kt](file:///C:/dev/JavaZone/app/src/main/java/com/olavbg/javazone/data/repository/SessionRepository.kt)
- Add `rescheduleAllFavorites()` method to allow batch scheduling/updating of alarms.
- Call `rescheduleAllFavorites()` after refreshing sessions from the network to account for any time/room changes.

### UI & Settings

#### [MODIFY] [SettingsViewModel.kt](file:///C:/dev/JavaZone/app/src/main/java/com/olavbg/javazone/ui/settings/SettingsViewModel.kt)
- Trigger `rescheduleAllFavorites()` in the repository whenever the lead time setting is updated.

#### [MODIFY] [SettingsScreen.kt](file:///C:/dev/JavaZone/app/src/main/java/com/olavbg/javazone/ui/settings/SettingsScreen.kt)
- Add a check for "Exact Alarm" permission and show a status/warning if it's missing (on Android 12+).

## Verification Plan

### Automated Tests
- Unit tests for `ReminderManager` time calculation logic (if applicable).
- Verification of `SessionRepository` calling `ReminderManager` on favorite toggles.

### Manual Verification
- Favorite a session that starts in 11 minutes (with 10 min lead time) and verify the notification fires in 1 minute.
- Change the lead time in Settings and verify that the scheduled alarm time updates accordingly.
- Reboot the device (simulated via `adb shell am broadcast -a android.intent.action.BOOT_COMPLETED`) and verify alarms are rescheduled.
- Test on Android 13+ to verify the `POST_NOTIFICATIONS` permission prompt works.
