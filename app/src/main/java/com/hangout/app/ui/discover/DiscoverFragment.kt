package com.hangout.app.ui.discover

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.hangout.app.R
import com.hangout.app.data.EventItem
import com.hangout.app.utils.EventHolder
import com.hangout.app.databinding.FragmentDiscoverBinding
import com.hangout.app.ui.eventdetail.EventDetailFragment
import com.hangout.app.utils.toast

class DiscoverFragment : Fragment(), DiscoverContract.View {

    private var _binding: FragmentDiscoverBinding? = null
    private val binding get() = _binding!!
    private lateinit var presenter: DiscoverContract.Presenter
    private lateinit var adapter: EventGridAdapter
    private var currentFilter = ""
    private var currentSearch = ""

    // Filter button references
    private val filterButtons by lazy {
        listOf(
            binding.btnFilterAll,
            binding.btnFilterFree,
            binding.btnFilterPaid,
            binding.btnFilterToday
        )
    }

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

        // Setup RecyclerView with GridLayoutManager (2 columns)
        adapter = EventGridAdapter(onEventClick = { event ->
            openEventDetail(event)
        })
        binding.rvDiscoverEvents.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvDiscoverEvents.adapter = adapter

        // Setup search listener
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val imm = requireContext().getSystemService(InputMethodManager::class.java)
                imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
                currentSearch = binding.etSearch.text.toString()
                loadEvents()
                true
            } else false
        }

        // Setup filter button listeners
        setupFilterButtons()

        // Load initial events
        presenter.loadEvents(search = currentSearch, filter = currentFilter)

        // ── Setup pull-to-refresh ──────────────────────────────────
        binding.swipeRefresh.setColorSchemeResources(R.color.purple_main)
        binding.swipeRefresh.setOnRefreshListener {
            loadEvents()
        }
    }

    private fun setupFilterButtons() {
        filterButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                // Update filter
                currentFilter = button.tag as String
                currentSearch = ""
                binding.etSearch.setText("")

                // Update button styles
                updateFilterButtonStyles(index)

                // Load events with new filter
                loadEvents()
            }
        }
    }

    private fun updateFilterButtonStyles(selectedIndex: Int) {
        filterButtons.forEachIndexed { index, button ->
            if (index == selectedIndex) {
                // Selected state
                button.setTextColor(resources.getColor(R.color.white, null))
                button.setBackgroundResource(R.drawable.btn_pill_purple)
            } else {
                // Unselected state
                button.setTextColor(resources.getColor(R.color.text_muted, null))
                button.setBackgroundResource(R.drawable.btn_ghost)
            }
        }
    }

    private fun loadEvents() {
        presenter.loadEvents(search = currentSearch, filter = currentFilter)
    }

    // ── DiscoverContract.View ──────────────────────────────────────────────

    override fun showLoading(show: Boolean) {
        binding.swipeRefresh.isRefreshing = show
    }

    override fun showError(message: String) {
        toast(message)
        showEvents(emptyList())
    }

    override fun showEvents(events: List<EventItem>) {
        if (events.isEmpty()) {
            adapter.clearEvents()
            toast("No events found")
            return
        }
        adapter.updateEvents(events)
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun openEventDetail(event: EventItem) {
        EventHolder.currentEvent = event
        val fragment = EventDetailFragment.newInstance(
            onBack = {
                if (_binding != null) {
                    loadEvents()
                }
            }
        )
        parentFragmentManager.beginTransaction()
            .add(R.id.nav_host_fragment, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.detachView()
        _binding = null
    }
}
