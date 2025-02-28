package com.example.copypastemanager.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Category(
    val id: String = System.currentTimeMillis().toString(),
    val name: String,
    val parentId: String? = null,
    @Transient var children: MutableList<Category> = mutableListOf()
) : Parcelable {
    fun isRoot(): Boolean = parentId == null

    fun getFullPath(): String {
        if (parentId == null) return name
        return "$parentId > $name"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Category) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}