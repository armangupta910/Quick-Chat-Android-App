package com.example.chat

import android.provider.CalendarContract.Colors
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val messages: MutableList<Message>, private val gravities: MutableList<Int>) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView) {
        val messageContent: TextView = itemView.findViewById(R.id.message_content)
        val card:CardView = itemView.findViewById(R.id.card)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]
        val gravity = gravities[position] // Get gravity for the current message

        holder.messageContent.text = message.content

        // Adjust gravity dynamically
        val layoutParams = holder.card.layoutParams as FrameLayout.LayoutParams
        layoutParams.gravity = if (gravity == 1) android.view.Gravity.END else android.view.Gravity.START
        holder.card.layoutParams = layoutParams

        // Change card background color based on gravity value
        if (gravity == 0) {
            holder.card.setCardBackgroundColor(holder.card.context.getColor(android.R.color.darker_gray))
        } else {
            holder.card.setCardBackgroundColor(ContextCompat.getColor(holder.card.context, R.color.newGreen))
        }
    }

    override fun getItemCount() = messages.size

    fun addMessage(message: Message, gravity: Int) {
        messages.add(message)
        gravities.add(gravity)
        notifyItemInserted(messages.size - 1)
    }
}
