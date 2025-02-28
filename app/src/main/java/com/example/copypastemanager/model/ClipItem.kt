package com.example.copypastemanager.model

import android.graphics.Bitmap
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import java.util.Date

/**
 * Modèle de données pour les éléments sauvegardés
 */
@Parcelize
data class ClipItem(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Date = Date(),
    val title: String = "",
    val type: ClipType,
    val text: String? = null,
    val imageUri: String? = null,
    val categoryId: String = "0",
    val parentCategoryId: String? = null,
    @Transient var bitmap: @RawValue Bitmap? = null
) : Parcelable {
    // Génère un aperçu du contenu
    fun getPreview(): String {
        return when (type) {
            ClipType.TEXT -> text?.let {
                if (it.length > 100) it.substring(0, 97) + "..." else it
            } ?: ""
            ClipType.IMAGE -> "[Image]"
        }
    }

    // Pour assurer la compatibilité avec le code existant
    val category: String
        get() = categoryId
}