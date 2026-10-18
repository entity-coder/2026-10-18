package com.gymtrack.app.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.gymtrack.app.data.GymDatabase
import com.gymtrack.app.databinding.FragmentMembersBinding
import com.gymtrack.app.logic.MembershipCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

/**
 * Member roster with live search. Tapping a row opens the member detail
 * screen; the + button opens the add-member dialog.
 */
class MembersFragment : Fragment() {

    private var _binding: FragmentMembersBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMembersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = GymDatabase.getInstance(requireContext())
        val today = MembershipCalculator.startOfToday()

        val adapter = MemberAdapter(
            onMemberClick = { member ->
                val intent = Intent(requireContext(), MemberDetailActivity::class.java)
                intent.putExtra(MemberDetailActivity.EXTRA_MEMBER_ID, member.id)
                startActivity(intent)
            },
            onCheckIn = null // check-ins happen on the Check-in tab
        )
        binding.memberList.layoutManager = LinearLayoutManager(requireContext())
        binding.memberList.adapter = adapter

        binding.addMemberFab.setOnClickListener {
            AddEditMemberDialogFragment.newInstance(null)
                .show(parentFragmentManager, "add_member")
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Re-query on every keystroke: search() returns a fresh Flow per query.
                MutableStateFlow("").also { queryFlow ->
                    binding.searchInput.addTextChangedListener(object : TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                        override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                        override fun afterTextChanged(s: Editable?) {
                            queryFlow.value = s?.toString().orEmpty()
                        }
                    })
                    queryFlow.flatMapLatest { q ->
                        if (q.isBlank()) db.memberDao().observeAll()
                        else db.memberDao().search(q.trim())
                    }.collect { members ->
                        adapter.submit(members.map {
                            MemberAdapter.Row(it, MembershipCalculator.statusOf(it, today))
                        })
                        binding.emptyHint.visibility =
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
