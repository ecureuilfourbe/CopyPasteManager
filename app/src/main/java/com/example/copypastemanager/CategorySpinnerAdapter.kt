package com.example.copypastemanager

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.copypastemanager.model.Category

/**
 * Adaptateur personnalisé pour afficher les catégories dans un Spinner
 */
class CategorySpinnerAdapter(
    private val context: Context,
    private var categories: List<CategorySpinnerItem> = emptyList()
) : BaseAdapter() {

    data class CategorySpinnerItem(
        val category: Category,
        val isSpecial: Boolean = false  // Pour des éléments comme "Aucune sous-catégorie"
    )

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createView(position, convertView, parent)
    }

    private fun createView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(android.R.layout.simple_spinner_dropdown_item, parent, false)

        val textView = view.findViewById<TextView>(android.R.id.text1)
        val item = getItem(position)

        textView.text = item.category.name

        return view
    }

    fun setCategories(newCategories: List<Category>, includeEmpty: Boolean = false) {
        val items = mutableListOf<CategorySpinnerItem>()

        // Ajouter une option vide si demandé
        if (includeEmpty) {
            items.add(CategorySpinnerItem(
                Category(id = "", name = "Aucune sous-catégorie"),
                isSpecial = true
            ))
        }

        // Ajouter les catégories
        items.addAll(newCategories.map { CategorySpinnerItem(it) })

        Log.d("CategorySpinnerAdapter", "Setting ${items.size} categories")
        this.categories = items
        notifyDataSetChanged()
    }

    fun getSelectedCategory(position: Int): Category? {
        return if (position >= 0 && position < categories.size) {
            categories[position].category
        } else null
    }

    override fun getItem(position: Int): CategorySpinnerItem {
        return categories[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return categories.size
    }

    fun findPositionById(categoryId: String): Int {
        return categories.indexOfFirst { it.category.id == categoryId }.let {
            if (it >= 0) it else 0
        }
    }
}