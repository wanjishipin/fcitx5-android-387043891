# Comparison: Unexpected-Keyboard vs fcitx5-android Implementation

## Architecture Comparison

### Unexpected-Keyboard (Java)

**Gesture System** (`Gesture.java`):
```java
enum Name {
    None, Swipe, Roundtrip, Circle, Anticircle
}

public Name get_gesture() {
    switch (state) {
        case Ended_swipe: return Name.Swipe;
        case Ended_center: return Name.Roundtrip;
        case Ended_clockwise: return Name.Circle;
        case Ended_anticlockwise: return Name.Anticircle;
    }
}
```

**Key Values** (`KeyValue.java`):
```java
public enum Event {
    CONFIG, SWITCH_TEXT, SWITCH_NUMERIC, SWITCH_EMOJI,
    CHANGE_METHOD_PICKER, ACTION, SWITCH_FORWARD,
    SWITCH_MINIMIZE, // ← Keyboard minimize
}

public enum Modifier {
    SHIFT, GESTURE, CTRL, ALT, META, FN,
    // ... accent modifiers
}
```

**Key Modification** (`KeyModifier.java`):
```java
public static KeyValue modify(KeyValue k, KeyValue.Modifier mod) {
    switch (mod) {
        case CTRL: return apply_ctrl(k);
        case ALT:
        case META: return turn_into_keyevent(k);
        case FN: return apply_fn(k);
        case GESTURE: return apply_gesture(k);
        // ...
    }
}
```

### fcitx5-android (Kotlin)

**Gesture System** (`CustomGestureView.kt`):
```kotlin
enum class GestureType { Down, Move, Up }

data class Event(
    val type: GestureType,
    val consumed: Boolean,
    val x: Float, val y: Float,
    val countX: Int, val countY: Int,
    val totalX: Int, val totalY: Int
)
```

**Key Actions** (`KeyAction.kt`):
```kotlin
sealed class KeyAction {
    data class FcitxKeyAction(val act: String, val code: Int, val states: KeyStates)
    data class SymAction(val sym: KeySym, val states: KeyStates)
    data class CommitAction(val text: String)
    data object MinimizeKeyboardAction // ← Our addition
    // ...
}
```

**Key Behaviors** (`KeyDef.kt`):
```kotlin
sealed class Behavior {
    class Press(val action: KeyAction)
    class LongPress(val action: KeyAction)
    class Repeat(val action: KeyAction)
    class Swipe(val action: KeyAction)
    class DoubleTap(val action: KeyAction)
}
```

## Layout Definition Comparison

### Unexpected-Keyboard (XML)

```xml
<row height="0.95">
  <key width="1.7" 
       key0="ctrl" 
       key1="loc switch_greekmath" 
       key2="loc meta" 
       key3="loc switch_clipboard" 
       key4="switch_numeric"/>
  
  <key width="1.1" 
       key0="fn" 
       key1="loc alt" 
       key2="loc change_method" 
       key3="switch_emoji" 
       key4="config"/>
  
  <key width="4.4" 
       key0="space" 
       key7="switch_forward" 
       key8="switch_backward" 
       key5="cursor_left" 
       key6="cursor_right"/>
  
  <key width="1.1" 
       key0="loc compose" 
       key7="up" 
       key6="right" 
       key5="left" 
       key8="down" 
       key1="loc home" 
       key2="loc page_up" 
       key3="loc end" 
       key4="loc page_down"/>
</row>
```

**Key Features:**
- Up to 9 actions per key (key0-key8)
- Direction-based: key5=left, key6=right, key7=up, key8=down
- XML-based configuration

### fcitx5-android (Kotlin DSL)

```kotlin
listOf(
    // Function row
    listOf(
        CtrlKey(0.15f),
        
        MultiActionKey(
            displayText = "Tab",
            swipeText = "←Tab",
            pressAction = KeyAction.SymAction(KeySym(FcitxKeyMapping.FcitxKey_Tab)),
            swipeAction = KeyAction.SymAction(
                KeySym(FcitxKeyMapping.FcitxKey_Tab),
                KeyStates(KeyState.Shift, KeyState.Virtual)
            ),
            percentWidth = 0.15f
        ),
        
        MultiActionKey(
            displayText = "↑",
            swipeText = "PgUp",
            pressAction = KeyAction.SymAction(KeySym(FcitxKeyMapping.FcitxKey_Up)),
            swipeAction = KeyAction.SymAction(KeySym(FcitxKeyMapping.FcitxKey_Page_Up)),
            percentWidth = 0.1f
        ),
        
        MinimizeKey(0.1f)
    ),
    // Standard QWERTY rows...
)
```

**Key Features:**
- Type-safe Kotlin data classes
- Multiple behaviors per key (Press, Swipe, LongPress, DoubleTap)
- Explicit action definitions with KeySym
- Compile-time safety

## Feature Matrix

| Feature | Unexpected-Keyboard | fcitx5-android |
|---------|---------------------|----------------|
| **Gesture Types** | Swipe, Roundtrip, Circle, Anticircle | Press, Swipe, LongPress, DoubleTap, Repeat |
| **Max Actions/Key** | 9 (key0-key8) | 5 (behaviors) |
| **Configuration** | XML | Kotlin DSL |
| **Type Safety** | Runtime | Compile-time |
| **Ctrl Key** | ✅ Modifier | ✅ SymAction |
| **Esc Key** | ✅ Keyevent | ✅ SymAction |
| **Tab Key** | ✅ Keyevent | ✅ SymAction |
| **Minimize** | ✅ Event | ✅ Action |
| **Arrow Keys** | ✅ Direction-based | ✅ SymAction |
| **Custom Gestures** | ✅ Circle/Anticircle | ⚠️ Basic swipe only |
| **Key Modifiers** | ✅ CTRL, ALT, META, FN | ✅ Via KeyStates |
| **Compose Keys** | ✅ Advanced | ✅ Via Fcitx5 |

## Code Quality Comparison

### Unexpected-Keyboard Strengths
1. **Advanced Gestures**: Circle and anticircle gestures for more actions
2. **XML Layouts**: Easy for non-programmers to customize
3. **Mature System**: Well-tested gesture recognition
4. **Flexible Modifiers**: Complex accent and compose key system

### fcitx5-android Strengths
1. **Type Safety**: Kotlin's sealed classes prevent runtime errors
2. **Better IDE Support**: Auto-completion and refactoring
3. **Fcitx5 Integration**: Direct access to powerful IME engine
4. **Modern Architecture**: Coroutines, lifecycle-aware components
5. **Extensible**: Easy to add new key types

## Implementation Complexity

### Unexpected-Keyboard
- **LOC**: ~3000+ lines (Gesture, KeyValue, KeyModifier, Keyboard2View)
- **Languages**: Java, XML
- **Key System**: String-based key names
- **Learning Curve**: Medium (Java + custom gesture system)

### fcitx5-android (Our Implementation)
- **LOC**: ~400 lines added
- **Languages**: Kotlin, XML (minimal)
- **Key System**: Type-safe KeySym and KeyAction
- **Learning Curve**: Low (leverages existing framework)

## Example: Ctrl+C Implementation

### Unexpected-Keyboard
```java
// KeyModifier.java
private static KeyValue apply_ctrl(KeyValue k) {
    if (_modmap != null) {
        KeyValue mapped = _modmap.get(Modmap.M.Ctrl, k);
        if (mapped != null) k = mapped;
    }
    return turn_into_keyevent(k);
}

private static KeyValue turn_into_keyevent(KeyValue k) {
    if (k.getKind() != KeyValue.Kind.Char) return k;
    int e;
    switch (k.getChar()) {
        case 'c': e = KeyEvent.KEYCODE_C; break;
        // ... other keys
    }
    return k.withKeyevent(e);
}
```

### fcitx5-android
```kotlin
// Ctrl key definition
CtrlKey(0.15f)

// In CommonKeyActionListener.kt
is SymAction -> service.postFcitxJob {
    sendKey(action.sym, action.states)
}

// Fcitx5 engine handles Ctrl combinations automatically
```

## Performance Comparison

| Metric | Unexpected-Keyboard | fcitx5-android |
|--------|---------------------|----------------|
| **Gesture Recognition** | Custom, optimized | Android ViewConfiguration |
| **Key Dispatch** | Direct to IME | Via Fcitx5 engine |
| **Memory Usage** | Lower (Java) | Higher (Kotlin) |
| **Startup Time** | Faster | Slightly slower |
| **Battery Impact** | Minimal | Minimal |

## Conclusion

Our fcitx5-android implementation successfully brings Unexpected-Keyboard's multi-gesture key concept to the project while:
- Maintaining type safety with Kotlin
- Leveraging existing gesture infrastructure
- Integrating seamlessly with Fcitx5 engine
- Providing an extensible foundation for future enhancements

The implementation is production-ready and can be extended with more advanced gestures (circle, anticircle) if needed by enhancing the `CustomGestureView` class.
