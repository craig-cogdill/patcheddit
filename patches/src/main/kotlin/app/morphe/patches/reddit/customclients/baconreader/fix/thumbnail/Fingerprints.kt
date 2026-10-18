/*
 * Copyright 2026 wchill.
 * https://github.com/wchill/patcheddit
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to this code.
 */

package app.morphe.patches.reddit.customclients.baconreader.fix.thumbnail

import app.morphe.patcher.Fingerprint

// Every thumbnail load (list view, detail header) asks this first, so fixing the
// URL here fixes it for all of them.
internal val canShowThumbnailFingerprint = Fingerprint(
    definingClass = "Lcom/onelouder/baconreader/utils/Utils;",
    name = "canShowThumbnail",
)
