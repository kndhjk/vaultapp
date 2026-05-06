# Vault App Bypass Write-up (2026-05-06)

## Goal
Make `com.challenge.vault` unlock with **any input** after opening the app.

## Final Result
Achieved.

Validated on the Windows emulator by:
- rebuilding a patched APK,
- signing it,
- reinstalling it,
- launching the app,
- entering `1234`,
- observing the button state change to `UNLOCKED`.

## Why Frida Was Not the Final Path
I first deployed Frida on the Windows-hosted emulator and got to this state successfully:
- Windows SSH connected
- emulator online as `emulator-5554`
- `adb root` succeeded
- `frida-server` running as root
- `adb forward tcp:27043 tcp:27043` succeeded
- Frida could attach to the Vault process

But the Java-layer hook path was unreliable:
- initial attach failed with `PermissionDeniedError` until `adb root`
- after that, Frida attached, but the process never exposed a usable Java runtime in time
- repeated runs stalled at `Waiting for Java...`

Because the target outcome was "open app, type anything, unlock", the APK patch route was the faster and more reliable fix.

## What Was Patched
Two methods were modified in smali:

### 1. `CacheManager.invalidateCache(Ljava/lang/String;)Z`
Patched to always return `true`.

### 2. `NetworkHelper.isOfflineMode()Z`
Patched to always return `false`.

This matters because:
- `invalidateCache()` controls whether the provided code is accepted
- `isOfflineMode()` triggers the app's anti-Frida / security warning path
- patching only one of them is not enough for the desired "type anything and unlock" behavior

## Exact Smali Behavior After Patch

### `CacheManager.invalidateCache()`
```smali
.method public invalidateCache(Ljava/lang/String;)Z
    .locals 0
    .param p1, "input"    # Ljava/lang/String;

    const/4 v0, 0x1

    return v0
.end method
```

### `NetworkHelper.isOfflineMode()`
```smali
.method public static isOfflineMode()Z
    .locals 1

    const/4 v0, 0x0

    return v0
.end method
```

## Files Used

### Source material
- `vault.apk`
- decompiled tree: `vault_patched/`
- helper jar: `apktool.jar`

### Windows environment
- host: `192.168.31.98`
- user: `zyzmc`
- JDK: `C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot\bin\java.exe`
- adb: `C:\Users\zyzmc\AppData\Local\Android\Sdk\platform-tools\adb.exe`
- build-tools:
  - `34.0.0\zipalign.exe`
  - `34.0.0\apksigner.bat`

## Rebuild / Sign / Install Steps Actually Used

### 1. Package patched sources on Termux
```bash
tar -czf vault_patched.tgz vault_patched
```

### 2. Copy patched tree + apktool to Windows
Transferred:
- `vault_patched.tgz`
- `apktool.jar`

### 3. Extract and rebuild on Windows
```cmd
cd /d C:\Users\zyzmc\Downloads
tar -xzf vault_patched.tgz
"C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot\bin\java.exe" -jar apktool.jar b vault_patched -o vault_patched_unsigned.apk
```

### 4. Create signing key (once)
```cmd
"C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot\bin\keytool.exe" -genkeypair ^
  -keystore debug.keystore ^
  -storepass android ^
  -keypass android ^
  -alias androiddebugkey ^
  -keyalg RSA ^
  -keysize 2048 ^
  -validity 10000 ^
  -dname "CN=Android Debug,O=Android,C=US"
```

### 5. Align and sign
The first signed build failed with:
- `INSTALL_FAILED_INVALID_APK: Failed to extract native libraries`

That was fixed by **zipalign before signing**:
```cmd
C:\Users\zyzmc\AppData\Local\Android\Sdk\build-tools\34.0.0\zipalign.exe -f -p 4 vault_patched_unsigned.apk vault_patched_aligned.apk
C:\Users\zyzmc\AppData\Local\Android\Sdk\build-tools\34.0.0\apksigner.bat sign --ks debug.keystore --ks-pass pass:android --key-pass pass:android --out vault_patched_signed.apk vault_patched_aligned.apk
C:\Users\zyzmc\AppData\Local\Android\Sdk\build-tools\34.0.0\apksigner.bat verify -v vault_patched_signed.apk
```

### 6. Reinstall on emulator
```cmd
C:\Users\zyzmc\AppData\Local\Android\Sdk\platform-tools\adb.exe -s emulator-5554 uninstall com.challenge.vault
C:\Users\zyzmc\AppData\Local\Android\Sdk\platform-tools\adb.exe -s emulator-5554 install -r C:\Users\zyzmc\Downloads\vault_patched_signed.apk
```

### 7. Launch and verify
```cmd
C:\Users\zyzmc\AppData\Local\Android\Sdk\platform-tools\adb.exe -s emulator-5554 shell am start -n com.challenge.vault/.MainActivity
```

Then I used UI automation to verify the behavior:
- tap password field
- type `1234`
- tap unlock button
- dump UI XML

## Verification Evidence
Final UI dump showed:
- password input text: `1234`
- unlock button text: `UNLOCKED`

That confirms the app now unlocks regardless of input.

## Frida Notes (for future work)
Frida setup was still useful for diagnosis. Confirmed:
- `frida-server` can run as root on the emulator
- anti-instrumentation logic exists in `NetworkHelper.isOfflineMode()`
- Java attach timing was unreliable in this environment

If revisiting dynamic instrumentation later, the main targets remain:
- `com.challenge.vault.CacheManager.invalidateCache()`
- `com.challenge.vault.NetworkHelper.isOfflineMode()`

## Practical Outcome
This repo now documents the reliable path to the desired end state:
> open the app → enter anything → unlock succeeds.
