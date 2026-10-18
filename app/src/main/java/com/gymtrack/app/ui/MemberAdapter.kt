package com.gymtrack.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gymtrack.app.R
import com.gymtrack.app.data.Member
import com.gymtrack.app.data.MembershipStatus
import com.gymtrack.app.databinding.ItemMemberBinding
import com.gymtrack.app.logic.MembershipCalculator

/**
 * Member rows used on the Members tab (tap → detail), the Check-in tab
 * (with a Check In button), and the defaulter list.
 *
 * Pass [onCheckIn] = null to hide the button; [onMemberClick] = null to
 * disable row taps.
 */
class MemberAdapter(
    private val onMemberClick: ((Member) -> Unit)?,
    private val onCheckIn: ((Member) -> Unit)?
) : ListAdapter<MemberAdapter.Row, MemberAdapter.ViewHolder>(Diff) {

    data class Row(
        val member: Member,
        val status: MembershipStatus,
        val checkedInToday: Boolean = false
    )

    object Diff : DiffUtil.ItemCallback<Row>() {
        override fun areItemsTheSame(a: Row, b: Row): Boolean =
            a.member.id == b.member.id

        override fun areContentsTheSame(a: Row, b: Row): Boolean = a == b
    }

    inner class ViewHolder(val binding: ItemMemberBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMemberBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val row = getItem(position)
        val member = row.member
        val context = holder.itemView.context
        val b = holder.binding

        b.avatarLetter.text = member.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        b.memberName.text = member.name
        b.memberPhone.text = member.phone
        b.planBadge.text = context.getString(
            R.string.plan_badge_format,
            member.planType.displayName,
            MembershipCalculator.formatMoney(member.planFee)
        )

        // Status badge: OK / EXPIRING / FEE DUE with matching color.
        val (badgeText, badgeColor) = when (row.status) {
            MembershipStatus.OK -> context.getString(R.string.status_ok) to R.color.status_safe
            MembershipStatus.EXPIRING_SOON ->
                context.getString(
                    R.string.status_expiring_format,
                    MembershipCalculator.daysUntil(
                        MembershipCalculator.startOfToday(), member.planEndDate
                    )
                ) to R.color.status_at_risk
            MembershipStatus.FEE_DUE ->
                context.getString(R.string.status_fee_due) to R.color.status_below
        }
        b.statusBadge.text = badgeText
        b.statusBadge.setTextColor(ContextCompat.getColor(context, badgeColor))

        // Check-in button (Check-in tab only).
        if (onCheckIn != null) {
            b.checkInButton.visibility = View.VISIBLE
            if (row.checkedInToday) {
                b.checkInButton.isEnabled = false
                b.checkInButton.text = context.getString(R.string.checked_in)
            } else {
                b.checkInButton.isEnabled = true
                b.checkInButton.text = context.getString(R.string.check_in)
                b.checkInButton.setOnClickListener { onCheckIn.invoke(member) }
            }
        } else {
            b.checkInButton.visibility = View.GONE
        }

        if (onMemberClick != null) {
            b.root.setOnClickListener { onMemberClick.invoke(member) }
        } else {
            b.root.setOnClickListener(null)
            b.root.isClickable = false
        }
    }
}
