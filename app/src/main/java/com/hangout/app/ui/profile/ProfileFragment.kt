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
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.switchmaterial.SwitchMaterial
import com.hangout.app.R
import com.hangout.app.databinding.DialogChangePasswordBinding
import com.hangout.app.databinding.DialogEditProfileBinding
import com.hangout.app.databinding.FragmentProfileBinding
import com.hangout.app.data.*
import com.hangout.app.network.RetrofitClient
import com.hangout.app.ui.auth.AuthActivity
import com.hangout.app.ui.components.createStyledAlertDialog
import com.hangout.app.ui.components.createStyledDialogEditText
import com.hangout.app.ui.components.styleDialogEditText
import android.content.res.ColorStateList
import com.hangout.app.utils.AppCache
import com.hangout.app.utils.SessionManager
import com.hangout.app.utils.getValue
import com.hangout.app.utils.hide
import com.hangout.app.utils.show
import com.hangout.app.utils.showIf
import com.hangout.app.utils.toast
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
        presenter.loadAll(forceRefresh = false)

        // ── Setup pull-to-refresh ──────────────────────────────────
        binding.swipeRefresh.setColorSchemeResources(R.color.purple_main)
        binding.swipeRefresh.setOnRefreshListener {
            presenter.loadAll(forceRefresh = true)
        }

        // ── Only allow refresh when scrolled to top ────────────────
        binding.scrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            binding.swipeRefresh.isEnabled = (scrollY == 0)
        }

        binding.btnEditProfile.setOnClickListener  { showEditProfileDialog() }
        binding.btnEditPersonalInfo.setOnClickListener { showEditProfileDialog() }

        // Personal Information row
        setupSettingsRow(
            binding.rowPersonalInfo.root,
            "Personal Information", "Update your name and details", R.drawable.ic_user
        ) { showEditProfileDialog() }

        // Privacy & Security row
        setupSettingsRow(
            binding.rowChangePassword.root,
            "Privacy & Security", "Change password & privacy", R.drawable.ic_lock
        ) { showChangePasswordDialog() }

        // Notification Preferences row
        setupSettingsRow(
            binding.rowNotifications.root,
            "Notification Preferences", "Manage what you're notified about", R.drawable.ic_bell
        ) { showNotificationPreferencesDialog() }

        // Hosting Eligibility row
        setupSettingsRow(
            binding.rowHostingEligibility.root,
            "Hosting Eligibility", "Profile completion status", R.drawable.ic_user
        ) { showHostingEligibilityDialog() }

        // Delete Account row (danger)
        setupSettingsRow(
            binding.rowDeleteAccount.root,
            "Delete Account", "Permanently remove your account", R.drawable.ic_logout,
            danger = true
        ) { promptDeleteAccount() }

        // Sign Out row
        setupSettingsRow(
            binding.rowSignOut.root,
            "Sign Out", "Log out of your account", R.drawable.ic_logout
        ) { signOut() }
    }

    // ── ProfileContract.View ───────────────────────────────────────────────

    override fun showLoading(show: Boolean) {
        binding.progressBar.showIf(show)
        binding.swipeRefresh.isRefreshing = show
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

        // Update hosting eligibility status
        val completionPercent = calculateCompletionPercent(profile)
        val statusText = if (completionPercent == 100)
            "$completionPercent% Complete - Unlocked"
        else
            "$completionPercent% Complete - Incomplete"
        binding.tvHostingEligibilityStatus.text = statusText
        binding.progressHostingEligibility.max = 100
        binding.progressHostingEligibility.progress = completionPercent
        val progressColor = if (completionPercent == 100) R.color.green_accent else R.color.yellow_accent
        binding.progressHostingEligibility.progressTintList = ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), progressColor)
        )

        // Populate grid information fields
        binding.tvFullNameInfo.text = fullName.ifBlank { "Not provided" }
        binding.tvEmailInfo.text = profile.email.ifBlank { "Not provided" }

        // Phone
        try {
            val phoneField = profile.javaClass.getDeclaredField("phone")
            phoneField.isAccessible = true
            val phoneVal = (phoneField.get(profile) as? String)?.takeIf { it.isNotBlank() }
            binding.tvPhoneInfo.text = phoneVal ?: "-"
        } catch (_: Exception) {
            binding.tvPhoneInfo.text = "-"
        }

        // Gender
        try {
            val genderField = profile.javaClass.getDeclaredField("gender")
            genderField.isAccessible = true
            val genderVal = (genderField.get(profile) as? String)?.takeIf { it.isNotBlank() }
            binding.tvGenderInfo.text = genderVal ?: "-"
        } catch (_: Exception) {
            binding.tvGenderInfo.text = "-"
        }

        // Birthdate
        try {
            val birthdateField = profile.javaClass.getDeclaredField("birthdate")
            birthdateField.isAccessible = true
            val birthdateVal = birthdateField.get(profile) as? String
            binding.tvBirthdateInfo.text = birthdateVal?.takeIf { it.isNotBlank() } ?: "-"
        } catch (_: Exception) {
            binding.tvBirthdateInfo.text = "-"
        }

        // Street
        try {
            val streetField = profile.javaClass.getDeclaredField("street")
            streetField.isAccessible = true
            val streetVal = (streetField.get(profile) as? String)?.takeIf { it.isNotBlank() }
            binding.tvStreetInfo.text = streetVal ?: "-"
        } catch (_: Exception) {
            binding.tvStreetInfo.text = "-"
        }

        // Municipality
        try {
            binding.tvMunicipalityInfo.text = profile.city?.takeIf { it.isNotBlank() } ?: "-"
        } catch (_: Exception) {
            binding.tvMunicipalityInfo.text = "-"
        }

        // State
        try {
            val stateField = profile.javaClass.getDeclaredField("state")
            stateField.isAccessible = true
            val stateVal = (stateField.get(profile) as? String)?.takeIf { it.isNotBlank() }
            binding.tvStateInfo.text = stateVal ?: "-"
        } catch (_: Exception) {
            binding.tvStateInfo.text = "-"
        }

        // Country
        try {
            val countryField = profile.javaClass.getDeclaredField("country")
            countryField.isAccessible = true
            val countryVal = (countryField.get(profile) as? String)?.takeIf { it.isNotBlank() }
            binding.tvCountryInfo.text = countryVal ?: "-"
        } catch (_: Exception) {
            binding.tvCountryInfo.text = "-"
        }

        // Zip code
        try {
            val zipcodeField = profile.javaClass.getDeclaredField("zipcode")
            zipcodeField.isAccessible = true
            val zipcodeVal = (zipcodeField.get(profile) as? String)?.takeIf { it.isNotBlank() }
            binding.tvZipcodeInfo.text = zipcodeVal ?: "-"
        } catch (_: Exception) {
            binding.tvZipcodeInfo.text = "-"
        }

        // Bio
        try {
            val bioField = profile.javaClass.getDeclaredField("bio")
            bioField.isAccessible = true
            val bioVal = (bioField.get(profile) as? String)?.takeIf { it.isNotBlank() }
            binding.tvBioInfo.text = bioVal ?: "-"
        } catch (_: Exception) {
            binding.tvBioInfo.text = "-"
        }
    }

    override fun showStats(stats: UserStats) {
        // hostingCount = published (non-draft) hosted events
        // attendingCount = confirmed attending events
        // totalAttendees = total attendees across hosted published events
        binding.tvHostingCount.text   = stats.hostingCount.toString()
        binding.tvAttendingCount.text = stats.attendingCount.toString()
        binding.tvAttendeesCount.text = stats.totalAttendees.toString()
    }

    override fun showPhoto(photoData: String) {
        currentPhotoData = photoData
        try {
            val bytes = Base64.decode(photoData.substringAfter("base64,"), Base64.DEFAULT)
            val bmp   = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            binding.ivAvatar.setImageBitmap(bmp)
            binding.ivAvatar.show()
            binding.tvInitials.hide()
        } catch (_: Exception) {
            binding.ivAvatar.hide()
            binding.tvInitials.show()
        }
    }

    override fun clearPhoto() {
        currentPhotoData = null
        binding.ivAvatar.hide()
        binding.tvInitials.show()
    }

    override fun onProfileUpdateSuccess() {
        editProfileDialog?.dismiss()
        // Auto-check eligibility after profile update
        presenter.loadAll(forceRefresh = true)
    }

    override fun onPasswordUpdateSuccess() {
        changePasswordDialog?.dismiss()
    }

    // ── Completion percent helper ──────────────────────────────────────────

    private fun calculateCompletionPercent(profile: UserProfile): Int {
        var filled = 0
        val total  = 10
        // Required fields for 100% completion:
        if (profile.firstname.isNotBlank()) filled++
        if (profile.lastname.isNotBlank())  filled++
        if (!profile.email.isNullOrBlank()) filled++
        
        // Age must be a positive number
        val ageVal = when (val a = profile.age) {
            is Int    -> a
            is Double -> a.toInt()
            is String -> a.toIntOrNull()
            else      -> null
        }
        if (ageVal != null && ageVal > 0) filled++
        
        if (!profile.birthdate.isNullOrBlank()) filled++
        if (!profile.street.isNullOrBlank()) filled++
        if (!profile.city.isNullOrBlank()) filled++
        if (!profile.state.isNullOrBlank()) filled++
        if (!profile.country.isNullOrBlank()) filled++
        if (!profile.zipcode.isNullOrBlank()) filled++
        return (filled.toFloat() / total * 100).toInt()
    }

    // ── Sign Out ───────────────────────────────────────────────────────────

    private fun signOut() {
        createStyledAlertDialog(
            context = requireContext(),
            title = "Sign Out",
            message = "Are you sure you want to sign out?",
            positiveButtonText = "Sign Out",
            positiveButtonListener = { _, _ ->
                AppCache.bustAll(requireContext())
                presenter.clearSession()
                val intent = Intent(requireContext(), AuthActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            },
            negativeButtonText = "Cancel",
            negativeButtonListener = null
        ).show()
    }

    // ── Delete Account ─────────────────────────────────────────────────────

    private fun promptDeleteAccount() {
        val input = createStyledDialogEditText(requireContext(), "Type DELETE to confirm")
        AlertDialog.Builder(requireContext(), R.style.HangOutAlertDialogTheme)
            .setTitle("Delete Account")
            .setMessage(
                "This will permanently delete your account and all your data.\n\n" +
                        "This action CANNOT be undone. All your events, RSVPs, and tickets will be lost."
            )
            .setView(input)
            .setPositiveButton("Delete Permanently") { _, _ ->
                if (input.text.toString().trim() == "DELETE") {
                    performDeleteAccount()
                } else {
                    toast("Type DELETE exactly to confirm deletion.")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performDeleteAccount() {
        val session = SessionManager(requireContext())
        val token   = session.getBearerToken()
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val api = RetrofitClient.getApiService(requireContext())
                val response = withContext(Dispatchers.IO) {
                    api.deleteAccount()
                }
                if (response.isSuccessful) {
                    toast("Account deleted successfully.")
                    AppCache.bustAll(requireContext())
                    session.clearSession()
                    val intent = Intent(requireContext(), AuthActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                } else {
                    toast("Failed to delete account. Please try again.")
                }
            } catch (e: Exception) {
                toast("Cannot connect to server.")
            }
        }
    }

    // ── Dialogs ────────────────────────────────────────────────────────────

    private fun showEditProfileDialog() {
        val db = DialogEditProfileBinding.inflate(layoutInflater)
        editProfileBinding = db

        // Setup gender spinner with custom styling
        val genders = arrayOf("Prefer not to say", "Male", "Female", "Non-binary", "Other")
        val genderAdapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_item,
            genders
        )
        genderAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        db.spinnerGender.adapter = genderAdapter

        // Pre-fill current data
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val api      = RetrofitClient.getApiService(requireContext())
                val response = withContext(Dispatchers.IO) { api.getProfile() }
                if (response.isSuccessful && response.body() != null) {
                    val profile = response.body()!!
                    db.etFirstName.setText(profile.firstname)
                    db.etLastName.setText(profile.lastname)
                    db.etEmail.setText(profile.email)

                    // Birthdate
                    try {
                        val birthdateField = profile.javaClass.getDeclaredField("birthdate")
                        birthdateField.isAccessible = true
                        val birthdateVal = birthdateField.get(profile) as? String
                        if (!birthdateVal.isNullOrBlank()) db.etBirthdate.setText(birthdateVal)
                    } catch (_: Exception) {}

                    // Phone
                    try {
                        val phoneField = profile.javaClass.getDeclaredField("phone")
                        phoneField.isAccessible = true
                        val phoneVal = phoneField.get(profile) as? String
                        if (!phoneVal.isNullOrBlank()) db.etPhone.setText(phoneVal)
                    } catch (_: Exception) {}

                    // Street/Barangay
                    try {
                        val streetField = profile.javaClass.getDeclaredField("street")
                        streetField.isAccessible = true
                        val streetVal = streetField.get(profile) as? String
                        if (!streetVal.isNullOrBlank()) db.etStreet.setText(streetVal)
                    } catch (_: Exception) {}

                    // Municipality
                    try {
                        val municipalityVal = profile.city?.takeIf { it.isNotBlank() }
                        if (!municipalityVal.isNullOrBlank()) db.etMunicipality.setText(municipalityVal)
                    } catch (_: Exception) {}

                    // State/Province
                    try {
                        val stateField = profile.javaClass.getDeclaredField("state")
                        stateField.isAccessible = true
                        val stateVal = stateField.get(profile) as? String
                        if (!stateVal.isNullOrBlank()) db.etState.setText(stateVal)
                    } catch (_: Exception) {}

                    // Country
                    try {
                        val countryField = profile.javaClass.getDeclaredField("country")
                        countryField.isAccessible = true
                        val countryVal = countryField.get(profile) as? String
                        if (!countryVal.isNullOrBlank()) db.etCountry.setText(countryVal)
                    } catch (_: Exception) {}

                    // Zip Code
                    try {
                        val zipcodeField = profile.javaClass.getDeclaredField("zipcode")
                        zipcodeField.isAccessible = true
                        val zipcodeVal = zipcodeField.get(profile) as? String
                        if (!zipcodeVal.isNullOrBlank()) db.etZipCode.setText(zipcodeVal)
                    } catch (_: Exception) {}

                    // Bio
                    try {
                        val bioField = profile.javaClass.getDeclaredField("bio")
                        bioField.isAccessible = true
                        val bioVal = bioField.get(profile) as? String
                        if (!bioVal.isNullOrBlank()) db.etBio.setText(bioVal)
                    } catch (_: Exception) {}

                    // Gender
                    try {
                        val genderField = profile.javaClass.getDeclaredField("gender")
                        genderField.isAccessible = true
                        val genderVal = genderField.get(profile) as? String
                        if (!genderVal.isNullOrBlank()) {
                            val genderIndex = genders.indexOf(genderVal)
                            if (genderIndex >= 0) db.spinnerGender.setSelection(genderIndex)
                        }
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }

        currentPhotoData?.let { photoData ->
            try {
                val bytes = Base64.decode(photoData.substringAfter("base64,"), Base64.DEFAULT)
                db.ivPhotoPreview.setImageBitmap(BitmapFactory.decodeByteArray(bytes, 0, bytes.size))
                db.tvPhotoInitials.hide()
                db.btnRemovePhoto.show()
            } catch (_: Exception) {}
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

            // Collect all profile fields
            val birthdate = db.etBirthdate.text.toString().trim()
            val phone = db.etPhone.text.toString().trim()
            val street = db.etStreet.text.toString().trim()
            val municipality = db.etMunicipality.text.toString().trim()
            val state = db.etState.text.toString().trim()
            val country = db.etCountry.text.toString().trim()
            val zipcode = db.etZipCode.text.toString().trim()
            val bio = db.etBio.text.toString().trim()
            val gender = genders[db.spinnerGender.selectedItemPosition]

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val api = RetrofitClient.getApiService(requireContext())

                    val body = mutableMapOf<String, String>()
                    body["firstname"] = firstname
                    body["lastname"]  = lastname
                    body["phone"]     = phone
                    body["street"]    = street
                    body["city"]      = municipality
                    body["state"]     = state
                    body["country"]   = country
                    body["zipcode"]   = zipcode
                    body["bio"]       = bio
                    body["gender"]    = if (gender == "Prefer not to say") "" else gender

                    val response = withContext(Dispatchers.IO) {
                        api.updateAdditionalInfo(body)
                    }

                    if (response.isSuccessful) {
                        AppCache.bust(requireContext(), AppCache.Keys.PROFILE)
                        toast("Profile updated successfully!")
                        presenter.loadAll(forceRefresh = true)
                        editProfileDialog?.dismiss()
                    } else {
                        toast("Failed to update profile. Please try again.")
                    }
                } catch (e: Exception) {
                    toast("Cannot connect to server.")
                }
            }
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

    private fun showAdditionalInfoDialog() {
        val ctx    = requireContext()
        val layout = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 24, 48, 24)
        }

        // Phone
        val etPhone = styleDialogEditText(
            android.widget.EditText(ctx).apply {
                hint = "Phone number"
                inputType = android.text.InputType.TYPE_CLASS_PHONE
            },
            ctx
        )
        // City
        val etCity = styleDialogEditText(
            android.widget.EditText(ctx).apply {
                hint = "City"
                inputType = android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS
            },
            ctx
        )
        // Bio
        val etBio = styleDialogEditText(
            android.widget.EditText(ctx).apply {
                hint = "Short bio (optional)"
                inputType = android.text.InputType.TYPE_CLASS_TEXT or
                        android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE or
                        android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                minLines = 3
                maxLines = 5
                gravity  = android.view.Gravity.TOP
            },
            ctx
        )
        // Gender dropdown
        val genderSpinner = Spinner(ctx)
        val genders = arrayOf("Prefer not to say", "Male", "Female", "Non-binary", "Other")
        val genderAdapter = ArrayAdapter(ctx, android.R.layout.simple_spinner_dropdown_item, genders)
        genderSpinner.adapter = genderAdapter

        fun addLabel(text: String) {
            layout.addView(TextView(ctx).apply {
                this.text = text
                setTextColor(resources.getColor(R.color.text_muted, null))
                textSize = 11f
                setPadding(0, 16, 0, 4)
            })
        }

        addLabel("PHONE")
        layout.addView(etPhone)
        addLabel("CITY")
        layout.addView(etCity)
        addLabel("GENDER")
        layout.addView(genderSpinner)
        addLabel("BIO")
        layout.addView(etBio)

        // Pre-fill from API
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val api      = RetrofitClient.getApiService(ctx)
                val response = withContext(Dispatchers.IO) { api.getProfile() }
                if (response.isSuccessful && response.body() != null) {
                    val profile = response.body()!!
                    // UserProfile may not have all fields depending on your data class
                    // We'll try to read them if they exist
                    try {
                        val phoneField = profile.javaClass.getDeclaredField("phone")
                        phoneField.isAccessible = true
                        val phoneVal = phoneField.get(profile) as? String
                        if (!phoneVal.isNullOrBlank()) etPhone.setText(phoneVal)
                    } catch (_: Exception) {}

                    try {
                        val cityField = profile.javaClass.getDeclaredField("city")
                        cityField.isAccessible = true
                        val cityVal = cityField.get(profile) as? String
                        if (!cityVal.isNullOrBlank()) etCity.setText(cityVal)
                    } catch (_: Exception) {}

                    try {
                        val bioField = profile.javaClass.getDeclaredField("bio")
                        bioField.isAccessible = true
                        val bioVal = bioField.get(profile) as? String
                        if (!bioVal.isNullOrBlank()) etBio.setText(bioVal)
                    } catch (_: Exception) {}

                    try {
                        val genderField = profile.javaClass.getDeclaredField("gender")
                        genderField.isAccessible = true
                        val genderVal = genderField.get(profile) as? String
                        if (!genderVal.isNullOrBlank()) {
                            val idx = genders.indexOfFirst {
                                it.equals(genderVal, ignoreCase = true)
                            }
                            if (idx >= 0) genderSpinner.setSelection(idx)
                        }
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }

        AlertDialog.Builder(ctx, R.style.HangOutAlertDialogTheme)
            .setTitle("Additional Info")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val phone  = etPhone.text.toString().trim()
                val city   = etCity.text.toString().trim()
                val bio    = etBio.text.toString().trim()
                val gender = if (genderSpinner.selectedItemPosition == 0) ""
                else genders[genderSpinner.selectedItemPosition]
                saveAdditionalInfo(phone, city, bio, gender)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveAllProfileFields(birthdate: String, phone: String, street: String, municipality: String, state: String, country: String, zipcode: String, bio: String, gender: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val api = RetrofitClient.getApiService(requireContext())
                val body = mutableMapOf<String, String>()
                body["birthdate"] = birthdate
                body["phone"] = phone
                body["street"] = street
                body["municipality"] = municipality
                body["state"] = state
                body["country"] = country
                body["zipcode"] = zipcode
                body["bio"] = bio
                body["gender"] = if (gender == "Prefer not to say") "" else gender

                if (body.isNotEmpty()) {
                    val response = withContext(Dispatchers.IO) {
                        api.updateAdditionalInfo(body)
                    }
                    if (response.isSuccessful) {
                        AppCache.bust(requireContext(), AppCache.Keys.PROFILE)
                        toast("Profile updated successfully!")
                        presenter.loadAll(forceRefresh = true)
                    } else {
                        toast("Failed to update profile.")
                    }
                } else {
                    toast("Profile updated!")
                }
            } catch (e: Exception) {
                toast("Cannot connect to server.")
            }
        }
    }

    private fun saveAdditionalInfo(phone: String, city: String, bio: String, gender: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val api = RetrofitClient.getApiService(requireContext())
                val body = mutableMapOf<String, String>()
                body["phone"]  = phone
                body["city"]   = city
                body["bio"]    = bio
                body["gender"] = if (gender == "Prefer not to say") "" else gender

                val response = withContext(Dispatchers.IO) {
                    api.updateAdditionalInfo(body)
                }
                if (response.isSuccessful) {
                    AppCache.bust(requireContext(), AppCache.Keys.PROFILE)
                    toast("Info updated successfully!")
                    presenter.loadAll(forceRefresh = true)
                } else {
                    toast("Failed to update info.")
                }
            } catch (e: Exception) {
                toast("Cannot connect to server.")
            }
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

    private fun showNotificationPreferencesDialog() {
        val ctx    = requireContext()
        val scroll = android.widget.ScrollView(ctx)
        val layout = android.widget.LinearLayout(ctx).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(48, 16, 48, 24)
        }
        scroll.addView(layout)

        data class NotifPref(
            val label: String,
            val subtitle: String,
            val key: String
        )

        val prefs = listOf(
            NotifPref("New RSVP",           "When someone RSVPs to your event",   "notifNewRsvp"),
            NotifPref("Payment Proof",       "When someone uploads payment proof", "notifPaymentProof"),
            NotifPref("RSVP Cancelled",      "When an attendee cancels their RSVP","notifRsvpCancelled"),
            NotifPref("Refund Request",      "When an attendee requests a refund", "notifRefundRequest"),
            NotifPref("Payment Approved",    "When your payment is approved",      "notifPaymentApproved"),
            NotifPref("Payment Rejected",    "When your payment is rejected",      "notifPaymentRejected"),
            NotifPref("Refund Processed",    "When your refund is processed",      "notifRefundProcessed"),
            NotifPref("Seat Assigned",       "When you're assigned a seat",        "notifSeatAssigned"),
            NotifPref("Event Cancelled",     "When an event you joined is cancelled","notifEventCancelled"),
            NotifPref("Event Reminder",      "Reminders for upcoming events",      "notifEventReminder")
        )

        val switches = mutableMapOf<String, SwitchMaterial>()

        prefs.forEach { pref ->
            val row = android.widget.LinearLayout(ctx).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity     = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, 8, 0, 8)
            }
            val textCol = android.widget.LinearLayout(ctx).apply {
                orientation = android.widget.LinearLayout.VERTICAL
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                )
            }
            textCol.addView(TextView(ctx).apply {
                text = pref.label
                setTextColor(resources.getColor(R.color.text_primary, null))
                textSize = 14f
            })
            textCol.addView(TextView(ctx).apply {
                text = pref.subtitle
                setTextColor(resources.getColor(R.color.text_muted, null))
                textSize = 12f
            })
            val sw = SwitchMaterial(ctx).apply {
                isChecked = true // default on; will update from API
            }
            switches[pref.key] = sw
            row.addView(textCol)
            row.addView(sw)
            layout.addView(row)

            // Thin divider
            layout.addView(View(ctx).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1
                ).also { it.setMargins(0, 4, 0, 4) }
                setBackgroundColor(resources.getColor(R.color.divider, null))
            })
        }

        // Load current preferences from API
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val api      = RetrofitClient.getApiService(ctx)
                val response = withContext(Dispatchers.IO) { api.getNotificationPreferences() }
                if (response.isSuccessful && response.body() != null) {
                    val prefMap = response.body()!!
                    switches.forEach { (key, sw) ->
                        val apiKey = camelToSnake(key)
                        val value  = prefMap[key] as? Boolean
                            ?: prefMap[apiKey] as? Boolean
                            ?: true
                        sw.isChecked = value
                    }
                }
            } catch (_: Exception) {}
        }

        AlertDialog.Builder(ctx, R.style.HangOutAlertDialogTheme)
            .setTitle("Notification Preferences")
            .setView(scroll)
            .setPositiveButton("Save") { _, _ ->
                val body = switches.mapValues { it.value.isChecked }
                saveNotificationPreferences(body)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun camelToSnake(camel: String): String {
        return camel.replace(Regex("([A-Z])")) { "_${it.value.lowercase()}" }
    }

    private fun saveNotificationPreferences(prefs: Map<String, Boolean>) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val api      = RetrofitClient.getApiService(requireContext())
                val response = withContext(Dispatchers.IO) {
                    api.updateNotificationPreferences(prefs)
                }
                if (response.isSuccessful) {
                    toast("Notification preferences saved!")
                } else {
                    toast("Failed to save preferences.")
                }
            } catch (e: Exception) {
                toast("Cannot connect to server.")
            }
        }
    }

    private fun showHostingEligibilityDialog() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val api      = RetrofitClient.getApiService(requireContext())
                val response = withContext(Dispatchers.IO) { api.getProfile() }
                if (response.isSuccessful && response.body() != null) {
                    val profile = response.body()!!
                    val completionPercent = calculateCompletionPercent(profile)
                    val isEligible = completionPercent == 100
                    
                    // Age validation
                    val ageVal = when (val a = profile.age) {
                        is Int    -> a
                        is Double -> a.toInt()
                        is String -> a.toIntOrNull()
                        else      -> null
                    }
                    val hasValidAge = ageVal != null && ageVal > 0

                    val statusIcon = if (isEligible) "✅" else "⚠️"
                    val statusText = if (isEligible)
                        "You are eligible to host events!"
                    else
                        "Complete your profile to unlock hosting.\nAll fields are required."

                    val checklist = buildString {
                        appendLine("Profile Checklist:")
                        appendLine("${if (profile.firstname.isNotBlank()) "✓" else "✗"} First name")
                        appendLine("${if (profile.lastname.isNotBlank()) "✓" else "✗"} Last name")
                        appendLine("${if (!profile.email.isNullOrBlank()) "✓" else "✗"} Email")
                        appendLine("${if (hasValidAge) "✓" else "✗"} Age")
                        appendLine("${if (!profile.birthdate.isNullOrBlank()) "✓" else "✗"} Birthdate")
                        appendLine("${if (!profile.street.isNullOrBlank()) "✓" else "✗"} Street/Barangay")
                        appendLine("${if (!profile.city.isNullOrBlank()) "✓" else "✗"} Municipality/City")
                        appendLine("${if (!profile.state.isNullOrBlank()) "✓" else "✗"} State/Province")
                        appendLine("${if (!profile.country.isNullOrBlank()) "✓" else "✗"} Country")
                        appendLine("${if (!profile.zipcode.isNullOrBlank()) "✓" else "✗"} Zip Code")
                        appendLine()
                        appendLine("Completion: $completionPercent%")
                    }

                    AlertDialog.Builder(requireContext(), R.style.HangOutAlertDialogTheme)
                        .setTitle("$statusIcon  Hosting Eligibility")
                        .setMessage("$statusText\n\n$checklist")
                        .setPositiveButton("Close", null)
                        .apply {
                            if (!isEligible) {
                                setNegativeButton("Update Profile") { _, _ ->
                                    showEditProfileDialog()
                                }
                            }
                        }
                        .show()
                }
            } catch (_: Exception) {
                toast("Cannot load profile.")
            }
        }
    }

    // ── Eligibility Utilities ─────────────────────────────────────────────

    fun checkHostingEligibility(): Boolean {
        // This method will be called from NavActivity
        // Returns true if eligible, false if not
        return false // Placeholder - will check async below
    }

    fun checkHostingEligibilityAsync(onResult: (isEligible: Boolean) -> Unit) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val api = RetrofitClient.getApiService(requireContext())
                val response = withContext(Dispatchers.IO) { api.getProfile() }
                if (response.isSuccessful && response.body() != null) {
                    val profile = response.body()!!
                    val completionPercent = calculateCompletionPercent(profile)
                    val isEligible = completionPercent == 100
                    onResult(isEligible)
                } else {
                    onResult(false)
                }
            } catch (_: Exception) {
                onResult(false)
            }
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun setupSettingsRow(
        rowView: View,
        title: String,
        subtitle: String,
        iconRes: Int,
        danger: Boolean = false,
        onClick: () -> Unit
    ) {
        rowView.findViewById<TextView>(R.id.tvRowTitle).apply {
            text = title
            if (danger) setTextColor(
                androidx.core.content.ContextCompat.getColor(rowView.context, R.color.red_accent)
            )
        }
        rowView.findViewById<TextView>(R.id.tvRowSubtitle).text = subtitle
        rowView.findViewById<ImageView>(R.id.ivRowIcon).apply {
            setImageResource(iconRes)
            if (danger) setColorFilter(
                androidx.core.content.ContextCompat.getColor(rowView.context, R.color.red_accent)
            )
        }
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