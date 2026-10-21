# SellerChat — Privacy Policy

_Last updated: 2026-10-21_

SellerChat ("the app") is built on one rule: **your data stays on your phone.**

## What the app stores — on your device only

- Phone numbers you enter or message, and the customer labels you give them (the "Recents" list).
- Per-customer details you add: a pipeline status (Inquiry / Ordered / Paid / Shipped), a memo, an optional payment amount, and a follow-up reminder time if you set one.
- Your message templates.
- Your settings: default country, default app (WhatsApp / WhatsApp Business), default template, preferred template language, default status, and your own WhatsApp number if you choose to save it for your shareable link.

All of this is stored in the app's private storage on your device. It is **never uploaded, never synced, and never shared with us or anyone else** — the app has no account system, no server, and no cloud component. Uninstalling the app deletes this data.

## Notifications (the one permission, and only if you ask)

SellerChat declares exactly two permissions, and neither is requested when the app opens:

1. **Notifications (`POST_NOTIFICATIONS`)** — used for one thing only: the "Follow-up in 2 days" reminder you can set on a customer. On Android 13 and above, the system asks for your permission **at the moment you tap that button** — never at launch, never in the background. If you decline, the reminder is still saved inside the app (the customer row shows "Reminder set (in-app)"); you simply won't get the notification. Reminder scheduling happens entirely on your device; the notification shows the customer's label or number on your own lock screen and opens that customer's chat when tapped. You can revoke the permission at any time in Android Settings → Apps → SellerChat → Notifications.
2. **Advertising ID (`com.google.android.gms.permission.AD_ID`)** — declared so it is visible on purpose: the AdMob SDK (below) may use your device's advertising ID to deliver the banner ad. See "Advertising".

That is the complete list. There is no contacts, location, camera, microphone, storage, SMS, or accessibility permission — declared or requested.

## What the app does NOT do

- It does **not** read your WhatsApp messages or chats. It only opens WhatsApp (via WhatsApp's own public wa.me link system) with text you chose, typed into the message box. Nothing is ever sent automatically — you always press send yourself inside WhatsApp.
- It does **not** collect analytics, track you, or build a profile of you.
- It does **not** sell or share personal data. There is no personal data collected to sell or share.

## Advertising

The app shows **one banner advertisement** on the Home screen, provided by Google AdMob. To serve ads, Google may collect and use data as described in Google's Privacy Policy (https://policies.google.com/privacy) and the Google Mobile Ads SDK documentation — this can include a device advertising identifier and general device information. That data is handled by Google, not by SellerChat, and no SellerChat data (your numbers, labels, or templates) is ever passed to the ad system.

You can reset or delete your advertising ID at any time in Android Settings → Privacy → Ads.

## Clipboard

The app reads your clipboard **only when you tap the Paste button**, only while the app is open, and only to fill in the phone-number field. It never reads the clipboard in the background or on launch.

## Children

The app is a business tool intended for sellers and is not directed at children under 13.

## Changes

If this policy changes, the new version will be posted at this URL and the "last updated" date above will change.

## Contact

Questions about this policy: TODO(FAHAD): add your support email here before publishing.

---

### For the Google Play Data Safety section (declaration reference)

- **Data collected by the app:** None. All user-entered data is stored locally on the device only.
- **Data shared with third parties:** None by the app itself. The Google Mobile Ads SDK may collect device/advertising identifiers for ad serving, per Google's own declarations — declare "Device or other IDs" under the AdMob SDK entry in Play Console, purpose: Advertising.
- **Data encrypted in transit:** Not applicable for app data (nothing leaves the device). Ad traffic is handled by Google's SDK over HTTPS.
- **Data deletion:** Users can delete all app data at any time by clearing the app's storage or uninstalling.
- **Permissions declared:** `POST_NOTIFICATIONS` (follow-up reminders the user explicitly sets; requested in-context only) and `com.google.android.gms.permission.AD_ID` (AdMob ad delivery). Neither is a Data Safety "data collection" item by itself; both are declared here for transparency.
