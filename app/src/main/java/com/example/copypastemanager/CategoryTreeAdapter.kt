package com.example.copypastemanager

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.copypastemanager.model.Category

/**
 * Adaptateur pour afficher une arborescence de catégories avec indentation
 */
class CategoryTreeAdapter(
    private val context: Context,
    private var categories: List<CategoryTreeItem> = emptyList(),
    private val onCategoryClick: (Category) -> Unit,
    private val onExpandClick: (CategoryTreeItem, Int) -> Unit
) : RecyclerView.Adapter<CategoryTreeAdapter.ViewHolder>() {

    data class CategoryTreeItem(
        val category: Category,
        val level: Int,
        var isExpanded: Boolean = false
    )

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textCategory: TextView = view.findViewById(R.id.textCategory)
        val iconExpand: ImageView = view.findViewById(R.id.iconExpand)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_category, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = categories[position]
        val category = item.category

        // Appliquer l'indentation
        val indentation = "  ".repeat(item.level)
        holder.textCategory.text = "$indentation${category.name}"

        // Afficher/masquer l'icône d'expansion
        holder.iconExpand.visibility = if (hasSubs(category.id)) View.VISIBLE else View.GONE

        // Définir l'icône en fonction de l'état d'expansion
        holder.iconExpand.setImageResource(
            if (item.isExpanded) android.R.drawable.arrow_up_float
            else android.R.drawable.arrow_down_float
        )

        // Clic sur la catégorie
        holder.textCategory.setOnClickListener {
            onCategoryClick(category)
        }

        // Clic sur l'icône d'expansion
        holder.iconExpand.setOnClickListener {
            onExpandClick(item, position)
        }
    }

    private fun hasSubs(categoryId: String): Boolean {
        return categories.any { it.category.parentId == categoryId }
    }

    fun updateCategories(rootCategories: List<Category>) {
        val flattenedItems = mutableListOf<CategoryTreeItem>()

        fun flattenCategory(category: Category, level: Int, isExpanded: Boolean) {
            // Trouver l'item dans la liste actuelle pour conserver l'état d'expansion
            val existingItem = categories.find { it.category.id == category.id }
            val itemExpanded = existingItem?.isExpanded ?: isExpanded

            // Ajouter cette catégorie
            flattenedItems.add(CategoryTreeItem(category, level, itemExpanded))

            // Ajouter les sous-catégories si elle est étendue
            if (itemExpanded) {
                // Trouver toutes les sous-catégories directes
                val subCategories = rootCategories.filter { it.parentId == category.id }
                for (sub in subCategories) {
                    flattenCategory(sub, level + 1, false)
                }
            }
        }

        // Commencer par les catégories racines
        rootCategories.filter { it.parentId == null }.forEach {
            flattenCategory(it, 0, false)
        }

        categories = flattenedItems
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = categories.size

    fun toggleExpand(position: Int) {
        if (position in categories.indices) {
            val item = categories[position]
            item.isExpanded = !item.isExpanded

            // Mettre à jour la liste
            updateCategories(getAllCategoriesFlatList())
        }
    }

    private fun getAllCategoriesFlatList(): List<Category> {
        return categories.map { it.category }
    }
}