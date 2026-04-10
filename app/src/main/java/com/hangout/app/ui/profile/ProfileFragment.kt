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
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.hangout.app.R
import com.hangout.app.databinding.DialogChangePasswordBinding
import com.hangout.app.databinding.DialogEditProfileBinding
import com.hangout.app.databinding.FragmentProfileBinding
import com.hangout.app.models.UserProfile
import com.hangout.app.models.UserStats
import com.hangout.app.utils.SessionManager
import java.io.File
import java.io.FileOutputStream

class ProfileFragment : Fragment(), ProfileContract.View {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var presenter: ProfileContract.Presenter
    private lateinit var session: SessionManager

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
            presenter = ProfilePresenter(this, requireContext())
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        session  = SessionManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter.loadAll()

        binding.btnEditProfile.setOnClickListener  { showEditProfileDialog() }
        binding.ivCameraOverlay.setOnClickListener { showEditProfileDialog() }

        setupSettingsRow(binding.rowPersonalInfo.root,
            "Personal Information", "Update your details", R.drawable.ic_user
        ) { showEditProfileDialog() }

        setupSettingsRow(binding.rowChangePassword.root,
            "Privacy & Security", "Change password & privacy", R.drawable.ic_lock
        ) { showChangePasswordDialog() }

        setupSettingsRow(binding.rowSignOut.root,
            "Sign Out", "Log out of your account", R.drawable.ic_logout
        ) {
            session.clearSession()
            requireActivity().finish()
        }
    }

    // ── ProfileContract.View ───────────────────────────────────────────────

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
        binding.ivAvatar.visibility   = View.VISIBLE
        binding.tvInitials.visibility = View.GONE
    }

    override fun clearPhoto() {
        currentPhotoData = null
        binding.ivAvatar.visibility   = View.GONE
        binding.tvInitials.visibility = View.VISIBLE
    }

    override fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun showMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onProfileUpdateSuccess() {
        editProfileDialog?.dismiss()
    }

    override fun onPasswordUpdateSuccess() {
        changePasswordDialog?.dismiss()
    }

    // ── Dialogs ────────────────────────────────────────────────────────────

    private fun showEditProfileDialog() {
        val db = DialogEditProfileBinding.inflate(layoutInflater)
        editProfileBinding = db

        currentPhotoData?.let { photoData ->
            val bytes = Base64.decode(photoData.substringAfter("base64,"), Base64.DEFAULT)
            db.ivPhotoPreview.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
            db.tvPhotoInitials.visibility = View.GONE
            db.btnRemovePhoto.visibility  = View.VISIBLE
        } ?: run {
            db.btnRemovePhoto.visibility = View.GONE
        }

        db.btnChangePhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            @Suppress("DEPRECATION")
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        db.btnRemovePhoto.setOnClickListener {
            presenter.deletePhoto()
            db.ivPhotoPreview.setImageDrawable(null)
            db.tvPhotoInitials.visibility = View.VISIBLE
            db.btnRemovePhoto.visibility  = View.GONE
        }

        db.btnSave.setOnClickListener {
            val firstname = db.etFirstName.text?.toString()?.trim() ?: ""
            val lastname  = db.etLastName.text?.toString()?.trim() ?: ""
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
            bottomSheet?.elevation = 0f
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            window?.decorView?.setBackgroundResource(android.R.color.transparent)
        }
    }

    private fun showChangePasswordDialog() {
        val db = DialogChangePasswordBinding.inflate(layoutInflater)

        db.btnUpdate.setOnClickListener {
            val old     = db.etCurrentPassword.text?.toString() ?: ""
            val newPw   = db.etNewPassword.text?.toString() ?: ""
            val confirm = db.etConfirmPassword.text?.toString() ?: ""
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
            bottomSheet?.elevation = 0f
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
                db.tvPhotoInitials.visibility = View.GONE
                db.btnRemovePhoto.visibility  = View.VISIBLE
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