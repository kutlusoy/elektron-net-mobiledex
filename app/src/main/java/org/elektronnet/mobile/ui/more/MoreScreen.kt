// SPDX-License-Identifier: GPL-3.0-only
// This file is part of the Elektron DEX line, see LICENSE-GPL-3.0 and doc-elektron/concept-elektron-dex.md.

package org.elektronnet.mobile.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.elektronnet.mobile.R
import org.elektronnet.mobile.ui.tools.Hint
import org.elektronnet.mobile.ui.tools.ToolPage

/**
 * Everything that is not trading: history, node, settings and the tools hub.
 * The DEX keeps Market and Swap in the bottom bar; the wallet-side pages live
 * here so the bar stays short.
 */
@Composable
fun MoreScreen(
    onOpenHistory: () -> Unit,
    onOpenNode: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTools: () -> Unit,
) {
    ToolPage(title = stringResource(R.string.more_title), onBack = {}, showBack = false) {
        MoreEntry(R.string.more_history, R.string.more_history_hint, onOpenHistory)
        MoreEntry(R.string.more_node, R.string.more_node_hint, onOpenNode)
        MoreEntry(R.string.more_tools, R.string.more_tools_hint, onOpenTools)
        MoreEntry(R.string.more_settings, R.string.more_settings_hint, onOpenSettings)
        Hint(stringResource(R.string.more_footer))
    }
}

@Composable
private fun MoreEntry(title: Int, hint: Int, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
