/*
 * Copyright 2026 wchill.
 * https://github.com/wchill/patcheddit
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to this code.
 */

package app.morphe.patches.reddit.customclients.baconreader.fix.thumbnail

import app.morphe.util.addInstructionsAtControlFlowLabel
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

    // Reddit HTML-escapes '&' in the thumbnail field. Right before the method reads
    // link.thumbnail, hand the Link object to the extension, which unescapes the field
    // in place. The object register is taken from the read itself, so no assumption is
    // made about which register holds the Link (the compiler reuses p0 for the result).
    execute {
        canShowThumbnailFingerprint.method.apply {
            val index = indexOfFirstInstructionOrThrow {
                opcode == Opcode.IGET_OBJECT && getReference<FieldReference>()?.name == "thumbnail"
            }
            val linkRegister = getInstruction<TwoRegisterInstruction>(index).registerB

            // The read is a branch target (both NSFW paths jump to it). Insert at the
            // control-flow label so the jumps land on the hook, not past it.
            addInstructionsAtControlFlowLabel(
                index,
                "invoke-static { v$linkRegister }, $EXTENSION_CLASS_DESCRIPTOR->fixLink(Ljava/lang/Object;)V"
            )
        }
    }
}
