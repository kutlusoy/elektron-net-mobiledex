# How the swap engine reaches the APK

- **Status:** documented as it is, not as it should be. The build recipe is **UNVERIFIED**: neither this repository nor `elektron-net-mobile` contains a script that produces `libkdfbin.so`.
- **Audience:** release engineers, developers who build a test APK

## 1. The artifact

| Property | Value |
|---|---|
| File | `libkdfbin.so` (the Komodo DeFi Framework engine, `mm2` binary renamed for Android) |
| ABI | **arm64-v8a only**. There is no `x86_64` build, so the engine does not start on an x86_64 emulator |
| Upstream | `GLEECBTC/komodo-defi-framework`, commit `367e4dd4d77dd873ab2a5b9d38a998b36e9efe3c` (`kdf-upstream-20261001-86-g367e4dd4d`) |
| Size | about 108 MB, which is why it is not in Git |
| SHA-256 of the current build | `3be96442ff41a93664f26197d589ca48ed1643831ecd5eddba98c78d692939d1` |
| License | GPL-3.0 plus the Gleec notice, see [`legal`](./concept-elektron-dex.md) and [`NOTICE-komodo-defi-framework.md`](./NOTICE-komodo-defi-framework.md) |

## 2. Where it goes

```text
app/src/main/jniLibs/arm64-v8a/libkdfbin.so
```

`app/src/main/jniLibs/` is in `.gitignore`, so the binary never enters the repository. The same holds for the node (`libelektrond.so`), Tor and i2pd. A fresh checkout cannot build a runnable APK until all of them are present.

## 3. Where the other native libraries come from

| Library | Produced by | Repository |
|---|---|---|
| `libelektrond.so` | `native-build/build.sh` with the Android NDK, from the node source pinned in `native-build/core-source.env` | documented in the README |
| `libtor.so`, `libi2pd.so` | `native-build/fetch-helpers.sh` / `native-build/helpers` | documented in `guideline-tor-and-i2p.md` |
| `libkdfbin.so` | **no script in the repository** | this document, §4 |

## 4. What has to be added

A reproducible build of the engine is a release prerequisite (M4). Until it exists, record provenance for every build by hand:

1. Check out KDF at the pinned commit.
2. Build for `aarch64-linux-android` with the NDK (Rust cross-compilation; the framework produces a `mm2`-style binary that runs as a child process and speaks JSON-RPC over localhost).
3. Strip it, place it as `app/src/main/jniLibs/arm64-v8a/libkdfbin.so`.
4. Record `sha256sum` and the commit in the release notes.
5. Verify the packaging:

```bash
./gradlew assembleTnetDebug
unzip -l app/build/outputs/apk/tnet/debug/app-tnet-debug.apk | grep libkdfbin.so
```

The current binary was produced that way in the wallet line and copied over unchanged; its checksum matches the one in `elektron-net-mobile`. **No attempt was made in this session to rebuild it, and no step of the build is verified.**

## 5. Why this is a risk

- Without a script, a release cannot be reproduced from source alone; the GPL-3.0 source offer would be incomplete in spirit even if the source tag exists.
- A rebuild with a different toolchain can change behavior (TLS, DNS, database backends). The release notes must name the binary's checksum, not only the source commit.
- The engine only exists for arm64. A 32-bit phone or an emulator cannot run a swap.

Adding the script is part of M4 in [`concept-elektron-dex.md`](./concept-elektron-dex.md).
