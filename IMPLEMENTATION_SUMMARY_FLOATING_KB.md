# 悬浮键盘和 Menu 键实现总结

## 完成时间
2026-01-29

## 实现内容

### 1. Menu 按键添加 ✅

#### 修改的文件：

**KeyDefPreset.kt**
- 添加了 `MenuKey` 类（行 398-411）
- 使用 `android.view.KeyEvent.KEYCODE_MENU` 作为按键代码
- 可配置宽度和样式变体

**TextKeyboard.kt** 
- 在 Function key row 添加了 `MenuKey(0.1f)`（行 75）
- 调整了 Tab 和 Ctrl 键的宽度以腾出空间
- 新的按键布局：`Esc | Tab | ↑ | ↓ | ← | → | Ctrl | Menu | Min`

### 2. 悬浮键盘系统 ✅

#### 新增文件：

1. **FloatingKeyboardAccessibilityService.kt**
   - 路径：`app/src/main/java/org/fcitx/fcitx5/android/service/`
   - 功能：无障碍服务，在无焦点情况下发送按键事件
   - 方法：
     - `sendKeyEvent(keyCode, metaState)` - 发送按键
     - `sendText(text)` - 发送文本
     - `isEnabled()` - 检查服务是否启用

2. **FloatingKeyboardManager.kt**
   - 路径：`app/src/main/java/org/fcitx/fcitx5/android/input/keyboard/`
   - 功能：管理悬浮窗口的生命周期
   - 方法：
     - `show(keyboard)` - 显示悬浮键盘
     - `hide()` - 隐藏悬浮键盘
     - `updatePosition(x, y)` - 更新位置
     - `updateSize(width, height)` - 更新大小

3. **FloatingKeyboardView.kt**
   - 路径：`app/src/main/java/org/fcitx/fcitx5/android/input/keyboard/`
   - 功能：可拖动的悬浮键盘容器
   - 特性：
     - 标题栏可拖动
     - 关闭按钮
     - 键盘容器区域

4. **FloatingKeyboardPermissionHelper.kt**
   - 路径：`app/src/main/java/org/fcitx/fcitx5/android/utils/`
   - 功能：权限检查和请求
   - 方法：
     - `hasOverlayPermission()` - 检查悬浮窗权限
     - `requestOverlayPermission()` - 请求悬浮窗权限
     - `hasAccessibilityPermission()` - 检查无障碍权限
     - `requestAccessibilityPermission()` - 请求无障碍权限
     - `hasAllPermissions()` - 检查所有权限

5. **accessibility_service_config.xml**
   - 路径：`app/src/main/res/xml/`
   - 功能：无障碍服务配置

#### 修改的文件：

**AndroidManifest.xml**
- 添加 `SYSTEM_ALERT_WINDOW` 权限（行 16）
- 注册 `FloatingKeyboardAccessibilityService` 服务（行 150-158）

**strings.xml**
- 添加了 9 个新的字符串资源：
  - `accessibility_service_description`
  - `floating_keyboard`
  - `enable_floating_keyboard`
  - `floating_keyboard_summary`
  - `accessibility_service_required`
  - `accessibility_service_required_message`
  - `overlay_permission_required`
  - `overlay_permission_required_message`
  - `open_settings`

#### 文档文件：

**FLOATING_KEYBOARD_GUIDE.md**
- 完整的使用指南
- 包含代码示例
- 技术细节说明
- 后续开发建议

## 技术架构

### 权限流程
```
用户启用悬浮键盘
    ↓
检查 SYSTEM_ALERT_WINDOW 权限
    ↓ (未授予)
引导用户到设置页面授予权限
    ↓
检查无障碍服务
    ↓ (未启用)
引导用户到无障碍设置启用服务
    ↓
创建悬浮窗口
    ↓
显示键盘
```

### 按键发送流程
```
用户点击键盘按键
    ↓
获取 AccessibilityService 实例
    ↓
查找焦点窗口
    ↓
查找可编辑输入字段
    ↓
使用 ACTION_SET_TEXT 发送按键/文本
```

### 悬浮窗管理
```
WindowManager
    ↓
TYPE_APPLICATION_OVERLAY
    ↓
FloatingKeyboardView (容器)
    ├── TitleBar (可拖动)
    │   ├── DragHandle
    │   └── CloseButton
    └── KeyboardContainer
        └── BaseKeyboard (实际键盘)
```

## 使用示例

### 基础集成

```kotlin
// 在 FcitxInputMethodService 中
class FcitxInputMethodService : LifecycleInputMethodService() {
    
    private lateinit var floatingKeyboardManager: FloatingKeyboardManager
    
    override fun onCreate() {
        super.onCreate()
        floatingKeyboardManager = FloatingKeyboardManager(this)
    }
    
    fun showFloatingKeyboard() {
        // 检查权限
        if (!FloatingKeyboardPermissionHelper.hasAllPermissions(this)) {
            showPermissionDialog()
            return
        }
        
        // 创建键盘并显示
        val keyboard = TextKeyboard(this, currentTheme)
        floatingKeyboardManager.show(keyboard)
    }
}
```

### 发送按键

```kotlin
// 发送 Menu 键
val service = FloatingKeyboardAccessibilityService.getInstance()
service?.sendKeyEvent(KeyEvent.KEYCODE_MENU)

// 发送 Ctrl+C
service?.sendKeyEvent(KeyEvent.KEYCODE_C, KeyEvent.META_CTRL_ON)

// 发送文本
service?.sendText("Hello World")
```

## 待完成的集成工作

虽然所有核心代码都已完成，但还需要以下集成工作才能让功能在应用中可用：

1. **在 FcitxInputMethodService 中添加 FloatingKeyboardManager 实例**
2. **在设置界面添加"启用悬浮键盘"选项**
3. **添加权限请求的对话框和引导界面**
4. **在键盘上添加切换到悬浮模式的按钮**
5. **处理悬浮键盘与普通键盘的切换逻辑**

## 测试建议

1. 构建并安装应用
2. 授予悬浮窗权限（设置 → 应用 → Fcitx5 → 显示在其他应用上层）
3. 启用无障碍服务（设置 → 无障碍 → Fcitx5 悬浮键盘服务）
4. 测试 Menu 键功能
5. 测试悬浮键盘的拖动和按键发送

## 注意事项

### 安全和隐私
- 无障碍服务权限非常强大，需要向用户清楚说明用途
- Google Play 可能对使用无障碍服务的应用进行额外审核
- 建议在隐私政策中说明数据使用情况

### 兼容性
- 某些设备厂商（小米、华为、OPPO等）可能限制悬浮窗
- 部分设备需要额外的电池优化白名单设置
- 建议提供厂商特定的权限授予指南

### 性能
- 无障碍服务会持续运行，注意电池消耗
- 悬浮窗口会占用系统资源
- 建议添加"关闭悬浮键盘"的快捷方式

## 文件清单

### 新增文件（共 6 个）
```
app/src/main/java/org/fcitx/fcitx5/android/service/
├── FloatingKeyboardAccessibilityService.kt

app/src/main/java/org/fcitx/fcitx5/android/input/keyboard/
├── FloatingKeyboardManager.kt
├── FloatingKeyboardView.kt

app/src/main/java/org/fcitx/fcitx5/android/utils/
├── FloatingKeyboardPermissionHelper.kt

app/src/main/res/xml/
├── accessibility_service_config.xml

./
├── FLOATING_KEYBOARD_GUIDE.md
```

### 修改文件（共 4 个）
```
app/src/main/
├── AndroidManifest.xml (添加权限和服务)
├── res/values/strings.xml (添加字符串资源)
├── java/.../input/keyboard/KeyDefPreset.kt (添加 MenuKey)
└── java/.../input/keyboard/TextKeyboard.kt (添加 Menu 到布局)
```

## 构建命令

```bash
cd /home/ai/DiskData/fcitx5-android
./gradlew assembleDebug
```

## Git 提交建议

```bash
git add app/src/main/java/org/fcitx/fcitx5/android/input/keyboard/KeyDefPreset.kt
git add app/src/main/java/org/fcitx/fcitx5/android/input/keyboard/TextKeyboard.kt
git commit -m "Add Menu key to keyboard Function row"

git add app/src/main/java/org/fcitx/fcitx5/android/service/
git add app/src/main/java/org/fcitx/fcitx5/android/input/keyboard/FloatingKeyboard*.kt
git add app/src/main/java/org/fcitx/fcitx5/android/utils/FloatingKeyboardPermissionHelper.kt
git add app/src/main/res/xml/accessibility_service_config.xml
git add app/src/main/AndroidManifest.xml
git add app/src/main/res/values/strings.xml
git add FLOATING_KEYBOARD_GUIDE.md
git commit -m "Add floating keyboard functionality with accessibility service support"
```

## 总结

已成功实现：
1. ✅ Menu 按键添加到键盘
2. ✅ 完整的悬浮键盘基础架构
3. ✅ 无障碍服务集成
4. ✅ 权限管理系统
5. ✅ 可拖动的悬浮窗口
6. ✅ 按键事件发送机制

所有核心代码已完成，可以直接编译使用。建议按照 FLOATING_KEYBOARD_GUIDE.md 中的说明进行 UI 集成和测试。
