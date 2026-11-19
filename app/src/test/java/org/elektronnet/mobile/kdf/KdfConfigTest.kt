package org.elektronnet.mobile.kdf

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

class KdfConfigTest {

    private fun configuredTickers(isTestnet: Boolean): List<String> {
        val conf = KdfConfig.buildConfig(
            kdfDir = File(System.getProperty("java.io.tmpdir"), "kdf-config-test"),
            isTestnet = isTestnet,
            passphrase = "test-passphrase-0123456789abcdef",
            rpcPassword = "K_0123456789abcdef0123456789abcdef",
        )
        return (conf["coins"] as List<*>).map { (it as Map<*, *>)["coin"].toString() }
    }

    @Test
    fun testnetActivationListMatchesConfiguration() {
        assertEquals(
            configuredTickers(true),
            KdfConfig.coinsFor(true, emptyList()).map { it.ticker },
        )
    }

    @Test
    fun mainnetActivationListMatchesConfiguration() {
        assertEquals(
            configuredTickers(false),
            KdfConfig.coinsFor(false, emptyList()).map { it.ticker },
        )
    }

    @Test
    fun electrumProfileReachesEveryUtxoCoin() {
        val servers = listOf("192.168.178.21:50005")
        val utxo = KdfConfig.coinsFor(true, servers).filter { it.kind == KdfConfig.KdfCoinKind.UTXO }
        assertEquals(2, utxo.size)
        assert(utxo.all { it.electrum == servers })
    }

    @Test
    fun ethereumCoinsCarryRpcEndpointsAndSwapContract() {
        val eth = KdfConfig.coinsFor(true, emptyList()).first { it.kind == KdfConfig.KdfCoinKind.ETH }
        assertEquals("SEPOLIAETH", eth.ticker)
        assert(eth.rpcUrls.isNotEmpty())
        assertEquals(KdfConfig.SEPOLIA_SWAP_CONTRACT, eth.swapContract)
        assertEquals(18, eth.decimals)
    }

    @Test
    fun pairsCoverEveryCombinationOnce() {
        assertEquals(
            listOf("tELEK" to "tBTC", "tELEK" to "SEPOLIAETH", "tBTC" to "SEPOLIAETH"),
            KdfConfig.pairs(true),
        )
        assertEquals(
            listOf("ELEK" to "BTC", "ELEK" to "ETH", "BTC" to "ETH"),
            KdfConfig.pairs(false),
        )
    }

    @Test
    fun pairsFollowTheCoinOrder() {
        val pairs = KdfConfig.pairs(true)
        assertEquals(3, pairs.size)
        assert(pairs.all { (base, rel) -> base != rel })
        assert(pairs.none { (base, rel) -> (rel to base) in pairs })
    }

    @Test
    fun configCarriesTheNetworkIdAndCoins() {
        val conf = KdfConfig.buildConfig(
            kdfDir = File(System.getProperty("java.io.tmpdir"), "kdf-config-test"),
            isTestnet = true,
            passphrase = "test-passphrase-0123456789abcdef",
            rpcPassword = "K_0123456789abcdef0123456789abcdef",
            extraSeednodes = listOf("192.168.178.26"),
            electrumServers = listOf("192.168.178.21:50002"),
        )
        assertEquals(8888, conf["netid"])
        assertEquals(KdfConfig.DEFAULT_RPC_PORT, conf["rpcport"])
        val seeds = conf["seednodes"] as List<*>
        assert(seeds.contains("192.168.178.26"))
        val coins = conf["coins"] as List<*>
        val utxo = coins.map { it as Map<*, *> }.filter { (it["protocol"] as Map<*, *>)["type"] == "UTXO" }
        assertEquals(2, utxo.size)
        utxo.forEach { coin ->
            val electrum = coin["electrum"] as List<*>
            assertEquals(listOf("192.168.178.21:50002"), electrum.map { (it as Map<*, *>)["url"] })
        }
        assert(conf["i_am_seed"] == null)
    }

    @Test
    fun relayRoleTurnsTheInstallIntoASeed() {
        val conf = KdfConfig.buildConfig(
            kdfDir = File(System.getProperty("java.io.tmpdir"), "kdf-config-test"),
            isTestnet = true,
            passphrase = "test-passphrase-0123456789abcdef",
            rpcPassword = "K_0123456789abcdef0123456789abcdef",
            relayIp = "192.168.178.26",
        )
        assertEquals(true, conf["i_am_seed"])
        assertEquals(false, conf["is_bootstrap_node"])
        assertEquals("192.168.178.26", conf["myipaddr"])
    }

    @Test
    fun mainnetElekFollowsTheProfileWhileBtcKeepsItsOwnServers() {
        val profile = listOf("192.168.178.21:50012")
        val coins = KdfConfig.coinsFor(false, profile)
        assertEquals(profile, coins.first { it.ticker == "ELEK" }.electrum)
        assertEquals(KdfConfig.BTC_ELECTRUM_SERVERS, coins.first { it.ticker == "BTC" }.electrum)
    }

    @Test
    fun mainnetElekFallsBackToThePublicServers() {
        val coins = KdfConfig.coinsFor(false, emptyList())
        assertEquals(KdfConfig.ELEK_ELECTRUM_SERVERS, coins.first { it.ticker == "ELEK" }.electrum)
    }
}
