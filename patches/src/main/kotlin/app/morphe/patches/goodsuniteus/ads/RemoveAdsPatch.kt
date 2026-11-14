package app.morphe.patches.goodsuniteus.ads

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.goodsuniteus.GoodsUniteUsCompatible
import app.morphe.util.returnEarly

@Suppress("unused")
val removeAdsPatch = bytecodePatch(
    name = "Remove ads",
    description = "Removes banner, list, splash, and interstitial ads. Opt-in rewarded ads in the games stay available.",
    default = true,
) {
    compatibleWith(*GoodsUniteUsCompatible)

    execute {
        adsDisabledFingerprint.method.returnEarly(true)
        interstitialLoadAdFingerprint.method.returnEarly()
    }
}
