package com.hangout.app.ui.discover

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.hangout.app.data.EventItem
import com.hangout.app.databinding.ItemEventCardGridBinding

class EventGridAdapter(
    private val events: MutableList<EventItem> = mutableListOf(),
    private val onEventClick: (EventItem) -> Unit
) : RecyclerView.Adapter<EventGridAdapter.EventViewHolder>() {

    inner class EventViewHolder(private val binding: ItemEventCardGridBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(event: EventItem) {
            binding.tvEventTitle.text = event.title
            binding.tvEventDateTime.text = event.date ?: ""
            binding.tvEventLocation.text = event.location ?: "—"
            binding.tvEventPrice.text = if ((event.price ?: 0.0) == 0.0) "FREE" else "₱${event.price?.toInt()}"
            binding.tvEventFormat.text = event.format ?: "In-Person"

            if (!event.imageUrl.isNullOrBlank()) {
                Glide.with(binding.root.context)
                    .load(event.imageUrl)
                    .centerCrop()
                    .into(binding.ivEventImage)
            }

            binding.root.setOnClickListener {
                onEventClick(event)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventCardGridBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(events[position])
    }

    override fun getItemCount() = events.size

    fun updateEvents(newEvents: List<EventItem>) {
        events.clear()
        events.addAll(newEvents)
        notifyDataSetChanged()
    }

    fun clearEvents() {
        events.clear()
        notifyDataSetChanged()
    }
}
