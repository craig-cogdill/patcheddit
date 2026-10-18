/*
 * Copyright 2026 wchill.
 * https://github.com/wchill/patcheddit
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to this code.
 */

package app.morphe.patches.reddit.customclients.baconreader.ads

import app.morphe.patcher.Fingerprint

// Every ad placement (banner, native feed ads, interstitials) first asks this method.
// The premium flavor compiles it to a constant `false`.
internal val showAdsFingerprint = Fingerprint(
    definingClass = "Lcom/onelouder/baconreader/ads/baconads/AdHelper;",
    name = "showAds",
)

// Gates upgrade prompts and premium-only settings.
// The premium flavor compiles it to a constant `true`.
internal val isProFingerprint = Fingerprint(
    definingClass = "Lcom/onelouder/baconreader/ads/billing/PurchaseHandler;",
    name = "isPro",
)
