package com.example.copypastemanager

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.copypastemanager.model.ClipItem
import com.example.copypastemanager.model.ClipType
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var textEmpty: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialiser les vues avec findViewById
        recyclerView = findViewById(R.id.recyclerView)
        fabAdd = findViewById(R.id.fabAdd)
        textEmpty = findViewById(R.id.textEmpty)

        // Configurer le RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Configurer le bouton d'ajout
        fabAdd.setOnClickListener {
            Toast.makeText(this, "Ajouter un élément", Toast.LENGTH_SHORT).show()
        }

        // Mettre en place la liste avec quelques exemples
        setupExampleData()
    }

    private fun setupExampleData() {
        // Créer quelques éléments d'exemple
        val items = listOf(
            ClipItem(
                title = "Note importante",
                type = ClipType.TEXT,
                text = "Voici un exemple de texte qui pourrait être sauvegardé dans l'application.",
                category = "Notes"
            ),
            ClipItem(
                title = "Liens utiles",
                type = ClipType.TEXT,
                text = "https://developer.android.com\nhttps://kotlinlang.org",
                category = "Liens"
            )
        )

        // Configurer l'adaptateur
        val adapter = ClipAdapter(
            this,
            { item -> Toast.makeText(this, "Élément cliqué: ${item.title}", Toast.LENGTH_SHORT).show() },
            { item -> Toast.makeText(this, "Suppression: ${item.title}", Toast.LENGTH_SHORT).show() }
        )

        // Assigner l'adaptateur au RecyclerView
        recyclerView.adapter = adapter

        // Soumettre la liste à l'adaptateur
        adapter.submitList(items)

        // Message vide si nécessaire
        textEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }
}