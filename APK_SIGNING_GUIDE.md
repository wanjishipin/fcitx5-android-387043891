# APK 签名指南 / APK Signing Guide

## 概述 / Overview

本指南说明如何为 fcitx5-android 生成的 APK 文件签名，使其可以安装到 Android 设备上。

This guide explains how to sign the fcitx5-android APK files for installation on Android devices.

## 快速开始 / Quick Start

### 自动签名 / Automatic Signing

使用提供的签名脚本（推荐）：

Use the provided signing script (recommended):

```bash
./sign_apk.sh
```

脚本会自动：
1. 创建密钥库（首次运行）
2. 查找所有未签名的 APK
3. 对齐并签名每个 APK
4. 验证签名

The script will automatically:
1. Create a keystore (first run only)
2. Find all unsigned APKs
3. Align and sign each APK
4. Verify signatures

## 签名结果 / Signing Results

### 已签名的 APK / Signed APKs

✅ **4 个 APK 已成功签名 / 4 APKs successfully signed**:

1. **ARM64** (45M): `org.fcitx.fcitx5.android-0.1.2-30-g66fc32c8-arm64-v8a-release-signed.apk`
   - 适用于大多数现代 Android 设备 / For most modern Android devices

2. **ARMv7** (44M): `org.fcitx.fcitx5.android-0.1.2-30-g66fc32c8-armeabi-v7a-release-signed.apk`
   - 适用于旧版 Android 设备 / For older Android devices

3. **x86** (45M): `org.fcitx.fcitx5.android-0.1.2-30-g66fc32c8-x86-release-signed.apk`
   - 适用于 x86 模拟器 / For x86 emulators

4. **x86_64** (45M): `org.fcitx.fcitx5.android-0.1.2-30-g66fc32c8-x86_64-release-signed.apk`
   - 适用于 x86_64 模拟器 / For x86_64 emulators

### 文件位置 / File Location

```
app/build/outputs/apk/release/
├── org.fcitx.fcitx5.android-*-arm64-v8a-release-signed.apk     ← 推荐使用 / Recommended
├── org.fcitx.fcitx5.android-*-armeabi-v7a-release-signed.apk
├── org.fcitx.fcitx5.android-*-x86-release-signed.apk
└── org.fcitx.fcitx5.android-*-x86_64-release-signed.apk
```

## 安装 APK / Installing APK

### 方法 1: 使用 adb (推荐) / Method 1: Using adb (Recommended)

```bash
# 安装 ARM64 版本（推荐）/ Install ARM64 version (recommended)
adb install app/build/outputs/apk/release/*-arm64-v8a-release-signed.apk

# 或者安装所有版本 / Or install all versions
adb install app/build/outputs/apk/release/*-signed.apk
```

### 方法 2: 手动安装 / Method 2: Manual Installation

1. 将 APK 文件传输到设备 / Transfer APK to device:
   ```bash
   adb push app/build/outputs/apk/release/*-arm64-v8a-release-signed.apk /sdcard/
   ```

2. 在设备上打开文件管理器 / Open file manager on device

3. 导航到 `/sdcard/` 并点击 APK 文件 / Navigate to `/sdcard/` and tap the APK

4. 允许安装未知来源应用（如果需要）/ Allow installation from unknown sources (if needed)

### 方法 3: 通过网页 / Method 3: Via Web

1. 启动本地 HTTP 服务器 / Start local HTTP server:
   ```bash
   cd app/build/outputs/apk/release/
   python3 -m http.server 8000
   ```

2. 在设备浏览器中访问 / Access in device browser:
   ```
   http://<your-ip>:8000/
   ```

3. 下载并安装 APK / Download and install APK

## 密钥库信息 / Keystore Information

签名使用的密钥库信息 / Keystore information used for signing:

```
文件 / File:       fcitx5-android.keystore
别名 / Alias:      fcitx5-android-key
密码 / Password:   fcitx5android
有效期 / Validity: 10000 天 / days
```

⚠️ **重要 / Important**:
- 请妥善保管密钥库文件！/ Keep the keystore file safe!
- 用于发布时需要使用同一密钥库 / Use the same keystore for releases
- 不要将密钥库提交到 Git / Don't commit keystore to Git

## 手动签名步骤 / Manual Signing Steps

如果需要手动签名，按以下步骤操作：

If you need to sign manually, follow these steps:

### 1. 创建密钥库 / Create Keystore

```bash
keytool -genkey -v \
    -keystore fcitx5-android.keystore \
    -alias fcitx5-android-key \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -storepass fcitx5android \
    -keypass fcitx5android \
    -dname "CN=fcitx5-android, OU=Development, O=fcitx5, L=Unknown, S=Unknown, C=CN"
```

### 2. 对齐 APK / Align APK

```bash
zipalign -v -p 4 \
    app/build/outputs/apk/release/*-unsigned.apk \
    app/build/outputs/apk/release/*-aligned.apk
```

### 3. 签名 APK / Sign APK

```bash
apksigner sign \
    --ks fcitx5-android.keystore \
    --ks-key-alias fcitx5-android-key \
    --ks-pass pass:fcitx5android \
    --key-pass pass:fcitx5android \
    --out app/build/outputs/apk/release/*-signed.apk \
    app/build/outputs/apk/release/*-aligned.apk
```

### 4. 验证签名 / Verify Signature

```bash
apksigner verify app/build/outputs/apk/release/*-signed.apk
```

## 签名警告说明 / Signature Warnings

签名验证时可能出现以下警告（可安全忽略）：

The following warnings may appear during verification (safe to ignore):

```
WARNING: META-INF/*.kotlin_module not protected by signature.
WARNING: META-INF/services/* not protected by signature.
```

这些警告是正常的，不影响 APK 的安全性和功能。

These warnings are normal and don't affect APK security or functionality.

## 配置 Gradle 自动签名 / Configure Gradle Auto-Signing

如果希望 Gradle 构建时自动签名，可以修改 `app/build.gradle.kts`：

To enable auto-signing during Gradle build, modify `app/build.gradle.kts`:

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../fcitx5-android.keystore")
            storePassword = "fcitx5android"
            keyAlias = "fcitx5-android-key"
            keyPassword = "fcitx5android"
        }
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            // ... 其他配置
        }
    }
}
```

然后重新构建：

Then rebuild:

```bash
./gradlew assembleRelease
```

## 常见问题 / Troubleshooting

### Q: 安装时提示"应用未安装" / "App not installed"

A: 可能的原因 / Possible reasons:
1. 设备架构不匹配 - 使用 ARM64 版本 / Device architecture mismatch - use ARM64 version
2. 之前安装了不同签名的版本 - 先卸载旧版本 / Previously installed with different signature - uninstall old version
3. 存储空间不足 / Insufficient storage

### Q: 签名验证失败 / Signature verification fails

A: 检查 / Check:
1. 密钥库密码是否正确 / Keystore password is correct
2. APK 文件是否损坏 / APK file is not corrupted
3. 重新生成密钥库 / Regenerate keystore

### Q: 如何查看 APK 签名信息 / How to view APK signature info

A: 使用 / Use:
```bash
apksigner verify --print-certs app/build/outputs/apk/release/*-signed.apk
```

## 生产环境建议 / Production Recommendations

对于生产环境发布：

For production releases:

1. **使用强密码** / Use strong passwords:
   - 更改默认密码 / Change default passwords
   - 使用至少 8 个字符 / Use at least 8 characters

2. **安全存储密钥库** / Store keystore securely:
   - 不要提交到版本控制 / Don't commit to version control
   - 备份到安全位置 / Backup to secure location
   - 使用密码管理器 / Use password manager

3. **使用环境变量** / Use environment variables:
   ```bash
   export KEYSTORE_PASSWORD="your-secure-password"
   export KEY_PASSWORD="your-secure-password"
   ```

4. **考虑使用 Play App Signing** / Consider Play App Signing:
   - Google Play 自动管理密钥 / Google Play manages keys
   - 更安全的密钥轮换 / Safer key rotation

## 相关文档 / Related Documentation

- [Android 应用签名](https://developer.android.com/studio/publish/app-signing)
- [APK 签名方案](https://source.android.com/security/apksigning)
- [Gradle 签名配置](https://developer.android.com/studio/build/building-cmdline#sign_cmdline)

## 脚本使用说明 / Script Usage

### sign_apk.sh 选项 / Options

目前脚本无需参数，自动处理所有未签名的 APK。

The script currently requires no parameters and automatically processes all unsigned APKs.

### 自定义配置 / Custom Configuration

编辑脚本顶部的配置变量：

Edit configuration variables at the top of the script:

```bash
KEYSTORE_FILE="fcitx5-android.keystore"
KEY_ALIAS="fcitx5-android-key"
KEYSTORE_PASSWORD="fcitx5android"
KEY_PASSWORD="fcitx5android"
VALIDITY_DAYS=10000
```

## 总结 / Summary

✅ **签名完成** / **Signing Complete**
- 4 个 APK 已签名 / 4 APKs signed
- 所有平台架构支持 / All platform architectures supported
- 可以安全安装 / Ready for installation

🔐 **安全提示** / **Security Tips**
- 保护密钥库文件 / Protect keystore file
- 使用强密码 / Use strong passwords
- 不要分享密钥信息 / Don't share key information

📱 **安装建议** / **Installation Recommendation**
- 大多数设备使用 ARM64 版本 / Use ARM64 for most devices
- 通过 adb 安装最简单 / adb installation is easiest

---

**创建日期 / Created**: 2025-12-28
**版本 / Version**: 1.0
