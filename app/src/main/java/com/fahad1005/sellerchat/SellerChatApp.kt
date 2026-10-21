package com.fahad1005.sellerchat

import android.app.Application
import com.google.android.gms.ads.MobileAds

class SellerChatApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // AdMob init for the single Home banner. Sample app ID until
        // TODO(FAHAD) is resolved in AndroidManifest.xml + README.
        MobileAds.initialize(this) {}
    }
}
