package com.gymtrack.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.gymtrack.app.data.GymDatabase
import com.gymtrack.app.databinding.FragmentPaymentsBinding
import com.gymtrack.app.logic.MembershipCalculator
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Fee desk: record a payment, see the defaulter list, and browse the
 * full payment history (newest first).
 */
class PaymentsFragment : Fragment() {

    private var _binding: FragmentPaymentsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaymentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = GymDatabase.getInstance(requireContext())
        val today = MembershipCalculator.startOfToday()

        val defaulterAdapter = MemberAdapter(onMemberClick = null, onCheckIn = null)
        binding.defaulterList.layoutManager = LinearLayoutManager(requireContext())
        binding.defaulterList.adapter = defaulterAdapter
        binding.defaulterList.isNestedScrollingEnabled = false

        val paymentAdapter = PaymentAdapter()
        binding.paymentList.layoutManager = LinearLayoutManager(requireContext())
        binding.paymentList.adapter = paymentAdapter

        binding.recordPaymentButton.setOnClickListener {
            RecordPaymentDialogFragment.newInstance(null)
                .show(parentFragmentManager, "record_payment")
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    db.memberDao().defaulters(today).collect { members ->
                        defaulterAdapter.submit(members.map {
                            MemberAdapter.Row(it, MembershipCalculator.statusOf(it, today))
                        })
                        binding.noDefaulters.visibility =
                            if (members.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    // Join payments with member names for the history list.
                    combine(
                        db.paymentDao().observeAll(),
                        db.memberDao().observeAll()
                    ) { payments, members ->
                        val names = members.associate { it.id to it.name }
                        payments.map { PaymentAdapter.Row(it, names[it.memberId].orEmpty()) }
                    }.collect { rows ->
                        paymentAdapter.submit(rows)
                        binding.noPayments.visibility =
                            if (rows.isEmpty()) View.VISIBLE else View.GONE
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
