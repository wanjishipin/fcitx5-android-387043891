# Ctrl 修饰键更新 / Ctrl Modifier Key Update

## 更新日期 / Update Date: 2025-12-28

## 更新内容 / What's New

Ctrl 键现在作为**修饰键（Modifier Key）**工作，类似于 Caps Lock：
- 点击一次 Ctrl → 激活一次（高亮显示）
- 点击字母键 → 发送 Ctrl+字母 组合键
- 长按或双击 Ctrl → 锁定 Ctrl（持续高亮）
- 再次点击 Ctrl → 取消激活

The Ctrl key now works as a **Modifier Key**, similar to Caps Lock:
- Tap Ctrl once → Activate once (highlighted)
- Tap a letter key → Send Ctrl+letter combination
- Long press or double tap Ctrl → Lock Ctrl (highlighted)
- Tap Ctrl again → Deactivate

## 使用方法 / How to Use

### 单次使用 / Single Use
```
1. 点击 Ctrl 键 → Ctrl 高亮显示
   Tap Ctrl key → Ctrl is highlighted

2. 点击 C 键 → 发送 Ctrl+C（复制）
   Tap C key → Send Ctrl+C (copy)

3. Ctrl 自动取消高亮
   Ctrl automatically deactivates
```

### 锁定使用 / Lock Mode
```
1. 长按 Ctrl 键（或双击）→ Ctrl 高亮且锁定
   Long press Ctrl (or double tap) → Ctrl highlighted and locked

2. 点击多个字母 → 每次都发送 Ctrl+字母
   Tap multiple letters → Each sends Ctrl+letter

3. 再次点击 Ctrl → 解除锁定
   Tap Ctrl again → Unlock
```

## 视觉反馈 / Visual Feedback

Ctrl 键的三种状态：

Three states of Ctrl key:

| 状态 / State | 外观 / Appearance | 说明 / Description |
|------------|-----------------|-------------------|
| **正常 / Normal** | 默认样式 / Default style | Ctrl 未激活 / Ctrl not active |
| **激活一次 / Once** | 半透明高亮 / Semi-transparent highlight | 点击一次，使用后自动取消 / Tap once, auto-deactivate after use |
| **锁定 / Locked** | 完全高亮 / Full highlight | 长按/双击锁定，需手动取消 / Long press/double tap to lock |

## 常用组合键 / Common Key Combinations

### 文本编辑 / Text Editing
- **Ctrl + C** = 复制 / Copy
- **Ctrl + V** = 粘贴 / Paste
- **Ctrl + X** = 剪切 / Cut
- **Ctrl + A** = 全选 / Select All
- **Ctrl + Z** = 撤销 / Undo

### 终端操作 / Terminal Operations
- **Ctrl + C** = 中断进程 / Interrupt process
- **Ctrl + D** = EOF / End of file
- **Ctrl + L** = 清屏 / Clear screen
- **Ctrl + R** = 反向搜索 / Reverse search

### Vim 编辑 / Vim Editing
- **Ctrl + F** = 向下翻页 / Page down
- **Ctrl + B** = 向上翻页 / Page up
- **Ctrl + U** = 向上半页 / Half page up
- **Ctrl + D** = 向下半页 / Half page down

## 实现细节 / Implementation Details

### 核心变更 / Core Changes

1. **新增 ModifierAction** (`KeyAction.kt`):
   ```kotlin
   data class ModifierAction(
       val modifier: KeyState, 
       val lock: Boolean = false
   ) : KeyAction()
   ```

2. **Ctrl 状态管理** (`TextKeyboard.kt`):
   ```kotlin
   enum class ModifierState { None, Once, Lock }
   private var ctrlState: ModifierState = ModifierState.None
   ```

3. **按键转换逻辑**:
   - Ctrl 激活时，字母键自动转换为 Ctrl+字母
   - 使用 `KeyStates(KeyState.Ctrl, KeyState.Virtual)`

### 状态转换 / State Transitions

```
Normal ──(tap)──> Once ──(tap)──> Normal
  ↑                 │
  │              (use key)
  │                 │
  └────────────────┘

Normal ──(long press / double tap)──> Lock ──(tap Ctrl)──> Normal
```

## 技术规格 / Technical Specifications

### KeyState 集成 / KeyState Integration
```kotlin
// Ctrl 修饰键状态
KeyStates(KeyState.Ctrl, KeyState.Virtual)

// 支持的修饰键
enum class KeyState {
    Ctrl(1u shl 2),    // Ctrl 键
    Alt(1u shl 3),     // Alt 键
    Shift(1u shl 0),   // Shift 键
    Meta(1u shl 28),   // Meta 键
    // ...
}
```

### 字符转换 / Character Conversion
```kotlin
// 获取字符的小写形式
val char = action.act[0].lowercaseChar()

// 创建带 Ctrl 的 KeySym
KeySym(char.code)  // Unicode code point
```

## 测试说明 / Testing Instructions

### 基础测试 / Basic Test
1. 打开文本编辑器 / Open text editor
2. 输入一些文字 / Type some text
3. 点击 Ctrl → 点击 A / Tap Ctrl → Tap A
4. 验证: 文本全选 / Verify: Text is selected
5. 点击 Ctrl → 点击 C / Tap Ctrl → Tap C
6. 验证: 文本已复制 / Verify: Text is copied

### 锁定测试 / Lock Test
1. 长按 Ctrl 键 / Long press Ctrl
2. 验证: Ctrl 保持高亮 / Verify: Ctrl stays highlighted
3. 点击 C → 点击 V → 点击 A / Tap C → Tap V → Tap A
4. 验证: 每次都发送 Ctrl 组合键 / Verify: Each sends Ctrl combination
5. 点击 Ctrl / Tap Ctrl
6. 验证: Ctrl 取消高亮 / Verify: Ctrl deactivates

### 终端测试 / Terminal Test
1. 打开终端应用（如 Termux）/ Open terminal app (e.g., Termux)
2. 运行长时间命令 / Run a long command: `sleep 100`
3. 点击 Ctrl → 点击 C / Tap Ctrl → Tap C
4. 验证: 命令被中断 / Verify: Command is interrupted

## 已知限制 / Known Limitations

1. **应用兼容性** / App Compatibility
   - 某些应用可能不支持 Ctrl 组合键
   - Some apps may not support Ctrl combinations

2. **视觉反馈** / Visual Feedback
   - 目前使用半透明效果显示状态
   - Currently uses transparency to show state
   - 未来可能添加更明显的图标 / May add more obvious icons in future

3. **符号和数字** / Symbols and Numbers
   - Ctrl 主要用于字母键 / Ctrl mainly works with letter keys
   - 符号和数字的组合键支持取决于应用 / Symbol/number combinations depend on app

## 升级说明 / Upgrade Instructions

### 从之前版本升级 / Upgrading from Previous Version

如果您安装了之前的版本（Ctrl 作为普通键）:
If you installed the previous version (Ctrl as normal key):

```bash
# 覆盖安装（保留数据）
# Upgrade install (keep data)
adb install -r app/build/outputs/apk/release/*-arm64-v8a-release-signed.apk
```

### 新安装 / Fresh Install

```bash
# 新安装
# Fresh install
adb install app/build/outputs/apk/release/*-arm64-v8a-release-signed.apk
```

## 常见问题 / FAQ

### Q: Ctrl 键高亮后如何取消？
### Q: How to deactivate highlighted Ctrl key?

A: 再次点击 Ctrl 键即可取消高亮。
A: Tap the Ctrl key again to deactivate.

### Q: 如何知道 Ctrl 是否处于锁定状态？
### Q: How to know if Ctrl is locked?

A: 锁定状态下，Ctrl 键会保持完全高亮，即使点击其他键后也不会取消。
A: In locked state, Ctrl key stays fully highlighted even after pressing other keys.

### Q: Ctrl+字母 在某个应用不工作怎么办？
### Q: Ctrl+letter doesn't work in某 app?

A: 这可能是应用的限制。尝试在其他应用（如终端、文本编辑器）中测试。
A: This may be an app limitation. Try testing in other apps (e.g., terminal, text editor).

### Q: 可以同时使用 Ctrl 和 Shift 吗？
### Q: Can I use Ctrl and Shift together?

A: 当前版本暂不支持同时使用多个修饰键。这是一个计划中的功能。
A: Current version doesn't support multiple modifiers simultaneously. This is a planned feature.

## 未来改进 / Future Improvements

1. **多修饰键支持** / Multiple Modifiers
   - Ctrl+Shift+Key
   - Ctrl+Alt+Key

2. **更好的视觉反馈** / Better Visual Feedback
   - 专用图标表示状态 / Dedicated icons for states
   - 动画效果 / Animation effects

3. **Alt 和 Meta 键** / Alt and Meta Keys
   - 类似 Ctrl 的修饰键实现 / Similar modifier implementation

4. **自定义组合键** / Custom Key Combinations
   - 用户可配置的快捷键 / User-configurable shortcuts

## 版本历史 / Version History

### Version 1.2 (2025-12-28) - Current
- ✅ Ctrl 作为修饰键 / Ctrl as modifier key
- ✅ 单次激活模式 / Single activation mode
- ✅ 锁定模式 / Lock mode
- ✅ 视觉反馈 / Visual feedback

### Version 1.1 (2025-12-28)
- ✅ 添加功能键行 / Added function key row
- ⚠️  Ctrl 作为普通键 / Ctrl as normal key (改进 / improved)

### Version 1.0 (2025-12-28)
- ✅ 初始实现 / Initial implementation
- ⚠️  功能键未显示 / Function keys not visible (已修复 / fixed)

---

**现在请安装最新 APK 体验 Ctrl 修饰键功能！**  
**Now install the latest APK to experience the Ctrl modifier key!**

```bash
adb install -r app/build/outputs/apk/release/*-arm64-v8a-release-signed.apk
```
