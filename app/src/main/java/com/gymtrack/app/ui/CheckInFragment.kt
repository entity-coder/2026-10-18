package com.gymtrack.app.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.gymtrack.app.R
import com.gymtrack.app.data.CheckIn
import com.gymtrack.app.data.GymDatabase
import com.gymtrack.app.databinding.FragmentCheckinBinding
import com.gymtrack.app.logic.MembershipCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

/**
 * Front-desk check-in screen: search a member, tap "Check in".
 * One tap per member per day (enforced by a unique DB index); the header
 * shows today's visitor count and names.
 */
class CheckInFragment : Fragment() {

    private var _binding: FragmentCheckinBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCheckinBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = GymDatabase.getInstance(requireContext())
        val today = MembershipCalculator.startOfToday()

        val adapter = MemberAdapter(
            onMemberClick = null,
            onCheckIn = { member ->
                viewLifecycleOwner.lifecycleScope.launch {
                    db.checkInDao().checkIn(CheckIn(memberId = member.id, day = today))
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.checked_in_format, member.name),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
        binding.memberList.layoutManager = LinearLayoutManager(requireContext())
        binding.memberList.adapter = adapter

        val todayAdapter = TodayCheckInAdapter()
        binding.todayList.layoutManager = LinearLayoutManager(requireContext())
        binding.todayList.adapter = todayAdapter
        binding.todayList.isNestedScrollingEnabled = false

        val queryFlow = MutableStateFlow("")
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                queryFlow.value = s?.toString().orEmpty()
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    queryFlow.flatMapLatest { q ->
                        val members = if (q.isBlank()) db.memberDao().observeAll()
                        else db.memberDao().search(q.trim())
                        val checkedIn = db.checkInDao().checkedInMemberIds(today)
                        combine(members, checkedIn) { list, done -> list to done }
                    }.collect { (members, checkedInIds) ->
                        adapter.submit(members.map {
                            MemberAdapter.Row(
                                member = it,
                                status = MembershipCalculator.statusOf(it, today),
                                checkedInToday = checkedInIds.contains(it.id)
                            )
                        })
                    }
                }
                launch {
                    db.checkInDao().observeForDay(today).collect { rows ->
                        binding.todayCount.text =
                            getString(R.string.today_checkins_format, rows.size)
                        todayAdapter.submit(rows)
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
