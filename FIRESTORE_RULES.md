# Firestore Security Rules for Arete

This document contains the Firestore security rules for the Arete app. These rules ensure that users can only access their own data while maintaining proper security.

## Rules Structure

The database follows this structure:
```
users/{userId}/
  ├── data/
  │   ├── profile        (user profile information)
  │   └── preferences    (user settings and preferences)
  └── apps/
      └── {appId}        (per-app usage statistics)
```

## Security Rules

Copy and paste these rules into your Firebase Console → Firestore Database → Rules:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Helper function to check if user is authenticated and matches the document owner
    function isAuthenticated() {
      return request.auth != null;
    }

    function isOwner(userId) {
      return isAuthenticated() && request.auth.uid == userId;
    }

    // Users collection
    match /users/{userId} {

      // User data subcollection (profile, preferences)
      match /data/{document} {
        // Users can read and write their own profile and preferences
        allow read, write: if isOwner(userId);
      }

      // Apps usage subcollection (matches Chrome extension structure)
      match /apps/{appId} {
        // Users can read and write their own app usage data
        allow read: if isOwner(userId);

        // Allow create and update for usage tracking
        allow create: if isOwner(userId)
          && request.resource.data.keys().hasAll(['date', 'secondsToday', 'lastUpdated']);

        allow update: if isOwner(userId)
          && request.resource.data.keys().hasAll(['date', 'secondsToday', 'lastUpdated']);

        // Allow delete for cleanup operations
        allow delete: if isOwner(userId);
      }
    }

    // Deny all other access by default
    match /{document=**} {
      allow read, write: if false;
    }
  }
}
```

## Field Validation (Optional Enhanced Rules)

For stricter validation, you can use these enhanced rules:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    function isAuthenticated() {
      return request.auth != null;
    }

    function isOwner(userId) {
      return isAuthenticated() && request.auth.uid == userId;
    }

    match /users/{userId} {

      match /data/profile {
        allow read: if isOwner(userId);
        allow create: if isOwner(userId)
          && request.resource.data.uid == userId
          && request.resource.data.email is string;
        allow update: if isOwner(userId);
      }

      match /data/preferences {
        allow read, write: if isOwner(userId);
      }

      match /apps/{appId} {
        allow read: if isOwner(userId);

        allow create, update: if isOwner(userId)
          && request.resource.data.date is string
          && request.resource.data.date.matches('^[0-9]{4}-[0-9]{2}-[0-9]{2}$')
          && request.resource.data.secondsToday is number
          && request.resource.data.secondsToday >= 0;

        allow delete: if isOwner(userId);
      }
    }
  }
}
```

## Testing Your Rules

Before deploying to production, test your rules in the Firebase Console:

1. Go to Firebase Console → Firestore Database → Rules
2. Click on "Rules Playground"
3. Test these scenarios:

### Test Case 1: User can read their own app usage
```
Location: /users/USER_ID/apps/com.instagram.android
Authentication: Authenticated as USER_ID
Operation: get
Expected: Allow
```

### Test Case 2: User cannot read another user's data
```
Location: /users/OTHER_USER_ID/apps/com.instagram.android
Authentication: Authenticated as USER_ID
Operation: get
Expected: Deny
```

### Test Case 3: User can write their own app usage
```
Location: /users/USER_ID/apps/com.instagram.android
Authentication: Authenticated as USER_ID
Operation: set
Data: {
  "date": "2026-01-22",
  "secondsToday": 3600,
  "lastUpdated": <server_timestamp>
}
Expected: Allow
```

## Migration from Old Structure

If you were using the old `usage/{date}` structure, you'll need to migrate your data:

1. Export existing data from `users/{userId}/usage/{date}`
2. Transform to new per-app structure
3. Import into `users/{userId}/apps/{appId}`

No automatic migration is provided - handle this manually based on your data volume.

## Indexes (Optional)

For better query performance, add these indexes in Firebase Console → Firestore Database → Indexes:

1. **Collection**: `apps` (collection group)
   - Fields: `date` (Ascending), `secondsToday` (Descending)
   - Query scope: Collection group

This allows efficient querying of app usage across all users if needed for analytics.

## Notes

- The `lastUpdated` field uses `@ServerTimestamp` to ensure consistent timing
- The `secondsToday` field uses `FieldValue.increment()` for atomic updates
- Chrome extension and Android app use the same structure for seamless sync
- Each app's usage is stored in a separate document for efficient updates
