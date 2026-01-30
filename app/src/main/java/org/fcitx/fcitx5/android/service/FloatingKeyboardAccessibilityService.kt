/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2025 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.service

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import timber.log.Timber
import java.io.DataOutputStream

/**
 * Accessibility service for floating keyboard functionality.
 * Allows sending key events to the currently focused application
 * even when the input method has no focus.
 */
class FloatingKeyboardAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        private var instance: FloatingKeyboardAccessibilityService? = null

        fun getInstance(): FloatingKeyboardAccessibilityService? = instance

        fun isEnabled(): Boolean = instance != null
        
        // Map keyCode to character for letters
        private val keyCodeToChar = mapOf(
            KeyEvent.KEYCODE_A to 'a',
            KeyEvent.KEYCODE_B to 'b',
            KeyEvent.KEYCODE_C to 'c',
            KeyEvent.KEYCODE_D to 'd',
            KeyEvent.KEYCODE_E to 'e',
            KeyEvent.KEYCODE_F to 'f',
            KeyEvent.KEYCODE_G to 'g',
            KeyEvent.KEYCODE_H to 'h',
            KeyEvent.KEYCODE_I to 'i',
            KeyEvent.KEYCODE_J to 'j',
            KeyEvent.KEYCODE_K to 'k',
            KeyEvent.KEYCODE_L to 'l',
            KeyEvent.KEYCODE_M to 'm',
            KeyEvent.KEYCODE_N to 'n',
            KeyEvent.KEYCODE_O to 'o',
            KeyEvent.KEYCODE_P to 'p',
            KeyEvent.KEYCODE_Q to 'q',
            KeyEvent.KEYCODE_R to 'r',
            KeyEvent.KEYCODE_S to 's',
            KeyEvent.KEYCODE_T to 't',
            KeyEvent.KEYCODE_U to 'u',
            KeyEvent.KEYCODE_V to 'v',
            KeyEvent.KEYCODE_W to 'w',
            KeyEvent.KEYCODE_X to 'x',
            KeyEvent.KEYCODE_Y to 'y',
            KeyEvent.KEYCODE_Z to 'z',
            KeyEvent.KEYCODE_SPACE to ' ',
            KeyEvent.KEYCODE_0 to '0',
            KeyEvent.KEYCODE_1 to '1',
            KeyEvent.KEYCODE_2 to '2',
            KeyEvent.KEYCODE_3 to '3',
            KeyEvent.KEYCODE_4 to '4',
            KeyEvent.KEYCODE_5 to '5',
            KeyEvent.KEYCODE_6 to '6',
            KeyEvent.KEYCODE_7 to '7',
            KeyEvent.KEYCODE_8 to '8',
            KeyEvent.KEYCODE_9 to '9',
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Timber.d("FloatingKeyboardAccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We don't need to handle accessibility events for this service
    }

    override fun onInterrupt() {
        Timber.d("FloatingKeyboardAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Timber.d("FloatingKeyboardAccessibilityService destroyed")
    }

    /**
     * Send a key event to the currently focused application
     */
    fun sendKeyEvent(keyCode: Int, metaState: Int = 0): Boolean {
        Timber.w("sendKeyEvent: keyCode=$keyCode, metaState=$metaState")
        
        // Try Shizuku first (has shell privileges)
        val shizukuRunning = ShizukuShellManager.isShizukuRunning()
        val shizukuAvailable = ShizukuShellManager.isAvailable()
        val hasPermission = ShizukuShellManager.hasPermission()
        Timber.w("Shizuku status: running=$shizukuRunning, available=$shizukuAvailable, hasPermission=$hasPermission")
        
        if (shizukuAvailable) {
            val shizukuResult = sendKeyEventViaShizuku(keyCode, metaState)
            if (shizukuResult) {
                Timber.w("Key event sent via Shizuku successfully")
                return true
            }
            Timber.w("Shizuku sendKeyEvent failed")
        } else if (shizukuRunning && !hasPermission) {
            Timber.w("Shizuku running but no permission, requesting...")
            ShizukuShellManager.requestPermission()
        } else if (!shizukuRunning) {
            Timber.w("Shizuku is not running")
        }
        
        // Fallback to accessibility-based methods
        try {
            // Handle global actions
            when (keyCode) {
                KeyEvent.KEYCODE_BACK -> {
                    return performGlobalAction(GLOBAL_ACTION_BACK)
                }
                KeyEvent.KEYCODE_HOME -> {
                    return performGlobalAction(GLOBAL_ACTION_HOME)
                }
                KeyEvent.KEYCODE_APP_SWITCH -> {
                    return performGlobalAction(GLOBAL_ACTION_RECENTS)
                }
            }
            
            // Handle DPAD keys (TV remote style navigation)
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN,
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_DPAD_CENTER -> {
                    return handleDpadKey(keyCode)
                }
                
                KeyEvent.KEYCODE_TAB -> {
                    return handleTabKey(metaState)
                }
                
                KeyEvent.KEYCODE_ENTER -> {
                    return handleEnterKey()
                }
                
                KeyEvent.KEYCODE_ESCAPE -> {
                    return performGlobalAction(GLOBAL_ACTION_BACK)
                }
                
                KeyEvent.KEYCODE_MENU -> {
                    return handleMenuKey()
                }
            }
            
            // For text input keys, find editable node
            val focusedNode = findFocusedEditableNode()
            if (focusedNode != null) {
                val result = handleTextInputKey(focusedNode, keyCode, metaState)
                focusedNode.recycle()
                return result
            }
            
            Timber.w("No editable node found for text input, shell command also failed")
            return false
            
        } catch (e: Exception) {
            Timber.e(e, "Error sending key event")
            return false
        }
    }
    
    /**
     * Send key event using Shizuku shell service
     * This has shell privileges and can send key events to any app
     */
    private fun sendKeyEventViaShizuku(keyCode: Int, metaState: Int): Boolean {
        return try {
            val command = buildKeyEventCommand(keyCode, metaState)
            Timber.d("Executing via Shizuku: $command")
            val result = ShizukuShellManager.exec(command)
            Timber.d("Shizuku command result: $result")
            result == 0
        } catch (e: Exception) {
            Timber.e(e, "Failed to send key event via Shizuku")
            false
        }
    }
    
    private fun buildKeyEventCommand(keyCode: Int, metaState: Int): String {
        return if (metaState != 0) {
            when {
                (metaState and KeyEvent.META_CTRL_ON) != 0 -> {
                    // Ctrl + key combination
                    "input keyevent --longpress $keyCode"
                }
                (metaState and KeyEvent.META_SHIFT_ON) != 0 -> {
                    "input keyevent $keyCode"
                }
                else -> "input keyevent $keyCode"
            }
        } else {
            "input keyevent $keyCode"
        }
    }
    
    /**
     * Handle DPAD navigation keys (TV remote style)
     */
    private fun handleDpadKey(keyCode: Int): Boolean {
        val direction = when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> View.FOCUS_UP
            KeyEvent.KEYCODE_DPAD_DOWN -> View.FOCUS_DOWN
            KeyEvent.KEYCODE_DPAD_LEFT -> View.FOCUS_LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT -> View.FOCUS_RIGHT
            KeyEvent.KEYCODE_DPAD_CENTER -> {
                // Center/OK button - click the focused element
                return clickFocusedNode()
            }
            else -> return false
        }
        
        // Find currently focused node
        val focusedNode = findFocusedNode()
        if (focusedNode == null) {
            Timber.w("No focused node for DPAD navigation")
            // Try to focus the first focusable element
            return focusFirstElement()
        }
        
        // Find next focusable in the direction
        val nextNode = focusedNode.focusSearch(direction)
        focusedNode.recycle()
        
        if (nextNode != null) {
            val result = nextNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            Timber.d("DPAD navigation to next node: $result")
            nextNode.recycle()
            return result
        }
        
        Timber.d("No next focusable node in direction $direction")
        return false
    }
    
    /**
     * Handle Tab key for focus navigation
     */
    private fun handleTabKey(metaState: Int): Boolean {
        val isShiftPressed = (metaState and KeyEvent.META_SHIFT_ON) != 0
        // Use LEFT/RIGHT for backward/forward navigation
        val direction = if (isShiftPressed) {
            View.FOCUS_LEFT
        } else {
            View.FOCUS_RIGHT
        }
        
        val focusedNode = findFocusedNode()
        if (focusedNode == null) {
            return focusFirstElement()
        }
        
        val nextNode = focusedNode.focusSearch(direction)
        focusedNode.recycle()
        
        if (nextNode != null) {
            val result = nextNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            Timber.d("Tab navigation result: $result")
            nextNode.recycle()
            return result
        }
        
        return false
    }
    
    /**
     * Handle Enter key - click the focused element or add newline in text
     */
    private fun handleEnterKey(): Boolean {
        // First check if we're in an editable field
        val editableNode = findFocusedEditableNode()
        if (editableNode != null) {
            // In text field, add newline
            val text = editableNode.text?.toString() ?: ""
            val newText = text + "\n"
            val arguments = Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText)
            val result = editableNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            editableNode.recycle()
            return result
        }
        
        // Otherwise, click the focused element
        return clickFocusedNode()
    }
    
    /**
     * Handle Menu key - try to open context menu or long-click
     */
    private fun handleMenuKey(): Boolean {
        val focusedNode = findFocusedNode()
        if (focusedNode != null) {
            // Try to long click to open context menu
            val result = focusedNode.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
            Timber.d("Menu key (long click) result: $result")
            focusedNode.recycle()
            if (result) return true
        }
        
        // Fallback: try to open recents
        return performGlobalAction(GLOBAL_ACTION_RECENTS)
    }
    
    /**
     * Click the currently focused node
     */
    private fun clickFocusedNode(): Boolean {
        val focusedNode = findFocusedNode()
        if (focusedNode != null) {
            val result = focusedNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Timber.d("Click focused node result: $result")
            focusedNode.recycle()
            return result
        }
        return false
    }
    
    /**
     * Focus the first focusable element on screen
     */
    private fun focusFirstElement(): Boolean {
        val windows = windows ?: return false
        
        for (window in windows) {
            val root = window.root ?: continue
            val focusable = findFirstFocusable(root)
            if (focusable != null) {
                val result = focusable.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                focusable.recycle()
                return result
            }
        }
        return false
    }
    
    private fun findFirstFocusable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isFocusable) {
            return AccessibilityNodeInfo.obtain(node)
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findFirstFocusable(child)
            child.recycle()
            if (result != null) return result
        }
        
        return null
    }
    
    /**
     * Find any focused node (input focus or accessibility focus)
     */
    private fun findFocusedNode(): AccessibilityNodeInfo? {
        val windows = windows
        if (windows.isNullOrEmpty()) {
            Timber.w("No accessible windows available")
            return null
        }

        for (window in windows) {
            try {
                val rootNode = window.root ?: continue
                
                // Try input focus first
                var focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                if (focusedNode != null) {
                    return focusedNode
                }
                
                // Then try accessibility focus
                focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY)
                if (focusedNode != null) {
                    return focusedNode
                }
            } catch (e: Exception) {
                Timber.w(e, "Error finding focused node in window")
            }
        }
        
        return null
    }
    
    private fun findFocusedEditableNode(): AccessibilityNodeInfo? {
        val windows = windows
        if (windows.isNullOrEmpty()) {
            Timber.w("No accessible windows available")
            return null
        }

        for (window in windows) {
            try {
                val rootNode = window.root ?: continue
                val focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                if (focusedNode != null && focusedNode.isEditable) {
                    return focusedNode
                }
                focusedNode?.recycle()
            } catch (e: Exception) {
                Timber.w(e, "Error finding focused node in window")
            }
        }
        
        return null
    }
    
    private fun handleTextInputKey(node: AccessibilityNodeInfo, keyCode: Int, metaState: Int): Boolean {
        val arguments = Bundle()
        
        when (keyCode) {
            KeyEvent.KEYCODE_DEL -> {
                val text = node.text?.toString() ?: ""
                if (text.isNotEmpty()) {
                    val newText = text.substring(0, text.length - 1)
                    arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText)
                    val result = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                    Timber.d("Backspace result: $result")
                    return result
                }
                return true
            }
            
            else -> {
                val char = keyCodeToChar[keyCode]
                if (char != null) {
                    val text = node.text?.toString() ?: ""
                    val charToAdd = if (metaState and KeyEvent.META_SHIFT_ON != 0) {
                        char.uppercaseChar()
                    } else {
                        char
                    }
                    val newText = text + charToAdd
                    arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText)
                    val result = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                    Timber.d("Character '$charToAdd' result: $result")
                    return result
                }
            }
        }
        
        return false
    }

    /**
     * Send text directly to the currently focused application
     */
    fun sendText(text: String): Boolean {
        Timber.d("sendText: $text")
        
        // Try via shell first
        try {
            val command = "input text '${text.replace("'", "\\'")}'"
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                Timber.d("Text sent via shell command")
                return true
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to send text via shell")
        }
        
        // Fallback to accessibility
        try {
            val focusedNode = findFocusedEditableNode() ?: return false
            
            val currentText = focusedNode.text?.toString() ?: ""
            val newText = currentText + text
            val arguments = Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText)
            val result = focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            focusedNode.recycle()
            
            Timber.d("sendText result: $result")
            return result
        } catch (e: Exception) {
            Timber.e(e, "Error sending text")
            return false
        }
    }
}
