package com.hangout.app.ui.ticketscanner

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.hangout.app.R
import com.hangout.app.data.TicketVerifyResult
import com.hangout.app.databinding.FragmentTicketVerifyResultBinding
import com.hangout.app.utils.hide
import com.hangout.app.utils.show

class TicketVerifyResultFragment : Fragment() {

    private var _binding: FragmentTicketVerifyResultBinding? = null
    private val binding get() = _binding!!

    private var result: TicketVerifyResult? = null
    private var eventId: Long = 0L
    var onBackCallback: (() -> Unit)? = null

    companion object {
        fun newInstance(
            result:  TicketVerifyResult,
            eventId: Long,
            onBack:  (() -> Unit)? = null
        ) = TicketVerifyResultFragment().apply {
            this.result        = result
            this.eventId       = eventId
            this.onBackCallback = onBack
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTicketVerifyResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        result?.let { bindResult(it) }

        binding.btnScanNext.setOnClickListener {
            onBackCallback?.let { callback ->
                parentFragmentManager.popBackStack()
                binding.root.post { callback() }
            } ?: run {
                parentFragmentManager.popBackStack()
            }
        }

        binding.btnBack.setOnClickListener {
            onBackCallback?.let { callback ->
                parentFragmentManager.popBackStack()
                binding.root.post { callback() }
            } ?: run {
                parentFragmentManager.popBackStack()
            }
        }
    }

    private fun bindResult(r: TicketVerifyResult) {
        val ctx = requireContext()

        // ── Status banner ──────────────────────────────────────────────────
        if (r.valid) {
            val isAttended = r.checkInStatus == "attended" || r.attendeeStatus == "attended"
            if (isAttended) {
                // Just checked in - show success
                binding.layoutStatusBanner.setBackgroundColor(0xFF22C55E.toInt())
                binding.tvStatusIcon.text  = "✓"
                binding.tvStatusTitle.text = "Check-In Successful!"
                binding.tvStatusTitle.setTextColor(ContextCompat.getColor(ctx, R.color.success_green))
            } else {
                // Valid but not yet checked in - shouldn't reach here due to auto-marking, but handle it
                binding.layoutStatusBanner.setBackgroundColor(0xFF22C55E.toInt())
                binding.tvStatusIcon.text  = "✓"
                binding.tvStatusTitle.text = "Valid Ticket"
                binding.tvStatusTitle.setTextColor(ContextCompat.getColor(ctx, R.color.success_green))
            }
        } else {
            binding.layoutStatusBanner.setBackgroundColor(0xFFEF4444.toInt())
            binding.tvStatusIcon.text  = "✗"
            binding.tvStatusTitle.text = "Invalid Ticket"
            binding.tvStatusTitle.setTextColor(ContextCompat.getColor(ctx, R.color.red_accent))
        }

        binding.tvStatusMessage.text = r.message ?: if (r.valid) "Attendee verified" else "Ticket not recognised"

        // ── Attendee photo ─────────────────────────────────────────────────
        if (!r.attendeePhoto.isNullOrBlank()) {
            try {
                val bytes = Base64.decode(
                    r.attendeePhoto.substringAfter("base64,"), Base64.DEFAULT
                )
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                binding.ivAvatar.setImageBitmap(bmp)
                binding.tvInitials.hide()
            } catch (_: Exception) {
                showInitials(r)
            }
        } else {
            showInitials(r)
        }

        // ── Attendee info ──────────────────────────────────────────────────
        binding.tvAttendeeName.text  = r.attendeeName?.ifBlank { "Unknown Attendee" } ?: "Unknown Attendee"
        binding.tvAttendeeEmail.text = r.attendeeEmail ?: ""

        // ── Ticket details ─────────────────────────────────────────────────
        binding.tvEventTitle.text   = r.eventTitle ?: "—"
        binding.tvTicketNumber.text = r.ticketNumber ?: "—"
        binding.tvSeatNumber.text   = r.seatNumber?.ifBlank { "Open Seating" } ?: "Open Seating"

        val checkIn = r.checkInStatus ?: "pending"
        binding.tvCheckinStatus.text = when (checkIn) {
            "attended" -> "✓ Checked In"
            "pending"  -> "Not yet checked in"
            else       -> checkIn.replaceFirstChar { it.uppercase() }
        }
        binding.tvCheckinStatus.setTextColor(
            ContextCompat.getColor(
                ctx,
                if (checkIn == "attended") R.color.success_green else R.color.text_muted
            )
        )

        // ── Event date ─────────────────────────────────────────────────────
        if (!r.eventDate.isNullOrBlank()) {
            binding.tvEventDate.text = r.eventDate
            binding.tvEventDate.show()
        } else {
            binding.tvEventDate.hide()
        }
    }

    private fun showInitials(r: TicketVerifyResult) {
        binding.tvInitials.show()
        val name = r.attendeeName ?: ""
        val parts = name.trim().split(" ")
        binding.tvInitials.text = when {
            parts.size >= 2 ->
                "${parts[0].firstOrNull()?.uppercase() ?: ""}${parts[1].firstOrNull()?.uppercase() ?: ""}"
            parts.size == 1 && parts[0].isNotBlank() ->
                parts[0].take(2).uppercase()
            else -> "??"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}