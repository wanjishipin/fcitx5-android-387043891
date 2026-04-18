/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2025 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.Uri
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
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import org.fcitx.fcitx5.android.data.theme.Theme
import timber.log.Timber

/**
 * Manager for floating keyboard window that appears as a system overlay
 */
class FloatingKeyboardManager(private val context: Context) {

    companion object {
        // Light gray with 30% opacity for visibility on any app
        private val KEYBOARD_BG_COLOR = Color.argb(77, 200, 200, 200)  // 30% opacity light gray
        private val TITLE_BAR_BG_COLOR = Color.argb(77, 200, 200, 200)  // 30% opacity light gray
    }

    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var floatingContainer: LinearLayout? = null
    private var keyboardContainer: FrameLayout? = null
    private var minimizedView: FrameLayout? = null
    private var onKeyEventCallback: ((Int, Int) -> Unit)? = null
    private var onTextInputCallback: ((String) -> Unit)? = null
    private var onToggleMainKeyboardCallback: (() -> Unit)? = null
    
    // State tracking
    private var isMinimized = false
    private var currentTheme: Theme? = null
    
    // Modifier key states
    private var isShiftPressed = false
    private var isShiftLocked = false  // Caps Lock state
    private var lastShiftClickTime = 0L
    private val doubleClickThreshold = 300L  // ms for double-click detection
    private var shiftButton: android.widget.Button? = null
    
    // Collapsible rows state
    private var isKeyboardExpanded = false  // Default to collapsed
    private var collapsibleRowsContainer: LinearLayout? = null
    private var expandCollapseButton: ImageButton? = null
    
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
    
    // Window parameters for full keyboard
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
    
    // Window parameters for minimized view
    private val minimizedParams = WindowManager.LayoutParams().apply {
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
        width = WindowManager.LayoutParams.WRAP_CONTENT
        height = WindowManager.LayoutParams.WRAP_CONTENT
        x = context.resources.displayMetrics.widthPixels - 80
        y = (context.resources.displayMetrics.heightPixels * 0.3).toInt()
    }

    fun isShowing(): Boolean = floatingContainer?.parent != null || minimizedView?.parent != null

    /**
     * Show floating keyboard with a simple key layout
     */
    fun showFloatingKeyboard(
        theme: Theme,
        onKeyEvent: (keyCode: Int, metaState: Int) -> Unit,
        onTextInput: (String) -> Unit = {},
        onToggleMainKeyboard: (() -> Unit)? = null
    ) {
        if (isShowing()) {
            Timber.d("Floating keyboard already showing")
            return
        }

        onKeyEventCallback = onKeyEvent
        onTextInputCallback = onTextInput
        onToggleMainKeyboardCallback = onToggleMainKeyboard
        currentTheme = theme
        isMinimized = false
        
        val density = context.resources.displayMetrics.density
        
        // Create main container with semi-transparent background
        floatingContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(KEYBOARD_BG_COLOR)
            elevation = 8f * density
        }
        
        // Create title bar with drag handle, minimize and close buttons
        val titleBar = createTitleBar(theme, density)
        floatingContainer?.addView(titleBar)
        
        // Create keyboard container
        keyboardContainer = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundColor(KEYBOARD_BG_COLOR)
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
    
    /**
     * Minimize the floating keyboard to a small icon
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun minimize() {
        if (isMinimized) return
        
        val theme = currentTheme ?: return
        val density = context.resources.displayMetrics.density
        
        // Save current position for restore
        windowParams.let { params ->
            minimizedParams.x = params.x + params.width - (56 * density).toInt()
            minimizedParams.y = params.y
        }
        
        // Remove full keyboard
        floatingContainer?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                Timber.e(e, "Failed to remove floating keyboard")
            }
        }
        floatingContainer = null
        keyboardContainer = null
        
        // Create minimized view
        minimizedView = FrameLayout(context).apply {
            val size = (56 * density).toInt()
            layoutParams = FrameLayout.LayoutParams(size, size)
            setBackgroundColor(TITLE_BAR_BG_COLOR)
            elevation = 8f * density
        }
        
        // Add keyboard icon
        val iconButton = ImageButton(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setImageResource(android.R.drawable.ic_menu_recent_history)
            setBackgroundColor(Color.TRANSPARENT)
            setColorFilter(Color.WHITE)
            scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
            setPadding((8 * density).toInt(), (8 * density).toInt(), 
                (8 * density).toInt(), (8 * density).toInt())
        }
        
        // Click to restore
        iconButton.setOnClickListener {
            restore()
        }
        
        // Drag support for minimized view
        iconButton.setOnTouchListener { _, event ->
            handleMinimizedDragTouch(event)
        }
        
        minimizedView?.addView(iconButton)
        
        try {
            windowManager.addView(minimizedView, minimizedParams)
            isMinimized = true
            Timber.d("Floating keyboard minimized")
        } catch (e: Exception) {
            Timber.e(e, "Failed to show minimized view")
        }
    }
    
    /**
     * Restore the floating keyboard from minimized state
     */
    private fun restore() {
        if (!isMinimized) return
        
        val theme = currentTheme ?: return
        val callback = onKeyEventCallback ?: return
        
        // Save minimized position for keyboard position
        minimizedParams.let { params ->
            windowParams.x = (context.resources.displayMetrics.widthPixels * 0.025).toInt()
            windowParams.y = params.y
        }
        
        // Remove minimized view
        minimizedView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                Timber.e(e, "Failed to remove minimized view")
            }
        }
        minimizedView = null
        isMinimized = false
        
        // Show full keyboard again
        val density = context.resources.displayMetrics.density
        
        floatingContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(KEYBOARD_BG_COLOR)
            elevation = 8f * density
        }
        
        val titleBar = createTitleBar(theme, density)
        floatingContainer?.addView(titleBar)
        
        keyboardContainer = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundColor(KEYBOARD_BG_COLOR)
        }
        
        val keyboardLayout = createSimpleKeyboardLayout(theme, density)
        keyboardContainer?.addView(keyboardLayout)
        floatingContainer?.addView(keyboardContainer)

        try {
            windowManager.addView(floatingContainer, windowParams)
            Timber.d("Floating keyboard restored")
        } catch (e: Exception) {
            Timber.e(e, "Failed to restore floating keyboard")
        }
    }
    
    private fun handleMinimizedDragTouch(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = minimizedParams.x
                initialY = minimizedParams.y
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                return false // Allow click events
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - initialTouchX
                val dy = event.rawY - initialTouchY
                
                // Only start drag if moved enough
                if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                    minimizedParams.x = initialX + dx.toInt()
                    minimizedParams.y = initialY + dy.toInt()
                    try {
                        windowManager.updateViewLayout(minimizedView, minimizedParams)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to update minimized view position")
                    }
                    return true
                }
            }
        }
        return false
    }
    
    private fun createTitleBar(theme: Theme, density: Float): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (36 * density).toInt()
            )
            setBackgroundColor(TITLE_BAR_BG_COLOR)
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
            
            // Minimize button
            val minimizeButton = ImageButton(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    (36 * density).toInt(),
                    (36 * density).toInt()
                )
                setImageResource(android.R.drawable.ic_menu_recent_history)
                setBackgroundColor(Color.TRANSPARENT)
                setColorFilter(theme.keyTextColor)
                setOnClickListener {
                    minimize()
                }
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
            
            // Keyboard toggle button (show/hide main keyboard) - using TextView for keyboard symbol
            val keyboardToggleButton = android.widget.TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    (36 * density).toInt(),
                    (36 * density).toInt()
                )
                text = "⌨"  // Keyboard Unicode symbol
                textSize = 20f
                setTextColor(theme.keyTextColor)
                gravity = Gravity.CENTER
                setBackgroundColor(Color.TRANSPARENT)
                setOnClickListener {
                    onToggleMainKeyboardCallback?.invoke()
                }
            }
            
            // Expand/Collapse button for number and letter rows
            expandCollapseButton = ImageButton(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    (36 * density).toInt(),
                    (36 * density).toInt()
                )
                setImageResource(
                    if (isKeyboardExpanded) android.R.drawable.arrow_up_float
                    else android.R.drawable.arrow_down_float
                )
                setBackgroundColor(Color.TRANSPARENT)
                setColorFilter(theme.keyTextColor)
                contentDescription = "Expand/Collapse keyboard"
                setOnClickListener {
                    toggleKeyboardExpanded()
                }
            }
            
            // About button (opens Bilibili page)
            val aboutButton = ImageButton(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    (36 * density).toInt(),
                    (36 * density).toInt()
                )
                setImageResource(android.R.drawable.ic_menu_info_details)
                setBackgroundColor(Color.TRANSPARENT)
                setColorFilter(theme.keyTextColor)
                setOnClickListener {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://space.bilibili.com/387043891"))
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to open about page")
                    }
                }
            }
            
            addView(dragHandle)
            addView(expandCollapseButton)
            addView(aboutButton)
            addView(keyboardToggleButton)
            addView(minimizeButton)
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
        
        // Row 1: Function keys with Space and Enter (always visible)
        val row1 = createKeyRow(theme, density, listOf(
            KeyConfig("Esc", KeyEvent.KEYCODE_ESCAPE),
            KeyConfig("Tab", KeyEvent.KEYCODE_TAB),
            KeyConfig("↑", KeyEvent.KEYCODE_DPAD_UP, repeatable = true),
            KeyConfig("↓", KeyEvent.KEYCODE_DPAD_DOWN, repeatable = true),
            KeyConfig("←", KeyEvent.KEYCODE_DPAD_LEFT, repeatable = true),
            KeyConfig("→", KeyEvent.KEYCODE_DPAD_RIGHT, repeatable = true),
            KeyConfig("OK", KeyEvent.KEYCODE_DPAD_CENTER),
            KeyConfig("M", KeyEvent.KEYCODE_MENU),  // Menu shortened
            KeyConfig("SP", KeyEvent.KEYCODE_SPACE, weight = 1.2f, repeatable = true),  // Space shortened
            KeyConfig("⏎", KeyEvent.KEYCODE_ENTER),  // Enter as symbol
        ))
        layout.addView(row1)
        
        // Row 2: Scrollable symbols row (always visible)
        val symbolsScrollView = createScrollableSymbolsRow(theme, density)
        layout.addView(symbolsScrollView)
        
        // Collapsible container for number and letter rows
        collapsibleRowsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            visibility = if (isKeyboardExpanded) View.VISIBLE else View.GONE
        }
        
        // Row 3: Number row
        val row3 = createKeyRow(theme, density, listOf(
            KeyConfig("1", KeyEvent.KEYCODE_1, repeatable = true),
            KeyConfig("2", KeyEvent.KEYCODE_2, repeatable = true),
            KeyConfig("3", KeyEvent.KEYCODE_3, repeatable = true),
            KeyConfig("4", KeyEvent.KEYCODE_4, repeatable = true),
            KeyConfig("5", KeyEvent.KEYCODE_5, repeatable = true),
            KeyConfig("6", KeyEvent.KEYCODE_6, repeatable = true),
            KeyConfig("7", KeyEvent.KEYCODE_7, repeatable = true),
            KeyConfig("8", KeyEvent.KEYCODE_8, repeatable = true),
            KeyConfig("9", KeyEvent.KEYCODE_9, repeatable = true),
            KeyConfig("0", KeyEvent.KEYCODE_0, repeatable = true),
        ))
        collapsibleRowsContainer?.addView(row3)
        
        // Row 4: QWERTY top row
        val row4 = createKeyRow(theme, density, listOf(
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
        collapsibleRowsContainer?.addView(row4)
        
        // Row 5: QWERTY middle row
        val row5 = createKeyRow(theme, density, listOf(
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
        collapsibleRowsContainer?.addView(row5)
        
        // Row 6: QWERTY bottom row with Shift
        val row6 = createKeyRow(theme, density, listOf(
            KeyConfig("⇧", KeyEvent.KEYCODE_SHIFT_LEFT, isModifier = true),
            KeyConfig("Z", KeyEvent.KEYCODE_Z, repeatable = true),
            KeyConfig("X", KeyEvent.KEYCODE_X, repeatable = true),
            KeyConfig("C", KeyEvent.KEYCODE_C, repeatable = true),
            KeyConfig("V", KeyEvent.KEYCODE_V, repeatable = true),
            KeyConfig("B", KeyEvent.KEYCODE_B, repeatable = true),
            KeyConfig("N", KeyEvent.KEYCODE_N, repeatable = true),
            KeyConfig("M", KeyEvent.KEYCODE_M, repeatable = true),
            KeyConfig("⌫", KeyEvent.KEYCODE_DEL, repeatable = true),
        ))
        collapsibleRowsContainer?.addView(row6)
        
        layout.addView(collapsibleRowsContainer)
        
        return layout
    }
    
    private fun toggleKeyboardExpanded() {
        isKeyboardExpanded = !isKeyboardExpanded
        collapsibleRowsContainer?.visibility = if (isKeyboardExpanded) View.VISIBLE else View.GONE
        updateExpandCollapseButtonIcon()
        
        // Update window layout
        try {
            windowManager.updateViewLayout(floatingContainer, windowParams)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update window layout")
        }
    }
    
    private fun updateExpandCollapseButtonIcon() {
        expandCollapseButton?.setImageResource(
            if (isKeyboardExpanded) android.R.drawable.arrow_up_float
            else android.R.drawable.arrow_down_float
        )
    }
    
    private fun createScrollableSymbolsRow(theme: Theme, density: Float): HorizontalScrollView {
        val scrollView = HorizontalScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (42 * density).toInt()
            ).apply {
                bottomMargin = (2 * density).toInt()
            }
            isHorizontalScrollBarEnabled = false
        }
        
        val symbolsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        
        // Extended symbols list - most used first
        val symbols = listOf(
            // Most frequently used symbols first
            KeyConfig(":", KeyEvent.KEYCODE_SEMICOLON, metaState = KeyEvent.META_SHIFT_ON),
            KeyConfig("/", KeyEvent.KEYCODE_SLASH),
            KeyConfig("\"", KeyEvent.KEYCODE_APOSTROPHE, metaState = KeyEvent.META_SHIFT_ON),
            KeyConfig(".", KeyEvent.KEYCODE_PERIOD),
            KeyConfig(",", KeyEvent.KEYCODE_COMMA),
            KeyConfig("-", KeyEvent.KEYCODE_MINUS),
            KeyConfig("_", KeyEvent.KEYCODE_MINUS, metaState = KeyEvent.META_SHIFT_ON),
            KeyConfig("=", KeyEvent.KEYCODE_EQUALS),
            KeyConfig("+", KeyEvent.KEYCODE_EQUALS, metaState = KeyEvent.META_SHIFT_ON),
            KeyConfig("'", KeyEvent.KEYCODE_APOSTROPHE),
            KeyConfig(";", KeyEvent.KEYCODE_SEMICOLON),
            KeyConfig("?", KeyEvent.KEYCODE_SLASH, metaState = KeyEvent.META_SHIFT_ON),
            KeyConfig("!", KeyEvent.KEYCODE_1, metaState = KeyEvent.META_SHIFT_ON),
            KeyConfig("@", KeyEvent.KEYCODE_2, metaState = KeyEvent.META_SHIFT_ON),
            KeyConfig("#", KeyEvent.KEYCODE_3, metaState = KeyEvent.META_SHIFT_ON),
            KeyConfig("$", KeyEvent.KEYCODE_4, metaState = KeyEvent.META_SHIFT_ON),
            KeyConfig("%", KeyEvent.KEYCODE_5, metaState = KeyEvent.META_SHIFT_ON),
            KeyConfig("^", KeyEvent.KEYCODE_6, metaState = KeyEvent.META_SHIFT_ON),
            KeyConfig("&", KeyEvent.KEYCODE_7, metaState = KeyEvent.META_SHIFT_ON),
            KeyConfig("*", KeyEvent.KEYCODE_8, metaState = KeyEvent.META_SHIFT_ON),
            KeyConfig("(", KeyEvent.KEYCODE_9, metaState = KeyEvent.META_SHIFT_ON),
            KeyConfig(")", KeyEvent.KEYCODE_0, metaState = KeyEvent.META_SHIFT_ON),
            KeyConfig("[", KeyEvent.KEYCODE_LEFT_BRACKET),
            KeyConfig("]", KeyEvent.KEYCODE_RIGHT_BRACKET),
            KeyConfig("{", KeyEvent.KEYCODE_LEFT_BRACKET, metaState = KeyEvent.META_SHIFT_ON),
            KeyConfig("}", KeyEvent.KEYCODE_RIGHT_BRACKET, metaState = KeyEvent.META_SHIFT_ON),
            KeyConfig("<", KeyEvent.KEYCODE_COMMA, metaState = KeyEvent.META_SHIFT_ON),
            KeyConfig(">", KeyEvent.KEYCODE_PERIOD, metaState = KeyEvent.META_SHIFT_ON),
            KeyConfig("\\", KeyEvent.KEYCODE_BACKSLASH),
            KeyConfig("|", KeyEvent.KEYCODE_BACKSLASH, metaState = KeyEvent.META_SHIFT_ON),
            KeyConfig("`", KeyEvent.KEYCODE_GRAVE),
            KeyConfig("~", KeyEvent.KEYCODE_GRAVE, metaState = KeyEvent.META_SHIFT_ON),
        )
        
        val keyWidth = (36 * density).toInt()
        for (symbol in symbols) {
            val button = createSymbolButton(theme, density, symbol, keyWidth)
            symbolsContainer.addView(button)
        }
        
        scrollView.addView(symbolsContainer)
        return scrollView
    }
    
    @SuppressLint("ClickableViewAccessibility")
    private fun createSymbolButton(theme: Theme, density: Float, key: KeyConfig, width: Int): android.widget.Button {
        return android.widget.Button(context).apply {
            text = key.label
            layoutParams = LinearLayout.LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT).apply {
                marginStart = (1 * density).toInt()
                marginEnd = (1 * density).toInt()
            }
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(theme.keyTextColor)
            textSize = 16f
            isAllCaps = false
            setPadding(0, 0, 0, 0)

            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        v.isPressed = true
                        v.setBackgroundColor(Color.argb(128, 200, 200, 200))
                        v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.isPressed = false
                        v.setBackgroundColor(Color.TRANSPARENT)
                        if (event.action == MotionEvent.ACTION_UP) {
                            onTextInputCallback?.invoke(key.label)
                            Timber.d("Symbol key tapped: ${key.label}")
                        }
                        true
                    }
                    else -> false
                }
            }
        }
    }
    
    private data class KeyConfig(
        val label: String,
        val keyCode: Int,
        val metaState: Int = 0,
        val weight: Float = 1f,
        val repeatable: Boolean = false,
        val isModifier: Boolean = false
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
        val button = android.widget.Button(context).apply {
            text = key.label
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.MATCH_PARENT,
                key.weight
            ).apply {
                marginStart = (2 * density).toInt()
                marginEnd = (2 * density).toInt()
            }
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(theme.keyTextColor)
            textSize = 14f
            isAllCaps = false
            setPadding(0, 0, 0, 0)
        }
        
        // Track modifier buttons for visual feedback
        if (key.isModifier) {
            when (key.keyCode) {
                KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> {
                    shiftButton = button
                }
            }
        }
        
        if (key.isModifier) {
            // Modifier keys toggle state
            button.setOnClickListener {
                button.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                when (key.keyCode) {
                    KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT -> {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastShiftClickTime < doubleClickThreshold) {
                            // Double click: toggle Caps Lock
                            isShiftLocked = !isShiftLocked
                            isShiftPressed = isShiftLocked
                            updateShiftButtonState(shiftButton, theme)
                            Timber.d("Shift locked: $isShiftLocked")
                        } else {
                            // Single click: toggle Shift (unless locked)
                            if (isShiftLocked) {
                                // Unlock on single click when locked
                                isShiftLocked = false
                                isShiftPressed = false
                                updateShiftButtonState(shiftButton, theme)
                                Timber.d("Shift unlocked")
                            } else {
                                isShiftPressed = !isShiftPressed
                                updateShiftButtonState(shiftButton, theme)
                                Timber.d("Shift toggled: $isShiftPressed")
                            }
                        }
                        lastShiftClickTime = currentTime
                    }
                }
            }
        } else if (key.repeatable) {
            // Use touch listener for repeatable keys
            button.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        v.isPressed = true
                        v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                        if (isCharacterKey(key.keyCode)) {
                            // For character keys (letters, numbers), send as text input
                            val charToSend = if (isShiftPressed || isShiftLocked) {
                                key.label.uppercase()
                            } else {
                                key.label.lowercase()
                            }
                            Timber.d("Sending character: $charToSend")
                            onTextInputCallback?.invoke(charToSend)
                            // Only reset shift if not locked
                            if (!isShiftLocked) {
                                resetModifiersAfterKey(theme)
                            }
                            // Start repeat for character
                            if (isShiftLocked) {
                                startKeyRepeatWithText(key.label.uppercase())
                            } else {
                                startKeyRepeatWithText(key.label.lowercase())
                            }
                        } else {
                            // For function keys, send key event
                            val metaState = getCurrentMetaState()
                            Timber.d("Key pressed: ${key.label}, keyCode: ${key.keyCode}, metaState: $metaState")
                            onKeyEventCallback?.invoke(key.keyCode, metaState)
                            resetModifiersAfterKey(theme)
                            startKeyRepeat(key.keyCode, 0)
                        }
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
            button.setOnClickListener {
                button.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                // For character keys, send as text input
                if (isCharacterKey(key.keyCode)) {
                    val charToSend = if (isShiftPressed || isShiftLocked) {
                        key.label.uppercase()
                    } else {
                        key.label.lowercase()
                    }
                    Timber.d("Sending character: $charToSend")
                    onTextInputCallback?.invoke(charToSend)
                    // Only reset shift if not locked
                    if (!isShiftLocked) {
                        resetModifiersAfterKey(theme)
                    }
                } else {
                    // For function keys, send key event
                    val metaState = getCurrentMetaState()
                    Timber.d("Key clicked: ${key.label}, keyCode: ${key.keyCode}, metaState: $metaState")
                    onKeyEventCallback?.invoke(key.keyCode, metaState)
                    resetModifiersAfterKey(theme)
                }
            }
        }
        
        return button
    }
    
    private fun getCurrentMetaState(): Int {
        var metaState = 0
        if (isShiftPressed || isShiftLocked) {
            metaState = metaState or KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON
        }
        return metaState
    }
    
    private fun resetModifiersAfterKey(theme: Theme) {
        // Don't reset Shift if locked
        if (isShiftPressed && !isShiftLocked) {
            isShiftPressed = false
            updateShiftButtonState(shiftButton, theme)
        }
    }
    
    private fun updateShiftButtonState(button: android.widget.Button?, theme: Theme) {
        button?.let {
            when {
                isShiftLocked -> {
                    // Locked state: different color (e.g., green)
                    it.setBackgroundColor(Color.argb(180, 50, 150, 50))
                    it.text = "⇪"  // Caps Lock symbol
                }
                isShiftPressed -> {
                    // Pressed state: blue
                    it.setBackgroundColor(Color.argb(128, 100, 100, 255))
                    it.text = "⇧"
                }
                else -> {
                    // Normal state
                    it.setBackgroundColor(Color.TRANSPARENT)
                    it.text = "⇧"
                }
            }
        }
    }
    
    @Suppress("UNUSED_PARAMETER")
    private fun updateModifierButtonState(button: android.widget.Button?, isActive: Boolean, theme: Theme) {
        button?.let {
            if (isActive) {
                it.setBackgroundColor(Color.argb(128, 100, 100, 255))
            } else {
                it.setBackgroundColor(Color.TRANSPARENT)
            }
        }
    }

    private fun isLetterKey(keyCode: Int): Boolean {
        return keyCode in KeyEvent.KEYCODE_A..KeyEvent.KEYCODE_Z
    }

    /**
     * Check if a key is a character key that can be sent as text input.
     * This includes letters, numbers, and printable symbols.
     * Note: Space and Enter cannot be sent via "input text", so they use key events instead.
     */
    private fun isCharacterKey(keyCode: Int): Boolean {
        return when (keyCode) {
            // Numbers
            KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_3,
            KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_7,
            KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_9,
            // Letters
            KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_B, KeyEvent.KEYCODE_C, KeyEvent.KEYCODE_D,
            KeyEvent.KEYCODE_E, KeyEvent.KEYCODE_F, KeyEvent.KEYCODE_G, KeyEvent.KEYCODE_H,
            KeyEvent.KEYCODE_I, KeyEvent.KEYCODE_J, KeyEvent.KEYCODE_K, KeyEvent.KEYCODE_L,
            KeyEvent.KEYCODE_M, KeyEvent.KEYCODE_N, KeyEvent.KEYCODE_O, KeyEvent.KEYCODE_P,
            KeyEvent.KEYCODE_Q, KeyEvent.KEYCODE_R, KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_T,
            KeyEvent.KEYCODE_U, KeyEvent.KEYCODE_V, KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_X,
            KeyEvent.KEYCODE_Y, KeyEvent.KEYCODE_Z,
            // Punctuation and symbols (for the symbol row)
            KeyEvent.KEYCODE_COMMA, KeyEvent.KEYCODE_PERIOD, KeyEvent.KEYCODE_MINUS,
            KeyEvent.KEYCODE_EQUALS, KeyEvent.KEYCODE_LEFT_BRACKET, KeyEvent.KEYCODE_RIGHT_BRACKET,
            KeyEvent.KEYCODE_BACKSLASH, KeyEvent.KEYCODE_SEMICOLON, KeyEvent.KEYCODE_APOSTROPHE,
            KeyEvent.KEYCODE_SLASH, KeyEvent.KEYCODE_GRAVE
            -> true
            else -> false
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
    
    private fun startKeyRepeatWithText(text: String) {
        stopKeyRepeat()
        repeatRunnable = object : Runnable {
            override fun run() {
                Timber.d("Text repeat: $text")
                onTextInputCallback?.invoke(text)
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
        
        // Remove minimized view if showing
        minimizedView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                Timber.e(e, "Failed to remove minimized view")
            }
        }
        minimizedView = null
        
        // Remove full keyboard if showing
        floatingContainer?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                Timber.e(e, "Failed to hide floating keyboard")
            }
        }
        floatingContainer = null
        keyboardContainer = null
        onKeyEventCallback = null
        isMinimized = false
        Timber.d("Floating keyboard hidden")
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
