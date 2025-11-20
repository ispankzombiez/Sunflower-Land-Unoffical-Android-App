package com.sunflowerland.mobile.wallet

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.preference.PreferenceManager

/**
 * Manages wallet connection state persistence
 * Stores and retrieves wallet connection information in SharedPreferences
 */
object WalletStatePersistence {
    private const val TAG = "WalletStatePersistence"
    
    // Preference keys
    private const val PREF_CONNECTED_WALLET = "wallet_connected_wallet"
    private const val PREF_WALLET_ADDRESS = "wallet_address"
    private const val PREF_WALLET_CHAIN_ID = "wallet_chain_id"
    private const val PREF_WALLET_CONNECTED_AT = "wallet_connected_at"
    private const val PREF_WALLET_CONNECTION_METHOD = "wallet_connection_method"

    /**
     * Data class to represent wallet connection state
     */
    data class WalletConnectionState(
        val walletId: String,
        val address: String,
        val chainId: String? = null,
        val connectionMethod: String,
        val connectedAt: Long = System.currentTimeMillis()
    )

    /**
     * Save wallet connection state
     */
    fun saveWalletConnection(context: Context, state: WalletConnectionState) {
        try {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            with(prefs.edit()) {
                putString(PREF_CONNECTED_WALLET, state.walletId)
                putString(PREF_WALLET_ADDRESS, state.address)
                if (state.chainId != null) {
                    putString(PREF_WALLET_CHAIN_ID, state.chainId)
                }
                putString(PREF_WALLET_CONNECTION_METHOD, state.connectionMethod)
                putLong(PREF_WALLET_CONNECTED_AT, state.connectedAt)
                apply()
            }
            Log.d(TAG, "Saved wallet connection state: ${state.walletId}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving wallet connection: ${e.message}", e)
        }
    }

    /**
     * Get current wallet connection state
     */
    fun getWalletConnection(context: Context): WalletConnectionState? {
        return try {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val walletId = prefs.getString(PREF_CONNECTED_WALLET, null)
            
            if (walletId == null) {
                Log.d(TAG, "No wallet connection found")
                return null
            }

            val address = prefs.getString(PREF_WALLET_ADDRESS, "") ?: ""
            val chainId = prefs.getString(PREF_WALLET_CHAIN_ID, null)
            val connectionMethod = prefs.getString(PREF_WALLET_CONNECTION_METHOD, "deeplink") ?: "deeplink"
            val connectedAt = prefs.getLong(PREF_WALLET_CONNECTED_AT, System.currentTimeMillis())

            WalletConnectionState(
                walletId = walletId,
                address = address,
                chainId = chainId,
                connectionMethod = connectionMethod,
                connectedAt = connectedAt
            ).also {
                Log.d(TAG, "Retrieved wallet connection: $walletId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving wallet connection: ${e.message}", e)
            null
        }
    }

    /**
     * Clear wallet connection state
     */
    fun clearWalletConnection(context: Context) {
        try {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            with(prefs.edit()) {
                remove(PREF_CONNECTED_WALLET)
                remove(PREF_WALLET_ADDRESS)
                remove(PREF_WALLET_CHAIN_ID)
                remove(PREF_WALLET_CONNECTED_AT)
                remove(PREF_WALLET_CONNECTION_METHOD)
                apply()
            }
            Log.d(TAG, "Cleared wallet connection state")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing wallet connection: ${e.message}", e)
        }
    }

    /**
     * Check if a wallet is currently connected
     */
    fun isWalletConnected(context: Context): Boolean {
        return getWalletConnection(context) != null
    }

    /**
     * Get connected wallet ID
     */
    fun getConnectedWalletId(context: Context): String? {
        return getWalletConnection(context)?.walletId
    }

    /**
     * Get connected wallet address
     */
    fun getConnectedWalletAddress(context: Context): String? {
        return getWalletConnection(context)?.address
    }

    /**
     * Update wallet address (e.g., if user switched accounts in MetaMask)
     */
    fun updateWalletAddress(context: Context, address: String) {
        try {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            prefs.edit().putString(PREF_WALLET_ADDRESS, address).apply()
            Log.d(TAG, "Updated wallet address")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating wallet address: ${e.message}", e)
        }
    }

    /**
     * Update chain ID (e.g., if user switched networks in MetaMask)
     */
    fun updateChainId(context: Context, chainId: String) {
        try {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            prefs.edit().putString(PREF_WALLET_CHAIN_ID, chainId).apply()
            Log.d(TAG, "Updated chain ID: $chainId")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating chain ID: ${e.message}", e)
        }
    }

    /**
     * Get time when wallet was connected
     */
    fun getWalletConnectedTime(context: Context): Long? {
        return getWalletConnection(context)?.connectedAt
    }
}
