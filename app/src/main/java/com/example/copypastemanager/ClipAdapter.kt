package com.example.copypastemanager

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.copypastemanager.model.ClipItem
import com.example.copypastemanager.model.ClipType
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Adaptateur pour la liste des éléments sauvegardés
 */
class ClipAdapter(
    private val context: Context,
    private val onItemClick: (ClipItem) -> Unit,
    private val onDeleteClick: (ClipItem) -> Unit
) : ListAdapter<ClipItem, ClipAdapter.ClipViewHolder>(DIFF_CALLBACK) {

    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClipViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_clip, parent, false)
        return ClipViewHolder(view)
    }

    override fun onBindViewHolder(holder: ClipViewHolder, position: Int) {
        val item = getItem(position)

        // Afficher le titre et la date
        holder.textTitle.text = item.title
        holder.textDate.text = dateFormat.format(item.timestamp)
        holder.textCategory.text = item.category

        // Gérer les différents types de contenu
        when (item.type) {
            ClipType.TEXT -> {
                holder.textContent.visibility = View.VISIBLE
                holder.imageContent.visibility = View.GONE
                holder.textContent.text = item.text
            }
            ClipType.IMAGE -> {
                holder.textContent.visibility = View.GONE
                holder.imageContent.visibility = View.VISIBLE
                holder.imageContent.setImageBitmap(item.bitmap)
            }
        }

        // Action lorsqu'on clique sur un élément (afficher le détail ou copier)
        holder.cardView.setOnClickListener {
            onItemClick(item)
        }

        // Action lorsqu'on fait un appui long (copier dans le presse-papiers)
        holder.cardView.setOnLongClickListener {
            copyToClipboard(item)
            true
        }

        // Action lorsqu'on clique sur le bouton de suppression
        holder.btnDelete.setOnClickListener {
            onDeleteClick(item)
        }
    }

    /**
     * Copie le contenu de l'élément dans le presse-papiers
     */
    private fun copyToClipboard(item: ClipItem) {
        when (item.type) {
            ClipType.TEXT -> {
                val clip = ClipData.newPlainText("Clip Manager", item.text)
                clipboardManager.setPrimaryClip(clip)
                Toast.makeText(context, "Texte copié", Toast.LENGTH_SHORT).show()
            }
            ClipType.IMAGE -> {
                Toast.makeText(context, "Image copiée", Toast.LENGTH_SHORT).show()
                // Note: La copie d'image est plus complexe et nécessiterait un ContentProvider
            }
        }
    }

    class ClipViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: CardView = itemView.findViewById(R.id.cardView)
        val textTitle: TextView = itemView.findViewById(R.id.textTitle)
        val textDate: TextView = itemView.findViewById(R.id.textDate)
        val textCategory: TextView = itemView.findViewById(R.id.textCategory)
        val textContent: TextView = itemView.findViewById(R.id.textContent)
        val imageContent: ImageView = itemView.findViewById(R.id.imageContent)
        val btnDelete: TextView = itemView.findViewById(R.id.btnDelete)
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ClipItem>() {
            override fun areItemsTheSame(oldItem: ClipItem, newItem: ClipItem): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ClipItem, newItem: ClipItem): Boolean {
                return oldItem == newItem
            }
        }
    }
}