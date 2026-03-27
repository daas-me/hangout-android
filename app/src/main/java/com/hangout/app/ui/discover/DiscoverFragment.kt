package com.hangout.app.ui.discover

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.hangout.app.R
import com.hangout.app.databinding.FragmentDiscoverBinding
import com.hangout.app.databinding.ItemEventCardBinding
import com.hangout.app.models.EventItem
import com.hangout.app.network.RetrofitClient
import com.hangout.app.utils.SessionManager
import kotlinx.coroutines.launch

class DiscoverFragment : Fragment() {

    private var _binding: FragmentDiscoverBinding? = null
    private val binding get() = _binding!!
    private lateinit var session: SessionManager
    private val api = RetrofitClient.apiService

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiscoverBinding.inflate(inflater, container, false)
        session  = SessionManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load all events on start
        loadEvents()

        // Search on keyboard action
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val imm = requireContext().getSystemService(InputMethodManager::class.java)
                imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
                loadEvents(search = binding.etSearch.text.toString())
                true
            } else false
        }
    }

    private fun loadEvents(search: String = "", filter: String = "") {
        lifecycleScope.launch {
            try {
                val res = api.getDiscoverEvents("Bearer ${session.getToken()}", search, filter)
                if (res.isSuccessful) {
                    renderEvents(res.body() ?: emptyList())
                } else {
                    renderEvents(emptyList())
                }
            } catch (e: Exception) {
                renderEvents(emptyList())
            }
        }
    }

    private fun renderEvents(events: List<EventItem>) {
        binding.layoutDiscoverEventsContainer.removeAllViews()

        if (events.isEmpty()) {
            val empty = TextView(requireContext()).apply {
                text = "No events found. Try a different search."
                setTextColor(resources.getColor(R.color.text_muted, null))
                textSize = 14f
                gravity = android.view.Gravity.CENTER
                setPadding(0, 48, 0, 48)
            }
            binding.layoutDiscoverEventsContainer.addView(empty)
            return
        }

        events.forEach { event ->
            val cardBinding = ItemEventCardBinding.inflate(
                layoutInflater, binding.layoutDiscoverEventsContainer, false
            )
            cardBinding.tvEventTitle.text    = event.title
            cardBinding.tvEventDateTime.text = "${event.date ?: ""} • ${event.time ?: ""}"
            cardBinding.tvEventLocation.text = event.location ?: "—"
            cardBinding.tvEventPrice.text    = if ((event.price ?: 0.0) == 0.0) "FREE"
            else "₱${event.price?.toInt()}"
            cardBinding.tvEventFormat.text   = event.format ?: "In-Person"

            if (!event.imageUrl.isNullOrBlank()) {
                Glide.with(this).load(event.imageUrl).centerCrop().into(cardBinding.ivEventImage)
            }

            binding.layoutDiscoverEventsContainer.addView(cardBinding.root)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}