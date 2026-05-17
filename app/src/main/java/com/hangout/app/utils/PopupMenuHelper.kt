package com.hangout.app.utils

import android.content.Context
import android.widget.PopupMenu
import androidx.appcompat.view.ContextThemeWrapper
import com.hangout.app.R

/**
 * Helper function to create a styled PopupMenu for HangOut
 * Ensures dark background with readable light text
 */
fun createStyledPopupMenu(context: Context, anchor: android.view.View): PopupMenu {
    val themedContext = ContextThemeWrapper(context, R.style.HangOutPopupTheme)
    return PopupMenu(themedContext, anchor)
}
