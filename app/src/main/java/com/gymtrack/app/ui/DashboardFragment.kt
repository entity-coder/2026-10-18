package com.gymtrack.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.gymtrack.app.data.GymDatabase
import com.gymtrack.app.databinding.FragmentDashboardBinding
import com.gymtrack.app.logic.BackupExporter
import com.gymtrack.app.logic.MembershipCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Front-desk overview: active members, fees collected this month,
 * today's check-in count, and the expiring-soon watchlist.
 */
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = GymDatabase.getInstance(requireContext())
        val today = MembershipCalculator.startOfToday()
        val soonEnd = today + MembershipCalculator.EXPIRING_SOON_DAYS * 86_400_000L

        val adapter = ExpiringAdapter()
        binding.expiringList.layoutManager = LinearLayoutManager(requireContext())
        binding.expiringList.adapter = adapter
        binding.expiringList.isNestedScrollingEnabled = false

        binding.exportButton.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val message = withContext(Dispatchers.IO) {
                    BackupExporter.export(requireContext())
                }
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    db.memberDao().countActive(today).collect { count ->
                        binding.activeMembersValue.text = count.toString()
                    }
                }
                launch {
                    db.paymentDao().sumSince(MembershipCalculator.startOfMonth()).collect { total ->
                        binding.feesMonthValue.text = MembershipCalculator.formatMoney(total)
                    }
                }
                launch {
                    db.checkInDao().countForDay(today).collect { count ->
                        binding.checkinsValue.text = count.toString()
                    }
                }
                launch {
                    db.memberDao().expiringSoon(today, soonEnd).collect { members ->
                        binding.expiringSoonValue.text = members.size.toString()
                        adapter.submit(members)
                        binding.expiringEmpty.visibility =
                            if (members.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
