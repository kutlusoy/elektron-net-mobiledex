# Elektron Electrum and the mobile node: key and address compatibility

- **Date:** November 15, 2026
- **Question:** Does an Elektron Electrum wallet use other keys or addresses than the node? Is a sweep the only safe way to bring an Electrum wallet into the mobile node?
- **Answer:** No and no. Curve, key serialisation and address encoding are identical; only the **seed mnemonic scheme** and the **derivation paths** differ, and the importer handles both explicitly. A wallet file can be imported without moving a single coin.

## 1. Measured parameters

Electrum fork: `elektron-net-electrum/electrum/constants.py`. Node: `elektron-net/src/kernel/chainparams.cpp`.

| Parameter | Electrum fork | Node mainnet | Node testnet | Match |
|---|---|---|---|---|
| WIF prefix | `0x80` (`constants.py:155`) | `128` = `0x80` (`chainparams.cpp:227`) | `239` = `0xef` | yes |
| P2PKH version | `0` (`constants.py:156`) | `0` (`chainparams.cpp:225`) | `111` | yes |
| P2SH version | `5` (`constants.py:157`) | `5` (`chainparams.cpp:226`) | `196` | yes |
| Extended private key header | `XPRV_HEADERS` (`constants.py:194`) | `0x0488ADE4` (`chainparams.cpp:229`) | `0x04358394` | yes |
| Bech32 HRP | `"be"` (`constants.py:164`) | `"be"` (`chainparams.cpp:231`) | `"tb"` | yes |
| Curve / key format | secp256k1, BIP32 xprv/xpub | secp256k1, descriptor wallet with `wpkh`/`pkh` | same | yes |

The fork even documents the intent in `constants.py:158-163`: WIF/P2PKH/P2SH bytes are *intentionally* identical to Bitcoin mainnet, while the bech32 HRP is Elektron Net's own `be`. So a private key or address is valid on both sides, byte for byte.

## 2. What actually differs

| Difference | Electrum | Node | Handling |
|---|---|---|---|
| Seed mnemonic | Own scheme: HMAC-SHA512("Seed version", words) starts with `01` (standard), `100` (segwit), `101` (2FA), `102` (2FA segwit) (`version.py:17-20`) | BIP-39 | `ElectrumSeed.seedType` / `toSeed` derive the seed with Electrum's PBKDF2 (salt `electrum`+passphrase, 2048 rounds) |
| Derivation | `m/0'/0` and `m/0'/1` for native SegWit, `m/0` and `m/1` for legacy | BIP84 `m/84'/1370'/0'` for new wallets | `Descriptors.forElectrumSeed` imports the Electrum paths explicitly; the BIP84 path would find nothing |
| Wallet file | JSON, optionally encrypted as a `BIE1` storage blob (ECDH + AES-128-CBC + HMAC-SHA256, password PBKDF2 with empty salt, 1024 rounds) | `wallet.dat` / SQLite descriptor wallet | `ElectrumWalletParser.decryptStorage` implements the same format; per-value `pw_hash` uses `sha256d` AES |
| 2FA wallets | Derivation works, spending needs the trustless server | — | Import derives the addresses; spending a 2FA wallet still needs that service |

## 3. Import paths in the DEX app

| Input | Path | Result |
|---|---|---|
| Electrum wallet file, plain or encrypted | `RestoreFileSection` → `NodeHost.restoreWalletFile(file, passphrase)` → `WalletRestore.restoreElectrum` | Keystore imported as descriptors, wallet active, rescan |
| Electrum seed words | `ElectrumSeed.toSeed` → `Bip32.masterExtendedKey` → `Descriptors.forElectrumSeed` | Same as above, no file needed |
| Electrum `xprv` keystore | `Descriptors.forElectrumSeed(xprv, type)` | Same |
| `imported` wallet (WIF keys) | `wpkh`/`pkh` descriptors per key | Same; each key keeps its script type |
| Single private key | Sweep (existing tool) | Moves the coins of that one key |

The encrypted file asks for the **Electrum password** before anything is derived; a wrong password is reported as such, not as a corrupted wallet.

## 4. Why a sweep as the only path is the risky option

A sweep is a **transaction**: it spends the keys' coins to a new wallet. If anything is missed, the coins stay behind while the user sees an empty destination wallet:

- an address beyond the gap limit is not swept;
- a script type the sweeper does not build (e.g. a p2sh-wrapped key) leaves its coins untouched;
- a 2FA or multisig wallet cannot be swept at all with one key;
- a sweep confirms on chain, so "undo" does not exist.

An **import** derives and watches the whole keystore inside the node wallet. No transaction is signed, no coin moves, and a wrong path is fixable by re-importing with the right one. That is why import is the default and sweep remains an explicit tool for single keys.

## 5. Verification

- `ElectrumWalletParserTest` (5 tests) and `ElectrumSeedTest` (4 tests) run green in this line; they cover the `BIE1` storage decryption, seed version detection, `pw_decode`, and the imported-keys path.
- **Independent cross-check:** two seeds were re-derived with Python's stdlib only (`hmac` + `hashlib.pbkdf2_hmac`). The version prefixes came out as `1000` (segwit) and `0117` (standard), and both PBKDF2 seeds (empty passphrase and `my_password`) matched the Kotlin vectors byte for byte. The vectors are therefore Electrum's, not the implementation's own.
- `WalletRestoreTest` in the wallet line covers the full restore; the DEX line reuses the same `WalletRestore`.
- Build: `assembleTnetDebug` succeeds with the importer included.

## 6. Limits, stated plainly

- **Multisig Electrum wallets** (`wallet_type: "multisig"`, keystores `x1`/`x2`) are rejected with "Missing keystore object" rather than silently imported half-way. A sweep is equally unsafe there; the honest state is "not supported yet".
- **2FA wallets** import their addresses, but spending still requires the 2FA service.
- A testnet wallet file and a mainnet wallet file are not interchangeable; the HRP differs (`tb` vs `be`), and the import validates against the chain it runs on.
