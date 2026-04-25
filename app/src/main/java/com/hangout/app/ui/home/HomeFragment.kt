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
import com.hangout.app.models.EventItem
import com.hangout.app.models.UserProfile
import com.hangout.app.models.UserStats
import com.hangout.app.utils.toast

class HomeFragment : Fragment(), HomeContract.View {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var presenter: HomeContract.Presenter

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
        presenter.loadAll()

        binding.ivNotifications.setOnClickListener {
            toast("No new notifications")
        }
        binding.tvManageHosting.setOnClickListener {
            toast("Opening My HangOuts...")
        }
    }

    // ── HomeContract.View ──────────────────────────────────────────────────

    override fun showLoading(show: Boolean) {
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
        if (events.isEmpty()) {
            binding.layoutHostingContainer.addView(
                emptyText("No events yet — create your first HangOut!")
            )
        } else {
            events.forEach { event ->
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
                emptyText("Nothing happening right now.")
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
        cardBinding.tvEventDateTime.text = "${event.date ?: ""} • ${event.time ?: ""}"
        cardBinding.tvEventLocation.text = event.location ?: "—"
        cardBinding.tvEventPrice.text    = if ((event.price ?: 0.0) == 0.0) "FREE" else "₱${event.price?.toInt()}"
        cardBinding.tvEventFormat.text   = event.format ?: "In-Person"
        if (!event.imageUrl.isNullOrBlank()) {
            Glide.with(this).load(event.imageUrl).centerCrop().into(cardBinding.ivEventImage)
        }
        cardBinding.root.setOnClickListener {
        }
    }

    private fun emptyText(msg: String) = TextView(requireContext()).apply {
        text = msg
        setTextColor(resources.getColor(R.color.text_muted, null))
        textSize = 14f
        setPadding(0, 16, 0, 16)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.detachView()
        _binding = null
    }
}