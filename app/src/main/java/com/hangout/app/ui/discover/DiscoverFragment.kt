package com.hangout.app.ui.discover

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.hangout.app.R
import com.hangout.app.databinding.FragmentDiscoverBinding
import com.hangout.app.databinding.ItemEventCardBinding
import com.hangout.app.models.EventItem
import com.hangout.app.utils.toast

class DiscoverFragment : Fragment(), DiscoverContract.View {

    private var _binding: FragmentDiscoverBinding? = null
    private val binding get() = _binding!!
    private lateinit var presenter: DiscoverContract.Presenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        if (!::presenter.isInitialized) {
            presenter = DiscoverPresenter(this, DiscoverModel(requireContext()))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiscoverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter.loadEvents()

        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val imm = requireContext().getSystemService(InputMethodManager::class.java)
                imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
                presenter.loadEvents(search = binding.etSearch.text.toString())
                true
            } else false
        }
    }

    // ── DiscoverContract.View ──────────────────────────────────────────────

    override fun showLoading(show: Boolean) {
    }

    override fun showError(message: String) {
        toast(message)
        showEvents(emptyList())
    }

    override fun showEvents(events: List<EventItem>) {
        binding.layoutDiscoverEventsContainer.removeAllViews()
        if (events.isEmpty()) {
            binding.layoutDiscoverEventsContainer.addView(emptyText())
            return
        }
        events.forEach { event ->
            val card = ItemEventCardBinding.inflate(
                layoutInflater, binding.layoutDiscoverEventsContainer, false
            )
            bindEventCard(card, event)
            binding.layoutDiscoverEventsContainer.addView(card.root)
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun bindEventCard(card: ItemEventCardBinding, event: EventItem) {
        card.tvEventTitle.text    = event.title
        card.tvEventDateTime.text = "${event.date ?: ""} • ${event.time ?: ""}"
        card.tvEventLocation.text = event.location ?: "—"
        card.tvEventPrice.text    = if ((event.price ?: 0.0) == 0.0) "FREE" else "₱${event.price?.toInt()}"
        card.tvEventFormat.text   = event.format ?: "In-Person"
        if (!event.imageUrl.isNullOrBlank()) {
            Glide.with(this).load(event.imageUrl).centerCrop().into(card.ivEventImage)
        }
        card.root.setOnClickListener {

        }
    }

    private fun emptyText() = TextView(requireContext()).apply {
        text = "No events found. Try a different search."
        setTextColor(resources.getColor(R.color.text_muted, null))
        textSize = 14f
        gravity = Gravity.CENTER
        setPadding(0, 48, 0, 48)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.detachView()
        _binding = null
    }
}