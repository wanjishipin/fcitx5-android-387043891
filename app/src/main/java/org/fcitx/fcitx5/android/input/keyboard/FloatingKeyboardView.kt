/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2025 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.keyboard

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import org.fcitx.fcitx5.android.R
import org.fcitx.fcitx5.android.data.theme.Theme

/**
 * Container view for floating keyboard with drag and control buttons
 */
class FloatingKeyboardView(
    context: Context,
    private val theme: Theme,
    private val onClose: () -> Unit,
    private val onPositionChanged: (x: Int, y: Int) -> Unit
) : FrameLayout(context) {

    private var initialX: Int = 0
    private var initialY: Int = 0
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f

    private val titleBar: LinearLayout
    private val keyboardContainer: FrameLayout
    private val dragHandle: View
    private val closeButton: ImageButton

    init {
        // Set background
        setBackgroundColor(theme.barColor)
        elevation = 8f * resources.displayMetrics.density

        // Create title bar with drag handle and close button
        titleBar = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (40 * resources.displayMetrics.density).toInt()
            )
            setBackgroundColor(theme.keyboardColor)
            gravity = Gravity.CENTER_VERTICAL
        }

        // Drag handle
        dragHandle = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                1f
            )
            setBackgroundColor(Color.TRANSPARENT)
        }

        // Close button
        closeButton = ImageButton(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                (40 * resources.displayMetrics.density).toInt(),
                (40 * resources.displayMetrics.density).toInt()
            )
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setBackgroundColor(Color.TRANSPARENT)
            setColorFilter(theme.keyTextColor)
            setOnClickListener {
                onClose()
            }
        }

        titleBar.addView(dragHandle)
        titleBar.addView(closeButton)

        // Keyboard container
        keyboardContainer = FrameLayout(context).apply {
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply {
                topMargin = (40 * resources.displayMetrics.density).toInt()
            }
        }

        addView(keyboardContainer)
        addView(titleBar)

        // Set up drag handling on title bar
        dragHandle.setOnTouchListener { _, event ->
            handleDragTouch(event)
        }
    }

    fun setKeyboard(keyboard: View) {
        keyboardContainer.removeAllViews()
        keyboardContainer.addView(keyboard)
    }

    private fun handleDragTouch(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = (parent as? View)?.x?.toInt() ?: 0
                initialY = (parent as? View)?.y?.toInt() ?: 0
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = (event.rawX - initialTouchX).toInt()
                val deltaY = (event.rawY - initialTouchY).toInt()
                val newX = initialX + deltaX
                val newY = initialY + deltaY
                onPositionChanged(newX, newY)
                return true
            }
            MotionEvent.ACTION_UP -> {
                return true
            }
        }
        return false
    }
}
