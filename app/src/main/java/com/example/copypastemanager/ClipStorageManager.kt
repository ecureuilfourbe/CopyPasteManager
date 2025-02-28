package com.example.copypastemanager

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.copypastemanager.model.Category
import com.example.copypastemanager.model.ClipItem
import com.example.copypastemanager.model.ClipType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import java.util.UUID

/**
 * Gestionnaire de stockage pour les éléments sauvegardés et les catégories
 */
class ClipStorageManager(private val context: Context) {
    private val TAG = "ClipStorageManager"

    private val sharedPreferences = context.getSharedPreferences(
        "clip_manager_prefs", Context.MODE_PRIVATE
    )

    private val _items = MutableLiveData<List<ClipItem>>(emptyList())
    val items: LiveData<List<ClipItem>> = _items

    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> = _categories

    init {
        Log.d(TAG, "Initializing ClipStorageManager")

        // Initialiser les catégories par défaut et les charger
        if (sharedPreferences.getString(PREF_CATEGORIES, null) == null) {
            Log.d(TAG, "No categories found, initializing defaults")
            val defaultCategories = listOf(
                Category(id = "0", name = "Non classé")
            )
            saveCategories(defaultCategories)
            _categories.postValue(defaultCategories)
        } else {
            Log.d(TAG, "Loading existing categories")
            loadCategories()
        }

        // Charger les éléments
        loadItems()
    }

    /**
     * Charge les catégories depuis les préférences
     */
    private fun loadCategories() {
        val categoriesJson = sharedPreferences.getString(PREF_CATEGORIES, null)

        if (categoriesJson == null) {
            // Catégorie par défaut
            val defaultCategories = listOf(Category(id = "0", name = "Non classé"))
            _categories.postValue(defaultCategories)
            return
        }

        try {
            val jsonArray = JSONArray(categoriesJson)
            val categoriesList = mutableListOf<Category>()

            // S'assurer que la catégorie "Non classé" existe toujours
            var hasDefaultCategory = false

            // Charger toutes les catégories
            for (i in 0 until jsonArray.length()) {
                val categoryJson = jsonArray.getJSONObject(i)

                val id = categoryJson.getString("id")
                if (id == "0") hasDefaultCategory = true

                val category = Category(
                    id = id,
                    name = categoryJson.getString("name"),
                    parentId = if (categoryJson.has("parentId") && !categoryJson.isNull("parentId"))
                        categoryJson.getString("parentId") else null
                )

                categoriesList.add(category)
            }

            // Ajouter la catégorie par défaut si nécessaire
            if (!hasDefaultCategory) {
                categoriesList.add(Category(id = "0", name = "Non classé"))
            }

            Log.d(TAG, "Loaded ${categoriesList.size} categories")

            // Construire l'arborescence
            buildCategoryTree(categoriesList)

        } catch (e: Exception) {
            Log.e(TAG, "Error loading categories", e)
            // S'assurer qu'il y a au moins la catégorie par défaut
            _categories.postValue(listOf(Category(id = "0", name = "Non classé")))
        }
    }

    /**
     * Construit l'arborescence des catégories
     */
    private fun buildCategoryTree(flatCategories: List<Category>) {
        // Créer une map pour un accès rapide par ID
        val categoryMap = flatCategories.associateBy { it.id }.toMutableMap()

        // Vider les enfants existants
        flatCategories.forEach { it.children.clear() }

        // Organiser les enfants
        for (category in flatCategories) {
            if (category.parentId != null) {
                val parent = categoryMap[category.parentId]
                parent?.children?.add(category)
            }
        }

        Log.d(TAG, "Category tree built with ${flatCategories.size} categories")

        // Mettre à jour la liste
        _categories.postValue(flatCategories)
    }

    /**
     * Sauvegarde les catégories dans les préférences
     */
    private fun saveCategories(categories: List<Category>) {
        val jsonArray = JSONArray()

        for (category in categories) {
            val categoryJson = JSONObject()
            categoryJson.put("id", category.id)
            categoryJson.put("name", category.name)
            if (category.parentId != null) {
                categoryJson.put("parentId", category.parentId)
            }

            jsonArray.put(categoryJson)
        }

        sharedPreferences.edit()
            .putString(PREF_CATEGORIES, jsonArray.toString())
            .apply()

        Log.d(TAG, "Saved ${categories.size} categories")
    }

    /**
     * Charge les éléments depuis les préférences
     */
    private fun loadItems() {
        val itemsJson = sharedPreferences.getString(PREF_ITEMS, null)

        if (itemsJson == null) {
            _items.postValue(emptyList())
            return
        }

        try {
            val jsonArray = JSONArray(itemsJson)
            val items = mutableListOf<ClipItem>()

            for (i in 0 until jsonArray.length()) {
                val itemJson = jsonArray.getJSONObject(i)

                val type = if (itemJson.getString("type") == "TEXT")
                    ClipType.TEXT else ClipType.IMAGE

                // Utiliser categoryId et parentCategoryId, avec rétrocompatibilité
                val categoryId = if (itemJson.has("categoryId"))
                    itemJson.getString("categoryId")
                else if (itemJson.has("category"))
                    itemJson.getString("category")
                else
                    "0"

                val parentCategoryId = if (itemJson.has("parentCategoryId") && !itemJson.isNull("parentCategoryId"))
                    itemJson.getString("parentCategoryId")
                else
                    null

                val item = ClipItem(
                    id = itemJson.getLong("id"),
                    timestamp = Date(itemJson.getLong("timestamp")),
                    title = if (itemJson.has("title")) itemJson.getString("title") else "",
                    type = type,
                    text = if (type == ClipType.TEXT) itemJson.getString("text") else null,
                    imageUri = if (type == ClipType.IMAGE) itemJson.getString("imageUri") else null,
                    categoryId = categoryId,
                    parentCategoryId = parentCategoryId
                )

                // Charger l'image si nécessaire
                if (type == ClipType.IMAGE && item.imageUri != null) {
                    val file = File(item.imageUri)
                    if (file.exists()) {
                        item.bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    }
                }

                items.add(item)
            }

            Log.d(TAG, "Loaded ${items.size} items")
            _items.postValue(items)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading items", e)
            _items.postValue(emptyList())
        }
    }

    /**
     * Sauvegarde les éléments dans les préférences
     */
    private fun saveItems(items: List<ClipItem>) {
        val jsonArray = JSONArray()

        for (item in items) {
            val itemJson = JSONObject()
            itemJson.put("id", item.id)
            itemJson.put("timestamp", item.timestamp.time)
            itemJson.put("title", item.title)
            itemJson.put("type", item.type.name)
            itemJson.put("categoryId", item.categoryId)

            if (item.parentCategoryId != null) {
                itemJson.put("parentCategoryId", item.parentCategoryId)
            }

            if (item.type == ClipType.TEXT) {
                itemJson.put("text", item.text)
            } else {
                itemJson.put("imageUri", item.imageUri)
            }

            jsonArray.put(itemJson)
        }

        sharedPreferences.edit()
            .putString(PREF_ITEMS, jsonArray.toString())
            .apply()

        Log.d(TAG, "Saved ${items.size} items")
    }

    /**
     * Ajoute une nouvelle catégorie
     */
    suspend fun addCategory(name: String, parentId: String? = null) = withContext(Dispatchers.IO) {
        if (name.isBlank()) return@withContext

        val currentCategories = _categories.value?.toMutableList() ?: mutableListOf()

        // Vérifier si la catégorie existe déjà
        if (currentCategories.any { it.name == name && it.parentId == parentId }) {
            Log.d(TAG, "Category already exists: $name with parent $parentId")
            return@withContext
        }

        // Créer une nouvelle catégorie
        val newCategory = Category(
            name = name,
            parentId = parentId
        )

        Log.d(TAG, "Adding new category: $name with ID ${newCategory.id}, parent: $parentId")

        // Ajouter à la liste
        currentCategories.add(newCategory)

        // Mettre à jour l'arborescence et sauvegarder
        buildCategoryTree(currentCategories)
        saveCategories(currentCategories)
    }

    /**
     * Renomme une catégorie
     */
    suspend fun renameCategory(categoryId: String, newName: String) = withContext(Dispatchers.IO) {
        if (newName.isBlank()) return@withContext

        Log.d(TAG, "Renaming category $categoryId to: $newName")

        val currentCategories = _categories.value?.toMutableList() ?: mutableListOf()

        // Trouver et renommer la catégorie
        val categoryIndex = currentCategories.indexOfFirst { it.id == categoryId }
        if (categoryIndex >= 0) {
            val categoryToRename = currentCategories[categoryIndex]
            val updatedCategory = categoryToRename.copy(name = newName)
            currentCategories[categoryIndex] = updatedCategory

            // Mettre à jour l'arborescence et sauvegarder
            buildCategoryTree(currentCategories)
            saveCategories(currentCategories)

            Log.d(TAG, "Category renamed successfully")
        } else {
            Log.d(TAG, "Failed to rename category: not found")
        }
    }

    /**
     * Supprime une catégorie
     */
    suspend fun deleteCategory(categoryId: String) = withContext(Dispatchers.IO) {
        if (categoryId == "0") {
            Log.d(TAG, "Cannot delete default category")
            return@withContext
        }

        Log.d(TAG, "Deleting category: $categoryId")

        val currentCategories = _categories.value?.toMutableList() ?: mutableListOf()

        // Trouver toutes les sous-catégories récursivement
        val categoriesToDelete = mutableListOf<String>()

        fun findAllChildCategories(parentId: String) {
            categoriesToDelete.add(parentId)

            // Trouver les enfants directs
            val children = currentCategories.filter { it.parentId == parentId }
            for (child in children) {
                findAllChildCategories(child.id)
            }
        }

        findAllChildCategories(categoryId)

        // Filtrer les catégories à garder
        val remainingCategories = currentCategories.filterNot { it.id in categoriesToDelete }

        // Réaffecter les éléments des catégories supprimées à "Non classé"
        val items = _items.value?.toMutableList() ?: mutableListOf()
        val updatedItems = items.map { item ->
            if (item.categoryId in categoriesToDelete) {
                // Si la catégorie est supprimée, mettre l'élément dans "Non classé"
                item.copy(categoryId = "0", parentCategoryId = null)
            } else if (item.parentCategoryId in categoriesToDelete) {
                // Si la sous-catégorie est supprimée, garder la catégorie mais enlever la sous-catégorie
                item.copy(parentCategoryId = null)
            } else {
                item
            }
        }

        // Mettre à jour les listes et sauvegarder
        buildCategoryTree(remainingCategories.toMutableList())
        saveCategories(remainingCategories)

        _items.postValue(updatedItems)
        saveItems(updatedItems)

        Log.d(TAG, "Category and ${categoriesToDelete.size - 1} subcategories deleted")
    }

    /**
     * Récupère les sous-catégories d'une catégorie
     */
    fun getSubcategoriesByParentId(parentId: String): List<Category> {
        val allCategories = _categories.value ?: emptyList()
        val subcategories = allCategories.filter { it.parentId == parentId }
        Log.d(TAG, "Getting subcategories for $parentId: found ${subcategories.size}")
        return subcategories
    }

    /**
     * Ajoute un élément texte
     */
    suspend fun addTextItem(title: String, text: String, categoryId: String, subCategoryId: String? = null) = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext

        val effectiveCategoryId = categoryId.ifEmpty { "0" }
        val effectiveSubCategoryId = if (subCategoryId.isNullOrEmpty()) null else subCategoryId

        Log.d(TAG, "Adding text item: '${title.take(20)}...', category: $effectiveCategoryId, subcategory: $effectiveSubCategoryId")

        val item = ClipItem(
            title = title.ifBlank { "Texte ${Date()}" },
            type = ClipType.TEXT,
            text = text,
            categoryId = effectiveCategoryId,
            parentCategoryId = effectiveSubCategoryId
        )

        val items = _items.value?.toMutableList() ?: mutableListOf()
        items.add(0, item) // Ajouter au début de la liste
        _items.postValue(items)
        saveItems(items)

        Log.d(TAG, "Text item added, total items: ${items.size}")
    }

    /**
     * Ajoute un élément image
     */
    suspend fun addImageItem(title: String, bitmap: Bitmap, categoryId: String, subCategoryId: String? = null) = withContext(Dispatchers.IO) {
        // Sauvegarder l'image dans le stockage interne
        val filename = "clip_image_${UUID.randomUUID()}.jpg"
        val file = File(context.filesDir, filename)

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }

        val effectiveCategoryId = categoryId.ifEmpty { "0" }
        val effectiveSubCategoryId = if (subCategoryId.isNullOrEmpty()) null else subCategoryId

        Log.d(TAG, "Adding image item: '${title.take(20)}...', category: $effectiveCategoryId, subcategory: $effectiveSubCategoryId")

        val item = ClipItem(
            title = title.ifBlank { "Image ${Date()}" },
            type = ClipType.IMAGE,
            imageUri = file.absolutePath,
            categoryId = effectiveCategoryId,
            parentCategoryId = effectiveSubCategoryId
        )

        item.bitmap = bitmap

        val items = _items.value?.toMutableList() ?: mutableListOf()
        items.add(0, item) // Ajouter au début de la liste
        _items.postValue(items)
        saveItems(items)

        Log.d(TAG, "Image item added, total items: ${items.size}")
    }

    /**
     * Supprime un élément
     */
    suspend fun deleteItem(item: ClipItem) = withContext(Dispatchers.IO) {
        val items = _items.value?.toMutableList() ?: return@withContext

        // Supprimer l'élément
        items.removeAll { it.id == item.id }
        _items.postValue(items)
        saveItems(items)

        Log.d(TAG, "Item deleted: ${item.id}, remaining items: ${items.size}")

        // Supprimer l'image si nécessaire
        if (item.type == ClipType.IMAGE && item.imageUri != null) {
            val file = File(item.imageUri)
            if (file.exists()) {
                file.delete()
            }
        }
    }

    /**
     * Met à jour un élément existant
     */
    suspend fun updateItem(updatedItem: ClipItem) = withContext(Dispatchers.IO) {
        Log.d(TAG, "Updating item: ${updatedItem.id}")

        val items = _items.value?.toMutableList() ?: mutableListOf()
        val index = items.indexOfFirst { it.id == updatedItem.id }

        if (index >= 0) {
            // Remplacer l'ancien élément par le nouveau
            items[index] = updatedItem

            // Mettre à jour la liste et sauvegarder
            _items.postValue(items)
            saveItems(items)

            Log.d(TAG, "Item updated successfully")
        } else {
            Log.d(TAG, "Failed to update item: not found")
        }
    }

    /**
     * Recherche des éléments
     */
    suspend fun searchItems(query: String): List<ClipItem> = withContext(Dispatchers.IO) {
        val items = _items.value ?: emptyList()

        if (query.isBlank()) {
            return@withContext items
        }

        return@withContext items.filter { item ->
            val textMatch = item.text?.contains(query, ignoreCase = true) ?: false
            val titleMatch = item.title.contains(query, ignoreCase = true)

            textMatch || titleMatch
        }
    }

    /**
     * Filtre les éléments par catégorie
     */
    suspend fun filterByCategory(categoryId: String, subCategoryId: String? = null): List<ClipItem> = withContext(Dispatchers.IO) {
        val items = _items.value ?: emptyList()

        Log.d(TAG, "Filtering by category: $categoryId, subcategory: $subCategoryId")

        if (categoryId == "tous" || categoryId == "0") {
            Log.d(TAG, "No category filter, returning all ${items.size} items")
            return@withContext items
        }

        return@withContext items.filter {
            if (subCategoryId != null && subCategoryId.isNotEmpty()) {
                Log.d(TAG, "Filtering by subcategory: checking item categoryId=${it.categoryId}, parentCategoryId=${it.parentCategoryId}")
                it.categoryId == categoryId && it.parentCategoryId == subCategoryId
            } else {
                Log.d(TAG, "Filtering by main category only: checking item categoryId=${it.categoryId}")
                it.categoryId == categoryId
            }
        }.also {
            Log.d(TAG, "Filter result: ${it.size} items")
        }
    }

    companion object {
        private const val PREF_ITEMS = "saved_items"
        private const val PREF_CATEGORIES = "saved_categories"
    }
}