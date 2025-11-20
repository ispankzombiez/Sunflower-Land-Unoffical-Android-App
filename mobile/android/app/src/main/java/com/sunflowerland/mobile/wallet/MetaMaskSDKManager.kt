package com.sunflowerland.mobile.wallet

import android.content.Context
import android.util.Log
import io.metamask.androidsdk.Ethereum
import io.metamask.androidsdk.DappMetadata
import io.metamask.androidsdk.SDKOptions
import io.metamask.androidsdk.EthereumRequest
import io.metamask.androidsdk.Result

/**
 * Wrapper around MetaMask Android SDK
 * Provides a simplified interface for wallet connections and operations
 */
class MetaMaskSDKManager(private val context: Context) {
    private var ethereum: Ethereum? = null
    private var isInitialized = false

    companion object {
        private const val TAG = "MetaMaskSDKManager"
        private const val INFURA_API_KEY = "1234567890"  // TODO: Replace with actual Infura key
    }

    /**
     * Initialize the MetaMask SDK
     * Must be called before any wallet operations
     */
    fun initialize() {
        if (isInitialized) {
            Log.d(TAG, "SDK already initialized")
            return
        }

        try {
            val dappMetadata = DappMetadata(
                name = "Sunflower Land",
                url = "https://sunflowerland.com"
            )

            // Optional: Add Infura API key for read-only RPC calls
            val sdkOptions = SDKOptions(
                infuraAPIKey = INFURA_API_KEY
            )

            ethereum = Ethereum(context, dappMetadata, sdkOptions)
            isInitialized = true
            Log.d(TAG, "MetaMask SDK initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MetaMask SDK: ${e.message}", e)
        }
    }

    /**
     * Connect to MetaMask wallet
     */
    fun connect(callback: (Result<Any>) -> Unit) {
        if (!isInitialized) {
            initialize()
        }

        if (ethereum == null) {
            Log.e(TAG, "Ethereum SDK is null")
            callback(Result.Error(Exception("MetaMask SDK not initialized")))
            return
        }

        try {
            ethereum!!.connect { result ->
                when (result) {
                    is Result.Success.Item -> {
                        Log.d(TAG, "Connected to MetaMask: ${result.value}")
                        callback(result)
                    }
                    is Result.Error -> {
                        Log.e(TAG, "MetaMask connection error: ${result.error.message}")
                        callback(result)
                    }
                    else -> {
                        Log.w(TAG, "Unexpected result type: $result")
                        callback(result)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during connect: ${e.message}", e)
            callback(Result.Error(e))
        }
    }

    /**
     * Disconnect from MetaMask wallet
     */
    fun disconnect(callback: ((Boolean) -> Unit)? = null) {
        if (ethereum == null) {
            Log.w(TAG, "Ethereum SDK is null, skipping disconnect")
            callback?.invoke(false)
            return
        }

        try {
            ethereum = null
            isInitialized = false
            Log.d(TAG, "Disconnected from MetaMask")
            callback?.invoke(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect: ${e.message}", e)
            callback?.invoke(false)
        }
    }

    /**
     * Get currently connected account address
     */
    fun getSelectedAddress(): String? {
        return try {
            ethereum?.selectedAddress
        } catch (e: Exception) {
            Log.e(TAG, "Error getting selected address: ${e.message}")
            null
        }
    }

    /**
     * Get current chain ID
     */
    fun getChainId(): String? {
        return try {
            ethereum?.chainId
        } catch (e: Exception) {
            Log.e(TAG, "Error getting chain ID: ${e.message}")
            null
        }
    }

    /**
     * Sign a message
     */
    fun signMessage(message: String, address: String, callback: (Result<Any>) -> Unit) {
        if (ethereum == null) {
            Log.e(TAG, "Ethereum SDK is null")
            callback(Result.Error(Exception("MetaMask SDK not initialized")))
            return
        }

        try {
            ethereum!!.personalSign(message, address) { result ->
                when (result) {
                    is Result.Success.Item -> {
                        Log.d(TAG, "Message signed successfully")
                        callback(result)
                    }
                    is Result.Error -> {
                        Log.e(TAG, "Sign message error: ${result.error.message}")
                        callback(result)
                    }
                    else -> {
                        Log.w(TAG, "Unexpected result type")
                        callback(result)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during signMessage: ${e.message}", e)
            callback(Result.Error(e))
        }
    }

    /**
     * Send a transaction
     */
    fun sendTransaction(
        from: String,
        to: String,
        value: String,
        callback: (Result<Any>) -> Unit
    ) {
        if (ethereum == null) {
            Log.e(TAG, "Ethereum SDK is null")
            callback(Result.Error(Exception("MetaMask SDK not initialized")))
            return
        }

        try {
            ethereum!!.sendTransaction(from, to, value) { result ->
                when (result) {
                    is Result.Success.Item -> {
                        Log.d(TAG, "Transaction sent: ${result.value}")
                        callback(result)
                    }
                    is Result.Error -> {
                        Log.e(TAG, "Send transaction error: ${result.error.message}")
                        callback(result)
                    }
                    else -> {
                        Log.w(TAG, "Unexpected result type")
                        callback(result)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during sendTransaction: ${e.message}", e)
            callback(Result.Error(e))
        }
    }

    /**
     * Send a custom RPC request
     */
    fun sendRequest(request: EthereumRequest, callback: (Result<Any>) -> Unit) {
        if (ethereum == null) {
            Log.e(TAG, "Ethereum SDK is null")
            callback(Result.Error(Exception("MetaMask SDK not initialized")))
            return
        }

        try {
            ethereum!!.sendRequest(request) { result ->
                when (result) {
                    is Result.Success.Item -> {
                        Log.d(TAG, "RPC request successful")
                        callback(result)
                    }
                    is Result.Error -> {
                        Log.e(TAG, "RPC request error: ${result.error.message}")
                        callback(result)
                    }
                    else -> {
                        Log.w(TAG, "Unexpected result type")
                        callback(result)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during sendRequest: ${e.message}", e)
            callback(Result.Error(e))
        }
    }

    /**
     * Check if MetaMask is initialized and ready
     */
    fun isReady(): Boolean = isInitialized && ethereum != null
}
