package com.hangout.app.ui.attendingdashboard

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.hangout.app.R
import com.hangout.app.data.EventItem
import com.hangout.app.databinding.FragmentAttendingDashboardBinding
import com.hangout.app.ui.paymentproof.PaymentProofBottomSheet
import com.hangout.app.ui.ticket.DigitalTicketFragment
import com.hangout.app.utils.EventHolder
import com.hangout.app.utils.hide
import com.hangout.app.utils.show
import com.hangout.app.utils.showIf
import com.hangout.app.utils.toast

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

        // ── Event info ────────────────────────────────────────────────────
        binding.tvEventTitle.text    = event.title ?: "Untitled"
        binding.tvEventDate.text     = "📅 ${event.date ?: "—"}  ·  ${formatTime(event)}"
        binding.tvEventLocation.text = "📍 ${event.location?.ifBlank { "Virtual / TBD" } ?: "Virtual / TBD"}"
        binding.tvEventFormat.text   = event.format ?: "In-Person"
        binding.tvEventPrice.text    = if ((event.price ?: 0.0) == 0.0) "Free"
        else "₱${event.price?.toInt()}"

        // ── Status banner ─────────────────────────────────────────────────
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
            val fragment = DigitalTicketFragment.newInstance(
                onBack = { /* stays on attending dashboard */ }
            )
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .addToBackStack(null)
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

        // ── Payment status section (paid events) ──────────────────────────
        val isPaidEvent = (event.price ?: 0.0) > 0.0
        binding.layoutPaymentSection.showIf(isPaidEvent)
        if (isPaidEvent) {
            val payLabel = when (event.rsvpPaymentStatus) {
                "pending"   -> "⏳ Pending Host Approval"
                "confirmed" -> "✓ Payment Approved"
                "rejected"  -> "✗ Payment Rejected"
                else        -> "Not yet submitted"
            }
            val payColor = when (event.rsvpPaymentStatus) {
                "pending"   -> R.color.yellow_accent
                "confirmed" -> R.color.success_green
                "rejected"  -> R.color.red_accent
                else        -> R.color.text_muted
            }
            binding.tvPaymentStatus.text = payLabel
            binding.tvPaymentStatus.setTextColor(
                ContextCompat.getColor(requireContext(), payColor)
            )
        }

        // ── Refund status ─────────────────────────────────────────────────
        val hasRefund = !event.refundStatus.isNullOrBlank() &&
                event.refundStatus != "none"
        binding.layoutRefundSection.showIf(hasRefund)
        if (hasRefund) {
            val refundLabel = when (event.refundStatus) {
                "pending"                -> "Refund Requested"
                "waiting_acknowledgement"-> "Refund Processed — tap to acknowledge"
                "completed"              -> "Refund Completed ✓"
                "rejected"               -> "Refund Rejected"
                else                     -> event.refundStatus ?: ""
            }
            val refundColor = when (event.refundStatus) {
                "pending"                -> R.color.yellow_accent
                "waiting_acknowledgement"-> R.color.purple_light
                "completed"              -> R.color.success_green
                "rejected"              -> R.color.red_accent
                else                    -> R.color.text_muted
            }
            binding.tvRefundStatus.text = refundLabel
            binding.tvRefundStatus.setTextColor(
                ContextCompat.getColor(requireContext(), refundColor)
            )
        }

        // ── Action buttons ────────────────────────────────────────────────
        setupActionButtons(event)
    }

    private fun setupActionButtons(event: EventItem) {
        val eventId        = event.id ?: return
        val isCancelled    = event.status == "cancelled"
        val isRejected     = event.attendeeStatus == "rejected" ||
                event.rsvpPaymentStatus == "rejected"
        val isConfirmed    = event.rsvpPaymentStatus == "confirmed" ||
                (event.status == "confirmed" && event.rsvpPaymentStatus != "pending")
        val isPending      = event.rsvpPaymentStatus == "pending"
        val noProof        = event.rsvpPaymentStatus == null && (event.price ?: 0.0) > 0.0
        val noRefundPolicy = event.noRefundPolicy == true
        val refundStatus   = event.refundStatus
        val isEventActive  = event.eventStatus == "active" || event.eventStatus == null

        // Upload proof — show when paid + no proof yet or payment rejected
        val canUploadProof = (event.price ?: 0.0) > 0.0 &&
                !isCancelled && !isRejected &&
                (noProof || event.rsvpPaymentStatus == "rejected")
        binding.btnUploadProof.showIf(canUploadProof)
        binding.btnUploadProof.setOnClickListener {
            PaymentProofBottomSheet.newInstance(
                event     = event,
                onSuccess = {
                    toast("Proof submitted! Awaiting host approval.")
                    parentFragmentManager.popBackStack()
                    onBackCallback?.invoke()
                }
            ).show(parentFragmentManager, "payment_proof")
        }

        // Cancel RSVP — show when not already cancelled/rejected and event is active
        val canCancel = !isCancelled && !isRejected && isEventActive &&
                refundStatus.isNullOrBlank()
        binding.btnCancelRsvp.showIf(canCancel)
        binding.btnCancelRsvp.setOnClickListener {
            promptCancelRsvp(eventId)
        }

        // Request refund — confirmed + paid + not already refunded + no-refund policy off
        val canRefund = isConfirmed && (event.price ?: 0.0) > 0.0 &&
                !noRefundPolicy && isEventActive &&
                (refundStatus.isNullOrBlank() || refundStatus == "rejected")
        binding.btnRequestRefund.showIf(canRefund)
        binding.btnRequestRefund.setOnClickListener {
            promptRequestRefund(eventId)
        }

        // Acknowledge refund — only when host has processed it
        val canAcknowledge = refundStatus == "waiting_acknowledgement"
        binding.btnAcknowledgeRefund.showIf(canAcknowledge)
        binding.btnAcknowledgeRefund.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Acknowledge Refund")
                .setMessage("Confirm that you have received your refund?")
                .setPositiveButton("Yes, I received it") { _, _ ->
                    presenter.acknowledgeRefund(eventId)
                }
                .setNegativeButton("Not yet", null)
                .show()
        }
    }

    // ── Contract callbacks ────────────────────────────────────────────────

    override fun showLoading(show: Boolean) {
        binding.progressBar.showIf(show)
    }

    override fun showMessage(message: String) = toast(message)

    override fun onCancelSuccess() {
        toast("RSVP cancelled.")
        parentFragmentManager.popBackStack()
        onBackCallback?.invoke()
    }

    override fun onRefundRequested() {
        toast("Refund requested. The host will review it.")
        parentFragmentManager.popBackStack()
        onBackCallback?.invoke()
    }

    override fun onRefundAcknowledged() {
        toast("Refund acknowledged. Thank you!")
        parentFragmentManager.popBackStack()
        onBackCallback?.invoke()
    }

    // ── Dialogs ───────────────────────────────────────────────────────────

    private fun promptCancelRsvp(eventId: Long) {
        AlertDialog.Builder(requireContext())
            .setTitle("Cancel RSVP")
            .setMessage("Are you sure you want to cancel your spot? This cannot be undone.")
            .setPositiveButton("Yes, Cancel") { _, _ -> presenter.cancelRsvp(eventId) }
            .setNegativeButton("Keep RSVP", null)
            .show()
    }

    private fun promptRequestRefund(eventId: Long) {
        val input = EditText(requireContext()).apply {
            hint = "Reason for refund request"
            setPadding(48, 24, 48, 24)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Request Refund")
            .setMessage("Please provide a reason:")
            .setView(input)
            .setPositiveButton("Submit") { _, _ ->
                val reason = input.text.toString().trim()
                    .ifBlank { "Refund requested by attendee." }
                presenter.requestRefund(eventId, reason)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun resolveStatusDisplay(event: EventItem): Pair<String, Int> = when {
        event.status         == "cancelled"              -> "Cancelled"             to R.color.text_muted
        event.attendeeStatus == "rejected"               -> "Rejected by Host"      to R.color.red_accent
        event.rsvpPaymentStatus == "rejected"            -> "Payment Rejected"      to R.color.red_accent
        event.attendeeStatus == "attended"               -> "Attended ✓"            to R.color.success_green
        event.eventStatus    == "completed"              -> "Event Completed"       to R.color.purple_light
        event.rsvpPaymentStatus == "pending"             -> "Pending Payment Approval" to R.color.yellow_accent
        event.rsvpPaymentStatus == "confirmed"           -> "Confirmed ✓"           to R.color.success_green
        event.status         == "confirmed"              -> "Confirmed ✓"           to R.color.success_green
        event.refundStatus   == "waiting_acknowledgement"-> "Refund Processed"      to R.color.purple_light
        event.refundStatus   == "completed"              -> "Refund Completed"      to R.color.success_green
        else                                             -> "Registered"            to R.color.purple_main
    }

    private fun formatTime(event: EventItem): String {
        val start = event.startTime ?: event.time ?: return "Time TBD"
        val end   = event.endTime
        return if (!end.isNullOrBlank()) "$start – $end" else start
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.detachView()
        _binding = null
    }
}