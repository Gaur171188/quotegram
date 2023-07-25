package com.nesib.quotegram.adapters

import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.nesib.quotegram.R
import com.nesib.quotegram.databinding.NotificationItemBinding
import com.nesib.quotegram.models.Notification
import com.nesib.quotegram.utils.toFormattedNumber

class NotificationAdapter : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {
    var onNotificationClickListener:((String?)->Unit)? = null

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val binding = NotificationItemBinding.bind(itemView)

        init {
            binding.root.setOnClickListener {
                onNotificationClickListener?.let {
                    it(differ.currentList[adapterPosition].likedQuoteId)
                }
            }
        }

        fun bindData(notification: Notification) {
            binding.otherPeopleCount.text = "+${(notification.likeCount-1).toFormattedNumber()}"
            val htmlText = Html.fromHtml("${notification.likeCount} people liked the quote you added from the book called <i>\"${notification.bookName}\"</i>")
            binding.notificationText.text = htmlText
            if (notification.userPhoto != null && notification.userPhoto != "") {
                binding.notificationUserPhoto.load(notification.userPhoto)
            } else {
                binding.notificationUserPhoto.load(R.drawable.user)
            }

        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): NotificationAdapter.ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.notification_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationAdapter.ViewHolder, position: Int) {
        holder.bindData(differ.currentList[position])
    }

    private val diffCallback = object : DiffUtil.ItemCallback<Notification>() {
        override fun areItemsTheSame(oldItem: Notification, newItem: Notification): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Notification, newItem: Notification): Boolean {
            return oldItem == newItem
        }

    }

    private val differ = AsyncListDiffer(this, diffCallback)

    fun setData(newList: List<Notification>) {
        differ.submitList(newList)
    }

    override fun getItemCount() = differ.currentList.size
}