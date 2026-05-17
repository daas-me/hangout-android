package com.hangout.app.ui.ticket

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
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
    private var qrContent: String = ""
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

        binding.btnBack.setOnClickListener { navigateBack() }
        binding.btnClose.setOnClickListener { navigateBack() }
        binding.btnDownloadQr.setOnClickListener { downloadQrCode() }

        event?.let { bindTicket(it) }
    }

    // ── Navigation ────────────────────────────────────────────────────────

    private fun navigateBack() {
        onBackCallback?.let { callback ->
            parentFragmentManager.popBackStack()
            binding.root.post { callback() }
        } ?: run {
            parentFragmentManager.popBackStack()
        }
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

        // Header
        binding.tvEventTitle.text = e.title ?: "Untitled"
        val fmt = e.format
        if (fmt.isNullOrBlank()) {
            binding.tvEventFormat.hide()
        } else {
            binding.tvEventFormat.text = fmt
            binding.tvEventFormat.show()
        }

        // Guest
        binding.tvAttendeeName.text =
            "${e.hostFirstName ?: ""} ${e.hostLastName ?: ""}".trim().ifBlank { "Attendee" }

        // Event info
        binding.tvEventDate.text     = formatDate(e)
        binding.tvEventTime.text     = formatTime(e)
        binding.tvEventLocation.text = e.location?.ifBlank { "Virtual / TBD" } ?: "Virtual / TBD"

        // Ticket chips
        val ticketNumber = e.ticketNumber ?: "TKT-${e.id}"
        binding.tvTicketNumber.text = ticketNumber
        binding.tvSeatNumber.text   = e.seatNumber?.ifBlank { "Open" } ?: "Open"

        // QR
        val frontendBase = "https://hangout-web.onrender.com"
        qrContent = "$frontendBase/verify/${e.id}/${e.ticketToken ?: ticketNumber}"

        generateQrCode(qrContent)?.let { bitmap ->
            binding.ivQrCode.setImageBitmap(bitmap)
            binding.ivQrCode.show()
            binding.tvQrFallback.hide()
        } ?: run {
            binding.tvQrFallback.show()
            binding.ivQrCode.hide()
        }

        binding.tvQrLabel.text     = "Scan at the entrance to verify attendance"
        binding.tvQrTicketRef.text = ticketNumber

        // Check-in status
        val isAttended = e.attendeeStatus == "attended"
        if (isAttended) {
            binding.tvCheckinStatus.text = "Checked In"
            val green = androidx.core.content.ContextCompat
                .getColor(requireContext(), R.color.success_green)
            binding.tvCheckinStatus.setTextColor(green)
            binding.viewCheckinBg.setBackgroundColor(green)
        } else {
            binding.tvCheckinStatus.text = "Not Yet Checked In"
            val muted = androidx.core.content.ContextCompat
                .getColor(requireContext(), R.color.text_muted)
            binding.tvCheckinStatus.setTextColor(muted)
            binding.viewCheckinBg.setBackgroundColor(muted)
        }
    }

    // ── Download QR to gallery ────────────────────────────────────────────

    private fun downloadQrCode() {
        if (qrContent.isBlank()) { toast("QR not available"); return }
        val bitmap = generateQrCode(qrContent) ?: run { toast("Failed to generate QR"); return }
        try {
            val name = "QR_${event?.ticketNumber ?: "ticket"}.png"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/HangOut")
            }
            val uri = requireContext().contentResolver
                .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                requireContext().contentResolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                toast("QR code saved to gallery")
            } else {
                toast("Failed to save QR code")
            }
        } catch (ex: Exception) {
            toast("Error: ${ex.message}")
        }
    }

    // ── QR code generation (black on white to match web) ──────────────────

    private fun generateQrCode(content: String): Bitmap? {
        return try {
            val hints = mapOf(
                EncodeHintType.MARGIN          to 2,
                EncodeHintType.ERROR_CORRECTION to com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.H
            )
            val writer    = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512, hints)
            val w = bitMatrix.width
            val h = bitMatrix.height
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
            for (x in 0 until w) {
                for (y in 0 until h) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
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
            "${com.hangout.app.utils.formatTime12Hr(start)} – ${com.hangout.app.utils.formatTime12Hr(end)}"
        } else {
            com.hangout.app.utils.formatTime12Hr(start)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}