package app.morphe.patches.goodsuniteus

import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

internal val GoodsUniteUsCompatible = arrayOf(
    Compatibility(
        name = "Goods Unite Us",
        packageName = "com.goodsuniteus.goods",
        targets = listOf(AppTarget(version = "4.0.1")),
    )
)
