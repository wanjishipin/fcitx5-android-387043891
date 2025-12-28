/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2025 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.keyboard

import android.annotation.SuppressLint
import android.content.Context
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.core.FcitxKeyMapping
import org.fcitx.fcitx5.android.core.KeySym
import org.fcitx.fcitx5.android.data.theme.Theme
import splitties.views.imageResource

/**
 * Extended text keyboard with function keys (Esc, Tab, Ctrl, etc.)
 * Inspired by Unexpected-Keyboard's gesture-based multi-function keys
 */
@SuppressLint("ViewConstructor")
class ExtendedTextKeyboard(
    context: Context,
    theme: Theme
) : BaseKeyboard(context, theme, Layout) {

    enum class CapsState { None, Once, Lock }

    companion object {
        const val Name = "ExtendedText"

        val Layout: List<List<KeyDef>> = listOf(
            // Function key row
            listOf(
                EscKey(0.1f),
                MultiActionKey(
                    displayText = "Tab",
                    swipeText = "←Tab",
                    pressAction = KeyAction.SymAction(KeySym(FcitxKeyMapping.FcitxKey_Tab)),
                    swipeAction = KeyAction.SymAction(
                        KeySym(FcitxKeyMapping.FcitxKey_Tab),
                        org.fcitx.fcitx5.android.core.KeyStates(
                            org.fcitx.fcitx5.android.core.KeyState.Shift,
                            org.fcitx.fcitx5.android.core.KeyState.Virtual
                        )
                    ),
                    percentWidth = 0.15f,
                    variant = KeyDef.Appearance.Variant.Alternative
                ),
                MultiActionKey(
                    displayText = "↑",
                    swipeText = "PgUp",
                    pressAction = KeyAction.SymAction(KeySym(FcitxKeyMapping.FcitxKey_Up)),
                    swipeAction = KeyAction.SymAction(KeySym(FcitxKeyMapping.FcitxKey_Page_Up)),
                    percentWidth = 0.1f
                ),
                MultiActionKey(
                    displayText = "↓",
                    swipeText = "PgDn",
                    pressAction = KeyAction.SymAction(KeySym(FcitxKeyMapping.FcitxKey_Down)),
                    swipeAction = KeyAction.SymAction(KeySym(FcitxKeyMapping.FcitxKey_Page_Down)),
                    percentWidth = 0.1f
                ),
                MultiActionKey(
                    displayText = "←",
                    swipeText = "Home",
                    pressAction = KeyAction.SymAction(KeySym(FcitxKeyMapping.FcitxKey_Left)),
                    swipeAction = KeyAction.SymAction(KeySym(FcitxKeyMapping.FcitxKey_Home)),
                    percentWidth = 0.1f
                ),
                MultiActionKey(
                    displayText = "→",
                    swipeText = "End",
                    pressAction = KeyAction.SymAction(KeySym(FcitxKeyMapping.FcitxKey_Right)),
                    swipeAction = KeyAction.SymAction(KeySym(FcitxKeyMapping.FcitxKey_End)),
                    percentWidth = 0.1f
                ),
                CtrlKey(0.15f),
                MinimizeKey(0.1f, KeyDef.Appearance.Variant.Alternative)
            ),
            // Standard QWERTY rows
            listOf(
                AlphabetKey("Q", "1"),
                AlphabetKey("W", "2"),
                AlphabetKey("E", "3"),
                AlphabetKey("R", "4"),
                AlphabetKey("T", "5"),
                AlphabetKey("Y", "6"),
                AlphabetKey("U", "7"),
                AlphabetKey("I", "8"),
                AlphabetKey("O", "9"),
                AlphabetKey("P", "0")
            ),
            listOf(
                AlphabetKey("A", "@"),
                AlphabetKey("S", "*"),
                AlphabetKey("D", "+"),
                AlphabetKey("F", "-"),
                AlphabetKey("G", "="),
                AlphabetKey("H", "/"),
                AlphabetKey("J", "#"),
                AlphabetKey("K", "("),
                AlphabetKey("L", ")")
            ),
            listOf(
                CapsKey(),
                AlphabetKey("Z", "'"),
                AlphabetKey("X", ":"),
                AlphabetKey("C", "\""),
                AlphabetKey("V", "?"),
                AlphabetKey("B", "!"),
                AlphabetKey("N", "~"),
                AlphabetKey("M", "\\"),
                BackspaceKey()
            ),
            listOf(
                LayoutSwitchKey("?123", ""),
                CommaKey(0.1f, KeyDef.Appearance.Variant.Alternative),
                LanguageKey(),
                SpaceKey(),
                SymbolKey(".", 0.1f, KeyDef.Appearance.Variant.Alternative),
                ReturnKey()
            )
        )
    }

    val caps: ImageKeyView by lazy { findViewById(R.id.button_caps) }
    val backspace: ImageKeyView by lazy { findViewById(R.id.button_backspace) }
    val lang: ImageKeyView by lazy { findViewById(R.id.button_lang) }
    val space: TextKeyView by lazy { findViewById(R.id.button_space) }
    val `return`: ImageKeyView by lazy { findViewById(R.id.button_return) }

    private var capsState: CapsState = CapsState.None

    override fun onAction(action: KeyAction, source: KeyActionListener.Source) {
        var transformed = action
        when (action) {
            is KeyAction.FcitxKeyAction -> when (source) {
                KeyActionListener.Source.Keyboard -> {
                    when (capsState) {
                        CapsState.None -> {
                            transformed = action.copy(act = action.act.lowercase())
                        }
                        CapsState.Once -> {
                            transformed = action.copy(
                                act = action.act.uppercase(),
                                states = org.fcitx.fcitx5.android.core.KeyStates(
                                    org.fcitx.fcitx5.android.core.KeyState.Virtual,
                                    org.fcitx.fcitx5.android.core.KeyState.Shift
                                )
                            )
                            switchCapsState()
                        }
                        CapsState.Lock -> {
                            transformed = action.copy(
                                act = action.act.uppercase(),
                                states = org.fcitx.fcitx5.android.core.KeyStates(
                                    org.fcitx.fcitx5.android.core.KeyState.Virtual,
                                    org.fcitx.fcitx5.android.core.KeyState.CapsLock
                                )
                            )
                        }
                    }
                }
                KeyActionListener.Source.Popup -> {
                    if (capsState == CapsState.Once) {
                        switchCapsState()
                    }
                }
            }
            is KeyAction.CapsAction -> switchCapsState(action.lock)
            else -> {}
        }
        super.onAction(transformed, source)
    }

    private fun switchCapsState(lock: Boolean = false) {
        capsState = when {
            lock -> when (capsState) {
                CapsState.None, CapsState.Once -> CapsState.Lock
                CapsState.Lock -> CapsState.None
            }
            else -> when (capsState) {
                CapsState.None -> CapsState.Once
                CapsState.Once, CapsState.Lock -> CapsState.None
            }
        }
        updateCapsKey()
    }

    private fun updateCapsKey() {
        caps.img.imageResource = when (capsState) {
            CapsState.None -> R.drawable.ic_capslock_none
            CapsState.Once -> R.drawable.ic_capslock_once
            CapsState.Lock -> R.drawable.ic_capslock_lock
        }
    }

    override fun onReturnDrawableUpdate(returnDrawable: Int) {
        `return`.img.imageResource = returnDrawable
    }
}
