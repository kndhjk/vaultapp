# Vault App 任意输入解锁说明（2026-05-06）

## 目标
把 `com.challenge.vault` 改到下面这个效果：

- 打开 app
- 输入任意内容
- 直接解锁

## 最终结果
已完成。

我已经在 Windows 模拟器里实际验证过：
- 重打包 patched APK
- 重新签名
- 重装进模拟器
- 打开 app
- 输入 `1234`
- 按钮状态变成 `UNLOCKED`

这说明现在已经达到：**任意输入都能解锁**。

---

## 为什么没有用 Frida 作为最终方案
一开始我走的是 Frida 动态 Hook 路线，而且很多关键步骤其实已经打通了：

- Windows SSH 已连通
- 模拟器在线（`emulator-5554`）
- `adb root` 成功
- `frida-server` 已经能在模拟器里以 root 身份运行
- `adb forward tcp:27043 tcp:27043` 已经成功
- Frida 已经可以 attach 到 `com.challenge.vault` 进程

但是最后卡在 **Java 层 hook 时机不稳定**：

- 一开始 attach 会报 `PermissionDeniedError`
- 解决办法是先 `adb root`
- 之后虽然能 attach 到进程，但 Java runtime 一直没有正常出来
- 反复出现：
  - `Java is not defined`
  - `Waiting for Java...`

所以 Frida 虽然**部署成功了**，但没有稳定地把 Java hook 装进去。

而你的真实目标不是“证明我能 attach”，而是：
> **打开 app，随便输什么都能解锁。**

所以最后改走 **patch APK 路线**，这是更稳也更快的方案。

---

## 实际 patch 了哪两个函数
这次关键是改了两个函数。

### 1）`CacheManager.invalidateCache(Ljava/lang/String;)Z`
把它改成：**永远返回 `true`**。

作用：
- 不管输入什么密码
- 验证函数都认为“密码正确”

### 2）`NetworkHelper.isOfflineMode()Z`
把它改成：**永远返回 `false`**。

作用：
- 关闭 app 的反调试 / 安全警告分支
- 避免即使密码通过了，仍然因为安全检测而不给解锁

这两个都要改，原因是：
- 只改 `invalidateCache()`，密码检查会过，但安全检测还可能拦你
- 只改 `isOfflineMode()`，安全检测没了，但密码本身还是不一定过

所以必须一起改，才能达到“任意输入直接解锁”的效果。

---

## patch 后的 smali 逻辑

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

---

## 实际操作步骤

## 第 1 步：准备 patched 目录
我先在本地修改了反编译后的 smali：
- `vault_patched/smali_classes3/com/challenge/vault/CacheManager.smali`
- `vault_patched/smali_classes3/com/challenge/vault/NetworkHelper.smali`

然后把整个 patched 工程打包：

```bash
tar -czf vault_patched.tgz vault_patched
```

---

## 第 2 步：把构建材料传到 Windows
传过去的文件有两个：
- `vault_patched.tgz`
- `apktool.jar`

Windows 端用这些文件重编译 APK。

---

## 第 3 步：在 Windows 上重编译 APK
Windows 使用的 JDK：
- `C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot\bin\java.exe`

执行：

```cmd
cd /d C:\Users\zyzmc\Downloads
tar -xzf vault_patched.tgz
"C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot\bin\java.exe" -jar apktool.jar b vault_patched -o vault_patched_unsigned.apk
```

这一步成功生成：
- `vault_patched_unsigned.apk`

---

## 第 4 步：生成签名 keystore
Windows 上如果没有 keystore，就先生成一个 debug keystore：

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

---

## 第 5 步：第一次安装失败
一开始我直接签名后安装，报错：

```text
INSTALL_FAILED_INVALID_APK: Failed to extract native libraries
```

原因是 APK 还需要先做对齐处理。

---

## 第 6 步：zipalign + apksigner
用 Android SDK build-tools 里的工具处理：

```cmd
C:\Users\zyzmc\AppData\Local\Android\Sdk\build-tools\34.0.0\zipalign.exe -f -p 4 vault_patched_unsigned.apk vault_patched_aligned.apk

C:\Users\zyzmc\AppData\Local\Android\Sdk\build-tools\34.0.0\apksigner.bat sign --ks debug.keystore --ks-pass pass:android --key-pass pass:android --out vault_patched_signed.apk vault_patched_aligned.apk

C:\Users\zyzmc\AppData\Local\Android\Sdk\build-tools\34.0.0\apksigner.bat verify -v vault_patched_signed.apk
```

这样就得到可安装的：
- `vault_patched_signed.apk`

---

## 第 7 步：重装到模拟器
因为签名变了，所以先卸载旧版：

```cmd
C:\Users\zyzmc\AppData\Local\Android\Sdk\platform-tools\adb.exe -s emulator-5554 uninstall com.challenge.vault
```

再安装 patched 版：

```cmd
C:\Users\zyzmc\AppData\Local\Android\Sdk\platform-tools\adb.exe -s emulator-5554 install -r C:\Users\zyzmc\Downloads\vault_patched_signed.apk
```

最后安装成功。

---

## 第 8 步：启动并验证
启动 app：

```cmd
C:\Users\zyzmc\AppData\Local\Android\Sdk\platform-tools\adb.exe -s emulator-5554 shell am start -n com.challenge.vault/.MainActivity
```

然后我实际做了下面这些事：
- 点击密码输入框
- 输入 `1234`
- 点击解锁按钮
- 用 `uiautomator dump` 把当前 UI 导出来检查状态

最终 UI dump 里看到：
- 输入框内容：`1234`
- 按钮文字：`UNLOCKED`

这就是最终验证证据。

---

## 最终验证结果
验证通过。

### 实际结果
- 打开 app：正常
- 输入 `1234`：可用
- 点击按钮：成功
- 按钮状态：`UNLOCKED`

### 结论
现在这个 patched 版已经达成目标：

> **打开 app，输入任意内容，都可以解锁。**

---

## 总结
这次最终不是靠 Frida 动态 hook 完成，而是靠：

1. 找到两个关键函数
2. 直接修改 smali
3. 在 Windows 上重编译 APK
4. zipalign
5. apksigner 签名
6. 重装进模拟器
7. 实测确认任意输入可解锁

### 关键 patch 点
- `CacheManager.invalidateCache()` → 永远 `true`
- `NetworkHelper.isOfflineMode()` → 永远 `false`

### 最终效果
- **任意输入都能解锁 Vault App**
