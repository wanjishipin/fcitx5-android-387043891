/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * SPDX-FileCopyrightText: Copyright 2021-2025 Fcitx5 for Android Contributors
 */
package org.fcitx.fcitx5.android.input.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * A minimal-height view that appears when the keyboard is minimized.
 * Shows a small pill-shaped button that can be tapped to restore the keyboard.
 * The view height is minimal to avoid blocking screen content.
 */
class FloatingButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    interface OnRestoreListener {
        fun onRestore()
    }

    companion object {
        private const val BUTTON_HEIGHT_DP = 32
        private const val BUTTON_WIDTH_DP = 64
        private const val PADDING_DP = 4
    }

    private var restoreListener: OnRestoreListener? = null
    private val bgPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val iconPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val density: Float = context.resources.displayMetrics.density
    private val buttonWidth: Int = (BUTTON_WIDTH_DP * density).toInt()
    private val buttonHeight: Int = (BUTTON_HEIGHT_DP * density).toInt()
    private val totalHeight: Int = ((BUTTON_HEIGHT_DP + PADDING_DP * 2) * density).toInt()

    init {
        iconPaint.strokeWidth = 2 * density
    }

    fun setOnRestoreListener(listener: OnRestoreListener) {
        restoreListener = listener
    }

    fun setColors(bgColor: Int, iconColor: Int) {
        bgPaint.color = bgColor
        iconPaint.color = iconColor
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = context.resources.displayMetrics.widthPixels
        // Only take up minimal height - just enough for the button
        setMeasuredDimension(width, totalHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val viewWidth = width
        val viewHeight = height

        // Center the button horizontally
        val buttonLeft = (viewWidth - buttonWidth) / 2f
        val buttonTop = (viewHeight - buttonHeight) / 2f
        val buttonRight = buttonLeft + buttonWidth
        val buttonBottom = buttonTop + buttonHeight
        val cornerRadius = buttonHeight / 2f

        // Draw pill-shaped button background
        canvas.drawRoundRect(
            buttonLeft, buttonTop, buttonRight, buttonBottom,
            cornerRadius, cornerRadius, bgPaint
        )

        // Draw keyboard icon (three horizontal lines)
        val centerX = viewWidth / 2f
        val centerY = viewHeight / 2f
        val iconWidth = buttonWidth * 0.35f
        val lineSpacing = buttonHeight * 0.18f

        // Top line
        canvas.drawLine(
            centerX - iconWidth, centerY - lineSpacing,
            centerX + iconWidth, centerY - lineSpacing, iconPaint
        )
        // Middle line
        canvas.drawLine(
            centerX - iconWidth, centerY,
            centerX + iconWidth, centerY, iconPaint
        )
        // Bottom line
        canvas.drawLine(
            centerX - iconWidth, centerY + lineSpacing,
            centerX + iconWidth, centerY + lineSpacing, iconPaint
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> true
            MotionEvent.ACTION_UP -> {
                // Tap detected - restore keyboard
                restoreListener?.onRestore()
                true
            }
            else -> super.onTouchEvent(event)
        }
    }
}
