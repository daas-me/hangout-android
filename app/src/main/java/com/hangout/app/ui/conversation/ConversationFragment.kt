package com.hangout.app.ui.conversation

import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.*
import android.view.animation.AnimationUtils
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.hangout.app.R
import com.hangout.app.data.ConversationDetail
import com.hangout.app.data.MessageItem
import com.hangout.app.data.OtherUser
import com.hangout.app.databinding.FragmentConversationBinding
import com.hangout.app.utils.ChatHolder
import com.hangout.app.utils.hide
import com.hangout.app.utils.show
import com.hangout.app.utils.showIf
import com.hangout.app.utils.toast

class ConversationFragment : Fragment(), ConversationContract.View {

    private var _binding: FragmentConversationBinding? = null
    private val binding get() = _binding!!
    private lateinit var presenter: ConversationContract.Presenter

    private var otherUser: OtherUser? = null
    private var currentUserId: Long = -1L
    private val pollHandler = Handler(Looper.getMainLooper())
    private var lastMessageCount = 0

    // Track whether we have ever successfully rendered messages.
    // This prevents the early-return guard from blocking the very first render
    // when a network error previously left lastMessageCount at 0.
    private var hasRenderedOnce = false

    var onBackCallback: (() -> Unit)? = null

    companion object {
        fun newInstance(
            user: OtherUser? = null,
            onBack: (() -> Unit)? = null
        ) = ConversationFragment().apply {
            if (user != null) ChatHolder.currentChatUser = user
            onBackCallback = onBack
        }
    }

    private val pollRunnable = object : Runnable {
        override fun run() {
            otherUser?.id?.let { presenter.load(it) }
            pollHandler.postDelayed(this, 4000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        retainInstance = true
        if (!::presenter.isInitialized) {
            presenter = ConversationPresenter(this, ConversationModel(requireContext()))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentConversationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        otherUser = ChatHolder.currentChatUser ?: run {
            toast("No chat target.")
            parentFragmentManager.popBackStack()
            return
        }

        currentUserId = -1L
        bindHeader(otherUser!!)

        binding.btnBack.setOnClickListener {
            pollHandler.removeCallbacks(pollRunnable)
            parentFragmentManager.popBackStack()
            onBackCallback?.invoke()
        }

        binding.btnSend.setOnClickListener { handleSend() }

        binding.etMessage.setOnEditorActionListener { _, _, _ ->
            handleSend()
            true
        }

        // Manual refresh — spins the icon and forces a fresh load.
        binding.btnRefresh.setOnClickListener {
            val spin = AnimationUtils.loadAnimation(requireContext(), R.anim.rotate_refresh)
            binding.btnRefresh.startAnimation(spin)
            // Reset the guard so the next load always re-renders even if message
            // count hasn't changed (e.g. after a previous failed request).
            hasRenderedOnce = false
            lastMessageCount = 0
            otherUser?.id?.let { uid -> presenter.load(uid) }
        }

        // Immediate load on open, then poll every 4 s.
        otherUser?.id?.let { presenter.load(it) }
        pollHandler.postDelayed(pollRunnable, 4000L)
    }

    private fun handleSend() {
        val content = binding.etMessage.text.toString().trim()
        if (content.isBlank()) return
        binding.etMessage.setText("")
        otherUser?.id?.let { presenter.send(it, content) }
    }

    private fun bindHeader(user: OtherUser) {
        binding.tvChatName.text = user.displayName()
        binding.tvChatStatus.text = user.email ?: ""

        if (!user.photo.isNullOrBlank()) {
            try {
                val bytes = Base64.decode(user.photo.substringAfter("base64,"), Base64.DEFAULT)
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                binding.ivHeaderAvatar.setImageBitmap(bmp)
                binding.ivHeaderAvatar.show()
                binding.tvHeaderInitials.hide()
            } catch (_: Exception) {
                binding.ivHeaderAvatar.hide()
                binding.tvHeaderInitials.text = user.initials()
                binding.tvHeaderInitials.show()
            }
        } else {
            binding.ivHeaderAvatar.hide()
            binding.tvHeaderInitials.text = user.initials()
            binding.tvHeaderInitials.show()
        }
    }

    override fun showLoading(show: Boolean) {
        // Only show the progress bar before any messages have been rendered,
        // so it doesn't flicker on every poll tick.
        if (!hasRenderedOnce) binding.progressBar.showIf(show)
    }

    override fun showSendLoading(show: Boolean) {
        binding.btnSend.isEnabled = !show
    }

    override fun showError(message: String) = toast(message)

    override fun showConversation(detail: ConversationDetail) {
        if (currentUserId == -1L && detail.messages.isNotEmpty()) {
            val msg = detail.messages.first()
            currentUserId = if (msg.senderId == otherUser?.id) msg.recipientId else msg.senderId
        }

        val newCount = detail.messages.size

        // FIX: only skip re-render after we have successfully rendered at least once.
        // Without this guard the very first load is skipped when the server returns
        // an empty list and a subsequent error leaves lastMessageCount at 0.
        if (hasRenderedOnce && newCount == lastMessageCount) return

        hasRenderedOnce = true
        lastMessageCount = newCount

        binding.layoutMessages.removeAllViews()
        detail.messages.forEach { msg ->
            binding.layoutMessages.addView(buildBubble(msg))
        }
        scrollToBottom()
    }

    override fun appendMessage(message: MessageItem) {
        lastMessageCount++
        hasRenderedOnce = true
        binding.layoutMessages.addView(buildBubble(message))
        scrollToBottom()
        val imm = requireContext().getSystemService(InputMethodManager::class.java)
        imm.hideSoftInputFromWindow(binding.etMessage.windowToken, 0)
    }

    private fun buildBubble(msg: MessageItem): View {
        val isMine = msg.senderId != otherUser?.id
        val ctx = requireContext()

        val container = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            gravity = if (isMine) android.view.Gravity.END else android.view.Gravity.START
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 4, 0, 4) }
        }

        val bubbleWrapper = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            gravity = if (isMine) android.view.Gravity.END else android.view.Gravity.START
        }

        val bubble = android.widget.TextView(ctx).apply {
            text = msg.content
            textSize = 14f
            setTextColor(ContextCompat.getColor(ctx, R.color.white))
            setPadding(dpToPx(14), dpToPx(10), dpToPx(14), dpToPx(10))
            maxWidth = dpToPx(272)
            background = if (isMine) {
                createRoundedBackground(0xFF7C3AED.toInt())
            } else {
                createRoundedBackground(0xFF1E2040.toInt())
            }
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                val h = dpToPx(56)
                if (isMine) setMargins(h, 0, dpToPx(16), 0)
                else setMargins(dpToPx(16), 0, h, 0)
            }
        }

        val timeView = android.widget.TextView(ctx).apply {
            text = formatTime(msg.sentAt)
            textSize = 10f
            setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
            alpha = 0.7f
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                if (isMine) setMargins(0, 3, dpToPx(16), 8)
                else setMargins(dpToPx(16), 3, 0, 8)
            }
        }

        bubbleWrapper.addView(bubble)
        bubbleWrapper.addView(timeView)
        container.addView(bubbleWrapper)
        return container
    }

    private fun createRoundedBackground(color: Int): android.graphics.drawable.GradientDrawable {
        return android.graphics.drawable.GradientDrawable().apply {
            setColor(color)
            cornerRadius = dpToPx(18).toFloat()
        }
    }

    private fun scrollToBottom() {
        binding.scrollMessages.post {
            binding.scrollMessages.fullScroll(android.view.View.FOCUS_DOWN)
        }
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    private fun formatTime(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
            val date = sdf.parse(raw.substringBefore(".")) ?: return ""
            java.text.SimpleDateFormat("h:mm a", java.util.Locale.US).format(date)
        } catch (_: Exception) { "" }
    }

    override fun onPause() {
        super.onPause()
        pollHandler.removeCallbacks(pollRunnable)
    }

    override fun onResume() {
        super.onResume()
        // Force a fresh render on re-entry (e.g. coming back from another screen).
        hasRenderedOnce = false
        otherUser?.id?.let { presenter.load(it) }
        pollHandler.postDelayed(pollRunnable, 4000L)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pollHandler.removeCallbacks(pollRunnable)
        presenter.detachView()
        _binding = null
    }
}