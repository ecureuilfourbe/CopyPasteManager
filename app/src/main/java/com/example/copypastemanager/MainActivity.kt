package com.example.copypastemanager

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.copypastemanager.databinding.ActivityMainBinding
import com.example.copypastemanager.databinding.DialogAddCategoryBinding
import com.example.copypastemanager.databinding.DialogAddItemBinding
import com.example.copypastemanager.model.Category
import com.example.copypastemanager.model.ClipItem

class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: ClipViewModel
    private lateinit var clipAdapter: ClipAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "onCreate called")

        // Use view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[ClipViewModel::class.java]

        // Setup RecyclerView
        setupRecyclerView()

        // Setup FAB
        binding.fabAdd.setOnClickListener {
            showAddItemDialog()
        }

        // Observer les éléments filtrés
        viewModel.filteredItems.observe(this) { items ->
            Log.d(TAG, "Filtered items updated: ${items.size} items")
            clipAdapter.submitList(items)
            binding.textEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        }

        // Observer toutes les catégories pour déboguer
        viewModel.allCategories.observe(this) { categories ->
            Log.d(TAG, "All categories updated: ${categories.size} categories")
            for (category in categories) {
                Log.d(TAG, "Category: ${category.name}, ID: ${category.id}, Parent: ${category.parentId}")
            }
        }

        // Réinitialiser les filtres pour afficher tous les éléments au démarrage
        viewModel.resetFilters()
    }

    private fun setupRecyclerView() {
        clipAdapter = ClipAdapter(
            this,
            onItemClick = { item ->
                // Afficher les options d'édition
                showItemOptionsDialog(item)
            },
            onDeleteClick = { item ->
                // Confirm deletion
                AlertDialog.Builder(this)
                    .setTitle("Supprimer l'élément")
                    .setMessage("Voulez-vous vraiment supprimer cet élément ?")
                    .setPositiveButton("Oui") { _, _ ->
                        viewModel.deleteItem(item)
                    }
                    .setNegativeButton("Non", null)
                    .show()
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = clipAdapter
        }
    }

    private fun showItemOptionsDialog(item: ClipItem) {
        val options = arrayOf("Copier", "Éditer", "Modifier la catégorie")

        AlertDialog.Builder(this)
            .setTitle("Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // Copier le contenu
                        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clipData = ClipData.newPlainText("Clip Manager", item.text)
                        clipboardManager.setPrimaryClip(clipData)
                        Toast.makeText(this, "Texte copié", Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        // Éditer l'élément
                        showEditItemDialog(item)
                    }
                    2 -> {
                        // Modifier la catégorie
                        showChangeCategoryDialog(item)
                    }
                }
            }
            .show()
    }

    private fun showAddItemDialog() {
        val dialogBinding = DialogAddItemBinding.inflate(layoutInflater)
        val dialog = Dialog(this)
        dialog.setContentView(dialogBinding.root)

        var selectedCategoryId: String = "0"
        var selectedSubcategoryId: String? = null

        // Créer et configurer l'adaptateur de catégories
        val categoryAdapter = CategorySpinnerAdapter(this)
        dialogBinding.spinnerCategory.adapter = categoryAdapter

        // Créer et configurer l'adaptateur de sous-catégories
        val subcategoryAdapter = CategorySpinnerAdapter(this)
        dialogBinding.spinnerSubcategory.adapter = subcategoryAdapter

        // Observer les catégories racines
        viewModel.rootCategories.observe(this) { rootCategories ->
            // S'assurer que la catégorie "Non classé" existe toujours
            val displayCategories = rootCategories.toMutableList()
            if (displayCategories.none { it.id == "0" }) {
                displayCategories.add(0, Category(id = "0", name = "Non classé"))
            }

            Log.d(TAG, "Updating category spinner with ${displayCategories.size} categories")
            categoryAdapter.setCategories(displayCategories)

            // Sélectionner la catégorie actuelle si définie
            if (selectedCategoryId.isNotEmpty()) {
                val position = categoryAdapter.findPositionById(selectedCategoryId)
                dialogBinding.spinnerCategory.setSelection(position)
            }
        }

        // Listener pour le changement de catégorie
        dialogBinding.spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategory = categoryAdapter.getSelectedCategory(position)
                selectedCategoryId = selectedCategory?.id ?: "0"

                Log.d(TAG, "Category selected: ${selectedCategory?.name}, ID: $selectedCategoryId")

                // Mise à jour des sous-catégories
                if (selectedCategoryId != "0") {
                    viewModel.getSubcategories(selectedCategoryId).observe(this@MainActivity) { subcategories ->
                        Log.d(TAG, "Got ${subcategories.size} subcategories for $selectedCategoryId")

                        if (subcategories.isNotEmpty()) {
                            // Mettre à jour l'adaptateur de sous-catégories
                            subcategoryAdapter.setCategories(subcategories, includeEmpty = true)

                            // Afficher le layout des sous-catégories
                            dialogBinding.layoutSubcategory.visibility = View.VISIBLE

                            // Sélectionner la sous-catégorie actuelle si définie
                            if (selectedSubcategoryId != null) {
                                val subcatPosition = subcategoryAdapter.findPositionById(selectedSubcategoryId!!)
                                dialogBinding.spinnerSubcategory.setSelection(subcatPosition)
                            }
                        } else {
                            // Cacher le layout des sous-catégories s'il n'y en a pas
                            dialogBinding.layoutSubcategory.visibility = View.GONE
                            selectedSubcategoryId = null
                        }
                    }
                } else {
                    // Cacher le layout des sous-catégories pour "Non classé"
                    dialogBinding.layoutSubcategory.visibility = View.GONE
                    selectedSubcategoryId = null
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedCategoryId = "0"
                dialogBinding.layoutSubcategory.visibility = View.GONE
                selectedSubcategoryId = null
            }
        }

        // Listener pour le changement de sous-catégorie
        dialogBinding.spinnerSubcategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = subcategoryAdapter.getSelectedCategory(position)
                selectedSubcategoryId = if (selected?.id?.isNotEmpty() == true) selected.id else null

                Log.d(TAG, "Subcategory selected: ${selected?.name}, ID: $selectedSubcategoryId")
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedSubcategoryId = null
            }
        }

        // Bouton pour ajouter une catégorie
        dialogBinding.btnAddCategory.setOnClickListener {
            showAddCategoryDialog(null) // null pour une catégorie racine
        }

        // Bouton pour ajouter une sous-catégorie
        dialogBinding.btnAddSubcategory.setOnClickListener {
            if (selectedCategoryId != "0") {
                showAddCategoryDialog(selectedCategoryId)
            } else {
                Toast.makeText(this, "Sélectionne d'abord une catégorie", Toast.LENGTH_SHORT).show()
            }
        }

        // Bouton Sauvegarder
        dialogBinding.btnSave.setOnClickListener {
            val title = dialogBinding.editTitle.text.toString()
            val content = dialogBinding.editContent.text.toString()

            if (content.isBlank()) {
                Toast.makeText(this, "Le contenu ne peut pas être vide", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d(TAG, "Saving item - Title: $title, Category: $selectedCategoryId, Subcategory: $selectedSubcategoryId")

            // Ajouter l'élément avec la catégorie et sous-catégorie sélectionnées
            viewModel.addTextItem(title, content, selectedCategoryId, selectedSubcategoryId)
            Toast.makeText(this, "Élément ajouté", Toast.LENGTH_SHORT).show()

            dialog.dismiss()
        }

        // Bouton Annuler
        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    // Utilise le même DialogAddItemBinding pour éditer un élément existant
    private fun showEditItemDialog(item: ClipItem) {
        val dialogBinding = DialogAddItemBinding.inflate(layoutInflater)
        val dialog = Dialog(this)
        dialog.setContentView(dialogBinding.root)

        // Changer le titre du dialogue
        dialog.setTitle("Modifier l'élément")

        // Pré-remplir les champs
        dialogBinding.editTitle.setText(item.title)
        dialogBinding.editContent.setText(item.text)

        var selectedCategoryId: String = item.categoryId
        var selectedSubcategoryId: String? = item.parentCategoryId

        // Créer et configurer l'adaptateur de catégories
        val categoryAdapter = CategorySpinnerAdapter(this)
        dialogBinding.spinnerCategory.adapter = categoryAdapter

        // Créer et configurer l'adaptateur de sous-catégories
        val subcategoryAdapter = CategorySpinnerAdapter(this)
        dialogBinding.spinnerSubcategory.adapter = subcategoryAdapter

        // Observer les catégories racines
        viewModel.rootCategories.observe(this) { rootCategories ->
            // S'assurer que la catégorie "Non classé" existe toujours
            val displayCategories = rootCategories.toMutableList()
            if (displayCategories.none { it.id == "0" }) {
                displayCategories.add(0, Category(id = "0", name = "Non classé"))
            }

            categoryAdapter.setCategories(displayCategories)

            // Sélectionner la catégorie actuelle
            val categoryPosition = categoryAdapter.findPositionById(selectedCategoryId)
            dialogBinding.spinnerCategory.setSelection(categoryPosition)
        }

        // Listener pour le changement de catégorie
        dialogBinding.spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCategory = categoryAdapter.getSelectedCategory(position)
                selectedCategoryId = selectedCategory?.id ?: "0"

                // Mise à jour des sous-catégories
                if (selectedCategoryId != "0") {
                    viewModel.getSubcategories(selectedCategoryId).observe(this@MainActivity) { subcategories ->
                        if (subcategories.isNotEmpty()) {
                            // Mettre à jour l'adaptateur de sous-catégories
                            subcategoryAdapter.setCategories(subcategories, includeEmpty = true)

                            // Afficher le layout des sous-catégories
                            dialogBinding.layoutSubcategory.visibility = View.VISIBLE

                            // Sélectionner la sous-catégorie actuelle si définie
                            if (selectedSubcategoryId != null) {
                                val subcatPosition = subcategoryAdapter.findPositionById(selectedSubcategoryId!!)
                                dialogBinding.spinnerSubcategory.setSelection(subcatPosition)
                            }
                        } else {
                            // Cacher le layout des sous-catégories s'il n'y en a pas
                            dialogBinding.layoutSubcategory.visibility = View.GONE
                            selectedSubcategoryId = null
                        }
                    }
                } else {
                    // Cacher le layout des sous-catégories pour "Non classé"
                    dialogBinding.layoutSubcategory.visibility = View.GONE
                    selectedSubcategoryId = null
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedCategoryId = "0"
                dialogBinding.layoutSubcategory.visibility = View.GONE
                selectedSubcategoryId = null
            }
        }

        // Listener pour le changement de sous-catégorie
        dialogBinding.spinnerSubcategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = subcategoryAdapter.getSelectedCategory(position)
                selectedSubcategoryId = if (selected?.id?.isNotEmpty() == true) selected.id else null
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedSubcategoryId = null
            }
        }

        // Bouton pour ajouter une catégorie
        dialogBinding.btnAddCategory.setOnClickListener {
            showAddCategoryDialog(null)
        }

        // Bouton pour ajouter une sous-catégorie
        dialogBinding.btnAddSubcategory.setOnClickListener {
            if (selectedCategoryId != "0") {
                showAddCategoryDialog(selectedCategoryId)
            } else {
                Toast.makeText(this, "Sélectionne d'abord une catégorie", Toast.LENGTH_SHORT).show()
            }
        }

        // Bouton Sauvegarder
        dialogBinding.btnSave.setOnClickListener {
            val newTitle = dialogBinding.editTitle.text.toString()
            val newContent = dialogBinding.editContent.text.toString()

            if (newContent.isBlank()) {
                Toast.makeText(this, "Le contenu ne peut pas être vide", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Mettre à jour l'élément
            viewModel.updateTextItem(item, newTitle, newContent, selectedCategoryId, selectedSubcategoryId)
            Toast.makeText(this, "Élément mis à jour", Toast.LENGTH_SHORT).show()

            dialog.dismiss()
        }

        // Bouton Annuler
        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    // Utilise un AlertDialog standard pour changer la catégorie
    private fun showChangeCategoryDialog(item: ClipItem) {
        // Récupérer les catégories pour le menu
        val categories = viewModel.allCategories.value ?: emptyList()
        val categoryNames = categories.map { it.name }.toTypedArray()

        // Afficher un simple sélecteur de catégorie
        AlertDialog.Builder(this)
            .setTitle("Choisir une catégorie")
            .setItems(categoryNames) { _, which ->
                val newCategoryId = categories[which].id

                // Si c'est une catégorie principale
                if (categories[which].parentId == null) {
                    // Vérifier s'il y a des sous-catégories
                    val subcategories = viewModel.getSubcategories(newCategoryId).value ?: emptyList()

                    if (subcategories.isNotEmpty()) {
                        // Afficher les sous-catégories
                        val subcategoryNames = subcategories.map { it.name }.toTypedArray()

                        AlertDialog.Builder(this)
                            .setTitle("Choisir une sous-catégorie")
                            .setItems(subcategoryNames) { _, subWhich ->
                                // Mettre à jour avec catégorie et sous-catégorie
                                val newSubcategoryId = subcategories[subWhich].id
                                viewModel.updateItemCategory(item, newCategoryId, newSubcategoryId)
                                Toast.makeText(this, "Catégorie mise à jour", Toast.LENGTH_SHORT).show()
                            }
                            .setNeutralButton("Aucune sous-catégorie") { _, _ ->
                                // Mise à jour avec catégorie uniquement
                                viewModel.updateItemCategory(item, newCategoryId, null)
                                Toast.makeText(this, "Catégorie mise à jour", Toast.LENGTH_SHORT).show()
                            }
                            .setNegativeButton("Annuler", null)
                            .show()
                    } else {
                        // Pas de sous-catégories disponibles
                        viewModel.updateItemCategory(item, newCategoryId, null)
                        Toast.makeText(this, "Catégorie mise à jour", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // C'est déjà une sous-catégorie
                    viewModel.updateItemCategory(item, categories[which].parentId ?: "0", newCategoryId)
                    Toast.makeText(this, "Catégorie mise à jour", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun showAddCategoryDialog(parentId: String? = null) {
        val dialogBinding = DialogAddCategoryBinding.inflate(layoutInflater)
        val dialog = Dialog(this)
        dialog.setContentView(dialogBinding.root)

        // Modifier le titre en fonction de si c'est une catégorie ou sous-catégorie
        val titleText = if (parentId == null) {
            "Ajouter une nouvelle catégorie"
        } else {
            "Ajouter une sous-catégorie"
        }
        dialogBinding.textTitle.text = titleText

        dialogBinding.btnSaveCategory.setOnClickListener {
            val categoryName = dialogBinding.editCategoryName.text.toString().trim()

            if (categoryName.isBlank()) {
                Toast.makeText(this, "Le nom de catégorie ne peut pas être vide", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d(TAG, "Adding category: $categoryName with parent $parentId")
            viewModel.addCategory(categoryName, parentId)
            Toast.makeText(this, "Catégorie ajoutée", Toast.LENGTH_SHORT).show()

            dialog.dismiss()
        }

        dialogBinding.btnCancelCategory.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    // Utilise un AlertDialog standard pour gérer les catégories
    private fun showManageCategoriesDialog() {
        // Obtenir toutes les catégories sauf "Non classé"
        val categories = viewModel.allCategories.value?.filter { it.id != "0" } ?: emptyList()

        if (categories.isEmpty()) {
            // Pas de catégories à gérer, proposer d'en créer une
            AlertDialog.Builder(this)
                .setTitle("Aucune catégorie")
                .setMessage("Voulez-vous créer une nouvelle catégorie?")
                .setPositiveButton("Créer") { _, _ -> showAddCategoryDialog() }
                .setNegativeButton("Annuler", null)
                .show()
            return
        }

        // Afficher la liste des catégories existantes
        val items = categories.map { it.name }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Gérer les catégories")
            .setItems(items) { _, which ->
                val selectedCategory = categories[which]

                // Options pour la catégorie sélectionnée
                val options = arrayOf("Renommer", "Supprimer")

                AlertDialog.Builder(this)
                    .setTitle(selectedCategory.name)
                    .setItems(options) { _, optionIndex ->
                        when (optionIndex) {
                            0 -> {
                                // Renommer
                                val input = EditText(this)
                                input.setText(selectedCategory.name)

                                AlertDialog.Builder(this)
                                    .setTitle("Renommer la catégorie")
                                    .setView(input)
                                    .setPositiveButton("Enregistrer") { _, _ ->
                                        val newName = input.text.toString().trim()
                                        if (newName.isNotEmpty()) {
                                            viewModel.renameCategory(selectedCategory.id, newName)
                                            Toast.makeText(this, "Catégorie renommée", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .setNegativeButton("Annuler", null)
                                    .show()
                            }
                            1 -> {
                                // Supprimer
                                AlertDialog.Builder(this)
                                    .setTitle("Confirmer la suppression")
                                    .setMessage("Voulez-vous vraiment supprimer cette catégorie? Les éléments seront déplacés vers 'Non classé'.")
                                    .setPositiveButton("Supprimer") { _, _ ->
                                        viewModel.deleteCategory(selectedCategory.id)
                                        Toast.makeText(this, "Catégorie supprimée", Toast.LENGTH_SHORT).show()
                                    }
                                    .setNegativeButton("Annuler", null)
                                    .show()
                            }
                        }
                    }
                    .show()
            }
            .setPositiveButton("Nouvelle catégorie") { _, _ ->
                showAddCategoryDialog()
            }
            .setNegativeButton("Fermer", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add_category -> {
                showAddCategoryDialog()
                true
            }
            R.id.action_manage_categories -> {
                showManageCategoriesDialog()
                true
            }
            R.id.action_about -> {
                showAboutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.about_title)
            .setMessage(R.string.about_message)
            .setPositiveButton(R.string.ok, null)
            .show()
    }
}