// SPDX-License-Identifier: GPL-3.0-only
// This file is part of the Elektron DEX line, see LICENSE-GPL-3.0 and doc-elektron/concept-elektron-dex.md.

package org.elektronnet.mobile.ui.market

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.math.BigDecimal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.elektronnet.mobile.ChainConfig
import org.elektronnet.mobile.R
import org.elektronnet.mobile.kdf.KdfState
import org.elektronnet.mobile.kdf.CoinBalance
import org.elektronnet.mobile.kdf.KdfConfig
import org.elektronnet.mobile.kdf.MakerOrder
import org.elektronnet.mobile.kdf.Orderbook
import org.elektronnet.mobile.kdf.orderbookVwap
import org.elektronnet.mobile.service.NodeHost
import org.elektronnet.mobile.ui.tools.ErrorText
import org.elektronnet.mobile.ui.tools.Hint
import org.elektronnet.mobile.ui.tools.ToolPage

/**
 * The front page of the DEX: what the market offers right now.
 *
 * It only projects what the engine reports - the book, the derived VWAP and the
 * user's own resting orders. Placing and taking orders stays on the Swap tab,
 * so this screen carries no trading state of its own.
 */
@Composable
fun MarketScreen(onOpenSwap: (prefillVolume: String?) -> Unit) {
    val scope = rememberCoroutineScope()
    val kdf = NodeHost.kdfSupervisor
    val pairs = remember { KdfConfig.pairs(ChainConfig.isTestnet) }
    var pairIndex by remember { mutableIntStateOf(0) }
    val pair = pairs[pairIndex]
    val base = pair.first
    val rel = pair.second

    var book by remember { mutableStateOf<Orderbook?>(null) }
    var myOrders by remember { mutableStateOf<List<MakerOrder>>(emptyList()) }
    var coinBalances by remember { mutableStateOf<Map<String, CoinBalance>>(emptyMap()) }
    var error by remember { mutableStateOf<String?>(null) }
    var lastUpdate by remember { mutableIntStateOf(0) }

    LaunchedEffect(kdf?.state, pairIndex) {
        if (kdf?.state != KdfState.Running) return@LaunchedEffect
        while (true) {
            val client = kdf.client
            scope.launch(Dispatchers.IO) {
                runCatching {
                    val ob = client.orderbook(base, rel)
                    val orders = client.myOrders().makerOrders.filter { it.base == base && it.rel == rel }
                    val all = KdfConfig.coinsFor(ChainConfig.isTestnet, emptyList()).map { it.ticker }
                        .mapNotNull { ticker -> runCatching { ticker to client.myBalance(ticker) }.getOrNull() }
                        .toMap()
                    withContext(Dispatchers.Main) {
                        book = ob
                        myOrders = orders
                        coinBalances = all
                        error = null
                        lastUpdate++
                    }
                }.onFailure {
                    withContext(Dispatchers.Main) { error = it.message }
                }
            }
            delay(3_000)
        }
    }

    ToolPage(title = stringResource(R.string.market_title), onBack = {}, showBack = false) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            pairs.forEachIndexed { index, p ->
                val selected = index == pairIndex
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f),
                ) {
                    TextButton(onClick = { pairIndex = index }) {
                        Text(
                            "${p.first}/${p.second}",
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val running = kdf?.state is KdfState.Running
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (running) Color(0xFF4CAF50) else Color.Gray)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (running) stringResource(R.string.market_live) else stringResource(R.string.market_offline),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (kdf?.state !is KdfState.Running) {
                    TextButton(onClick = { onOpenSwap(null) }) { Text(stringResource(R.string.market_start_engine)) }
                } else if (coinBalances.isNotEmpty()) {
                    Text(
                        stringResource(R.string.market_coins, coinBalances.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (kdf?.state is KdfState.Running && coinBalances.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                coinBalances.entries.forEach { (ticker, value) ->
                    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceVariant) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(ticker, style = MaterialTheme.typography.labelSmall)
                            Text(
                                value.balance.toPlainString(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }

        val asks = book?.asks.orEmpty()
        val bids = book?.bids.orEmpty()
        val vwap = orderbookVwap(asks, bids)

        if (vwap != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.market_vwap_title), style = MaterialTheme.typography.labelMedium)
                    Text(
                        "${vwap.price.toPlainString()} $rel",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        stringResource(R.string.market_vwap_depth, vwap.baseVolume.toPlainString(), base),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.market_col_price, rel), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.market_col_volume, base), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider()
                if (asks.isEmpty() && bids.isEmpty()) {
                    Hint(stringResource(R.string.market_empty))
                }
                asks.take(6).forEach { ask ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenSwap(ask.maxVolume.toPlainString()) },
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(ask.price.toPlainString(), color = Color(0xFFE53935), fontFamily = FontFamily.Monospace)
                        Text(ask.maxVolume.toPlainString(), fontFamily = FontFamily.Monospace)
                    }
                }
                if (asks.isNotEmpty() && bids.isNotEmpty()) HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                bids.take(6).forEach { bid ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onOpenSwap(bid.maxVolume.toPlainString()) },
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(bid.price.toPlainString(), color = Color(0xFF43A047), fontFamily = FontFamily.Monospace)
                        Text(bid.maxVolume.toPlainString(), fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        error?.let { ErrorText(it) }

        if (myOrders.isNotEmpty()) {
            Text(stringResource(R.string.market_my_orders), style = MaterialTheme.typography.titleMedium)
            myOrders.forEach { order ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                "${order.price.toPlainString()} $rel",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                stringResource(R.string.market_order_volume, order.maxBaseVol.toPlainString(), base),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        TextButton(onClick = {
                            scope.launch(Dispatchers.IO) {
                                runCatching { kdf?.client?.cancelOrder(order.uuid) }
                            }
                        }) { Text(stringResource(R.string.market_cancel)) }
                    }
                }
            }
        }

        Button(onClick = { onOpenSwap(null) }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.market_open_swap), fontWeight = FontWeight.SemiBold)
        }
    }
}
