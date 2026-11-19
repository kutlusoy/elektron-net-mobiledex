package org.elektronnet.mobile.kdf

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SwapClaimPolicyTest {

    private fun swap(
        uuid: String = "c84dcb0e-0fad-4a1e-8214-f21557799427",
        state: String = "Finished",
        myCoin: String = "tELEK",
        myAmount: String = "0.2",
        otherCoin: String = "tBTC",
        otherAmount: String = "0.00014",
    ) = SwapStatus(
        uuid = uuid,
        state = state,
        error = null,
        events = emptyList(),
        myCoin = myCoin,
        otherCoin = otherCoin,
        myAmount = BigDecimal(myAmount),
        otherAmount = BigDecimal(otherAmount),
        isPruned = false,
        secretHash = null,
    )

    private val anyWallet: (String) -> String? = { "elektrontestnet" }
    private val noWallet: (String) -> String? = { null }
    private val utxo: (String) -> Boolean = { true }
    private val noChain: (String) -> Boolean = { false }

    @Test
    fun finishedSwapMovesTheReceivedSide() {
        val decision = SwapClaimPolicy.decide(swap(), emptySet(), anyWallet, utxo)!!
        assertEquals(ClaimAction.Claim, decision.action)
        assertEquals("tELEK", decision.coin)
        assertEquals(0, BigDecimal("0.2").compareTo(decision.amount))
        assertEquals("elektrontestnet", decision.wallet)
    }

    @Test
    fun thePaidSideIsNeverClaimed() {
        val decision = SwapClaimPolicy.decide(swap(), emptySet(), anyWallet, utxo)!!
        assertEquals("tELEK", decision.coin)
        assertEquals(false, decision.coin == "tBTC")
    }

    @Test
    fun runningSwapsAreIgnored() {
        assertNull(SwapClaimPolicy.decide(swap(state = "Running"), emptySet(), anyWallet, utxo))
        assertNull(SwapClaimPolicy.decide(swap(state = "MakerPaymentSpent"), emptySet(), anyWallet, utxo))
    }

    @Test
    fun refundedAndFailedAreNotClaimed() {
        assertNull(SwapClaimPolicy.decide(swap(state = "Failed"), emptySet(), anyWallet, utxo))
        assertNull(SwapClaimPolicy.decide(swap(state = "Refunded"), emptySet(), anyWallet, utxo))
    }

    @Test
    fun alreadyClaimedIsSkipped() {
        val claimed = setOf("c84dcb0e-0fad-4a1e-8214-f21557799427")
        assertNull(SwapClaimPolicy.decide(swap(), claimed, anyWallet, utxo))
    }

    @Test
    fun zeroAmountIsSkipped() {
        assertNull(SwapClaimPolicy.decide(swap(myAmount = "0"), emptySet(), anyWallet, utxo))
        assertNull(SwapClaimPolicy.decide(swap(myAmount = "0.00000000"), emptySet(), anyWallet, utxo))
    }

    @Test
    fun coinWithoutWalletAsksWhenTheNodeCouldHoldIt() {
        val decision = SwapClaimPolicy.decide(swap(), emptySet(), noWallet, utxo)!!
        assertEquals(ClaimAction.AskForWallet, decision.action)
        assertEquals("tELEK", decision.coin)
    }

    @Test
    fun coinTheNodeCannotHoldStaysInTheMesh() {
        val decision = SwapClaimPolicy.decide(
            swap(myCoin = "SEPOLIAETH", myAmount = "0.5", otherCoin = "tELEK", otherAmount = "1"),
            emptySet(),
            noWallet,
            noChain,
        )!!
        assertEquals(ClaimAction.KeepInMesh, decision.action)
        assertEquals("SEPOLIAETH", decision.coin)
    }

    @Test
    fun amountTextDropsTrailingZeros() {
        assertEquals("0.2", SwapClaimPolicy.amountText(BigDecimal("0.20000000")))
        assertEquals("1", SwapClaimPolicy.amountText(BigDecimal("1.00000000")))
        assertEquals("0.00014", SwapClaimPolicy.amountText(BigDecimal("0.00014000")))
        assertEquals("12", SwapClaimPolicy.amountText(BigDecimal("12")))
    }
}
