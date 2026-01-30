# 悬浮键盘功能指南

## 概述

已添加完整的悬浮键盘功能到 fcitx5-android，允许键盘作为系统级悬浮窗显示，即使在失去输入焦点的情况下也能发送按键事件。

## 新增功能

### 1. Menu 按键
- 在 `KeyDefPreset.kt` 中添加了 `MenuKey` 类
- 在 `TextKeyboard.kt` 的 Function key row 中添加了 Menu 按钮
- 按键布局：`Esc | Tab | ↑ | ↓ | ← | → | Ctrl | Menu | Min`

### 2. 悬浮键盘系统

#### 新增文件：

1. **FloatingKeyboardAccessibilityService.kt**
   - 无障碍服务，用于在无焦点情况下发送按键事件
   - 位置：`app/src/main/java/org/fcitx/fcitx5/android/service/`

2. **FloatingKeyboardManager.kt**
   - 管理悬浮窗口的显示、隐藏、移动和调整大小
   - 位置：`app/src/main/java/org/fcitx/fcitx5/android/input/keyboard/`

3. **FloatingKeyboardView.kt**
   - 可拖动的悬浮键盘容器视图，包含标题栏和关闭按钮
   - 位置：`app/src/main/java/org/fcitx/fcitx5/android/input/keyboard/`

4. **FloatingKeyboardPermissionHelper.kt**
   - 权限检查和请求工具类
   - 位置：`app/src/main/java/org/fcitx/fcitx5/android/utils/`

5. **accessibility_service_config.xml**
   - 无障碍服务配置文件
   - 位置：`app/src/main/res/xml/`

#### 权限和配置：

已在 `AndroidManifest.xml` 中添加：
- `SYSTEM_ALERT_WINDOW` 权限（悬浮窗）
- `FloatingKeyboardAccessibilityService` 服务注册

## 使用方法

### 激活悬浮键盘

用户需要完成以下步骤：

1. **授予悬浮窗权限**
   ```kotlin
   if (!FloatingKeyboardPermissionHelper.hasOverlayPermission(context)) {
       FloatingKeyboardPermissionHelper.requestOverlayPermission(context)
   }
   ```

2. **启用无障碍服务**
   - 打开系统设置 → 无障碍 → Fcitx5 悬浮键盘服务
   - 或使用代码引导用户：
   ```kotlin
   if (!FloatingKeyboardPermissionHelper.hasAccessibilityPermission()) {
       FloatingKeyboardPermissionHelper.requestAccessibilityPermission(context)
   }
   ```

### 集成到输入法服务

在 `FcitxInputMethodService` 中集成悬浮键盘：

```kotlin
class FcitxInputMethodService : LifecycleInputMethodService() {
    
    private lateinit var floatingKeyboardManager: FloatingKeyboardManager
    
    override fun onCreate() {
        super.onCreate()
        floatingKeyboardManager = FloatingKeyboardManager(this)
    }
    
    // 显示悬浮键盘
    fun showFloatingKeyboard() {
        if (FloatingKeyboardPermissionHelper.hasAllPermissions(this)) {
            val keyboard = TextKeyboard(this, currentTheme)
            floatingKeyboardManager.show(keyboard)
        } else {
            // 提示用户授予权限
            showPermissionDialog()
        }
    }
    
    // 隐藏悬浮键盘
    fun hideFloatingKeyboard() {
        floatingKeyboardManager.hide()
    }
}
```

### 发送按键事件

通过无障碍服务发送按键：

```kotlin
val accessibilityService = FloatingKeyboardAccessibilityService.getInstance()
if (accessibilityService != null) {
    // 发送单个按键
    accessibilityService.sendKeyEvent(KeyEvent.KEYCODE_MENU)
    
    // 发送带修饰符的按键 (Ctrl+C)
    accessibilityService.sendKeyEvent(
        KeyEvent.KEYCODE_C, 
        KeyEvent.META_CTRL_ON
    )
    
    // 发送文本
    accessibilityService.sendText("Hello World")
}
```

## 技术细节

### 悬浮窗类型
使用 `TYPE_APPLICATION_OVERLAY` (Android 8.0+) 或 `TYPE_PHONE` (旧版本)

### 无障碍服务功能
- 捕获当前焦点窗口
- 查找可编辑的输入字段
- 通过 `AccessibilityNodeInfo.ACTION_SET_TEXT` 发送文本
- 支持特殊按键（删除、方向键等）

### 窗口管理
- 可拖动位置
- 可调整大小
- 自动保存位置和大小配置

## 注意事项

1. **权限要求**
   - 需要用户手动授予悬浮窗权限（Android 6.0+）
   - 需要用户手动启用无障碍服务

2. **兼容性**
   - 某些设备厂商（如小米、华为）可能限制悬浮窗或无障碍服务
   - 需要在应用设置中添加电池优化白名单

3. **性能考虑**
   - 无障碍服务会持续运行，注意电池消耗
   - 悬浮窗会占用系统资源

4. **安全性**
   - 无障碍服务权限强大，需要向用户明确说明用途
   - Google Play 可能对使用无障碍服务的应用进行额外审核

## 后续开发建议

1. **UI集成**
   - 在设置中添加"启用悬浮键盘"选项
   - 添加切换按钮从普通键盘切换到悬浮模式
   - 添加权限状态检查和引导界面

2. **功能增强**
   - 添加键盘大小调节功能
   - 支持多种键盘布局
   - 添加透明度调节
   - 支持键盘位置预设

3. **用户体验**
   - 添加教程和帮助文档
   - 提供权限授予的详细步骤说明
   - 添加悬浮键盘的快捷切换方式

## 测试建议

1. 测试在不同应用中的按键发送
2. 测试特殊字符和组合键
3. 测试多任务切换时的悬浮窗状态
4. 测试不同屏幕尺寸和方向
5. 测试系统权限被撤销时的行为

## 构建和编译

所有必要的代码已添加，编译项目即可：

```bash
./gradlew assembleDebug
```

确保在 `build.gradle` 中有正确的依赖项（Timber 用于日志记录）。
