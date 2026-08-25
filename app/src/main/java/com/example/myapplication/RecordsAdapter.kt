package com.example.myapplication

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView

class RecordsAdapter(
    private val context: Context,
    private var records: ArrayList<RecordModel>,
    private val dbHelper: DatabaseHelper
) : RecyclerView.Adapter<RecordsAdapter.RecordViewHolder>() {

    class RecordViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvCity: TextView = itemView.findViewById(R.id.tvCity)
        val tvDetails: TextView = itemView.findViewById(R.id.tvDetails)
        val btnEdit: Button = itemView.findViewById(R.id.btnEdit)
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_record, parent, false)
        return RecordViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        val item = records[position]
        holder.tvCity.text = item.city
        holder.tvDetails.text = "${item.temp} | ${item.condition}"

        // Delete record functionality
        holder.btnDelete.setOnClickListener {
            dbHelper.deleteRecord(item.id)
            records.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, records.size)
        }

        // Update record functionality via Dialog
        holder.btnEdit.setOnClickListener {
            showUpdateDialog(item, position)
        }
    }

    override fun getItemCount(): Int = records.size

    private fun showUpdateDialog(item: RecordModel, position: Int) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Update Record for ${item.city}")

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val etTemp = EditText(context).apply { hint = "New Temp"; setText(item.temp) }
        val etCondition = EditText(context).apply { hint = "New Condition"; setText(item.condition) }

        layout.addView(etTemp)
        layout.addView(etCondition)
        builder.setView(layout)

        builder.setPositiveButton("Update") { _, _ ->
            val updatedTemp = etTemp.text.toString()
            val updatedCond = etCondition.text.toString()
            if (updatedTemp.isNotEmpty() && updatedCond.isNotEmpty()) {
                dbHelper.updateRecord(item.id, updatedTemp, updatedCond)
                records[position] = RecordModel(item.id, item.city, updatedTemp, updatedCond)
                notifyItemChanged(position)
            }
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }
}