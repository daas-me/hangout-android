package com.hangout.app.ui.messages

import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.hangout.app.R
import com.hangout.app.data.ConversationItem
import com.hangout.app.databinding.FragmentMessagesBinding
import com.hangout.app.databinding.ItemConversationRowBinding
import com.hangout.app.ui.conversation.ConversationFragment
import com.hangout.app.utils.ChatHolder
import com.hangout.app.utils.hide
import com.hangout.app.utils.show
import com.hangout.app.utils.showIf
import com.hangout.app.utils.toast

class MessagesFragment : Fragment(), MessagesContract.View {

    private var _binding: FragmentMessagesBinding? = null
    private val binding get() = _binding!!
    private lateinit var presenter: MessagesContract.Presenter
    private var allConversations: List<ConversationItem> = emptyList()

    var onBackCallback: (() -> Unit)? = null

    companion object {
        fun newInstance(onBack: (() -> Unit)? = null) =
            MessagesFragment().apply { onBackCallback = onBack }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMessagesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter = MessagesPresenter(this, MessagesModel(requireContext()))

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
            onBackCallback?.invoke()
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterConversations(s?.toString().orEmpty())
            }
        })

        presenter.load()
    }

    private fun filterConversations(query: String) {
        val filtered = if (query.isBlank()) {
            allConversations
        } else {
            allConversations.filter {
                it.otherUser.displayName().contains(query, ignoreCase = true) ||
                        it.lastMessage.content.contains(query, ignoreCase = true)
            }
        }
        renderConversations(filtered)
    }

    override fun showLoading(show: Boolean) {
        binding.progressBar.showIf(show)
    }

    override fun showError(message: String) = toast(message)

    override fun showEmpty() {
        allConversations = emptyList()
        binding.layoutContainer.removeAllViews()
        binding.layoutContainer.addView(emptyText("No conversations yet.\nMessage a host or start chatting!"))
    }

    override fun showConversations(items: List<ConversationItem>) {
        allConversations = items
        renderConversations(items)
    }

    private fun renderConversations(items: List<ConversationItem>) {
        binding.layoutContainer.removeAllViews()
        if (items.isEmpty()) {
            binding.layoutContainer.addView(emptyText("No conversations found."))
            return
        }
        items.forEachIndexed { index, conv ->
            val row = ItemConversationRowBinding.inflate(layoutInflater, binding.layoutContainer, false)
            bindRow(row, conv)
            binding.layoutContainer.addView(row.root)

            if (index < items.size - 1) {
                val divider = View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1
                    ).apply { setMargins(80, 0, 0, 0) }
                    setBackgroundColor(resources.getColor(R.color.divider, null))
                }
                binding.layoutContainer.addView(divider)
            }
        }
    }

    private fun bindRow(row: ItemConversationRowBinding, conv: ConversationItem) {
        val user = conv.otherUser
        row.tvName.text = user.displayName()
        row.tvLastMessage.text = conv.lastMessage.content
        row.tvTime.text = formatTime(conv.lastMessage.sentAt)

        if (conv.unreadCount > 0) {
            row.tvUnreadBadge.text = if (conv.unreadCount > 9) "9+" else conv.unreadCount.toString()
            row.tvUnreadBadge.show()
            row.tvLastMessage.setTypeface(null, Typeface.BOLD)
            row.tvLastMessage.setTextColor(resources.getColor(R.color.text_primary, null))
        } else {
            row.tvUnreadBadge.hide()
            row.tvLastMessage.setTypeface(null, Typeface.NORMAL)
        }

        if (!user.photo.isNullOrBlank()) {
            try {
                val bytes = Base64.decode(user.photo.substringAfter("base64,"), Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                row.ivAvatar.setImageBitmap(bmp)
                row.ivAvatar.show()
                row.tvInitials.hide()
            } catch (_: Exception) {
                row.ivAvatar.hide()
                row.tvInitials.text = user.initials()
                row.tvInitials.show()
            }
        } else {
            row.ivAvatar.hide()
            row.tvInitials.text = user.initials()
            row.tvInitials.show()
        }

        row.root.setOnClickListener {
            ChatHolder.currentChatUser = user
            val fragment = ConversationFragment.newInstance(onBack = { presenter.load() })
            parentFragmentManager.beginTransaction()
                .add(R.id.nav_host_fragment, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun formatTime(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
            val date = sdf.parse(raw.substringBefore(".")) ?: return ""
            val now = System.currentTimeMillis()
            val diff = now - date.time
            when {
                diff < 60_000 -> "now"
                diff < 3_600_000 -> "${diff / 60_000}m"
                diff < 86_400_000 -> "${diff / 3_600_000}h"
                else -> java.text.SimpleDateFormat("MMM d", java.util.Locale.US).format(date)
            }
        } catch (_: Exception) { "" }
    }

    private fun emptyText(msg: String) = TextView(requireContext()).apply {
        text = msg
        setTextColor(resources.getColor(R.color.text_muted, null))
        textSize = 14f
        gravity = android.view.Gravity.CENTER
        setPadding(0, 120, 0, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.detachView()
        _binding = null
    }
}