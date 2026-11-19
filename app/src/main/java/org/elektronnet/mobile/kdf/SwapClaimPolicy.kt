// SPDX-License-Identifier: GPL-3.0-only
// This file is part of the Elektron DEX line, see LICENSE-GPL-3.0 and doc-elektron/concept-elektron-dex.md.

package org.elektronnet.mobile.kdf

import java.math.BigDecimal

enum class ClaimAction {
    /** Move the received amount into the mapped wallet. */
    Claim,

    /** The coin could get a wallet, but the app has none: ask the user. */
    AskForWallet,

    /** The node does not carry that chain: the swap wallet stays the wallet. */
    KeepInMesh,
}

data class ClaimDecision(
    val action: ClaimAction,
    val coin: String,
    val amount: BigDecimal,
    val wallet: String? = null,
)

/**
 * Decides what happens to the received side of a finished swap. Pure so the
 * rule can be tested without a node: the engine reports `my_*` as what this
 * wallet receives, `other_*` as what it paid.
 */
object SwapClaimPolicy {

    fun decide(
        swap: SwapStatus,
        claimed: Set<String>,
        walletForCoin: (String) -> String?,
        canNodeHold: (String) -> Boolean,
    ): ClaimDecision? {
        if (swap.state != "Finished") return null
        if (swap.uuid in claimed) return null
        // the engine reports my_coin/my_amount as the side this wallet receives
        if (swap.myAmount <= BigDecimal.ZERO) return null
        val coin = swap.myCoin
        val wallet = walletForCoin(coin)
        return when {
            wallet != null -> ClaimDecision(ClaimAction.Claim, coin, swap.myAmount, wallet)
            canNodeHold(coin) -> ClaimDecision(ClaimAction.AskForWallet, coin, swap.myAmount)
            else -> ClaimDecision(ClaimAction.KeepInMesh, coin, swap.myAmount)
        }
    }

    /** Only the received amount moves, the rest stays as float in the swap wallet. */
    fun amountText(amount: BigDecimal): String = amount.stripTrailingZeros().toPlainString()
}
