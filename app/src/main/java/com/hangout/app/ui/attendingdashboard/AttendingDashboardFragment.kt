package com.hangout.app.ui.attendingdashboard

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.hangout.app.R
import com.hangout.app.data.EventItem
import com.hangout.app.databinding.FragmentAttendingDashboardBinding
import com.hangout.app.network.RetrofitClient
import com.hangout.app.ui.paymentproof.PaymentProofBottomSheet
import com.hangout.app.ui.ticket.DigitalTicketFragment
import com.hangout.app.utils.EventHolder
import com.hangout.app.utils.hide
import com.hangout.app.utils.show
import com.hangout.app.utils.showIf
import com.hangout.app.utils.toast
import com.hangout.app.ui.components.createStyledAlertDialog

class AttendingDashboardFragment : Fragment(), AttendingDashboardContract.View {

    private var _binding: FragmentAttendingDashboardBinding? = null
    private val binding get() = _binding!!

    private lateinit var presenter: AttendingDashboardContract.Presenter
    private var event: EventItem? = null
    var onBackCallback: (() -> Unit)? = null

    companion object {
        fun newInstance(onBack: (() -> Unit)? = null) =
            AttendingDashboardFragment().apply { onBackCallback = onBack }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        retainInstance = true
        if (!::presenter.isInitialized) {
            presenter = AttendingDashboardPresenter(this, AttendingDashboardModel(requireContext()))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAttendingDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        event = EventHolder.currentEvent ?: return

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
            onBackCallback?.invoke()
        }

        event?.let { presenter.loadEvent(it) }
    }

    // ── AttendingDashboardContract.View ───────────────────────────────────

    override fun showEvent(event: EventItem) {
        this.event = event

        binding.tvEventTitle.text    = event.title ?: "Untitled"
        binding.tvEventDate.text     = "${event.date ?: "—"} · ${formatTime(event)}"
        binding.tvEventLocation.text = event.location?.ifBlank { "Virtual / TBD" } ?: "Virtual / TBD"
        binding.tvEventFormat.text   = event.format ?: "In-Person"
        binding.tvEventPrice.text    = if ((event.price ?: 0.0) == 0.0) "Free"
        else "₱${event.price?.toInt()}"

        val (statusLabel, statusColor) = resolveStatusDisplay(event)
        binding.tvStatusLabel.text = statusLabel
        binding.tvStatusLabel.setTextColor(ContextCompat.getColor(requireContext(), statusColor))
        binding.viewStatusStripe.setBackgroundColor(
            ContextCompat.getColor(requireContext(), statusColor)
        )

        // ── Ticket info (confirmed only) ──────────────────────────────────
        val isConfirmed = event.rsvpPaymentStatus == "confirmed" ||
                (event.status == "confirmed" && event.rsvpPaymentStatus != "pending")
        binding.layoutTicketInfo.showIf(isConfirmed)
        if (isConfirmed) {
            binding.tvTicketNumber.text = event.ticketNumber ?: "TKT-${event.id}"
            binding.tvSeatNumber.text   = event.seatNumber?.ifBlank { "Open Seating" } ?: "Open Seating"
        }

        binding.btnViewTicket.setOnClickListener {
            val fragment = DigitalTicketFragment.newInstance(onBack = {})
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .commit()
        }

        // ── Virtual link ──────────────────────────────────────────────────
        val hasVirtualLink = !event.virtualLink.isNullOrBlank()
        binding.layoutVirtualLink.showIf(hasVirtualLink && isConfirmed)
        binding.btnJoinMeeting.setOnClickListener {
            event.virtualLink?.let { link ->
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
            }
        }

        // ── Payment status section ────────────────────────────────────────
        val isPaidEvent = (event.price ?: 0.0) > 0.0
        binding.layoutPaymentSection.showIf(isPaidEvent)
        if (isPaidEvent) {
            val (payLabel, payColor) = when (event.rsvpPaymentStatus) {
                "pending"   -> "Pending Host Approval"        to R.color.yellow_accent
                "confirmed" -> "Payment Approved ✓"           to R.color.success_green
                "rejected"  -> "Payment Rejected"             to R.color.red_accent
                else        -> "Not yet submitted"            to R.color.text_muted
            }
            binding.tvPaymentStatus.text = payLabel
            binding.tvPaymentStatus.setTextColor(ContextCompat.getColor(requireContext(), payColor))

            // Show rejection reason if payment was rejected
            val rejectionReason = event.paymentRejectionReason
            if (event.rsvpPaymentStatus == "rejected" && !rejectionReason.isNullOrBlank()) {
                binding.tvPaymentRejectionReason.show()
                binding.tvPaymentRejectionReason.text = "Reason: $rejectionReason"
            } else {
                binding.tvPaymentRejectionReason.hide()
            }
        }

        // ── Refund section ────────────────────────────────────────────────
        bindRefundSection(event)

        // ── Action buttons ────────────────────────────────────────────────
        setupActionButtons(event)
    }

    // ── Refund section ────────────────────────────────────────────────────

    private fun bindRefundSection(event: EventItem) {
        val refundStatus = event.refundStatus
        val hasRefund    = !refundStatus.isNullOrBlank() && refundStatus != "none"
        binding.layoutRefundSection.showIf(hasRefund)
        if (!hasRefund) return

        val (label, colorRes) = when (refundStatus) {
            "pending"                 -> "Refund requested — awaiting host review"    to R.color.yellow_accent
            "waiting_acknowledgement" -> "Refund processed — please acknowledge below" to R.color.purple_light
            "completed"               -> "Refund completed ✓"                         to R.color.success_green
            "rejected"                -> "Refund request declined"                    to R.color.red_accent
            else                      -> (refundStatus ?: "")                         to R.color.text_muted
        }
        binding.tvRefundStatus.text = label
        binding.tvRefundStatus.setTextColor(ContextCompat.getColor(requireContext(), colorRes))

        // Show refund proof image when host has sent it — guest can verify before acknowledging
        val proofUrl = event.refundProofUrl
        if (refundStatus == "waiting_acknowledgement" && !proofUrl.isNullOrBlank()) {
            binding.ivRefundProof.show()
            val fullUrl = RetrofitClient.BASE_URL + proofUrl
            Glide.with(this)
                .load(fullUrl)
                .placeholder(R.drawable.ic_avatar_placeholder)
                .error(R.drawable.ic_avatar_placeholder)
                .into(binding.ivRefundProof)
        } else {
            binding.ivRefundProof.hide()
        }

        // Show rejection reason when declined
        val rejectionReason = event.refundRejectionReason
        if (refundStatus == "rejected" && !rejectionReason.isNullOrBlank()) {
            binding.tvRefundRejectionReason.show()
            binding.tvRefundRejectionReason.text = "Reason: $rejectionReason"
        } else {
            binding.tvRefundRejectionReason.hide()
        }
    }

    // ── Action buttons ────────────────────────────────────────────────────

    private fun setupActionButtons(event: EventItem) {
        val eventId        = event.id ?: return
        val isCancelled    = event.status == "cancelled"
        val isRejected     = event.attendeeStatus == "rejected" || event.rsvpPaymentStatus == "rejected"
        val isConfirmed    = event.rsvpPaymentStatus == "confirmed" ||
                (event.status == "confirmed" && event.rsvpPaymentStatus != "pending")
        val noProof        = event.rsvpPaymentStatus == null && (event.price ?: 0.0) > 0.0
        val noRefundPolicy = event.noRefundPolicy == true
        val refundStatus   = event.refundStatus
        val isEventActive  = event.eventStatus == "active" || event.eventStatus == null
        val isPaid         = (event.price ?: 0.0) > 0.0

        // Upload proof
        val canUploadProof = isPaid && !isCancelled && !isRejected &&
                (noProof || event.rsvpPaymentStatus == "rejected")
        binding.btnUploadProof.showIf(canUploadProof)
        binding.btnUploadProof.setOnClickListener {
            PaymentProofBottomSheet.newInstance(
                event     = event,
                onSuccess = { event.id?.let { presenter.refreshEvent(it) } }
            ).show(parentFragmentManager, "payment_proof")
        }

        // Cancel RSVP — update label dynamically
        val canCancel = !isCancelled && !isRejected && isEventActive && refundStatus.isNullOrBlank()
        binding.btnCancelRsvp.showIf(canCancel)
        if (canCancel) {
            val cancelLabel = when {
                isPaid && !noRefundPolicy -> "Cancel RSVP & Request Refund"
                else                      -> "Cancel RSVP"
            }
            // tvCancelRsvpLabel is the TextView inside the btnCancelRsvp LinearLayout
            binding.tvCancelRsvpLabel.text = cancelLabel
            binding.btnCancelRsvp.setOnClickListener { promptCancelRsvp(eventId) }
        }

        // Request refund (confirmed + paid + not already refunded)
        val canRefund = isConfirmed && isPaid && !noRefundPolicy && isEventActive &&
                (refundStatus.isNullOrBlank() || refundStatus == "rejected")
        binding.btnRequestRefund.showIf(canRefund)
        binding.btnRequestRefund.setOnClickListener { promptRequestRefund(eventId) }

        // Acknowledge refund
        val canAcknowledge = refundStatus == "waiting_acknowledgement"
        binding.btnAcknowledgeRefund.showIf(canAcknowledge)
        if (canAcknowledge) {
            binding.btnAcknowledgeRefund.setOnClickListener {
                promptAcknowledgeRefund(eventId)
            }
        }
    }

    // ── Contract callbacks ────────────────────────────────────────────────

    override fun showLoading(show: Boolean) = binding.progressBar.showIf(show)

    override fun showMessage(message: String) = toast(message)

    override fun onCancelSuccess() {
        toast("RSVP cancelled.")
        onBackCallback?.let { callback ->
            parentFragmentManager.popBackStack()
            view?.post { callback() }
        } ?: run {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onRefundRequested() {
        toast("Refund requested. The host will review it.")
        onBackCallback?.let { callback ->
            parentFragmentManager.popBackStack()
            view?.post { callback() }
        } ?: run {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onRefundAcknowledged() {
        toast("Refund acknowledged. Thank you!")
        onBackCallback?.let { callback ->
            parentFragmentManager.popBackStack()
            view?.post { callback() }
        } ?: run {
            parentFragmentManager.popBackStack()
        }
    }

    // ── Rich cancel dialog (mirrors EventDetailFragment) ──────────────────

    private fun promptCancelRsvp(eventId: Long) {
        val event      = this.event ?: return
        val isPaid     = (event.price ?: 0.0) > 0.0
        val noRefund   = event.noRefundPolicy == true
        val ctx        = requireContext()

        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24.dpToPx(), 8.dpToPx(), 24.dpToPx(), 8.dpToPx())
        }

        // Subtitle
        container.addView(TextView(ctx).apply {
            text      = "You're about to cancel your spot for \"${event.title}\"."
            textSize  = 13f
            setTextColor(resources.getColor(R.color.text_muted, null))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16.dpToPx() }
        })

        // Paid + refundable — indigo info banner
        if (isPaid && !noRefund) {
            container.addView(LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity     = Gravity.TOP
                background  = android.graphics.drawable.GradientDrawable().apply {
                    setColor(0x1A6366F1.toInt())
                    setStroke(1.dpToPx(), 0x406366F1.toInt())
                    cornerRadius = 10.dpToPx().toFloat()
                }
                setPadding(14.dpToPx(), 12.dpToPx(), 14.dpToPx(), 12.dpToPx())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 16.dpToPx() }

                addView(ImageView(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(18.dpToPx(), 18.dpToPx()).apply {
                        marginEnd = 10.dpToPx(); topMargin = 2.dpToPx()
                    }
                    setImageResource(R.drawable.ic_refund)
                    setColorFilter(0xFFA5B4FC.toInt())
                })
                addView(LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    addView(TextView(ctx).apply {
                        text     = "Paid Event — Reimbursement Request"
                        textSize = 12f
                        typeface = android.graphics.Typeface.defaultFromStyle(android.graphics.Typeface.BOLD)
                        setTextColor(0xFFA5B4FC.toInt())
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { bottomMargin = 2.dpToPx() }
                    })
                    addView(TextView(ctx).apply {
                        text     = "Cancelling a paid RSVP will initiate a reimbursement request. Please explain your reason below to help the host process it."
                        textSize = 12f
                        setTextColor(0xFF818CF8.toInt())
                        setLineSpacing(0f, 1.4f)
                    })
                })
            })
        }

        // Paid + no-refund — red warning banner
        if (isPaid && noRefund) {
            container.addView(LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity     = Gravity.TOP
                background  = android.graphics.drawable.GradientDrawable().apply {
                    setColor(0x1AEF4444.toInt())
                    setStroke(1.dpToPx(), 0x40EF4444.toInt())
                    cornerRadius = 10.dpToPx().toFloat()
                }
                setPadding(14.dpToPx(), 12.dpToPx(), 14.dpToPx(), 12.dpToPx())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 16.dpToPx() }

                addView(ImageView(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(18.dpToPx(), 18.dpToPx()).apply {
                        marginEnd = 10.dpToPx(); topMargin = 2.dpToPx()
                    }
                    setImageResource(R.drawable.ic_warning)
                    setColorFilter(resources.getColor(R.color.red_accent, null))
                })
                addView(LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    addView(TextView(ctx).apply {
                        text     = "No Refunds Policy"
                        textSize = 12f
                        typeface = android.graphics.Typeface.defaultFromStyle(android.graphics.Typeface.BOLD)
                        setTextColor(resources.getColor(R.color.red_accent, null))
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { bottomMargin = 2.dpToPx() }
                    })
                    addView(TextView(ctx).apply {
                        text     = "This event has a no-refund policy. Cancelling your RSVP cannot be reversed and no refund will be issued."
                        textSize = 12f
                        setTextColor(0xFFF87171.toInt())
                        setLineSpacing(0f, 1.4f)
                    })
                })
            })
        }

        // Reason input
        val reasonInput = android.widget.EditText(ctx).apply {
            hint      = if (isPaid && !noRefund)
                "Explain your reason for cancelling (helps the host process your refund)…"
            else
                "Tell us why you're cancelling (optional)…"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                    android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines  = 3
            maxLines  = 5
            setPadding(14.dpToPx(), 12.dpToPx(), 14.dpToPx(), 12.dpToPx())
            setTextColor(resources.getColor(R.color.text_primary, null))
            setHintTextColor(resources.getColor(R.color.text_muted, null))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0x0DFFFFFF.toInt())
                setStroke(1.dpToPx(), 0x1FFFFFFF.toInt())
                cornerRadius = 10.dpToPx().toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        container.addView(reasonInput)

        val confirmLabel = if (isPaid && !noRefund) "Cancel RSVP & Request Refund" else "Cancel RSVP"

        val dialog = AlertDialog.Builder(ctx, R.style.HangOutAlertDialogTheme)
            .setTitle("Cancel RSVP")
            .setView(container)
            .setPositiveButton(confirmLabel) { _, _ ->
                val reason = reasonInput.text.toString().trim()
                presenter.cancelRsvp(eventId)
                // Note: pass reason through cancelRsvp if your model/API supports it
            }
            .setNegativeButton("Keep RSVP", null)
            .setCancelable(true)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                ?.setTextColor(resources.getColor(R.color.red_accent, null))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                ?.setTextColor(resources.getColor(R.color.text_muted, null))
        }

        dialog.show()
    }

    private fun promptRequestRefund(eventId: Long) {
        val ctx = requireContext()
        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24.dpToPx(), 8.dpToPx(), 24.dpToPx(), 8.dpToPx())
        }

        container.addView(TextView(ctx).apply {
            text     = "Please provide a reason so the host can process your refund."
            textSize = 13f
            setTextColor(resources.getColor(R.color.text_muted, null))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 14.dpToPx() }
        })

        val reasonInput = android.widget.EditText(ctx).apply {
            hint      = "Reason for refund request…"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                    android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines  = 3
            maxLines  = 5
            setPadding(14.dpToPx(), 12.dpToPx(), 14.dpToPx(), 12.dpToPx())
            setTextColor(resources.getColor(R.color.text_primary, null))
            setHintTextColor(resources.getColor(R.color.text_muted, null))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0x0DFFFFFF.toInt())
                setStroke(1.dpToPx(), 0x1FFFFFFF.toInt())
                cornerRadius = 10.dpToPx().toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        container.addView(reasonInput)

        val dialog = AlertDialog.Builder(ctx, R.style.HangOutAlertDialogTheme)
            .setTitle("Request Refund")
            .setView(container)
            .setPositiveButton("Submit Request") { _, _ ->
                val reason = reasonInput.text.toString().trim()
                    .ifBlank { "Refund requested by attendee." }
                presenter.requestRefund(eventId, reason)
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                ?.setTextColor(resources.getColor(R.color.yellow_accent, null))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                ?.setTextColor(resources.getColor(R.color.text_muted, null))
        }

        dialog.show()
    }

    private fun promptAcknowledgeRefund(eventId: Long) {
        val ctx = requireContext()

        // Track selection state
        var selectedChoice = "received"  // default

        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24.dpToPx(), 8.dpToPx(), 24.dpToPx(), 8.dpToPx())
        }

        // Description
        container.addView(TextView(ctx).apply {
            text     = "Please confirm whether you received the refund for \"${event?.title}\"."
            textSize = 13f
            setTextColor(resources.getColor(R.color.text_muted, null))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 20.dpToPx() }
        })

        // Radio group
        val radioGroup = RadioGroup(ctx).apply {
            orientation = RadioGroup.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16.dpToPx() }
        }

        val radioReceived = RadioButton(ctx).apply {
            id      = android.view.View.generateViewId()
            text    = "I received the refund"
            textSize = 14f
            isChecked = true
            setTextColor(resources.getColor(R.color.text_primary, null))
            buttonTintList = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(ctx, R.color.success_green)
            )
        }

        val radioNotReceived = RadioButton(ctx).apply {
            id       = android.view.View.generateViewId()
            text     = "I did not receive the refund"
            textSize = 14f
            setTextColor(resources.getColor(R.color.text_primary, null))
            buttonTintList = android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(ctx, R.color.red_accent)
            )
        }

        radioGroup.addView(radioReceived)
        radioGroup.addView(radioNotReceived)
        container.addView(radioGroup)

        // Reason input — shown only when "not received" is selected
        val reasonLabel = TextView(ctx).apply {
            text     = "Please describe the issue  *"
            textSize = 13f
            typeface = android.graphics.Typeface.defaultFromStyle(android.graphics.Typeface.BOLD)
            setTextColor(resources.getColor(R.color.text_primary, null))
            visibility = android.view.View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 8.dpToPx() }
        }
        container.addView(reasonLabel)

        val reasonInput = android.widget.EditText(ctx).apply {
            hint      = "Describe why you did not receive the refund…"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                    android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines  = 3
            maxLines  = 5
            visibility = android.view.View.GONE
            setPadding(14.dpToPx(), 12.dpToPx(), 14.dpToPx(), 12.dpToPx())
            setTextColor(resources.getColor(R.color.text_primary, null))
            setHintTextColor(resources.getColor(R.color.text_muted, null))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0x0DFFFFFF.toInt())
                setStroke(1.dpToPx(), 0x1FFFFFFF.toInt())
                cornerRadius = 10.dpToPx().toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        container.addView(reasonInput)

        // Toggle reason field visibility based on radio selection
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val notReceived = checkedId == radioNotReceived.id
            selectedChoice  = if (notReceived) "rejected" else "received"
            val vis = if (notReceived) android.view.View.VISIBLE else android.view.View.GONE
            reasonLabel.visibility = vis
            reasonInput.visibility = vis
        }

        val dialog = AlertDialog.Builder(ctx, R.style.HangOutAlertDialogTheme)
            .setTitle("Confirm Refund Receipt")
            .setView(container)
            .setPositiveButton("Confirm") { _, _ ->
                val reason = reasonInput.text.toString().trim()
                if (selectedChoice == "rejected" && reason.isBlank()) {
                    toast("Please describe why you did not receive the refund.")
                    return@setPositiveButton
                }
                presenter.acknowledgeRefund(
                    eventId = eventId,
                    choice  = selectedChoice,
                    reason  = if (selectedChoice == "rejected") reason else null
                )
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                ?.setTextColor(resources.getColor(R.color.success_green, null))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                ?.setTextColor(resources.getColor(R.color.text_muted, null))
        }

        dialog.show()
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun resolveStatusDisplay(event: EventItem): Pair<String, Int> = when {
        event.status         == "cancelled"               -> "Cancelled"                to R.color.text_muted
        event.attendeeStatus == "rejected"                -> "Rejected by Host"         to R.color.red_accent
        event.rsvpPaymentStatus == "rejected"             -> "Payment Rejected"         to R.color.red_accent
        event.attendeeStatus == "attended"                -> "Attended"                 to R.color.success_green
        event.eventStatus    == "completed"               -> "Event Completed"          to R.color.purple_light
        event.rsvpPaymentStatus == "pending"              -> "Pending Payment Approval" to R.color.yellow_accent
        event.rsvpPaymentStatus == "confirmed"            -> "Confirmed"                to R.color.success_green
        event.status         == "confirmed"               -> "Confirmed"                to R.color.success_green
        event.refundStatus   == "waiting_acknowledgement" -> "Refund Processed"         to R.color.purple_light
        event.refundStatus   == "completed"               -> "Refund Completed"         to R.color.success_green
        else                                              -> "Registered"               to R.color.purple_main
    }

    private fun formatTime(event: EventItem): String {
        val start = event.startTime ?: event.time ?: return "Time TBD"
        val end   = event.endTime
        return if (!end.isNullOrBlank()) {
            "${com.hangout.app.utils.formatTime12Hr(start)} – ${com.hangout.app.utils.formatTime12Hr(end)}"
        } else {
            com.hangout.app.utils.formatTime12Hr(start)
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.detachView()
        _binding = null
    }
}