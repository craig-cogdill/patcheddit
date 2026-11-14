package app.morphe.patches.goodsuniteus.ads

import app.morphe.patcher.Fingerprint

// The app's own "ads off" override for testing. AdGate.shouldShowAds(), the splash
// banner, the search banner, and SubscriptionUtils all read it. Normally it is always
// false because prefs() returns null in release builds.
internal val adsDisabledFingerprint = Fingerprint(
    definingClass = "Lcom/goodsuniteus/goods/util/GuuStateOverrides;",
    name = "adsDisabled",
    returnType = "Z",
    parameters = listOf(),
)

// Preloads an interstitial when the ad SDK is ready and on every app foreground.
// No gate, so it keeps fetching ads that can never show once adsDisabled() is true.
internal val interstitialLoadAdFingerprint = Fingerprint(
    definingClass = "Lcom/goodsuniteus/goods/util/InterstitialAdManager;",
    name = "loadAd",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
)
