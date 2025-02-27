package com.example.copypastemanager

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.copypastemanager.databinding.ActivityMainBinding
import com.example.copypastemanager.databinding.DialogAddCategoryBinding
import com.example.copypastemanager.databinding.DialogAddItemBinding
import com.example.copypastemanager.model.ClipItem
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: ClipViewModel
    private lateinit var clipAdapter: ClipAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        // Observe items
        viewModel.filteredItems.observe(this) { items ->
            clipAdapter.submitList(items)
            binding.textEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun setupRecyclerView() {
        clipAdapter = ClipAdapter(
            this,
            onItemClick = { item ->
                // Show item details or copy to clipboard
                Toast.makeText(this, "Détails de : ${item.title}", Toast.LENGTH_SHORT).show()
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

    private fun showAddItemDialog() {
        val dialogBinding = DialogAddItemBinding.inflate(layoutInflater)
        val dialog = Dialog(this)
        dialog.setContentView(dialogBinding.root)

        // Populate category spinner
        viewModel.categories.observe(this) { categories ->
            val adapter = ArrayAdapter(
                this,
                android.R.layout.simple_spinner_item,
                categories
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            dialogBinding.spinnerCategoryDialog.adapter = adapter
        }

        // Add category button
        dialogBinding.btnAddCategory.setOnClickListener {
            showAddCategoryDialog()
        }

        // Save button
        dialogBinding.btnSave.setOnClickListener {
            val title = dialogBinding.editTitle.text.toString()
            val content = dialogBinding.editContent.text.toString()
            val category = dialogBinding.spinnerCategoryDialog.selectedItem.toString()

            if (content.isBlank()) {
                Toast.makeText(this, "Le contenu ne peut pas être vide", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Add text item
            viewModel.addTextItem(title, content, category)
            dialog.dismiss()
        }

        // Cancel button
        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showAddCategoryDialog() {
        val dialogBinding = DialogAddCategoryBinding.inflate(layoutInflater)
        val dialog = Dialog(this)
        dialog.setContentView(dialogBinding.root)

        dialogBinding.btnSaveCategory.setOnClickListener {
            val categoryName = dialogBinding.editCategoryName.text.toString().trim()

            if (categoryName.isBlank()) {
                Toast.makeText(this, "Le nom de catégorie ne peut pas être vide", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.addCategory(categoryName)
            dialog.dismiss()
        }

        dialogBinding.btnCancelCategory.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
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