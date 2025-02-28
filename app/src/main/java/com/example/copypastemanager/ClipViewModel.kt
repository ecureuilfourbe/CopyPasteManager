package com.example.copypastemanager

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.copypastemanager.model.Category
import com.example.copypastemanager.model.ClipItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel pour les opérations sur les éléments sauvegardés
 */
class ClipViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "ClipViewModel"

    private val storageManager = ClipStorageManager(application)

    // Source de données primaire
    val allItems: LiveData<List<ClipItem>> = storageManager.items
    val allCategories: LiveData<List<Category>> = storageManager.categories

    // Catégories racines (sans parent)
    val rootCategories: LiveData<List<Category>> = allCategories.map { categories ->
        Log.d(TAG, "Mapping root categories, total: ${categories.size}")
        categories.filter { category -> category.parentId == null }.also {
            Log.d(TAG, "Root categories: ${it.size}")
        }
    }

    // Éléments filtrés
    private val _filteredItems = MediatorLiveData<List<ClipItem>>()
    val filteredItems: LiveData<List<ClipItem>> = _filteredItems

    // Paramètres de filtrage
    private val _searchQuery = MutableLiveData("")
    private val _selectedCategoryId = MutableLiveData("0") // "0" = tous
    private val _selectedSubcategoryId = MutableLiveData<String?>(null)

    init {
        Log.d(TAG, "Initializing ClipViewModel")

        // Observer les changements sur allItems
        _filteredItems.addSource(allItems) { items ->
            Log.d(TAG, "All items changed, count: ${items.size}")
            applyFilters()
        }

        // Observer les changements de recherche
        _filteredItems.addSource(_searchQuery) { query ->
            Log.d(TAG, "Search query changed: $query")
            applyFilters()
        }

        // Observer les changements de catégorie
        _filteredItems.addSource(_selectedCategoryId) { categoryId ->
            Log.d(TAG, "Selected category changed: $categoryId")
            applyFilters()
        }

        // Observer les changements de sous-catégorie
        _filteredItems.addSource(_selectedSubcategoryId) { subcategoryId ->
            Log.d(TAG, "Selected subcategory changed: $subcategoryId")
            applyFilters()
        }
    }

    /**
     * Applique les filtres actuels sur la liste des éléments
     */
    private fun applyFilters() {
        viewModelScope.launch {
            val items = allItems.value ?: emptyList()
            val query = _searchQuery.value ?: ""
            val categoryId = _selectedCategoryId.value ?: "0"
            val subcategoryId = _selectedSubcategoryId.value

            Log.d(TAG, "Applying filters - Query: $query, Category: $categoryId, Subcategory: $subcategoryId")
            Log.d(TAG, "Total items before filtering: ${items.size}")

            // Filtrer par recherche
            val searchFiltered = if (query.isNotBlank()) {
                items.filter { item ->
                    val textMatch = item.text?.contains(query, ignoreCase = true) ?: false
                    val titleMatch = item.title.contains(query, ignoreCase = true)
                    textMatch || titleMatch
                }
            } else {
                items
            }

            Log.d(TAG, "Items after search filter: ${searchFiltered.size}")

            // Filtrer par catégorie et sous-catégorie
            val categoryFiltered = if (categoryId != "0" && categoryId != "tous") {
                searchFiltered.filter { item ->
                    if (subcategoryId != null && subcategoryId.isNotEmpty()) {
                        Log.d(TAG, "Checking item with categoryId=${item.categoryId}, parentCategoryId=${item.parentCategoryId}")
                        item.categoryId == categoryId && item.parentCategoryId == subcategoryId
                    } else {
                        item.categoryId == categoryId
                    }
                }
            } else {
                searchFiltered
            }

            Log.d(TAG, "Items after category filter: ${categoryFiltered.size}")

            // Mettre à jour la liste filtrée
            _filteredItems.postValue(categoryFiltered)
        }
    }

    /**
     * Ajoute un élément texte
     */
    fun addTextItem(title: String, text: String, categoryId: String, subcategoryId: String? = null) {
        viewModelScope.launch {
            Log.d(TAG, "Adding text item - Category: $categoryId, Subcategory: $subcategoryId")
            storageManager.addTextItem(title, text, categoryId, subcategoryId)
        }
    }

    /**
     * Ajoute un élément image
     */
    fun addImageItem(title: String, bitmap: Bitmap, categoryId: String, subcategoryId: String? = null) {
        viewModelScope.launch {
            Log.d(TAG, "Adding image item - Category: $categoryId, Subcategory: $subcategoryId")
            storageManager.addImageItem(title, bitmap, categoryId, subcategoryId)
        }
    }

    /**
     * Supprime un élément
     */
    fun deleteItem(item: ClipItem) {
        viewModelScope.launch {
            Log.d(TAG, "Deleting item: ${item.id}")
            storageManager.deleteItem(item)
        }
    }

    /**
     * Ajoute une nouvelle catégorie
     */
    fun addCategory(name: String, parentId: String? = null) {
        viewModelScope.launch {
            Log.d(TAG, "Adding category: $name, Parent: $parentId")
            storageManager.addCategory(name, parentId)
        }
    }

    /**
     * Renomme une catégorie
     */
    fun renameCategory(categoryId: String, newName: String) {
        viewModelScope.launch {
            Log.d(TAG, "Renaming category $categoryId to: $newName")
            storageManager.renameCategory(categoryId, newName)
        }
    }

    /**
     * Supprime une catégorie
     */
    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            Log.d(TAG, "Deleting category: $categoryId")
            storageManager.deleteCategory(categoryId)
        }
    }

    /**
     * Récupère les sous-catégories d'une catégorie
     */
    fun getSubcategories(categoryId: String): LiveData<List<Category>> {
        Log.d(TAG, "Getting subcategories for: $categoryId")
        val result = MutableLiveData<List<Category>>()

        viewModelScope.launch {
            val subcategories = storageManager.getSubcategoriesByParentId(categoryId)
            Log.d(TAG, "Found ${subcategories.size} subcategories for $categoryId")
            result.postValue(subcategories)
        }

        return result
    }

    /**
     * Définit la requête de recherche
     */
    fun setSearchQuery(query: String) {
        Log.d(TAG, "Setting search query: $query")
        _searchQuery.value = query
    }

    /**
     * Définit la catégorie et sous-catégorie sélectionnées pour le filtrage
     */
    fun setCategory(categoryId: String, subcategoryId: String? = null) {
        Log.d(TAG, "Setting category filter - Category: $categoryId, Subcategory: $subcategoryId")
        _selectedCategoryId.value = categoryId
        _selectedSubcategoryId.value = subcategoryId
    }

    /**
     * Remet à zéro tous les filtres
     */
    fun resetFilters() {
        Log.d(TAG, "Resetting all filters")
        _searchQuery.value = ""
        _selectedCategoryId.value = "0"
        _selectedSubcategoryId.value = null
    }

    /**
     * Met à jour un élément texte existant
     */
    fun updateTextItem(item: ClipItem, newTitle: String, newText: String, newCategoryId: String, newSubcategoryId: String? = null) {
        viewModelScope.launch {
            Log.d(TAG, "Updating text item ${item.id} - Category: $newCategoryId, Subcategory: $newSubcategoryId")

            // Créer une copie mise à jour de l'élément
            val updatedItem = item.copy(
                title = newTitle,
                text = newText,
                categoryId = newCategoryId,
                parentCategoryId = newSubcategoryId
            )

            val items = allItems.value?.toMutableList() ?: mutableListOf()
            val index = items.indexOfFirst { it.id == item.id }

            if (index >= 0) {
                // Remplacer l'ancien élément par le nouveau
                items[index] = updatedItem

                // Mettre à jour via le StorageManager
                storageManager.updateItem(updatedItem)

                // La mise à jour du LiveData se fera via le StorageManager
            }
        }
    }

    /**
     * Met à jour la catégorie/sous-catégorie d'un élément
     */
    fun updateItemCategory(item: ClipItem, newCategoryId: String, newSubcategoryId: String? = null) {
        viewModelScope.launch {
            Log.d(TAG, "Updating category for item ${item.id} - New Category: $newCategoryId, New Subcategory: $newSubcategoryId")

            // Créer une copie mise à jour de l'élément avec uniquement les catégories modifiées
            val updatedItem = item.copy(
                categoryId = newCategoryId,
                parentCategoryId = newSubcategoryId
            )

            // Mettre à jour via le StorageManager
            storageManager.updateItem(updatedItem)
        }
    }
}