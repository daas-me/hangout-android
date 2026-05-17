package com.hangout.app.ui.components

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.hangout.app.R

class ProfileInformationDialogFragment : DialogFragment() {

    private var name: String? = null
    private var email: String? = null
    private var photo: String? = null
    private var age: String? = null
    private var gender: String? = null
    private var phone: String? = null
    private var location: String? = null
    private var about: String? = null
    private var isHost: Boolean = false

    companion object {
        private const val ARG_NAME = "name"
        private const val ARG_EMAIL = "email"
        private const val ARG_PHOTO = "photo"
        private const val ARG_AGE = "age"
        private const val ARG_GENDER = "gender"
        private const val ARG_PHONE = "phone"
        private const val ARG_LOCATION = "location"
        private const val ARG_ABOUT = "about"
        private const val ARG_IS_HOST = "isHost"

        fun newInstance(
            name: String? = null,
            email: String? = null,
            photo: String? = null,
            age: String? = null,
            gender: String? = null,
            phone: String? = null,
            location: String? = null,
            about: String? = null,
            isHost: Boolean = false
        ) = ProfileInformationDialogFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_NAME, name)
                putString(ARG_EMAIL, email)
                putString(ARG_PHOTO, photo)
                putString(ARG_AGE, age)
                putString(ARG_GENDER, gender)
                putString(ARG_PHONE, phone)
                putString(ARG_LOCATION, location)
                putString(ARG_ABOUT, about)
                putBoolean(ARG_IS_HOST, isHost)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            name = it.getString(ARG_NAME)
            email = it.getString(ARG_EMAIL)
            photo = it.getString(ARG_PHOTO)
            age = it.getString(ARG_AGE)
            gender = it.getString(ARG_GENDER)
            phone = it.getString(ARG_PHONE)
            location = it.getString(ARG_LOCATION)
            about = it.getString(ARG_ABOUT)
            isHost = it.getBoolean(ARG_IS_HOST, false)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        val ctx = requireContext()
        val contentView = LinearLayout(ctx).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF2A2A2A.toInt())
            setPadding(
                20.dpToPx(),
                20.dpToPx(),
                20.dpToPx(),
                20.dpToPx()
            )

            // Header with avatar and name
            val headerLayout = LinearLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 20.dpToPx() }
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
            }

            // Avatar
            val avatarFrameLayout = android.widget.FrameLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    56.dpToPx(),
                    56.dpToPx()
                ).apply { marginEnd = 16.dpToPx() }
                clipChildren = false
            }

            val avatarImageView = ImageView(requireContext()).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    56.dpToPx(),
                    56.dpToPx()
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
                clipToOutline = true
                setImageResource(R.drawable.ic_user)

                if (!photo.isNullOrBlank()) {
                    Glide.with(this@ProfileInformationDialogFragment)
                        .load(photo)
                        .centerCrop()
                        .circleCrop()
                        .into(this)
                }
            }
            avatarFrameLayout.addView(avatarImageView)
            headerLayout.addView(avatarFrameLayout)

            // Name and email
            val nameEmailLayout = LinearLayout(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
                orientation = LinearLayout.VERTICAL
            }

            val nameTextView = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                text = name ?: "User"
                textSize = 16f
                typeface = Typeface.defaultFromStyle(Typeface.BOLD)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            }
            nameEmailLayout.addView(nameTextView)

            if (!email.isNullOrBlank()) {
                val emailTextView = TextView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = 4.dpToPx() }
                    text = email
                    textSize = 13f
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
                }
                nameEmailLayout.addView(emailTextView)
            }

            headerLayout.addView(nameEmailLayout)
            addView(headerLayout)

            // Divider
            val divider1 = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1.dpToPx()
                ).apply { bottomMargin = 16.dpToPx() }
                setBackgroundColor(0x1AFFFFFF)
            }
            addView(divider1)

            // Personal Information Section
            val hasAge      = !age.isNullOrBlank()      && age      != "-"
            val hasGender   = !gender.isNullOrBlank()   && gender   != "-"
            val hasPhone    = !phone.isNullOrBlank()    && phone    != "-"
            val hasLocation = !location.isNullOrBlank() && location != "-"

            if (hasAge || hasGender || hasPhone || hasLocation) {
                val personalInfoLabel = TextView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = 12.dpToPx() }
                    text = "PERSONAL INFORMATION"
                    textSize = 12f
                    typeface = Typeface.defaultFromStyle(Typeface.BOLD)
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.purple_light))
                }
                addView(personalInfoLabel)

                if (hasAge)      addInfoRow("Age",      age!!)
                if (hasGender)   addInfoRow("Gender",   gender!!)
                if (hasPhone)    addInfoRow("Phone",    phone!!)
                if (hasLocation) addInfoRow("Location", location!!)
            } else {
                addView(TextView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = 8.dpToPx() }
                    text = "No additional information provided."
                    textSize = 13f
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
                })
            }

            // About/Bio
            if (!about.isNullOrBlank() && about != "-") {
                val divider2 = View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        1.dpToPx()
                    ).apply {
                        topMargin = 12.dpToPx()
                        bottomMargin = 12.dpToPx()
                    }
                    setBackgroundColor(0x1AFFFFFF)
                }
                addView(divider2)

                val aboutLabel = TextView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = 8.dpToPx() }
                    text = "ABOUT"
                    textSize = 12f
                    typeface = Typeface.defaultFromStyle(Typeface.BOLD)
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.purple_light))
                }
                addView(aboutLabel)

                val aboutTextView = TextView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    text = about
                    textSize = 13f
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                    setLineSpacing(4f, 1.2f)
                }
                addView(aboutTextView)
            }
        }
        
        return AlertDialog.Builder(ctx)
            .setView(contentView)
            .setCancelable(true)
            .create()
    }

    private fun LinearLayout.addInfoRow(label: String, value: String) {
        val row = LinearLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12.dpToPx() }
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val labelView = TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            text = label
            textSize = 12f
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_muted))
        }
        row.addView(labelView)

        val valueView = TextView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = value
            textSize = 13f
            typeface = Typeface.defaultFromStyle(Typeface.BOLD)
            setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            maxLines = 2
        }
        row.addView(valueView)

        addView(row)
    }

    override fun onStart() {
        super.onStart()
        dialog?.apply {
            window?.apply {
                setBackgroundDrawable(android.graphics.drawable.ColorDrawable(0x1A1A1A))
                attributes = attributes?.apply {
                    width = (resources.displayMetrics.widthPixels * 0.88).toInt()
                }
            }
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}
