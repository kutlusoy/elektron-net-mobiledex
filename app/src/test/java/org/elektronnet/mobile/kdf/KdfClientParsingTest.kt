package org.elektronnet.mobile.kdf

import java.math.BigDecimal
import org.elektronnet.mobile.node.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KdfClientParsingTest {

    private val client = KdfClient(rpcHost = "127.0.0.1", rpcPort = 7823, rpcPasswordProvider = { "K_test" })

    private fun json(text: String): Map<*, *> = Json.parse(text) as Map<*, *>

    // ── order book entries ────────────────────────────────────────────

    @Test
    fun askWithPerSideVolumes() {
        val entry = client.parseOrderEntry(
            json("""{"price":{"decimal":"0.001","fraction":{"numer":"1","denom":"1000"}},
                     "base_max_volume":{"decimal":"0.2"},"rel_max_volume":{"decimal":"0.0002"},
                     "base_min_volume":{"decimal":"0.1"},"uuid":"abc"}"""),
            isAsk = true,
        )!!
        assertEquals(0, BigDecimal("0.001").compareTo(entry.price))
        assertEquals(0, BigDecimal("0.2").compareTo(entry.maxVolume))
        assertEquals(0, BigDecimal("0.0002").compareTo(entry.relVolume))
        assertEquals("abc", entry.uuid)
    }

    @Test
    fun legacyAskVolumeIsInTheBaseCoin() {
        val entry = client.parseOrderEntry(
            json("""{"price":{"decimal":"0.002"},"maxvolume":{"decimal":"0.5"}}"""),
            isAsk = true,
        )!!
        assertEquals(0, BigDecimal("0.5").compareTo(entry.maxVolume))
        assertEquals(0, BigDecimal("0.001").compareTo(entry.relVolume))
    }

    @Test
    fun legacyBidVolumeIsInTheRelCoinAndIsConverted() {
        val entry = client.parseOrderEntry(
            json("""{"price":{"decimal":"0.005"},"maxvolume":{"decimal":"0.01"}}"""),
            isAsk = false,
        )!!
        assertEquals(0, BigDecimal("2").compareTo(entry.maxVolume))
        assertEquals(0, BigDecimal("0.01").compareTo(entry.relVolume))
    }

    @Test
    fun addressCanBeANestedObject() {
        val entry = client.parseOrderEntry(
            json("""{"price":"0.001","maxvolume":"1","address":{"address_data":"tb1qexample"}}"""),
            isAsk = true,
        )!!
        assertEquals("tb1qexample", entry.address)
    }

    @Test
    fun addressCanBeAPlainStringAndMayBeMissing() {
        val withAddress = client.parseOrderEntry(json("""{"price":"1","volume":"1","address":"tb1qplain"}"""), true)!!
        assertEquals("tb1qplain", withAddress.address)
        val without = client.parseOrderEntry(json("""{"price":"1","volume":"1"}"""), true)!!
        assertEquals("", without.address)
    }

    @Test
    fun nonObjectsAreIgnored() {
        assertNull(client.parseOrderEntry("nonsense", true))
        assertNull(client.parseOrderEntry(null, false))
    }

    // ── swap status ───────────────────────────────────────────────────

    private fun swapResult(state: String, myCoin: String = "tELEK", otherCoin: String = "tBTC"): Map<*, *> =
        json(
            """
            {"swap_type":"TakerV1","swap_data":{
              "uuid":"c84dcb0e-0fad-4a1e-8214-f21557799427",
              "my_info":{"my_coin":"$myCoin","other_coin":"$otherCoin",
                         "my_amount":"0.2","other_amount":"0.00014"},
              "events":[
                {"event":{"type":"Started","timestamp":1,"data":{"secret_hash":"f5a2ced0"}}},
                {"event":{"type":"Negotiated","timestamp":2}},
                {"event":{"type":"$state","timestamp":3}}
              ]
            }}
            """.trimIndent(),
        )

    @Test
    fun finishedSwapCarriesBothSidesAndTheSecretHash() {
        val status = client.parseSwapStatus("c84dcb0e-0fad-4a1e-8214-f21557799427", swapResult("Finished"))
        assertEquals("Finished", status.state)
        assertEquals("tELEK", status.myCoin)
        assertEquals("tBTC", status.otherCoin)
        assertEquals(0, BigDecimal("0.2").compareTo(status.myAmount))
        assertEquals(0, BigDecimal("0.00014").compareTo(status.otherAmount))
        assertEquals("f5a2ced0", status.secretHash)
        assertEquals(3, status.events.size)
    }

    @Test
    fun failedAndRefundedStates() {
        assertEquals("Failed", client.parseSwapStatus("u", swapResult("Failed")).state)
        assertEquals("Refunded", client.parseSwapStatus("u", swapResult("Refunded")).state)
    }

    @Test
    fun unfinishedSwapStaysRunning() {
        val status = client.parseSwapStatus("u", swapResult("MakerPaymentSpent"))
        assertEquals("Running", status.state)
        assertEquals(3, status.events.size)
    }

    @Test
    fun makerSideFallsBackToTheSwapTopLevelAmounts() {
        val result = json(
            """
            {"swap_data":{"uuid":"m1","maker_coin":"tELEK","taker_coin":"tBTC",
              "maker_amount":"1.5","taker_amount":"0.001",
              "events":[{"event":{"type":"Finished","timestamp":1}}]}}
            """.trimIndent(),
        )
        val status = client.parseSwapStatus("m1", result)
        assertEquals("tELEK", status.myCoin)
        assertEquals(0, BigDecimal("1.5").compareTo(status.myAmount))
        assertEquals("tBTC", status.otherCoin)
        assertNull(status.secretHash)
    }

    @Test
    fun errorsAreExposed() {
        val result = json(
            """
            {"swap_data":{"uuid":"e1","events":[],"my_info":{"my_coin":"tELEK","my_amount":"1"}},
             "error_events":["TakerFeeSendFailed"]}
            """.trimIndent(),
        )
        val status = client.parseSwapStatus("e1", result)
        assertNotNull(status.error)
        assertTrue(status.error!!.contains("TakerFeeSendFailed"))
    }

    @Test
    fun resultWithoutSwapDataIsTolerated() {
        val status = client.parseSwapStatus("x", json("""{"uuid":"x"}"""))
        assertEquals("x", status.uuid)
        assertTrue(status.events.isEmpty())
    }

    // ── enabled coins ─────────────────────────────────────────────────

    @Test
    fun enabledCoinsReadsTheTickerList() {
        val reply = json("""{"mmrpc":"2.0","result":{"coins":[{"ticker":"tELEK"},{"ticker":"SEPOLIAETH"}]}}""")
        val result = reply["result"] as Map<*, *>
        val tickers = (result["coins"] as List<*>).mapNotNull { (it as Map<*, *>)["ticker"]?.toString() }
        assertEquals(listOf("tELEK", "SEPOLIAETH"), tickers)
    }
}
