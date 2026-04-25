package com.hangout.app.ui.nav

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.hangout.app.R
import com.hangout.app.databinding.ActivityMainBinding
import com.hangout.app.ui.discover.DiscoverFragment
import com.hangout.app.ui.home.HomeFragment
import com.hangout.app.ui.profile.ProfileFragment

class NavActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNavigation.menu.getItem(2).isEnabled = false
        binding.bottomNavigation.background = null

        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
            binding.bottomNavigation.selectedItemId = R.id.nav_home
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home     -> { replaceFragment(HomeFragment()); true }
                R.id.nav_discover -> { replaceFragment(DiscoverFragment()); true }
                R.id.nav_hangouts -> { true /* TODO: MyHangOutsFragment */ }
                R.id.nav_profile  -> { replaceFragment(ProfileFragment()); true }
                else -> false
            }
        }

        binding.fabCreate.setOnClickListener {
            // TODO: navigate to Create HangOut
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()
    }
}