package com.example.copypastemanager.model

import android.graphics.Bitmap
import java.util.Date

/**
 * Modèle de données pour les éléments sauvegardés
 */
data class ClipItem(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Date = Date(),
    val title: String = "",
    val type: ClipType,
    val text: String? = null,
    val imageUri: String? = null,
    val category: String = "Non classé"
) {
    // Champ pour stocker l'image en mémoire (non sérialisé)
    var bitmap: Bitmap? = null

    // Génère un aperçu du contenu
    fun getPreview(): String {
        return when (type) {
            ClipType.TEXT -> text?.let {
                if (it.length > 100) it.substring(0, 97) + "..." else it
            } ?: ""
            ClipType.IMAGE -> "[Image]"
        }
    }
}