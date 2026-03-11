# Cross-Platform Sync: Android + Chrome Extension

This document explains how the Arete Android app and Chrome extension sync data seamlessly through Firebase Firestore.

## Architecture Overview

Both platforms use the **same Firestore structure** to ensure data consistency:

```
Firestore Database
└── users/{userId}/
    ├── data/
    │   ├── profile        → User profile info
    │   └── preferences    → Settings and preferences
    └── apps/
        └── {appId}        → Per-app usage tracking
            ├── date: "2026-01-22"
            ├── secondsToday: 3600
            └── lastUpdated: <timestamp>
```

## How Data Flows

### Android App Flow

1. **UsageStatsRepository** → Reads local Android usage stats via UsageStatsManager
2. **UsageSyncHelper** → Batches and syncs to Firestore
3. **FirestoreRepository** → Performs incremental updates using `FieldValue.increment()`

```kotlin
// Android: Increment app usage
firestoreRepo.incrementAppUsage(
    userId = "user123",
    appId = "com.instagram.android",
    deltaSeconds = 300,  // 5 minutes
    date = "2026-01-22"
)
```

### Chrome Extension Flow

1. **content.js** → Detects active tab and tracks time
2. **background.js** → Receives USAGE_DELTA messages
3. **Firebase** → Writes to Firestore using `increment()`

```javascript
// Chrome: Increment app usage
const ref = doc(db, "users", userId, "apps", appId);
setDoc(ref, {
    date: "2026-01-22",
    secondsToday: increment(deltaSeconds),
    lastUpdated: serverTimestamp(),
}, { merge: true });
```

## Key Features

### ✅ Seamless Sync
- Both platforms write to the same Firestore documents
- Changes are reflected in real-time across devices
- No conflicts due to atomic increment operations

### ✅ Efficient Updates
- Uses `FieldValue.increment()` for atomic counter updates
- No need to read-modify-write (prevents race conditions)
- Minimal data transfer (only deltas are sent)

### ✅ Real-time Listeners
- Android can observe Chrome extension updates in real-time
- Chrome extension can observe Android updates in real-time
- Uses Firestore snapshot listeners with `callbackFlow`

### ✅ Offline Support
- Firestore handles offline caching automatically
- Writes queue when offline and sync when back online
- Both platforms benefit from Firebase's offline capabilities

## Data Model Compatibility

### Android: AppUsageData.kt
```kotlin
data class AppUsageData(
    @DocumentId val appId: String = "",
    val date: String = "",
    val secondsToday: Long = 0,
    @ServerTimestamp val lastUpdated: Date? = null
)
```

### Chrome Extension: JavaScript Object
```javascript
{
    appId: "com.instagram.android",
    date: "2026-01-22",
    secondsToday: 3600,
    lastUpdated: <Firebase Timestamp>
}
```

**Result:** Perfect 1:1 mapping between platforms! 🎉

## Usage Examples

### Android: Sync Today's Usage
```kotlin
val syncHelper = UsageSyncHelper(context)

// Sync all apps
syncHelper.syncTodayUsage()

// Sync specific app delta
syncHelper.syncAppUsageDelta(
    appId = "com.instagram.android",
    deltaSeconds = 120  // 2 minutes
)
```

### Android: Observe Real-time Updates
```kotlin
val firestoreRepo = FirestoreRepository()

// Observe all apps in real-time
firestoreRepo.observeAllAppUsage(userId).collect { appUsageList ->
    // Update UI with latest data from both Android + Chrome
    appUsageList.forEach { usage ->
        println("${usage.appId}: ${usage.secondsToday} seconds")
    }
}
```

### Chrome Extension: Send Usage Delta
```javascript
// In content.js or background.js
chrome.runtime.sendMessage({
    type: "USAGE_DELTA",
    appId: "instagram.com",
    deltaSeconds: 60,  // 1 minute
    date: new Date().toISOString().slice(0, 10)
});
```

## Cross-Platform App Naming

### Android Package Names
- `com.instagram.android`
- `com.facebook.katana`
- `com.twitter.android`

### Chrome Extension URLs → App IDs
Convert website URLs to consistent app IDs:
```javascript
// Example: Convert URL to appId
function getAppIdFromUrl(url) {
    const hostname = new URL(url).hostname;
    // instagram.com → instagram.com
    // www.instagram.com → instagram.com
    return hostname.replace(/^www\./, '');
}
```

**Recommendation:** Use domain names (e.g., `instagram.com`) for Chrome extension to keep them separate from Android package names (`com.instagram.android`).

## Preferences Sync

Preferences also sync across platforms:

### Android
```kotlin
// Update preference
prefsManager.setDailyGoalMinutes(180)

// Automatically syncs to Firestore
// Chrome extension can read: users/{userId}/data/preferences
```

### Chrome Extension
```javascript
// Read preferences from Firestore
const prefDoc = await getDoc(doc(db, "users", userId, "data", "preferences"));
const dailyGoalMinutes = prefDoc.data().dailyGoalMinutes;
```

## Testing Cross-Platform Sync

1. **Setup:**
   - Ensure both Android app and Chrome extension use the same Firebase project
   - Login with the same email/password on both platforms

2. **Test Android → Chrome:**
   - Use app on Android for 5 minutes (e.g., Instagram)
   - Wait for sync (automatic every few minutes)
   - Check Chrome extension dashboard
   - Should see Instagram usage updated

3. **Test Chrome → Android:**
   - Browse website for 5 minutes (e.g., twitter.com)
   - Wait for sync
   - Open Android app
   - Should see twitter.com usage in dashboard

4. **Test Real-time Sync:**
   - Open Android app dashboard
   - Use Chrome extension to browse
   - Watch Android dashboard update in real-time

## Performance Considerations

### Android Sync Strategy
- **Periodic Sync:** Every 15-30 minutes via WorkManager
- **Manual Sync:** User-triggered from settings
- **On App Launch:** Quick sync when app opens
- **Batch Updates:** Group multiple app updates into single batch

### Chrome Extension Sync Strategy
- **Immediate Sync:** Every minute of active usage
- **Debounced Writes:** Wait 60 seconds of inactivity before writing
- **Background Sync:** Uses background.js for reliable syncing
- **Tab Close:** Sync immediately when tab closes

### Firestore Costs
- **Writes:** Each increment = 1 write operation
- **Reads:** Dashboard queries = 1 read per app
- **Real-time Listeners:** 1 read per document update

**Cost Estimation (per user per month):**
- 10 apps tracked
- 8 hours of usage per day
- ~480 writes per day (1 per minute per app)
- ~14,400 writes per month
- Free tier: 20K writes/day, 50K reads/day
- **Result:** Well within free tier for small user base

## Troubleshooting

### Sync Delays
- **Android:** Check if user is authenticated (`firebaseAuth.currentUser != null`)
- **Chrome:** Check browser console for Firebase errors
- **Both:** Verify Firestore security rules allow writes

### Data Not Syncing
1. Check Firebase Console → Firestore → users/{userId}/apps
2. Verify documents are being created/updated
3. Check timestamps to see which platform last wrote
4. Verify same Firebase project ID in both platforms

### Duplicate Data
- **Cause:** Using different app IDs for same app
- **Fix:** Standardize app ID format across platforms
- **Prevention:** Document app ID naming convention

## Security

Both platforms use the same security rules (see FIRESTORE_RULES.md):
- Users can only read/write their own data
- Authentication required for all operations
- Field validation ensures data integrity

## Migration from Old Structure

If you have existing data in the old `users/{userId}/usage/{date}` structure:

1. Export old data from Firebase Console
2. Run migration script to transform to new structure
3. Import into new `users/{userId}/apps/{appId}` structure
4. Update security rules to remove old collection access

## Future Enhancements

### Planned Features
- [ ] Web dashboard (React) for desktop viewing
- [ ] Historical data archiving (monthly rollup)
- [ ] Weekly/monthly aggregation documents
- [ ] Cross-device notifications (e.g., "You've reached your goal!")
- [ ] Shared family accounts (multi-user tracking)

### Performance Optimizations
- [ ] Client-side caching to reduce Firestore reads
- [ ] Batch writes for multiple apps at once
- [ ] Compression for large usage histories
- [ ] Indexing for faster queries

## Summary

The Android app and Chrome extension now share the same Firestore structure:
- ✅ Same data model (`users/{userId}/apps/{appId}`)
- ✅ Atomic increments prevent conflicts
- ✅ Real-time sync with snapshot listeners
- ✅ Offline support via Firestore caching
- ✅ Secure with proper authentication rules

This architecture ensures a seamless cross-platform experience! 🚀
