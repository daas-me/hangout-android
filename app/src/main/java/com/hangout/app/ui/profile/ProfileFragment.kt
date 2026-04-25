package com.hangout.app.ui.profile

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.hangout.app.R
import com.hangout.app.databinding.DialogChangePasswordBinding
import com.hangout.app.databinding.DialogEditProfileBinding
import com.hangout.app.databinding.FragmentProfileBinding
import com.hangout.app.models.*
import com.hangout.app.ui.auth.AuthActivity
import com.hangout.app.utils.getValue
import com.hangout.app.utils.hide
import com.hangout.app.utils.show
import com.hangout.app.utils.showIf
import com.hangout.app.utils.toast
import java.io.File
import java.io.FileOutputStream

class ProfileFragment : Fragment(), ProfileContract.View {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var presenter: ProfileContract.Presenter

    private var editProfileDialog: BottomSheetDialog? = null
    private var changePasswordDialog: BottomSheetDialog? = null
    private var editProfileBinding: DialogEditProfileBinding? = null
    private var currentPhotoData: String? = null

    companion object {
        private const val PICK_IMAGE_REQUEST = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        if (!::presenter.isInitialized) {
            presenter = ProfilePresenter(this, ProfileModel(requireContext()))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter.loadAll()

        binding.btnEditProfile.setOnClickListener  { showEditProfileDialog() }
        binding.ivCameraOverlay.setOnClickListener { showEditProfileDialog() }

        setupSettingsRow(
            binding.rowPersonalInfo.root,
            "Personal Information", "Update your details", R.drawable.ic_user
        ) { showEditProfileDialog() }

        setupSettingsRow(
            binding.rowChangePassword.root,
            "Privacy & Security", "Change password & privacy", R.drawable.ic_lock
        ) { showChangePasswordDialog() }

        setupSettingsRow(
            binding.rowSignOut.root,
            "Sign Out", "Log out of your account", R.drawable.ic_logout
        ) { signOut() }
    }

    // ── ProfileContract.View ───────────────────────────────────────────────

    override fun showLoading(show: Boolean) {
        binding.progressBar.showIf(show)
    }

    override fun showMessage(message: String) {
        toast(message)
    }

    override fun showProfile(profile: UserProfile) {
        val fullName = "${profile.firstname} ${profile.lastname}".trim()
        binding.tvFullName.text = fullName.ifBlank { "Your Name" }
        binding.tvEmail.text    = profile.email
        val initials = (profile.firstname.firstOrNull()?.uppercase() ?: "") +
                (profile.lastname.firstOrNull()?.uppercase() ?: "")
        binding.tvInitials.text = initials.ifBlank { "YO" }
    }

    override fun showStats(stats: UserStats) {
        binding.tvHostingCount.text   = stats.hostingCount.toString()
        binding.tvAttendingCount.text = stats.attendingCount.toString()
        binding.tvAttendeesCount.text = stats.totalAttendees.toString()
    }

    override fun showPhoto(photoData: String) {
        currentPhotoData = photoData
        val bytes = Base64.decode(photoData.substringAfter("base64,"), Base64.DEFAULT)
        val bmp   = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        binding.ivAvatar.setImageBitmap(bmp)
        binding.ivAvatar.show()
        binding.tvInitials.hide()
    }

    override fun clearPhoto() {
        currentPhotoData = null
        binding.ivAvatar.hide()
        binding.tvInitials.show()
    }

    override fun onProfileUpdateSuccess() {
        editProfileDialog?.dismiss()
    }

    override fun onPasswordUpdateSuccess() {
        changePasswordDialog?.dismiss()
    }

    // ── Sign Out ───────────────────────────────────────────────────────────

    private fun signOut() {
        presenter.clearSession()
        val intent = Intent(requireContext(), AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    // ── Dialogs ────────────────────────────────────────────────────────────

    private fun showEditProfileDialog() {
        val db = DialogEditProfileBinding.inflate(layoutInflater)
        editProfileBinding = db

        currentPhotoData?.let { photoData ->
            val bytes = Base64.decode(photoData.substringAfter("base64,"), Base64.DEFAULT)
            db.ivPhotoPreview.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            db.tvPhotoInitials.hide()
            db.btnRemovePhoto.show()
        } ?: run {
            db.btnRemovePhoto.hide()
        }

        db.btnChangePhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            @Suppress("DEPRECATION")
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        db.btnRemovePhoto.setOnClickListener {
            presenter.deletePhoto()
            db.ivPhotoPreview.setImageDrawable(null)
            db.tvPhotoInitials.show()
            db.btnRemovePhoto.hide()
        }

        db.btnSave.setOnClickListener {
            val firstname = db.etFirstName.getValue()
            val lastname  = db.etLastName.getValue()
            if (firstname.isBlank()) {
                db.tilFirstName.error = "First name is required"
                return@setOnClickListener
            }
            db.tilFirstName.error = null
            presenter.updateProfile(firstname, lastname)
        }

        db.btnClose.setOnClickListener  { editProfileDialog?.dismiss() }
        db.btnCancel.setOnClickListener { editProfileDialog?.dismiss() }

        editProfileDialog = BottomSheetDialog(requireContext(), R.style.AppBottomSheetDialogTheme).apply {
            setContentView(db.root)
            show()
            val bottomSheet = findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.background = null
            bottomSheet?.elevation  = 0f
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            window?.decorView?.setBackgroundResource(android.R.color.transparent)
        }
    }

    private fun showChangePasswordDialog() {
        val db = DialogChangePasswordBinding.inflate(layoutInflater)

        db.btnUpdate.setOnClickListener {
            val old     = db.etCurrentPassword.getValue()
            val newPw   = db.etNewPassword.getValue()
            val confirm = db.etConfirmPassword.getValue()
            var valid   = true

            if (old.isBlank())    { db.tilCurrentPassword.error = "Required"; valid = false }
            else db.tilCurrentPassword.error = null
            if (newPw.length < 6) { db.tilNewPassword.error = "Minimum 6 characters"; valid = false }
            else db.tilNewPassword.error = null
            if (newPw != confirm) { db.tilConfirmPassword.error = "Passwords do not match"; valid = false }
            else db.tilConfirmPassword.error = null

            if (valid) presenter.updatePassword(old, newPw)
        }

        db.btnClose.setOnClickListener  { changePasswordDialog?.dismiss() }
        db.btnCancel.setOnClickListener { changePasswordDialog?.dismiss() }

        changePasswordDialog = BottomSheetDialog(requireContext(), R.style.AppBottomSheetDialogTheme).apply {
            setContentView(db.root)
            show()
            val bottomSheet = findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.background = null
            bottomSheet?.elevation  = 0f
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            window?.decorView?.setBackgroundResource(android.R.color.transparent)
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun setupSettingsRow(
        rowView: View, title: String, subtitle: String,
        iconRes: Int, danger: Boolean = false, onClick: () -> Unit
    ) {
        rowView.findViewById<android.widget.TextView>(R.id.tvRowTitle).apply {
            text = title
            if (danger) setTextColor(
                androidx.core.content.ContextCompat.getColor(rowView.context, R.color.red_accent)
            )
        }
        rowView.findViewById<android.widget.TextView>(R.id.tvRowSubtitle).text = subtitle
        rowView.findViewById<android.widget.ImageView>(R.id.ivRowIcon).setImageResource(iconRes)
        rowView.setOnClickListener { onClick() }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK) {
            val uri  = data?.data ?: return
            val file = uriToFile(uri) ?: return
            presenter.uploadPhoto(file)
            editProfileBinding?.let { db ->
                db.ivPhotoPreview.setImageURI(uri)
                db.tvPhotoInitials.hide()
                db.btnRemovePhoto.show()
            }
        }
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val input = requireContext().contentResolver.openInputStream(uri) ?: return null
            val file  = File(requireContext().cacheDir, "upload_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { input.copyTo(it) }
            file
        } catch (e: Exception) { null }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.detachView()
        _binding           = null
        editProfileBinding = null
    }
}