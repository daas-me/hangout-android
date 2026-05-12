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

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            // Save bitmap to cache and use that URI
            val file = File(requireContext().cacheDir, "proof_${System.currentTimeMillis()}.jpg")
            file.outputStream().use {
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, it)
            }
            handleImageSelected(Uri.fromFile(file))
        }
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

        // Expand sheet fully by default
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

        binding.btnCopyAccount.setOnClickListener {
            val number = event?.accountNumber ?: return@setOnClickListener
            val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("account", number))
            toast("Account number copied!")
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

    private fun openCamera() {
        cameraLauncher.launch(null)
    }

    private fun handleImageSelected(uri: Uri) {
        selectedImageUri = uri
        Glide.with(this)
            .load(uri)
            .centerCrop()
            .into(binding.ivPreview)
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
        val uri = selectedImageUri ?: run {
            toast("Please select a proof of payment first.")
            return
        }
        if (event?.noRefundPolicy == true && !binding.cbAcknowledge.isChecked) {
            toast("Please acknowledge the refund policy first.")
            return
        }

        val path = copyUriToCache(requireContext(), uri) ?: run {
            toast("Could not read image. Please try again.")
            return
        }

        val eventId = event?.id ?: return
        presenter.submitPaymentProof(eventId, File(path))
    }

    private fun updateSubmitState() {
        val hasImage     = selectedImageUri != null
        val policyOk     = event?.noRefundPolicy != true || binding.cbAcknowledge.isChecked
        binding.btnSubmit.isEnabled = hasImage && policyOk

        binding.cbAcknowledge.setOnCheckedChangeListener { _, _ -> updateSubmitState() }
    }

    // ── PaymentProofContract.View ─────────────────────────────────────────

    override fun showLoading(show: Boolean) {
        binding.progressBar.isVisible = show
        binding.btnSubmit.isEnabled   = !show
        binding.btnCancel.isEnabled   = !show
    }

    override fun showMessage(message: String) = toast(message)

    override fun onSubmitSuccess() {
        toast("Payment proof submitted! Waiting for host approval.")
        onSubmitSuccess?.invoke()
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.detachView()
        _binding = null
    }
}