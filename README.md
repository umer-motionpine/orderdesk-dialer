# Order Desk Dialer (Android)

Laptop pe **📱 Phone se call karo** dabao → ye app phone par number dial kar deta hai.

## Kaise chalta hai
1. Desk (Claude artifact) `dial/current` document likhta hai: `{n, o, nm, ts}`.
2. Ye app desk ko `#phone` mode mein WebView mein kholta hai.
3. Desk ka page `dial/current` sunta hai; naya number aaye to `AndroidDialer.dial(n)` call karta hai.
4. App `ACTION_CALL` fire karta hai → phone dial karta hai. Desk ko `ack` wapas.

## Install
- GitHub → Releases → `OrderDeskDialer.apk` download → install (Unknown apps allow karna padega).
- App kholo → Claude mein sign in (ek baar) → "Allow phone calls" → done.
- Phone ko charging par rakho, app khula rakho.

## Phone badalna
Naye phone par APK install karo, sign in karo. Bas.

## Build
GitHub Actions har push par APK banata hai (`.github/workflows/build.yml`). Local build: Android Studio → Run.
