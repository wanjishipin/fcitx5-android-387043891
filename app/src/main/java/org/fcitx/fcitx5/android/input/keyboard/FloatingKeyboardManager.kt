/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2025 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import org.fcitx.fcitx5.android.data.theme.Theme
import org.fcitx.fcitx5.android.service.FloatingKeyboardAccessibilityService
import timber.log.Timber

/**
 * Manager for floating keyboard window that appears as a system overlay
 */
class FloatingKeyboardManager(private val context: Context) {

    private val windowManager: WindowManager = 
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    
    private var floatingContainer: LinearLayout? = null
    private var keyboardContainer: FrameLayout? = null
    private var onKeyEventCallback: ((Int, Int) -> Unit)? = null
    
    // For drag functionality
    private var initialX: Int = 0
    private var initialY: Int = 0
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f
    
    // For key repeat
    private val handler = Handler(Looper.getMainLooper())
    private var repeatRunnable: Runnable? = null
    private val initialRepeatDelay = 400L  // Initial delay before repeat starts
    private val repeatInterval = 50L       // Interval between repeats
    
    // Window parameters
    private val windowParams = WindowManager.LayoutParams().apply {
        type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        format = PixelFormat.TRANSLUCENT
        gravity = Gravity.TOP or Gravity.START
        
        // Default size and position
        width = (context.resources.displayMetrics.widthPixels * 0.95).toInt()
        height = WindowManager.LayoutParams.WRAP_CONTENT
        x = (context.resources.displayMetrics.widthPixels * 0.025).toInt()
        y = (context.resources.displayMetrics.heightPixels * 0.5).toInt()
    }

    fun isShowing(): Boolean = floatingContainer?.parent != null

    /**
     * Show floating keyboard with a simple key layout
     */
    fun showFloatingKeyboard(theme: Theme, onKeyEvent: (keyCode: Int, metaState: Int) -> Unit) {
        if (!FloatingKeyboardAccessibilityService.isEnabled()) {
            Timber.w("Accessibility service not enabled, cannot show floating keyboard")
            return
        }

        if (isShowing()) {
            Timber.d("Floating keyboard already showing")
            return
        }

        onKeyEventCallback = onKeyEvent
        
        val density = context.resources.displayMetrics.density
        
        // Create main container
        floatingContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(theme.barColor)
            elevation = 8f * density
        }
        
        // Create title bar with drag handle and close button
        val titleBar = createTitleBar(theme, density)
        floatingContainer?.addView(titleBar)
        
        // Create keyboard container
        keyboardContainer = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundColor(theme.keyboardColor)
        }
        
        // Add a simple floating keyboard layout
        val keyboardLayout = createSimpleKeyboardLayout(theme, density)
        keyboardContainer?.addView(keyboardLayout)
        floatingContainer?.addView(keyboardContainer)

        try {
            windowManager.addView(floatingContainer, windowParams)
            Timber.d("Floating keyboard shown")
        } catch (e: Exception) {
            Timber.e(e, "Failed to show floating keyboard")
        }
    }
    
    private fun createTitleBar(theme: Theme, density: Float): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (36 * density).toInt()
            )
            setBackgroundColor(theme.keyBackgroundColor)
            gravity = Gravity.CENTER_VERTICAL
            
            // Drag handle area
            val dragHandle = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    1f
                )
                setBackgroundColor(Color.TRANSPARENT)
            }
            
            dragHandle.setOnTouchListener { _, event ->
                handleDragTouch(event)
            }
            
            // Close button
            val closeButton = ImageButton(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    (36 * density).toInt(),
                    (36 * density).toInt()
                )
                setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                setBackgroundColor(Color.TRANSPARENT)
                setColorFilter(theme.keyTextColor)
                setOnClickListener {
                    hide()
                }
            }
            
            addView(dragHandle)
            addView(closeButton)
        }
    }
    
    private fun createSimpleKeyboardLayout(theme: Theme, density: Float): LinearLayout {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding((4 * density).toInt(), (4 * density).toInt(), 
                (4 * density).toInt(), (4 * density).toInt())
        }
        
        // Row 1: Function keys
        val row1 = createKeyRow(theme, density, listOf(
            KeyConfig("Esc", KeyEvent.KEYCODE_ESCAPE),
            KeyConfig("Tab", KeyEvent.KEYCODE_TAB),
            KeyConfig("↑", KeyEvent.KEYCODE_DPAD_UP, repeatable = true),
            KeyConfig("↓", KeyEvent.KEYCODE_DPAD_DOWN, repeatable = true),
            KeyConfig("←", KeyEvent.KEYCODE_DPAD_LEFT, repeatable = true),
            KeyConfig("→", KeyEvent.KEYCODE_DPAD_RIGHT, repeatable = true),
            KeyConfig("OK", KeyEvent.KEYCODE_DPAD_CENTER),
            KeyConfig("Menu", KeyEvent.KEYCODE_MENU),
        ))
        layout.addView(row1)
        
        // Row 2: QWERTY top row
        val row2 = createKeyRow(theme, density, listOf(
            KeyConfig("Q", KeyEvent.KEYCODE_Q, repeatable = true),
            KeyConfig("W", KeyEvent.KEYCODE_W, repeatable = true),
            KeyConfig("E", KeyEvent.KEYCODE_E, repeatable = true),
            KeyConfig("R", KeyEvent.KEYCODE_R, repeatable = true),
            KeyConfig("T", KeyEvent.KEYCODE_T, repeatable = true),
            KeyConfig("Y", KeyEvent.KEYCODE_Y, repeatable = true),
            KeyConfig("U", KeyEvent.KEYCODE_U, repeatable = true),
            KeyConfig("I", KeyEvent.KEYCODE_I, repeatable = true),
            KeyConfig("O", KeyEvent.KEYCODE_O, repeatable = true),
            KeyConfig("P", KeyEvent.KEYCODE_P, repeatable = true),
        ))
        layout.addView(row2)
        
        // Row 3: QWERTY middle row
        val row3 = createKeyRow(theme, density, listOf(
            KeyConfig("A", KeyEvent.KEYCODE_A, repeatable = true),
            KeyConfig("S", KeyEvent.KEYCODE_S, repeatable = true),
            KeyConfig("D", KeyEvent.KEYCODE_D, repeatable = true),
            KeyConfig("F", KeyEvent.KEYCODE_F, repeatable = true),
            KeyConfig("G", KeyEvent.KEYCODE_G, repeatable = true),
            KeyConfig("H", KeyEvent.KEYCODE_H, repeatable = true),
            KeyConfig("J", KeyEvent.KEYCODE_J, repeatable = true),
            KeyConfig("K", KeyEvent.KEYCODE_K, repeatable = true),
            KeyConfig("L", KeyEvent.KEYCODE_L, repeatable = true),
        ))
        layout.addView(row3)
        
        // Row 4: QWERTY bottom row
        val row4 = createKeyRow(theme, density, listOf(
            KeyConfig("Z", KeyEvent.KEYCODE_Z, repeatable = true),
            KeyConfig("X", KeyEvent.KEYCODE_X, repeatable = true),
            KeyConfig("C", KeyEvent.KEYCODE_C, repeatable = true),
            KeyConfig("V", KeyEvent.KEYCODE_V, repeatable = true),
            KeyConfig("B", KeyEvent.KEYCODE_B, repeatable = true),
            KeyConfig("N", KeyEvent.KEYCODE_N, repeatable = true),
            KeyConfig("M", KeyEvent.KEYCODE_M, repeatable = true),
            KeyConfig("⌫", KeyEvent.KEYCODE_DEL, repeatable = true),
        ))
        layout.addView(row4)
        
        // Row 5: Space row
        val row5 = createKeyRow(theme, density, listOf(
            KeyConfig("Ctrl", KeyEvent.KEYCODE_CTRL_LEFT, weight = 1f),
            KeyConfig("Space", KeyEvent.KEYCODE_SPACE, weight = 4f, repeatable = true),
            KeyConfig("Enter", KeyEvent.KEYCODE_ENTER, weight = 1.5f),
        ))
        layout.addView(row5)
        
        return layout
    }
    
    private data class KeyConfig(
        val label: String,
        val keyCode: Int,
        val metaState: Int = 0,
        val weight: Float = 1f,
        val repeatable: Boolean = false
    )
    
    private fun createKeyRow(theme: Theme, density: Float, keys: List<KeyConfig>): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (42 * density).toInt()
            ).apply {
                bottomMargin = (2 * density).toInt()
            }
            
            for (key in keys) {
                val keyButton = createKeyButton(theme, density, key)
                addView(keyButton)
            }
        }
    }
    
    @SuppressLint("ClickableViewAccessibility")
    private fun createKeyButton(theme: Theme, density: Float, key: KeyConfig): android.widget.Button {
        return android.widget.Button(context).apply {
            text = key.label
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                key.weight
            ).apply {
                marginStart = (2 * density).toInt()
                marginEnd = (2 * density).toInt()
            }
            setBackgroundColor(theme.keyBackgroundColor)
            setTextColor(theme.keyTextColor)
            textSize = 14f
            isAllCaps = false
            setPadding(0, 0, 0, 0)
            
            if (key.repeatable) {
                // Use touch listener for repeatable keys
                setOnTouchListener { v, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            v.isPressed = true
                            v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                            // Send first key event immediately
                            Timber.d("Key pressed: ${key.label}, keyCode: ${key.keyCode}")
                            onKeyEventCallback?.invoke(key.keyCode, key.metaState)
                            // Start repeat after delay
                            startKeyRepeat(key.keyCode, key.metaState)
                            true
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            v.isPressed = false
                            stopKeyRepeat()
                            true
                        }
                        else -> false
                    }
                }
            } else {
                // Use click listener for non-repeatable keys
                setOnClickListener {
                    Timber.d("Key clicked: ${key.label}, keyCode: ${key.keyCode}")
                    performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                    onKeyEventCallback?.invoke(key.keyCode, key.metaState)
                }
            }
        }
    }
    
    private fun startKeyRepeat(keyCode: Int, metaState: Int) {
        stopKeyRepeat()
        repeatRunnable = object : Runnable {
            override fun run() {
                Timber.d("Key repeat: keyCode=$keyCode")
                onKeyEventCallback?.invoke(keyCode, metaState)
                handler.postDelayed(this, repeatInterval)
            }
        }
        handler.postDelayed(repeatRunnable!!, initialRepeatDelay)
    }
    
    private fun stopKeyRepeat() {
        repeatRunnable?.let {
            handler.removeCallbacks(it)
        }
        repeatRunnable = null
    }
    
    private fun handleDragTouch(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = windowParams.x
                initialY = windowParams.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = (event.rawX - initialTouchX).toInt()
                val deltaY = (event.rawY - initialTouchY).toInt()
                windowParams.x = initialX + deltaX
                windowParams.y = initialY + deltaY
                try {
                    windowManager.updateViewLayout(floatingContainer, windowParams)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to update position")
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                return true
            }
        }
        return false
    }

    fun hide() {
        if (!isShowing()) return

        stopKeyRepeat()
        
        try {
            windowManager.removeView(floatingContainer)
            floatingContainer = null
            keyboardContainer = null
            onKeyEventCallback = null
            Timber.d("Floating keyboard hidden")
        } catch (e: Exception) {
            Timber.e(e, "Failed to hide floating keyboard")
        }
    }

    fun updatePosition(x: Int, y: Int) {
        if (!isShowing()) return

        windowParams.x = x
        windowParams.y = y
        try {
            windowManager.updateViewLayout(floatingContainer, windowParams)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update floating keyboard position")
        }
    }
}
