package com.hangout.app.utils

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat

// ── Toast ──────────────────────────────────────────────────────────────────────

fun Activity.toast(message: String) =
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

fun Fragment.toast(message: String) =
    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

fun Context.toast(message: String) =
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

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