// SPDX-License-Identifier: GPL-3.0-only
// This file is part of the Elektron DEX line, see LICENSE-GPL-3.0 and doc-elektron/concept-elektron-dex.md.

package org.elektronnet.mobile.kdf

import java.io.File
import java.security.MessageDigest
import org.elektronnet.mobile.ChainConfig
import org.elektronnet.mobile.wallet.SystemEntropy

object KdfConfig {
    const val DEFAULT_RPC_HOST = "127.0.0.1"
    const val DEFAULT_RPC_PORT = 7823
    const val NETID_TESTNET = 8888
    const val NETID_MAINNET = 2
    const val DEFAULT_SEEDNODE = "wss://elektron-market.onrender.com"

    private const val PASSPHRASE_TAG = "elektron-net/kdf-passphrase/v1"
    private const val PASSPHRASE_PREFIX = "elkkdf1"

    val SEPOLIA_RPC_URLS = listOf(
        "https://ethereum-sepolia-rpc.publicnode.com",
        "https://sepolia.drpc.org",
    )
    const val SEPOLIA_SWAP_CONTRACT = "0xeA6D65434A15377081495a9E7C5893543E7c32cB"

    val MAINNET_ETH_RPC_URLS = listOf(
        "https://ethereum-rpc.publicnode.com",
        "https://eth.drpc.org",
    )
    const val MAINNET_ETH_SWAP_CONTRACT = "0x8500AFc0bc5214728082163326C2FF0C73f4a871"

    val TELEK_ELECTRUM_SERVERS = listOf(
        "127.0.0.1:50005",
    )

    val TBTC_ELECTRUM_SERVERS = listOf(
        "127.0.0.1:50005",
    )

    val ELEK_ELECTRUM_SERVERS = listOf(
        "electrs.elektron-net.org:50002",
        "electrs2.elektron-net.org:50002",
    )

    val BTC_ELECTRUM_SERVERS = listOf(
        "btc.electrum1.cipig.net:10000",
        "btc.electrum2.cipig.net:10000",
    )

    enum class KdfCoinKind { UTXO, ETH, ERC20 }

    /**
     * One coin the engine can hold. UTXO coins run on electrum (no chain data on
     * the phone), ETH and tokens talk to public RPC nodes, so the DEX stays
     * multi-coin without downloading a single block.
     */
    data class KdfCoin(
        val ticker: String,
        val kind: KdfCoinKind,
        val electrum: List<String> = emptyList(),
        val rpcUrls: List<String> = emptyList(),
        val swapContract: String? = null,
        val contractAddress: String? = null,
        val decimals: Int = 8,
    )

    /**
     * The coins this line activates. Mirrors the `coins` array that
     * [buildConfig] writes; `KdfConfigTest` fails when the two drift apart.
     */
    fun coinsFor(isTestnet: Boolean, electrumServers: List<String>): List<KdfCoin> = if (isTestnet) {
        listOf(
            KdfCoin("tELEK", KdfCoinKind.UTXO, electrum = electrumServers),
            KdfCoin("tBTC", KdfCoinKind.UTXO, electrum = electrumServers),
            KdfCoin(
                "SEPOLIAETH",
                KdfCoinKind.ETH,
                rpcUrls = SEPOLIA_RPC_URLS,
                swapContract = SEPOLIA_SWAP_CONTRACT,
                decimals = 18,
            ),
        )
    } else {
        listOf(
            // the profile is this install's indexer; it also covers the private
            // mainnet chain we run for the tests, the public servers are the default
            KdfCoin("ELEK", KdfCoinKind.UTXO, electrum = electrumServers.ifEmpty { ELEK_ELECTRUM_SERVERS }),
            KdfCoin("BTC", KdfCoinKind.UTXO, electrum = BTC_ELECTRUM_SERVERS),
            KdfCoin(
                "ETH",
                KdfCoinKind.ETH,
                rpcUrls = MAINNET_ETH_RPC_URLS,
                swapContract = MAINNET_ETH_SWAP_CONTRACT,
                decimals = 18,
            ),
        )
    }

    /** Every pair the activated coins can trade, in the order the screens show them. */
    fun pairs(isTestnet: Boolean): List<Pair<String, String>> {
        val tickers = coinsFor(isTestnet, emptyList()).map { it.ticker }
        return tickers.flatMapIndexed { index, base -> tickers.drop(index + 1).map { rel -> base to rel } }
    }

    /**
     * Deterministic KDF Passphrase derived from the wallet seed phrase (Task #98).
     * Returns "elkkdf1" + sha256("elektron-net/kdf-passphrase/v1/" + seedText).
     */
    fun deriveKdfPassphrase(seedText: String?): String {
        val text = seedText?.trim().orEmpty()
        if (text.isEmpty()) return ""
        val bytes = "$PASSPHRASE_TAG/$text".toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return PASSPHRASE_PREFIX + digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Validates an RPC password against the strict KDF mm2 password policy:
     * - Length >= 8
     * - At least one digit
     * - At least one lowercase letter
     * - At least one uppercase letter
     * - At least one special / non-alphanumeric character
     * - No 3 identical characters in a row
     * - "password" substring banned (case-insensitive)
     */
    fun isPasswordPolicyOk(pw: String): Boolean {
        if (pw.length < 8) return false
        if (pw.contains("password", ignoreCase = true)) return false
        if (!pw.any { it.isDigit() }) return false
        if (!pw.any { it.isLowerCase() }) return false
        if (!pw.any { it.isUpperCase() }) return false
        if (pw.all { it.isLetterOrDigit() }) return false
        for (i in 0 until pw.length - 2) {
            if (pw[i] == pw[i + 1] && pw[i] == pw[i + 2]) return false
        }
        return true
    }

    /**
     * Generates a policy-compliant RPC password with prefix "K_" followed by 16 random hex bytes.
     */
    /**
     * Per-install passphrase for the swap wallet. A fixed constant here would
     * give every installation on every phone the same keys, so the hot wallet
     * has to be unique per device.
     */
    fun generateKdfPassphrase(entropy: SystemEntropy = SystemEntropy()): String {
        val bytes = entropy.draw(32)
        return "elkkdf1_" + bytes.joinToString("") { "%02x".format(it) }
    }

    fun generateRpcPassword(entropy: SystemEntropy = SystemEntropy()): String {
        while (true) {
            val bytes = entropy.draw(16)
            val hex = bytes.joinToString("") { "%02x".format(it) }
            val candidate = "K_$hex"
            if (isPasswordPolicyOk(candidate)) return candidate
        }
    }

    /**
     * Builds the complete MM2.json configuration.
     */
    fun buildConfig(
        kdfDir: File,
        isTestnet: Boolean,
        passphrase: String,
        rpcPassword: String,
        rpcPort: Int = DEFAULT_RPC_PORT,
        seednode: String = DEFAULT_SEEDNODE,
        extraSeednodes: List<String> = emptyList(),
        electrumServers: List<String> = TELEK_ELECTRUM_SERVERS,
        relayIp: String? = null,
    ): Map<String, Any?> {
        val dbDir = File(kdfDir, "db").apply { mkdirs() }
        val netid = if (isTestnet) NETID_TESTNET else NETID_MAINNET

        val coins = if (isTestnet) {
            listOf(
                mapOf(
                    "coin" to "tELEK",
                    "name" to "elektron-testnet",
                    "fname" to "Elektron (testnet)",
                    "pubtype" to 111,
                    "p2shtype" to 196,
                    "wiftype" to 239,
                    "segwit" to true,
                    "bech32_hrp" to "tb",
                    "address_format" to mapOf("format" to "segwit"),
                    "decimals" to 8,
                    "txfee" to 1000,
                    "estimate_fee_mode" to "ECONOMICAL",
                    "estimate_fee_blocks" to 4,
                    "avg_blocktime" to 1,
                    "required_confirmations" to 2,
                    "mature_confirmations" to 1,
                    "derivation_path" to "m/84'/1370'",
                    "orderbook_ticker" to "tELEK",
                    "rpcport" to ChainConfig.rpcPort,
                    "mm2" to 1,
                    "protocol" to mapOf("type" to "UTXO"),
                    "electrum" to electrumServers.map { mapOf("url" to it, "protocol" to "TCP") },
                ),
                mapOf(
                    "coin" to "tBTC",
                    "name" to "bitcoin",
                    "fname" to "tBTC (Elektron fork-testnet electrs-tn)",
                    "pubtype" to 111,
                    "p2shtype" to 196,
                    "wiftype" to 239,
                    "segwit" to true,
                    "bech32_hrp" to "tb",
                    "address_format" to mapOf("format" to "segwit"),
                    "decimals" to 8,
                    "txfee" to 1000,
                    "estimate_fee_mode" to "ECONOMICAL",
                    "estimate_fee_blocks" to 4,
                    "avg_blocktime" to 1,
                    "required_confirmations" to 2,
                    "mature_confirmations" to 1,
                    "derivation_path" to "m/84'/1'",
                    "orderbook_ticker" to "tBTC",
                    "rpcport" to ChainConfig.rpcPort,
                    "mm2" to 1,
                    "is_testnet" to true,
                    "protocol" to mapOf("type" to "UTXO"),
                    "electrum" to electrumServers.map { mapOf("url" to it, "protocol" to "TCP") },
                ),
                mapOf(
                    "coin" to "SEPOLIAETH",
                    "name" to "ethereum",
                    "fname" to "Ethereum Sepolia (kdf-native, public RPC nodes)",
                    "derivation_path" to "m/44'/60'",
                    "protocol" to mapOf("type" to "ETH", "protocol_data" to mapOf("chain_id" to 11155111)),
                    "max_eth_tx_type" to 2,
                    "mm2" to 1,
                    "is_testnet" to true,
                    "urls" to SEPOLIA_RPC_URLS,
                    "swap_contract_address" to SEPOLIA_SWAP_CONTRACT,
                ),
            )
        } else {
            listOf(
                mapOf(
                    "coin" to "ELEK",
                    "name" to "elektron",
                    "fname" to "Elektron",
                    "pubtype" to 0,
                    "p2shtype" to 5,
                    "wiftype" to 128,
                    "segwit" to true,
                    "bech32_hrp" to "be",
                    "address_format" to mapOf("format" to "segwit"),
                    "decimals" to 8,
                    "txfee" to 1000,
                    "estimate_fee_mode" to "ECONOMICAL",
                    "estimate_fee_blocks" to 4,
                    "avg_blocktime" to 1,
                    "required_confirmations" to 2,
                    "mature_confirmations" to 1,
                    "derivation_path" to "m/84'/1370'",
                    "orderbook_ticker" to "ELEK",
                    "rpcport" to 8332,
                    "mm2" to 1,
                    "protocol" to mapOf("type" to "UTXO"),
                    "electrum" to electrumServers.ifEmpty { ELEK_ELECTRUM_SERVERS }.map { mapOf("url" to it, "protocol" to "TCP") },
                ),
                mapOf(
                    "coin" to "BTC",
                    "name" to "bitcoin",
                    "fname" to "Bitcoin",
                    "pubtype" to 0,
                    "p2shtype" to 5,
                    "wiftype" to 128,
                    "segwit" to true,
                    "bech32_hrp" to "bc",
                    "address_format" to mapOf("format" to "segwit"),
                    "txfee" to 0,
                    "estimate_fee_mode" to "ECONOMICAL",
                    "avg_blocktime" to 600,
                    "required_confirmations" to 1,
                    "derivation_path" to "m/84'/0'",
                    "protocol" to mapOf("type" to "UTXO", "chain_variant" to "BTC"),
                    "rpcport" to 8332,
                    "mm2" to 1,
                    "sign_message_prefix" to "Bitcoin Signed Message:\n",
                    "electrum" to BTC_ELECTRUM_SERVERS.map { mapOf("url" to it, "protocol" to "TCP") },
                ),
                mapOf(
                    "coin" to "ETH",
                    "name" to "ethereum",
                    "fname" to "Ethereum",
                    "derivation_path" to "m/44'/60'",
                    "max_eth_tx_type" to 2,
                    "mm2" to 1,
                    "chain_id" to 1,
                    "avg_blocktime" to 15,
                    "required_confirmations" to 3,
                    "rpcport" to 80,
                    "swap_gas_fee_policy" to "Medium",
                    "sign_message_prefix" to "Ethereum Signed Message:\n",
                    "protocol" to mapOf("type" to "ETH", "protocol_data" to mapOf("chain_id" to 1)),
                    "urls" to MAINNET_ETH_RPC_URLS,
                    "swap_contract_address" to MAINNET_ETH_SWAP_CONTRACT,
                ),
            )
        }

        val seednodes = if (isTestnet) {
            listOf("127.0.0.1", seednode.trim().ifEmpty { DEFAULT_SEEDNODE })
        } else {
            listOf(seednode.trim().ifEmpty { DEFAULT_SEEDNODE })
        } + extraSeednodes

        val config = linkedMapOf<String, Any?>(
            "gui" to "elektron-net",
            "netid" to netid,
            "rpcip" to DEFAULT_RPC_HOST,
            "rpcport" to rpcPort,
            "passphrase" to passphrase,
            "rpc_password" to rpcPassword,
            "dbdir" to dbDir.absolutePath,
            "seednodes" to seednodes,
            "coins" to coins,
        )
        if (!relayIp.isNullOrBlank()) {
            // Turn the phone into a P2P relay that listens on its own address:
            // needed so two phones can meet without a host seed in between.
            config["i_am_seed"] = true
            config["is_bootstrap_node"] = false
            config["myipaddr"] = relayIp.trim()
        }
        return config
    }
}