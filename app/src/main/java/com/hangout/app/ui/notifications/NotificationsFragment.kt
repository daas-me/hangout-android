package com.hangout.app.ui.notifications

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.hangout.app.R
import com.hangout.app.data.NotificationItem
import com.hangout.app.databinding.FragmentNotificationsBinding
import com.hangout.app.databinding.ItemNotificationBinding
import com.hangout.app.utils.showIf
import com.hangout.app.utils.toast
import com.hangout.app.ui.components.createStyledAlertDialog

class NotificationsFragment : Fragment(), NotificationsContract.View {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private lateinit var presenter: NotificationsContract.Presenter

    private val items = mutableListOf<NotificationItem>()

    var onBackCallback: (() -> Unit)? = null

    companion object {
        fun newInstance(onBack: (() -> Unit)? = null) =
            NotificationsFragment().apply { onBackCallback = onBack }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter = NotificationsPresenter(this, NotificationsModel(requireContext()))

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
            onBackCallback?.invoke()
        }

        binding.btnMarkAllRead.setOnClickListener {
            presenter.markAllRead()
        }

        presenter.load()
    }

    // ── Contract ──────────────────────────────────────────────────────────

    override fun showLoading(show: Boolean) {
        binding.progressBar.showIf(show)
    }

    override fun showError(message: String) = toast(message)

    override fun showEmpty() {
        items.clear()
        binding.layoutContainer.removeAllViews()
        binding.layoutContainer.addView(buildEmptyState())
        binding.btnMarkAllRead.isEnabled = false
        updateUnreadBadge()
    }

    override fun showNotifications(newItems: List<NotificationItem>) {
        items.clear()
        items.addAll(newItems)
        binding.btnMarkAllRead.isEnabled = items.any { !it.isRead }
        updateUnreadBadge()
        renderAll()
    }

    override fun onNotificationDeleted(id: Long) {
        items.removeAll { it.id == id }
        updateUnreadBadge()
        if (items.isEmpty()) showEmpty() else renderAll()
    }

    override fun onAllDeleted() = showEmpty()

    // ── Unread badge ──────────────────────────────────────────────────────

    private fun updateUnreadBadge() {
        val count = items.count { !it.isRead }
        binding.tvUnreadCount.text = when {
            count == 0 -> "All caught up"
            count == 1 -> "1 unread"
            else       -> "$count unread"
        }
    }

    // ── Render ────────────────────────────────────────────────────────────

    private fun renderAll() {
        binding.layoutContainer.removeAllViews()
        items.forEach { item ->
            val row = ItemNotificationBinding.inflate(
                layoutInflater, binding.layoutContainer, false
            )
            bindRow(row, item)
            binding.layoutContainer.addView(row.root)
        }
    }

    private fun bindRow(row: ItemNotificationBinding, item: NotificationItem) {
        row.tvTitle.text = item.title ?: "Notification"
        row.tvBody.text  = item.body  ?: ""
        row.tvTime.text  = formatTime(item.createdAt)

        // Event tag extracted from body text
        val eventName = extractEventName(item.body)
        if (!eventName.isNullOrBlank()) {
            row.tvEventTag.text = eventName
            row.tvEventTag.visibility = View.VISIBLE
        } else {
            row.tvEventTag.visibility = View.GONE
        }

        // Unread dot
        row.viewUnreadDot.showIf(!item.isRead)

        // Dim read items
        val alpha = if (item.isRead) 0.5f else 1f
        row.tvTitle.alpha = alpha
        row.tvBody.alpha  = alpha

        // Icon + tint by type
        val (iconRes, colorRes) = iconAndColor(item.type)
        row.ivIcon.setImageResource(iconRes)
        row.ivIcon.setColorFilter(
            ContextCompat.getColor(requireContext(), colorRes)
        )

        // Tap → mark read + show detail
        row.root.setOnClickListener {
            // Mark as read and wait for completion
            if (!item.isRead) {
                presenter.markRead(item.id)
                // Update UI immediately for better UX
                row.viewUnreadDot.showIf(false)
                row.tvTitle.alpha = 0.5f
                row.tvBody.alpha  = 0.5f
                val idx = items.indexOfFirst { it.id == item.id }
                if (idx >= 0) items[idx] = item.copy(isRead = true)
                updateUnreadBadge()
                binding.btnMarkAllRead.isEnabled = items.any { !it.isRead }
            }
            showDetailDialog(item)
        }

        // Long-press → delete
        row.root.setOnLongClickListener {
            createStyledAlertDialog(
                context = requireContext(),
                title = "Delete Notification",
                message = "Remove this notification?",
                positiveButtonText = "Delete",
                positiveButtonListener = { _, _ ->
                    presenter.deleteNotification(item.id)
                },
                negativeButtonText = "Cancel",
                cancelable = true
            ).show()
            true
        }
    }

    // ── Icon + color mapping ──────────────────────────────────────────────

    private fun iconAndColor(type: String?): Pair<Int, Int> = when (type) {
        "NEW_RSVP"                         -> R.drawable.ic_users       to R.color.success_green
        "RSVP_CANCELLED", "RSVP_REJECTED"  -> R.drawable.ic_users       to R.color.red_accent
        "PAYMENT_PROOF"                    -> R.drawable.ic_credit_card  to R.color.yellow_accent
        "PAYMENT_APPROVED"                 -> R.drawable.ic_credit_card  to R.color.success_green
        "PAYMENT_REJECTED"                 -> R.drawable.ic_credit_card  to R.color.red_accent
        "REFUND_REQUEST",
        "REFUND_PROCESSED"                 -> R.drawable.ic_ticket       to R.color.yellow_accent
        "REFUND_COMPLETED",
        "REFUND_ACKNOWLEDGED"              -> R.drawable.ic_ticket       to R.color.success_green
        "EVENT_CANCELLED",
        "EVENT_DELETED"                    -> R.drawable.ic_bell         to R.color.red_accent
        "EVENT_REMINDER"                   -> R.drawable.ic_bell         to R.color.purple_light
        "SEAT_ASSIGNED"                    -> R.drawable.ic_seat         to R.color.purple_light
        else                               -> R.drawable.ic_bell         to R.color.purple_light
    }

    // ── Detail dialog ─────────────────────────────────────────────────────

    private fun showDetailDialog(item: NotificationItem) {
        createStyledAlertDialog(
            context = requireContext(),
            title = item.title ?: "Notification",
            message = buildString {
                append(item.body ?: "")
                if (!item.createdAt.isNullOrBlank()) {
                    append("\n\n")
                    append(formatTime(item.createdAt))
                }
            },
            positiveButtonText = "Close",
            negativeButtonText = "Delete",
            negativeButtonListener = { _, _ ->
                presenter.deleteNotification(item.id)
            },
            cancelable = true
        ).show()
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun extractEventName(body: String?): String? {
        if (body.isNullOrBlank()) return null
        // Match text inside single or double quotes
        val match = Regex("""["']([^"']{2,50})["']""").find(body)
        return match?.groupValues?.getOrNull(1)
    }

    private fun formatTime(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return try {
            val sdf  = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
            val date = sdf.parse(raw.substringBefore(".")) ?: return raw
            val diff = System.currentTimeMillis() - date.time
            when {
                diff < 60_000      -> "Just now"
                diff < 3_600_000   -> "${diff / 60_000}m ago"
                diff < 86_400_000  -> "${diff / 3_600_000}h ago"
                diff < 604_800_000 -> "${diff / 86_400_000}d ago"
                else -> java.text.SimpleDateFormat(
                    "MMM d, yyyy", java.util.Locale.US
                ).format(date)
            }
        } catch (_: Exception) { raw }
    }

    private fun buildEmptyState() = TextView(requireContext()).apply {
        text = "No notifications yet"
        setTextColor(resources.getColor(R.color.text_muted, null))
        textSize = 14f
        gravity  = android.view.Gravity.CENTER
        setPadding(0, 120, 0, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.detachView()
        _binding = null
    }
}