package com.gymtrack.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gymtrack.app.R
import com.gymtrack.app.data.Member
import com.gymtrack.app.databinding.ItemExpiringBinding
import com.gymtrack.app.logic.MembershipCalculator

/**
 * Compact rows for the dashboard "expiring soon" watchlist:
 * name + how many days are left.
 */
class ExpiringAdapter :
    ListAdapter<Member, ExpiringAdapter.ViewHolder>(Diff) {

    object Diff : DiffUtil.ItemCallback<Member>() {
        override fun areItemsTheSame(a: Member, b: Member): Boolean = a.id == b.id
        override fun areContentsTheSame(a: Member, b: Member): Boolean = a == b
    }

    inner class ViewHolder(val binding: ItemExpiringBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemExpiringBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val member = getItem(position)
        val context = holder.itemView.context
        val daysLeft = MembershipCalculator.daysUntil(
            MembershipCalculator.startOfToday(), member.planEndDate
        )
        holder.binding.expiringName.text = member.name
        holder.binding.expiringDetail.text = context.getString(
            R.string.expiring_detail_format,
            MembershipCalculator.formatDate(member.planEndDate),
            daysLeft
        )
    }
}
