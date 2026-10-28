package com.example.studybuddy

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

class AddAssignmentActivity : AppCompatActivity() {

    private var dueDate = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_assignment)

        val nameInput = findViewById<EditText>(R.id.assignmentName)
        val descriptionInput = findViewById<EditText>(R.id.assignmentDescription)
        val dateButton = findViewById<Button>(R.id.dueDateButton)
        val saveButton = findViewById<Button>(R.id.saveButton)

        dateButton.setOnClickListener {
            val calendar = Calendar.getInstance()

            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    dueDate = "${selectedMonth + 1}/$selectedDay/$selectedYear"
                    dateButton.text = dueDate
                },
                year,
                month,
                day
            )

            datePicker.show()
        }

        saveButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val description = descriptionInput.text.toString().trim()

            if (name.isEmpty() || dueDate.isEmpty()) {
                Toast.makeText(
                    this,
                    "Enter a name and due date",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                val result = Intent()
                result.putExtra("name", name)
                result.putExtra("description", description)
                result.putExtra("dueDate", dueDate)

                setResult(Activity.RESULT_OK, result)
                finish()
            }
        }
    }
}
