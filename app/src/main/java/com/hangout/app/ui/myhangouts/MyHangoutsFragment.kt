package com.hangout.app.ui.myhangouts

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.hangout.app.R
import com.hangout.app.data.EventItem
import com.hangout.app.databinding.FragmentMyHangoutsBinding
import com.hangout.app.databinding.ItemHostingCardBinding
import com.hangout.app.utils.createStyledPopupMenu
import com.hangout.app.databinding.ItemAttendingCardMyhangoutsBinding
import com.hangout.app.databinding.ItemFavoritesCardBinding
import com.hangout.app.ui.attendingdashboard.AttendingDashboardFragment
import com.hangout.app.ui.components.createStyledAlertDialog

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

    private enum class ActiveTab { HOSTING, ATTENDING, FAVORITES }
    private var activeTab = ActiveTab.HOSTING
    
    private var hostingEvents: List<EventItem> = emptyList()
    private var attendingEvents: List<EventItem> = emptyList()
    private var favoriteEvents: List<EventItem> = emptyList()

    // Filter states
    private enum class HostingFilterType { PUBLISHED, DRAFT, COMPLETED }
    private enum class AttendingFilterType { CONFIRMED, PENDING, REJECTED, CANCELLED, COMPLETED }
    
    private var hostingFilterType = HostingFilterType.PUBLISHED
    private var attendingFilterType = AttendingFilterType.CONFIRMED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
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
        
        setupSearch()
        setupTabs()
        setupFilters()

        activateTab(ActiveTab.HOSTING)
        
        // Load initial data only if cache is empty
        if (hostingEvents.isEmpty()) {
            presenter.loadHosting()
        }
    }

    // ── Contract.View ──────────────────────────────────────────────────────

    override fun showLoading(show: Boolean) {
        binding.progressBar.showIf(show)
    }

    override fun showError(message: String) {
        toast(message)
    }

    // ── Search ─────────────────────────────────────────────────────────────

    private fun setupSearch() {
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val imm = requireContext().getSystemService(InputMethodManager::class.java)
                imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
                filterAndDisplayCurrentTab()
                true
            } else false
        }
    }

    private fun filterAndDisplayCurrentTab() {
        val query = binding.etSearch.text.toString().lowercase()
        when (activeTab) {
            ActiveTab.HOSTING -> {
                val filtered = hostingEvents.filter { event ->
                    (event.title?.lowercase()?.contains(query) == true) && when (hostingFilterType) {
                        HostingFilterType.PUBLISHED -> event.isDraft != true && event.eventStatus != "completed"
                        HostingFilterType.DRAFT -> event.isDraft == true
                        HostingFilterType.COMPLETED -> event.eventStatus == "completed"
                    }
                }
                clearContainer()
                if (filtered.isEmpty()) {
                    showEmpty("No results found.")
                    return
                }
                filtered.forEach { binding.layoutEventsContainer.addView(buildHostingCard(it)) }
            }
            ActiveTab.ATTENDING -> {
                val filtered = attendingEvents.filter { event ->
                    val status = deriveAttendingStatus(event)
                    (event.title?.lowercase()?.contains(query) == true) && when (attendingFilterType) {
                        AttendingFilterType.CONFIRMED -> status == AttendingStatus.CONFIRMED
                        AttendingFilterType.PENDING -> status == AttendingStatus.PENDING
                        AttendingFilterType.REJECTED -> status == AttendingStatus.REJECTED
                        AttendingFilterType.CANCELLED -> status == AttendingStatus.CANCELLED
                        AttendingFilterType.COMPLETED -> status == AttendingStatus.COMPLETED
                    }
                }
                clearContainer()
                if (filtered.isEmpty()) {
                    showEmpty("No results found.")
                    return
                }
                filtered.forEach { binding.layoutEventsContainer.addView(buildAttendingCard(it)) }
            }
            ActiveTab.FAVORITES -> {
                val filtered = favoriteEvents.filter { 
                    it.title?.lowercase()?.contains(query) == true 
                }
                clearContainer()
                if (filtered.isEmpty()) {
                    showEmpty("No results found.")
                    return
                }
                filtered.forEach { binding.layoutEventsContainer.addView(buildFavoriteCard(it)) }
            }
        }
    }

    // ── Tabs ───────────────────────────────────────────────────────────────

    private fun setupTabs() {
        binding.btnTabHosting.setOnClickListener   { activateTab(ActiveTab.HOSTING)   }
        binding.btnTabAttending.setOnClickListener { activateTab(ActiveTab.ATTENDING) }
        binding.btnTabFavorites.setOnClickListener { activateTab(ActiveTab.FAVORITES) }
    }

    private fun setupFilters() {
        binding.filterSpinner.setOnItemClickListener { _, _, position, _ ->
            when (activeTab) {
                ActiveTab.HOSTING -> {
                    hostingFilterType = HostingFilterType.values()[position]
                    filterAndDisplayHosting()
                }
                ActiveTab.ATTENDING -> {
                    attendingFilterType = AttendingFilterType.values()[position]
                    filterAndDisplayAttending()
                }
                else -> {}
            }
        }
    }

    private fun updateTabCounts() {
        val published = hostingEvents.filter { it.isDraft != true }
        binding.btnTabHosting.text = "Hosting (${published.size})"
        binding.btnTabAttending.text = "Attending (${attendingEvents.size})"
        binding.btnTabFavorites.text = "♥ Favorites (${favoriteEvents.size})"
    }

    private fun activateTab(tab: ActiveTab) {
        activeTab = tab
        binding.etSearch.setText("")

        // reset all tabs
        listOf(binding.btnTabHosting, binding.btnTabAttending, binding.btnTabFavorites).forEach {
            it.setBackgroundResource(android.R.color.transparent)
            it.setTextColor(color(R.color.text_muted))
        }

        // activate selected
        val activeBtn = when (tab) {
            ActiveTab.HOSTING   -> binding.btnTabHosting
            ActiveTab.ATTENDING -> binding.btnTabAttending
            ActiveTab.FAVORITES -> binding.btnTabFavorites
        }
        activeBtn.setBackgroundResource(R.drawable.tab_active_bg)
        activeBtn.setTextColor(color(R.color.white))
        
        // Setup filter spinner based on tab
        when (tab) {
            ActiveTab.HOSTING -> {
                binding.filterContainer.show()
                setupHostingFilter()
                if (hostingEvents.isEmpty()) presenter.loadHosting()
                else filterAndDisplayHosting()
            }
            ActiveTab.ATTENDING -> {
                binding.filterContainer.show()
                setupAttendingFilter()
                if (attendingEvents.isEmpty()) presenter.loadAttending()
                else filterAndDisplayAttending()
            }
            ActiveTab.FAVORITES -> {
                binding.filterContainer.hide()
                if (favoriteEvents.isEmpty()) presenter.loadFavorites()
                else showFavoriteEvents(favoriteEvents)
            }
        }
    }

    private fun setupHostingFilter() {
        val options = listOf("Published", "Draft", "Completed")
        val adapter = ArrayAdapter(requireContext(), R.layout.dropdown_item_white_text, options)
        binding.filterSpinner.setAdapter(adapter)
        binding.filterSpinner.setText(options[hostingFilterType.ordinal], false)
    }

    private fun setupAttendingFilter() {
        val options = listOf("Confirmed", "Pending", "Rejected", "Cancelled", "Completed")
        val adapter = ArrayAdapter(requireContext(), R.layout.dropdown_item_white_text, options)
        binding.filterSpinner.setAdapter(adapter)
        binding.filterSpinner.setText(options[attendingFilterType.ordinal], false)
    }

    override fun showHostingEvents(events: List<EventItem>) {
        hostingEvents = events
        updateTabCounts()
        filterAndDisplayHosting()
    }

    override fun showAttendingEvents(events: List<EventItem>) {
        attendingEvents = events
        updateTabCounts()
        filterAndDisplayAttending()
    }

    private fun filterAndDisplayHosting() {
        clearContainer()
        val filtered = hostingEvents.filter { event ->
            when (hostingFilterType) {
                HostingFilterType.PUBLISHED -> event.isDraft != true && event.eventStatus != "completed"
                HostingFilterType.DRAFT -> event.isDraft == true
                HostingFilterType.COMPLETED -> event.eventStatus == "completed"
            }
        }
        if (filtered.isEmpty()) {
            showEmpty("No ${hostingFilterType.name.lowercase()} HangOuts found.")
            return
        }
        filtered.forEach { binding.layoutEventsContainer.addView(buildHostingCard(it)) }
    }

    private fun filterAndDisplayAttending() {
        clearContainer()
        val filtered = attendingEvents.filter { event ->
            val status = deriveAttendingStatus(event)
            when (attendingFilterType) {
                AttendingFilterType.CONFIRMED -> status == AttendingStatus.CONFIRMED
                AttendingFilterType.PENDING -> status == AttendingStatus.PENDING
                AttendingFilterType.REJECTED -> status == AttendingStatus.REJECTED
                AttendingFilterType.CANCELLED -> status == AttendingStatus.CANCELLED
                AttendingFilterType.COMPLETED -> status == AttendingStatus.COMPLETED
            }
        }
        if (filtered.isEmpty()) {
            showEmpty("No ${attendingFilterType.name.lowercase()} HangOuts found.")
            return
        }
        filtered.forEach { binding.layoutEventsContainer.addView(buildAttendingCard(it)) }
    }

    override fun showFavoriteEvents(events: List<EventItem>) {
        favoriteEvents = events
        updateTabCounts()
        clearContainer()
        if (events.isEmpty()) {
            showEmpty("No saved HangOuts yet.\nTap ♥ on any event to save it.")
            return
        }
        events.forEach { binding.layoutEventsContainer.addView(buildFavoriteCard(it)) }
    }

    override fun onCancelSuccess(eventId: Long) {
        toast("RSVP cancelled.")
        presenter.loadAttending()
    }

    override fun onUnfavoriteSuccess(eventId: Long) {
        toast("Removed from saved.")
        presenter.loadFavorites()
    }

    // ── Card builders ──────────────────────────────────────────────────────

    private fun buildHostingCard(event: EventItem): View {
        val card = ItemHostingCardBinding.inflate(
            layoutInflater, binding.layoutEventsContainer, false
        )
        
        card.tvEventTitle.text    = event.title ?: "Untitled"
        card.tvDate.text          = "${event.date ?: "—"} • ${formatTimeRange(event)}"
        card.tvLocation.text      = event.location?.ifBlank { "Virtual / TBD" } ?: "Virtual / TBD"
        card.tvEventFormat.text   = event.format ?: "In-Person"
        card.tvAttendees.text     = "${event.attendeeCount ?: 0}/${event.capacity ?: 0} attending"
        card.tvPrice.text         = if ((event.price ?: 0.0) == 0.0) "Free" else "₱${event.price?.toInt()}"

        // Load event image
        if (!event.imageUrl.isNullOrBlank()) {
            Glide.with(this).load(event.imageUrl).centerCrop().into(card.ivEventImage)
        }

        // Status badge
        val isCompleted = event.eventStatus == "completed"
        val isDraft = event.isDraft == true
        card.tvStatusBadge.text = when {
            isCompleted -> "Completed"
            isDraft -> "Draft"
            else -> "Published"
        }
        card.tvStatusBadge.setBackgroundResource(
            when {
                isCompleted -> R.drawable.status_pill_gray
                isDraft -> R.drawable.status_pill_yellow
                else -> R.drawable.status_pill_green
            }
        )
        card.tvStatusBadge.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.white)
        )

        card.btnManageEvent.setOnClickListener {
            EventHolder.currentEvent = event
            val tab = activeTab
            val fragment = HostDashboardFragment.newInstance(
                onBack = { if (_binding != null) activateTab(tab) }
            )
            parentFragmentManager.beginTransaction()
                .add(R.id.nav_host_fragment, fragment)  // ← add not replace
                .addToBackStack(null)
                .commit()
        }

        card.btnEditEvent.setOnClickListener {
            val intent = Intent(requireContext(), com.hangout.app.ui.createevent.CreateEventActivity::class.java).apply {
                putExtra("eventId", event.id)
                putExtra("isEdit", true)
            }
            startActivity(intent)
        }

        card.btnMoreOptions.setOnClickListener { anchor ->
            showHostEventMenu(anchor, event)
        }

        return card.root
    }

    private fun showHostEventMenu(anchor: View, event: EventItem) {
        val popup = createStyledPopupMenu(requireContext(), anchor)
        popup.menuInflater.inflate(R.menu.host_event_menu, popup.menu)
        
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_unpublish -> {
                    createStyledAlertDialog(
                        context = requireContext(),
                        title = "Unpublish Event",
                        message = "Are you sure you want to unpublish \"${event.title}\"? Attendees will be notified.",
                        positiveButtonText = "Unpublish",
                        positiveButtonListener = { _, _ ->
                            toast("Unpublish functionality coming soon")
                        },
                        negativeButtonText = "Cancel",
                        negativeButtonListener = null
                    ).show()
                    true
                }
                R.id.action_delete -> {
                    createStyledAlertDialog(
                        context = requireContext(),
                        title = "Delete Event",
                        message = "Are you sure you want to delete \"${event.title}\"? This action cannot be undone.",
                        positiveButtonText = "Delete",
                        positiveButtonListener = { _, _ ->
                            toast("Delete functionality coming soon")
                        },
                        negativeButtonText = "Cancel",
                        negativeButtonListener = null
                    ).show()
                    true
                }
                else -> false
            }
        }
        
        popup.show()
    }

    private fun buildAttendingCard(event: EventItem): View {
        val card = ItemAttendingCardMyhangoutsBinding.inflate(
            layoutInflater, binding.layoutEventsContainer, false
        )
        
        card.tvEventTitle.text = event.title ?: "Untitled"
        card.tvDate.text = "${event.date ?: "—"} • ${formatTimeRange(event)}"
        card.tvLocation.text   = event.location?.ifBlank { "Virtual / TBD" } ?: "Virtual / TBD"

        // Load event image
        if (!event.imageUrl.isNullOrBlank()) {
            Glide.with(this).load(event.imageUrl).centerCrop().into(card.ivEventImage)
        }

        val status = deriveAttendingStatus(event)
        applyStatusStyle(card, status)

        // Ticket info (confirmed only)
        if (status == AttendingStatus.CONFIRMED) {
            card.layoutTicketInfo.show()
            card.tvTicketNumber.text = event.ticketNumber ?: "TKT-${event.id}"
            card.tvSeatNumber.text   = event.seatNumber?.ifBlank { "Open" } ?: "Open"
        } else {
            card.layoutTicketInfo.hide()
        }

        card.btnViewETicket.setOnClickListener {
            EventHolder.currentEvent = event
            val tab = activeTab
            val fragment = AttendingDashboardFragment.newInstance(
                onBack = {
                    if (_binding != null) {
                        activateTab(ActiveTab.ATTENDING)
                    }
                }
            )
            parentFragmentManager.beginTransaction()
                .add(R.id.nav_host_fragment, fragment)  // ← add not replace
                .addToBackStack(null)
                .commit()
        }

        card.btnDownloadTicket.setOnClickListener {
            toast("Download ticket (not implemented)")
        }

        return card.root
    }

    private fun buildFavoriteCard(event: EventItem): View {
        val card = ItemFavoritesCardBinding.inflate(
            layoutInflater, binding.layoutEventsContainer, false
        )
        
        card.tvEventTitle.text    = event.title ?: "Untitled"
        card.tvDate.text = "${event.date ?: "—"} • ${formatTimeRange(event)}"
        card.tvLocation.text      = event.location?.ifBlank { "Virtual / TBD" } ?: "Virtual / TBD"
        card.tvEventFormat.text   = event.format ?: "In-Person"
        card.tvAttendees.text     = "${event.attendeeCount ?: 0}/${event.capacity ?: 0} attending"
        card.tvPrice.text         = if ((event.price ?: 0.0) == 0.0) "Free" else "₱${event.price?.toInt()}"

        // Load event image
        if (!event.imageUrl.isNullOrBlank()) {
            Glide.with(this).load(event.imageUrl).centerCrop().into(card.ivEventImage)
        }

        card.btnViewEvent.setOnClickListener {
            openEventDetail(event)
        }

        card.btnRemoveFavorite.setOnClickListener {
            event.id?.let { id ->
                createStyledAlertDialog(
                    context = requireContext(),
                    title = "Remove from Saved",
                    message = "Remove \"${event.title}\" from your saved HangOuts?",
                    positiveButtonText = "Remove",
                    positiveButtonListener = { _, _ -> presenter.unfavorite(id) },
                    negativeButtonText = "Keep",
                    negativeButtonListener = null
                ).show()
            }
        }

        card.btnRsvpNow.setOnClickListener {
            openEventDetail(event)
        }

        return card.root
    }

    // ── Status helpers ─────────────────────────────────────────────────────

    private enum class AttendingStatus { CONFIRMED, PENDING, REJECTED, CANCELLED, COMPLETED }

    private fun deriveAttendingStatus(event: EventItem): AttendingStatus = when {
        event.status         == "cancelled"   -> AttendingStatus.CANCELLED
        event.attendeeStatus == "rejected"    -> AttendingStatus.REJECTED
        event.rsvpPaymentStatus == "rejected" -> AttendingStatus.REJECTED
        event.eventStatus    == "completed"   -> AttendingStatus.COMPLETED
        event.rsvpPaymentStatus == "pending"  -> AttendingStatus.PENDING
        event.status         == "confirmed"   -> AttendingStatus.CONFIRMED
        event.rsvpPaymentStatus == "confirmed"-> AttendingStatus.CONFIRMED
        else                                  -> AttendingStatus.CONFIRMED
    }

    private fun applyStatusStyle(card: ItemAttendingCardMyhangoutsBinding, status: AttendingStatus) {
        val ctx = requireContext()
        val (label, bgDrawable) = when (status) {
            AttendingStatus.CONFIRMED -> "Confirmed" to R.drawable.status_pill_green
            AttendingStatus.PENDING   -> "Pending"   to R.drawable.status_pill_yellow
            AttendingStatus.REJECTED  -> "Rejected"  to R.drawable.status_pill_red
            AttendingStatus.CANCELLED -> "Cancelled" to R.drawable.status_pill_gray
            AttendingStatus.COMPLETED -> "Completed" to R.drawable.status_pill_purple
        }
        card.tvStatusBadge.text = label
        card.tvStatusBadge.setBackgroundResource(bgDrawable)
        card.tvStatusBadge.setTextColor(ContextCompat.getColor(ctx, R.color.white))
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun clearContainer() {
        binding.layoutEventsContainer.removeAllViews()
    }

    private fun showEmpty(msg: String) {
        binding.layoutEventsContainer.addView(TextView(requireContext()).apply {
            text = msg
            setTextColor(resources.getColor(R.color.text_muted, null))
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, 64, 0, 0)
        })
    }

    private fun openEventDetail(event: EventItem) {
        EventHolder.currentEvent = event
        val tab = activeTab
        val fragment = EventDetailFragment.newInstance(onBack = {
            if (_binding != null) activateTab(tab)
        })
        parentFragmentManager.beginTransaction()
            .add(R.id.nav_host_fragment, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun formatTimeRange(event: EventItem): String {
        val start = event.startTime ?: event.time ?: return "Time TBD"
        val end   = event.endTime
        return if (!end.isNullOrBlank()) {
            "${com.hangout.app.utils.formatTime12Hr(start)} – ${com.hangout.app.utils.formatTime12Hr(end)}"
        } else {
            com.hangout.app.utils.formatTime12Hr(start)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.detachView()
        _binding = null
    }
}