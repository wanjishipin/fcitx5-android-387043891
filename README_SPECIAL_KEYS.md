# Special Function Keys Implementation for fcitx5-android

## 项目说明 / Project Description

参考 [Unexpected-Keyboard](https://github.com/Julow/Unexpected-Keyboard) 项目，为 fcitx5-android 实现了特殊功能按键（Ctrl、Esc、Tab、Min 等），并支持手势操作，一个按键可以根据手势的不同拥有多个功能。

Inspired by [Unexpected-Keyboard](https://github.com/Julow/Unexpected-Keyboard), this implementation adds special function keys (Ctrl, Esc, Tab, Minimize, etc.) to fcitx5-android with gesture support, allowing each key to have multiple functions based on different gestures.

## 主要特性 / Key Features

### 1. 特殊功能按键 / Special Function Keys

- **Ctrl 键**: 控制键，支持 Ctrl+C/V/A 等快捷键
- **Esc 键**: 退出键，取消操作或退出 vim 插入模式
- **Tab 键**: 制表键，用于缩进和表单导航
- **最小化键**: 隐藏键盘按钮

---

- **Ctrl Key**: Control modifier for shortcuts like Ctrl+C/V/A
- **Esc Key**: Escape key for canceling operations or exiting vim insert mode
- **Tab Key**: Tab key for indentation and form navigation
- **Minimize Key**: Button to hide the keyboard

### 2. 手势支持 / Gesture Support

类似 Unexpected-Keyboard，每个按键支持多个手势：

Similar to Unexpected-Keyboard, each key supports multiple gestures:

- **点击 / Press**: 主要功能 / Primary action
- **滑动 / Swipe**: 次要功能 / Secondary action  
- **长按 / Long Press**: 第三功能 / Tertiary action
- **双击 / Double Tap**: 第四功能 / Quaternary action

### 3. 多功能按键示例 / Multi-Action Key Examples

```kotlin
// 上箭头键 (点击) / 翻页向上 (滑动)
// Up arrow (press) / Page Up (swipe)
MultiActionKey(
    displayText = "↑",
    swipeText = "PgUp",
    pressAction = KeyAction.SymAction(KeySym(FcitxKeyMapping.FcitxKey_Up)),
    swipeAction = KeyAction.SymAction(KeySym(FcitxKeyMapping.FcitxKey_Page_Up))
)

// Tab键 (点击) / 反向Tab (滑动)
// Tab (press) / Reverse Tab (swipe)
MultiActionKey(
    displayText = "Tab",
    swipeText = "←Tab",
    pressAction = KeyAction.SymAction(KeySym(FcitxKeyMapping.FcitxKey_Tab)),
    swipeAction = KeyAction.SymAction(
        KeySym(FcitxKeyMapping.FcitxKey_Tab),
        KeyStates(KeyState.Shift, KeyState.Virtual)
    )
)
```

## 文件结构 / File Structure

### 修改的文件 / Modified Files

1. **KeyAction.kt**
   - 添加 `MinimizeKeyboardAction` 用于隐藏键盘
   - Added `MinimizeKeyboardAction` for keyboard hiding

2. **KeyDefPreset.kt**
   - 添加 `CtrlKey`, `EscKey`, `TabKey`, `MinimizeKey`
   - 添加 `MultiActionKey` 支持多手势功能
   - Added special key definitions and multi-gesture support

3. **CommonKeyActionListener.kt**
   - 添加最小化键盘的处理逻辑
   - Added handler for keyboard minimize action

### 新增的文件 / New Files

1. **ExtendedTextKeyboard.kt**
   - 完整的示例键盘布局，包含功能键行
   - Complete example keyboard layout with function key row

2. **ic_baseline_keyboard_hide_24.xml**
   - 最小化键盘按钮的图标
   - Icon for keyboard minimize button

3. **文档文件 / Documentation Files**
   - `SPECIAL_KEYS_IMPLEMENTATION.md`: 实现细节 / Implementation details
   - `COMPARISON.md`: 与 Unexpected-Keyboard 的对比 / Comparison with Unexpected-Keyboard
   - `QUICK_START.md`: 快速开始指南 / Quick start guide
   - `IMPLEMENTATION_SUMMARY.md`: 实现总结 / Implementation summary

## 快速开始 / Quick Start

### 1. 构建项目 / Build the Project

```bash
cd /media/aitest/DiskData/fcitx5-android
./gradlew assembleRelease
```

### 2. 使用示例键盘 / Use Example Keyboard

`ExtendedTextKeyboard` 提供了完整的功能键行：

`ExtendedTextKeyboard` provides a complete function key row:

```
[Esc] [Tab↔] [↑PgUp] [↓PgDn] [←Home] [→End] [Ctrl] [Min]
```

### 3. 自定义键盘布局 / Customize Keyboard Layout

在任何键盘类中添加功能键行：

Add function key row to any keyboard class:

```kotlin
val Layout: List<List<KeyDef>> = listOf(
    // 功能键行 / Function key row
    listOf(
        EscKey(0.1f),
        TabKey(0.15f),
        MultiActionKey("↑", "PgUp", upAction, pageUpAction),
        CtrlKey(0.15f),
        MinimizeKey(0.1f)
    ),
    // 其他行 / Other rows
    listOf(/* ... */),
)
```

## 使用场景 / Use Cases

### 1. 编程 / Programming
- **Tab**: 代码缩进 / Code indentation
- **Esc**: 退出 vim 插入模式 / Exit vim insert mode
- **Ctrl+键**: 快捷键 / Keyboard shortcuts

### 2. 终端 / Terminal
- **Ctrl+C**: 中断进程 / Interrupt process
- **Ctrl+D**: EOF信号 / EOF signal
- **Tab**: 命令补全 / Command completion

### 3. 文本编辑 / Text Editing
- **Ctrl+C/V/A**: 复制/粘贴/全选 / Copy/Paste/Select All
- **方向键 + Home/End**: 光标导航 / Cursor navigation
- **Page Up/Down**: 翻页 / Page scrolling

### 4. 网页浏览 / Web Browsing
- **Tab**: 在表单字段间切换 / Switch between form fields
- **Esc**: 停止加载页面 / Stop loading page

## 技术对比 / Technical Comparison

### Unexpected-Keyboard 方式 / Unexpected-Keyboard Approach
```xml
<key width="1.1" 
     key0="fn" 
     key1="loc alt" 
     key2="loc change_method" 
     key3="switch_emoji" 
     key4="config"/>
```
- XML 配置 / XML-based configuration
- 最多9个功能 (key0-key8) / Up to 9 actions (key0-key8)
- 运行时解析 / Runtime parsing

### fcitx5-android 方式 / fcitx5-android Approach
```kotlin
KeyDef(
    appearance = Appearance.AltText("main", "swipe", ...),
    behaviors = setOf(
        Behavior.Press(action1),
        Behavior.Swipe(action2),
        Behavior.LongPress(action3),
        Behavior.DoubleTap(action4)
    )
)
```
- Kotlin DSL 配置 / Kotlin DSL configuration
- 类型安全 / Type-safe
- 编译时检查 / Compile-time checking
- 易于扩展 / Easy to extend

## 编译状态 / Build Status

✅ **构建成功 / BUILD SUCCESSFUL**

```
> Task :app:compileReleaseKotlin
BUILD SUCCESSFUL in 1m 8s
93 actionable tasks: 3 executed, 90 up-to-date
```

## 后续改进 / Future Improvements

1. **更多手势类型 / More Gesture Types**: 斜向滑动、圆形手势 / Diagonal swipes, circle gestures
2. **可配置按键 / Configurable Keys**: 用户自定义功能键行 / User-customizable function key row
3. **按键组合 / Key Combinations**: Ctrl+Key, Alt+Key 组合 / Key combination support
4. **粘性修饰键 / Sticky Modifiers**: 锁定 Ctrl/Alt/Shift / Lock modifier keys
5. **按键宏 / Key Macros**: 自定义按键序列 / Custom key sequences

## 参考资料 / References

- [Unexpected-Keyboard](https://github.com/Julow/Unexpected-Keyboard) - 灵感来源 / Original inspiration
- [fcitx5-android](https://github.com/fcitx5-android/fcitx5-android) - 基础项目 / Base project
- Fcitx5 键符定义 / Fcitx5 KeySym definitions

## 许可证 / License

本实现遵循 fcitx5-android 项目的许可证 (LGPL-2.1-or-later)

This implementation follows the fcitx5-android project license (LGPL-2.1-or-later)

## 贡献 / Contributing

欢迎提交问题和拉取请求！

Issues and pull requests are welcome!

---

**作者 / Author**: GitHub Copilot CLI  
**日期 / Date**: 2025-12-28  
**版本 / Version**: 1.0
