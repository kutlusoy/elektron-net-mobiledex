package org.elektronnet.mobile.kdf

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KdfNetProfileTest {

    @Test
    fun readsSeedsElectrumAndRelay() {
        val profile = KdfNetProfile.parse(
            """
            # the phones meet through each other
            seed=192.168.178.26
            seed = 192.168.178.95
            electrum=192.168.178.21:50002
            relay=192.168.178.26
            """.trimIndent(),
        )
        assertEquals(listOf("192.168.178.26", "192.168.178.95"), profile.seeds)
        assertEquals(listOf("192.168.178.21:50002"), profile.electrum)
        assertEquals("192.168.178.26", profile.relayIp)
    }

    @Test
    fun acceptsTheLegacyAliases() {
        val profile = KdfNetProfile.parse(
            """
            peer=192.168.178.26
            seednode=seed.example.org
            electrum_server=electrum.example.org:50002
            relay_ip=192.168.178.26
            """.trimIndent(),
        )
        assertEquals(listOf("192.168.178.26", "seed.example.org"), profile.seeds)
        assertEquals(listOf("electrum.example.org:50002"), profile.electrum)
        assertEquals("192.168.178.26", profile.relayIp)
    }

    @Test
    fun ignoresCommentsBlanksAndUnknownKeys() {
        val profile = KdfNetProfile.parse(
            """
            
            # only a comment
            unknown=value
            seed=
            =192.168.178.26
            seed=192.168.178.26
            """.trimIndent(),
        )
        assertEquals(listOf("192.168.178.26"), profile.seeds)
        assertTrue(profile.electrum.isEmpty())
        assertNull(profile.relayIp)
    }

    @Test
    fun duplicatesDoNotPileUp() {
        val profile = KdfNetProfile.parse("seed=a\nseed=a\nseed=a\n")
        assertEquals(listOf("a"), profile.seeds)
    }

    @Test
    fun renderRoundTrips() {
        val original = NetworkProfile(
            seeds = listOf("192.168.178.26", " 192.168.178.95 "),
            electrum = listOf("192.168.178.21:50002"),
            relayIp = "192.168.178.26",
        )
        val parsed = KdfNetProfile.parse(KdfNetProfile.render(original))
        assertEquals(listOf("192.168.178.26", "192.168.178.95"), parsed.seeds)
        assertEquals(original.electrum, parsed.electrum)
        assertEquals(original.relayIp, parsed.relayIp)
    }

    @Test
    fun emptyProfileRendersNothing() {
        assertEquals("", KdfNetProfile.render(NetworkProfile()))
    }

    @Test
    fun endpointsNeedHostAndPort() {
        assertTrue(KdfNetProfile.validEndpoint("192.168.178.21:50002"))
        assertTrue(KdfNetProfile.validEndpoint("electrum.example.org:50002"))
        assertTrue(KdfNetProfile.validEndpoint("127.0.0.1:50005"))
        assertFalse(KdfNetProfile.validEndpoint("192.168.178.21"))
        assertFalse(KdfNetProfile.validEndpoint("192.168.178.21:"))
        assertFalse(KdfNetProfile.validEndpoint(":50002"))
        assertFalse(KdfNetProfile.validEndpoint("host:0"))
        assertFalse(KdfNetProfile.validEndpoint("host:70000"))
        assertFalse(KdfNetProfile.validEndpoint("host:abc"))
    }
}
