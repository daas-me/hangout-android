package com.hangout.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.hangout.app.R
import com.hangout.app.databinding.FragmentHomeBinding
import com.hangout.app.databinding.ItemEventCardHorizontalBinding
import com.hangout.app.models.EventItem
import com.hangout.app.utils.SessionManager
import com.hangout.app.viewmodel.HomeViewModel

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val vm: HomeViewModel by viewModels()
    private lateinit var session: SessionManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        session  = SessionManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vm.loadAll(session.getToken() ?: "")

        // ── Profile / greeting ──
        vm.profile.observe(viewLifecycleOwner) { profile ->
            binding.tvUserName.text = if (profile != null) "${profile.firstname}!" else "${session.getFirstname()}!"
        }

        // ── Stats ──
        vm.stats.observe(viewLifecycleOwner) { stats ->
            stats ?: return@observe
            binding.tvStatHosting.text   = stats.hostingCount.toString()
            binding.tvStatAttending.text = stats.attendingCount.toString()
            binding.tvStatAttendees.text = stats.totalAttendees.toString()
        }

        // ── Hosting events ──
        vm.hostingEvents.observe(viewLifecycleOwner) { events ->
            binding.layoutHostingContainer.removeAllViews()
            if (events.isEmpty()) {
                val empty = TextView(requireContext()).apply {
                    text = "No events yet — create your first HangOut!"
                    setTextColor(resources.getColor(R.color.text_muted, null))
                    textSize = 14f
                    setPadding(0, 16, 0, 16)
                }
                binding.layoutHostingContainer.addView(empty)
            } else {
                events.forEach { event ->
                    val cardBinding = ItemEventCardHorizontalBinding.inflate(
                        layoutInflater, binding.layoutHostingContainer, false
                    )
                    bindEventCard(cardBinding, event)
                    binding.layoutHostingContainer.addView(cardBinding.root)
                }
            }
        }

        // ── Happening Now events ──
        vm.todayEvents.observe(viewLifecycleOwner) { events ->
            binding.layoutHappeningNowContainer.removeAllViews()
            if (events.isEmpty()) {
                val empty = TextView(requireContext()).apply {
                    text = "Nothing happening right now."
                    setTextColor(resources.getColor(R.color.text_muted, null))
                    textSize = 14f
                    setPadding(0, 16, 0, 16)
                }
                binding.layoutHappeningNowContainer.addView(empty)
            } else {
                events.forEach { event ->
                    val cardBinding = ItemEventCardHorizontalBinding.inflate(
                        layoutInflater, binding.layoutHappeningNowContainer, false
                    )
                    bindEventCard(cardBinding, event)
                    binding.layoutHappeningNowContainer.addView(cardBinding.root)
                }
            }
        }

        // ── Click listeners ──
        binding.ivNotifications.setOnClickListener {
            Toast.makeText(requireContext(), "No new notifications", Toast.LENGTH_SHORT).show()
        }

        binding.tvManageHosting.setOnClickListener {
            Toast.makeText(requireContext(), "Opening My HangOuts...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun bindEventCard(cardBinding: ItemEventCardHorizontalBinding, event: EventItem) {
        cardBinding.tvEventTitle.text    = event.title
        cardBinding.tvEventDateTime.text = "${event.date ?: ""} • ${event.time ?: ""}"
        cardBinding.tvEventLocation.text = event.location ?: "—"
        cardBinding.tvEventPrice.text    = if ((event.price ?: 0.0) == 0.0) "FREE" else "₱${event.price?.toInt()}"
        cardBinding.tvEventFormat.text   = event.format ?: "In-Person"

        if (!event.imageUrl.isNullOrBlank()) {
            Glide.with(this)
                .load(event.imageUrl)
                .centerCrop()
                .into(cardBinding.ivEventImage)
        }

        cardBinding.root.setOnClickListener {
            Toast.makeText(requireContext(), event.title, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}