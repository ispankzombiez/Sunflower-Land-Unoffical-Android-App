package com.sunflowerland.mobile.wallet

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

/**
 * Detects which crypto wallets are installed on the device.
 * Returns only installed wallets that the app supports.
 */
data class InstalledWallet(
    val id: String,              // "metamask", "phantom", "rabby", etc.
    val name: String,            // "MetaMask", "Phantom", etc.
    val packageName: String,     // Android package name
    val connectionMethod: String // "sdk" for MetaMask, "deeplink" for others
)

object WalletDetector {
    private const val TAG = "WalletDetector"

    // Comprehensive list of supported wallets
    private val _SUPPORTED_WALLETS = listOf(
        WalletInfo(
            id = "metamask",
            name = "MetaMask",
            packageName = "io.metamask",
            connectionMethod = "sdk"
        ),
        WalletInfo(
            id = "phantom",
            name = "Phantom",
            packageName = "app.phantom",
            connectionMethod = "deeplink"
        ),
        WalletInfo(
            id = "rabby",
            name = "Rabby Wallet",
            packageName = "com.debank.rabbymobile",
            connectionMethod = "deeplink"
        ),
        WalletInfo(
            id = "trust",
            name = "Trust Wallet",
            packageName = "com.wallet.crypto.trustapp",
            connectionMethod = "deeplink"
        ),
        WalletInfo(
            id = "coinbase",
            name = "Coinbase Wallet",
            packageName = "com.coinbase.android",
            connectionMethod = "deeplink"
        ),
        WalletInfo(
            id = "ledger",
            name = "Ledger Live",
            packageName = "com.ledger.live",
            connectionMethod = "deeplink"
        ),
        WalletInfo(
            id = "okx",
            name = "OKX Wallet",
            packageName = "com.okinc.okex.gp",
            connectionMethod = "deeplink"
        ),
        WalletInfo(
            id = "safepal",
            name = "SafePal",
            packageName = "io.safepal.wallet",
            connectionMethod = "deeplink"
        ),
        WalletInfo(
            id = "exodus",
            name = "Exodus",
            packageName = "com.exodus",
            connectionMethod = "deeplink"
        ),
        WalletInfo(
            id = "keplr",
            name = "Keplr",
            packageName = "com.chainapsis.keplr",
            connectionMethod = "deeplink"
        )
    )

    // Public map of supported wallets for easy lookup
    val SUPPORTED_WALLETS: Map<String, WalletInfo> = _SUPPORTED_WALLETS.associateBy { it.id }

    /**
     * Get all installed wallets that the app supports
     */
    fun getInstalledWallets(context: Context): List<InstalledWallet> {
        val packageManager = context.packageManager
        val installedWallets = mutableListOf<InstalledWallet>()

        Log.d(TAG, "Scanning for installed wallets...")

        for (walletInfo in _SUPPORTED_WALLETS) {
            try {
                // Check if wallet app is installed
                packageManager.getApplicationInfo(walletInfo.packageName, 0)
                
                val installedWallet = InstalledWallet(
                    id = walletInfo.id,
                    name = walletInfo.name,
                    packageName = walletInfo.packageName,
                    connectionMethod = walletInfo.connectionMethod
                )
                
                installedWallets.add(installedWallet)
                Log.d(TAG, "Found installed wallet: ${walletInfo.name} (${walletInfo.packageName})")
            } catch (e: PackageManager.NameNotFoundException) {
                // Wallet not installed, skip it
                Log.d(TAG, "${walletInfo.name} not installed")
            } catch (e: Exception) {
                Log.w(TAG, "Error checking ${walletInfo.name}: ${e.message}")
            }
        }

        Log.d(TAG, "Wallet scan complete. Found ${installedWallets.size} installed wallet(s)")
        return installedWallets
    }

    /**
     * Check if a specific wallet is installed
     */
    fun isWalletInstalled(context: Context, walletId: String): Boolean {
        val walletInfo = _SUPPORTED_WALLETS.find { it.id == walletId }
        return if (walletInfo != null) {
            try {
                context.packageManager.getApplicationInfo(walletInfo.packageName, 0)
                true
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }

    /**
     * Get wallet info by ID
     */
    fun getWalletInfo(walletId: String): WalletInfo? {
        return _SUPPORTED_WALLETS.find { it.id == walletId }
    }

    /**
     * Get installed wallet count
     */
    fun getInstalledWalletCount(context: Context): Int {
        return getInstalledWallets(context).size
    }
}

/**
 * Internal wallet configuration
 */
data class WalletInfo(
    val id: String,
    val name: String,
    val packageName: String,
    val connectionMethod: String // "sdk" or "deeplink"
)
