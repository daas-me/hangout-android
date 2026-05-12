package com.hangout.app.ui.myhangouts

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.hangout.app.R
import com.hangout.app.data.EventItem
import com.hangout.app.databinding.FragmentMyHangoutsBinding
import com.hangout.app.databinding.ItemAttendingCardBinding
import com.hangout.app.ui.attendingdashboard.AttendingDashboardFragment
import com.hangout.app.ui.eventdetail.EventDetailFragment
import com.hangout.app.ui.hostdashboard.HostDashboardFragment
import com.hangout.app.utils.EventHolder
import com.hangout.app.utils.color
import com.hangout.app.utils.hide
import com.hangout.app.utils.show
import com.hangout.app.utils.showIf
import com.hangout.app.utils.toast

class MyHangoutsFragment : Fragment(), MyHangoutsContract.View {

    private var _binding: FragmentMyHangoutsBinding? = null
    private val binding get() = _binding!!
    private lateinit var presenter: MyHangoutsContract.Presenter

    private var showingHosting = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        if (!::presenter.isInitialized) {
            presenter = MyHangoutsPresenter(this, MyHangoutsModel(requireContext()))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyHangoutsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTabs()
        showHostingTab()
    }

    // ── Tab setup ──────────────────────────────────────────────────────────

    private fun setupTabs() {
        binding.btnTabHosting.setOnClickListener {
            if (!showingHosting) showHostingTab()
        }
        binding.btnTabAttending.setOnClickListener {
            if (showingHosting) showAttendingTab()
        }
    }

    private fun showHostingTab() {
        showingHosting = true
        binding.btnTabHosting.setBackgroundResource(R.drawable.tab_active_bg)
        binding.btnTabHosting.setTextColor(color(R.color.white))
        binding.btnTabAttending.setBackgroundResource(android.R.color.transparent)
        binding.btnTabAttending.setTextColor(color(R.color.text_muted))
        presenter.loadHosting()
    }

    private fun showAttendingTab() {
        showingHosting = false
        binding.btnTabAttending.setBackgroundResource(R.drawable.tab_active_bg)
        binding.btnTabAttending.setTextColor(color(R.color.white))
        binding.btnTabHosting.setBackgroundResource(android.R.color.transparent)
        binding.btnTabHosting.setTextColor(color(R.color.text_muted))
        presenter.loadAttending()
    }

    // ── MyHangoutsContract.View ────────────────────────────────────────────

    override fun showLoading(show: Boolean) {
        binding.progressBar.showIf(show)
    }

    override fun showError(message: String) {
        toast(message)
    }

    override fun showAttendingEvents(events: List<EventItem>) {
        binding.layoutEventsContainer.removeAllViews()
        binding.layoutEventsContainer.addView(binding.progressBar)

        if (events.isEmpty()) {
            binding.layoutEventsContainer.addView(emptyText("You haven't RSVP'd to any HangOuts yet."))
            return
        }

        events.forEach { event -> binding.layoutEventsContainer.addView(buildAttendingCard(event)) }
    }

    override fun showHostingEvents(events: List<EventItem>) {
        binding.layoutEventsContainer.removeAllViews()

        if (events.isEmpty()) {
            binding.layoutEventsContainer.addView(emptyText("You haven't created any HangOuts yet."))
            return
        }

        // Show all events — drafts included
        events.forEach { event ->
            binding.layoutEventsContainer.addView(buildHostingCard(event))
        }
    }

    override fun onCancelSuccess(eventId: Long) {
        toast("RSVP cancelled.")
        presenter.loadAttending()
    }

    // ── Card builders ──────────────────────────────────────────────────────

    private fun buildAttendingCard(event: EventItem): View {
        val card = ItemAttendingCardBinding.inflate(layoutInflater, binding.layoutEventsContainer, false)

        card.tvEventTitle.text = event.title ?: "Untitled"
        card.tvDate.text       = "${event.date ?: "—"} • ${event.startTime ?: event.time ?: "—"}"
        card.tvLocation.text   = if (event.location.isNullOrBlank()) "Virtual / TBD" else event.location
        card.tvPrice.text      = if ((event.price ?: 0.0) == 0.0) "Free" else "₱${event.price?.toInt()}"

        val status = deriveAttendingStatus(event)
        applyStatusStyle(card, event, status)

        // Ticket info — show only when confirmed
        if (status == AttendingStatus.CONFIRMED) {
            card.layoutTicketInfo.show()
            card.tvTicketNumber.text = event.ticketNumber ?: "TKT-${event.id}"
            card.tvSeatNumber.text   = event.seatNumber ?: "Open"
        } else {
            card.layoutTicketInfo.hide()
        }

        // Cancel button — only for active, non-cancelled, non-rejected RSVPs
        val canCancel = status == AttendingStatus.CONFIRMED || status == AttendingStatus.PENDING
        card.btnCancelRsvp.showIf(canCancel)
        card.btnCancelRsvp.setOnClickListener {
            event.id?.let { id -> confirmCancel(id) }
        }

        card.btnViewDetails.setOnClickListener {
            EventHolder.currentEvent = event
            val fragment = AttendingDashboardFragment.newInstance(
                onBack = { presenter.loadAttending() }
            )
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .addToBackStack(null)
                .commit()
        }

        return card.root
    }

    private fun buildHostingCard(event: EventItem): View {
        val card = ItemAttendingCardBinding.inflate(layoutInflater, binding.layoutEventsContainer, false)

        card.tvEventTitle.text = event.title ?: "Untitled"
        card.tvDate.text       = "${event.date ?: "—"} • ${event.startTime ?: event.time ?: "—"}"
        card.tvLocation.text   = if (event.location.isNullOrBlank()) "Virtual / TBD" else event.location
        card.tvPrice.text      = if ((event.price ?: 0.0) == 0.0) "Free" else "₱${event.price?.toInt()}"

        val isDraft     = event.isDraft == true
        val isCancelled = event.eventStatus == "cancelled"
        val isCompleted = event.eventStatus == "completed"

        card.tvStatusBadge.text = when {
            isDraft     -> "Draft"
            isCancelled -> "Cancelled"
            isCompleted -> "Completed"
            else        -> "Published"
        }

        val badgeColor = when {
            isDraft     -> ContextCompat.getColor(requireContext(), R.color.yellow_accent)
            isCancelled -> ContextCompat.getColor(requireContext(), R.color.red_accent)
            isCompleted -> ContextCompat.getColor(requireContext(), R.color.text_muted)
            else        -> ContextCompat.getColor(requireContext(), R.color.success_green)
        }
        card.tvStatusBadge.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        card.tvStatusBadge.setTextColor(badgeColor)

        card.viewStatusStripe.setBackgroundColor(
            when {
                isDraft     -> ContextCompat.getColor(requireContext(), R.color.yellow_accent)
                isCancelled -> ContextCompat.getColor(requireContext(), R.color.red_accent)
                isCompleted -> ContextCompat.getColor(requireContext(), R.color.text_muted)
                else        -> ContextCompat.getColor(requireContext(), R.color.purple_main)
            }
        )

        card.layoutTicketInfo.hide()
        card.btnCancelRsvp.hide()

        card.btnViewDetails.text = if (isDraft) "Edit Draft" else "Manage"
        card.btnViewDetails.setOnClickListener {
            if (isDraft) {
                startActivity(
                    android.content.Intent(requireContext(),
                        com.hangout.app.ui.createevent.CreateEventActivity::class.java)
                )
            } else {
                EventHolder.currentEvent = event
                val fragment = HostDashboardFragment.newInstance(
                    onBack = { presenter.loadHosting() }
                )
                parentFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        }

        return card.root
    }

    // ── Status helpers ─────────────────────────────────────────────────────

    private enum class AttendingStatus {
        CONFIRMED, PENDING, REJECTED, CANCELLED, COMPLETED
    }

    private fun deriveAttendingStatus(event: EventItem): AttendingStatus {
        val rsvpStatus     = event.status
        val paymentStatus  = event.rsvpPaymentStatus
        val attendeeStatus = event.attendeeStatus
        val eventStatus    = event.eventStatus

        return when {
            rsvpStatus    == "cancelled"  -> AttendingStatus.CANCELLED
            attendeeStatus == "rejected"  -> AttendingStatus.REJECTED
            paymentStatus  == "rejected"  -> AttendingStatus.REJECTED
            eventStatus    == "completed" -> AttendingStatus.COMPLETED
            paymentStatus  == "pending"   -> AttendingStatus.PENDING
            rsvpStatus     == "confirmed" -> AttendingStatus.CONFIRMED
            paymentStatus  == "confirmed" -> AttendingStatus.CONFIRMED
            else                          -> AttendingStatus.CONFIRMED
        }
    }

    private fun applyStatusStyle(
        card: ItemAttendingCardBinding,
        event: EventItem,
        status: AttendingStatus
    ) {
        val ctx = requireContext()
        when (status) {
            AttendingStatus.CONFIRMED -> {
                card.tvStatusBadge.text = "Confirmed ✓"
                card.tvStatusBadge.setTextColor(ContextCompat.getColor(ctx, R.color.success_green))
                card.viewStatusStripe.setBackgroundColor(ContextCompat.getColor(ctx, R.color.success_green))
            }
            AttendingStatus.PENDING -> {
                card.tvStatusBadge.text = "Pending"
                card.tvStatusBadge.setTextColor(ContextCompat.getColor(ctx, R.color.yellow_accent))
                card.viewStatusStripe.setBackgroundColor(ContextCompat.getColor(ctx, R.color.yellow_accent))
            }
            AttendingStatus.REJECTED -> {
                card.tvStatusBadge.text = "Rejected"
                card.tvStatusBadge.setTextColor(ContextCompat.getColor(ctx, R.color.red_accent))
                card.viewStatusStripe.setBackgroundColor(ContextCompat.getColor(ctx, R.color.red_accent))
            }
            AttendingStatus.CANCELLED -> {
                card.tvStatusBadge.text = "Cancelled"
                card.tvStatusBadge.setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
                card.viewStatusStripe.setBackgroundColor(ContextCompat.getColor(ctx, R.color.text_muted))
            }
            AttendingStatus.COMPLETED -> {
                card.tvStatusBadge.text = "Completed"
                card.tvStatusBadge.setTextColor(ContextCompat.getColor(ctx, R.color.purple_light))
                card.viewStatusStripe.setBackgroundColor(ContextCompat.getColor(ctx, R.color.purple_light))
            }
        }
        // Clear badge background so only the text color shows
        card.tvStatusBadge.setBackgroundColor(android.graphics.Color.TRANSPARENT)
    }

    // ── Actions ────────────────────────────────────────────────────────────

    private fun confirmCancel(eventId: Long) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Cancel RSVP")
            .setMessage("Are you sure you want to cancel your spot?")
            .setPositiveButton("Yes, Cancel") { _, _ -> presenter.cancelRsvp(eventId) }
            .setNegativeButton("Keep RSVP", null)
            .show()
    }

    private fun openEventDetail(event: EventItem) {
        EventHolder.currentEvent = event
        val fragment = EventDetailFragment.newInstance(
            onBack = {
                if (showingHosting) presenter.loadHosting()
                else presenter.loadAttending()
            }
        )
        parentFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .addToBackStack(null)
            .commit()
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun emptyText(msg: String) = TextView(requireContext()).apply {
        text = msg
        setTextColor(resources.getColor(R.color.text_muted, null))
        textSize = 14f
        gravity = Gravity.CENTER
        setPadding(0, 64, 0, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.detachView()
        _binding = null
    }
}