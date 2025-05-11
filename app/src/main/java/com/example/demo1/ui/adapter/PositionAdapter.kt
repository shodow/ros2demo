package com.example.demo1.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.demo1.R
import com.example.demo1.data.entity.Position

class PositionAdapter(private val onItemClick: (Position) -> Unit) :
    RecyclerView.Adapter<PositionAdapter.PositionViewHolder>() {

    private var positions: List<Position> = emptyList()

    fun setPositions(positions: List<Position>) {
        this.positions = positions
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PositionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_position, parent, false)
        return PositionViewHolder(view)
    }

    override fun onBindViewHolder(holder: PositionViewHolder, position: Int) {
        val currentPosition = positions[position]
        holder.bind(currentPosition)
        holder.itemView.setOnClickListener { onItemClick(currentPosition) }
    }

    override fun getItemCount(): Int = positions.size

    inner class PositionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.position_name)
        private val sequenceTextView: TextView = itemView.findViewById(R.id.position_sequence)
        private val coordinatesTextView: TextView = itemView.findViewById(R.id.position_coordinates)

        fun bind(position: Position) {
            nameTextView.text = position.name
            sequenceTextView.text = "序号: ${position.sequence}"
            coordinatesTextView.text = "坐标: (${position.x}, ${position.y}, ${position.z})"
        }
    }
}    