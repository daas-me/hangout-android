package com.hangout.app.ui.paymentproof

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.hangout.app.R
import com.hangout.app.data.EventItem
import com.hangout.app.databinding.BottomSheetPaymentProofBinding
import com.hangout.app.utils.copyUriToCache
import com.hangout.app.utils.hide
import com.hangout.app.utils.show
import com.hangout.app.utils.toast
import java.io.File

class PaymentProofBottomSheet : BottomSheetDialogFragment(), PaymentProofContract.View {

    private var _binding: BottomSheetPaymentProofBinding? = null
    private val binding get() = _binding!!

    private lateinit var presenter: PaymentProofContract.Presenter
    private var selectedImageUri: Uri? = null
    private var event: EventItem? = null

    /** Called when proof is successfully submitted — parent can refresh state */
    var onSubmitSuccess: (() -> Unit)? = null

    companion object {
        fun newInstance(
            event: EventItem,
            onSuccess: (() -> Unit)? = null
        ) = PaymentProofBottomSheet().apply {
            this.event         = event
            this.onSubmitSuccess = onSuccess
        }
    }

    // ── Image pickers ─────────────────────────────────────────────────────

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { handleImageSelected(it) }
        }
    }

    private var cameraOutputUri: Uri? = null

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraOutputUri?.let { handleImageSelected(it) }
        }
    }

    private fun openCamera() {
        val file = File(requireContext().cacheDir, "proof_${System.currentTimeMillis()}.jpg")
        val uri = androidx.core.content.FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",  // must match your AndroidManifest FileProvider authority
            file
        )
        cameraOutputUri = uri
        cameraLauncher.launch(uri)
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetPaymentProofBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet))?.let {
            BottomSheetBehavior.from(it).state = BottomSheetBehavior.STATE_EXPANDED
        }

        presenter = PaymentProofPresenter(this, PaymentProofModel(requireContext()))

        event?.let { bindEvent(it) }

        binding.btnPickGallery.setOnClickListener { openGallery() }
        binding.btnPickCamera.setOnClickListener  { openCamera()  }
        binding.btnRemoveImage.setOnClickListener { clearImage()  }
        binding.btnSubmit.setOnClickListener      { handleSubmit() }
        binding.btnCancel.setOnClickListener      { dismiss()      }

        // Wire once here, not inside updateSubmitState()
        binding.cbAcknowledge.setOnCheckedChangeListener { _, _ -> updateSubmitState() }

        binding.btnCopyAccount.setOnClickListener {
            val number = event?.accountNumber ?: return@setOnClickListener
            val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("account", number))
        }
    }


    // ── Bind event data ───────────────────────────────────────────────────

    private fun bindEvent(e: EventItem) {
        val price = e.price?.toInt() ?: 0

        binding.tvEventTitle.text = e.title ?: "Untitled"
        binding.tvEventPrice.text = "₱${"%,d".format(price)}"

        val methodLabel = when (e.paymentMethod?.lowercase()) {
            "gcash"   -> "GCash"
            "paymaya",
            "maya"    -> "Maya"
            "bank"    -> "Bank Transfer"
            else      -> e.paymentMethod?.replaceFirstChar { it.uppercase() } ?: "—"
        }
        binding.tvPaymentMethod.text  = methodLabel
        binding.tvAccountName.text    = e.accountName   ?: "—"
        binding.tvAccountNumber.text  = e.accountNumber ?: "—"

        // No-refund policy section
        if (e.noRefundPolicy == true) {
            binding.layoutNoRefund.show()
        } else {
            binding.layoutNoRefund.hide()
        }

        updateSubmitState()
    }

    // ── Image handling ────────────────────────────────────────────────────

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        galleryLauncher.launch(intent)
    }

    private fun handleImageSelected(uri: Uri) {
        // Check file size from URI metadata
        val fileSizeBytes = try {
            val cursor = requireContext().contentResolver.query(
                uri, null, null, null, null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val sizeIdx = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (sizeIdx >= 0) it.getLong(sizeIdx) else -1L
                } else -1L
            } ?: -1L
        } catch (e: Exception) {
            -1L
        }

        val minBytes = 10_240L      // 10 KB
        val maxBytes = 10_485_760L  // 10 MB

        if (fileSizeBytes in 1 until minBytes) {
            val sizeKb = String.format("%.1f", fileSizeBytes / 1_024.0)
            toast("Image too small (${sizeKb} KB) — please use a real screenshot")
            return
        }
        if (fileSizeBytes > maxBytes) {
            val sizeMb = String.format("%.1f", fileSizeBytes / 1_048_576.0)
            toast("Image too large (${sizeMb} MB) — max 10 MB")
            return
        }

        // Copy to cache  ← val file is declared HERE, not before
        val file = try {
            val path = copyUriToCache(requireContext(), uri) ?: run {
                toast("Could not read image file")
                return
            }
            File(path)
        } catch (e: Exception) {
            toast("Error reading file: ${e.localizedMessage}")
            return
        }

        // Sanity check on the cached file  ← moved to AFTER val file exists
        if (file.length() < minBytes) {
            toast("Could not read the image — please try again")
            return
        }

        selectedImageUri = uri
        Glide.with(this).load(uri).centerCrop().into(binding.ivPreview)
        binding.layoutPreview.show()
        binding.layoutPickButtons.hide()
        updateSubmitState()
    }

    private fun clearImage() {
        selectedImageUri = null
        binding.layoutPreview.hide()
        binding.layoutPickButtons.show()
        updateSubmitState()
    }

    private fun handleSubmit() {
        val uri = selectedImageUri
        if (uri == null) {
            toast("Select an image first")
            return
        }

        if (event?.noRefundPolicy == true && !binding.cbAcknowledge.isChecked) {
            toast("Acknowledge policy first")
            return
        }

        val eventId = event?.id
        if (eventId == null || eventId <= 0) {
            toast("Event error - please try again")
            return
        }

        val path = copyUriToCache(requireContext(), uri)
        if (path.isNullOrBlank()) {
            toast("Failed to read image")
            return
        }

        val file = File(path)
        if (!file.exists()) {
            toast("Image file not found")
            return
        }

        presenter.submitPaymentProof(eventId, file)
    }

    private fun updateSubmitState() {
        val hasImage = selectedImageUri != null
        val policyOk = event?.noRefundPolicy != true || binding.cbAcknowledge.isChecked
        val isEnabled = hasImage && policyOk
        binding.btnSubmit.isEnabled = isEnabled
    }

    // ── PaymentProofContract.View ─────────────────────────────────────────

    override fun showLoading(show: Boolean) {
        binding.progressBar.isVisible = show
        binding.btnCancel.isEnabled   = !show
        if (show) {
            binding.btnSubmit.isEnabled = false
        } else {
            updateSubmitState()
        }
    }

    override fun showMessage(message: String) = toast(message)

    override fun onSubmitSuccess() {
        onSubmitSuccess?.invoke()
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.detachView()
        _binding = null
    }
}