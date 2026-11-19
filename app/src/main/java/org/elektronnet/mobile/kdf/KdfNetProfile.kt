// SPDX-License-Identifier: GPL-3.0-only
// This file is part of the Elektron DEX line, see LICENSE-GPL-3.0 and doc-elektron/concept-elektron-dex.md.

package org.elektronnet.mobile.kdf

/** P2P seeds, indexer and relay role of this install. */
data class NetworkProfile(
    val seeds: List<String> = emptyList(),
    val electrum: List<String> = emptyList(),
    val relayIp: String? = null,
)

/**
 * Reads and writes the network profile of this install (`kdf/net.txt`), one
 * key per line: `seed=` (repeatable), `electrum=` (repeatable), `relay=`.
 * Kept free of Android types so the parsing is testable.
 */
object KdfNetProfile {

    fun parse(text: String): NetworkProfile {
        val seeds = mutableListOf<String>()
        val electrum = mutableListOf<String>()
        var relay: String? = null
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            val key = line.substringBefore('=', "").trim().lowercase()
            val value = line.substringAfter('=', "").trim()
            if (value.isEmpty()) continue
            when (key) {
                "seed", "seednode", "peer" -> if (value !in seeds) seeds += value
                "electrum", "electrum_server" -> if (value !in electrum) electrum += value
                "relay", "relay_ip" -> relay = value
            }
        }
        return NetworkProfile(seeds = seeds, electrum = electrum, relayIp = relay)
    }

    /** Round trip form for the settings screen; keeps the order the user sees. */
    fun render(profile: NetworkProfile): String = buildString {
        profile.seeds.filter { it.isNotBlank() }.forEach { appendLine("seed=" + it.trim()) }
        profile.electrum.filter { it.isNotBlank() }.forEach { appendLine("electrum=" + it.trim()) }
        profile.relayIp?.trim()?.takeIf { it.isNotEmpty() }?.let { appendLine("relay=$it") }
    }

    /** True when the line is a host:port pair the engine can dial. */
    fun validEndpoint(value: String): Boolean {
        val text = value.trim()
        val host = text.substringBeforeLast(':', "")
        val port = text.substringAfterLast(':', "")
        if (host.isEmpty() || port.isEmpty()) return false
        val number = port.toIntOrNull() ?: return false
        return number in 1..65535
    }
}
