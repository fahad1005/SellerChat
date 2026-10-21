# SellerChat ProGuard/R8 rules. R8 full mode is on (gradle.properties).
# Most libraries (Room, Compose, play-services-ads, ZXing) ship their own
# consumer rules; the keeps below cover what they cannot infer.

# Room: keep entities, DAOs and the generated database implementation.
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class com.fahad1005.sellerchat.data.** { *; }

# ZXing core: the QR writer is reached by reflection-free direct calls, but
# keep the encode entry points so full-mode shrinking cannot rename them away
# from any future reflective use.
-keep class com.google.zxing.** { *; }

# AdMob / Google Mobile Ads ships extensive consumer ProGuard rules; this is a
# belt-and-braces keep for the SDK's public entry points.
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.**

# Kotlin coroutines (library rules cover most; keep the main dispatcher glue).
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
