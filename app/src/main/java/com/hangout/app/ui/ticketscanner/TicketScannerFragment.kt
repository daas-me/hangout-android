package com.hangout.app.ui.ticketscanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.zxing.BarcodeFormat
import com.google.zxing.ResultPoint
import com.hangout.app.R
import com.hangout.app.data.EventItem
import com.hangout.app.data.TicketVerifyResult
import com.hangout.app.databinding.FragmentTicketScannerBinding
import com.hangout.app.utils.EventHolder
import com.hangout.app.utils.hide
import com.hangout.app.utils.show
import com.hangout.app.utils.showIf
import com.hangout.app.utils.toast
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DefaultDecoderFactory

class TicketScannerFragment : Fragment(), TicketScannerContract.View {

    private var _binding: FragmentTicketScannerBinding? = null
    private val binding get() = _binding!!

    private lateinit var presenter: TicketScannerContract.Presenter
    private var event: EventItem? = null
    private var isProcessing = false   // debounce — prevent double-scan

    var onBackCallback: (() -> Unit)? = null

    companion object {
        private const val CAMERA_PERMISSION = Manifest.permission.CAMERA
        private const val CAMERA_REQUEST    = 1001

        fun newInstance(onBack: (() -> Unit)? = null) =
            TicketScannerFragment().apply { onBackCallback = onBack }
    }

    // ── ZXing continuous scanner callback ──────────────────────────────────

    private val scanCallback = object : BarcodeCallback {
        override fun barcodeResult(result: BarcodeResult?) {
            val raw = result?.text?.trim() ?: return
            if (isProcessing || raw.isBlank()) return

            isProcessing = true
            binding.barcodeScanner.pause()
            showScanningOverlay(raw)

            // ── Parse URL or fall back to raw token ──────────────────────────
            val (parsedEventId, parsedToken) = parseQrContent(raw)

            if (parsedEventId == null) {
                // QR is a plain token — need a known eventId from context
                val eventId = event?.id ?: run {
                    toast("No event selected.")
                    resetScanner()
                    return
                }
                presenter.verifyTicket(eventId, parsedToken)
            } else {
                presenter.verifyTicket(parsedEventId, parsedToken)
            }
        }

        override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {}
    }

    private fun parseQrContent(raw: String): Pair<Long?, String> {
        val pattern = Regex("""/verify/(\d+)/(.+)$""")
        val match   = pattern.find(raw)
        return if (match != null) {
            val eventId = match.groupValues[1].toLongOrNull()
            val token   = match.groupValues[2]
            eventId to token
        } else {
            null to raw   // plain token — caller provides eventId from context
        }
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        if (!::presenter.isInitialized) {
            presenter = TicketScannerPresenter(this, TicketScannerModel(requireContext()))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTicketScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        event = EventHolder.currentEvent

        binding.tvEventName.text = event?.title ?: "Scan Ticket"

        binding.btnBack.setOnClickListener {
            onBackCallback?.let { callback ->
                parentFragmentManager.popBackStack()
                binding.root.post { callback() }
            } ?: run {
                parentFragmentManager.popBackStack()
            }
        }

        binding.btnRetry.setOnClickListener {
            resetScanner()
        }

        // Configure ZXing scanner
        binding.barcodeScanner.decoderFactory = DefaultDecoderFactory(
            listOf(BarcodeFormat.QR_CODE)
        )
        binding.barcodeScanner.cameraSettings.isAutoFocusEnabled = true

        checkCameraAndStart()
    }

    override fun onResume() {
        super.onResume()
        if (hasCameraPermission()) binding.barcodeScanner.resume()
    }

    override fun onPause() {
        super.onPause()
        binding.barcodeScanner.pause()
    }

    // ── Camera permission ─────────────────────────────────────────────────

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(requireContext(), CAMERA_PERMISSION) ==
                PackageManager.PERMISSION_GRANTED

    private fun checkCameraAndStart() {
        if (hasCameraPermission()) {
            startScanning()
        } else {
            requestPermissions(arrayOf(CAMERA_PERMISSION), CAMERA_REQUEST)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        if (requestCode == CAMERA_REQUEST &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startScanning()
        } else {
            toast("Camera permission is required to scan tickets.")
        }
    }

    private fun startScanning() {
        binding.barcodeScanner.decodeContinuous(scanCallback)
        binding.barcodeScanner.resume()
        showIdleState()
    }

    // ── TicketScannerContract.View ────────────────────────────────────────

    override fun showLoading(show: Boolean) {
        binding.progressBar.showIf(show)
    }

    override fun showMessage(message: String) = toast(message)

    override fun onVerifySuccess(result: TicketVerifyResult) {
        // Navigate to the result fragment
        val fragment = TicketVerifyResultFragment.newInstance(
            result    = result,
            eventId   = event?.id ?: 0L,
            onBack    = { resetScanner() }
        )
        parentFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onAttendanceMarked(result: TicketVerifyResult) {
        // Same as verify success - show result screen
        onVerifySuccess(result)
    }

    override fun onVerifyFailed(message: String) {
        showInvalidState(message)
        // Auto-reset after 3 seconds so host can try again
        Handler(Looper.getMainLooper()).postDelayed({
            if (_binding != null) resetScanner()
        }, 3000)
    }

    override fun resetScanner() {
        isProcessing = false
        showIdleState()
        binding.barcodeScanner.resume()
    }

    // ── UI states ─────────────────────────────────────────────────────────

    private fun showIdleState() {
        binding.layoutResult.hide()
        binding.btnRetry.hide()
        binding.tvScanHint.show()
        binding.tvScanHint.text = "Point camera at attendee's QR code"
    }

    private fun showScanningOverlay(raw: String) {
        binding.tvScanHint.text = "Verifying…"
    }

    private fun showInvalidState(message: String) {
        binding.layoutResult.show()
        binding.layoutResult.setBackgroundColor(0xCCEF4444.toInt())
        binding.tvResultIcon.text  = "✗"
        binding.tvResultTitle.text = "Invalid Ticket"
        binding.tvResultTitle.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.red_accent)
        )
        binding.tvResultMessage.text = message
        binding.tvResultMessage.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.text_primary)
        )
        binding.btnRetry.show()
        binding.tvScanHint.hide()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.barcodeScanner.pause()
        presenter.detachView()
        _binding = null
    }
}