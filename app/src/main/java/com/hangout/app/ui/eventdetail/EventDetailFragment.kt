package com.hangout.app.ui.eventdetail

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.hangout.app.R
import com.hangout.app.data.EventItem
import com.hangout.app.databinding.FragmentEventDetailBinding
import com.hangout.app.ui.components.createStyledAlertDialog
import com.hangout.app.ui.components.createStyledDialogEditText
import com.hangout.app.ui.components.ProfileInformationDialogFragment
import com.hangout.app.ui.createevent.CreateEventActivity
import com.hangout.app.ui.hostdashboard.HostDashboardFragment
import com.hangout.app.ui.paymentproof.PaymentProofBottomSheet
import com.hangout.app.utils.ChatHolder
import com.hangout.app.utils.EventHolder
import com.hangout.app.utils.SessionManager
import com.hangout.app.utils.hide
import com.hangout.app.utils.show
import com.hangout.app.utils.toast
import kotlinx.coroutines.launch

class EventDetailFragment : Fragment(), EventDetailContract.View {

    private var _binding: FragmentEventDetailBinding? = null
    private val binding get() = _binding!!
    private lateinit var presenter: EventDetailContract.Presenter
    private lateinit var sessionManager: SessionManager

    private var isLiked  = false
    private var isRsvped = false
    private var isHost   = false
    private var hostingControlsExpanded = false

    var onBackCallback: (() -> Unit)? = null

    companion object {
        fun newInstance(onBack: (() -> Unit)? = null) =
            EventDetailFragment().apply { onBackCallback = onBack }
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        retainInstance = true
        if (!::presenter.isInitialized) {
            presenter = EventDetailPresenter(this, EventDetailModel(requireContext()))
        }
        sessionManager = SessionManager(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val event = EventHolder.currentEvent ?: return
        presenter.loadEvent(event)

        // ── Setup pull-to-refresh ──────────────────────────────────
        binding.swipeRefresh.setColorSchemeResources(R.color.purple_main)
        binding.swipeRefresh.setOnRefreshListener {
            val latest = EventHolder.currentEvent
            if (latest != null) {
                presenter.loadEvent(latest)
            } else {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    // ── EventDetailContract.View ───────────────────────────────────────────

    override fun showEvent(event: EventItem) {
        android.util.Log.d("EventDetail", "=== showEvent START ===")
        isHost = checkIfUserIsHost(event)
        android.util.Log.d("EventDetail", "After checkIfUserIsHost: isHost=$isHost")
        
        bindHero(event)
        bindDateLocation(event)
        bindStats(event)
        bindAvailability(event)
        bindAbout(event)
        bindHost(event)
        bindPayment(event)
        
        if (isHost) {
            android.util.Log.d("EventDetail", "Branch: HOSTING CONTROLS")
            bindHostingControls(event)
        } else {
            android.util.Log.d("EventDetail", "Branch: RSVP BAR")
            bindRsvpBar(event)
        }
        
        setupClickListeners(event)
        android.util.Log.d("EventDetail", "=== showEvent END ===")
    }

    override fun showLoading(show: Boolean) {
        binding.swipeRefresh.isRefreshing = show
    }

    override fun showMessage(message: String) = toast(message)

    override fun onRsvpSuccess() {
        val isPaid = (EventHolder.currentEvent?.price ?: 0.0) > 0.0
        if (isPaid) {
            // For paid events, mark as pending until payment proof is uploaded
            isRsvped = true
            binding.btnRsvp.text = "Upload Payment Proof"
            binding.btnRsvp.setBackgroundResource(R.drawable.btn_rsvp_bg)
            openPaymentProofSheet()
        } else {
            // For free events, confirm immediately
            isRsvped = true
            binding.btnRsvp.text = "✓  RSVP'd!"
            binding.btnRsvp.setBackgroundResource(R.drawable.btn_rsvp_success_bg)
        }
    }

    override fun onRsvpRemoved() {
        isRsvped = false
        binding.btnRsvp.text      = "RSVP Now"
        binding.btnRsvp.isEnabled = true
        binding.btnRsvp.setBackgroundResource(R.drawable.btn_rsvp_bg)
    }

    override fun onRsvpCancelled(isPaid: Boolean) {
        isRsvped = false
        if (isPaid) {
            // Cancellation is pending host review — disable button, show waiting state
            binding.btnRsvp.text      = "Cancellation Pending"
            binding.btnRsvp.isEnabled = false
            binding.btnRsvp.setBackgroundResource(R.drawable.btn_rsvp_pending_bg)
        } else {
            // Free event — fully cancelled, can re-RSVP immediately
            binding.btnRsvp.text      = "RSVP Now"
            binding.btnRsvp.isEnabled = true
            binding.btnRsvp.setBackgroundResource(R.drawable.btn_rsvp_bg)
        }
    }

    override fun onRsvpStatusLoaded(isRsvped: Boolean, paymentStatus: String?) {
        if (isHost) return  // Host doesn't have RSVP status
        
        this.isRsvped = isRsvped
        when {
            paymentStatus == null && isRsvped && (EventHolder.currentEvent?.price ?: 0.0) > 0.0 -> {
                binding.btnRsvp.text = "Upload Payment Proof"
                binding.btnRsvp.isEnabled = true
                binding.btnRsvp.setBackgroundResource(R.drawable.btn_rsvp_bg)
                binding.btnRsvp.setOnClickListener { openPaymentProofSheet() }
            }
            paymentStatus == "pending" -> {
                binding.btnRsvp.text = "Awaiting Approval"
                binding.btnRsvp.isEnabled = false
                binding.btnRsvp.setBackgroundResource(R.drawable.btn_rsvp_pending_bg)
            }
            isRsvped -> {
                binding.btnRsvp.text = "✓  RSVP'd!"
                binding.btnRsvp.setBackgroundResource(R.drawable.btn_rsvp_success_bg)
            }
            else -> {
                binding.btnRsvp.text = "RSVP Now"
                binding.btnRsvp.setBackgroundResource(R.drawable.btn_rsvp_bg)
            }
        }
    }

    // ── Favorite callbacks — top-level, NOT inside onRsvpStatusLoaded ──────

    override fun onFavoriteStatusLoaded(isFavorite: Boolean) {
        isLiked = isFavorite
        val color = if (isFavorite)
            requireContext().getColor(R.color.pink_accent)
        else
            requireContext().getColor(R.color.text_primary)
        binding.btnLike.setColorFilter(color)
    }

    override fun onFavoriteToggled(isFavorite: Boolean) {
        isLiked = isFavorite
        val color = if (isFavorite)
            requireContext().getColor(R.color.pink_accent)
        else
            requireContext().getColor(R.color.text_primary)
        binding.btnLike.setColorFilter(color)
        toast(if (isFavorite) "Added to saved ♥" else "Removed from saved")
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
        binding.tvEventTime.text = formatTimeRange(event)
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
                (last.firstOrNull()?.uppercase() ?: "")
        binding.tvHostInitials.text = initials.ifBlank { "HO" }
        binding.tvHostName.text     = "$first $last".trim()
        binding.tvHostEmail.text    = event.hostEmail ?: ""
        
        // Load host profile photo if available
        if (!event.hostPhoto.isNullOrBlank()) {
            Glide.with(this)
                .load(event.hostPhoto)
                .centerCrop()
                .into(binding.ivHostProfilePhoto)
            binding.ivHostProfilePhoto.visibility = android.view.View.VISIBLE
        } else {
            binding.ivHostProfilePhoto.visibility = android.view.View.GONE
        }

        // Make host information clickable to view profile
        val displayName = "$first $last".trim().ifBlank { "Host" }
        val profileClickListener = View.OnClickListener {
            showHostProfileInformation(displayName, event)
        }
        binding.tvHostName.setOnClickListener(profileClickListener)
        binding.tvHostInitials.setOnClickListener(profileClickListener)
        binding.ivHostProfilePhoto.setOnClickListener(profileClickListener)
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
        binding.btnBack.setOnClickListener { navigateBack() }

        binding.btnLike.setOnClickListener {
            val id = event.id ?: return@setOnClickListener
            presenter.toggleFavorite(id, isLiked)
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

        if (isHost) {
            // Hosting controls
            android.util.Log.d("EventDetail", "setupClickListeners: Setting up HOST controls (isHost=$isHost)")
            binding.hostingControlsBar.show()
            binding.rsvpBar.hide()
            binding.btnRsvp.visibility = View.GONE
            android.util.Log.d("EventDetail", "setupClickListeners: btnRsvp.visibility set to GONE")
            
            // Toggle hosting controls expansion
            binding.hostingControlsMinimized.setOnClickListener {
                toggleHostingControls()
            }
            
            binding.btnHostDashboard.setOnClickListener {
                navigateToHostDashboard(event)
            }
            
            binding.btnEditEvent.setOnClickListener {
                navigateToEditEvent(event)
            }
            
            binding.btnUnpublishEvent.setOnClickListener {
                promptCancelEvent(event)
            }
            
            binding.btnDeleteEvent.setOnClickListener {
                promptDeleteEvent(event)
            }
        } else {
            // Attendee controls
            android.util.Log.d("EventDetail", "setupClickListeners: Setting up ATTENDEE controls (isHost=$isHost)")
            binding.hostingControlsBar.hide()
            binding.rsvpBar.show()
            binding.btnRsvp.visibility = View.VISIBLE
            android.util.Log.d("EventDetail", "setupClickListeners: btnRsvp.visibility set to VISIBLE")

            binding.btnRsvp.setOnClickListener {
                val id = event.id ?: return@setOnClickListener
                if (isRsvped) promptCancelRsvp(event)
                else presenter.rsvp(id)
            }
        }

        // Message Host button visible for everyone
        binding.btnMessageHost.show()
        binding.btnMessageHost.setOnClickListener {
            val hostId = event.hostId ?: return@setOnClickListener
            ChatHolder.currentChatUser = com.hangout.app.data.OtherUser(
                id = hostId,
                firstname = event.hostFirstName,
                lastname = event.hostLastName,
                email = event.hostEmail,
                photo = event.hostPhoto
            )
            val fragment = com.hangout.app.ui.conversation.ConversationFragment.newInstance(
                onBack = { /* stay on event detail */ }
            )
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    // ── Host check & controls ──────────────────────────────────────────────

    private fun checkIfUserIsHost(event: EventItem): Boolean {
        val currentUserId = sessionManager.getUserId()
        val hostId = event.hostId
        
        android.util.Log.d("EventDetail", "checkIfUserIsHost START")
        android.util.Log.d("EventDetail", "  - sessionManager.getUserId() = $currentUserId")
        android.util.Log.d("EventDetail", "  - event.hostId = $hostId")
        
        // Both are Long?, so direct comparison should work
        // But let's be explicit about null handling
        val isHost = when {
            currentUserId == null || hostId == null -> {
                android.util.Log.d("EventDetail", "Host check FAILED: currentUserId=$currentUserId (null=${currentUserId == null}), hostId=$hostId (null=${hostId == null})")
                false
            }
            currentUserId == hostId -> {
                android.util.Log.d("EventDetail", "Host check PASSED: currentUserId=$currentUserId == hostId=$hostId")
                true
            }
            else -> {
                android.util.Log.d("EventDetail", "Host check FAILED: currentUserId=$currentUserId != hostId=$hostId")
                false
            }
        }
        
        android.util.Log.d("EventDetail", "checkIfUserIsHost END - returning isHost=$isHost")
        return isHost
    }

    private fun bindHostingControls(event: EventItem) {
        binding.hostingControlsBar.show()
        binding.rsvpBar.hide()
    }

    private fun navigateToHostDashboard(event: EventItem) {
        val fragment = HostDashboardFragment.newInstance(
            onBack = { /* refresh if needed */ }
        )
        parentFragmentManager.beginTransaction()
            .add(R.id.nav_host_fragment, fragment)  // ← add not replace
            .addToBackStack(null)
            .commit()
    }

    private val editEventLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            // Event was modified, refresh the event detail
            presenter.loadEvent(EventHolder.currentEvent ?: return@registerForActivityResult)
        }
    }

    private fun navigateToEditEvent(event: EventItem) {
        val intent = Intent(requireContext(), CreateEventActivity::class.java).apply {
            putExtra("eventId", event.id)
            putExtra("isEdit", true)
        }
        editEventLauncher.launch(intent)
    }

    private fun promptCancelEvent(event: EventItem) {
        val input = createStyledDialogEditText(requireContext(), "Reason for cancellation (optional)")
        input.minLines = 3
        AlertDialog.Builder(requireContext(), R.style.HangOutAlertDialogTheme)
            .setTitle("Unpublish Event")
            .setMessage("This will unpublish the event for all attendees.")
            .setView(input)
            .setPositiveButton("Unpublish") { _, _ ->
                event.id?.let { 
                    val reason = input.text.toString().trim().ifBlank { "Event unpublished by host." }
                    presentCancelEvent(it, reason)
                }
            }
            .setNegativeButton("Keep Event", null)
            .show()
    }

    private fun promptDeleteEvent(event: EventItem) {
        createStyledAlertDialog(
            context = requireContext(),
            title = "Delete Event",
            message = "Permanently delete this event? This cannot be undone.",
            positiveButtonText = "Delete",
            positiveButtonListener = { _, _ ->
                event.id?.let { presentDeleteEvent(it) }
            },
            negativeButtonText = "Cancel",
            negativeButtonListener = null
        ).show()
    }

    private fun presentCancelEvent(eventId: Long, reason: String) {
        // Create a temporary model to handle cancel action
        val model = EventDetailModel(requireContext())
        // We would need to add cancelEvent method to EventDetailModel and Presenter
        toast("Event cancellation — feature coming soon")
    }

    private fun presentDeleteEvent(eventId: Long) {
        // Create a temporary model to handle delete action
        val model = EventDetailModel(requireContext())
        // We would need to add deleteEvent method to EventDetailModel and Presenter
        toast("Event deletion — feature coming soon")
    }

    private fun toggleHostingControls() {
        hostingControlsExpanded = !hostingControlsExpanded
        if (hostingControlsExpanded) {
            binding.hostingControlsExpanded.visibility = View.VISIBLE
            binding.hostingControlsToggleIcon.rotation = 90f
        } else {
            binding.hostingControlsExpanded.visibility = View.GONE
            binding.hostingControlsToggleIcon.rotation = 0f
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun openPaymentProofSheet() {
        val e = EventHolder.currentEvent ?: return
        PaymentProofBottomSheet.newInstance(
            event     = e,
            onSuccess = { e.id?.let { presenter.checkRsvpStatus(it) } }
        ).show(parentFragmentManager, "payment_proof")
    }

    private fun navigateBack() {
        onBackCallback?.let { callback ->
            parentFragmentManager.popBackStack()
            // Post callback to ensure parent fragment's view is ready
            binding.root.post { callback() }
        } ?: run {
            parentFragmentManager.popBackStack()
        }
    }

    override fun onStart() {
        super.onStart()
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwnerLiveData.value ?: return,
            object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() { navigateBack() }
            }
        )
    }

    private fun formatDate(raw: String?): String {
        if (raw.isNullOrBlank()) return "Date TBD"
        return try {
            val sdf    = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val outFmt = java.text.SimpleDateFormat("EEE, MMM d, yyyy", java.util.Locale.US)
            val date   = sdf.parse(raw) ?: return raw
            outFmt.format(date)
        } catch (e: Exception) { raw }
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

    private fun showHostProfileInformation(displayName: String, event: EventItem) {
        val hostId = event.hostId ?: return

        // Show basic dialog immediately

        // Fetch full profile then reopen with complete data
        lifecycleScope.launch {
            var dialog: ProfileInformationDialogFragment? = null
            try {
                val api = com.hangout.app.network.RetrofitClient.getApiService(requireContext())
                val response = api.getPublicUserProfile(hostId)

                android.util.Log.d("HostProfile", "Response code: ${response.code()}")
                android.util.Log.d("HostProfile", "Response body: ${response.body()}")
                android.util.Log.d("HostProfile", "Error body: ${response.errorBody()?.string()}")

                if (response.isSuccessful) {
                    val p = response.body() ?: return@launch

                    val location = listOfNotNull(
                        p["city"]    as? String,
                        p["state"]   as? String,
                        p["country"] as? String
                    ).joinToString(", ").ifBlank { null }

                    val age = (p["birthDate"] as? String)?.let { birthdate ->
                        try {
                            val sdf    = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                            val dob    = sdf.parse(birthdate) ?: return@let null
                            val today  = java.util.Calendar.getInstance()
                            val dobCal = java.util.Calendar.getInstance().apply { time = dob }
                            var a = today.get(java.util.Calendar.YEAR) - dobCal.get(java.util.Calendar.YEAR)
                            if (today.get(java.util.Calendar.DAY_OF_YEAR) < dobCal.get(java.util.Calendar.DAY_OF_YEAR)) a--
                            a.toString()
                        } catch (e: Exception) { null }
                    }

                    if (isAdded) {
                        ProfileInformationDialogFragment.newInstance(
                            name     = displayName,
                            email    = p["email"]    as? String,
                            photo    = (p["photoUrl"] as? String) ?: event.hostPhoto,
                            age      = age,
                            gender   = p["gender"]   as? String,
                            phone    = p["phone"]    as? String,
                            location = location,
                            about    = p["bio"]      as? String,
                            isHost   = true
                        ).show(parentFragmentManager, "ProfileInformationDialog")
                    }
                }
            } catch (e: Exception) {
                if (isAdded) {
                    ProfileInformationDialogFragment.newInstance(
                        name   = displayName,
                        email  = event.hostEmail,   // or attendee.email
                        photo  = event.hostPhoto,   // or attendee.photo
                        isHost = true               // or false
                    ).show(parentFragmentManager, "ProfileInformationDialog")
                }
                android.util.Log.e("HostProfile", "Failed to fetch host profile", e)
            }
        }
    }

    private fun promptCancelRsvp(event: EventItem) {
        val id       = event.id ?: return
        val isPaid   = (event.price ?: 0.0) > 0.0
        val noRefund = event.noRefundPolicy == true
        val ctx      = requireContext()

        val container = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(24.dpToPx(), 8.dpToPx(), 24.dpToPx(), 8.dpToPx())
        }

        // Subtitle
        container.addView(android.widget.TextView(ctx).apply {
            text     = "You're about to cancel your spot for \"${event.title}\"."
            textSize = 13f
            setTextColor(resources.getColor(R.color.text_muted, null))
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16.dpToPx() }
        })

        // Paid + refundable banner (indigo)
        if (isPaid && !noRefund) {
            container.addView(android.widget.LinearLayout(ctx).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity     = android.view.Gravity.TOP
                background  = android.graphics.drawable.GradientDrawable().apply {
                    setColor(0x1A6366F1.toInt())
                    setStroke(1.dpToPx(), 0x406366F1.toInt())
                    cornerRadius = 10.dpToPx().toFloat()
                }
                setPadding(14.dpToPx(), 12.dpToPx(), 14.dpToPx(), 12.dpToPx())
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 16.dpToPx() }

                addView(android.widget.ImageView(ctx).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(18.dpToPx(), 18.dpToPx()).apply {
                        marginEnd = 10.dpToPx(); topMargin = 2.dpToPx()
                    }
                    setImageResource(R.drawable.ic_refund)
                    setColorFilter(0xFFA5B4FC.toInt())
                })
                addView(android.widget.LinearLayout(ctx).apply {
                    orientation = android.widget.LinearLayout.VERTICAL
                    layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    addView(android.widget.TextView(ctx).apply {
                        text     = "Paid Event — Reimbursement Request"
                        textSize = 12f
                        typeface = android.graphics.Typeface.defaultFromStyle(android.graphics.Typeface.BOLD)
                        setTextColor(0xFFA5B4FC.toInt())
                        layoutParams = android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { bottomMargin = 2.dpToPx() }
                    })
                    addView(android.widget.TextView(ctx).apply {
                        text     = "Cancelling a paid RSVP will initiate a reimbursement request. Please explain your reason below to help the host process it."
                        textSize = 12f
                        setTextColor(0xFF818CF8.toInt())
                        setLineSpacing(0f, 1.4f)
                    })
                })
            })
        }

        // Paid + no-refund banner (red)
        if (isPaid && noRefund) {
            container.addView(android.widget.LinearLayout(ctx).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity     = android.view.Gravity.TOP
                background  = android.graphics.drawable.GradientDrawable().apply {
                    setColor(0x1AEF4444.toInt())
                    setStroke(1.dpToPx(), 0x40EF4444.toInt())
                    cornerRadius = 10.dpToPx().toFloat()
                }
                setPadding(14.dpToPx(), 12.dpToPx(), 14.dpToPx(), 12.dpToPx())
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 16.dpToPx() }

                addView(android.widget.ImageView(ctx).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(18.dpToPx(), 18.dpToPx()).apply {
                        marginEnd = 10.dpToPx(); topMargin = 2.dpToPx()
                    }
                    setImageResource(R.drawable.ic_warning)
                    setColorFilter(resources.getColor(R.color.red_accent, null))
                })
                addView(android.widget.LinearLayout(ctx).apply {
                    orientation = android.widget.LinearLayout.VERTICAL
                    layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    addView(android.widget.TextView(ctx).apply {
                        text     = "No Refunds Policy"
                        textSize = 12f
                        typeface = android.graphics.Typeface.defaultFromStyle(android.graphics.Typeface.BOLD)
                        setTextColor(resources.getColor(R.color.red_accent, null))
                        layoutParams = android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { bottomMargin = 2.dpToPx() }
                    })
                    addView(android.widget.TextView(ctx).apply {
                        text     = "This event has a no-refund policy. Cancelling your RSVP cannot be reversed and no refund will be issued."
                        textSize = 12f
                        setTextColor(0xFFF87171.toInt())
                        setLineSpacing(0f, 1.4f)
                    })
                })
            })
        }

        // Reason input
        val reasonInput = android.widget.EditText(ctx).apply {
            hint      = if (isPaid && !noRefund)
                "Explain your reason for cancelling (helps the host process your refund)…"
            else
                "Tell us why you're cancelling (optional)…"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                    android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines  = 3
            maxLines  = 5
            setPadding(14.dpToPx(), 12.dpToPx(), 14.dpToPx(), 12.dpToPx())
            setTextColor(resources.getColor(R.color.text_primary, null))
            setHintTextColor(resources.getColor(R.color.text_muted, null))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0x0DFFFFFF.toInt())
                setStroke(1.dpToPx(), 0x1FFFFFFF.toInt())
                cornerRadius = 10.dpToPx().toFloat()
            }
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        container.addView(reasonInput)

        val confirmLabel = if (isPaid && !noRefund) "Cancel & Request Refund" else "Cancel RSVP"

        val dialog = AlertDialog.Builder(ctx, R.style.HangOutAlertDialogTheme)
            .setTitle("Cancel RSVP")
            .setView(container)
            .setPositiveButton(confirmLabel) { _, _ ->
                val reason = reasonInput.text.toString().trim()
                presenter.removeRsvp(id, reason)
            }
            .setNegativeButton("Keep RSVP", null)
            .setCancelable(true)
            .create()

        dialog.setOnShowListener {
            dialog.findViewById<android.widget.TextView>(android.R.id.title)
                ?.setTextColor(resources.getColor(R.color.text_primary, null))
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                ?.setTextColor(resources.getColor(R.color.red_accent, null))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                ?.setTextColor(resources.getColor(R.color.text_muted, null))
        }

        dialog.show()
    }

    private fun Int.dpToPx(): Int =
        (this * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.detachView()
        _binding = null
    }
}