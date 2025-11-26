package com.sfl.browser.wallet;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

/**
 * Detects which crypto wallets are installed on the device.
 * Returns only installed wallets that the app supports.
 */
public class InstalledWallet {
    private String id;
    private String name;
    private String packageName;
    private String connectionMethod;

    public InstalledWallet(String id, String name, String packageName, String connectionMethod) {
        this.id = id;
        this.name = name;
        this.packageName = packageName;
        this.connectionMethod = connectionMethod;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getConnectionMethod() {
        return connectionMethod;
    }
}
