package com.example.copypastemanager

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.copypastemanager.model.ClipItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel pour les opérations sur les éléments sauvegardés
 */
class ClipViewModel(application: Application) : AndroidViewModel(application) {

    private val storageManager = ClipStorageManager(application)

    val allItems: LiveData<List<ClipItem>> = storageManager.items
    val categories: LiveData<List<String>> = storageManager.categories

    private val _filteredItems = MutableLiveData<List<ClipItem>>(emptyList())
    val filteredItems: LiveData<List<ClipItem>> = _filteredItems

    private val _searchQuery = MutableLiveData<String>("")
    private val _currentCategory = MutableLiveData<String>("Tous")

    init {
        // Initialiser les éléments filtrés avec tous les éléments
        viewModelScope.launch {
            updateFilteredItems()
        }
    }

    /**
     * Ajoute un élément texte
     */
    fun addTextItem(title: String, text: String, category: String) {
        viewModelScope.launch {
            storageManager.addTextItem(title, text, category)
            updateFilteredItems()
        }
    }

    /**
     * Ajoute un élément image
     */
    fun addImageItem(title: String, bitmap: Bitmap, category: String) {
        viewModelScope.launch {
            storageManager.addImageItem(title, bitmap, category)
            updateFilteredItems()
        }
    }

    /**
     * Supprime un élément
     */
    fun deleteItem(item: ClipItem) {
        viewModelScope.launch {
            storageManager.deleteItem(item)
            updateFilteredItems()
        }
    }

    /**
     * Ajoute une nouvelle catégorie
     */
    fun addCategory(name: String) {
        viewModelScope.launch {
            storageManager.addCategory(name)
        }
    }

    /**
     * Définit la requête de recherche
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            updateFilteredItems()
        }
    }

    /**
     * Définit la catégorie actuelle pour le filtre
     */
    fun setCategory(category: String) {
        _currentCategory.value = category
        viewModelScope.launch {
            updateFilteredItems()
        }
    }

    /**
     * Met à jour la liste des éléments filtrés
     */
    private suspend fun updateFilteredItems() = withContext(Dispatchers.IO) {
        val query = _searchQuery.value ?: ""
        val category = _currentCategory.value ?: "Tous"

        val searchResults = if (query.isNotBlank()) {
            storageManager.searchItems(query)
        } else {
            storageManager.items.value ?: emptyList()
        }

        val filtered = if (category != "Tous") {
            searchResults.filter { it.category == category }
        } else {
            searchResults
        }

        _filteredItems.postValue(filtered)
    }
}