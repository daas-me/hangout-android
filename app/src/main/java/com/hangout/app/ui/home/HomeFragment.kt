package com.hangout.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.hangout.app.R
import com.hangout.app.databinding.FragmentHomeBinding
import com.hangout.app.databinding.ItemEventCardHorizontalBinding
import com.hangout.app.data.*
import com.hangout.app.repository.NotificationRepository
import com.hangout.app.ui.components.SkeletonLoadingHelper
import com.hangout.app.ui.eventdetail.EventDetailFragment
import com.hangout.app.ui.notifications.NotificationsFragment
import com.hangout.app.utils.EventHolder
import com.hangout.app.utils.toast
import com.hangout.app.repository.Result
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeFragment : Fragment(), HomeContract.View {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var presenter: HomeContract.Presenter
    private var isFirstLoad = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        if (!::presenter.isInitialized) {
            presenter = HomePresenter(this, HomeModel(requireContext()))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Always load — cache makes this instant on return
        if (isFirstLoad) {
            presenter.loadAll()
            isFirstLoad = false
        }

        binding.ivNotifications.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .add(R.id.nav_host_fragment, NotificationsFragment.newInstance(
                    onBack = { loadUnreadBadge(); loadUnreadMessageBadge() }
                ))
                .addToBackStack(null)
                .commit()
        }

        binding.ivMessages.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .add(R.id.nav_host_fragment, com.hangout.app.ui.messages.MessagesFragment.newInstance(  // ← add not replace
                    onBack = { loadUnreadMessageBadge() }
                ))
                .addToBackStack(null)
                .commit()
        }

        binding.tvManageHosting.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, com.hangout.app.ui.myhangouts.MyHangoutsFragment())
                .addToBackStack(null)
                .commit()
            (activity as? com.hangout.app.ui.nav.NavActivity)?.setSelectedNavItem(R.id.nav_hangouts)
        }

        loadUnreadBadge()
        loadUnreadMessageBadge()
    }

    // ── HomeContract.View ──────────────────────────────────────────────────

    override fun showLoading(show: Boolean) {
        val skeletonContainer = binding.root.findViewById<View>(R.id.skeletonContainer)
        if (show) {
            SkeletonLoadingHelper.showSkeleton(skeletonContainer, binding.contentScroll)
        } else {
            SkeletonLoadingHelper.hideSkeleton(skeletonContainer, binding.contentScroll)
        }
    }

    override fun showError(message: String) {
        toast(message)
    }

    override fun showProfile(profile: UserProfile) {
        binding.tvUserName.text = "${profile.firstname}!"
    }

    override fun showStats(stats: UserStats) {
        binding.tvStatHosting.text   = stats.hostingCount.toString()
        binding.tvStatAttending.text = stats.attendingCount.toString()
        binding.tvStatAttendees.text = stats.totalAttendees.toString()
    }

    override fun showHostingEvents(events: List<EventItem>) {
        binding.layoutHostingContainer.removeAllViews()

        val published = events.filter { it.isDraft != true }.take(3)

        if (published.isEmpty()) {
            binding.layoutHostingContainer.addView(
                createEmptyState(
                    iconRes    = R.drawable.ic_calendar,
                    title      = "No events yet",
                    subtitle   = "Create your first HangOut to get started",
                    actionText = null
                )
            )
        } else {
            published.forEach { event ->
                val card = ItemEventCardHorizontalBinding.inflate(
                    layoutInflater, binding.layoutHostingContainer, false
                )
                bindEventCard(card, event)
                binding.layoutHostingContainer.addView(card.root)
            }
        }
    }

    override fun showTodayEvents(events: List<EventItem>) {
        binding.layoutHappeningNowContainer.removeAllViews()
        if (events.isEmpty()) {
            binding.layoutHappeningNowContainer.addView(
                createEmptyState(
                    iconRes = R.drawable.ic_clock,
                    title = "Nothing happening right now",
                    subtitle = "Check back soon for upcoming events",
                    actionText = null
                )
            )
        } else {
            events.forEach { event ->
                val card = ItemEventCardHorizontalBinding.inflate(
                    layoutInflater, binding.layoutHappeningNowContainer, false
                )
                bindEventCard(card, event)
                binding.layoutHappeningNowContainer.addView(card.root)
            }
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun bindEventCard(cardBinding: ItemEventCardHorizontalBinding, event: EventItem) {
        cardBinding.tvEventTitle.text    = event.title
        // Format time range with both start and end times
        val timeRange = formatTimeRange(event)
        cardBinding.tvEventDateTime.text = "${event.date ?: ""} • $timeRange"
        cardBinding.tvEventLocation.text = event.location ?: "—"
        cardBinding.tvEventPrice.text    = if ((event.price ?: 0.0) == 0.0) "FREE" else "₱${event.price?.toInt()}"
        cardBinding.tvEventFormat.text   = event.format ?: "In-Person"
        cardBinding.tvEventAttendees.text     = "${event.attendeeCount ?: 0}/${event.capacity ?: 0} attending"
        if (!event.imageUrl.isNullOrBlank()) {
            Glide.with(this).load(event.imageUrl).centerCrop().into(cardBinding.ivEventImage)
        }
        cardBinding.root.setOnClickListener {
            openEventDetail(event)
        }
    }

    private fun formatTimeRange(event: EventItem): String {
        val start = event.startTime ?: event.time ?: return "Time TBD"
        val end = event.endTime
        return if (!end.isNullOrBlank()) {
            "${com.hangout.app.utils.formatTime12Hr(start)} – ${com.hangout.app.utils.formatTime12Hr(end)}"
        } else {
            com.hangout.app.utils.formatTime12Hr(start)
        }
    }

    private fun createEmptyState(
        iconRes: Int,
        title: String,
        subtitle: String,
        actionText: String?
    ): android.view.View {
        val view = layoutInflater.inflate(R.layout.item_empty_state, null, false)
        val ivIcon = view.findViewById<android.widget.ImageView>(R.id.ivEmptyIcon)
        val tvTitle = view.findViewById<TextView>(R.id.tvEmptyTitle)
        val tvSubtext = view.findViewById<TextView>(R.id.tvEmptySubtext)
        val btnAction = view.findViewById<android.widget.Button>(R.id.btnEmptyAction)

        ivIcon.setImageResource(iconRes)
        tvTitle.text = title
        tvSubtext.text = subtitle

        if (!actionText.isNullOrBlank()) {
            btnAction.text = actionText
            btnAction.visibility = android.view.View.VISIBLE
        } else {
            btnAction.visibility = android.view.View.GONE
        }

        return view
    }

    private fun openEventDetail(event: EventItem) {
        EventHolder.currentEvent = event
        val fragment = EventDetailFragment.newInstance(
            onBack = { presenter.loadAll() }
        )
        parentFragmentManager.beginTransaction()
            .add(R.id.nav_host_fragment, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun emptyText(msg: String) = TextView(requireContext()).apply {
        text = msg
        setTextColor(resources.getColor(R.color.text_muted, null))
        textSize = 14f
        setPadding(0, 16, 0, 16)
    }

    private fun loadUnreadBadge() {
        CoroutineScope(Dispatchers.Main).launch {
            val repo = NotificationRepository(requireContext())
            val result = repo.getUnreadCount(forceRefresh = true)
            if (_binding == null) return@launch
            val count = if (result is Result.Success) result.data else 0
            if (count > 0) {
                binding.tvNotifBadge.text = if (count > 9) "9+" else count.toString()
                binding.tvNotifBadge.visibility = android.view.View.VISIBLE
            } else {
                binding.tvNotifBadge.visibility = android.view.View.GONE
            }
        }
    }

    private fun loadUnreadMessageBadge() {
        CoroutineScope(Dispatchers.Main).launch {
            val repo = com.hangout.app.repository.MessageRepository(requireContext())
            val result = repo.getUnreadCount()
            if (_binding == null) return@launch
            val count = if (result is Result.Success) result.data else 0
            if (count > 0) {
                binding.tvMessageBadge.text = if (count > 9) "9+" else count.toString()
                binding.tvMessageBadge.visibility = android.view.View.VISIBLE
            } else {
                binding.tvMessageBadge.visibility = android.view.View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.detachView()
        _binding = null
    }
}