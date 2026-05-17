package com.hangout.app.ui.components

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.hangout.app.R

/**
 * Base DialogFragment for HangOut custom dialogs.
 * Ensures consistent theming:
 * - Dark background matching the app theme
 * - Purple accents for text and buttons
 * - Readable text on all backgrounds
 * - Rounded corners
 */
open class HangOutDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            // Set dark background
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            // Dark themed dialog
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            attributes = attributes?.apply {
                width = (resources.displayMetrics.widthPixels * 0.9).toInt()
            }
        }
    }

    /**
     * Helper to create a standard HangOut AlertDialog with proper theming
     */
    protected fun createHangOutAlertDialog(): AlertDialog.Builder {
        return AlertDialog.Builder(requireContext(), R.style.HangOutAlertDialogTheme)
    }
}

/**
 * Custom AlertDialog theme builder for HangOut
 * Provides consistent styling across all alert dialogs
 */
fun createStyledAlertDialog(
    context: android.content.Context,
    title: String? = null,
    message: String? = null,
    positiveButtonText: String? = null,
    positiveButtonListener: ((android.content.DialogInterface, Int) -> Unit)? = null,
    negativeButtonText: String? = null,
    negativeButtonListener: ((android.content.DialogInterface, Int) -> Unit)? = null,
    cancelable: Boolean = true
): AlertDialog {
    val builder = AlertDialog.Builder(context, R.style.HangOutAlertDialogTheme)
    
    title?.let { builder.setTitle(it) }
    message?.let { builder.setMessage(it) }
    
    positiveButtonText?.let {
        builder.setPositiveButton(it, positiveButtonListener)
    }
    
    negativeButtonText?.let {
        builder.setNegativeButton(it, negativeButtonListener)
    }
    
    builder.setCancelable(cancelable)
    
    val dialog = builder.create()
    
    // Override text colors to ensure readability
    dialog.setOnShowListener {
        val titleView = dialog.findViewById<android.widget.TextView>(android.R.id.title)
        titleView?.setTextColor(context.resources.getColor(R.color.text_primary, null))
        titleView?.textSize = 18f
        
        val messageView = dialog.findViewById<android.widget.TextView>(android.R.id.message)
        messageView?.setTextColor(context.resources.getColor(R.color.text_primary, null))
        messageView?.textSize = 14f
        
        // Style buttons
        val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        positiveButton?.setTextColor(context.resources.getColor(R.color.purple_light, null))
        
        val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
        negativeButton?.setTextColor(context.resources.getColor(R.color.text_muted, null))
        
        val neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
        neutralButton?.setTextColor(context.resources.getColor(R.color.text_muted, null))
    }
    
    return dialog
}

/**
 * Helper to create a styled EditText for use in dialogs
 * Ensures text is readable on dark backgrounds
 */
fun createStyledDialogEditText(
    context: android.content.Context,
    hint: String? = null,
    inputType: Int = android.text.InputType.TYPE_CLASS_TEXT
): android.widget.EditText {
    return android.widget.EditText(context).apply {
        this.hint = hint
        this.inputType = inputType
        setPadding(48, 24, 48, 24)
        // Set text color for readability
        setTextColor(context.resources.getColor(R.color.text_primary, null))
        setHintTextColor(context.resources.getColor(R.color.text_muted, null))
        // Dark background
        setBackgroundColor(context.resources.getColor(R.color.input_bg, null))
    }
}

/**
 * Helper to create styled EditText fields for layout dialogs
 * Used when EditText is added to a layout (not directly to dialog)
 */
fun styleDialogEditText(
    editText: android.widget.EditText,
    context: android.content.Context
): android.widget.EditText {
    editText.apply {
        setTextColor(context.resources.getColor(R.color.text_primary, null))
        setHintTextColor(context.resources.getColor(R.color.text_muted, null))
        setBackgroundColor(context.resources.getColor(R.color.input_bg, null))
    }
    return editText
}
