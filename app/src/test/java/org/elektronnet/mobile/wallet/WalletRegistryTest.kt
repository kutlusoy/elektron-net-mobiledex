package org.elektronnet.mobile.wallet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletRegistryTest {
    @Test
    fun askingForTheActiveWalletNeverCreatesOne() {
        val registry = WalletRegistry(MemoryWalletRegistryStore())
        assertEquals(WalletBootstrap.DEFAULT_WALLET, registry.active().name)
        assertTrue(registry.wallets().isEmpty())
        assertEquals(WalletBootstrap.DEFAULT_WALLET, registry.nextName())
    }

    @Test
    fun namesAlreadyOnDiskAreSkipped() {
        val registry = WalletRegistry(MemoryWalletRegistryStore())
        registry.add("elektron", "")
        assertEquals("elektron_3", registry.nextName(setOf("elektron_2")))
        assertEquals("elektron_2", registry.nextName())
    }

    @Test
    fun theActiveWalletIsTheChosenOneOrTheFirst() {
        val registry = WalletRegistry(MemoryWalletRegistryStore())
        registry.add("elektron", "")
        registry.add("elektron_2", "second")
        assertEquals("elektron", registry.active().name)
        registry.setActive("elektron_2")
        assertEquals("elektron_2", registry.active().name)
        registry.remove("elektron_2")
        assertEquals("elektron", registry.active().name)
    }

    @Test
    fun multiCoinInferenceAndPersistenceRoundTrip() {
        val registry = WalletRegistry(MemoryWalletRegistryStore())
        val elekWallet = registry.add("wallet", "My Daily Spending")
        val btcWallet = registry.add("btc_vault", "Bitcoin Cold Storage")
        val customBtc = registry.add("savings", "Emergency Fund", coin = "BTC")

        assertEquals("ELEK", elekWallet.coin)
        assertEquals("BTC", btcWallet.coin)
        assertEquals("BTC", customBtc.coin)

        val loaded = registry.wallets()
        assertEquals(3, loaded.size)
        assertEquals("ELEK", loaded[0].coin)
        assertEquals("BTC", loaded[1].coin)
        assertEquals("BTC", loaded[2].coin)
    }
}
