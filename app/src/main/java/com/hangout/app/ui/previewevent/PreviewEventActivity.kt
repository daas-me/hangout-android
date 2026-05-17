package com.hangout.app.ui.previewevent

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.hangout.app.R
import com.hangout.app.data.CreateEventFormState
import com.hangout.app.databinding.ActivityPreviewEventBinding
import com.hangout.app.utils.hide
import com.hangout.app.utils.show
import java.text.SimpleDateFormat
import java.util.*

class PreviewEventActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPreviewEventBinding
    private lateinit var formState: CreateEventFormState
    private var coverImagePath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreviewEventBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retrieve data from holder instead of Intent to avoid TransactionTooLargeException
        formState = com.hangout.app.utils.CreateEventHolder.formState ?: CreateEventFormState()
        coverImagePath = com.hangout.app.utils.CreateEventHolder.coverImagePath ?: formState.coverImagePath

        setupToolbar()
        populatePreview()
        setupButtons()
    }

    private fun setupToolbar() {
        // Back button is set up in setupButtons()
    }

    private fun populatePreview() {
        // Cover photo - use local path if available, otherwise use remote imageUrl
        val imageSource = coverImagePath ?: formState.imageUrl
        if (imageSource != null) {
            Glide.with(this)
                .load(imageSource)
                .centerCrop()
                .into(binding.ivCoverPhoto)
            binding.ivCoverPhoto.show()
        } else {
            binding.ivCoverPhoto.hide()
        }

        // Title
        binding.tvEventTitle.text = formState.title.ifBlank { "Untitled Event" }

        // Format badge
        binding.tvFormat.text = formState.format.ifBlank { "In-Person" }

        // Date - format as "May 12, 2026"
        binding.tvDateStatus.text = if (formState.date.isNotBlank()) formatDateToText(formState.date) else "Not set"

        // Time - format as 12HR range (startTime - endTime)
        val timeText = buildString {
            if (formState.startTime.isNotBlank()) {
                append(format24To12(formState.startTime))
            } else {
                append("—")
            }
            if (formState.endTime.isNotBlank()) {
                append(" - ")
                append(format24To12(formState.endTime))
            }
        }
        binding.tvTimeStatus.text = if (timeText.isNotBlank()) timeText else "—"

        // Location
        binding.tvLocation.text = if (formState.location.isNotBlank()) {
            formState.location
        } else {
            "—"
        }

        // Stats: Price, Attending, Format, Capacity
        val priceText = when {
            formState.eventType == "free" -> "Free"
            formState.price.isNotBlank() -> "₱${formState.price}"
            else -> "—"
        }
        binding.tvStatPrice.text = priceText

        binding.tvStatAttending.text = "0"  // For new events being created, attending is always 0
        
        binding.tvStatFormat.text = formState.format.ifBlank { "—" }
        
        binding.tvStatCapacity.text = if (formState.capacity.isNotBlank()) {
            formState.capacity
        } else {
            "—"
        }

        // Seating Type
        binding.tvSeatingType.text = when (formState.seatingType) {
            "reserved" -> "Assigned Seats"
            "open" -> "Open Seating"
            else -> "—"
        }

        // Description / About - always show the section
        binding.tvAboutDescription.text = if (formState.description.isNotBlank()) {
            formState.description
        } else {
            "No description provided."
        }

        // Host Information - always show with placeholder if empty
        val fullName = "${formState.hostFirstName} ${formState.hostLastName}".trim()
        binding.tvHostName.text = fullName.ifBlank { "—" }
        binding.tvHostEmail.text = formState.hostEmail.ifBlank { "—" }
        
        // Host initials
        val initials = (formState.hostFirstName.firstOrNull()?.uppercase() ?: "") +
                      (formState.hostLastName.firstOrNull()?.uppercase() ?: "")
        binding.tvHostInitials.text = if (initials.isNotBlank()) initials else "—"

        // Host profile photo - load if available
        if (!formState.hostPhoto.isNullOrBlank()) {
            Glide.with(this)
                .load(formState.hostPhoto)
                .centerCrop()
                .into(binding.ivHostProfilePhoto)
            binding.ivHostProfilePhoto.show()
        } else {
            binding.ivHostProfilePhoto.hide()
        }

        // Update Message Host button text with host name
        val hostDisplayName = formState.hostFirstName.ifBlank { "Host" }
        binding.btnMessageHost.text = "Message $hostDisplayName"
    }

    // Convert date from "YYYY-MM-DD" to "May 12, 2026"
    private fun formatDateToText(dateStr: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val outputFormat = SimpleDateFormat("MMMM d, yyyy", Locale.US)
            val date = inputFormat.parse(dateStr) ?: return dateStr
            outputFormat.format(date)
        } catch (e: Exception) {
            dateStr
        }
    }

    // Convert 24HR format (HH:mm) to 12HR format (h:mm AM/PM)
    private fun format24To12(time24: String): String {
        return try {
            val parts = time24.split(":")
            if (parts.size < 2) return time24
            
            val hour = parts[0].toIntOrNull() ?: return time24
            val minute = parts[1].toIntOrNull() ?: 0
            
            val isPm = hour >= 12
            val hour12 = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            
            val amPm = if (isPm) "PM" else "AM"
            String.format("%d:%02d %s", hour12, minute, amPm)
        } catch (e: Exception) {
            time24
        }
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener {
            com.hangout.app.utils.CreateEventHolder.clear()
            setResult(Activity.RESULT_CANCELED)
            finish()
        }

        binding.btnSaveDraft.setOnClickListener {
            com.hangout.app.utils.CreateEventHolder.clear()
            setResult(RESULT_OK, Intent().apply {
                putExtra("action", "saveDraft")
            })
            finish()
        }

        binding.btnPublish.setOnClickListener {
            com.hangout.app.utils.CreateEventHolder.clear()
            setResult(RESULT_OK, Intent().apply {
                putExtra("action", "publish")
            })
            finish()
        }
    }

    companion object {
        const val REQUEST_CODE_PREVIEW = 1001
    }
}
