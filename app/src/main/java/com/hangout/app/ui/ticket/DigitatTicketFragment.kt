package com.hangout.app.ui.ticket

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.hangout.app.R
import com.hangout.app.data.EventItem
import com.hangout.app.databinding.FragmentDigitalTicketBinding
import com.hangout.app.utils.EventHolder
import com.hangout.app.utils.hide
import com.hangout.app.utils.show
import com.hangout.app.utils.toast

class DigitalTicketFragment : Fragment() {

    private var _binding: FragmentDigitalTicketBinding? = null
    private val binding get() = _binding!!

    private var event: EventItem? = null
    var onBackCallback: (() -> Unit)? = null

    companion object {
        fun newInstance(onBack: (() -> Unit)? = null) =
            DigitalTicketFragment().apply { onBackCallback = onBack }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDigitalTicketBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        event = EventHolder.currentEvent ?: run {
            toast("No ticket data found.")
            parentFragmentManager.popBackStack()
            return
        }

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
            onBackCallback?.invoke()
        }

        event?.let { bindTicket(it) }
    }

    // ── Bind all ticket data ──────────────────────────────────────────────

    private fun bindTicket(e: EventItem) {

        // Cover image
        if (!e.imageUrl.isNullOrBlank()) {
            Glide.with(this)
                .load(e.imageUrl)
                .centerCrop()
                .placeholder(R.drawable.cover_photo_bg)
                .into(binding.ivCoverImage)
            binding.ivCoverImage.show()
        } else {
            binding.ivCoverImage.hide()
        }

        // Event info
        binding.tvEventTitle.text    = e.title ?: "Untitled"
        binding.tvEventDate.text     = formatDate(e)
        binding.tvEventTime.text     = formatTime(e)
        binding.tvEventLocation.text = e.location?.ifBlank { "Virtual / TBD" } ?: "Virtual / TBD"
        binding.tvEventFormat.text   = e.format ?: "In-Person"

        // Ticket details
        val ticketNumber = e.ticketNumber ?: "TKT-${e.id}"
        binding.tvTicketNumber.text  = ticketNumber
        binding.tvSeatNumber.text    = e.seatNumber?.ifBlank { "Open Seating" } ?: "Open Seating"
        binding.tvAttendeeName.text  = "${e.hostFirstName ?: ""} ${e.hostLastName ?: ""}".trim()
            .ifBlank { "Attendee" }

        // Check-in status
        val isAttended = e.attendeeStatus == "attended"
        if (isAttended) {
            binding.tvCheckinStatus.text = "✓ Checked In"
            binding.tvCheckinStatus.setTextColor(
                androidx.core.content.ContextCompat.getColor(requireContext(), R.color.success_green)
            )
            binding.viewCheckinBg.setBackgroundColor(
                androidx.core.content.ContextCompat.getColor(requireContext(), R.color.success_green)
            )
        } else {
            binding.tvCheckinStatus.text = "Not Yet Checked In"
            binding.tvCheckinStatus.setTextColor(
                androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_muted)
            )
        }

        // QR Code — generate from ticket token or ticket number
        val qrContent = e.ticketToken ?: ticketNumber
        generateQrCode(qrContent)?.let { bitmap ->
            binding.ivQrCode.setImageBitmap(bitmap)
        } ?: run {
            binding.tvQrFallback.show()
            binding.ivQrCode.hide()
        }

        // QR label under the code
        binding.tvQrLabel.text = "Scan this code at the event entrance"
    }

    // ── QR code generation ────────────────────────────────────────────────

    private fun generateQrCode(content: String): Bitmap? {
        return try {
            val hints = mapOf(
                EncodeHintType.MARGIN          to 1,
                EncodeHintType.ERROR_CORRECTION to com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H
            )
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512, hints)
            val width  = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    // Dark modules = purple, light modules = dark background
                    bitmap.setPixel(
                        x, y,
                        if (bitMatrix[x, y]) 0xFFC084FC.toInt()   // purple_light
                        else                  0xFF13131F.toInt()   // bg_dark
                    )
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun formatDate(e: EventItem): String = e.date ?: "Date TBD"

    private fun formatTime(e: EventItem): String {
        val start = e.startTime ?: e.time ?: return "Time TBD"
        val end   = e.endTime
        return if (!end.isNullOrBlank()) {
            "${to12Hr(start)} – ${to12Hr(end)}"
        } else {
            to12Hr(start)
        }
    }

    private fun to12Hr(time24: String): String {
        return try {
            val parts = time24.split(":")
            val h = parts[0].toInt()
            val m = parts[1].toInt()
            val ampm    = if (h >= 12) "PM" else "AM"
            val display = when {
                h == 0  -> 12
                h > 12  -> h - 12
                else    -> h
            }
            String.format("%d:%02d %s", display, m, ampm)
        } catch (_: Exception) { time24 }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}