package com.sunflowerland.mobile.wallet;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;

/**
 * Manages wallet preferences (default wallet selection).
 */
public class WalletPreferenceManager {
    private static final String PREF_DEFAULT_WALLET = "default_wallet";
    private static final String PREF_DEFAULT_WALLET_NAME = "default_wallet_name";

    /**
     * Save the default wallet preference.
     */
    public static void setDefaultWallet(Context context, String walletId, String walletName) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_DEFAULT_WALLET, walletId);
        editor.putString(PREF_DEFAULT_WALLET_NAME, walletName);
        editor.apply();
    }

    /**
     * Get the default wallet ID.
     * Returns null if no default wallet is set.
     */
    public static String getDefaultWalletId(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(PREF_DEFAULT_WALLET, null);
    }

    /**
     * Get the default wallet name.
     * Returns null if no default wallet is set.
     */
    public static String getDefaultWalletName(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(PREF_DEFAULT_WALLET_NAME, null);
    }

    /**
     * Clear the default wallet preference.
     */
    public static void clearDefaultWallet(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(PREF_DEFAULT_WALLET);
        editor.remove(PREF_DEFAULT_WALLET_NAME);
        editor.apply();
    }

    /**
     * Check if a default wallet is set.
     */
    public static boolean hasDefaultWallet(Context context) {
        return getDefaultWalletId(context) != null;
    }
}
