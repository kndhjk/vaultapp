# Vault Password Analysis

## Password Structure (5 parts)
```
getCacheKey() = getPartition1("K3Y1") + getPartition2() + syncRemoteConfig() + loadAssetIndex() + getPartition5()
               = Part1           + Part2        + Part3          + Part4             + Part5
```

## Known Parts
- **Part 3** (server): `"e9xx"` (4 chars from server signature substring(10,14))
- **Part 4** (PNG tEXt chunk): `b6HBt0lNEBDpT9LP2KkUiA==` (16 bytes base64)

## libvault.so Static Analysis
File: `libvault.so` (18KB, x86_64, android-ndk-r27 clang)

### AES Parameters found in .rodata (0xb20):
- IV at 0xe10: `637c777bf26b6fc53001672bfed7ab76ca` (12 bytes!)
- Key material at 0xe03: `F3WR2QX1Z` (9 bytes)
- Encrypted data at 0xe6b: `394a4c58cfd0efaafb434d338545f902` (16 bytes)
- Encrypted data at 0xe74: `7f503c9fa851a3408f929d38f5bcb6da` (16 bytes)
- String at 0xd80: `ImageMetaKey00` (14 bytes, zero-padded)

### Native Functions (symbol addresses):
- `getPartition1`: VMA 0x1680 (301 bytes)
- `getPartition2`: VMA 0x1dd0 (418 bytes)  
- `getPartition5`: VMA 0x21a0 (389 bytes)
- `decodeAssetChunk`: VMA 0x17d0 (inferred)
- `getRemoteConfigKey`: VMA 0x2330

### Part 4 Decoding (loadAssetIndex):
1. Java: reads vault_bg.png from assets, extracts tEXt chunk "Comment"
2. PNG tEXt: `Comment\x00b6HBt0lNEBDpT9LP2KkUiA==` 
3. Base64 decode → 16 bytes: `6fa1c1b7494d1010e94fd2cfd8a91488`
4. `decodeAssetChunk(base64_decoded)` → native AES decode

### Key Derivation:
- `ImageMetaKey0042` string at 0xd80 (16 bytes with padding)
- Used to derive AES-256 key via SHA256: `SHA256("ImageMetaKey0042")`
- 12-byte IV suggests AES-CTR mode (not CBC)

## Frida Deployment (MacBook ready)
- frida-server-17.9.5-android-x86_64: `/tmp/frida-server` (106MB)
- Deploy: `adb push /tmp/frida-server /data/local/tmp/`
- Start: `adb shell /data/local/tmp/frida-server -l 0.0.0.0:27043 &`
- Connect from Termux: `frida -H localhost:27042 com.challenge.vault`

## Emulator Issue
Medium_Phone_userdebug AVD:
- QEMU running, ports listening, adbd starting
- `adb devices` shows `offline` — known macOS Android emulator issue
- Console auth token: stored in `~/.emulator_console_auth_token`
- Workaround: use emulator console `redir add tcp:5557:5555` then `adb connect localhost:5557`

## GitHub
https://github.com/kndhjk/vaultapp
