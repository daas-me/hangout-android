package com.hangout.app.ui.hostdashboard

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.hangout.app.R
import com.hangout.app.data.AttendeeItem
import com.hangout.app.data.EventItem
import com.hangout.app.databinding.FragmentHostDashboardBinding
import com.hangout.app.databinding.ItemAttendeeRowBinding
import com.hangout.app.utils.EventHolder
import com.hangout.app.utils.hide
import com.hangout.app.utils.show
import com.hangout.app.utils.showIf
import com.hangout.app.utils.toast

class HostDashboardFragment : Fragment(), HostDashboardContract.View {

    private var _binding: FragmentHostDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var presenter: HostDashboardContract.Presenter

    private var event: EventItem? = null
    var onBackCallback: (() -> Unit)? = null

    companion object {
        fun newInstance(onBack: (() -> Unit)? = null) =
            HostDashboardFragment().apply { onBackCallback = onBack }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        if (!::presenter.isInitialized) {
            presenter = HostDashboardPresenter(this, HostDashboardModel(requireContext()))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHostDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        event = EventHolder.currentEvent ?: return

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
            onBackCallback?.invoke()
        }

        event?.let { e ->
            showEvent(e)
            e.id?.let { presenter.loadAttendees(it) }
        }

        binding.btnCancelEvent.setOnClickListener { promptCancelEvent() }
        binding.btnDeleteEvent.setOnClickListener { promptDeleteEvent() }
        binding.btnRefresh.setOnClickListener {
            event?.id?.let { presenter.loadAttendees(it) }
        }
    }

    // ── HostDashboardContract.View ────────────────────────────────────────

    override fun showEvent(event: EventItem) {
        this.event = event
        binding.tvEventTitle.text = event.title ?: "Untitled"
        binding.tvEventMeta.text  = "${event.date ?: "—"} · ${event.startTime ?: event.time ?: "—"}"
        binding.tvEventLocation.text = event.location ?: "Virtual / TBD"

        val total   = event.capacity ?: 0
        val current = event.attendeeCount ?: 0
        binding.tvSlots.text = "$current / $total attendees"
        if (total > 0) {
            binding.progressSlots.max      = total
            binding.progressSlots.progress = current
        }

        val isCancelled = event.eventStatus == "cancelled"
        binding.btnCancelEvent.showIf(!isCancelled)
        binding.tvCancelledBanner.showIf(isCancelled)
    }

    override fun showAttendees(attendees: List<AttendeeItem>) {
        binding.layoutAttendeesContainer.removeAllViews()

        if (attendees.isEmpty()) {
            binding.layoutAttendeesContainer.addView(emptyText("No RSVPs yet."))
            return
        }

        // Summary counts
        val pending   = attendees.count { it.paymentStatus == "pending" }
        val confirmed = attendees.count {
            it.paymentStatus == "confirmed" || it.status == "confirmed"
        }
        val cancelled = attendees.count { it.status == "cancelled" }
        binding.tvSummary.text = "Pending: $pending · Confirmed: $confirmed · Cancelled: $cancelled"
        binding.tvSummary.show()

        attendees.forEach { attendee ->
            val row = ItemAttendeeRowBinding.inflate(
                layoutInflater, binding.layoutAttendeesContainer, false
            )
            bindAttendeeRow(row, attendee)
            binding.layoutAttendeesContainer.addView(row.root)
        }
    }

    override fun showLoading(show: Boolean) {
        binding.progressBar.showIf(show)
    }

    override fun showMessage(message: String) = toast(message)

    override fun onActionSuccess(rsvpId: Long, newStatus: String) {
        // Refresh attendee list after any action
        event?.id?.let { presenter.loadAttendees(it) }
    }

    override fun onEventCancelled() {
        toast("Event cancelled.")
        binding.tvCancelledBanner.show()
        binding.btnCancelEvent.hide()
        onBackCallback?.invoke()
        parentFragmentManager.popBackStack()
    }

    override fun onEventDeleted() {
        toast("Event deleted.")
        onBackCallback?.invoke()
        parentFragmentManager.popBackStack()
    }

    // ── Attendee row binding ──────────────────────────────────────────────

    private fun bindAttendeeRow(row: ItemAttendeeRowBinding, attendee: AttendeeItem) {
        val ctx = requireContext()

        row.tvAttendeeName.text =
            "${attendee.firstName ?: ""} ${attendee.lastName ?: ""}".trim()
                .ifBlank { attendee.email ?: "Unknown" }
        row.tvAttendeeEmail.text = attendee.email ?: ""

        // Avatar
        if (!attendee.photo.isNullOrBlank()) {
            Glide.with(this).load(attendee.photo).circleCrop().into(row.ivAvatar)
        }

        // Status badge
        val (label, color) = resolveStatus(attendee)
        row.tvStatusBadge.text = label
        row.tvStatusBadge.setTextColor(ContextCompat.getColor(ctx, color))

        // Seat number
        if (!attendee.seatNumber.isNullOrBlank()) {
            row.tvSeatNumber.text = "Seat ${attendee.seatNumber}"
            row.tvSeatNumber.show()
        } else {
            row.tvSeatNumber.hide()
        }

        // Payment proof thumbnail
        val hasProof = !attendee.paymentProofUrl.isNullOrBlank()
        row.btnViewProof.showIf(hasProof)
        row.btnViewProof.setOnClickListener {
            showPaymentProofDialog(attendee)
        }

        // Action buttons
        setupActionButtons(row, attendee)
    }

    private fun resolveStatus(a: AttendeeItem): Pair<String, Int> = when {
        a.status == "cancelled"         -> "Cancelled"    to R.color.text_muted
        a.attendeeStatus == "rejected"  -> "Rejected"     to R.color.red_accent
        a.attendeeStatus == "attended"  -> "Attended ✓"   to R.color.success_green
        a.paymentStatus  == "rejected"  -> "Pay Rejected" to R.color.red_accent
        a.paymentStatus  == "pending"   -> "Pending Pay"  to R.color.yellow_accent
        a.paymentStatus  == "confirmed" -> "Confirmed"    to R.color.success_green
        a.status         == "confirmed" -> "Confirmed"    to R.color.success_green
        else                            -> "Registered"   to R.color.purple_light
    }

    private fun setupActionButtons(row: ItemAttendeeRowBinding, attendee: AttendeeItem) {
        val eventId = event?.id ?: return
        val rsvpId  = attendee.id

        // Show approve/reject only when payment is pending
        val isPending = attendee.paymentStatus == "pending"
        row.btnApprove.showIf(isPending)
        row.btnReject.showIf(isPending)

        // Show assign seat when confirmed + seating is reserved + no seat yet
        val isReservedSeating = event?.seatingType == "reserved"
        val canAssignSeat = attendee.paymentStatus == "confirmed" &&
                isReservedSeating &&
                attendee.seatNumber.isNullOrBlank()
        row.btnAssignSeat.showIf(canAssignSeat)

        row.btnApprove.setOnClickListener {
            if (isReservedSeating) {
                promptApproveWithSeat(eventId, rsvpId)
            } else {
                AlertDialog.Builder(requireContext())
                    .setTitle("Approve Payment")
                    .setMessage("Approve this payment?")
                    .setPositiveButton("Approve") { _, _ ->
                        presenter.approvePayment(eventId, rsvpId, null)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }

        row.btnReject.setOnClickListener {
            promptRejectWithReason { reason ->
                presenter.rejectPayment(eventId, rsvpId, reason)
            }
        }

        row.btnAssignSeat.setOnClickListener {
            promptAssignSeat(eventId, rsvpId)
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────

    private fun showPaymentProofDialog(attendee: AttendeeItem) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_payment_proof, null)
        val imageView = dialogView.findViewById<ImageView>(R.id.ivProofImage)
        Glide.with(this)
            .load(attendee.paymentProofUrl)
            .into(imageView)

        AlertDialog.Builder(requireContext())
            .setTitle("Payment Proof")
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun promptApproveWithSeat(eventId: Long, rsvpId: Long) {
        val input = EditText(requireContext()).apply {
            hint = "Seat number (e.g. A1)"
            setPadding(48, 24, 48, 24)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Approve Payment")
            .setMessage("Assign a seat number (optional):")
            .setView(input)
            .setPositiveButton("Approve") { _, _ ->
                val seat = input.text.toString().trim().ifBlank { null }
                presenter.approvePayment(eventId, rsvpId, seat)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun promptRejectWithReason(onConfirm: (String) -> Unit) {
        val input = EditText(requireContext()).apply {
            hint = "Reason for rejection"
            setPadding(48, 24, 48, 24)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Reject Payment")
            .setView(input)
            .setPositiveButton("Reject") { _, _ ->
                val reason = input.text.toString().trim().ifBlank { "Payment rejected by host." }
                onConfirm(reason)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun promptAssignSeat(eventId: Long, rsvpId: Long) {
        val input = EditText(requireContext()).apply {
            hint = "Seat number (e.g. B3)"
            setPadding(48, 24, 48, 24)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Assign Seat")
            .setView(input)
            .setPositiveButton("Assign") { _, _ ->
                val seat = input.text.toString().trim()
                if (seat.isNotBlank()) presenter.assignSeat(eventId, rsvpId, seat)
                else toast("Please enter a seat number.")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun promptCancelEvent() {
        val input = EditText(requireContext()).apply {
            hint = "Reason for cancellation"
            setPadding(48, 24, 48, 24)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Cancel Event")
            .setMessage("This will notify all attendees. Reason:")
            .setView(input)
            .setPositiveButton("Cancel Event") { _, _ ->
                val reason = input.text.toString().trim().ifBlank { "Event cancelled by host." }
                event?.id?.let { presenter.cancelEvent(it, reason) }
            }
            .setNegativeButton("Keep Event", null)
            .show()
    }

    private fun promptDeleteEvent() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Event")
            .setMessage("Permanently delete this event? This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                event?.id?.let { presenter.deleteEvent(it) }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun emptyText(msg: String) = TextView(requireContext()).apply {
        text = msg
        setTextColor(resources.getColor(R.color.text_muted, null))
        textSize = 14f
        gravity = android.view.Gravity.CENTER
        setPadding(0, 64, 0, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.detachView()
        _binding = null
    }
}