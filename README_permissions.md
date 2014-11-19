Reasons for permissions:
========================

Android's permission system is not precise. To access the app's own data
and settings we must often request access to the data and settings for all
apps. This is apparently unavoidable.

* WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE
  (only requested on Android 4.3 and older.)
  To access the app's local cache of images.
  The app does not access files belonging to any other application.

* INTERNET:
  To download new images to classify and to upload your classifications.

* ACCESS_NETWORK_STATE:
  To discover whether your device is connected so the app knows when to 
  try downloading new images and uploading your classifications.

* GET_ACCOUNTS, MANAGE_ACCOUNTS, AUTHENTICATE_ACCOUNTS, and USE_CREDENTIALS:
  To discover and use your login details which the app stores in the Android
  accounts system. It uses this system for security and so it can use Android's
  SyncAdapter system to download new images and upload your classifications.
  The app does not access accounts belonging any other application.

* READ_SYNC_SETTINGS and WRITE_SYNC_SETTINGS:
  To use Android's SyncAdapter system to download new images and upload your
  classifications.
  The app does not access sync settings belonging to any other application.



Android is a trademark of Google Inc.

