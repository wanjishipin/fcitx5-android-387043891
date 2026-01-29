# Implementation Summary: Special Function Keys for fcitx5-android

## Overview

Successfully implemented special function keys (Ctrl, Esc, Tab, Minimize) with gesture-based multi-function support, inspired by [Unexpected-Keyboard](https://github.com/Julow/Unexpected-Keyboard).

## Changes Made

### 1. Core Key Action Support
**File**: `app/src/main/java/org/fcitx/fcitx5/android/input/keyboard/KeyAction.kt`
- Added `MinimizeKeyboardAction` to support keyboard hiding functionality

### 2. New Key Definitions
**File**: `app/src/main/java/org/fcitx/fcitx5/android/input/keyboard/KeyDefPreset.kt`
- **CtrlKey**: Control modifier key with press and long-press support
- **EscKey**: Escape key for canceling operations
- **TabKey**: Tab key for indentation and navigation
- **MinimizeKey**: Keyboard hide button with custom icon
- **MultiActionKey**: Generic multi-gesture key supporting:
  - Press action (primary)
  - Swipe action (secondary)
  - Displays both actions (main text + swipe hint)

### 3. Action Handler
**File**: `app/src/main/java/org/fcitx/fcitx5/android/input/keyboard/CommonKeyActionListener.kt`
- Added handler for `MinimizeKeyboardAction` to hide keyboard via `service.requestHideSelf(0)`

### 4. Example Implementation
**File**: `app/src/main/java/org/fcitx/fcitx5/android/input/keyboard/ExtendedTextKeyboard.kt`
- New keyboard layout with function key row demonstrating all special keys
- Includes gesture-based multi-function keys:
  - **Esc**: Escape
  - **Tab** (swipe: ←Tab): Tab / Reverse Tab
  - **↑** (swipe: PgUp): Up arrow / Page Up
  - **↓** (swipe: PgDn): Down arrow / Page Down
  - **←** (swipe: Home): Left arrow / Home
  - **→** (swipe: End): Right arrow / End
  - **Ctrl**: Control modifier
  - **Minimize**: Hide keyboard

### 5. Assets
**File**: `app/src/main/res/drawable/ic_baseline_keyboard_hide_24.xml`
- Vector drawable icon for keyboard minimize button

### 6. Documentation
**File**: `SPECIAL_KEYS_IMPLEMENTATION.md`
- Comprehensive documentation on usage, implementation details, and examples

## Key Features

### Gesture Support (Like Unexpected-Keyboard)

Each key can support multiple actions through different gestures:
- **Press**: Primary action (tap)
- **Swipe**: Secondary action (swipe on key)
- **Long Press**: Tertiary action (hold)
- **Double Tap**: Quaternary action (tap twice)

Example multi-action key:
```kotlin
MultiActionKey(
    displayText = "↑",
    swipeText = "PgUp",
    pressAction = KeyAction.SymAction(KeySym(FcitxKeyMapping.FcitxKey_Up)),
    swipeAction = KeyAction.SymAction(KeySym(FcitxKeyMapping.FcitxKey_Page_Up))
)
```

### Key Mappings

All keys use proper Fcitx key symbols from the codegen system:
- Escape: `FcitxKey_Escape` (0xff1b)
- Tab: `FcitxKey_Tab` (0xff09)
- Control: `FcitxKey_Control_L` (0xffe3)
- Arrow keys: `FcitxKey_Up/Down/Left/Right`
- Navigation: `FcitxKey_Home/End/Page_Up/Page_Down`

## Comparison with Unexpected-Keyboard

### Unexpected-Keyboard Approach
```xml
<key width="1.1" 
     key0="fn" 
     key1="loc alt" 
     key2="loc change_method" 
     key3="switch_emoji" 
     key4="config"/>
```

### fcitx5-android Approach
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

Both support multiple actions per key, but fcitx5-android uses type-safe Kotlin data classes instead of XML attributes.

## Usage Example

### Adding Special Keys to Existing Keyboards

Modify any keyboard layout (e.g., `TextKeyboard.kt`):

```kotlin
val Layout: List<List<KeyDef>> = listOf(
    // Add function row
    listOf(
        EscKey(0.1f),
        TabKey(0.15f),
        MultiActionKey("↑", "PgUp", upAction, pageUpAction),
        MultiActionKey("↓", "PgDn", downAction, pageDownAction),
        CtrlKey(0.15f),
        MinimizeKey(0.1f)
    ),
    // Existing rows...
    listOf(/* Q, W, E, R, ... */),
    // ...
)
```

### Using ExtendedTextKeyboard

The `ExtendedTextKeyboard` is ready to use and demonstrates all features. It can be registered in the keyboard window manager to make it available as a keyboard layout option.

## Build Status

✅ **BUILD SUCCESSFUL** - All code compiles without errors

```
> Task :app:compileReleaseKotlin
BUILD SUCCESSFUL in 1m 8s
93 actionable tasks: 3 executed, 90 up-to-date
```

## Files Modified

```
Modified:
  app/src/main/java/org/fcitx/fcitx5/android/input/keyboard/KeyAction.kt
  app/src/main/java/org/fcitx/fcitx5/android/input/keyboard/KeyDefPreset.kt
  app/src/main/java/org/fcitx/fcitx5/android/input/keyboard/CommonKeyActionListener.kt

Added:
  app/src/main/java/org/fcitx/fcitx5/android/input/keyboard/ExtendedTextKeyboard.kt
  app/src/main/res/drawable/ic_baseline_keyboard_hide_24.xml
  SPECIAL_KEYS_IMPLEMENTATION.md
```

## Testing Recommendations

1. **Unit Tests**: Test each KeyAction handles correctly
2. **Integration Tests**: Verify gesture detection works with each key type
3. **UI Tests**: Ensure keyboard layout renders correctly
4. **Manual Tests**: 
   - Verify Ctrl+C/V work in text editors
   - Test Tab navigation in forms
   - Check Esc cancels operations
   - Confirm arrow keys with swipe gestures work
   - Test keyboard minimize

## Future Enhancements

1. **More Gesture Types**: Add diagonal swipe directions (like Unexpected-Keyboard's circle gestures)
2. **Configurable Keys**: Allow users to customize function key row in settings
3. **Key Combinations**: Support Ctrl+Key, Alt+Key combinations
4. **Sticky Modifiers**: Lock Ctrl/Alt/Shift keys for multiple key presses
5. **Key Macros**: Support custom key sequences (like Unexpected-Keyboard's macro system)

## References

- [Unexpected-Keyboard](https://github.com/Julow/Unexpected-Keyboard) - Gesture-based keyboard inspiration
- [fcitx5-android](https://github.com/fcitx5-android/fcitx5-android) - Base project
- Fcitx5 keysym definitions from codegen/GenKeyMapping.kt
