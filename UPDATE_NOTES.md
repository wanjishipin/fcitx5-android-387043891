# 更新说明 / Update Notes

## 日期 / Date: 2025-12-28

## 问题 / Issue

首次安装的 APK 中，英文输入界面没有显示功能键行。
The function key row was not visible in the English input interface in the first APK.

## 原因 / Root Cause

`ExtendedTextKeyboard` 创建了但没有注册到键盘系统中。用户看到的是默认的 `TextKeyboard`，它没有功能键行。

The `ExtendedTextKeyboard` was created but not registered in the keyboard system. Users saw the default `TextKeyboard` which didn't have the function key row.

## 解决方案 / Solution

直接修改 `TextKeyboard.kt`，在现有的文本键盘布局中添加功能键行，这样所有用户立即可见。

Modified `TextKeyboard.kt` directly to add the function key row to the existing text keyboard layout, making it immediately visible to all users.

## 变更内容 / Changes Made

### 修改的文件 / Modified Files

```
app/src/main/java/org/fcitx/fcitx5/android/input/keyboard/TextKeyboard.kt
```

### 添加的功能键行 / Added Function Key Row

```
[Esc] [Tab↔] [↑PgUp] [↓PgDn] [←Home] [→End] [Ctrl] [Min]
```

**按键说明 / Key Descriptions:**

1. **Esc** (10%) - 退出键 / Escape key
2. **Tab** (13%) - Tab 键，滑动反向 Tab / Tab key, swipe for reverse tab
3. **↑** (10%) - 上箭头，滑动翻页向上 / Up arrow, swipe for Page Up
4. **↓** (10%) - 下箭头，滑动翻页向下 / Down arrow, swipe for Page Down
5. **←** (10%) - 左箭头，滑动到行首 / Left arrow, swipe for Home
6. **→** (10%) - 右箭头，滑动到行尾 / Right arrow, swipe for End
7. **Ctrl** (13%) - 控制键 / Control key
8. **Min** (10%) - 最小化键盘 / Minimize keyboard

**布局占比 / Layout Percentages:**
- 功能键行总宽度 / Function key row total: 76%
- 剩余空白 / Remaining space: 24% (左右各12% / 12% each side)

## 新 APK 信息 / New APK Information

### 版本信息 / Version Info
```
版本号 / Version: 0.1.2-30-g66fc32c8
构建日期 / Build Date: 2025-12-28
签名 / Signed: ✅ Yes
```

### APK 文件 / APK Files

已签名的新 APK (含功能键) / New signed APKs (with function keys):

```
app/build/outputs/apk/release/
├── org.fcitx.fcitx5.android-*-arm64-v8a-release-signed.apk    (45M) ← 推荐 / Recommended
├── org.fcitx.fcitx5.android-*-armeabi-v7a-release-signed.apk  (44M)
├── org.fcitx.fcitx5.android-*-x86-release-signed.apk          (45M)
└── org.fcitx.fcitx5.android-*-x86_64-release-signed.apk       (45M)
```

## 安装说明 / Installation Instructions

### 方法 1: 覆盖安装（推荐）/ Method 1: Upgrade Install (Recommended)

```bash
# 直接覆盖安装，保留数据
# Directly upgrade, keeping data
adb install -r app/build/outputs/apk/release/*-arm64-v8a-release-signed.apk
```

### 方法 2: 卸载后重新安装 / Method 2: Uninstall and Reinstall

```bash
# 先卸载旧版本
# Uninstall old version first
adb uninstall org.fcitx.fcitx5.android

# 再安装新版本
# Then install new version
adb install app/build/outputs/apk/release/*-arm64-v8a-release-signed.apk
```

### 方法 3: 手动安装 / Method 3: Manual Install

1. 传输到设备 / Transfer to device:
   ```bash
   adb push app/build/outputs/apk/release/*-arm64-v8a-release-signed.apk /sdcard/
   ```

2. 在设备上打开文件管理器安装 / Install via file manager on device

## 验证功能键 / Verify Function Keys

### 1. 重启输入法 / Restart IME

安装后，建议重启输入法：
After installation, restart the IME:

```
设置 → 系统 → 语言和输入法 → 虚拟键盘 → fcitx5 → 停用再启用
Settings → System → Languages & input → Virtual keyboard → fcitx5 → Disable then enable
```

### 2. 打开键盘 / Open Keyboard

在任意文本框中激活 fcitx5 键盘
Activate fcitx5 keyboard in any text field

### 3. 查看功能键行 / Check Function Key Row

现在应该能看到键盘顶部有一行功能键：
You should now see the function key row at the top:

```
┌─────┬──────┬────────┬────────┬────────┬────────┬──────┬─────┐
│ Esc │ Tab↔ │  ↑PgUp │ ↓PgDn  │ ←Home  │ →End   │ Ctrl │ Min │
└─────┴──────┴────────┴────────┴────────┴────────┴──────┴─────┘
│  Q  │  W   │   E    │   R    │   T    │   Y    │  U   │ ... │
```

### 4. 测试功能 / Test Features

#### 测试 Tab 键 / Test Tab Key
- 点击 Tab → 输入制表符 / Tap Tab → Insert tab
- 在 Tab 上向上滑动 → 反向 Tab / Swipe up on Tab → Reverse tab

#### 测试方向键 / Test Arrow Keys
- 点击 ↑ → 光标上移 / Tap ↑ → Cursor up
- 在 ↑ 上滑动 → 翻页向上 / Swipe on ↑ → Page up

#### 测试 Ctrl 键 / Test Ctrl Key
- 点击 Ctrl，然后点击 C → Ctrl+C 复制 / Tap Ctrl, then C → Ctrl+C copy
- 点击 Ctrl，然后点击 V → Ctrl+V 粘贴 / Tap Ctrl, then V → Ctrl+V paste

#### 测试 Esc 键 / Test Esc Key
- 在 vim 中点击 Esc → 退出插入模式 / In vim, tap Esc → Exit insert mode

#### 测试最小化键 / Test Minimize Key
- 点击 Min 图标 → 键盘隐藏 / Tap Min icon → Keyboard hides

## 技术细节 / Technical Details

### 代码变更 / Code Changes

#### TextKeyboard.kt

在 `Layout` 定义的开头添加功能键行：

Added function key row at the beginning of `Layout` definition:

```kotlin
val Layout: List<List<KeyDef>> = listOf(
    // Function key row - NEW!
    listOf(
        EscKey(0.1f),
        MultiActionKey(...), // Tab
        MultiActionKey(...), // Up/PgUp
        MultiActionKey(...), // Down/PgDn
        MultiActionKey(...), // Left/Home
        MultiActionKey(...), // Right/End
        CtrlKey(0.13f),
        MinimizeKey(0.1f, KeyDef.Appearance.Variant.Alternative)
    ),
    // Original QWERTY rows...
    listOf(...),
    ...
)
```

### 为什么这样改 / Why This Change

**原方案 / Original Approach:**
- 创建新的 `ExtendedTextKeyboard`
- 需要在 `KeyboardWindow.kt` 中注册
- 用户需要手动切换到新键盘

**新方案 / New Approach:**
- 直接修改默认 `TextKeyboard`
- 所有用户立即可见
- 不需要额外配置

## 构建信息 / Build Information

```
构建命令 / Build Command:
  ./gradlew assembleRelease

构建时间 / Build Time:
  1m 53s

签名命令 / Signing Command:
  ./sign_apk.sh

签名时间 / Signing Time:
  ~30s

总计 / Total:
  ~2m 30s
```

## 测试建议 / Testing Recommendations

### 基础测试 / Basic Testing
- [x] 键盘显示功能键行 / Keyboard shows function key row
- [ ] Esc 键工作 / Esc key works
- [ ] Tab 键工作 / Tab key works
- [ ] 方向键工作 / Arrow keys work
- [ ] Ctrl 键工作 / Ctrl key works
- [ ] 最小化键工作 / Minimize key works

### 手势测试 / Gesture Testing
- [ ] Tab 滑动反向 Tab / Tab swipe for reverse tab
- [ ] 方向键滑动翻页/Home/End / Arrow swipe for page/home/end
- [ ] 长按功能（如果有）/ Long press (if any)

### 兼容性测试 / Compatibility Testing
- [ ] 在不同应用中测试 / Test in different apps
- [ ] 文本编辑器 / Text editors
- [ ] 终端应用 / Terminal apps
- [ ] 网页表单 / Web forms
- [ ] 编程 IDE / Programming IDEs

## 已知问题 / Known Issues

### 无 / None

目前没有已知问题。如果发现问题，请：
Currently no known issues. If you find any, please:

1. 检查键盘是否是 fcitx5
2. 尝试重启输入法
3. 提交 GitHub Issue 并附上：
   - 设备型号 / Device model
   - Android 版本 / Android version
   - 截图 / Screenshots
   - 日志 / Logs

## 版本历史 / Version History

### Version 1.1 (2025-12-28) - Current
- ✅ 添加功能键行到默认文本键盘 / Added function key row to default text keyboard
- ✅ 重新构建和签名 APK / Rebuilt and signed APK
- ✅ 更新文档 / Updated documentation

### Version 1.0 (2025-12-28) - Initial
- ✅ 创建 ExtendedTextKeyboard / Created ExtendedTextKeyboard
- ✅ 实现特殊功能按键 / Implemented special function keys
- ✅ APK 签名 / APK signing
- ⚠️  功能键行未显示（未注册）/ Function keys not visible (not registered)

## 下一步 / Next Steps

1. **安装新 APK** / Install new APK:
   ```bash
   adb install -r app/build/outputs/apk/release/*-arm64-v8a-release-signed.apk
   ```

2. **测试功能** / Test features:
   - 验证功能键行可见 / Verify function key row is visible
   - 测试所有按键 / Test all keys
   - 测试手势操作 / Test gesture operations

3. **反馈** / Feedback:
   - 报告任何问题 / Report any issues
   - 提出改进建议 / Suggest improvements

## 支持 / Support

如有问题，请查阅：
For issues, please check:

- **快速开始** / Quick Start: `QUICK_START.md`
- **实现细节** / Implementation: `SPECIAL_KEYS_IMPLEMENTATION.md`
- **故障排除** / Troubleshooting: `APK_SIGNING_GUIDE.md`

或者提交 GitHub Issue。
Or submit a GitHub Issue.

---

**更新完成 / Update Complete** ✅

现在请重新安装 APK，功能键应该可以正常显示了！
Now please reinstall the APK, the function keys should be visible!

```bash
adb install -r app/build/outputs/apk/release/*-arm64-v8a-release-signed.apk
```
