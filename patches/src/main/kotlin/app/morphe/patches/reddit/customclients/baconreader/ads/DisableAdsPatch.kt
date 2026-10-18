/*
 * Copyright 2026 wchill.
 * https://github.com/wchill/patcheddit
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to this code.
 */

package app.morphe.patches.reddit.customclients.baconreader.ads

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.reddit.customclients.baconreader.BaconReaderFreeCompatible
import app.morphe.util.returnEarly

@Suppress("unused")
val disableAdsPatch = bytecodePatch(
    name = "Disable ads",
    description = "Disables ads and unlocks premium features, matching the Premium build.",
    default = true
) {
    compatibleWith(*BaconReaderFreeCompatible)

    execute {
        showAdsFingerprint.method.returnEarly(false)
        isProFingerprint.method.returnEarly(true)
    }
}
