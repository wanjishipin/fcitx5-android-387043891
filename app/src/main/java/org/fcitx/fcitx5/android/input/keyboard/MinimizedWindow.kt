/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2025 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.keyboard

import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.transition.Fade
import androidx.transition.Transition
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.input.InputView
import org.fcitx.fcitx5.android.input.dependency.inputView
import org.fcitx.fcitx5.android.input.dependency.theme
import org.fcitx.fcitx5.android.input.wm.EssentialWindow
import org.fcitx.fcitx5.android.input.wm.InputWindow
import org.fcitx.fcitx5.android.input.wm.InputWindowManager
import org.mechdancer.dependency.manager.must

/**
 * A minimized keyboard view that shows only a small floating button.
 * Clicking the button restores the full keyboard.
 */
class MinimizedWindow : InputWindow.SimpleInputWindow<MinimizedWindow>(), EssentialWindow {

    private val theme by manager.theme()
    private val windowManager: InputWindowManager by manager.must()
    private val inputView: InputView by manager.inputView()

    companion object : EssentialWindow.Key

    override val key: EssentialWindow.Key
        get() = MinimizedWindow

    private lateinit var floatingButton: FloatingButtonView
    
    // Store original state to restore later
    private var originalWindowManagerHeight: Int = 0
    private var savedViewsState = mutableListOf<Pair<View, Int>>()

    override fun enterAnimation(lastWindow: InputWindow): Transition? = Fade()

    override fun exitAnimation(nextWindow: InputWindow): Transition? = Fade()

    override fun onCreateView(): View {
        floatingButton = FloatingButtonView(context).apply {
            id = R.id.floating_button
            // Set colors from theme
            setColors(
                theme.keyBackgroundColor,
                theme.keyTextColor
            )
            setOnRestoreListener(object : FloatingButtonView.OnRestoreListener {
                override fun onRestore() {
                    // Switch back to keyboard window
                    windowManager.attachWindow(KeyboardWindow)
                }
            })
        }
        return floatingButton
    }

    override fun onAttached() {
        savedViewsState.clear()
        val floatingButtonHeight = (40 * context.resources.displayMetrics.density).toInt()
        
        // Save and resize windowManager.view height
        originalWindowManagerHeight = windowManager.view.layoutParams.height
        windowManager.view.updateLayoutParams {
            height = floatingButtonHeight
        }
        
        // Get the keyboard_view container and hide its children (except input_window)
        val keyboardViewContainer = inputView.findViewById<View>(R.id.keyboard_view)
        if (keyboardViewContainer is ViewGroup) {
            for (i in 0 until keyboardViewContainer.childCount) {
                val child = keyboardViewContainer.getChildAt(i)
                // Keep input_window visible (it contains our floating button)
                if (child.id == R.id.input_window) continue
                // Save and hide other children
                savedViewsState.add(child to child.visibility)
                child.visibility = View.GONE
            }
        }
        
        // Hide InputView's direct children that we don't need
        // The preedit view and popup
        (inputView as ViewGroup).let { parent ->
            for (i in 0 until parent.childCount) {
                val child = parent.getChildAt(i)
                // Keep keyboard_view visible (it contains our floating button via input_window)
                if (child.id == R.id.keyboard_view) continue
                // Save and hide other children
                savedViewsState.add(child to child.visibility)
                child.visibility = View.GONE
            }
        }
    }

    override fun onDetached() {
        // Restore windowManager.view height
        if (originalWindowManagerHeight != 0) {
            windowManager.view.updateLayoutParams {
                height = originalWindowManagerHeight
            }
        }
        
        // Restore all saved views visibility
        for ((view, visibility) in savedViewsState) {
            view.visibility = visibility
        }
        savedViewsState.clear()
    }
}
