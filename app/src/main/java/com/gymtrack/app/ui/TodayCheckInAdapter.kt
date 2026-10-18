package com.gymtrack.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gymtrack.app.data.CheckInWithMember
import com.gymtrack.app.databinding.ItemCheckinBinding

/**
 * Today's visitor list on the Check-in tab — just names, newest first.
 */
class TodayCheckInAdapter :
    ListAdapter<CheckInWithMember, TodayCheckInAdapter.ViewHolder>(Diff) {

    object Diff : DiffUtil.ItemCallback<CheckInWithMember>() {
        override fun areItemsTheSame(a: CheckInWithMember, b: CheckInWithMember): Boolean =
            a.checkIn.id == b.checkIn.id

        override fun areContentsTheSame(a: CheckInWithMember, b: CheckInWithMember): Boolean =
            a == b
    }

    inner class ViewHolder(val binding: ItemCheckinBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCheckinBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val row = getItem(position)
        holder.binding.avatarLetter.text =
            row.memberName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
        holder.binding.visitorName.text = row.memberName
    }
}
