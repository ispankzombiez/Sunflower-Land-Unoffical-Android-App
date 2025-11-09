# Sunflower Land Unofficial Android App

An unofficial Android mobile app for [Sunflower Land](https://sunflower-land.com/) built with Capacitor and WalletConnect. This app provides a mobile-optimized WebView experience with integrated Web3 wallet connectivity and native Android push-notifications.

## 🚀 Features

- **Mobile WebView**: Full Sunflower Land game experience in a native Android app
- **WalletConnect v2**: Seamless wallet connectivity with MetaMask and other supported wallets
- **Multi-Wallet Support**: Connect various Ethereum wallets via WalletConnect
- **EIP-1193 Provider**: Injected Web3 provider for dApp compatibility
- **Native Android UI**: Optimized for mobile gaming experience

## 🧪 Become a Tester!

Want to try the app before anyone else? Join our early access program!

📝 **[Fill out this form](https://forms.gle/Wc5oQVWpg8nT8ycB8)** to request early access

📱 Once early-access has been granted, you can download the app on Google Play **[here](https://play.google.com/store/apps/details?id=com.sunflowerland.mobile)**

💬 For more info, or to reach out to the developer iSPANK, go **[here](https://discord.com/channels/880987707214544966/1314031342182338651)**

## 📱 Screenshots

*Screenshots coming soon*

## 🛠️ Prerequisites

Before building the app, make sure you have:

- **Node.js** (v16 or later)
- **Android Studio** with Android SDK
- **Android emulator** or physical device for testing
- **Java JDK 11** or later

## 🏗️ Project Structure

```
├── mobile/                 # Capacitor mobile app source
│   ├── src/               # Web app source code
│   ├── android/           # Android native project
│   └── README_ANDROID.md  # Detailed Android setup guide
├── www/                   # Built web assets (generated)
├── .github/               # GitHub workflows and templates
└── package.json           # Root dependencies
```

## 🚀 Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/ispankzombiez/Sunflower-Land-Unoffical-Android-App.git
cd Sunflower-Land-Unoffical-Android-App
```

### 2. Install Dependencies

```bash
# Install root dependencies
npm install

# Install mobile app dependencies
cd mobile
npm install
```

### 3. Configure Signing (for release builds)

Copy the signing template and add your credentials:

```bash
cp mobile/android/gradle.properties.template mobile/android/signing.properties
```

Edit `mobile/android/signing.properties` with your keystore details:

```properties
RELEASE_STORE_FILE=path/to/your/keystore.jks
RELEASE_STORE_PASSWORD=your_store_password
RELEASE_KEY_ALIAS=your_key_alias
RELEASE_KEY_PASSWORD=your_key_password
```

### 4. Build and Run

```bash
# Build the web assets
npm run build

# Copy to Android and open in Android Studio
npx cap copy android
npx cap open android
```

### 5. Run in Android Studio

- Create an AVD (Android Virtual Device) with Google Play Services
- Build and run the project
- The app will launch with WalletConnect integration ready

## 🔧 Development

### Development Build

```bash
cd mobile
npm run dev          # Start development server
npm run build        # Build for production
npm run clean        # Clean build artifacts
```

### Android Development

See the detailed guide in [`mobile/README_ANDROID.md`](mobile/README_ANDROID.md) for:
- Android Studio setup
- Emulator configuration
- Native development workflow
- Debugging tips

## 🌐 WalletConnect Setup

1. Get your WalletConnect Project ID from [WalletConnect Cloud](https://cloud.walletconnect.com/)
2. Enter it in the app when prompted
3. Scan the QR code with your mobile wallet (MetaMask, etc.)
4. Enjoy playing Sunflower Land with your connected wallet!

## 📂 Key Files

- `mobile/src/index.js` - Main app entry point
- `mobile/src/inject-shim.js` - EIP-1193 provider injection
- `mobile/android/app/build.gradle` - Android build configuration
- `.gitignore` - Excludes sensitive files and build artifacts

## 🛡️ Security

- Signing credentials are kept in `signing.properties` (not committed to git)
- API keys and sensitive data are excluded from version control
- All builds require proper signing configuration

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature-name`
3. Commit changes: `git commit -m 'Add feature'`
4. Push to branch: `git push origin feature-name`
5. Open a pull request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## ⚠️ Disclaimer

This is an **unofficial** app and is not affiliated with, endorsed by, or connected to Sunflower Land or its developers. Use at your own risk.

## 🐛 Issues & Support

If you encounter any issues:

1. Check the [Issues](https://github.com/ispankzombiez/Sunflower-Land-Unoffical-Android-App/issues) page
2. Create a new issue with detailed information
3. Include device info, Android version, and error logs

## 🙏 Acknowledgments

- [Sunflower Land](https://sunflower-land.com/) - The amazing game this app is built for
- [Capacitor](https://capacitorjs.com/) - Cross-platform native runtime
- [WalletConnect](https://walletconnect.com/) - Web3 wallet connectivity protocol
