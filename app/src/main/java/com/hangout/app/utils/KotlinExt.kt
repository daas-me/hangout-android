package com.hangout.app.utils

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat

// ── Toast ──────────────────────────────────────────────────────────────────────

fun Activity.toast(message: String, type: CustomToast.Type = CustomToast.Type.DEFAULT) =
    CustomToast.show(this, message, type)

fun Fragment.toast(message: String, type: CustomToast.Type = CustomToast.Type.DEFAULT) =
    CustomToast.show(requireContext(), message, type)

fun Context.toast(message: String, type: CustomToast.Type = CustomToast.Type.DEFAULT) =
    CustomToast.show(this, message, type)

// ── EditText ───────────────────────────────────────────────────────────────────

fun EditText.getValue(): String = text.toString().trim()

fun EditText.isEmpty(): Boolean = text.toString().isBlank()

// ── View Visibility ────────────────────────────────────────────────────────────

fun View.show() { visibility = View.VISIBLE }

fun View.hide() { visibility = View.GONE }

fun View.invisible() { visibility = View.INVISIBLE }

fun View.showIf(condition: Boolean) {
    visibility = if (condition) View.VISIBLE else View.GONE
}

// ── Color ──────────────────────────────────────────────────────────────────────

fun Context.color(res: Int) = ContextCompat.getColor(this, res)

fun Fragment.color(res: Int) = ContextCompat.getColor(requireContext(), res)

// ── Time Formatting ────────────────────────────────────────────────────────────

/**
 * Converts 24-hour time format (HH:mm) to 12-hour format (h:mm a)
 * Example: "14:30" -> "2:30 PM", "09:15" -> "9:15 AM"
 */
fun formatTime12Hr(time24: String?): String {
    if (time24.isNullOrBlank()) return ""
    return try {
        val parts = time24.split(":")
        val h = parts[0].toInt()
        val m = parts.getOrNull(1)?.toInt() ?: 0
        val ampm = if (h >= 12) "PM" else "AM"
        val display = when {
            h == 0  -> 12
            h > 12  -> h - 12
            else    -> h
        }
        String.format("%d:%02d %s", display, m, ampm)
    } catch (_: Exception) {
        time24
    }
}