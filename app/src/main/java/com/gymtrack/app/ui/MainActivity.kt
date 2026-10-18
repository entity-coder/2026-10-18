package com.gymtrack.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.gymtrack.app.R
import com.gymtrack.app.databinding.ActivityMainBinding

/**
 * Single-activity host. Four tabs (bottom navigation), one fragment each —
 * the whole app is offline and local, so no navigation graph is needed.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_dashboard -> DashboardFragment()
                R.id.nav_members -> MembersFragment()
                R.id.nav_checkin -> CheckInFragment()
                R.id.nav_payments -> PaymentsFragment()
                else -> return@setOnItemSelectedListener false
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit()
            true
        }

        if (savedInstanceState == null) {
            // Triggers the listener above, which shows the dashboard.
            binding.bottomNav.selectedItemId = R.id.nav_dashboard
        }
    }
}
