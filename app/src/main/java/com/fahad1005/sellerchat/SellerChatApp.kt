package com.fahad1005.sellerchat

import android.app.Application
import com.fahad1005.sellerchat.notify.FollowUps
import com.google.android.gms.ads.MobileAds

class SellerChatApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // AdMob init for the single Home banner. Sample app ID until
        // TODO(FAHAD) is resolved in AndroidManifest.xml + README.
        MobileAds.initialize(this) {}
        // Follow-up reminder channel (v2). Creating a channel needs no
        // permission; notifications are only ever posted after the
        // user asks for a follow-up and grants POST_NOTIFICATIONS.
        FollowUps.createChannel(this)
    }
}
