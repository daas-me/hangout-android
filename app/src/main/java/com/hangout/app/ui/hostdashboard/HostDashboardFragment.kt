package com.hangout.app.ui.hostdashboard

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.hangout.app.R
import com.hangout.app.data.AttendeeItem
import com.hangout.app.data.EventItem
import com.hangout.app.databinding.FragmentHostDashboardBinding
import com.hangout.app.databinding.ItemAttendeeRowBinding
import com.hangout.app.databinding.ItemAttendeeSlimRowBinding
import com.hangout.app.databinding.DialogAttendeeDetailsBinding
import com.hangout.app.network.RetrofitClient
import com.hangout.app.ui.components.createStyledAlertDialog
import com.hangout.app.ui.components.createStyledDialogEditText
import com.hangout.app.ui.components.ProfileInformationDialogFragment
import com.hangout.app.utils.createStyledPopupMenu
import com.hangout.app.ui.ticketscanner.TicketScannerFragment
import com.hangout.app.utils.EventHolder
import com.hangout.app.utils.hide
import com.hangout.app.utils.show
import com.hangout.app.utils.showIf
import com.hangout.app.utils.toast
import kotlinx.coroutines.launch

class HostDashboardFragment : Fragment(), HostDashboardContract.View {

    private var _binding: FragmentHostDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var presenter: HostDashboardContract.Presenter

    private var event: EventItem? = null
    var onBackCallback: (() -> Unit)? = null

    private var currentTab = TAB_OVERVIEW
    private var allAttendees = listOf<AttendeeItem>()

    // ── Refund proof state ─────────────────────────────────────
    // Kept here so the file picker callback can write to it, and
    // promptRefundApproval can read it when "Submit" is tapped.
    private var pendingRefundProofUri: Uri? = null
    private var pendingRefundProofLabel: TextView? = null   // updated after pick

    // ActivityResult launcher — picks an image from the gallery
    private val pickRefundProof =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri == null) return@registerForActivityResult
            pendingRefundProofUri = uri
            // Update the label inside the currently-open dialog
            pendingRefundProofLabel?.let { tv ->
                val name = uri.lastPathSegment ?: "image selected"
                tv.text  = "✓  $name"
                tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.success_green))
            }
        }

    companion object {
        private const val TAB_OVERVIEW  = 0
        private const val TAB_PENDING   = 1
        private const val TAB_REFUNDS   = 2
        private const val TAB_ATTENDEES = 3
        private const val TAB_ANALYTICS = 4

        fun newInstance(onBack: (() -> Unit)? = null) =
            HostDashboardFragment().apply { onBackCallback = onBack }
    }

    // ── Lifecycle ──────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        retainInstance = true
        if (!::presenter.isInitialized) {
            presenter = HostDashboardPresenter(this, HostDashboardModel(requireContext()))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHostDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        event = EventHolder.currentEvent ?: return

        binding.btnBack.setOnClickListener {
            onBackCallback?.invoke()
            parentFragmentManager.popBackStack()
        }

        binding.btnRefresh.setOnClickListener {
            event?.id?.let { presenter.loadAttendees(it) }
        }

        binding.btnMoreOptions.setOnClickListener { showMoreOptionsMenu(it) }

        binding.tabOverview.setOnClickListener       { selectTab(TAB_OVERVIEW) }
        binding.tabPendingReviews.setOnClickListener { selectTab(TAB_PENDING) }
        binding.tabRefundRequests.setOnClickListener { selectTab(TAB_REFUNDS) }
        binding.tabAttendees.setOnClickListener      { selectTab(TAB_ATTENDEES) }
        binding.tabAnalytics.setOnClickListener      { selectTab(TAB_ANALYTICS) }

        event?.let { e ->
            showEvent(e)
            e.id?.let { presenter.loadAttendees(it) }
        }

        selectTab(TAB_OVERVIEW)

        // ── Setup pull-to-refresh ──────────────────────────────────
        binding.swipeRefresh.setColorSchemeResources(R.color.purple_main)
        binding.swipeRefresh.setOnRefreshListener {
            event?.id?.let { eventId ->
                presenter.loadAttendees(eventId)
            } ?: run {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    // ── Tab system ─────────────────────────────────────────────

    private fun selectTab(tab: Int) {
        currentTab = tab
        updateTabStyles()
        updateContentBasedOnTab()
    }

    private fun updateTabStyles() {
        val ctx   = requireContext()
        val white  = ContextCompat.getColor(ctx, R.color.white)
        val muted  = ContextCompat.getColor(ctx, R.color.text_muted)

        data class TabEntry(val label: TextView, val indicator: View, val id: Int)

        val tabs = listOf(
            TabEntry(binding.tabOverview,        binding.tabOverviewIndicator,        TAB_OVERVIEW),
            TabEntry(binding.tabPendingReviews,  binding.tabPendingReviewsIndicator,  TAB_PENDING),
            TabEntry(binding.tabRefundRequests,  binding.tabRefundRequestsIndicator,  TAB_REFUNDS),
            TabEntry(binding.tabAttendees,       binding.tabAttendeesIndicator,       TAB_ATTENDEES),
            TabEntry(binding.tabAnalytics,       binding.tabAnalyticsIndicator,       TAB_ANALYTICS),
        )

        tabs.forEach { entry ->
            val active = currentTab == entry.id
            entry.label.setTextColor(if (active) white else muted)
            entry.indicator.visibility = if (active) View.VISIBLE else View.INVISIBLE
        }
    }

    private fun updateContentBasedOnTab() {
        binding.contentContainer.removeAllViews()
        when (currentTab) {
            TAB_OVERVIEW  -> buildOverviewContent()
            TAB_PENDING   -> buildPendingReviewsContent()
            TAB_REFUNDS   -> buildRefundRequestsContent()
            TAB_ATTENDEES -> buildAttendeesContent()
            TAB_ANALYTICS -> buildAnalyticsContent()
        }
    }

    // ── Overview tab ───────────────────────────────────────────

    private fun buildOverviewContent() {
        val ctx       = requireContext()
        val container = binding.contentContainer
        val total     = event?.capacity ?: 0
        val current   = event?.attendeeCount ?: allAttendees.size

        if (total > 0) {
            val progressSection = LinearLayout(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 20.dpToPx() }
                orientation = LinearLayout.VERTICAL
                background  = ContextCompat.getDrawable(ctx, R.drawable.card_bg)
                setPadding(16.dpToPx(), 16.dpToPx(), 16.dpToPx(), 16.dpToPx())
            }

            val slotsLabel = TextView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 8.dpToPx() }
                text      = "$current / $total attendees"
                textSize  = 13f
                setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))
            }
            progressSection.addView(slotsLabel)

            val progressBar = ProgressBar(
                ctx, null, android.R.attr.progressBarStyleHorizontal
            ).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 8.dpToPx()
                )
                max      = total
                progress = current
                progressTintList = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(ctx, R.color.purple_main)
                )
                progressBackgroundTintList = android.content.res.ColorStateList.valueOf(
                    0x33A855F7.toInt()
                )
            }
            progressSection.addView(progressBar)
            container.addView(progressSection)
        }

        container.addView(buildSectionLabel("Quick Actions"))

        val scanCard = buildQuickActionCard(
            icon        = R.drawable.ic_ticket,
            title       = "Scan Tickets",
            desc        = "Verify attendees at the entrance",
            bgColor     = 0x1A7C3AED.toInt(),
            borderColor = 0x337C3AED.toInt()
        ) {
            val fragment = TicketScannerFragment.newInstance(
                onBack = { event?.id?.let { presenter.loadAttendees(it) } }
            )
            parentFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .addToBackStack(null)
                .commit()
        }
        container.addView(scanCard)

        val exportCard = buildQuickActionCard(
            icon        = R.drawable.ic_export,
            title       = "Export Attendees",
            desc        = "Download attendee list as CSV",
            bgColor     = 0x1A10B981.toInt(),
            borderColor = 0x3310B981.toInt()
        ) { toast("Export feature available on the Web.") }
        container.addView(exportCard)

        if (allAttendees.isNotEmpty()) {
            container.addView(buildSummaryCard())
        }
    }

    private fun buildQuickActionCard(
        icon: Int, title: String, desc: String,
        bgColor: Int, borderColor: Int,
        onClick: () -> Unit
    ): LinearLayout {
        val ctx = requireContext()
        return LinearLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 10.dpToPx() }
            orientation = LinearLayout.HORIZONTAL
            gravity     = Gravity.CENTER_VERTICAL
            background  = android.graphics.drawable.GradientDrawable().apply {
                setColor(bgColor)
                setStroke(1.dpToPx(), borderColor)
                cornerRadius = 12.dpToPx().toFloat()
            }
            setPadding(16.dpToPx(), 14.dpToPx(), 16.dpToPx(), 14.dpToPx())
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }

            addView(ImageView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(28.dpToPx(), 28.dpToPx()).apply {
                    marginEnd = 14.dpToPx()
                }
                setImageResource(icon)
            })

            val textCol = LinearLayout(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                )
                orientation = LinearLayout.VERTICAL
            }
            textCol.addView(TextView(ctx).apply {
                text     = title
                textSize = 14f
                typeface = Typeface.defaultFromStyle(Typeface.BOLD)
                setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
            })
            textCol.addView(TextView(ctx).apply {
                text     = desc
                textSize = 12f
                setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
            })
            addView(textCol)

            addView(ImageView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(18.dpToPx(), 18.dpToPx())
                setImageResource(R.drawable.ic_chevron_right)
            })
        }
    }

    private fun buildSummaryCard(): LinearLayout {
        val ctx       = requireContext()
        val pending   = allAttendees.count { it.paymentStatus == "pending" }
        val confirmed = allAttendees.count {
            (it.paymentStatus == "confirmed" || it.status == "confirmed") && it.status != "cancelled"
        }
        val cancelled = allAttendees.count { it.status == "cancelled" }

        return LinearLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 10.dpToPx() }
            orientation = LinearLayout.VERTICAL
            background  = ContextCompat.getDrawable(ctx, R.drawable.card_bg)
            setPadding(16.dpToPx(), 14.dpToPx(), 16.dpToPx(), 14.dpToPx())

            addView(TextView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 10.dpToPx() }
                text     = "Status Breakdown"
                textSize = 13f
                typeface = Typeface.defaultFromStyle(Typeface.BOLD)
                setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
            })

            listOf(
                Triple("Pending",   pending.toString(),   R.color.yellow_accent),
                Triple("Confirmed", confirmed.toString(), R.color.success_green),
                Triple("Cancelled", cancelled.toString(), R.color.text_muted)
            ).forEach { (label, count, colorRes) ->
                val row = LinearLayout(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = 4.dpToPx() }
                    orientation = LinearLayout.HORIZONTAL
                    gravity     = Gravity.CENTER_VERTICAL
                }
                row.addView(TextView(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                    )
                    text     = label
                    textSize = 12f
                    setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
                })
                row.addView(TextView(ctx).apply {
                    text     = count
                    textSize = 13f
                    typeface = Typeface.defaultFromStyle(Typeface.BOLD)
                    setTextColor(ContextCompat.getColor(ctx, colorRes))
                })
                addView(row)
            }
        }
    }

    // ── Pending Reviews tab ────────────────────────────────────

    private fun buildPendingReviewsContent() {
        val ctx       = requireContext()
        val container = binding.contentContainer

        val pendingPayments = allAttendees.filter { a ->
            a.paymentStatus == "pending" &&
                    !a.paymentProofUrl.isNullOrBlank() &&
                    a.status != "cancelled"
        }

        if (pendingPayments.isEmpty()) {
            container.addView(
                emptyStateView(R.drawable.ic_empty_check, "All Caught Up", "No payment proofs awaiting review.")
            )
            return
        }

        container.addView(
            buildAlertBanner(
                iconRes     = R.drawable.ic_warning,
                text        = "${pendingPayments.size} payment proof(s) awaiting your review",
                bgColor     = 0x33FFA500.toInt(),
                borderColor = 0x55FFA500.toInt(),
                textColor   = ContextCompat.getColor(ctx, R.color.yellow_accent)
            )
        )

        pendingPayments.forEach { attendee ->
            container.addView(createPaymentProofCard(attendee))
        }
    }

    private fun createPaymentProofCard(attendee: AttendeeItem): LinearLayout {
        val ctx   = requireContext()
        val price = (event?.price ?: 0.0).toInt()
        val method = when (event?.paymentMethod?.lowercase()) {
            "gcash" -> "GCash"; "maya", "paymaya" -> "Maya"
            "bank"  -> "Bank"; else -> event?.paymentMethod ?: "—"
        }
        val displayName = "${attendee.firstName ?: ""} ${attendee.lastName ?: ""}".trim()
            .ifBlank { attendee.email ?: "Unknown" }
        val initial = displayName.firstOrNull()?.uppercase() ?: "?"
        val registeredDate = attendee.registeredAt?.take(10) ?: ""

        val card = LinearLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12.dpToPx() }
            orientation = LinearLayout.VERTICAL
            background  = ContextCompat.getDrawable(ctx, R.drawable.card_bg)
            setPadding(16.dpToPx(), 16.dpToPx(), 16.dpToPx(), 16.dpToPx())
        }

        // ── Header row ──
        val headerRow = LinearLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 14.dpToPx() }
            orientation = LinearLayout.HORIZONTAL
            gravity     = Gravity.CENTER_VERTICAL
        }

        headerRow.addView(TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(42.dpToPx(), 42.dpToPx()).apply {
                marginEnd = 12.dpToPx()
            }
            text      = initial
            textSize  = 15f
            typeface  = Typeface.defaultFromStyle(Typeface.BOLD)
            gravity   = Gravity.CENTER
            setTextColor(ContextCompat.getColor(ctx, R.color.purple_light))
            background = android.graphics.drawable.GradientDrawable().apply {
                shape        = android.graphics.drawable.GradientDrawable.OVAL
                setColor(0x1AA855F7.toInt())
            }
        })

        val nameCol = LinearLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            orientation  = LinearLayout.VERTICAL
        }
        nameCol.addView(TextView(ctx).apply {
            text     = displayName
            textSize = 14f
            typeface = Typeface.defaultFromStyle(Typeface.BOLD)
            setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        })
        if (registeredDate.isNotBlank()) {
            nameCol.addView(TextView(ctx).apply {
                text     = registeredDate
                textSize = 12f
                setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
            })
        }
        headerRow.addView(nameCol)

        headerRow.addView(TextView(ctx).apply {
            text     = "Pending"
            textSize = 11f
            typeface = Typeface.defaultFromStyle(Typeface.BOLD)
            setTextColor(ContextCompat.getColor(ctx, R.color.yellow_accent))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0x22EAB308.toInt())
                setStroke(1.dpToPx(), 0x55EAB308.toInt())
                cornerRadius = 20.dpToPx().toFloat()
            }
            setPadding(10.dpToPx(), 4.dpToPx(), 10.dpToPx(), 4.dpToPx())
        })
        card.addView(headerRow)

        card.addView(View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1.dpToPx()
            ).apply { bottomMargin = 14.dpToPx() }
            setBackgroundColor(0x1AFFFFFF)
        })

        if (price > 0) {
            val statRow = LinearLayout(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 14.dpToPx() }
                orientation = LinearLayout.HORIZONTAL
            }
            listOf("Amount" to "₱${"%,d".format(price)}", "Method" to method).forEach { (label, value) ->
                statRow.addView(LinearLayout(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        marginEnd = 10.dpToPx()
                    }
                    orientation = LinearLayout.VERTICAL
                    background  = android.graphics.drawable.GradientDrawable().apply {
                        setColor(0x11FFFFFF.toInt())
                        cornerRadius = 8.dpToPx().toFloat()
                    }
                    setPadding(12.dpToPx(), 10.dpToPx(), 12.dpToPx(), 10.dpToPx())
                    addView(TextView(ctx).apply {
                        text     = label
                        textSize = 11f
                        setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
                    })
                    addView(TextView(ctx).apply {
                        text     = value
                        textSize = 15f
                        typeface = Typeface.defaultFromStyle(Typeface.BOLD)
                        setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
                    })
                })
            }
            card.addView(statRow)
        }

        card.addView(Button(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 44.dpToPx()
            ).apply { bottomMargin = 10.dpToPx() }
            text     = "View payment proof"
            textSize = 13f
            typeface = Typeface.defaultFromStyle(Typeface.BOLD)
            setTextColor(ContextCompat.getColor(ctx, R.color.purple_light))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0x1AA855F7.toInt())
                setStroke(1.dpToPx(), 0x55A855F7.toInt())
                cornerRadius = 10.dpToPx().toFloat()
            }
            setOnClickListener { showPaymentProofDialog(attendee) }
        })

        card.addView(buildActionButtonRow(
            positiveLabel = "Approve",
            positiveIcon  = 0,
            negativeLabel = "Reject",
            negativeIcon  = 0,
            onPositive    = {
                val eventId = event?.id ?: return@buildActionButtonRow
                if (event?.seatingType == "reserved") promptApproveWithSeat(eventId, attendee.id ?: 0)
                else AlertDialog.Builder(ctx, R.style.HangOutAlertDialogTheme)
                    .setTitle("Approve payment")
                    .setMessage("Approve ${displayName}'s payment?")
                    .setPositiveButton("Approve") { _, _ -> presenter.approvePayment(eventId, attendee.id ?: 0, null) }
                    .setNegativeButton("Cancel", null).show()
            },
            onNegative = {
                val eventId = event?.id ?: return@buildActionButtonRow
                promptWithReason("Reject payment", "Reason for rejection (shown to attendee):") { reason ->
                    presenter.rejectPayment(eventId, attendee.id ?: 0, reason)
                }
            }
        ))

        return card
    }

    // ── Refund Requests tab ────────────────────────────────────

    private fun buildRefundRequestsContent() {
        val ctx       = requireContext()
        val container = binding.contentContainer

        val refundPending = allAttendees.filter {
            it.refundStatus == "pending" || it.refundStatus == "waiting_acknowledgement"
        }

        if (refundPending.isEmpty()) {
            container.addView(
                emptyStateView(R.drawable.ic_empty_check, "No Refund Requests", "All refund requests have been resolved.")
            )
            return
        }

        val activePending = refundPending.count { it.refundStatus == "pending" }
        if (activePending > 0) {
            container.addView(
                buildAlertBanner(
                    iconRes     = R.drawable.ic_warning,
                    text        = "$activePending refund request(s) awaiting your review",
                    bgColor     = 0x33EF4444.toInt(),
                    borderColor = 0x55EF4444.toInt(),
                    textColor   = ContextCompat.getColor(ctx, R.color.red_accent)
                )
            )
        }

        refundPending.forEach { attendee ->
            container.addView(createRefundCard(attendee))
        }
    }

    @SuppressLint("SuspiciousIndentation")
    private fun createRefundCard(attendee: AttendeeItem): LinearLayout {
        val ctx  = requireContext()
        val card = LinearLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12.dpToPx() }
            orientation = LinearLayout.VERTICAL
            background  = ContextCompat.getDrawable(ctx, R.drawable.card_bg)
            setPadding(16.dpToPx(), 16.dpToPx(), 16.dpToPx(), 16.dpToPx())
        }

        // ── Top row: avatar + name + badge ──
        val topRow = LinearLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12.dpToPx() }
            orientation = LinearLayout.HORIZONTAL
            gravity     = Gravity.CENTER_VERTICAL
        }

        topRow.addView(ImageView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(40.dpToPx(), 40.dpToPx()).apply {
                marginEnd = 12.dpToPx()
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
            Glide.with(this@HostDashboardFragment)
                .load(attendee.photo.takeIf { !it.isNullOrBlank() })
                .placeholder(R.drawable.ic_avatar_placeholder)
                .error(R.drawable.ic_avatar_placeholder)
                .circleCrop()
                .into(this)
        })

        val nameCol = LinearLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            orientation = LinearLayout.VERTICAL
        }
        nameCol.addView(TextView(ctx).apply {
            text     = "${attendee.firstName ?: ""} ${attendee.lastName ?: ""}".trim()
                .ifBlank { attendee.email ?: "Unknown" }
            textSize = 14f
            typeface = Typeface.defaultFromStyle(Typeface.BOLD)
            setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
        })
        nameCol.addView(TextView(ctx).apply {
            text     = attendee.email ?: ""
            textSize = 12f
            setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
        })
        topRow.addView(nameCol)

        val (statusText, statusBg, statusBorder, statusColorRes) = when (attendee.refundStatus) {
            "pending"                 -> Quad("Pending Review",   0x33EAB308.toInt(), 0x55EAB308.toInt(), R.color.yellow_accent)
            "waiting_acknowledgement" -> Quad("Awaiting Ack.",    0x1AA855F7.toInt(), 0x33A855F7.toInt(), R.color.purple_light)
            else                      -> Quad("Requested",        0x22FFFFFF.toInt(), 0x33FFFFFF.toInt(), R.color.text_muted)
        }
        topRow.addView(TextView(ctx).apply {
            text     = statusText
            textSize = 11f
            typeface = Typeface.defaultFromStyle(Typeface.BOLD)
            setTextColor(ContextCompat.getColor(ctx, statusColorRes))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(statusBg)
                setStroke(1.dpToPx(), statusBorder)
                cornerRadius = 6.dpToPx().toFloat()
            }
            setPadding(8.dpToPx(), 4.dpToPx(), 8.dpToPx(), 4.dpToPx())
        })
        card.addView(topRow)

        // ── Divider ──
        card.addView(View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1.dpToPx()
            ).apply { bottomMargin = 12.dpToPx() }
            setBackgroundColor(0x1AFFFFFF)
        })

        // ── Cancellation reason (shown when provided) ──────────
        if (!attendee.cancellationReason.isNullOrBlank()) {
            val reasonBox = LinearLayout(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12.dpToPx() }
                orientation = LinearLayout.VERTICAL
                background  = android.graphics.drawable.GradientDrawable().apply {
                    setColor(0x0DFFFFFF.toInt())
                    setStroke(1.dpToPx(), 0x1AFFFFFF.toInt())
                    cornerRadius = 8.dpToPx().toFloat()
                }
                setPadding(12.dpToPx(), 10.dpToPx(), 12.dpToPx(), 10.dpToPx())
            }
            reasonBox.addView(TextView(ctx).apply {
                text     = "Cancellation Reason"
                textSize = 11f
                typeface = Typeface.defaultFromStyle(Typeface.BOLD)
                setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 4.dpToPx() }
            })
            reasonBox.addView(TextView(ctx).apply {
                text     = attendee.cancellationReason
                textSize = 13f
                setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
                setLineSpacing(0f, 1.4f)
            })
            card.addView(reasonBox)
        }

        // ── Refund amount row ──
        val price = (event?.price ?: 0.0).toInt()
        if (price > 0) {
            val amountRow = LinearLayout(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12.dpToPx() }
                orientation = LinearLayout.HORIZONTAL
                gravity     = Gravity.CENTER_VERTICAL
            }
            amountRow.addView(LinearLayout(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                orientation = LinearLayout.VERTICAL
                addView(TextView(ctx).apply {
                    text     = "Refund Amount"
                    textSize = 11f
                    this.setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
                })
                addView(TextView(ctx).apply {
                    text     = "\u20B1$price"
                    textSize = 18f
                    typeface = Typeface.defaultFromStyle(Typeface.BOLD)
                    setTextColor(ContextCompat.getColor(ctx, R.color.red_accent))
                })
            })
            card.addView(amountRow)
        }

        // ── Action buttons or status note ──
        when (attendee.refundStatus) {
            "pending" -> {
                card.addView(buildActionButtonRow(
                    positiveLabel = "Approve Refund",
                    positiveIcon  = 0,
                    negativeLabel = "Decline",
                    negativeIcon  = 0,
                    onPositive    = {
                        val eventId = event?.id ?: return@buildActionButtonRow
                        promptRefundApproval(eventId, attendee.id ?: 0, attendee)
                    },
                    onNegative    = {
                        val eventId = event?.id ?: return@buildActionButtonRow
                        promptWithReason("Decline Refund", "Reason for declining (shown to attendee):") { reason ->
                            presenter.rejectRefund(eventId, attendee.id ?: 0, reason)
                        }
                    }
                ))
            }
            "waiting_acknowledgement" -> {
                card.addView(LinearLayout(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    orientation = LinearLayout.HORIZONTAL
                    gravity     = Gravity.CENTER_VERTICAL
                    background  = android.graphics.drawable.GradientDrawable().apply {
                        setColor(0x1AA855F7.toInt())
                        setStroke(1.dpToPx(), 0x33A855F7.toInt())
                        cornerRadius = 8.dpToPx().toFloat()
                    }
                    setPadding(12.dpToPx(), 10.dpToPx(), 12.dpToPx(), 10.dpToPx())

                    addView(ImageView(ctx).apply {
                        layoutParams = LinearLayout.LayoutParams(16.dpToPx(), 16.dpToPx()).apply {
                            marginEnd = 8.dpToPx()
                        }
                        setImageResource(R.drawable.ic_clock)
                        setColorFilter(ContextCompat.getColor(ctx, R.color.purple_light))
                    })
                    addView(TextView(ctx).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                        )
                        text     = "Refund processed — awaiting attendee acknowledgement"
                        textSize = 12f
                        setTextColor(ContextCompat.getColor(ctx, R.color.purple_light))
                    })
                })
            }
        }

        return card
    }

    // ── Attendees tab ──────────────────────────────────────────

    private var attendeeSearchQuery = ""

    private fun buildAttendeesContent() {
        val ctx       = requireContext()
        val container = binding.contentContainer

        if (allAttendees.isEmpty()) {
            container.addView(
                emptyStateView(R.drawable.ic_empty_group, "No Attendees Yet", "No one has RSVPed to this event yet.")
            )
            return
        }

        val searchBox = LinearLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 14.dpToPx() }
            orientation = LinearLayout.HORIZONTAL
            gravity     = Gravity.CENTER_VERTICAL
            background  = android.graphics.drawable.GradientDrawable().apply {
                setColor(0x22FFFFFF.toInt())
                setStroke(1.dpToPx(), 0x1AFFFFFF.toInt())
                cornerRadius = 10.dpToPx().toFloat()
            }
            setPadding(12.dpToPx(), 0, 12.dpToPx(), 0)
        }

        val searchIcon = ImageView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(18.dpToPx(), 18.dpToPx()).apply {
                marginEnd = 8.dpToPx()
            }
            setImageResource(R.drawable.ic_search)
            alpha = 0.5f
        }
        searchBox.addView(searchIcon)

        val searchInput = android.widget.EditText(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, 42.dpToPx(), 1f)
            hint      = "Search by name, email or seat…"
            textSize  = 13f
            setHintTextColor(0x66FFFFFF.toInt())
            setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
            background = null
            setPadding(0, 0, 0, 0)
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            maxLines  = 1
        }
        searchBox.addView(searchInput)
        container.addView(searchBox)

        val headerRow = LinearLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12.dpToPx() }
            orientation = LinearLayout.HORIZONTAL
            gravity     = Gravity.CENTER_VERTICAL
        }
        headerRow.addView(TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            text     = "Attendee List (${allAttendees.size})"
            textSize = 14f
            typeface = Typeface.defaultFromStyle(Typeface.BOLD)
            setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
        })
        container.addView(headerRow)

        val listContainer = LinearLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
        }
        container.addView(listContainer)

        fun renderList(query: String) {
            listContainer.removeAllViews()
            val filtered = if (query.isBlank()) allAttendees else allAttendees.filter {
                val name = "${it.firstName ?: ""} ${it.lastName ?: ""}".trim()
                name.contains(query, ignoreCase = true) ||
                        it.email.orEmpty().contains(query, ignoreCase = true) ||
                        it.seatNumber.orEmpty().contains(query, ignoreCase = true)
            }

            if (filtered.isEmpty()) {
                listContainer.addView(TextView(ctx).apply {
                    text     = "No attendees match \"$query\""
                    textSize = 13f
                    setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
                    gravity = Gravity.CENTER
                    setPadding(0, 32.dpToPx(), 0, 32.dpToPx())
                })
                return
            }

            val sorted = filtered.sortedWith(
                compareByDescending<AttendeeItem> { it.refundStatus == "pending" }
                    .thenByDescending { it.paymentStatus == "pending" }
            )
            sorted.forEach { attendee ->
                val row = ItemAttendeeSlimRowBinding.inflate(layoutInflater, listContainer, false)
                bindAttendeeSlimRow(row, attendee)
                listContainer.addView(row.root)
            }
        }

        renderList("")

        searchInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                attendeeSearchQuery = s?.toString() ?: ""
                renderList(attendeeSearchQuery)
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    // ── Analytics tab ──────────────────────────────────────────

    private fun buildAnalyticsContent() {
        val ctx       = requireContext()
        val container = binding.contentContainer
        val total     = event?.capacity ?: 0
        val current   = event?.attendeeCount ?: allAttendees.size
        val confirmed = allAttendees.count { it.paymentStatus == "confirmed" || it.status == "confirmed" }
        val pending   = allAttendees.count { it.paymentStatus == "pending" }
        val cancelled = allAttendees.count { it.status == "cancelled" }
        val price     = (event?.price ?: 0.0).toInt()
        val revenue = calculateRevenue(allAttendees, price)

        container.addView(buildSectionLabel("Event Analytics"))

        val analyticsCard = LinearLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16.dpToPx() }
            orientation = LinearLayout.VERTICAL
            background  = ContextCompat.getDrawable(ctx, R.drawable.card_bg)
            setPadding(16.dpToPx(), 16.dpToPx(), 16.dpToPx(), 16.dpToPx())
        }

        listOf(
            Triple("Total Capacity",    "$total",       R.color.text_primary),
            Triple("Current Attendees", "$current",     R.color.text_primary),
            Triple("Confirmed",         "$confirmed",   R.color.success_green),
            Triple("Pending Payment",   "$pending",     R.color.yellow_accent),
            Triple("Cancelled",         "$cancelled",   R.color.text_muted),
            Triple("Capacity Filled",   if (total > 0) "${current * 100 / total}%" else "—", R.color.purple_light),
            Triple(
                "Total Revenue",
                if (price > 0) "₱$revenue" else "Free Event",
                R.color.purple_light
            )

        ).forEachIndexed { index, (label, value, colorRes) ->
            val row = LinearLayout(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 10.dpToPx() }
                orientation = LinearLayout.HORIZONTAL
                gravity     = Gravity.CENTER_VERTICAL
            }
            row.addView(TextView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text     = label
                textSize = 13f
                setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
            })
            row.addView(TextView(ctx).apply {
                text     = value
                textSize = 14f
                typeface = Typeface.defaultFromStyle(Typeface.BOLD)
                setTextColor(ContextCompat.getColor(ctx, colorRes))
            })
            analyticsCard.addView(row)

            if (index < 6) {
                analyticsCard.addView(View(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1.dpToPx()
                    ).apply { bottomMargin = 10.dpToPx() }
                    setBackgroundColor(0x1AFFFFFF)
                })
            }
        }
        container.addView(analyticsCard)

        if (total > 0) {
            container.addView(buildSectionLabel("Capacity"))
            val progressCard = LinearLayout(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                orientation = LinearLayout.VERTICAL
                background  = ContextCompat.getDrawable(ctx, R.drawable.card_bg)
                setPadding(16.dpToPx(), 14.dpToPx(), 16.dpToPx(), 14.dpToPx())
            }
            progressCard.addView(TextView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 8.dpToPx() }
                text     = "$current of $total spots filled (${if (total > 0) current * 100 / total else 0}%)"
                textSize = 12f
                setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
            })
            progressCard.addView(ProgressBar(ctx, null, android.R.attr.progressBarStyleHorizontal).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 10.dpToPx()
                )
                max      = total
                progress = current
                progressTintList = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(ctx, R.color.purple_main)
                )
                progressBackgroundTintList = android.content.res.ColorStateList.valueOf(0x33A855F7.toInt())
            })
            container.addView(progressCard)
        }
    }

    // ── HostDashboardContract.View ─────────────────────────────

    override fun showEvent(event: EventItem) {
        this.event = event
        binding.tvEventTitle.text    = event.title ?: "Untitled"
        val start = event.startTime ?: event.time ?: ""
        val end   = event.endTime
        val timeStr = when {
            start.isBlank()    -> "Time TBD"
            !end.isNullOrBlank() -> "${com.hangout.app.utils.formatTime12Hr(start)} – ${com.hangout.app.utils.formatTime12Hr(end)}"
            else               -> com.hangout.app.utils.formatTime12Hr(start)
        }
        binding.tvEventMeta.text = "${event.date ?: "—"}  ·  $timeStr"
        binding.tvEventLocation.text = event.location ?: "Virtual / TBD"

        val imageUrl = event.imageUrl
        if (!imageUrl.isNullOrBlank()) {
            Glide.with(this).load(imageUrl).centerCrop().into(binding.ivHeroImage)
        }

        val total   = event.capacity ?: 0
        val current = event.attendeeCount ?: 0
        binding.tvTotalAttendeesCount.text = current.toString()
        binding.tvSlotsLeft.text           = "${total - current} spots left"
        binding.tvPendingReviewsCount.text = "—"
        binding.tvConfirmedCount.text      = "—"
        val price = (event.price ?: 0.0).toInt()
        binding.tvTotalRevenue.text = if (price > 0) "\u20B1—" else "Free"

        binding.tvCancelledBanner.showIf(event.eventStatus == "cancelled")
    }

    override fun showAttendees(attendees: List<AttendeeItem>) {
        allAttendees = attendees

        val total     = event?.capacity ?: 0
        val current   = event?.attendeeCount ?: attendees.size
        val pending   = attendees.count {
            it.paymentStatus == "pending" && !it.paymentProofUrl.isNullOrBlank() && it.status != "cancelled"
        }
        val confirmed = attendees.count {
            (it.paymentStatus == "confirmed" || it.status == "confirmed") && it.status != "cancelled"
        }
        val refunds   = attendees.count { it.refundStatus == "pending" }
        val price     = (event?.price ?: 0.0).toInt()
        val revenue = calculateRevenue(attendees, price)

        binding.tvTotalAttendeesCount.text = current.toString()
        binding.tvSlotsLeft.text           = "${total - current} spots left"
        binding.tvPendingReviewsCount.text = pending.toString()
        binding.tvPendingNeedsAttention.showIf(pending > 0)
        binding.tvConfirmedCount.text      = confirmed.toString()
        binding.tvTotalRevenue.text        = if (price > 0) "\u20B1$revenue" else "Free"

        updateBadge(binding.tvPendingReviewsBadge, pending)
        updateBadge(binding.tvRefundRequestsBadge, refunds)
        updateBadge(binding.tvAttendeesBadge,      attendees.size)

        updateContentBasedOnTab()
    }

    private fun updateBadge(badge: TextView, count: Int) {
        if (count > 0) { badge.text = count.toString(); badge.show() }
        else badge.hide()
    }

    override fun showLoading(show: Boolean) {
        binding.progressBar.showIf(show)
        binding.swipeRefresh.isRefreshing = show
    }

    override fun showMessage(message: String) = toast(message)

    override fun onActionSuccess(rsvpId: Long, newStatus: String) {
        event?.id?.let { presenter.loadAttendees(it) }
    }

    override fun onEventCancelled() {
        toast("Event cancelled.")
        binding.tvCancelledBanner.show()
        onBackCallback?.invoke()
        parentFragmentManager.popBackStack()
    }

    override fun onEventDeleted() {
        toast("Event deleted.")
        onBackCallback?.invoke()
        parentFragmentManager.popBackStack()
    }

    // ── Attendee row binding ───────────────────────────────────

    private fun bindAttendeeRow(row: ItemAttendeeRowBinding, attendee: AttendeeItem) {
        val ctx     = requireContext()
        val eventId = event?.id ?: return
        val rsvpId  = attendee.id

        row.tvAttendeeName.text = attendee.name
            ?.takeIf { it.isNotBlank() }
            ?: "${attendee.firstName ?: ""} ${attendee.lastName ?: ""}".trim()
                .ifBlank { attendee.email ?: "Unknown" }
        row.tvAttendeeEmail.text = attendee.email ?: ""

        if (!attendee.photo.isNullOrBlank()) {
            Glide.with(this).load(attendee.photo).circleCrop().into(row.ivAvatar)
        }

        row.tvRegisteredDate.text = attendee.registeredAt
            ?.take(10)
            ?.let { "Registered $it" }
            ?: ""

        row.tvSeatNumber.text = if (!attendee.seatNumber.isNullOrBlank()) "Seat ${attendee.seatNumber}" else "No Seat"
        row.layoutSeat.setOnClickListener { promptAssignSeat(eventId, rsvpId) }

        val (label, colorRes) = resolveStatusDisplay(attendee)
        row.tvStatusBadge.text = label
        row.tvStatusBadge.setTextColor(ContextCompat.getColor(ctx, colorRes))

        // ── Cancellation reason chip (shown in attendee row when cancelled) ──
        if (!attendee.cancellationReason.isNullOrBlank() && attendee.status == "cancelled") {
            row.tvCancellationReason.text      = "\"${attendee.cancellationReason}\""
            row.tvCancellationReason.visibility = View.VISIBLE
        } else {
            row.tvCancellationReason.visibility = View.GONE
        }

        bindRefundChip(row, attendee)

        // ── Attendance dropdown ──
        val attendanceOptions = listOf(
            "not_marked"  to "Not Marked",
            "attended"    to "✓ Attended",
            "no_show"     to "No Show"
        )
        val currentStatus = attendee.attendeeStatus ?: "not_marked"
        val currentLabel  = attendanceOptions.find { it.first == currentStatus }?.second ?: "Not Marked"
        val currentColor  = when (currentStatus) {
            "attended" -> ContextCompat.getColor(ctx, R.color.success_green)
            "no_show"  -> ContextCompat.getColor(ctx, R.color.red_accent)
            else       -> ContextCompat.getColor(ctx, R.color.text_muted)
        }
        row.tvAttendanceStatus.text = currentLabel
        row.tvAttendanceStatus.setTextColor(currentColor)

        row.btnAttendanceDropdown.setOnClickListener { anchor ->
            val popup = createStyledPopupMenu(ctx, anchor)
            attendanceOptions.forEachIndexed { i, (_, optLabel) ->
                popup.menu.add(0, i, i, optLabel)
            }
            popup.setOnMenuItemClickListener { item ->
                val (statusKey, statusLabel) = attendanceOptions[item.itemId]
                row.tvAttendanceStatus.text = statusLabel
                row.tvAttendanceStatus.setTextColor(
                    when (statusKey) {
                        "attended" -> ContextCompat.getColor(ctx, R.color.success_green)
                        "no_show"  -> ContextCompat.getColor(ctx, R.color.red_accent)
                        else       -> ContextCompat.getColor(ctx, R.color.text_muted)
                    }
                )
                presenter.markAttendance(eventId, rsvpId, statusKey)
                true
            }
            popup.show()
        }

        val isPendingPayment  = attendee.paymentStatus == "pending" &&
                !attendee.paymentProofUrl.isNullOrBlank() &&
                attendee.status != "cancelled"
        val hasProof          = !attendee.paymentProofUrl.isNullOrBlank()
        val isReservedSeating = event?.seatingType == "reserved"
        val canAssignSeat     = attendee.paymentStatus == "confirmed" &&
                isReservedSeating && attendee.seatNumber.isNullOrBlank()

        row.layoutPaymentActions.showIf(hasProof || isPendingPayment || canAssignSeat)
        row.btnViewProof.showIf(hasProof)
        row.btnApprove.showIf(isPendingPayment)
        row.btnReject.showIf(isPendingPayment)
        row.btnAssignSeat.showIf(canAssignSeat)

        row.btnViewProof.setOnClickListener { showPaymentProofDialog(attendee) }
        row.btnApprove.setOnClickListener {
            if (isReservedSeating) promptApproveWithSeat(eventId, rsvpId)
            else AlertDialog.Builder(ctx, R.style.HangOutAlertDialogTheme)
                .setTitle("Approve Payment")
                .setMessage("Approve ${attendee.firstName}'s payment?")
                .setPositiveButton("Approve") { _, _ -> presenter.approvePayment(eventId, rsvpId, null) }
                .setNegativeButton("Cancel", null)
                .show()
        }
        row.btnReject.setOnClickListener {
            promptWithReason("Reject Payment", "Reason for rejection (shown to attendee):") { reason ->
                presenter.rejectPayment(eventId, rsvpId, reason)
            }
        }
        row.btnAssignSeat.setOnClickListener { promptAssignSeat(eventId, rsvpId) }

        val hasPendingRefund = attendee.refundStatus == "pending"
        row.layoutRefundActions.showIf(hasPendingRefund)
        if (hasPendingRefund) {
            row.btnApproveRefund.setOnClickListener { promptRefundApproval(eventId, rsvpId, attendee) }
            row.btnRejectRefund.setOnClickListener {
                promptRejectRefund(eventId, rsvpId, attendee)
            }
        }
    }

    private fun bindAttendeeSlimRow(row: ItemAttendeeSlimRowBinding, attendee: AttendeeItem) {
        val ctx = requireContext()
        val eventId = event?.id ?: return

        // Bind name
        row.tvAttendeeName.text = attendee.name
            ?.takeIf { it.isNotBlank() }
            ?: "${attendee.firstName ?: ""} ${attendee.lastName ?: ""}".trim()
                .ifBlank { attendee.email ?: "Unknown" }

        // Bind avatar - fix clipping and show profile picture
        if (!attendee.photo.isNullOrBlank()) {
            Glide.with(this)
                .load(attendee.photo)
                .centerCrop()
                .circleCrop()
                .into(row.ivAvatar)
        } else {
            row.ivAvatar.setImageResource(R.drawable.ic_user)
        }

        // Bind seat number - ensure it shows correctly
        val seatDisplay = if (!attendee.seatNumber.isNullOrBlank()) {
            "Seat ${attendee.seatNumber}"
        } else {
            "—"
        }
        row.tvSeatNumber.text = seatDisplay

        // Bind status badge
        val (label, colorRes) = resolveStatusDisplay(attendee)
        row.tvStatusBadge.text = label
        row.tvStatusBadge.setTextColor(ContextCompat.getColor(ctx, colorRes))

        // Make seat clickable to edit directly from row
        row.layoutSeat.setOnClickListener {
            promptAssignSeat(eventId, attendee.id)
        }

        // Click listeners for profile information
        val displayName = attendee.name
            ?.takeIf { it.isNotBlank() }
            ?: "${attendee.firstName ?: ""} ${attendee.lastName ?: ""}".trim()
                .ifBlank { attendee.email ?: "Unknown" }
        
        val profileClickListener = View.OnClickListener {
            showProfileInformation(displayName, attendee)
        }
        row.tvAttendeeName.setOnClickListener(profileClickListener)
        row.ivAvatar.setOnClickListener(profileClickListener)

        // Expand button - open full details dialog
        row.btnExpand.setOnClickListener {
            showAttendeeDetailsDialog(attendee)
        }
    }

    private fun showAttendeeDetailsDialog(attendee: AttendeeItem) {
        val ctx = requireContext()
        val eventId = event?.id ?: return

        val dialogView = layoutInflater.inflate(R.layout.dialog_attendee_details, null)
        val binding = DialogAttendeeDetailsBinding.bind(dialogView)

        // Bind data to dialog
        val displayName = attendee.name
            ?.takeIf { it.isNotBlank() }
            ?: "${attendee.firstName ?: ""} ${attendee.lastName ?: ""}".trim()
                .ifBlank { attendee.email ?: "Unknown" }
        binding.tvDialogAttendeeName.text = displayName
        binding.tvDialogAttendeeEmail.text = attendee.email ?: "—"

        // Avatar
        if (!attendee.photo.isNullOrBlank()) {
            Glide.with(this)
                .load(attendee.photo)
                .centerCrop()
                .circleCrop()
                .into(binding.ivAttendeePhoto)
        } else {
            binding.ivAttendeePhoto.setImageResource(R.drawable.ic_user)
        }

        // Make header clickable to show profile information
        val headerClickListener = View.OnClickListener {
            showProfileInformation(displayName, attendee)
        }
        binding.tvDialogAttendeeName.setOnClickListener(headerClickListener)
        binding.ivAttendeePhoto.setOnClickListener(headerClickListener)

        // Status badge
        val (statusLabel, statusColorRes) = resolveStatusDisplay(attendee)
        binding.tvDialogStatusBadge.text = statusLabel
        binding.tvDialogStatusBadge.setTextColor(ContextCompat.getColor(ctx, statusColorRes))

        // Seat info
        binding.tvDialogSeat.text = if (!attendee.seatNumber.isNullOrBlank()) {
            "Seat ${attendee.seatNumber}"
        } else {
            "Not assigned"
        }

        // Registered date
        binding.tvDialogRegisteredDate.text = attendee.registeredAt?.take(10) ?: "—"

        // Payment status
        binding.tvDialogPaymentStatus.text = attendee.paymentStatus?.replaceFirstChar { it.uppercase() } ?: "—"

        // Attendance status
        binding.tvDialogAttendanceStatus.text = when (attendee.attendeeStatus) {
            "attended"  -> "Attended"
            "no_show"   -> "No Show"
            else        -> "Not Marked"
        }

        // Cancellation reason
        if (!attendee.cancellationReason.isNullOrBlank() && attendee.status == "cancelled") {
            binding.tvDialogCancellationReason.text = "Cancellation: ${attendee.cancellationReason}"
            binding.tvDialogCancellationReason.visibility = View.VISIBLE
        }

        // Configure action buttons visibility
        val isPendingPayment = attendee.paymentStatus == "pending" &&
                !attendee.paymentProofUrl.isNullOrBlank() &&
                attendee.status != "cancelled"
        val hasProof = !attendee.paymentProofUrl.isNullOrBlank()
        val isReservedSeating = event?.seatingType == "reserved"
        val canAssignSeat = attendee.paymentStatus == "confirmed" &&
                isReservedSeating && attendee.seatNumber.isNullOrBlank()
        val hasPendingRefund = attendee.refundStatus == "pending"

        binding.btnDialogViewProof.showIf(hasProof)
        binding.btnDialogApprovePayment.showIf(isPendingPayment)
        binding.btnDialogRejectPayment.showIf(isPendingPayment)
        binding.btnDialogAssignSeat.showIf(canAssignSeat)
        binding.btnDialogMarkAttendance.show()
        binding.btnDialogApproveRefund.showIf(hasPendingRefund)
        binding.btnDialogRejectRefund.showIf(hasPendingRefund)

        // Button click listeners
        binding.btnDialogViewProof.setOnClickListener { showPaymentProofDialog(attendee) }
        binding.btnDialogApprovePayment.setOnClickListener {
            AlertDialog.Builder(ctx, R.style.HangOutAlertDialogTheme)
                .setTitle("Approve Payment")
                .setMessage("Approve ${attendee.firstName}'s payment?")
                .setPositiveButton("Approve") { _, _ -> presenter.approvePayment(eventId, attendee.id, null) }
                .setNegativeButton("Cancel", null)
                .show()
        }
        binding.btnDialogRejectPayment.setOnClickListener {
            promptWithReason("Reject Payment", "Reason for rejection:") { reason ->
                presenter.rejectPayment(eventId, attendee.id, reason)
            }
        }
        binding.btnDialogAssignSeat.setOnClickListener { promptAssignSeat(eventId, attendee.id) }
        binding.btnDialogMarkAttendance.setOnClickListener {
            val attendanceOptions = listOf(
                "not_marked" to "Not Marked",
                "attended" to "✓ Attended",
                "no_show" to "No Show"
            )
            val popup = createStyledPopupMenu(ctx, binding.btnDialogMarkAttendance)
            attendanceOptions.forEachIndexed { i, (_, optLabel) ->
                popup.menu.add(0, i, i, optLabel)
            }
            popup.setOnMenuItemClickListener { item ->
                val (statusKey, _) = attendanceOptions[item.itemId]
                presenter.markAttendance(eventId, attendee.id, statusKey)
                true
            }
            popup.show()
        }
        binding.btnDialogApproveRefund.setOnClickListener { promptRefundApproval(eventId, attendee.id, attendee) }
        binding.btnDialogRejectRefund.setOnClickListener { promptRejectRefund(eventId, attendee.id, attendee) }

        // Show dialog
        AlertDialog.Builder(ctx, R.style.HangOutAlertDialogTheme)
            .setView(dialogView)
            .setNeutralButton("Close", null)
            .show()
    }

    private fun bindRefundChip(row: ItemAttendeeRowBinding, attendee: AttendeeItem) {
        val ctx = requireContext()
        when (attendee.refundStatus) {
            "pending" -> {
                row.tvRefundChip.text = "Refund requested"
                row.tvRefundChip.setTextColor(ContextCompat.getColor(ctx, R.color.yellow_accent))
                row.tvRefundChip.setBackgroundColor(0x33EAB308.toInt())
                row.tvRefundChip.show()
            }
            "waiting_acknowledgement" -> {
                row.tvRefundChip.text = "Refund processed — awaiting acknowledgement"
                row.tvRefundChip.setTextColor(ContextCompat.getColor(ctx, R.color.purple_light))
                row.tvRefundChip.setBackgroundColor(0x33A855F7.toInt())
                row.tvRefundChip.show()
            }
            "completed" -> {
                row.tvRefundChip.text = "Refund completed"
                row.tvRefundChip.setTextColor(ContextCompat.getColor(ctx, R.color.success_green))
                row.tvRefundChip.setBackgroundColor(0x3322C55E.toInt())
                row.tvRefundChip.show()
            }
            "rejected" -> {
                row.tvRefundChip.text = "Refund declined"
                row.tvRefundChip.setTextColor(ContextCompat.getColor(ctx, R.color.red_accent))
                row.tvRefundChip.setBackgroundColor(0x33EF4444.toInt())
                row.tvRefundChip.show()
            }
            else -> row.tvRefundChip.hide()
        }
    }

    private fun resolveStatusDisplay(a: AttendeeItem): Pair<String, Int> = when {
        a.status         == "cancelled" -> "Cancelled"        to R.color.text_muted
        a.attendeeStatus == "rejected"  -> "Rejected"         to R.color.red_accent
        a.attendeeStatus == "attended"  -> "Attended"         to R.color.success_green
        a.paymentStatus  == "rejected"  -> "Pay Rejected"     to R.color.red_accent
        a.refundStatus   == "pending"   -> "Refund Requested" to R.color.yellow_accent
        a.paymentStatus  == "pending"   -> "Pending Payment"  to R.color.yellow_accent
        a.paymentStatus  == "confirmed" -> "Confirmed"        to R.color.success_green
        a.status         == "confirmed" -> "Confirmed"        to R.color.success_green
        else                            -> "Registered"       to R.color.purple_light
    }

    // ── More options menu ──────────────────────────────────────

    private fun showMoreOptionsMenu(anchor: View) {
        val popup = createStyledPopupMenu(requireContext(), anchor)
        popup.menu.add(0, 1, 0, "Scan Tickets")
        popup.menu.add(0, 2, 1, "Export Attendees")
        popup.menu.add(1, 3, 2, "Cancel Event")
        popup.menu.add(1, 4, 3, "Delete Event")
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    val fragment = TicketScannerFragment.newInstance(
                        onBack = { event?.id?.let { presenter.loadAttendees(it) } }
                    )
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.nav_host_fragment, fragment)
                        .addToBackStack(null)
                        .commit()
                    true
                }
                2    -> { toast("Export feature coming soon."); true }
                3    -> { promptCancelEvent(); true }
                4    -> { promptDeleteEvent(); true }
                else -> false
            }
        }
        popup.show()
    }

    // ── Dialogs ────────────────────────────────────────────────

    private fun showPaymentProofDialog(attendee: AttendeeItem) {
        val proofUrl = attendee.paymentProofUrl ?: return
        val fullUrl  = RetrofitClient.BASE_URL + proofUrl

        val displayName = "${attendee.firstName ?: ""} ${attendee.lastName ?: ""}".trim()
            .ifBlank { attendee.email ?: "Attendee" }

        val dialogView = layoutInflater.inflate(R.layout.dialog_payment_proof, null)
        val iv = dialogView.findViewById<ImageView>(R.id.ivProofImage)

        Glide.with(this).load(fullUrl)
            .placeholder(R.drawable.ic_avatar_placeholder)
            .error(R.drawable.ic_avatar_placeholder)
            .into(iv)

        AlertDialog.Builder(requireContext(), R.style.HangOutAlertDialogTheme)
            .setTitle("Payment Proof — $displayName")
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun promptApproveWithSeat(eventId: Long, rsvpId: Long) {
        val input = createStyledDialogEditText(requireContext(), "Seat number (e.g. A3)")
        AlertDialog.Builder(requireContext(), R.style.HangOutAlertDialogTheme)
            .setTitle("Approve and Assign Seat")
            .setMessage("Enter the seat number for this attendee:")
            .setView(input)
            .setPositiveButton("Approve") { _, _ ->
                val seat = input.text.toString().trim()
                presenter.approvePayment(eventId, rsvpId, seat.ifBlank { null })
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun promptAssignSeat(eventId: Long, rsvpId: Long) {
        // Find the attendee to get current seat info
        val attendee = allAttendees.find { it.id == rsvpId } ?: return
        val ctx = requireContext()

        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24.dpToPx(), 16.dpToPx(), 24.dpToPx(), 16.dpToPx())
        }

        // Current seat info
        val currentSeatLabel = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16.dpToPx() }
        }
        currentSeatLabel.addView(TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            text = "Current Seat"
            textSize = 12f
            setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
        })
        val currentSeatValue = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            text = if (!attendee.seatNumber.isNullOrBlank()) attendee.seatNumber else "Not assigned"
            textSize = 12f
            typeface = Typeface.defaultFromStyle(Typeface.BOLD)
            setTextColor(ContextCompat.getColor(ctx, if (!attendee.seatNumber.isNullOrBlank()) R.color.purple_light else R.color.text_muted))
        }
        currentSeatLabel.addView(currentSeatValue)
        container.addView(currentSeatLabel)

        // Input field for new seat
        val seatInput = android.widget.EditText(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                48.dpToPx()
            ).apply { bottomMargin = 16.dpToPx() }
            hint = "Enter new seat number (e.g. B7, A1)"
            setText(attendee.seatNumber ?: "")
            textSize = 13f
            setHintTextColor(0x66FFFFFF.toInt())
            setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0x22FFFFFF.toInt())
                setStroke(1.dpToPx(), 0x33FFFFFF.toInt())
                cornerRadius = 8.dpToPx().toFloat()
            }
            setPadding(12.dpToPx(), 0, 12.dpToPx(), 0)
            inputType = android.text.InputType.TYPE_CLASS_TEXT
            maxLines = 1
        }
        container.addView(seatInput)

        // Preview label
        val previewLabel = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 8.dpToPx() }
            text = "Preview"
            textSize = 12f
            typeface = Typeface.defaultFromStyle(Typeface.BOLD)
            setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
        }
        container.addView(previewLabel)

        // Preview box
        val previewBox = LinearLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 20.dpToPx() }
            orientation = LinearLayout.VERTICAL
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0x0DFFFFFF.toInt())
                setStroke(1.dpToPx(), 0x22A855F7.toInt())
                cornerRadius = 8.dpToPx().toFloat()
            }
            setPadding(12.dpToPx(), 12.dpToPx(), 12.dpToPx(), 12.dpToPx())
        }

        val attendeeName = TextView(ctx).apply {
            text = attendee.name?.takeIf { it.isNotBlank() }
                ?: "${attendee.firstName ?: ""} ${attendee.lastName ?: ""}".trim()
                    .ifBlank { attendee.email ?: "Unknown" }
            textSize = 12f
            typeface = Typeface.defaultFromStyle(Typeface.BOLD)
            setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
        }
        previewBox.addView(attendeeName)

        val previewContent = TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 8.dpToPx() }
            text = "Seat: — → —"
            textSize = 11f
            setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
        }
        previewBox.addView(previewContent)
        container.addView(previewBox)

        // Listen to seat input changes for live preview
        seatInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val newSeat = s?.toString()?.trim() ?: ""
                val currentDisplay = if (!attendee.seatNumber.isNullOrBlank()) attendee.seatNumber else "—"
                val newDisplay = if (newSeat.isNotBlank()) newSeat else "—"
                previewContent.text = "Seat: $currentDisplay → $newDisplay"
                previewContent.setTextColor(
                    ContextCompat.getColor(ctx, if (newSeat != attendee.seatNumber) R.color.purple_light else R.color.text_muted)
                )
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        AlertDialog.Builder(ctx, R.style.HangOutAlertDialogTheme)
            .setTitle("Edit Seat Assignment")
            .setView(container)
            .setPositiveButton("Apply") { _, _ ->
                val newSeat = seatInput.text.toString().trim()
                if (newSeat.isEmpty()) {
                    toast("Please enter a seat number or keep the current one.")
                } else if (newSeat == attendee.seatNumber) {
                    toast("No changes made.")
                } else {
                    presenter.assignSeat(eventId, rsvpId, newSeat)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Shows the refund approval dialog.
     *
     * - Displays the attendee's cancellation reason (if any) so the host can
     *   read why they cancelled before deciding.
     * - Allows uploading a refund proof image (required for refundable paid events).
     * - Includes an optional reference note field.
     */
    private fun promptRefundApproval(eventId: Long, rsvpId: Long, attendee: AttendeeItem) {
        val ctx      = requireContext()
        val name     = "${attendee.firstName ?: ""} ${attendee.lastName ?: ""}".trim().ifBlank { "this attendee" }
        val isRefundable = (event?.noRefundPolicy != true) && (event?.price ?: 0.0) > 0.0

        // Reset file-picker state for this dialog session
        pendingRefundProofUri   = null
        pendingRefundProofLabel = null

        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24.dpToPx(), 8.dpToPx(), 24.dpToPx(), 8.dpToPx())
        }

        // ── Cancellation reason block ──────────────────────────
        if (!attendee.cancellationReason.isNullOrBlank()) {
            container.addView(LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                background  = android.graphics.drawable.GradientDrawable().apply {
                    setColor(0x0DFFFFFF.toInt())
                    setStroke(1.dpToPx(), 0x22FFFFFF.toInt())
                    cornerRadius = 10.dpToPx().toFloat()
                }
                setPadding(14.dpToPx(), 12.dpToPx(), 14.dpToPx(), 12.dpToPx())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 16.dpToPx() }

                addView(TextView(ctx).apply {
                    text     = "Attendee's cancellation reason"
                    textSize = 11f
                    typeface = Typeface.defaultFromStyle(Typeface.BOLD)
                    setTextColor(resources.getColor(R.color.text_muted, null))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = 4.dpToPx() }
                })
                addView(TextView(ctx).apply {
                    text     = attendee.cancellationReason
                    textSize = 13f
                    setTextColor(resources.getColor(R.color.text_primary, null))
                    setLineSpacing(0f, 1.4f)
                })
            })
        }



        // ── Refund amount label ────────────────────────────────
        val price = (event?.price ?: 0.0).toInt()
        if (price > 0) {
            container.addView(LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity     = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 16.dpToPx() }
                background  = android.graphics.drawable.GradientDrawable().apply {
                    setColor(0x1A22C55E.toInt())
                    setStroke(1.dpToPx(), 0x3322C55E.toInt())
                    cornerRadius = 8.dpToPx().toFloat()
                }
                setPadding(12.dpToPx(), 10.dpToPx(), 12.dpToPx(), 10.dpToPx())

                addView(TextView(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    text     = "Refund Amount"
                    textSize = 12f
                    setTextColor(resources.getColor(R.color.text_muted, null))
                })
                addView(TextView(ctx).apply {
                    text     = "₱$price"
                    textSize = 15f
                    typeface = Typeface.defaultFromStyle(Typeface.BOLD)
                    setTextColor(resources.getColor(R.color.success_green, null))
                })
            })
        }

        // ── Proof upload section (required for refundable events) ──
        var proofLabelView: TextView? = null
        if (isRefundable) {
            container.addView(TextView(ctx).apply {
                text     = "Upload Refund Proof  *"
                textSize = 13f
                typeface = Typeface.defaultFromStyle(Typeface.BOLD)
                setTextColor(resources.getColor(R.color.text_primary, null))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 8.dpToPx() }
            })

            // Upload button + selected-file label
            val uploadRow = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 16.dpToPx() }
            }

            val pickBtn = Button(ctx).apply {
                text     = "Choose Image from Gallery"
                textSize = 13f
                typeface = Typeface.defaultFromStyle(Typeface.BOLD)
                setTextColor(resources.getColor(R.color.purple_light, null))
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(0x1AA855F7.toInt())
                    setStroke(1.dpToPx(), 0x55A855F7.toInt())
                    cornerRadius = 10.dpToPx().toFloat()
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    46.dpToPx()
                ).apply { bottomMargin = 8.dpToPx() }
            }

            val fileLabel = TextView(ctx).apply {
                text     = "No file selected"
                textSize = 12f
                setTextColor(resources.getColor(R.color.text_muted, null))
            }
            proofLabelView        = fileLabel
            pendingRefundProofLabel = fileLabel   // fragment-level ref for the callback

            pickBtn.setOnClickListener {
                pickRefundProof.launch("image/*")
            }

            uploadRow.addView(pickBtn)
            uploadRow.addView(fileLabel)
            container.addView(uploadRow)
        }

        // ── Reference note field ───────────────────────────────
        val noteInput = createStyledDialogEditText(
            ctx,
            "Reference note (optional, e.g. GCash ref 12345)"
        )
        container.addView(noteInput)

        val dialog = AlertDialog.Builder(ctx, R.style.HangOutAlertDialogTheme)
            .setTitle("Process Refund for $name")
            .setMessage(
                if (isRefundable)
                    "Upload proof that you've sent the refund. The attendee will then be asked to acknowledge receipt."
                else
                    "Mark the refund for $name as processed? The attendee will be asked to acknowledge receipt."
            )
            .setView(container)
            .setPositiveButton("Confirm Refund Sent") { _, _ ->
                // Validate proof if this is a refundable paid event
                if (isRefundable && pendingRefundProofUri == null) {
                    toast("Please select a refund proof image before confirming.")
                    return@setPositiveButton
                }
                presenter.approveRefund(
                    eventId  = eventId,
                    rsvpId   = rsvpId,
                    note     = noteInput.text.toString().trim(),
                    proofUri = pendingRefundProofUri
                )
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                ?.setTextColor(resources.getColor(R.color.success_green, null))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                ?.setTextColor(resources.getColor(R.color.text_muted, null))
        }

        dialog.show()
    }

    private fun promptRejectRefund(eventId: Long, rsvpId: Long, attendee: AttendeeItem) {
        val ctx          = requireContext()
        val name         = "${attendee.firstName ?: ""} ${attendee.lastName ?: ""}".trim()
            .ifBlank { "Guest" }
        val price        = (event?.price ?: 0.0).toInt()
        val isNoRefund   = event?.noRefundPolicy == true
        val defaultReason = if (isNoRefund) "This event has a no-refund policy." else ""

        val container = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24.dpToPx(), 8.dpToPx(), 24.dpToPx(), 8.dpToPx())
        }

        // ── Attendee's cancellation reason ──
        if (!attendee.cancellationReason.isNullOrBlank()) {
            container.addView(LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                background  = android.graphics.drawable.GradientDrawable().apply {
                    setColor(0x0DFFFFFF.toInt())
                    setStroke(1.dpToPx(), 0x22FFFFFF.toInt())
                    cornerRadius = 10.dpToPx().toFloat()
                }
                setPadding(14.dpToPx(), 12.dpToPx(), 14.dpToPx(), 12.dpToPx())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 12.dpToPx() }

                addView(TextView(ctx).apply {
                    text     = "Reason for Cancellation:"
                    textSize = 11f
                    typeface = Typeface.defaultFromStyle(Typeface.BOLD)
                    setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = 4.dpToPx() }
                })
                addView(TextView(ctx).apply {
                    text     = attendee.cancellationReason
                    textSize = 13f
                    setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
                    setLineSpacing(0f, 1.4f)
                })
            })
        }

        // ── Refund amount ──
        if (price > 0) {
            container.addView(LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                background  = android.graphics.drawable.GradientDrawable().apply {
                    setColor(0x1AEF4444.toInt())
                    setStroke(1.dpToPx(), 0x33EF4444.toInt())
                    cornerRadius = 8.dpToPx().toFloat()
                }
                setPadding(12.dpToPx(), 10.dpToPx(), 12.dpToPx(), 10.dpToPx())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 16.dpToPx() }

                addView(TextView(ctx).apply {
                    text     = "Refund Amount:"
                    textSize = 11f
                    typeface = Typeface.defaultFromStyle(Typeface.BOLD)
                    setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = 2.dpToPx() }
                })
                addView(TextView(ctx).apply {
                    text     = "₱$price"
                    textSize = 20f
                    typeface = Typeface.defaultFromStyle(Typeface.BOLD)
                    setTextColor(ContextCompat.getColor(ctx, R.color.red_accent))
                })
            })
        }

        // ── Rejection reason label ──
        container.addView(TextView(ctx).apply {
            text     = "Rejection Reason  *"
            textSize = 13f
            typeface = Typeface.defaultFromStyle(Typeface.BOLD)
            setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 8.dpToPx() }
        })

        // ── Reason input ──
        val reasonInput = android.widget.EditText(ctx).apply {
            hint      = "Please provide a reason for rejecting this refund request…"
            setText(defaultReason)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                    android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines  = 3
            maxLines  = 5
            setPadding(14.dpToPx(), 12.dpToPx(), 14.dpToPx(), 12.dpToPx())
            setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
            setHintTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(0x0DFFFFFF.toInt())
                setStroke(1.dpToPx(), 0x1FFFFFFF.toInt())
                cornerRadius = 10.dpToPx().toFloat()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        container.addView(reasonInput)

        val dialog = AlertDialog.Builder(ctx, R.style.HangOutAlertDialogTheme)
            .setTitle("Reject Refund")
            .setMessage("Guest: $name")
            .setView(container)
            .setPositiveButton("Reject Refund") { _, _ ->
                val reason = reasonInput.text.toString().trim()
                if (reason.isBlank()) {
                    toast("Please provide a rejection reason.")
                    return@setPositiveButton
                }
                presenter.rejectRefund(eventId, rsvpId, reason)
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                ?.setTextColor(ContextCompat.getColor(ctx, R.color.red_accent))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                ?.setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
        }

        dialog.show()
    }

    private fun promptWithReason(title: String, hint: String, onConfirm: (String) -> Unit) {
        val input = createStyledDialogEditText(
            requireContext(),
            hint,
            android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        )
        AlertDialog.Builder(requireContext(), R.style.HangOutAlertDialogTheme)
            .setTitle(title)
            .setView(input)
            .setPositiveButton("Confirm") { _, _ ->
                onConfirm(input.text.toString().trim().ifBlank { "No reason provided." })
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun promptCancelEvent() {
        val input = createStyledDialogEditText(
            requireContext(),
            "Reason for cancellation (shown to attendees)",
            android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        )
        AlertDialog.Builder(requireContext(), R.style.HangOutAlertDialogTheme)
            .setTitle("Cancel Event")
            .setMessage("All attendees will be notified. If the event is paid, they will be able to request refunds.")
            .setView(input)
            .setPositiveButton("Cancel Event") { _, _ ->
                val reason = input.text.toString().trim().ifBlank { "Event cancelled by host." }
                event?.id?.let { presenter.cancelEvent(it, reason) }
            }
            .setNegativeButton("Keep Event", null)
            .show()
    }

    private fun promptDeleteEvent() {
        createStyledAlertDialog(
            context = requireContext(),
            title = "Delete Event",
            message = "This will permanently delete the event and all RSVP data. This cannot be undone.",
            positiveButtonText = "Delete",
            positiveButtonListener = { _, _ ->
                event?.id?.let { presenter.deleteEvent(it) }
            },
            negativeButtonText = "Cancel",
            negativeButtonListener = null
        ).show()
    }

    // ── UI helpers ─────────────────────────────────────────────

    private fun buildActionButtonRow(
        positiveLabel: String,
        positiveIcon: Int,
        negativeLabel: String,
        negativeIcon: Int,
        onPositive: () -> Unit,
        onNegative: () -> Unit
    ): LinearLayout {
        val ctx = requireContext()
        return LinearLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.HORIZONTAL

            addView(Button(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(0, 46.dpToPx(), 1f).apply {
                    marginEnd = 8.dpToPx()
                }
                text     = positiveLabel
                textSize = 13f
                typeface = Typeface.defaultFromStyle(Typeface.BOLD)
                setTextColor(ContextCompat.getColor(ctx, R.color.white))
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(ContextCompat.getColor(ctx, R.color.success_green))
                    cornerRadius = 10.dpToPx().toFloat()
                }
                setCompoundDrawablesWithIntrinsicBounds(positiveIcon, 0, 0, 0)
                compoundDrawablePadding = 6.dpToPx()
                setOnClickListener { onPositive() }
            })

            addView(Button(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(0, 46.dpToPx(), 1f)
                text     = negativeLabel
                textSize = 13f
                typeface = Typeface.defaultFromStyle(Typeface.BOLD)
                setTextColor(ContextCompat.getColor(ctx, R.color.white))
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(ContextCompat.getColor(ctx, R.color.red_accent))
                    cornerRadius = 10.dpToPx().toFloat()
                }
                setCompoundDrawablesWithIntrinsicBounds(negativeIcon, 0, 0, 0)
                compoundDrawablePadding = 6.dpToPx()
                setOnClickListener { onNegative() }
            })
        }
    }

    private fun emptyStateView(iconRes: Int, title: String, subtitle: String): LinearLayout {
        val ctx = requireContext()
        return LinearLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            gravity     = Gravity.CENTER
            setPadding(20.dpToPx(), 60.dpToPx(), 20.dpToPx(), 60.dpToPx())

            addView(ImageView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(56.dpToPx(), 56.dpToPx()).apply {
                    bottomMargin = 12.dpToPx()
                    gravity      = Gravity.CENTER_HORIZONTAL
                }
                setImageResource(iconRes)
            })
            addView(TextView(ctx).apply {
                text     = title
                textSize = 16f
                typeface = Typeface.defaultFromStyle(Typeface.BOLD)
                setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
                gravity      = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 6.dpToPx() }
            })
            addView(TextView(ctx).apply {
                text     = subtitle
                textSize = 13f
                setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
                gravity = Gravity.CENTER
            })
        }
    }

    private fun calculateRevenue(attendees: List<AttendeeItem>, price: Int): Int {
        if (price <= 0) return 0
        val noRefundPolicy = event?.noRefundPolicy == true
        val confirmed = attendees.count {
            (it.paymentStatus == "confirmed" || it.status == "confirmed") && it.status != "cancelled"
        }
        val refunded = if (noRefundPolicy) 0
        else attendees.count { it.refundStatus == "completed" }
        return (confirmed - refunded) * price
    }

    private fun buildAlertBanner(
        iconRes: Int, text: String,
        bgColor: Int, borderColor: Int, textColor: Int
    ): LinearLayout {
        val ctx = requireContext()
        return LinearLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 16.dpToPx() }
            orientation = LinearLayout.HORIZONTAL
            gravity     = Gravity.CENTER_VERTICAL
            background  = android.graphics.drawable.GradientDrawable().apply {
                setColor(bgColor)
                setStroke(1.dpToPx(), borderColor)
                cornerRadius = 10.dpToPx().toFloat()
            }
            setPadding(14.dpToPx(), 12.dpToPx(), 14.dpToPx(), 12.dpToPx())

            addView(ImageView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(20.dpToPx(), 20.dpToPx()).apply {
                    marginEnd = 10.dpToPx()
                }
                setImageResource(iconRes)
                setColorFilter(textColor)
            })
            addView(TextView(ctx).apply {
                this.text = text
                textSize  = 13f
                typeface  = Typeface.defaultFromStyle(Typeface.BOLD)
                setTextColor(textColor)
            })
        }
    }

    private fun buildSectionLabel(text: String): TextView {
        val ctx = requireContext()
        return TextView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin    = 8.dpToPx()
                bottomMargin = 12.dpToPx()
            }
            this.text = text
            textSize  = 14f
            typeface  = Typeface.defaultFromStyle(Typeface.BOLD)
            setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun showProfileInformation(displayName: String, attendee: AttendeeItem) {
        val userId = attendee.userId ?: return

        lifecycleScope.launch {
            var dialog: ProfileInformationDialogFragment? = null
            try {
                val api = RetrofitClient.getApiService(requireContext())
                val response = api.getPublicUserProfile(userId)
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
                            email    = p["email"]  as? String,
                            photo    = (p["photoUrl"] as? String) ?: attendee.photo,
                            age      = age,
                            gender   = p["gender"] as? String,
                            phone    = p["phone"]  as? String,
                            location = location,
                            about    = p["bio"]    as? String,
                            isHost   = false
                        ).show(parentFragmentManager, "ProfileInformationDialog")
                    }
                }
            } catch (e: Exception) {
                if (isAdded) {
                    ProfileInformationDialogFragment.newInstance(
                        name   = displayName,
                        email  = event?.hostEmail,   // or attendee.email
                        photo  = event?.hostPhoto,   // or attendee.photo
                        isHost = true               // or false
                    ).show(parentFragmentManager, "ProfileInformationDialog")
                }
                android.util.Log.e("AttendeeProfile", "Failed to fetch profile", e)
            }
        }
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
    private operator fun <A, B, C, D> Quad<A, B, C, D>.component1() = first
    private operator fun <A, B, C, D> Quad<A, B, C, D>.component2() = second
    private operator fun <A, B, C, D> Quad<A, B, C, D>.component3() = third
    private operator fun <A, B, C, D> Quad<A, B, C, D>.component4() = fourth

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.detachView()
        _binding = null
    }
}