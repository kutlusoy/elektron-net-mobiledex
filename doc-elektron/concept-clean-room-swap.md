# Clean-room swap protocol: the parallel track

- **Version:** 0.1 (draft)
- **Date:** November 14, 2026
- **Status:** proposal, runs in parallel to the GPL-3.0 line. Not a blocker for M1-M4 of [`concept-elektron-dex.md`](./concept-elektron-dex.md).
- **Purpose:** an independently written swap protocol that can be licensed freely (MIT), so the marketplace does not depend on the Komodo DeFi Framework and its copyright notice forever.

## 1. Why in parallel

The GPL-3.0 line ships first and is legally clean: the engine is free, the notice is respected, the app is free of charge. The clean-room line buys a second, independent option: no copyleft, no third-party notice, no "no charge" condition, and freedom to change the fee logic and the protocol itself. It is also the answer if a lawyer rejects the process boundary between the MIT wallet and the GPL-3.0 app.

Running both means the shipping app is never blocked by a protocol rewrite that takes months.

## 2. What clean room means here

The accepted model is the two-team model:

1. **Specification team** writes a functional specification of the protocol: messages, state machine, HTLC scripts, timeouts, error cases, test vectors. The specification describes behavior, not code.
2. **Implementation team** writes the implementation from that specification only, and never sees the original source.

The integrity rule is the whole point: if the implementer has read the original code, the result is a derivative work in the ordinary sense, and calling it clean room is wrong. That is not a formality, it is the only thing that makes the track worth its cost.

## 3. Disclosure and the staffing problem

The contributors who built the current swap integration, including the AI assistant used in this session, **have read parts of the engine source** (`lp_swap.rs`, `lp_ordermatch.rs`, `orderbook_rpc.rs`, coin configuration). They can therefore:

- write the public-source specification and the test vectors,
- review the implementation for protocol conformance,
- maintain the test harness and the blockchain fixtures,

but they **cannot** be the clean-room implementers. The track needs an implementer (person or agent) with no access to the engine source. If no such implementer can be found, the track is not a clean room and should be renamed to "independent protocol rewrite" and reviewed as a derivative work.

## 4. Allowed and forbidden inputs

| Allowed | Forbidden for the implementer |
|---|---|
| Public protocol documentation of the Komodo/AtomicDEX family, the Elektron Net whitepaper, BIP-9/65/68/141/143/173/174 | The engine source tree, including comments |
| Black-box observation: RPC responses of a running daemon, network messages, log output with secrets removed | Code excerpts, even "just the constants" |
| Public blockchain data: HTLC scripts of executed swaps, timelock values, fee behavior | Internal identifiers that only exist in the source |
| The specification produced by the specification team | Any conversation with someone who read the source about implementation details |

Black-box observation is the normal route for interoperable implementations and the reason the track is feasible at all: the wire format, the HTLC scripts and the timeout behavior are visible in traffic and on chain.

## 5. Scope of the first version

| ID | Part | Content |
|---|---|---|
| S1 | Order announcement | signed offers on a P2P topic, price, min/max volume, address, expiry |
| S2 | Matching | taker selection, reservation, negotiation messages |
| S3 | HTLC construction | Elektron and Bitcoin scripts, hashlock, timelocks, sequence, dust limit, fee selection |
| S4 | Atomicity | preimage reveal, redeem path, refund path, what happens if one side stalls |
| S5 | Failure handling | timeouts, reorgs, partial broadcast, fee spikes, recovery of funds without the counterparty |
| S6 | Book and receipts | local order storage, history, pruned receipts without addresses |

Transport is a separate decision: reuse the Elektron node's P2P layer, reuse the framework's network but not its swap code (license question), or a new gossip channel.

## 6. Deliverables

1. `spec/swap-protocol.md`: state machine, message table, HTLC script templates, timeout table, error catalogue.
2. `spec/test-vectors/`: deterministic vectors for scripts, hashes, fee math, state transitions.
3. `spec/transcripts/`: sanitized black-box captures that back each message type.
4. Reference implementation (Rust core, Kotlin client) under MIT.
5. Conformance harness: runs the vectors and a regtest end-to-end swap against the specification, not against the engine.

## 7. Milestones

| ID | Milestone | Acceptance | Falsifier |
|---|---|---|---|
| CR0 | Specification skeleton | message table and state machine cover a whole swap, every claim has a public source or a transcript | a message type only explained by source code |
| CR1 | HTLC on regtest | Elektron and Bitcoin HTLCs lock, redeem and refund on regtest with the vectors | a timeout cannot be enforced |
| CR2 | Two-party swap | two implementations of the same spec complete a swap on regtest, including one abort path | only one implementation exists, so the spec is untested |
| CR3 | Testnet run | a real testnet swap between two phones, with the same evidence format as the GPL line | funds stuck or a manual repair step |
| CR4 | Review | external review of spec and implementation, plus a lawyer's note on provenance | open finding in the atomicity or recovery path |

## 8. Effort and risks

- Two people minimum for the two-team split, plus review. A single implementer who has seen the source is not a clean room.
- The recovery and timeout paths are where swaps lose money; they need more tests than the happy path, and they are the part that black-box observation reveals least.
- Protocol drift: if the shipped app follows the engine, the clean-room line must either stay compatible or fork the protocol. Decide early which one is the goal.
- Naming and branding: a second marketplace implementation must not be presented as the same product with different guarantees.

## 9. Relationship to the GPL line

- Shared: UI patterns, test devices, on-chain evidence format, the pruned receipt format.
- Not shared: any engine code, any engine-derived constant that was not observed in traffic, any engine patch.
- The GPL line stays the reference for what users see; the clean-room line has to earn its own trust with its own evidence.
