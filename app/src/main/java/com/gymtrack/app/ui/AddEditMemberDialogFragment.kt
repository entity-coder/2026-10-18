package com.gymtrack.app.ui

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.gymtrack.app.R
import com.gymtrack.app.data.GymDatabase
import com.gymtrack.app.data.Member
import com.gymtrack.app.data.PlanType
import com.gymtrack.app.databinding.DialogMemberBinding
import com.gymtrack.app.logic.MembershipCalculator
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * Add / edit member dialog: name, phone, plan, fee, optional photo from
 * the system photo picker (no storage permission needed).
 *
 * New members start a paid period today; editing never rewrites the
 * paid-period dates — those only move via recorded payments.
 */
class AddEditMemberDialogFragment : DialogFragment() {

    private var _binding: DialogMemberBinding? = null
    private val binding get() = _binding!!
    private var photoUriString: String? = null
    private var editingMember: Member? = null

    private val pickPhoto = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            // Keep read access across restarts; harmless if the grant is
            // already persistent (the picker grants it on most devices).
            try {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Fall through: the URI still works for this session.
            }
            photoUriString = uri.toString()
            binding.photoPreview.setImageURI(uri)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogMemberBinding.inflate(LayoutInflater.from(requireContext()))

        val planNames = PlanType.values().map { it.displayName }
        binding.planSpinner.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_item, planNames
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        val memberId = arguments?.getLong(ARG_MEMBER_ID, -1L) ?: -1L
        val isEdit = memberId != -1L

        if (isEdit) {
            // Load the existing member, then fill the form (one-shot read).
            lifecycleScope.launch {
                val member = GymDatabase.getInstance(requireContext())
                    .memberDao().observeById(memberId).firstOrNull()
                if (member != null && _binding != null && isAdded) {
                    editingMember = member
                    fillForm(member)
                }
            }
        }

        binding.pickPhotoButton.setOnClickListener {
            pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        return AlertDialog.Builder(requireContext())
            .setTitle(if (isEdit) R.string.edit_member else R.string.add_member)
            .setView(binding.root)
            .setPositiveButton(R.string.save, null) // overridden below for validation
            .setNegativeButton(R.string.cancel, null)
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        if (saveMember(memberId)) dialog.dismiss()
                    }
                }
            }
    }

    private fun fillForm(member: Member) {
        binding.nameInput.setText(member.name)
        binding.phoneInput.setText(member.phone)
        binding.planSpinner.setSelection(PlanType.values().indexOf(member.planType))
        binding.feeInput.setText(
            if (member.planFee % 1.0 == 0.0) member.planFee.toLong().toString()
            else member.planFee.toString()
        )
        photoUriString = member.photoUri
        if (!member.photoUri.isNullOrBlank()) {
            try {
                binding.photoPreview.setImageURI(Uri.parse(member.photoUri))
            } catch (_: Exception) {
                // Keep the placeholder; the stored URI may have gone stale.
            }
        }
    }

    /** Validates and saves; returns false to keep the dialog open on errors. */
    private fun saveMember(memberId: Long): Boolean {
        val name = binding.nameInput.text.toString().trim()
        val phone = binding.phoneInput.text.toString().trim()
        val fee = binding.feeInput.text.toString().trim().toDoubleOrNull()
        val planType = PlanType.values()[binding.planSpinner.selectedItemPosition]

        if (name.isEmpty()) {
            binding.nameInput.error = getString(R.string.error_name_required)
            return false
        }
        if (phone.isEmpty()) {
            binding.phoneInput.error = getString(R.string.error_phone_required)
            return false
        }
        if (fee == null || fee <= 0) {
            binding.feeInput.error = getString(R.string.error_fee_positive)
            return false
        }

        lifecycleScope.launch {
            val dao = GymDatabase.getInstance(requireContext()).memberDao()
            val existing = editingMember
            if (existing != null && memberId != -1L) {
                dao.update(
                    existing.copy(
                        name = name,
                        phone = phone,
                        planType = planType,
                        planFee = fee,
                        photoUri = photoUriString
                    )
                )
            } else {
                val today = MembershipCalculator.startOfToday()
                dao.insert(
                    Member(
                        name = name,
                        phone = phone,
                        photoUri = photoUriString,
                        planType = planType,
                        planFee = fee,
                        planStartDate = today,
                        planEndDate = MembershipCalculator.newMemberExpiry(planType.months, today)
                    )
                )
            }
        }
        Toast.makeText(requireContext(), R.string.member_saved, Toast.LENGTH_SHORT).show()
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_MEMBER_ID = "member_id"

        /** Pass null to add a new member, or an id to edit. */
        fun newInstance(memberId: Long?): AddEditMemberDialogFragment =
            AddEditMemberDialogFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_MEMBER_ID, memberId ?: -1L)
                }
            }
    }
}
