package com.example.unigearmanager

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton

fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

fun Context.screenRoot(): LinearLayout {
    return LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundResource(R.drawable.bg_scrapbook)
        setPadding(dp(20), dp(20), dp(20), dp(20))
    }
}

fun Context.poster(): LinearLayout {
    return LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundResource(R.drawable.bg_poster)
        setPadding(dp(18), dp(18), dp(18), dp(18))
    }
}

fun Context.titleTab(title: String): TextView {
    return TextView(this).apply {
        text = title
        textSize = 25f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(getColor(R.color.ink))
        gravity = android.view.Gravity.CENTER
        setBackgroundResource(R.drawable.bg_title_tab)
        setPadding(dp(14), dp(10), dp(14), dp(10))
    }
}

fun Context.card(): LinearLayout {
    return LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundResource(R.drawable.bg_feature_panel)
        setPadding(dp(14), dp(14), dp(14), dp(14))
    }
}

fun Context.label(textValue: String, size: Float = 14f, bold: Boolean = false): TextView {
    return TextView(this).apply {
        text = textValue
        textSize = size
        setTextColor(getColor(R.color.ink))
        if (bold) typeface = Typeface.DEFAULT_BOLD
        setPadding(0, dp(3), 0, dp(3))
    }
}

fun Context.chip(textValue: String, available: Boolean = true): TextView {
    return TextView(this).apply {
        text = textValue
        textSize = 12f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(getColor(R.color.ink))
        setBackgroundResource(if (available) R.drawable.bg_chip else R.drawable.bg_feature_accent_red)
        setPadding(dp(12), dp(7), dp(12), dp(7))
    }
}

fun Context.primaryButton(textValue: String, red: Boolean = true): MaterialButton {
    return MaterialButton(this).apply {
        text = textValue
        textSize = 14f
        isAllCaps = false
        typeface = Typeface.DEFAULT_BOLD
        minHeight = dp(48)
        minWidth = 0
        insetTop = 0
        insetBottom = 0
        setPadding(dp(14), 0, dp(14), 0)
        setTextColor(getColor(if (red) R.color.white else R.color.ink))
        backgroundTintList = android.content.res.ColorStateList.valueOf(getColor(if (red) R.color.maroon else R.color.yellow))
        strokeColor = android.content.res.ColorStateList.valueOf(getColor(R.color.line))
        strokeWidth = dp(2)
        cornerRadius = dp(14)
    }
}

fun View.withMargins(left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0): View {
    val density = resources.displayMetrics.density
    layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply {
        setMargins((left * density).toInt(), (top * density).toInt(), (right * density).toInt(), (bottom * density).toInt())
    }
    return this
}

