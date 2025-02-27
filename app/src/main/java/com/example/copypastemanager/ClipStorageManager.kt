package com.example.copypastemanager

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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
 * Gestionnaire de stockage pour les éléments sauvegardés
 */
class ClipStorageManager(private val context: Context) {

    private val sharedPreferences = context.getSharedPreferences(
        "clip_manager_prefs", Context.MODE_PRIVATE
    )

    private val _items = MutableLiveData<List<ClipItem>>(emptyList())
    val items: LiveData<List<ClipItem>> = _items

    private val _categories = MutableLiveData<List<String>>(listOf("Non classé"))
    val categories: LiveData<List<String>> = _categories

    init {
        // Charger les éléments sauvegardés au démarrage
        loadItems()
    }

    /**
     * Charge les éléments depuis les préférences
     */
    private fun loadItems() {
        val itemsJson = sharedPreferences.getString(PREF_ITEMS, null) ?: return

        try {
            val jsonArray = JSONArray(itemsJson)
            val items = mutableListOf<ClipItem>()
            val categoriesSet = mutableSetOf("Non classé")

            for (i in 0 until jsonArray.length()) {
                val itemJson = jsonArray.getJSONObject(i)

                val type = if (itemJson.getString("type") == "TEXT")
                    ClipType.TEXT else ClipType.IMAGE

                val category = if (itemJson.has("category"))
                    itemJson.getString("category") else "Non classé"

                categoriesSet.add(category)

                val item = ClipItem(
                    id = itemJson.getLong("id"),
                    timestamp = Date(itemJson.getLong("timestamp")),
                    title = if (itemJson.has("title")) itemJson.getString("title") else "",
                    type = type,
                    text = if (type == ClipType.TEXT) itemJson.getString("text") else null,
                    imageUri = if (type == ClipType.IMAGE) itemJson.getString("imageUri") else null,
                    category = category
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

            _items.postValue(items)
            _categories.postValue(categoriesSet.toList().sorted())
        } catch (e: Exception) {
            e.printStackTrace()
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
            itemJson.put("category", item.category)

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
    }

    /**
     * Ajoute une nouvelle catégorie
     */
    suspend fun addCategory(name: String) = withContext(Dispatchers.IO) {
        if (name.isBlank()) return@withContext

        val currentCategories = _categories.value?.toMutableList() ?: mutableListOf()
        if (!currentCategories.contains(name)) {
            currentCategories.add(name)
            _categories.postValue(currentCategories.sorted())
        }
    }

    /**
     * Ajoute un élément texte
     */
    suspend fun addTextItem(title: String, text: String, category: String) = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext

        val item = ClipItem(
            title = title.ifBlank { "Texte ${Date()}" },
            type = ClipType.TEXT,
            text = text,
            category = category.ifBlank { "Non classé" }
        )

        val items = _items.value?.toMutableList() ?: mutableListOf()
        items.add(0, item) // Ajouter au début de la liste
        _items.postValue(items)
        saveItems(items)

        // Ajouter la catégorie si elle n'existe pas
        addCategory(item.category)
    }

    /**
     * Ajoute un élément image
     */
    suspend fun addImageItem(title: String, bitmap: Bitmap, category: String) = withContext(Dispatchers.IO) {
        // Sauvegarder l'image dans le stockage interne
        val filename = "clip_image_${UUID.randomUUID()}.jpg"
        val file = File(context.filesDir, filename)

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }

        val item = ClipItem(
            title = title.ifBlank { "Image ${Date()}" },
            type = ClipType.IMAGE,
            imageUri = file.absolutePath,
            category = category.ifBlank { "Non classé" }
        )

        item.bitmap = bitmap

        val items = _items.value?.toMutableList() ?: mutableListOf()
        items.add(0, item) // Ajouter au début de la liste
        _items.postValue(items)
        saveItems(items)

        // Ajouter la catégorie si elle n'existe pas
        addCategory(item.category)
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

        // Supprimer l'image si nécessaire
        if (item.type == ClipType.IMAGE && item.imageUri != null) {
            val file = File(item.imageUri)
            if (file.exists()) {
                file.delete()
            }
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
    suspend fun filterByCategory(category: String): List<ClipItem> = withContext(Dispatchers.IO) {
        val items = _items.value ?: emptyList()

        if (category == "Tous") {
            return@withContext items
        }

        return@withContext items.filter { it.category == category }
    }

    companion object {
        private const val PREF_ITEMS = "saved_items"
    }
}