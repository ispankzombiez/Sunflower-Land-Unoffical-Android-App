# Mobile (Capacitor) wrapper — build & run on Android emulator

This folder contains a minimal mobile web app that uses WalletConnect v2 to pair a mobile wallet inside an Android WebView (Capacitor). It bundles with Parcel and outputs into `../www/`, ready for Capacitor.

Prerequisites
- Node.js and npm
- Android Studio and Android SDK (emulator + platform-tools)
- Capacitor CLI (we'll use npx so no global install required)

Quick steps (PowerShell)

1. Install dependencies for the mobile app

```powershell
cd mobile
npm install
```

2. Build the web assets (outputs to `../www`)

```powershell
npm run build
```

3. Initialize Capacitor Android once (only first time)

```powershell
npx cap init --web-dir=www "sunflower-mobile" "com.sunflowerland.mobile"
npx cap add android
```

4. Copy web assets and open Android project in Android Studio

```powershell
npx cap copy android
npx cap open android
```

5. In Android Studio
- Build and run the app on an emulator (create an AVD with Google Play to install MetaMask) or a connected device.

Using the app
- Enter your WalletConnect v2 Project ID (WC_PROJECT_ID) in the field and press Connect. The app will open the WalletConnect modal to pair with MetaMask Mobile.
-- After pairing, the app navigates to the Sunflower Land site inside the WebView. The provider is exposed on `window.injectedProvider` (best-effort).

Injecting a full EIP-1193 shim into arbitrary websites

Because external pages are cross-origin, we can't inject JS from the web app itself after navigation. Instead, we inject the shim at the WebView/native layer after the page loads. I added `mobile/src/inject-shim.js` which defines a robust EIP-1193 shim that forwards calls to `window.injectedProvider` when present.

To activate this shim inside the Android WebView, open the Android project in Android Studio (after `npx cap add android`) and modify the `MainActivity` to evaluate the shim script on each page load.

Exact Kotlin snippet to add (paste into `android/app/src/main/kotlin/<your_package>/MainActivity.kt` inside `onCreate` after `super.onCreate` and after Capacitor setup):

```kotlin
import android.webkit.WebView
import android.webkit.WebViewClient
import java.nio.charset.StandardCharsets
import android.content.res.AssetManager

// add inside your MainActivity class (which extends BridgeActivity)
override fun onStart() {
  super.onStart()
  val webView = this.bridge.webView
  // load inject script from assets (www/inject-shim.js)
  val am: AssetManager = this.assets
  val input = am.open("inject-shim.js")
  val bytes = input.readBytes()
  input.close()
  val injectScript = String(bytes, StandardCharsets.UTF_8)

  webView.webViewClient = object : WebViewClient() {
    override fun onPageFinished(view: WebView?, url: String?) {
      super.onPageFinished(view, url)
      try {
        // Evaluate the shim in the page context
        view?.evaluateJavascript(injectScript, null)
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }
}
```

Important notes:
- This code reads `inject-shim.js` from the app's assets (Capacitor copies `www/` into the Android assets). The `inject-shim.js` file we added to `mobile/src/` will be included in `www/` after `npm run build` and `npx cap copy`.
- The snippet uses `evaluateJavascript` to inject the shim after each page finishes loading. This approach allows us to inject a consistent EIP-1193 shim into external pages loaded in the WebView.
- Test carefully: inject only for trusted targets or add allowlist checks for `url` before injecting to avoid interfering with unrelated pages.

After adding the Kotlin snippet:

1. Build and run the Android app from Android Studio on your emulator.
2. In the app, enter your `WC_PROJECT_ID` and press Connect. After pairing, when the WebView loads Sunflower Land, the shim will be injected and `window.ethereum` should be available to the site.

If you'd like, I can also provide a ready-made patch for the Android `MainActivity` after you run `npx cap add android` so you can paste it directly into your project.

Notes and next steps
- This is a pragmatic starter. For production use we should:
  - Inject a robust EIP-1193 shim into the WebView when the target site loads.
  - Add UI to manage connect/disconnect and show selected account/chain.
  - Secure the WebView (navigation allowlist, CSP) before distribution.
