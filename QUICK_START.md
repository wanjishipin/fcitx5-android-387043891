# Quick Start Guide: Using Special Function Keys

## Installation

All necessary files have been added to the project. Simply rebuild the app to include the new features.

## Available Special Keys

### 1. CtrlKey - Control Modifier
```kotlin
CtrlKey(
    percentWidth = 0.15f,
    variant = KeyDef.Appearance.Variant.Alternative
)
```

**Actions:**
- Press: Send Control key
- Long press: Lock Control modifier

**Use cases:**
- Ctrl+C (Copy)
- Ctrl+V (Paste)
- Ctrl+A (Select All)

### 2. EscKey - Escape
```kotlin
EscKey(
    percentWidth = 0.1f,
    variant = KeyDef.Appearance.Variant.Normal
)
```

**Actions:**
- Press: Send Escape key

**Use cases:**
- Cancel operations
- Exit vim insert mode
- Close dialogs

### 3. TabKey - Tab Navigation
```kotlin
TabKey(
    percentWidth = 0.15f,
    variant = KeyDef.Appearance.Variant.Alternative
)
```

**Actions:**
- Press: Send Tab key

**Use cases:**
- Indent code
- Navigate form fields
- Switch UI elements

### 4. MinimizeKey - Hide Keyboard
```kotlin
MinimizeKey(
    percentWidth = 0.1f,
    variant = KeyDef.Appearance.Variant.Alternative
)
```

**Actions:**
- Press: Hide the keyboard

**Use cases:**
- Quickly dismiss keyboard
- View full screen content

### 5. MultiActionKey - Gesture-Based Multi-Function

**Example: Arrow with Page Navigation**
```kotlin
MultiActionKey(
    displayText = "↑",
    swipeText = "PgUp",
    pressAction = KeyAction.SymAction(KeySym(FcitxKeyMapping.FcitxKey_Up)),
    swipeAction = KeyAction.SymAction(KeySym(FcitxKeyMapping.FcitxKey_Page_Up)),
    percentWidth = 0.1f
)
```

**Actions:**
- Press: Up arrow
- Swipe up: Page Up

**More examples:**

```kotlin
// Tab with reverse tab
MultiActionKey(
    displayText = "Tab",
    swipeText = "←Tab",
    pressAction = KeyAction.SymAction(KeySym(FcitxKeyMapping.FcitxKey_Tab)),
    swipeAction = KeyAction.SymAction(
        KeySym(FcitxKeyMapping.FcitxKey_Tab),
        KeyStates(KeyState.Shift, KeyState.Virtual)
    )
)

// Left arrow with Home
MultiActionKey(
    displayText = "←",
    swipeText = "Home",
    pressAction = KeyAction.SymAction(KeySym(FcitxKeyMapping.FcitxKey_Left)),
    swipeAction = KeyAction.SymAction(KeySym(FcitxKeyMapping.FcitxKey_Home))
)

// Right arrow with End
MultiActionKey(
    displayText = "→",
    swipeText = "End",
    pressAction = KeyAction.SymAction(KeySym(FcitxKeyMapping.FcitxKey_Right)),
    swipeAction = KeyAction.SymAction(KeySym(FcitxKeyMapping.FcitxKey_End))
)
```

## Pre-built Keyboard: ExtendedTextKeyboard

A complete keyboard layout with function keys is ready to use:

### Layout Structure
```
Row 1: [Esc] [Tab↔] [↑PgUp] [↓PgDn] [←Home] [→End] [Ctrl] [Min]
Row 2: [Q] [W] [E] [R] [T] [Y] [U] [I] [O] [P]
Row 3: [A] [S] [D] [F] [G] [H] [J] [K] [L]
Row 4: [⇪] [Z] [X] [C] [V] [B] [N] [M] [⌫]
Row 5: [?123] [,] [🌐] [________] [.] [↵]
```

### Function Key Row Features
- **Esc**: Escape key
- **Tab↔**: Tab (swipe for reverse tab)
- **↑PgUp**: Up arrow (swipe for Page Up)
- **↓PgDn**: Down arrow (swipe for Page Down)
- **←Home**: Left arrow (swipe for Home)
- **→End**: Right arrow (swipe for End)
- **Ctrl**: Control modifier
- **Min**: Minimize keyboard

## Integration into Your App

### Option 1: Use ExtendedTextKeyboard as-is

Register the keyboard in `KeyboardWindow.kt`:

```kotlin
private val keyboards = mapOf(
    TextKeyboard.Name to TextKeyboard::class.java,
    NumberKeyboard.Name to NumberKeyboard::class.java,
    ExtendedTextKeyboard.Name to ExtendedTextKeyboard::class.java  // Add this
)
```

### Option 2: Add Function Row to Existing Keyboard

Modify `TextKeyboard.kt` or create a custom keyboard:

```kotlin
companion object {
    val Layout: List<List<KeyDef>> = listOf(
        // Add function row
        listOf(
            EscKey(0.1f),
            TabKey(0.15f),
            MultiActionKey("↑", "PgUp", upAction, pageUpAction, 0.1f),
            MultiActionKey("↓", "PgDn", downAction, pageDownAction, 0.1f),
            CtrlKey(0.15f),
            MinimizeKey(0.1f, KeyDef.Appearance.Variant.Alternative)
        ),
        // Your existing rows
        listOf(/* ... */),
    )
}
```

### Option 3: Custom Multi-Action Keys

Create your own gesture-based keys:

```kotlin
// Delete with delete word
MultiActionKey(
    displayText = "⌫",
    swipeText = "Del",
    pressAction = KeyAction.SymAction(KeySym(FcitxKeyMapping.FcitxKey_BackSpace)),
    swipeAction = KeyAction.SymAction(KeySym(FcitxKeyMapping.FcitxKey_Delete)),
    percentWidth = 0.15f
)

// Space with underscore
MultiActionKey(
    displayText = "Space",
    swipeText = "_",
    pressAction = KeyAction.SymAction(KeySym(FcitxKeyMapping.FcitxKey_space)),
    swipeAction = KeyAction.CommitAction("_"),
    percentWidth = 0.4f
)
```

## Gesture Guide

### How to Use Multi-Action Keys

1. **Tap**: Primary action (main text)
   - Example: Tap "↑" → Up arrow

2. **Swipe**: Secondary action (swipe text)
   - Example: Swipe up on "↑" → Page Up

3. **Visual Indicator**: 
   - Main text = Tap action
   - Small text = Swipe action

### Gesture Detection

The keyboard automatically detects:
- Short tap = Press
- Swipe in any direction = Swipe action
- Hold for 500ms = Long press (if configured)
- Double tap quickly = Double tap (if configured)

## Testing Your Implementation

### 1. Test Ctrl Key
```
1. Open a text editor
2. Type some text
3. Press Ctrl key
4. Press 'c' key
5. Verify: Text is copied
```

### 2. Test Tab Key
```
1. Open a web form
2. Focus first field
3. Press Tab key
4. Verify: Focus moves to next field
```

### 3. Test Esc Key
```
1. Open a terminal app (e.g., Termux)
2. Start vim: `vi test.txt`
3. Press 'i' to enter insert mode
4. Press Esc key
5. Verify: Returns to command mode
```

### 4. Test Multi-Action Keys
```
1. Open any text editor
2. Tap "↑" key → Cursor moves up
3. Swipe up on "↑" key → Page scrolls up
4. Tap "←" key → Cursor moves left
5. Swipe on "←" key → Cursor jumps to line start
```

### 5. Test Minimize Key
```
1. Open keyboard in any app
2. Press minimize button
3. Verify: Keyboard hides
```

## Customization

### Change Key Sizes
```kotlin
// Larger Ctrl key
CtrlKey(percentWidth = 0.2f)  // Default: 0.15f

// Smaller Esc key
EscKey(percentWidth = 0.08f)  // Default: 0.1f
```

### Change Key Appearance
```kotlin
// Alternative style (darker)
TabKey(
    percentWidth = 0.15f,
    variant = KeyDef.Appearance.Variant.Alternative
)

// Accent style (themed color)
CtrlKey(
    percentWidth = 0.15f,
    variant = KeyDef.Appearance.Variant.Accent
)

// Normal style (default)
EscKey(
    percentWidth = 0.1f,
    variant = KeyDef.Appearance.Variant.Normal
)
```

## Troubleshooting

### Key Not Working?
1. Check if key sends correct KeySym
2. Verify app supports the key (some apps ignore certain keys)
3. Test in different apps (text editor, terminal, web browser)

### Gesture Not Triggering?
1. Ensure swipe distance is sufficient
2. Check `swipeThresholdX/Y` values in BaseKeyboard
3. Verify Behavior.Swipe is configured in KeyDef

### Minimize Not Hiding Keyboard?
1. Check system permissions
2. Verify `service.requestHideSelf(0)` is called
3. Test in different apps

## Advanced Usage

### Custom Key with Multiple Behaviors
```kotlin
KeyDef(
    appearance = Appearance.Text("Multi", 16f, percentWidth = 0.15f),
    behaviors = setOf(
        Behavior.Press(KeyAction.FcitxKeyAction("m")),
        Behavior.Swipe(KeyAction.FcitxKeyAction("M")),
        Behavior.LongPress(KeyAction.SymAction(KeySym(FcitxKeyMapping.FcitxKey_Meta_L))),
        Behavior.DoubleTap(KeyAction.MinimizeKeyboardAction)
    )
)
```

### Function Key Row Only
```kotlin
// Create a minimal function keyboard
class FunctionKeyboard(context: Context, theme: Theme) : 
    BaseKeyboard(context, theme, listOf(
        listOf(
            EscKey(0.125f),
            TabKey(0.125f),
            // F1-F12 keys
            NumPadKey("F1", FcitxKeyMapping.FcitxKey_F1, 14f, 0.125f),
            NumPadKey("F2", FcitxKeyMapping.FcitxKey_F2, 14f, 0.125f),
            // ...
            MinimizeKey(0.125f)
        )
    ))
```

## Support

For issues or questions:
1. Check `SPECIAL_KEYS_IMPLEMENTATION.md` for implementation details
2. Review `COMPARISON.md` for architecture understanding
3. Examine `ExtendedTextKeyboard.kt` for working examples
4. Test with different apps to isolate app-specific issues
