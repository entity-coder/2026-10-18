package com.gymtrack.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.gymtrack.app.data.Payment
import com.gymtrack.app.databinding.ItemPaymentBinding
import com.gymtrack.app.logic.MembershipCalculator

/**
 * Payment history rows: member name, amount, date, optional note.
 */
class PaymentAdapter :
    ListAdapter<PaymentAdapter.Row, PaymentAdapter.ViewHolder>(Diff) {

    data class Row(val payment: Payment, val memberName: String)

    object Diff : DiffUtil.ItemCallback<Row>() {
        override fun areItemsTheSame(a: Row, b: Row): Boolean =
            a.payment.id == b.payment.id

        override fun areContentsTheSame(a: Row, b: Row): Boolean = a == b
    }

    inner class ViewHolder(val binding: ItemPaymentBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPaymentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val row = getItem(position)
        holder.binding.paymentMember.text = row.memberName.ifEmpty { "?" }
        holder.binding.paymentAmount.text =
            MembershipCalculator.formatMoney(row.payment.amount)
        holder.binding.paymentDate.text =
            MembershipCalculator.formatDate(row.payment.date)
        if (row.payment.note.isBlank()) {
            holder.binding.paymentNote.visibility = View.GONE
        } else {
            holder.binding.paymentNote.visibility = View.VISIBLE
            holder.binding.paymentNote.text = row.payment.note
        }
    }
}
