package com.example.demo1.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.demo1.R
import com.example.demo1.data.entity.PatrolTask

class PatrolTaskAdapter(private val onItemClick: (PatrolTask) -> Unit,
                        private val onToggleActive: (PatrolTask, Boolean) -> Unit) :
    RecyclerView.Adapter<PatrolTaskAdapter.PatrolTaskViewHolder>() {

    private var tasks: List<PatrolTask> = emptyList()

    fun setTasks(tasks: List<PatrolTask>) {
        this.tasks = tasks
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatrolTaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_patrol_task, parent, false)
        return PatrolTaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: PatrolTaskViewHolder, position: Int) {
        val currentTask = tasks[position]
        holder.bind(currentTask)
        holder.itemView.setOnClickListener { onItemClick(currentTask) }
        
        holder.activeSwitch.setOnCheckedChangeListener { _, isChecked ->
            onToggleActive(currentTask, isChecked)
        }
    }

    override fun getItemCount(): Int = tasks.size

    inner class PatrolTaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.task_name)
        private val timeTextView: TextView = itemView.findViewById(R.id.task_time)
        val activeSwitch: Switch = itemView.findViewById(R.id.task_active)

        fun bind(task: PatrolTask) {
            nameTextView.text = task.name
            timeTextView.text = String.format("%02d:%02d:%02d", task.hour, task.minute, task.second)
            activeSwitch.isChecked = task.isActive
        }
    }
}    