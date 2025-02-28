package com.example.copypastemanager

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.example.copypastemanager.model.Category

class CategoryAdapter(
    private val context: Context,
    private var categories: List<CategoryItem> = emptyList()
) : BaseAdapter() {

    data class CategoryItem(
        val category: Category,
        val level: Int,
        var isExpanded: Boolean = false
    )

    fun updateCategories(rootCategories: List<Category>) {
        val flattenedList = mutableListOf<CategoryItem>()

        fun flatten(categories: List<Category>, level: Int) {
            for (category in categories) {
                val item = CategoryItem(category, level)
                flattenedList.add(item)

                if (item.isExpanded && category.children.isNotEmpty()) {
                    flatten(category.children, level + 1)
                }
            }
        }

        flatten(rootCategories, 0)
        categories = flattenedList
        notifyDataSetChanged()
    }

    fun toggleExpand(position: Int) {
        if (position >= 0 && position < categories.size) {
            categories[position].isExpanded = !categories[position].isExpanded
            notifyDataSetChanged()
        }
    }

    override fun getCount(): Int = categories.size

    override fun getItem(position: Int): CategoryItem = categories[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_category, parent, false)

        val item = getItem(position)
        val category = item.category

        val textCategory = view.findViewById<TextView>(R.id.textCategory)
        val iconExpand = view.findViewById<ImageView>(R.id.iconExpand)

        // Appliquer l'indentation
        val indentation = "  ".repeat(item.level)
        textCategory.text = "$indentation${category.name}"

        // Afficher/masquer l'icône d'expansion
        if (category.children.isNotEmpty()) {
            iconExpand.visibility = View.VISIBLE
            iconExpand.setImageResource(
                if (item.isExpanded) android.R.drawable.arrow_up_float
                else android.R.drawable.arrow_down_float
            )
        } else {
            iconExpand.visibility = View.GONE
        }

        return view
    }
}