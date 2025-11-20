package com.sunflowerland.mobile.wallet

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import io.metamask.androidsdk.Result

/**
 * Routes wallet connections to the appropriate integration method
 * - MetaMask: Use native Android SDK
 * - Others: Use deeplinks
 */
class WalletConnectionHandler(private val context: Context) {
    companion object {
        private const val TAG = "WalletConnectionHandler"
    }

    // Callback interface for connection results
    interface ConnectionCallback {
        fun onSuccess(address: String?, chainId: String?)
        fun onError(error: String)
    }

    private val metaMaskManager = MetaMaskSDKManager(context)

    /**
     * Connect to wallet using the appropriate method
     */
    fun connectWallet(walletId: String, callback: ConnectionCallback? = null) {
        Log.d(TAG, "Attempting to connect to wallet: $walletId")

        val wallet = getWalletInfo(walletId)
        if (wallet == null) {
            Log.e(TAG, "Unknown wallet: $walletId")
            callback?.onError("Unknown wallet")
            return
        }

        when (wallet.connectionMethod) {
            "sdk" -> connectWithSDK(wallet, callback)
            "deeplink" -> connectWithDeeplink(wallet, callback)
            else -> {
                Log.e(TAG, "Unknown connection method: ${wallet.connectionMethod}")
                callback?.onError("Unknown connection method")
            }
        }
    }

    /**
     * Connect using native SDK (MetaMask)
     */
    private fun connectWithSDK(wallet: WalletInfo, callback: ConnectionCallback? = null) {
        Log.d(TAG, "Connecting via SDK: ${wallet.name}")

        metaMaskManager.initialize()

        metaMaskManager.connect { result ->
            when (result) {
                is Result.Success.Item -> {
                    val address = result.value as? String ?: ""
                    val chainId = getCurrentChainId()
                    Log.d(TAG, "SDK connection successful, address: $address")
                    callback?.onSuccess(address, chainId)
                }
                is Result.Error -> {
                    Log.e(TAG, "SDK connection failed: ${result.error.message}")
                    callback?.onError(result.error.message ?: "Connection failed")
                }
                else -> {
                    Log.w(TAG, "Unexpected result type from SDK")
                    callback?.onError("Unexpected result")
                }
            }
        }
    }

    /**
     * Connect using deeplink (Phantom, Rabby, Trust Wallet, etc.)
     */
    private fun connectWithDeeplink(wallet: WalletInfo, callback: ConnectionCallback? = null) {
        Log.d(TAG, "Connecting via deeplink: ${wallet.name}")

        try {
            val deeplink = buildDeeplink(wallet.id)
            Log.d(TAG, "Launching deeplink: $deeplink")

            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(deeplink)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(intent)

            // Deeplink connection is async - success is determined when app receives callback
            // Callback will be handled via MainActivity.handleIntent()
            Log.d(TAG, "Deeplink launched successfully")
            callback?.onSuccess(null, null)  // Return true since launch succeeded
        } catch (e: Exception) {
            Log.e(TAG, "Error launching deeplink: ${e.message}", e)
            callback?.onError(e.message ?: "Failed to launch wallet")
        }
    }

    /**
     * Build deeplink URI for wallet connection
     */
    private fun buildDeeplink(walletId: String): String {
        // Standard deeplink format for wallet connection
        // Format: <wallet-scheme>://connect
        return when (walletId) {
            "phantom" -> "phantom://phantom_connect"
            "rabby" -> "rabby://connect"
            "trust" -> "trust://connect"
            "coinbase" -> "coinbase://connect"
            else -> "$walletId://connect"
        }
    }

    /**
     * Disconnect from connected wallet
     */
    fun disconnectWallet(walletId: String, callback: ((Boolean) -> Unit)? = null) {
        Log.d(TAG, "Disconnecting from wallet: $walletId")

        when (walletId) {
            "metamask" -> {
                metaMaskManager.disconnect { success ->
                    Log.d(TAG, "Disconnected from MetaMask: $success")
                    callback?.invoke(success)
                }
            }
            else -> {
                // For deeplink-based wallets, just clear local state
                Log.d(TAG, "Deeplink wallet disconnected (clearing local state)")
                callback?.invoke(true)
            }
        }
    }

    /**
     * Get the currently connected account address (if connected via SDK)
     */
    fun getConnectedAddress(): String? {
        return metaMaskManager.getSelectedAddress()
    }

    /**
     * Get the current chain ID (if connected via SDK)
     */
    fun getCurrentChainId(): String? {
        return metaMaskManager.getChainId()
    }

    /**
     * Check if wallet is ready for operations
     */
    fun isWalletReady(): Boolean {
        return metaMaskManager.isReady()
    }

    /**
     * Get wallet info by ID
     */
    private fun getWalletInfo(walletId: String): WalletInfo? {
        return WalletDetector.SUPPORTED_WALLETS[walletId]
    }
}


