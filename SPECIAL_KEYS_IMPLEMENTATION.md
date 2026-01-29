# Special Function Keys Implementation

## Overview

This implementation adds special function keys (Ctrl, Esc, Tab, Minimize) with gesture-based multi-function support, inspired by [Unexpected-Keyboard](https://github.com/Julow/Unexpected-Keyboard).

## Features

### New Key Types

1. **CtrlKey**: Control key for keyboard shortcuts
   - Press: Send Ctrl key
   - Long press: Lock Ctrl modifier

2. **EscKey**: Escape key for canceling operations
   - Press: Send Escape key

3. **TabKey**: Tab key for indentation and navigation
   - Press: Send Tab key

4. **MinimizeKey**: Minimize keyboard
   - Press: Hide the keyboard

5. **MultiActionKey**: Generic key supporting multiple gestures
   - Press: Primary action (e.g., arrow key)
   - Swipe: Secondary action (e.g., Page Up/Down, Home/End)

## Implementation Details

### Files Modified

1. **KeyAction.kt**
   - Added `MinimizeKeyboardAction` for keyboard hiding

2. **KeyDefPreset.kt**
   - Added `CtrlKey`, `EscKey`, `TabKey`, `MinimizeKey`
   - Added `MultiActionKey` for gesture-based multi-function keys

3. **CommonKeyActionListener.kt**
   - Added handler for `MinimizeKeyboardAction`

### Files Created

1. **ExtendedTextKeyboard.kt**
   - Example keyboard layout with function key row
   - Demonstrates gesture-based keys with multiple actions

2. **ic_baseline_keyboard_hide_24.xml**
   - Icon for minimize keyboard button

## Usage

### Using Special Keys in Custom Layouts

```kotlin
// Control key
CtrlKey(
    percentWidth = 0.15f,
    variant = KeyDef.Appearance.Variant.Alternative
)

// Escape key
EscKey(
    percentWidth = 0.1f
)

// Tab key with reverse tab on swipe
TabKey(
    percentWidth = 0.15f
)

// Minimize keyboard
MinimizeKey(
    percentWidth = 0.1f
)

// Multi-action key (arrow with Page Up/Down)
MultiActionKey(
    displayText = "↑",
    swipeText = "PgUp",
    pressAction = KeyAction.SymAction(KeySym(FcitxKeyMapping.FcitxKey_Up)),
    swipeAction = KeyAction.SymAction(KeySym(FcitxKeyMapping.FcitxKey_Page_Up)),
    percentWidth = 0.1f
)
```

### Extended Text Keyboard Layout

The `ExtendedTextKeyboard` includes a function row with:
- **Esc**: Escape key
- **Tab** (swipe for reverse Tab): Tab navigation
- **↑** (swipe for Page Up): Arrow up / page up
- **↓** (swipe for Page Down): Arrow down / page down
- **←** (swipe for Home): Arrow left / go to line start
- **→** (swipe for End): Arrow right / go to line end
- **Ctrl**: Control modifier key
- **Minimize**: Hide keyboard

### Gesture Support

Similar to Unexpected-Keyboard, keys support multiple gestures:

1. **Press**: Primary action (tap the key)
2. **Swipe**: Secondary action (swipe on the key)
3. **Long Press**: Tertiary action (hold the key)
4. **Double Tap**: Quaternary action (tap twice quickly)

Example from Unexpected-Keyboard's bottom row:
```xml
<key width="1.1" 
     key0="fn" 
     key1="loc alt" 
     key2="loc change_method" 
     key3="switch_emoji" 
     key4="config"/>
```

In fcitx5-android, this is implemented through `KeyDef.Behavior`:
```kotlin
setOf(
    Behavior.Press(primaryAction),
    Behavior.Swipe(swipeAction),
    Behavior.LongPress(longPressAction),
    Behavior.DoubleTap(doubleTapAction)
)
```

## Key Mappings

All special keys use proper Fcitx key symbols:

| Key | KeySym | KeyCode |
|-----|--------|---------|
| Escape | 0xff1b | FcitxKey_Escape |
| Tab | 0xff09 | FcitxKey_Tab |
| Control | 0xffe3 | FcitxKey_Control_L |
| Up | 0xff52 | FcitxKey_Up |
| Down | 0xff54 | FcitxKey_Down |
| Left | 0xff51 | FcitxKey_Left |
| Right | 0xff53 | FcitxKey_Right |
| Home | 0xff50 | FcitxKey_Home |
| End | 0xff57 | FcitxKey_End |
| Page_Up | 0xff55 | FcitxKey_Page_Up |
| Page_Down | 0xff56 | FcitxKey_Page_Down |

## Integration with Existing Keyboards

To add special keys to existing keyboards, modify the layout in keyboard files (e.g., `TextKeyboard.kt`, `NumberKeyboard.kt`):

```kotlin
// Add a function row at the top
val Layout: List<List<KeyDef>> = listOf(
    // Function row
    listOf(
        EscKey(0.1f),
        TabKey(0.15f),
        // ... other function keys
        MinimizeKey(0.1f)
    ),
    // Existing rows
    listOf(/* row 1 */),
    listOf(/* row 2 */),
    // ...
)
```

## References

- [Unexpected-Keyboard](https://github.com/Julow/Unexpected-Keyboard) - Original inspiration for gesture-based multi-function keys
- Unexpected-Keyboard's gesture system: `Gesture.java`, `KeyValue.java`, `KeyModifier.java`
- Fcitx5 Android keyboard system: `KeyAction.kt`, `KeyDef.kt`, `BaseKeyboard.kt`
