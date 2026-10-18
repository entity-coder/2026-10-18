package com.gymtrack.app.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.gymtrack.app.R
import com.gymtrack.app.data.GymDatabase
import com.gymtrack.app.data.Member
import com.gymtrack.app.data.Payment
import com.gymtrack.app.databinding.DialogPaymentBinding
import com.gymtrack.app.logic.MembershipCalculator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Records one fee payment: pick a member, confirm the amount (prefilled
 * with their plan fee), optional note. Saving inserts the Payment row AND
 * extends the member's paid period via [MembershipCalculator.renewedExpiry].
 */
class RecordPaymentDialogFragment : DialogFragment() {

    private var _binding: DialogPaymentBinding? = null
    private val binding get() = _binding!!
    private var members: List<Member> = emptyList()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogPaymentBinding.inflate(LayoutInflater.from(requireContext()))
        val preselectId = arguments?.getLong(ARG_MEMBER_ID, -1L) ?: -1L

        lifecycleScope.launch {
            val db = GymDatabase.getInstance(requireContext())
            members = db.memberDao().observeAll().first()
            if (_binding == null || !isAdded) return@launch

            binding.memberSpinner.adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                members.map { "${it.name} (${it.phone})" }
            ).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

            val preselectIndex = members.indexOfFirst { it.id == preselectId }
            if (preselectIndex >= 0) {
                binding.memberSpinner.setSelection(preselectIndex)
            } else if (members.isNotEmpty()) {
                binding.memberSpinner.setSelection(0)
            }
            prefillAmount()

            binding.memberSpinner.onItemSelectedListener =
                object : android.widget.AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: android.widget.AdapterView<*>?, v: android.view.View?,
                        position: Int, id: Long
                    ) = prefillAmount()

                    override fun onNothingSelected(
                        parent: android.widget.AdapterView<*>?
                    ) {}
                }
        }

        return AlertDialog.Builder(requireContext())
            .setTitle(R.string.record_payment)
            .setView(binding.root)
            .setPositiveButton(R.string.save, null) // validated on click
            .setNegativeButton(R.string.cancel, null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        if (savePayment()) dialog.dismiss()
                    }
                }
            }
    }

    /** Prefills the amount with the selected member's plan fee. */
    private fun prefillAmount() {
        val position = binding.memberSpinner.selectedItemPosition
        if (position in members.indices) {
            val fee = members[position].planFee
            binding.amountInput.setText(
                if (fee % 1.0 == 0.0) fee.toLong().toString() else fee.toString()
            )
        }
    }

    /** Returns false to keep the dialog open when validation fails. */
    private fun savePayment(): Boolean {
        val position = binding.memberSpinner.selectedItemPosition
        if (position !in members.indices) {
            Toast.makeText(requireContext(), R.string.error_no_members, Toast.LENGTH_SHORT).show()
            return false
        }
        val amount = binding.amountInput.text.toString().trim().toDoubleOrNull()
        if (amount == null || amount <= 0) {
            binding.amountInput.error = getString(R.string.error_fee_positive)
            return false
        }
        val member = members[position]
        val note = binding.noteInput.text.toString().trim()

        lifecycleScope.launch {
            val db = GymDatabase.getInstance(requireContext())
            val today = MembershipCalculator.startOfToday()
            db.paymentDao().insert(
                Payment(
                    memberId = member.id,
                    amount = amount,
                    date = System.currentTimeMillis(),
                    note = note
                )
            )
            // Extend the paid period: from today if lapsed, else from current end.
            val newEnd = MembershipCalculator.renewedExpiry(member, today)
            db.memberDao().update(member.copy(planEndDate = newEnd, planStartDate = today))
        }
        Toast.makeText(
            requireContext(),
            getString(
                R.string.payment_saved_format,
                MembershipCalculator.formatMoney(amount),
                member.name
            ),
            Toast.LENGTH_SHORT
        ).show()
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_MEMBER_ID = "member_id"

        /** Pass a member id to preselect it, or null for a free choice. */
        fun newInstance(memberId: Long?): RecordPaymentDialogFragment =
            RecordPaymentDialogFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_MEMBER_ID, memberId ?: -1L)
                }
            }
    }
}
