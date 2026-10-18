package com.gymtrack.app.ui

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.gymtrack.app.R
import com.gymtrack.app.data.CheckIn
import com.gymtrack.app.data.GymDatabase
import com.gymtrack.app.data.Member
import com.gymtrack.app.data.MembershipStatus
import com.gymtrack.app.databinding.ActivityMemberDetailBinding
import com.gymtrack.app.logic.MembershipCalculator
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * One member's full card: photo, plan, fee status, quick actions
 * (check in / record payment / edit / delete) and their payment history.
 */
class MemberDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMemberDetailBinding
    private var memberId: Long = -1L
    private var currentMember: Member? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMemberDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        memberId = intent.getLongExtra(EXTRA_MEMBER_ID, -1L)
        if (memberId == -1L) {
            finish()
            return
        }

        val db = GymDatabase.getInstance(this)
        val today = MembershipCalculator.startOfToday()

        val paymentAdapter = PaymentAdapter()
        binding.paymentHistory.layoutManager = LinearLayoutManager(this)
        binding.paymentHistory.adapter = paymentAdapter
        binding.paymentHistory.isNestedScrollingEnabled = false

        binding.checkInButton.setOnClickListener {
            lifecycleScope.launch {
                db.checkInDao().checkIn(CheckIn(memberId = memberId, day = today))
                Toast.makeText(
                    this@MemberDetailActivity,
                    getString(R.string.checked_in_format, currentMember?.name.orEmpty()),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        binding.recordPaymentButton.setOnClickListener {
            RecordPaymentDialogFragment.newInstance(memberId)
                .show(supportFragmentManager, "record_payment")
        }
        binding.editButton.setOnClickListener {
            AddEditMemberDialogFragment.newInstance(memberId)
                .show(supportFragmentManager, "edit_member")
        }
        binding.deleteButton.setOnClickListener {
            val name = currentMember?.name.orEmpty()
            AlertDialog.Builder(this)
                .setTitle(R.string.delete_title)
                .setMessage(getString(R.string.delete_message, name))
                .setPositiveButton(R.string.delete) { _, _ ->
                    lifecycleScope.launch {
                        currentMember?.let { db.memberDao().delete(it) }
                        finish()
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    combine(
                        db.memberDao().observeById(memberId),
                        db.checkInDao().checkedInMemberIds(today)
                    ) { member, checkedIn -> member to checkedIn }
                        .collect { (member, checkedInIds) ->
                            if (member == null) {
                                finish() // deleted elsewhere
                                return@collect
                            }
                            currentMember = member
                            render(member, checkedInIds.contains(member.id), today)
                        }
                }
                launch {
                    db.paymentDao().observeForMember(memberId).collect { payments ->
                        paymentAdapter.submit(payments.map {
                            PaymentAdapter.Row(it, currentMember?.name.orEmpty())
                        })
                        binding.noPayments.visibility =
                            if (payments.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }

    private fun render(member: Member, checkedInToday: Boolean, today: Long) {
        binding.detailName.text = member.name
        binding.detailPhone.text = member.phone
        binding.detailPlan.text = getString(
            R.string.detail_plan_format,
            member.planType.displayName,
            MembershipCalculator.formatMoney(member.planFee)
        )
        binding.detailValidity.text = MembershipCalculator.validityLine(member, today)

        val status = MembershipCalculator.statusOf(member, today)
        val (badgeText, badgeColor) = when (status) {
            MembershipStatus.OK -> getString(R.string.status_ok) to R.color.status_safe
            MembershipStatus.EXPIRING_SOON -> getString(
                R.string.status_expiring_format,
                MembershipCalculator.daysUntil(today, member.planEndDate)
            ) to R.color.status_at_risk
            MembershipStatus.FEE_DUE -> getString(R.string.status_fee_due) to R.color.status_below
        }
        binding.detailStatus.text = badgeText
        binding.detailStatus.setTextColor(ContextCompat.getColor(this, badgeColor))

        // Photo, or a big initial-letter avatar as fallback.
        val uriString = member.photoUri
        if (!uriString.isNullOrBlank()) {
            try {
                binding.detailPhoto.setImageURI(Uri.parse(uriString))
                binding.detailPhoto.visibility = View.VISIBLE
                binding.detailAvatar.visibility = View.GONE
            } catch (_: Exception) {
                binding.detailPhoto.visibility = View.GONE
                binding.detailAvatar.visibility = View.VISIBLE
            }
        } else {
            binding.detailPhoto.visibility = View.GONE
            binding.detailAvatar.visibility = View.VISIBLE
        }
        binding.detailAvatar.text =
            member.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

        binding.checkInButton.isEnabled = !checkedInToday
        binding.checkInButton.text =
            if (checkedInToday) getString(R.string.checked_in) else getString(R.string.check_in)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    companion object {
        const val EXTRA_MEMBER_ID = "member_id"
    }
}
