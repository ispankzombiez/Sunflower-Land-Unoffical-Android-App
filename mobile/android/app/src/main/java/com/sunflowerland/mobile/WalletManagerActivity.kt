package com.sunflowerland.mobile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

import com.sunflowerland.mobile.wallet.WalletConnectionHandler
import com.sunflowerland.mobile.wallet.WalletDetector
import com.sunflowerland.mobile.wallet.WalletStatePersistence
import com.sunflowerland.mobile.wallet.InstalledWallet

class WalletManagerActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "WalletManager"
    }
    
    private lateinit var walletDetector: WalletDetector
    private lateinit var connectionHandler: WalletConnectionHandler
    private lateinit var persistence: WalletStatePersistence
    private lateinit var walletContainer: LinearLayout
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var noWalletsMessage: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet_manager)
        
        // Initialize utility classes
        walletDetector = WalletDetector()
        connectionHandler = WalletConnectionHandler(this)
        persistence = WalletStatePersistence(this)
        
        // Set up toolbar
        val toolbar: Toolbar = findViewById(R.id.wallet_toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Connect Wallet"
        }
        
        // Get UI elements
        walletContainer = findViewById(R.id.wallet_container)
        loadingIndicator = findViewById(R.id.loading_indicator)
        noWalletsMessage = findViewById(R.id.no_wallets_message)
        
        // Load and display installed wallets
        loadInstalledWallets()
    }
    
    private fun loadInstalledWallets() {
        try {
            loadingIndicator.visibility = View.VISIBLE
            walletContainer.removeAllViews()
            
            val installedWallets = walletDetector.getInstalledWallets(this)
            
            if (installedWallets.isEmpty()) {
                loadingIndicator.visibility = View.GONE
                noWalletsMessage.visibility = View.VISIBLE
                Log.w(TAG, "No wallets detected on device")
                return
            }
            
            loadingIndicator.visibility = View.GONE
            noWalletsMessage.visibility = View.GONE
            
            // Create a button for each installed wallet
            for (wallet in installedWallets) {
                addWalletButton(wallet)
            }
            
            Log.d(TAG, "Loaded ${installedWallets.size} installed wallets")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading wallets", e)
            loadingIndicator.visibility = View.GONE
            Toast.makeText(this, "Error loading wallets", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun addWalletButton(wallet: InstalledWallet) {
        // Create button container
        val buttonContainer = LinearLayout(this)
        buttonContainer.orientation = LinearLayout.VERTICAL
        val containerParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        containerParams.setMargins(16, 12, 16, 12)
        buttonContainer.layoutParams = containerParams
        
        // Create wallet button
        val walletButton = Button(this)
        walletButton.text = wallet.name
        walletButton.tag = wallet.id
        val buttonParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        walletButton.layoutParams = buttonParams
        
        // Set click listener
        walletButton.setOnClickListener { connectToWallet(wallet) }
        
        buttonContainer.addView(walletButton)
        
        // Add status text if already connected to this wallet
        val connectedWalletConnection = persistence.getWalletConnection()
        val connectedWalletId = connectedWalletConnection?.walletId
        if (wallet.id == connectedWalletId) {
            val statusText = TextView(this)
            statusText.text = "✓ Connected"
            statusText.textSize = 12f
            statusText.setTextColor(resources.getColor(android.R.color.holo_green_dark))
            val statusParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            statusParams.setMargins(16, 4, 16, 0)
            statusText.layoutParams = statusParams
            buttonContainer.addView(statusText)
        }
        
        walletContainer.addView(buttonContainer)
    }
    
    private fun connectToWallet(wallet: InstalledWallet) {
        Log.d(TAG, "Attempting to connect to wallet: ${wallet.name}")
        
        try {
            // Use WalletConnectionHandler to initiate connection
            connectionHandler.connectWallet(wallet.id, object : WalletConnectionHandler.ConnectionCallback {
                override fun onSuccess(address: String?, chainId: String?) {
                    Log.d(TAG, "Successfully connected to ${wallet.name}")
                    handleConnectionSuccess(wallet, address, chainId)
                }
                
                override fun onError(error: String) {
                    Log.e(TAG, "Failed to connect to ${wallet.name}: $error")
                    handleConnectionError(wallet, error)
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to wallet", e)
            Toast.makeText(this, "Connection failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun handleConnectionSuccess(wallet: InstalledWallet, address: String?, chainId: String?) {
        Log.d(TAG, "Connection successful for ${wallet.name}: $address")
        
        // Show success message
        val message = "Connected to ${wallet.name}\n" + 
                        (if (address != null) "Address: ${shortenAddress(address)}" else "")
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        
        // Reload UI to show connected status
        loadInstalledWallets()
        
        // Send result back to calling activity/game
        val resultIntent = Intent()
        resultIntent.putExtra("wallet_id", wallet.id)
        resultIntent.putExtra("wallet_name", wallet.name)
        resultIntent.putExtra("address", address)
        resultIntent.putExtra("chain_id", chainId)
        setResult(RESULT_OK, resultIntent)
        
        // Close this activity after short delay to show UI update
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
            { finish() },
            1500
        )
    }
    
    private fun handleConnectionError(wallet: InstalledWallet, error: String) {
        Log.e(TAG, "Connection error for ${wallet.name}: $error")
        Toast.makeText(
            this,
            "Failed to connect to ${wallet.name}: $error",
            Toast.LENGTH_LONG
        ).show()
    }
    
    private fun shortenAddress(address: String): String {
        return if (address.length < 10) {
            address
        } else {
            "${address.substring(0, 6)}...${address.substring(address.length - 4)}"
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
