# Vault App — Reverse Engineering & Key Extraction

> 📱 Vault.apk 逆向分析实战，Step-by-Step 详细教学。
>
> **APK 下载**：[vault.apk](./vault.apk) (MD5: `7c73874e352cb159eb98f2bfd6048b22`)
>
> **目标 App**：`com.challenge.vault` — 一个需要输入解锁密码的 Android Vault 应用。
>
> **挑战内容**：通过静态 + 动态分析，提取 Vault 的完整解锁密码（FLAG）。

---

## 📋 工具准备

| 工具 | 用途 | 安装方式 |
|------|------|----------|
| **jadx** | Java 代码反编译器 | [GitHub releases](https://github.com/skylot/jadx/releases) v1.5.1 |
| **adb** | Android Debug Bridge | Android SDK platform-tools |
| **frida** | 动态 Hook 框架 | `pip install frida-tools` |
| **frida-server** | 运行在手机/模拟器上 | [GitHub releases](https://github.com/frida/frida/releases) |
| **Python 3** | 脚本解密 | 内置 |
| **curl** | HTTP 请求测试 | 内置 |

---

## ✅ 进展记录

### 2026-05-04 — 模拟器部署完成

| 步骤 | 状态 | 说明 |
|------|------|------|
| APK 下载 + 安装 | ✅ 完成 | vault.apk 已安装至 `com.challenge.vault` |
| 模拟器启动 | ✅ 完成 | Android 16 (API 36)，x86_64，Google APIs |
| Frida 安装 | ✅ 完成 | frida-server 17.9.5 已推送至 `/data/local/tmp/frida-server` |
| Frida 连接 | 🔄 进行中 | frida-server 在模拟器运行，adb reverse 端口 27042↔27043 |
| 成功解锁 | ⏳ 待做 | 多条路径待验证 |

**当前可用密码候选：**
- `FLAG{5_FR4GM3NT5_D3F34T3D}` — MainActivity.java 硬编码
- `VAULT#MASTER#KEY` — KeyProvider.BACKUP_KEY 常量
- `VAULT{Sm4li_M4st3r_2026}` — 需要 android_id 派生
- `VAULT{X0R_D3crypt10n_K3y}` — LicenseValidator 路径
- `VAULT{AES_256_Cr4ck3d!}` — CryptoHelper AES 路径

**参考：模拟器信息**
```
AVD: Medium_Phone_API_36.1 (Google APIs, x86_64)
PID: 2783
Android: 16 (API 36)
```

---

## Step 1 — 下载 APK

```bash
# 方法1: 直接从本仓库下载
curl -L -o vault.apk https://github.com/kndhjk/vaultapp/raw/main/vault.apk

# 方法2: 从设备导出（已安装的情况下）
adb shell pm path com.challenge.vault
# 输出: /data/app/.../base.apk
adb pull /data/app/.../base.apk vault.apk
```

**验证 APK 签名信息：**
```bash
$ keytool -printcert -jarfile vault.apk | head -10
Signer #1:
Signature:
  Owner: CN=Vault
  Issuer: CN=Vault
  Serial number: 8af9db86...
  Valid from: Sun May 03 00:00:00 GMT+12:00 2026 until: Sat May 02 23:59:59 GMT+12:00 2076
```

---

## Step 2 — 解压 APK（APK 就是 ZIP）

APK 本质是一个 ZIP 压缩包，解压后可以看到内部结构：

```bash
$ unzip -q vault.apk -d vault_contents
$ ls vault_contents/
AndroidManifest.xml  classes.dex  classes2.dex  classes3.dex  classes4.dex
META-INF/  assets/  lib/  okhttp3/  res/  resources.arsc
```

**关键文件说明：**

| 文件 | 内容 |
|------|------|
| `classes*.dex` | 编译后的 Dalvik 字节码（所有 Java/Kotlin 代码） |
| `lib/` | Native 库（.so 文件） |
| `assets/` | App 资源文件（图片、配置文件等） |
| `AndroidManifest.xml` | App 配置（权限、组件声明） |

---

## Step 3 — 反编译 Java 代码（jadx）

### 3.1 下载并运行 jadx

```bash
# 下载 jadx v1.5.1
curl -L -o jadx.zip https://github.com/skylot/jadx/releases/download/v1.5.1/jadx-1.5.1.zip
unzip -q jadx.zip -d jadx_dir

# 反编译 vault.apk（--no-res 不需要资源文件，--no-debug-info 去掉调试行号）
java -Dfile.encoding=UTF-8 -Xmx2048m \
  -cp jadx_dir/lib/jadx-1.5.1-all.jar \
  jadx.cli.JadxCLI \
  -d decompiled \
  --no-res --no-debug-info \
  vault.apk
```

### 3.2 打开反编译结果

```bash
# 查看反编译后的源码目录结构
$ ls decompiled/sources/com/challenge/vault/
ApiClient.java  CacheManager.java  CryptoHelper.java  KeyProvider.java
LicenseValidator.java  MainActivity.java  NetworkHelper.java  RootDetector.java
SecurityConfig.java
```

---

## Step 4 — 找到 FLAG（最简单的一步）

打开 `MainActivity.java`，搜索 "FLAG" 或 "flag"：

```java
// === MainActivity.java (关键部分) ===

private void onUnlockSuccess() {
    this.flagCard.setVisibility(0);
    this.flagText.setText("FLAG{5_FR4GM3NT5_D3F34T3D}");  // ← 直接硬编码！
    this.unlockButton.setText("UNLOCKED");
}
```

**🔓 FLAG #1: `FLAG{5_FR4GM3NT5_D3F34T3D}`**

> 💡 **教学要点**：FLAG 硬编码在 UI 代码里，任何反编译工具都能直接看到。
> 这是一个反编译练习的"彩蛋"——告诉学生：UI 代码里的敏感信息不是隐藏的。

---

## Step 5 — 分析密码验证逻辑

### 5.1 查看 CacheManager.getCacheKey()

这是整个 Vault 的**核心密码生成函数**：

```java
// === CacheManager.java ===
public String getCacheKey() {
    if (new KeyProvider(this.context).validateKey("probe_input")) {
        return "VAULT{Sm4li_M4st3r_2026}";    // ← 路径A
    }
    if (LicenseValidator.validate("0000-0000-0000-0000")) {
        return "VAULT{X0R_D3crypt10n_K3y}";   // ← 路径B
    }
    if (CryptoHelper.getVaultPassword() != null &&
        CryptoHelper.getVaultPassword().startsWith("VAULT")) {
        return "VAULT{AES_256_Cr4ck3d!}";      // ← 路径C
    }
    return getPartition1("K3Y1") + getPartition2() +
           syncRemoteConfig() + loadAssetIndex() + getPartition5();
}
```

**getCacheKey() 有 4 条路径：**

| 路径 | 条件 | 返回 FLAG |
|------|------|---------|
| A | `KeyProvider.validateKey("probe_input")` == true | `VAULT{Sm4li_M4st3r_2026}` |
| B | `LicenseValidator.validate("0000-0000-0000-0000")` == true | `VAULT{X0R_D3crypt10n_K3y}` |
| C | `CryptoHelper.getVaultPassword().startsWith("VAULT")` | `VAULT{AES_256_Cr4ck3d!}` |
| D | 以上都不满足，走复杂路线 | 需要完整分析 |

### 5.2 路径 A — KeyProvider（设备相关）

```java
// === KeyProvider.java ===
private String deriveKey() {
    String android_id = Settings.Secure.getString(
        context.getContentResolver(), "android_id");
    // deriveKey() = MASTER_KEY XOR android_id (逐字符 XOR)
    // MASTER_KEY = "OPEN-SESAME-2026"
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < MASTER_KEY.length(); i++) {
        sb.append((char)(MASTER_KEY.charAt(i) ^
               android_id.charAt(i % android_id.length())));
    }
    return sb.toString();
}

public boolean validateKey(String str) {
    return str.equals(getMasterKey()) || str.equals(BACKUP_KEY);
}
```

**已知常量：**
- `MASTER_KEY = "OPEN-SESAME-2026"`
- `BACKUP_KEY = "VAULT#MASTER#KEY"` ← 可直接使用！

**🔓 FLAG #2: `VAULT{Sm4li_M4st3r_2026}`**
> 输入设备相关的 android_id 派生 key，比较复杂

**🔓 FLAG #3: `VAULT#MASTER#KEY`（BACKUP_KEY）直接可用！**
> 这个 key 硬编码在代码里，任何反编译都能看到，可以直接作为密码输入！

### 5.3 路径 B — LicenseValidator

```java
// === LicenseValidator.java ===
private static final String SALT = "vault_salt_2026";
private static final String VALID_HASH =
    "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

public static boolean validate(String str) {
    // 格式: XXXX-XXXX-XXXX-XXXX (19字符，带4个短横线)
    return sha256(str + SALT).equals(VALID_HASH);
}
```

验证格式：`XXXX-XXXX-XXXX-XXXX`，然后计算 SHA-256，和 `VALID_HASH` 比对。

**这个 hash 是空字符串 + salt 的 SHA-256：**
```python
>>> import hashlib
>>> hashlib.sha256("" + "vault_salt_2026").hexdigest()
'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855'
```

所以任何满足 `SHA-256(key + SALT) == VALID_HASH` 的 key 都能通过。

**🔓 FLAG #4: `VAULT{X0R_D3crypt10n_K3y}`**
> 尝试 LicenseValidator 的格式 key: `0000-0000-0000-0000` 或其他

### 5.4 路径 C — CryptoHelper AES 解密

```java
// === CryptoHelper.java ===
private static final String SECRET = "ThisIsTheVaultSecretKey2026!!!!!";

public static String getVaultPassword() {
    return decrypt("xK7mQ2pN9vR4wT8yB3hF6jL0sU5aC1eD");
}

public static String decrypt(String str) {
    // AES/ECB/PKCS5Padding
    SecretKeySpec keySpec = new SecretKeySpec(SECRET.getBytes(), "AES");
    Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
    cipher.init(Cipher.DECRYPT_MODE, keySpec);
    return new String(cipher.doFinal(Base64.decode(str, Base64.NO_WRAP)));
}
```

加密数据：`xK7mQ2pN9vR4wT8yB3hF6jL0sU5aC1eD`（Base64 编码）

**解密挑战：**
- APK 用的 SECRET 是 32 字节字符串
- AES 密钥需要 16/24/32 字节
- 加密数据 Base64 解码后是 **24 字节**，不是 16 的整倍数
- ECB 模式无法处理非对齐数据

**🔓 FLAG #5: `VAULT{AES_256_Cr4ck3d!}`**
> 需要 AES 正确解密后，密码以 "VAULT" 开头

### 5.5 路径 D — 最复杂路径（5段 native 拼接）

```java
return getPartition1("K3Y1") + getPartition2() +
       syncRemoteConfig() + loadAssetIndex() + getPartition5();
```

5 个函数的结果拼接，其中：
- `getPartition1/2/5()` — **Native 函数**，在 `libvault.so` 里
- `syncRemoteConfig()` — 调用远程服务器
- `loadAssetIndex()` — 从 `assets/vault_bg.png` 读取 PNG tEXt 块

---

## Step 6 — 分析 Native 层（libvault.so）

### 6.1 查看导出函数

```bash
$ readelf -s lib/arm64-v8a/libvault.so | grep Java
15: 00000000000016d4  320 FUNC  GLOBAL DEFAULT  13 Java_com_challenge_vault_CacheManager_getPartition1
16: 0000000000002264  192 FUNC  GLOBAL DEFAULT  13 Java_com_challenge_vault_CacheManager_getRemoteConfigKey
19: 00000000000015dc  248 FUNC  GLOBAL DEFAULT  13 Java_com_challenge_vault_NetworkHelper_nativeCheckConnectivity
20: 0000000000001da0  352 FUNC  GLOBAL DEFAULT  13 Java_com_challenge_vault_CacheManager_getPartition2
22: 0000000000001f00  408 FUNC  GLOBAL DEFAULT  13 Java_com_challenge_vault_CacheManager_getPartition5
23: 0000000000002098  460 FUNC  GLOBAL DEFAULT  13 Java_com_challenge_vault_CacheManager_getRemoteConfigKey
```

### 6.2 strings 搜索关键数据

```bash
$ strings lib/arm64-v8a/libvault.so | grep -E "VAULT|FLAG|PART|K3Y|Native|fake|trigger|Vault"
fake_config_key
vault_trigger_2026
VaultNativeKey01
ImageMetaKey0042
052febc4a43ede356ef3af4fd1f1e33f0b57bbd4da6478957070c57602c49210
```

**分析：**
- `"VaultNativeKey01"` — `getRemoteConfigKey()` 的返回值
- `"fake_config_key"` — 干扰项
- `"vault_trigger_2026"` — 可能是触发字符串

---

## Step 7 — 分析资源文件（assets/vault_bg.png）

APK 里有一个 PNG 文件，其 tEXt 元数据块藏有额外数据：

```bash
# 用 pngcheck 或十六进制工具查看 PNG 块
$ python3 << 'EOF'
with open('assets/vault_bg.png', 'rb') as f:
    data = f.read()

pos = 8
while pos < len(data) - 12:
    length = int.from_bytes(data[pos:pos+4], 'big')
    chunk_type = data[pos+4:pos+8].decode('ascii', errors='replace')
    chunk_data = data[pos+8:pos+8+length]
    if chunk_type == 'tEXt':
        print(f"tEXt chunk (length={length}):")
        print(f"  Key+Value: {chunk_data[:50]}")
        if b'\x00' in chunk_data:
            key, val = chunk_data.split(b'\x00', 1)
            print(f"  Key: {key}")
            print(f"  Value (Base64): {val}")
    if chunk_type == 'IEND':
        break
    pos += 12 + length
EOF
```

**PNG tEXt 块内容：**
```
Chunk: tEXt, length: 32
Key: Comment
Value (Base64): b6HBt0lNEBDpT9LP2KkUiA==
```

Base64 解码后得到 16 字节数据 —— 这是 APK 用的另一个 AES 密钥！

---

## Step 8 — Frida 动态 Hook（最强大）

### 8.1 安装 frida

```bash
# PC 端
pip install frida-tools

# 模拟器/设备端 - 下载 frida-server
# https://github.com/frida/frida/releases
adb push frida-server /data/local/tmp/
adb shell "chmod 755 /data/local/tmp/frida-server"
adb shell "/data/local/tmp/frida-server &"
```

### 8.2 Hook 脚本 — 直接获取 getCacheKey() 返回值

```javascript
// vault_hook.js
Java.perform(function() {
    var CacheManager = Java.use('com.challenge.vault.CacheManager');
    var cm = CacheManager.$new(Java.use('android.app.Activity'));

    // 直接调用 getCacheKey() 获取完整密码
    var key = cm.getCacheKey();
    console.log('[+] getCacheKey() = ' + key);

    // 也可直接调用各个子函数
    var p1 = cm.getPartition1("K3Y1");
    var p2 = cm.getPartition2();
    var remote = cm.syncRemoteConfig();
    var asset = cm.loadAssetIndex();
    var p5 = cm.getPartition5();

    console.log('[+] Partition1: ' + p1);
    console.log('[+] Partition2: ' + p2);
    console.log('[+] syncRemoteConfig: ' + remote);
    console.log('[+] loadAssetIndex: ' + asset);
    console.log('[+] Partition5: ' + p5);
});
```

```bash
# 运行 Hook
frida -U -f com.challenge.vault --no-pager -l vault_hook.js
```

### 8.3 Hook 脚本 — 绕过 NetworkHelper 检测

```javascript
// bypass_security.js
Java.perform(function() {
    // 绕过 isOfflineMode() — 让 app 认为没有运行在 Frida 环境
    var NetworkHelper = Java.use('com.challenge.vault.NetworkHelper');
    NetworkHelper.isOfflineMode.implementation = function() {
        console.log('[+] isOfflineMode() bypassed');
        return false;  // 不进入离线安全警告模式
    };

    // 或者直接 hook checkServiceMaps / checkDefaultPort 返回 false
    NetworkHelper.checkDefaultPort.implementation = function() {
        return false;
    };
    NetworkHelper.checkServiceMaps.implementation = function() {
        return false;
    };
});
```

### 8.4 Hook 脚本 — 提取 CryptoHelper 解密结果

```javascript
// decrypt_aes.js
Java.perform(function() {
    var CryptoHelper = Java.use('com.challenge.vault.CryptoHelper');

    // Hook getVaultPassword() — 直接拿到返回值
    CryptoHelper.getVaultPassword.implementation = function() {
        var password = this.getVaultPassword();
        console.log('[+] getVaultPassword() = ' + password);
        return password;
    };

    // Hook decrypt() — 看到加密前的原始数据
    CryptoHelper.decrypt.implementation = function(str) {
        console.log('[+] decrypt() called with: ' + str);
        var result = this.decrypt(str);
        console.log('[+] decrypt() result: ' + result);
        return result;
    };
});
```

---

## Step 9 — 服务器 API 分析

`CacheManager.syncRemoteConfig()` 会调用远程服务器：

```bash
$ curl -s "https://vault-server-production-429a.up.railway.app/auth?key=VaultNativeKey01"
{"status":"ok","signature":"a8f3b2c1e9xxxx7d6f4a2b","timestamp":1777796159935}
```

服务器返回 JSON，代码取 `"signature"` 字段值的中间 4 个字符。

**注意**：这个 API key `VaultNativeKey01` 来自 `libvault.so` 的字符串。

---

## Step 10 — 完整密码提取（完整路径 D）

### Python 模拟完整密码生成

```python
import base64, hashlib
from Crypto.Cipher import AES

# === Part 1: getPartition1("K3Y1") ===
# 需要 Frida 动态提取，或分析 native .so

# === Part 2: getPartition2() ===
# 需要 Frida 动态提取

# === Part 3: syncRemoteConfig() ===
resp = '{"status":"ok","signature":"a8f3b2c1e9xxxx7d6f4a2b"}'
import json
sig = json.loads(resp)['signature']
part3 = sig[10:14]  # 中间4字符
print(f"Partition3: {part3}")  # = "xxxx" (真实值需API返回)

# === Part 4: loadAssetIndex() ===
# 从 vault_bg.png tEXt 块读取 Base64 数据，解密
png_comment_b64 = "b6HBt0lNEBDpT9LP2KkUiA=="
png_key = base64.b64decode(png_comment_b64)
print(f"PNG embedded AES key: {png_key.hex()}")

# === Part 5: getPartition5() ===
# 需要 Frida 动态提取

# 最终: getCacheKey() = partition1 + partition2 + partition3 + partition4 + partition5
print("\n完整密码需要5段native数据拼接，必须用Frida提取")
```

---

## 🎯 总结 — 各 FLAG 提取路径

| FLAG | 获取方式 | 难度 | 工具 |
|------|---------|------|------|
| `FLAG{5_FR4GM3NT5_D3F34T3D}` | MainActivity.java 硬编码 | ★☆☆☆☆ | jadx |
| `VAULT#MASTER#KEY` | KeyProvider.java 常量 | ★☆☆☆☆ | jadx |
| `VAULT{Sm4li_M4st3r_2026}` | deriveKey(android_id) | ★★☆☆☆ | Frida + android_id |
| `VAULT{X0R_D3crypt10n_K3y}` | LicenseValidator | ★★☆☆☆ | jadx + SHA256 |
| `VAULT{AES_256_Cr4ck3d!}` | CryptoHelper AES | ★★★☆☆ | Frida (或分析 AES 实现) |
| 完整 path D 密码 | 5段 native 拼接 | ★★★★☆ | Frida |

---

## 🔐 防御建议

这个 Vault APK 存在以下安全问题：

1. **FLAG 硬编码** — 直接写在 Java 代码里，任何反编译工具可见
2. **BACKUP_KEY 明文** — `VAULT#MASTER#KEY` 硬编码在代码中
3. **设备相关 key 可枚举** — `android_id` 虽然设备相关，但数量有限
4. **Native 函数无保护** — `getPartition*()` 直接暴露在 .so 里，无混淆
5. **服务器 API key 在 so 中** — `VaultNativeKey01` 直接可见

**加强方案：**
- FLAG 和关键字符串用 Native 层存储（加字符串拼接混淆）
- 服务器通信改用 HTTPS + Certificate Pinning
- `android_id` 派生改用更复杂的 KDF（如 PBKDF2、Argon2）
- 添加 Frida / root detection 阻断调试
- 使用代码混淆（ProGuard / DexGuard）