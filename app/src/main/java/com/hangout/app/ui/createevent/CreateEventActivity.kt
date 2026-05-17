package com.hangout.app.ui.createevent

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import com.bumptech.glide.Glide
import com.hangout.app.R
import com.hangout.app.data.CreateEventFormState
import com.hangout.app.databinding.ActivityCreateEventBinding
import com.hangout.app.ui.components.createStyledAlertDialog
import com.hangout.app.ui.components.createStyledDialogEditText
import com.hangout.app.utils.getRealPathFromUri
import com.hangout.app.utils.show
import com.hangout.app.utils.hide
import com.hangout.app.utils.toast
import java.util.Calendar

class CreateEventActivity : AppCompatActivity(), CreateEventContract.View {

    private lateinit var binding: ActivityCreateEventBinding
    private lateinit var presenter: CreateEventContract.Presenter

    private var currentStep = 1
    private val totalSteps = 4
    private var formState = CreateEventFormState()
    private var coverImagePath: String? = null

    // Edit mode properties
    private var isEditMode = false
    private var editEventId: Long? = null

    // ── Image picker ───────────────────────────────────────────────────────

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri -> handleImageSelected(uri) }
        }
    }

    // ── Preview launcher ───────────────────────────────────────────────────

    private val previewLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val action = result.data?.getStringExtra("action")
            when (action) {
                "saveDraft" -> handleSaveDraft()
                "publish" -> handlePublish()
            }
        }
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateEventBinding.inflate(layoutInflater)
        presenter = CreateEventPresenter(this, CreateEventModel(this), this)
        setContentView(binding.root)

        // Check for edit mode
        isEditMode = intent.getBooleanExtra("isEdit", false)
        editEventId = intent.getLongExtra("eventId", -1).takeIf { it != -1L }

        setupToolbar()
        setupStepNavigation()

        if (isEditMode && editEventId != null) {
            // Load event data for editing
            presenter.loadEventForEdit(editEventId!!)
        } else {
            showStep(1)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.detachView()
    }

    // ── Toolbar ────────────────────────────────────────────────────────────

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            if (currentStep > 1) goBack()
            else finish()
        }
    }

    // ── Step navigation ────────────────────────────────────────────────────

    private fun setupStepNavigation() {
        binding.btnNext.setOnClickListener { handleNext() }
        binding.btnSaveDraft.setOnClickListener { handleSaveDraft() }
        binding.btnPublish.setOnClickListener { handlePublish() }
        binding.btnPreview.setOnClickListener { handlePreview() }
    }

    private fun showStep(step: Int) {
        currentStep = step
        updateStepIndicator(step, totalSteps)

        binding.layoutStep1.visibility = if (step == 1) View.VISIBLE else View.GONE
        binding.layoutStep2.visibility = if (step == 2) View.VISIBLE else View.GONE
        binding.layoutStep3.visibility = if (step == 3) View.VISIBLE else View.GONE
        binding.layoutStep4.visibility = if (step == 4) View.VISIBLE else View.GONE

        // Button visibility
        binding.btnNext.visibility = if (step < totalSteps) View.VISIBLE else View.GONE
        binding.btnSaveDraft.visibility = if (step == totalSteps) View.VISIBLE else View.GONE
        binding.btnPublish.visibility = if (step == totalSteps) View.VISIBLE else View.GONE

        // Populate fields when stepping into a section
        when (step) {
            1 -> setupStep1()
            2 -> setupStep2()
            3 -> setupStep3()
            4 -> setupStep4()
        }

        // Update only the step counter subtitle, keep main title as "Create HangOut"
        binding.tvStepCounter.text = "${stepTitles[step - 1]} · Step $step of $totalSteps"
    }

    private val stepTitles = listOf(
        "Basic Info",
        "Date, Time & Format",
        "Capacity & Seating",
        "Pricing & Payment"
    )

    private fun handleNext() {
        // Collect current step data first
        collectCurrentStepData()
        val error = validateCurrentStep()
        if (error != null) {
            toast(error); return
        }
        showStep(currentStep + 1)
    }

    private fun goBack() {
        collectCurrentStepData()
        showStep(currentStep - 1)
    }

    private fun handleSaveDraft() {
        collectCurrentStepData()
        presenter.saveDraft(formState, coverImagePath)
    }

    private fun handlePublish() {
        collectCurrentStepData()
        presenter.publishEvent(formState, coverImagePath)
    }

    private fun handlePreview() {
        collectCurrentStepData()
        // Store in holder to avoid TransactionTooLargeException
        com.hangout.app.utils.CreateEventHolder.formState = formState
        com.hangout.app.utils.CreateEventHolder.coverImagePath = coverImagePath
        val intent = Intent(this, com.hangout.app.ui.previewevent.PreviewEventActivity::class.java)
        previewLauncher.launch(intent)
    }

    // ── Per-step setup ─────────────────────────────────────────────────────

    private fun setupStep1() {
        binding.etTitle.setText(formState.title)
        binding.etDescription.setText(formState.description)

        // Cover photo preview
        if (coverImagePath != null) {
            Glide.with(this).load(coverImagePath).centerCrop().into(binding.ivCoverPreview)
            binding.ivCoverPreview.show()
            binding.tvCoverPlaceholder.hide()
        }

        binding.layoutCoverPhoto.setOnClickListener { openImagePicker() }
    }

    private fun setupStep2() {

        // Platform dropdown with custom styled dropdown items
        val platforms =
            listOf("Zoom", "Google Meet", "Microsoft Teams", "Webex", "Discord", "Other")
        val platformAdapter =
            ArrayAdapter(this, R.layout.autocomplete_dropdown_item, platforms)
        binding.etVirtualPlatform.setAdapter(platformAdapter)
        binding.etVirtualPlatform.setOnClickListener { binding.etVirtualPlatform.showDropDown() }
// Restore saved value
        if (formState.virtualPlatform.isNotBlank()) {
            binding.etVirtualPlatform.setText(formState.virtualPlatform, false)
        }

        binding.etDate.setText(formState.date)
        binding.etStartTime.setText(format24to12(formState.startTime))
        binding.etEndTime.setText(format24to12(formState.endTime))
        binding.etLocation.setText(formState.location)
        binding.etVirtualPlatform.setText(formState.virtualPlatform)
        binding.etVirtualLink.setText(formState.virtualLink)

        // Format selection
        updateFormatSelection(formState.format)
        binding.btnFormatInPerson.setOnClickListener { selectFormat("In-Person") }
        binding.btnFormatVirtual.setOnClickListener { selectFormat("Virtual") }
        binding.btnFormatHybrid.setOnClickListener { selectFormat("Hybrid") }

        // Date/time pickers
        binding.etDate.setOnClickListener { showDatePicker() }
        binding.etStartTime.setOnClickListener { showStartTimePicker() }
        binding.etEndTime.setOnClickListener { showEndTimePicker() }
        binding.tilDate.setEndIconOnClickListener { showDatePicker() }
        binding.tilStartTime.setEndIconOnClickListener { showStartTimePicker() }
        binding.tilEndTime.setEndIconOnClickListener { showEndTimePicker() }
    }

    private fun setupStep3() {
        binding.etCapacity.setText(formState.capacity)
        updateSeatingSelection(formState.seatingType)
        binding.btnSeatingOpen.setOnClickListener { selectSeating("open") }
        binding.btnSeatingReserved.setOnClickListener { selectSeating("reserved") }
    }

    private fun setupStep4() {
        binding.etPrice.setText(formState.price)
        binding.etPaymentMethod.setText(formState.paymentMethod)
        binding.etAccountName.setText(formState.accountName)
        binding.etAccountNumber.setText(formState.accountNumber)
        binding.switchNoRefund.isChecked = formState.noRefundPolicy

        updateEventTypeSelection(formState.eventType)
        binding.btnTypeFree.setOnClickListener { selectEventType("free") }
        binding.btnTypePaid.setOnClickListener { selectEventType("paid") }
    }

    // ── Data collection ────────────────────────────────────────────────────

    private fun collectCurrentStepData() {
        formState = when (currentStep) {
            1 -> formState.copy(
                title = binding.etTitle.text.toString().trim(),
                description = binding.etDescription.text.toString().trim()
            )

            2 -> formState.copy(
                date = binding.etDate.text.toString(),
                startTime = binding.etStartTime.text.toString(),
                endTime = binding.etEndTime.text.toString(),
                location = binding.etLocation.text.toString().trim(),
                virtualPlatform = binding.etVirtualPlatform.text.toString().trim(),
                virtualLink = binding.etVirtualLink.text.toString().trim()
            )

            3 -> formState.copy(
                capacity = binding.etCapacity.text.toString()
            )

            4 -> formState.copy(
                price = binding.etPrice.text.toString(),
                paymentMethod = binding.etPaymentMethod.text.toString().trim(),
                accountName = binding.etAccountName.text.toString().trim(),
                accountNumber = binding.etAccountNumber.text.toString().trim(),
                noRefundPolicy = binding.switchNoRefund.isChecked
            )

            else -> formState
        }
    }

    // ── Per-step validation ────────────────────────────────────────────────

    private fun validateCurrentStep(): String? = when (currentStep) {
        1 -> {
            val title = binding.etTitle.text.toString().trim()
            if (title.isBlank()) "Event title is required" else null
        }

        2 -> {
            val date = binding.etDate.text.toString()
            val start = binding.etStartTime.text.toString()
            when {
                date.isBlank() -> "Event date is required"
                start.isBlank() -> "Start time is required"
                else -> null
            }
        }

        3 -> {
            val cap = binding.etCapacity.text.toString().toIntOrNull()
            if (cap == null || cap <= 0) "Enter a valid capacity (e.g. 50)" else null
        }

        else -> null
    }

    // ── Format & Type helpers ──────────────────────────────────────────────

    private fun selectFormat(format: String) {
        formState = formState.copy(format = format)
        updateFormatSelection(format)
    }

    private fun updateFormatSelection(format: String) {
        val active = ContextCompat.getColor(this, R.color.purple_main)
        val inactive = ContextCompat.getColor(this, R.color.bg_card)
        val white = ContextCompat.getColor(this, R.color.white)
        val muted = ContextCompat.getColor(this, R.color.text_muted)

        fun style(btn: Button, selected: Boolean) {
            btn.setBackgroundColor(if (selected) active else inactive)
            btn.setTextColor(if (selected) white else muted)
        }

        style(binding.btnFormatInPerson, format == "In-Person")
        style(binding.btnFormatVirtual, format == "Virtual")
        style(binding.btnFormatHybrid, format == "Hybrid")

        // Show/hide location and virtual fields based on format
        val showLocation = format != "Virtual"
        val showVirtual = format != "In-Person"
        binding.tilLocation.visibility = if (showLocation) View.VISIBLE else View.GONE
        binding.layoutVirtualFields.visibility = if (showVirtual) View.VISIBLE else View.GONE
    }

    private fun selectSeating(type: String) {
        formState = formState.copy(seatingType = type)
        updateSeatingSelection(type)
    }

    private fun updateSeatingSelection(type: String) {
        val active = ContextCompat.getColor(this, R.color.purple_main)
        val inactive = ContextCompat.getColor(this, R.color.bg_card)
        val white = ContextCompat.getColor(this, R.color.white)
        val muted = ContextCompat.getColor(this, R.color.text_muted)

        binding.btnSeatingOpen.setBackgroundColor(if (type == "open") active else inactive)
        binding.btnSeatingOpen.setTextColor(if (type == "open") white else muted)
        binding.btnSeatingReserved.setBackgroundColor(if (type == "reserved") active else inactive)
        binding.btnSeatingReserved.setTextColor(if (type == "reserved") white else muted)
    }

    private fun selectEventType(type: String) {
        formState = formState.copy(eventType = type)
        updateEventTypeSelection(type)
    }

    private fun updateEventTypeSelection(type: String) {
        val active = ContextCompat.getColor(this, R.color.purple_main)
        val inactive = ContextCompat.getColor(this, R.color.bg_card)
        val white = ContextCompat.getColor(this, R.color.white)
        val muted = ContextCompat.getColor(this, R.color.text_muted)

        binding.btnTypeFree.setBackgroundColor(if (type == "free") active else inactive)
        binding.btnTypeFree.setTextColor(if (type == "free") white else muted)
        binding.btnTypePaid.setBackgroundColor(if (type == "paid") active else inactive)
        binding.btnTypePaid.setTextColor(if (type == "paid") white else muted)

        binding.layoutPaidFields.visibility = if (type == "paid") View.VISIBLE else View.GONE
    }

    // ── Date / Time pickers ────────────────────────────────────────────────

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                val mm = String.format("%02d", month + 1)
                val dd = String.format("%02d", day)
                val formatted = "$year-$mm-$dd"
                formState = formState.copy(date = formatted)
                binding.etDate.setText(formatted)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis() - 1000 // today or future
        }.show()
    }

    private fun showStartTimePicker() {
        val cal = Calendar.getInstance()
        TimePickerDialog(this, { _, hour, minute ->
            val formatted24 = String.format("%02d:%02d", hour, minute)
            formState = formState.copy(startTime = formatted24)
            binding.etStartTime.setText(to12Hr(hour, minute))
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
    }

    private fun showEndTimePicker() {
        val cal = Calendar.getInstance()
        TimePickerDialog(this, { _, hour, minute ->
            val formatted24 = String.format("%02d:%02d", hour, minute)
            formState = formState.copy(endTime = formatted24)
            binding.etEndTime.setText(to12Hr(hour, minute))
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
    }

    private fun to12Hr(hour: Int, minute: Int): String {
        val ampm = if (hour >= 12) "PM" else "AM"
        val h12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format("%d:%02d %s", h12, minute, ampm)
    }

    private fun format24to12(time24: String): String {
        if (time24.isBlank()) return ""
        return try {
            val parts = time24.split(":")
            to12Hr(parts[0].toInt(), parts[1].toInt())
        } catch (_: Exception) {
            time24
        }
    }

    // ── Image picker ───────────────────────────────────────────────────────

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        imagePickerLauncher.launch(intent)
    }

    private fun handleImageSelected(uri: Uri) {
        coverImagePath = getRealPathFromUri(this, uri) ?: uri.toString()
        Glide.with(this).load(uri).centerCrop().into(binding.ivCoverPreview)
        binding.ivCoverPreview.show()
        binding.tvCoverPlaceholder.hide()
    }

    // ── CreateEventContract.View ───────────────────────────────────────────

    override fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnPublish.isEnabled = !show
        binding.btnSaveDraft.isEnabled = !show
        binding.btnNext.isEnabled = !show
    }

    override fun showError(message: String) {
        toast(message)
    }

    override fun showSuccess(message: String) {
        toast(message)
    }

    override fun onEventCreated(eventId: Long, isDraft: Boolean) {
        val resultIntent = Intent().apply {
            putExtra("eventId", eventId)
            putExtra("isDraft", isDraft)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    override fun onEventLoaded(eventId: Long) {
        // Get the loaded event from the presenter
        val event = (presenter as? CreateEventPresenter)?.loadedEvent
        if (event != null) {
            // Convert EventItem to CreateEventFormState
            formState = CreateEventFormState(
                title = event.title ?: "",
                description = event.description ?: "",
                coverImagePath = null,  // Local path not available; image will be loaded from URL
                imageUrl = event.imageUrl,  // Use remote image URL from server
                date = event.date ?: "",
                startTime = event.startTime ?: "",
                endTime = event.endTime ?: "",
                format = event.format ?: "In-Person",
                location = event.location ?: "",
                virtualPlatform = event.virtualPlatform ?: "",
                virtualLink = event.virtualLink ?: "",
                capacity = event.capacity?.toString() ?: "",
                seatingType = event.seatingType ?: "open",
                eventType = if ((event.price ?: 0.0) > 0) "paid" else "free",
                price = event.price?.toString() ?: "0",
                paymentMethod = event.paymentMethod ?: "",
                accountName = event.accountName ?: "",
                accountNumber = event.accountNumber ?: "",
                noRefundPolicy = event.noRefundPolicy ?: false,
                hostFirstName = event.hostFirstName ?: "",
                hostLastName = event.hostLastName ?: "",
                hostEmail = event.hostEmail ?: "",
                hostPhoto = event.hostPhoto
            )

            // Load cover image if available
            if (!event.imageUrl.isNullOrBlank()) {
                Glide.with(this).load(event.imageUrl).centerCrop().into(binding.ivCoverPreview)
                binding.ivCoverPreview.show()
                binding.tvCoverPlaceholder.hide()
            }
        }

        // Event data is loaded, now show the form
        showStep(1)
        // Show unpublish button if in edit mode
        if (isEditMode) {
            binding.btnUnpublish.visibility = View.VISIBLE
            binding.btnUnpublish.setOnClickListener { promptUnpublishEvent() }
        }
    }

    override fun onEventUnpublished() {
        toast("Event unpublished")
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun updateStepIndicator(currentStep: Int, totalSteps: Int) {
        binding.tvStepCounter.text = "Step $currentStep of $totalSteps"

        // Update step dot indicators
        val dots = listOf(binding.dot1, binding.dot2, binding.dot3, binding.dot4)
        val active = ContextCompat.getColor(this, R.color.purple_main)
        val inactive = ContextCompat.getColor(this, R.color.bg_card)
        dots.forEachIndexed { index, view ->
            view.setBackgroundColor(if (index < currentStep) active else inactive)
        }
    }

    private fun promptUnpublishEvent() {
        val input = createStyledDialogEditText(this, "Reason for unpublishing (optional)")
        input.minLines = 3
        android.app.AlertDialog.Builder(this, R.style.HangOutAlertDialogTheme)
            .setTitle("Unpublish Event")
            .setMessage("This will unpublish the event for all attendees.")
            .setView(input)
            .setPositiveButton("Unpublish") { _, _ ->
                editEventId?.let { presenter.unpublishEvent(it) }
            }
            .setNegativeButton("Keep Event", null)
            .show()
    }
}