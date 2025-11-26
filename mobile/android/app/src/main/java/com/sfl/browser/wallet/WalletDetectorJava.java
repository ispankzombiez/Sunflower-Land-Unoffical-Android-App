package com.sfl.browser.wallet;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Java version of WalletDetector for compatibility with Java code
 * Detects which crypto wallets are installed on the device.
 */
public class WalletDetectorJava {
    private static final String TAG = "WalletDetector";

    private static final class WalletInfo {
        String id;
        String name;
        String packageName;
        String connectionMethod;

        WalletInfo(String id, String name, String packageName, String connectionMethod) {
            this.id = id;
            this.name = name;
            this.packageName = packageName;
            this.connectionMethod = connectionMethod;
        }
    }

    private static final List<WalletInfo> SUPPORTED_WALLETS = new ArrayList<>();

    static {
        SUPPORTED_WALLETS.add(new WalletInfo("metamask", "MetaMask", "io.metamask", "sdk"));
        SUPPORTED_WALLETS.add(new WalletInfo("phantom", "Phantom", "app.phantom", "deeplink"));
        SUPPORTED_WALLETS.add(new WalletInfo("rabby", "Rabby Wallet", "com.debank.rabbymobile", "deeplink"));
        SUPPORTED_WALLETS.add(new WalletInfo("trust", "Trust Wallet", "com.wallet.crypto.trustapp", "deeplink"));
        SUPPORTED_WALLETS.add(new WalletInfo("coinbase", "Coinbase Wallet", "com.coinbase.android", "deeplink"));
        SUPPORTED_WALLETS.add(new WalletInfo("ledger", "Ledger Live", "com.ledger.live", "deeplink"));
        SUPPORTED_WALLETS.add(new WalletInfo("okx", "OKX Wallet", "com.okinc.okex.gp", "deeplink"));
        SUPPORTED_WALLETS.add(new WalletInfo("safepal", "SafePal", "io.safepal.wallet", "deeplink"));
        SUPPORTED_WALLETS.add(new WalletInfo("exodus", "Exodus", "com.exodus", "deeplink"));
        SUPPORTED_WALLETS.add(new WalletInfo("keplr", "Keplr", "com.chainapsis.keplr", "deeplink"));
    }

    /**
     * Get all installed wallets that the app supports
     */
    public static List<InstalledWallet> getInstalledWallets(Context context) {
        PackageManager packageManager = context.getPackageManager();
        List<InstalledWallet> installedWallets = new ArrayList<>();

        Log.d(TAG, "Scanning for installed wallets...");

        for (WalletInfo walletInfo : SUPPORTED_WALLETS) {
            try {
                // Check if wallet app is installed
                packageManager.getApplicationInfo(walletInfo.packageName, 0);

                InstalledWallet installedWallet = new InstalledWallet(
                    walletInfo.id,
                    walletInfo.name,
                    walletInfo.packageName,
                    walletInfo.connectionMethod
                );

                installedWallets.add(installedWallet);
                Log.d(TAG, "Found installed wallet: " + walletInfo.name + " (" + walletInfo.packageName + ")");
            } catch (PackageManager.NameNotFoundException e) {
                // Wallet not installed, skip it
                Log.d(TAG, walletInfo.name + " not installed");
            } catch (Exception e) {
                Log.w(TAG, "Error checking " + walletInfo.name + ": " + e.getMessage());
            }
        }

        Log.d(TAG, "Wallet scan complete. Found " + installedWallets.size() + " installed wallet(s)");
        return installedWallets;
    }

    /**
     * Check if a specific wallet is installed
     */
    public static boolean isWalletInstalled(Context context, String walletId) {
        for (WalletInfo walletInfo : SUPPORTED_WALLETS) {
            if (walletInfo.id.equals(walletId)) {
                try {
                    context.getPackageManager().getApplicationInfo(walletInfo.packageName, 0);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Get wallet info by ID
     */
    public static WalletInfo getWalletInfo(String walletId) {
        for (WalletInfo walletInfo : SUPPORTED_WALLETS) {
            if (walletInfo.id.equals(walletId)) {
                return walletInfo;
            }
        }
        return null;
    }

    /**
     * Get installed wallet count
     */
    public static int getInstalledWalletCount(Context context) {
        return getInstalledWallets(context).size();
    }
}
