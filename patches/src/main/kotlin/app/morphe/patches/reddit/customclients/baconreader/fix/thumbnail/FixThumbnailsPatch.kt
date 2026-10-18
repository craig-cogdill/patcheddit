/*
 * Copyright 2026 wchill.
 * https://github.com/wchill/patcheddit
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to this code.
 */

package app.morphe.patches.reddit.customclients.baconreader.fix.thumbnail

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.reddit.customclients.baconreader.BaconReaderCompatible
import app.morphe.patches.reddit.customclients.baconreader.misc.extension.sharedExtensionPatch
import app.morphe.util.getReference
import app.morphe.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/morphe/extension/baconreader/FixThumbnailsPatch;"

@Suppress("unused")
val fixThumbnailsPatch = bytecodePatch(
    name = "Fix post thumbnails",
    description = "Fixes missing post thumbnails by unescaping their URLs.",
    default = true
) {
    dependsOn(sharedExtensionPatch)

    compatibleWith(*BaconReaderCompatible)

    // Reddit HTML-escapes '&' in the thumbnail field. Unescape it and write it back
    // to the Link object, so every later reader of link.thumbnail sees the fixed URL.
    execute {
        canShowThumbnailFingerprint.method.apply {
            val index = indexOfFirstInstructionOrThrow {
                opcode == Opcode.IGET_OBJECT && getReference<FieldReference>()?.name == "thumbnail"
            }
            val instruction = getInstruction<TwoRegisterInstruction>(index)
            val urlRegister = instruction.registerA
            val linkRegister = instruction.registerB

            addInstructions(
                index + 1,
                """
                    invoke-static { v$urlRegister }, $EXTENSION_CLASS_DESCRIPTOR->fixUrl(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v$urlRegister
                    iput-object v$urlRegister, v$linkRegister, Lcom/onelouder/baconreader/reddit/Link;->thumbnail:Ljava/lang/String;
                """
            )
        }
    }
}
