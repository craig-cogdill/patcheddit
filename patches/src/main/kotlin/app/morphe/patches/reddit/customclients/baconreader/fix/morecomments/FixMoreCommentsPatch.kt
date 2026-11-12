/*
 * Copyright 2026 wchill.
 * https://github.com/wchill/patcheddit
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to this code.
 */

package app.morphe.patches.reddit.customclients.baconreader.fix.morecomments

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patches.reddit.customclients.baconreader.BaconReaderCompatible
import app.morphe.patches.reddit.customclients.baconreader.misc.extension.sharedExtensionPatch

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/morphe/extension/baconreader/FixMoreCommentsPatch;"

@Suppress("unused")
val fixMoreCommentsPatch = bytecodePatch(
    name = "Fix loading more comments",
    description = "Fixes \"load more comments\" failing with a 403 error from Reddit.",
    default = true
) {
    dependsOn(sharedExtensionPatch)

    compatibleWith(*BaconReaderCompatible)

    // Reddit blocks /api/morechildren for the RedReader client ID. BaconReader sends every
    // Reddit request through the static RedditApi.httpRequest(Context, RequestMethod, String,
    // Map, SessionToken). At its start, let the extension answer api/morechildren from the
    // comment permalinks instead. A null result falls through to the original code.
    execute {
        httpRequestFingerprint.method.apply {
            addInstructionsWithLabels(
                0,
                """
                    invoke-static/range { p0 .. p4 }, $EXTENSION_CLASS_DESCRIPTOR->loadMoreChildren(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;Ljava/util/Map;Ljava/lang/Object;)Ljava/lang/String;
                    move-result-object v0
                    if-eqz v0, :original
                    return-object v0
                """,
                ExternalLabel("original", getInstruction(0))
            )
        }
    }
}
