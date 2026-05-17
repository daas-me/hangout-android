package com.hangout.app.ui.nav

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.hangout.app.R
import com.hangout.app.databinding.ActivityMainBinding
import com.hangout.app.ui.createevent.CreateEventActivity
import com.hangout.app.ui.discover.DiscoverFragment
import com.hangout.app.ui.home.HomeFragment
import com.hangout.app.ui.myhangouts.MyHangoutsFragment
import com.hangout.app.ui.profile.ProfileFragment
import com.hangout.app.ui.components.createStyledAlertDialog
import com.hangout.app.utils.CustomToast
import com.hangout.app.utils.toast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.lifecycle.lifecycleScope
import com.hangout.app.utils.show

class NavActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNavigation.menu.getItem(2).isEnabled = false
        binding.bottomNavigation.background = null

        // ✅ Hide the profile icon here — unconditionally, once at startup
        binding.bottomNavigation.menu.findItem(R.id.nav_profile)
            .setIcon(android.R.color.transparent)

        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
            binding.bottomNavigation.selectedItemId = R.id.nav_home
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home     -> { replaceFragment(HomeFragment()); true }
                R.id.nav_discover -> { replaceFragment(DiscoverFragment()); true }
                R.id.nav_hangouts -> { replaceFragment(MyHangoutsFragment()); true }
                R.id.nav_profile  -> { replaceFragment(ProfileFragment()); true }
                else -> false
            }
        }

        binding.fabCreate.setOnClickListener {
            checkHostingEligibilityAndNavigate()
        }

        loadAndDisplayProfileAvatar()
    }

    private fun loadAndDisplayProfileAvatar() {
        lifecycleScope.launch {
            try {
                val repo = com.hangout.app.repository.UserRepository(this@NavActivity)

                // Do network + decoding off the main thread
                val bitmap = withContext(Dispatchers.IO) {
                    when (val result = repo.getPhoto(forceRefresh = true)) {
                        is com.hangout.app.repository.Result.Success -> {
                            val photoData = result.data
                            val clean = photoData.substringAfter("base64,")
                            val bytes = Base64.decode(clean, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        }
                        is com.hangout.app.repository.Result.Error -> null
                    }
                }

                // Back on Main thread — update UI
                if (bitmap != null) {
                    Glide.with(this@NavActivity)
                        .load(bitmap)
                        .transform(CircleCrop())
                        .into(binding.ivProfileAvatar)
                } else {
                    binding.ivProfileAvatar.setImageResource(R.drawable.ic_user)
                }

                binding.ivProfileAvatar.show()
                binding.ivProfileRing.show()

                // Wait for layout pass before positioning
                binding.bottomNavigation.post {
                    positionAvatarOverProfileItem()
                }

            } catch (e: Exception) {
                android.util.Log.e("NavActivity", "Avatar load failed: ${e.message}", e)
                binding.ivProfileAvatar.setImageResource(R.drawable.ic_user)
                binding.ivProfileAvatar.show()
                binding.ivProfileRing.show()
                binding.bottomNavigation.post {
                    positionAvatarOverProfileItem()
                }
            }
        }
    }

    private fun positionAvatarOverProfileItem() {
        val navWidth = binding.bottomNavigation.width
        val itemCount = binding.bottomNavigation.menu.size()          // 5
        val itemWidth = navWidth / itemCount                           // width of each slot
        val lastItemCenterX = navWidth - (itemWidth / 2)              // center of last item

        val avatarSize = binding.ivProfileAvatar.width
        val navLeft = binding.bottomNavigation.left

        // Translate avatar to center over the profile icon
        val offset = (navLeft + lastItemCenterX - binding.ivProfileAvatar.width / 2 - binding.ivProfileAvatar.left).toFloat()
        binding.ivProfileAvatar.translationX = offset
        binding.ivProfileRing.translationX = offset
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }

    fun setSelectedNavItem(itemId: Int) {
        binding.bottomNavigation.selectedItemId = itemId
    }

    private fun checkHostingEligibilityAndNavigate() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val api = com.hangout.app.network.RetrofitClient.getApiService(this@NavActivity)
                val response = api.getProfile()
                if (response.isSuccessful && response.body() != null) {
                    val profile = response.body()!!
                    val completionPercent = calculateCompletionPercent(profile)
                    if (completionPercent == 100) {
                        // Eligible - allow event creation
                        startActivity(android.content.Intent(this@NavActivity, CreateEventActivity::class.java))
                    } else {
                        // Not eligible - show eligibility dialog
                        showEligibilityBlockDialog(completionPercent, profile)
                    }
                } else {
                    this@NavActivity.toast("Unable to verify eligibility", CustomToast.Type.ERROR)
                }
            } catch (e: Exception) {
                this@NavActivity.toast("Cannot connect to server", CustomToast.Type.ERROR)
            }
        }
    }

    private fun showEligibilityBlockDialog(completionPercent: Int, profile: com.hangout.app.data.UserProfile) {
        val ageVal = when (val a = profile.age) {
            is Int    -> a
            is Double -> a.toInt()
            is String -> a.toIntOrNull()
            else      -> null
        }
        val hasValidAge = ageVal != null && ageVal > 0

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

        createStyledAlertDialog(
            context = this,
            title = "⚠️  Cannot Create Event",
            message = "Your profile is incomplete. All fields are required to host events.\n\n$checklist",
            positiveButtonText = "Complete Profile",
            positiveButtonListener = { _, _ ->
                // Navigate to profile and show edit dialog
                binding.bottomNavigation.selectedItemId = R.id.nav_profile
            },
            negativeButtonText = "Later",
            cancelable = true
        ).show()
    }

    private fun calculateCompletionPercent(profile: com.hangout.app.data.UserProfile): Int {
        var filled = 0
        val total  = 10
        if (profile.firstname.isNotBlank()) filled++
        if (profile.lastname.isNotBlank())  filled++
        if (!profile.email.isNullOrBlank()) filled++
        
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
}