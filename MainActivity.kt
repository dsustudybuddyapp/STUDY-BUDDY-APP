package com.example.studybuddy

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.view.ViewStub
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Calendar
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private data class Assignment(
        val name: String,
        val description: String,
        val dueDate: String,
        var completed: Boolean = false
    )

    private val assignments = mutableListOf<Assignment>()
    private val preferences by lazy {
        getSharedPreferences("study_buddy", MODE_PRIVATE)
    }

    private lateinit var bottomNavigation: BottomNavigationView
    private var addForm: View? = null
    private var dueDate = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_assignments)
        applyScreenInsets()

        loadAssignments()
        displayAssignments()

        bottomNavigation = findViewById(R.id.bottomNavigation)
        val list = findViewById<View>(R.id.assignmentList)
        val backToList = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                bottomNavigation.selectedItemId = R.id.nav_assignments
            }
        }
        onBackPressedDispatcher.addCallback(this, backToList)

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_assignments -> {
                    if (addForm?.isShown == true) {
                        WindowInsetsControllerCompat(window, list)
                            .hide(WindowInsetsCompat.Type.ime())
                        addForm?.findFocus()?.clearFocus()
                        addForm?.visibility = View.GONE
                    }
                    list.visibility = View.VISIBLE
                    backToList.isEnabled = false
                    true
                }
                R.id.nav_add_assignment -> {
                    val form = getOrCreateAddForm()
                    list.visibility = View.GONE
                    form.visibility = View.VISIBLE
                    backToList.isEnabled = true
                    true
                }
                else -> false
            }
        }

        savedInstanceState?.getString("formDueDate")?.let {
            dueDate = it
            getOrCreateAddForm().visibility = View.GONE
        }
        bottomNavigation.selectedItemId =
            savedInstanceState?.getInt("selectedTab", R.id.nav_assignments)
                ?: R.id.nav_assignments
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("selectedTab", bottomNavigation.selectedItemId)
        if (addForm != null) outState.putString("formDueDate", dueDate)
    }

    private fun getOrCreateAddForm(): View {
        addForm?.let { return it }
        val form = findViewById<ViewStub>(R.id.addAssignmentStub).inflate()
        addForm = form
        val nameInput = form.findViewById<EditText>(R.id.assignmentName)
        val descriptionInput = form.findViewById<EditText>(R.id.assignmentDescription)
        val dateButton = form.findViewById<Button>(R.id.dueDateButton)
        val defaultDateLabel = dateButton.text
        if (dueDate.isNotEmpty()) dateButton.text = dueDate

        dateButton.setOnClickListener {
            val today = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    dueDate = "${month + 1}/$day/$year"
                    dateButton.text = dueDate
                },
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                today.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        form.findViewById<Button>(R.id.saveButton).setOnClickListener {
            val name = nameInput.text.toString().trim()
            if (name.isBlank() || dueDate.isBlank()) {
                Toast.makeText(this, "Enter a name and due date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            assignments.add(Assignment(name, descriptionInput.text.toString().trim(), dueDate))
            saveAssignments()
            displayAssignments()
            bottomNavigation.selectedItemId = R.id.nav_assignments
            nameInput.text.clear()
            descriptionInput.text.clear()
            dueDate = ""
            dateButton.text = defaultDateLabel
        }
        return form
    }

    private fun displayAssignments() {
        val container = findViewById<LinearLayout>(R.id.assignmentsContainer)
        val emptyMessage = findViewById<TextView>(R.id.emptyAssignmentsText)
        container.removeAllViews()
        emptyMessage.visibility = if (assignments.isEmpty()) View.VISIBLE else View.GONE

        assignments.forEach { assignment ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(12), 0, dp(12))
            }
            val checkbox = CheckBox(this).apply {
                contentDescription = "${assignment.name} completion"
                isChecked = assignment.completed
                setOnCheckedChangeListener { _, checked ->
                    assignment.completed = checked
                    saveAssignments()
                }
            }
            val details = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(8), dp(8), 0, 0)
            }
            details.addView(TextView(this).apply {
                text = assignment.name
                textSize = 18f
            })
            if (assignment.description.isNotBlank()) {
                details.addView(TextView(this).apply {
                    text = assignment.description
                    textSize = 14f
                    setPadding(0, dp(6), 0, 0)
                })
            }
            details.addView(TextView(this).apply {
                text = "Due: ${assignment.dueDate}"
                textSize = 14f
                setPadding(0, dp(6), 0, 0)
            })
            val deleteOnLongPress = View.OnLongClickListener {
                AlertDialog.Builder(this)
                    .setTitle("Delete assignment?")
                    .setMessage(assignment.name)
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete") { _, _ ->
                        val index = assignments.indexOfFirst { it === assignment }
                        if (index >= 0) {
                            assignments.removeAt(index)
                            saveAssignments()
                            displayAssignments()
                        }
                    }
                    .show()
                true
            }
            row.setOnLongClickListener(deleteOnLongPress)
            checkbox.setOnLongClickListener(deleteOnLongPress)
            row.addView(checkbox, LinearLayout.LayoutParams(dp(48), dp(48)))
            row.addView(details, LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            ))
            container.addView(row)
        }
    }

    private fun saveAssignments() {
        val array = JSONArray()
        assignments.forEach { assignment ->
            array.put(JSONObject().apply {
                put("name", assignment.name)
                put("description", assignment.description)
                put("dueDate", assignment.dueDate)
                put("completed", assignment.completed)
            })
        }
        preferences.edit().putString("assignments", array.toString()).apply()
    }

    private fun loadAssignments() {
        assignments.clear()
        val array = JSONArray(preferences.getString("assignments", "[]") ?: "[]")
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            assignments.add(Assignment(
                name = item.getString("name"),
                description = item.optString("description"),
                dueDate = item.getString("dueDate"),
                completed = item.optBoolean("completed", false)
            ))
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}

internal fun AppCompatActivity.applyScreenInsets() {
    val root = findViewById<View>(R.id.main)
    ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        // BottomNavigationView handles its own bottom inset.
        view.setPadding(bars.left, bars.top, bars.right, 0)
        insets
    }
}
