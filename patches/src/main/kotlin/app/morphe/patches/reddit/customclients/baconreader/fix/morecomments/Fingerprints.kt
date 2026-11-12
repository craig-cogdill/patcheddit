/*
 * Copyright 2026 wchill.
 * https://github.com/wchill/patcheddit
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to this code.
 */

package app.morphe.patches.reddit.customclients.baconreader.fix.morecomments

import app.morphe.patcher.Fingerprint

internal val httpRequestFingerprint = Fingerprint(
    definingClass = "Lcom/onelouder/baconreader/reddit/RedditApi;",
    name = "httpRequest",
    returnType = "Ljava/lang/String;",
    strings = listOf("api/morechildren"),
)
