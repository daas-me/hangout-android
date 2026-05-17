package com.hangout.app.utils

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.core.graphics.drawable.DrawableCompat
import com.hangout.app.R

/**
 * Custom Toast utility for displaying styled toasts throughout the application.
 * Provides different toast types: default, success, error, warning, info
 */
object CustomToast {

    enum class Type {
        DEFAULT,    // Neutral gray background
        SUCCESS,    // Green check icon
        ERROR,      // Red error icon
        WARNING,    // Orange warning icon
        INFO        // Blue info icon
    }

    fun show(
        context: Context,
        message: String,
        type: Type = Type.DEFAULT,
        duration: Int = Toast.LENGTH_SHORT
    ) {
        val toast = Toast(context)
        toast.duration = duration

        // Inflate custom layout
        val layout = android.view.LayoutInflater.from(context)
            .inflate(R.layout.layout_custom_toast, null)

        // Set message
        val tvMessage = layout.findViewById<TextView>(R.id.tvMessage)
        tvMessage.text = message

        // Set icon based on type
        val ivIcon = layout.findViewById<ImageView>(R.id.ivIcon)
        val (iconRes, bgColor) = when (type) {
            Type.SUCCESS -> R.drawable.ic_check_circle to context.getColor(R.color.success_green)
            Type.ERROR -> R.drawable.ic_error_circle to context.getColor(R.color.red_accent)
            Type.WARNING -> R.drawable.ic_warning_circle to context.getColor(R.color.yellow_accent)
            Type.INFO -> R.drawable.ic_info_circle to context.getColor(R.color.purple_light)
            Type.DEFAULT -> R.drawable.ic_info_circle to 0xFF333333.toInt()
        }

        ivIcon.setImageResource(iconRes)
        ivIcon.setColorFilter(context.getColor(android.R.color.white))
        
        // Set rounded background with color
        val background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(bgColor)
            cornerRadius = context.resources.displayMetrics.density * 12
        }
        layout.background = background

        toast.view = layout
        toast.show()
    }
}
