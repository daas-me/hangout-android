package com.hangout.app.ui.components

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.hangout.app.R

/**
 * Custom spinner adapter for HangOut dropdowns.
 * Ensures readable text on dark backgrounds.
 */
class HangOutSpinnerAdapter<T>(
    context: Context,
    private val items: List<T>,
    private val itemFormatter: (T) -> String = { it.toString() }
) : ArrayAdapter<T>(context, R.layout.spinner_item, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getCustomView(position, convertView, parent, isDropdown = false)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getCustomView(position, convertView, parent, isDropdown = true)
    }

    private fun getCustomView(
        position: Int,
        convertView: View?,
        parent: ViewGroup,
        isDropdown: Boolean
    ): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(
            if (isDropdown) R.layout.spinner_dropdown_item else R.layout.spinner_item,
            parent,
            false
        )

        val textView = when {
            view is TextView -> view
            else -> view.findViewById(android.R.id.text1) as? TextView
                ?: view.findViewById<TextView>(android.R.id.text1)
                ?: (view as? ViewGroup)?.let { vg ->
                    var found: TextView? = null
                    for (i in 0 until vg.childCount) {
                        val child = vg.getChildAt(i)
                        if (child is TextView) {
                            found = child
                            break
                        }
                    }
                    found
                }
        }

        textView?.apply {
            if (position >= 0 && position < items.size) {
                text = itemFormatter(items[position])
            }
            // Ensure readable text
            setTextColor(
                ContextCompat.getColor(
                    context,
                    if (isDropdown) R.color.text_primary else R.color.text_primary
                )
            )
            textSize = 14f
            setBackgroundColor(Color.TRANSPARENT)
        }

        return view
    }
}
