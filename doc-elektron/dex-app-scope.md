# Elektron DEX: interface and scope

- **Version:** 1.0
- **Date:** November 15, 2026
- **Audience:** UI/Android developers, maintainers of the wallet line, reviewers
- **Related:** [`concept-elektron-dex.md`](./concept-elektron-dex.md), [`concept-clean-room-swap.md`](./concept-clean-room-swap.md), [`guideline-ui-design-system.md`](./guideline-ui-design-system.md), [`guideline-ux-and-screens.md`](./guideline-ux-and-screens.md)

## 1. Vision

**The marketplace is the front page; the wallet is one tap away.**

The wallet line answers "where are my coins". The DEX answers "what is the market
doing, and what can I trade right now". Every screen is measured against that
sentence: a surface that does not help trade or fund a trade belongs under
*More*.

## 2. Navigation

The bottom bar has four destinations, trading first:

| Tab | Responsibility | Source |
|---|---|---|
| **Market** | The front page: pair selector, engine state, volume-weighted price, depth (asks/bids), own resting orders, cancel, "Open swap" | new `ui/market/MarketScreen.kt` |
| **Swap** | Instant swap, limit order, engine start/stop, withdraw and address sharing, recent swaps | `ui/tools/SwapScreen.kt` |
| **Wallet** | Balances, receive, send, recent history, wallet switcher | wallet line |
| **More** | History, Node, Tools (escrow, PSBT, sweep, messages), Settings | wallet line + `ui/more/MoreScreen.kt` |

Rules that follow from the vision:

- **No trading surface behind a submenu.** The swap screen was reachable only
  through *Tools* before; it is a primary destination now, and the Tools hub no
  longer links to it.
- **One state owner.** The engine owns the book, the orders and the swaps; the
  screens project it. The market screen keeps no book state of its own beyond
  the last poll.
- **Market is read-only.** Placing, taking and cancelling live on Swap/Market
  only through the engine's RPC; the market screen is a projection plus the
  cancel action on the user's own orders.
- **The wallet stays reachable.** Funding a swap must never require leaving the
  DEX: Wallet is one tap, and every swap screen shows the engine balances.

## 3. Contracts the interface depends on

| Fact | Evidence |
|---|---|
| The order book is requested with MMRPC 2.0 (`base`, `rel`); the legacy call returns an empty list for replicated orders while `orderbook_depth` counts them | `KdfClient.orderbook`, verified on two phones (JV `depth=2` vs legacy `asks=0`; 2.0 listed both asks) |
| Engine state is `Stopped`, `Starting`, `Running`, `Error` | `KdfState` |
| Own orders come from `my_orders` (`maker_orders`), cancel via `cancel_order` | `KdfClient.myOrders`, `cancelOrder` |
| Swap lifecycle is projected from `my_recent_swaps` plus the pruned receipts | `KdfSupervisor.getRecentSwapsMerged` |
| Network endpoints come from `kdf/net.txt` (`seed`, `electrum`, `relay`) | `KdfSupervisor.readNetworkProfile` |

## 4. What the DEX needs from the upstream wallet line

The DEX app is a fork of `elektron-net-mobile`. Everything below is inherited
code; the table says whether the DEX keeps it, and why.

| Upstream module | Use in the DEX | Decision |
|---|---|---|
| `NodeHost`, `NodeService`, `NodeSupervisor`, `NodeConfig`, `NetworkServices` | Runs the phone node; every balance and swap depends on it | **Keep** |
| Tor and I2P helpers (`libtor`, `libi2pd`, runtime wiring) | Privacy of node and market traffic | **Keep** |
| Wallet core: BIP39/BIP32, descriptors, `WalletRegistry`, `Amount`, `NumberStyle` | Hot wallet, receive addresses for withdrawals, formatting | **Keep** |
| Receive and Send (incl. coin control) | Fund the engine, withdraw from the engine | **Keep** |
| Transaction history | "Where did my coins go" after a swap | **Keep** |
| Sweep private key | The clean path for importing an Electrum wallet (maintainer decision) | **Keep** |
| PSBT tools | Signing with an external signer is not a v1 swap need | **Optional** |
| Escrow, messages | Wallet features, not marketplace features | **Drop from DEX v1** (stay in the wallet) |
| Expert console | Diagnostics; useful during testnet bring-up | **Optional** |
| Update check, geoip, explorer links | Comfort features | **Optional** |
| `controller`, node RPC plumbing | Shared by node and wallet | **Keep** |

The DEX line adds, and never the other way round:

- `kdf/` (engine client, config, supervisor, swap pruner, VWAP helper) - GPL-3.0
- `ui/market/`, `ui/more/`, the swap screen - GPL-3.0
- `kdf/net.txt` profile handling - GPL-3.0

## 5. Module boundary

| Layer | License | Where it lives |
|---|---|---|
| node, wallet, history, send/receive, sweep, Tor/I2P | MIT | wallet line, reused by the DEX |
| swap engine bridge, market and swap UI | GPL-3.0 | this repository |

A change that belongs to both lines is made in the MIT modules and cherry-picked;
a change that touches the engine or the marketplace UI stays here. This keeps
the MIT wallet free of GPL-3.0 code (see `concept-elektron-dex.md` §5).

## 6. Open interface work

1. **Light-client order book mesh** - replicated orders arrive, but the topic
   mesh reports `Contains: 0 needs: 2`, so the legacy book stays empty. The 2.0
   path is the working projection; the mesh still needs a fix for a book that
   updates without polling.
2. **Indexer over LAN** - the app cannot reach a remote Electrum endpoint yet
   (20 s connect timeout while a root shell connects); until fixed, `adb
   reverse` is the bridge for testnet.
3. **Market screen extensions** - spread, 24 h volume and a price chart are the
   next additions; each must be derived from the same engine state, not from a
   second model.
