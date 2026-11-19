// SPDX-License-Identifier: GPL-3.0-only
// This file is part of the Elektron DEX line, see LICENSE-GPL-3.0 and doc-elektron/concept-elektron-dex.md.

package org.elektronnet.mobile.kdf

/**
 * The engine nests its reasons behind "<file>:<line>] " markers and answers with
 * a JSON body; the last marker carries the sentence a user can act on.
 */
object KdfErrorText {

    fun readable(message: String?): String {
        var text = message.orEmpty()
        text = text.substringAfter("\"error\":\"", text)
        text = text.substringBeforeLast("\"", text)
        text = text.replace("KDF RPC error HTTP 500: ", "")
        text = text.split("] ").lastOrNull().orEmpty()
        return text.trim()
    }
}
