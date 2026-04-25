package com.hangout.app.ui.eventdetail

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.hangout.app.R
import com.hangout.app.databinding.FragmentEventDetailBinding
import com.hangout.app.models.EventItem
import com.hangout.app.utils.hide
import com.hangout.app.utils.show
import com.hangout.app.utils.toast

class EventDetailFragment : Fragment(), EventDetailContract.View {

    private var _binding: FragmentEventDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var presenter: EventDetailContract.Presenter

    private var isLiked  = false
    private var isRsvped = false

    // Callback invoked when this fragment is popped so Discover can refresh
    var onBackCallback: (() -> Unit)? = null

    companion object {
        private const val ARG_EVENT = "arg_event"

        fun newInstance(event: EventItem, onBack: (() -> Unit)? = null) =
            EventDetailFragment().apply {
                arguments = Bundle().apply { putParcelable(ARG_EVENT, event) }
                onBackCallback = onBack
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        retainInstance = true
        if (!::presenter.isInitialized) {
            presenter = EventDetailPresenter(this, EventDetailModel(requireContext()))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        @Suppress("DEPRECATION")
        val event = arguments?.getParcelable<EventItem>(ARG_EVENT) ?: return
        presenter.loadEvent(event)
    }

    // ── EventDetailContract.View ───────────────────────────────────────────

    override fun showEvent(event: EventItem) {
        bindHero(event)
        bindDateLocation(event)
        bindStats(event)
        bindAvailability(event)
        bindAbout(event)
        bindHost(event)
        bindPayment(event)
        bindRsvpBar(event)
        setupClickListeners(event)
    }

    override fun showLoading(show: Boolean) {}

    override fun showMessage(message: String) {
        toast(message)
    }

    override fun onRsvpSuccess() {
        isRsvped = true
        binding.btnRsvp.text = "✓  RSVP'd!"
        binding.btnRsvp.setBackgroundResource(R.drawable.btn_rsvp_success_bg)
    }

    override fun onRsvpRemoved() {
        isRsvped = false
        binding.btnRsvp.text = "RSVP Now"
        binding.btnRsvp.setBackgroundResource(R.drawable.btn_rsvp_bg)
    }

    // ── Bind helpers ───────────────────────────────────────────────────────

    private fun bindHero(event: EventItem) {
        if (!event.imageUrl.isNullOrBlank()) {
            Glide.with(this).load(event.imageUrl).centerCrop().into(binding.ivEventHero)
        }
        binding.tvFormatBadge.text = event.format ?: "In-Person"
        binding.tvEventTitle.text  = event.title
    }

    private fun bindDateLocation(event: EventItem) {
        binding.tvEventDate.text = formatDate(event.date)
        binding.tvEventTime.text = event.time ?: "TBD"

        val isVirtual = event.format?.lowercase() == "virtual"
        if (!event.location.isNullOrBlank() && !isVirtual) {
            binding.cardLocation.show()
            binding.tvEventLocation.text = event.location
        } else {
            binding.cardLocation.hide()
        }
    }

    private fun bindStats(event: EventItem) {
        val isPaid = (event.price ?: 0.0) > 0
        binding.tvStatPrice.text     = if (isPaid) "₱${event.price?.toInt()}" else "Free"
        binding.tvStatAttending.text = (event.attendeeCount ?: 0).toString()
        binding.tvStatFormat.text    = event.format ?: "In-Person"
        binding.tvStatCapacity.text  = (event.capacity ?: 0).toString()
    }

    private fun bindAvailability(event: EventItem) {
        val current = event.attendeeCount ?: 0
        val max     = (event.capacity ?: 100).coerceAtLeast(1)
        val left    = (max - current).coerceAtLeast(0)
        val pct     = (current.toFloat() / max.toFloat() * 100).toInt().coerceIn(0, 100)

        binding.tvAttendingCount.text     = "$current attending"
        binding.tvSpotsLeft.text          = "$left spots left"
        binding.progressCapacity.progress = pct
    }

    private fun bindAbout(event: EventItem) {
        binding.tvEventDescription.text = event.description ?: "No description provided."
        binding.tvSeatingType.text =
            if (event.seatingType == "reserved") "Assigned Seats" else "Open Seating"
    }

    private fun bindHost(event: EventItem) {
        val first    = event.hostFirstName ?: ""
        val last     = event.hostLastName  ?: ""
        val initials = (first.firstOrNull()?.uppercase() ?: "") +
                (last.firstOrNull()?.uppercase()  ?: "")
        binding.tvHostInitials.text = initials.ifBlank { "HO" }
        binding.tvHostName.text     = "$first $last".trim()
        binding.tvHostEmail.text    = event.hostEmail ?: ""
    }

    private fun bindPayment(event: EventItem) {
        val isPaid = (event.price ?: 0.0) > 0
        if (isPaid) {
            binding.cardPayment.show()
            binding.tvPaymentMethod.text = when (event.paymentMethod?.lowercase()) {
                "gcash"   -> "GCash"
                "paymaya" -> "PayMaya"
                "bank"    -> "Bank Transfer"
                else      -> event.paymentMethod?.replaceFirstChar { it.uppercase() } ?: "GCash"
            }
            binding.tvAccountNumber.text = event.accountNumber ?: "—"
        } else {
            binding.cardPayment.hide()
        }
    }

    private fun bindRsvpBar(event: EventItem) {
        val isPaid = (event.price ?: 0.0) > 0
        val left   = ((event.capacity ?: 100) - (event.attendeeCount ?: 0)).coerceAtLeast(0)
        binding.tvRsvpPrice.text  = if (isPaid) "₱${event.price?.toInt()}" else "Free"
        binding.tvRsvpSeats.text  = if (left > 0) "$left spots remaining" else "Fully booked"
        binding.btnRsvp.isEnabled = left > 0
    }

    // ── Click listeners ────────────────────────────────────────────────────

    private fun setupClickListeners(event: EventItem) {
        binding.btnBack.setOnClickListener {
            navigateBack()
        }

        binding.btnLike.setOnClickListener {
            isLiked = !isLiked
            val color = if (isLiked)
                requireContext().getColor(R.color.red_accent)
            else
                requireContext().getColor(R.color.text_primary)
            binding.btnLike.setColorFilter(color)
        }

        binding.btnShare.setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Check out: ${event.title}")
            }
            startActivity(Intent.createChooser(intent, "Share Event"))
        }

        binding.tvOpenMaps.setOnClickListener {
            val query = Uri.encode(event.location ?: "")
            val uri   = Uri.parse("https://www.google.com/maps/search/?api=1&query=$query")
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }

        binding.btnMessageHost.setOnClickListener {
            toast("Message host — coming soon!")
        }

        binding.btnRsvp.setOnClickListener {
            val id = event.id ?: return@setOnClickListener
            if (isRsvped) presenter.removeRsvp(id) else presenter.rsvp(id)
        }
    }

    // ── Navigation ─────────────────────────────────────────────────────────

    private fun navigateBack() {
        // Fire the callback BEFORE popping so the target fragment is still
        // in the back stack and can react (e.g. trigger a refresh)
        onBackCallback?.invoke()
        parentFragmentManager.popBackStack()
    }

    // Also handle the system back gesture
    override fun onStart() {
        super.onStart()
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwnerLiveData.value ?: return,
            object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    navigateBack()
                }
            }
        )
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun formatDate(raw: String?): String {
        if (raw.isNullOrBlank()) return "Date TBD"
        return try {
            val sdf    = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val outFmt = java.text.SimpleDateFormat("EEE, MMM d, yyyy", java.util.Locale.US)
            val date   = sdf.parse(raw) ?: return raw
            outFmt.format(date)
        } catch (e: Exception) { raw }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.detachView()
        _binding = null
    }
}